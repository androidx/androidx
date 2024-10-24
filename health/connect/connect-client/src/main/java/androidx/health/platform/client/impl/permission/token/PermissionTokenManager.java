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

package androidx.health.platform.client.impl.permission.token;

import android.content.Context;
import android.content.SharedPreferences;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Class to store and retrieve the token was given by the service side. */
public final class PermissionTokenManager {
    private static final String PREFERENCES_FILE_NAME = "PermissionTokenManager.healthdata";
    private static final String KEY_TOKEN = "token";

    private PermissionTokenManager() {}

    /** Retrieves current token. */
    public static @Nullable String getCurrentToken(@NonNull Context context) {
        return getSharedPreferences(context).getString(KEY_TOKEN, null);
    }

    /** Sets current token. */
    public static void setCurrentToken(@NonNull Context context, @Nullable String token) {
        getSharedPreferences(context).edit().putString(KEY_TOKEN, token).commit();
    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }
}
