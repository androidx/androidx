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

public class ICUCompat {

    interface ICUCompatImpl {
        public String getScript(String locale);
        public String addLikelySubtags(String locale);
    }

    static class ICUCompatImplBase implements ICUCompatImpl {
        @Override
        public String getScript(String locale) {
            return null;
        }

        @Override
        public String addLikelySubtags(String locale) {
            return locale;
        }
    }

    static class ICUCompatImplIcs implements ICUCompatImpl {
        @Override
        public String getScript(String locale) {
            return ICUCompatIcs.getScript(locale);
        }

        @Override
        public String addLikelySubtags(String locale) {
            return ICUCompatIcs.addLikelySubtags(locale);
        }
    }

    private static final ICUCompatImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 14) {
            IMPL = new ICUCompatImplIcs();
        } else {
            IMPL = new ICUCompatImplBase();
        }
    }

    /**
     * Returns the script (language code) of a script.
     *
     * @param locale The locale.
     * @return a String representing the script (language code) of the locale.
     */
    public static String getScript(String locale) {
        return IMPL.getScript(locale);
    }

    /**
     * Add the likely subtags for a provided locale ID, per the algorithm described in the following
     * CLDR technical report:
     *
     * http://www.unicode.org/reports/tr35/#Likely_Subtags
     *
     * If locale is already in the maximal form, or there is no data available for maximization,
     * it will be just returned. For example, "und-Zzzz" cannot be maximized, since there is no
     * reasonable maximization.
     *
     * Examples:
     *
     * "en" maximizes to "en_Latn_US"
     * "de" maximizes to "de_Latn_US"
     * "sr" maximizes to "sr_Cyrl_RS"
     * "sh" maximizes to "sr_Latn_RS" (Note this will not reverse.)
     * "zh_Hani" maximizes to "zh_Hans_CN" (Note this will not reverse.)

     * @param locale The locale to maximize
     *
     * @return the maximized locale
     */
    public static String addLikelySubtags(String locale) {
        return IMPL.addLikelySubtags(locale);
    }
}
