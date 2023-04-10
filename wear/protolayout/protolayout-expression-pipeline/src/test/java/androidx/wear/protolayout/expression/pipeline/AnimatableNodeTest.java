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

import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AnimatableNodeTest {

    @Test
    public void infiniteAnimator_onlyStartsWhenNodeIsVisible() {
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 10.0f);
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        TestQuotaAwareAnimator quotaAwareAnimator =
                new TestQuotaAwareAnimator(animator, quotaManager);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        quotaAwareAnimator.isInfiniteAnimator = true;

        assertThat(animator.isRunning()).isFalse();

        animNode.startOrSkipAnimator();
        assertThat(animator.isRunning()).isFalse();

        animNode.setVisibility(true);
        assertThat(animator.isRunning()).isTrue();
    }

    @Test
    public void infiniteAnimator_pausesWhenNodeIsInvisible() {
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 10.0f);
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        TestQuotaAwareAnimator quotaAwareAnimator =
                new TestQuotaAwareAnimator(animator, quotaManager);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        quotaAwareAnimator.isInfiniteAnimator = true;

        animNode.setVisibility(true);
        assertThat(animator.isRunning()).isTrue();

        animNode.setVisibility(false);
        assertThat(animator.isPaused()).isTrue();

        animNode.setVisibility(true);
        assertThat(animator.isRunning()).isTrue();
    }

    @Test
    public void animator_noQuota_notPlayed() {
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 10.0f);
        QuotaManager quotaManager = new TestNoQuotaManagerImpl();
        TestQuotaAwareAnimator quotaAwareAnimator =
                new TestQuotaAwareAnimator(animator, quotaManager);
        TestAnimatableNode animNode = new TestAnimatableNode(quotaAwareAnimator);

        // Check that animator hasn't started because there is no quota.
        animNode.setVisibility(true);
        assertThat(animator.isStarted()).isFalse();
        assertThat(animator.isRunning()).isFalse();
    }

    static class TestAnimatableNode extends AnimatableNode {

        TestAnimatableNode(QuotaAwareAnimator quotaAwareAnimator) {
            super(quotaAwareAnimator);
        }
    }

    static class TestQuotaAwareAnimator extends QuotaAwareAnimator {
        public boolean isInfiniteAnimator = false;

        TestQuotaAwareAnimator(
                @Nullable ValueAnimator animator, @NonNull QuotaManager mQuotaManager) {
            super(animator, mQuotaManager);
        }

        @Override
        protected boolean isInfiniteAnimator() {
            return isInfiniteAnimator;
        }
    }
}
