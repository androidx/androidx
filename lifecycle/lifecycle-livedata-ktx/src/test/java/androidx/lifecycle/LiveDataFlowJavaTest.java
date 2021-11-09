/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import kotlinx.coroutines.flow.Flow;

@RunWith(JUnit4.class)
public class LiveDataFlowJavaTest {

    /**
     * A purpose of this function only to show case java interop.
     * Real tests are in {@link FlowAsLiveDataTest} and {@link LiveDataAsFlowTest}
     */
    @Test
    public void noOp() {
        LiveData<String> liveData = new MutableLiveData<>("no-op");
        Flow<String> flow = FlowLiveDataConversions.asFlow(liveData);
        FlowLiveDataConversions.asLiveData(flow);
    }
}
