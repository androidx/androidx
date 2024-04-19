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
import bench.flame.diff.interop.FileWithId
import bench.flame.diff.interop.withId
import bench.flame.diff.ui.printFileTable
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.markdown.Markdown
import java.io.File
import kotlin.collections.List

class List : CliktCommand(help = "List all saved trace files.") {
    override fun run() {
        val savedTracesDir = Paths.savedTracesDir.toFile()
        val traces = savedTraces(savedTracesDir)

        if (traces.isEmpty()) {
            echo(
                Markdown(
                    "No trace files saved. Run the **${Save().commandName}** command" +
                            " to save traces for future comparison."
                )
            )
            return
        }

        printFileTable(traces, savedTracesDir)
    }

    companion object {
        /** Returns a list of saved traces sorted by 'most recently modified first' */
        internal fun savedTraces(savedTracesDir: File): List<FileWithId> =
            savedTracesDir
                .listFiles()
                .let { it ?: return emptyList() }
                .filter { !it.name.lowercase().endsWith(".json") }
                .sortedBy { -it.lastModified() }
                .asSequence()
                .withId()
                .toList()
    }
}
