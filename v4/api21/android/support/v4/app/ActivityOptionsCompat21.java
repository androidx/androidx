/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

import android.app.ActivityOptions;
import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

class ActivityOptionsCompat21 {

    private final ActivityOptions mActivityOptions;

    public static ActivityOptionsCompat21 makeSceneTransitionAnimation(Activity activity,
            View sharedElement, String sharedElementName) {
        return new ActivityOptionsCompat21(
                ActivityOptions.makeSceneTransitionAnimation(activity, sharedElement,
                        sharedElementName));
    }

    public static ActivityOptionsCompat21 makeSceneTransitionAnimation(Activity activity,
            View[] sharedElements, String[] sharedElementNames) {
        Pair[] pairs = null;
        if (sharedElements != null) {
            pairs = new Pair[sharedElements.length];
            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = Pair.create(sharedElements[i], sharedElementNames[i]);
            }
        }
        return new ActivityOptionsCompat21(
                ActivityOptions.makeSceneTransitionAnimation(activity, pairs));
    }

    private ActivityOptionsCompat21(ActivityOptions activityOptions) {
        mActivityOptions = activityOptions;
    }

    public Bundle toBundle() {
        return mActivityOptions.toBundle();
    }

    public void update(ActivityOptionsCompat21 otherOptions) {
        mActivityOptions.update(otherOptions.mActivityOptions);
    }
}
