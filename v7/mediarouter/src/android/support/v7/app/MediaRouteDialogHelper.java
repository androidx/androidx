/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.app;

import android.content.Context;
import android.support.v7.mediarouter.R;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;

final class MediaRouteDialogHelper {
    /**
     * The framework should set the dialog width properly, but somehow it doesn't work, hence
     * duplicating a similar logic here to determine the appropriate dialog width.
     */
    public static int getDialogWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        boolean isPortrait = metrics.widthPixels < metrics.heightPixels;

        TypedValue value = new TypedValue();
        context.getResources().getValue(isPortrait ? R.dimen.mr_dialog_fixed_width_minor
                : R.dimen.mr_dialog_fixed_width_major, value, true);
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return (int) value.getDimension(metrics);
        } else if (value.type == TypedValue.TYPE_FRACTION) {
            return (int) value.getFraction(metrics.widthPixels, metrics.widthPixels);
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }
}
