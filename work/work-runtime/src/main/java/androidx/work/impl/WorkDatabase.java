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
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_4_5;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_6_7;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_7_8;
import static androidx.work.impl.WorkDatabaseMigrations.MIGRATION_8_9;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_10;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_11;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_2;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_3;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_5;
import static androidx.work.impl.WorkDatabaseMigrations.VERSION_6;
import static androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.work.Data;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.Preference;
import androidx.work.impl.model.PreferenceDao;
import androidx.work.impl.model.RawWorkInfoDao;
import androidx.work.impl.model.SystemIdInfo;
import androidx.work.impl.model.SystemIdInfoDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkNameDao;
import androidx.work.impl.model.WorkProgress;
import androidx.work.impl.model.WorkProgressDao;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTag;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.model.WorkTypeConverters;

import java.util.concurrent.Executor;
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
        WorkName.class,
        WorkProgress.class,
        Preference.class},
        version = 12)
@TypeConverters(value = {Data.class, WorkTypeConverters.class})
public abstract class WorkDatabase extends RoomDatabase {
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

    private static final long PRUNE_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(1);

    /**
     * Creates an instance of the WorkDatabase.
     *
     * @param context         A context (this method will use the application context from it)
     * @param queryExecutor   An {@link Executor} that will be used to execute all async Room
     *                        queries.
     * @param useTestDatabase {@code true} to generate an in-memory database that allows main thread
     *                        access
     * @return The created WorkDatabase
     */
    @NonNull
    public static WorkDatabase create(
            @NonNull final Context context,
            @NonNull Executor queryExecutor,
            boolean useTestDatabase) {
        RoomDatabase.Builder<WorkDatabase> builder;
        if (useTestDatabase) {
            builder = Room.inMemoryDatabaseBuilder(context, WorkDatabase.class)
                    .allowMainThreadQueries();
        } else {
            String name = WorkDatabasePathHelper.getWorkDatabaseName();
            builder = Room.databaseBuilder(context, WorkDatabase.class, name);
            builder.openHelperFactory(new SupportSQLiteOpenHelper.Factory() {
                @NonNull
                @Override
                public SupportSQLiteOpenHelper create(
                        @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
                    SupportSQLiteOpenHelper.Configuration.Builder configBuilder =
                            SupportSQLiteOpenHelper.Configuration.builder(context);
                    configBuilder.name(configuration.name)
                            .callback(configuration.callback)
                            .noBackupDirectory(true);
                    FrameworkSQLiteOpenHelperFactory factory =
                            new FrameworkSQLiteOpenHelperFactory();
                    return factory.create(configBuilder.build());
                }
            });
        }

        return builder.setQueryExecutor(queryExecutor)
                .addCallback(generateCleanupCallback())
                .addMigrations(WorkDatabaseMigrations.MIGRATION_1_2)
                .addMigrations(
                        new WorkDatabaseMigrations.RescheduleMigration(context, VERSION_2,
                                VERSION_3))
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(
                        new WorkDatabaseMigrations.RescheduleMigration(context, VERSION_5,
                                VERSION_6))
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(new WorkDatabaseMigrations.WorkMigration9To10(context))
                .addMigrations(
                        new WorkDatabaseMigrations.RescheduleMigration(context, VERSION_10,
                                VERSION_11))
                .addMigrations(WorkDatabaseMigrations.MIGRATION_11_12)
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    static String getPruneSQL() {
        return PRUNE_SQL_FORMAT_PREFIX + getPruneDate() + PRUNE_SQL_FORMAT_SUFFIX;
    }

    static long getPruneDate() {
        return System.currentTimeMillis() - PRUNE_THRESHOLD_MILLIS;
    }

    /**
     * @return The Data Access Object for {@link WorkSpec}s.
     */
    @NonNull
    public abstract WorkSpecDao workSpecDao();

    /**
     * @return The Data Access Object for {@link Dependency}s.
     */
    @NonNull
    public abstract DependencyDao dependencyDao();

    /**
     * @return The Data Access Object for {@link WorkTag}s.
     */
    @NonNull
    public abstract WorkTagDao workTagDao();

    /**
     * @return The Data Access Object for {@link SystemIdInfo}s.
     */
    @NonNull
    public abstract SystemIdInfoDao systemIdInfoDao();

    /**
     * @return The Data Access Object for {@link WorkName}s.
     */
    @NonNull
    public abstract WorkNameDao workNameDao();

    /**
     * @return The Data Access Object for {@link WorkProgress}.
     */
    @NonNull
    public abstract WorkProgressDao workProgressDao();

    /**
     * @return The Data Access Object for {@link Preference}.
     */
    @NonNull
    public abstract PreferenceDao preferenceDao();

    /**
     * @return The Data Access Object which can be used to execute raw queries.
     */
    @NonNull
    public abstract RawWorkInfoDao rawWorkInfoDao();
}
