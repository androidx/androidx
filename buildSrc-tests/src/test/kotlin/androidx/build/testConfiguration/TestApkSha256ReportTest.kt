/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.testConfiguration

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TestApkSha256ReportTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun writeShaXml() {
        val file1 = temporaryFolder.newFile(
            "foo.txt"
        ).also {
            it.writeText(
                "test-file-1"
            )
        }
        // validated via an external tool so we don't rely on what we generate.
        val file1Sha = "db7bb0ef3ae21cafba57068bab4bcdd5129ba8a25ef5f8c16ad33fc686c7467e"
        val output = temporaryFolder.newFile("output.xml")
        val subject = TestApkSha256Report()
        subject.addFile("renamed.txt", file1)
        subject.writeToFile(output)
        assertThat(
            output.readText(Charsets.UTF_8).trimIndent()
        ).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <sha256Report>
                <file name="renamed.txt" sha256="$file1Sha"/>
            </sha256Report>
        """.trimIndent()
        )
    }

    @Test
    fun conflictingFileName() {
        val file1 = temporaryFolder.newFile(
            "foo.txt"
        ).also {
            it.writeText(
                "test-file-1"
            )
        }
        val file2 = temporaryFolder.newFile(
            "foo2.txt"
        ).also {
            it.writeText(
                "test-file-2"
            )
        }
        val subject = TestApkSha256Report()
        subject.addFile("file1.txt", file1)
        val result = runCatching {
            // intentionally use the same name for a different file
            subject.addFile("file1.txt", file2)
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Same file name sent with different sha256 values"
        )
    }
}