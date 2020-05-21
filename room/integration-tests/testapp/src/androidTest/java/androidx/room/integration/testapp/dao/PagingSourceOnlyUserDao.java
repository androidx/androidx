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

package androidx.room.integration.testapp.dao;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.vo.User;

@SuppressWarnings("SameParameterValue")
@Dao
public abstract class PagingSourceOnlyUserDao {

    private final TestDatabase mDatabase;

    public PagingSourceOnlyUserDao(TestDatabase database) {
        mDatabase = database;
    }

    @Query("SELECT * FROM user where mAge > :age")
    public abstract PagingSource<Integer, User> loadPagedByAgePagingSource(int age);
}
