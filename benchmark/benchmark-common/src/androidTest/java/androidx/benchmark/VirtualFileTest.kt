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

package androidx.benchmark

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class VirtualFileTest(
    private val file1: VirtualFile,
    private val file2: VirtualFile,
    private val expectedFile1Path: String,
    private val expectedFileType: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> =
            listOf(
                arrayOf(
                    UserFile.inOutputsDir("test1.file"),
                    UserFile.inOutputsDir("test2.file"),
                    "/storage/emulated/0/Android/media/androidx.benchmark.test/test1.file",
                    "UserFile",
                ),
                arrayOf(
                    ShellFile.inTempDir("test1.file"),
                    ShellFile.inTempDir("test2.file"),
                    "/data/local/tmp/test1.file",
                    "ShellFile",
                )
            )
    }

    @Before
    fun setUp() {
        file1.delete()
        file2.delete()
    }

    @After
    fun tearDown() {
        file1.delete()
        file2.delete()
    }

    @Test
    fun absolutePath() {
        assertThat(file1.absolutePath).isEqualTo(expectedFile1Path)
    }

    @Test
    fun fileType() {
        assertThat(file1.fileType).isEqualTo(expectedFileType)
    }

    @Test
    fun writeReadText() {
        file1.writeText("test")
        assertThat(file1.readText()).isEqualTo("test")
    }

    @Test
    fun writeReadBytes() {
        val bytes = ByteArray(3) { it.toByte() }
        file1.writeBytes(bytes)
        assertThat(file1.readBytes()).isEqualTo(bytes)
    }

    @Test
    fun existDelete() {
        file1.writeText("text")
        assertThat(file1.exists()).isTrue()
        assertThat(file1.delete()).isTrue()
        assertThat(file1.exists()).isFalse()
    }

    @Test
    fun copyFrom() {
        file2.writeText("text")
        file1.copyFrom(file2)
        assertThat(file1.readText()).isEqualTo("text")
    }

    @Test
    fun copyTo() {
        file1.writeText("text")
        file1.copyTo(file2)
        assertThat(file2.readText()).isEqualTo("text")
    }

    @Test
    fun moveTo() {
        file1.writeText("text")
        file1.moveTo(file2)
        assertThat(file2.readText()).isEqualTo("text")
        assertThat(file1.exists()).isFalse()
    }

    @Test
    fun md5sum() {
        file1.writeText("text")
        assertThat(file1.md5sum()).isEqualTo("1cb251ec0d568de6a929b520c4aed8d1")
    }

    @Test
    fun ls() {
        file1.writeText("text")
        assertThat(file1.ls().first()).isEqualTo(expectedFile1Path)
    }
}

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class VirtualFileIoTest {

    private lateinit var userFile: UserFile
    private lateinit var shellFile: ShellFile

    @Before
    fun setUp() {
        userFile = UserFile.inOutputsDir("user.file1").apply { delete() }
        shellFile = ShellFile.inTempDir("shell.file1").apply { delete() }
    }

    @Test
    fun shellCopyToUser() {
        shellFile.writeText("test")
        shellFile.copyTo(userFile)
        assertThat(userFile.readText()).isEqualTo("test")
    }

    @Test
    fun shellCopyFromUser() {
        userFile.writeText("test")
        shellFile.copyFrom(userFile)
        assertThat(shellFile.readText()).isEqualTo("test")
    }

    @Test
    fun userCopyToShell() {
        userFile.writeText("test")
        userFile.copyTo(shellFile)
        assertThat(shellFile.readText()).isEqualTo("test")
    }

    @Test
    fun userCopyFromShell() {
        shellFile.writeText("test")
        userFile.copyFrom(shellFile)
        assertThat(userFile.readText()).isEqualTo("test")
    }
}
