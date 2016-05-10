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
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Base class for platform specific Transition implementations.
 */
abstract class TransitionImpl {

    public abstract void init(TransitionInterface external, Object internal);

    public void init(TransitionInterface external) {
        init(external, null);
    }

    public abstract TransitionImpl addListener(TransitionInterfaceListener listener);

    public abstract TransitionImpl removeListener(TransitionInterfaceListener listener);

    public abstract TransitionImpl addTarget(View target);

    public abstract TransitionImpl addTarget(int targetId);

    public abstract void captureEndValues(TransitionValues transitionValues);

    public abstract void captureStartValues(TransitionValues transitionValues);

    public abstract Animator createAnimator(ViewGroup sceneRoot,
            TransitionValues startValues, TransitionValues endValues);

    public abstract TransitionImpl excludeChildren(View target, boolean exclude);

    public abstract TransitionImpl excludeChildren(int targetId, boolean exclude);

    public abstract TransitionImpl excludeChildren(Class type, boolean exclude);

    public abstract TransitionImpl excludeTarget(View target, boolean exclude);

    public abstract TransitionImpl excludeTarget(int targetId, boolean exclude);

    public abstract TransitionImpl excludeTarget(Class type, boolean exclude);

    public abstract long getDuration();

    public abstract TransitionImpl setDuration(long duration);

    public abstract TimeInterpolator getInterpolator();

    public abstract TransitionImpl setInterpolator(TimeInterpolator interpolator);

    public abstract String getName();

    public abstract long getStartDelay();

    public abstract TransitionImpl setStartDelay(long startDelay);

    public abstract List<Integer> getTargetIds();

    public abstract List<View> getTargets();

    public abstract String[] getTransitionProperties();

    public abstract TransitionValues getTransitionValues(View view, boolean start);

    public abstract TransitionImpl removeTarget(View target);

    public abstract TransitionImpl removeTarget(int targetId);

}
