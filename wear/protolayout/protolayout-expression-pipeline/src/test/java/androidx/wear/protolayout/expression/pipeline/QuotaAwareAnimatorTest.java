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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.ArgbEvaluator;
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
public class QuotaAwareAnimatorTest {

    private static final int ANIMATION_DURATION = 500;
    private static final int ANIMATION_START_DELAY = 100;
    private static final float[] FLOAT_VALUES = {0f, 5f, 10f};
    private static final int[] INT_VALUES = {0, 50, 100};
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock private QuotaManager mMockQuotaManager;
    private AnimationSpec mDefaultAnimationSpec;
    private AnimationSpec mAnimationSpecWithDuration;

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
    public void getTypeEvaluator_returnsCorrectEvaluator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());

        assertThat(animator.getTypeEvaluator()).isInstanceOf(FloatEvaluator.class);
    }

    @Test
    public void setFloatValues_updatesAnimatorValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());

        animator.setFloatValues(FLOAT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        // Check the animated value at the beginning of the animation
        animator.advanceToAnimationTime(0);
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[0]);

        // Check the animated value at the end of the animation
        animator.advanceToAnimationTime(ANIMATION_DURATION); // Assuming default 300ms duration
        assertThat(animator.getCurrentValue()).isEqualTo(FLOAT_VALUES[FLOAT_VALUES.length - 1]);
    }

    @Test
    public void setFloatValues_cancelsAnimator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());
        animator.mAnimator.start(); // Simulate running animator

        animator.setFloatValues(FLOAT_VALUES);

        assertThat(animator.mAnimator.isStarted()).isFalse();
    }

    @Test
    public void setFloatValues_throwsExceptionWithIncorrectEvaluator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new IntEvaluator());

        assertThrows(IllegalArgumentException.class, () -> animator.setFloatValues(FLOAT_VALUES));
    }

    @Test
    public void setIntValues_updatesAnimatorValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new IntEvaluator());

        animator.setIntValues(INT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        // Check the animated value at the beginning of the animation
        animator.advanceToAnimationTime(0);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[0]);

        // Check the animated value at the end of the animation
        animator.advanceToAnimationTime(ANIMATION_DURATION);
        assertThat(animator.getCurrentValue()).isEqualTo(INT_VALUES[INT_VALUES.length - 1]);
    }

    @Test
    public void setIntValues_cancelsAnimator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new IntEvaluator());
        animator.mAnimator.start(); // Simulate running animator

        animator.setIntValues(INT_VALUES);

        assertThat(animator.mAnimator.isStarted()).isFalse();
    }

    @Test
    public void setIntValues_throwsExceptionWithIncorrectEvaluator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());

        assertThrows(IllegalArgumentException.class, () -> animator.setIntValues(INT_VALUES));
    }

    @Test
    public void setIntValues_worksWithArgbEvaluator() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new ArgbEvaluator());

        animator.setIntValues(INT_VALUES); // Should not throw an exception
    }

    @Test
    public void advanceToAnimationTime_setsCurrentPlayTime() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new FloatEvaluator());
        long newTime = 250L;

        animator.advanceToAnimationTime(newTime);

        assertThat(animator.mAnimator.getCurrentPlayTime())
                .isEqualTo(newTime - ANIMATION_START_DELAY);
    }

    @Test
    public void getPropertyValuesHolders_returnsAnimatorValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());
        animator.setFloatValues(FLOAT_VALUES);

        Object startValue = animator.getStartValue();
        Object endValue = animator.getEndValue();

        assertThat(startValue).isEqualTo(FLOAT_VALUES[0]);
        assertThat(endValue).isEqualTo(FLOAT_VALUES[FLOAT_VALUES.length - 1]);
    }

    @Test
    public void getCurrentValue_returnsCorrectValue() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());
        animator.setFloatValues(0f, 10f);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback
        animator.mAnimator.setCurrentPlayTime(150); // Halfway through the default 300ms duration

        Object lastValue = animator.getCurrentValue();

        assertThat(lastValue).isEqualTo(5f); // Expected interpolated value
    }

    @Test
    public void getDuration_returnsAnimatorDurationMs() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new FloatEvaluator());

        long duration = animator.getDurationMs();

        assertThat(duration).isEqualTo(ANIMATION_DURATION);
    }

    @Test
    public void getStartDelay_returnsAnimatorStartDelayMs() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new FloatEvaluator());

        long startDelay = animator.getStartDelayMs();

        assertThat(startDelay).isEqualTo(ANIMATION_START_DELAY);
    }

    @Test
    public void tryStartAnimation_acquiresQuotaAndStarts_whenQuotaAvailable() {
        when(mMockQuotaManager.tryAcquireQuota(anyInt()))
                .thenReturn(true); // Simulate quota available
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mDefaultAnimationSpec, new FloatEvaluator());
        animator.setFloatValues(FLOAT_VALUES);
        animator.addUpdateCallback(value -> {}); // Add an empty update callback

        animator.tryStartAnimation();

        verify(mMockQuotaManager).tryAcquireQuota(1); // Verify quota acquisition
        assertThat(animator.mAnimator.isStarted()).isTrue(); // Verify animator started
        assertThat(animator.mListener.mIsUsingQuota.get()).isTrue(); // Verify quota flag is set
    }

    @Test
    public void testStartAndEndValueCaching_FloatValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new FloatEvaluator());
        float[] values = {10f, 20f, 30f};
        animator.setFloatValues(values);

        assertEquals(10f, animator.getStartValue());
        assertEquals(30f, animator.getEndValue());
    }

    @Test
    public void testStartAndEndValueCaching_IntValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new IntEvaluator());
        int[] values = {5, 15, 25};
        animator.setIntValues(values);

        assertEquals(5, animator.getStartValue());
        assertEquals(25, animator.getEndValue());
    }

    @Test
    public void testStartAndEndValue_BeforeSettingValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new FloatEvaluator());
        assertNull(animator.getStartValue());
        assertNull(animator.getEndValue());
    }

    @Test
    public void testStartAndEndValue_AfterResettingValues() {
        QuotaAwareAnimator animator =
                new QuotaAwareAnimator(
                        mMockQuotaManager, mAnimationSpecWithDuration, new IntEvaluator());
        animator.setIntValues(1, 2);
        animator.setIntValues(3, 4);

        assertEquals(3, animator.getStartValue());
        assertEquals(4, animator.getEndValue());
    }
}
