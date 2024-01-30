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
package androidx.room.integration.kotlintestapp.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
class Pet(
    @PrimaryKey
    var mPetId: Int,
    var mUserId: Int,

    @ColumnInfo(name = "mPetName")
    var mName: String?,
    var mAdoptionDate: Date?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val pet = other as Pet
        if (mPetId != pet.mPetId) return false
        if (mUserId != pet.mUserId) return false
        if (if (mName != null) mName != pet.mName else pet.mName != null) return false
        return if (mAdoptionDate != null) mAdoptionDate == pet.mAdoptionDate else
            pet.mAdoptionDate == null
    }

    override fun hashCode(): Int {
        var result = mPetId
        result = 31 * result + mUserId
        result = 31 * result + if (mName != null) mName.hashCode() else 0
        result = 31 * result + if (mAdoptionDate != null) mAdoptionDate.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return ("Pet{" +
            "mPetId=" + mPetId +
            ", mUserId=" + mUserId +
            ", mName='" + mName + '\'' +
            ", mAdoptionDate=" + mAdoptionDate +
            '}')
    }
}
