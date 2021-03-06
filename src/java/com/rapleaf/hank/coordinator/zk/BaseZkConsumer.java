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

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

abstract class BaseZkConsumer {
  protected final ZooKeeper zk;

  protected BaseZkConsumer(ZooKeeper zk) {
    this.zk = zk;
  }

  protected Integer getIntOrNull(String path) throws KeeperException, InterruptedException {
    if (zk.exists(path, false) == null) {
      return null;
    } else {
      return Integer.parseInt(new String(zk.getData(path, false, new Stat())));
    }
  }

  protected int getInt(String path) throws KeeperException, InterruptedException {
    return Integer.parseInt(new String(zk.getData(path, false, new Stat())));
  }

  protected String getString(String path) throws KeeperException, InterruptedException {
    try {
      byte[] data = zk.getData(path, false, null);
      if (data == null) {
        return null;
      }
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  protected void setInt(String path, int nextVersion) throws KeeperException,
      InterruptedException {
    zk.setData(path, ("" + nextVersion).getBytes(), -1);
  }

  protected void setString(String path, String value) throws KeeperException,
      InterruptedException {
    zk.setData(path, value.getBytes(), -1);
  }

  protected void deleteIfExists(String path) throws KeeperException, InterruptedException {
    if (zk.exists(path, false) != null) {
      zk.delete(path, -1);
    }
  }

  protected void setOrCreate(String path, int value, CreateMode createMode) throws KeeperException, InterruptedException {
    setOrCreate(path, "" + value, createMode);
  }

  protected void setOrCreate(String path, String value, CreateMode createMode) throws KeeperException, InterruptedException {
    if (zk.exists(path, false) == null) {
      zk.create(path, value.getBytes(), Ids.OPEN_ACL_UNSAFE, createMode);
    } else {
      zk.setData(path, value.getBytes(), -1);
    }
  }

  public static void deleteNodeRecursively(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
    try {
      zk.delete(path, -1);
    } catch (KeeperException.NotEmptyException e) {
      List<String> children = zk.getChildren(path, null);
      for (String child : children) {
        deleteNodeRecursively(zk, path + "/" + child);
      }
      zk.delete(path, -1);
    } catch (KeeperException.NoNodeException e) {
      // Silently return if the node has already been deleted.
      return;
    }
  }
}
