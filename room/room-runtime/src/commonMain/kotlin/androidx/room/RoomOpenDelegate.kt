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
 * @see [BaseRoomConnectionManager.openDelegate]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public abstract class RoomOpenDelegate(
    public val version: Int,
    public val identityHash: String,
    public val legacyIdentityHash: String,
) : RoomOpenDelegateMarker {
    public abstract fun onCreate(connection: SQLiteConnection)

    public abstract fun onPreMigrate(connection: SQLiteConnection)

    public abstract fun onValidateSchema(connection: SQLiteConnection): ValidationResult

    public abstract fun onPostMigrate(connection: SQLiteConnection)

    public abstract fun onOpen(connection: SQLiteConnection)

    public abstract fun createAllTables(connection: SQLiteConnection)

    public abstract fun dropAllTables(connection: SQLiteConnection)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    public class ValidationResult(
        @JvmField public val isValid: Boolean,
        @JvmField public val expectedFoundMsg: String?,
    )
}

/** Marker interface for Room's code generated delegate. */
public interface RoomOpenDelegateMarker
