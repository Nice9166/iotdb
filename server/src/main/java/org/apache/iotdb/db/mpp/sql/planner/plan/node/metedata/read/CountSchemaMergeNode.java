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

package org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.read;

import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeType;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanVisitor;

import java.nio.ByteBuffer;

public class CountSchemaMergeNode extends AbstractSchemaMergeNode {

  public CountSchemaMergeNode(PlanNodeId id) {
    super(id);
  }

  @Override
  public PlanNode clone() {
    return new CountSchemaMergeNode(getPlanNodeId());
  }

  @Override
  protected void serializeAttributes(ByteBuffer byteBuffer) {
    PlanNodeType.COUNT_MERGE.serialize(byteBuffer);
  }

  public static PlanNode deserialize(ByteBuffer byteBuffer) {
    PlanNodeId planNodeId = PlanNodeId.deserialize(byteBuffer);
    return new CountSchemaMergeNode(planNodeId);
  }

  @Override
  public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
    return visitor.visitCountMerge(this, context);
  }
}
