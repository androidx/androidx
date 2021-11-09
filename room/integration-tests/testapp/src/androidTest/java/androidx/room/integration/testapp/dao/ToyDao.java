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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.integration.testapp.vo.Toy;

@Dao
public interface ToyDao {
    @Insert
    void insert(Toy... toys);

    @Query("SELECT * FROM Toy WHERE mId = :id")
    Toy getToy(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Toy toy);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertOrIgnore(Toy toy);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateOrReplace(Toy toy);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    int updateOrIgnore(Toy toy);
}
