/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class used to emulate the behavior of {@link View#setTooltipText(CharSequence)} prior
 * to API level 26.
 *
 */
public class TooltipCompat  {
    /**
     * Sets the tooltip text for the view.
     * <p> Prior to API 26 this method sets or clears (when tooltip is null) the view's
     * OnLongClickListener and OnHoverListener. A toast-like subpanel will be created on long click
     * or mouse hover.
     *
     * @param view the view to set the tooltip text on
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT >= 26) {
            view.setTooltipText(tooltipText);
        } else {
            TooltipCompatHandler.setTooltipText(view, tooltipText);
        }
    }

    private TooltipCompat() {}
}
