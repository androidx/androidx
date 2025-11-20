/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.app;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.AnyThread;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.LocaleListCompat;

import org.jspecify.annotations.NonNull;

/**
 * Helper for accessing features in {@link android.app.LocaleManager} in a backwards compatible
 * fashion.
 */
public final class LocaleManagerCompat {

    private LocaleManagerCompat() {}

    /**
     * Returns the current system locales, ignoring app-specific overrides.
     *
     * <p><b>Note:</b> Apps should generally access the user's locale preferences as indicated in
     * their in-process {@link android.os.LocaleList}s. However, in case an app-specific locale
     * is set, this method helps cater to rare use-cases which might require specifically knowing
     * the system locale.
     */
    @AnyThread
    public static @NonNull LocaleListCompat getSystemLocales(@NonNull Context context) {
        LocaleListCompat systemLocales = LocaleListCompat.getEmptyLocaleList();
        // TODO: modify the check to Build.Version.SDK_INT >= 33.
        if (Build.VERSION.SDK_INT >= 33) {
            // If the API version is 33 or above we want to redirect the call to the framework API.
            Object localeManager = getLocaleManagerForApplication(context);
            if (localeManager != null) {
                systemLocales = LocaleListCompat.wrap(Api33Impl.localeManagerGetSystemLocales(
                        localeManager));
            }
        } else {
            systemLocales = getConfigurationLocales(Resources.getSystem().getConfiguration());
        }
        return systemLocales;
    }

    /**
     * Returns application locales for the calling app as a {@link LocaleListCompat}. This API
     * for non-{@link androidx.appcompat.app.AppCompatDelegate} context to easily get the per-app
     * locale on the prior API 33 devices.
     *
     * <p>Returns a {@link LocaleListCompat#getEmptyLocaleList()} if no app-specific locales are
     * set.
     */
    @AnyThread
    public static @NonNull LocaleListCompat getApplicationLocales(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            // If the API version is 33 or above we want to redirect the call to the framework API.
            Object localeManager = getLocaleManagerForApplication(context);
            if (localeManager != null) {
                return LocaleListCompat.wrap(Api33Impl.localeManagerGetApplicationLocales(
                        localeManager));
            } else {
                return LocaleListCompat.getEmptyLocaleList();
            }
        } else {
            return LocaleListCompat.forLanguageTags(AppLocalesStorageHelper.readLocales(context));
        }
    }

    /**
     * Returns the localeManager for the current application.
     */
    @RequiresApi(33)
    private static Object getLocaleManagerForApplication(Context context) {
        return context.getSystemService(Context.LOCALE_SERVICE);
    }

    @VisibleForTesting
    static LocaleListCompat getConfigurationLocales(Configuration conf) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getLocales(conf);
        } else {
            return LocaleListCompat.forLanguageTags(conf.locale.toLanguageTag());
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {}

        static LocaleListCompat getLocales(Configuration configuration) {
            return LocaleListCompat.forLanguageTags(configuration.getLocales().toLanguageTags());
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {}

        static LocaleList localeManagerGetSystemLocales(Object localeManager) {
            LocaleManager mLocaleManager = (LocaleManager) localeManager;
            return mLocaleManager.getSystemLocales();
        }

        static LocaleList localeManagerGetApplicationLocales(Object localeManager) {
            LocaleManager mLocaleManager = (LocaleManager) localeManager;
            return mLocaleManager.getApplicationLocales();
        }
    }
}
