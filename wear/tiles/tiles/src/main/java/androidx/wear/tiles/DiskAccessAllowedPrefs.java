/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.tiles;

import static java.util.Collections.emptyMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** Get versions of shared prefs that ignore strict mode and allows disk reads and writes. */
@RestrictTo(Scope.LIBRARY_GROUP)
final class DiskAccessAllowedPrefs {

    private final @NonNull SharedPreferences preferences;

    private DiskAccessAllowedPrefs(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Returns a simplified version of {@link SharedPreferences} wrapped to ignore disk read and
     * write StrictMode violations.
     */
    static @Nullable DiskAccessAllowedPrefs wrap(@NonNull Context context, @NonNull String name) {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            SharedPreferences sharedPref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            return sharedPref != null ? new DiskAccessAllowedPrefs(sharedPref) : null;
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    @NonNull Map<String, ?> getAll() {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            Map<String, ?> all = preferences.getAll();
            return all == null ? emptyMap() : all;
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    long getLong(@NonNull String key, long defValue) {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            return preferences.getLong(key, defValue);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    String getString(@NonNull String key, String defValue) {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            return preferences.getString(key, defValue);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    void putLong(@NonNull String key, long value) {
        ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
        try {
            preferences.edit().putLong(key, value).apply();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    void putString(@NonNull String key, @NonNull String value) {
        ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
        try {
            preferences.edit().putString(key, value).apply();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    boolean contains(@NonNull String key) {
        ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try {
            return preferences.contains(key);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }

    void remove(@NonNull String key) {
        ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
        try {
            preferences.edit().remove(key).apply();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }
}
