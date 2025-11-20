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

import androidx.aab.ApkInfo
import androidx.aab.BundleInfo
import androidx.aab.CsvColumn
import androidx.aab.analysis.AnalyzedApkInfo
import androidx.aab.analysis.AnalyzedBundleInfo
import androidx.aab.fullHeader
import androidx.aab.rowStringForItem
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

var VERBOSE = false
const val OUTPUT_PATH_PREFIX = "--out="
const val SO_PATTERN_PREFIX = "--soPatterns="

// this is a simple way to abstract the difference between the two, but should probably define
// interfaces
internal abstract class PackageProcessor<T>(val typeLabel: String) {
    abstract fun getCsvColumns(): List<CsvColumn<T>>

    abstract fun transform(file: File): T

    abstract fun printAnalysis(item: T)

    abstract fun sortKey(item: T): String

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun process(files: List<File>, outputContext: OutputContext) {
        println("Analyzing ${files.size} ${typeLabel}s...")
        val items =
            files
                .asFlow()
                .flatMapMerge { file ->
                    flow {
                            try {
                                emit(transform(file))
                            } catch (t: Throwable) {
                                println("ERROR during parsing ${file.absolutePath}, skipping")
                                t.printStackTrace()
                            }
                        }
                        .flowOn(Dispatchers.IO)
                }
                .toList()
                .sortedBy { sortKey(it) }
        if (outputContext.csvFile != null) {
            val columns = getCsvColumns()
            println("$typeLabel parsing complete, constructing CSV...")
            outputContext.csvFile.bufferedWriter().use {
                it.write(columns.fullHeader() + "\n")
                items.forEach { item -> it.write(columns.rowStringForItem(item) + "\n") }
            }
            println("Analysis complete, CSV saved to ${outputContext.csvFile.absolutePath}")
        } else {
            println("$typeLabel parsing complete, reporting problems...")
            items.forEach { printAnalysis(it) }
        }
        outputContext.dumpPackagePrefixInfo()
    }
}

fun usageAndDie() {
    println(
        """
            Report Usage:
                 java -jar <path-to-jar> [--verbose] [--out=<output_dir_path>] <path-to-aab> [<path-to-aab2>...]
            CSV Usage:
                 java -jar <path-to-jar> [--verbose] --out=<output_dir_path> --csv <path-to-aab> [<path-to-aab2>...]

            --out must be passed to see obfuscation and package keep statistics.
            """
            .trimIndent()
    )
    exitProcess(1)
}

lateinit var outputContext: OutputContext

fun main(args: Array<String>) = runBlocking {
    outputContext =
        OutputContext(
            outputPath =
                args
                    .singleOrNull { it.startsWith(OUTPUT_PATH_PREFIX) }
                    ?.substringAfter(OUTPUT_PATH_PREFIX)
                    ?.replaceFirst("~", System.getProperty("user.home")),
            csv = args.contains("--csv"),
            soMatchPatterns =
                args
                    .singleOrNull { it.startsWith(SO_PATTERN_PREFIX) }
                    ?.substringAfter(SO_PATTERN_PREFIX)
                    ?.split(",") ?: emptyList(),
        )

    VERBOSE = args.contains("--verbose") || args.contains("-v")

    val pathArgs = args.filter { !it.startsWith("--") }
    if (pathArgs.isEmpty()) {
        println(
            "Expected one or more android app bundle/apk files, or directories of bundles/apks to be passed."
        )
        usageAndDie()
    }

    val files =
        pathArgs
            .flatMap { path ->
                val f = File(path)
                if (f.isDirectory) {
                    (f.listFiles()?.toList()) ?: emptyList()
                } else {
                    listOf(f)
                }
            }
            .filter { it.name != ".DS_Store" }
            .sorted()

    val processor =
        if (files.any { it.name.endsWith(".apk") }) {
            object : PackageProcessor<AnalyzedApkInfo>("APK") {
                override fun getCsvColumns() = AnalyzedApkInfo.CSV_COLUMNS

                override fun transform(file: File): AnalyzedApkInfo =
                    AnalyzedApkInfo(ApkInfo.from(file))

                override fun printAnalysis(item: AnalyzedApkInfo) = item.printAnalysis()

                override fun sortKey(item: AnalyzedApkInfo): String = item.apkInfo.path
            }
        } else {
            object : PackageProcessor<AnalyzedBundleInfo>("Bundle") {
                override fun getCsvColumns() = AnalyzedBundleInfo.CSV_COLUMNS

                override fun transform(file: File): AnalyzedBundleInfo =
                    AnalyzedBundleInfo(BundleInfo.from(file))

                override fun printAnalysis(item: AnalyzedBundleInfo) = item.printAnalysis()

                override fun sortKey(item: AnalyzedBundleInfo): String = item.bundleInfo.path
            }
        }

    processor.process(files, outputContext)
}
