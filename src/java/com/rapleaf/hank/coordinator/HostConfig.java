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
package com.rapleaf.hank.coordinator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the configuration and listening/modification of individual
 * PartDaemon hosts within the cluster.
 */
public interface HostConfig {
  public PartDaemonAddress getAddress();

  /**
   * Return this host's current state.
   * @return
   * @throws IOException
   */
  public HostState getState() throws IOException;

  /**
   * Set this host's current state. This method should only be called by the
   * host itself.
   * 
   * @param state
   * @throws IOException
   */
  public void setState(HostState state) throws IOException;

  /**
   * Returns true when the host is online. Note that this is distinct from
   * "serving data" - a host is online when it's NOT offline.
   * 
   * @return
   * @throws IOException
   */
  public boolean isOnline() throws IOException;

  /**
   * The listener will be notified when host state changes.
   * @param listener
   * @throws IOException
   */
  public void setStateChangeListener(HostStateChangeListener listener) throws IOException;

  /**
   * Add a command to this host's command queue. Consecutive duplicate commands
   * will be ignored.
   * 
   * @param command
   * @throws IOException
   */
  public void enqueueCommand(HostCommand command) throws IOException;

  /**
   * Get the (immutable) list of commands in this host's command queue.
   * @return
   * @throws IOException
   */
  public List<HostCommand> getCommandQueue() throws IOException;

  /**
   * Return the command at the head of this host's command queue and set it as
   * the current command.
   * 
   * @return
   * @throws IOException
   */
  public HostCommand processNextCommand() throws IOException;

  /**
   * Get the currently pending command.
   * @return
   * @throws IOException
   */
  public HostCommand getCurrentCommand() throws IOException;

  /**
   * Discard the current command.
   * @throws IOException
   */
  public void completeCommand() throws IOException;

  /**
   * The listener will be notified when there are changes to this host's command
   * queue.
   * 
   * @param listener
   * @throws IOException
   */
  public void setCommandQueueChangeListener(HostCommandQueueChangeListener listener) throws IOException;

  /**
   * Get the HostDomainConfigs for the domains assigned to this host.
   * @return
   * @throws IOException
   */
  public Set<HostDomainConfig> getAssignedDomains() throws IOException;

  /**
   * Add a new domain to this host.
   * @param domainId
   * @return
   * @throws IOException
   */
  public HostDomainConfig addDomain(int domainId) throws IOException;

  /**
   * Get the HostDomainConfig for the provided domainId.
   * @param domainId
   * @return
   */
  public HostDomainConfig getDomainById(int domainId);
}
