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

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomWarnings
import androidx.room3.integration.kotlintestapp.vo.Coordinates
import androidx.room3.integration.kotlintestapp.vo.School
import androidx.room3.integration.kotlintestapp.vo.SchoolRef

@Dao
abstract class SchoolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun insert(vararg schools: School)

    @Query("SELECT * from School WHERE address_street LIKE '%' || :street || '%'")
    abstract fun findByStreet(street: String): List<School>

    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT id, name, manager_uId FROM School")
    abstract fun schoolAndManagerNames(): List<School>

    @Query("SELECT id, name, manager_uId FROM School")
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    abstract fun schoolAndManagerIdsAsPoKo(): List<SchoolRef>

    @Query("SELECT address_lat as lat, address_lng as lng FROM School WHERE id = :schoolId")
    abstract fun loadCoordinates(schoolId: Int): Coordinates

    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT id, address_lat, address_lng FROM School WHERE id = :schoolId")
    abstract fun loadCoordinatesAsSchool(schoolId: Int): School
}
