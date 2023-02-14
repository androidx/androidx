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

package androidx.preference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * A data store interface to be implemented and provided to the {@link Preference} framework.
 * This can be used to replace the default {@link android.content.SharedPreferences}, if needed.
 *
 * <p>In most cases you want to use {@link android.content.SharedPreferences} as it is
 * automatically backed up and migrated to new devices. However, providing custom data store to
 * preferences can be useful if your app stores its preferences in a local database, cloud, or
 * they are device specific like "Developer settings". It might be also useful when you want to
 * use the preferences UI but the data is not supposed to be stored at all because they are only
 * valid per session.
 *
 * <p>Once a put method is called it is the full responsibility of the data store implementation
 * to safely store the given values. Time expensive operations need to be done in the background
 * to prevent from blocking the UI. You also need to have a plan on how to serialize the data in
 * case the activity holding this object gets destroyed.
 *
 * <p>By default, all "put" methods throw {@link UnsupportedOperationException}.
 *
 * @see Preference#setPreferenceDataStore(PreferenceDataStore)
 * @see PreferenceManager#setPreferenceDataStore(PreferenceDataStore)
 */
public abstract class PreferenceDataStore {

    /**
     * Sets a {@link String} value to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see #getString(String, String)
     */
    public void putString(@NonNull String key, @Nullable String value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Sets a set of {@link String}s to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key    The name of the preference to modify
     * @param values The set of new values for the preference
     * @see #getStringSet(String, Set)
     */
    public void putStringSet(@NonNull String key, @Nullable Set<String> values) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Sets an {@link Integer} value to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see #getInt(String, int)
     */
    public void putInt(@NonNull String key, int value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Sets a {@link Long} value to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see #getLong(String, long)
     */
    public void putLong(@NonNull String key, long value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Sets a {@link Float} value to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see #getFloat(String, float)
     */
    public void putFloat(@NonNull String key, float value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Sets a {@link Boolean} value to the data store.
     *
     * <p>Once the value is set the data store is responsible for holding it.
     *
     * @param key   The name of the preference to modify
     * @param value The new value for the preference
     * @see #getBoolean(String, boolean)
     */
    public void putBoolean(@NonNull String key, boolean value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    /**
     * Retrieves a {@link String} value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see #putString(String, String)
     */
    @Nullable
    public String getString(@NonNull String key, @Nullable String defValue) {
        return defValue;
    }

    /**
     * Retrieves a set of Strings from the data store.
     *
     * @param key       The name of the preference to retrieve
     * @param defValues Values to return if this preference does not exist in the storage
     * @return The values from the data store or the default return values
     * @see #putStringSet(String, Set)
     */
    @Nullable
    public Set<String> getStringSet(@NonNull String key, @Nullable Set<String> defValues) {
        return defValues;
    }

    /**
     * Retrieves an {@link Integer} value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see #putInt(String, int)
     */
    public int getInt(@NonNull String key, int defValue) {
        return defValue;
    }

    /**
     * Retrieves a {@link Long} value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see #putLong(String, long)
     */
    public long getLong(@NonNull String key, long defValue) {
        return defValue;
    }

    /**
     * Retrieves a {@link Float} value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return The value from the data store or the default return value
     * @see #putFloat(String, float)
     */
    public float getFloat(@NonNull String key, float defValue) {
        return defValue;
    }

    /**
     * Retrieves a {@link Boolean} value from the data store.
     *
     * @param key      The name of the preference to retrieve
     * @param defValue Value to return if this preference does not exist in the storage
     * @return the value from the data store or the default return value
     * @see #getBoolean(String, boolean)
     */
    public boolean getBoolean(@NonNull String key, boolean defValue) {
        return defValue;
    }
}

