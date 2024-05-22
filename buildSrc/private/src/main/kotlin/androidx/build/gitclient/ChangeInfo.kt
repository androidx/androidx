/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.build.getCheckoutRoot
import androidx.build.parseXml
import com.google.gson.Gson
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * A provider of changed files based on changeinfo files and manifest files created by the build
 * server.
 *
 * For sample changeinfo config files, see: ChangeInfoProvidersTest.kt
 * https://android-build.googleplex.com/builds/pending/P28356101/androidx_incremental/latest/incremental/P28356101-changeInfo
 *
 * For more information, see b/171569941
 */
fun Project.getChangedFilesFromChangeInfoProvider(
    manifestPath: String,
    changeInfoPath: String,
    projectDirRelativeToRoot: String = projectDir.relativeTo(getCheckoutRoot()).toString()
): Provider<List<String>> {
    val manifestTextProvider =
        project.providers
            .fileContents(project.objects.fileProperty().fileValue(File(manifestPath)))
            .asText
    return project.providers
        .fileContents(project.objects.fileProperty().fileValue(File(changeInfoPath)))
        .asText
        .zip(manifestTextProvider) { changeInfoText, manifestText ->
            val fileList = mutableListOf<String>()
            val fileSet = mutableSetOf<String>()
            val gson = Gson()
            val changeInfoEntries = gson.fromJson(changeInfoText, ChangeInfo::class.java)
            val projectName = computeProjectName(projectDirRelativeToRoot, manifestText)
            val changes =
                changeInfoEntries.changes?.filter { it.project == projectName } ?: emptyList()
            for (change in changes) {
                val revisions = change.revisions ?: listOf()
                for (revision in revisions) {
                    val fileInfos = revision.fileInfos ?: listOf()
                    for (fileInfo in fileInfos) {
                        fileInfo.oldPath?.let { path ->
                            if (!fileSet.contains(path)) {
                                fileList.add(path)
                                fileSet.add(path)
                            }
                        }
                        fileInfo.path?.let { path ->
                            if (!fileSet.contains(path)) {
                                fileList.add(path)
                                fileSet.add(path)
                            }
                        }
                    }
                }
            }
            return@zip fileList
        }
}

// Data classes uses to parse CHANGE_INFO json files
internal data class ChangeInfo(val changes: List<ChangeEntry>?)

internal data class ChangeEntry(val project: String, val revisions: List<Revisions>?)

internal data class Revisions(val fileInfos: List<FileInfo>?)

internal data class FileInfo(val path: String?, val oldPath: String?, val status: String)

/**
 * A provider of HEAD SHA based on manifest file created by the build server.
 *
 * For sample manifest files, see: ChangeInfoProvidersTest.kt
 *
 * For more information, see b/171569941
 */
fun Project.getHeadShaFromManifestProvider(
    manifestPath: String,
    projectDirRelativeToRoot: String = projectDir.relativeTo(getCheckoutRoot()).toString()
): Provider<String> {
    val contentsProvider =
        project.providers.fileContents(project.objects.fileProperty().fileValue(File(manifestPath)))
    return contentsProvider.asText.map { manifestContent ->
        val projectName = computeProjectName(projectDirRelativeToRoot, manifestContent)
        val revisionRegex = Regex("revision=\"([^\"]*)\"")
        for (line in manifestContent.split("\n")) {
            if (line.contains("name=\"${projectName}\"")) {
                val result = revisionRegex.find(line)?.groupValues?.get(1)
                if (result != null) {
                    return@map result
                }
            }
        }
        throw GradleException(
            "Could not identify version of project '$projectName' from config text" +
                "'$manifestContent'"
        )
    }
}

private fun computeProjectName(projectPath: String, config: String): String {
    fun pathContains(ancestor: String, child: String): Boolean {
        return "$child/".startsWith("$ancestor/")
    }
    val document = parseXml(config, mapOf())
    val projectIterator = document.rootElement.elementIterator()
    while (projectIterator.hasNext()) {
        val project = projectIterator.next()
        val repositoryPath = project.attributeValue("path")
        if (repositoryPath != null) {
            if (pathContains(repositoryPath, projectPath)) {
                val name = project.attributeValue("name")
                check(name != null) { "Could not get name for project $project" }
                return name
            }
        }
    }
    throw GradleException("Could not find project with path '$projectPath' in config")
}
