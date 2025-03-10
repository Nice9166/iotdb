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
package org.apache.iotdb.db.service;

import org.apache.iotdb.common.rpc.thrift.TDataNodeLocation;
import org.apache.iotdb.common.rpc.thrift.TEndPoint;
import org.apache.iotdb.commons.concurrent.IoTDBDefaultThreadExceptionHandler;
import org.apache.iotdb.commons.conf.IoTDBConstant;
import org.apache.iotdb.commons.exception.BadNodeUrlException;
import org.apache.iotdb.commons.exception.ConfigurationException;
import org.apache.iotdb.commons.exception.StartupException;
import org.apache.iotdb.commons.service.JMXService;
import org.apache.iotdb.commons.service.RegisterManager;
import org.apache.iotdb.commons.service.StartupChecks;
import org.apache.iotdb.confignode.rpc.thrift.TDataNodeRegisterReq;
import org.apache.iotdb.confignode.rpc.thrift.TDataNodeRegisterResp;
import org.apache.iotdb.db.client.ConfigNodeClient;
import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBConfigCheck;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.conf.rest.IoTDBRestServiceDescriptor;
import org.apache.iotdb.db.consensus.ConsensusImpl;
import org.apache.iotdb.db.engine.StorageEngineV2;
import org.apache.iotdb.db.engine.cache.CacheHitRatioMonitor;
import org.apache.iotdb.db.engine.compaction.CompactionTaskManager;
import org.apache.iotdb.db.engine.cq.ContinuousQueryService;
import org.apache.iotdb.db.engine.flush.FlushManager;
import org.apache.iotdb.db.engine.trigger.service.TriggerRegistrationService;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.mpp.buffer.DataBlockService;
import org.apache.iotdb.db.mpp.schedule.DriverScheduler;
import org.apache.iotdb.db.protocol.influxdb.meta.InfluxDBMetaManager;
import org.apache.iotdb.db.protocol.rest.RestService;
import org.apache.iotdb.db.query.udf.service.TemporaryQueryDataFileService;
import org.apache.iotdb.db.query.udf.service.UDFClassLoaderManager;
import org.apache.iotdb.db.query.udf.service.UDFRegistrationService;
import org.apache.iotdb.db.service.basic.ServiceProvider;
import org.apache.iotdb.db.service.basic.StandaloneServiceProvider;
import org.apache.iotdb.db.service.metrics.MetricsService;
import org.apache.iotdb.db.service.thrift.impl.DataNodeTSIServiceImpl;
import org.apache.iotdb.db.sync.receiver.ReceiverService;
import org.apache.iotdb.db.sync.sender.service.SenderService;
import org.apache.iotdb.db.wal.WALManager;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.TSStatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DataNode implements DataNodeMBean {
  private static final Logger logger = LoggerFactory.getLogger(DataNode.class);

  private final String mbeanName =
      String.format(
          "%s:%s=%s", "org.apache.iotdb.datanode.service", IoTDBConstant.JMX_TYPE, "DataNode");

  /**
   * when joining a cluster this node will retry at most "DEFAULT_JOIN_RETRY" times before returning
   * a failure to the client
   */
  private static final int DEFAULT_JOIN_RETRY = 10;

  private TEndPoint thisNode = new TEndPoint();

  private DataNode() {
    // we do not init anything here, so that we can re-initialize the instance in IT.
  }

  private static final RegisterManager registerManager = new RegisterManager();
  public static ServiceProvider serviceProvider;

  // private IClientManager clientManager;

  public static DataNode getInstance() {
    return DataNodeHolder.INSTANCE;
  }

  public static void main(String[] args) {
    new DataNodeServerCommandLine().doMain(args);
  }

  protected void serverCheckAndInit() throws ConfigurationException, IOException {
    IoTDBConfigCheck.getInstance().checkConfig();
    IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();
    // TODO: check configuration for data node

    // if client ip is the default address, set it same with internal ip
    if (config.getRpcAddress().equals("0.0.0.0")) {
      config.setRpcAddress(config.getInternalIp());
    }

    thisNode.setIp(IoTDBDescriptor.getInstance().getConfig().getInternalIp());
    thisNode.setPort(IoTDBDescriptor.getInstance().getConfig().getInternalPort());
  }

  protected void doAddNode(String[] args) {
    try {
      // TODO : contact with config node to join into the cluster
      joinCluster();
      active();
    } catch (StartupException e) {
      logger.error("Fail to start server", e);
      stop();
    }
  }

  protected void doRemoveNode(String[] args) {
    // TODO: remove data node
  }

  /** initialize the current node and its services */
  public boolean initLocalEngines() {
    IoTDB.setClusterMode();
    return true;
  }

  public void joinCluster() throws StartupException {
    int retry = DEFAULT_JOIN_RETRY;
    ConfigNodeClient configNodeClient = null;
    try {
      configNodeClient = new ConfigNodeClient();
    } catch (IoTDBConnectionException | BadNodeUrlException e) {
      throw new StartupException(e.getMessage());
    }

    while (retry > 0) {
      logger.info("start joining the cluster.");
      try {
        IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();
        TDataNodeRegisterReq req = new TDataNodeRegisterReq();
        TDataNodeLocation location = new TDataNodeLocation();
        location.setDataNodeId(config.getDataNodeId());
        location.setExternalEndPoint(new TEndPoint(config.getRpcAddress(), config.getRpcPort()));
        location.setInternalEndPoint(
            new TEndPoint(config.getInternalIp(), config.getInternalPort()));
        location.setDataBlockManagerEndPoint(
            new TEndPoint(config.getInternalIp(), config.getDataBlockManagerPort()));
        location.setConsensusEndPoint(
            new TEndPoint(config.getInternalIp(), config.getConsensusPort()));
        req.setDataNodeLocation(location);

        TDataNodeRegisterResp dataNodeRegisterResp = configNodeClient.registerDataNode(req);
        if (dataNodeRegisterResp.getStatus().getCode()
                == TSStatusCode.SUCCESS_STATUS.getStatusCode()
            || dataNodeRegisterResp.getStatus().getCode()
                == TSStatusCode.DATANODE_ALREADY_REGISTERED.getStatusCode()) {
          int dataNodeID = dataNodeRegisterResp.getDataNodeId();
          if (dataNodeID != config.getDataNodeId()) {
            IoTDBConfigCheck.getInstance().serializeDataNodeId(dataNodeID);
            config.setDataNodeId(dataNodeID);
          }
          IoTDBDescriptor.getInstance().loadGlobalConfig(dataNodeRegisterResp.globalConfig);
          logger.info("Joined the cluster successfully");
          return;
        }
      } catch (IOException | IoTDBConnectionException e) {
        logger.warn("Cannot join the cluster, because: {}", e.getMessage());
      }

      try {
        // wait 5s to start the next try
        Thread.sleep(IoTDBDescriptor.getInstance().getConfig().getJoinClusterTimeOutMs());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Unexpected interruption when waiting to join the cluster", e);
        break;
      }

      // start the next try
      retry--;
    }
    // all tries failed
    logger.error("Cannot join the cluster after {} retries", DEFAULT_JOIN_RETRY);
    throw new StartupException("Cannot join the cluster.");
  }

  public void active() throws StartupException {
    // set the mpp mode to true
    IoTDBDescriptor.getInstance().getConfig().setMppMode(true);
    IoTDBDescriptor.getInstance().getConfig().setClusterMode(true);
    // start iotdb server first
    StartupChecks checks = new StartupChecks().withDefaultTest();
    try {
      checks.verify();
    } catch (StartupException e) {
      // TODO: what are some checks
      logger.error("IoTDB DataNode: failed to start because some checks failed. ", e);
      return;
    }
    try {
      setUp();
    } catch (StartupException | QueryProcessException e) {
      logger.error("meet error while starting up.", e);
      deactivate();
      logger.error("IoTDB DataNode exit");
      return;
    }
    logger.info("IoTDB DataNode has started.");

    try {
      // TODO: Start consensus layer in some where else
      ConsensusImpl.getInstance().start();
    } catch (IOException e) {
      throw new StartupException(e);
    }

    /** Register services */
    JMXService.registerMBean(getInstance(), mbeanName);
    // TODO: move rpc service initialization from iotdb instance here
    // init influxDB MManager
    if (IoTDBDescriptor.getInstance().getConfig().isEnableInfluxDBRpcService()) {
      IoTDB.initInfluxDBMManager();
    }
  }

  private void setUp() throws StartupException, QueryProcessException {
    logger.info("Setting up IoTDB DataNode...");

    Runtime.getRuntime().addShutdownHook(new IoTDBShutdownHook());
    setUncaughtExceptionHandler();
    initServiceProvider();
    registerManager.register(MetricsService.getInstance());
    logger.info("recover the schema...");
    initConfigManager();
    registerManager.register(new JMXService());
    registerManager.register(FlushManager.getInstance());
    registerManager.register(CacheHitRatioMonitor.getInstance());
    registerManager.register(CompactionTaskManager.getInstance());
    JMXService.registerMBean(getInstance(), mbeanName);
    registerManager.register(WALManager.getInstance());

    // in mpp mode we need to start some other services
    registerManager.register(StorageEngineV2.getInstance());
    registerManager.register(DataBlockService.getInstance());
    registerManager.register(InternalService.getInstance());
    registerManager.register(DriverScheduler.getInstance());
    IoTDBDescriptor.getInstance()
        .getConfig()
        .setRpcImplClassName(DataNodeTSIServiceImpl.class.getName());

    registerManager.register(TemporaryQueryDataFileService.getInstance());
    registerManager.register(UDFClassLoaderManager.getInstance());
    registerManager.register(UDFRegistrationService.getInstance());
    registerManager.register(ReceiverService.getInstance());
    registerManager.register(MetricsService.getInstance());

    // in cluster mode, RPC service is not enabled.
    if (IoTDBDescriptor.getInstance().getConfig().isEnableRpcService()) {
      registerManager.register(RPCService.getInstance());
    }

    initProtocols();

    if (IoTDBDescriptor.getInstance().getConfig().isEnableInfluxDBRpcService()) {
      InfluxDBMetaManager.getInstance().recover();
    }

    logger.info(
        "IoTDB DataNode is setting up, some storage groups may not be ready now, please wait several seconds...");

    while (!StorageEngineV2.getInstance().isAllSgReady()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.warn("IoTDB DataNode failed to set up.", e);
        Thread.currentThread().interrupt();
        return;
      }
    }

    registerManager.register(SenderService.getInstance());
    registerManager.register(UpgradeSevice.getINSTANCE());
    // in mpp mode we temporarily don't start settle service because it uses StorageEngine directly
    // in itself, but currently we need to use StorageEngineV2 instead of StorageEngine in mpp mode.
    // registerManager.register(SettleService.getINSTANCE());
    registerManager.register(TriggerRegistrationService.getInstance());
    registerManager.register(ContinuousQueryService.getInstance());

    // start reporter
    MetricsService.getInstance().startAllReporter();

    logger.info("Congratulation, IoTDB DataNode is set up successfully. Now, enjoy yourself!");
  }

  private void initConfigManager() {
    long time = System.currentTimeMillis();
    IoTDB.configManager.init();
    long end = System.currentTimeMillis() - time;
    logger.info("spend {}ms to recover schema.", end);
    logger.info(
        "After initializing, sequence tsFile threshold is {}, unsequence tsFile threshold is {}",
        IoTDBDescriptor.getInstance().getConfig().getSeqTsFileSize(),
        IoTDBDescriptor.getInstance().getConfig().getUnSeqTsFileSize());
  }

  public void stop() {
    deactivate();
  }

  private void initServiceProvider() throws QueryProcessException {
    serviceProvider = new StandaloneServiceProvider();
  }

  public static void initProtocols() throws StartupException {
    if (IoTDBDescriptor.getInstance().getConfig().isEnableInfluxDBRpcService()) {
      registerManager.register(InfluxDBRPCService.getInstance());
    }
    if (IoTDBDescriptor.getInstance().getConfig().isEnableMQTTService()) {
      registerManager.register(MQTTService.getInstance());
    }
    if (IoTDBRestServiceDescriptor.getInstance().getConfig().isEnableRestService()) {
      registerManager.register(RestService.getInstance());
    }
  }

  private void deactivate() {
    logger.info("Deactivating IoTDB DataNode...");
    // stopThreadPools();
    registerManager.deregisterAll();
    JMXService.deregisterMBean(mbeanName);
    logger.info("IoTDB DataNode is deactivated.");
  }

  private void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new IoTDBDefaultThreadExceptionHandler());
  }

  private void dataNodeIdChecker() {}

  private static class DataNodeHolder {

    private static final DataNode INSTANCE = new DataNode();

    private DataNodeHolder() {}
  }
}
