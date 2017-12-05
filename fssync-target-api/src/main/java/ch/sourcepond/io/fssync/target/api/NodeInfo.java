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
package ch.sourcepond.io.fssync.target.api;

import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.hash;

public class NodeInfo {
    private final String senderNode;
    private final String localNode;

    public NodeInfo(final String pSenderNode, final String pLocalNode) {
        senderNode = pSenderNode;
        localNode = pLocalNode;
    }

    public String getSender() {
        return senderNode;
    }

    public String getLocal() {
        return localNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(senderNode, nodeInfo.senderNode) &&
                Objects.equals(localNode, nodeInfo.localNode);
    }

    @Override
    public int hashCode() {
        return hash(senderNode, localNode);
    }

    @Override
    public String toString() {
        return format("[sender:%s, local:%s]", senderNode, localNode);
    }
}
