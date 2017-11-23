/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.distributor.impl.binding;

import com.hazelcast.config.ReliableTopicConfig;

class TopicConfigs {
    private ReliableTopicConfig responseTopicConfig;
    private ReliableTopicConfig deleteTopicConfig;
    private ReliableTopicConfig transferTopicConfig;
    private ReliableTopicConfig storeTopicConfig;
    private ReliableTopicConfig lockTopicConfig;
    private ReliableTopicConfig unlockTopicConfig;

    public ReliableTopicConfig getResponseTopicConfig() {
        return responseTopicConfig;
    }

    void setResponseTopicConfig(ReliableTopicConfig responseTopicConfig) {
        this.responseTopicConfig = responseTopicConfig;
    }

    public ReliableTopicConfig getDeleteTopicConfig() {
        return deleteTopicConfig;
    }

    void setDeleteTopicConfig(ReliableTopicConfig deleteTopicConfig) {
        this.deleteTopicConfig = deleteTopicConfig;
    }

    public ReliableTopicConfig getTransferTopicConfig() {
        return transferTopicConfig;
    }

    void setTransferTopicConfig(ReliableTopicConfig transferTopicConfig) {
        this.transferTopicConfig = transferTopicConfig;
    }

    public ReliableTopicConfig getStoreTopicConfig() {
        return storeTopicConfig;
    }

    void setStoreTopicConfig(ReliableTopicConfig storeTopicConfig) {
        this.storeTopicConfig = storeTopicConfig;
    }

    public ReliableTopicConfig getLockTopicConfig() {
        return lockTopicConfig;
    }

    void setLockTopicConfig(ReliableTopicConfig lockTopicConfig) {
        this.lockTopicConfig = lockTopicConfig;
    }

    public ReliableTopicConfig getUnlockTopicConfig() {
        return unlockTopicConfig;
    }

    void setUnlockTopicConfig(ReliableTopicConfig unlockTopicConfig) {
        this.unlockTopicConfig = unlockTopicConfig;
    }
}
