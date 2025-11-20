/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.integration.kotlintestapp.vo.DataClassFromDependency
import androidx.room3.integration.kotlintestapp.vo.EmbeddedFromDependency
import androidx.room3.integration.kotlintestapp.vo.PojoFromDependency
import androidx.room3.integration.kotlintestapp.vo.RelationFromDependency

@Dao
interface DependencyDao {
    @Query("select * from DataClassFromDependency") fun selectAll(): List<DataClassFromDependency>

    @Query("select * from DataClassFromDependency where id = :id LIMIT 1")
    fun findEmbedded(id: Int): EmbeddedFromDependency?

    @Query("select * from DataClassFromDependency where id = :id LIMIT 1")
    fun findPojo(id: Int): PojoFromDependency?

    @Query("select * from DataClassFromDependency where id = :id LIMIT 1")
    fun findById(id: Int): DataClassFromDependency?

    @Transaction
    @Query("WITH nameTable( sharedName ) AS ( SELECT :name ) SELECT * from nameTable")
    fun relation(name: String): RelationFromDependency?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg input: DataClassFromDependency)
}
