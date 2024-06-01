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

import androidx.kruth.assertThat
import androidx.room.Room.databaseBuilder
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BuilderTest {
    @Test
    fun databaseBuilderWithFactory() {
        val db =
            databaseBuilder(
                    name = "TestDatabase",
                    factory = { TestDatabase::class.instantiateImpl() }
                )
                .setDriver(NativeSQLiteDriver())
                .build()

        // Assert that the db is built successfully.
        assertThat(db).isInstanceOf<TestDatabase>()
    }

    @Test
    fun missingDriver() {
        assertThat(
                assertFailsWith<IllegalArgumentException> {
                        databaseBuilder(
                                name = "TestDatabase",
                                factory = { TestDatabase::class.instantiateImpl() }
                            )
                            .build()
                    }
                    .message
            )
            .isEqualTo(
                "Cannot create a RoomDatabase without providing a SQLiteDriver via setDriver()."
            )
    }

    internal abstract class TestDatabase : RoomDatabase()
}
