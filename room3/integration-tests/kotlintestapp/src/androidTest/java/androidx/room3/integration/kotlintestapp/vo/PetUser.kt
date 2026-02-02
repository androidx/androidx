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
package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.TypeConverters
import androidx.room3.integration.kotlintestapp.TestDatabase
import java.util.Date

@Entity
@TypeConverters(TestDatabase.Converters::class)
class PetUser {
    @PrimaryKey var id = 0
    var name: String? = null
    var lastName: String? = null
    var age = 0
    var admin = false
    var weight = 0f
    var birthday: Date? = null

    @ColumnInfo(name = "custommm", collate = ColumnInfo.NOCASE) var customField: String? = null

    // bit flags
    lateinit var workDays: Set<Day>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PetUser

        if (id != other.id) return false
        if (age != other.age) return false
        if (admin != other.admin) return false
        if (weight != other.weight) return false
        if (name != other.name) return false
        if (lastName != other.lastName) return false
        if (birthday != other.birthday) return false
        if (customField != other.customField) return false
        if (workDays != other.workDays) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + age
        result = 31 * result + admin.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        result = 31 * result + (birthday?.hashCode() ?: 0)
        result = 31 * result + (customField?.hashCode() ?: 0)
        result = 31 * result + workDays.hashCode()
        return result
    }

    override fun toString(): String {
        return "PetUser(id=$id, name=$name, lastName=$lastName, age=$age, admin=$admin, weight=$weight, birthday=$birthday, customField=$customField, workDays=$workDays)"
    }
}
