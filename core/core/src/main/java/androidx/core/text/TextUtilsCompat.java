/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.core.text;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import java.util.Locale;

/**
 * Backwards compatible version of {@link TextUtils}.
 */
public final class TextUtilsCompat {

    /**
     * Html-encode the string.
     *
     * @param s the string to be encoded
     * @return the encoded string
     */
    @NonNull
    public static String htmlEncode(@NonNull String s) {
        return TextUtils.htmlEncode(s);
    }

    /**
     * Returns the layout direction for a given Locale
     *
     * @param locale the {@link Locale} for which we want the layout direction, maybe be
     *               {@code null}.
     * @return the layout direction, either {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     */
    public static int getLayoutDirectionFromLocale(@Nullable Locale locale) {
        return TextUtils.getLayoutDirectionFromLocale(locale);
    }

    private TextUtilsCompat() {
    }
}
