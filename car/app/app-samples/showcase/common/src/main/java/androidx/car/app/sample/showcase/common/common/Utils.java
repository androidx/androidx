/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.common;

import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.ClickableSpan;
import androidx.car.app.model.ForegroundCarColorSpan;

/** Assorted utilities. */
public abstract class Utils {

    /** Colorize the given string. */
    public static void colorize(@NonNull SpannableString s, @NonNull CarColor color, int index,
            int length) {
        s.setSpan(
                ForegroundCarColorSpan.create(color),
                index,
                index + length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** Colorize the given string. */
    @NonNull
    public static CharSequence colorize(@NonNull String s, @NonNull CarColor color, int index,
            int length) {
        SpannableString ss = new SpannableString(s);
        ss.setSpan(
                ForegroundCarColorSpan.create(color),
                index,
                index + length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    /** Make the given string clickable. */
    @NonNull
    public static CharSequence clickable(@NonNull String s, int index, int length,
            @NonNull Runnable action) {
        SpannableString ss = new SpannableString(s);
        ss.setSpan(
                ClickableSpan.create(action::run),
                index,
                index + length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    private Utils() {
    }
}
