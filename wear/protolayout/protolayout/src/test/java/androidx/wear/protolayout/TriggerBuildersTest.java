/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.DynamicBuilders;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TriggerBuildersTest {

    @Test
    public void onLoadTrigger() {
        TriggerBuilders.Trigger onLoadTrigger = TriggerBuilders.createOnLoadTrigger();

        assertThat(onLoadTrigger.toTriggerProto().hasOnLoadTrigger()).isTrue();
    }


    @Test
    public void onConditionTrigger() {
        DynamicBuilders.DynamicBool condition = DynamicBuilders.DynamicBool.fromState("state");

        TriggerBuilders.Trigger onConditionMetTrigger =
                TriggerBuilders.createOnConditionMetTrigger(
                condition);

        assertThat(
                onConditionMetTrigger.toTriggerProto().getOnConditionMetTrigger().getCondition())
                .isEqualTo(condition.toDynamicBoolProto());
    }
}
