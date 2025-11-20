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

package androidx.tracing.perfetto.security

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SafeLibLoaderTest {
    private lateinit var context: Context
    private lateinit var allowedLibDir: File
    private lateinit var disallowedLibDir: File
    private lateinit var safeLibLoader: SafeLibLoader
    private lateinit var abi: String

    // we're using the fact that the .so is already present in the test apk; otherwise any .so works
    private val libFileName = "libtracing_perfetto.so"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        allowedLibDir = context.cacheDir
        @Suppress("DEPRECATION")
        disallowedLibDir = context.externalMediaDirs.first()
        safeLibLoader = SafeLibLoader(context)
        abi = Build.SUPPORTED_ABIS.first()
    }

    @Test
    fun test_success_case() {
        val libFile = copyRealLibTo(allowedLibDir)
        try {
            safeLibLoader.loadLib(libFile, mapOf(abi to calcSha(libFile)))
            // no crash or exception; loaded library working is tested in other places
        } finally {
            libFile.delete()
        }
    }

    @Test(expected = MissingChecksumException::class)
    fun test_missing_sha() {
        val libFile = copyFakeLibTo(allowedLibDir)
        try {
            safeLibLoader.loadLib(libFile, abiToSha256Map = emptyMap()) // note: no sha entries
        } finally {
            libFile.delete()
        }
    }

    @Test(expected = IncorrectChecksumException::class)
    fun test_incorrect_sha() {
        val libFile = copyFakeLibTo(allowedLibDir)
        try {
            safeLibLoader.loadLib(libFile, mapOf(abi to "zzz")) // note: "zzz" is not a valid sha
        } finally {
            libFile.delete()
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun test_file_not_found() {
        safeLibLoader.loadLib(allowedLibDir.resolve("file-does-not-exist"), mapOf(abi to "zzz"))
    }

    @Test
    fun test_unapproved_location() {
        val libFile = copyRealLibTo(disallowedLibDir)
        try {
            safeLibLoader.loadLib(libFile, mapOf(abi to calcSha(libFile)))
        } finally {
            libFile.delete()
        }
    }

    private fun copyRealLibTo(dstDir: File): File =
        dstDir.resolve(libFileName).also { dstFile ->
            val srcZip = ZipFile(File(context.applicationInfo.publicSourceDir))
            val libEntry =
                srcZip.entries().asSequence().single { entry ->
                    entry.name.matches(Regex(".*lib/$abi/$libFileName"))
                }
            srcZip.getInputStream(libEntry).use { src ->
                dstFile.outputStream().use { dst -> src.copyTo(dst) }
            }
        }

    private fun copyFakeLibTo(dstDir: File): File =
        dstDir.resolve("libfake.so").also {
            it.outputStream().bufferedWriter().use { w -> w.write("foo") }
        }

    private fun calcSha(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .also { it.update(file.readBytes()) }
            .digest()
            .joinToString(separator = "") { "%02x".format(it) }
            .also { assertThat(it).matches("[0-9a-f]{64}") }
}
