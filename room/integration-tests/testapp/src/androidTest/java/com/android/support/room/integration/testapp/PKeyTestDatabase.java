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

package com.android.support.room.integration.testapp;

import com.android.support.room.Dao;
import com.android.support.room.Database;
import com.android.support.room.Insert;
import com.android.support.room.Query;
import com.android.support.room.RoomDatabase;
import com.android.support.room.integration.testapp.vo.IntAutoIncPKeyEntity;
import com.android.support.room.integration.testapp.vo.IntegerAutoIncPKeyEntity;

@Database(entities = {IntAutoIncPKeyEntity.class, IntegerAutoIncPKeyEntity.class})
public abstract class PKeyTestDatabase extends RoomDatabase {
    public abstract IntPKeyDao intPKeyDao();
    public abstract IntegerPKeyDao integerPKeyDao();

    @Dao
    public interface IntPKeyDao {
        @Insert
        void insertMe(IntAutoIncPKeyEntity... items);
        @Query("select * from IntAutoIncPKeyEntity WHERE pKey = ?")
        IntAutoIncPKeyEntity getMe(int key);
    }

    @Dao
    public interface IntegerPKeyDao {
        @Insert
        void insertMe(IntegerAutoIncPKeyEntity items);
        @Query("select * from IntegerAutoIncPKeyEntity WHERE pKey = ?")
        IntegerAutoIncPKeyEntity getMe(int key);
    }
}
