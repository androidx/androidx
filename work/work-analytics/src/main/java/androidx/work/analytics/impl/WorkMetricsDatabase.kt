/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.analytics.impl

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.work.analytics.impl.model.WorkMetricsInfoTypeConverters
import androidx.work.analytics.impl.model.WorkMetricsSpec
import androidx.work.analytics.impl.model.WorkMetricsSpecDao

/** Internal database for storing historical work execution analytics. */
@Database(entities = [WorkMetricsSpec::class], version = 1, exportSchema = false)
@TypeConverters(WorkMetricsInfoTypeConverters::class)
internal abstract class WorkMetricsDatabase : RoomDatabase() {
    /** Returns the DAO for [WorkMetricsSpec]. */
    abstract fun workMetricsSpecDao(): WorkMetricsSpecDao
}
