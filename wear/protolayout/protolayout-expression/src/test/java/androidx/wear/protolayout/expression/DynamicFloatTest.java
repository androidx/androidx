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

import static org.junit.Assert.assertThrows;

import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
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
    private static final AnimationSpec SPEC =
            new AnimationSpec.Builder()
                    .setAnimationParameters(
                            new AnimationParameters.Builder()
                                    .setDurationMillis(2)
                                    .setDelayMillis(1)
                                    .build())
                    .setRepeatable(
                            new AnimationParameterBuilders.Repeatable.Builder()
                                    .setRepeatMode(REPEAT_MODE_REVERSE)
                                    .setIterations(10)
                                    .build())
                    .build();

    @Test
    public void constantFloat() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        assertThat(constantFloat.toDynamicFloatProto().getFixed().getValue())
                .isWithin(0.0001f)
                .of(CONSTANT_VALUE);
    }

    @Test
    public void constantToString() {
        assertThat(DynamicFloat.constant(1f).toString()).isEqualTo("FixedFloat{value=1.0}");
    }

    @Test
    public void stateEntryValueFloat() {
        DynamicFloat stateFloat = DynamicFloat.fromState(STATE_KEY);

        assertThat(stateFloat.toDynamicFloatProto().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void stateToString() {
        assertThat(DynamicFloat.fromState("key").toString())
                .isEqualTo("StateFloatSource{sourceKey=key}");
    }

    @Test
    public void constantFloat_asInt() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        DynamicInt32 dynamicInt32 = constantFloat.asInt();

        assertThat(
                        dynamicInt32
                                .toDynamicInt32Proto()
                                .getFloatToInt()
                                .getInput()
                                .getFixed()
                                .getValue())
                .isWithin(0.0001f)
                .of(CONSTANT_VALUE);
    }

    @Test
    public void constantFloat_asIntToString() {
        assertThat(DynamicFloat.constant(1f).asInt().toString())
                .isEqualTo("FloatToInt32Op{input=FixedFloat{value=1.0}, roundMode=1}");
    }

    @Test
    public void formatFloat_defaultParameters() {
        DynamicFloat constantFloat = DynamicFloat.constant(CONSTANT_VALUE);

        DynamicString defaultFormat = constantFloat.format();

        DynamicProto.FloatFormatOp floatFormatOp =
                defaultFormat.toDynamicStringProto().getFloatFormatOp();
        assertThat(floatFormatOp.getInput()).isEqualTo(constantFloat.toDynamicFloatProto());
        assertThat(floatFormatOp.getGroupingUsed()).isFalse();
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
                new DynamicFloat.FloatFormatter.Builder()
                        .setMinFractionDigits(minFractionDigits)
                        .setMaxFractionDigits(maxFractionDigits)
                        .setMinIntegerDigits(minIntegerDigits)
                        .setGroupingUsed(groupingUsed)
                        .build();

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
    public void formatToString() {
        assertThat(
                        DynamicFloat.constant(1f)
                                .format(
                                        new DynamicFloat.FloatFormatter.Builder()
                                                .setMaxFractionDigits(2)
                                                .setMinFractionDigits(3)
                                                .setMinIntegerDigits(4)
                                                .setGroupingUsed(true)
                                                .build())
                                .toString())
                .isEqualTo(
                        "FloatFormatOp{input=FixedFloat{value=1.0}, maxFractionDigits=2, "
                                + "minFractionDigits=3, minIntegerDigits=4, groupingUsed=true}");
    }

    @Test
    public void rangeAnimatedFloat() {
        float startFloat = 100f;
        float endFloat = 200f;

        DynamicFloat animatedFloat = DynamicFloat.animate(startFloat, endFloat);
        DynamicFloat animatedFloatWithSpec = DynamicFloat.animate(startFloat, endFloat, SPEC);

        assertThat(animatedFloat.toDynamicFloatProto().getAnimatableFixed().hasAnimationSpec())
                .isFalse();
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableFixed().getFromValue())
                .isEqualTo(startFloat);
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableFixed().getToValue())
                .isEqualTo(endFloat);
        assertThat(
                        animatedFloatWithSpec
                                .toDynamicFloatProto()
                                .getAnimatableFixed()
                                .getAnimationSpec())
                .isEqualTo(SPEC.toProto());
    }

    @Test
    public void rangeAnimatedToString() {
        assertThat(
                        DynamicFloat.animate(
                                        /* start= */ 1f,
                                        /* end= */ 2f,
                                        new AnimationSpec.Builder().build())
                                .toString())
                .isEqualTo(
                        "AnimatableFixedFloat{fromValue=1.0, toValue=2.0,"
                                + " animationSpec=AnimationSpec{animationParameters=null,"
                                + " repeatable=null}}");
    }

    @Test
    public void stateAnimatedFloat() {
        DynamicFloat stateFloat = DynamicFloat.fromState(STATE_KEY);

        DynamicFloat animatedFloat = DynamicFloat.animate(STATE_KEY);
        DynamicFloat animatedFloatWithSpec = DynamicFloat.animate(STATE_KEY, SPEC);

        assertThat(animatedFloat.toDynamicFloatProto().getAnimatableDynamic().hasAnimationSpec())
                .isFalse();
        assertThat(animatedFloatWithSpec.toDynamicFloatProto().getAnimatableDynamic().getInput())
                .isEqualTo(stateFloat.toDynamicFloatProto());
        assertThat(
                        animatedFloatWithSpec
                                .toDynamicFloatProto()
                                .getAnimatableDynamic()
                                .getAnimationSpec())
                .isEqualTo(SPEC.toProto());
        assertThat(animatedFloat.toDynamicFloatProto())
                .isEqualTo(stateFloat.animate().toDynamicFloatProto());
    }

    @Test
    public void stateAnimatedToString() {
        assertThat(
                        DynamicFloat.animate(
                                        /* stateKey= */ "key",
                                        new AnimationSpec.Builder()
                                                .setAnimationParameters(
                                                        new AnimationParameters.Builder()
                                                                .setDelayMillis(1)
                                                                .build())
                                                .build())
                                .toString())
                .isEqualTo(
                        "AnimatableDynamicFloat{input=StateFloatSource{sourceKey=key},"
                                + " animationSpec=AnimationSpec{animationParameters"
                                + "=AnimationParameters{durationMillis=0,"
                                + " easing=null, delayMillis=1}, repeatable=null}}");
    }

    @Test
    public void validProto() {
        DynamicFloat from = DynamicFloat.constant(CONSTANT_VALUE);
        DynamicFloat to = DynamicFloat.fromByteArray(from.toDynamicFloatByteArray());

        assertThat(to.toDynamicFloatProto().getFixed().getValue()).isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void invalidProto() {
        assertThrows(
                IllegalArgumentException.class, () -> DynamicFloat.fromByteArray(new byte[] {1}));
    }
}
