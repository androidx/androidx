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

object Room {

    /**
     * Creates a RoomDatabase.Builder for a persistent database. Once a database is built, you
     * should keep a reference to it and re-use it.
     *
     * This [databaseBuilder] avoids using reflection to access the generated database
     * implementation.
     *
     * @param T     The type of the database class.
     * @param name    The name of the database file.
     * @param factory The lambda calling `initializeImpl()` on the database class which returns
     * the generated database implementation.
     * @return A `RoomDatabaseBuilder<T>` which you can use to create the database.
     */
    inline fun <reified T : RoomDatabase> databaseBuilder(
        name: String,
        noinline factory: () -> T
    ): RoomDatabase.Builder<T> {
        return RoomDatabase.Builder(
            T::class,
            name,
            factory
        )
    }
}
