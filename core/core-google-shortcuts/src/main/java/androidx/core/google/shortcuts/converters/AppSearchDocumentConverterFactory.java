/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.google.shortcuts.converters;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * A factory for {@link AppSearchDocumentConverter}. Given a schema type, if the schema type has
 * a supported converter, then return that converter. Otherwise a default
 * {@link GenericDocumentConverter} will be returned.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class AppSearchDocumentConverterFactory {
    private static final String TAG = "AppSearchDocumentConver"; // NOTYPO

    private static final String TIMER_SCHEMA_TYPE = "builtin:Timer";
    private static final String ALARM_SCHEMA_TYPE = "builtin:Alarm";
    private static final String ALARM_INSTANCE_SCHEMA_TYPE = "builtin:AlarmInstance";

    /**
     * Returns a {@link AppSearchDocumentConverter} given a schema type. If the schema type is not
     * supported, then the {@link GenericDocumentConverter} will be returned.
     */
    @NonNull
    public static AppSearchDocumentConverter getConverter(@NonNull String schemaType) {
        Preconditions.checkNotNull(schemaType);

        if (TIMER_SCHEMA_TYPE.equals(schemaType)) {
            return new TimerConverter();
        } else if (ALARM_SCHEMA_TYPE.equals(schemaType)) {
            return new AlarmConverter();
        } else if (ALARM_INSTANCE_SCHEMA_TYPE.equals(schemaType)) {
            return new AlarmInstanceConverter();
        }

        Log.w(TAG, "schema type " + schemaType + " does not have a Typed Converter registered. "
                + "Returning the default GenericDocument converter.");
        return new GenericDocumentConverter();
    }

    private AppSearchDocumentConverterFactory() {}
}
