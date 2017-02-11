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

package com.android.flatfoot.apireviewdemo.common.github;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.support.lifecycle.LiveData;
import com.android.support.room.Dao;
import com.android.support.room.Insert;
import com.android.support.room.Query;

/**
 * Data access object for github data table.
 */
@Dao
public interface GithubDao {
    /**
     * Load full data for a person based on the login.
     */
    @Query("select * from person where login = ?")
    LiveData<Person> getLivePerson(String login);

    /**
     * Load full data for a person based on the login.
     */
    @Query("select * from person where login = ?")
    Person getPerson(String login);

    /**
     * Insert or update full data for a person.
     */
    @Insert(onConflict = Insert.REPLACE)
    void insertOrReplacePerson(Person personData);
}
