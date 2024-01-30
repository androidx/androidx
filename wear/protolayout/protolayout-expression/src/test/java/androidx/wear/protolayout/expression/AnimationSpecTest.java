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

package androidx.wear.protolayout.expression;

import static androidx.wear.protolayout.expression.AnimationParameterBuilders.REPEAT_MODE_RESTART;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.CubicBezierEasing;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.Repeatable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AnimationSpecTest {
    @Test
    public void animationSpecToString_deprecated() {
        assertThat(
                        new AnimationSpec.Builder()
                                .setAnimationParameters(
                                        new AnimationParameters.Builder()
                                                .setDurationMillis(1)
                                                .setDelayMillis(2)
                                                .setEasing(
                                                        new CubicBezierEasing.Builder()
                                                                .setX1(3f)
                                                                .build())
                                                .build())
                                .setRepeatable(new Repeatable.Builder().setIterations(4).build())
                                .build()
                                .toString())
                .isEqualTo(
                        "AnimationSpec{animationParameters=AnimationParameters{durationMillis=1, "
                                + "easing=CubicBezierEasing{x1=3.0, y1=0.0, x2=0.0, y2=0.0}, "
                                + "delayMillis=2}, "
                                + "repeatable=Repeatable{iterations=4, repeatMode=0, "
                                + "forwardRepeatOverride=null, "
                                + "reverseRepeatOverride=null}}");
    }

    @Test
    public void animationSpecToString() {
        assertThat(
                        new AnimationSpec.Builder()
                                .setAnimationParameters(
                                        new AnimationParameters.Builder()
                                                .setDurationMillis(20)
                                                .setEasing(
                                                        new CubicBezierEasing.Builder()
                                                                .setX1(3f)
                                                                .build())
                                                .setDelayMillis(10)
                                                .build())
                                .setRepeatable(new Repeatable.Builder().setIterations(4).build())
                                .build()
                                .toString())
                .isEqualTo(
                        "AnimationSpec{animationParameters=AnimationParameters{durationMillis=20,"
                                + " easing=CubicBezierEasing{x1=3.0, y1=0.0, x2=0.0, y2=0.0},"
                                + " delayMillis=10}, repeatable=Repeatable{iterations=4,"
                                + " repeatMode=0, forwardRepeatOverride=null, "
                                + "reverseRepeatOverride=null}}");
    }

    @Test
    public void cubicBezierEasingToString() {
        assertThat(
                        new CubicBezierEasing.Builder()
                                .setX1(1f)
                                .setY1(2f)
                                .setX2(3f)
                                .setY2(4f)
                                .build()
                                .toString())
                .isEqualTo("CubicBezierEasing{x1=1.0, y1=2.0, x2=3.0, y2=4.0}");
    }

    @Test
    public void repeatableToString() {
        assertThat(
                        new Repeatable.Builder()
                                .setIterations(10)
                                .setRepeatMode(REPEAT_MODE_RESTART)
                                .setReverseRepeatOverride(
                                        new AnimationParameters.Builder()
                                                .setDurationMillis(20)
                                                .setEasing(
                                                        new CubicBezierEasing.Builder()
                                                                .setX1(3f)
                                                                .build())
                                                .setDelayMillis(10)
                                                .build())
                                .build()
                                .toString())
                .isEqualTo(
                        "Repeatable{iterations=10, repeatMode=1, forwardRepeatOverride=null,"
                                + " reverseRepeatOverride=AnimationParameters{durationMillis"
                                + "=20,"
                                + " easing=CubicBezierEasing{x1=3.0, y1=0.0, x2=0.0, y2=0.0}, "
                                + "delayMillis=10}}");
    }

    @Test
    public void animationParametersToString() {
        assertThat(
                        new AnimationParameters.Builder()
                                .setDurationMillis(100)
                                .setEasing(new CubicBezierEasing.Builder().setX1(3f).build())
                                .setDelayMillis(20)
                                .build()
                                .toString())
                .isEqualTo(
                        "AnimationParameters{durationMillis=100, easing=CubicBezierEasing{x1=3.0, "
                                + "y1=0.0, x2=0.0, y2=0.0}, delayMillis=20}");
    }
}
