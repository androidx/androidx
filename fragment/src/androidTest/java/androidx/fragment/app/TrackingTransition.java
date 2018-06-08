/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.fragment.app;

import android.animation.Animator;
import android.graphics.Rect;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * A transition that tracks which targets are applied to it.
 * It will assume any target that it applies to will have differences
 * between the start and end state, regardless of the differences
 * that actually exist. In other words, it doesn't actually check
 * any size or position differences or any other property of the view.
 * It just records the difference.
 * <p>
 * Both start and end value Views are recorded, but no actual animation
 * is created.
 */
class TrackingTransition extends Transition implements TargetTracking {
    public final ArrayList<View> targets = new ArrayList<>();
    private final Rect[] mEpicenter = new Rect[1];
    private static final String PROP = "tracking:prop";
    private static final String[] PROPS = { PROP };

    @Override
    public String[] getTransitionProperties() {
        return PROPS;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROP, 0);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROP, 1);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        if (startValues != null) {
            targets.add(startValues.view);
        }
        if (endValues != null) {
            targets.add(endValues.view);
        }
        Rect epicenter = getEpicenter();
        if (epicenter != null) {
            mEpicenter[0] = new Rect(epicenter);
        } else {
            mEpicenter[0] = null;
        }
        return null;
    }

    @Override
    public ArrayList<View> getTrackedTargets() {
        return targets;
    }

    @Override
    public void clearTargets() {
        targets.clear();
    }

    @Override
    public Rect getCapturedEpicenter() {
        return mEpicenter[0];
    }
}
