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

package org.apache.iotdb.db.mpp.sql.constant;

/**
 * Type code of statement.
 *
 * <p>NOTE: If you want to add new StatementType, you must add it in the LAST.
 */
public enum StatementType {
  NULL,

  AUTHOR,
  LOAD_DATA,
  CREATE_USER,
  DELETE_USER,
  MODIFY_PASSWORD,
  GRANT_USER_PRIVILEGE,
  REVOKE_USER_PRIVILEGE,
  GRANT_USER_ROLE,
  REVOKE_USER_ROLE,
  CREATE_ROLE,
  DELETE_ROLE,
  GRANT_ROLE_PRIVILEGE,
  REVOKE_ROLE_PRIVILEGE,
  LIST_USER,
  LIST_ROLE,
  LIST_USER_PRIVILEGE,
  LIST_ROLE_PRIVILEGE,
  LIST_USER_ROLES,
  LIST_ROLE_USERS,
  GRANT_WATERMARK_EMBEDDING,
  REVOKE_WATERMARK_EMBEDDING,

  SET_STORAGE_GROUP,
  DELETE_STORAGE_GROUP,
  CREATE_TIMESERIES,
  CREATE_ALIGNED_TIMESERIES,
  CREATE_MULTI_TIMESERIES,
  DELETE_TIMESERIES,
  ALTER_TIMESERIES,
  CHANGE_ALIAS,
  CHANGE_TAG_OFFSET,

  INSERT,
  BATCH_INSERT,
  BATCH_INSERT_ROWS,
  BATCH_INSERT_ONE_DEVICE,
  MULTI_BATCH_INSERT,

  DELETE,

  QUERY,
  LAST,
  GROUP_BY_TIME,
  GROUP_BY_FILL,
  AGGREGATION,
  FILL,
  UDAF,
  UDTF,

  SELECT_INTO,

  CREATE_FUNCTION,
  DROP_FUNCTION,

  SHOW,
  SHOW_MERGE_STATUS,

  CREATE_INDEX,
  DROP_INDEX,
  QUERY_INDEX,

  LOAD_FILES,
  REMOVE_FILE,
  UNLOAD_FILE,

  CREATE_TRIGGER,
  DROP_TRIGGER,
  START_TRIGGER,
  STOP_TRIGGER,

  CREATE_TEMPLATE,
  SET_TEMPLATE,
  ACTIVATE_TEMPLATE,

  MERGE,
  FULL_MERGE,

  MNODE,
  MEASUREMENT_MNODE,
  STORAGE_GROUP_MNODE,
  AUTO_CREATE_DEVICE_MNODE,

  TTL,
  KILL,
  FLUSH,
  TRACING,
  CLEAR_CACHE,
  DELETE_PARTITION,
  LOAD_CONFIGURATION,
  CREATE_SCHEMA_SNAPSHOT,

  CREATE_CONTINUOUS_QUERY,
  DROP_CONTINUOUS_QUERY,
  SHOW_CONTINUOUS_QUERIES,
  SET_SYSTEM_MODE,

  SETTLE,

  UNSET_TEMPLATE,
  PRUNE_TEMPLATE,
  APPEND_TEMPLATE,
  DROP_TEMPLATE,

  SHOW_QUERY_RESOURCE,

  FETCH_SCHEMA,
  FETCH_SCHEMA_WITH_AUTO_CREATE,

  COUNT
}
