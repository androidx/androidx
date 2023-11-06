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

import android.graphics.Color;

import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ColorProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ColorBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final ColorBuilders.ColorProp COLOR =
            new ColorBuilders.ColorProp.Builder(Color.RED)
                    .setDynamicValue(DynamicBuilders.DynamicColor.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    @SuppressWarnings("deprecation")
    private static final ColorBuilders.ColorProp.Builder COLOR_BUILDER_WITHOUT_STATIC_VALUE =
            new ColorBuilders.ColorProp.Builder()
                    .setDynamicValue(
                            DynamicBuilders.DynamicColor.from(new AppDataKey<>(STATE_KEY)));

    @Test
    public void colorPropSupportsDynamicColor() {
        ColorProto.ColorProp colorPropProto = COLOR.toProto();

        assertThat(colorPropProto.getArgb()).isEqualTo(COLOR.getArgb());
        assertThat(colorPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void colorProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, COLOR_BUILDER_WITHOUT_STATIC_VALUE::build);
    }
}
