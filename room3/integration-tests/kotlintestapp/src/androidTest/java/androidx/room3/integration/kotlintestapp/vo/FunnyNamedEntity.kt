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
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity.Companion.COLUMN_ID
import androidx.room3.integration.kotlintestapp.vo.FunnyNamedEntity.Companion.TABLE_NAME

/** An entity that was weird names */
@Entity(tableName = TABLE_NAME)
data class FunnyNamedEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = COLUMN_ID) var id: Int,
    @ColumnInfo(name = COLUMN_VALUE) var value: String?,
) {
    companion object {
        const val TABLE_NAME = "funny but not so funny"
        const val COLUMN_ID = "_this \$is id$"
        const val COLUMN_VALUE = "unlikely-Ωşå¨ıünames"
    }
}
