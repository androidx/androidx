/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.build.gitclient.ChangeInfoGitClient
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitCommitRange
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.gradle.api.GradleException
import org.json.simple.parser.ParseException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ChangeInfoGitClientTest {
    @Test
    fun findChangedFilesSince_oneChange() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "path": "core/core-remoteviews/src/androidTest/res/layout/remote_views_text.xml",
                          "status": "added"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            listOf("core/core-remoteviews/src/androidTest/res/layout/remote_views_text.xml")
        )
    }

    @Test
    fun findChangedFilesSince_multipleChanges() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "path": "path1",
                          "status": "added"
                        }
                      ]
                    }
                  ]
                },
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "path": "path2",
                          "status": "added"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            listOf("path1", "path2")
        )
    }

    @Test
    fun findChangedFilesSince_otherRepo() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "path": "supportPath",
                          "status": "added"
                        }
                      ]
                    }
                  ]
                },
                {
                  "project": "platform/prebuilts/androidx/external",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "path": "externalPath",
                          "status": "added"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            listOf("supportPath")
        )
    }

    @Test
    fun findChangedFilesSince_emptyChange() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                    }
                  ]
                }
              ]
            }
            """,
            listOf()
        )
    }

    @Test
    fun findChangedFilesSince_noChanges() {
        checkChangedFiles(
            """
            {
            }
            """,
            listOf()
        )
    }

    @Test
    fun findChangedFilesSince_deletedFile() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "oldPath": "Gone",
                          "status": "deleted"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            listOf("Gone")
        )
    }

    @Test
    fun findChangedFilesSince_movedFile() {
        checkChangedFiles(
            """
            {
              "changes": [
                {
                  "project": "platform/frameworks/support",
                  "revisions": [
                    {
                      "fileInfos": [
                        {
                          "oldPath": "PrevPath",
                          "path": "NewPath",
                          "status": "renamed"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            listOf("PrevPath", "NewPath")
        )
    }

    @Test
    fun findChangedFilesSince_malformedJson() {
        var threw = false
        try {
            checkChangedFiles("{", listOf())
        } catch (e: ParseException) {
            threw = true
        }
        assertEquals("Did not detect malformed json", threw, true)
    }

    fun checkChangedFiles(config: String, expectedFiles: List<String>) {
        assertEquals(getChangedFiles(config), expectedFiles)
    }

    fun getChangedFiles(config: String): List<String> {
        return ChangeInfoGitClient(config, "").findChangedFilesSince("", "", false)
    }


    @Test
    fun getGitLog_hasVersion() {
        checkVersion("""
            <project path="prebuilts/internal" revision="prebuiltsVersion1"/>
            <project path="frameworks/support" revision="supportVersion1"/>
            <project path="tools/external/gradle" revision="gradleVersion1"/>
            """,
            "supportVersion1"
        )
    }

    @Test
    fun getGitLog_noVersion() {
        var threw = false
        try {
            checkVersion("""
                """,
                null
            )
        } catch (e: GradleException) {
            threw = true
        }
        assertEquals("Did not detect malformed manifest", threw, true)
    }

    fun checkVersion(config: String, expectedVersion: String?) {
        assertEquals(expectedVersion, getVersion(config))
    }
    fun getVersion(config: String): String? {
        return ChangeInfoGitClient("{}", config)
            .getGitLog(GitCommitRange(n = 1), keepMerges = true, fullProjectDir = File("."))
            .getOrNull(0)?.gitCommit
    }
}
