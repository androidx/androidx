/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.aab

import androidx.aab.cli.VERBOSE
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile

/** Bundle information captured from `.dex` files (Not just classesXXX.dex, any .dex files) */
data class DexInfo(
    /** Entry name (relative path) within the containing bundle */
    val entryName: String,

    /**
     * crc32 of whole dex file
     *
     * Despite being 4 bytes this is *NOT* the dex-embedded checksum. It's the one embedded in
     * profiles to verify the dex they match with.
     */
    val crc32: String,

    /** Sha256 of whole file */
    val sha256: String,

    /** Size of bytes in the uncompressed dex */
    val uncompressedSize: Long,

    /** Size of bytes in the zip container */
    val compressedSize: Long,

    /** r8 map id, if present in the dex strings */
    val r8MapId: String?,

    /** r8 markers */
    val r8Markers: List<R8Marker>,

    /**
     * top level (non-inner) classes which appear to be minified (based on starting with lowercase
     * identifier)
     */
    val minifiedClassCountLowercase: Int,
    /** top level (non-inner) classes which appear to be minified (based on length heuristic) */
    val minifiedClassCountLengthHeuristic: Int,
    val noPackageClassCount: Int,
    val classInfo: List<ClassInfo>,
) {
    class ClassInfo(val packageName: String, val className: String, val size: Int) {
        val fullName: String = if (packageName.isEmpty()) className else "$packageName.$className"
        val startsWithLowerCase = className[0].isLowerCase()
        val classNameAppearsMinified =
            className.length <= 2 || className.length <= 3 && hasObfuscatedPackageName()

        private fun hasObfuscatedPackageName(): Boolean {
            val parts = packageName.split("\\.".toRegex())
            for (part in parts) {
                if (part.length > 2) {
                    return false
                }
            }
            return true
        }
    }

    init {
        require(uncompressedSize >= 0) { "Uncompressed size must be non-negative" }
    }

    data class R8Marker(val compiler: String, val map: Map<String, String>) {
        companion object {
            // """~~R8{"backend":"dex","compilation-mode":"release","has-checksums":false,"min-api":21,"pg-map-id":"17647d6605bb91237bf2b0766cb45b010d6e01aa899f20d7d6b08253bc38712e","r8-mode":"full","sha-1":"4ce18528a68a4b7401548810621405baaf439a48","version":"8.12.13-dev"}"""
            fun from(markerString: String): R8Marker {
                val entries = markerString.substringAfter('{').substringBefore('}').split(',')
                return R8Marker(
                    compiler = markerString.substring(2, markerString.indexOf("{")),
                    map =
                        entries.associate { it ->
                            val kv = it.split(':')
                            kv.first().removeSurrounding("\"") to kv.last().removeSurrounding("\"")
                        },
                )
            }
        }
    }

    companion object {
        internal fun packageNameForType(type: String?): String {
            val parts = type!!.split("\\.".toRegex())
            val sb = StringBuilder()
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                if (part.isNotEmpty()) {
                    if (sb.isNotEmpty()) {
                        sb.append('.')
                    }
                    sb.append(part)
                }
            }
            return sb.toString()
        }

        private fun signatureToType(signature: String): String {
            return if (signature.isEmpty()) {
                signature
            } else {
                when (signature[0]) {
                    'L' -> signature.substring(1, signature.length - 1).replace('/', '.')
                    '[' -> // We strip arrays as we only care about the underlying type used.
                    signatureToType(signature.substring(1))
                    else -> ""
                }
            }
        }

        private fun classNameForType(type: String): String {
            val parts = type.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (parts.isEmpty()) {
                ""
            } else {
                parts[parts.size - 1]
            }
        }

        fun from(entryName: String, compressedSize: Long, src: InputStream): DexInfo {
            val crc = CRC32()
            val sha256 = MessageDigest.getInstance("SHA-256")

            val bytes = src.readAllBytes()
            crc.update(bytes)
            sha256.update(bytes)
            val dexFile = DexBackedDexFile(Opcodes.getDefault(), bytes)

            val r8Markers = mutableListOf<R8Marker>()
            var r8MapId: String? = null
            dexFile.stringSection.forEach {
                if (it.startsWith("~~") && it.endsWith("}")) {
                    r8Markers.add(R8Marker.from(it))
                } else if (it.startsWith("r8-map-id-")) {
                    r8MapId = it
                }
            }
            var minifiedClassCountLowercase = 0
            var minifiedClassCountLengthHeuristic = 0
            var noPackage = 0
            var classCount = 0
            val classInfo = mutableListOf<ClassInfo>()
            dexFile.classes.forEach { cls ->
                val isTopLevel = !cls.type.contains("$")

                if (!isTopLevel) return@forEach

                classCount++

                val type = signatureToType(cls.type)
                val packageName = packageNameForType(type)
                val className = classNameForType(type)

                ClassInfo(packageName, className, cls.size).apply {
                    if (startsWithLowerCase) minifiedClassCountLowercase++
                    if (classNameAppearsMinified) minifiedClassCountLengthHeuristic++
                    if (packageName.isEmpty()) noPackage++
                    classInfo.add(this)
                }
            }
            check(classCount == classInfo.size)

            // Finalize the SHA-256 hash and format it as a hex string.
            val sha256Bytes = sha256.digest()
            val sha256Hex = sha256Bytes.joinToString("") { "%02x".format(it) }
            val crc32Hex = crc.value.toInt().toHexString()

            // 4. Return the results in the data class.
            return DexInfo(
                entryName = entryName,
                crc32 = crc32Hex,
                sha256 = sha256Hex,
                uncompressedSize = bytes.size.toLong(),
                compressedSize = compressedSize,
                r8MapId = r8MapId,
                r8Markers = r8Markers,
                minifiedClassCountLowercase = minifiedClassCountLowercase,
                minifiedClassCountLengthHeuristic = minifiedClassCountLengthHeuristic,
                noPackageClassCount = noPackage,
                classInfo = classInfo,
            )
        }

        val CSV_TITLES =
            listOf(
                "dex_totalSizeMb",
                "dex_minifiedClassesLower",
                "dex_minifiedClassesLength",
                "dex_noPackageClasses",
                "dex_classes",
            ) +
                if (VERBOSE) {
                    listOf("dex_names", "dex_sortedChecksumsSha256", "dex_sortedChecksumsCrc32")
                } else {
                    emptyList()
                }

        fun List<DexInfo>.csvEntries(): List<String> {
            return listOf(
                (this.sumOf { it.uncompressedSize } / (1024.0 * 1024)).toString(),
                this.sumOf { it.minifiedClassCountLowercase }.toString(),
                this.sumOf { it.minifiedClassCountLengthHeuristic }.toString(),
                this.sumOf { it.noPackageClassCount }.toString(),
                this.sumOf { it.classInfo.size }.toString(),
            ) +
                if (VERBOSE)
                    listOf(
                        joinToString(INTERNAL_CSV_SEPARATOR) { it.entryName },
                        // NOTE: we individually sort each of these, so they aren't associated with
                        // each other, but they are easy to compare when joined
                        this.map { it.sha256 }.sorted().joinToString(INTERNAL_CSV_SEPARATOR),
                        this.map { it.crc32 }.sorted().joinToString(INTERNAL_CSV_SEPARATOR),
                    )
                else {
                    emptyList()
                }
        }
    }
}
