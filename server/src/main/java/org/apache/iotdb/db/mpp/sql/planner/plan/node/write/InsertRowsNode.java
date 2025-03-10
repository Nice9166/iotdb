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
package org.apache.iotdb.db.mpp.sql.planner.plan.node.write;

import org.apache.iotdb.common.rpc.thrift.TRegionReplicaSet;
import org.apache.iotdb.common.rpc.thrift.TSStatus;
import org.apache.iotdb.commons.utils.StatusUtils;
import org.apache.iotdb.db.engine.StorageEngineV2;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.common.header.ColumnHeader;
import org.apache.iotdb.db.mpp.common.schematree.SchemaTree;
import org.apache.iotdb.db.mpp.sql.analyze.Analysis;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeType;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.WritePlanNode;
import org.apache.iotdb.db.mpp.sql.statement.crud.BatchInsert;
import org.apache.iotdb.tsfile.exception.NotImplementedException;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InsertRowsNode extends InsertNode implements BatchInsert {

  /**
   * Suppose there is an InsertRowsNode, which contains 5 InsertRowNodes,
   * insertRowNodeList={InsertRowNode_0, InsertRowNode_1, InsertRowNode_2, InsertRowNode_3,
   * InsertRowNode_4}, then the insertRowNodeIndexList={0, 1, 2, 3, 4} respectively. But when the
   * InsertRowsNode is split into two InsertRowsNodes according to different storage group in
   * cluster version, suppose that the InsertRowsNode_1's insertRowNodeList = {InsertRowNode_0,
   * InsertRowNode_3, InsertRowNode_4}, then InsertRowsNode_1's insertRowNodeIndexList = {0, 3, 4};
   * InsertRowsNode_2's insertRowNodeList = {InsertRowNode_1, * InsertRowNode_2} then
   * InsertRowsNode_2's insertRowNodeIndexList= {1, 2} respectively;
   */
  private List<Integer> insertRowNodeIndexList;

  /** the InsertRowsNode list */
  private List<InsertRowNode> insertRowNodeList;

  public InsertRowsNode(PlanNodeId id) {
    super(id);
    insertRowNodeList = new ArrayList<>();
    insertRowNodeIndexList = new ArrayList<>();
  }

  /** record the result of insert rows */
  private Map<Integer, TSStatus> results = new HashMap<>();

  public List<Integer> getInsertRowNodeIndexList() {
    return insertRowNodeIndexList;
  }

  public void setInsertRowNodeIndexList(List<Integer> insertRowNodeIndexList) {
    this.insertRowNodeIndexList = insertRowNodeIndexList;
  }

  public List<InsertRowNode> getInsertRowNodeList() {
    return insertRowNodeList;
  }

  public void setInsertRowNodeList(List<InsertRowNode> insertRowNodeList) {
    this.insertRowNodeList = insertRowNodeList;
  }

  public void addOneInsertRowNode(InsertRowNode node, int index) {
    insertRowNodeList.add(node);
    insertRowNodeIndexList.add(index);
  }

  public Map<Integer, TSStatus> getResults() {
    return results;
  }

  public TSStatus[] getFailingStatus() {
    return StatusUtils.getFailingStatus(results, insertRowNodeList.size());
  }

  @Override
  public List<PlanNode> getChildren() {
    return null;
  }

  @Override
  public void addChild(PlanNode child) {}

  @Override
  public boolean validateSchema(SchemaTree schemaTree) {
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      if (!insertRowNode.validateSchema(schemaTree)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    InsertRowsNode that = (InsertRowsNode) o;
    return Objects.equals(insertRowNodeIndexList, that.insertRowNodeIndexList)
        && Objects.equals(insertRowNodeList, that.insertRowNodeList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), insertRowNodeIndexList, insertRowNodeList);
  }

  @Override
  public PlanNode clone() {
    throw new NotImplementedException("clone of Insert is not implemented");
  }

  @Override
  public int allowedChildCount() {
    return NO_CHILD_ALLOWED;
  }

  @Override
  public List<ColumnHeader> getOutputColumnHeaders() {
    return null;
  }

  @Override
  public List<String> getOutputColumnNames() {
    return null;
  }

  @Override
  public List<TSDataType> getOutputColumnTypes() {
    return null;
  }

  @Override
  public List<PartialPath> getDevicePaths() {
    List<PartialPath> partialPaths = new ArrayList<>();
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      partialPaths.add(insertRowNode.devicePath);
    }
    return partialPaths;
  }

  @Override
  public List<String[]> getMeasurementsList() {
    List<String[]> measurementsList = new ArrayList<>();
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      measurementsList.add(insertRowNode.measurements);
    }
    return measurementsList;
  }

  @Override
  public List<TSDataType[]> getDataTypesList() {
    List<TSDataType[]> dataTypesList = new ArrayList<>();
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      dataTypesList.add(insertRowNode.dataTypes);
    }
    return dataTypesList;
  }

  @Override
  public List<Boolean> getAlignedList() {
    List<Boolean> alignedList = new ArrayList<>();
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      alignedList.add(insertRowNode.isAligned);
    }
    return alignedList;
  }

  public static InsertRowsNode deserialize(ByteBuffer byteBuffer) {
    PlanNodeId planNodeId;
    List<InsertRowNode> insertRowNodeList = new ArrayList<>();
    List<Integer> insertRowNodeIndex = new ArrayList<>();

    int size = byteBuffer.getInt();
    for (int i = 0; i < size; i++) {
      InsertRowNode insertRowNode = new InsertRowNode(new PlanNodeId(""));
      insertRowNode.subDeserialize(byteBuffer);
      insertRowNodeList.add(insertRowNode);
    }
    for (int i = 0; i < size; i++) {
      insertRowNodeIndex.add(byteBuffer.getInt());
    }

    planNodeId = PlanNodeId.deserialize(byteBuffer);
    for (InsertRowNode insertRowNode : insertRowNodeList) {
      insertRowNode.setPlanNodeId(planNodeId);
    }

    InsertRowsNode insertRowsNode = new InsertRowsNode(planNodeId);
    insertRowsNode.setInsertRowNodeList(insertRowNodeList);
    insertRowsNode.setInsertRowNodeIndexList(insertRowNodeIndex);
    return insertRowsNode;
  }

  @Override
  protected void serializeAttributes(ByteBuffer byteBuffer) {
    PlanNodeType.INSERT_ROWS.serialize(byteBuffer);

    byteBuffer.putInt(insertRowNodeList.size());

    for (InsertRowNode node : insertRowNodeList) {
      node.subSerialize(byteBuffer);
    }
    for (Integer index : insertRowNodeIndexList) {
      byteBuffer.putInt(index);
    }
  }

  @Override
  public List<WritePlanNode> splitByPartition(Analysis analysis) {
    Map<TRegionReplicaSet, InsertRowsNode> splitMap = new HashMap<>();
    for (int i = 0; i < insertRowNodeList.size(); i++) {
      InsertRowNode insertRowNode = insertRowNodeList.get(i);
      // data region for insert row node
      TRegionReplicaSet dataRegionReplicaSet =
          analysis
              .getDataPartitionInfo()
              .getDataRegionReplicaSetForWriting(
                  insertRowNode.devicePath.getFullPath(),
                  StorageEngineV2.getTimePartitionSlot(insertRowNode.getTime()));
      if (splitMap.containsKey(dataRegionReplicaSet)) {
        InsertRowsNode tmpNode = splitMap.get(dataRegionReplicaSet);
        tmpNode.addOneInsertRowNode(insertRowNode, i);
      } else {
        InsertRowsNode tmpNode = new InsertRowsNode(this.getPlanNodeId());
        tmpNode.setDataRegionReplicaSet(dataRegionReplicaSet);
        tmpNode.addOneInsertRowNode(insertRowNode, i);
        splitMap.put(dataRegionReplicaSet, tmpNode);
      }
    }

    return new ArrayList<>(splitMap.values());
  }
}
