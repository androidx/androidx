/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.rxjava2

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import io.reactivex.Single
import kotlinx.coroutines.rx2.await

/**
 * Client implemented migration interface.
 **/
public interface RxSharedPreferencesMigration<T> {
    /**
     * Whether or not the migration should be run. This can be used to skip a read from the
     * SharedPreferences.
     *
     * @param currentData the most recently persisted data
     * @return a Single indicating whether or not the migration should be run.
     */
    public fun shouldMigrate(currentData: T): Single<Boolean> {
        return Single.just(true)
    }

    /**
     * Maps SharedPreferences into T. Implementations should be idempotent
     * since this may be called multiple times. See [DataMigration.migrate] for more
     * information. The method accepts a SharedPreferencesView which is the view of the
     * SharedPreferences to migrate from (limited to [keysToMigrate] and a T which represent
     * the current data. The function must return the migrated data.
     *
     * If SharedPreferences is empty or does not contain any keys which you specified, this
     * callback will not run.
     *
     * @param sharedPreferencesView the current state of the SharedPreferences
     * @param currentData the most recently persisted data
     * @return a Single of the updated data
     */
    public fun migrate(sharedPreferencesView: SharedPreferencesView, currentData: T): Single<T>
}

/**
 * RxSharedPreferencesMigrationBuilder for the RxSharedPreferencesMigration.
 */
@SuppressLint("TopLevelBuilder")
public class RxSharedPreferencesMigrationBuilder<T>
/**
 * Construct a RxSharedPreferencesMigrationBuilder.
 *
 * @param context the Context used for getting the SharedPreferences.
 * @param sharedPreferencesName the name of the SharedPreference from which to migrate.
 * @param rxSharedPreferencesMigration the user implemented migration for this SharedPreference.
 */
constructor(
    private val context: Context,
    private val sharedPreferencesName: String,
    private val rxSharedPreferencesMigration: RxSharedPreferencesMigration<T>
) {

    private var keysToMigrate: Set<String>? = null

    /**
     * Set the list of keys to migrate. The keys will be mapped to datastore.Preferences with
     * their same values. If the key is already present in the new Preferences, the key
     * will not be migrated again. If the key is not present in the SharedPreferences it
     * will not be migrated.
     *
     * This method is optional and if keysToMigrate is not set, all keys will be migrated from the
     * existing SharedPreferences.
     *
     * @param keys the keys to migrate
     * @return this
     */
    @Suppress("MissingGetterMatchingBuilder")
    public fun setKeysToMigrate(vararg keys: String): RxSharedPreferencesMigrationBuilder<T> =
        apply {
            keysToMigrate = setOf(*keys)
        }

    /**
     * Build and return the DataMigration instance.
     *
     * @return the DataMigration.
     */
    public fun build(): DataMigration<T> {
        return if (keysToMigrate == null) {
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = sharedPreferencesName,
                migrate = { spView, curData ->
                    rxSharedPreferencesMigration.migrate(spView, curData).await()
                },
                shouldRunMigration = { curData ->
                    rxSharedPreferencesMigration.shouldMigrate(curData).await()
                }
            )
        } else {
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = sharedPreferencesName,
                migrate = { spView, curData ->
                    rxSharedPreferencesMigration.migrate(spView, curData).await()
                },
                keysToMigrate = keysToMigrate!!,
                shouldRunMigration = { curData ->
                    rxSharedPreferencesMigration.shouldMigrate(curData).await()
                }
            )
        }
    }
}
