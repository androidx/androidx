/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.dependencyTracker

import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitClientImpl
import androidx.build.gitclient.GitClientImpl.Companion.CHANGED_FILES_CMD_PREFIX
import androidx.build.gitclient.GitClientImpl.Companion.PREVIOUS_SUBMITTED_CMD
import androidx.build.gitclient.GitCommitRange
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class GitClientImplTest {
    @Rule
    @JvmField
    val attachLogsRule = AttachLogsTestRule()
    private val logger = attachLogsRule.logger
    private val commandRunner = MockCommandRunner(logger)
    /** The [GitClientImpl.workingDir] uses `System.getProperty("user.dir")` because the working
     * directory passed to the [GitClientImpl] constructor needs to contain the a .git
     * directory somewhere in the parent directory tree.  @see [GitClientImpl]
     */
    private val workingDir = File(System.getProperty("user.dir")).parentFile
    private val client = GitClientImpl(
        workingDir = workingDir,
        logger = logger,
        commandRunner = commandRunner
    )

    @Test
    fun findMerge() {
        commandRunner.addReply(
                PREVIOUS_SUBMITTED_CMD,
                "abcdefghij (m/androidx-md, aosp/androidx-md) Merge blah blah into and"
        )
        assertEquals(
                "abcdefghij",
                client.findPreviousSubmittedChange())
    }

    @Test
    fun findMerge_fail() {
        assertNull(client.findPreviousSubmittedChange())
    }

    @Test
    fun findChangesSince() {
        var changes = listOf(
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX HEAD..mySha",
                changes.joinToString(System.lineSeparator())
        )
        assertEquals(
                changes,
                client.findChangedFilesSince(sha = "mySha", includeUncommitted = true))
    }

    @Test
    fun findChangesSince_empty() {
        assertEquals(
                emptyList<String>(),
                client.findChangedFilesSince("foo"))
    }

    @Test
    fun findChangesSince_twoCls() {
        var changes = listOf(
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX otherSha mySha",
                changes.joinToString(System.lineSeparator())
        )
        assertEquals(
                changes,
                client.findChangedFilesSince(
                        sha = "mySha",
                        top = "otherSha",
                        includeUncommitted = false))
    }

    @Test
    fun checkLatestCommitExists() {
        /* Do not use the MockCommandRunner because it's a better test to check the validity of
         * the git command against the actual git in the repo
         */
        val commitList: List<Commit> = GitClientImpl(workingDir, logger)
            .getGitLog(
                GitCommitRange(
                    fromExclusive = "",
                    untilInclusive = "HEAD",
                    n = 1
                ),
                keepMerges = true,
                fullProjectDir = workingDir
        )
        assertEquals(1, commitList.size)
    }

    // For both Linux/Windows
    fun convertToFilePath(vararg list: String): String {
        return list.toList().joinToString(File.separator)
    }

    private class MockCommandRunner(val logger: ToStringLogger) : GitClient.CommandRunner {
        private val replies = mutableMapOf<String, List<String>>()

        fun addReply(command: String, response: String) {
            logger.info("add reply. cmd: $command response: $response")
            replies[command] = response.split(System.lineSeparator())
        }

        override fun execute(command: String): String {
            return replies.getOrDefault(command, emptyList())
                .joinToString(System.lineSeparator()).also {
                    logger.info("cmd: $command response: $it")
            }
        }

        override fun executeAndParse(command: String): List<String> {
            return replies.getOrDefault(command, emptyList()).also {
                logger.info("cmd: $command response: $it")
            }
        }
    }
}
