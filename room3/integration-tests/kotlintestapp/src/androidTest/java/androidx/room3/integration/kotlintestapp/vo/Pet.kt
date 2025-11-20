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
package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.util.Date

@Entity
class Pet(
    @PrimaryKey var petId: Int,
    var userId: Int,
    @ColumnInfo(name = "petName") var name: String?,
    var adoptionDate: Date?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pet

        if (petId != other.petId) return false
        if (userId != other.userId) return false
        if (name != other.name) return false
        if (adoptionDate != other.adoptionDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = petId
        result = 31 * result + userId
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (adoptionDate?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Pet(petId=$petId, userId=$userId, name=$name, adoptionDate=$adoptionDate)"
    }
}
