/*
 * Copyright 2018 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@file:JvmName("GuavaRoom")

package androidx.room3.guava

import androidx.annotation.RestrictTo
import androidx.room3.RoomDatabase
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteConnection
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.future

/** Marker class used by annotation processor to identify dependency is in the classpath. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GuavaRoomArtifactMarker private constructor()

/**
 * Returns a [ListenableFuture] created by launching the input `block` in [RoomDatabase]'s Coroutine
 * scope.
 */
public fun <T> createListenableFuture(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: suspend (SQLiteConnection) -> T,
): ListenableFuture<T> =
    db.getCoroutineScope().future { performSuspending(db, isReadOnly, inTransaction, block) }
