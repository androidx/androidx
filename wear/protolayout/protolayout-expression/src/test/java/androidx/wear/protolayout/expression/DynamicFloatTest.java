/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicFloatTest {
    private static final String STATE_KEY = "state-key";
    private static final float CONSTANT_VALUE = 42.42f;

    @Test
    public void constantFloat() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        assertThat(constantFloat.toDynamicFloatProto().getFixed().getValue())
                .isWithin(0.0001f).of(CONSTANT_VALUE);
    }

    @Test
    public void stateEntryValueFloat() {
        DynamicFloat stateFloat = DynamicFloat.fromState(STATE_KEY);

        assertThat(stateFloat.toDynamicFloatProto().getStateSource().getSourceKey()).isEqualTo(
                STATE_KEY);
    }

    @Test
    public void constantFloat_asInt() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        DynamicInt32 dynamicInt32 = constantFloat.asInt();

        assertThat(dynamicInt32.toDynamicInt32Proto().getFloatToInt()
                .getInput().getFixed().getValue()).isWithin(0.0001f).of(CONSTANT_VALUE);
    }
}
