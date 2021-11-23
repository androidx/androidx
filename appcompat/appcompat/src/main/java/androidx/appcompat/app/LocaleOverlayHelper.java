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

package androidx.appcompat.app;

import android.os.LocaleList;

import androidx.annotation.RequiresApi;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Static utilities to overlay locales on top of another LocaleListCompat.
 *
 * <p>This is used to overlay application-specific locales on top of
 *  system locales.</p>
 */
@RequiresApi(24)
final class LocaleOverlayHelper {

    private LocaleOverlayHelper() {}

    /**
     * Combines the overlay locales and base locales.
     * @return the combined {@link LocaleListCompat} if the overlay locales is not empty/null else
     * returns an empty LocaleListCompat.
     */
    static LocaleListCompat combineLocalesIfOverlayExists(LocaleListCompat overlayLocales,
            LocaleListCompat baseLocales) {
        if (overlayLocales == null || overlayLocales.isEmpty()) {
            return LocaleListCompat.getEmptyLocaleList();
        }
        return combineLocales(overlayLocales, baseLocales);
    }

    static LocaleListCompat combineLocalesIfOverlayExists(LocaleList overlayLocales,
            LocaleList baseLocales) {
        if (overlayLocales == null || overlayLocales.isEmpty()) {
            return LocaleListCompat.getEmptyLocaleList();
        }
        return combineLocales(LocaleListCompat.wrap(overlayLocales),
                LocaleListCompat.wrap(baseLocales));
    }

    /**
     * Creates a combined {@link LocaleListCompat} by placing overlay locales before base
     * locales and dropping duplicates from the base locales.
     */
    private static LocaleListCompat combineLocales(LocaleListCompat overlayLocales,
            LocaleListCompat baseLocales) {
        Locale[] combinedLocales =
                new Locale[overlayLocales.size() + baseLocales.size()];
        for (int i = 0; i < overlayLocales.size(); i++) {
            combinedLocales[i] = overlayLocales.get(i);
        }
        for (int i = 0; i < baseLocales.size(); i++) {
            combinedLocales[i + overlayLocales.size()] = baseLocales.get(i);
        }
        LocaleList combineLocaleList = new LocaleList(combinedLocales);
        // Constructor of {@link LocaleListCompat} removes duplicates
        return LocaleListCompat.wrap(combineLocaleList);
    }
}
