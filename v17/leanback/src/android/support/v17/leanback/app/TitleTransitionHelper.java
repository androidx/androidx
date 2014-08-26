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

import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.SlideCallback;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

class TitleTransitionHelper {

    final static SlideCallback sSlideCallback = new SlideCallback() {
        @Override
        public boolean getSlide(View view, boolean appear, int[] edge, float[] distance) {
            edge[0] = TransitionHelper.SLIDE_TOP;
            distance[0] = view.getHeight();
            return true;
        }
    };

    private static Interpolator createTransitionInterpolatorUp() {
        return new DecelerateInterpolator(4);
    }

    private static Interpolator createTransitionInterpolatorDown() {
        return new DecelerateInterpolator();
    }

    static public Object createTransitionTitleUp(TransitionHelper helper) {
        Object transition = helper.createSlide(sSlideCallback);
        helper.setInterpolator(transition, createTransitionInterpolatorUp());
        return transition;
    }

    static public Object createTransitionTitleDown(TransitionHelper helper) {
        Object transition = helper.createSlide(sSlideCallback);
        helper.setInterpolator(transition, createTransitionInterpolatorDown());
        return transition;
    }

}
