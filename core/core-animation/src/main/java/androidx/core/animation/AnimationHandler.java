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
        if (mAnimationCallbacks.size() > 0) {
            mProvider.postFrameCallback();
        }
    }
    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     */

    public static final ThreadLocal<AnimationHandler> sAnimationHandler = new ThreadLocal<>();
    private static AnimationHandler sTestHandler = null;
    private final AnimationFrameCallbackProvider mProvider;
    private final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    boolean mListDirty = false;

    AnimationHandler(AnimationFrameCallbackProvider provider) {
        if (provider == null) {
            mProvider = new FrameCallbackProvider16();
        } else {
            mProvider = provider;
        }
    }

    public static AnimationHandler getInstance() {
        if (sTestHandler != null) {
            return sTestHandler;
        }
        if (sAnimationHandler.get() == null) {
            sAnimationHandler.set(new AnimationHandler(null));
        }
        return sAnimationHandler.get();
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

    /**
     * Register to get a callback on the next frame after the delay.
     */
    void addAnimationFrameCallback(final AnimationFrameCallback callback) {
        if (mAnimationCallbacks.size() == 0) {
            mProvider.postFrameCallback();
        }
        if (!mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks.add(callback);
        }
        mProvider.onNewCallbackAdded(callback);
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     */
    public void removeCallback(AnimationFrameCallback callback) {
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            mAnimationCallbacks.set(id, null);
            mListDirty = true;
        }
    }

    void autoCancelBasedOn(ObjectAnimator objectAnimator) {
        for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
            AnimationFrameCallback cb = mAnimationCallbacks.get(i);
            if (cb == null) {
                continue;
            }
            if (objectAnimator.shouldAutoCancel(cb)) {
                ((Animator) mAnimationCallbacks.get(i)).cancel();
            }
        }
    }

    private void doAnimationFrame(long frameTime) {
        for (int i = 0; i < mAnimationCallbacks.size(); i++) {
            final AnimationFrameCallback callback = mAnimationCallbacks.get(i);
            if (callback == null) {
                continue;
            }
            callback.doAnimationFrame(frameTime);
        }
        cleanUpList();
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

    private int getCallbackSize() {
        int count = 0;
        int size = mAnimationCallbacks.size();
        for (int i = size - 1; i >= 0; i--) {
            if (mAnimationCallbacks.get(i) != null) {
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
