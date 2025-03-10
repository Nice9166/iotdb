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
package org.apache.iotdb.db.mpp.schedule;

import org.apache.iotdb.db.mpp.schedule.task.DriverTask;
import org.apache.iotdb.db.utils.stats.CpuTimer;

import io.airlift.units.Duration;

/** The execution context of a {@link DriverTask} */
public class ExecutionContext {
  private CpuTimer.CpuDuration cpuDuration;
  private Duration timeSlice;

  public CpuTimer.CpuDuration getCpuDuration() {
    return cpuDuration;
  }

  public void setCpuDuration(CpuTimer.CpuDuration cpuDuration) {
    this.cpuDuration = cpuDuration;
  }

  public Duration getTimeSlice() {
    return timeSlice;
  }

  public void setTimeSlice(Duration timeSlice) {
    this.timeSlice = timeSlice;
  }
}
