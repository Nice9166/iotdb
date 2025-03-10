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

package org.apache.iotdb.commons.consensus;

import org.apache.iotdb.common.rpc.thrift.TConsensusGroupType;

import java.util.Objects;

public class SchemaRegionId implements ConsensusGroupId {

  private int id;

  public SchemaRegionId() {}

  public SchemaRegionId(int id) {
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public TConsensusGroupType getType() {
    return TConsensusGroupType.SchemaRegion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SchemaRegionId that = (SchemaRegionId) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, TConsensusGroupType.SchemaRegion);
  }

  public String toString() {
    return String.format("%s[%d]", getType(), getId());
  }
}
