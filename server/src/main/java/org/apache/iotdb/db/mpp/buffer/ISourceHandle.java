/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.mpp.buffer;

import org.apache.iotdb.mpp.rpc.thrift.TFragmentInstanceId;
import org.apache.iotdb.tsfile.read.common.block.TsBlock;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;

public interface ISourceHandle extends Closeable {

  /** Get the local fragment instance ID that this source handle belongs to. */
  TFragmentInstanceId getLocalFragmentInstanceId();

  /** Get the local plan node ID that this source handle belongs to. */
  String getLocalPlanNodeId();

  /** Get the total amount of memory used by buffered tsblocks. */
  long getBufferRetainedSizeInBytes();

  /**
   * Get a {@link TsBlock}. If the source handle is blocked, a null will be returned. A {@link
   * RuntimeException} will be thrown if any error happened.
   */
  TsBlock receive();

  /** If there are more tsblocks. */
  boolean isFinished();

  /** Get a future that will be completed when the input buffer is not empty. */
  ListenableFuture<Void> isBlocked();

  /** If this handle is closed. */
  boolean isClosed();

  /**
   * Close the handle. Discard all tsblocks which may still be in the memory buffer and complete the
   * future returned by {@link #isBlocked()}.
   */
  @Override
  void close();
}
