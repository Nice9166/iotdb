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
package org.apache.iotdb.db.mpp.operator.schema;

import org.apache.iotdb.commons.concurrent.IoTDBThreadPoolFactory;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.localconfignode.LocalConfigNode;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.metadata.schemaregion.ISchemaRegion;
import org.apache.iotdb.db.metadata.schemaregion.SchemaEngine;
import org.apache.iotdb.db.mpp.common.FragmentInstanceId;
import org.apache.iotdb.db.mpp.common.PlanFragmentId;
import org.apache.iotdb.db.mpp.common.QueryId;
import org.apache.iotdb.db.mpp.execution.FragmentInstanceContext;
import org.apache.iotdb.db.mpp.execution.FragmentInstanceStateMachine;
import org.apache.iotdb.db.mpp.execution.SchemaDriverContext;
import org.apache.iotdb.db.mpp.operator.OperatorContext;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.query.reader.series.SeriesReaderTestUtil;
import org.apache.iotdb.tsfile.exception.write.WriteProcessException;
import org.apache.iotdb.tsfile.read.common.block.TsBlock;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.apache.iotdb.db.mpp.execution.FragmentInstanceContext.createFragmentInstanceContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaCountOperatorTest {
  private static final String SCHEMA_COUNT_OPERATOR_TEST_SG = "root.SchemaCountOperatorTest";
  private final List<String> deviceIds = new ArrayList<>();
  private final List<MeasurementSchema> measurementSchemas = new ArrayList<>();

  private final List<TsFileResource> seqResources = new ArrayList<>();
  private final List<TsFileResource> unSeqResources = new ArrayList<>();

  @Before
  public void setUp() throws MetadataException, IOException, WriteProcessException {
    SeriesReaderTestUtil.setUp(
        measurementSchemas, deviceIds, seqResources, unSeqResources, SCHEMA_COUNT_OPERATOR_TEST_SG);
  }

  @After
  public void tearDown() throws IOException {
    SeriesReaderTestUtil.tearDown(seqResources, unSeqResources);
  }

  @Test
  public void testDeviceCountOperator() {
    ExecutorService instanceNotificationExecutor =
        IoTDBThreadPoolFactory.newFixedThreadPool(1, "test-instance-notification");
    try {
      QueryId queryId = new QueryId("stub_query");
      FragmentInstanceId instanceId =
          new FragmentInstanceId(new PlanFragmentId(queryId, 0), "stub-instance");
      FragmentInstanceStateMachine stateMachine =
          new FragmentInstanceStateMachine(instanceId, instanceNotificationExecutor);
      FragmentInstanceContext fragmentInstanceContext =
          createFragmentInstanceContext(instanceId, stateMachine);
      PlanNodeId planNodeId = queryId.genPlanNodeId();
      OperatorContext operatorContext =
          fragmentInstanceContext.addOperatorContext(
              1, planNodeId, DevicesCountOperator.class.getSimpleName());
      PartialPath partialPath = new PartialPath(SCHEMA_COUNT_OPERATOR_TEST_SG);
      ISchemaRegion schemaRegion =
          SchemaEngine.getInstance()
              .getSchemaRegion(
                  LocalConfigNode.getInstance().getBelongedSchemaRegionId(partialPath));
      operatorContext
          .getInstanceContext()
          .setDriverContext(new SchemaDriverContext(fragmentInstanceContext, schemaRegion));
      DevicesCountOperator devicesCountOperator =
          new DevicesCountOperator(
              planNodeId, fragmentInstanceContext.getOperatorContexts().get(0), partialPath, true);
      TsBlock tsBlock = null;
      while (devicesCountOperator.hasNext()) {
        tsBlock = devicesCountOperator.next();
      }
      assertNotNull(tsBlock);
      assertEquals(tsBlock.getColumn(0).getInt(0), 10);
    } catch (MetadataException e) {
      e.printStackTrace();
      fail();
    } finally {
      instanceNotificationExecutor.shutdown();
    }
  }

  @Test
  public void testTimeSeriesCountOperator() {
    ExecutorService instanceNotificationExecutor =
        IoTDBThreadPoolFactory.newFixedThreadPool(1, "test-instance-notification");
    try {
      QueryId queryId = new QueryId("stub_query");
      FragmentInstanceId instanceId =
          new FragmentInstanceId(new PlanFragmentId(queryId, 0), "stub-instance");
      FragmentInstanceStateMachine stateMachine =
          new FragmentInstanceStateMachine(instanceId, instanceNotificationExecutor);
      FragmentInstanceContext fragmentInstanceContext =
          createFragmentInstanceContext(instanceId, stateMachine);
      PlanNodeId planNodeId = queryId.genPlanNodeId();
      OperatorContext operatorContext =
          fragmentInstanceContext.addOperatorContext(
              1, planNodeId, TimeSeriesCountOperator.class.getSimpleName());
      PartialPath partialPath = new PartialPath(SCHEMA_COUNT_OPERATOR_TEST_SG);
      ISchemaRegion schemaRegion =
          SchemaEngine.getInstance()
              .getSchemaRegion(
                  LocalConfigNode.getInstance().getBelongedSchemaRegionId(partialPath));
      operatorContext
          .getInstanceContext()
          .setDriverContext(new SchemaDriverContext(fragmentInstanceContext, schemaRegion));
      TimeSeriesCountOperator timeSeriesCountOperator =
          new TimeSeriesCountOperator(
              planNodeId, fragmentInstanceContext.getOperatorContexts().get(0), partialPath, true);
      TsBlock tsBlock = null;
      while (timeSeriesCountOperator.hasNext()) {
        tsBlock = timeSeriesCountOperator.next();
      }
      assertNotNull(tsBlock);
      assertEquals(100, tsBlock.getColumn(0).getInt(0));
      TimeSeriesCountOperator timeSeriesCountOperator2 =
          new TimeSeriesCountOperator(
              planNodeId,
              fragmentInstanceContext.getOperatorContexts().get(0),
              new PartialPath(SCHEMA_COUNT_OPERATOR_TEST_SG + ".device1.*"),
              false);
      tsBlock = timeSeriesCountOperator2.next();
      assertFalse(timeSeriesCountOperator2.hasNext());
      assertTrue(timeSeriesCountOperator2.isFinished());
      assertEquals(10, tsBlock.getColumn(0).getInt(0));
    } catch (MetadataException e) {
      e.printStackTrace();
      fail();
    } finally {
      instanceNotificationExecutor.shutdown();
    }
  }

  @Test
  public void testLevelTimeSeriesCountOperator() {
    ExecutorService instanceNotificationExecutor =
        IoTDBThreadPoolFactory.newFixedThreadPool(1, "test-instance-notification");
    try {
      QueryId queryId = new QueryId("stub_query");
      FragmentInstanceId instanceId =
          new FragmentInstanceId(new PlanFragmentId(queryId, 0), "stub-instance");
      FragmentInstanceStateMachine stateMachine =
          new FragmentInstanceStateMachine(instanceId, instanceNotificationExecutor);
      FragmentInstanceContext fragmentInstanceContext =
          createFragmentInstanceContext(instanceId, stateMachine);
      PlanNodeId planNodeId = queryId.genPlanNodeId();
      OperatorContext operatorContext =
          fragmentInstanceContext.addOperatorContext(
              1, planNodeId, LevelTimeSeriesCountOperator.class.getSimpleName());
      PartialPath partialPath = new PartialPath(SCHEMA_COUNT_OPERATOR_TEST_SG);
      ISchemaRegion schemaRegion =
          SchemaEngine.getInstance()
              .getSchemaRegion(
                  LocalConfigNode.getInstance().getBelongedSchemaRegionId(partialPath));
      operatorContext
          .getInstanceContext()
          .setDriverContext(new SchemaDriverContext(fragmentInstanceContext, schemaRegion));
      LevelTimeSeriesCountOperator timeSeriesCountOperator =
          new LevelTimeSeriesCountOperator(
              planNodeId,
              fragmentInstanceContext.getOperatorContexts().get(0),
              partialPath,
              true,
              2);
      TsBlock tsBlock = null;
      while (timeSeriesCountOperator.hasNext()) {
        tsBlock = timeSeriesCountOperator.next();
        assertFalse(timeSeriesCountOperator.hasNext());
      }
      assertNotNull(tsBlock);

      for (int i = 0; i < 10; i++) {
        String path = tsBlock.getColumn(0).getBinary(i).getStringValue();
        assertTrue(path.startsWith(SCHEMA_COUNT_OPERATOR_TEST_SG + ".device"));
        assertEquals(10, tsBlock.getColumn(1).getInt(i));
      }

      LevelTimeSeriesCountOperator timeSeriesCountOperator2 =
          new LevelTimeSeriesCountOperator(
              planNodeId,
              fragmentInstanceContext.getOperatorContexts().get(0),
              partialPath,
              true,
              1);
      while (timeSeriesCountOperator2.hasNext()) {
        tsBlock = timeSeriesCountOperator2.next();
      }
      assertNotNull(tsBlock);
      assertEquals(100, tsBlock.getColumn(1).getInt(0));
    } catch (MetadataException e) {
      e.printStackTrace();
      fail();
    } finally {
      instanceNotificationExecutor.shutdown();
    }
  }
}
