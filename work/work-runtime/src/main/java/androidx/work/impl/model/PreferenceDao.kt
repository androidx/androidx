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
package androidx.work.impl.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * A Data Access Object for accessing [Preference]s.
 */
@Dao
interface PreferenceDao {
    /**
     * Inserts a [Preference] into the database.
     *
     * @param preference The [Preference] entity to be inserted into the database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPreference(preference: Preference)

    /**
     * Fetches the value for the given [String] key.
     *
     * @param key The [String] key
     * @return The value if present for the given [String] key
     */
    @Query("SELECT long_value FROM Preference where `key`=:key")
    fun getLongValue(key: String): Long?

    /**
     * Fetches a [LiveData] of [Long] for the given [String] key.
     *
     * @param key The [String] key
     * @return The [LiveData] of  [Long] if present for the given
     * [String] key
     */
    @Query("SELECT long_value FROM Preference where `key`=:key")
    fun getObservableLongValue(key: String): LiveData<Long?>
}
