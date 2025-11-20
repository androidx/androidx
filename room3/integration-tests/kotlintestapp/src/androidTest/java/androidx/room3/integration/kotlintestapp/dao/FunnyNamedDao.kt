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
package androidx.room3.integration.kotlintestapp.dao

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity.Companion.COLUMN_ID
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity.Companion.TABLE_NAME

@Dao
interface FunnyNamedDao {
    @Insert fun insert(vararg entities: FunnyNamedEntity)

    @Delete fun delete(vararg entities: FunnyNamedEntity)

    @Update fun update(vararg entities: FunnyNamedEntity)

    @Query("select * from \"$TABLE_NAME\" WHERE \"$COLUMN_ID\" IN (:ids)")
    fun loadAll(vararg ids: Int): List<FunnyNamedEntity>

    @Query("select * from \"$TABLE_NAME\" WHERE \"$COLUMN_ID\" = :id")
    fun observableOne(id: Int): LiveData<FunnyNamedEntity>

    @Query("select * from \"$TABLE_NAME\" WHERE \"$COLUMN_ID\" = :id")
    fun load(id: Int): FunnyNamedEntity?
}
