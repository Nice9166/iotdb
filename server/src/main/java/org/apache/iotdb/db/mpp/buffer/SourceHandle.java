/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.mpp.buffer;

import org.apache.iotdb.common.rpc.thrift.TEndPoint;
import org.apache.iotdb.commons.client.IClientManager;
import org.apache.iotdb.commons.client.sync.SyncDataNodeDataBlockServiceClient;
import org.apache.iotdb.db.mpp.buffer.DataBlockManager.SourceHandleListener;
import org.apache.iotdb.db.mpp.memory.LocalMemoryManager;
import org.apache.iotdb.mpp.rpc.thrift.TAcknowledgeDataBlockEvent;
import org.apache.iotdb.mpp.rpc.thrift.TFragmentInstanceId;
import org.apache.iotdb.mpp.rpc.thrift.TGetDataBlockRequest;
import org.apache.iotdb.mpp.rpc.thrift.TGetDataBlockResponse;
import org.apache.iotdb.tsfile.read.common.block.TsBlock;
import org.apache.iotdb.tsfile.read.common.block.column.TsBlockSerde;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang3.Validate;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.Futures.nonCancellationPropagating;

public class SourceHandle implements ISourceHandle {

  private static final Logger logger = LoggerFactory.getLogger(SourceHandle.class);

  public static final int MAX_ATTEMPT_TIMES = 3;

  private final TEndPoint remoteEndpoint;
  private final TFragmentInstanceId remoteFragmentInstanceId;
  private final TFragmentInstanceId localFragmentInstanceId;
  private final String localPlanNodeId;
  private final LocalMemoryManager localMemoryManager;
  private final ExecutorService executorService;
  private final TsBlockSerde serde;
  private final SourceHandleListener sourceHandleListener;

  private final Map<Integer, TsBlock> sequenceIdToTsBlock = new HashMap<>();
  private final Map<Integer, Long> sequenceIdToDataBlockSize = new HashMap<>();

  private final IClientManager<TEndPoint, SyncDataNodeDataBlockServiceClient>
      dataBlockServiceClientManager;

  private volatile SettableFuture<Void> blocked = SettableFuture.create();
  private long bufferRetainedSizeInBytes = 0L;
  private int currSequenceId = 0;
  private int nextSequenceId = 0;
  private int lastSequenceId = Integer.MAX_VALUE;
  private boolean closed = false;

  public SourceHandle(
      TEndPoint remoteEndpoint,
      TFragmentInstanceId remoteFragmentInstanceId,
      TFragmentInstanceId localFragmentInstanceId,
      String localPlanNodeId,
      LocalMemoryManager localMemoryManager,
      ExecutorService executorService,
      TsBlockSerde serde,
      SourceHandleListener sourceHandleListener,
      IClientManager<TEndPoint, SyncDataNodeDataBlockServiceClient> dataBlockServiceClientManager) {
    this.remoteEndpoint = Validate.notNull(remoteEndpoint);
    this.remoteFragmentInstanceId = Validate.notNull(remoteFragmentInstanceId);
    this.localFragmentInstanceId = Validate.notNull(localFragmentInstanceId);
    this.localPlanNodeId = Validate.notNull(localPlanNodeId);
    this.localMemoryManager = Validate.notNull(localMemoryManager);
    this.executorService = Validate.notNull(executorService);
    this.serde = Validate.notNull(serde);
    this.sourceHandleListener = Validate.notNull(sourceHandleListener);
    bufferRetainedSizeInBytes = 0L;
    this.dataBlockServiceClientManager = dataBlockServiceClientManager;
  }

  @Override
  public TsBlock receive() {
    if (closed) {
      throw new IllegalStateException("Source handle is closed.");
    }
    if (!blocked.isDone()) {
      throw new IllegalStateException("Source handle is blocked.");
    }
    TsBlock tsBlock;
    synchronized (this) {
      tsBlock = sequenceIdToTsBlock.remove(currSequenceId);
      currSequenceId += 1;
      bufferRetainedSizeInBytes -= tsBlock.getRetainedSizeInBytes();
      localMemoryManager
          .getQueryPool()
          .free(localFragmentInstanceId.getQueryId(), tsBlock.getRetainedSizeInBytes());

      if (sequenceIdToTsBlock.isEmpty() && !isFinished()) {
        blocked = SettableFuture.create();
      }
    }
    if (isFinished()) {
      sourceHandleListener.onFinished(this);
    }
    trySubmitGetDataBlocksTask();
    return tsBlock;
  }

  private synchronized void trySubmitGetDataBlocksTask() {
    final int startSequenceId = nextSequenceId;
    int endSequenceId = nextSequenceId;
    long reservedBytes = 0L;
    ListenableFuture<?> future = null;
    while (sequenceIdToDataBlockSize.containsKey(endSequenceId)) {
      Long bytesToReserve = sequenceIdToDataBlockSize.get(endSequenceId);
      if (bytesToReserve == null) {
        throw new IllegalStateException("Data block size is null.");
      }
      future =
          localMemoryManager
              .getQueryPool()
              .reserve(localFragmentInstanceId.getQueryId(), bytesToReserve);
      if (future.isDone()) {
        endSequenceId += 1;
        reservedBytes += bytesToReserve;
        bufferRetainedSizeInBytes += bytesToReserve;
      } else {
        break;
      }
    }

    if (future == null) {
      // Next data block not generated yet. Do nothing.
      return;
    }

    if (future.isDone()) {
      nextSequenceId = endSequenceId;
      executorService.submit(new GetDataBlocksTask(startSequenceId, endSequenceId, reservedBytes));
    } else {
      nextSequenceId = endSequenceId + 1;
      // The future being not completed indicates,
      //   1. Memory has been reserved for blocks in [startSequenceId, endSequenceId).
      //   2. Memory reservation for block whose sequence ID equals endSequenceId is blocked.
      //   3. Have not reserve memory for the rest of blocks.
      //
      //  startSequenceId             endSequenceId  endSequenceId + 1
      //         |-------- reserved --------|--- blocked ---|--- not reserved ---|

      if (endSequenceId > startSequenceId) {
        // Memory has been reserved. Submit a GetDataBlocksTask for these blocks.
        executorService.submit(
            new GetDataBlocksTask(startSequenceId, endSequenceId, reservedBytes));
      }

      // Submit a GetDataBlocksTask when memory is freed.
      final int sequenceIdOfUnReservedDataBlock = endSequenceId;
      final long sizeOfUnReservedDataBlock = sequenceIdToDataBlockSize.get(endSequenceId);
      future.addListener(
          () -> {
            executorService.submit(
                new GetDataBlocksTask(
                    sequenceIdOfUnReservedDataBlock,
                    sequenceIdOfUnReservedDataBlock + 1,
                    sizeOfUnReservedDataBlock));
            bufferRetainedSizeInBytes += sizeOfUnReservedDataBlock;
          },
          executorService);

      // Schedule another call of trySubmitGetDataBlocksTask for the rest of blocks.
      future.addListener(SourceHandle.this::trySubmitGetDataBlocksTask, executorService);
    }
  }

  @Override
  public ListenableFuture<Void> isBlocked() {
    if (closed) {
      throw new IllegalStateException("Source handle is closed.");
    }
    return nonCancellationPropagating(blocked);
  }

  synchronized void setNoMoreTsBlocks(int lastSequenceId) {
    this.lastSequenceId = lastSequenceId;
    if (!blocked.isDone() && remoteTsBlockedConsumedUp()) {
      blocked.set(null);
    }
  }

  synchronized void updatePendingDataBlockInfo(int startSequenceId, List<Long> dataBlockSizes) {
    for (int i = 0; i < dataBlockSizes.size(); i++) {
      sequenceIdToDataBlockSize.put(i + startSequenceId, dataBlockSizes.get(i));
    }
    trySubmitGetDataBlocksTask();
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    if (blocked != null && !blocked.isDone()) {
      blocked.cancel(true);
    }
    sequenceIdToDataBlockSize.clear();
    if (bufferRetainedSizeInBytes > 0) {
      localMemoryManager
          .getQueryPool()
          .free(localFragmentInstanceId.getQueryId(), bufferRetainedSizeInBytes);
      bufferRetainedSizeInBytes = 0;
    }
    closed = true;
    sourceHandleListener.onClosed(this);
  }

  @Override
  public boolean isFinished() {
    return remoteTsBlockedConsumedUp();
  }

  // Return true indicates two points:
  //   1. Remote SinkHandle has told SourceHandle the total count of TsBlocks by lastSequenceId
  //   2. All the TsBlocks has been consumed up
  private boolean remoteTsBlockedConsumedUp() {
    return currSequenceId - 1 == lastSequenceId;
  }

  public TEndPoint getRemoteEndpoint() {
    return remoteEndpoint;
  }

  public TFragmentInstanceId getRemoteFragmentInstanceId() {
    return remoteFragmentInstanceId.deepCopy();
  }

  public TFragmentInstanceId getLocalFragmentInstanceId() {
    return localFragmentInstanceId;
  }

  public String getLocalPlanNodeId() {
    return localPlanNodeId;
  }

  @Override
  public long getBufferRetainedSizeInBytes() {
    return bufferRetainedSizeInBytes;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SourceHandle.class.getSimpleName() + "[", "]")
        .add("remoteEndpoint='" + remoteEndpoint + "'")
        .add("remoteFragmentInstanceId=" + remoteFragmentInstanceId)
        .add("localFragmentInstanceId=" + localFragmentInstanceId)
        .add("localPlanNodeId='" + localPlanNodeId + "'")
        .toString();
  }

  /** Get data blocks from an upstream fragment instance. */
  class GetDataBlocksTask implements Runnable {
    private final int startSequenceId;
    private final int endSequenceId;
    private final long reservedBytes;

    GetDataBlocksTask(int startSequenceId, int endSequenceId, long reservedBytes) {
      Validate.isTrue(
          startSequenceId >= 0,
          "Start sequence ID should be greater than or equal to zero. Start sequence ID: "
              + startSequenceId);
      this.startSequenceId = startSequenceId;
      Validate.isTrue(
          endSequenceId > startSequenceId,
          "End sequence ID should be greater than the start sequence ID. Start sequence ID: "
              + startSequenceId
              + ", end sequence ID: "
              + endSequenceId);
      this.endSequenceId = endSequenceId;
      Validate.isTrue(reservedBytes > 0L, "Reserved bytes should be greater than zero.");
      this.reservedBytes = reservedBytes;
    }

    @Override
    public void run() {
      logger.debug(
          "Get data blocks [{}, {}) from {} for plan node {} of {}.",
          startSequenceId,
          endSequenceId,
          remoteFragmentInstanceId,
          localPlanNodeId,
          localFragmentInstanceId);
      TGetDataBlockRequest req =
          new TGetDataBlockRequest(remoteFragmentInstanceId, startSequenceId, endSequenceId);
      int attempt = 0;
      while (attempt < MAX_ATTEMPT_TIMES) {
        attempt += 1;
        SyncDataNodeDataBlockServiceClient client = null;
        try {
          client = dataBlockServiceClientManager.borrowClient(remoteEndpoint);
          if (client == null) {
            logger.warn("can't get client for node {}", remoteEndpoint);
            if (attempt == MAX_ATTEMPT_TIMES) {
              throw new TException("Can't get client for node " + remoteEndpoint);
            }
            continue;
          }
          TGetDataBlockResponse resp = client.getDataBlock(req);
          List<TsBlock> tsBlocks = new ArrayList<>(resp.getTsBlocks().size());
          for (ByteBuffer byteBuffer : resp.getTsBlocks()) {
            TsBlock tsBlock = serde.deserialize(byteBuffer);
            tsBlocks.add(tsBlock);
          }
          synchronized (SourceHandle.this) {
            if (closed) {
              return;
            }
            for (int i = startSequenceId; i < endSequenceId; i++) {
              sequenceIdToTsBlock.put(i, tsBlocks.get(i - startSequenceId));
            }
            if (!blocked.isDone()) {
              blocked.set(null);
            }
          }
          executorService.submit(
              new SendAcknowledgeDataBlockEventTask(startSequenceId, endSequenceId));
          break;
        } catch (Throwable e) {
          if (e instanceof TException && client != null) {
            client.close();
          }
          logger.error(
              "Failed to get data block from {} due to {}, attempt times: {}",
              remoteFragmentInstanceId,
              e.getMessage(),
              attempt);
          if (attempt == MAX_ATTEMPT_TIMES) {
            synchronized (SourceHandle.this) {
              bufferRetainedSizeInBytes -= reservedBytes;
              localMemoryManager
                  .getQueryPool()
                  .free(localFragmentInstanceId.getQueryId(), reservedBytes);
              sourceHandleListener.onFailure(SourceHandle.this, e);
            }
          }
        } finally {
          if (client != null) {
            client.returnSelf();
          }
        }
      }
      // TODO: try to issue another GetDataBlocksTask to make the query run faster.
    }
  }

  class SendAcknowledgeDataBlockEventTask implements Runnable {

    private final int startSequenceId;
    private final int endSequenceId;

    public SendAcknowledgeDataBlockEventTask(int startSequenceId, int endSequenceId) {
      this.startSequenceId = startSequenceId;
      this.endSequenceId = endSequenceId;
    }

    @Override
    public void run() {
      logger.debug(
          "Send ack data block event [{}, {}) to {}.",
          startSequenceId,
          endSequenceId,
          remoteFragmentInstanceId);
      int attempt = 0;
      TAcknowledgeDataBlockEvent acknowledgeDataBlockEvent =
          new TAcknowledgeDataBlockEvent(remoteFragmentInstanceId, startSequenceId, endSequenceId);
      while (attempt < MAX_ATTEMPT_TIMES) {
        attempt += 1;
        SyncDataNodeDataBlockServiceClient client = null;
        try {
          client = dataBlockServiceClientManager.borrowClient(remoteEndpoint);
          if (client == null) {
            logger.warn("can't get client for node {}", remoteEndpoint);
            if (attempt == MAX_ATTEMPT_TIMES) {
              throw new TException("Can't get client for node " + remoteEndpoint);
            }
          } else {
            client.onAcknowledgeDataBlockEvent(acknowledgeDataBlockEvent);
            break;
          }
        } catch (Throwable e) {
          if (e instanceof TException && client != null) {
            client.close();
          }
          logger.error(
              "Failed to send ack data block event [{}, {}) to {} due to {}, attempt times: {}",
              startSequenceId,
              endSequenceId,
              remoteFragmentInstanceId,
              e.getMessage(),
              attempt);
          if (attempt == MAX_ATTEMPT_TIMES) {
            synchronized (this) {
              sourceHandleListener.onFailure(SourceHandle.this, e);
            }
          }
        } finally {
          if (client != null) {
            client.returnSelf();
          }
        }
      }
    }
  }
}
