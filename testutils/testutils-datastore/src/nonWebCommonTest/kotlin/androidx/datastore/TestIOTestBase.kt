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

package androidx.datastore

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test

class OkioTestIOTest : TestIOTestBase(OkioTestIO())

abstract class TestIOTestBase(private val testIO: TestIO<*, *>) {
    @Test
    fun writeNewFile() {
        val file = testIO.newTempFile()
        assertThat(file.exists()).isFalse()
        file.write("body")
        assertThat(file.exists()).isTrue()
        assertThat(file.isDirectory()).isFalse()
        assertThat(file.isRegularFile()).isTrue()
    }

    @Test
    fun createDirectory() {
        val file = testIO.newTempFile(relativePath = "some/sub/directory")
        assertThat(file.exists()).isFalse()
        file.mkdirs(mustCreate = true)
        assertThat(file.isDirectory()).isTrue()

        // try to re-create over it, should fail
        assertThrows<Throwable> { file.mkdirs(mustCreate = true) }
        // don't need to create so should be fine
        file.mkdirs(mustCreate = false)
    }

    @Test
    fun createDirectory_overFile() {
        val file = testIO.newTempFile(relativePath = "some/sub/file")
        file.write("test")
        assertThrows<Throwable> { file.mkdirs(mustCreate = false) }
        assertThrows<Throwable> { file.mkdirs(mustCreate = true) }
    }

    @Test
    fun writeToDirectory() {
        val file = testIO.newTempFile(relativePath = "some/dir")
        file.mkdirs()
        assertThrows<Throwable> { file.write("some text") }
    }

    @Test
    fun resolve() {
        val file = testIO.newTempFile(relativePath = "dir1/dir2/file")
        file.write("test")
        val file2 = testIO.newTempFile(relativePath = "dir1/dir2/file")
        assertThat(file2.readText()).isEqualTo("test")
        val parent = testIO.newTempFile(relativePath = "dir1")
        assertThat(parent.resolve("dir2/file").readText()).isEqualTo("test")
    }

    @Test
    fun delete_regularFile() {
        val file = testIO.newTempFile(relativePath = "foo")
        file.write("foo")
        assertThat(file.isRegularFile()).isTrue()
        file.delete()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun delete_directory() {
        val file = testIO.newTempFile(relativePath = "foo/bar")
        file.mkdirs()
        assertThat(file.isDirectory()).isTrue()
        file.delete()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun readBytes() {
        val file = testIO.newTempFile()
        // cannot read non-existing file
        assertThrows<Throwable> { file.readBytes() }
        file.mkdirs()
        // cannot read a directory
        assertThrows<Throwable> { file.readBytes() }
    }
}
