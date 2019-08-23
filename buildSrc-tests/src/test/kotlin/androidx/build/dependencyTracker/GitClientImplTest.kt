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
import androidx.build.gitclient.CommitType
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitClientImpl
import androidx.build.gitclient.GitClientImpl.Companion.CHANGED_FILES_CMD_PREFIX
import androidx.build.gitclient.GitClientImpl.Companion.PREV_MERGE_CMD
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
    private val client = GitClientImpl(
        workingDir = File(System.getProperty("user.dir")),
        logger = logger,
        commandRunner = commandRunner
    )

    @Test
    fun findMerge() {
        commandRunner.addReply(
                PREV_MERGE_CMD,
                "abcdefghij (m/androidx-md, aosp/androidx-md) Merge blah blah into and"
        )
        assertEquals(
                "abcdefghij",
                client.findPreviousMergeCL())
    }

    @Test
    fun findMerge_fail() {
        assertNull(client.findPreviousMergeCL())
    }

    @Test
    fun findChangesSince() {
        var changes = listOf(
                convertToFilePath("a", "b", "c.java"),
                convertToFilePath("d", "e", "f.java"))
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX mySha",
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
    fun parseAPICommitWithDefaultDelimiters() {
        val commitWithApiChangeString: String =
            """
                _CommitStart
                _CommitSHA:mySha
                _Author:anemail@google.com
                _Date:Tue Aug 6 15:05:55 2019 -0700
                _Subject:Added a new API!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit

                Bug: 123456
                Bug: b/1234567
                Fixes: 123123
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                projectdir/a.java
                projectdir/b.java
                projectdir/androidTest/c.java
                projectdir/api/some_api_file.txt
                projectdir/api/current.txt
            """
        val commitWithApiChange: Commit = Commit(commitWithApiChangeString, "/projectdir/")
        assertEquals("mySha", commitWithApiChange.sha)
        assertEquals("anemail@google.com", commitWithApiChange.authorEmail)
        assertEquals("myChangeId", commitWithApiChange.changeId)
        assertEquals("Added a new API!", commitWithApiChange.summary)
        assertEquals(CommitType.API_CHANGE, commitWithApiChange.type)
        assertEquals(mutableListOf(123456, 1234567, 123123), commitWithApiChange.bugs)
        assertEquals(
            mutableListOf(
                "projectdir/a.java",
                "projectdir/b.java",
                "projectdir/androidTest/c.java",
                "projectdir/api/some_api_file.txt",
                "projectdir/api/current.txt"
            ),
            commitWithApiChange.files
        )
    }

    @Test
    fun parseBugFixCommitWithCustomDelimiters() {
        val commitSHADelimiter: String = "_MyCommitSHA:"
        val subjectDelimiter: String = "_MySubject:"
        val authorEmailDelimiter: String = "_MyAuthor:"
        val projectDir: String = "group/artifact"
        val commitWithABugFixString: String =
            """
                _CommitStart
                ${commitSHADelimiter}mySha
                ${authorEmailDelimiter}anemail@google.com
                _Date:Tue Aug 6 15:05:55 2019 -0700
                ${subjectDelimiter}Fixed a bug!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit that changes a kotlin file

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.kt
                $projectDir/androidTest/c.java
            """
        val commitWithABugFix: Commit = Commit(
            commitWithABugFixString,
            projectDir,
            commitSHADelimiter = commitSHADelimiter,
            subjectDelimiter = subjectDelimiter,
            authorEmailDelimiter = authorEmailDelimiter
        )
        assertEquals("mySha", commitWithABugFix.sha)
        assertEquals("anemail@google.com", commitWithABugFix.authorEmail)
        assertEquals("myChangeId", commitWithABugFix.changeId)
        assertEquals("Fixed a bug!", commitWithABugFix.summary)
        assertEquals(CommitType.BUG_FIX, commitWithABugFix.type)
        assertEquals(mutableListOf(111111, 222222), commitWithABugFix.bugs)
        assertEquals(
            mutableListOf(
                "$projectDir/a.java",
                "$projectDir/b.kt",
                "$projectDir/androidTest/c.java"
            ),
            commitWithABugFix.files
        )
    }

    @Test
    fun parseExternalContributorCommitWithCustomDelimiters() {
        val commitSHADelimiter: String = "_MyCommitSHA:"
        val subjectDelimiter: String = "_MySubject:"
        val authorEmailDelimiter: String = "_MyAuthor:"
        val projectDir: String = "group/artifact"
        val commitFromExternalContributorString: String =
            """
                _CommitStart
                ${commitSHADelimiter}mySha
                ${authorEmailDelimiter}externalcontributor@gmail.com
                _Date:Thurs Aug 8 15:05:55 2019 -0700
                ${subjectDelimiter}New compat API!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit that changes a java file

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.java
                $projectDir/androidTest/c.java
                $projectDir/api/current.txt
                $projectDir/api/1.2.0-alpha04.txt
            """
        val commitFromExternalContributor: Commit = Commit(
            commitFromExternalContributorString,
            projectDir,
            commitSHADelimiter = commitSHADelimiter,
            subjectDelimiter = subjectDelimiter,
            authorEmailDelimiter = authorEmailDelimiter
        )
        assertEquals("mySha", commitFromExternalContributor.sha)
        assertEquals("externalcontributor@gmail.com", commitFromExternalContributor.authorEmail)
        assertEquals("myChangeId", commitFromExternalContributor.changeId)
        assertEquals("New compat API!", commitFromExternalContributor.summary)
        assertEquals(CommitType.EXTERNAL_CONTRIBUTION, commitFromExternalContributor.type)
        assertEquals(mutableListOf(111111, 222222), commitFromExternalContributor.bugs)
        assertEquals(
            mutableListOf(
                "$projectDir/a.java",
                "$projectDir/b.java",
                "$projectDir/androidTest/c.java",
                "$projectDir/api/current.txt",
                "$projectDir/api/1.2.0-alpha04.txt"
            ),
            commitFromExternalContributor.files
        )
    }

    @Test
    fun parseGitLog() {
        /*
         * This is the default set-up boilerplate from `GitClientImpl.getGitLog` to set up the
         * default git log command (val gitLogCmd)
         */
        val commitStartDelimiter: String = "_CommitStart"
        val commitSHADelimiter: String = "_CommitSHA:"
        val subjectDelimiter: String = "_Subject:"
        val authorEmailDelimiter: String = "_Author:"
        val dateDelimiter: String = "_Date:"
        val bodyDelimiter: String = "_Body:"
        val projectDir: String = "group/artifact"
        val gitLogOptions: String =
            "--pretty=format:$commitStartDelimiter%n" +
                    "$commitSHADelimiter%H%n" +
                    "$authorEmailDelimiter%ae%n" +
                    "$dateDelimiter%ad%n" +
                    "$subjectDelimiter%s%n" +
                    "$bodyDelimiter%b" +
                    " --no-merges"
        val gitLogCmd: String = "${GitClientImpl.GIT_LOG_CMD_PREFIX} " +
                "$gitLogOptions sha..topSha $projectDir"

        // Check with default delimiters
        val gitLogString: String =
            """
                _CommitStart
                _CommitSHA:topSha
                _Author:anemail@google.com
                _Date:Tue Aug 6 15:05:55 2019 -0700
                _Subject:Added a new API!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit

                Bug: 123456
                Bug: b/1234567
                Fixes: 123123
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.java
                $projectDir/androidTest/c.java
                $projectDir/api/some_api_file.txt
                $projectDir/api/current.txt

                _CommitStart
                _CommitSHA:midSha
                _Author:anemail@google.com
                _Date:Tue Aug 6 15:05:55 2019 -0700
                _Subject:Fixed a bug!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.java
                $projectDir/androidTest/c.java

                _CommitStart
                _CommitSHA:sha
                _Author:externalcontributor@gmail.com
                _Date:Thurs Aug 8 15:05:55 2019 -0700
                _Subject:New compat API!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.java
                $projectDir/androidTest/c.java
                $projectDir/api/current.txt
                $projectDir/api/1.2.0-alpha04.txt
            """

        commandRunner.addReply(
            gitLogCmd,
            gitLogString
        )

        val commitWithAPIChange: Commit = Commit("", projectDir)
        commitWithAPIChange.sha = "topSha"
        commitWithAPIChange.authorEmail = "anemail@google.com"
        commitWithAPIChange.changeId = "myChangeId"
        commitWithAPIChange.summary = "Added a new API!"
        commitWithAPIChange.type = CommitType.API_CHANGE
        commitWithAPIChange.bugs = mutableListOf(123456, 1234567, 123123)
        commitWithAPIChange.files = mutableListOf(
            "$projectDir/a.java",
            "$projectDir/b.java",
            "$projectDir/androidTest/c.java",
            "$projectDir/api/some_api_file.txt",
            "$projectDir/api/current.txt"
        )

        val commitWithBugFix: Commit = Commit("", projectDir)
        commitWithBugFix.sha = "midSha"
        commitWithBugFix.authorEmail = "anemail@google.com"
        commitWithBugFix.changeId = "myChangeId"
        commitWithBugFix.summary = "Fixed a bug!"
        commitWithBugFix.type = CommitType.BUG_FIX
        commitWithBugFix.bugs = mutableListOf(111111, 222222)
        commitWithBugFix.files = mutableListOf(
            "$projectDir/a.java",
            "$projectDir/b.java",
            "$projectDir/androidTest/c.java"
        )

        val commitFromExternalContributor: Commit = Commit("", projectDir)
        commitFromExternalContributor.sha = "sha"
        commitFromExternalContributor.authorEmail = "externalcontributor@gmail.com"
        commitFromExternalContributor.changeId = "myChangeId"
        commitFromExternalContributor.summary = "New compat API!"
        commitFromExternalContributor.type = CommitType.EXTERNAL_CONTRIBUTION
        commitFromExternalContributor.bugs = mutableListOf(111111, 222222)
        commitFromExternalContributor.files = mutableListOf(
            "$projectDir/a.java",
            "$projectDir/b.java",
            "$projectDir/androidTest/c.java",
            "$projectDir/api/current.txt",
            "$projectDir/api/1.2.0-alpha04.txt"
        )

        val gitLogList: List<Commit> = client.getGitLog(
            GitCommitRange(
                top = "topSha",
                sha = "sha"
            ),
            keepMerges = false,
            fullProjectDir = File(projectDir)
        )
        gitLogList.forEach { commit ->
            when (commit.sha) {
                "topSha" -> assertCommitsAreEqual(commitWithAPIChange, commit)
                "midSha" -> assertCommitsAreEqual(commitWithBugFix, commit)
                "sha" -> assertCommitsAreEqual(commitFromExternalContributor, commit)
                else -> throw RuntimeException("Incorrectly parsed commit: $commit")
            }
        }
    }

    fun assertCommitsAreEqual(commitA: Commit, commitB: Commit) {
        assertEquals(commitA.summary, commitB.summary)
        assertEquals(commitA.files, commitB.files)
        assertEquals(commitA.sha, commitB.sha)
        assertEquals(commitA.changeId, commitB.changeId)
        assertEquals(commitA.authorEmail, commitB.authorEmail)
        assertEquals(commitA.type, commitB.type)
        assertEquals(commitA.projectDir, commitB.projectDir)
        assertEquals(commitA.summary, commitB.summary)
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