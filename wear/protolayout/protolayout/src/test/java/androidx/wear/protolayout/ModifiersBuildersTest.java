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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.protolayout.ColorBuilders.argb;
import static androidx.wear.protolayout.DimensionBuilders.dp;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.os.BundleCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ActionBuilders.LoadAction;
import androidx.wear.protolayout.ActionBuilders.PendingIntentAction;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.ColorBuilders.LinearGradient;
import androidx.wear.protolayout.ColorBuilders.SweepGradient;
import androidx.wear.protolayout.ModifiersBuilders.Background;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.ColorProto;
import androidx.wear.protolayout.proto.ModifiersProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ModifiersBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final ColorProp COLOR =
            new ColorProp.Builder(Color.RED)
                    .setDynamicValue(DynamicBuilders.DynamicColor.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    private static final PendingIntent TEST_PENDING_INTENT =
            PendingIntent.getActivity(
                    getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);

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
        Background background1 = new Background.Builder().setColor(COLOR).build();

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
                                        new ColorProp.Builder(Color.BLACK)
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

        ModifiersBuilders.Transformation transformationModifier =
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

    @Test
    public void defaultRipple() {
        ModifiersBuilders.Clickable clickableRippleNotSet =
                new ModifiersBuilders.Clickable.Builder().build();

        assertThat(clickableRippleNotSet.isVisualFeedbackEnabled()).isTrue();
    }

    @Test
    public void disableRipple() {
        ModifiersBuilders.Clickable clickableRippleDisabled =
                new ModifiersBuilders.Clickable.Builder().setVisualFeedbackEnabled(false).build();

        assertThat(clickableRippleDisabled.isVisualFeedbackEnabled()).isFalse();
    }

    @Test
    public void buildAsymmetricalCornerModifier() {
        float[] values = new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f};
        ModifiersBuilders.Corner cornerModifier =
                new ModifiersBuilders.Corner.Builder()
                        .setTopLeftRadius(dp(values[0]), dp(values[1]))
                        .setTopRightRadius(dp(values[2]), dp(values[3]))
                        .setBottomRightRadius(dp(values[4]), dp(values[5]))
                        .setBottomLeftRadius(dp(values[6]), dp(values[7]))
                        .build();

        ModifiersProto.Corner cornerProto = cornerModifier.toProto();
        ModifiersProto.CornerRadius cornerRadius = cornerProto.getTopLeftRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(values[0]);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(values[1]);
        cornerRadius = cornerProto.getTopRightRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(values[2]);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(values[3]);
        cornerRadius = cornerProto.getBottomRightRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(values[4]);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(values[5]);
        cornerRadius = cornerProto.getBottomLeftRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(values[6]);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(values[7]);
    }

    @Test
    public void buildAsymmetricalCornerModifier_dynamicValueX_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ModifiersBuilders.Corner.Builder()
                                .setTopLeftRadius(
                                        new DimensionBuilders.DpProp.Builder(2.f)
                                                .setDynamicValue(
                                                        DynamicBuilders.DynamicFloat.animate(
                                                                2.f, 5.f))
                                                .build(),
                                        dp(1f)));
    }

    @Test
    public void buildAsymmetricalCornerModifier_dynamicValueY_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ModifiersBuilders.Corner.Builder()
                                .setTopLeftRadius(
                                        dp(1f),
                                        new DimensionBuilders.DpProp.Builder(2.f)
                                                .setDynamicValue(
                                                        DynamicBuilders.DynamicFloat.animate(
                                                                2.f, 5.f))
                                                .build()));
    }

    @Test
    public void asymmetricalCornerModifierGetters_returnDefaultValues() {
        ModifiersBuilders.Corner cornerModifier =
                new ModifiersBuilders.Corner.Builder()
                        .setRadius(dp(5f))
                        .setTopLeftRadius(dp(1f), dp(2f))
                        .build();

        ModifiersBuilders.CornerRadius cornerRadius = cornerModifier.getTopLeftRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(1f);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(2f);
        cornerRadius = cornerModifier.getTopRightRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(5f);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(5f);
        cornerRadius = cornerModifier.getBottomRightRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(5f);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(5f);
        cornerRadius = cornerModifier.getBottomLeftRadius();
        assertThat(cornerRadius.getX().getValue()).isEqualTo(5f);
        assertThat(cornerRadius.getY().getValue()).isEqualTo(5f);
    }

    @Test
    public void backgroundSupportsLinearGradient() {
        Background backgroundLinear =
                new Background.Builder()
                        .setBrush(
                                new LinearGradient.Builder(argb(Color.BLUE), argb(Color.RED))
                                        .build())
                        .build();
        ModifiersProto.Background backgroundLinearProto = backgroundLinear.toProto();
        assertThat(backgroundLinearProto.getBrush().getInnerCase())
                .isEqualTo(ColorProto.Brush.InnerCase.LINEAR_GRADIENT);
    }

    @Test
    public void background_withSweepGradient_throws() {
        SweepGradient sweep = new SweepGradient.Builder(argb(Color.BLUE), argb(Color.RED)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new Background.Builder().setBrush(sweep).build());
    }

    @Test
    public void clickable_withPendingIntent_withoutScope_throws() {
        ModifiersBuilders.Clickable.Builder builder = new ModifiersBuilders.Clickable.Builder();

        assertThrows(IllegalStateException.class, () -> builder.setOnClick(TEST_PENDING_INTENT));
    }

    @Test
    public void clickable_withPendingIntent_registeredToScope() {
        ProtoLayoutScope scope = new ProtoLayoutScope();
        String clickableId = "test_id";
        ModifiersBuilders.Clickable clickable =
                new ModifiersBuilders.Clickable.Builder(scope, clickableId)
                        .setOnClick(TEST_PENDING_INTENT)
                        .build();

        Bundle collectedPendingIntents = scope.collectPendingIntents();

        assertThat(clickable.getOnClick()).isInstanceOf(PendingIntentAction.class);
        assertThat(collectedPendingIntents.containsKey(clickableId)).isTrue();
        assertThat(
                        BundleCompat.getParcelable(
                                collectedPendingIntents, clickableId, PendingIntent.class))
                .isEqualTo(TEST_PENDING_INTENT);
    }

    @Test
    public void clickable_withPendingIntent_overrideByLoadAction() {
        ProtoLayoutScope scope = new ProtoLayoutScope();
        String clickableId = "test_id";
        ModifiersBuilders.Clickable clickable =
                new ModifiersBuilders.Clickable.Builder(scope, clickableId)
                        .setOnClick(TEST_PENDING_INTENT)
                        .setOnClick(new LoadAction.Builder().build())
                        .build();

        Bundle collectedPendingIntents = scope.collectPendingIntents();

        // verify that the setOnClick(loadAction) clears the PendingIntent
        assertThat(clickable.getOnClick()).isInstanceOf(LoadAction.class);
        assertThat(collectedPendingIntents.containsKey(clickableId)).isFalse();
    }
}
