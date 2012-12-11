/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

class ActivityOptionsCompatJB {

    public static ActivityOptionsCompatJB makeCustomAnimation(Context context,
            int enterResId, int exitResId) {
        return new ActivityOptionsCompatJB(
            ActivityOptions.makeCustomAnimation(context, enterResId, exitResId));
    }

    public static ActivityOptionsCompatJB makeScaleUpAnimation(View source,
            int startX, int startY, int startWidth, int startHeight) {
        return new ActivityOptionsCompatJB(
            ActivityOptions.makeScaleUpAnimation(source, startX, startY, startWidth, startHeight));
    }

    public static ActivityOptionsCompatJB makeThumbnailScaleUpAnimation(View source,
            Bitmap thumbnail, int startX, int startY) {
        return new ActivityOptionsCompatJB(
            ActivityOptions.makeThumbnailScaleUpAnimation(source, thumbnail, startX, startY));
    }

    private final ActivityOptions mActivityOptions;

    private ActivityOptionsCompatJB(ActivityOptions activityOptions) {
        mActivityOptions = activityOptions;
    }

    public Bundle toBundle() {
        return mActivityOptions.toBundle();
    }

    public void update(ActivityOptionsCompatJB otherOptions) {
        mActivityOptions.update(otherOptions.mActivityOptions);
    }
}
