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

import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Work.STATUS_RUNNING;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.ContentUriTriggers;
import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.DependencyDao;
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

/**
 * A Room database for keeping track of work statuses.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Database(entities = {Dependency.class, WorkSpec.class, WorkTag.class},
        version = 1)
@TypeConverters(value = {Arguments.class, ContentUriTriggers.class})
public abstract class WorkDatabase extends RoomDatabase {

    private static final String DB_NAME = "android.arch.background.workmanager.work";
    private static final String CLEANUP_SQL =
            "UPDATE workspec SET status=" + STATUS_ENQUEUED + " WHERE status=" + STATUS_RUNNING;

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
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        };
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
