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

package android.support.v4.os;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;

import java.util.Locale;

/**
 * Helper to deal with new {@link Locale} APIs.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
final class LocaleHelper {

    // Simpleton implementation for Locale.forLanguageTag(...)
    static Locale forLanguageTag(String str) {
        if (str.contains("-")) {
            String[] args = str.split("-");
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            } else if (args.length > 1) {
                return new Locale(args[0], args[1]);
            } else if (args.length == 1) {
                return new Locale(args[0]);
            }
        } else if (str.contains("_")) {
            String[] args = str.split("_");
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            } else if (args.length > 1) {
                return new Locale(args[0], args[1]);
            } else if (args.length == 1) {
                return new Locale(args[0]);
            }
        } else {
            return new Locale(str);
        }

        throw new IllegalArgumentException("Can not parse language tag: [" + str + "]");
    }

    // Simpleton implementation for Locale.toLanguageTag(...)
    static String toLanguageTag(Locale locale) {
        StringBuilder buf = new StringBuilder();
        buf.append(locale.getLanguage());
        final String country = locale.getCountry();
        if (country != null && !country.isEmpty()) {
            buf.append("-");
            buf.append(locale.getCountry());
        }

        return buf.toString();
    }
}
