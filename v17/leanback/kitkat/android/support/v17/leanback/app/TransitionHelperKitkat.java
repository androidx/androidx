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

import android.animation.Animator;
import android.content.Context;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

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

    Object createTransitionSet(boolean sequential) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(sequential ? TransitionSet.ORDERING_SEQUENTIAL :
            TransitionSet.ORDERING_TOGETHER);
        return set;
    }

    void addTransition(Object transitionSet, Object transition) {
        ((TransitionSet) transitionSet).addTransition((Transition) transition);
    }

    Object createAutoTransition() {
        return new AutoTransition();
    }

    Object createFadeTransition(int fadingMode) {
        Fade fade = new Fade(fadingMode);
        return fade;
    }

    /**
     * change bounds that support customized start delay.
     */
    static class CustomChangeBounds extends ChangeBoundsKitKat {

        int mDefaultStartDelay;
        // View -> delay
        final HashMap<View, Integer> mViewStartDelays = new HashMap<View, Integer>();
        // id -> delay
        final SparseIntArray mIdStartDelays = new SparseIntArray();
        // Class.getName() -> delay
        final HashMap<String, Integer> mClassStartDelays = new HashMap<String, Integer>();

        private int getDelay(View view) {
            Integer delay = mViewStartDelays.get(view);
            if (delay != null) {
                return delay;
            }
            int idStartDelay = mIdStartDelays.get(view.getId(), -1);
            if (idStartDelay != -1) {
                return idStartDelay;
            }
            delay = mClassStartDelays.get(view.getClass().getName());
            if (delay != null) {
                return delay;
            }
            return mDefaultStartDelay;
        }

        @Override
        public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                TransitionValues endValues) {
            Animator animator = super.createAnimator(sceneRoot, startValues, endValues);
            if (animator != null && endValues != null && endValues.view != null) {
                animator.setStartDelay(getDelay(endValues.view));
            }
            return animator;
        }

        public void setStartDelay(View view, int startDelay) {
            mViewStartDelays.put(view, startDelay);
        }

        public void setStartDelay(int viewId, int startDelay) {
            mIdStartDelays.put(viewId, startDelay);
        }

        public void setStartDelay(String className, int startDelay) {
            mClassStartDelays.put(className, startDelay);
        }

        public void setDefaultStartDelay(int startDelay) {
            mDefaultStartDelay = startDelay;
        }
    }

    Object createChangeBounds(boolean reparent) {
        CustomChangeBounds changeBounds = new CustomChangeBounds();
        changeBounds.setReparent(reparent);
        return changeBounds;
    }

    void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
        ((CustomChangeBounds) changeBounds).setStartDelay(viewId, startDelay);
    }

    void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
        ((CustomChangeBounds) changeBounds).setStartDelay(view, startDelay);
    }

    void setChangeBoundsStartDelay(Object changeBounds, String className, int startDelay) {
        ((CustomChangeBounds) changeBounds).setStartDelay(className, startDelay);
    }

    void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
        ((CustomChangeBounds) changeBounds).setDefaultStartDelay(startDelay);
    }

    void exclude(Object transition, int targetId, boolean exclude) {
        ((Transition) transition).excludeTarget(targetId, exclude);
    }

    void exclude(Object transition, View targetView, boolean exclude) {
        ((Transition) transition).excludeTarget(targetView, exclude);
    }

    void excludeChildren(Object transition, int targetId, boolean exclude) {
        ((Transition) transition).excludeChildren(targetId, exclude);
    }

    void excludeChildren(Object transition, View targetView, boolean exclude) {
        ((Transition) transition).excludeChildren(targetView, exclude);
    }

    void include(Object transition, int targetId) {
        ((Transition) transition).addTarget(targetId);
    }

    void include(Object transition, View targetView) {
        ((Transition) transition).addTarget(targetView);
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
