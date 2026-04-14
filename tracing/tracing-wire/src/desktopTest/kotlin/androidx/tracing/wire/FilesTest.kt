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

package androidx.tracing.wire

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    internal fun testTracingFileNames() {
        val tmpDir = temporaryFolder.newFolder()
        val perfettoTraceFile = tmpDir.createPerfettoFile()
        val path = perfettoTraceFile.nameWithoutExtension
        // Should have the pattern PREFIX-yyyy-MM-dd-HH-mm-ss-suffix
        val parts = path.split("-")
        // There should a total of 8 tokens
        assertEquals(parts.size, 8)
        assertEquals(PREFIX, parts.first())
    }

    @Test
    internal fun testTracingFileNameSuffix() {
        val tmpDir = temporaryFolder.newFolder()
        // Using placeholders for time
        val fileName = "trace-yyyy-MM-dd-HH-mm-ss"
        val files = mutableListOf<File>()
        val count = 10
        repeat(count) { files += tmpDir.createPerfettoFile(fileName = fileName) }
        // This one should have a suffix `count`
        val path = files.last().nameWithoutExtension
        val parts = path.split("-")
        assertEquals("${count - 1}", parts.last())
    }
}
