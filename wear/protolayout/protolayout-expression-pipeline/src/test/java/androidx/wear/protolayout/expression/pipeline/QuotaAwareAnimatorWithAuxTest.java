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

import static org.junit.Assert.assertThrows;

import android.animation.FloatEvaluator;
import android.animation.IntEvaluator;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class QuotaAwareAnimatorWithAuxTest {
    private static final int ANIMATION_DURATION = 500;
    private static final int ANIMATION_START_DELAY = 100;
    private static final float[] FLOAT_VALUES = {0f, 5f, 10f};
    private static final int[] INT_VALUES = {0, 50, 100};

    @Mock private QuotaManager mMockQuotaManager;

    private AnimationSpec mDefaultAnimationSpec;
    private AnimationSpec mAnimationSpecWithDuration;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        // Create AnimationSpec instances
        mDefaultAnimationSpec = AnimationSpec.newBuilder().build();
        mAnimationSpecWithDuration =
                AnimationSpec.newBuilder()
                        .setDurationMillis(ANIMATION_DURATION)
                        .setStartDelayMillis(ANIMATION_START_DELAY)
                        .build();
    }

    @Test
    public void getTypeEvaluator_returnsCorrectEvaluator_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new FloatEvaluator());

        assertThat(animator.getTypeEvaluator()).isInstanceOf(FloatEvaluator.class);
    }

    @Test
    public void setFloatValues_updatesBothAnimators_withAux() {
        long mainDuration = 300L;
        long auxDuration = 400L;
        long auxStartDelay = 300L;

        AnimationSpec mainSpec =
                AnimationSpec.newBuilder().setDurationMillis((int) mainDuration).build();
        AnimationSpec auxSpec =
                AnimationSpec.newBuilder()
                        .setDurationMillis((int) auxDuration)
                        .setStartDelayMillis((int) auxStartDelay)
                        .build();

        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mainSpec, auxSpec, new FloatEvaluator());

        animator.setFloatValues(FLOAT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        // Check main animator at the beginning
        animator.advanceToAnimationTime(0);
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[0]);

        // Check main animator at the end
        animator.advanceToAnimationTime(mainDuration);
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[FLOAT_VALUES.length - 1]);

        // Check aux animator at the beginning (should be the reversed end value of the main
        // animator)
        animator.advanceToAnimationTime(auxStartDelay);
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[FLOAT_VALUES.length - 1]);

        // Check aux animator at the end (should be the reversed start value of the main animator)
        animator.advanceToAnimationTime(auxStartDelay + auxDuration);
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[0]);
    }

    @Test
    public void setFloatValues_cancelsBothAnimators_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new FloatEvaluator());
        animator.tryStartAnimation(); // Simulate running animators

        animator.setFloatValues(FLOAT_VALUES);

        assertThat(animator.isRunning()).isFalse();
    }

    @Test
    public void setFloatValues_throwsExceptionWithIncorrectEvaluator_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new IntEvaluator());

        assertThrows(IllegalArgumentException.class, () -> animator.setFloatValues(FLOAT_VALUES));
    }

    @Test
    public void setIntValues_updatesBothAnimators_withAux() {
        long mainDuration = 300L;
        long auxDuration = 400L;
        long auxStartDelay = 300L;

        AnimationSpec mainSpec =
                AnimationSpec.newBuilder().setDurationMillis((int) mainDuration).build();
        AnimationSpec auxSpec =
                AnimationSpec.newBuilder()
                        .setDurationMillis((int) auxDuration)
                        .setStartDelayMillis((int) auxStartDelay)
                        .build();

        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mainSpec, auxSpec, new IntEvaluator());

        animator.setIntValues(INT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        // Check main animator at the beginning
        animator.advanceToAnimationTime(0);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[0]);

        // Check main animator at the end
        animator.advanceToAnimationTime(mainDuration);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[INT_VALUES.length - 1]);

        // Check aux animator at the beginning (should be the reversed end value of the main
        // animator)
        animator.advanceToAnimationTime(auxStartDelay);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[INT_VALUES.length - 1]);

        // Check aux animator at the end (should be the reversed start value of the main animator)
        animator.advanceToAnimationTime(auxStartDelay + auxDuration);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[0]);
    }

    @Test
    public void setIntValues_throwsExceptionWithIncorrectEvaluator_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new FloatEvaluator());

        assertThrows(IllegalArgumentException.class, () -> animator.setIntValues(INT_VALUES));
    }

    @Test
    public void setIntValues_worksWithArgbEvaluator_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager,
                        mDefaultAnimationSpec,
                        auxSpec,
                        AnimatableNode.ARGB_EVALUATOR);

        animator.setIntValues(INT_VALUES); // Should not throw an exception
    }

    @Test
    public void getPropertyValuesHolders_returnsMainAnimatorValues_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new FloatEvaluator());
        animator.setFloatValues(FLOAT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        Object startValue = animator.getStartValue();
        Object endValue = animator.getEndValue();

        assertThat(startValue).isEqualTo(FLOAT_VALUES[0]);
        assertThat(endValue).isEqualTo(FLOAT_VALUES[FLOAT_VALUES.length - 1]);
    }

    @Test
    public void getCurrentValue_returnsCorrectValue_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager, mDefaultAnimationSpec, auxSpec, new FloatEvaluator());
        animator.setFloatValues(0f, 10f);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback
        animator.mAnimator.setCurrentPlayTime(150); // Halfway through the default 300ms duration

        Object lastValue = animator.getCurrentValue();

        assertThat(lastValue).isEqualTo(5f); // Expected interpolated value from main animator
    }

    @Test
    public void getDuration_returnsCombinedDuration_Ms_withAux() {
        long auxDuration = 400L;
        AnimationSpec auxSpec =
                AnimationSpec.newBuilder().setDurationMillis((int) auxDuration).build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager,
                        mAnimationSpecWithDuration,
                        auxSpec,
                        new FloatEvaluator());

        long duration = animator.getDurationMs();

        assertThat(duration).isEqualTo(ANIMATION_DURATION + auxDuration);
    }

    @Test
    public void getStartDelay_returnsMainAnimatorStartDelay_Ms_withAux() {
        AnimationSpec auxSpec = AnimationSpec.newBuilder().build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager,
                        mAnimationSpecWithDuration,
                        auxSpec,
                        new FloatEvaluator());

        long startDelay = animator.getStartDelayMs();

        assertThat(startDelay)
                .isEqualTo(ANIMATION_START_DELAY); // Should return main animator's start delay
    }

    @Test
    public void advanceToAnimationTime_setsCurrentPlayTime_onMainAnimator() {
        long auxStartDelay = 300L;
        AnimationSpec auxSpec =
                AnimationSpec.newBuilder().setStartDelayMillis((int) auxStartDelay).build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager,
                        mAnimationSpecWithDuration,
                        auxSpec,
                        new FloatEvaluator());
        animator.setFloatValues(0f, 10f); // Set values for both animators
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        long newTime = 200L; // Less than aux animator's start delay

        animator.advanceToAnimationTime(newTime);

        // Indirectly verify main animator's current play time
        assertThat(animator.getCurrentValue())
                .isEqualTo(
                        new FloatEvaluator()
                                .evaluate(
                                        (newTime - ANIMATION_START_DELAY)
                                                / (float) ANIMATION_DURATION,
                                        0f,
                                        10f));
    }

    @Test
    public void advanceToAnimationTime_setsCurrentPlayTime_onAuxAnimator() {
        long auxStartDelay = 300L;
        long auxDuration = 400L;
        AnimationSpec auxSpec =
                AnimationSpec.newBuilder()
                        .setStartDelayMillis((int) auxStartDelay)
                        .setDurationMillis((int) auxDuration)
                        .build();
        QuotaAwareAnimatorWithAux animator =
                new QuotaAwareAnimatorWithAux(
                        mMockQuotaManager,
                        mAnimationSpecWithDuration,
                        auxSpec,
                        new FloatEvaluator());
        animator.setFloatValues(0f, 10f); // Set values for both animators
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        long newTime = 400L; // Greater than or equal to aux animator's start delay

        animator.advanceToAnimationTime(newTime);

        // Indirectly verify auxiliary animator's current play time
        assertThat(animator.getCurrentValue())
                .isEqualTo(
                        new FloatEvaluator()
                                .evaluate(
                                        (newTime - auxStartDelay) / (float) auxDuration,
                                        10f,
                                        0f)); // Reversed values in aux animator
    }
}
