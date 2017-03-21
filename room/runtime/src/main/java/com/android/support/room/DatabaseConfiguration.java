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

package com.android.support.room;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.support.db.SupportSQLiteOpenHelper;

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

    DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull RoomDatabase.MigrationContainer migrationContainer) {
        this.sqliteOpenHelperFactory = sqliteOpenHelperFactory;
        this.context = context;
        this.name = name;
        this.migrationContainer = migrationContainer;
    }
}
