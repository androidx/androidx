/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("FlowUtil")

package androidx.room.coroutines

import androidx.annotation.RestrictTo
import androidx.room.RoomDatabase
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun <R> createFlow(
    db: RoomDatabase,
    inTransaction: Boolean,
    tableNames: Array<String>,
    block: (SQLiteConnection) -> R
): Flow<R> =
    db.invalidationTracker.createFlow(*tableNames, emitInitialState = true).conflate().map {
        performSuspending(db, true, inTransaction, block)
    }
