/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.util.List;
import java.util.Set;

/**
 * Configuration class for a {@link RoomDatabase}.
 */
@SuppressWarnings("WeakerAccess")
public class DatabaseConfiguration {
    /**
     * The factory to use to access the database.
     */
    @NonNull
    public final SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory;
    /**
     * The context to use while connecting to the database.
     */
    @NonNull
    public final Context context;
    /**
     * The name of the database file or null if it is an in-memory database.
     */
    @Nullable
    public final String name;

    /**
     * Collection of available migrations.
     */
    @NonNull
    public final RoomDatabase.MigrationContainer migrationContainer;

    @Nullable
    public final List<RoomDatabase.Callback> callbacks;

    /**
     * Whether Room should throw an exception for queries run on the main thread.
     */
    public final boolean allowMainThreadQueries;

    /**
     * The journal mode for this database.
     */
    public final RoomDatabase.JournalMode journalMode;

    /**
     * If true, Room should crash if a migration is missing.
     */
    public final boolean requireMigration;

    /**
     * The collection of schema versions from which migrations aren't required.
     */
    private final Set<Integer> mMigrationNotRequiredFrom;

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param requireMigration True if Room should require a valid migration if version changes,
     *                        instead of recreating the tables.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull RoomDatabase.MigrationContainer migrationContainer,
            @Nullable List<RoomDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            RoomDatabase.JournalMode journalMode,
            boolean requireMigration,
            @Nullable Set<Integer> migrationNotRequiredFrom) {
        this.sqliteOpenHelperFactory = sqliteOpenHelperFactory;
        this.context = context;
        this.name = name;
        this.migrationContainer = migrationContainer;
        this.callbacks = callbacks;
        this.allowMainThreadQueries = allowMainThreadQueries;
        this.journalMode = journalMode;
        this.requireMigration = requireMigration;
        this.mMigrationNotRequiredFrom = migrationNotRequiredFrom;
    }

    /**
     * Returns whether a migration is required from the specified version.
     *
     * @param version  The schema version.
     * @return True if a valid migration is required, false otherwise.
     */
    public boolean isMigrationRequiredFrom(int version) {
        // Migrations are required from this version if we generally require migrations AND EITHER
        // there are no exceptions OR the supplied version is not one of the exceptions.
        return requireMigration
                && (mMigrationNotRequiredFrom == null
                || !mMigrationNotRequiredFrom.contains(version));

    }
}
