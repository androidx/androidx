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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

class BuilderTest : BaseNonWebBuilderTest() {
    private val tempDir = createTempDirectory().toFile().also { it.deleteOnExit() }

    override fun getRoomDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase> {
        val tempFile = createTempFile("test.db").also { it.toFile().deleteOnExit() }
        return getRoomDatabaseBuilder(tempFile.toString())
    }

    override fun getRoomDatabaseBuilder(fileName: String): RoomDatabase.Builder<SampleDatabase> {
        return Room.databaseBuilder<SampleDatabase>(
                name = fileName,
                factory = SampleDatabaseConstructor::initialize,
            )
            .setDriver(BundledSQLiteDriver())
    }

    override fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase> {
        return Room.inMemoryDatabaseBuilder<SampleDatabase>(
                factory = SampleDatabaseConstructor::initialize
            )
            .setDriver(BundledSQLiteDriver())
    }

    override fun getDatabasePath(name: String): String {
        return File(tempDir, "$name.db").absolutePath
    }

    override fun createCorruptedFile(path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText("corrupted sqlite header")
    }

    override fun deleteFile(path: String) {
        File(path).delete()
    }

    override fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    override fun readFileContent(path: String): String {
        return File(path).readText()
    }
}
