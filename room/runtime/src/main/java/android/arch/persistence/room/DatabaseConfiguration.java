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

package android.arch.persistence.room;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.List;

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
     * If true, Room should crash if a migration is missing.
     */
    public final boolean requireMigration;

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param requireMigration True if Room should require a valid migration if version changes,
     *                        instead of recreating the tables.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull RoomDatabase.MigrationContainer migrationContainer,
            @Nullable List<RoomDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            boolean requireMigration) {
        this.sqliteOpenHelperFactory = sqliteOpenHelperFactory;
        this.context = context;
        this.name = name;
        this.migrationContainer = migrationContainer;
        this.callbacks = callbacks;
        this.allowMainThreadQueries = allowMainThreadQueries;
        this.requireMigration = requireMigration;
    }
}
