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

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;

import java.util.Locale;

public class TextUtilsCompat {
    private static class TextUtilsCompatImpl {
        @NonNull
        public String htmlEncode(@NonNull String s) {
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

        public int getLayoutDirectionFromLocale(@Nullable Locale locale) {
            if (locale != null && !locale.equals(ROOT)) {
                final String scriptSubtag = ICUCompat.maximizeAndGetScript(locale);
                if (scriptSubtag == null) return getLayoutDirectionFromFirstChar(locale);

                if (scriptSubtag.equalsIgnoreCase(ARAB_SCRIPT_SUBTAG) ||
                        scriptSubtag.equalsIgnoreCase(HEBR_SCRIPT_SUBTAG)) {
                    return ViewCompat.LAYOUT_DIRECTION_RTL;
                }
            }
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        /**
         * Fallback algorithm to detect the locale direction. Rely on the first char of the
         * localized locale name. This will not work if the localized locale name is in English
         * (this is the case for ICU 4.4 and "Urdu" script)
         *
         * @param locale
         * @return the layout direction. This may be one of:
         * {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
         * {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
         *
         * Be careful: this code will need to be updated when vertical scripts will be supported
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
    }

    private static class TextUtilsCompatJellybeanMr1Impl extends TextUtilsCompatImpl {
        @NonNull
        public String htmlEncode(@NonNull String s) {
            return TextUtilsCompatJellybeanMr1.htmlEncode(s);
        }

        @Override
        public int getLayoutDirectionFromLocale(@Nullable Locale locale) {
            return TextUtilsCompatJellybeanMr1.getLayoutDirectionFromLocale(locale);
        }
    }

    private static final TextUtilsCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 17) { // JellyBean MR1
            IMPL = new TextUtilsCompatJellybeanMr1Impl();
        } else {
            IMPL = new TextUtilsCompatImpl();
        }
    }

    /**
     * Html-encode the string.
     * @param s the string to be encoded
     * @return the encoded string
     */
    @NonNull
    public static String htmlEncode(@NonNull String s) {
        return IMPL.htmlEncode(s);
    }

    /**
     * Return the layout direction for a given Locale
     *
     * @param locale the Locale for which we want the layout direction. Can be null.
     * @return the layout direction. This may be one of:
     * {@link ViewCompat#LAYOUT_DIRECTION_LTR} or
     * {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     *
     * Be careful: this code will need to be updated when vertical scripts will be supported
     */
    public static int getLayoutDirectionFromLocale(@Nullable Locale locale) {
        return IMPL.getLayoutDirectionFromLocale(locale);
    }

    public static final Locale ROOT = new Locale("", "");

    private static String ARAB_SCRIPT_SUBTAG = "Arab";
    private static String HEBR_SCRIPT_SUBTAG = "Hebr";
}
