/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

class AnimatorUtil {

    static ObjectAnimator ofTranslationY(float startValue, float endValue, View target) {
        return ObjectAnimator.ofFloat(target, "translationY", startValue, endValue);
    }

    static AnimatorSet ofTranslationYTogether(float startValue, float endValue, View[] targets) {
        AnimatorSet set = new AnimatorSet();
        if (targets.length == 0) return set;
        AnimatorSet.Builder builder = set.play(ofTranslationY(startValue, endValue, targets[0]));
        for (int i = 1; i < targets.length; i++) {
            builder.with(ofTranslationY(startValue, endValue, targets[i]));
        }
        return set;
    }

    private AnimatorUtil() {
    }

}
