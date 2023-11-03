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
package androidx.room.integration.kotlintestapp.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.integration.kotlintestapp.TestDatabase
import java.lang.Float.floatToIntBits
import java.util.Date

@Entity
@TypeConverters(TestDatabase.Converters::class)
class PetUser {
    @PrimaryKey
    var mId = 0
    var mName: String? = null
    var mLastName: String? = null
    var mAge = 0
    var mAdmin = false
    var mWeight = 0f
    var mBirthday: Date? = null

    @ColumnInfo(name = "custommm", collate = ColumnInfo.NOCASE)
    var mCustomField: String? = null

    // bit flags
    lateinit var mWorkDays: Set<Day>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val user = other as PetUser
        if (mId != user.mId) return false
        if (mAge != user.mAge) return false
        if (mAdmin != user.mAdmin) return false
        if (user.mWeight.compareTo(mWeight) != 0) return false
        if (if (mName != null) mName != user.mName else user.mName != null) return false
        if (if (mLastName != null) mLastName != user.mLastName else user.mLastName != null) {
            return false
        }
        if (if (mBirthday != null) mBirthday != user.mBirthday else user.mBirthday != null) {
            return false
        }
        if (if (mCustomField != null) mCustomField != user.mCustomField else
            user.mCustomField != null) {
            return false
        }
        return mWorkDays == user.mWorkDays
    }

    override fun hashCode(): Int {
        var result = mId
        result = 31 * result + if (mName != null) mName.hashCode() else 0
        result = 31 * result + if (mLastName != null) mLastName.hashCode() else 0
        result = 31 * result + mAge
        result = 31 * result + if (mAdmin) 1 else 0
        result = 31 * result + if (mWeight != +0.0f) floatToIntBits(mWeight) else 0
        result = 31 * result + if (mBirthday != null) mBirthday.hashCode() else 0
        result = 31 * result + if (mCustomField != null) mCustomField.hashCode() else 0
        result = 31 * result + mWorkDays.hashCode()
        return result
    }

    override fun toString(): String {
        return ("User{" +
            "mId=" + mId +
            ", mName='" + mName + '\'' +
            ", mLastName='" + mLastName + '\'' +
            ", mAge=" + mAge +
            ", mAdmin=" + mAdmin +
            ", mWeight=" + mWeight +
            ", mBirthday=" + mBirthday +
            ", mCustomField='" + mCustomField + '\'' +
            ", mWorkDays=" + mWorkDays +
            '}')
    }
}
