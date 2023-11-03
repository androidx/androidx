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

import androidx.build.gitclient.getChangedFilesFromChangeInfoProvider
import androidx.build.gitclient.getHeadShaFromManifestProvider
import com.google.gson.JsonSyntaxException
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ChangeInfoProvidersTest {
    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

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

    private fun checkChangedFiles(
        changeInfoContent: String,
        expectedChangedFiles: List<String>
    ) {
        val manifestFile = folder.newFile()
        manifestFile.writeText(basicManifest)
        val changeInfoFile = folder.newFile()
        changeInfoFile.writeText(changeInfoContent)
        val project = ProjectBuilder.builder().build()
        val changedFilesProvider = project.getChangedFilesFromChangeInfoProvider(
            manifestFile.absolutePath,
            changeInfoFile.absolutePath,
            frameworksSupportPath
        )
        val changedFiles = changedFilesProvider.get()
        assertEquals(expectedChangedFiles, changedFiles)
    }

    @Test
    fun headShaFromManifest() {
        val manifestFile = folder.newFile()
        manifestFile.writeText(basicManifest)
        val project = ProjectBuilder.builder().build()
        val headShaProvider = project.getHeadShaFromManifestProvider(
            manifestFile.absolutePath,
            frameworksSupportPath
        )
        assertEquals(frameworksSupportSha, headShaProvider.get())
    }

    @Test
    fun missingProjectHeadShaFromManifest() {
        val manifestFile = folder.newFile()
        manifestFile.writeText(basicManifest)
        val project = ProjectBuilder.builder().build()
        val headShaProvider = project.getHeadShaFromManifestProvider(
            manifestFile.absolutePath,
            "missing/project/path"
        )
        assertThrows(GradleException::class.java) {
            headShaProvider.get()
        }
    }
}

private const val frameworksSupportPath = "frameworks/support"
private const val frameworksSupportSha = "bbcf23f3ee42fc9e59e0cf5fbca71f526f760dba"
private const val basicManifest = """<?xml version='1.0' encoding='UTF-8'?>
<manifest>
  <remote name="aosp" fetch="https://android.googlesource.com/" review="https://android.googlesource.com/" />
  <default revision="androidx-main" remote="aosp" />
  <superproject name="platform/superproject" remote="aosp" />
  <project path="external/icing" name="platform/external/icing" clone-depth="1" revision="ea81bd0613730609fcf3c6ffa7d2e52bcc10a9ac" />
  <project path="$frameworksSupportPath" name="platform/frameworks/support" revision="$frameworksSupportSha" />
  <repo-hooks in-project="platform/tools/repohooks" enabled-list="pre-upload" />
</manifest>
"""
