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

package org.apache.iotdb.db.query.udf.api.customizer.parameter;

import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.query.expression.Expression;
import org.apache.iotdb.db.query.expression.multi.FunctionExpression;
import org.apache.iotdb.db.query.udf.api.UDTF;
import org.apache.iotdb.db.query.udf.api.customizer.config.UDTFConfigurations;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used in {@link UDTF#beforeStart(UDFParameters, UDTFConfigurations)}.
 *
 * <p>This class is used to parse the parameters in the UDF entered by the user.
 *
 * <p>The input parameters of UDF have two parts. The first part is the paths (measurements) of the
 * time series that the UDF needs to process, and the second part is the key-value pair attributes
 * for customization. Only the second part can be empty.
 *
 * <p>Note that the user must enter the paths (measurements) part before entering the attributes
 * part.
 */
public class UDFParameters {

  private final List<Expression> expressions;
  private final List<PartialPath> paths;
  private final Map<String, String> attributes;
  private final List<TSDataType> dataTypes;

  public UDFParameters(
      FunctionExpression functionExpression, Map<Expression, TSDataType> expressionDataTypeMap)
      throws QueryProcessException {
    expressions = functionExpression.getExpressions();
    paths = functionExpression.getPaths();
    attributes = functionExpression.getFunctionAttributes();
    dataTypes = new ArrayList<>();
    for (Expression expression : expressions) {
      dataTypes.add(expressionDataTypeMap.get(expression));
    }
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  public List<PartialPath> getPaths() {
    return paths;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public List<TSDataType> getDataTypes() throws MetadataException {
    return dataTypes;
  }

  public PartialPath getPath(int index) {
    return paths.get(index);
  }

  public TSDataType getDataType(int index) throws MetadataException {
    return dataTypes.get(index);
  }

  public boolean hasAttribute(String attributeKey) {
    return attributes.containsKey(attributeKey);
  }

  public String getString(String key) {
    return attributes.get(key);
  }

  public Boolean getBoolean(String key) {
    String value = attributes.get(key);
    return value == null ? null : Boolean.parseBoolean(value);
  }

  public Integer getInt(String key) {
    String value = attributes.get(key);
    return value == null ? null : Integer.parseInt(value);
  }

  public Long getLong(String key) {
    String value = attributes.get(key);
    return value == null ? null : Long.parseLong(value);
  }

  public Float getFloat(String key) {
    String value = attributes.get(key);
    return value == null ? null : Float.parseFloat(value);
  }

  public Double getDouble(String key) {
    String value = attributes.get(key);
    return value == null ? null : Double.parseDouble(value);
  }

  public String getStringOrDefault(String key, String defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : value;
  }

  public boolean getBooleanOrDefault(String key, boolean defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : Boolean.parseBoolean(value);
  }

  public int getIntOrDefault(String key, int defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : Integer.parseInt(value);
  }

  public long getLongOrDefault(String key, long defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : Long.parseLong(value);
  }

  public float getFloatOrDefault(String key, float defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : Float.parseFloat(value);
  }

  public double getDoubleOrDefault(String key, double defaultValue) {
    String value = attributes.get(key);
    return value == null ? defaultValue : Double.parseDouble(value);
  }
}
