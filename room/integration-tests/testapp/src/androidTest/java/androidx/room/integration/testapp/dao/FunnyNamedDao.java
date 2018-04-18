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

package androidx.room.integration.testapp.dao;

import static androidx.room.integration.testapp.vo.FunnyNamedEntity.COLUMN_ID;
import static androidx.room.integration.testapp.vo.FunnyNamedEntity.TABLE_NAME;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.integration.testapp.vo.FunnyNamedEntity;

import java.util.List;

@Dao
public interface FunnyNamedDao {
    String SELECT_ONE = "select * from \"" +  TABLE_NAME + "\" WHERE \"" + COLUMN_ID + "\" = :id";
    @Insert
    void insert(FunnyNamedEntity... entities);
    @Delete
    void delete(FunnyNamedEntity... entities);
    @Update
    void update(FunnyNamedEntity... entities);

    @Query("select * from \"" +  TABLE_NAME + "\" WHERE \"" + COLUMN_ID + "\" IN (:ids)")
    List<FunnyNamedEntity> loadAll(int... ids);

    @Query(SELECT_ONE)
    LiveData<FunnyNamedEntity> observableOne(int id);

    @Query(SELECT_ONE)
    FunnyNamedEntity load(int id);
}
