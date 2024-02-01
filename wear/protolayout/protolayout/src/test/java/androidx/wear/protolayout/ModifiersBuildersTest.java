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

import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Color;

import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ModifiersProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ModifiersBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final ColorBuilders.ColorProp COLOR =
            new ColorBuilders.ColorProp.Builder(Color.RED)
                    .setDynamicValue(DynamicBuilders.DynamicColor.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    @Test
    public void borderSupportsDynamicColor() {
        ModifiersBuilders.Border border =
                new ModifiersBuilders.Border.Builder().setColor(COLOR).build();

        ModifiersProto.Border borderProto = border.toProto();
        assertThat(borderProto.getColor().getArgb()).isEqualTo(COLOR.getArgb());
        assertThat(borderProto.getColor().getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void backgroundSupportsDynamicColor() {
        ModifiersBuilders.Background background1 =
                new ModifiersBuilders.Background.Builder().setColor(COLOR).build();

        ModifiersProto.Background background1Proto = background1.toProto();
        assertThat(background1Proto.getColor().getArgb()).isEqualTo(COLOR.getArgb());
        assertThat(background1Proto.getColor().getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void buildShadow() {
        float blurRadius = 5f;
        int color = Color.BLUE;
        ModifiersBuilders.Shadow shadow =
                new ModifiersBuilders.Shadow.Builder()
                        .setBlurRadius(dp(blurRadius))
                        .setColor(argb(color))
                        .build();

        ModifiersProto.Shadow shadowProto = shadow.toProto();
        assertThat(shadowProto.getBlurRadius().getValue()).isEqualTo(blurRadius);
        assertThat(shadowProto.getColor().getArgb()).isEqualTo(color);
    }

    @Test
    public void buildShadow_noSupportForDynamicValues() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ModifiersBuilders.Shadow.Builder()
                                .setBlurRadius(
                                        new DimensionBuilders.DpProp.Builder(5f)
                                                .setDynamicValue(
                                                        DynamicBuilders.DynamicFloat.constant(7f))
                                                .build()));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ModifiersBuilders.Shadow.Builder()
                                .setColor(
                                        new ColorBuilders.ColorProp.Builder(Color.BLACK)
                                                .setDynamicValue(
                                                        DynamicBuilders.DynamicColor.constant(
                                                                Color.GRAY))
                                                .build()));
    }
}
