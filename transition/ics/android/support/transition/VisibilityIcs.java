/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.transition;

import android.animation.Animator;
import android.view.ViewGroup;

class VisibilityIcs extends TransitionIcs implements VisibilityImpl {

    @Override
    public void init(TransitionInterface external, Object internal) {
        mExternalTransition = external;
        if (internal == null) {
            mTransition = new VisibilityWrapper((VisibilityInterface) external);
        } else {
            mTransition = (VisibilityPort) internal;
        }
    }

    @Override
    public boolean isVisible(TransitionValues values) {
        return ((VisibilityPort) mTransition).isVisible(values);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        return ((VisibilityPort) mTransition).onAppear(sceneRoot, startValues, startVisibility,
                endValues, endVisibility);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
            int startVisibility, TransitionValues endValues, int endVisibility) {
        return ((VisibilityPort) mTransition).onDisappear(sceneRoot, startValues, startVisibility,
                endValues, endVisibility);
    }

    private static class VisibilityWrapper extends VisibilityPort {

        private VisibilityInterface mVisibility;

        VisibilityWrapper(VisibilityInterface visibility) {
            mVisibility = visibility;
        }

        @Override
        public void captureStartValues(TransitionValues transitionValues) {
            mVisibility.captureStartValues(transitionValues);
        }

        @Override
        public void captureEndValues(TransitionValues transitionValues) {
            mVisibility.captureEndValues(transitionValues);
        }

        @Override
        public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                TransitionValues endValues) {
            return mVisibility.createAnimator(sceneRoot, startValues, endValues);
        }

        @Override
        public boolean isVisible(TransitionValues values) {
            return mVisibility.isVisible(values);
        }

        @Override
        public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues,
                int startVisibility,
                TransitionValues endValues, int endVisibility) {
            return mVisibility.onAppear(sceneRoot, startValues, startVisibility,
                    endValues, endVisibility);
        }

        @Override
        public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
                int startVisibility, TransitionValues endValues, int endVisibility) {
            return mVisibility.onDisappear(sceneRoot, startValues, startVisibility,
                    endValues, endVisibility);
        }

    }

}
