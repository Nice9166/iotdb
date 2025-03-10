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

package org.apache.iotdb.db.query.udf.core.executor;

import org.apache.iotdb.db.query.expression.ResultColumn;
import org.apache.iotdb.db.query.expression.multi.FunctionExpression;
import org.apache.iotdb.db.query.udf.service.UDFClassLoaderManager;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UDTFContext {

  protected final ZoneId zoneId;

  protected Map<String, UDTFExecutor> expressionName2Executor = new HashMap<>();

  public UDTFContext(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  public void constructUdfExecutors(List<ResultColumn> resultColumns) {
    for (ResultColumn resultColumn : resultColumns) {
      resultColumn.getExpression().constructUdfExecutors(expressionName2Executor, zoneId);
    }
  }

  public void finalizeUDFExecutors(long queryId) {
    try {
      for (UDTFExecutor executor : expressionName2Executor.values()) {
        executor.beforeDestroy();
      }
    } finally {
      UDFClassLoaderManager.getInstance().finalizeUDFQuery(queryId);
    }
  }

  public UDTFExecutor getExecutorByFunctionExpression(FunctionExpression functionExpression) {
    return expressionName2Executor.get(functionExpression.getExpressionString());
  }
}
