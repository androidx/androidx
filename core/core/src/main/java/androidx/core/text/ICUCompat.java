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

import android.icu.util.ULocale;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public final class ICUCompat {
    private static final String TAG = "ICUCompat";

    private static Method sAddLikelySubtagsMethod;

    static {
        if (Build.VERSION.SDK_INT < 24) {
            try {
                // This class should always exist on API-21 since it's CTS tested.
                final Class<?> clazz = Class.forName("libcore.icu.ICU");
                sAddLikelySubtagsMethod = clazz.getMethod("addLikelySubtags", Locale.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
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
    public static @Nullable String maximizeAndGetScript(@NonNull Locale locale) {
        if (Build.VERSION.SDK_INT >= 24) {
            Object uLocale = Api24Impl.addLikelySubtags(Api24Impl.forLocale(locale));
            return Api24Impl.getScript(uLocale);
        } else {
            try {
                final Object[] args = new Object[] { locale };
                Locale localeWithSubtags = (Locale) sAddLikelySubtagsMethod.invoke(null, args);
                // ULocale.addLikelySubtags(ULocale) is @NonNull
                //noinspection ConstantConditions
                return localeWithSubtags.getScript();
            } catch (InvocationTargetException e) {
                Log.w(TAG, e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, e);
            }
            return locale.getScript();
        }
    }
    private ICUCompat() {
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static ULocale forLocale(Locale loc) {
            return ULocale.forLocale(loc);
        }

        static ULocale addLikelySubtags(Object loc) {
            return ULocale.addLikelySubtags((ULocale) loc);
        }

        static String getScript(Object uLocale) {
            return ((ULocale) uLocale).getScript();
        }
    }
}
