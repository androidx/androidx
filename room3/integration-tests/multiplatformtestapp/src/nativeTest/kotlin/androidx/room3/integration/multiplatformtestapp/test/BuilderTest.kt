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
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.EOF
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fgetc
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class)
class BuilderTest : BaseNonWebBuilderTest() {

    private val filename = "/tmp/test-${Random.nextInt()}.db"

    override fun getRoomDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase> {
        return getRoomDatabaseBuilder(filename)
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
        return "/tmp/$name-${Random.nextInt()}.db"
    }

    override fun createCorruptedFile(path: String) {
        val file = fopen(path, "w") ?: error("Failed to open $path")
        try {
            fputs("corrupted sqlite header", file)
        } finally {
            fclose(file)
        }
    }

    override fun deleteFile(path: String) {
        remove(path)
        remove("$path-wal")
        remove("$path-shm")
    }

    override fun fileExists(path: String): Boolean {
        return access(path, F_OK) == 0
    }

    override fun readFileContent(path: String): String {
        val file = fopen(path, "r") ?: error("Failed to open $path")
        val sb = StringBuilder()
        try {
            while (true) {
                val c = fgetc(file)
                if (c == EOF) break
                sb.append(c.toChar())
            }
        } finally {
            fclose(file)
        }
        return sb.toString()
    }

    @BeforeTest
    fun before() {
        deleteDatabaseFile()
    }

    @AfterTest
    fun after() {
        deleteDatabaseFile()
    }

    private fun deleteDatabaseFile() {
        deleteFile(filename)
    }
}
