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
package org.apache.iotdb.consensus.ratis;

import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.server.storage.RaftStorageDirectory;
import org.apache.ratis.server.storage.RaftStorageMetadataFile;
import org.apache.ratis.statemachine.SnapshotInfo;
import org.apache.ratis.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SnapshotTest {

  private static final File testDir = new File("target" + File.separator + "sm");

  // Mock Storage which only provides the state machine dir
  private static class EmptyStorageWithOnlySMDir implements RaftStorage {

    @Override
    public RaftStorageDirectory getStorageDir() {
      return new RaftStorageDirectory() {
        @Override
        public File getRoot() {
          return null;
        }

        @Override
        public boolean isHealthy() {
          return false;
        }

        @Override
        public File getStateMachineDir() {
          return testDir;
        }
      };
    }

    @Override
    public RaftStorageMetadataFile getMetadataFile() {
      return null;
    }

    @Override
    public RaftServerConfigKeys.Log.CorruptionPolicy getLogCorruptionPolicy() {
      return null;
    }

    @Override
    public void close() throws IOException {}
  }

  @Before
  public void setUp() throws IOException {
    FileUtils.deleteFully(testDir);
    FileUtils.createDirectories(testDir);
  }

  @After
  public void tearDown() throws IOException {
    FileUtils.deleteFully(testDir);
  }

  @Test
  public void testSnapshot() throws Exception {
    ApplicationStateMachineProxy proxy =
        new ApplicationStateMachineProxy(new TestUtils.IntegerCounter());

    proxy.initialize(null, null, new EmptyStorageWithOnlySMDir());

    // take a snapshot at 421-616
    proxy.notifyTermIndexUpdated(421, 616);
    String snapshotFilename = TestUtils.IntegerCounter.ensureSnapshotFileName(testDir, "421_616");
    long index = proxy.takeSnapshot();
    Assert.assertEquals(index, 616);
    Assert.assertTrue(new File(snapshotFilename).exists());

    // take a snapshot at 616-4217
    proxy.notifyTermIndexUpdated(616, 4217);
    String snapshotFilenameLatest =
        TestUtils.IntegerCounter.ensureSnapshotFileName(testDir, "616_4217");
    long indexLatest = proxy.takeSnapshot();
    Assert.assertEquals(indexLatest, 4217);
    Assert.assertTrue(new File(snapshotFilenameLatest).exists());

    // query the latest snapshot
    SnapshotInfo info = proxy.getLatestSnapshot();
    Assert.assertEquals(info.getTerm(), 616);
    Assert.assertEquals(info.getIndex(), 4217);
    Assert.assertTrue(info.getFiles().get(0).getPath().endsWith(snapshotFilenameLatest));

    // clean up
    proxy.getStateMachineStorage().cleanupOldSnapshots(null);
    Assert.assertFalse(new File(snapshotFilename).exists());
    Assert.assertTrue(new File(snapshotFilenameLatest).exists());
  }
}
