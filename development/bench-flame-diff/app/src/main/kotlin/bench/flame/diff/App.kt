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
package bench.flame.diff

import bench.flame.diff.command.Diff
import bench.flame.diff.command.Init
import bench.flame.diff.command.List
import bench.flame.diff.command.Open
import bench.flame.diff.command.Save
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.withPadding

fun main(args: Array<out String>) {
    BenchFlameDiff().subcommands(Init(), Save(), List(), Diff(), Open()).main(args)
}

class BenchFlameDiff : CliktCommand(
    help = "Generate differential flame graphs from microbenchmark CPU traces.",
    epilog = """
        Dependencies
        FlameGraph - https://github.com/brendangregg/FlameGraph for generating graphs.
        Simpleperf - Android NDK's Simpleperf for converting traces into FlameGraph input format.
        Clikt      - com.github.ajalt.clikt:clikt for the CLI interface.
        NuProcess  - com.zaxxer:nuprocess for running external processes.
    """.trimIndent()
) {
    init {
        context {
            helpFormatter = {
                object : MordantHelpFormatter(it, showRequiredTag = true) {
                    override fun renderEpilog(epilog: String): Widget = verticalLayout {
                        val lines = epilog.lines()
                        val header = lines.first() + ":"
                        val dependencies = lines.drop(1).map {
                            it.split(" - ").also { check(it.size == 2) }
                                .let { (name, desc) -> name to desc }
                        }

                        cell(Text(theme.warning(header)))
                        for ((name, desc) in dependencies) cell(
                            horizontalLayout {
                                cell(Text(theme.info(name)).withPadding { left = 2; right = 1 })
                                cell(desc)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun run() = Unit
}
