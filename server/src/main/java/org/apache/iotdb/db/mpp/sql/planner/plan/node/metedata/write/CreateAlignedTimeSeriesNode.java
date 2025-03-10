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

package org.apache.iotdb.db.mpp.sql.planner.plan.node.metedata.write;

import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.mpp.common.header.ColumnHeader;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNode;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeId;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanNodeType;
import org.apache.iotdb.db.mpp.sql.planner.plan.node.PlanVisitor;
import org.apache.iotdb.tsfile.exception.NotImplementedException;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.apache.iotdb.tsfile.utils.ReadWriteIOUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CreateAlignedTimeSeriesNode extends PlanNode {
  private PartialPath devicePath;
  private List<String> measurements;
  private List<TSDataType> dataTypes;
  private List<TSEncoding> encodings;
  private List<CompressionType> compressors;
  private List<String> aliasList;
  private List<Map<String, String>> tagsList;
  private List<Map<String, String>> attributesList;
  private List<Long> tagOffsets;

  public CreateAlignedTimeSeriesNode(
      PlanNodeId id,
      PartialPath devicePath,
      List<String> measurements,
      List<TSDataType> dataTypes,
      List<TSEncoding> encodings,
      List<CompressionType> compressors,
      List<String> aliasList,
      List<Map<String, String>> tagsList,
      List<Map<String, String>> attributesList) {
    super(id);
    this.devicePath = devicePath;
    this.measurements = measurements;
    this.dataTypes = dataTypes;
    this.encodings = encodings;
    this.compressors = compressors;
    this.aliasList = aliasList;
    this.tagsList = tagsList;
    this.attributesList = attributesList;
  }

  public PartialPath getDevicePath() {
    return devicePath;
  }

  public void setDevicePath(PartialPath devicePath) {
    this.devicePath = devicePath;
  }

  public List<String> getMeasurements() {
    return measurements;
  }

  public void setMeasurements(List<String> measurements) {
    this.measurements = measurements;
  }

  public List<TSDataType> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<TSDataType> dataTypes) {
    this.dataTypes = dataTypes;
  }

  public List<TSEncoding> getEncodings() {
    return encodings;
  }

  public void setEncodings(List<TSEncoding> encodings) {
    this.encodings = encodings;
  }

  public List<CompressionType> getCompressors() {
    return compressors;
  }

  public void setCompressors(List<CompressionType> compressors) {
    this.compressors = compressors;
  }

  public List<String> getAliasList() {
    return aliasList;
  }

  public void setAliasList(List<String> aliasList) {
    this.aliasList = aliasList;
  }

  public List<Map<String, String>> getTagsList() {
    return tagsList;
  }

  public void setTagsList(List<Map<String, String>> tagsList) {
    this.tagsList = tagsList;
  }

  public List<Map<String, String>> getAttributesList() {
    return attributesList;
  }

  public void setAttributesList(List<Map<String, String>> attributesList) {
    this.attributesList = attributesList;
  }

  public List<Long> getTagOffsets() {
    return tagOffsets;
  }

  public void setTagOffsets(List<Long> tagOffsets) {
    this.tagOffsets = tagOffsets;
  }

  @Override
  public List<PlanNode> getChildren() {
    return null;
  }

  @Override
  public void addChild(PlanNode child) {}

  @Override
  public PlanNode clone() {
    throw new NotImplementedException("Clone of CreateAlignedTimeSeriesNode is not implemented");
  }

  @Override
  public int allowedChildCount() {
    return NO_CHILD_ALLOWED;
  }

  @Override
  public List<ColumnHeader> getOutputColumnHeaders() {
    return null;
  }

  @Override
  public List<String> getOutputColumnNames() {
    return null;
  }

  @Override
  public List<TSDataType> getOutputColumnTypes() {
    return null;
  }

  @Override
  public <R, C> R accept(PlanVisitor<R, C> visitor, C schemaRegion) {
    return visitor.visitCreateAlignedTimeSeries(this, schemaRegion);
  }

  public static CreateAlignedTimeSeriesNode deserialize(ByteBuffer byteBuffer) {
    String id;
    PartialPath devicePath = null;
    List<String> measurements;
    List<TSDataType> dataTypes;
    List<TSEncoding> encodings;
    List<CompressionType> compressors;
    List<String> aliasList = null;
    List<Map<String, String>> tagsList = null;
    List<Map<String, String>> attributesList = null;

    int length = byteBuffer.getInt();
    byte[] bytes = new byte[length];
    byteBuffer.get(bytes);
    try {
      devicePath = new PartialPath(new String(bytes));
    } catch (IllegalPathException e) {
      throw new IllegalArgumentException("Can not deserialize CreateAlignedTimeSeriesNode", e);
    }

    measurements = new ArrayList<>();
    int size = byteBuffer.getInt();
    for (int i = 0; i < size; i++) {
      measurements.add(ReadWriteIOUtils.readString(byteBuffer));
    }

    dataTypes = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      dataTypes.add(TSDataType.values()[byteBuffer.get()]);
    }

    encodings = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      encodings.add(TSEncoding.values()[byteBuffer.get()]);
    }

    compressors = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      compressors.add(CompressionType.values()[byteBuffer.get()]);
    }

    byte label = byteBuffer.get();
    if (label >= 0) {
      aliasList = new ArrayList<>();
      if (label == 1) {
        for (int i = 0; i < size; i++) {
          aliasList.add(ReadWriteIOUtils.readString(byteBuffer));
        }
      }
    }

    label = byteBuffer.get();
    if (label >= 0) {
      tagsList = new ArrayList<>();
      if (label == 1) {
        for (int i = 0; i < size; i++) {
          tagsList.add(ReadWriteIOUtils.readMap(byteBuffer));
        }
      }
    }

    label = byteBuffer.get();
    if (label >= 0) {
      attributesList = new ArrayList<>();
      if (label == 1) {
        for (int i = 0; i < size; i++) {
          attributesList.add(ReadWriteIOUtils.readMap(byteBuffer));
        }
      }
    }

    id = ReadWriteIOUtils.readString(byteBuffer);

    return new CreateAlignedTimeSeriesNode(
        new PlanNodeId(id),
        devicePath,
        measurements,
        dataTypes,
        encodings,
        compressors,
        aliasList,
        tagsList,
        attributesList);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateAlignedTimeSeriesNode that = (CreateAlignedTimeSeriesNode) o;
    return this.getPlanNodeId().equals(that.getPlanNodeId())
        && Objects.equals(devicePath, that.devicePath)
        && Objects.equals(measurements, that.measurements)
        && Objects.equals(dataTypes, that.dataTypes)
        && Objects.equals(encodings, that.encodings)
        && Objects.equals(compressors, that.compressors)
        && Objects.equals(tagOffsets, that.tagOffsets)
        && Objects.equals(aliasList, that.aliasList)
        && Objects.equals(tagsList, that.tagsList)
        && Objects.equals(attributesList, that.attributesList);
  }

  @Override
  protected void serializeAttributes(ByteBuffer byteBuffer) {
    PlanNodeType.CREATE_ALIGNED_TIME_SERIES.serialize(byteBuffer);
    byte[] bytes = devicePath.getFullPath().getBytes();
    byteBuffer.putInt(bytes.length);
    byteBuffer.put(bytes);

    // measurements
    byteBuffer.putInt(measurements.size());
    for (String measurement : measurements) {
      ReadWriteIOUtils.write(measurement, byteBuffer);
    }

    // dataTypes
    for (TSDataType dataType : dataTypes) {
      byteBuffer.put((byte) dataType.ordinal());
    }

    // encodings
    for (TSEncoding encoding : encodings) {
      byteBuffer.put((byte) encoding.ordinal());
    }

    // compressors
    for (CompressionType compressor : compressors) {
      byteBuffer.put((byte) compressor.ordinal());
    }

    // alias
    if (aliasList == null) {
      byteBuffer.put((byte) -1);
    } else if (aliasList.isEmpty()) {
      byteBuffer.put((byte) 0);
    } else {
      byteBuffer.put((byte) 1);
      for (String alias : aliasList) {
        ReadWriteIOUtils.write(alias, byteBuffer);
      }
    }

    // tags
    if (tagsList == null) {
      byteBuffer.put((byte) -1);
    } else if (tagsList.isEmpty()) {
      byteBuffer.put((byte) 0);
    } else {
      byteBuffer.put((byte) 1);
      for (Map<String, String> tags : tagsList) {
        ReadWriteIOUtils.write(tags, byteBuffer);
      }
    }

    // attributes
    if (attributesList == null) {
      byteBuffer.put((byte) -1);
    } else if (attributesList.isEmpty()) {
      byteBuffer.put((byte) 0);
    } else {
      byteBuffer.put((byte) 1);
      for (Map<String, String> attributes : attributesList) {
        ReadWriteIOUtils.write(attributes, byteBuffer);
      }
    }
  }

  public int hashCode() {
    return Objects.hash(
        this.getPlanNodeId(),
        devicePath,
        measurements,
        dataTypes,
        encodings,
        compressors,
        tagOffsets,
        aliasList,
        tagsList,
        attributesList);
  }

  //  @Override
  //  public void executeOn(SchemaRegion schemaRegion) throws MetadataException {
  //    schemaRegion.createAlignedTimeSeries((CreateAlignedTimeSeriesPlan)
  // transferToPhysicalPlan());
  //  }

}
