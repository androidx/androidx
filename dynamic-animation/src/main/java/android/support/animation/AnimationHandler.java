/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.animation;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.util.SimpleArrayMap;
import android.view.Choreographer;

import java.util.ArrayList;

/**
 * This custom, static handler handles the timing pulse that is shared by all active
 * ValueAnimators. This approach ensures that the setting of animation values will happen on the
 * same thread that animations start on, and that all animations will share the same times for
 * calculating their values, which makes synchronizing animations possible.
 *
 * The handler uses the Choreographer by default for doing periodic callbacks. A custom
 * AnimationFrameCallbackProvider can be set on the handler to provide timing pulse that
 * may be independent of UI frame update. This could be useful in testing.
 *
 * @hide
 */
class AnimationHandler {
    /**
     * Callbacks that receives notifications for animation timing
     */
    interface AnimationFrameCallback {
        /**
         * Run animation based on the frame time.
         * @param frameTime The frame start time
         */
        boolean doAnimationFrame(long frameTime);
    }

    /**
     * This class is responsible for interacting with the available frame provider by either
     * registering frame callback or posting runnable, and receiving a callback for when a
     * new frame has arrived. This dispatcher class then notifies all the on-going animations of
     * the new frame, so that they can update animation values as needed.
     */
    class AnimationCallbackDispatcher {
        public void dispatchAnimationFrame() {
            mCurrentFrameTime = SystemClock.uptimeMillis();
            AnimationHandler.this.doAnimationFrame(mCurrentFrameTime);
            if (mAnimationCallbacks.size() > 0) {
                getProvider().postFrameCallback();
            }
        }
    }

    private static final long FRAME_DELAY_MS = 10;
    public static final ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();

    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     * @hide
     */
    private final SimpleArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime =
            new SimpleArrayMap<>();
    private final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final AnimationCallbackDispatcher mCallbackDispatcher =
            new AnimationCallbackDispatcher();

    private AnimationFrameCallbackProvider mProvider;
    private long mCurrentFrameTime = 0;
    private boolean mListDirty = false;

    public static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            sAnimatorHandler.set(new AnimationHandler());
        }
        return sAnimatorHandler.get();
    }

    public static long getFrameTime() {
        if (sAnimatorHandler.get() == null) {
            return 0;
        }
        return sAnimatorHandler.get().mCurrentFrameTime;
    }

    /**
     * By default, the Choreographer is used to provide timing for frame callbacks. A custom
     * provider can be used here to provide different timing pulse.
     */
    public void setProvider(AnimationFrameCallbackProvider provider) {
        mProvider = provider;
    }

    private AnimationFrameCallbackProvider getProvider() {
        if (mProvider == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mProvider = new FrameCallbackProvider16(mCallbackDispatcher);
            } else {
                mProvider = new FrameCallbackProvider14(mCallbackDispatcher);
            }
        }
        return mProvider;
    }

    /**
     * Register to get a callback on the next frame after the delay.
     */
    public void addAnimationFrameCallback(final AnimationFrameCallback callback, long delay) {
        if (mAnimationCallbacks.size() == 0) {
            getProvider().postFrameCallback();
        }
        if (!mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks.add(callback);
        }

        if (delay > 0) {
            mDelayedCallbackStartTime.put(callback, (SystemClock.uptimeMillis() + delay));
        }
    }
    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     */
    public void removeCallback(AnimationFrameCallback callback) {
        mDelayedCallbackStartTime.remove(callback);
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            mAnimationCallbacks.set(id, null);
            mListDirty = true;
        }
    }

    private void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        for (int i = 0; i < mAnimationCallbacks.size(); i++) {
            final AnimationFrameCallback callback = mAnimationCallbacks.get(i);
            if (callback == null) {
                continue;
            }
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
        cleanUpList();
    }

    /**
     * Remove the callbacks from mDelayedCallbackStartTime once they have passed the initial delay
     * so that they can start getting frame callbacks.
     *
     * @return true if they have passed the initial delay or have no delay, false otherwise.
     */
    private boolean isCallbackDue(AnimationFrameCallback callback, long currentTime) {
        Long startTime = mDelayedCallbackStartTime.get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime < currentTime) {
            mDelayedCallbackStartTime.remove(callback);
            return true;
        }
        return false;
    }

    private void cleanUpList() {
        if (mListDirty) {
            for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (mAnimationCallbacks.get(i) == null) {
                    mAnimationCallbacks.remove(i);
                }
            }
            mListDirty = false;
        }
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class FrameCallbackProvider16 extends AnimationFrameCallbackProvider {

        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Choreographer.FrameCallback mChoreographerCallback;

        FrameCallbackProvider16(AnimationCallbackDispatcher dispatcher) {
            super(dispatcher);
            mChoreographerCallback = new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNanos) {
                        mDispatcher.dispatchAnimationFrame();
                    }
                };
        }

        @Override
        void postFrameCallback() {
            mChoreographer.postFrameCallback(mChoreographerCallback);
        }
    }

    /**
     * Frame provider for ICS and ICS-MR1 releases. The frame callback is achieved via posting
     * a Runnable to the main thread Handler with a delay.
     */
    private static class FrameCallbackProvider14 extends AnimationFrameCallbackProvider {

        private final Runnable mRunnable;
        private final Handler mHandler;
        private long mLastFrameTime = -1;

        FrameCallbackProvider14(AnimationCallbackDispatcher dispatcher) {
            super(dispatcher);
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    mLastFrameTime = SystemClock.uptimeMillis();
                    mDispatcher.dispatchAnimationFrame();
                }
            };
            mHandler = new Handler(Looper.myLooper());
        }

        @Override
        void postFrameCallback() {
            long delay = FRAME_DELAY_MS - (SystemClock.uptimeMillis() - mLastFrameTime);
            delay = Math.max(delay, 0);
            mHandler.postDelayed(mRunnable, delay);
        }
    }

    /**
     * The intention for having this interface is to increase the testability of ValueAnimator.
     * Specifically, we can have a custom implementation of the interface below and provide
     * timing pulse without using Choreographer. That way we could use any arbitrary interval for
     * our timing pulse in the tests.
     */
    public abstract static class AnimationFrameCallbackProvider {
        final AnimationCallbackDispatcher mDispatcher;
        AnimationFrameCallbackProvider(AnimationCallbackDispatcher dispatcher) {
            mDispatcher = dispatcher;
        }

        abstract void postFrameCallback();
    }
}
