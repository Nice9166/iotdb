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

package org.apache.iotdb.db.mpp.sql.statement.metadata;

import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.sql.constant.StatementType;

/**
 * COUNT statement.
 *
 * <p>Here is the syntax definition:
 *
 * <p>COUNT {STORAGE GROUP | DEVICES | TIMESERIES | NODES} [prefixPath] [GROUP BY] LEVEL = level
 */
public class CountStatement extends ShowStatement {
  protected PartialPath partialPath;

  public CountStatement(PartialPath partialPath) {
    this.partialPath = partialPath;
    setType(StatementType.COUNT);
  }

  public PartialPath getPartialPath() {
    return partialPath;
  }

  public void setPartialPath(PartialPath partialPath) {
    this.partialPath = partialPath;
  }
}
