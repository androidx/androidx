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

package android.support.v4.text;

import static android.os.Build.VERSION.SDK_INT;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Backwards compatible version of {@link TextUtils}.
 */
public final class TextUtilsCompat {
    private static final Locale ROOT = new Locale("", "");
    private static final String ARAB_SCRIPT_SUBTAG = "Arab";
    private static final String HEBR_SCRIPT_SUBTAG = "Hebr";

    /**
     * Html-encode the string.
     *
     * @param s the string to be encoded
     * @return the encoded string
     */
    @NonNull
    public static String htmlEncode(@NonNull String s) {
        if (SDK_INT >= 17) {
            return TextUtils.htmlEncode(s);
        } else {
            StringBuilder sb = new StringBuilder();
            char c;
            for (int i = 0; i < s.length(); i++) {
                c = s.charAt(i);
                switch (c) {
                    case '<':
                        sb.append("&lt;"); //$NON-NLS-1$
                        break;
                    case '>':
                        sb.append("&gt;"); //$NON-NLS-1$
                        break;
                    case '&':
                        sb.append("&amp;"); //$NON-NLS-1$
                        break;
                    case '\'':
                        //http://www.w3.org/TR/xhtml1
                        // The named character reference &apos; (the apostrophe, U+0027) was
                        // introduced in XML 1.0 but does not appear in HTML. Authors should
                        // therefore use &#39; instead of &apos; to work as expected in HTML 4
                        // user agents.
                        sb.append("&#39;"); //$NON-NLS-1$
                        break;
                    case '"':
                        sb.append("&quot;"); //$NON-NLS-1$
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
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
        if (SDK_INT >= 17) {
            return TextUtils.getLayoutDirectionFromLocale(locale);
        } else {
            if (locale != null && !locale.equals(ROOT)) {
                final String scriptSubtag = ICUCompat.maximizeAndGetScript(locale);
                if (scriptSubtag == null) return getLayoutDirectionFromFirstChar(locale);

                // This is intentionally limited to Arabic and Hebrew scripts, since older
                // versions of Android platform only considered those scripts to be right-to-left.
                if (scriptSubtag.equalsIgnoreCase(ARAB_SCRIPT_SUBTAG)
                        || scriptSubtag.equalsIgnoreCase(HEBR_SCRIPT_SUBTAG)) {
                    return ViewCompat.LAYOUT_DIRECTION_RTL;
                }
            }
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }
    }

    /**
     * Fallback algorithm to detect the locale direction. Rely on the first char of the
     * localized locale name. This will not work if the localized locale name is in English
     * (this is the case for ICU 4.4 and "Urdu" script)
     *
     * @param locale the {@link Locale} for which we want the layout direction, maybe be
     *               {@code null}.
     * @return the layout direction, either {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     */
    private static int getLayoutDirectionFromFirstChar(@NonNull Locale locale) {
        switch(Character.getDirectionality(locale.getDisplayName(locale).charAt(0))) {
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                return ViewCompat.LAYOUT_DIRECTION_RTL;

            case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
            default:
                return ViewCompat.LAYOUT_DIRECTION_LTR;
        }
    }

    private TextUtilsCompat() {}
}
