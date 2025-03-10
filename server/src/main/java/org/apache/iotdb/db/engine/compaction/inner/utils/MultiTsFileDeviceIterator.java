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
package org.apache.iotdb.db.engine.compaction.inner.utils;

import org.apache.iotdb.db.engine.modification.Modification;
import org.apache.iotdb.db.engine.modification.ModificationFile;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.query.control.FileReaderManager;
import org.apache.iotdb.db.utils.QueryUtils;
import org.apache.iotdb.tsfile.file.metadata.AlignedChunkMetadata;
import org.apache.iotdb.tsfile.file.metadata.ChunkMetadata;
import org.apache.iotdb.tsfile.file.metadata.IChunkMetadata;
import org.apache.iotdb.tsfile.file.metadata.TimeseriesMetadata;
import org.apache.iotdb.tsfile.read.TsFileDeviceIterator;
import org.apache.iotdb.tsfile.read.TsFileSequenceReader;
import org.apache.iotdb.tsfile.utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MultiTsFileDeviceIterator implements AutoCloseable {
  private List<TsFileResource> tsFileResources;
  private Map<TsFileResource, TsFileSequenceReader> readerMap = new HashMap<>();
  private Map<TsFileResource, TsFileDeviceIterator> deviceIteratorMap = new HashMap<>();
  private Map<TsFileResource, List<Modification>> modificationCache = new HashMap<>();
  private Pair<String, Boolean> currentDevice = null;

  /** Used for inner space compaction. */
  public MultiTsFileDeviceIterator(List<TsFileResource> tsFileResources) throws IOException {
    this.tsFileResources = new ArrayList<>(tsFileResources);
    Collections.sort(this.tsFileResources, TsFileResource::compareFileName);
    try {
      for (TsFileResource tsFileResource : this.tsFileResources) {
        TsFileSequenceReader reader = new TsFileSequenceReader(tsFileResource.getTsFilePath());
        readerMap.put(tsFileResource, reader);
        deviceIteratorMap.put(tsFileResource, reader.getAllDevicesIteratorWithIsAligned());
      }
    } catch (Throwable throwable) {
      // if there is any exception occurs
      // existing readers should be closed
      for (TsFileSequenceReader reader : readerMap.values()) {
        reader.close();
      }
      throw throwable;
    }
  }

  /** Used for cross space compaction. */
  public MultiTsFileDeviceIterator(
      List<TsFileResource> seqResources, List<TsFileResource> unseqResources) throws IOException {
    for (TsFileResource tsFileResource : seqResources) {
      TsFileSequenceReader reader =
          FileReaderManager.getInstance().get(tsFileResource.getTsFilePath(), true);
      readerMap.put(tsFileResource, reader);
      deviceIteratorMap.put(tsFileResource, reader.getAllDevicesIteratorWithIsAligned());
    }
    for (TsFileResource tsFileResource : unseqResources) {
      TsFileSequenceReader reader =
          FileReaderManager.getInstance().get(tsFileResource.getTsFilePath(), true);
      readerMap.put(tsFileResource, reader);
      deviceIteratorMap.put(tsFileResource, reader.getAllDevicesIteratorWithIsAligned());
    }
  }

  public boolean hasNextDevice() {
    boolean hasNext = false;
    for (TsFileDeviceIterator iterator : deviceIteratorMap.values()) {
      hasNext =
          hasNext
              || iterator.hasNext()
              || (iterator.current() != null && !iterator.current().equals(currentDevice));
    }
    return hasNext;
  }

  /**
   * Return next device that is minimal in lexicographical order
   *
   * @return Pair of device full path and whether this device is aligned
   */
  public Pair<String, Boolean> nextDevice() {
    List<TsFileResource> toBeRemovedResources = new LinkedList<>();
    Pair<String, Boolean> minDevice = null;
    for (TsFileResource resource : deviceIteratorMap.keySet()) {
      TsFileDeviceIterator deviceIterator = deviceIteratorMap.get(resource);
      if (deviceIterator.current() == null || deviceIterator.current().equals(currentDevice)) {
        if (deviceIterator.hasNext()) {
          deviceIterator.next();
        } else {
          // this iterator does not have next device
          // remove them after the loop
          toBeRemovedResources.add(resource);
          continue;
        }
      }
      if (minDevice == null || minDevice.left.compareTo(deviceIterator.current().left) > 0) {
        minDevice = deviceIterator.current();
      }
    }
    currentDevice = minDevice;
    // remove the iterator with no device remaining
    for (TsFileResource resource : toBeRemovedResources) {
      deviceIteratorMap.remove(resource);
    }
    return currentDevice;
  }

  /**
   * return MeasurementIterator, who iterates the measurements of not aligned device
   *
   * @param device the full path of the device to be iterated
   * @return
   * @throws IOException
   */
  public MeasurementIterator iterateNotAlignedSeries(
      String device, boolean derserializeTimeseriesMetadata) throws IOException {
    return new MeasurementIterator(readerMap, device, derserializeTimeseriesMetadata);
  }

  public AlignedMeasurementIterator iterateAlignedSeries(String device) {
    return new AlignedMeasurementIterator(device, new ArrayList<>(readerMap.values()));
  }

  /**
   * return a list of the tsfile reader and its aligned chunk metadata list for the aligned device
   * which this iterator is visiting. If there is any modification for this device, it will be
   * applied to the AlignedChunkMetadata, so that the user of this function can reader Chunk
   * directly using the reader and the chunkMetadata returned. Notice, if the TsFile corresponding
   * to a TsFileSequenceReader does not contain the current device, the TsFileSequenceReader will
   * not appear in the return list.
   *
   * @return a list of pair(TsFileSequenceReader, the list of AlignedChunkMetadata for current
   *     device)
   * @throws IOException
   */
  public LinkedList<Pair<TsFileSequenceReader, List<AlignedChunkMetadata>>>
      getReaderAndChunkMetadataForCurrentAlignedSeries() throws IOException {
    if (currentDevice == null || !currentDevice.right) {
      return null;
    }

    LinkedList<Pair<TsFileSequenceReader, List<AlignedChunkMetadata>>> readerAndChunkMetadataList =
        new LinkedList<>();
    for (TsFileResource tsFileResource : tsFileResources) {
      if (!deviceIteratorMap.containsKey(tsFileResource)) {
        continue;
      }
      TsFileDeviceIterator iterator = deviceIteratorMap.get(tsFileResource);
      if (!currentDevice.equals(iterator.current())) {
        continue;
      }
      TsFileSequenceReader reader = readerMap.get(tsFileResource);
      List<AlignedChunkMetadata> alignedChunkMetadataList =
          reader.getAlignedChunkMetadata(currentDevice.left);
      applyModificationForAlignedChunkMetadataList(tsFileResource, alignedChunkMetadataList);
      readerAndChunkMetadataList.add(new Pair<>(reader, alignedChunkMetadataList));
    }

    return readerAndChunkMetadataList;
  }

  /**
   * collect the modification for current device and apply it to the alignedChunkMetadataList
   *
   * @param tsFileResource
   * @param alignedChunkMetadataList
   */
  private void applyModificationForAlignedChunkMetadataList(
      TsFileResource tsFileResource, List<AlignedChunkMetadata> alignedChunkMetadataList) {
    ModificationFile modificationFile = ModificationFile.getNormalMods(tsFileResource);
    if (!modificationFile.exists()) {
      return;
    }
    List<Modification> modifications =
        modificationCache.computeIfAbsent(
            tsFileResource, r -> new ArrayList<>(modificationFile.getModifications()));

    // construct the input params List<List<Modification>> for QueryUtils.modifyAlignedChunkMetaData
    AlignedChunkMetadata alignedChunkMetadata = alignedChunkMetadataList.get(0);
    List<IChunkMetadata> valueChunkMetadataList = alignedChunkMetadata.getValueChunkMetadataList();
    List<List<Modification>> modificationForCurDevice = new ArrayList<>();
    for (int i = 0; i < valueChunkMetadataList.size(); ++i) {
      modificationForCurDevice.add(new ArrayList<>());
    }

    for (Modification modification : modifications) {
      if (modification.getDevice().equals(currentDevice.left)) {
        for (int i = 0; i < valueChunkMetadataList.size(); ++i) {
          IChunkMetadata chunkMetadata = valueChunkMetadataList.get(i);
          if (modification.getMeasurement().equals(chunkMetadata.getMeasurementUid())) {
            modificationForCurDevice.get(i).add(modification);
          }
        }
      }
    }

    QueryUtils.modifyAlignedChunkMetaData(alignedChunkMetadataList, modificationForCurDevice);
  }

  @Override
  public void close() throws IOException {
    for (TsFileSequenceReader reader : readerMap.values()) {
      reader.close();
    }
  }

  public class AlignedMeasurementIterator {
    private List<TsFileSequenceReader> sequenceReaders;
    private String device;

    private AlignedMeasurementIterator(String device, List<TsFileSequenceReader> sequenceReaders) {
      this.device = device;
      this.sequenceReaders = sequenceReaders;
    }

    public Set<String> getAllMeasurements() throws IOException {
      Map<String, TimeseriesMetadata> deviceMeasurementsMap = new ConcurrentHashMap<>();
      for (TsFileSequenceReader reader : sequenceReaders) {
        deviceMeasurementsMap.putAll(reader.readDeviceMetadata(device));
      }
      deviceMeasurementsMap.remove("");
      return deviceMeasurementsMap.keySet();
    }
  }

  public class MeasurementIterator {
    private Map<TsFileResource, TsFileSequenceReader> readerMap;
    private String device;
    private String currentCompactingSeries = null;
    private LinkedList<String> seriesInThisIteration = new LinkedList<>();
    // tsfile sequence reader -> series -> list<ChunkMetadata>
    private Map<TsFileSequenceReader, Map<String, List<ChunkMetadata>>> chunkMetadataCacheMap =
        new HashMap<>();
    // this map cache the chunk metadata list iterator for each tsfile
    // the iterator return a batch of series and all chunk metadata of these series in this tsfile
    private Map<TsFileResource, Iterator<Map<String, List<ChunkMetadata>>>>
        chunkMetadataIteratorMap = new HashMap<>();

    private MeasurementIterator(
        Map<TsFileResource, TsFileSequenceReader> readerMap,
        String device,
        boolean needDeserializeTimeseries)
        throws IOException {
      this.readerMap = readerMap;
      this.device = device;

      if (needDeserializeTimeseries) {
        for (TsFileResource resource : tsFileResources) {
          TsFileSequenceReader reader = readerMap.get(resource);
          chunkMetadataIteratorMap.put(
              resource, reader.getMeasurementChunkMetadataListMapIterator(device));
          chunkMetadataCacheMap.put(reader, new TreeMap<>());
        }
      }
    }

    public Set<String> getAllMeasurements() throws IOException {
      Set<String> measurementsSet = new HashSet<>();
      for (TsFileSequenceReader reader : readerMap.values()) {
        measurementsSet.addAll(reader.readDeviceMetadata(device).keySet());
      }
      return measurementsSet;
    }

    /**
     * Collect series from files using iterator, and the collected series will be store in
     * seriesInThisIteration. To ensure that each serie is compacted once, when iterator of each
     * file returns a batch of series, we will find the max of it, and find the min series marked as
     * `last series` among the max series in each batch.
     *
     * <p>That is, lastSeries = min([max(series return in file 1),..., max(series return in file
     * n)]). Only the series that are greater than the lastSeries in lexicographical order will be
     * collected.
     *
     * @return true if there is any series is collected, else false.
     */
    private boolean collectSeries() {
      String lastSeries = null;
      List<String> tempCollectedSeries = new ArrayList<>();
      for (TsFileResource resource : tsFileResources) {
        TsFileSequenceReader reader = readerMap.get(resource);
        Map<String, List<ChunkMetadata>> chunkMetadataListMap = chunkMetadataCacheMap.get(reader);
        if (chunkMetadataListMap.size() == 0) {
          if (chunkMetadataIteratorMap.get(resource).hasNext()) {
            chunkMetadataListMap = chunkMetadataIteratorMap.get(resource).next();
            chunkMetadataCacheMap.put(reader, chunkMetadataListMap);
          } else {
            continue;
          }
        }
        // get the min last series in the current chunk metadata
        String maxSeries = Collections.max(chunkMetadataListMap.keySet());
        if (lastSeries == null) {
          lastSeries = maxSeries;
        } else {
          if (maxSeries.compareTo(lastSeries) < 0) {
            lastSeries = maxSeries;
          }
        }
        tempCollectedSeries.addAll(chunkMetadataListMap.keySet());
      }
      if (tempCollectedSeries.size() > 0) {
        if (!hasRemainingSeries()) {
          lastSeries = Collections.max(tempCollectedSeries);
        }
        String finalLastSeries = lastSeries;
        List<String> finalCollectedSeriesInThisIteration =
            tempCollectedSeries.stream()
                .filter(series -> series.compareTo(finalLastSeries) <= 0)
                .collect(Collectors.toList());
        seriesInThisIteration.addAll(finalCollectedSeriesInThisIteration);
        return true;
      } else {
        return false;
      }
    }

    private boolean hasRemainingSeries() {
      boolean remaining = false;
      for (Iterator<Map<String, List<ChunkMetadata>>> iterator :
          chunkMetadataIteratorMap.values()) {
        remaining = remaining || iterator.hasNext();
      }
      return remaining;
    }

    public boolean hasNextSeries() {
      if (seriesInThisIteration.size() == 0 && !collectSeries()) {
        return false;
      } else {
        return true;
      }
    }

    public String nextSeries() {
      if (!hasNextSeries()) {
        return null;
      } else {
        currentCompactingSeries = seriesInThisIteration.removeFirst();
        return currentCompactingSeries;
      }
    }

    /**
     * Collect all the chunk metadata of current series from the source files.
     *
     * <p>If there are any modifications for these chunk, we will apply them to the metadata. Use
     * `ChunkMetadata.getDeleteIntervalList() == null` to judge if the chunk is modified.
     *
     * @return
     * @throws IllegalPathException
     */
    public LinkedList<Pair<TsFileSequenceReader, List<ChunkMetadata>>>
        getMetadataListForCurrentSeries() throws IllegalPathException {
      if (currentCompactingSeries == null) {
        return null;
      }

      LinkedList<Pair<TsFileSequenceReader, List<ChunkMetadata>>>
          readerAndChunkMetadataForThisSeries = new LinkedList<>();
      PartialPath path = new PartialPath(device, currentCompactingSeries);

      for (TsFileResource resource : tsFileResources) {
        TsFileSequenceReader reader = readerMap.get(resource);
        Map<String, List<ChunkMetadata>> chunkMetadataListMap = chunkMetadataCacheMap.get(reader);

        if (chunkMetadataListMap.containsKey(currentCompactingSeries)) {
          // get the chunk metadata list and modification list of current series in this tsfile
          List<ChunkMetadata> chunkMetadataListInThisResource =
              chunkMetadataListMap.get(currentCompactingSeries);
          chunkMetadataListMap.remove(currentCompactingSeries);

          List<Modification> modificationsInThisResource =
              modificationCache.computeIfAbsent(
                  resource,
                  r -> new LinkedList<>(ModificationFile.getNormalMods(r).getModifications()));
          LinkedList<Modification> modificationForCurrentSeries = new LinkedList<>();
          // collect the modifications for current series
          for (Modification modification : modificationsInThisResource) {
            if (modification.getPath().matchFullPath(path)) {
              modificationForCurrentSeries.add(modification);
            }
          }

          // if there are modifications of current series, apply them to the chunk metadata
          if (modificationForCurrentSeries.size() != 0) {
            QueryUtils.modifyChunkMetaData(
                chunkMetadataListInThisResource, modificationForCurrentSeries);
          }

          readerAndChunkMetadataForThisSeries.add(
              new Pair<>(reader, chunkMetadataListInThisResource));
        }
      }
      return readerAndChunkMetadataForThisSeries;
    }
  }
}
