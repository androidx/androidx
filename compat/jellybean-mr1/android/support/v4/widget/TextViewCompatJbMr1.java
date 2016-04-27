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

package android.support.v4.widget;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

class TextViewCompatJbMr1 {

    public static void setCompoundDrawablesRelative(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        textView.setCompoundDrawables(rtl ? end : start, top, rtl ? start : end, bottom);
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top, rtl ? start : end,
                bottom);
    }

    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            int start, int top, int end, int bottom) {
        boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top, rtl ? start : end,
                bottom);
    }

}
