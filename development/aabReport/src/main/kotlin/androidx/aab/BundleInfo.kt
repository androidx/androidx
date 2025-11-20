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

import com.android.tools.build.libraries.metadata.AppDependencies
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/** Separator for CSV output within entries (such as multiple dex SHAs in one column) */
const val INTERNAL_CSV_SEPARATOR = "--"

/**
 * Container for all information extracted directly from the bundle, prior to any cross-reference
 * analysis.
 */
data class BundleInfo(
    val path: String,
    val profileInfo: ProfInfo?,
    val dexInfo: List<DexInfo>,
    val soInfo: List<SoInfo>,
    val mappingFileInfo: MappingFileInfo?,
    val r8JsonFileInfo: R8JsonFileInfo?,
    val dotVersionFiles: Map<String, String>, // map maven coordinates -> version number
    val appBundleDependencies: AppDependencies?,
    val appMetadataPropsInfoMetaInf: AppMetadataPropsInfo?,
    val appMetadataPropsInfoBundleMetadata: AppMetadataPropsInfo?,
) {
    companion object {
        val CSV_COLUMNS =
            listOf(
                CsvColumn<BundleInfo>(
                    "filename",
                    description = "Filename when tool was run",
                    calculate = { it.path.substringAfterLast(File.separatorChar) },
                )
            ) +
                ProfInfo.CSV_COLUMNS.mapAll { it.profileInfo } +
                DexInfo.CSV_COLUMNS.mapAll { it.dexInfo } +
                SoInfo.CSV_COLUMNS.mapAll { it.soInfo } +
                MappingFileInfo.CSV_COLUMNS.mapAll { it.mappingFileInfo } +
                R8JsonFileInfo.CSV_COLUMNS.mapAll { it.r8JsonFileInfo } +
                AppMetadataPropsInfo.CSV_COLUMNS_META_INF.mapAll {
                    it.appMetadataPropsInfoMetaInf
                } +
                AppMetadataPropsInfo.CSV_COLUMNS_BUNDLE.mapAll {
                    it.appMetadataPropsInfoBundleMetadata
                }

        // TODO: Move to wrapper object
        const val DEPENDENCIES_PB_LOCATION =
            "BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb"

        fun from(file: File): BundleInfo {
            return FileInputStream(file).use { from(file.path, it) }
        }

        fun from(path: String, inputStream: InputStream): BundleInfo {
            val dexInfo = mutableListOf<DexInfo>()
            val soInfo = mutableListOf<SoInfo>()
            val dotVersionFiles = mutableMapOf<String, String>()
            var mappingFileInfo: MappingFileInfo? = null
            var r8MetadataFileInfo: R8JsonFileInfo? = null
            var appDependencies: AppDependencies? = null
            var profileInfo: ProfInfo? = null
            var appMetadataPropsInfoMetaInf: AppMetadataPropsInfo? = null
            var appMetadataPropsInfoBundleMetadata: AppMetadataPropsInfo? = null
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                while (entry != null) {
                    when {
                        entry.name.contains("/dex/classes") && entry.name.endsWith(".dex") -> {
                            dexInfo.add(DexInfo.from(entry.name, entry.compressedSize, zis))
                        }

                        entry.name == ProfInfo.BUNDLE_LOCATION -> {
                            profileInfo = ProfInfo.readFromProfile(zis)
                        }

                        entry.name.endsWith(".version") && entry.name.contains("/META-INF/") -> {
                            dotVersionFiles[entry.name] = zis.bufferedReader().readText().trim()
                        }

                        entry.name == R8JsonFileInfo.BUNDLE_LOCATION_D8 -> {
                            if (r8MetadataFileInfo != null) {
                                println("Found duplicate r8 or d8 json files")
                            }
                            r8MetadataFileInfo = R8JsonFileInfo.fromD8()
                        }

                        entry.name == R8JsonFileInfo.BUNDLE_LOCATION_R8 -> {
                            if (r8MetadataFileInfo != null) {
                                println("Found duplicate r8 or d8 json files")
                            }
                            r8MetadataFileInfo = R8JsonFileInfo.fromR8Json(zis)
                        }

                        entry.name == DEPENDENCIES_PB_LOCATION -> {
                            appDependencies = AppDependencies.ADAPTER.decode(zis)
                        }

                        entry.name == MappingFileInfo.BUNDLE_LOCATION -> {
                            mappingFileInfo = MappingFileInfo.from(zis)
                        }

                        entry.name == AppMetadataPropsInfo.BUNDLE_LOCATION_METADATA -> {
                            appMetadataPropsInfoBundleMetadata = AppMetadataPropsInfo.from(zis)
                        }

                        entry.name == AppMetadataPropsInfo.BUNDLE_LOCATION_META_INF -> {
                            appMetadataPropsInfoMetaInf = AppMetadataPropsInfo.from(zis)
                        }

                        entry.name.endsWith(".so") -> {
                            soInfo.add(SoInfo(bundlePath = entry.name, size = zis.countBytes()))
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            return BundleInfo(
                path = path,
                profileInfo = profileInfo,
                dexInfo = dexInfo,
                soInfo = soInfo,
                mappingFileInfo = mappingFileInfo,
                r8JsonFileInfo = r8MetadataFileInfo,
                dotVersionFiles = dotVersionFiles,
                appBundleDependencies = appDependencies,
                appMetadataPropsInfoMetaInf = appMetadataPropsInfoMetaInf,
                appMetadataPropsInfoBundleMetadata = appMetadataPropsInfoBundleMetadata,
            )
        }
    }
}
