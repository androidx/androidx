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
package android.support.v17.leanback.app;

import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Helper for view transitions.
 */
final class TransitionHelper {

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

        public Object createScene(ViewGroup sceneRoot, Runnable r);

        public Object createAutoTransition();

        public void setTransitionCompleteListener(Object transition, Runnable listener);

        public void runTransition(Object scene, Object transition);

        public void excludeChildren(Object transition, int targetId, boolean exclude);

        public void excludeChildren(Object transition, View target, boolean exclude);
    }

    /**
     * Interface used when we do not support Transition animations.
     */
    private static final class TransitionHelperStubImpl implements TransitionHelperVersionImpl {

        private static class TransitionStub {
            Runnable mCompleteListener;
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
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
        }

        @Override
        public void setTransitionCompleteListener(Object transition, Runnable listener) {
            ((TransitionStub) transition).mCompleteListener = listener;
        }

        @Override
        public void runTransition(Object scene, Object transition) {
            Runnable r = ((Runnable) scene);
            if (r != null) {
                r.run();
            }
            TransitionStub transitionStub = (TransitionStub) transition;
            if (transitionStub != null && transitionStub.mCompleteListener != null) {
                transitionStub.mCompleteListener.run();
            }
        }
    }

    /**
     * Implementation used on KitKat (and above).
     */
    private static final class TransitionHelperKitkatImpl implements TransitionHelperVersionImpl {
        private final TransitionHelperKitkat mTransitionHelper;

        TransitionHelperKitkatImpl(Context context) {
            mTransitionHelper = new TransitionHelperKitkat(context);
        }

        @Override
        public Object createScene(ViewGroup sceneRoot, Runnable r) {
            return mTransitionHelper.createScene(sceneRoot, r);
        }

        @Override
        public Object createAutoTransition() {
            return mTransitionHelper.createAutoTransition();
        }

        @Override
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
            mTransitionHelper.excludeChildren(transition, targetId, exclude);
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
            mTransitionHelper.excludeChildren(transition, targetView, exclude);
        }

        @Override
        public void setTransitionCompleteListener(Object transition, Runnable listener) {
            mTransitionHelper.setTransitionCompleteListener(transition, listener);
        }

        @Override
        public void runTransition(Object scene, Object transition) {
            mTransitionHelper.runTransition(scene, transition);
        }
    }

    /**
     * Returns the TransitionHelper that can be used to perform Transition
     * animations.
     *
     * @param context A context for accessing system resources.
     */
    public TransitionHelper(Context context) {
        if (systemSupportsTransitions()) {
            mImpl = new TransitionHelperKitkatImpl(context);
        } else {
            mImpl = new TransitionHelperStubImpl();
        }
    }

    public Object createScene(ViewGroup sceneRoot, Runnable r) {
        return mImpl.createScene(sceneRoot, r);
    }

    public void excludeChildren(Object transition, int targetId, boolean exclude) {
        mImpl.excludeChildren(transition, targetId, exclude);
    }

    public void excludeChildren(Object transition, View targetView, boolean exclude) {
        mImpl.excludeChildren(transition, targetView, exclude);
    }

    public Object createAutoTransition() {
        return mImpl.createAutoTransition();
    }

    public void setTransitionCompleteListener(Object transition, Runnable listener) {
        mImpl.setTransitionCompleteListener(transition, listener);
    }

    public void runTransition(Object scene, Object transition) {
        mImpl.runTransition(scene, transition);
    }
}
