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
import bench.flame.diff.interop.log
import bench.flame.diff.interop.openFileInOs
import bench.flame.diff.interop.withId
import bench.flame.diff.ui.promptPickFile
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.markdown.Markdown

class Open : CliktCommand(help = "Open a saved diff.") {
    override fun run() {
        val diffsDir = Paths.savedDiffsDir.toFile()
        val diffFiles =
            diffsDir.walkTopDown().filter { it.name == "index.html" }
                .sortedBy { -it.lastModified() }.withId().toList()

        if (diffFiles.isEmpty()) {
            echo(
                Markdown(
                    "No diffs saved. Run the **${Diff().commandName}** command to compare traces."
                )
            )
            return
        }

        val pickedDiff = promptPickFile(diffFiles, diffsDir)
        openFileInOs(pickedDiff)
        log("Opened '${pickedDiff.canonicalPath}' in the browser.", isStdErr = true)
    }
}
