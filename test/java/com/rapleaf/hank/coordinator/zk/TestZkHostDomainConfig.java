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

import com.rapleaf.hank.ZkTestCase;
import com.rapleaf.hank.coordinator.HostDomainConfig;
import com.rapleaf.hank.coordinator.HostDomainPartitionConfig;

public class TestZkHostDomainConfig extends ZkTestCase {

  public void testIt() throws Exception {
    HostDomainConfig hdc = ZkHostDomainConfig.create(getZk(), getRoot(), (byte) 1);
    assertEquals(1, hdc.getDomainId());
    assertEquals(0, hdc.getPartitions().size());

    hdc.addPartition(1, 1);
    assertEquals(1, hdc.getPartitions().size());
    assertEquals(1, ((HostDomainPartitionConfig)hdc.getPartitions().toArray()[0]).getPartNum());
  }
}
