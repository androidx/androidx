/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager.impl;

import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.BLOCKED;
import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.CANCELLED;
import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.ENQUEUED;
import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.FAILED;
import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.RUNNING;
import static android.arch.background.workmanager.impl.model.EnumTypeConverters.StatusIds.SUCCEEDED;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.ContentUriTriggers;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.DependencyDao;
import android.arch.background.workmanager.impl.model.EnumTypeConverters;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.impl.model.WorkTag;
import android.arch.background.workmanager.impl.model.WorkTagDao;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.concurrent.TimeUnit;

/**
 * A Room database for keeping track of work statuses.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Database(entities = {Dependency.class, WorkSpec.class, WorkTag.class},
        version = 1)
@TypeConverters(value = {Arguments.class, ContentUriTriggers.class, EnumTypeConverters.class})
public abstract class WorkDatabase extends RoomDatabase {

    private static final String DB_NAME = "android.arch.background.workmanager.work";
    private static final String CLEANUP_SQL = "UPDATE workspec SET status=" + ENQUEUED
            + " WHERE status=" + RUNNING;
    private static final String PRUNE_SQL_PREFIX = "DELETE FROM workspec WHERE status IN ("
            + SUCCEEDED + ", " + FAILED + ", " + CANCELLED + ") AND period_start_time < ";
    private static final String BLOCKED_WITHOUT_PREREQUISITES_WHERE_CLAUSE =
            "status=" + BLOCKED + " AND id NOT IN "
            + "(SELECT DISTINCT work_spec_id FROM dependency)";

    private static final long PRUNE_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(7);

    /**
     * Creates an instance of the WorkDatabase.
     *
     * @param context A context (this method will use the application context from it)
     * @param useTestDatabase {@code true} to generate an in-memory database that allows main thread
     *                        access
     * @return The created WorkDatabase
     */
    public static WorkDatabase create(Context context, boolean useTestDatabase) {
        RoomDatabase.Builder<WorkDatabase> builder;
        if (useTestDatabase) {
            builder = Room.inMemoryDatabaseBuilder(context, WorkDatabase.class)
                    .allowMainThreadQueries();
        } else {
            builder = Room.databaseBuilder(context, WorkDatabase.class, DB_NAME);
        }
        return builder.addCallback(generateCleanupCallback()).build();
    }

    static Callback generateCleanupCallback() {
        return new Callback() {
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                db.beginTransaction();
                db.execSQL(CLEANUP_SQL);

                // Delete everything that's finished and older than PRUNE_THRESHOLD_MILLIS.
                db.execSQL(PRUNE_SQL_PREFIX + getPruneDate(), new Object[0]);
                // Keep deleting everything that's blocked but has no prerequisites (it had a failed
                // or cancelled prerequisite that got deleted in the above step).
                int deletedCount;
                do {
                    deletedCount = db.delete("workspec",
                            BLOCKED_WITHOUT_PREREQUISITES_WHERE_CLAUSE,
                            null);
                } while (deletedCount > 0);

                db.setTransactionSuccessful();
                db.endTransaction();
            }
        };
    }

    static long getPruneDate() {
        return System.currentTimeMillis() - PRUNE_THRESHOLD_MILLIS;
    }

    /**
     * @return The Data Access Object for {@link WorkSpec}s.
     */
    public abstract WorkSpecDao workSpecDao();

    /**
     * @return The Data Access Object for {@link Dependency}s.
     */
    public abstract DependencyDao dependencyDao();

    /**
     * @return The Data Access Object for {@link WorkTag}s.
     */
    public abstract WorkTagDao workTagDao();
}
