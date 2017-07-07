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

package com.android.sample.githubbrowser.db;

import com.android.sample.githubbrowser.data.ContributorSearchData;
import com.android.sample.githubbrowser.data.GeneralRepoSearchData;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;
import com.android.sample.githubbrowser.data.SearchQueryData;
import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

/**
 * Database for Github entities.
 */
@Database(version = 1, entities = {PersonData.class, SearchQueryData.class,
        GeneralRepoSearchData.class, RepositoryData.class, ContributorSearchData.class})
public abstract class GithubDatabase extends RoomDatabase {
    /**
     * Gets the data access object.
     */
    public abstract GithubDao getGithubDao();
}
