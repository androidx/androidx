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

package androidx.core.os;

import static android.os.Build.VERSION.SDK_INT;

import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

import java.util.Locale;

/**
 * Helper class which allows access to properties of {@link Configuration} in
 * a backward compatible fashion.
 */
public final class ConfigurationCompat {
    private ConfigurationCompat() {
    }

    /**
     * Get the {@link LocaleListCompat} from the {@link Configuration}.
     *
     * @return The locale list.
     */
    @SuppressWarnings("deprecation")
    public static @NonNull LocaleListCompat getLocales(@NonNull Configuration configuration) {
        if (SDK_INT >= 24) {
            return LocaleListCompat.wrap(Api24Impl.getLocales(configuration));
        } else {
            return LocaleListCompat.create(configuration.locale);
        }
    }

    /**
     * Set the {@link Locale} into {@link Configuration}.
     */
    public static void setLocales(
            @NonNull Configuration configuration, @NonNull LocaleListCompat locales) {
        if (SDK_INT >= 24) {
            Api24Impl.setLocales(configuration, locales);
        } else {
            if (!locales.isEmpty()) {
                configuration.setLocale(locales.get(0));
            }
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static android.os.LocaleList getLocales(Configuration configuration) {
            return configuration.getLocales();
        }

        static void setLocales(
                @NonNull Configuration configuration, @NonNull LocaleListCompat locales) {
            configuration.setLocales((LocaleList) locales.unwrap());
        }
    }
}
