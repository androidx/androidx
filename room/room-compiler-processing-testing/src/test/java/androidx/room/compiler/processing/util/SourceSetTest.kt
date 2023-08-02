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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.util.compiler.SourceSet
import com.google.common.truth.Truth.assertThat
import org.jetbrains.kotlin.konan.file.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SourceSetTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private val BY_ROUNDS_DIR = "byRounds"
    val SOURCE_DIR = "test"
    val SOURCE_CLASS = "A"
    val SOURCE_FILE = "$SOURCE_CLASS.java"

    @Test
    fun testFindSourceFile() {
        tempFolder.newFolder(SOURCE_DIR)
        val sourceFile = tempFolder.newFile(SOURCE_DIR + File.separator + SOURCE_FILE)
        sourceFile.writeText("public class A {}")
        val sourceSet = SourceSet.fromExistingFiles(tempFolder.root)
        val path = tempFolder.root.path + File.separator + SOURCE_DIR + File.separator + SOURCE_FILE

        val f = sourceSet.findSourceFile(path)
        assertThat(f).isNotNull()
        assertThat(f!!.relativePath).isEqualTo(SOURCE_DIR + File.separator + SOURCE_FILE)
    }

    @Test
    fun testFindSourceFileShouldIgnoreRoundsDir() {
        for (i in 0..100) {
            val round = i.toString()

            tempFolder.newFolder(BY_ROUNDS_DIR, round, SOURCE_DIR)
            tempFolder.newFile(BY_ROUNDS_DIR + File.separator + round + File.separator +
                    SOURCE_DIR + File.separator + SOURCE_FILE)
            val sourceSet = SourceSet(tempFolder.root, listOf(
                Source.java("$SOURCE_DIR.$SOURCE_CLASS", "public class A {}")))
            val path = tempFolder.root.path + File.separator + BY_ROUNDS_DIR + File.separator +
                    round + File.separator + SOURCE_DIR + File.separator + SOURCE_FILE

            val f = sourceSet.findSourceFile(path)
            assertThat(f).isNotNull()
            assertThat(f!!.relativePath).isEqualTo(SOURCE_DIR + File.separator + SOURCE_FILE)
        }
    }
}
