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
package android.support.v17.leanback.transition;

import android.content.Context;
import android.support.v17.leanback.R;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

class LeanbackTransitionHelperKitKat {

    static public Object loadTitleInTransition(Context context) {
        SlideKitkat slide = new SlideKitkat();
        slide.setSlideEdge(Gravity.TOP);
        slide.setInterpolator(AnimationUtils.loadInterpolator(context,
                android.R.anim.decelerate_interpolator));
        slide.addTarget(R.id.browse_title_group);
        return slide;
    }

    static public Object loadTitleOutTransition(Context context) {
        SlideKitkat slide = new SlideKitkat();
        slide.setSlideEdge(Gravity.TOP);
        slide.setInterpolator(AnimationUtils.loadInterpolator(context,
                R.animator.lb_decelerator_4));
        slide.addTarget(R.id.browse_title_group);
        return slide;
    }

}
