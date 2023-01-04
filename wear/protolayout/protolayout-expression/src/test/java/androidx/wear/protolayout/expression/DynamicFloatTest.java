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

import static androidx.wear.protolayout.expression.AnimationParameterBuilders.REPEAT_MODE_REVERSE;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicFloatTest {
    private static final String STATE_KEY = "state-key";
    private static final float CONSTANT_VALUE = 42.42f;
    private static final AnimationParameterBuilders.AnimationSpec
            SPEC = new AnimationParameterBuilders.AnimationSpec.Builder().setDelayMillis(1)
            .setDurationMillis(2).setRepeatable(new AnimationParameterBuilders.Repeatable.Builder()
                    .setRepeatMode(REPEAT_MODE_REVERSE).setIterations(10).build()).build();

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

    @Test
    public void formatFloat_defaultParameters() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        DynamicString defaultFormat = constantFloat.format();

        DynamicProto.FloatFormatOp floatFormatOp =
                defaultFormat.toDynamicStringProto().getFloatFormatOp();
        assertThat(floatFormatOp.getInput()).isEqualTo(constantFloat.toDynamicFloatProto());
        assertThat(floatFormatOp.getGroupingUsed()).isEqualTo(false);
        assertThat(floatFormatOp.hasMaxFractionDigits()).isFalse();
        assertThat(floatFormatOp.getMinFractionDigits()).isEqualTo(0);
        assertThat(floatFormatOp.hasMinIntegerDigits()).isFalse();
    }
    @Test
    public void formatFloat_customFormatter() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);
        boolean groupingUsed = true;
        int minFractionDigits = 1;
        int maxFractionDigits = 2;
        int minIntegerDigits = 3;
        DynamicFloat.FloatFormatter floatFormatter =
                DynamicFloat.FloatFormatter.with().minFractionDigits(minFractionDigits)
                        .maxFractionDigits(maxFractionDigits).minIntegerDigits(minIntegerDigits)
                        .groupingUsed(groupingUsed);

        DynamicString customFormat = constantFloat.format(floatFormatter);

        DynamicProto.FloatFormatOp floatFormatOp =
                customFormat.toDynamicStringProto().getFloatFormatOp();
        assertThat(floatFormatOp.getInput()).isEqualTo(constantFloat.toDynamicFloatProto());
        assertThat(floatFormatOp.getGroupingUsed()).isEqualTo(groupingUsed);
        assertThat(floatFormatOp.getMaxFractionDigits()).isEqualTo(maxFractionDigits);
        assertThat(floatFormatOp.getMinFractionDigits()).isEqualTo(minFractionDigits);
        assertThat(floatFormatOp.getMinIntegerDigits()).isEqualTo(minIntegerDigits);
    }

    @Test
    public void rangeAnimatedFloat() {
        float startFloat = 100f;
        float endFloat = 200f;

        DynamicFloat animatedFloat = DynamicFloat.animate(startFloat,
                endFloat);
        DynamicFloat animatedFloatWithSpec = DynamicFloat.animate(startFloat, endFloat, SPEC);

        assertThat(animatedFloat.toDynamicFloatProto().getAnimatableFixed().hasSpec()).isFalse();
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableFixed().getFromValue())
                .isEqualTo(startFloat);
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableFixed().getToValue())
                .isEqualTo(endFloat);
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableFixed().getSpec())
                .isEqualTo(SPEC.toProto());
    }

    @Test
    public void stateAnimatedFloat() {
        DynamicFloat stateFloat = DynamicFloat.fromState(STATE_KEY);

        DynamicFloat animatedColor = DynamicFloat.animate(STATE_KEY);
        DynamicFloat animatedColorWithSpec = DynamicFloat.animate(STATE_KEY, SPEC);

        assertThat(animatedColor.toDynamicFloatProto().getAnimatableDynamic().hasSpec()).isFalse();
        assertThat(animatedColorWithSpec.toDynamicFloatProto().getAnimatableDynamic().getInput())
                .isEqualTo(stateFloat.toDynamicFloatProto());
        assertThat(animatedColorWithSpec.toDynamicFloatProto().getAnimatableDynamic().getSpec())
                .isEqualTo(SPEC.toProto());
        assertThat(animatedColor.toDynamicFloatProto())
                .isEqualTo(stateFloat.animate().toDynamicFloatProto());
    }
}
