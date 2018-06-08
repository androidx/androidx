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

package androidx.room.integration.testapp;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.vo.IntAutoIncPKeyEntity;
import androidx.room.integration.testapp.vo.IntegerAutoIncPKeyEntity;
import androidx.room.integration.testapp.vo.IntegerPKeyEntity;
import androidx.room.integration.testapp.vo.ObjectPKeyEntity;

import java.util.List;

@Database(entities = {IntAutoIncPKeyEntity.class, IntegerAutoIncPKeyEntity.class,
        ObjectPKeyEntity.class, IntegerPKeyEntity.class}, version = 1,
        exportSchema = false)
public abstract class PKeyTestDatabase extends RoomDatabase {
    public abstract IntPKeyDao intPKeyDao();
    public abstract IntegerAutoIncPKeyDao integerAutoIncPKeyDao();
    public abstract ObjectPKeyDao objectPKeyDao();
    public abstract IntegerPKeyDao integerPKeyDao();

    @Dao
    public interface IntPKeyDao {
        @Insert
        void insertMe(IntAutoIncPKeyEntity... items);
        @Insert
        long insertAndGetId(IntAutoIncPKeyEntity item);

        @Insert
        long[] insertAndGetIds(IntAutoIncPKeyEntity... item);

        @Query("select * from IntAutoIncPKeyEntity WHERE pKey = :key")
        IntAutoIncPKeyEntity getMe(int key);

        @Query("select data from IntAutoIncPKeyEntity WHERE pKey IN(:ids)")
        List<String> loadDataById(long... ids);
    }

    @Dao
    public interface IntegerAutoIncPKeyDao {
        @Insert
        void insertMe(IntegerAutoIncPKeyEntity item);

        @Query("select * from IntegerAutoIncPKeyEntity WHERE pKey = :key")
        IntegerAutoIncPKeyEntity getMe(int key);

        @Insert
        long insertAndGetId(IntegerAutoIncPKeyEntity item);

        @Insert
        long[] insertAndGetIds(IntegerAutoIncPKeyEntity... item);

        @Query("select data from IntegerAutoIncPKeyEntity WHERE pKey IN(:ids)")
        List<String> loadDataById(long... ids);
    }

    @Dao
    public interface ObjectPKeyDao {
        @Insert
        void insertMe(ObjectPKeyEntity item);
    }

    @Dao
    public interface IntegerPKeyDao {
        @Insert
        void insertMe(IntegerPKeyEntity item);

        @Query("select * from IntegerPKeyEntity")
        List<IntegerPKeyEntity> loadAll();
    }
}
