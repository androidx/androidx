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

import androidx.room.util.findDatabaseConstructorAndInitDatabaseImpl

/** Entry point for building and initializing a [RoomDatabase]. */
actual object Room {

    /** The master table name where Room keeps its metadata information. */
    actual const val MASTER_TABLE_NAME = RoomMasterTable.TABLE_NAME

    /**
     * Creates a RoomDatabase.Builder for an in memory database. Information stored in an in memory
     * database disappears when the process is killed. Once a database is built, you should keep a
     * reference to it and re-use it.
     *
     * @param T The type of the database class.
     * @param factory An optional lambda calling [RoomDatabaseConstructor.initialize] corresponding
     *   to the database class of this builder. If not provided then the associated
     *   [RoomDatabaseConstructor] is searched via the [ConstructedBy] annotation and is used to
     *   instantiate the database implementation class.
     * @return A `RoomDatabaseBuilder<T>` which you can use to create the database.
     */
    inline fun <reified T : RoomDatabase> inMemoryDatabaseBuilder(
        noinline factory: () -> T = { findDatabaseConstructorAndInitDatabaseImpl(T::class) }
    ): RoomDatabase.Builder<T> {
        return RoomDatabase.Builder(T::class, null, factory)
    }

    /**
     * Creates a RoomDatabase.Builder for a persistent database. Once a database is built, you
     * should keep a reference to it and re-use it.
     *
     * @param T The type of the database class.
     * @param name The name of the database file.
     * @param factory An optional lambda calling [RoomDatabaseConstructor.initialize] corresponding
     *   to the database class of this builder. If not provided then the associated
     *   [RoomDatabaseConstructor] is searched via the [ConstructedBy] annotation and is used to
     *   instantiate the database implementation class.
     * @return A `RoomDatabaseBuilder<T>` which you can use to create the database.
     */
    inline fun <reified T : RoomDatabase> databaseBuilder(
        name: String,
        noinline factory: () -> T = { findDatabaseConstructorAndInitDatabaseImpl(T::class) }
    ): RoomDatabase.Builder<T> {
        require(name.isNotBlank()) {
            "Cannot build a database with empty name." +
                " If you are trying to create an in memory database, use Room" +
                ".inMemoryDatabaseBuilder()."
        }
        require(name != ":memory:") {
            "Cannot build a database with the special name ':memory:'." +
                " If you are trying to create an in memory database, use Room" +
                ".inMemoryDatabaseBuilder()."
        }
        return RoomDatabase.Builder(T::class, name, factory)
    }
}
