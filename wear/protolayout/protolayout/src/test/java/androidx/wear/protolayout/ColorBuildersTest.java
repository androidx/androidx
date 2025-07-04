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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ColorBuilders.LinearGradient;
import androidx.wear.protolayout.DimensionBuilders.BoundingBoxRatio;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.proto.ColorProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ColorBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final ColorBuilders.ColorProp COLOR =
            new ColorBuilders.ColorProp.Builder(Color.RED)
                    .setDynamicValue(DynamicBuilders.DynamicColor.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    @Test
    public void colorPropSupportsDynamicColor() {
        ColorProto.ColorProp colorPropProto = COLOR.toProto();

        assertThat(colorPropProto.getArgb()).isEqualTo(COLOR.getArgb());
        assertThat(colorPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void colorProp_withoutStaticValue_throws() {
        assertThrows(
                IllegalStateException.class,
                new ColorBuilders.ColorProp.Builder()
                                .setDynamicValue(
                                        DynamicBuilders.DynamicColor.from(
                                                new AppDataKey<>(STATE_KEY)))
                        ::build);
    }

    @Test
    public void linearGradient_supportsDynamicOffsets() {
        DynamicFloat dynamicFloat = DynamicFloat.constant(1.0f);
        DpProp startX = dp(0f);
        DpProp startY = new DpProp.Builder(0f).setDynamicValue(dynamicFloat).build();
        BoundingBoxRatio endX =
                new BoundingBoxRatio.Builder()
                        .setRatio(new FloatProp.Builder(1.0f).build())
                        .build();
        BoundingBoxRatio endY =
                new BoundingBoxRatio.Builder()
                        .setRatio(new FloatProp.Builder(1.0f).setDynamicValue(dynamicFloat).build())
                        .build();
        LinearGradient linearGrad =
                new LinearGradient.Builder(argb(Color.BLUE), argb(Color.RED))
                        .setStartX(startX)
                        .setStartY(startY)
                        .setEndX(endX)
                        .setEndY(endY)
                        .build();

        assertThat(((DpProp) linearGrad.getStartX()).getValue()).isEqualTo(startX.getValue());
        assertThat(((DpProp) linearGrad.getStartY()).getDynamicValue()).isNotNull();
        assertThat(((BoundingBoxRatio) linearGrad.getEndX()).getRatio().getValue()).isEqualTo(1.0f);
        assertThat(((BoundingBoxRatio) linearGrad.getEndY()).getRatio().getDynamicValue())
                .isNotNull();
    }
}
