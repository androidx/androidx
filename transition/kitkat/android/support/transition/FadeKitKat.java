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

class FadeKitKat extends TransitionKitKat implements VisibilityImpl {

    public FadeKitKat(TransitionInterface transition) {
        init(transition, new android.transition.Fade());
    }

    public FadeKitKat(TransitionInterface transition, int fadingMode) {
        init(transition, new android.transition.Fade(fadingMode));
    }

    @Override
    public boolean isVisible(TransitionValues values) {
        return ((android.transition.Fade) mTransition).isVisible(convertToPlatform(values));
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        return ((android.transition.Fade) mTransition).onAppear(sceneRoot,
                convertToPlatform(startValues), startVisibility,
                convertToPlatform(endValues), endVisibility);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
            int startVisibility, TransitionValues endValues, int endVisibility) {
        return ((android.transition.Fade) mTransition).onDisappear(sceneRoot,
                convertToPlatform(startValues), startVisibility,
                convertToPlatform(endValues), endVisibility);
    }

}
