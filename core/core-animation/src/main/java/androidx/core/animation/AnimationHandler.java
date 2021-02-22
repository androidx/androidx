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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;

import androidx.annotation.RequiresApi;
import androidx.collection.SimpleArrayMap;

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
     * This method notifies all the on-going animations of the new frame, so that
     * they can update animation values as needed.
     */
    void onAnimationFrame(long frameTime) {
        AnimationHandler.this.doAnimationFrame(frameTime);
        if (getAnimationCallbacks().size() > 0) {
            mProvider.postFrameCallback();
        }
    }
    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     */

    static class AnimationCallbackData {
        final SimpleArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime =
                new SimpleArrayMap<>();
        final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
        boolean mListDirty = false;
    }

    public static AnimationHandler sAnimationHandler = null;
    private static AnimationHandler sTestHandler = null;
    private static final ThreadLocal<AnimationCallbackData> sAnimationCallbackData =
            new ThreadLocal<>();
    private final AnimationFrameCallbackProvider mProvider;

    AnimationHandler(AnimationFrameCallbackProvider provider) {
        if (provider == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mProvider = new FrameCallbackProvider16();
            } else {
                mProvider = new FrameCallbackProvider14(this);
            }
        } else {
            mProvider = provider;
        }
    }

    public static AnimationHandler getInstance() {
        if (sTestHandler != null) {
            return sTestHandler;
        }
        if (sAnimationHandler == null) {
            sAnimationHandler = new AnimationHandler(null);
        }
        return sAnimationHandler;
    }

    static void setTestHandler(AnimationHandler handler) {
        sTestHandler = handler;
    }

    void setFrameDelay(long frameDelay) {
        mProvider.setFrameDelay(frameDelay);
    }

    long getFrameDelay() {
        return mProvider.getFrameDelay();
    }

    private SimpleArrayMap<AnimationFrameCallback, Long> getDelayedCallbackStartTime() {
        AnimationCallbackData data = sAnimationCallbackData.get();
        if (data == null) {
            data = new AnimationCallbackData();
            sAnimationCallbackData.set(data);
        }

        return data.mDelayedCallbackStartTime;
    }

    private ArrayList<AnimationFrameCallback> getAnimationCallbacks() {
        AnimationCallbackData data = sAnimationCallbackData.get();
        if (data == null) {
            data = new AnimationCallbackData();
            sAnimationCallbackData.set(data);
        }

        return data.mAnimationCallbacks;
    }

    private boolean isListDirty() {
        AnimationCallbackData data = sAnimationCallbackData.get();
        if (data == null) {
            data = new AnimationCallbackData();
            sAnimationCallbackData.set(data);
        }

        return data.mListDirty;
    }

    private void setListDirty(boolean dirty) {
        AnimationCallbackData data = sAnimationCallbackData.get();
        if (data == null) {
            data = new AnimationCallbackData();
            sAnimationCallbackData.set(data);
        }

        data.mListDirty = dirty;
    }

    /**
     * Register to get a callback on the next frame after the delay.
     */
    void addAnimationFrameCallback(final AnimationFrameCallback callback) {
        if (getAnimationCallbacks().size() == 0) {
            mProvider.postFrameCallback();
        }
        if (!getAnimationCallbacks().contains(callback)) {
            getAnimationCallbacks().add(callback);
        }
        mProvider.onNewCallbackAdded(callback);
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     */
    public void removeCallback(AnimationFrameCallback callback) {
        getDelayedCallbackStartTime().remove(callback);
        int id = getAnimationCallbacks().indexOf(callback);
        if (id >= 0) {
            getAnimationCallbacks().set(id, null);
            setListDirty(true);
        }
    }

    void autoCancelBasedOn(ObjectAnimator objectAnimator) {
        for (int i = getAnimationCallbacks().size() - 1; i >= 0; i--) {
            AnimationFrameCallback cb = getAnimationCallbacks().get(i);
            if (cb == null) {
                continue;
            }
            if (objectAnimator.shouldAutoCancel(cb)) {
                ((Animator) getAnimationCallbacks().get(i)).cancel();
            }
        }
    }

    private void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        for (int i = 0; i < getAnimationCallbacks().size(); i++) {
            final AnimationFrameCallback callback = getAnimationCallbacks().get(i);
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
        Long startTime = getDelayedCallbackStartTime().get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime < currentTime) {
            getDelayedCallbackStartTime().remove(callback);
            return true;
        }
        return false;
    }

    private void cleanUpList() {
        if (isListDirty()) {
            for (int i = getAnimationCallbacks().size() - 1; i >= 0; i--) {
                if (getAnimationCallbacks().get(i) == null) {
                    getAnimationCallbacks().remove(i);
                }
            }
            setListDirty(false);
        }
    }

    private int getCallbackSize() {
        int count = 0;
        int size = getAnimationCallbacks().size();
        for (int i = size - 1; i >= 0; i--) {
            if (getAnimationCallbacks().get(i) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Return the number of callbacks that have registered for frame callbacks.
     */
    public static int getAnimationCount() {
        AnimationHandler handler = AnimationHandler.getInstance();
        if (handler == null) {
            return 0;
        }
        return handler.getCallbackSize();
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private class FrameCallbackProvider16 implements AnimationFrameCallbackProvider,
            Choreographer.FrameCallback {

        FrameCallbackProvider16() {
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            onAnimationFrame(frameTimeNanos / 1000000);
        }

        @Override
        public void onNewCallbackAdded(AnimationFrameCallback callback) {}

        @Override
        public void postFrameCallback() {
            Choreographer.getInstance().postFrameCallback(this);
        }

        @Override
        public void setFrameDelay(long delay) {
            android.animation.ValueAnimator.setFrameDelay(delay);
        }

        @Override
        public long getFrameDelay() {
            return android.animation.ValueAnimator.getFrameDelay();
        }
    }

    /**
     * Frame provider for ICS and ICS-MR1 releases. The frame callback is achieved via posting
     * a Runnable to the main thread Handler with a delay.
     */
    private static class FrameCallbackProvider14 implements AnimationFrameCallbackProvider,
            Runnable {

        private static final ThreadLocal<Handler> sHandler = new ThreadLocal<>();
        private long mLastFrameTime = -1;
        private long mFrameDelay = 16;
        AnimationHandler mAnimationHandler;

        FrameCallbackProvider14(AnimationHandler animationHandler) {
            mAnimationHandler = animationHandler;
        }

        Handler getHandler() {
            if (sHandler.get() == null) {
                sHandler.set(new Handler(Looper.myLooper()));
            }
            return sHandler.get();
        }

        @Override
        public void run() {
            mLastFrameTime = SystemClock.uptimeMillis();
            mAnimationHandler.onAnimationFrame(mLastFrameTime);
        }

        @Override
        public void onNewCallbackAdded(AnimationFrameCallback callback) {}

        @Override
        public void postFrameCallback() {
            long delay = mFrameDelay - (SystemClock.uptimeMillis()
                    - mLastFrameTime);
            delay = Math.max(delay, 0);
            getHandler().postDelayed(this, delay);
        }

        // TODO: consider removing frame delay setter/getter from ValueAnimator
        @Override
        public void setFrameDelay(long delay) {
            mFrameDelay = delay > 0 ? delay : 0;
        }

        @Override
        public long getFrameDelay() {
            return mFrameDelay;
        }
    }

    /**
     * The intention for having this interface is to increase the testability of ValueAnimator.
     * Specifically, we can have a custom implementation of the interface below and provide
     * timing pulse without using Choreographer. That way we could use any arbitrary interval for
     * our timing pulse in the tests.
     */
    interface AnimationFrameCallbackProvider {

        void onNewCallbackAdded(AnimationFrameCallback callback);

        void postFrameCallback();

        void setFrameDelay(long delay);

        long getFrameDelay();
    }
}
