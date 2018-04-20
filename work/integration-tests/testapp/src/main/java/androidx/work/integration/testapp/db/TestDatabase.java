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
package androidx.work.integration.testapp.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * A test database.
 */
@Database(entities = {WordCount.class, Image.class}, version = 1, exportSchema = false)
public abstract class TestDatabase extends RoomDatabase {

    private static TestDatabase sInstance;

    /**
     * Gets a static instance of the test database.
     *
     * @param context A {@link Context} for initialization (we use the application context)
     * @return The static instance of a {@link TestDatabase}
     */
    public static TestDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(
                    context.getApplicationContext(), TestDatabase.class, "testdb").build();
        }
        return sInstance;
    }

    /**
     * Gets the Data Access Object for the wordcount table.
     *
     * @return The Data Access Object for the wordcount table
     */
    public abstract WordCountDao getWordCountDao();

    /**
     * Gets the Data Access Object for the image table.
     *
     * @return The Data Access Object for the image table
     */
    public abstract ImageDao getImageDao();
}
