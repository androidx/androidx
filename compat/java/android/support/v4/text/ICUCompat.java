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

import android.support.annotation.RequiresApi;
import android.os.Build;
import android.support.annotation.Nullable;

import java.util.Locale;

public final class ICUCompat {
    static class ICUCompatBaseImpl {
        public String maximizeAndGetScript(Locale locale) {
            return ICUCompatIcs.maximizeAndGetScript(locale);
        }
    }

    @RequiresApi(21)
    static class ICUCompatApi21Impl extends ICUCompatBaseImpl {
        @Override
        public String maximizeAndGetScript(Locale locale) {
            return ICUCompatApi21.maximizeAndGetScript(locale);
        }
    }

    private static final ICUCompatBaseImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new ICUCompatApi21Impl();
        } else {
            IMPL = new ICUCompatBaseImpl();
        }
    }

    /**
     * Returns the script for a given Locale.
     *
     * If the locale isn't already in its maximal form, likely subtags for the provided locale
     * ID are added before we determine the script. For further details, see the following CLDR
     * technical report :
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
     *
     * @return The script for a given Locale if ICU library is available, otherwise null.
     */
    @Nullable
    public static String maximizeAndGetScript(Locale locale) {
        return IMPL.maximizeAndGetScript(locale);
    }

    private ICUCompat() {}
}
