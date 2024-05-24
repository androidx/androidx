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

package androidx.benchmark.darwin.gradle

import androidx.benchmark.darwin.gradle.skia.Metrics
import androidx.benchmark.darwin.gradle.xcode.GsonHelpers
import androidx.benchmark.darwin.gradle.xcode.XcResultParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
abstract class DarwinBenchmarkResultsTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xcResultPath: DirectoryProperty

    @get:Input @get:Optional abstract val referenceSha: Property<String>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @get:OutputFile @get:Optional abstract val distFile: RegularFileProperty

    @TaskAction
    fun benchmarkResults() {
        val xcResultFile = xcResultPath.get().asFile
        val parser =
            XcResultParser(xcResultFile) { args ->
                val output = ByteArrayOutputStream()
                execOperations.exec { spec ->
                    spec.commandLine = args
                    spec.standardOutput = output
                }
                output.use {
                    val input = ByteArrayInputStream(output.toByteArray())
                    input.use { input.reader().readText() }
                }
            }
        val (record, summaries) = parser.parseResults()
        val metrics = Metrics.buildMetrics(record, summaries, referenceSha.orNull)
        val output = GsonHelpers.gsonBuilder().setPrettyPrinting().create().toJson(metrics)

        outputFile.get().asFile.writeText(output)

        // Add output to the DIST_DIR when specified
        if (distFile.isPresent) {
            distFile.get().asFile.writeText(output)
        }
    }
}
