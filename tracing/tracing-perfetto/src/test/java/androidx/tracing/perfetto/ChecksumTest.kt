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
package androidx.tracing.perfetto

import androidx.tracing.perfetto.jni.PerfettoNative
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ChecksumTest {
    @Test
    fun test_prebuilts_checksums() {

        // get the AAR from resources

        val allFiles = javaClass.classLoader!!.getResources("").asSequence()
            .filter { it.protocol == "file" }
            .map { File(it.path) }
            .flatMap { curr ->
                if (curr.isFile) listOf(curr)
                else curr.walkTopDown().filter { it.isFile }.toList()
            }

        val fileNameRx = Regex("tracing-perfetto-binary-(\\d+.*)\\.aar")
        val aarFile = allFiles.single { it.name.matches(fileNameRx) }

        // verify version

        val version: String? = fileNameRx.matchEntire(aarFile.name)?.groupValues?.get(1)
        assertThat(version).isEqualTo(PerfettoNative.Metadata.version)

        // verify checksums

        val libName = "libtracing_perfetto.so"
        val zipFile = ZipFile(aarFile)
        val soFiles = zipFile.entries()
            .asSequence()
            .filter { it.name.matches(Regex("jni/[^/]+/$libName")) }
            .toList()

        assertThat(soFiles.size).isEqualTo(PerfettoNative.Metadata.checksums.size)

        soFiles.forEach { entry ->
            val (_, arch, _) = entry.name.split("/")
            val actual = calcSha(zipFile.getInputStream(entry))
            val expected = PerfettoNative.Metadata.checksums[arch]
            assertThat(actual).isEqualTo(expected)
        }
    }

    private fun calcSha(input: InputStream): String =
        MessageDigest.getInstance("SHA-256")
            .also { it.update(input.readBytes()) }
            .digest()
            .joinToString(separator = "") { "%02x".format(it) }
            .also { assertThat(it).matches("[0-9a-f]{64}") }
}
