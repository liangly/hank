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
import java.util.Map;
import java.util.SortedSet;

import com.rapleaf.hank.exception.DataNotFoundException;

/**
 * Encapsulates the configuration and listening/modification of a domain group.
 */
public interface DomainGroupConfig {
  public String getName();

  /**
   * Get the DomainConfig for the domain with <i>domainId</i>
   * @param domainId
   * @return
   * @throws DataNotFoundException
   */
  public DomainConfig getDomainConfig(int domainId)
  throws DataNotFoundException;

  /**
   * Get the ID of the domain named <i>domainName</i>
   * @param domainName
   * @return
   * @throws DataNotFoundException
   */
  public int getDomainId(String domainName) throws DataNotFoundException;

  /**
   * Get a set of DomainGroupConfigVersions ordered by version number
   * (ascending).
   * 
   * @return
   * @throws IOException 
   */
  public SortedSet<DomainGroupConfigVersion> getVersions() throws IOException;

  /**
   * Convenience method to get the most recent version of the domain group.
   * @return
   * @throws IOException 
   */
  public DomainGroupConfigVersion getLatestVersion() throws IOException;

  public void setListener(DomainGroupChangeListener listener);

  public void addDomain(DomainConfig domainConfig, int domainId) throws IOException;

  public DomainGroupConfigVersion createNewVersion(Map<String, Integer> domainIdToVersion) throws IOException;
}
