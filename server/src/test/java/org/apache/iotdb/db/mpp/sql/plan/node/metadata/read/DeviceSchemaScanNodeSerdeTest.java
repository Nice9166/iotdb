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
package org.apache.iotdb.db.mpp.sql.plan.node.metadata.read;

import org.apache.iotdb.common.rpc.thrift.TEndPoint;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.common.FragmentInstanceId;
import org.apache.iotdb.db.mpp.common.PlanFragmentId;
import org.apache.iotdb.db.mpp.sql.plan.node.PlanNodeDeserializeHelper;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.read.DevicesSchemaScanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.read.SeriesSchemaMergeNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.process.ExchangeNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.process.LimitNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.process.OffsetNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.sink.FragmentSinkNode;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class DeviceSchemaScanNodeSerdeTest {

  @Test
  public void testSerializeAndDeserialize() throws IllegalPathException {
    OffsetNode offsetNode = new OffsetNode(new PlanNodeId("offset"), 10);
    LimitNode limitNode = new LimitNode(new PlanNodeId("limit"), 10);
    SeriesSchemaMergeNode schemaMergeNode =
        new SeriesSchemaMergeNode(new PlanNodeId("schemaMerge"));
    ExchangeNode exchangeNode = new ExchangeNode(new PlanNodeId("exchange"));
    DevicesSchemaScanNode devicesSchemaScanNode =
        new DevicesSchemaScanNode(
            new PlanNodeId("deviceSchemaScan"),
            new PartialPath("root.sg.device"),
            10,
            10,
            false,
            false);
    FragmentSinkNode fragmentSinkNode = new FragmentSinkNode(new PlanNodeId("fragmentSink"));
    fragmentSinkNode.addChild(devicesSchemaScanNode);
    fragmentSinkNode.setDownStream(
        new TEndPoint("127.0.0.1", 6667),
        new FragmentInstanceId(new PlanFragmentId("q", 1), "ds"),
        new PlanNodeId("test"));
    exchangeNode.addChild(schemaMergeNode);
    exchangeNode.setRemoteSourceNode(fragmentSinkNode);
    exchangeNode.setUpstream(
        new TEndPoint("127.0.0.1", 6667),
        new FragmentInstanceId(new PlanFragmentId("q", 1), "ds"),
        new PlanNodeId("test"));
    offsetNode.addChild(exchangeNode);
    limitNode.addChild(offsetNode);
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    limitNode.serialize(byteBuffer);
    byteBuffer.flip();
    LimitNode limitNode1 = (LimitNode) PlanNodeDeserializeHelper.deserialize(byteBuffer);
    Assert.assertEquals(limitNode, limitNode1);
  }
}
