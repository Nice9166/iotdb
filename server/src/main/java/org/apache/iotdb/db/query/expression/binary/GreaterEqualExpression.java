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

package org.apache.iotdb.db.query.expression.binary;

import org.apache.iotdb.db.query.expression.Expression;
import org.apache.iotdb.db.query.expression.ExpressionType;
import org.apache.iotdb.db.query.udf.core.reader.LayerPointReader;
import org.apache.iotdb.db.query.udf.core.transformer.CompareBinaryTransformer;
import org.apache.iotdb.db.query.udf.core.transformer.CompareGreaterEqualTransformer;

import java.nio.ByteBuffer;

public class GreaterEqualExpression extends BinaryExpression {

  public GreaterEqualExpression(Expression leftExpression, Expression rightExpression) {
    super(leftExpression, rightExpression);
  }

  public GreaterEqualExpression(ByteBuffer byteBuffer) {
    super(byteBuffer);
  }

  @Override
  protected CompareBinaryTransformer constructTransformer(
      LayerPointReader leftParentLayerPointReader, LayerPointReader rightParentLayerPointReader) {
    return new CompareGreaterEqualTransformer(
        leftParentLayerPointReader, rightParentLayerPointReader);
  }

  @Override
  protected String operator() {
    return ">=";
  }

  @Override
  public ExpressionType getExpressionType() {
    return ExpressionType.GREATER_EQUAL;
  }
}
