/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.model;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * A Data Access Object for accessing {@link Preference}s.
 */
@Dao
public interface PreferenceDao {
    /**
     * Inserts a {@link Preference} into the database.
     *
     * @param preference The {@link Preference} entity to be inserted into the database
     */
    @Insert(onConflict = REPLACE)
    void insertPreference(@NonNull Preference preference);

    /**
     * Fetches the value for the given {@link String} key.
     *
     * @param key The {@link String} key
     * @return The value if present for the given {@link String} key
     */
    @Nullable
    @Query("SELECT long_value FROM Preference where `key`=:key")
    Long getLongValue(@NonNull String key);

    /**
     * Fetches a {@link LiveData} of {@link Long} for the given {@link String} key.
     *
     * @param key The {@link String} key
     * @return The {@link LiveData} of  {@link Long} if present for the given
     * {@link String} key
     */
    @NonNull
    @Query("SELECT long_value FROM Preference where `key`=:key")
    LiveData<Long> getObservableLongValue(@NonNull String key);
}
