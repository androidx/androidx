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

package androidx.build.gitclient

import java.io.File
import java.util.concurrent.TimeUnit
import org.gradle.api.logging.Logger

/**
 * A simple git client that uses system process commands to communicate with the git setup in the
 * given working directory.
 */
class GitRunnerGitClient(
    /** The root location for git */
    private val workingDir: File,
    private val logger: Logger?,
    private val commandRunner: GitClient.CommandRunner =
        RealCommandRunner(workingDir = workingDir, logger = logger)
) : GitClient {

    /** Finds file paths changed in a commit since the given sha */
    override fun findChangedFilesSince(sha: String): List<String> {
        return commandRunner.executeAndParse("$CHANGED_FILES_CMD_PREFIX HEAD $sha")
    }

    /** checks the history to find the first merge CL. */
    override fun findPreviousSubmittedChange(): String? {
        return commandRunner
            .executeAndParse(PREVIOUS_SUBMITTED_CMD)
            .firstOrNull()
            ?.split(" ")
            ?.firstOrNull()
    }

    override fun getHeadSha(): String {
        val gitLogCmd = "git log --name-only --pretty=format:%H HEAD -n 1 -- ./"
        val gitLogString: String = commandRunner.execute(gitLogCmd)
        if (gitLogString.isEmpty()) {
            logger?.warn(
                "No git commits found! Ran this command: '$gitLogCmd ' and received no output"
            )
        }
        return gitLogString
    }

    private class RealCommandRunner(private val workingDir: File, private val logger: Logger?) :
        GitClient.CommandRunner {
        override fun execute(command: String): String {
            val parts = command.split("\\s".toRegex())
            logger?.info("running command $command in $workingDir")
            val proc =
                ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            // Read output, waiting for process to finish, as needed
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val message = stdout + stderr
            // wait potentially a little bit longer in case Git was waiting for us to
            // read its response before it exited
            proc.waitFor(10, TimeUnit.SECONDS)
            logger?.info("Response: $message")
            check(proc.exitValue() == 0) {
                "Nonzero exit value running git command. Response: $message"
            }
            return stdout
        }

        override fun executeAndParse(command: String): List<String> {
            return execute(command).split(System.lineSeparator()).filterNot { it.isEmpty() }
        }
    }

    companion object {
        const val PREVIOUS_SUBMITTED_CMD = "git log -1 --merges --oneline"
        const val CHANGED_FILES_CMD_PREFIX = "git diff --name-only"
    }
}

/** Finds the git directory containing the given File by checking parent directories */
internal fun findGitDirInParentFilepath(filepath: File): File? {
    var curDirectory: File = filepath
    while (curDirectory.path != "/") {
        if (File("$curDirectory/.git").exists()) {
            return curDirectory
        }
        curDirectory = curDirectory.parentFile
    }
    return null
}
