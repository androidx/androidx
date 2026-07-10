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
package androidx.abbenchmarking.common

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

internal data class BenchmarkArguments(
    val revA: String,
    val revB: String,
    val module: String,
    val benchmarkTest: String,
    val runCount: Int,
    val iterationCount: Int?,
    val serial: String?,
    val outputDirectoryPath: String?,
)

/**
 * Extension function on [ArgParser] to define and parse benchmark arguments.
 *
 * @return A [BenchmarkArguments] instance populated from the command line.
 */
internal fun ArgParser.parseBenchmarkArguments(args: Array<String>): BenchmarkArguments {
    val revA by
        argument(
            ArgType.String,
            fullName = "rev_a",
            description = "First branch / commit (e.g., 'main')",
        )

    val revB by
        argument(
            ArgType.String,
            fullName = "rev_b",
            description = "Second branch / commit (e.g., a feature branch)",
        )

    val module by
        argument(
            ArgType.String,
            fullName = "module",
            description =
                "Module containing the benchmark test class (e.g., 'compose:ui:ui-benchmark')",
        )

    val benchmarkTest by
        argument(
            ArgType.String,
            fullName = "benchmark_test",
            description =
                "Fully qualified name of the test class and optionally the method.\n" +
                    "Can also include parameters for parameterized tests.\n" +
                    "Example: 'androidx.compose.ui.benchmark.ModifiersBenchmark#full[clickable_1x]",
        )

    val runCount by
        option(
                ArgType.Int,
                fullName = "run_count",
                description = "Number of times to run the test on each git revision.",
            )
            .default(1)

    val iterationCount: Int? by
        option(
            ArgType.Int,
            fullName = "iteration_count",
            description =
                "Number of benchmark runs to perform on each test run. Total number of measurements = iteration_count * run_count ",
        )

    val serial by
        option(
            ArgType.String,
            fullName = "serial",
            description =
                "The SERIAL of the device to run the tests on. This is an optional parameter if only one device is connected.",
        )

    val outputDirectoryPath by
        option(
            ArgType.String,
            fullName = "output_path",
            description =
                "The path where output files will be stored, such as benchmark measurements, run metadata, and result plots.",
        )

    this.parse(args)

    return BenchmarkArguments(
        revA = revA,
        revB = revB,
        module = module,
        benchmarkTest = benchmarkTest,
        runCount = runCount,
        iterationCount = iterationCount,
        serial = serial,
        outputDirectoryPath = outputDirectoryPath,
    )
}
