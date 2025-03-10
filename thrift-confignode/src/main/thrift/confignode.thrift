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

include "common.thrift"
namespace java org.apache.iotdb.confignode.rpc.thrift
namespace py iotdb.thrift.confignode

// DataNode
struct TDataNodeRegisterReq {
  1: required common.TDataNodeLocation dataNodeLocation
  // Map<StorageGroupName, TStorageGroupSchema>
  // DataNode can use statusMap to report its status to the ConfigNode when restart
  2: optional map<string, TStorageGroupSchema> statusMap
}

struct TGlobalConfig {
  1: required string dataNodeConsensusProtocolClass
  2: required i32 seriesPartitionSlotNum
  3: required string seriesPartitionExecutorClass
  4: required i64 timePartitionInterval
}

struct TDataNodeRegisterResp {
  1: required common.TSStatus status
  2: optional i32 dataNodeId
  3: optional TGlobalConfig globalConfig
}

struct TDataNodeLocationResp {
  1: required common.TSStatus status
  // map<DataNodeId, DataNodeLocation>
  2: optional map<i32, common.TDataNodeLocation> dataNodeLocationMap
}

// StorageGroup
struct TSetStorageGroupReq {
  1: required TStorageGroupSchema storageGroup
}

struct TDeleteStorageGroupReq {
  1: required string storageGroup
}

struct TSetTTLReq {
  1: required string storageGroup
  2: required i64 TTL
}


struct TSetSchemaReplicationFactorReq {
  1: required string storageGroup
  2: required i32 schemaReplicationFactor
}

struct TSetDataReplicationFactorReq {
  1: required string storageGroup
  2: required i32 dataReplicationFactor
}

struct TSetTimePartitionIntervalReq {
  1: required string storageGroup
  2: required i64 timePartitionInterval
}

struct TCountStorageGroupResp {
  1: required common.TSStatus status
  2: optional i32 count
}

struct TStorageGroupSchemaResp {
  1: required common.TSStatus status
  // map<string, StorageGroupMessage>
  2: optional map<string, TStorageGroupSchema> storageGroupSchemaMap
}

struct TStorageGroupSchema {
  1: required string name
  2: optional i64 TTL
  3: optional i32 schemaReplicationFactor
  4: optional i32 dataReplicationFactor
  5: optional i64 timePartitionInterval
  6: optional list<common.TConsensusGroupId> dataRegionGroupIds
  7: optional list<common.TConsensusGroupId> schemaRegionGroupIds
}

// Schema
struct TSchemaPartitionReq {
  1: required binary pathPatternTree
}

struct TSchemaPartitionResp {
  1: required common.TSStatus status
  // map<StorageGroupName, map<TSeriesPartitionSlot, TRegionReplicaSet>>
  2: optional map<string, map<common.TSeriesPartitionSlot, common.TRegionReplicaSet>> schemaRegionMap
}

// Data
struct TDataPartitionReq {
  // map<StorageGroupName, map<TSeriesPartitionSlot, list<TTimePartitionSlot>>>
  1: required map<string, map<common.TSeriesPartitionSlot, list<common.TTimePartitionSlot>>> partitionSlotsMap
}

struct TDataPartitionResp {
  1: required common.TSStatus status
  // map<StorageGroupName, map<TSeriesPartitionSlot, map<TTimePartitionSlot, list<TRegionReplicaSet>>>>
  2: optional map<string, map<common.TSeriesPartitionSlot, map<common.TTimePartitionSlot, list<common.TRegionReplicaSet>>>> dataPartitionMap
}

// Authorize
struct TAuthorizerReq {
  1: required i32 authorType
  2: required string userName
  3: required string roleName
  4: required string password
  5: required string newPassword
  6: required set<i32> permissions
  7: required string nodeName
}

struct TAuthorizerResp {
    1: required common.TSStatus status
    2: required map<string, list<string>> authorizerInfo
}

struct TLoginReq {
    1: required string userrname
    2: required string password
}

service ConfigIService {

  /* DataNode */

  TDataNodeRegisterResp registerDataNode(TDataNodeRegisterReq req)

  TDataNodeLocationResp getDataNodeLocations(i32 dataNodeId)

  /* StorageGroup */

  common.TSStatus setStorageGroup(TSetStorageGroupReq req)

  common.TSStatus deleteStorageGroup(TDeleteStorageGroupReq req)

  common.TSStatus setTTL(TSetTTLReq req)

  common.TSStatus setSchemaReplicationFactor(TSetSchemaReplicationFactorReq req)

  common.TSStatus setDataReplicationFactor(TSetDataReplicationFactorReq req)

  common.TSStatus setTimePartitionInterval(TSetTimePartitionIntervalReq req)

  TCountStorageGroupResp countMatchedStorageGroups(list<string> storageGroupPathPattern)

  TStorageGroupSchemaResp getMatchedStorageGroupSchemas(list<string> storageGroupPathPattern)

  /* Schema */

  TSchemaPartitionResp getSchemaPartition(TSchemaPartitionReq req)

  TSchemaPartitionResp getOrCreateSchemaPartition(TSchemaPartitionReq req)

  /* Data */

  TDataPartitionResp getDataPartition(TDataPartitionReq req)

  TDataPartitionResp getOrCreateDataPartition(TDataPartitionReq req)

  /* Authorize */

  common.TSStatus operatePermission(TAuthorizerReq req)

  TAuthorizerResp queryPermission(TAuthorizerReq req)

  common.TSStatus login(TLoginReq req)
}