/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.mpp.sql.planner;

import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.common.MPPQueryContext;
import org.apache.iotdb.db.mpp.sql.analyze.Analysis;
import org.apache.iotdb.db.mpp.sql.optimization.PlanOptimizer;
import org.apache.iotdb.db.mpp.sql.planner.plan.LogicalQueryPlan;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.write.AlterTimeSeriesNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.write.CreateAlignedTimeSeriesNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.write.CreateTimeSeriesNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertMultiTabletsNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertRowNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertRowsNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertRowsOfOneDeviceNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.write.InsertTabletNode;
import org.apache.iotdb.db.mpp.sql.statement.StatementVisitor;
import org.apache.iotdb.db.mpp.sql.statement.crud.AggregationQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.FillQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.GroupByFillQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.GroupByQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.InsertMultiTabletsStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.InsertRowStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.InsertRowsOfOneDeviceStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.InsertRowsStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.InsertTabletStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.LastQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.QueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.UDAFQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.crud.UDTFQueryStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.AlterTimeSeriesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.CountDevicesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.CountLevelTimeSeriesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.CountTimeSeriesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.CreateAlignedTimeSeriesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.CreateTimeSeriesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.SchemaFetchStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.ShowDevicesStatement;
import org.apache.iotdb.db.mpp.sql.statement.metadata.ShowTimeSeriesStatement;
import org.apache.iotdb.db.query.aggregation.AggregationType;
import org.apache.iotdb.tsfile.read.expression.ExpressionType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Generate a logical plan for the statement. */
public class LogicalPlanner {

  private final MPPQueryContext context;
  private final List<PlanOptimizer> optimizers;

  public LogicalPlanner(MPPQueryContext context, List<PlanOptimizer> optimizers) {
    this.context = context;
    this.optimizers = optimizers;
  }

  public LogicalQueryPlan plan(Analysis analysis) {
    PlanNode rootNode = new LogicalPlanVisitor(analysis).process(analysis.getStatement(), context);

    // optimize the query logical plan
    if (analysis.getStatement() instanceof QueryStatement) {
      for (PlanOptimizer optimizer : optimizers) {
        rootNode = optimizer.optimize(rootNode, context);
      }

      analysis.getRespDatasetHeader().setColumnToTsBlockIndexMap(rootNode.getOutputColumnNames());
    }

    return new LogicalQueryPlan(context, rootNode);
  }

  /**
   * This visitor is used to generate a logical plan for the statement and returns the {@link
   * PlanNode}.
   */
  private static class LogicalPlanVisitor extends StatementVisitor<PlanNode, MPPQueryContext> {

    private final Analysis analysis;

    public LogicalPlanVisitor(Analysis analysis) {
      this.analysis = analysis;
    }

    @Override
    public PlanNode visitQuery(QueryStatement queryStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);

      planBuilder.planRawDataQuerySource(
          queryStatement.getDeviceNameToDeduplicatedPathsMap(),
          queryStatement.getResultOrder(),
          queryStatement.isAlignByDevice(),
          analysis.getQueryFilter(),
          queryStatement.getSelectedPathNames());

      planBuilder.planFilterNull(queryStatement.getFilterNullComponent());
      planBuilder.planLimit(queryStatement.getRowLimit());
      planBuilder.planOffset(queryStatement.getRowOffset());
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitAggregationQuery(
        AggregationQueryStatement queryStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      Map<String, Map<PartialPath, Set<AggregationType>>> deviceNameToAggregationsMap;

      if (analysis.getQueryFilter() != null
          && analysis.getQueryFilter().getType() != ExpressionType.GLOBAL_TIME) {
        // with value filter
        planBuilder.planAggregationSourceWithValueFilter(
            queryStatement.getDeviceNameToAggregationsMap(),
            queryStatement.getDeviceNameToDeduplicatedPathsMap(),
            queryStatement.getResultOrder(),
            queryStatement.isAlignByDevice(),
            analysis.getQueryFilter(),
            queryStatement.getSelectedPathNames());
      } else {
        // without value filter
        planBuilder.planAggregationSourceWithoutValueFilter(
            queryStatement.getDeviceNameToAggregationsMap(),
            queryStatement.getResultOrder(),
            queryStatement.isAlignByDevice(),
            analysis.getQueryFilter());
      }

      planBuilder.planGroupByLevel(queryStatement.getGroupByLevelComponent());
      planBuilder.planFilterNull(queryStatement.getFilterNullComponent());
      planBuilder.planLimit(queryStatement.getRowLimit());
      planBuilder.planOffset(queryStatement.getRowOffset());
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitGroupByQuery(
        GroupByQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitGroupByFillQuery(
        GroupByFillQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitFillQuery(FillQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitLastQuery(LastQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitUDTFQuery(UDTFQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitUDAFQuery(UDAFQueryStatement queryStatement, MPPQueryContext context) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PlanNode visitCreateTimeseries(
        CreateTimeSeriesStatement createTimeSeriesStatement, MPPQueryContext context) {
      return new CreateTimeSeriesNode(
          context.getQueryId().genPlanNodeId(),
          createTimeSeriesStatement.getPath(),
          createTimeSeriesStatement.getDataType(),
          createTimeSeriesStatement.getEncoding(),
          createTimeSeriesStatement.getCompressor(),
          createTimeSeriesStatement.getProps(),
          createTimeSeriesStatement.getTags(),
          createTimeSeriesStatement.getAttributes(),
          createTimeSeriesStatement.getAlias());
    }

    @Override
    public PlanNode visitCreateAlignedTimeseries(
        CreateAlignedTimeSeriesStatement createAlignedTimeSeriesStatement,
        MPPQueryContext context) {
      return new CreateAlignedTimeSeriesNode(
          context.getQueryId().genPlanNodeId(),
          createAlignedTimeSeriesStatement.getDevicePath(),
          createAlignedTimeSeriesStatement.getMeasurements(),
          createAlignedTimeSeriesStatement.getDataTypes(),
          createAlignedTimeSeriesStatement.getEncodings(),
          createAlignedTimeSeriesStatement.getCompressors(),
          createAlignedTimeSeriesStatement.getAliasList(),
          createAlignedTimeSeriesStatement.getTagsList(),
          createAlignedTimeSeriesStatement.getAttributesList());
    }

    @Override
    public PlanNode visitAlterTimeseries(
        AlterTimeSeriesStatement alterTimeSeriesStatement, MPPQueryContext context) {
      return new AlterTimeSeriesNode(
          context.getQueryId().genPlanNodeId(),
          alterTimeSeriesStatement.getPath(),
          alterTimeSeriesStatement.getAlterType(),
          alterTimeSeriesStatement.getAlterMap(),
          alterTimeSeriesStatement.getAlias(),
          alterTimeSeriesStatement.getTagsMap(),
          alterTimeSeriesStatement.getAttributesMap());
    }

    @Override
    public PlanNode visitInsertTablet(
        InsertTabletStatement insertTabletStatement, MPPQueryContext context) {
      // convert insert statement to insert node
      return new InsertTabletNode(
          context.getQueryId().genPlanNodeId(),
          insertTabletStatement.getDevicePath(),
          insertTabletStatement.isAligned(),
          insertTabletStatement.getMeasurements(),
          insertTabletStatement.getDataTypes(),
          insertTabletStatement.getTimes(),
          insertTabletStatement.getBitMaps(),
          insertTabletStatement.getColumns(),
          insertTabletStatement.getRowCount());
    }

    @Override
    public PlanNode visitInsertRow(InsertRowStatement insertRowStatement, MPPQueryContext context) {
      // convert insert statement to insert node
      return new InsertRowNode(
          context.getQueryId().genPlanNodeId(),
          insertRowStatement.getDevicePath(),
          insertRowStatement.isAligned(),
          insertRowStatement.getMeasurements(),
          insertRowStatement.getDataTypes(),
          insertRowStatement.getTime(),
          insertRowStatement.getValues(),
          insertRowStatement.isNeedInferType());
    }

    @Override
    public PlanNode visitCountDevices(
        CountDevicesStatement countDevicesStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planDevicesCountSource(
          countDevicesStatement.getPartialPath(), countDevicesStatement.isPrefixPath());
      planBuilder.planCountMerge();
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitCountTimeSeries(
        CountTimeSeriesStatement countTimeSeriesStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planTimeSeriesCountSource(
          countTimeSeriesStatement.getPartialPath(), countTimeSeriesStatement.isPrefixPath());
      planBuilder.planCountMerge();
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitCountLevelTimeSeries(
        CountLevelTimeSeriesStatement countLevelTimeSeriesStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planLevelTimeSeriesCountSource(
          countLevelTimeSeriesStatement.getPartialPath(),
          countLevelTimeSeriesStatement.isPrefixPath(),
          countLevelTimeSeriesStatement.getLevel());
      planBuilder.planCountMerge();
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitInsertRows(
        InsertRowsStatement insertRowsStatement, MPPQueryContext context) {
      // convert insert statement to insert node
      InsertRowsNode insertRowsNode = new InsertRowsNode(context.getQueryId().genPlanNodeId());
      for (int i = 0; i < insertRowsStatement.getInsertRowStatementList().size(); i++) {
        InsertRowStatement insertRowStatement =
            insertRowsStatement.getInsertRowStatementList().get(i);
        insertRowsNode.addOneInsertRowNode(
            new InsertRowNode(
                insertRowsNode.getPlanNodeId(),
                insertRowStatement.getDevicePath(),
                insertRowStatement.isAligned(),
                insertRowStatement.getMeasurements(),
                insertRowStatement.getDataTypes(),
                insertRowStatement.getTime(),
                insertRowStatement.getValues(),
                insertRowStatement.isNeedInferType()),
            i);
      }
      return insertRowsNode;
    }

    @Override
    public PlanNode visitInsertMultiTablets(
        InsertMultiTabletsStatement insertMultiTabletsStatement, MPPQueryContext context) {
      // convert insert statement to insert node
      InsertMultiTabletsNode insertMultiTabletsNode =
          new InsertMultiTabletsNode(context.getQueryId().genPlanNodeId());
      for (int i = 0; i < insertMultiTabletsStatement.getInsertTabletStatementList().size(); i++) {
        InsertTabletStatement insertTabletStatement =
            insertMultiTabletsStatement.getInsertTabletStatementList().get(i);
        insertMultiTabletsNode.addInsertTabletNode(
            new InsertTabletNode(
                insertMultiTabletsNode.getPlanNodeId(),
                insertTabletStatement.getDevicePath(),
                insertTabletStatement.isAligned(),
                insertTabletStatement.getMeasurements(),
                insertTabletStatement.getDataTypes(),
                insertTabletStatement.getTimes(),
                insertTabletStatement.getBitMaps(),
                insertTabletStatement.getColumns(),
                insertTabletStatement.getRowCount()),
            i);
      }
      return insertMultiTabletsNode;
    }

    @Override
    public PlanNode visitInsertRowsOfOneDevice(
        InsertRowsOfOneDeviceStatement insertRowsOfOneDeviceStatement, MPPQueryContext context) {
      // convert insert statement to insert node
      InsertRowsOfOneDeviceNode insertRowsOfOneDeviceNode =
          new InsertRowsOfOneDeviceNode(context.getQueryId().genPlanNodeId());
      for (int i = 0; i < insertRowsOfOneDeviceStatement.getInsertRowStatementList().size(); i++) {
        InsertRowStatement insertRowStatement =
            insertRowsOfOneDeviceStatement.getInsertRowStatementList().get(i);
        insertRowsOfOneDeviceNode.addOneInsertRowNode(
            new InsertRowNode(
                insertRowsOfOneDeviceNode.getPlanNodeId(),
                insertRowStatement.getDevicePath(),
                insertRowStatement.isAligned(),
                insertRowStatement.getMeasurements(),
                insertRowStatement.getDataTypes(),
                insertRowStatement.getTime(),
                insertRowStatement.getValues(),
                insertRowStatement.isNeedInferType()),
            i);
      }
      return insertRowsOfOneDeviceNode;
    }

    @Override
    public PlanNode visitShowTimeSeries(
        ShowTimeSeriesStatement showTimeSeriesStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planTimeSeriesMetaSource(
          showTimeSeriesStatement.getPathPattern(),
          showTimeSeriesStatement.getKey(),
          showTimeSeriesStatement.getValue(),
          showTimeSeriesStatement.getLimit(),
          showTimeSeriesStatement.getOffset(),
          showTimeSeriesStatement.isOrderByHeat(),
          showTimeSeriesStatement.isContains(),
          showTimeSeriesStatement.isPrefixPath());
      planBuilder.planSchemaMerge(showTimeSeriesStatement.isOrderByHeat());
      if (showTimeSeriesStatement.getLimit() > 0) {
        planBuilder.planOffset(showTimeSeriesStatement.getOffset());
        planBuilder.planLimit(showTimeSeriesStatement.getLimit());
      }
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitShowDevices(
        ShowDevicesStatement showDevicesStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planDeviceSchemaSource(
          showDevicesStatement.getPathPattern(),
          showDevicesStatement.getLimit(),
          showDevicesStatement.getOffset(),
          showDevicesStatement.isPrefixPath(),
          showDevicesStatement.hasSgCol());
      planBuilder.planSchemaMerge(false);
      planBuilder.planOffset(showDevicesStatement.getOffset());
      planBuilder.planLimit(showDevicesStatement.getLimit());
      return planBuilder.getRoot();
    }

    @Override
    public PlanNode visitSchemaFetch(
        SchemaFetchStatement schemaFetchStatement, MPPQueryContext context) {
      QueryPlanBuilder planBuilder = new QueryPlanBuilder(context);
      planBuilder.planSchemaFetchSource(schemaFetchStatement.getPatternTree());
      planBuilder.planSchemaMerge(false);
      return planBuilder.getRoot();
    }
  }
}
