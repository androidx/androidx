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

package androidx.dynamicanimation.animation;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;

import java.util.ArrayList;

/**
 * This custom handler handles the timing pulse that is shared by all active ValueAnimators.
 * This approach ensures that the setting of animation values will happen on the
 * same thread that animations start on, and that all animations will share the same times for
 * calculating their values, which makes synchronizing animations possible.
 *
 * The handler uses the Choreographer by default for doing periodic callbacks. A custom
 * AnimationFrameCallbackProvider can be set on the handler to provide timing pulse that
 * may be independent of UI frame update. This could be useful in testing.
 */
public final class AnimationHandler {
    /**
     * Callbacks that receives notifications for animation timing
     */
    interface AnimationFrameCallback {
        /**
         * Run animation based on the frame time.
         *
         * @param frameTime The frame start time
         */
        boolean doAnimationFrame(long frameTime);
    }

    /**
     * A scheduler that runs the given Runnable on the next frame.
     */
    public interface FrameCallbackScheduler {
        /**
         * Callbacks on new frame arrived.
         *
         * @param frameCallback The runnable of new frame should be posted
         */
        void postFrameCallback(@NonNull Runnable frameCallback);

        /**
         * Returns whether the current thread is the same as the thread that the scheduler is
         * running on.
         *
         * @return true if the scheduler is running on the same thread as the current thread.
         */
        boolean isCurrentThread();
    }

    /**
     * This class is responsible for interacting with the available frame provider by either
     * registering frame callback or posting runnable, and receiving a callback for when a
     * new frame has arrived. This dispatcher class then notifies all the on-going animations of
     * the new frame, so that they can update animation values as needed.
     */
    private class AnimationCallbackDispatcher {
        /**
         * Notifies all the on-going animations of the new frame.
         */
        @SuppressWarnings("SyntheticAccessor") /* synthetic access */
        void dispatchAnimationFrame() {
            mCurrentFrameTime = SystemClock.uptimeMillis();
            AnimationHandler.this.doAnimationFrame(mCurrentFrameTime);
            if (mAnimationCallbacks.size() > 0) {
                mScheduler.postFrameCallback(mRunnable);
            }
        }
    }

    private static final long FRAME_DELAY_MS = 10;
    private static final ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();

    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     */
    private final SimpleArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime =
            new SimpleArrayMap<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    @SuppressWarnings("SyntheticAccessor") /* synthetic access */
    private final AnimationCallbackDispatcher mCallbackDispatcher =
            new AnimationCallbackDispatcher();
    @SuppressWarnings("SyntheticAccessor") /* synthetic access */
    private final Runnable mRunnable = () -> mCallbackDispatcher.dispatchAnimationFrame();
    @SuppressWarnings("SyntheticAccessor") /* synthetic access */
    private FrameCallbackScheduler mScheduler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mCurrentFrameTime = 0;
    private boolean mListDirty = false;

    static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            AnimationHandler handler = new AnimationHandler(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                            ? new FrameCallbackScheduler16()
                            : new FrameCallbackScheduler14());
            sAnimatorHandler.set(handler);
        }
        return sAnimatorHandler.get();
    }

    /**
     * The constructor of the AnimationHandler with {@link FrameCallbackScheduler} which is handle
     * running the given Runnable on the next frame.
     *
     * @param scheduler The scheduler for this handler to run the given runnable.
     */
    public AnimationHandler(@NonNull FrameCallbackScheduler scheduler) {
        mScheduler = scheduler;
    }

    /**
     * Register to get a callback on the next frame after the delay.
     */
    void addAnimationFrameCallback(final AnimationFrameCallback callback, long delay) {
        if (mAnimationCallbacks.size() == 0) {
            mScheduler.postFrameCallback(mRunnable);
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
    void removeCallback(AnimationFrameCallback callback) {
        mDelayedCallbackStartTime.remove(callback);
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            mAnimationCallbacks.set(id, null);
            mListDirty = true;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void doAnimationFrame(long frameTime) {
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
     * Returns whether the current thread is the same thread as the animation handler
     * frame scheduler.
     *
     * @return true the current thread is the same thread as the animation handler frame scheduler.
     */
    boolean isCurrentThread() {
        return mScheduler.isCurrentThread();
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
     * Sets the FrameCallbackScheduler for this handler.
     * Used in testing only.
     *
     * @param scheduler The FrameCallbackScheduler to set
     * @hide
     */

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setScheduler(@NonNull FrameCallbackScheduler scheduler) {
        mScheduler = scheduler;
    }

    /**
     * Gets the FrameCallbackScheduler in this handler.
     * Used in testing only.
     *
     * @return The FrameCallbackScheduler in this handler
     * @hide
     */
    @NonNull
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FrameCallbackScheduler getScheduler() {
        return mScheduler;
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @VisibleForTesting
    static final class FrameCallbackScheduler16 implements FrameCallbackScheduler {

        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Looper mLooper = Looper.myLooper();

        @Override
        public void postFrameCallback(@NonNull Runnable frameCallback) {
            mChoreographer.postFrameCallback(time -> frameCallback.run());
        }

        @Override
        public boolean isCurrentThread() {
            return Thread.currentThread() == mLooper.getThread();
        }
    }

    /**
     * Frame provider for ICS and ICS-MR1 releases. The frame callback is achieved via posting
     * a Runnable to the main thread Handler with a delay.
     */
    @VisibleForTesting
    static class FrameCallbackScheduler14 implements FrameCallbackScheduler {

        private final Handler mHandler = new Handler(Looper.myLooper());
        private long mLastFrameTime;

        @Override
        public void postFrameCallback(@NonNull Runnable frameCallback) {
            long delay = FRAME_DELAY_MS - (SystemClock.uptimeMillis() - mLastFrameTime);
            delay = Math.max(delay, 0);
            mHandler.postDelayed(() -> {
                mLastFrameTime = SystemClock.uptimeMillis();
                frameCallback.run();
            }, delay);
        }

        @Override
        public boolean isCurrentThread() {
            return Thread.currentThread() == mHandler.getLooper().getThread();
        }
    }
}
