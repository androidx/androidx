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
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import okio.FileSystem
import okio.Path.Companion.toPath

class PlatformUtilTest {

    private val baseDir =
        (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "platform-util-test-${Random.nextInt()}")
            .toString()

    @BeforeTest
    fun setUp() {
        FileSystem.SYSTEM.createDirectories(baseDir.toPath())
    }

    @AfterTest
    fun tearDown() {
        FileSystem.SYSTEM.deleteRecursively(baseDir.toPath())
    }

    @Test
    fun ensureParentDirectoryExists_createsDirectories() {
        val dbFilePath = "$baseDir/dir1/dir2/test.db"
        val parentPath = "$baseDir/dir1/dir2".toPath()
        assertThat(FileSystem.SYSTEM.exists(parentPath)).isFalse()

        ensureParentDirectoryExists(dbFilePath)

        assertThat(FileSystem.SYSTEM.exists(parentPath)).isTrue()
    }

    @Test
    fun ensureParentDirectoryExists_alreadyExists_doesNothing() {
        val dbFilePath = "$baseDir/test.db"
        assertThat(FileSystem.SYSTEM.exists(baseDir.toPath())).isTrue()

        ensureParentDirectoryExists(dbFilePath)

        assertThat(FileSystem.SYSTEM.exists(baseDir.toPath())).isTrue()
    }

    @Test
    fun ensureParentDirectoryExists_boundaryPaths_doNotFail() {
        ensureParentDirectoryExists(":memory:")
        ensureParentDirectoryExists("test.db")
        ensureParentDirectoryExists("/test.db")
    }

    @Test
    fun deleteDatabaseFiles_deletesAllCompanionFiles() {
        val dbFilePath = "$baseDir/sample.db"
        val dbPath = dbFilePath.toPath()
        val walPath = "$dbFilePath-wal".toPath()
        val shmPath = "$dbFilePath-shm".toPath()
        val journalPath = "$dbFilePath-journal".toPath()

        FileSystem.SYSTEM.write(dbPath) {}
        FileSystem.SYSTEM.write(walPath) {}
        FileSystem.SYSTEM.write(shmPath) {}
        FileSystem.SYSTEM.write(journalPath) {}

        assertThat(FileSystem.SYSTEM.exists(dbPath)).isTrue()
        assertThat(FileSystem.SYSTEM.exists(walPath)).isTrue()
        assertThat(FileSystem.SYSTEM.exists(shmPath)).isTrue()
        assertThat(FileSystem.SYSTEM.exists(journalPath)).isTrue()

        val deleted = deleteDatabaseFiles(dbFilePath)

        assertThat(deleted).isTrue()
        assertThat(FileSystem.SYSTEM.exists(dbPath)).isFalse()
        assertThat(FileSystem.SYSTEM.exists(walPath)).isFalse()
        assertThat(FileSystem.SYSTEM.exists(shmPath)).isFalse()
        assertThat(FileSystem.SYSTEM.exists(journalPath)).isFalse()
    }

    @Test
    fun deleteDatabaseFiles_nonExistent_returnsFalse() {
        val deleted = deleteDatabaseFiles("$baseDir/doesNotExist.db")
        assertThat(deleted).isFalse()
    }

    @Test
    fun deleteDatabaseFiles_memory_returnsFalse() {
        val deleted = deleteDatabaseFiles(":memory:")
        assertThat(deleted).isFalse()
    }
}
