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

package androidx.datastore.rxjava3;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface for migrations to DataStore. Methods on this migration ([shouldMigrate], [migrate]
 * and [cleanUp]) may be called multiple times, so their implementations must be idempotent.
 * These methods may be called multiple times if DataStore encounters issues when writing the
 * newly migrated data to disk or if any migration installed in the same DataStore throws an
 * Exception.
 *
 * If you're migrating from SharedPreferences see [SharedPreferencesMigration].
 *
 * @param <T> the exception type
 */
public interface RxDataMigration<T> {

    /**
     * Return whether this migration needs to be performed. If this returns false, no migration or
     * cleanup will occur. Apps should do the cheapest possible check to determine if this migration
     * should run, since this will be called every time the DataStore is initialized. This method
     * may be run multiple times when any failure is encountered.
     *
     * Note that this will always be called before each call to [migrate].
     *
     * @param currentData the current data (which might already be populated from previous runs of
     *                    this or other migrations). Only Nullable if the type used with DataStore
     *                    is Nullable.
     */
    @NonNull Single<Boolean> shouldMigrate(@Nullable T currentData);

    /**
     * Perform the migration. Implementations should be idempotent since this may be called
     * multiple times. If migrate fails, DataStore will not commit any data to disk, cleanUp will
     * not be called, and the exception will be propagated back to the DataStore call that
     * triggered the migration. Future calls to DataStore will result in DataMigrations being
     * attempted again. This method may be run multiple times when any failure is encountered.
     *
     * Note that this will always be called before a call to [cleanUp].
     *
     * @param currentData the current data (it might be populated from other migrations or from
     *                    manual changes before this migration was added to the app). Only
     *                    Nullable if the type used with DataStore is Nullable.
     * @return The migrated data.
     */
    @NonNull Single<T> migrate(@Nullable T currentData);

    /**
     * Clean up any old state/data that was migrated into the DataStore. This will not be called
     * if the migration fails. If cleanUp throws an exception, the exception will be propagated
     * back to the DataStore call that triggered the migration and future calls to DataStore will
     * result in DataMigrations being attempted again. This method may be run multiple times when
     * any failure is encountered.
     */
    @NonNull Completable cleanUp();
}
