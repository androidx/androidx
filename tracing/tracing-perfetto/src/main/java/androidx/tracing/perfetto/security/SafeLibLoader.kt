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
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

internal class SafeLibLoader(context: Context) {
    private val approvedLocations = listOfNotNull(getCodeCacheDir(context), context.cacheDir)

    // TODO(235105064): consider moving off the main thread (I/O work)
    fun loadLib(file: File, abiToSha256Map: Map<String, String>) {
        // ensure the file is in an approved location (and if not, copy it over to one)
        val safeLocationFile = copyToSafeLocation(file)

        // verify checksum of the file
        verifyChecksum(safeLocationFile, findAbiAwareSha(abiToSha256Map))

        // load the library
        System.load(safeLocationFile.absolutePath)
    }

    /**
     * Copies the file to a location where the app has exclusive write access. No-op if the file is
     * already in such location.
     */
    private fun copyToSafeLocation(file: File): File {
        if (!file.exists()) throw FileNotFoundException("Cannot locate library file: $file")
        val isInApprovedLocation =
            approvedLocations.any { approvedLocation -> file.isDescendantOf(approvedLocation) }
        return if (isInApprovedLocation) file
        else file.copyTo(approvedLocations.first().resolve(file.name), overwrite = true)
    }

    private fun verifyChecksum(file: File, expectedSha: String) {
        val actualSha = calcSha256Digest(file)
        if (actualSha != expectedSha)
            throw IncorrectChecksumException(
                "Invalid checksum for file: $file. Ensure you are using correct" +
                    " version of the library and clear local caches."
            )
    }

    private fun findAbiAwareSha(abiToShaMap: Map<String, String>): String {
        @Suppress("DEPRECATION")
        val abi =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                    Build.SUPPORTED_ABIS.first()
                else -> Build.CPU_ABI
            }
        return abiToShaMap.getOrElse(abi) {
            throw MissingChecksumException("Cannot locate checksum for ABI: $abi in $abiToShaMap")
        }
    }

    private fun calcSha256Digest(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        val buffer = ByteArray(1024)
        file.inputStream().buffered().use { s ->
            while (true) {
                val readCount = s.read(buffer)
                if (readCount <= 0) break
                digest.update(buffer, 0, readCount)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun File.isDescendantOf(ancestor: File) =
        generateSequence(this.parentFile) { it.parentFile }.any { it == ancestor }

    private fun getCodeCacheDir(context: Context): File? =
        if (Build.VERSION.SDK_INT >= 21) Impl21.getCodeCacheDir(context) else null

    @RequiresApi(21)
    private object Impl21 {
        fun getCodeCacheDir(context: Context): File? = context.codeCacheDir
    }
}

internal class MissingChecksumException(message: String) : NoSuchElementException(message)

internal class IncorrectChecksumException(message: String) : SecurityException(message)
