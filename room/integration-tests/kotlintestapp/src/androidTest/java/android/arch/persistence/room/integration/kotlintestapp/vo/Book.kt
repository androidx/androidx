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

package android.arch.persistence.room.integration.kotlintestapp.vo

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters

@Entity(foreignKeys = arrayOf(
        ForeignKey(entity = Publisher::class,
                parentColumns = arrayOf("publisherId"),
                childColumns = arrayOf("bookPublisherId"),
                deferred = true)))
data class Book(
        @PrimaryKey val bookId: String,
        val title: String,
        val bookPublisherId: String,
        @field:TypeConverters(Lang::class)
        val languages: Set<Lang>,
        val salesCnt: Int)
