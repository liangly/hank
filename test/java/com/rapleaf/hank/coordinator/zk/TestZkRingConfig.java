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
package com.rapleaf.hank.coordinator.zk;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.rapleaf.hank.ZkTestCase;
import com.rapleaf.hank.coordinator.HostCommand;
import com.rapleaf.hank.coordinator.HostConfig;
import com.rapleaf.hank.coordinator.HostDomainConfig;
import com.rapleaf.hank.coordinator.HostState;
import com.rapleaf.hank.coordinator.PartDaemonAddress;
import com.rapleaf.hank.coordinator.RingConfig;
import com.rapleaf.hank.coordinator.RingState;
import com.rapleaf.hank.coordinator.RingStateChangeListener;

public class TestZkRingConfig extends ZkTestCase {
  private static final class MockListener implements RingStateChangeListener {
    public RingConfig calledWith;
    @Override
    public void onRingStateChange(RingConfig ringConfig) {
      calledWith = ringConfig;
      synchronized (this) {
        notifyAll();
      }
    }
  }

  private static final PartDaemonAddress LOCALHOST = PartDaemonAddress.parse("localhost:1");

  private final String ring_root = getRoot() + "/ring-group-one/ring-1";

  public void testLoadNotUpdating() throws Exception {
    ZkHostConfig hostConfig = ZkHostConfig.create(getZk(), ring_root + "/hosts", LOCALHOST);
    create(ring_root + "/current_version", "1");

    RingConfig ringConf = new ZkRingConfig(getZk(), ring_root, null);

    assertEquals("expected ring number", 1, ringConf.getRingNumber());
    assertEquals("version number", Integer.valueOf(1), ringConf.getVersionNumber());
    assertFalse("should not be updating", ringConf.isUpdatePending());
    assertEquals("number of hosts", 1, ringConf.getHosts().size());
    assertEquals("expected hosts", Collections.singleton(hostConfig), ringConf.getHosts());
  }

  public void testLoadUpdating() throws Exception {
    create(ring_root + "/current_version", "1");
    create(ring_root + "/updating_to_version", "2");

    RingConfig ringConf = new ZkRingConfig(getZk(), ring_root, null);

    assertEquals("expected ring number", 1, ringConf.getRingNumber());
    assertEquals("version number", Integer.valueOf(1), ringConf.getVersionNumber());
    assertTrue("should be updating", ringConf.isUpdatePending());
    assertEquals("updating_to_version number", Integer.valueOf(2), ringConf.getUpdatingToVersionNumber());
  }

  public void testUpdateComplete() throws Exception {
    create(ring_root + "/current_version", "1");
    create(ring_root + "/updating_to_version", "2");

    RingConfig ringConf = new ZkRingConfig(getZk(), ring_root, null);

    assertEquals("expected ring number", 1, ringConf.getRingNumber());
    assertEquals("version number", Integer.valueOf(1), ringConf.getVersionNumber());
    assertTrue("should be updating", ringConf.isUpdatePending());
    assertEquals("updating_to_version number", Integer.valueOf(2), ringConf.getUpdatingToVersionNumber());

    ringConf.updateComplete();

    ringConf = new ZkRingConfig(getZk(), ring_root, null);

    assertEquals("expected ring number", 1, ringConf.getRingNumber());
    assertEquals("version number", Integer.valueOf(2), ringConf.getVersionNumber());
    assertFalse("should not be updating", ringConf.isUpdatePending());
  }

  public void testHosts() throws Exception {
    create(ring_root + "/current_version", "1");

    ZkRingConfig ringConf = new ZkRingConfig(getZk(), ring_root, null);
    assertEquals(0, ringConf.getHosts().size());

    HostConfig hc = ringConf.addHost(LOCALHOST);
    assertEquals(LOCALHOST, hc.getAddress());
    assertEquals(Collections.singleton(hc), ringConf.getHosts());

    assertEquals(LOCALHOST, ringConf.getHostConfigByAddress(LOCALHOST).getAddress());
  }

  public void testCommandAll() throws Exception {
    create(ring_root + "/current_version", "1");
    ZkHostConfig hc = ZkHostConfig.create(getZk(), ring_root + "/hosts", LOCALHOST);
    assertNull(hc.getCurrentCommand());
    ZkRingConfig rc = new ZkRingConfig(getZk(), ring_root, null);
    rc.commandAll(HostCommand.SERVE_DATA);
    assertEquals(Arrays.asList(HostCommand.SERVE_DATA), hc.getCommandQueue());
  }

  public void testGetOldestVersionOnHosts() throws Exception {
    create(ring_root + "/current_version", "1");
    ZkHostConfig hc = ZkHostConfig.create(getZk(), ring_root + "/hosts", LOCALHOST);
    HostDomainConfig d = hc.addDomain(0);
    d.addPartition(1, 1).setCurrentDomainGroupVersion(1);
    d = hc.addDomain(1);
    d.addPartition(1, 2).setCurrentDomainGroupVersion(2);
    ZkRingConfig rc = new ZkRingConfig(getZk(), ring_root, null);
    assertEquals(Integer.valueOf(1), rc.getOldestVersionOnHosts());
  }

  public void testGetHostsForDomainPartition() throws Exception {
    RingConfig rc = ZkRingConfig.create(getZk(), getRoot(), 1, null, 1);
    PartDaemonAddress h1 = new PartDaemonAddress("localhost", 1);
    PartDaemonAddress h2 = new PartDaemonAddress("localhost", 2);
    PartDaemonAddress h3 = new PartDaemonAddress("localhost", 3);

    HostConfig hc1 = rc.addHost(h1);
    HostDomainConfig d = hc1.addDomain(1);
    d.addPartition(0, 1);
    HostConfig hc2 = rc.addHost(h2);
    d = hc2.addDomain(1);
    d.addPartition(1, 1);
    HostConfig hc3 = rc.addHost(h3);
    d = hc3.addDomain(1);
    d.addPartition(2, 1);
    d.addPartition(0, 1);

    assertEquals(new HashSet<HostConfig>(Arrays.asList(hc1, hc3)), rc.getHostsForDomainPartition(1, 0));
  }

  public void testGetRingState() throws Exception {
    RingConfig rc = ZkRingConfig.create(getZk(), getRoot(), 1, null, 1);
    assertEquals(RingState.DOWN, rc.getState());
    rc.setState(RingState.UP);
    assertEquals(RingState.UP, rc.getState());
    rc = new ZkRingConfig(getZk(), getRoot() + "/ring-1", null);
    assertEquals(RingState.UP, rc.getState());
  }

  public void testGetNumHostsInState() throws Exception {
    RingConfig rc = ZkRingConfig.create(getZk(), getRoot(), 1, null, 1);
    PartDaemonAddress h1 = new PartDaemonAddress("localhost", 1);
    PartDaemonAddress h2 = new PartDaemonAddress("localhost", 2);
    PartDaemonAddress h3 = new PartDaemonAddress("localhost", 3);

    HostConfig hc1 = rc.addHost(h1);
    hc1.setState(HostState.IDLE);
    HostConfig hc2 = rc.addHost(h2);
    hc2.setState(HostState.SERVING);
    HostConfig hc3 = rc.addHost(h3);
    hc3.setState(HostState.OFFLINE);

    assertEquals(Collections.singleton(hc1), rc.getHostsInState(HostState.IDLE));
    assertEquals(Collections.singleton(hc2), rc.getHostsInState(HostState.SERVING));
    assertEquals(Collections.singleton(hc3), rc.getHostsInState(HostState.OFFLINE));
  }

  public void testSetUpdatingToVersion() throws Exception {
    RingConfig rc = ZkRingConfig.create(getZk(), getRoot(), 1, null, 1);
    rc.updateComplete();
    assertNull(rc.getUpdatingToVersionNumber());
    rc.setUpdatingToVersion(7);
    assertEquals(Integer.valueOf(7), rc.getUpdatingToVersionNumber());
  }

  public void testRingStateListener() throws Exception {
    RingConfig rc = ZkRingConfig.create(getZk(), getRoot(), 1, null, 1);
    MockListener mockListener = new MockListener();
    rc.setStateChangeListener(mockListener);
    synchronized (mockListener) {
      mockListener.wait(1000);
    }
    assertNull(mockListener.calledWith);
    rc.setState(RingState.DOWN);
    synchronized (mockListener) {
      mockListener.wait(1000);
    }
    assertEquals(rc, mockListener.calledWith);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    create(getRoot() + "/ring-group-one");
    create(ring_root);
    create(ring_root + "/hosts");
  }
}
