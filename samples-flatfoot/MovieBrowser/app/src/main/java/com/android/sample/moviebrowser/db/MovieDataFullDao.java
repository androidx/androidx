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

package com.android.sample.moviebrowser.db;

import com.android.sample.moviebrowser.MovieDataFull;
import com.android.support.room.Dao;
import com.android.support.room.Insert;
import com.android.support.room.Query;

/**
 * Data access object for movie database.
 */
@Dao
public interface MovieDataFullDao {
    /**
     * Load full data for a movie based on the IMDB ID.
     */
    @Query("select * from moviedatafull where imdbID = ?")
    MovieDataFull load(String imdbID);

    /**
     * Insert or update full data for a movie.
     */
    @Insert(onConflict = Insert.REPLACE)
    void insertOrReplace(MovieDataFull movieDataFull);
}
