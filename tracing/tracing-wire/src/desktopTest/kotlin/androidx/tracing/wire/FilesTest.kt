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

class FilesTest {
    @Test
    internal fun testTracingFileNames() {
        // We are not really creating files here. We just need some paths.
        val tmpDir = File("/tmp")
        val perfettoTraceFile = tmpDir.perfettoTraceFile()
        val path = perfettoTraceFile.nameWithoutExtension
        // Should have the pattern PREFIX-yyyy-MM-dd-HH-mm-ss-suffix
        val parts = path.split("-")
        // There should a total of 8 tokens
        assertEquals(parts.size, 8)
        assertEquals(parts[0], PREFIX)
    }

    @Test
    internal fun testTracingFileNameSuffix() {
        // We are not really creating files here. We just need some paths.
        val tmpDir = File("/tmp")
        val count = 10
        repeat(count) { tmpDir.perfettoTraceFile() }
        // This one should have a suffix `count`
        val perfettoTraceFile = tmpDir.perfettoTraceFile()
        val path = perfettoTraceFile.nameWithoutExtension
        val parts = path.split("-")
        assertEquals(parts.last(), "$count")
    }
}
