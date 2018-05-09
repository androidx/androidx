/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.os.LocaleList;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.os.LocaleListCompat;

import java.time.ZonedDateTime;
import java.util.Calendar;

/**
 * Provides utils to convert between platform and support library objects.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(28)
final class ConvertUtils {

    private ConvertUtils() {
    }

    @Nullable
    static LocaleList unwrapLocalListCompat(@Nullable LocaleListCompat localeListCompat) {
        if (localeListCompat == null) {
            return null;
        }
        return (LocaleList) localeListCompat.unwrap();
    }

    @Nullable
    static android.view.textclassifier.TextClassifier.EntityConfig toPlatformEntityConfig(
            @Nullable TextClassifier.EntityConfig entityConfig) {
        if (entityConfig == null) {
            return null;
        }
        return TextClassifier.EntityConfig.Convert.toPlatform(entityConfig);
    }

    @Nullable
    static ZonedDateTime buildZonedDateTimeFromCalendar(
            @Nullable Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
    }
}
