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

import org.apache.iotdb.common.rpc.thrift.TRegionReplicaSet;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanVisitor;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.source.SourceNode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class SchemaScanNode extends SourceNode {
  protected int limit;
  protected int offset;
  protected PartialPath path;
  private boolean hasLimit;
  protected boolean isPrefixPath;

  private TRegionReplicaSet schemaRegionReplicaSet;

  protected SchemaScanNode(PlanNodeId id) {
    this(id, null, false);
  }

  protected SchemaScanNode(
      PlanNodeId id, PartialPath partialPath, int limit, int offset, boolean isPrefixPath) {
    super(id);
    this.path = partialPath;
    setLimit(limit);
    this.offset = offset;
    this.isPrefixPath = isPrefixPath;
  }

  protected SchemaScanNode(PlanNodeId id, PartialPath partialPath, boolean isPrefixPath) {
    this(id, partialPath, 0, 0, isPrefixPath);
  }

  @Override
  public void open() throws Exception {}

  @Override
  public int allowedChildCount() {
    return NO_CHILD_ALLOWED;
  }

  @Override
  public List<PlanNode> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public void addChild(PlanNode child) {}

  @Override
  public void close() throws Exception {}

  public boolean isPrefixPath() {
    return isPrefixPath;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
    if (limit == 0) {
      hasLimit = false;
    } else {
      hasLimit = true;
    }
  }

  @Override
  public TRegionReplicaSet getRegionReplicaSet() {
    return schemaRegionReplicaSet;
  }

  @Override
  public void setRegionReplicaSet(TRegionReplicaSet schemaRegionReplicaSet) {
    this.schemaRegionReplicaSet = schemaRegionReplicaSet;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public PartialPath getPath() {
    return path;
  }

  public void setPath(PartialPath path) {
    this.path = path;
  }

  public boolean isHasLimit() {
    return hasLimit;
  }

  public void setHasLimit(boolean hasLimit) {
    this.hasLimit = hasLimit;
  }

  @Override
  public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
    return visitor.visitSchemaScan(this, context);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    SchemaScanNode that = (SchemaScanNode) o;
    return limit == that.limit
        && offset == that.offset
        && isPrefixPath == that.isPrefixPath
        && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), limit, offset, path, isPrefixPath);
  }
}
