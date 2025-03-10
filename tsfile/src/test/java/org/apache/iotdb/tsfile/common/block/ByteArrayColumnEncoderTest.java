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

package org.apache.iotdb.tsfile.common.block;

import org.apache.iotdb.tsfile.read.common.block.column.BooleanColumn;
import org.apache.iotdb.tsfile.read.common.block.column.BooleanColumnBuilder;
import org.apache.iotdb.tsfile.read.common.block.column.ColumnEncoder;
import org.apache.iotdb.tsfile.read.common.block.column.ColumnEncoderFactory;
import org.apache.iotdb.tsfile.read.common.block.column.ColumnEncoding;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;

public class ByteArrayColumnEncoderTest {
  @Test
  public void testBooleanColumn() {
    final int positionCount = 10;

    Random random = new Random();

    boolean[] nullIndicators = new boolean[positionCount];
    boolean[] values = new boolean[positionCount];
    for (int i = 0; i < positionCount; i++) {
      nullIndicators[i] = i % 2 == 0;
      if (i % 2 != 0) {
        values[i] = random.nextBoolean();
      }
    }
    BooleanColumn input = new BooleanColumn(positionCount, Optional.of(nullIndicators), values);
    ColumnEncoder encoder = ColumnEncoderFactory.get(ColumnEncoding.BYTE_ARRAY);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(byteArrayOutputStream);
    try {
      encoder.writeColumn(dos, input);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    ByteBuffer buffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    BooleanColumnBuilder booleanColumnBuilder = new BooleanColumnBuilder(null, positionCount);
    encoder.readColumn(booleanColumnBuilder, buffer, positionCount);
    BooleanColumn output = (BooleanColumn) booleanColumnBuilder.build();
    Assert.assertEquals(positionCount, output.getPositionCount());
    Assert.assertTrue(output.mayHaveNull());
    for (int i = 0; i < positionCount; i++) {
      Assert.assertEquals(i % 2 == 0, output.isNull(i));
      if (i % 2 != 0) {
        Assert.assertEquals(values[i], output.getBoolean(i));
      }
    }
  }
}
