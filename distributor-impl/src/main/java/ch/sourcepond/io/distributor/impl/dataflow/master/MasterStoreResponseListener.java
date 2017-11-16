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
package ch.sourcepond.io.distributor.impl.dataflow.master;

import ch.sourcepond.io.distributor.impl.common.master.AnswerValidatingMasterListener;
import com.hazelcast.core.Member;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class MasterStoreResponseListener extends AnswerValidatingMasterListener<Exception >{

    public MasterStoreResponseListener(final String pPath, final long pTimeout, final TimeUnit pUnit, final Collection<Member> pMembers) {
        super(pPath, pTimeout, pUnit, pMembers);
    }

    @Override
    protected void addValidationFailureMessage(StringBuilder pBuilder) {

    }

    @Override
    protected void throwValidationException(String pMessage) throws Exception {

    }
}
