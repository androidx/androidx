/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room

import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmField

/**
 * A delegate to perform key database operation during initialization.
 *
 * This abstract class should only be implemented by generated code and bridges generated
 * implementation of a RoomDatabase with runtime.
 *
 * @see [RoomDatabase.createOpenDelegate]
 * @see [RoomConnectionManager.openDelegate]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class RoomOpenDelegate(
    val version: Int,
    val identityHash: String,
    val legacyIdentityHash: String
) : RoomOpenDelegateMarker {
    abstract fun onCreate(connection: SQLiteConnection)

    abstract fun onPreMigrate(connection: SQLiteConnection)

    abstract fun onValidateSchema(connection: SQLiteConnection): ValidationResult

    abstract fun onPostMigrate(connection: SQLiteConnection)

    abstract fun onOpen(connection: SQLiteConnection)

    abstract fun createAllTables(connection: SQLiteConnection)

    abstract fun dropAllTables(connection: SQLiteConnection)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class ValidationResult(@JvmField val isValid: Boolean, @JvmField val expectedFoundMsg: String?)
}

/** Marker interface for Room's code generated delegate. */
interface RoomOpenDelegateMarker
