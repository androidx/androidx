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

import java.util.zip.ZipInputStream

/**
 * Bundle information captured from `proguard.map`
 *
 * Currently, it just includes information from the header
 */
data class RemappedClass(val originalName: String, val wasRemapped: Boolean, val mapLine: String)

data class MappingFileInfo(
    val compiler: String?,
    val compilerVersion: String?,
    val minApi: String?,
    val mappingVersion: String?,
    val pgMapId: String?,
    val pgMapHash: String?,
    val dexClassNameToOriginalName: Map<String, RemappedClass>,
) {
    // Summary type of patterns we see in mapping files
    enum class Type {
        Minimal,
        R8,
        D8,
        Other,
        R8Incomplete,
        D8Incomplete,
        OtherIncomplete,
    }

    fun getType(): Type {
        if (
            this ==
                MappingFileInfo(
                    mappingVersion = this.mappingVersion,
                    compiler = null,
                    compilerVersion = null,
                    minApi = null,
                    pgMapId = null,
                    pgMapHash = null,
                    dexClassNameToOriginalName = emptyMap(),
                )
        ) {
            return Type.Minimal
        }
        if (
            compiler == null ||
                compilerVersion == null ||
                minApi == null ||
                mappingVersion == null ||
                pgMapId == null ||
                pgMapHash == null
        ) {
            return when (compiler) {
                "R8" -> Type.R8Incomplete
                "D8" -> Type.D8Incomplete
                else -> Type.OtherIncomplete
            }
        }
        return when (compiler) {
            "R8" -> Type.R8
            "D8" -> Type.D8
            else -> Type.Other
        }
    }

    companion object {
        const val BUNDLE_LOCATION =
            "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"

        val CSV_COLUMNS =
            listOf(
                CsvColumn<MappingFileInfo?>(
                    "mapping_file_version",
                    "Version number in mapping file, or null if no mapping file present",
                    calculate = { it?.mappingVersion.toString() },
                ),
                CsvColumn<MappingFileInfo?>(
                    "mapping_file_type",
                    "Mapping file header pattern, of those observed in common mapping file headers",
                    calculate = { it?.getType().toString() },
                ),
            )

        private fun List<String>.findAfterPrefix(prefix: String): String? {
            return firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()
        }

        private fun List<String>.findMappingVersion(): String? {
            val prefix = """# {"id":"com.android.tools.r8.mapping","version":""""
            return firstOrNull { it.startsWith(prefix) }
                ?.removePrefix(prefix)
                ?.substringBefore("\"")
        }

        private fun List<String>.findMappingVersionSQ(): String? {
            val prefix = """# {'id':'com.android.tools.r8.mapping','version':'"""
            return firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.substringBefore("'")
        }

        private val classMappingRegex = """^([\w-.$]+)\s+->\s+([\w-.$]+):\s*$""".toRegex()

        fun from(zis: ZipInputStream): MappingFileInfo {
            val sequence = zis.bufferedReader().lineSequence()

            val headerLines = mutableListOf<String>()
            var readingHeader = true

            val dexClassNameToOriginalName = mutableMapOf<String, RemappedClass>()
            sequence.forEach { line ->
                val trimmedLine = line.trim()
                if (readingHeader) {
                    if (trimmedLine.startsWith("#")) {
                        headerLines.add(line)
                    } else {
                        readingHeader = false
                    }
                }

                if (!readingHeader) {
                    val match = classMappingRegex.matchEntire(line)
                    if (match != null) {
                        val originalName = match.groupValues[1]
                        val dexName = match.groupValues[2]
                        if (!dexName.contains("$") /* ignore dex inner classes */) {
                            dexClassNameToOriginalName[dexName] =
                                RemappedClass(
                                    originalName = originalName,
                                    wasRemapped =
                                        dexName.substringAfterLast(".") !=
                                            originalName.substringAfterLast("."),
                                    mapLine = line,
                                )
                        }
                    }
                }
            }

            /**
             * Example preamble:
             * ```
             * # compiler: R8
             * # compiler_version: 8.10.18
             * # min_api: 22
             * # common_typos_disable
             * # {"id":"com.android.tools.r8.mapping","version":"2.2"}
             * # pg_map_id: 4be8256
             * # pg_map_hash: SHA-256 4be82561ad6ce216e1334c2143d280d8f812fcb21d7b3179135be0392f39fe15
             * ```
             */
            return MappingFileInfo(
                compiler = headerLines.findAfterPrefix("# compiler:"),
                compilerVersion = headerLines.findAfterPrefix("# compiler_version:"),
                minApi = headerLines.findAfterPrefix("# min_api:"),
                mappingVersion =
                    headerLines.findMappingVersion() ?: headerLines.findMappingVersionSQ(),
                pgMapId = headerLines.findAfterPrefix("# pg_map_id:"),
                pgMapHash = headerLines.findAfterPrefix("# pg_map_hash:"),
                dexClassNameToOriginalName = dexClassNameToOriginalName,
            )
        }
    }
}
