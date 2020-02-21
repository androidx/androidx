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

package androidx.build.releasenotes

import androidx.build.dependencyTracker.AttachLogsTestRule
import androidx.build.dependencyTracker.ToStringLogger
import androidx.build.gitclient.Commit
import androidx.build.gitclient.CommitType
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitClientImpl
import androidx.build.gitclient.GitCommitRange
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(JUnit4::class)
class ReleaseNotesImplTest {
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
                "$gitLogOptions sha..topSha -- ./$projectDir"

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
                Relnote: Added an awesome new API!

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
                _CommitSHA:midSha2
                _Author:anemail@google.com
                _Date:Tue Aug 6 15:05:55 2019 -0700
                _Subject:Fixed a bug!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit
                Relnote: "Fixed a critical bug"

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/b.java
                $projectDir/androidTest/c.java

                _CommitStart
                _CommitSHA:midSha1
                _Author:anemail@google.com
                _Date:Tue Aug 6 16:05:55 2019 -0700
                _Subject:Fixed a small test failure on API level 15
                _Body:Just a small test fix

                Here is an explanation of my commit

                Bug: 111111, 222222
                Test: ./gradlew buildOnServer

                Change-Id: myChangeId

                $projectDir/a.java
                $projectDir/androidTest/c.java

                _CommitStart
                _CommitSHA:sha
                _Author:externalcontributor@gmail.com
                _Date:Thurs Aug 8 15:05:55 2019 -0700
                _Subject:New compat API!
                _Body:Also fixed some other bugs

                Here is an explanation of my commit
                Relnote: Added a new compat API

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
        commitWithAPIChange.releaseNote = "Added an awesome new API!"

        val commitWithBugFix: Commit = Commit("", projectDir)
        commitWithBugFix.sha = "midSha2"
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
        commitWithBugFix.releaseNote = "Fixed a critical bug"

        val commitWithTestFix: Commit = Commit("", projectDir)
        commitWithTestFix.sha = "midSha1"
        commitWithTestFix.authorEmail = "anemail@google.com"
        commitWithTestFix.changeId = "myChangeId"
        commitWithTestFix.summary = "Fixed a small test failure on API level 15"
        commitWithTestFix.type = CommitType.BUG_FIX
        commitWithTestFix.bugs = mutableListOf(111111, 222222)
        commitWithTestFix.files = mutableListOf(
            "$projectDir/a.java",
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
        commitFromExternalContributor.releaseNote = "Added a new compat API"

        val gitLogList: List<Commit> = client.getGitLog(
            GitCommitRange(
                fromExclusive = "sha",
                untilInclusive = "topSha"
            ),
            keepMerges = false,
            fullProjectDir = File(workingDir, "$projectDir")
        )
        gitLogList.forEach { commit ->
            when (commit.sha) {
                "topSha" -> assertCommitsAreEqual(commitWithAPIChange, commit)
                "midSha2" -> assertCommitsAreEqual(commitWithBugFix, commit)
                "midSha1" -> assertCommitsAreEqual(commitWithTestFix, commit)
                "sha" -> assertCommitsAreEqual(commitFromExternalContributor, commit)
                else -> throw RuntimeException("Incorrectly parsed commit: $commit")
            }
        }

        val expectedReleaseNotesResult = "### groupId Version 1.0.0-alpha01 {:#1.0.0-alpha01}\n" +
                "October 23, 2019\n\n" +
                "`groupId:groupId:1.0.0-alpha01` is released.  The commits included in this " +
                    "version can be found ([here](https://android.googlesource.com/platform" +
                    "/frameworks/support/+log/sha..topSha/group/artifact)).\n\n" +
                "{# **New Features** #}\n\n\n" +
                "**API Changes**\n\n" +
                "- Added an awesome new API! " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/123456](https://issuetracker.google.com/issues/123456)) " +
                    "([b/1234567](https://issuetracker.google.com/issues/1234567)) " +
                    "([b/123123](https://issuetracker.google.com/issues/123123))\n\n" +
                "**Bug Fixes**\n\n" +
                "- Fixed a critical bug " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                    "([b/222222](https://issuetracker.google.com/issues/222222))\n\n" +
                "**External Contribution**\n\n" +
                    "- Added a new compat API " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                    "([b/222222](https://issuetracker.google.com/issues/222222))\n"
        val releaseNotes = LibraryReleaseNotes(
            "groupId",
            mutableListOf("groupId"),
            "1.0.0-alpha01",
            LocalDate.parse("2019-10-23", DateTimeFormatter.ISO_DATE),
            "sha",
            "topSha",
            projectDir,
            gitLogList,
            true,
            false
        )
        assertEquals(
            expectedReleaseNotesResult,
            releaseNotes.toString()
        )

        val expectedReleaseNotesWithAllCommits = "### groupId Version 1.0.0-alpha01 " +
                "{:#1.0.0-alpha01}\n" +
                "October 23, 2019\n\n" +
                "`groupId:groupId:1.0.0-alpha01` is released.  The commits included in this " +
                    "version can be found " +
                    "([here](https://android.googlesource.com/platform/frameworks" +
                    "/support/+log/sha..topSha/group/artifact)).\n\n" +
                "{# **New Features** #}\n\n\n" +
                "**API Changes**\n\n" +
                "- Added an awesome new API! " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/123456](https://issuetracker.google.com/issues/123456)) " +
                    "([b/1234567](https://issuetracker.google.com/issues/1234567)) " +
                    "([b/123123](https://issuetracker.google.com/issues/123123))\n\n" +
                "**Bug Fixes**\n\n" +
                "- Fixed a critical bug " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                    "([b/222222](https://issuetracker.google.com/issues/222222))\n" +
                "- Fixed a small test failure on API level 15 " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                    "([b/222222](https://issuetracker.google.com/issues/222222))\n\n" +
                "**External Contribution**\n\n" +
                    "- Added a new compat API " +
                    "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                    "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                    "([b/222222](https://issuetracker.google.com/issues/222222))\n"
        val releaseNotesWithAllCommits = LibraryReleaseNotes(
            "groupId",
            mutableListOf("groupId"),
            "1.0.0-alpha01",
            LocalDate.parse("2019-10-23", DateTimeFormatter.ISO_DATE),
            "sha",
            "topSha",
            projectDir,
            gitLogList,
            true,
            true
        )
        assertEquals(
            expectedReleaseNotesWithAllCommits,
            releaseNotesWithAllCommits.toString()
        )

        val expectedReleaseNotesWithArtifactIdHeader = "### " +
                "artifactId1, artifactId2, artifactId3 Version 1.0.0-alpha01 {:#1.0.0-alpha01}\n" +
                "October 23, 2019\n\n" +
                "`groupId:artifactId1:1.0.0-alpha01`, `groupId:artifactId2:1.0.0-alpha01`, and " +
                "`groupId:artifactId3:1.0.0-alpha01` are released. The commits included in this " +
                "version can be found ([here](https://android.googlesource.com/platform" +
                "/frameworks/support/+log/sha..topSha/group/artifact)).\n\n" +
                "{# **New Features** #}\n\n\n" +
                "**API Changes**\n\n" +
                "- Added an awesome new API! " +
                "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                "([b/123456](https://issuetracker.google.com/issues/123456)) " +
                "([b/1234567](https://issuetracker.google.com/issues/1234567)) " +
                "([b/123123](https://issuetracker.google.com/issues/123123))\n\n" +
                "**Bug Fixes**\n\n" +
                "- Fixed a critical bug " +
                "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                "([b/222222](https://issuetracker.google.com/issues/222222))\n\n" +
                "**External Contribution**\n\n" +
                "- Added a new compat API " +
                "([myChan](https://android-review.googlesource.com/#/q/myChangeId)) " +
                "([b/111111](https://issuetracker.google.com/issues/111111)) " +
                "([b/222222](https://issuetracker.google.com/issues/222222))\n"
        val releaseNotesWithArtifactIdHeader = LibraryReleaseNotes(
            "groupId",
            mutableListOf("artifactId1", "artifactId2", "artifactId3"),
            "1.0.0-alpha01",
            LocalDate.parse("2019-10-23", DateTimeFormatter.ISO_DATE),
            "sha",
            "topSha",
            projectDir,
            gitLogList,
            false,
            false
        )
        assertEquals(
            expectedReleaseNotesWithArtifactIdHeader,
            releaseNotesWithArtifactIdHeader.toString()
        )
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
        assertEquals(commitA.releaseNote, commitB.releaseNote)
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
