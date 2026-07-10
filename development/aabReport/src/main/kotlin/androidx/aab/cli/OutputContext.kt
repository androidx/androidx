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

package androidx.aab.cli

import androidx.aab.analysis.PackagePrefixKey
import androidx.aab.analysis.PackageStats
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class OutputContext(
    /** Output directory in which all outputs should */
    outputPath: String?,
    /** Pass true to generate an output csv file with optimization stats */
    csv: Boolean,
    /** List of patterns that will detect presence of .so file names in bundles/apks. */
    val soMatchPatterns: List<String>,
) {
    /**
     * Represents output directory for a specific app, holding references to individual files.
     *
     * Note that this isn't threadsafe, as each bundle/apk is only ever touched on a single thread.
     */
    inner class AppOutputDir(
        val outputDir: File?,
        val obfuscatedClasses: File?,
        val unobfuscatedClasses: File?,
    ) {
        constructor(
            outputDir: File?
        ) : this(
            outputDir = outputDir,
            obfuscatedClasses = outputDir?.run { File(this, "obf.csv") },
            unobfuscatedClasses = outputDir?.run { File(this, "unobf.csv") },
        ) {
            val header = "size, fullName, originalName, mappingFileLine,\n"
            obfuscatedClasses?.appendText(header)
            unobfuscatedClasses?.appendText(header)
        }

        init {
            if (outputDir != null) {
                Files.createDirectory(outputDir.toPath())
            }
        }
    }

    val outputDir =
        if (outputPath != null) {
            Path(outputPath)
                .also {
                    println("Outputting to: $outputPath")
                    it.createDirectories()
                }
                .toFile()
        } else null

    val csvFile: File? =
        if (csv) {
            if (outputDir == null) {
                println("--csv requires passing --out=<path_to_output_dir>")
                usageAndDie()
            }
            File(outputDir.toString(), "aabReport.csv").also { it.createNewFile() }
        } else null

    private val outputDirsForApps = mutableMapOf<String, AppOutputDir>()

    fun outputDirForApp(name: String): AppOutputDir {
        if (outputDir == null) {
            return AppOutputDir(null)
        }

        return synchronized(outputDirsForApps) {
            // shouldn't contend since there's one per app, but synchronized just to be safe
            outputDirsForApps.computeIfAbsent(name) { AppOutputDir(File(outputDir, name)) }
        }
    }

    private val packageStats = mutableListOf<Map<PackagePrefixKey, PackageStats>>()

    fun registerPackagePrefixInfo(stats: Map<PackagePrefixKey, PackageStats>) {
        synchronized(packageStats) { packageStats.add(stats) }
    }

    private fun File.append(packageStats: PackageStats) {
        packageStats.apply {
            appendText(
                "${identifierCount}, ${packagePrefix}, ${lowObfAppCount}, ${appCount}," +
                    " ${obfuscationRatioMedian()}, ${obfuscationRatio()}, ${bytesSeen}, ${obfBytesSeen}, ${xmlBytesSeen}\n"
            )
        }
    }

    fun dumpPackagePrefixInfoToFile(
        directory: File?,
        packagePrefixInfo: Map<PackagePrefixKey, PackageStats>,
        minAppCount: Int = 1,
    ) {
        if (directory == null) return
        File(directory, "packages.csv").apply {
            writeText(
                "identifierCount, packagePrefix, lowObfAppCount, appCount," +
                    " obfRatioMedian, obfRatioClassSize, classSize, obfClassSize, xmlClassSize\n"
            )
            packagePrefixInfo.values
                .filter {
                    it.classesSeen > 50 && it.appCount >= minAppCount
                } // note this filtration is just for dumping
                .sortedByDescending { it.classesSeen }
                .forEach { append(it) }
        }
    }

    fun dumpPerCategoryStatsToFile(
        directory: File?,
        packagePrefixInfo: Map<PackagePrefixKey, PackageStats>,
        appCountThreshold: Int,
    ) {
        if (directory == null) return

        // only look at deepest depth, to avoid double-counting

        val google = PackageStats("com.google", identifierCount = 0, appCount = 0)
        val androidx = PackageStats("androidx", identifierCount = 0, appCount = 0)
        val kotlin = PackageStats("kotlin", identifierCount = 0, appCount = 0)
        val other = PackageStats("library", identifierCount = 0, appCount = 0)
        val appCustom = PackageStats("app", identifierCount = 0, appCount = 0)

        packagePrefixInfo.values
            .filter { it.identifierCount == PackagePrefixDepths.max() }
            .forEach { packageStats ->
                when {
                    packageStats.appCount <= appCountThreshold -> appCustom
                    packageStats.packagePrefix.startsWith("androidx") -> androidx
                    packageStats.packagePrefix.startsWith("com.google") -> google
                    packageStats.packagePrefix.startsWith("kotlin") -> kotlin
                    else -> other
                }.apply { accumulate(packageStats) }
            }

        File(directory, "categories.csv").apply {
            append(google)
            append(androidx)
            append(kotlin)
            append(other)
            append(appCustom)
        }
    }

    fun dumpPackagePrefixInfo() {
        if (outputDir == null) return

        val result = mutableMapOf<PackagePrefixKey, PackageStats>()
        synchronized(packageStats) {
            println("Dumping package stats for ${packageStats.size} apps")
            packageStats.forEach { appPackagePrefixInfo ->
                appPackagePrefixInfo.forEach { (prefix, packageStats) ->
                    result
                        .computeIfAbsent(prefix) {
                            PackageStats(
                                packagePrefix = prefix.packagePrefix,
                                identifierCount = prefix.identifierCount,
                                appCount = 0,
                            )
                        }
                        .apply { accumulate(packageStats) }
                }
            }
        }

        dumpPackagePrefixInfoToFile(outputDir, result, minAppCount = 4 /* arbitrary! */)
        dumpPerCategoryStatsToFile(outputDir, result, appCountThreshold = 4)
    }

    companion object {
        val PackagePrefixDepths = listOf(2, 3, 4, 5, 6)
    }
}
