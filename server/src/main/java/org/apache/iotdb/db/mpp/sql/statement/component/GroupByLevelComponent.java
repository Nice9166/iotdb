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

package org.apache.iotdb.db.mpp.sql.statement.component;

import org.apache.iotdb.db.mpp.common.header.ColumnHeader;
import org.apache.iotdb.db.mpp.sql.statement.StatementNode;

import java.util.Map;

/** This class maintains information of {@code GROUP BY LEVEL} clause. */
public class GroupByLevelComponent extends StatementNode {

  protected GroupByLevelController groupByLevelController;
  protected int[] levels;

  public int[] getLevels() {
    return levels;
  }

  public void setLevels(int[] levels) {
    this.levels = levels;
  }

  public void setGroupByLevelController(GroupByLevelController groupByLevelController) {
    this.groupByLevelController = groupByLevelController;
  }

  public GroupByLevelController getGroupByLevelController() {
    return groupByLevelController;
  }

  public Map<String, String> getGroupedPathMap() {
    return groupByLevelController.getGroupedPathMap();
  }

  public Map<ColumnHeader, ColumnHeader> getGroupedHeaderMap() {
    return groupByLevelController.getGroupedHeaderMap();
  }
}
