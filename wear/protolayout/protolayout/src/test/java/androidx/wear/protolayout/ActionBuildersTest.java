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

import androidx.wear.protolayout.expression.StateEntryBuilders;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ActionBuildersTest {
    @Test
    public void setStateAction() {
        String key = "key";
        StateEntryBuilders.StateEntryValue value = StateEntryBuilders.StateEntryValue.fromString(
                "value");
        ActionBuilders.SetStateAction setStateAction = new ActionBuilders.SetStateAction.Builder()
                .setTargetKey(key).setValue(value).build();

        assertThat(setStateAction.getTargetKey()).isEqualTo(key);
        assertThat(setStateAction.getValue().toStateEntryValueProto()).isEqualTo(
                value.toStateEntryValueProto());
    }
}
