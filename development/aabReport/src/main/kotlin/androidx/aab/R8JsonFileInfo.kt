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

import com.android.tools.r8.metadata.R8BuildMetadata
import java.io.InputStream

enum class Compiler {
    Unknown,
    D8,
    R8,
    Both;

    companion object {
        fun fromPresence(d8: Boolean, r8: Boolean): Compiler {
            return when {
                d8 && r8 -> Both
                d8 -> D8
                r8 -> R8
                else -> Unknown
            }
        }

        fun fromMarkers(dexInfo: List<DexInfo>): Compiler {
            return Compiler.fromPresence(
                d8 =
                    dexInfo.any { dex ->
                        dex.r8Markers.any { it.compiler == "D8" && it.map["backend"] == "dex" }
                    },
                r8 =
                    dexInfo.any { dex ->
                        dex.r8Markers.any { it.compiler == "R8" && it.map["backend"] == "dex" }
                    },
            )
        }
    }
}

/**
 * Information captured from the `r8.json` or `d8.json` file.
 *
 * Note that if loaded from a D8 file, features not supported by D8 builds are reported as
 * non-functioning (e.g. shrinking)
 */
@ConsistentCopyVisibility
data class R8JsonFileInfo
private constructor(
    val compiler: Compiler,
    val dexShas: Set<String>,
    val startupDexShas: Set<String>,
    val optimizationEnabled: Boolean,
    val optimizationDisablePercent: Float,
    val obfuscationEnabled: Boolean,
    val obfuscationDisabledPercent: Float,
    val shrinkingEnabled: Boolean,
    val shrinkingDisabledPercent: Float,
    val optimizedResourceShrinkingEnabled: Boolean?,
    val fullMode: Boolean,
) {
    companion object {
        const val BUNDLE_LOCATION_R8 = "BUNDLE-METADATA/com.android.tools/r8.json"
        const val BUNDLE_LOCATION_D8 = "BUNDLE-METADATA/com.android.tools/d8.json"

        /**
         * All values are default, based on presence of d8.json
         *
         * Eventually could actually parse d8.json
         */
        fun fromD8(): R8JsonFileInfo {
            return R8JsonFileInfo(
                compiler = Compiler.D8,
                dexShas = emptySet(),
                optimizationEnabled = false,
                optimizationDisablePercent = 100.0f,
                obfuscationEnabled = false,
                obfuscationDisabledPercent = 100.0f,
                shrinkingEnabled = false,
                shrinkingDisabledPercent = 100.0f,
                optimizedResourceShrinkingEnabled = false,
                fullMode = false,
                startupDexShas = setOf(),
            )
        }

        /** Read R8Json info from r8.json file content */
        @Suppress("UNCHECKED_CAST")
        fun fromR8Json(src: InputStream): R8JsonFileInfo? {
            val text = src.bufferedReader().readText()
            val metadata = R8BuildMetadata.fromJson(text)
            if (metadata.dexFilesMetadata == null) {
                // assume this isn't properly formed metadata file. For example, have observed
                // json file with just the content `{"version":"8.7.18"}`, from before content was
                // filled
                return null
            }

            return R8JsonFileInfo(
                compiler = Compiler.R8,
                dexShas = metadata.dexFilesMetadata.map { it.checksum }.toSet(),
                optimizationEnabled = metadata.optionsMetadata.isOptimizationsEnabled,
                obfuscationEnabled = metadata.optionsMetadata.isObfuscationEnabled,
                shrinkingEnabled = metadata.optionsMetadata.isShrinkingEnabled,
                optimizationDisablePercent = metadata.statsMetadata.noOptimizationPercentage,
                obfuscationDisabledPercent = metadata.statsMetadata.noObfuscationPercentage,
                shrinkingDisabledPercent = metadata.statsMetadata.noShrinkingPercentage,
                optimizedResourceShrinkingEnabled =
                    metadata.resourceOptimizationMetadata?.isOptimizedShrinkingEnabled,
                fullMode = !metadata.optionsMetadata.isProGuardCompatibilityModeEnabled,
                startupDexShas =
                    metadata.dexFilesMetadata.filter { it.isStartup }.map { it.checksum }.toSet(),
            )
        }

        val CSV_COLUMNS =
            listOf(
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_optimizationEnabled",
                    description = "Is optimization enabled?",
                    calculate = { it?.optimizationEnabled.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_obfuscation",
                    description = "Is obfuscation enabled?",
                    calculate = { it?.obfuscationEnabled.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_shrinkingEnabled",
                    description = "Is shrinking enabled?",
                    calculate = { it?.shrinkingEnabled.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_fullMode",
                    description = "Is full mode enabled?",
                    calculate = { it?.fullMode.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_optimizationDisablePercent",
                    description = "Optimization disabled percent",
                    calculate = { it?.optimizationDisablePercent.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_obfuscationDisablePercent",
                    description = "Obfuscation disabled percent",
                    calculate = { it?.obfuscationDisabledPercent.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_optimizationDisablePercent",
                    description = "Shrinking disabled percent",
                    calculate = { it?.shrinkingDisabledPercent.toString() },
                ),
                CsvColumn<R8JsonFileInfo?>(
                    "r8json_sortedDexChecksumsSha256",
                    description =
                        "Sorted list of dex sha256 checksums - can be validated against content of dex",
                    requiresVerbose = true,
                    calculate = {
                        it?.dexShas?.sorted()?.joinToString(separator = INTERNAL_CSV_SEPARATOR)
                            ?: "null"
                    },
                ),
            )
    }
}
