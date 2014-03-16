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
import android.view.ViewGroup;

/**
 * Helper for view transitions.
 */
final class TransitionHelper {

    static final int SCENE_WITH_TITLE = 1;
    static final int SCENE_WITHOUT_TITLE = 2;
    static final int SCENE_WITH_HEADERS = 3;
    static final int SCENE_WITHOUT_HEADERS = 4;

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

        public void addSceneRunnable(int scene, ViewGroup sceneRoot, Runnable r);

        public void runTransition(int scene);
    }

    /**
     * Interface used when we do not support Transition animations.
     */
    private static final class TransitionHelperStubImpl implements TransitionHelperVersionImpl {
        SparseArray<Runnable> mSceneRunnables = new SparseArray<Runnable>();

        @Override
        public void addSceneRunnable(int scene, ViewGroup sceneRoot, Runnable r) {
            mSceneRunnables.put(scene, r);
        }

        @Override
        public void runTransition(int scene) {
            Runnable r = mSceneRunnables.get(scene);
            if (r != null) {
                r.run();
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
        public void addSceneRunnable(int scene, ViewGroup sceneRoot, Runnable r) {
            mTransitionHelper.addSceneRunnable(scene, sceneRoot, r);
        }

        @Override
        public void runTransition(int scene) {
            mTransitionHelper.runTransition(scene);
        }
    }

    /**
     * Returns the TransitionHelper that can be used to perform Transition
     * animations.
     *
     * @param context A context for accessing system resources.
     * @return the TransitionHelper to perform Transition animations
     */
    public TransitionHelper(Context context) {
        if (systemSupportsTransitions()) {
            mImpl = new TransitionHelperKitkatImpl(context);
        } else {
            mImpl = new TransitionHelperStubImpl();
        }
    }

    public void addSceneRunnable(int scene, ViewGroup sceneRoot, Runnable r) {
        mImpl.addSceneRunnable(scene, sceneRoot, r);
    }

    public void runTransition(int scene) {
        mImpl.runTransition(scene);
    }
}
