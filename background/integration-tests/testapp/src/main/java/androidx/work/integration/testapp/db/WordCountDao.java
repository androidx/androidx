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

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link WordCount}.
 */
@Dao
public interface WordCountDao {

    /**
     * Inserts a {@link WordCount} into the database.
     *
     * @param wordCount The {@link WordCount} to insert
     */
    @Insert
    void insertWordCount(WordCount wordCount);

    /**
     * Gets all {@link WordCount}s in the database.
     *
     * @return A {@link LiveData} list of all {@link WordCount}s in the database
     */
    @Query("SELECT * FROM wordcount")
    LiveData<List<WordCount>> getWordCounts();

    /**
     * Clears the database.
     */
    @Query("DELETE FROM wordcount")
    void clear();
}
