/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.util

import androidx.kruth.assertThat
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PlatformUtilTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("platform-util-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun ensureParentDirectoryExists_createsDirectories() {
        val dbFile = File(tempDir, "dir1/dir2/test.db")
        val parentDir = dbFile.parentFile!!
        assertThat(parentDir.exists()).isFalse()

        ensureParentDirectoryExists(dbFile.absolutePath)

        assertThat(parentDir.exists()).isTrue()
        assertThat(parentDir.isDirectory).isTrue()
    }

    @Test
    fun ensureParentDirectoryExists_alreadyExists_doesNothing() {
        val dbFile = File(tempDir, "test.db")
        assertThat(tempDir.exists()).isTrue()

        ensureParentDirectoryExists(dbFile.absolutePath)

        assertThat(tempDir.exists()).isTrue()
    }

    @Test
    fun ensureParentDirectoryExists_boundaryPaths_doNotFail() {
        ensureParentDirectoryExists(":memory:")
        ensureParentDirectoryExists("test.db")
        ensureParentDirectoryExists("/test.db")
    }

    @Test
    fun deleteDatabaseFiles_deletesAllCompanionFiles() {
        val dbFile = File(tempDir, "sample.db")
        val walFile = File(tempDir, "sample.db-wal")
        val shmFile = File(tempDir, "sample.db-shm")
        val journalFile = File(tempDir, "sample.db-journal")

        assertThat(dbFile.createNewFile()).isTrue()
        assertThat(walFile.createNewFile()).isTrue()
        assertThat(shmFile.createNewFile()).isTrue()
        assertThat(journalFile.createNewFile()).isTrue()

        val deleted = deleteDatabaseFiles(dbFile.absolutePath)

        assertThat(deleted).isTrue()
        assertThat(dbFile.exists()).isFalse()
        assertThat(walFile.exists()).isFalse()
        assertThat(shmFile.exists()).isFalse()
        assertThat(journalFile.exists()).isFalse()
    }

    @Test
    fun deleteDatabaseFiles_nonExistent_returnsFalse() {
        val dbFile = File(tempDir, "doesNotExist.db")
        val deleted = deleteDatabaseFiles(dbFile.absolutePath)
        assertThat(deleted).isFalse()
    }

    @Test
    fun deleteDatabaseFiles_memory_returnsFalse() {
        val deleted = deleteDatabaseFiles(":memory:")
        assertThat(deleted).isFalse()
    }
}
