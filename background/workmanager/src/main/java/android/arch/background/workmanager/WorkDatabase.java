/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * A Room database for keeping track of work statuses.
 */
@Database(entities = {WorkItem.class}, version = 1)
public abstract class WorkDatabase extends RoomDatabase {

    private static final String DB_NAME_PREFIX = "android.arch.background.workmanager.work.";

    private static WorkDatabase sInstance;

    /**
     * Returns a static instance of the WorkDatabase.
     *
     * @param context A context (this method will use the application context from it)
     * @param name The database name (will be prefixed by {@code DB_NAME_PREFIX})
     * @return The singleton WorkDatabase for this process
     */
    public static WorkDatabase getInstance(Context context, String name) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    WorkDatabase.class,
                    DB_NAME_PREFIX + name)
                    .build();
        }
        return sInstance;
    }

    /**
     * @return The Data Access Object for {@link WorkItem}s.
     */
    public abstract WorkItemDao workItemDao();
}
