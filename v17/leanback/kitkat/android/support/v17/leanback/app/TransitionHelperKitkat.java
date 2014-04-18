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
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

class TransitionHelperKitkat {
    private final Context mContext;

    TransitionHelperKitkat(Context context) {
        mContext = context;
    }

    Object createScene(ViewGroup sceneRoot, Runnable enterAction) {
        Scene scene = new Scene(sceneRoot);
        scene.setEnterAction(enterAction);
        return scene;
    }

    Object createAutoTransition() {
        return new AutoTransition();
    }

    void excludeChildren(Object transition, int targetId, boolean exclude) {
        ((Transition) transition).excludeChildren(targetId, exclude);
    }

    void excludeChildren(Object transition, View targetView, boolean exclude) {
        ((Transition) transition).excludeChildren(targetView, exclude);
    }

    public void setTransitionCompleteListener(Object transition, Runnable listener) {
        Transition t = (Transition) transition;
        final Runnable completeListener = listener;
        t.addListener(new Transition.TransitionListener() {

            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                completeListener.run();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }
        });
    }

    void runTransition(Object scene, Object transition) {
        TransitionManager.go((Scene) scene, (Transition) transition);
    }
}
