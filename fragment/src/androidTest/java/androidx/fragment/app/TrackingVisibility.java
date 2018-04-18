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
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Visibility transition that tracks which targets are applied to it.
 * This transition does no animation.
 */
class TrackingVisibility extends Visibility implements TargetTracking {
    public final ArrayList<View> targets = new ArrayList<>();
    private final Rect[] mEpicenter = new Rect[1];

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues,
            TransitionValues endValues) {
        targets.add(endValues.view);
        Rect epicenter = getEpicenter();
        if (epicenter != null) {
            mEpicenter[0] = new Rect(epicenter);
        } else {
            mEpicenter[0] = null;
        }
        return null;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
            TransitionValues endValues) {
        targets.add(startValues.view);
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
