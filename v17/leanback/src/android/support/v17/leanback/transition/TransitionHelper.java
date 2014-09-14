/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.transition;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Helper for view transitions.
 * @hide
 */
public final class TransitionHelper {

    public static final int FADE_IN = 0x1;
    public static final int FADE_OUT = 0x2;

    public static final int SLIDE_LEFT = 0;
    public static final int SLIDE_TOP = 1;
    public static final int SLIDE_RIGHT = 2;
    public static final int SLIDE_BOTTOM = 3;

    private final static TransitionHelper sHelper = new TransitionHelper();
    TransitionHelperVersionImpl mImpl;

    /**
     * Gets whether the system supports Transition animations.
     *
     * @return True if Transition animations are supported.
     */
    public static boolean systemSupportsTransitions() {
        if (Build.VERSION.SDK_INT >= 19) {
            // Supported on Android 4.4 or later.
            return true;
        }
        return false;
    }

    /**
     * Interface implemented by classes that support Transition animations.
     */
    static interface TransitionHelperVersionImpl {

        public Object getSharedElementEnterTransition(Window window);

        public Object getSharedElementReturnTransition(Window window);

        public Object getSharedElementExitTransition(Window window);

        public Object getSharedElementReenterTransition(Window window);

        public Object getEnterTransition(Window window);

        public Object getReturnTransition(Window window);

        public Object getExitTransition(Window window);

        public Object getReenterTransition(Window window);

        public Object createScene(ViewGroup sceneRoot, Runnable r);

        public Object createAutoTransition();

        public Object createSlide(SlideCallback callback);

        public Object createScale();

        public Object createFadeTransition(int fadingMode);

        public Object createChangeBounds(boolean reparent);

        public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay);

        public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay);

        public void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay);

        public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay);

        public Object createTransitionSet(boolean sequential);

        public void addTransition(Object transitionSet, Object transition);

        public void setTransitionListener(Object transition, TransitionListener listener);

        public void runTransition(Object scene, Object transition);

        public void exclude(Object transition, int targetId, boolean exclude);

        public void exclude(Object transition, View targetView, boolean exclude);

        public void excludeChildren(Object transition, int targetId, boolean exclude);

        public void excludeChildren(Object transition, View target, boolean exclude);

        public void include(Object transition, int targetId);

        public void include(Object transition, View targetView);

        public void setStartDelay(Object transition, long startDelay);

        public void setDuration(Object transition, long duration);

        public void setInterpolator(Object transition, Object timeInterpolator);

        public void addTarget(Object transition, View view);

        public Object createDefaultInterpolator(Context context);
    }

    /**
     * Interface used when we do not support Transition animations.
     */
    private static final class TransitionHelperStubImpl implements TransitionHelperVersionImpl {

        private static class TransitionStub {
            TransitionListener mTransitionListener;
        }

        @Override
        public Object getSharedElementEnterTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementReturnTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object getEnterTransition(Window window) {
            return null;
        }

        @Override
        public Object getReturnTransition(Window window) {
            return null;
        }

        @Override
        public Object getExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object createScene(ViewGroup sceneRoot, Runnable r) {
            return r;
        }

        @Override
        public Object createAutoTransition() {
            return new TransitionStub();
        }

        @Override
        public Object createFadeTransition(int fadingMode) {
            return new TransitionStub();
        }

        @Override
        public Object createChangeBounds(boolean reparent) {
            return new TransitionStub();
        }

        @Override
        public Object createSlide(SlideCallback callback) {
            return new TransitionStub();
        }

        @Override
        public Object createScale() {
            return new TransitionStub();
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay) {
        }

        @Override
        public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
        }

        @Override
        public Object createTransitionSet(boolean sequential) {
            return new TransitionStub();
        }

        @Override
        public void addTransition(Object transitionSet, Object transition) {
        }

        @Override
        public void exclude(Object transition, int targetId, boolean exclude) {
        }

        @Override
        public void exclude(Object transition, View targetView, boolean exclude) {
        }

        @Override
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
        }

        @Override
        public void include(Object transition, int targetId) {
        }

        @Override
        public void include(Object transition, View targetView) {
        }

        @Override
        public void setStartDelay(Object transition, long startDelay) {
        }

        @Override
        public void setDuration(Object transition, long duration) {
        }

        @Override
        public void setTransitionListener(Object transition, TransitionListener listener) {
            ((TransitionStub) transition).mTransitionListener = listener;
        }

        @Override
        public void runTransition(Object scene, Object transition) {
            TransitionStub transitionStub = (TransitionStub) transition;
            if (transitionStub != null && transitionStub.mTransitionListener != null) {
                transitionStub.mTransitionListener.onTransitionStart(transition);
            }
            Runnable r = ((Runnable) scene);
            if (r != null) {
                r.run();
            }
            if (transitionStub != null && transitionStub.mTransitionListener != null) {
                transitionStub.mTransitionListener.onTransitionEnd(transition);
            }
        }

        @Override
        public void setInterpolator(Object transition, Object timeInterpolator) {
        }

        @Override
        public void addTarget(Object transition, View view) {
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return null;
        }
    }

    /**
     * Implementation used on KitKat (and above).
     */
    private static class TransitionHelperKitkatImpl implements TransitionHelperVersionImpl {

        @Override
        public Object getSharedElementEnterTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementReturnTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object getEnterTransition(Window window) {
            return null;
        }

        @Override
        public Object getReturnTransition(Window window) {
            return null;
        }

        @Override
        public Object getExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object createScene(ViewGroup sceneRoot, Runnable r) {
            return TransitionHelperKitkat.createScene(sceneRoot, r);
        }

        @Override
        public Object createAutoTransition() {
            return TransitionHelperKitkat.createAutoTransition();
        }

        @Override
        public Object createFadeTransition(int fadingMode) {
            return TransitionHelperKitkat.createFadeTransition(fadingMode);
        }

        @Override
        public Object createChangeBounds(boolean reparent) {
            return TransitionHelperKitkat.createChangeBounds(reparent);
        }

        @Override
        public Object createSlide(SlideCallback callback) {
            return TransitionHelperKitkat.createSlide(callback);
        }

        @Override
        public Object createScale() {
            return TransitionHelperKitkat.createScale();
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, view, startDelay);
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, viewId, startDelay);
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, className, startDelay);
        }

        @Override
        public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsDefaultStartDelay(changeBounds, startDelay);
        }

        @Override
        public Object createTransitionSet(boolean sequential) {
            return TransitionHelperKitkat.createTransitionSet(sequential);
        }

        @Override
        public void addTransition(Object transitionSet, Object transition) {
            TransitionHelperKitkat.addTransition(transitionSet, transition);
        }

        @Override
        public void exclude(Object transition, int targetId, boolean exclude) {
            TransitionHelperKitkat.exclude(transition, targetId, exclude);
        }

        @Override
        public void exclude(Object transition, View targetView, boolean exclude) {
            TransitionHelperKitkat.exclude(transition, targetView, exclude);
        }

        @Override
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
            TransitionHelperKitkat.excludeChildren(transition, targetId, exclude);
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
            TransitionHelperKitkat.excludeChildren(transition, targetView, exclude);
        }

        @Override
        public void include(Object transition, int targetId) {
            TransitionHelperKitkat.include(transition, targetId);
        }

        @Override
        public void include(Object transition, View targetView) {
            TransitionHelperKitkat.include(transition, targetView);
        }

        @Override
        public void setStartDelay(Object transition, long startDelay) {
            TransitionHelperKitkat.setStartDelay(transition, startDelay);
        }

        @Override
        public void setDuration(Object transition, long duration) {
            TransitionHelperKitkat.setDuration(transition, duration);
        }

        @Override
        public void setTransitionListener(Object transition, TransitionListener listener) {
            TransitionHelperKitkat.setTransitionListener(transition, listener);
        }

        @Override
        public void runTransition(Object scene, Object transition) {
            TransitionHelperKitkat.runTransition(scene, transition);
        }

        @Override
        public void setInterpolator(Object transition, Object timeInterpolator) {
            TransitionHelperKitkat.setInterpolator(transition, timeInterpolator);
        }

        @Override
        public void addTarget(Object transition, View view) {
            TransitionHelperKitkat.addTarget(transition, view);
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return null;
        }
    }

    private static final class TransitionHelperApi21Impl extends TransitionHelperKitkatImpl {

        @Override
        public Object getSharedElementEnterTransition(Window window) {
            return TransitionHelperApi21.getSharedElementEnterTransition(window);
        }

        @Override
        public Object getSharedElementReturnTransition(Window window) {
            return TransitionHelperApi21.getSharedElementReturnTransition(window);
        }

        @Override
        public Object getSharedElementExitTransition(Window window) {
            return TransitionHelperApi21.getSharedElementExitTransition(window);
        }

        @Override
        public Object getSharedElementReenterTransition(Window window) {
            return TransitionHelperApi21.getSharedElementReenterTransition(window);
        }

        @Override
        public Object getEnterTransition(Window window) {
            return TransitionHelperApi21.getEnterTransition(window);
        }

        @Override
        public Object getReturnTransition(Window window) {
            return TransitionHelperApi21.getReturnTransition(window);
        }

        @Override
        public Object getExitTransition(Window window) {
            return TransitionHelperApi21.getExitTransition(window);
        }

        @Override
        public Object getReenterTransition(Window window) {
            return TransitionHelperApi21.getReenterTransition(window);
        }

        @Override
        public Object createScale() {
            return TransitionHelperApi21.createScale();
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return TransitionHelperApi21.createDefaultInterpolator(context);
        }
    }

    /**
     * Returns the TransitionHelper that can be used to perform Transition
     * animations.
     */
    public static TransitionHelper getInstance() {
        return sHelper;
    }

    private TransitionHelper() {
        if (Build.VERSION.SDK_INT >= 21) {
            mImpl = new TransitionHelperApi21Impl();
        } else  if (systemSupportsTransitions()) {
            mImpl = new TransitionHelperKitkatImpl();
        } else {
            mImpl = new TransitionHelperStubImpl();
        }
    }

    public Object getSharedElementEnterTransition(Window window) {
        return mImpl.getSharedElementEnterTransition(window);
    }

    public Object getSharedElementReturnTransition(Window window) {
        return mImpl.getSharedElementReturnTransition(window);
    }

    public Object getSharedElementExitTransition(Window window) {
        return mImpl.getSharedElementExitTransition(window);
    }

    public Object getSharedElementReenterTransition(Window window) {
        return mImpl.getSharedElementReenterTransition(window);
    }

    public Object getEnterTransition(Window window) {
        return mImpl.getEnterTransition(window);
    }

    public Object getReturnTransition(Window window) {
        return mImpl.getReturnTransition(window);
    }

    public Object getExitTransition(Window window) {
        return mImpl.getExitTransition(window);
    }

    public Object getReenterTransition(Window window) {
        return mImpl.getReenterTransition(window);
    }

    public Object createScene(ViewGroup sceneRoot, Runnable r) {
        return mImpl.createScene(sceneRoot, r);
    }

    public Object createChangeBounds(boolean reparent) {
        return mImpl.createChangeBounds(reparent);
    }

    public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
        mImpl.setChangeBoundsStartDelay(changeBounds, view, startDelay);
    }

    public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
        mImpl.setChangeBoundsStartDelay(changeBounds, viewId, startDelay);
    }

    public void setChangeBoundsStartDelay(Object changeBounds, String className, int startDelay) {
        mImpl.setChangeBoundsStartDelay(changeBounds, className, startDelay);
    }

    public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
        mImpl.setChangeBoundsDefaultStartDelay(changeBounds, startDelay);
    }

    public Object createTransitionSet(boolean sequential) {
        return mImpl.createTransitionSet(sequential);
    }

    public Object createSlide(SlideCallback callback) {
        return mImpl.createSlide(callback);
    }

    public Object createScale() {
        return mImpl.createScale();
    }

    public void addTransition(Object transitionSet, Object transition) {
        mImpl.addTransition(transitionSet, transition);
    }

    public void exclude(Object transition, int targetId, boolean exclude) {
        mImpl.exclude(transition, targetId, exclude);
    }

    public void exclude(Object transition, View targetView, boolean exclude) {
        mImpl.exclude(transition, targetView, exclude);
    }

    public void excludeChildren(Object transition, int targetId, boolean exclude) {
        mImpl.excludeChildren(transition, targetId, exclude);
    }

    public void excludeChildren(Object transition, View targetView, boolean exclude) {
        mImpl.excludeChildren(transition, targetView, exclude);
    }

    public void include(Object transition, int targetId) {
        mImpl.include(transition, targetId);
    }

    public void include(Object transition, View targetView) {
        mImpl.include(transition, targetView);
    }

    public void setStartDelay(Object transition, long startDelay) {
        mImpl.setStartDelay(transition, startDelay);
    }

    public void setDuration(Object transition, long duration) {
        mImpl.setDuration(transition, duration);
    }

    public Object createAutoTransition() {
        return mImpl.createAutoTransition();
    }

    public Object createFadeTransition(int fadeMode) {
        return mImpl.createFadeTransition(fadeMode);
    }

    public void setTransitionListener(Object transition, TransitionListener listener) {
        mImpl.setTransitionListener(transition, listener);
    }

    public void runTransition(Object scene, Object transition) {
        mImpl.runTransition(scene, transition);
    }

    public void setInterpolator(Object transition, Object timeInterpolator) {
        mImpl.setInterpolator(transition, timeInterpolator);
    }

    public void addTarget(Object transition, View view) {
        mImpl.addTarget(transition, view);
    }

    public Object createDefaultInterpolator(Context context) {
        return mImpl.createDefaultInterpolator(context);
    }
}
