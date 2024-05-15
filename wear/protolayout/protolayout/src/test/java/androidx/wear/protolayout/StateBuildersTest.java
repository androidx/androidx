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

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.StateBuilders.State;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StateBuildersTest {
    @Test
    public void emptyState() {
        StateBuilders.State state = new StateBuilders.State.Builder().build();

        assertThat(state.getKeyToValueMapping()).isEmpty();
    }

    @Test
    public void additionalState() {
        StateBuilders.State state =
                new StateBuilders.State.Builder()
                        .addKeyToValueMapping(
                                new AppDataKey<>("boolValue"), DynamicDataValue.fromBool(true))
                        .addKeyToValueMapping(
                                new AppDataKey<>("stringValue"),
                                DynamicDataValue.fromString("string"))
                        .build();
        assertThat(state.getKeyToValueMapping()).hasSize(2);
        assertThat(
                        state.getKeyToValueMapping()
                                .get(new AppDataKey<>("boolValue"))
                                .toDynamicDataValueProto())
                .isEqualTo(DynamicDataValue.fromBool(true).toDynamicDataValueProto());
        assertThat(
                        state.getKeyToValueMapping()
                                .get(new AppDataKey<>("stringValue"))
                                .toDynamicDataValueProto())
                .isEqualTo(DynamicDataValue.fromString("string").toDynamicDataValueProto());
    }

    @Test
    public void buildState_stateTooLarge_throws() {
        State.Builder builder = new State.Builder();
        int maxStateEntryCount = State.getMaxStateEntryCount();
        for (int i = 0; i < maxStateEntryCount; i++) {
            builder.addKeyToValueMapping(
                    new AppDataKey<>(Integer.toString(i)), DynamicDataValue.fromInt(0));
        }
        assertThrows(IllegalStateException.class, () -> builder.addKeyToValueMapping(
                new AppDataKey<>(Integer.toString(maxStateEntryCount + 1)),
                DynamicDataValue.fromInt(0)));
    }

    @Test
    public void buildState_stateSizeIsMaximum_buildSuccessfully() {
        State.Builder builder = new State.Builder();
        for (int i = 0; i < StateBuilders.State.getMaxStateEntryCount(); i++) {
            builder.addKeyToValueMapping(
                    new AppDataKey<>(Integer.toString(i)), DynamicDataValue.fromInt(0));
        }
        assertThat(builder.build().getKeyToValueMapping()).hasSize(30);
    }
}
