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

package org.apache.iotdb.db.mpp.sql.analyze;

import org.apache.iotdb.common.rpc.thrift.TConsensusGroupId;
import org.apache.iotdb.common.rpc.thrift.TConsensusGroupType;
import org.apache.iotdb.common.rpc.thrift.TDataNodeLocation;
import org.apache.iotdb.common.rpc.thrift.TEndPoint;
import org.apache.iotdb.common.rpc.thrift.TRegionReplicaSet;
import org.apache.iotdb.common.rpc.thrift.TSeriesPartitionSlot;
import org.apache.iotdb.common.rpc.thrift.TTimePartitionSlot;
import org.apache.iotdb.commons.partition.DataPartition;
import org.apache.iotdb.commons.partition.DataPartitionQueryParam;
import org.apache.iotdb.commons.partition.SchemaPartition;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.mpp.common.schematree.PathPatternTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakePartitionFetcherImpl implements IPartitionFetcher {

  @Override
  public SchemaPartition getSchemaPartition(PathPatternTree patternTree) {
    return null;
  }

  @Override
  public SchemaPartition getOrCreateSchemaPartition(PathPatternTree patternTree) {
    return null;
  }

  @Override
  public DataPartition getDataPartition(
      Map<String, List<DataPartitionQueryParam>> sgNameToQueryParamsMap) {
    String device1 = "root.sg.d1";
    String device2 = "root.sg.d22";
    String device3 = "root.sg.d333";

    DataPartition dataPartition =
        new DataPartition(
            IoTDBDescriptor.getInstance().getConfig().getSeriesPartitionExecutorClass(),
            IoTDBDescriptor.getInstance().getConfig().getSeriesPartitionSlotNum());
    Map<String, Map<TSeriesPartitionSlot, Map<TTimePartitionSlot, List<TRegionReplicaSet>>>>
        dataPartitionMap = new HashMap<>();
    Map<TSeriesPartitionSlot, Map<TTimePartitionSlot, List<TRegionReplicaSet>>> sgPartitionMap =
        new HashMap<>();

    List<TRegionReplicaSet> d1DataRegions = new ArrayList<>();
    d1DataRegions.add(
        new TRegionReplicaSet(
            new TConsensusGroupId(TConsensusGroupType.DataRegion, 1),
            Arrays.asList(
                new TDataNodeLocation()
                    .setDataNodeId(11)
                    .setExternalEndPoint(new TEndPoint("192.0.1.1", 9000)),
                new TDataNodeLocation()
                    .setDataNodeId(12)
                    .setExternalEndPoint(new TEndPoint("192.0.1.2", 9000)))));
    d1DataRegions.add(
        new TRegionReplicaSet(
            new TConsensusGroupId(TConsensusGroupType.DataRegion, 2),
            Arrays.asList(
                new TDataNodeLocation()
                    .setDataNodeId(21)
                    .setExternalEndPoint(new TEndPoint("192.0.2.1", 9000)),
                new TDataNodeLocation()
                    .setDataNodeId(22)
                    .setExternalEndPoint(new TEndPoint("192.0.2.2", 9000)))));
    Map<TTimePartitionSlot, List<TRegionReplicaSet>> d1DataRegionMap = new HashMap<>();
    d1DataRegionMap.put(new TTimePartitionSlot(), d1DataRegions);

    List<TRegionReplicaSet> d2DataRegions = new ArrayList<>();
    d2DataRegions.add(
        new TRegionReplicaSet(
            new TConsensusGroupId(TConsensusGroupType.DataRegion, 3),
            Arrays.asList(
                new TDataNodeLocation()
                    .setDataNodeId(31)
                    .setExternalEndPoint(new TEndPoint("192.0.3.1", 9000)),
                new TDataNodeLocation()
                    .setDataNodeId(32)
                    .setExternalEndPoint(new TEndPoint("192.0.3.2", 9000)))));
    Map<TTimePartitionSlot, List<TRegionReplicaSet>> d2DataRegionMap = new HashMap<>();
    d2DataRegionMap.put(new TTimePartitionSlot(), d2DataRegions);

    List<TRegionReplicaSet> d3DataRegions = new ArrayList<>();
    d3DataRegions.add(
        new TRegionReplicaSet(
            new TConsensusGroupId(TConsensusGroupType.DataRegion, 1),
            Arrays.asList(
                new TDataNodeLocation()
                    .setDataNodeId(11)
                    .setExternalEndPoint(new TEndPoint("192.0.1.1", 9000)),
                new TDataNodeLocation()
                    .setDataNodeId(12)
                    .setExternalEndPoint(new TEndPoint("192.0.1.2", 9000)))));
    d3DataRegions.add(
        new TRegionReplicaSet(
            new TConsensusGroupId(TConsensusGroupType.DataRegion, 4),
            Arrays.asList(
                new TDataNodeLocation()
                    .setDataNodeId(41)
                    .setExternalEndPoint(new TEndPoint("192.0.4.1", 9000)),
                new TDataNodeLocation()
                    .setDataNodeId(42)
                    .setExternalEndPoint(new TEndPoint("192.0.4.2", 9000)))));
    Map<TTimePartitionSlot, List<TRegionReplicaSet>> d3DataRegionMap = new HashMap<>();
    d3DataRegionMap.put(new TTimePartitionSlot(), d3DataRegions);

    sgPartitionMap.put(new TSeriesPartitionSlot(device1.length()), d1DataRegionMap);
    sgPartitionMap.put(new TSeriesPartitionSlot(device2.length()), d2DataRegionMap);
    sgPartitionMap.put(new TSeriesPartitionSlot(device3.length()), d3DataRegionMap);

    dataPartitionMap.put("root.sg", sgPartitionMap);

    dataPartition.setDataPartitionMap(dataPartitionMap);

    return dataPartition;
  }

  @Override
  public DataPartition getDataPartition(List<DataPartitionQueryParam> dataPartitionQueryParams) {
    return null;
  }

  @Override
  public DataPartition getOrCreateDataPartition(
      Map<String, List<DataPartitionQueryParam>> sgNameToQueryParamsMap) {
    return null;
  }

  @Override
  public DataPartition getOrCreateDataPartition(
      List<DataPartitionQueryParam> dataPartitionQueryParams) {
    return null;
  }
}
