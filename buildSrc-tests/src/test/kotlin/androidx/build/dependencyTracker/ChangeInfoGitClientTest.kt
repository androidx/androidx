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

import androidx.build.gitclient.ChangeInfoGitClient
import androidx.build.gitclient.GitCommitRange
import com.google.gson.JsonSyntaxException
import java.io.File
import junit.framework.TestCase.assertEquals
import org.gradle.api.GradleException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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

    @Test(expected = JsonSyntaxException::class)
    fun findChangedFilesSince_malformedJson() {
        checkChangedFiles("{", listOf())
    }

    fun checkChangedFiles(config: String, expectedFiles: List<String>) {
        assertEquals(getChangedFiles(config), expectedFiles)
    }

    fun getChangedFiles(config: String): List<String> {
        val client = ChangeInfoGitClient(
            config,
            """
            <manifest>
                <project path="frameworks/support" name="platform/frameworks/support"/>
            </manifest>
            """,
            "frameworks/support")
        return client.findChangedFilesSince("", "", false)
    }

    @Test
    fun getGitLog_hasVersion() {
        checkVersion("""
            <manifest>
                <project path="prebuilts/internal" name="platform/prebuilts/internal" revision="prebuiltsVersion1"/>
                <project path="frameworks/support" name="platform/frameworks/support" revision="supportVersion1"/>
                <project path="tools/external/gradle" name="platform/tools/external/gradle" revision="gradleVersion1"/>
            </manifest>
            """,
            "supportVersion1"
        )
    }

    @Test
    fun getGitLog_noVersion() {
        var threw = false
        try {
            checkVersion("""
                <manifest/>
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
        return ChangeInfoGitClient("{}", config, "frameworks/support")
            .getGitLog(GitCommitRange(n = 1), keepMerges = true, projectDir = File("."))
            .getOrNull(0)?.sha
    }
}
