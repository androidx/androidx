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
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import androidx.room3.RoomWarnings
import androidx.room3.TypeConverters

@SuppressWarnings(RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD)
@Entity(
    foreignKeys =
        arrayOf(
            ForeignKey(
                entity = Publisher::class,
                parentColumns = arrayOf("publisherId"),
                childColumns = arrayOf("bookPublisherId"),
                deferred = true,
            )
        )
)
data class Book(
    @PrimaryKey val bookId: String,
    val title: String,
    val bookPublisherId: String,
    @ColumnInfo(defaultValue = "0") @field:TypeConverters(Lang::class) val languages: Set<Lang>,
    @ColumnInfo(defaultValue = "0") val salesCnt: Int,
)
