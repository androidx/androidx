/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.animation;

import android.os.Looper;
import android.os.SystemClock;
import android.util.AndroidRuntimeException;

import androidx.annotation.NonNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit {@link TestRule} that can be used to run {@link Animator}s without actually waiting for the
 * duration of the animation. This also helps the test to be written in a deterministic manner.
 *
 * Create an instance of {@code AnimatorTestRule} and specify it as a {@link org.junit.ClassRule}
 * of the test class. Use {@link #advanceTimeBy(long)} to advance animators that have been started.
 * Note that {@link #advanceTimeBy(long)} should be called from the same thread you have used to
 * start the animator.
 *
 * <pre>
 * {@literal @}SmallTest
 * {@literal @}RunWith(AndroidJUnit4.class)
 * public class SampleAnimatorTest {
 *
 *     {@literal @}ClassRule
 *     public static AnimatorTestRule sAnimatorTestRule = new AnimatorTestRule();
 *
 *     {@literal @}UiThreadTest
 *     {@literal @}Test
 *     public void sample() {
 *         final ValueAnimator animator = ValueAnimator.ofInt(0, 1000);
 *         animator.setDuration(1000L);
 *         assertThat(animator.getAnimatedValue(), is(0));
 *         animator.start();
 *         sAnimatorTestRule.advanceTimeBy(500L);
 *         assertThat(animator.getAnimatedValue(), is(500));
 *     }
 * }
 * </pre>
 */
public final class AnimatorTestRule implements TestRule {

    final AnimationHandler mTestHandler;
    final long mStartTime;
    private long mTotalTimeDelta = 0;
    private final Object mLock = new Object();

    public AnimatorTestRule() {
        mStartTime = SystemClock.uptimeMillis();
        mTestHandler = new AnimationHandler(new TestProvider());
    }

    @NonNull
    @Override
    public Statement apply(@NonNull final Statement base, @NonNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AnimationHandler.setTestHandler(mTestHandler);
                try {
                    base.evaluate();
                } finally {
                    AnimationHandler.setTestHandler(null);
                }
            }
        };
    }

    /**
     * Advances the animation clock by the given amount of delta in milliseconds. This call will
     * produce an animation frame to all the ongoing animations. This method needs to be
     * called on the same thread as {@link Animator#start()}.
     *
     * @param timeDelta the amount of milliseconds to advance
     */
    public void advanceTimeBy(long timeDelta) {
        if (Looper.myLooper() == null) {
            // Throw an exception
            throw new AndroidRuntimeException("AnimationTestRule#advanceTimeBy(long) may only be"
                    + "called on Looper threads");
        }
        synchronized (mLock) {
            // Advance time & pulse a frame
            mTotalTimeDelta += timeDelta < 0 ? 0 : timeDelta;
        }
        // produce a frame
        mTestHandler.onAnimationFrame(getCurrentTime());
    }


    /**
     * Returns the current time in milliseconds tracked by AnimationHandler. Note that this is a
     * different time than the time tracked by @{link SystemClock} This method needs to be called on
     * the same thread as {@link Animator#start()}.
     */
    public long getCurrentTime() {
        if (Looper.myLooper() == null) {
            // Throw an exception
            throw new AndroidRuntimeException("AnimationTestRule#getCurrentTime() may only be"
                    + "called on Looper threads");
        }
        synchronized (mLock) {
            return mStartTime + mTotalTimeDelta;
        }
    }


    private class TestProvider implements AnimationHandler.AnimationFrameCallbackProvider {
        TestProvider() {}

        @Override
        public void onNewCallbackAdded(AnimationHandler.AnimationFrameCallback callback) {
            callback.doAnimationFrame(getCurrentTime());
        }

        @Override
        public void postFrameCallback() {
        }

        @Override
        public void setFrameDelay(long delay) {
        }

        @Override
        public long getFrameDelay() {
            return 0;
        }
    }
}

