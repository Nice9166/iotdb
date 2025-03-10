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
package org.apache.iotdb.db.qp.sql;

import org.apache.iotdb.commons.conf.IoTDBConstant;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.engine.trigger.executor.TriggerEvent;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.exception.sql.SQLParserException;
import org.apache.iotdb.db.index.common.IndexType;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.qp.constant.FilterConstant;
import org.apache.iotdb.db.qp.constant.FilterConstant.FilterType;
import org.apache.iotdb.db.qp.constant.SQLConstant;
import org.apache.iotdb.db.qp.logical.Operator;
import org.apache.iotdb.db.qp.logical.crud.AggregationQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.BasicFunctionOperator;
import org.apache.iotdb.db.qp.logical.crud.DeleteDataOperator;
import org.apache.iotdb.db.qp.logical.crud.FillClauseComponent;
import org.apache.iotdb.db.qp.logical.crud.FillQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.FilterOperator;
import org.apache.iotdb.db.qp.logical.crud.FromComponent;
import org.apache.iotdb.db.qp.logical.crud.GroupByClauseComponent;
import org.apache.iotdb.db.qp.logical.crud.GroupByFillClauseComponent;
import org.apache.iotdb.db.qp.logical.crud.GroupByFillQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.GroupByQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.InOperator;
import org.apache.iotdb.db.qp.logical.crud.InsertOperator;
import org.apache.iotdb.db.qp.logical.crud.LastQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.LikeOperator;
import org.apache.iotdb.db.qp.logical.crud.QueryOperator;
import org.apache.iotdb.db.qp.logical.crud.RegexpOperator;
import org.apache.iotdb.db.qp.logical.crud.SelectComponent;
import org.apache.iotdb.db.qp.logical.crud.SelectIntoOperator;
import org.apache.iotdb.db.qp.logical.crud.SpecialClauseComponent;
import org.apache.iotdb.db.qp.logical.crud.UDAFQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.UDTFQueryOperator;
import org.apache.iotdb.db.qp.logical.crud.WhereComponent;
import org.apache.iotdb.db.qp.logical.sys.ActivateTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.AlterTimeSeriesOperator;
import org.apache.iotdb.db.qp.logical.sys.AlterTimeSeriesOperator.AlterType;
import org.apache.iotdb.db.qp.logical.sys.AuthorOperator;
import org.apache.iotdb.db.qp.logical.sys.AuthorOperator.AuthorType;
import org.apache.iotdb.db.qp.logical.sys.ClearCacheOperator;
import org.apache.iotdb.db.qp.logical.sys.CountOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateAlignedTimeSeriesOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateContinuousQueryOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateFunctionOperator;
import org.apache.iotdb.db.qp.logical.sys.CreatePipeOperator;
import org.apache.iotdb.db.qp.logical.sys.CreatePipeSinkOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateTimeSeriesOperator;
import org.apache.iotdb.db.qp.logical.sys.CreateTriggerOperator;
import org.apache.iotdb.db.qp.logical.sys.DataAuthOperator;
import org.apache.iotdb.db.qp.logical.sys.DeletePartitionOperator;
import org.apache.iotdb.db.qp.logical.sys.DeleteStorageGroupOperator;
import org.apache.iotdb.db.qp.logical.sys.DeleteTimeSeriesOperator;
import org.apache.iotdb.db.qp.logical.sys.DropContinuousQueryOperator;
import org.apache.iotdb.db.qp.logical.sys.DropFunctionOperator;
import org.apache.iotdb.db.qp.logical.sys.DropPipeOperator;
import org.apache.iotdb.db.qp.logical.sys.DropPipeSinkOperator;
import org.apache.iotdb.db.qp.logical.sys.DropTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.DropTriggerOperator;
import org.apache.iotdb.db.qp.logical.sys.FlushOperator;
import org.apache.iotdb.db.qp.logical.sys.KillQueryOperator;
import org.apache.iotdb.db.qp.logical.sys.LoadConfigurationOperator;
import org.apache.iotdb.db.qp.logical.sys.LoadConfigurationOperator.LoadConfigurationOperatorType;
import org.apache.iotdb.db.qp.logical.sys.LoadDataOperator;
import org.apache.iotdb.db.qp.logical.sys.LoadFilesOperator;
import org.apache.iotdb.db.qp.logical.sys.MergeOperator;
import org.apache.iotdb.db.qp.logical.sys.RemoveFileOperator;
import org.apache.iotdb.db.qp.logical.sys.SetStorageGroupOperator;
import org.apache.iotdb.db.qp.logical.sys.SetSystemModeOperator;
import org.apache.iotdb.db.qp.logical.sys.SetTTLOperator;
import org.apache.iotdb.db.qp.logical.sys.SetTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.SettleOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowChildNodesOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowChildPathsOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowContinuousQueriesOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowDevicesOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowFunctionsOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowLockInfoOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowNodesInTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPathsSetTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPathsUsingTemplateOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPipeOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPipeServerOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPipeSinkOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowPipeSinkTypeOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowQueryResourceOperate;
import org.apache.iotdb.db.qp.logical.sys.ShowStorageGroupOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowTTLOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowTemplatesOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowTimeSeriesOperator;
import org.apache.iotdb.db.qp.logical.sys.ShowTriggersOperator;
import org.apache.iotdb.db.qp.logical.sys.StartPipeOperator;
import org.apache.iotdb.db.qp.logical.sys.StartPipeServerOperator;
import org.apache.iotdb.db.qp.logical.sys.StartTriggerOperator;
import org.apache.iotdb.db.qp.logical.sys.StopPipeOperator;
import org.apache.iotdb.db.qp.logical.sys.StopPipeServerOperator;
import org.apache.iotdb.db.qp.logical.sys.StopTriggerOperator;
import org.apache.iotdb.db.qp.logical.sys.UnSetTTLOperator;
import org.apache.iotdb.db.qp.logical.sys.UnloadFileOperator;
import org.apache.iotdb.db.qp.logical.sys.UnsetTemplateOperator;
import org.apache.iotdb.db.qp.sql.IoTDBSqlParser.ConstantContext;
import org.apache.iotdb.db.qp.sql.IoTDBSqlParser.CqGroupByTimeClauseContext;
import org.apache.iotdb.db.qp.sql.IoTDBSqlParser.ExpressionContext;
import org.apache.iotdb.db.qp.utils.DatetimeUtils;
import org.apache.iotdb.db.query.executor.fill.IFill;
import org.apache.iotdb.db.query.executor.fill.LinearFill;
import org.apache.iotdb.db.query.executor.fill.PreviousFill;
import org.apache.iotdb.db.query.executor.fill.ValueFill;
import org.apache.iotdb.db.query.expression.Expression;
import org.apache.iotdb.db.query.expression.ResultColumn;
import org.apache.iotdb.db.query.expression.binary.AdditionExpression;
import org.apache.iotdb.db.query.expression.binary.DivisionExpression;
import org.apache.iotdb.db.query.expression.binary.EqualToExpression;
import org.apache.iotdb.db.query.expression.binary.GreaterEqualExpression;
import org.apache.iotdb.db.query.expression.binary.GreaterThanExpression;
import org.apache.iotdb.db.query.expression.binary.LessEqualExpression;
import org.apache.iotdb.db.query.expression.binary.LessThanExpression;
import org.apache.iotdb.db.query.expression.binary.LogicAndExpression;
import org.apache.iotdb.db.query.expression.binary.LogicOrExpression;
import org.apache.iotdb.db.query.expression.binary.ModuloExpression;
import org.apache.iotdb.db.query.expression.binary.MultiplicationExpression;
import org.apache.iotdb.db.query.expression.binary.NonEqualExpression;
import org.apache.iotdb.db.query.expression.binary.SubtractionExpression;
import org.apache.iotdb.db.query.expression.leaf.ConstantOperand;
import org.apache.iotdb.db.query.expression.leaf.TimeSeriesOperand;
import org.apache.iotdb.db.query.expression.multi.FunctionExpression;
import org.apache.iotdb.db.query.expression.unary.LogicNotExpression;
import org.apache.iotdb.db.query.expression.unary.NegationExpression;
import org.apache.iotdb.db.query.expression.unary.RegularExpression;
import org.apache.iotdb.tsfile.common.conf.TSFileDescriptor;
import org.apache.iotdb.tsfile.common.constant.TsFileConstant;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.apache.iotdb.tsfile.utils.Pair;
import org.apache.iotdb.tsfile.utils.StringContainer;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.iotdb.db.index.common.IndexConstant.PATTERN;
import static org.apache.iotdb.db.index.common.IndexConstant.THRESHOLD;
import static org.apache.iotdb.db.index.common.IndexConstant.TOP_K;
import static org.apache.iotdb.db.qp.constant.SQLConstant.TIME_PATH;
import static org.apache.iotdb.db.qp.constant.SQLConstant.TOK_KILL_QUERY;

public class IoTDBSqlVisitor extends IoTDBSqlParserBaseVisitor<Operator> {

  private static final String DELETE_RANGE_ERROR_MSG =
      "For delete statement, where clause can only contain atomic expressions like : "
          + "time > XXX, time <= XXX, or two atomic expressions connected by 'AND'";

  private static final String DELETE_ONLY_SUPPORT_TIME_EXP_ERROR_MSG =
      "For delete statement, where clause can only contain time expressions, "
          + "value filter is not currently supported.";

  // used to match "{x}", where x is an integer.
  // for create-cq clause and select-into clause.
  private static final Pattern leveledPathNodePattern = Pattern.compile("\\$\\{\\w+}");

  private ZoneId zoneId;
  private QueryOperator queryOp;

  private IoTDBConstant.ClientVersion clientVersion = IoTDBConstant.ClientVersion.V_0_13;

  public void setZoneId(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  public void setClientVersion(IoTDBConstant.ClientVersion clientVersion) {
    this.clientVersion = clientVersion;
  }

  /** 1. Top Level Description */
  @Override
  public Operator visitSingleStatement(IoTDBSqlParser.SingleStatementContext ctx) {
    Operator operator = visit(ctx.statement());
    if (ctx.DEBUG() != null) {
      operator.setDebug(true);
    }
    return operator;
  }

  /** 2. Data Definition Language (DDL) */

  // Create Storage Group

  @Override
  public Operator visitSetStorageGroup(IoTDBSqlParser.SetStorageGroupContext ctx) {
    SetStorageGroupOperator setStorageGroupOperator =
        new SetStorageGroupOperator(SQLConstant.TOK_METADATA_SET_FILE_LEVEL);
    PartialPath path = parsePrefixPath(ctx.prefixPath());
    setStorageGroupOperator.setPath(path);
    return setStorageGroupOperator;
  }

  @Override
  public Operator visitCreateStorageGroup(IoTDBSqlParser.CreateStorageGroupContext ctx) {
    SetStorageGroupOperator setStorageGroupOperator =
        new SetStorageGroupOperator(SQLConstant.TOK_METADATA_SET_FILE_LEVEL);
    PartialPath path = parsePrefixPath(ctx.prefixPath());
    setStorageGroupOperator.setPath(path);
    return setStorageGroupOperator;
  }

  // Create Timeseries

  @Override
  public Operator visitCreateNonAlignedTimeseries(
      IoTDBSqlParser.CreateNonAlignedTimeseriesContext ctx) {
    CreateTimeSeriesOperator createTimeSeriesOperator =
        new CreateTimeSeriesOperator(SQLConstant.TOK_METADATA_CREATE);
    createTimeSeriesOperator.setPath(parseFullPath(ctx.fullPath()));
    if (ctx.attributeClauses() != null) {
      parseAttributeClauses(ctx.attributeClauses(), createTimeSeriesOperator);
    }
    return createTimeSeriesOperator;
  }

  @Override
  public Operator visitCreateAlignedTimeseries(IoTDBSqlParser.CreateAlignedTimeseriesContext ctx) {
    CreateAlignedTimeSeriesOperator createAlignedTimeSeriesOperator =
        new CreateAlignedTimeSeriesOperator(SQLConstant.TOK_METADATA_CREATE);
    createAlignedTimeSeriesOperator.setPrefixPath(parseFullPath(ctx.fullPath()));
    parseAlignedMeasurements(ctx.alignedMeasurements(), createAlignedTimeSeriesOperator);
    return createAlignedTimeSeriesOperator;
  }

  public void parseAlignedMeasurements(
      IoTDBSqlParser.AlignedMeasurementsContext ctx,
      CreateAlignedTimeSeriesOperator createAlignedTimeSeriesOperator) {
    for (int i = 0; i < ctx.nodeNameWithoutWildcard().size(); i++) {
      createAlignedTimeSeriesOperator.addMeasurement(
          parseNodeName(ctx.nodeNameWithoutWildcard(i).getText()));
      parseAttributeClauses(ctx.attributeClauses(i), createAlignedTimeSeriesOperator);
    }
  }

  public void parseAttributeClauses(
      IoTDBSqlParser.AttributeClausesContext ctx,
      CreateTimeSeriesOperator createTimeSeriesOperator) {
    if (ctx.alias() != null) {
      createTimeSeriesOperator.setAlias(parseNodeName(ctx.alias().nodeNameCanInExpr().getText()));
    }
    final String dataType = ctx.dataType.getText().toUpperCase();
    final TSDataType tsDataType = TSDataType.valueOf(dataType);
    createTimeSeriesOperator.setDataType(tsDataType);

    final IoTDBDescriptor ioTDBDescriptor = IoTDBDescriptor.getInstance();
    TSEncoding encoding = ioTDBDescriptor.getDefaultEncodingByType(tsDataType);
    if (Objects.nonNull(ctx.encoding)) {
      String encodingString = ctx.encoding.getText().toUpperCase();
      encoding = TSEncoding.valueOf(encodingString);
    }
    createTimeSeriesOperator.setEncoding(encoding);

    CompressionType compressor;
    List<IoTDBSqlParser.PropertyClauseContext> properties = ctx.propertyClause();
    if (ctx.compressor != null) {
      compressor = CompressionType.valueOf(ctx.compressor.getText().toUpperCase());
    } else {
      compressor = TSFileDescriptor.getInstance().getConfig().getCompressor();
    }
    Map<String, String> props = null;
    if (ctx.propertyClause(0) != null) {
      props = new HashMap<>(properties.size());
      for (IoTDBSqlParser.PropertyClauseContext property : properties) {
        props.put(
            parseIdentifier(property.identifier().getText()).toLowerCase(),
            parseStringLiteral(property.propertyValue().getText().toLowerCase()));
      }
    }
    createTimeSeriesOperator.setCompressor(compressor);
    createTimeSeriesOperator.setProps(props);
    if (ctx.tagClause() != null) {
      parseTagClause(ctx.tagClause(), createTimeSeriesOperator);
    }
    if (ctx.attributeClause() != null) {
      parseAttributeClause(ctx.attributeClause(), createTimeSeriesOperator);
    }
  }

  public void parseAttributeClauses(
      IoTDBSqlParser.AttributeClausesContext ctx,
      CreateAlignedTimeSeriesOperator createAlignedTimeSeriesOperator) {
    if (ctx.alias() != null) {
      createAlignedTimeSeriesOperator.addAliasList(
          parseNodeName(ctx.alias().nodeNameCanInExpr().getText()));
    } else {
      createAlignedTimeSeriesOperator.addAliasList(null);
    }

    String dataTypeString = ctx.dataType.getText().toUpperCase();
    TSDataType dataType = TSDataType.valueOf(dataTypeString);
    createAlignedTimeSeriesOperator.addDataType(dataType);

    TSEncoding encoding = IoTDBDescriptor.getInstance().getDefaultEncodingByType(dataType);
    if (Objects.nonNull(ctx.encoding)) {
      String encodingString = ctx.encoding.getText().toUpperCase();
      encoding = TSEncoding.valueOf(encodingString);
    }
    createAlignedTimeSeriesOperator.addEncoding(encoding);

    CompressionType compressor = TSFileDescriptor.getInstance().getConfig().getCompressor();
    if (ctx.compressor != null) {
      String compressorString = ctx.compressor.getText().toUpperCase();
      compressor = CompressionType.valueOf(compressorString);
    }
    createAlignedTimeSeriesOperator.addCompressor(compressor);

    if (ctx.propertyClause(0) != null) {
      throw new SQLParserException("create aligned timeseries: property is not supported yet.");
    }

    if (ctx.tagClause() != null) {
      parseTagClause(ctx.tagClause(), createAlignedTimeSeriesOperator);
    } else {
      createAlignedTimeSeriesOperator.addTagsList(null);
    }

    if (ctx.attributeClause() != null) {
      parseAttributeClause(ctx.attributeClause(), createAlignedTimeSeriesOperator);
    } else {
      createAlignedTimeSeriesOperator.addAttributesList(null);
    }
  }

  // Create Schema Template
  @Override
  public Operator visitCreateSchemaTemplate(IoTDBSqlParser.CreateSchemaTemplateContext ctx) {
    CreateTemplateOperator createTemplateOperator =
        new CreateTemplateOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_CREATE);
    createTemplateOperator.setName(parseIdentifier(ctx.templateName.getText()));
    if (ctx.ALIGNED() != null) {
      // aligned
      List<String> measurements = new ArrayList<>();
      List<TSDataType> dataTypes = new ArrayList<>();
      List<TSEncoding> encodings = new ArrayList<>();
      List<CompressionType> compressors = new ArrayList<>();
      for (IoTDBSqlParser.TemplateMeasurementClauseContext templateClauseContext :
          ctx.templateMeasurementClause()) {
        measurements.add(parseNodeName(templateClauseContext.nodeNameWithoutWildcard().getText()));
        parseAttributeClause(
            templateClauseContext.attributeClauses(), dataTypes, encodings, compressors);
      }
      createTemplateOperator.addMeasurements(measurements);
      createTemplateOperator.addDataTypes(dataTypes);
      createTemplateOperator.addEncodings(encodings);
      createTemplateOperator.addCompressor(compressors);
    } else {
      // non-aligned
      for (IoTDBSqlParser.TemplateMeasurementClauseContext templateClauseContext :
          ctx.templateMeasurementClause()) {
        parseTemplateMeasurementClause(templateClauseContext, createTemplateOperator);
      }
    }
    return createTemplateOperator;
  }

  private void parseTemplateMeasurementClause(
      IoTDBSqlParser.TemplateMeasurementClauseContext ctx,
      CreateTemplateOperator createTemplateOperator) {
    List<String> measurements = new ArrayList<>();
    List<TSDataType> dataTypes = new ArrayList<>();
    List<TSEncoding> encodings = new ArrayList<>();
    List<CompressionType> compressors = new ArrayList<>();
    // single template measurement
    measurements.add(parseNodeName(ctx.nodeNameWithoutWildcard().getText()));
    parseAttributeClause(ctx.attributeClauses(), dataTypes, encodings, compressors);
    createTemplateOperator.addMeasurements(measurements);
    createTemplateOperator.addDataTypes(dataTypes);
    createTemplateOperator.addEncodings(encodings);
    createTemplateOperator.addCompressor(compressors);
  }

  void parseAttributeClause(
      IoTDBSqlParser.AttributeClausesContext ctx,
      List<TSDataType> dataTypes,
      List<TSEncoding> encodings,
      List<CompressionType> compressors) {
    if (ctx.alias() != null) {
      throw new SQLParserException("schema template: alias is not supported yet.");
    }

    String dataTypeString = ctx.dataType.getText().toUpperCase();
    TSDataType dataType = TSDataType.valueOf(dataTypeString);
    dataTypes.add(dataType);

    TSEncoding encoding = IoTDBDescriptor.getInstance().getDefaultEncodingByType(dataType);
    if (Objects.nonNull(ctx.encoding)) {
      String encodingString = ctx.encoding.getText().toUpperCase();
      encoding = TSEncoding.valueOf(encodingString);
    }
    encodings.add(encoding);

    CompressionType compressor = TSFileDescriptor.getInstance().getConfig().getCompressor();
    if (ctx.compressor != null) {
      String compressorString = ctx.compressor.getText().toUpperCase();
      compressor = CompressionType.valueOf(compressorString);
    }
    compressors.add(compressor);

    if (ctx.propertyClause(0) != null) {
      throw new SQLParserException("schema template: property is not supported yet.");
    }

    if (ctx.tagClause() != null) {
      throw new SQLParserException("schema template: tag is not supported yet.");
    }

    if (ctx.attributeClause() != null) {
      throw new SQLParserException("schema template: attribute is not supported yet.");
    }
  }

  // Create Timeseries Of Schema Template

  @Override
  public Operator visitCreateTimeseriesOfSchemaTemplate(
      IoTDBSqlParser.CreateTimeseriesOfSchemaTemplateContext ctx) {
    ActivateTemplateOperator operator =
        new ActivateTemplateOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_ACTIVATE);
    operator.setPrefixPath(parsePrefixPath(ctx.prefixPath()));
    return operator;
  }

  // Create Function

  @Override
  public Operator visitCreateFunction(IoTDBSqlParser.CreateFunctionContext ctx) {
    CreateFunctionOperator createFunctionOperator =
        new CreateFunctionOperator(SQLConstant.TOK_FUNCTION_CREATE);
    createFunctionOperator.setUdfName(parseIdentifier(ctx.udfName.getText()));
    createFunctionOperator.setClassName(parseStringLiteral(ctx.className.getText()));
    return createFunctionOperator;
  }

  // Create Trigger

  @Override
  public Operator visitCreateTrigger(IoTDBSqlParser.CreateTriggerContext ctx) {
    CreateTriggerOperator createTriggerOperator =
        new CreateTriggerOperator(SQLConstant.TOK_TRIGGER_CREATE);
    createTriggerOperator.setTriggerName(parseIdentifier(ctx.triggerName.getText()));
    createTriggerOperator.setEvent(
        ctx.triggerEventClause().BEFORE() != null
            ? TriggerEvent.BEFORE_INSERT
            : TriggerEvent.AFTER_INSERT);
    createTriggerOperator.setFullPath(parseFullPath(ctx.fullPath()));
    createTriggerOperator.setClassName(parseStringLiteral(ctx.className.getText()));
    if (ctx.triggerAttributeClause() != null) {
      for (IoTDBSqlParser.TriggerAttributeContext triggerAttributeContext :
          ctx.triggerAttributeClause().triggerAttribute()) {
        createTriggerOperator.addAttribute(
            parseStringLiteral(triggerAttributeContext.key.getText()),
            parseStringLiteral(triggerAttributeContext.value.getText()));
      }
    }
    return createTriggerOperator;
  }

  // Create Continuous Query

  @Override
  public Operator visitCreateContinuousQuery(IoTDBSqlParser.CreateContinuousQueryContext ctx) {
    CreateContinuousQueryOperator createContinuousQueryOperator =
        new CreateContinuousQueryOperator(SQLConstant.TOK_CONTINUOUS_QUERY_CREATE);

    createContinuousQueryOperator.setQuerySql(ctx.getText());

    createContinuousQueryOperator.setContinuousQueryName(
        parseIdentifier(ctx.continuousQueryName.getText()));

    if (ctx.resampleClause() != null) {
      parseResampleClause(ctx.resampleClause(), createContinuousQueryOperator);
    }

    parseCqSelectIntoClause(ctx.cqSelectIntoClause(), createContinuousQueryOperator);

    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    sb.append(ctx.cqSelectIntoClause().selectClause().getText().substring(6));
    sb.append(" from ");
    sb.append(ctx.cqSelectIntoClause().fromClause().prefixPath(0).getText());

    sb.append(" group by ([now() - ");
    String groupByInterval =
        ctx.cqSelectIntoClause().cqGroupByTimeClause().DURATION_LITERAL().getText();
    if (createContinuousQueryOperator.getForInterval() == 0) {
      sb.append(groupByInterval);
    } else {
      List<TerminalNode> durations = ctx.resampleClause().DURATION_LITERAL();
      sb.append(durations.get(durations.size() - 1).getText());
    }
    sb.append(", now()), ");
    sb.append(groupByInterval);
    sb.append(")");
    if (queryOp.isGroupByLevel()) {
      sb.append(", level = ");
      int[] levels = queryOp.getSpecialClauseComponent().getLevels();
      sb.append(levels[0]);
      for (int i = 1; i < levels.length; i++) {
        sb.append(String.format(", %d", levels[i]));
      }
    }
    createContinuousQueryOperator.setQuerySql(sb.toString());

    if (createContinuousQueryOperator.getEveryInterval() == 0) {
      createContinuousQueryOperator.setEveryInterval(
          ((GroupByClauseComponent) queryOp.getSpecialClauseComponent()).getUnit());
    }

    if (createContinuousQueryOperator.getEveryInterval()
        < IoTDBDescriptor.getInstance().getConfig().getContinuousQueryMinimumEveryInterval()) {
      throw new SQLParserException(
          "CQ: every interval should not be lower than the minimum value you configured.");
    }

    if (createContinuousQueryOperator.getForInterval() == 0) {
      createContinuousQueryOperator.setForInterval(
          ((GroupByClauseComponent) queryOp.getSpecialClauseComponent()).getUnit());
    }

    return createContinuousQueryOperator;
  }

  public void parseCqSelectIntoClause(
      IoTDBSqlParser.CqSelectIntoClauseContext ctx,
      CreateContinuousQueryOperator createContinuousQueryOperator) {
    queryOp = new GroupByQueryOperator();

    parseSelectClause(ctx.selectClause());
    parseFromClause(ctx.fromClause());

    if (queryOp.getSelectComponent().getResultColumns().size() > 1) {
      throw new SQLParserException("CQ: CQ currently does not support multiple result columns.");
    }
    if (queryOp.getFromComponent().getPrefixPaths().size() > 1) {
      throw new SQLParserException("CQ: CQ currently does not support multiple series.");
    }

    parseCqGroupByTimeClause(ctx.cqGroupByTimeClause(), createContinuousQueryOperator);

    if (queryOp.isGroupByLevel()) {
      int[] groupByQueryLevels = queryOp.getSpecialClauseComponent().getLevels();
      int fromPrefixLevelLimit = queryOp.getFromComponent().getPrefixPaths().get(0).getNodeLength();
      if (Arrays.stream(groupByQueryLevels).max().getAsInt() >= fromPrefixLevelLimit) {
        throw new SQLParserException("CQ: Level should not exceed the <from_prefix> length.");
      }
    }

    createContinuousQueryOperator.setTargetPath(parseIntoPath(ctx.intoPath()));
    createContinuousQueryOperator.setQueryOperator(queryOp);
  }

  public void parseCqGroupByTimeClause(
      CqGroupByTimeClauseContext ctx, CreateContinuousQueryOperator createContinuousQueryOperator) {
    GroupByClauseComponent groupByClauseComponent = new GroupByClauseComponent();

    long groupByInterval =
        parseTimeUnitOrSlidingStep(ctx.DURATION_LITERAL().getText(), true, groupByClauseComponent);
    groupByClauseComponent.setUnit(groupByInterval);
    createContinuousQueryOperator.setGroupByTimeInterval(groupByInterval);
    createContinuousQueryOperator.setGroupByTimeIntervalString(ctx.DURATION_LITERAL().getText());

    groupByClauseComponent.setSlidingStep(groupByClauseComponent.getUnit());
    groupByClauseComponent.setSlidingStepByMonth(groupByClauseComponent.isIntervalByMonth());

    groupByClauseComponent.setLeftCRightO(true);

    if (ctx.LEVEL() != null && ctx.INTEGER_LITERAL() != null) {
      int[] levels = new int[ctx.INTEGER_LITERAL().size()];
      for (int i = 0; i < ctx.INTEGER_LITERAL().size(); i++) {
        levels[i] = Integer.parseInt(ctx.INTEGER_LITERAL().get(i).getText());
      }
      groupByClauseComponent.setLevels(levels);
    }

    queryOp.setSpecialClauseComponent(groupByClauseComponent);
  }

  public void parseResampleClause(
      IoTDBSqlParser.ResampleClauseContext ctx, CreateContinuousQueryOperator operator) {
    if (ctx.DURATION_LITERAL().size() == 1) {
      if (ctx.EVERY() != null) {
        operator.setEveryInterval(
            DatetimeUtils.convertDurationStrToLong(ctx.DURATION_LITERAL(0).getText()));
      } else if (ctx.FOR() != null) {
        operator.setForInterval(
            DatetimeUtils.convertDurationStrToLong(ctx.DURATION_LITERAL(0).getText()));
      }
    } else if (ctx.DURATION_LITERAL().size() == 2) {
      operator.setEveryInterval(
          DatetimeUtils.convertDurationStrToLong(ctx.DURATION_LITERAL(0).getText()));
      operator.setForInterval(
          DatetimeUtils.convertDurationStrToLong(ctx.DURATION_LITERAL(1).getText()));
    }

    if (ctx.BOUNDARY() != null) {
      operator.setFirstExecutionTimeBoundary(parseDateExpression(ctx.dateExpression()));
    }
  }

  // Alter Timeseries

  @Override
  public Operator visitAlterTimeseries(IoTDBSqlParser.AlterTimeseriesContext ctx) {
    AlterTimeSeriesOperator alterTimeSeriesOperator =
        new AlterTimeSeriesOperator(SQLConstant.TOK_METADATA_ALTER);
    alterTimeSeriesOperator.setPath(parseFullPath(ctx.fullPath()));
    parseAlterClause(ctx.alterClause(), alterTimeSeriesOperator);
    return alterTimeSeriesOperator;
  }

  private void parseAlterClause(
      IoTDBSqlParser.AlterClauseContext ctx, AlterTimeSeriesOperator alterTimeSeriesOperator) {
    Map<String, String> alterMap = new HashMap<>();
    // rename
    if (ctx.RENAME() != null) {
      alterTimeSeriesOperator.setAlterType(AlterType.RENAME);
      alterMap.put(
          parseIdentifier(ctx.beforeName.getText()), parseIdentifier(ctx.currentName.getText()));
    } else if (ctx.SET() != null) {
      // set
      alterTimeSeriesOperator.setAlterType(AlterType.SET);
      setMap(ctx, alterMap);
    } else if (ctx.DROP() != null) {
      // drop
      alterTimeSeriesOperator.setAlterType(AlterType.DROP);
      for (IoTDBSqlParser.IdentifierContext dropId : ctx.identifier()) {
        alterMap.put(parseIdentifier(dropId.getText()), null);
      }
    } else if (ctx.TAGS() != null) {
      // add tag
      alterTimeSeriesOperator.setAlterType(AlterType.ADD_TAGS);
      setMap(ctx, alterMap);
    } else if (ctx.ATTRIBUTES() != null) {
      // add attribute
      alterTimeSeriesOperator.setAlterType(AlterType.ADD_ATTRIBUTES);
      setMap(ctx, alterMap);
    } else {
      // upsert
      alterTimeSeriesOperator.setAlterType(AlterType.UPSERT);
      if (ctx.aliasClause() != null) {
        parseAliasClause(ctx.aliasClause(), alterTimeSeriesOperator);
      }
      if (ctx.tagClause() != null) {
        parseTagClause(ctx.tagClause(), alterTimeSeriesOperator);
      }
      if (ctx.attributeClause() != null) {
        parseAttributeClause(ctx.attributeClause(), alterTimeSeriesOperator);
      }
    }
    alterTimeSeriesOperator.setAlterMap(alterMap);
  }

  public void parseAliasClause(
      IoTDBSqlParser.AliasClauseContext ctx, AlterTimeSeriesOperator alterTimeSeriesOperator) {
    if (alterTimeSeriesOperator != null && ctx.identifier() != null) {
      alterTimeSeriesOperator.setAlias(parseIdentifier(ctx.identifier().getText()));
    }
  }

  // Delete Storage Group

  @Override
  public Operator visitDeleteStorageGroup(IoTDBSqlParser.DeleteStorageGroupContext ctx) {
    List<PartialPath> deletePaths = new ArrayList<>();
    List<IoTDBSqlParser.PrefixPathContext> prefixPaths = ctx.prefixPath();
    for (IoTDBSqlParser.PrefixPathContext prefixPath : prefixPaths) {
      deletePaths.add(parsePrefixPath(prefixPath));
    }
    DeleteStorageGroupOperator deleteStorageGroupOperator =
        new DeleteStorageGroupOperator(SQLConstant.TOK_METADATA_DELETE_FILE_LEVEL);
    deleteStorageGroupOperator.setDeletePathList(deletePaths);
    return deleteStorageGroupOperator;
  }

  // Delete Timeseries

  @Override
  public Operator visitDeleteTimeseries(IoTDBSqlParser.DeleteTimeseriesContext ctx) {
    List<PartialPath> deletePaths = new ArrayList<>();
    List<IoTDBSqlParser.PrefixPathContext> prefixPaths = ctx.prefixPath();
    for (IoTDBSqlParser.PrefixPathContext prefixPath : prefixPaths) {
      deletePaths.add(parsePrefixPath(prefixPath));
    }
    DeleteTimeSeriesOperator deleteTimeSeriesOperator =
        new DeleteTimeSeriesOperator(SQLConstant.TOK_METADATA_DELETE);
    deleteTimeSeriesOperator.setDeletePathList(deletePaths);
    return deleteTimeSeriesOperator;
  }

  // Delete Partition

  @Override
  public Operator visitDeletePartition(IoTDBSqlParser.DeletePartitionContext ctx) {
    DeletePartitionOperator deletePartitionOperator =
        new DeletePartitionOperator(SQLConstant.TOK_DELETE_PARTITION);
    deletePartitionOperator.setStorageGroupName(parsePrefixPath(ctx.prefixPath()));
    Set<Long> idSet = new HashSet<>();
    for (TerminalNode terminalNode : ctx.INTEGER_LITERAL()) {
      idSet.add(Long.parseLong(terminalNode.getText()));
    }
    deletePartitionOperator.setPartitionIds(idSet);
    return deletePartitionOperator;
  }

  // Drop Function

  @Override
  public Operator visitDropFunction(IoTDBSqlParser.DropFunctionContext ctx) {
    DropFunctionOperator dropFunctionOperator =
        new DropFunctionOperator(SQLConstant.TOK_FUNCTION_DROP);
    dropFunctionOperator.setUdfName(parseIdentifier(ctx.udfName.getText()));
    return dropFunctionOperator;
  }

  // Drop Trigger

  @Override
  public Operator visitDropTrigger(IoTDBSqlParser.DropTriggerContext ctx) {
    DropTriggerOperator dropTriggerOperator = new DropTriggerOperator(SQLConstant.TOK_TRIGGER_DROP);
    dropTriggerOperator.setTriggerName(parseIdentifier(ctx.triggerName.getText()));
    return dropTriggerOperator;
  }

  // Drop Continuous Query

  @Override
  public Operator visitDropContinuousQuery(IoTDBSqlParser.DropContinuousQueryContext ctx) {
    DropContinuousQueryOperator dropContinuousQueryOperator =
        new DropContinuousQueryOperator(SQLConstant.TOK_CONTINUOUS_QUERY_DROP);
    dropContinuousQueryOperator.setContinuousQueryName(
        parseIdentifier(ctx.continuousQueryName.getText()));
    return dropContinuousQueryOperator;
  }

  // Drop Schema Template

  @Override
  public Operator visitDropSchemaTemplate(IoTDBSqlParser.DropSchemaTemplateContext ctx) {
    DropTemplateOperator dropTemplateOperator =
        new DropTemplateOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_DROP);
    dropTemplateOperator.setTemplateName(parseIdentifier((ctx.templateName.getText())));
    return dropTemplateOperator;
  }

  // Set TTL

  @Override
  public Operator visitSetTTL(IoTDBSqlParser.SetTTLContext ctx) {
    SetTTLOperator operator = new SetTTLOperator(SQLConstant.TOK_SET);
    operator.setStorageGroup(parsePrefixPath(ctx.prefixPath()));
    operator.setDataTTL(Long.parseLong(ctx.INTEGER_LITERAL().getText()));
    return operator;
  }

  // Unset TTL

  @Override
  public Operator visitUnsetTTL(IoTDBSqlParser.UnsetTTLContext ctx) {
    UnSetTTLOperator operator = new UnSetTTLOperator(SQLConstant.TOK_UNSET);
    operator.setStorageGroup(parsePrefixPath(ctx.prefixPath()));
    return operator;
  }

  // Set Schema Template
  @Override
  public Operator visitSetSchemaTemplate(IoTDBSqlParser.SetSchemaTemplateContext ctx) {
    SetTemplateOperator operator = new SetTemplateOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_SET);
    operator.setPrefixPath(parsePrefixPath(ctx.prefixPath()));
    operator.setTemplateName(parseIdentifier(ctx.templateName.getText()));
    return operator;
  }

  // Unset Schema Template

  @Override
  public Operator visitUnsetSchemaTemplate(IoTDBSqlParser.UnsetSchemaTemplateContext ctx) {
    UnsetTemplateOperator operator =
        new UnsetTemplateOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_UNSET);
    operator.setPrefixPath(parsePrefixPath(ctx.prefixPath()));
    operator.setTemplateName(parseIdentifier(ctx.templateName.getText()));
    return operator;
  }

  // Start Trigger

  @Override
  public Operator visitStartTrigger(IoTDBSqlParser.StartTriggerContext ctx) {
    StartTriggerOperator startTriggerOperator =
        new StartTriggerOperator(SQLConstant.TOK_TRIGGER_START);
    startTriggerOperator.setTriggerName(parseIdentifier(ctx.triggerName.getText()));
    return startTriggerOperator;
  }

  // Stop Trigger

  @Override
  public Operator visitStopTrigger(IoTDBSqlParser.StopTriggerContext ctx) {
    StopTriggerOperator stopTriggerOperator = new StopTriggerOperator(SQLConstant.TOK_TRIGGER_STOP);
    stopTriggerOperator.setTriggerName(parseIdentifier(ctx.triggerName.getText()));
    return stopTriggerOperator;
  }

  // Show Storage Group

  @Override
  public Operator visitShowStorageGroup(IoTDBSqlParser.ShowStorageGroupContext ctx) {
    if (ctx.prefixPath() != null) {
      return new ShowStorageGroupOperator(
          SQLConstant.TOK_STORAGE_GROUP, parsePrefixPath(ctx.prefixPath()));
    } else {
      return new ShowStorageGroupOperator(
          SQLConstant.TOK_STORAGE_GROUP, new PartialPath(SQLConstant.getSingleRootArray()));
    }
  }

  // Show Devices

  @Override
  public Operator visitShowDevices(IoTDBSqlParser.ShowDevicesContext ctx) {
    ShowDevicesOperator showDevicesOperator;
    if (ctx.prefixPath() != null) {
      showDevicesOperator =
          new ShowDevicesOperator(SQLConstant.TOK_DEVICES, parsePrefixPath(ctx.prefixPath()));
    } else {
      showDevicesOperator =
          new ShowDevicesOperator(
              SQLConstant.TOK_DEVICES, new PartialPath(SQLConstant.getSingleRootArray()));
    }
    if (ctx.limitClause() != null) {
      parseLimitClause(ctx.limitClause(), showDevicesOperator);
    }
    // show devices wtih storage group
    if (ctx.WITH() != null) {
      showDevicesOperator.setSgCol(true);
    }
    return showDevicesOperator;
  }

  // Show Timeseries

  @Override
  public Operator visitShowTimeseries(IoTDBSqlParser.ShowTimeseriesContext ctx) {
    boolean orderByHeat = ctx.LATEST() != null;
    ShowTimeSeriesOperator showTimeSeriesOperator;
    if (ctx.prefixPath() != null) {
      showTimeSeriesOperator =
          new ShowTimeSeriesOperator(
              SQLConstant.TOK_TIMESERIES, parsePrefixPath(ctx.prefixPath()), orderByHeat);
    } else {
      showTimeSeriesOperator =
          new ShowTimeSeriesOperator(
              SQLConstant.TOK_TIMESERIES,
              new PartialPath(SQLConstant.getSingleRootArray()),
              orderByHeat);
    }
    if (ctx.showWhereClause() != null) {
      parseShowWhereClause(ctx.showWhereClause(), showTimeSeriesOperator);
    }
    if (ctx.limitClause() != null) {
      parseLimitClause(ctx.limitClause(), showTimeSeriesOperator);
    }
    return showTimeSeriesOperator;
  }

  private void parseShowWhereClause(
      IoTDBSqlParser.ShowWhereClauseContext ctx, ShowTimeSeriesOperator operator) {
    IoTDBSqlParser.PropertyValueContext propertyValueContext;
    if (ctx.containsExpression() != null) {
      operator.setContains(true);
      propertyValueContext = ctx.containsExpression().propertyValue();
      operator.setKey(parseIdentifier(ctx.containsExpression().identifier().getText()));
    } else {
      operator.setContains(false);
      propertyValueContext = ctx.propertyClause().propertyValue();
      operator.setKey(parseIdentifier(ctx.propertyClause().identifier().getText()));
    }
    operator.setValue(parseStringLiteral(propertyValueContext.getText()));
  }

  // Show Child Paths

  @Override
  public Operator visitShowChildPaths(IoTDBSqlParser.ShowChildPathsContext ctx) {
    if (ctx.prefixPath() != null) {
      return new ShowChildPathsOperator(
          SQLConstant.TOK_CHILD_PATHS, parsePrefixPath(ctx.prefixPath()));
    } else {
      return new ShowChildPathsOperator(
          SQLConstant.TOK_CHILD_PATHS, new PartialPath(SQLConstant.getSingleRootArray()));
    }
  }

  // Show Child Nodes

  @Override
  public Operator visitShowChildNodes(IoTDBSqlParser.ShowChildNodesContext ctx) {
    if (ctx.prefixPath() != null) {
      return new ShowChildNodesOperator(
          SQLConstant.TOK_CHILD_NODES, parsePrefixPath(ctx.prefixPath()));
    } else {
      return new ShowChildNodesOperator(
          SQLConstant.TOK_CHILD_NODES, new PartialPath(SQLConstant.getSingleRootArray()));
    }
  }

  // Show Functions

  @Override
  public Operator visitShowFunctions(IoTDBSqlParser.ShowFunctionsContext ctx) {
    return new ShowFunctionsOperator(SQLConstant.TOK_SHOW_FUNCTIONS);
  }

  // Show Triggers

  @Override
  public Operator visitShowTriggers(IoTDBSqlParser.ShowTriggersContext ctx) {
    return new ShowTriggersOperator(SQLConstant.TOK_SHOW_TRIGGERS);
  }

  // Show Continuous Queries

  @Override
  public Operator visitShowContinuousQueries(IoTDBSqlParser.ShowContinuousQueriesContext ctx) {
    return new ShowContinuousQueriesOperator(SQLConstant.TOK_SHOW_CONTINUOUS_QUERIES);
  }

  // Show TTL

  @Override
  public Operator visitShowTTL(IoTDBSqlParser.ShowTTLContext ctx) {
    List<PartialPath> storageGroups = new ArrayList<>();
    List<IoTDBSqlParser.PrefixPathContext> prefixPathList = ctx.prefixPath();
    for (IoTDBSqlParser.PrefixPathContext prefixPath : prefixPathList) {
      storageGroups.add(parsePrefixPath(prefixPath));
    }
    return new ShowTTLOperator(storageGroups);
  }

  // Show All TTL

  @Override
  public Operator visitShowAllTTL(IoTDBSqlParser.ShowAllTTLContext ctx) {
    List<PartialPath> storageGroups = new ArrayList<>();
    return new ShowTTLOperator(storageGroups);
  }

  // Show Schema Template

  @Override
  public Operator visitShowSchemaTemplates(IoTDBSqlParser.ShowSchemaTemplatesContext ctx) {
    return new ShowTemplatesOperator(SQLConstant.TOK_SCHEMA_TEMPLATE_SHOW);
  }

  // Show Measurements In Schema Template

  public Operator visitShowNodesInSchemaTemplate(
      IoTDBSqlParser.ShowNodesInSchemaTemplateContext ctx) {
    String templateName = parseIdentifier(ctx.templateName.getText());
    return new ShowNodesInTemplateOperator(
        SQLConstant.TOK_SCHEMA_TEMPLATE_SHOW_NODES, templateName);
  }

  // Show Paths Set Schema Template

  public Operator visitShowPathsSetSchemaTemplate(
      IoTDBSqlParser.ShowPathsSetSchemaTemplateContext ctx) {
    String templateName = parseIdentifier(ctx.templateName.getText());
    return new ShowPathsSetTemplateOperator(
        SQLConstant.TOK_SCHEMA_TEMPLATE_SHOW_PATHS_SET, templateName);
  }

  // Show Paths Using Schema Template

  public Operator visitShowPathsUsingSchemaTemplate(
      IoTDBSqlParser.ShowPathsUsingSchemaTemplateContext ctx) {
    String templateName = parseIdentifier(ctx.templateName.getText());
    return new ShowPathsUsingTemplateOperator(
        SQLConstant.TOK_SCHEMA_TEMPLATE_SHOW_PATHS_USING, templateName);
  }

  // countStorageGroup

  @Override
  public Operator visitCountStorageGroup(IoTDBSqlParser.CountStorageGroupContext ctx) {
    IoTDBSqlParser.PrefixPathContext pathContext = ctx.prefixPath();
    PartialPath path =
        (pathContext != null
            ? parsePrefixPath(pathContext)
            : new PartialPath(SQLConstant.getSingleRootArray()));
    return new CountOperator(SQLConstant.TOK_COUNT_STORAGE_GROUP, path);
  }

  // Count Devices

  @Override
  public Operator visitCountDevices(IoTDBSqlParser.CountDevicesContext ctx) {
    IoTDBSqlParser.PrefixPathContext pathContext = ctx.prefixPath();
    PartialPath path =
        (pathContext != null
            ? parsePrefixPath(pathContext)
            : new PartialPath(SQLConstant.getSingleRootArray()));
    return new CountOperator(SQLConstant.TOK_COUNT_DEVICES, path);
  }

  // Count Timeseries

  @Override
  public Operator visitCountTimeseries(IoTDBSqlParser.CountTimeseriesContext ctx) {
    IoTDBSqlParser.PrefixPathContext pathContext = ctx.prefixPath();
    PartialPath path =
        (pathContext != null
            ? parsePrefixPath(pathContext)
            : new PartialPath(SQLConstant.getSingleRootArray()));
    if (ctx.INTEGER_LITERAL() != null) {
      return new CountOperator(
          SQLConstant.TOK_COUNT_NODE_TIMESERIES,
          path,
          Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
    } else {
      return new CountOperator(SQLConstant.TOK_COUNT_TIMESERIES, path);
    }
  }

  // Count Nodes

  @Override
  public Operator visitCountNodes(IoTDBSqlParser.CountNodesContext ctx) {
    return new CountOperator(
        SQLConstant.TOK_COUNT_NODES,
        parsePrefixPath(ctx.prefixPath()),
        Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
  }

  /** 3. Data Manipulation Language (DML) */

  // Select Statement

  @Override
  public Operator visitSelectStatement(IoTDBSqlParser.SelectStatementContext ctx) {
    // 1. Visit special clause first to initialize different query operator
    if (ctx.specialClause() != null) {
      queryOp = (QueryOperator) visit(ctx.specialClause());
    }
    // 2. There is no special clause in query statement.
    if (queryOp == null) {
      queryOp = new QueryOperator();
    }
    // 3. Visit select, from, where in sequence
    parseSelectClause(ctx.selectClause());
    parseFromClause(ctx.fromClause());
    if (ctx.whereClause() != null) {
      WhereComponent whereComponent = parseWhereClause(ctx.whereClause());
      if (whereComponent != null) {
        queryOp.setWhereComponent(whereComponent);
      }
    }
    queryOp.setEnableTracing(ctx.TRACING() != null);
    // 4. Check whether it's a select-into clause
    return ctx.intoClause() == null ? queryOp : parseAndConstructSelectIntoOperator(ctx);
  }

  private SelectIntoOperator parseAndConstructSelectIntoOperator(
      IoTDBSqlParser.SelectStatementContext ctx) {
    if (queryOp.getFromComponent().getPrefixPaths().size() != 1) {
      throw new SQLParserException(
          "select into: the number of prefix paths in the from clause should be 1.");
    }

    int sourcePathsCount = queryOp.getSelectComponent().getResultColumns().size();
    if (sourcePathsCount != ctx.intoClause().intoPath().size()) {
      throw new SQLParserException(
          "select into: the number of source paths and the number of target paths should be the same.");
    }

    SelectIntoOperator selectIntoOperator = new SelectIntoOperator();
    selectIntoOperator.setQueryOperator(queryOp);
    List<PartialPath> intoPaths = new ArrayList<>();
    for (int i = 0; i < sourcePathsCount; ++i) {
      intoPaths.add(parseIntoPath(ctx.intoClause().intoPath(i)));
    }
    selectIntoOperator.setIntoPaths(intoPaths);
    selectIntoOperator.setIntoPathsAligned(ctx.intoClause().ALIGNED() != null);
    return selectIntoOperator;
  }

  private PartialPath parseIntoPath(IoTDBSqlParser.IntoPathContext intoPathContext) {
    int levelLimitOfSourcePrefixPath;
    if (queryOp.isGroupByLevel()) {
      levelLimitOfSourcePrefixPath =
          Arrays.stream(queryOp.getSpecialClauseComponent().getLevels()).max().getAsInt();
    } else {
      levelLimitOfSourcePrefixPath =
          queryOp.getFromComponent().getPrefixPaths().get(0).getNodeLength() - 1;
    }

    PartialPath intoPath = null;
    if (intoPathContext.fullPath() != null) {
      intoPath = parseFullPath(intoPathContext.fullPath());

      Matcher m = leveledPathNodePattern.matcher(intoPath.getFullPath());
      while (m.find()) {
        String param = m.group();
        int nodeIndex;
        try {
          nodeIndex = Integer.parseInt(param.substring(2, param.length() - 1).trim());
        } catch (NumberFormatException e) {
          throw new SQLParserException("the x of ${x} should be an integer.");
        }
        if (nodeIndex < 1 || levelLimitOfSourcePrefixPath < nodeIndex) {
          throw new SQLParserException(
              "the x of ${x} should be greater than 0 and equal to or less than <level> or the length of queried path prefix.");
        }
      }
    } else if (intoPathContext.nodeNameWithoutWildcard() != null) {
      List<IoTDBSqlParser.NodeNameWithoutWildcardContext> nodeNameWithoutStars =
          intoPathContext.nodeNameWithoutWildcard();
      String[] intoPathNodes =
          new String[1 + levelLimitOfSourcePrefixPath + nodeNameWithoutStars.size()];

      intoPathNodes[0] = "root";
      for (int i = 1; i <= levelLimitOfSourcePrefixPath; ++i) {
        intoPathNodes[i] = "${" + i + "}";
      }
      for (int i = 1; i <= nodeNameWithoutStars.size(); ++i) {
        intoPathNodes[levelLimitOfSourcePrefixPath + i] =
            parseNodeName(nodeNameWithoutStars.get(i - 1).getText());
      }

      intoPath = new PartialPath(intoPathNodes);
    }

    return intoPath;
  }

  @Override
  public Operator visitSpecialLimitStatement(IoTDBSqlParser.SpecialLimitStatementContext ctx) {
    return visit(ctx.specialLimit());
  }

  @Override
  public Operator visitOrderByTimeStatement(IoTDBSqlParser.OrderByTimeStatementContext ctx) {
    queryOp = new QueryOperator();
    parseOrderByTimeClause(ctx.orderByTimeClause());
    if (ctx.specialLimit() != null) {
      return visit(ctx.specialLimit());
    }
    return queryOp;
  }

  @Override
  public Operator visitGroupByTimeStatement(IoTDBSqlParser.GroupByTimeStatementContext ctx) {
    queryOp = new GroupByQueryOperator();
    parseGroupByTimeClause(ctx.groupByTimeClause());
    if (ctx.orderByTimeClause() != null) {
      parseOrderByTimeClause(ctx.orderByTimeClause());
    }
    if (ctx.specialLimit() != null) {
      return visit(ctx.specialLimit());
    }
    return queryOp;
  }

  @Override
  public Operator visitGroupByFillStatement(IoTDBSqlParser.GroupByFillStatementContext ctx) {
    queryOp = new GroupByFillQueryOperator();
    parseGroupByFillClause(ctx.groupByFillClause());
    if (ctx.orderByTimeClause() != null) {
      parseOrderByTimeClause(ctx.orderByTimeClause());
    }
    if (ctx.specialLimit() != null) {
      return visit(ctx.specialLimit());
    }
    return queryOp;
  }

  @Override
  public Operator visitGroupByLevelStatement(IoTDBSqlParser.GroupByLevelStatementContext ctx) {
    queryOp = new AggregationQueryOperator();
    parseGroupByLevelClause(ctx.groupByLevelClause());
    if (ctx.orderByTimeClause() != null) {
      parseOrderByTimeClause(ctx.orderByTimeClause());
    }
    if (ctx.specialLimit() != null) {
      return visit(ctx.specialLimit());
    }
    return queryOp;
  }

  @Override
  public Operator visitFillStatement(IoTDBSqlParser.FillStatementContext ctx) {
    queryOp = new FillQueryOperator();
    parseFillClause(ctx.fillClause());
    if (ctx.slimitClause() != null) {
      parseSlimitClause(ctx.slimitClause());
    }
    if (ctx.alignByDeviceClauseOrDisableAlign() != null) {
      parseAlignByDeviceClauseOrDisableAlign(ctx.alignByDeviceClauseOrDisableAlign());
    }
    return queryOp;
  }

  @Override
  public Operator visitLimitStatement(IoTDBSqlParser.LimitStatementContext ctx) {
    if (queryOp == null) {
      queryOp = new QueryOperator();
    }
    parseLimitClause(ctx.limitClause(), queryOp);
    if (ctx.slimitClause() != null) {
      parseSlimitClause(ctx.slimitClause());
    }
    if (ctx.alignByDeviceClauseOrDisableAlign() != null) {
      parseAlignByDeviceClauseOrDisableAlign(ctx.alignByDeviceClauseOrDisableAlign());
    }
    return queryOp;
  }

  @Override
  public Operator visitSlimitStatement(IoTDBSqlParser.SlimitStatementContext ctx) {
    if (queryOp == null) {
      queryOp = new QueryOperator();
    }
    parseSlimitClause(ctx.slimitClause());
    if (ctx.limitClause() != null) {
      parseLimitClause(ctx.limitClause(), queryOp);
    }
    if (ctx.alignByDeviceClauseOrDisableAlign() != null) {
      parseAlignByDeviceClauseOrDisableAlign(ctx.alignByDeviceClauseOrDisableAlign());
    }
    return queryOp;
  }

  @Override
  public Operator visitWithoutNullStatement(IoTDBSqlParser.WithoutNullStatementContext ctx) {
    if (queryOp == null) {
      queryOp = new QueryOperator();
    }
    parseWithoutNullClause(ctx.withoutNullClause());
    if (ctx.limitClause() != null) {
      parseLimitClause(ctx.limitClause(), queryOp);
    }
    if (ctx.slimitClause() != null) {
      parseSlimitClause(ctx.slimitClause());
    }
    if (ctx.alignByDeviceClauseOrDisableAlign() != null) {
      parseAlignByDeviceClauseOrDisableAlign(ctx.alignByDeviceClauseOrDisableAlign());
    }
    return queryOp;
  }

  @Override
  public Operator visitAlignByDeviceClauseOrDisableAlignStatement(
      IoTDBSqlParser.AlignByDeviceClauseOrDisableAlignStatementContext ctx) {
    if (queryOp == null) {
      queryOp = new QueryOperator();
    }
    parseAlignByDeviceClauseOrDisableAlign(ctx.alignByDeviceClauseOrDisableAlign());
    return queryOp;
  }

  private void parseAlignByDeviceClauseOrDisableAlign(
      IoTDBSqlParser.AlignByDeviceClauseOrDisableAlignContext ctx) {
    SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
    if (specialClauseComponent == null) {
      specialClauseComponent = new SpecialClauseComponent();
    }
    if (ctx.alignByDeviceClause() != null) {
      parseAlignByDeviceClause(specialClauseComponent);
    } else {
      parseDisableAlign(specialClauseComponent);
    }
    queryOp.setSpecialClauseComponent(specialClauseComponent);
  }

  private void parseAlignByDeviceClause(SpecialClauseComponent specialClauseComponent) {
    specialClauseComponent.setAlignByDevice(true);
  }

  private void parseDisableAlign(SpecialClauseComponent specialClauseComponent) {
    specialClauseComponent.setAlignByTime(false);
  }

  private void parseOrderByTimeClause(IoTDBSqlParser.OrderByTimeClauseContext ctx) {
    if (ctx.DESC() != null) {
      SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
      if (specialClauseComponent == null) {
        specialClauseComponent = new SpecialClauseComponent();
      }
      specialClauseComponent.setAscending(false);
      queryOp.setSpecialClauseComponent(specialClauseComponent);
    }
  }

  private void parseGroupByTimeClause(IoTDBSqlParser.GroupByTimeClauseContext ctx) {
    GroupByClauseComponent groupByClauseComponent = new GroupByClauseComponent();
    groupByClauseComponent.setLeftCRightO(ctx.timeRange().LS_BRACKET() != null);
    // parse timeUnit
    groupByClauseComponent.setUnit(
        parseTimeUnitOrSlidingStep(
            ctx.DURATION_LITERAL(0).getText(), true, groupByClauseComponent));
    if (groupByClauseComponent.getUnit() <= 0) {
      throw new SQLParserException(
          "The second parameter time interval should be a positive integer.");
    }
    // parse sliding step
    if (ctx.DURATION_LITERAL().size() == 2) {
      groupByClauseComponent.setSlidingStep(
          parseTimeUnitOrSlidingStep(
              ctx.DURATION_LITERAL(1).getText(), false, groupByClauseComponent));
    } else {
      groupByClauseComponent.setSlidingStep(groupByClauseComponent.getUnit());
      groupByClauseComponent.setSlidingStepByMonth(groupByClauseComponent.isIntervalByMonth());
    }

    parseTimeInterval(ctx.timeRange(), groupByClauseComponent);

    if (ctx.LEVEL() != null && ctx.INTEGER_LITERAL() != null) {
      int[] levels = new int[ctx.INTEGER_LITERAL().size()];
      for (int i = 0; i < ctx.INTEGER_LITERAL().size(); i++) {
        levels[i] = Integer.parseInt(ctx.INTEGER_LITERAL().get(i).getText());
      }
      groupByClauseComponent.setLevels(levels);
    }
    queryOp.setSpecialClauseComponent(groupByClauseComponent);
  }

  private void parseGroupByFillClause(IoTDBSqlParser.GroupByFillClauseContext ctx) {
    GroupByFillClauseComponent groupByFillClauseComponent = new GroupByFillClauseComponent();
    groupByFillClauseComponent.setLeftCRightO(ctx.timeRange().LS_BRACKET() != null);
    // parse timeUnit
    groupByFillClauseComponent.setUnit(
        parseTimeUnitOrSlidingStep(
            ctx.DURATION_LITERAL(0).getText(), true, groupByFillClauseComponent));
    // parse sliding step
    if (ctx.DURATION_LITERAL().size() == 2) {
      groupByFillClauseComponent.setSlidingStep(
          parseTimeUnitOrSlidingStep(
              ctx.DURATION_LITERAL(1).getText(), false, groupByFillClauseComponent));
      if (groupByFillClauseComponent.getSlidingStep() < groupByFillClauseComponent.getUnit()) {
        throw new SQLParserException(
            "The third parameter sliding step shouldn't be smaller than the second parameter time interval.");
      }
    } else {
      groupByFillClauseComponent.setSlidingStep(groupByFillClauseComponent.getUnit());
      groupByFillClauseComponent.setSlidingStepByMonth(
          groupByFillClauseComponent.isIntervalByMonth());
    }

    parseTimeInterval(ctx.timeRange(), groupByFillClauseComponent);

    if (ctx.fillClause().oldTypeClause().size() > 0) {
      // old type fill logic
      List<IoTDBSqlParser.OldTypeClauseContext> list = ctx.fillClause().oldTypeClause();
      Map<TSDataType, IFill> fillTypes = new EnumMap<>(TSDataType.class);
      for (IoTDBSqlParser.OldTypeClauseContext typeClause : list) {
        if (typeClause.ALL() != null) {
          parseAllTypeClause(typeClause, fillTypes);
        } else {
          parsePrimitiveTypeClause(typeClause, fillTypes);
        }
      }

      int usePrevious = 0;
      int useLinear = 0;
      int useValue = 0;
      for (IFill iFill : fillTypes.values()) {
        if (iFill instanceof PreviousFill) {
          usePrevious = 1;
        }
        if (iFill instanceof LinearFill) {
          useLinear = 1;
        }
        if (iFill instanceof ValueFill) {
          useValue = 1;
        }
      }
      if (usePrevious + useLinear + useValue > 1) {
        throw new SQLParserException("The old type logic could only use one type of fill");
      }

      groupByFillClauseComponent.setFillTypes(fillTypes);
    } else {
      groupByFillClauseComponent.setSingleFill(getSingleIFill(ctx.fillClause()));
    }
    queryOp.setSpecialClauseComponent(groupByFillClauseComponent);
  }

  public void parseGroupByLevelClause(IoTDBSqlParser.GroupByLevelClauseContext ctx) {
    SpecialClauseComponent groupByLevelClauseComponent = new SpecialClauseComponent();
    int[] levels = new int[ctx.INTEGER_LITERAL().size()];
    for (int i = 0; i < ctx.INTEGER_LITERAL().size(); i++) {
      levels[i] = Integer.parseInt(ctx.INTEGER_LITERAL().get(i).getText());
    }
    groupByLevelClauseComponent.setLevels(levels);

    queryOp.setSpecialClauseComponent(groupByLevelClauseComponent);
  }

  public void parseFillClause(IoTDBSqlParser.FillClauseContext ctx) {
    FillClauseComponent fillClauseComponent = new FillClauseComponent();

    if (ctx.oldTypeClause().size() > 0) {
      // old type fill logic
      List<IoTDBSqlParser.OldTypeClauseContext> list = ctx.oldTypeClause();
      Map<TSDataType, IFill> fillTypes = new EnumMap<>(TSDataType.class);
      for (IoTDBSqlParser.OldTypeClauseContext typeClause : list) {
        if (typeClause.ALL() != null) {
          if (typeClause.linearClause() != null) {
            throw new SQLParserException("fill all doesn't support linear fill");
          }
          parseAllTypeClause(typeClause, fillTypes);
          break;
        } else {
          parsePrimitiveTypeClause(typeClause, fillTypes);
        }
      }
      fillClauseComponent.setFillTypes(fillTypes);
    } else {
      // new single fill logic
      fillClauseComponent.setSingleFill(getSingleIFill(ctx));
    }
    queryOp.setSpecialClauseComponent(fillClauseComponent);
  }

  private void parseTimeInterval(
      IoTDBSqlParser.TimeRangeContext timeInterval, GroupByClauseComponent groupByClauseComponent) {
    long currentTime = DatetimeUtils.currentTime();
    long startTime = parseTimeValue(timeInterval.timeValue(0), currentTime);
    long endTime = parseTimeValue(timeInterval.timeValue(1), currentTime);
    groupByClauseComponent.setStartTime(startTime);
    groupByClauseComponent.setEndTime(endTime);
    if (startTime >= endTime) {
      throw new SQLParserException("Start time should be smaller than endTime in GroupBy");
    }
  }

  private void parseWithoutNullClause(IoTDBSqlParser.WithoutNullClauseContext ctx) {
    SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
    if (specialClauseComponent == null) {
      specialClauseComponent = new SpecialClauseComponent();
    }
    // add without null columns
    List<ExpressionContext> expressionContexts = ctx.expression();
    for (ExpressionContext expressionContext : expressionContexts) {
      specialClauseComponent.addWithoutNullColumn(parseExpression(expressionContext));
    }
    specialClauseComponent.setWithoutAnyNull(ctx.ANY() != null);
    specialClauseComponent.setWithoutAllNull(ctx.ALL() != null);
    queryOp.setSpecialClauseComponent(specialClauseComponent);
  }

  private IFill getSingleIFill(IoTDBSqlParser.FillClauseContext ctx) {
    int defaultFillInterval = IoTDBDescriptor.getInstance().getConfig().getDefaultFillInterval();
    if (ctx.linearClause() != null) { // linear
      if (ctx.linearClause().DURATION_LITERAL(0) != null) {
        String beforeStr = ctx.linearClause().DURATION_LITERAL(0).getText();
        String afterStr = ctx.linearClause().DURATION_LITERAL(1).getText();
        return new LinearFill(beforeStr, afterStr);
      } else {
        return new LinearFill(defaultFillInterval, defaultFillInterval);
      }
    } else if (ctx.previousClause() != null) { // previous
      if (ctx.previousClause().DURATION_LITERAL() != null) {
        String preRangeStr = ctx.previousClause().DURATION_LITERAL().getText();
        return new PreviousFill(preRangeStr);
      } else {
        return new PreviousFill(defaultFillInterval);
      }
    } else if (ctx.specificValueClause() != null) { // value
      if (ctx.specificValueClause().constant() != null) {
        return new ValueFill(ctx.specificValueClause().constant().getText());
      } else {
        throw new SQLParserException("fill value cannot be null");
      }
    } else if (ctx.previousUntilLastClause() != null) { // previous until last
      if (ctx.previousUntilLastClause().DURATION_LITERAL() != null) {
        String preRangeStr = ctx.previousUntilLastClause().DURATION_LITERAL().getText();
        return new PreviousFill(preRangeStr, true);
      } else {
        return new PreviousFill(defaultFillInterval, true);
      }
    } else {
      throw new SQLParserException("unknown single fill type");
    }
  }

  private void parseAllTypeClause(
      IoTDBSqlParser.OldTypeClauseContext ctx, Map<TSDataType, IFill> fillTypes) {
    IFill fill;
    int defaultFillInterval = IoTDBDescriptor.getInstance().getConfig().getDefaultFillInterval();

    if (ctx.linearClause() != null) { // linear
      if (ctx.linearClause().DURATION_LITERAL(0) != null) {
        String beforeStr = ctx.linearClause().DURATION_LITERAL(0).getText();
        String afterStr = ctx.linearClause().DURATION_LITERAL(1).getText();
        fill = new LinearFill(beforeStr, afterStr);
      } else {
        fill = new LinearFill(defaultFillInterval, defaultFillInterval);
      }
    } else if (ctx.previousClause() != null) { // previous
      if (ctx.previousClause().DURATION_LITERAL() != null) {
        String preRangeStr = ctx.previousClause().DURATION_LITERAL().getText();
        fill = new PreviousFill(preRangeStr);
      } else {
        fill = new PreviousFill(defaultFillInterval);
      }
    } else if (ctx.specificValueClause() != null) {
      throw new SQLParserException("fill all doesn't support value fill");
    } else { // previous until last
      if (ctx.previousUntilLastClause().DURATION_LITERAL() != null) {
        String preRangeStr = ctx.previousUntilLastClause().DURATION_LITERAL().getText();
        fill = new PreviousFill(preRangeStr, true);
      } else {
        fill = new PreviousFill(defaultFillInterval, true);
      }
    }

    for (TSDataType tsDataType : TSDataType.values()) {
      if (tsDataType == TSDataType.VECTOR) {
        // TODO: TSDataType VECTOR
        continue;
      }
      if (fill instanceof LinearFill
          && (tsDataType == TSDataType.BOOLEAN || tsDataType == TSDataType.TEXT)) {
        continue;
      }
      fillTypes.put(tsDataType, fill.copy());
    }
  }

  private void parsePrimitiveTypeClause(
      IoTDBSqlParser.OldTypeClauseContext ctx, Map<TSDataType, IFill> fillTypes) {
    TSDataType dataType = parseType(ctx.dataType.getText());
    if (dataType == TSDataType.VECTOR) {
      throw new SQLParserException(String.format("type %s cannot use fill function", dataType));
    }

    if (ctx.linearClause() != null
        && (dataType == TSDataType.TEXT || dataType == TSDataType.BOOLEAN)) {
      throw new SQLParserException(
          String.format(
              "type %s cannot use %s fill function",
              dataType, ctx.linearClause().LINEAR().getText()));
    }

    int defaultFillInterval = IoTDBDescriptor.getInstance().getConfig().getDefaultFillInterval();

    if (ctx.linearClause() != null) { // linear
      if (ctx.linearClause().DURATION_LITERAL(0) != null) {
        String beforeRangeStr = ctx.linearClause().DURATION_LITERAL(0).getText();
        String afterRangeStr = ctx.linearClause().DURATION_LITERAL(1).getText();
        LinearFill fill = new LinearFill(beforeRangeStr, afterRangeStr);
        fillTypes.put(dataType, fill);
      } else {
        fillTypes.put(dataType, new LinearFill(defaultFillInterval, defaultFillInterval));
      }
    } else if (ctx.previousClause() != null) { // previous
      if (ctx.previousClause().DURATION_LITERAL() != null) {
        String beforeStr = ctx.previousClause().DURATION_LITERAL().getText();
        fillTypes.put(dataType, new PreviousFill(beforeStr));
      } else {
        fillTypes.put(dataType, new PreviousFill(defaultFillInterval));
      }
    } else if (ctx.specificValueClause() != null) { // value
      if (ctx.specificValueClause().constant() != null) {
        fillTypes.put(
            dataType, new ValueFill(ctx.specificValueClause().constant().getText(), dataType));
      } else {
        throw new SQLParserException("fill value cannot be null");
      }
    } else { // previous until last
      if (ctx.previousUntilLastClause().DURATION_LITERAL() != null) {
        String preRangeStr = ctx.previousUntilLastClause().DURATION_LITERAL().getText();
        fillTypes.put(dataType, new PreviousFill(preRangeStr, true));
      } else {
        fillTypes.put(dataType, new PreviousFill(defaultFillInterval, true));
      }
    }
  }

  private TSDataType parseType(String datatype) {
    String type = datatype.toLowerCase();
    switch (type) {
      case "int32":
        return TSDataType.INT32;
      case "int64":
        return TSDataType.INT64;
      case "float":
        return TSDataType.FLOAT;
      case "double":
        return TSDataType.DOUBLE;
      case "boolean":
        return TSDataType.BOOLEAN;
      case "text":
        return TSDataType.TEXT;
      default:
        throw new SQLParserException("not a valid fill type : " + type);
    }
  }

  // Insert Statement

  @Override
  public Operator visitInsertStatement(IoTDBSqlParser.InsertStatementContext ctx) {
    InsertOperator insertOp = new InsertOperator(SQLConstant.TOK_INSERT);
    insertOp.setDevice(parsePrefixPath(ctx.prefixPath()));
    boolean isTimeDefault = parseInsertColumnSpec(ctx.insertColumnsSpec(), insertOp);
    parseInsertValuesSpec(ctx.insertValuesSpec(), insertOp, isTimeDefault);
    insertOp.setAligned(ctx.ALIGNED() != null);
    return insertOp;
  }

  private boolean parseInsertColumnSpec(
      IoTDBSqlParser.InsertColumnsSpecContext ctx, InsertOperator insertOp) {
    List<String> measurementList = new ArrayList<>();
    for (IoTDBSqlParser.NodeNameWithoutWildcardContext measurementName :
        ctx.nodeNameWithoutWildcard()) {
      measurementList.add(parseNodeName(measurementName.getText()));
    }
    insertOp.setMeasurementList(measurementList.toArray(new String[0]));
    return (ctx.TIME() == null && ctx.TIMESTAMP() == null);
  }

  private void parseInsertValuesSpec(
      IoTDBSqlParser.InsertValuesSpecContext ctx, InsertOperator insertOp, boolean isTimeDefault) {
    List<IoTDBSqlParser.InsertMultiValueContext> insertMultiValues = ctx.insertMultiValue();
    List<String[]> valueLists = new ArrayList<>();
    long[] timeArray = new long[insertMultiValues.size()];
    for (int i = 0; i < insertMultiValues.size(); i++) {
      // parse timestamp
      long timestamp;
      if (insertMultiValues.get(i).timeValue() != null) {
        if (isTimeDefault) {
          throw new SQLParserException(
              "the measurementList's size is not consistent with the valueList's size");
        }
        timestamp =
            parseTimeValue(insertMultiValues.get(i).timeValue(), DatetimeUtils.currentTime());
      } else {
        if (!isTimeDefault) {
          throw new SQLParserException(
              "the measurementList's size is not consistent with the valueList's size");
        }
        if (insertMultiValues.size() != 1) {
          throw new SQLParserException("need timestamps when insert multi rows");
        }
        timestamp = parseDateFormat(SQLConstant.NOW_FUNC);
      }
      timeArray[i] = timestamp;

      // parse values
      List<String> valueList = new ArrayList<>();
      List<IoTDBSqlParser.MeasurementValueContext> values =
          insertMultiValues.get(i).measurementValue();
      for (IoTDBSqlParser.MeasurementValueContext value : values) {
        for (IoTDBSqlParser.ConstantContext constant : value.constant()) {
          if (constant.STRING_LITERAL() != null) {
            valueList.add(parseStringLiteralInInsertValue(constant.getText()));
          } else {
            valueList.add(constant.getText());
          }
        }
      }
      valueLists.add(valueList.toArray(new String[0]));
    }
    insertOp.setTimes(timeArray);
    insertOp.setValueLists(valueLists);
  }

  // Delete Statement

  @Override
  public Operator visitDeleteStatement(IoTDBSqlParser.DeleteStatementContext ctx) {
    DeleteDataOperator deleteDataOp = new DeleteDataOperator(SQLConstant.TOK_DELETE);
    List<IoTDBSqlParser.PrefixPathContext> prefixPaths = ctx.prefixPath();
    for (IoTDBSqlParser.PrefixPathContext prefixPath : prefixPaths) {
      deleteDataOp.addPath(parsePrefixPath(prefixPath));
    }
    if (ctx.whereClause() != null) {
      WhereComponent whereComponent = parseWhereClause(ctx.whereClause());
      Pair<Long, Long> timeInterval = parseDeleteTimeInterval(whereComponent.getFilterOperator());
      deleteDataOp.setStartTime(timeInterval.left);
      deleteDataOp.setEndTime(timeInterval.right);
    } else {
      deleteDataOp.setStartTime(Long.MIN_VALUE);
      deleteDataOp.setEndTime(Long.MAX_VALUE);
    }
    return deleteDataOp;
  }

  private Pair<Long, Long> parseDeleteTimeInterval(FilterOperator filterOperator) {
    if (!filterOperator.isLeaf() && filterOperator.getFilterType() != FilterType.KW_AND) {
      throw new SQLParserException(DELETE_RANGE_ERROR_MSG);
    }

    if (filterOperator.isLeaf()) {
      return calcOperatorInterval(filterOperator);
    }

    List<FilterOperator> children = filterOperator.getChildren();
    FilterOperator lOperator = children.get(0);
    FilterOperator rOperator = children.get(1);
    if (!lOperator.isLeaf() || !rOperator.isLeaf()) {
      throw new SQLParserException(DELETE_RANGE_ERROR_MSG);
    }

    Pair<Long, Long> leftOpInterval = calcOperatorInterval(lOperator);
    Pair<Long, Long> rightOpInterval = calcOperatorInterval(rOperator);
    Pair<Long, Long> parsedInterval =
        new Pair<>(
            Math.max(leftOpInterval.left, rightOpInterval.left),
            Math.min(leftOpInterval.right, rightOpInterval.right));
    if (parsedInterval.left > parsedInterval.right) {
      throw new SQLParserException(
          "Invalid delete range: [" + parsedInterval.left + ", " + parsedInterval.right + "]");
    }
    return parsedInterval;
  }

  /** 4. Data Control Language (DCL) */

  // Create User

  @Override
  public Operator visitCreateUser(IoTDBSqlParser.CreateUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_CREATE, AuthorOperator.AuthorType.CREATE_USER);
    authorOperator.setUserName(ctx.userName.getText());
    authorOperator.setPassWord(parseStringLiteral(ctx.password.getText()));
    return authorOperator;
  }

  // Create Role

  @Override
  public Operator visitCreateRole(IoTDBSqlParser.CreateRoleContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_CREATE, AuthorOperator.AuthorType.CREATE_ROLE);
    authorOperator.setRoleName(ctx.roleName.getText());
    return authorOperator;
  }

  // Alter Password

  @Override
  public Operator visitAlterUser(IoTDBSqlParser.AlterUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(
            SQLConstant.TOK_AUTHOR_UPDATE_USER, AuthorOperator.AuthorType.UPDATE_USER);
    authorOperator.setUserName(ctx.userName.getText());
    authorOperator.setNewPassword(parseStringLiteral(ctx.password.getText()));
    return authorOperator;
  }

  // Grant User Privileges

  @Override
  public Operator visitGrantUser(IoTDBSqlParser.GrantUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_GRANT, AuthorOperator.AuthorType.GRANT_USER);
    authorOperator.setUserName(ctx.userName.getText());
    authorOperator.setPrivilegeList(parsePrivilege(ctx.privileges()));
    authorOperator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return authorOperator;
  }

  // Grant Role Privileges

  @Override
  public Operator visitGrantRole(IoTDBSqlParser.GrantRoleContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_GRANT, AuthorType.GRANT_ROLE);
    authorOperator.setRoleName(ctx.roleName.getText());
    authorOperator.setPrivilegeList(parsePrivilege(ctx.privileges()));
    authorOperator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return authorOperator;
  }

  // Grant User Role

  @Override
  public Operator visitGrantRoleToUser(IoTDBSqlParser.GrantRoleToUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(
            SQLConstant.TOK_AUTHOR_GRANT, AuthorOperator.AuthorType.GRANT_ROLE_TO_USER);
    authorOperator.setRoleName(ctx.roleName.getText());
    authorOperator.setUserName(ctx.userName.getText());
    return authorOperator;
  }

  // Revoke User Privileges

  @Override
  public Operator visitRevokeUser(IoTDBSqlParser.RevokeUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_GRANT, AuthorType.REVOKE_USER);
    authorOperator.setUserName(ctx.userName.getText());
    authorOperator.setPrivilegeList(parsePrivilege(ctx.privileges()));
    authorOperator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return authorOperator;
  }

  // Revoke Role Privileges

  @Override
  public Operator visitRevokeRole(IoTDBSqlParser.RevokeRoleContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_GRANT, AuthorType.REVOKE_ROLE);
    authorOperator.setRoleName(ctx.roleName.getText());
    authorOperator.setPrivilegeList(parsePrivilege(ctx.privileges()));
    authorOperator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return authorOperator;
  }

  // Revoke Role From User

  @Override
  public Operator visitRevokeRoleFromUser(IoTDBSqlParser.RevokeRoleFromUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_GRANT, AuthorType.REVOKE_ROLE_FROM_USER);
    authorOperator.setRoleName(ctx.roleName.getText());
    authorOperator.setUserName(ctx.userName.getText());
    return authorOperator;
  }

  // Drop User

  @Override
  public Operator visitDropUser(IoTDBSqlParser.DropUserContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_DROP, AuthorOperator.AuthorType.DROP_USER);
    authorOperator.setUserName(ctx.userName.getText());
    return authorOperator;
  }

  // Drop Role

  @Override
  public Operator visitDropRole(IoTDBSqlParser.DropRoleContext ctx) {
    AuthorOperator authorOperator =
        new AuthorOperator(SQLConstant.TOK_AUTHOR_DROP, AuthorOperator.AuthorType.DROP_ROLE);
    authorOperator.setRoleName(ctx.roleName.getText());
    return authorOperator;
  }

  // List Users

  @Override
  public Operator visitListUser(IoTDBSqlParser.ListUserContext ctx) {
    return new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_USER);
  }

  // List Roles

  @Override
  public Operator visitListRole(IoTDBSqlParser.ListRoleContext ctx) {
    return new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_ROLE);
  }

  // List Privileges

  @Override
  public Operator visitListPrivilegesUser(IoTDBSqlParser.ListPrivilegesUserContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_USER_PRIVILEGE);
    operator.setUserName(ctx.userName.getText());
    operator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return operator;
  }

  // List Privileges of Roles On Specific Path

  @Override
  public Operator visitListPrivilegesRole(IoTDBSqlParser.ListPrivilegesRoleContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_ROLE_PRIVILEGE);
    operator.setRoleName(ctx.roleName.getText());
    operator.setNodeNameList(parsePrefixPath(ctx.prefixPath()));
    return operator;
  }

  // List Privileges of Users

  @Override
  public Operator visitListUserPrivileges(IoTDBSqlParser.ListUserPrivilegesContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_USER_PRIVILEGE);
    operator.setUserName(ctx.userName.getText());
    return operator;
  }

  // List Privileges of Roles

  @Override
  public Operator visitListRolePrivileges(IoTDBSqlParser.ListRolePrivilegesContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_ROLE_PRIVILEGE);
    operator.setRoleName(ctx.roleName.getText());
    return operator;
  }

  // List Roles of Users

  @Override
  public Operator visitListAllRoleOfUser(IoTDBSqlParser.ListAllRoleOfUserContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_USER_ROLES);
    operator.setUserName(ctx.userName.getText());
    return operator;
  }

  // List Users of Role

  @Override
  public Operator visitListAllUserOfRole(IoTDBSqlParser.ListAllUserOfRoleContext ctx) {
    AuthorOperator operator =
        new AuthorOperator(SQLConstant.TOK_LIST, AuthorOperator.AuthorType.LIST_ROLE_USERS);
    operator.setRoleName(ctx.roleName.getText());
    return operator;
  }

  private String[] parsePrivilege(IoTDBSqlParser.PrivilegesContext ctx) {
    List<IoTDBSqlParser.PrivilegeValueContext> privilegeList = ctx.privilegeValue();
    List<String> privileges = new ArrayList<>();
    for (IoTDBSqlParser.PrivilegeValueContext privilegeValue : privilegeList) {
      privileges.add(privilegeValue.getText());
    }
    return privileges.toArray(new String[0]);
  }

  /** 5. Utility Statements */

  // Merge

  @Override
  public Operator visitMerge(IoTDBSqlParser.MergeContext ctx) {
    return new MergeOperator(SQLConstant.TOK_MERGE);
  }

  // Full Merge

  @Override
  public Operator visitFullMerge(IoTDBSqlParser.FullMergeContext ctx) {
    return new MergeOperator(SQLConstant.TOK_FULL_MERGE);
  }

  // Flush

  @Override
  public Operator visitFlush(IoTDBSqlParser.FlushContext ctx) {
    FlushOperator flushOperator = new FlushOperator(SQLConstant.TOK_FLUSH);
    if (ctx.BOOLEAN_LITERAL() != null) {
      flushOperator.setSeq(Boolean.parseBoolean(ctx.BOOLEAN_LITERAL().getText()));
    }
    if (ctx.prefixPath(0) != null) {
      List<PartialPath> storageGroups = new ArrayList<>();
      for (IoTDBSqlParser.PrefixPathContext prefixPathContext : ctx.prefixPath()) {
        storageGroups.add(parsePrefixPath(prefixPathContext));
      }
      flushOperator.setStorageGroupList(storageGroups);
    }
    return flushOperator;
  }

  // Clear Cache

  @Override
  public Operator visitClearCache(IoTDBSqlParser.ClearCacheContext ctx) {
    return new ClearCacheOperator(SQLConstant.TOK_CLEAR_CACHE);
  }

  // Settle

  @Override
  public Operator visitSettle(IoTDBSqlParser.SettleContext ctx) {
    SettleOperator settleOperator = new SettleOperator(SQLConstant.TOK_SETTLE);
    if (ctx.prefixPath() != null) {
      // Storage Group
      PartialPath sgPath = parsePrefixPath(ctx.prefixPath());
      settleOperator.setSgPath(sgPath);
      settleOperator.setIsSgPath(true);
    } else {
      // TsFile Path
      String tsFilePath = parseStringLiteral(ctx.tsFilePath.getText());
      settleOperator.setTsFilePath(tsFilePath);
      settleOperator.setIsSgPath(false);
    }
    return settleOperator;
  }

  // Set System To ReadOnly/Writable

  @Override
  public Operator visitSetSystemStatus(IoTDBSqlParser.SetSystemStatusContext ctx) {
    if (ctx.READONLY() != null) {
      // Set system to ReadOnly
      return new SetSystemModeOperator(SQLConstant.TOK_SET_SYSTEM_MODE, true);
    } else {
      // Set system to Writable
      return new SetSystemModeOperator(SQLConstant.TOK_SET_SYSTEM_MODE, false);
    }
  }

  // Show Version

  @Override
  public Operator visitShowVersion(IoTDBSqlParser.ShowVersionContext ctx) {
    return new ShowOperator(SQLConstant.TOK_VERSION);
  }

  // Show Flush Info

  @Override
  public Operator visitShowFlushInfo(IoTDBSqlParser.ShowFlushInfoContext ctx) {
    return new ShowOperator(SQLConstant.TOK_FLUSH_TASK_INFO);
  }

  // Show Lock Info

  @Override
  public Operator visitShowLockInfo(IoTDBSqlParser.ShowLockInfoContext ctx) {
    if (ctx.prefixPath() != null) {
      return new ShowLockInfoOperator(SQLConstant.TOK_LOCK_INFO, parsePrefixPath(ctx.prefixPath()));
    } else {
      return new ShowLockInfoOperator(
          SQLConstant.TOK_LOCK_INFO, new PartialPath(SQLConstant.getSingleRootArray()));
    }
  }

  // Show Query Resource

  @Override
  public Operator visitShowQueryResource(IoTDBSqlParser.ShowQueryResourceContext ctx) {
    return new ShowQueryResourceOperate(SQLConstant.TOK_SHOW_QUERY_RESOURCE);
  }
  // Show Query Processlist

  @Override
  public Operator visitShowQueryProcesslist(IoTDBSqlParser.ShowQueryProcesslistContext ctx) {
    return new ShowOperator(SQLConstant.TOK_QUERY_PROCESSLIST);
  }

  // Kill Query

  @Override
  public Operator visitKillQuery(IoTDBSqlParser.KillQueryContext ctx) {
    KillQueryOperator killQueryOperator = new KillQueryOperator(TOK_KILL_QUERY);
    if (ctx.INTEGER_LITERAL() != null) {
      killQueryOperator.setQueryId(Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
    }
    return killQueryOperator;
  }

  // Grant Watermark Embedding

  @Override
  public Operator visitGrantWatermarkEmbedding(IoTDBSqlParser.GrantWatermarkEmbeddingContext ctx) {
    List<IoTDBSqlParser.UsernameWithRootContext> usernameList = ctx.usernameWithRoot();
    List<String> users = new ArrayList<>();
    for (IoTDBSqlParser.UsernameWithRootContext username : usernameList) {
      users.add(username.getText());
    }
    return new DataAuthOperator(SQLConstant.TOK_GRANT_WATERMARK_EMBEDDING, users);
  }

  // Revoke Watermark Embedding

  @Override
  public Operator visitRevokeWatermarkEmbedding(
      IoTDBSqlParser.RevokeWatermarkEmbeddingContext ctx) {
    List<IoTDBSqlParser.UsernameWithRootContext> usernameList = ctx.usernameWithRoot();
    List<String> users = new ArrayList<>();
    for (IoTDBSqlParser.UsernameWithRootContext username : usernameList) {
      users.add(username.getText());
    }
    return new DataAuthOperator(SQLConstant.TOK_REVOKE_WATERMARK_EMBEDDING, users);
  }

  // Load Configuration

  @Override
  public Operator visitLoadConfiguration(IoTDBSqlParser.LoadConfigurationContext ctx) {
    if (ctx.GLOBAL() != null) {
      return new LoadConfigurationOperator(LoadConfigurationOperatorType.GLOBAL);
    } else {
      return new LoadConfigurationOperator(LoadConfigurationOperatorType.LOCAL);
    }
  }

  // Load Timeseries

  @Override
  public Operator visitLoadTimeseries(IoTDBSqlParser.LoadTimeseriesContext ctx) {
    if (ctx.prefixPath().nodeName().size() < 3) {
      throw new SQLParserException("data load command: child count < 3\n");
    }

    String csvPath = ctx.fileName.getText();
    StringContainer sc = new StringContainer(TsFileConstant.PATH_SEPARATOR);
    List<IoTDBSqlParser.NodeNameContext> nodeNames = ctx.prefixPath().nodeName();
    sc.addTail(ctx.prefixPath().ROOT().getText());
    for (IoTDBSqlParser.NodeNameContext nodeName : nodeNames) {
      sc.addTail(parseNodeName(nodeName));
    }
    return new LoadDataOperator(SQLConstant.TOK_DATALOAD, parseFilePath(csvPath), sc.toString());
  }

  // Load TsFile

  @Override
  public Operator visitLoadFile(IoTDBSqlParser.LoadFileContext ctx) {
    LoadFilesOperator loadFilesOperator =
        new LoadFilesOperator(
            new File(parseFilePath(ctx.fileName.getText())),
            true,
            IoTDBDescriptor.getInstance().getConfig().getDefaultStorageGroupLevel(),
            true);
    if (ctx.loadFilesClause() != null) {
      parseLoadFiles(loadFilesOperator, ctx.loadFilesClause());
    }
    return loadFilesOperator;
  }

  /**
   * used for parsing load tsfile, context will be one of "SCHEMA, LEVEL, METADATA", and maybe
   * followed by a recursion property statement
   *
   * @param operator the result operator, setting by clause context
   * @param ctx context of property statement
   */
  private void parseLoadFiles(
      LoadFilesOperator operator, IoTDBSqlParser.LoadFilesClauseContext ctx) {
    if (ctx.AUTOREGISTER() != null) {
      operator.setAutoCreateSchema(Boolean.parseBoolean(ctx.BOOLEAN_LITERAL().getText()));
    } else if (ctx.SGLEVEL() != null) {
      operator.setSgLevel(Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
    } else if (ctx.VERIFY() != null) {
      operator.setVerifyMetadata(Boolean.parseBoolean(ctx.BOOLEAN_LITERAL().getText()));
    } else {
      throw new SQLParserException(
          String.format(
              "load tsfile format %s error, please input AUTOREGISTER | SGLEVEL | VERIFY.",
              ctx.getText()));
    }
    if (ctx.loadFilesClause() != null) {
      parseLoadFiles(operator, ctx.loadFilesClause());
    }
  }

  // Remove TsFile

  @Override
  public Operator visitRemoveFile(IoTDBSqlParser.RemoveFileContext ctx) {
    return new RemoveFileOperator(new File(parseFilePath(ctx.fileName.getText())));
  }

  // Unload TsFile

  @Override
  public Operator visitUnloadFile(IoTDBSqlParser.UnloadFileContext ctx) {
    return new UnloadFileOperator(
        new File(parseFilePath(ctx.srcFileName.getText())),
        new File(parseFilePath(ctx.dstFileDir.getText())));
  }

  /** 6. sync statement */
  @Override
  public Operator visitCreatePipeSink(IoTDBSqlParser.CreatePipeSinkContext ctx) {
    CreatePipeSinkOperator operator =
        new CreatePipeSinkOperator(
            StringEscapeUtils.unescapeJava(ctx.pipeSinkName.getText()),
            StringEscapeUtils.unescapeJava(ctx.pipeSinkType.getText()));
    if (ctx.syncAttributeClauses() != null) {
      operator.setPipeSinkAttributes(parseSyncAttributeClauses(ctx.syncAttributeClauses()));
    }
    return operator;
  }

  private Map<String, String> parseSyncAttributeClauses(
      IoTDBSqlParser.SyncAttributeClausesContext ctx) {
    Map<String, String> attributes = new HashMap<>();
    Iterator<IoTDBSqlParser.PropertyClauseContext> propertyClauseIterator =
        ctx.propertyClause().iterator();
    while (propertyClauseIterator.hasNext()) {
      IoTDBSqlParser.PropertyClauseContext propertyClause = propertyClauseIterator.next();
      attributes.put(propertyClause.name.getText(), propertyClause.value.getText());
    }
    return attributes;
  }

  @Override
  public Operator visitDropPipeSink(IoTDBSqlParser.DropPipeSinkContext ctx) {
    DropPipeSinkOperator operator =
        new DropPipeSinkOperator(StringEscapeUtils.unescapeJava(ctx.pipeSinkName.getText()));
    return operator;
  }

  @Override
  public Operator visitShowPipeSink(IoTDBSqlParser.ShowPipeSinkContext ctx) {
    ShowPipeSinkOperator operator = new ShowPipeSinkOperator();
    if (ctx.pipeSinkName != null) {
      operator.setPipeSinkName(StringEscapeUtils.unescapeJava(ctx.pipeSinkName.getText()));
    }
    return operator;
  }

  @Override
  public Operator visitShowPipeSinkType(IoTDBSqlParser.ShowPipeSinkTypeContext ctx) {
    ShowPipeSinkTypeOperator operator = new ShowPipeSinkTypeOperator();
    return operator;
  }

  @Override
  public Operator visitCreatePipe(IoTDBSqlParser.CreatePipeContext ctx) throws SQLParserException {
    CreatePipeOperator operator =
        new CreatePipeOperator(
            StringEscapeUtils.unescapeJava(ctx.pipeName.getText()),
            StringEscapeUtils.unescapeJava(ctx.pipeSinkName.getText()));

    if (ctx.selectStatement() != null) {
      parseSelectStatementForPipe(ctx.selectStatement(), operator);
    }
    if (ctx.syncAttributeClauses() != null) {
      operator.setPipeAttributes(parseSyncAttributeClauses(ctx.syncAttributeClauses()));
    }
    return operator;
  }

  @Override
  public Operator visitShowPipe(IoTDBSqlParser.ShowPipeContext ctx) {
    ShowPipeOperator operator = new ShowPipeOperator();
    if (ctx.pipeName != null) {
      operator.setPipeName(StringEscapeUtils.unescapeJava(ctx.pipeName.getText()));
    }
    return operator;
  }

  @Override
  public Operator visitStopPipe(IoTDBSqlParser.StopPipeContext ctx) {
    return new StopPipeOperator(StringEscapeUtils.unescapeJava(ctx.pipeName.getText()));
  }

  @Override
  public Operator visitStartPipe(IoTDBSqlParser.StartPipeContext ctx) {
    return new StartPipeOperator(StringEscapeUtils.unescapeJava(ctx.pipeName.getText()));
  }

  @Override
  public Operator visitDropPipe(IoTDBSqlParser.DropPipeContext ctx) {
    return new DropPipeOperator(StringEscapeUtils.unescapeJava(ctx.pipeName.getText()));
  }

  private void parseSelectStatementForPipe(
      IoTDBSqlParser.SelectStatementContext ctx, CreatePipeOperator operator)
      throws SQLParserException {
    if (ctx.TRACING() != null || ctx.intoClause() != null || ctx.specialClause() != null) {
      throw new SQLParserException("Not support for this sql in pipe.");
    }

    // parse select
    IoTDBSqlParser.SelectClauseContext selectCtx = ctx.selectClause();
    if (selectCtx.LAST() != null
        || selectCtx.topClause() != null
        || selectCtx.resultColumn().size() != 1) {
      throw new SQLParserException("Not support for this sql in pipe.");
    }
    IoTDBSqlParser.ResultColumnContext resultColumnCtx = selectCtx.resultColumn(0);
    if (resultColumnCtx.AS() != null
        || !IoTDBConstant.MULTI_LEVEL_PATH_WILDCARD.equals(
            resultColumnCtx.expression().getText())) {
      throw new SQLParserException("Not support for this sql in pipe.");
    }

    // parse from
    IoTDBSqlParser.FromClauseContext fromCtx = ctx.fromClause();
    if (fromCtx.prefixPath().size() != 1
        || !IoTDBConstant.PATH_ROOT.equals(fromCtx.prefixPath(0).getText())) {
      throw new SQLParserException("Not support for this sql in pipe.");
    }

    // parse where
    IoTDBSqlParser.WhereClauseContext whereCtx = ctx.whereClause();
    if (whereCtx != null) {
      if (whereCtx.orExpression() == null || whereCtx.indexPredicateClause() != null) {
        throw new SQLParserException("Not support for this sql in pipe.");
      }
      IoTDBSqlParser.OrExpressionContext orExpressionCtx = whereCtx.orExpression();
      if (orExpressionCtx.andExpression().size() != 1) {
        throw new SQLParserException("Not support for this sql in pipe.");
      }
      IoTDBSqlParser.AndExpressionContext andExpressionCtx = orExpressionCtx.andExpression(0);
      if (andExpressionCtx.predicate().size() != 1) {
        throw new SQLParserException("Not support for this sql in pipe.");
      }
      IoTDBSqlParser.PredicateContext predicateCtx = andExpressionCtx.predicate(0);
      if (predicateCtx.comparisonOperator() == null
          || (predicateCtx.comparisonOperator().OPERATOR_GTE() == null
              && predicateCtx.comparisonOperator().OPERATOR_GT() == null)
          || predicateCtx.suffixPath() != null
          || predicateCtx.fullPath() != null) {
        throw new SQLParserException("Not support for this sql in pipe.");
      }
      IoTDBSqlParser.ConstantContext constantCtx = predicateCtx.constant();
      if (constantCtx.dateExpression() == null) {
        throw new SQLParserException("data type error for time limit");
      }
      operator.setStartTime(parseDateExpression(constantCtx.dateExpression()));
    }
  }

  @Override
  public Operator visitStartPipeServer(IoTDBSqlParser.StartPipeServerContext ctx) {
    return new StartPipeServerOperator(SQLConstant.TOK_PIPE_SERVER_START);
  }

  @Override
  public Operator visitStopPipeServer(IoTDBSqlParser.StopPipeServerContext ctx) {
    return new StopPipeServerOperator(SQLConstant.TOK_PIPE_SERVER_STOP);
  }

  @Override
  public Operator visitShowPipeServer(IoTDBSqlParser.ShowPipeServerContext ctx) {
    return new ShowPipeServerOperator(SQLConstant.TOK_SHOW_PIPE_SERVER);
  }

  /** 7. Common Clauses */

  // IoTDB Objects

  private PartialPath parseFullPath(IoTDBSqlParser.FullPathContext ctx) {
    List<IoTDBSqlParser.NodeNameWithoutWildcardContext> nodeNamesWithoutStar =
        ctx.nodeNameWithoutWildcard();
    String[] path = new String[nodeNamesWithoutStar.size() + 1];
    int i = 0;
    if (ctx.ROOT() != null) {
      path[0] = ctx.ROOT().getText();
    }
    for (IoTDBSqlParser.NodeNameWithoutWildcardContext nodeNameWithoutStar : nodeNamesWithoutStar) {
      i++;
      path[i] = parseNodeName(nodeNameWithoutStar.getText());
    }
    return new PartialPath(path);
  }

  private PartialPath parsePrefixPath(IoTDBSqlParser.PrefixPathContext ctx) {
    List<IoTDBSqlParser.NodeNameContext> nodeNames = ctx.nodeName();
    String[] path = new String[nodeNames.size() + 1];
    path[0] = ctx.ROOT().getText();
    for (int i = 0; i < nodeNames.size(); i++) {
      path[i + 1] = parseNodeName(nodeNames.get(i));
    }
    return new PartialPath(path);
  }

  private PartialPath parseSuffixPath(IoTDBSqlParser.SuffixPathContext ctx) {
    List<IoTDBSqlParser.NodeNameContext> nodeNames = ctx.nodeName();
    String[] path = new String[nodeNames.size()];
    for (int i = 0; i < nodeNames.size(); i++) {
      path[i] = parseNodeName(nodeNames.get(i));
    }
    return new PartialPath(path);
  }

  private PartialPath parseSuffixPathCanInExpr(IoTDBSqlParser.SuffixPathCanInExprContext ctx) {
    List<IoTDBSqlParser.NodeNameCanInExprContext> nodeNames = ctx.nodeNameCanInExpr();
    String[] path = new String[nodeNames.size()];
    for (int i = 0; i < nodeNames.size(); i++) {
      path[i] = parseNodeName(nodeNames.get(i).getText());
    }
    return new PartialPath(path);
  }

  private PartialPath convertConstantToPath(String src) throws IllegalPathException {
    return new PartialPath(src);
  }

  // node name

  /** function for parsing node name. */
  public String parseNodeName(IoTDBSqlParser.NodeNameContext ctx) {
    String src = ctx.getText();
    if (2 <= src.length() && src.charAt(0) == '`' && src.charAt(src.length() - 1) == '`') {
      return src.substring(1, src.length() - 1);
    }
    return src;
  }

  /** function for parsing datetime literal. */
  public long parseDateFormat(String timestampStr) throws SQLParserException {
    if (timestampStr == null || "".equals(timestampStr.trim())) {
      throw new SQLParserException("input timestamp cannot be empty");
    }
    if (timestampStr.equalsIgnoreCase(SQLConstant.NOW_FUNC)) {
      return DatetimeUtils.currentTime();
    }
    try {
      return DatetimeUtils.convertDatetimeStrToLong(timestampStr, zoneId);
    } catch (Exception e) {
      throw new SQLParserException(
          String.format(
              "Input time format %s error. "
                  + "Input like yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ss or "
                  + "refer to user document for more info.",
              timestampStr));
    }
  }

  public long parseDateFormat(String timestampStr, long currentTime) throws SQLParserException {
    if (timestampStr == null || "".equals(timestampStr.trim())) {
      throw new SQLParserException("input timestamp cannot be empty");
    }
    if (timestampStr.equalsIgnoreCase(SQLConstant.NOW_FUNC)) {
      return currentTime;
    }
    try {
      return DatetimeUtils.convertDatetimeStrToLong(timestampStr, zoneId);
    } catch (Exception e) {
      throw new SQLParserException(
          String.format(
              "Input time format %s error. "
                  + "Input like yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ss or "
                  + "refer to user document for more info.",
              timestampStr));
    }
  }

  // Expression & Predicate

  /**
   * parse time expression, which is addition and subtraction expression of duration time, now() or
   * DataTimeFormat time.
   *
   * <p>eg. now() + 1d - 2h
   */
  private Long parseDateExpression(IoTDBSqlParser.DateExpressionContext ctx) {
    long time;
    time = parseDateFormat(ctx.getChild(0).getText());
    for (int i = 1; i < ctx.getChildCount(); i = i + 2) {
      if ("+".equals(ctx.getChild(i).getText())) {
        time += DatetimeUtils.convertDurationStrToLong(time, ctx.getChild(i + 1).getText());
      } else {
        time -= DatetimeUtils.convertDurationStrToLong(time, ctx.getChild(i + 1).getText());
      }
    }
    return time;
  }

  private Long parseDateExpression(IoTDBSqlParser.DateExpressionContext ctx, long currentTime) {
    long time;
    time = parseDateFormat(ctx.getChild(0).getText(), currentTime);
    for (int i = 1; i < ctx.getChildCount(); i = i + 2) {
      if ("+".equals(ctx.getChild(i).getText())) {
        time += DatetimeUtils.convertDurationStrToLong(time, ctx.getChild(i + 1).getText());
      } else {
        time -= DatetimeUtils.convertDurationStrToLong(time, ctx.getChild(i + 1).getText());
      }
    }
    return time;
  }

  private long parseTimeValue(IoTDBSqlParser.TimeValueContext ctx, long currentTime) {
    if (ctx.INTEGER_LITERAL() != null) {
      return Long.parseLong(ctx.INTEGER_LITERAL().getText());
    } else if (ctx.dateExpression() != null) {
      return parseDateExpression(ctx.dateExpression(), currentTime);
    } else {
      return parseDateFormat(ctx.datetimeLiteral().getText(), currentTime);
    }
  }

  private Expression parseExpression(IoTDBSqlParser.ExpressionContext context) {
    if (context.unaryInBracket != null) {
      return parseExpression(context.unaryInBracket);
    }

    if (context.constant() != null && !context.constant().isEmpty()) {
      return parseConstantOperand(context.constant(0));
    }

    if (context.time != null) {
      throw new UnsupportedOperationException();
    }

    if (context.suffixPathCanInExpr() != null) {
      return new TimeSeriesOperand(parseSuffixPathCanInExpr(context.suffixPathCanInExpr()));
    }

    if (context.functionName() != null) {
      return parseFunctionExpression(context);
    }

    if (context.expressionAfterUnaryOperator != null) {
      if (context.MINUS() != null) {
        return new NegationExpression(parseExpression(context.expressionAfterUnaryOperator));
      }
      if (context.OPERATOR_NOT() != null) {
        return new LogicNotExpression(parseExpression(context.expressionAfterUnaryOperator));
      }
      return parseExpression(context.expressionAfterUnaryOperator);
    }

    if (context.leftExpression != null && context.rightExpression != null) {
      Expression leftExpression = parseExpression(context.leftExpression);
      Expression rightExpression = parseExpression(context.rightExpression);
      if (context.STAR() != null) {
        return new MultiplicationExpression(leftExpression, rightExpression);
      }
      if (context.DIV() != null) {
        return new DivisionExpression(leftExpression, rightExpression);
      }
      if (context.MOD() != null) {
        return new ModuloExpression(leftExpression, rightExpression);
      }
      if (context.PLUS() != null) {
        return new AdditionExpression(leftExpression, rightExpression);
      }
      if (context.MINUS() != null) {
        return new SubtractionExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_GT() != null) {
        return new GreaterThanExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_GTE() != null) {
        return new GreaterEqualExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_LT() != null) {
        return new LessThanExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_LTE() != null) {
        return new LessEqualExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_DEQ() != null) {
        return new EqualToExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_NEQ() != null) {
        return new NonEqualExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_AND() != null) {
        return new LogicAndExpression(leftExpression, rightExpression);
      }
      if (context.OPERATOR_OR() != null) {
        return new LogicOrExpression(leftExpression, rightExpression);
      }
      throw new UnsupportedOperationException();
    }

    if (context.unaryBeforeRegularOrLikeExpression != null) {
      if (context.REGEXP() != null) {
        return parseRegularExpression(context);
      }
      if (context.LIKE() != null) {
        return parseLikeExpression(context);
      }
      throw new UnsupportedOperationException();
    }

    if (context.unaryBeforeInExpression != null) {
      return parseInExpression(context);
    }

    throw new UnsupportedOperationException();
  }

  private Expression parseFunctionExpression(IoTDBSqlParser.ExpressionContext functionClause) {
    FunctionExpression functionExpression =
        new FunctionExpression(parseIdentifier(functionClause.functionName().getText()));

    // expressions
    boolean hasNonPureConstantSubExpression = false;
    for (IoTDBSqlParser.ExpressionContext expression : functionClause.expression()) {
      Expression subexpression = parseExpression(expression);
      if (!subexpression.isConstantOperand()) {
        hasNonPureConstantSubExpression = true;
      }
      functionExpression.addExpression(subexpression);
    }

    // It is not allowed to have function expressions like F(1, 1.0). There should be at least one
    // non-pure-constant sub-expression, otherwise the timestamp of the row cannot be inferred.
    if (!hasNonPureConstantSubExpression) {
      throw new SQLParserException(
          "Invalid function expression, all the arguments are constant operands: "
              + functionClause.getText());
    }

    // attributes
    for (IoTDBSqlParser.FunctionAttributeContext functionAttribute :
        functionClause.functionAttribute()) {
      functionExpression.addAttribute(
          parseStringLiteral(functionAttribute.functionAttributeKey.getText()),
          parseStringLiteral(functionAttribute.functionAttributeValue.getText()));
    }

    return functionExpression;
  }

  private Expression parseRegularExpression(ExpressionContext context) {
    return new RegularExpression(
        parseExpression(context.unaryBeforeRegularOrLikeExpression),
        parseStringLiteral(context.STRING_LITERAL().getText()));
  }

  private Expression parseLikeExpression(ExpressionContext context) {
    throw new UnsupportedOperationException();
  }

  private Expression parseInExpression(ExpressionContext context) {
    throw new UnsupportedOperationException();
  }

  private Expression parseConstantOperand(ConstantContext constantContext) {
    try {
      String text = constantContext.getText();
      if (clientVersion.equals(IoTDBConstant.ClientVersion.V_0_13)) {
        if (constantContext.BOOLEAN_LITERAL() != null) {
          return new ConstantOperand(TSDataType.BOOLEAN, text);
        } else if (constantContext.STRING_LITERAL() != null) {
          return new ConstantOperand(TSDataType.TEXT, parseStringLiteral(text));
        } else if (constantContext.INTEGER_LITERAL() != null) {
          return new ConstantOperand(TSDataType.INT64, text);
        } else if (constantContext.realLiteral() != null) {
          return new ConstantOperand(TSDataType.DOUBLE, text);
        } else {
          throw new SQLParserException("Unsupported constant operand: " + text);
        }
      } else if (clientVersion.equals(IoTDBConstant.ClientVersion.V_0_12)) {
        // if client version is before 0.13, node name in expression may be a constant
        return new TimeSeriesOperand(convertConstantToPath(text));
      } else {
        throw new UnsupportedOperationException();
      }
    } catch (IllegalPathException e) {
      throw new SQLParserException(e.getMessage());
    }
  }

  private FilterOperator parseOrExpression(IoTDBSqlParser.OrExpressionContext ctx) {
    if (ctx.andExpression().size() == 1) {
      return parseAndExpression(ctx.andExpression(0));
    }
    FilterOperator binaryOp = new FilterOperator(FilterType.KW_OR);
    if (ctx.andExpression().size() > 2) {
      binaryOp.addChildOperator(parseAndExpression(ctx.andExpression(0)));
      binaryOp.addChildOperator(parseAndExpression(ctx.andExpression(1)));
      for (int i = 2; i < ctx.andExpression().size(); i++) {
        FilterOperator op = new FilterOperator(FilterType.KW_OR);
        op.addChildOperator(binaryOp);
        op.addChildOperator(parseAndExpression(ctx.andExpression(i)));
        binaryOp = op;
      }
    } else {
      for (IoTDBSqlParser.AndExpressionContext andExpressionContext : ctx.andExpression()) {
        binaryOp.addChildOperator(parseAndExpression(andExpressionContext));
      }
    }
    return binaryOp;
  }

  private FilterOperator parseAndExpression(IoTDBSqlParser.AndExpressionContext ctx) {
    if (ctx.predicate().size() == 1) {
      return parsePredicate(ctx.predicate(0));
    }
    FilterOperator binaryOp = new FilterOperator(FilterType.KW_AND);
    int size = ctx.predicate().size();
    if (size > 2) {
      binaryOp.addChildOperator(parsePredicate(ctx.predicate(0)));
      binaryOp.addChildOperator(parsePredicate(ctx.predicate(1)));
      for (int i = 2; i < size; i++) {
        FilterOperator op = new FilterOperator(FilterType.KW_AND);
        op.addChildOperator(binaryOp);
        op.addChildOperator(parsePredicate(ctx.predicate(i)));
        binaryOp = op;
      }
    } else {
      for (IoTDBSqlParser.PredicateContext predicateContext : ctx.predicate()) {
        binaryOp.addChildOperator(parsePredicate(predicateContext));
      }
    }
    return binaryOp;
  }

  @SuppressWarnings("squid:S3776") // Suppress high Cognitive Complexity warning
  private FilterOperator parsePredicate(IoTDBSqlParser.PredicateContext ctx) {
    PartialPath path = null;
    if (ctx.OPERATOR_NOT() != null) {
      FilterOperator notOp = new FilterOperator(FilterType.KW_NOT);
      notOp.addChildOperator(parseOrExpression(ctx.orExpression()));
      return notOp;
    } else if (ctx.LR_BRACKET() != null && ctx.OPERATOR_NOT() == null) {
      return parseOrExpression(ctx.orExpression());
    } else if (ctx.REGEXP() != null || ctx.LIKE() != null) {
      if (ctx.suffixPath() != null) {
        path = parseSuffixPath(ctx.suffixPath());
      } else if (ctx.fullPath() != null) {
        path = parseFullPath(ctx.fullPath());
      }
      if (path == null) {
        throw new SQLParserException("Path is null, please check the sql.");
      }
      return ctx.REGEXP() != null
          ? new RegexpOperator(FilterType.REGEXP, path, ctx.STRING_LITERAL().getText())
          : new LikeOperator(FilterType.LIKE, path, ctx.STRING_LITERAL().getText());
    } else {
      if (ctx.TIME() != null || ctx.TIMESTAMP() != null) {
        path = new PartialPath(SQLConstant.getSingleTimeArray());
      }
      if (ctx.fullPath() != null) {
        path = parseFullPath(ctx.fullPath());
      }
      if (ctx.suffixPath() != null) {
        path = parseSuffixPath(ctx.suffixPath());
      }
      if (path == null) {
        throw new SQLParserException("Path is null, please check the sql.");
      }
      if (ctx.inClause() != null) {
        return parseInOperator(ctx.inClause(), path);
      } else {
        return parseBasicFunctionOperator(ctx, path);
      }
    }
  }

  private FilterOperator parseBasicFunctionOperator(
      IoTDBSqlParser.PredicateContext ctx, PartialPath path) {
    BasicFunctionOperator basic;
    if (ctx.constant().dateExpression() != null) {
      if (!path.equals(TIME_PATH)) {
        throw new SQLParserException(path.getFullPath(), "Date can only be used to time");
      }
      basic =
          new BasicFunctionOperator(
              FilterConstant.lexerToFilterType.get(ctx.comparisonOperator().type.getType()),
              path,
              Long.toString(parseDateExpression(ctx.constant().dateExpression())));
    } else {
      basic =
          new BasicFunctionOperator(
              FilterConstant.lexerToFilterType.get(ctx.comparisonOperator().type.getType()),
              path,
              parseStringLiteral(ctx.constant().getText()));
    }
    return basic;
  }

  private FilterOperator parseInOperator(IoTDBSqlParser.InClauseContext ctx, PartialPath path) {
    Set<String> values = new HashSet<>();
    boolean not = ctx.OPERATOR_NOT() != null;
    for (IoTDBSqlParser.ConstantContext constant : ctx.constant()) {
      if (constant.dateExpression() != null) {
        if (!path.equals(TIME_PATH)) {
          throw new SQLParserException(path.getFullPath(), "Date can only be used to time");
        }
        values.add(Long.toString(parseDateExpression(constant.dateExpression())));
      } else {
        values.add(constant.getText());
      }
    }
    return new InOperator(FilterType.IN, path, not, values);
  }

  private void parseIndexPredicate(IoTDBSqlParser.IndexPredicateClauseContext ctx) {
    Map<String, Object> props;
    PartialPath path;
    if (ctx.suffixPath() != null) {
      path = parseSuffixPath(ctx.suffixPath());
    } else {
      path = parseFullPath(ctx.fullPath());
    }
    if (ctx.LIKE() != null) {
      // whole matching case
      if (queryOp.getSelectComponent().getResultColumns().size() != 1) {
        throw new SQLParserException("Index query statement allows only one select path");
      }
      if (!path.equals(
          queryOp.getSelectComponent().getResultColumns().get(0).getExpression().toString())) {
        throw new SQLParserException(
            "In the index query statement, "
                + "the path in select element and the index predicate should be same");
      }
      if (queryOp.getProps() != null) {
        props = queryOp.getProps();
      } else {
        props = new HashMap<>();
      }
      props.put(PATTERN, parseSequence(ctx.sequenceClause(0)));
      queryOp.setIndexType(IndexType.RTREE_PAA);
    } else if (ctx.CONTAIN() != null) {
      // subsequence matching case
      List<double[]> compositePattern = new ArrayList<>();
      List<Double> thresholds = new ArrayList<>();
      for (int i = 0; i < ctx.sequenceClause().size(); i++) {
        compositePattern.add(parseSequence(ctx.sequenceClause(i)));
        thresholds.add(Double.parseDouble(ctx.constant(i).getText()));
      }
      if (queryOp.getProps() != null) {
        props = queryOp.getProps();
      } else {
        props = new HashMap<>();
      }
      List<ResultColumn> resultColumns = new ArrayList<>();
      resultColumns.add(new ResultColumn(new TimeSeriesOperand(path)));
      queryOp.getSelectComponent().setResultColumns(resultColumns);
      props.put(PATTERN, compositePattern);
      props.put(THRESHOLD, thresholds);
      queryOp.setIndexType(IndexType.ELB_INDEX);
    } else {
      throw new SQLParserException("Unknown index predicate: " + ctx);
    }
    queryOp.setProps(props);
  }

  private double[] parseSequence(IoTDBSqlParser.SequenceClauseContext ctx) {
    int seqLen = ctx.constant().size();
    double[] sequence = new double[seqLen];
    for (int i = 0; i < seqLen; i++) {
      sequence[i] = Double.parseDouble(ctx.constant(i).getText());
    }
    return sequence;
  }

  // Select Clause

  public void parseSelectClause(IoTDBSqlParser.SelectClauseContext ctx) {
    SelectComponent selectComponent = new SelectComponent(zoneId);
    if (ctx.topClause() != null) {
      // TODO: parse info of top clause into selectOp
      visitTopClause(ctx.topClause());
    } else if (ctx.LAST() != null) {
      queryOp = new LastQueryOperator(queryOp);
    }

    // add aliasSet
    Set<String> aliasSet = new HashSet<>();
    for (IoTDBSqlParser.ResultColumnContext resultColumnContext : ctx.resultColumn()) {
      ResultColumn resultColumn = parseResultColumn(resultColumnContext);
      if (resultColumn.hasAlias()) {
        aliasSet.add(resultColumn.getAlias());
      }
      selectComponent.addResultColumn(resultColumn);
    }

    // judge query type
    if (!hasDecidedQueryType()) {
      if (selectComponent.hasUserDefinedAggregationFunction()) {
        queryOp = new UDAFQueryOperator(new AggregationQueryOperator(queryOp));
      } else if (selectComponent.hasPlainAggregationFunction()) {
        queryOp = new AggregationQueryOperator(queryOp);
      } else if (selectComponent.hasTimeSeriesGeneratingFunction()) {
        queryOp = new UDTFQueryOperator(queryOp);
      }
    } else if (selectComponent.hasUserDefinedAggregationFunction()) {
      queryOp = new UDAFQueryOperator((AggregationQueryOperator) (queryOp));
    }
    queryOp.setAliasSet(aliasSet);
    queryOp.setSelectComponent(selectComponent);
  }

  @Override
  public Operator visitTopClause(IoTDBSqlParser.TopClauseContext ctx) {
    Map<String, Object> props = new HashMap<>();
    int top = Integer.parseInt(ctx.INTEGER_LITERAL().getText());
    if (top <= 0 || top > 1000) {
      throw new SQLParserException(
          String.format(
              "TOP <N>: N should be greater than 0 and less than 1000, current N is %d", top));
    }
    props.put(TOP_K, top);
    queryOp.setProps(props);
    return queryOp;
  }

  private ResultColumn parseResultColumn(IoTDBSqlParser.ResultColumnContext resultColumnContext) {
    Expression expression = parseExpression(resultColumnContext.expression());
    if (expression.isConstantOperand()) {
      throw new SQLParserException("Constant operand is not allowed: " + expression);
    }
    return new ResultColumn(
        expression,
        resultColumnContext.AS() == null
            ? null
            : parseIdentifier(resultColumnContext.identifier().getText()));
  }

  // From Clause

  public void parseFromClause(IoTDBSqlParser.FromClauseContext ctx) {
    FromComponent fromComponent = new FromComponent();
    List<IoTDBSqlParser.PrefixPathContext> prefixFromPaths = ctx.prefixPath();
    for (IoTDBSqlParser.PrefixPathContext prefixFromPath : prefixFromPaths) {
      PartialPath path = parsePrefixPath(prefixFromPath);
      fromComponent.addPrefixTablePath(path);
    }
    queryOp.setFromComponent(fromComponent);
  }

  // Where Clause

  public WhereComponent parseWhereClause(IoTDBSqlParser.WhereClauseContext ctx) {
    if (ctx.indexPredicateClause() != null) {
      parseIndexPredicate(ctx.indexPredicateClause());
      return null;
    }
    FilterOperator whereOp = new FilterOperator();
    whereOp.addChildOperator(parseOrExpression(ctx.orExpression()));
    return new WhereComponent(whereOp.getChildren().get(0));
  }

  // Tag & Property & Attribute

  public void parseTagClause(IoTDBSqlParser.TagClauseContext ctx, Operator operator) {
    Map<String, String> tags = extractMap(ctx.propertyClause(), ctx.propertyClause(0));
    if (operator instanceof CreateTimeSeriesOperator) {
      ((CreateTimeSeriesOperator) operator).setTags(tags);
    } else if (operator instanceof CreateAlignedTimeSeriesOperator) {
      ((CreateAlignedTimeSeriesOperator) operator).addTagsList(tags);
    } else if (operator instanceof AlterTimeSeriesOperator) {
      ((AlterTimeSeriesOperator) operator).setTagsMap(tags);
    }
  }

  public void parseAttributeClause(IoTDBSqlParser.AttributeClauseContext ctx, Operator operator) {
    Map<String, String> attributes = extractMap(ctx.propertyClause(), ctx.propertyClause(0));
    if (operator instanceof CreateTimeSeriesOperator) {
      ((CreateTimeSeriesOperator) operator).setAttributes(attributes);
    } else if (operator instanceof CreateAlignedTimeSeriesOperator) {
      ((CreateAlignedTimeSeriesOperator) operator).addAttributesList(attributes);
    } else if (operator instanceof AlterTimeSeriesOperator) {
      ((AlterTimeSeriesOperator) operator).setAttributesMap(attributes);
    }
  }

  // Limit & Offset Clause

  private void parseLimitClause(IoTDBSqlParser.LimitClauseContext ctx, Operator operator) {
    int limit;
    try {
      limit = Integer.parseInt(ctx.INTEGER_LITERAL().getText());
    } catch (NumberFormatException e) {
      throw new SQLParserException("Out of range. LIMIT <N>: N should be Int32.");
    }
    if (limit <= 0) {
      throw new SQLParserException("LIMIT <N>: N should be greater than 0.");
    }
    if (operator instanceof ShowTimeSeriesOperator) {
      ((ShowTimeSeriesOperator) operator).setLimit(limit);
    } else if (operator instanceof ShowDevicesOperator) {
      ((ShowDevicesOperator) operator).setLimit(limit);
    } else {
      SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
      if (specialClauseComponent == null) {
        specialClauseComponent = new SpecialClauseComponent();
      }
      specialClauseComponent.setRowLimit(limit);
      queryOp.setSpecialClauseComponent(specialClauseComponent);
    }
    if (ctx.offsetClause() != null) {
      parseOffsetClause(ctx.offsetClause(), operator);
    }
  }

  private void parseOffsetClause(IoTDBSqlParser.OffsetClauseContext ctx, Operator operator) {
    int offset;
    try {
      offset = Integer.parseInt(ctx.INTEGER_LITERAL().getText());
    } catch (NumberFormatException e) {
      throw new SQLParserException(
          "Out of range. OFFSET <OFFSETValue>: OFFSETValue should be Int32.");
    }
    if (offset < 0) {
      throw new SQLParserException("OFFSET <OFFSETValue>: OFFSETValue should >= 0.");
    }
    if (operator instanceof ShowTimeSeriesOperator) {
      ((ShowTimeSeriesOperator) operator).setOffset(offset);
    } else if (operator instanceof ShowDevicesOperator) {
      ((ShowDevicesOperator) operator).setOffset(offset);
    } else {
      SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
      if (specialClauseComponent == null) {
        specialClauseComponent = new SpecialClauseComponent();
      }
      specialClauseComponent.setRowOffset(offset);
      queryOp.setSpecialClauseComponent(specialClauseComponent);
    }
  }

  private void parseSlimitClause(IoTDBSqlParser.SlimitClauseContext ctx) {
    int slimit;
    try {
      slimit = Integer.parseInt(ctx.INTEGER_LITERAL().getText());
    } catch (NumberFormatException e) {
      throw new SQLParserException("Out of range. SLIMIT <SN>: SN should be Int32.");
    }
    if (slimit <= 0) {
      throw new SQLParserException("SLIMIT <SN>: SN should be greater than 0.");
    }
    SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
    if (specialClauseComponent == null) {
      specialClauseComponent = new SpecialClauseComponent();
    }
    specialClauseComponent.setSeriesLimit(slimit);
    queryOp.setSpecialClauseComponent(specialClauseComponent);
    if (ctx.soffsetClause() != null) {
      parseSoffsetClause(ctx.soffsetClause(), queryOp);
    }
  }

  public void parseSoffsetClause(IoTDBSqlParser.SoffsetClauseContext ctx, QueryOperator queryOp) {
    int soffset;
    try {
      soffset = Integer.parseInt(ctx.INTEGER_LITERAL().getText());
    } catch (NumberFormatException e) {
      throw new SQLParserException(
          "Out of range. SOFFSET <SOFFSETValue>: SOFFSETValue should be Int32.");
    }
    if (soffset < 0) {
      throw new SQLParserException("SOFFSET <SOFFSETValue>: SOFFSETValue should >= 0.");
    }
    SpecialClauseComponent specialClauseComponent = queryOp.getSpecialClauseComponent();
    if (specialClauseComponent == null) {
      specialClauseComponent = new SpecialClauseComponent();
    }
    specialClauseComponent.setSeriesOffset(soffset);
    queryOp.setSpecialClauseComponent(specialClauseComponent);
  }

  /** Utils */
  private boolean hasDecidedQueryType() {
    return queryOp instanceof GroupByQueryOperator
        || queryOp instanceof FillQueryOperator
        || queryOp instanceof LastQueryOperator
        || queryOp instanceof AggregationQueryOperator
        || queryOp instanceof UDTFQueryOperator
        || queryOp instanceof UDAFQueryOperator;
  }

  private String parseStringLiteral(String src) {
    if (2 <= src.length()) {
      if ((src.charAt(0) == '\"' && src.charAt(src.length() - 1) == '\"')
          || (src.charAt(0) == '\'' && src.charAt(src.length() - 1) == '\'')) {
        String unescapeString = StringEscapeUtils.unescapeJava(src.substring(1, src.length() - 1));
        return unescapeString.length() == 0 ? "" : unescapeString;
      }
    }
    return src;
  }

  private String parseStringLiteralInInsertValue(String src) {
    if (2 <= src.length()) {
      if ((src.charAt(0) == '\"' && src.charAt(src.length() - 1) == '\"')
          || (src.charAt(0) == '\'' && src.charAt(src.length() - 1) == '\'')) {
        return StringEscapeUtils.unescapeJava(src);
      }
    }
    return src;
  }

  private String parseIdentifier(String src) {
    if (2 <= src.length() && src.charAt(0) == '`' && src.charAt(src.length() - 1) == '`') {
      return StringEscapeUtils.unescapeJava(src.substring(1, src.length() - 1));
    }
    return src;
  }

  private String parseNodeName(String src) {
    if (2 <= src.length() && src.charAt(0) == '`' && src.charAt(src.length() - 1) == '`') {
      return src.substring(1, src.length() - 1);
    }
    return src;
  }

  /** function for parsing file path used by LOAD statement. */
  public String parseFilePath(String src) {
    return src.substring(1, src.length() - 1);
  }

  /**
   * parse time unit or sliding step in group by query.
   *
   * @param durationStr represent duration string like: 12d8m9ns, 1y1d, etc.
   * @return time in milliseconds, microseconds, or nanoseconds depending on the profile
   */
  private long parseTimeUnitOrSlidingStep(
      String durationStr, boolean isParsingTimeUnit, GroupByClauseComponent groupByComponent) {
    if (durationStr.toLowerCase().contains("mo")) {
      if (isParsingTimeUnit) {
        groupByComponent.setIntervalByMonth(true);
      } else {
        groupByComponent.setSlidingStepByMonth(true);
      }
    }
    return DatetimeUtils.convertDurationStrToLong(durationStr);
  }

  private void setMap(IoTDBSqlParser.AlterClauseContext ctx, Map<String, String> alterMap) {
    List<IoTDBSqlParser.PropertyClauseContext> tagsList = ctx.propertyClause();
    if (ctx.propertyClause(0) != null) {
      for (IoTDBSqlParser.PropertyClauseContext property : tagsList) {
        String value;
        value = parseStringLiteral(property.propertyValue().getText());
        alterMap.put(parseIdentifier(property.identifier().getText()), value);
      }
    }
  }

  private Map<String, String> extractMap(
      List<IoTDBSqlParser.PropertyClauseContext> property2,
      IoTDBSqlParser.PropertyClauseContext property3) {
    Map<String, String> tags = new HashMap<>(property2.size());
    if (property3 != null) {
      for (IoTDBSqlParser.PropertyClauseContext property : property2) {
        tags.put(
            parseIdentifier(property.identifier().getText()),
            parseStringLiteral(property.propertyValue().getText()));
      }
    }
    return tags;
  }

  private Pair<Long, Long> calcOperatorInterval(FilterOperator filterOperator) {

    if (filterOperator.getSinglePath() != null
        && !IoTDBConstant.TIME.equals(filterOperator.getSinglePath().getMeasurement())) {
      throw new SQLParserException(DELETE_ONLY_SUPPORT_TIME_EXP_ERROR_MSG);
    }

    long time = Long.parseLong(((BasicFunctionOperator) filterOperator).getValue());
    switch (filterOperator.getFilterType()) {
      case LESSTHAN:
        return new Pair<>(Long.MIN_VALUE, time - 1);
      case LESSTHANOREQUALTO:
        return new Pair<>(Long.MIN_VALUE, time);
      case GREATERTHAN:
        return new Pair<>(time + 1, Long.MAX_VALUE);
      case GREATERTHANOREQUALTO:
        return new Pair<>(time, Long.MAX_VALUE);
      case EQUAL:
        return new Pair<>(time, time);
      default:
        throw new SQLParserException(DELETE_RANGE_ERROR_MSG);
    }
  }
}
