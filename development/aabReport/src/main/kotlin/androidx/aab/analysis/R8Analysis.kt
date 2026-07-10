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

package androidx.aab.analysis

import androidx.aab.*
import androidx.aab.analysis.R8Issues.getPrimaryOptimizationIssue
import androidx.aab.cli.OutputContext
import androidx.aab.cli.OutputContext.Companion.PackagePrefixDepths
import androidx.aab.cli.outputContext
import kotlin.math.roundToInt

data class PackagePrefixKey(val packagePrefix: String, val identifierCount: Int)

data class PackageStats(
    val packagePrefix: String,
    val identifierCount: Int,
    var lowObfAppCount: Int = 0,
    var appCount: Int,
    var classesSeen: Long = 0,
    var obfClassesSeen: Long = 0,
    var xmlClassesSeen: Long = 0,
    var obfBytesSeen: Long = 0,
    var bytesSeen: Long = 0,
    var xmlBytesSeen: Long = 0,
    private val ratiosSeen: MutableList<Double> = mutableListOf(),
) {
    fun obfuscationRatio(): Double {
        return obfBytesSeen.toDouble() / (bytesSeen - xmlBytesSeen)
    }

    fun accumulate(other: PackageStats) {
        appCount += other.appCount
        lowObfAppCount += other.lowObfAppCount
        classesSeen += other.classesSeen
        obfClassesSeen += other.obfClassesSeen
        xmlClassesSeen += other.xmlClassesSeen
        bytesSeen += other.bytesSeen
        obfBytesSeen += other.obfBytesSeen
        xmlBytesSeen += other.xmlBytesSeen

        if (other.ratiosSeen.isEmpty()) {
            // only support calculating median when accumulating leaf PackageStats (from an app)
            ratiosSeen.add(other.obfuscationRatio())
        }
    }

    fun obfuscationRatioMedian(): Double? {
        return computeMedian(ratiosSeen)
    }

    private fun computeMedian(list: List<Double>): Double? {
        if (list.isEmpty()) return null

        // Sort the list in ascending order
        val sortedList = list.sorted()
        val size = sortedList.size

        return if (size % 2 == 0) {
            // If the size is even, average the two middle elements
            val midIndex = size / 2
            val median = (sortedList[midIndex - 1] + sortedList[midIndex]) / 2.0
            median
        } else {
            // If the size is odd, the median is the middle element
            val midIndex = size / 2
            sortedList[midIndex]
        }
    }
}

/**
 * Tracks stats respecting minification/obfuscation heuristics.
 *
 * Note that throughout aabReport, we only consider dex top level classes (that is classes that in
 * the dex file are not inner classes).
 */
data class MinificationStats(
    val minifiedClassesLowerAccuracy: Double,
    val minifiedClassesLengthAccuracy: Double,
    val minifiedRate: Double,
) {
    companion object {
        fun fromMappingAndDex(
            appOutputDir: OutputContext.AppOutputDir,
            mappingFileInfo: MappingFileInfo?,
            dexInfo: List<DexInfo>,
        ): MinificationStats? {
            if (mappingFileInfo == null) return null

            val classInfo = dexInfo.flatMap { it.classInfo }

            val allClassesInDex = classInfo.map { it.fullName }.toSet()
            val prunedMappingFileInfo =
                mappingFileInfo.dexClassNameToOriginalName.filter { it.key in allClassesInDex }

            var isObfuscatedCount = 0
            var isObfuscatedLowerCaseHits = 0
            var isObfuscatedAppearsMinifiedHits = 0

            val packagePrefixInfo = mutableMapOf<PackagePrefixKey, PackageStats>()

            val obfOutput = appOutputDir.obfuscatedClasses?.bufferedWriter()
            val unobfOutput = appOutputDir.unobfuscatedClasses?.bufferedWriter()

            try {
                classInfo.forEach { clazz ->
                    val remapInfo = prunedMappingFileInfo[clazz.fullName]

                    val isObfuscatedAccordingToMappingFile = remapInfo?.wasRemapped ?: false
                    val packageName =
                        remapInfo?.originalName?.substringBeforeLast(".") ?: clazz.packageName

                    val packageIdentifiers = packageName.split(".")
                    for (identifierCount in PackagePrefixDepths) {
                        val key =
                            PackagePrefixKey(
                                packagePrefix =
                                    packageIdentifiers.take(identifierCount).joinToString("."),
                                identifierCount = identifierCount,
                            )
                        packagePrefixInfo
                            .computeIfAbsent(key) {
                                PackageStats(
                                    packagePrefix = key.packagePrefix,
                                    identifierCount = key.identifierCount,
                                    appCount = 1,
                                )
                            }
                            .apply {
                                classesSeen += 1
                                bytesSeen += clazz.size
                                if (isObfuscatedAccordingToMappingFile) {
                                    obfClassesSeen++
                                    obfBytesSeen += clazz.size
                                } else if (clazz.usedByXml) {
                                    // not obfuscated, but referenced by xml
                                    xmlClassesSeen++
                                    xmlBytesSeen += clazz.size
                                }

                                lowObfAppCount =
                                    if (
                                        obfClassesSeen.toDouble() / (classesSeen - xmlClassesSeen) < 0.25
                                    ) {
                                        1
                                    } else {
                                        0
                                    }
                            }
                    }

                    if (clazz.startsWithLowerCase == isObfuscatedAccordingToMappingFile) {
                        isObfuscatedLowerCaseHits++
                    }
                    if (clazz.classNameAppearsMinified == isObfuscatedAccordingToMappingFile) {
                        isObfuscatedAppearsMinifiedHits++
                    }
                    if (isObfuscatedAccordingToMappingFile) {
                        isObfuscatedCount++
                    }

                    if (isObfuscatedAccordingToMappingFile) {
                            obfOutput
                        } else {
                            unobfOutput
                        }
                        ?.write(
                            "${clazz.size}, \"${clazz.fullName}\", \"${remapInfo?.originalName}\", \"${remapInfo?.mapLine}\",\n"
                        )
                }
            } finally {
                obfOutput?.close()
                unobfOutput?.close()
            }

            if (isObfuscatedCount > 0.25 * classInfo.count()) {
                outputContext.dumpPackagePrefixInfoToFile(appOutputDir.outputDir, packagePrefixInfo)
                // only register to global stats if app looks somewhat obfuscated
                outputContext.registerPackagePrefixInfo(packagePrefixInfo)
            }

            return MinificationStats(
                minifiedClassesLowerAccuracy = isObfuscatedLowerCaseHits.toDouble() / classInfo.size,
                minifiedClassesLengthAccuracy =
                    isObfuscatedAppearsMinifiedHits.toDouble() / classInfo.size,
                minifiedRate = isObfuscatedCount.toDouble() / classInfo.size,
            )
        }
    }
}

data class R8Analysis(
    val mappingPresent: Boolean,
    val compilerMarker: Compiler,
    val compilerJson: Compiler,
    val r8JsonFileExpected: Boolean,
    val r8JsonFileInfo: R8JsonFileInfo?,
    val dexSha256ChecksumsMatching: Set<String>,
    val dexSha256ChecksumsR8JsonOnly: Set<String>,
    val dexSha256ChecksumsDexOnly: Set<String>,
    val minificationStats: MinificationStats?,
) : ScoreReporter {

    override fun getSubScore(): SubScore {
        val issues =
            listOfNotNull(
                if (dexSha256ChecksumsR8JsonOnly.isNotEmpty()) R8Issues.DexChecksumsMismatched
                else null,
                if (!mappingPresent && r8JsonFileInfo == null) R8Issues.NoMappingFileOrJsonMetadata
                else null,
                if (r8JsonFileExpected && r8JsonFileInfo == null) R8Issues.MissingR8JsonMetadata
                else null,
                r8JsonFileInfo?.getPrimaryOptimizationIssue(),
            )

        return SubScore(
            label = "R8 / Dex Optimization",
            score = r8JsonFileInfo?.getScore(),
            maxScore = 50,
            issues = issues,
        )
    }

    fun getDexMatchRatio(): Double? {
        return if (
            r8JsonFileInfo != null &&
                (dexSha256ChecksumsR8JsonOnly.isNotEmpty() ||
                    dexSha256ChecksumsMatching.isNotEmpty())
        ) {
            dexSha256ChecksumsMatching.size.toDouble() /
                (dexSha256ChecksumsR8JsonOnly.size + dexSha256ChecksumsMatching.size)
        } else {
            null
        }
    }

    companion object {

        fun R8JsonFileInfo.getScore(): Int {
            return (50 *
                    ((if (this.shrinkingEnabled) 0.3 else 0.0) +
                        (if (this.optimizationEnabled) 0.5 else 0.0) +
                        (if (this.obfuscationEnabled) 0.2 else 0.0)))
                .roundToInt()
        }

        val CSV_COLUMNS =
            listOf(
                CsvColumn<R8Analysis>(
                    columnLabel = "r8_score",
                    description = "experimental - Score for R8 adoption, out of 50",
                    requiresVerbose = true,
                    calculate = { (it.r8JsonFileInfo?.getScore()).toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_compilerFromMarker",
                    description =
                        "Which compiler (d8 vs r8) is being used, based on dex marker string",
                    calculate = { it.compilerMarker.toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_compilerFromJson",
                    description =
                        "Which compiler (d8 vs r8) is being used, based on r8.json from bundle metadata. Requires AGP 8.8+",
                    calculate = { it.compilerJson.toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_ratio_json_shas_match_dex",
                    description =
                        "What portion of dex shas from r8.json match those of the dex files - indicator of dex post-processing",
                    calculate = { it.getDexMatchRatio().toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_minifiedClassesLowerAccuracy",
                    description = "Accuracy of lowercase heuristic, based upon mapping file",
                    calculate = { (it.minificationStats?.minifiedClassesLowerAccuracy).toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_minifiedClassesLengthAccuracy",
                    description = "Accuracy of length heuristic, based upon mapping file",
                    calculate = { (it.minificationStats?.minifiedClassesLowerAccuracy).toString() },
                ),
                CsvColumn(
                    columnLabel = "r8_minifiedRate",
                    description = "Obfuscation ratio based upon mapping file",
                    calculate = { (it.minificationStats?.minifiedRate).toString() },
                ),
            )

        fun ApkInfo.getR8Analysis(): R8Analysis {
            return R8Analysis(
                mappingPresent = false,
                compilerMarker = Compiler.fromMarkers(dexInfo),
                compilerJson = Compiler.Unknown,
                r8JsonFileExpected = false,
                r8JsonFileInfo = null,
                dexSha256ChecksumsDexOnly = emptySet(),
                dexSha256ChecksumsMatching = emptySet(),
                dexSha256ChecksumsR8JsonOnly = emptySet(),
                minificationStats = null,
            )
        }

        fun BundleInfo.getR8Analysis(): R8Analysis {
            val metadataJsonShas = r8JsonFileInfo?.dexShas?.toSet() ?: emptySet()
            val dexShas = dexInfo.map { it.sha256 }.toSet()

            return R8Analysis(
                mappingPresent = mappingFileInfo != null,
                compilerMarker = Compiler.fromMarkers(dexInfo),
                // technically, should capture all *8.json files, but in comparison to dex markers,
                // unlikely to be Both
                compilerJson = r8JsonFileInfo?.compiler ?: Compiler.Unknown,
                r8JsonFileExpected =
                    (this.appMetadataPropsInfoBundleMetadata ?: this.appMetadataPropsInfoMetaInf)
                        ?.agpAtLeast(8, 8) ?: false,
                r8JsonFileInfo = r8JsonFileInfo,
                dexSha256ChecksumsMatching = metadataJsonShas.intersect(dexShas),
                dexSha256ChecksumsDexOnly = dexShas - metadataJsonShas,
                dexSha256ChecksumsR8JsonOnly = metadataJsonShas - dexShas,
                minificationStats =
                    MinificationStats.fromMappingAndDex(
                        outputContext.outputDirForApp(this.path.substringAfterLast("/")),
                        mappingFileInfo,
                        dexInfo,
                    ),
            )
        }
    }
}
