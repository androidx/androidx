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
import bench.flame.diff.config.Uris
import bench.flame.diff.interop.Os
import bench.flame.diff.interop.execWithChecks
import bench.flame.diff.interop.exitProcessWithError
import bench.flame.diff.interop.output
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.markdown.Markdown
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name

private const val verifyOnlyFlagName = "--verify-only"

class Init : CliktCommand(help = "Initialize the tool's dependencies.") {
    private val verifyOnly by option(
        names = arrayOf(verifyOnlyFlagName),
        help = "Verify the dependencies without initialising any."
    ).flag(default = false)

    override fun run() {
        // Ensure we are on a Mac or Linux
        if (!Os.isMac && !Os.isLinux) {
            exitProcessWithError(
                "unsupported operating system '${Os.rawName}', only Mac and Linux are supported."
            )
        }

        /** Check out [Paths.flamegraphPl] from Git if not checked out */
        if (!Paths.flamegraphPl.exists()) {
            if (verifyOnly) exitProcessWithError(
                Markdown(
                    "dependency '${Paths.flamegraphDir.name}'" +
                            " not satisfied. Run the **${this.commandName}** command to fix."
                )
            )
            execWithChecks("mkdir", "-p", Paths.flamegraphDir.absolutePathString())
            execWithChecks(
                "git",
                "clone",
                "-n", // no checkout of HEAD is performed, ensuring we only use known revisions
                Uris.flamegraphGitHub,
                Paths.flamegraphDir.absolutePathString()
            )
            execWithChecks(
                "git", "checkout",
                "252e09ac0a56469c1f4a7d3440e5a7d352e1761e", // known good revision
                cwd = Paths.flamegraphDir
            )
        }

        /** Check that we can execute [Paths.flamegraphPl] */
        execWithChecks(
            Paths.flamegraphPl.absolutePathString(),
            "--help",
            checkIsSuccess = {
                it.output.contains("USAGE") && it.output.contains(Paths.flamegraphPl.name)
            }
        )

        /** Check that we can execute Simpleperf's [Paths.stackcollapsePy] */
        if (!Paths.stackcollapsePy.exists()) {
            if (verifyOnly) exitProcessWithError(
                Markdown(
                    "dependency '${Paths.simpleperfDir.name}'" +
                            " not satisfied. Run the **${this.commandName}** command to fix."
                )
            )
            val simplePerfDirPath = Paths.simpleperfDir.absolutePathString()
            execWithChecks("mkdir", "-p", simplePerfDirPath)
            val tmpSourceZip =
                Files.createTempFile(Paths.simpleperfDir, "simpleperf-scripts-snapshot", ".tar.gz")
            val tmpSourceZipPath = tmpSourceZip.absolutePathString()
            execWithChecks("curl", Uris.simpleperfGoogleSource, "-o", tmpSourceZipPath)
            execWithChecks(
                "bash", "-c", "tar -xzf '$tmpSourceZipPath' -O | shasum -a256 | awk '{print $1}'"
            ) {
                val expected = "b9b5b41b270a7cb77be56b6a8c55930e2d565cf3a6e07617e198a2c336e19f91"
                val actual = it.stdOut.trim()
                check(actual == expected) {
                    Files.delete(tmpSourceZip)
                    "Simpleperf checksum mismatch. Expected: $expected. Actual: $actual"
                }
            }
            execWithChecks("tar", "-xzf", tmpSourceZipPath, "-C", simplePerfDirPath)
        }
        execWithChecks(
            Paths.stackcollapsePy.absolutePathString(),
            "--help",
            cwd = Paths.stackcollapsePy.parent
        )
    }

    internal companion object {
        fun verifyDependencies() = Init().main(arrayOf(verifyOnlyFlagName))
    }
}
