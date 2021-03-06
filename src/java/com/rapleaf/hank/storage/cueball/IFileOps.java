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
package com.rapleaf.hank.storage.cueball;

import java.io.IOException;
import java.util.List;

/**
 * IFileOps is a micro-abstraction of a remote filesystem used by Fetcher to
 * provide a variety of retrieval strategies.
 */
public interface IFileOps {
  /**
   * Get the list of files in the remote location.
   * @return
   * @throws IOException
   */
  public List<String> listFiles() throws IOException;

  /**
   * Copy the specified file from remote to local.
   * @param fileName
   * @throws IOException
   */
  public void copyToLocal(String fileName) throws IOException;
}
