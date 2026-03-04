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

@file:JvmMultifileClass
@file:JvmName("DBUtil")

package androidx.room3.util

import androidx.room3.RoomDatabase
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.Job

/**
 * Gets the database [CoroutineContext] to perform database operation on utility functions. Prefer
 * using this function over directly accessing [RoomDatabase.getCoroutineScope] as it has platform
 * compatibility behaviour.
 */
internal actual suspend fun RoomDatabase.getCoroutineContext(
    inTransaction: Boolean
): CoroutineContext = getCoroutineScope().coroutineContext.minusKey(Job)
