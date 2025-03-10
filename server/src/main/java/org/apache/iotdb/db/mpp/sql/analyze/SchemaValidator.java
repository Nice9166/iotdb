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

import org.apache.iotdb.db.exception.sql.SemanticException;
import org.apache.iotdb.db.mpp.common.schematree.SchemaTree;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertNode;
import org.apache.iotdb.db.mpp.sql.statement.crud.BatchInsert;

public class SchemaValidator {

  private static final ISchemaFetcher schemaFetcher = ClusterSchemaFetcher.getInstance();

  public static SchemaTree validate(InsertNode insertNode) {

    SchemaTree schemaTree;
    if (insertNode instanceof BatchInsert) {
      BatchInsert batchInsert = (BatchInsert) insertNode;
      schemaTree =
          schemaFetcher.fetchSchemaListWithAutoCreate(
              batchInsert.getDevicePaths(),
              batchInsert.getMeasurementsList(),
              batchInsert.getDataTypesList(),
              batchInsert.getAlignedList());
    } else {
      schemaTree =
          schemaFetcher.fetchSchemaWithAutoCreate(
              insertNode.getDevicePath(),
              insertNode.getMeasurements(),
              insertNode.getDataTypes(),
              insertNode.isAligned());
    }

    if (!insertNode.validateSchema(schemaTree)) {
      throw new SemanticException("Data type mismatch");
    }

    return schemaTree;
  }
}
