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

package androidx.animation;

import android.os.Looper;
import android.os.SystemClock;
import android.util.AndroidRuntimeException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TODO: Mention caveat: 1) setFrameDelay/getFrameDelay will not be sensible.
 * 2) advanceTimeBy/getCurrentTime needs to be called on the same thread as where anims are started
 *
 */
public class AnimationTestRule implements TestRule {
    final AnimationHandler mTestHandler;
    final long mStartTime;
    private long mTotalTimeDelta = 0;
    private final Object mLock = new Object();

    public AnimationTestRule() {
        mStartTime = SystemClock.uptimeMillis();
        mTestHandler = new AnimationHandler(new TestProvider());
    }

    @Override
    public Statement apply(final Statement base, Description description) {
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

