/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.rapleaf.hank.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.THsHaServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

import com.rapleaf.hank.BaseTestCase;
import com.rapleaf.hank.coordinator.Coordinator;
import com.rapleaf.hank.coordinator.DomainConfig;
import com.rapleaf.hank.coordinator.DomainConfigVersion;
import com.rapleaf.hank.coordinator.DomainGroupConfigVersion;
import com.rapleaf.hank.coordinator.HostConfig;
import com.rapleaf.hank.coordinator.HostDomainConfig;
import com.rapleaf.hank.coordinator.HostDomainPartitionConfig;
import com.rapleaf.hank.coordinator.HostState;
import com.rapleaf.hank.coordinator.MockCoordinator;
import com.rapleaf.hank.coordinator.MockDomainConfig;
import com.rapleaf.hank.coordinator.MockDomainConfigVersion;
import com.rapleaf.hank.coordinator.MockDomainGroupConfig;
import com.rapleaf.hank.coordinator.MockDomainGroupConfigVersion;
import com.rapleaf.hank.coordinator.MockHostConfig;
import com.rapleaf.hank.coordinator.MockHostDomainPartitionConfig;
import com.rapleaf.hank.coordinator.MockRingConfig;
import com.rapleaf.hank.coordinator.MockRingGroupConfig;
import com.rapleaf.hank.coordinator.PartDaemonAddress;
import com.rapleaf.hank.coordinator.RingConfig;
import com.rapleaf.hank.coordinator.RingGroupConfig;
import com.rapleaf.hank.coordinator.RingState;
import com.rapleaf.hank.exception.DataNotFoundException;
import com.rapleaf.hank.generated.HankResponse;
import com.rapleaf.hank.generated.PartDaemon;
import com.rapleaf.hank.partitioner.MapPartitioner;

public class TestHankSmartClient extends BaseTestCase {
  private static final Logger LOG = Logger.getLogger(TestHankSmartClient.class);

  private static class ServerRunnable implements Runnable {
    private final TServer server;
    public ServerRunnable(TServer server) {
      this.server = server;
    }

    @Override
    public void run() {
      server.serve();
    }
  }

  private class MockPartDaemonHandler implements PartDaemon.Iface {
    @SuppressWarnings("unused")
    private final int domainId;
    private final ByteBuffer result;

    public MockPartDaemonHandler(int domainId, ByteBuffer result) {
      this.domainId = domainId;
      this.result = result;
    }

    @Override
    public HankResponse get(int domainId, ByteBuffer key) throws TException {
      return HankResponse.value(result);
    }
  }

  private static final ByteBuffer KEY_1 = ByteBuffer.wrap(new byte[]{1});
  private static final ByteBuffer VALUE_1 = ByteBuffer.wrap(new byte[]{1});
  private static final ByteBuffer KEY_2 = ByteBuffer.wrap(new byte[]{2});
  private static final ByteBuffer VALUE_2 = ByteBuffer.wrap(new byte[]{2});

  private static int server1Port = 12345;
  private static int server2Port = 0;

  public void testIt() throws Exception {
    // launch server 1
    final PartDaemon.Iface iface1 = new MockPartDaemonHandler(0, VALUE_1);

    TNonblockingServerSocket trans1 = null;
    while (true) {
      try {
        trans1 = new TNonblockingServerSocket(server1Port);
        LOG.debug("succeeded in binding server 1 to port " + server1Port);
        break;
      } catch (TTransportException e) {
        LOG.debug("failed to bind to port " + server1Port);
        server1Port++;
      }
    }

    Args args = new Args(trans1);
    args.processor(new PartDaemon.Processor(iface1));
    args.protocolFactory(new TCompactProtocol.Factory());
    TServer server1 = new THsHaServer(args);
    Thread thread1 = new Thread(new ServerRunnable(server1), "mock part daemon #1");
    thread1.start();

    // launch server 2;
    final PartDaemon.Iface iface2 = new MockPartDaemonHandler(0, VALUE_2);

    server2Port = server1Port + 1;
    TNonblockingServerSocket trans2 = null;
    while (true) {
      try {
        trans2 = new TNonblockingServerSocket(server2Port);
        LOG.debug("succeeded in binding server 2 to port " + server2Port);
        break;
      } catch (TTransportException e) {
        LOG.debug("failed to bind to port " + server2Port);
        server2Port++;
      }
    }
    args = new Args(trans2);
    args.processor(new PartDaemon.Processor(iface2));
    args.protocolFactory(new TCompactProtocol.Factory());
    final TServer server2 = new THsHaServer(args);
    Thread thread2 = new Thread(new ServerRunnable(server2), "mock part daemon #2");
    thread2.start();

    final HostConfig hostConfig1 = getHostConfig(new PartDaemonAddress("localhost", server1Port), 0);
    final HostConfig hostConfig2 = getHostConfig(new PartDaemonAddress("localhost", server2Port), 1);

    final MockRingConfig mockRingConfig = new MockRingConfig(null, null, 1, RingState.UP) {
      @Override
      public Set<HostConfig> getHostsForDomainPartition(int domainId, int partition) throws IOException {
        assertEquals(1, domainId);
        if (partition == 0) {
          return Collections.singleton(hostConfig1);
        } else if (partition == 1) {
          return Collections.singleton(hostConfig2);
        }
        fail("got partition id " + partition + " which is invalid");
        throw new IllegalStateException();
      }

      @Override
      public Set<HostConfig> getHosts() {
        return new HashSet<HostConfig>(Arrays.asList(hostConfig1, hostConfig2));
      }
    };

    final MockDomainConfig existentDomainConfig = new MockDomainConfig("existent_domain", 2, new MapPartitioner(KEY_1, 0, KEY_2, 1), null, 1);
    MockDomainGroupConfig mockDomainGroupConfig = new MockDomainGroupConfig("myDomainGroup") {
      private final Map<Integer, DomainConfig> domainConfigs = new HashMap<Integer, DomainConfig>() {{
        put(1, existentDomainConfig);
      }};

      @Override
      public DomainConfig getDomainConfig(int domainId) {
        return domainConfigs.get(domainId);
      }

      @Override
      public int getDomainId(String domainName) throws DataNotFoundException {
        if (domainName.equals("existent_domain")) {
          return 1;
        } else {
          throw new DataNotFoundException();
        }
      }

      @Override
      public DomainGroupConfigVersion getLatestVersion() {
        return new MockDomainGroupConfigVersion(new HashSet<DomainConfigVersion>(Arrays.asList(new MockDomainConfigVersion(existentDomainConfig, 1))), this, 1);
      }
    };
    final MockRingGroupConfig mockRingGroupConfig = new MockRingGroupConfig(mockDomainGroupConfig, "myRingGroup", null) {
      @Override
      public Set<RingConfig> getRingConfigs() {
        return Collections.singleton((RingConfig)mockRingConfig);
      }
    };
    Coordinator mockCoord = new MockCoordinator() {
      @Override
      public RingGroupConfig getRingGroupConfig(String ringGroupName) {
        return mockRingGroupConfig;
      }
    };

    Thread.sleep(1000);

    try {
      HankSmartClient c = new HankSmartClient(mockCoord, "myRingGroup");

      assertEquals(HankResponse.no_such_domain(true), c.get("nonexistent_domain", null));

      assertEquals(HankResponse.value(VALUE_1), c.get("existent_domain", KEY_1));
      assertEquals(HankResponse.value(VALUE_2), c.get("existent_domain", KEY_2));
    } finally {
      server1.stop();
      server2.stop();
      thread1.join();
      thread2.join();
      trans1.close();
      trans2.close();
    }
  }

  private HostConfig getHostConfig(PartDaemonAddress address, final int partNum) throws IOException {
    MockHostConfig hc = new MockHostConfig(address) {
      @Override
      public Set<HostDomainConfig> getAssignedDomains() throws IOException {
        return Collections.singleton((HostDomainConfig)new HostDomainConfig() {
          @Override
          public HostDomainPartitionConfig addPartition(int partNum, int initialVersion) {
            return null;
          }

          @Override
          public int getDomainId() {
            return 1;
          }

          @Override
          public Set<HostDomainPartitionConfig> getPartitions() {
            return Collections.singleton((HostDomainPartitionConfig)new MockHostDomainPartitionConfig(partNum, 1, -1));
          }
        });
      }
    };
    hc.setState(HostState.SERVING);
    return hc;
  }
}
