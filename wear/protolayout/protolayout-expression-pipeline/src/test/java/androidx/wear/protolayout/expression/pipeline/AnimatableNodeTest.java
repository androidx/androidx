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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.animation.TypeEvaluator;
import android.graphics.Color;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AnimatableNodeTest {

    private final List<Float> mUpdateValues = new ArrayList<>();
    private final List<Integer> mUpdateColors = new ArrayList<>();

    private void assertAnimation(List<Float> values, float start, float end) {
        for (Float value : values) {
            assertThat(value).isIn(Range.closed(start, end));
        }
        assertThat(values.size()).isGreaterThan(2);
        if (start < end) {
            assertThat(values).isInOrder();
        } else {
            assertThat(values).isInOrder(Comparator.reverseOrder());
        }
        assertThat(values.get(0)).isEqualTo(start);
        assertThat(Iterables.getLast(values)).isEqualTo(end);
    }

    @Test
    public void infiniteAnimator_onlyStartsWhenNodeIsVisible() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(Integer.MAX_VALUE);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        quotaAwareAnimator.isInfiniteAnimator = true;

        assertThat(quotaAwareAnimator.isRunning()).isFalse();

        animNode.startOrSkipAnimator();
        assertThat(quotaAwareAnimator.isRunning()).isFalse();

        animNode.setVisibility(true);
        assertThat(quotaAwareAnimator.isRunning()).isTrue();
    }

    @Test
    public void infiniteAnimator_pausesWhenNodeIsInvisible() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(Integer.MAX_VALUE);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);

        quotaAwareAnimator.isInfiniteAnimator = true;

        animNode.setVisibility(true);
        assertThat(quotaAwareAnimator.isRunning()).isTrue();

        animNode.setVisibility(false);
        assertThat(quotaAwareAnimator.isPaused()).isTrue();

        animNode.setVisibility(true);
        assertThat(quotaAwareAnimator.isRunning()).isTrue();
    }

    @Test
    public void animator_noQuota_notPlayed() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(0);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        // Check that animator hasn't started because there is no quota.
        animNode.setVisibility(true);
        assertThat(quotaAwareAnimator.isRunning()).isFalse();
    }

    @Test
    public void animator_reused_withFloatValues() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(Integer.MAX_VALUE);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        animNode.setVisibility(true);
        animNode.startOrSkipAnimator();
        assertThat(quotaAwareAnimator.isRunning()).isTrue();

        shadowOf(Looper.getMainLooper()).idle();
        assertThat(Iterables.getLast(mUpdateValues)).isEqualTo(10.0f);
        assertThat(quotaAwareAnimator.isRunning()).isFalse();

        quotaAwareAnimator.setFloatValues(10.0f, 15.0f);
        animNode.startOrSkipAnimator();
        assertThat(quotaAwareAnimator.isRunning()).isTrue();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(Iterables.getLast(mUpdateValues)).isEqualTo(15.0f);
        assertThat(quotaAwareAnimator.isRunning()).isFalse();
    }

    @Test
    public void animator_reused_withColorValues() {
        mUpdateColors.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(Integer.MAX_VALUE);
        TestQuotaAwareAnimator quotaAwareAnimator =
                new TestQuotaAwareAnimator(quotaManager, AnimatableNode.ARGB_EVALUATOR);
        quotaAwareAnimator.setIntValues(Color.BLUE, Color.MAGENTA);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateColors.add((int) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        animNode.setVisibility(true);
        animNode.startOrSkipAnimator();
        assertThat(quotaAwareAnimator.isRunning()).isTrue();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat((int) Iterables.getLast(mUpdateColors)).isEqualTo(Color.MAGENTA);
        assertThat(quotaAwareAnimator.isRunning()).isFalse();

        quotaAwareAnimator.setIntValues(Color.MAGENTA, Color.DKGRAY);
        animNode.startOrSkipAnimator();
        assertThat(quotaAwareAnimator.isRunning()).isTrue();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat((int) Iterables.getLast(mUpdateColors)).isEqualTo(Color.DKGRAY);
        assertThat(quotaAwareAnimator.isRunning()).isFalse();
    }

    @Test
    public void animator_reuse_withFloatValues() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(Integer.MAX_VALUE);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        animNode.setVisibility(true);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // Quota available, update listener was called with interpolated values.
        assertAnimation(mUpdateValues, 0.0f, 10.0f);

        mUpdateValues.clear();
        quotaAwareAnimator.setFloatValues(10.0f, 15.0f);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // Quota available, update listener was called with interpolated values.
        assertAnimation(mUpdateValues, 10.0f, 15.0f);
    }

    @Test
    public void animator_reuse_noQuota() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(0);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        animNode.setVisibility(true);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // No quota available, update listener was called only once end values.
        assertThat(mUpdateValues).containsExactly(10.f);

        mUpdateValues.clear();
        quotaAwareAnimator.setFloatValues(10.0f, 15.0f);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // No quota available, update listender was called only once with end values.
        assertThat(mUpdateValues).containsExactly(15.0f);
    }

    @Test
    public void animator_reuse_noQuota_then_withQuota() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(1);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);
        animNode.setVisibility(true);

        // Occupy the single quota
        quotaManager.tryAcquireQuota(1);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // No quota available, update listender was called only once with end values.
        assertThat(mUpdateValues).containsExactly(10.0f);

        mUpdateValues.clear();
        // Release the single quota
        quotaManager.releaseQuota(1);
        quotaAwareAnimator.setFloatValues(10.0f, 15.0f);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // Quota available, update listener was called with interpolated values.
        assertAnimation(mUpdateValues, 10.0f, 15.0f);
    }

    @Test
    public void animator_reuse_withQuota_then_noQuota() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(1);
        TestQuotaAwareAnimator quotaAwareAnimator = new TestQuotaAwareAnimator(quotaManager);
        quotaAwareAnimator.setFloatValues(0.0f, 10.0f);
        quotaAwareAnimator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);
        animNode.setVisibility(true);

        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // Quota available, update listener was called with interpolated values.
        assertAnimation(mUpdateValues, 0.0f, 10.0f);

        mUpdateValues.clear();
        // Release the single quota
        quotaManager.tryAcquireQuota(1);
        quotaAwareAnimator.setFloatValues(13.0f, 15.0f);
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();
        // No quota available, update listener was called only once with end values.
        assertThat(mUpdateValues).containsExactly(15.0f);
    }

    @Test
    public void animationWithCustomReverseDuration_animatorWithAux() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(1);
        TestAnimatableNode animNode =
                new TestAnimatableNode(quotaManager, createAnimationSpec(4, 100, 200, 0, 0));
        QuotaAwareAnimator animator = animNode.getQuotaAwareAnimator();

        assertThat(animator).isInstanceOf(QuotaAwareAnimatorWithAux.class);
    }

    @Test
    public void animationWithCustomReverseDuration_consumeOneQuota() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(2);
        TestAnimatableNode animNode =
                new TestAnimatableNode(quotaManager, createAnimationSpec(2, 100, 200, 0, 0));
        animNode.setVisibility(true);
        QuotaAwareAnimator animator = animNode.getQuotaAwareAnimator();
        animator.setIntValues(0, 10);

        animNode.startOrSkipAnimator();
        assertThat(animator.isRunning()).isTrue();

        assertThat(quotaManager.tryAcquireQuota(1)).isTrue();
        assertThat(quotaManager.tryAcquireQuota(1)).isFalse();

        shadowOf(Looper.getMainLooper()).idle();
        assertThat(quotaManager.tryAcquireQuota(1)).isTrue();
    }

    @Test
    public void animationWithCustomReverseDuration_releaseQuotaWhenInvisible() {
        QuotaManager quotaManager = new FixedQuotaManagerImpl(1);
        TestAnimatableNode animNode =
                new TestAnimatableNode(quotaManager, createAnimationSpec(2, 100, 200, 0, 0));
        animNode.setVisibility(true);
        QuotaAwareAnimator animator = animNode.getQuotaAwareAnimator();
        animator.setIntValues(0, 10);

        animNode.startOrSkipAnimator();
        assertThat(animator.isRunning()).isTrue();
        assertThat(quotaManager.tryAcquireQuota(1)).isFalse();

        animNode.setVisibility(false);
        assertThat(quotaManager.tryAcquireQuota(1)).isTrue();
    }

    @Test
    public void animationWithCustomReverseDuration_checkAnimationValues() {
        mUpdateValues.clear();
        QuotaManager quotaManager = new FixedQuotaManagerImpl(1);
        TestAnimatableNode animNode =
                new TestAnimatableNode(quotaManager, createAnimationSpec(2, 100, 200, 0, 0));
        animNode.setVisibility(true);
        QuotaAwareAnimator animator = animNode.getQuotaAwareAnimator();

        float start = 1.0f;
        float end = 10.0f;
        animator.setFloatValues(start, end);
        animator.addUpdateCallback(a -> mUpdateValues.add((float) a));
        animNode.startOrSkipAnimator();
        shadowOf(Looper.getMainLooper()).idle();

        for (Float value : mUpdateValues) {
            assertThat(value).isIn(Range.closed(start, end));
        }
        assertThat(mUpdateValues.size()).isGreaterThan(3);
        assertThat(mUpdateValues.get(0)).isEqualTo(start);
        assertThat(Iterables.getLast(mUpdateValues)).isEqualTo(start);

        int increasingValueCount = 0;
        int decreasingValueCount = 0;
        for (int i = 0; i < mUpdateValues.size() - 2; i++) {
            if (mUpdateValues.get(i) < mUpdateValues.get(i + 1)) {
                increasingValueCount++;
            } else {
                decreasingValueCount++;
            }
        }
        // reverse duration is double length of forward duration, expecting decreasing value
        // count about
        // double of the increasing value count.
        assertThat(decreasingValueCount).isAtLeast((increasingValueCount - 1) * 2);
        assertThat(decreasingValueCount).isAtMost((increasingValueCount + 1) * 2);
    }

    static class TestAnimatableNode extends AnimatableNode {

        TestAnimatableNode(QuotaAwareAnimator quotaAwareAnimator) {
            super(quotaAwareAnimator);
        }

        TestAnimatableNode(@NonNull QuotaManager quotaManager, @NonNull AnimationSpec spec) {
            super(quotaManager, spec);
        }

        QuotaAwareAnimator getQuotaAwareAnimator() {
            return mQuotaAwareAnimator;
        }
    }

    static class TestQuotaAwareAnimator extends QuotaAwareAnimator {
        public boolean isInfiniteAnimator = false;

        TestQuotaAwareAnimator(@NonNull QuotaManager mQuotaManager) {
            super(mQuotaManager, AnimationSpec.getDefaultInstance());
        }

        @SuppressWarnings("rawtypes")
        TestQuotaAwareAnimator(@NonNull QuotaManager mQuotaManager,
                @NonNull TypeEvaluator evaluator) {
            super(mQuotaManager, AnimationSpec.getDefaultInstance(), evaluator);
        }

        @Override
        protected boolean isInfiniteAnimator() {
            return isInfiniteAnimator;
        }
    }

    @NonNull
    AnimationSpec createAnimationSpec(
            int iterations,
            int forwardDuration,
            int reverseDuration,
            int forwardRepeatDelay,
            int reverseRepeatDelay) {
        return AnimationSpec.newBuilder()
                .setAnimationParameters(
                        AnimationParameters.newBuilder()
                                .setDurationMillis(forwardDuration)
                                .build())
                .setRepeatable(
                        Repeatable.newBuilder()
                                .setRepeatMode(
                                        AnimationParameterProto.RepeatMode.REPEAT_MODE_REVERSE)
                                .setForwardRepeatOverride(
                                        AnimationParameters.newBuilder()
                                                .setDelayMillis(forwardRepeatDelay)
                                                .build())
                                .setReverseRepeatOverride(
                                        AnimationParameters.newBuilder()
                                                .setDurationMillis(reverseDuration)
                                                .setDelayMillis(reverseRepeatDelay)
                                                .build()
                                )
                                .setIterations(iterations)
                                .build())
                .build();
    }
}
