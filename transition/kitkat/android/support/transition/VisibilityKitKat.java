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

import android.annotation.TargetApi;
import android.support.annotation.RequiresApi;

@RequiresApi(19)
@TargetApi(19)
class VisibilityKitKat extends TransitionKitKat implements VisibilityImpl {

    @Override
    public void init(TransitionInterface external, Object internal) {
        mExternalTransition = external;
        if (internal == null) {
            mTransition = new VisibilityWrapper((VisibilityInterface) external);
        } else {
            mTransition = (android.transition.Visibility) internal;
        }
    }

    @Override
    public boolean isVisible(TransitionValues values) {
        return ((android.transition.Visibility) mTransition).isVisible(convertToPlatform(values));
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        return ((android.transition.Visibility) mTransition).onAppear(sceneRoot,
                convertToPlatform(startValues), startVisibility,
                convertToPlatform(endValues), endVisibility);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
            int startVisibility, TransitionValues endValues, int endVisibility) {
        return ((android.transition.Visibility) mTransition).onDisappear(sceneRoot,
                convertToPlatform(startValues), startVisibility,
                convertToPlatform(endValues), endVisibility);
    }

    private static class VisibilityWrapper extends android.transition.Visibility {

        private final VisibilityInterface mVisibility;

        VisibilityWrapper(VisibilityInterface visibility) {
            mVisibility = visibility;
        }

        @Override
        public void captureStartValues(android.transition.TransitionValues transitionValues) {
            wrapCaptureStartValues(mVisibility, transitionValues);
        }

        @Override
        public void captureEndValues(android.transition.TransitionValues transitionValues) {
            wrapCaptureEndValues(mVisibility, transitionValues);
        }

        @Override
        public Animator createAnimator(ViewGroup sceneRoot,
                android.transition.TransitionValues startValues,
                android.transition.TransitionValues endValues) {
            return mVisibility.createAnimator(sceneRoot, convertToSupport(startValues),
                    convertToSupport(endValues));
        }

        @Override
        public boolean isVisible(android.transition.TransitionValues values) {
            if (values == null) {
                return false;
            }
            TransitionValues externalValues = new TransitionValues();
            copyValues(values, externalValues);
            return mVisibility.isVisible(externalValues);
        }

        @Override
        public Animator onAppear(ViewGroup sceneRoot,
                android.transition.TransitionValues startValues, int startVisibility,
                android.transition.TransitionValues endValues, int endVisibility) {
            return mVisibility.onAppear(sceneRoot, convertToSupport(startValues), startVisibility,
                    convertToSupport(endValues), endVisibility);
        }

        @Override
        public Animator onDisappear(ViewGroup sceneRoot,
                android.transition.TransitionValues startValues, int startVisibility,
                android.transition.TransitionValues endValues, int endVisibility) {
            return mVisibility.onDisappear(sceneRoot, convertToSupport(startValues),
                    startVisibility,
                    convertToSupport(endValues), endVisibility);
        }

    }

}
