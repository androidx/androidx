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
import android.support.v17.leanback.R;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.SparseArray;
import android.view.ViewGroup;

class TransitionHelperKitkat {
    private final Context mContext;
    private SparseArray<Scene> mScenes = new SparseArray<Scene>();
    private Transition mTransition;
    private TransitionManager mTransitionManager;

    TransitionHelperKitkat(Context context) {
        mContext = context;
        TransitionInflater transitionInflater = TransitionInflater.from(context);
        mTransition = transitionInflater.inflateTransition(R.transition.lb_browse_transition);
    }

    void addSceneRunnable(int sceneId, ViewGroup sceneRoot, Runnable r) {
        Scene scene = new Scene(sceneRoot);
        scene.setEnterAction(r);
        mScenes.put(sceneId, scene);
    }

    void runTransition(int sceneId) {
        Scene scene = mScenes.get(sceneId);
        TransitionManager.go(scene, mTransition);
    }
}
