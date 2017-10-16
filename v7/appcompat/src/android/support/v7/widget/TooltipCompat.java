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

package android.support.v7.widget;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Helper class used to emulate the behavior of {@link View#setTooltipText(CharSequence)} prior
 * to API level 26.
 *
 */
public class TooltipCompat  {
    private interface ViewCompatImpl {
        void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText);
    }

    private static class BaseViewCompatImpl implements ViewCompatImpl {
        @Override
        public void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
            TooltipCompatHandler.setTooltipText(view, tooltipText);
        }
    }

    @TargetApi(26)
    private static class Api26ViewCompatImpl implements ViewCompatImpl {
        @Override
        public void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
            view.setTooltipText(tooltipText);
        }
    }

    private static final ViewCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 26) {
            IMPL = new Api26ViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

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
        IMPL.setTooltipText(view, tooltipText);
    }

    private TooltipCompat() {}
}
