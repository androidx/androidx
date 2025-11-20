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
package androidx.leanback.widget;

import android.view.View;

import androidx.leanback.R;

/**
 * Helper for setting rounded rectangle backgrounds on a view.
 */
final class RoundedRectHelper {
    /**
     * Sets or removes a rounded rectangle outline on the given view.
     */
    static void setClipToRoundedOutline(View view, boolean clip) {
        int radius = view.getResources().getDimensionPixelSize(
                R.dimen.lb_rounded_rect_corner_radius);
        RoundedRectHelperApi21.setClipToRoundedOutline(view, clip, radius);
    }

    private RoundedRectHelper() {
    }
}
