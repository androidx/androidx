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

import androidx.annotation.ColorInt;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicColorTest {
    private static final String STATE_KEY = "state-key";
    private static final @ColorInt int CONSTANT_VALUE = 0xff00ff00;

    @Test
    public void constantColor() {
        DynamicColor constantColor = DynamicColor.constant(CONSTANT_VALUE);

        assertThat(constantColor.toDynamicColorProto().getFixed().getArgb())
                .isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void stateEntryValueColor() {
        DynamicColor stateColor = DynamicColor.fromState(STATE_KEY);

        assertThat(stateColor.toDynamicColorProto().getStateSource().getSourceKey()).isEqualTo(
                STATE_KEY);
    }
}
