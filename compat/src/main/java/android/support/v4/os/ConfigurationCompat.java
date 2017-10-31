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

import static android.os.Build.VERSION.SDK_INT;

import android.content.res.Configuration;

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
    public static LocaleListCompat getLocales(Configuration configuration) {
        if (SDK_INT >= 24) {
            return LocaleListCompat.wrap(configuration.getLocales());
        } else {
            return LocaleListCompat.create(configuration.locale);
        }
    }
}
