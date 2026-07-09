/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.CubicBezierEasing;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Easing;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AnimationsHelperTest {

    @Test
    public void getMainInterpolatorOrDefault_validCubicBezier_returnsPathInterpolator() {
        AnimationSpec spec = AnimationSpec.newBuilder()
                .setAnimationParameters(AnimationParameters.newBuilder()
                        .setEasing(Easing.newBuilder()
                                .setCubicBezier(CubicBezierEasing.newBuilder()
                                        .setX1(0.1f)
                                        .setY1(0.2f)
                                        .setX2(0.8f)
                                        .setY2(0.9f)
                                        .build())
                                .build())
                        .build())
                .build();

        Interpolator interpolator = AnimationsHelper.getMainInterpolatorOrDefault(spec);

        assertThat(interpolator).isInstanceOf(PathInterpolator.class);
    }

    @Test
    public void getMainInterpolatorOrDefault_invalidCubicBezier_returnsDefaultInterpolator() {
        AnimationSpec spec = AnimationSpec.newBuilder()
                .setAnimationParameters(AnimationParameters.newBuilder()
                        .setEasing(Easing.newBuilder()
                                .setCubicBezier(CubicBezierEasing.newBuilder()
                                        .setX1(1.5f)
                                        .setY1(0.2f)
                                        .setX2(0.8f)
                                        .setY2(0.9f)
                                        .build())
                                .build())
                        .build())
                .build();

        Interpolator interpolator = AnimationsHelper.getMainInterpolatorOrDefault(spec);

        assertThat(interpolator).isInstanceOf(LinearInterpolator.class);
    }
}
