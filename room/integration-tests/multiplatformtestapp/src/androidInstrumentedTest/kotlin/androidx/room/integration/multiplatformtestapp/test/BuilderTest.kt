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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class BuilderTest : BaseBuilderTest() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("test.db")

    override fun getRoomDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase> {
        return Room.databaseBuilder<SampleDatabase>(
                context = instrumentation.targetContext,
                name = file.path,
                factory = SampleDatabaseConstructor::initialize
            )
            .setDriver(BundledSQLiteDriver())
    }

    @BeforeTest
    fun before() {
        assertThat(file).isNotNull()
        file.parentFile?.mkdirs()
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        deleteDatabaseFile()
    }

    private fun deleteDatabaseFile() {
        instrumentation.targetContext.deleteDatabase(file.name)
    }
}
