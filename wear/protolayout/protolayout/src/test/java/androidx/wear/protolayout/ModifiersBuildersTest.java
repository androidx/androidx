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
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ModifiersProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
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

    @Test
    public void buildTransformationModifier() {
        DimensionBuilders.DpProp translation =
                new DimensionBuilders.DpProp.Builder(42)
                        .setDynamicValue(
                                DynamicBuilders.DynamicFloat.from(new AppDataKey<>("some-state")))
                        .build();
        TypeBuilders.FloatProp scaleX = new TypeBuilders.FloatProp.Builder(0.8f).build();
        TypeBuilders.FloatProp scaleY = new TypeBuilders.FloatProp.Builder(1.2f).build();
        DimensionBuilders.DegreesProp rotation =
                new DimensionBuilders.DegreesProp.Builder(210.f).build();
        DimensionBuilders.PivotDimension pivotDimensionX =
                new DimensionBuilders.DpProp.Builder(42)
                        .setDynamicValue(
                                DynamicBuilders.DynamicFloat.from(new AppDataKey<>("other-state")))
                        .build();
        DimensionBuilders.PivotDimension pivotDimensionY =
                new DimensionBuilders.BoundingBoxRatio.Builder(
                        new TypeBuilders.FloatProp.Builder(0.8f)
                                .setDynamicValue(
                                        DynamicBuilders.DynamicFloat.constant(0.2f))
                                .build())
                        .build();

        ModifiersBuilders.Transformation transformationModifier=
                new ModifiersBuilders.Transformation.Builder()
                        .setTranslationX(translation)
                        .setTranslationY(translation)
                        .setRotation(rotation)
                        .setScaleX(scaleX)
                        .setScaleY(scaleY)
                        .setPivotX(pivotDimensionX)
                        .setPivotY(pivotDimensionY)
                        .build();

        ModifiersProto.Transformation transformationProto = transformationModifier.toProto();
        assertThat(transformationProto.getTranslationX()).isEqualTo(translation.toProto());
        assertThat(transformationProto.getTranslationY())
                .isEqualTo(transformationProto.getTranslationX());
        assertThat(transformationProto.getRotation()).isEqualTo(rotation.toProto());
        assertThat(transformationProto.getScaleX()).isNotEqualTo(transformationProto.getScaleY());
        assertThat(transformationProto.getScaleX().getValue()).isEqualTo(0.8f);
        assertThat(transformationProto.getScaleY().getValue()).isEqualTo(1.2f);
        assertThat(transformationProto.getPivotX())
                .isEqualTo(pivotDimensionX.toPivotDimensionProto());
        assertThat(transformationProto.getPivotY())
                .isEqualTo(pivotDimensionY.toPivotDimensionProto());


    }
}
