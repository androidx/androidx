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
package android.support.v17.leanback.widget;

import android.support.v17.leanback.R;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewOutlineProvider;
import android.view.View;

class RoundedRectHelperApi21 {

    private static int sCornerRadius;

    private static final ViewOutlineProvider sOutlineProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (sCornerRadius == 0) {
                sCornerRadius = view.getResources().getDimensionPixelSize(
                        R.dimen.lb_rounded_rect_corner_radius);
            }
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), sCornerRadius);
            outline.setAlpha(1f);
        }
    };

    public static void setClipToRoundedOutline(View view, boolean clip) {
        view.setOutlineProvider(clip ? sOutlineProvider : ViewOutlineProvider.BACKGROUND);
        view.setClipToOutline(clip);
    }
}
