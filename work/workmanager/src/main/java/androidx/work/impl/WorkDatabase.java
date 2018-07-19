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

package androidx.work.impl;

import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_3_4;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_2;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_3;
import static androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES;
import static androidx.work.impl.model.WorkTypeConverters.StateIds.ENQUEUED;
import static androidx.work.impl.model.WorkTypeConverters.StateIds.RUNNING;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.Data;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkNameDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTag;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.model.WorkTypeConverters;

import java.util.concurrent.TimeUnit;

/**
 * A Room database for keeping track of work states.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Database(entities = {
        Dependency.class,
        WorkSpec.class,
        WorkTag.class,
        SystemIdInfo.class,
        WorkName.class},
        version = 4)
@TypeConverters(value = {Data.class, WorkTypeConverters.class})
public abstract class WorkDatabase extends RoomDatabase {

    private static final String DB_NAME = "androidx.work.workdb";
    private static final String CLEANUP_SQL = "UPDATE workspec "
            + "SET state=" + ENQUEUED + ","
            + " schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
            + " WHERE state=" + RUNNING;

    // Delete rows in the workspec table that...
    private static final String PRUNE_SQL_FORMAT_PREFIX = "DELETE FROM workspec WHERE "
            // are completed...
            + "state IN " + COMPLETED_STATES + " AND "
            // and the minimum retention time has expired...
            + "(period_start_time + minimum_retention_duration) < ";
    // and all dependents are completed.
    private static final String PRUNE_SQL_FORMAT_SUFFIX = " AND "
            + "(SELECT COUNT(*)=0 FROM dependency WHERE "
            + "    prerequisite_id=id AND "
            + "    work_spec_id NOT IN "
            + "        (SELECT id FROM workspec WHERE state IN " + COMPLETED_STATES + "))";

    private static final long PRUNE_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(7);

    /**
     * Creates an instance of the WorkDatabase.
     *
     * @param context         A context (this method will use the application context from it)
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

        return builder.addCallback(generateCleanupCallback())
                .addMigrations(WorkDatabaseMigrations.MIGRATION_1_2)
                .addMigrations(
                        new WorkDatabaseMigrations.WorkMigration(context, VERSION_2, VERSION_3))
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build();
    }

    static Callback generateCleanupCallback() {
        return new Callback() {
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                db.beginTransaction();
                try {
                    db.execSQL(CLEANUP_SQL);

                    // Prune everything that is completed, has an expired retention time, and has no
                    // active dependents:
                    db.execSQL(getPruneSQL());

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        };
    }

    private static String getPruneSQL() {
        return PRUNE_SQL_FORMAT_PREFIX + getPruneDate() + PRUNE_SQL_FORMAT_SUFFIX;
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

    /**
     * @return The Data Access Object for {@link SystemIdInfo}s.
     */
    public abstract SystemIdInfoDao systemIdInfoDao();

    /**
     * @return The Data Access Object for {@link WorkName}s.
     */
    public abstract WorkNameDao workNameDao();
}
