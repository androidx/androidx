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

package androidx.privacysandbox.sdkruntime.client.loader.impl

import android.content.Context
import android.os.Build
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class MigrationUtilsTest {

    private lateinit var context: Context
    private lateinit var fromDir: File
    private lateinit var toDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up between tests
        val testDir = File(context.cacheDir, "MigrationUtilsTest")
        testDir.deleteRecursively()
        testDir.deleteOnExit()

        fromDir = File(testDir, "from")
        fromDir.mkdirs()

        toDir = File(testDir, "to")
        toDir.mkdirs()
    }

    @Test
    fun moveFiles_moveFileContents() {
        val fileToMove = File(fromDir, "testFile")
        fileToMove.createNewFile()
        fileToMove.outputStream().use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                dataStream.writeInt(42)
            }
        }

        val result = MigrationUtils.moveFiles(fromDir, toDir, fileToMove.name)
        assertThat(result).isTrue()
        assertThat(fileToMove.exists()).isFalse()

        val resultFile = File(toDir, fileToMove.name)
        assertThat(resultFile.exists()).isTrue()

        val content = resultFile.inputStream().use { inputStream ->
            DataInputStream(inputStream).use { dataStream ->
                dataStream.readInt()
            }
        }

        assertThat(content)
            .isEqualTo(42)
    }

    @Test
    fun moveFiles_copyPermissions() {
        val fileToMove = File(fromDir, "testFile")
        fileToMove.createNewFile()
        Os.chmod(fileToMove.absolutePath, 511) // 0777
        val statFrom = Os.stat(fileToMove.absolutePath)

        MigrationUtils.moveFiles(fromDir, toDir, fileToMove.name)

        val resultFile = File(toDir, fileToMove.name)
        val stat = Os.stat(resultFile.absolutePath)
        assertThat(stat.st_mode).isEqualTo(statFrom.st_mode)
    }

    @Test
    fun moveFiles_moveMultipleFilesWithPrefix() {
        val fileToMove1 = File(fromDir, "testFile1")
        val fileToMove2 = File(fromDir, "testFile2")
        val fileToKeep = File(fromDir, "keepFile1")

        fileToMove1.createNewFile()
        fileToMove2.createNewFile()
        fileToKeep.createNewFile()

        val result = MigrationUtils.moveFiles(fromDir, toDir, "testFile")
        assertThat(result).isTrue()

        assertThat(fileToMove1.exists()).isFalse()
        assertThat(fileToMove2.exists()).isFalse()
        assertThat(fileToKeep.exists()).isTrue()

        val resultFile1 = File(toDir, fileToMove1.name)
        val resultFile2 = File(toDir, fileToMove2.name)
        val notCopiedFile = File(toDir, fileToKeep.name)

        assertThat(resultFile1.exists()).isTrue()
        assertThat(resultFile2.exists()).isTrue()
        assertThat(notCopiedFile.exists()).isFalse()
    }

    @Test
    fun moveFiles_whenSameFromAndTo_keepExistingFile() {
        val fileToMove = File(fromDir, "testFile")
        fileToMove.outputStream().use { outputStream ->
            DataOutputStream(outputStream).use { dataStream ->
                dataStream.writeInt(42)
            }
        }

        val result = MigrationUtils.moveFiles(fromDir, fromDir, fileToMove.name)
        assertThat(result).isTrue()

        assertThat(fileToMove.exists()).isTrue()
        val content = fileToMove.inputStream().use { inputStream ->
            DataInputStream(inputStream).use { dataStream ->
                dataStream.readInt()
            }
        }

        assertThat(content)
            .isEqualTo(42)
    }

    @Test
    fun moveFiles_skipFailedFilesAndReturnFalse() {
        val fileToMove1 = File(fromDir, "testFile1")
        val fileToFail = File(fromDir, "testFile2")
        val fileToMove2 = File(fromDir, "testFile3")

        fileToMove1.createNewFile()
        fileToFail.mkdir() // to fail copy
        fileToMove2.createNewFile()

        val result = MigrationUtils.moveFiles(fromDir, toDir, "testFile")
        assertThat(result).isFalse()

        assertThat(fileToMove1.exists()).isFalse()
        assertThat(fileToMove2.exists()).isFalse()

        val resultFile1 = File(toDir, fileToMove1.name)
        val resultFile2 = File(toDir, fileToMove2.name)

        assertThat(resultFile1.exists()).isTrue()
        assertThat(resultFile2.exists()).isTrue()
    }
}
