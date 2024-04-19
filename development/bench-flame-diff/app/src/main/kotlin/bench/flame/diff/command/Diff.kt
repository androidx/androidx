/*
 * Copyright 2024 The Android Open Source Project
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
package bench.flame.diff.command

import bench.flame.diff.config.Paths
import bench.flame.diff.interop.execWithChecks
import bench.flame.diff.interop.file
import bench.flame.diff.interop.id
import bench.flame.diff.interop.log
import bench.flame.diff.interop.openFileInOs
import bench.flame.diff.ui.promptProvideFile
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.MutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import kotlin.io.path.absolutePathString

class Diff : CliktCommand(help = "Compare two saved trace files.") {
    private val before by fileOption("before")
    private val after by fileOption("after")

    override fun run() {
        Init.verifyDependencies()

        val before: File = when {
            before == null -> promptProvideFile("Provide the 'before' path",
                excludePattern = ".*\\.json$",
                defaultSrcDir = Paths.savedTracesDir.toFile())
            else -> checkNotNull(before)
        }
        val after: File = when {
            after == null -> promptProvideFile("Provide the 'after' path",
                excludePattern = ".*\\.json$",
                defaultSrcDir = Paths.savedTracesDir.toFile())
            else -> checkNotNull(after)
        }

        // TODO: support custom labels
        val diffDir: File = run {
            fun createPath(number: Int): File {
                val dstDirBaseName =
                    "diff_${before.nameWithoutExtension}_${after.nameWithoutExtension}"
                val suffix = if (number == 0) "" else "_${String.format("%03d", number)}"
                return Paths.savedDiffsDir.resolve("$dstDirBaseName$suffix").toFile()
            }

            var i = 0
            var result = createPath(i)
            while (result.exists()) result = createPath(++i)
            result.mkdirs()
            result
        }

        val indexHtml = createDiffHtmlPage(before, after, diffDir)
        openFileInOs(indexHtml)
        log("Opened '${indexHtml.canonicalPath}' in the browser.", isStdErr = true)
    }

    private fun fetchBenchmarkResultForTrace(traceFile: File): String {
        val file = traceFile.parentFile.resolve(traceFile.nameWithoutExtension + ".json")
        if (!file.exists()) {
            log(
                "Could not locate benchmark result file for trace: ${traceFile.name} ",
                isStdErr = true
            )
            return ""
        }
        return file.readText()
    }

    private fun createDiffHtmlPage(beforeRaw: File, afterRaw: File, diffDir: File): File {
        val beforeRawFolded = diffDir.resolve("before-raw.folded")
        val afterRawFolded = diffDir.resolve("after-raw.folded")
        val beforeRawSvg = diffDir.resolve("before-raw.svg")
        val afterRawSvg = diffDir.resolve("after-raw.svg")

        val afterDiffFolded = diffDir.resolve("after-diff.folded")
        val beforeDiffFolded = diffDir.resolve("before-diff.folded")
        val afterDiffSvg = diffDir.resolve("after-diff.svg")
        val beforeDiffSvg = diffDir.resolve("before-diff.svg")

        val indexHtml = diffDir.resolve("index.html")

        collapseStacks(beforeRaw, beforeRawFolded)
        collapseStacks(afterRaw, afterRawFolded)
        createDiff(beforeRawFolded, afterRawFolded, afterDiffFolded)
        createDiff(afterRawFolded, beforeRawFolded, beforeDiffFolded)

        createFlameGraph(beforeRawFolded, beforeRawSvg)
        createFlameGraph(afterRawFolded, afterRawSvg)
        createFlameGraph(afterDiffFolded, afterDiffSvg)
        createFlameGraph(beforeDiffFolded, beforeDiffSvg, negate = true)

        // fetch benchmark result data
        val benchmarkResultBefore = fetchBenchmarkResultForTrace(beforeRaw)
        val benchmarkResultAfter = fetchBenchmarkResultForTrace(afterRaw)
        createIndexHtml(
            TraceDiffResult(
                beforeRawSvg,
                beforeDiffSvg,
                afterRawSvg,
                afterDiffSvg,
                benchmarkResultBefore,
                benchmarkResultAfter,
                beforeRaw.name,
                afterRaw.name
            ), indexHtml
        )

        for (tmpFile in listOf(beforeRawFolded, afterRawFolded, beforeDiffFolded, afterDiffFolded))
            tmpFile.delete()
        return indexHtml
    }

    private data class TraceDiffResult(
        val beforeRawGraph: File,
        val beforeDiffGraph: File,
        val afterRawGraph: File,
        val afterDiffGraph: File,
        val beforeBenchmarkResult: String,
        val afterBenchmarkResult: String,
        val beforeTraceFileName: String,
        val afterTraceFileName: String
    )

    private fun fileOption(role: String): MutuallyExclusiveOptions<File, File?> {
        return mutuallyExclusiveOptions(
            option(
                "--$role-file",
                help = "Path to the '$role' file."
            ).file(mustExist = true, canBeDir = false),
            option(
                "--$role-id",
                help = "Id of the '$role' file as per the **${List().commandName}** command"
            ).int()
                .convert { id: Int ->
                    checkNotNull(List.savedTraces(Paths.savedTracesDir.toFile())
                        .singleOrNull { it.id == id }) { "no saved trace with id '$id'" }
                        .file
                }
                .check { f: File -> f.exists() && f.isFile },
            name = "${role.take(1).uppercase()}${role.drop(1)} file",
        ).single()
    }

    private fun collapseStacks(srcFile: File, dstFile: File) =
        execWithChecks(
            Paths.stackcollapsePy.absolutePathString(),
            "--trace-offcpu=mixed-on-off-cpu",
            "--event-filter", "cpu-clock", // this only allows for on-cpu; pending (b/325484390)
            "-i",
            srcFile.absolutePath,
            cwd = Paths.stackcollapsePy.parent
        ) {
            dstFile.bufferedWriter().use { writer -> writer.write(it.stdOut) }
        }

    private fun createDiff(folded1: File, folded2: File, dstFile: File) =
        execWithChecks(
            Paths.difffoldedPl.absolutePathString(), "-n", folded1.absolutePath,
            folded2.absolutePath
        ) {
            dstFile.bufferedWriter().use { writer -> writer.write(it.stdOut) }
        }

    private fun createFlameGraph(srcFile: File, dstFile: File, negate: Boolean = false) =
        execWithChecks(
            Paths.flamegraphPl.absolutePathString(),
            "--title=${dstFile.nameWithoutExtension}"
                .replace("before-raw", "base") // TODO(327208814)
                .replace("after-raw", "current")
                .replace("before-diff", "base vs current")
                .replace("after-diff", "current vs base"),
            "--fonttype=Roboto, sans-serif",
            "--fontsize=13",
            "--bgcolors=#f5f5f5",
            "--minwidth=2.0",
            "--width=1800", // TODO: autosize
            "--inverted",
            "--hash",
            "--totaldiff",
            "--mindeltapc=0.1",
            "--fillopacity=0.65",
            if (negate) "--negate" else "",
            "--colors=grey",
            "--countname=ns",
            srcFile.absolutePath,
        ) {
            dstFile.bufferedWriter().use { writer -> writer.write(it.stdOut) }
        }

    private fun createIndexHtml(src: TraceDiffResult, dstFile: File) {
        val rawContent = checkNotNull(
            this::class.java.classLoader.getResourceAsStream("templates/index.html")
        ).bufferedReader().use { it.readText() }
        val content = rawContent
            .replace("%before_raw_file%", src.beforeRawGraph.name)
            .replace("%before_diff_file%", src.beforeDiffGraph.name)
            .replace("%after_raw_file%", src.afterRawGraph.name)
            .replace("%after_diff_file%", src.afterDiffGraph.name)
            .replace("%before_raw_name%", "show base")
            .replace("%before_diff_name%", "diff base vs curr")
            .replace("%after_raw_name%", "show curr")
            .replace("%after_diff_name%", "diff curr vs base")
            .replace("%benchmark_result_before_raw%", src.beforeBenchmarkResult)
            .replace("%benchmark_result_after_raw%", src.afterBenchmarkResult)
            .replace("%before_trace_file_name%", src.beforeTraceFileName)
            .replace("%after_trace_file_name%", src.afterTraceFileName)
        dstFile.writeText(content)
    }
}
