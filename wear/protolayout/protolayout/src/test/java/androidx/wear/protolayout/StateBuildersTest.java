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

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.AppDataKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StateBuildersTest {
    @Test
    public void emptyState() {
        StateBuilders.State state = new StateBuilders.State.Builder().build();

        assertThat(state.getIdToValueMapping()).isEmpty();
    }

    @Test
    public void additionalState() {
        DynamicDataBuilders.DynamicDataValue boolValue =
                DynamicDataBuilders.DynamicDataValue.fromBool(true);
        DynamicDataBuilders.DynamicDataValue stringValue =
                DynamicDataBuilders.DynamicDataValue.fromString("string");
        StateBuilders.State state = new StateBuilders.State.Builder()
                .addKeyToValueMapping(
                        new AppDataKey<DynamicBool>("boolValue"), boolValue)
                .addKeyToValueMapping(
                        new AppDataKey<DynamicString>("stringValue"), stringValue)
                .build();

        assertThat(state.getIdToValueMapping()).hasSize(2);
        assertThat(state.getIdToValueMapping().get("boolValue").toDynamicDataValueProto())
                .isEqualTo(boolValue.toDynamicDataValueProto());
        assertThat(state.getIdToValueMapping().get("stringValue").toDynamicDataValueProto())
                .isEqualTo(stringValue.toDynamicDataValueProto());
    }
}
