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

package androidx.build.metalava

import androidx.build.Version
import androidx.build.checkapi.ApiLocation
import androidx.build.registerAsComponentForPublishing
import java.io.File
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named

/**
 * Returns the API files that should be used to generate the API levels metadata. This will not
 * include the current version because the source is used as the current version.
 */
fun getFilesForApiLevels(apiFiles: Collection<File>, currentVersion: Version): List<File> {
    // Create a map from known versions of the library to signature files
    val versionToFileMap =
        apiFiles
            .mapNotNull { file ->
                // Resource API files are not included
                if (ApiLocation.isResourceApiFilename(file.name)) return@mapNotNull null
                val version = Version.parseFilenameOrNull(file.name)
                if (version != null) {
                    version to file
                } else {
                    null
                }
            }
            .toMap()

    val filteredVersions = filterVersions(versionToFileMap, currentVersion)
    return filteredVersions.map { versionToFileMap.getValue(it) }
}

/**
 * Returns the version names to use for the past API files when generating API version metadata.
 * This assumes that all [apiFiles] have names that can be parsed as versions.
 */
fun getVersionsForApiLevels(apiFiles: List<File>): List<Version> {
    return apiFiles.map { Version.parseFilenameOrNull(it.name)!!.roundUp() }
}

/**
 * From the full set of versions, generates a sorted list of the versions to use when generating the
 * API levels metadata. For previous major-minor version cycles, this only includes the latest
 * signature file, because we only want one file per stable release. Does not include any files for
 * the current major-minor version cycle.
 */
private fun filterVersions(
    versionToFileMap: Map<Version, File>,
    currentVersion: Version
): List<Version> {
    val filteredVersions = mutableListOf<Version>()
    var prev: Version? = null
    for (version in versionToFileMap.keys.sorted()) {
        // Add the previous version in the list only if this version is a different major.minor
        // version cycle.
        if (prev != null && !sameMajorMinor(prev, version)) {
            filteredVersions.add(prev)
        }
        prev = version
    }
    // Do not include the current version, as the source is used instead of an API file.
    if (prev != null && !sameMajorMinor(prev, currentVersion)) {
        filteredVersions.add(prev)
    }

    return filteredVersions
}

private fun sameMajorMinor(v1: Version, v2: Version) = v1.major == v2.major && v1.minor == v2.minor

private fun Version.roundUp() = Version(this.major, this.minor, this.patch)

/** Usage attribute to specify the version metadata component. */
internal val Project.versionMetadataUsage: Usage
    get() = objects.named("library-version-metadata")

/** Creates a component for the version metadata JSON and registers it for publishing. */
fun Project.registerVersionMetadataComponent(generateApiTask: TaskProvider<GenerateApiTask>) {
    configurations.create("libraryVersionMetadata") { configuration ->
        configuration.isVisible = false
        configuration.isCanBeResolved = false

        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.versionMetadataUsage)
        configuration.attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.named<Category>(Category.DOCUMENTATION)
        )
        configuration.attributes.attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            objects.named<Bundling>(Bundling.EXTERNAL)
        )

        // The generate API task has many output files, only add the version metadata as an artifact
        val levelsFile =
            generateApiTask.map { task ->
                task.apiLocation.map { location -> location.apiLevelsFile }
            }
        configuration.outgoing.artifact(levelsFile) { it.classifier = "versionMetadata" }

        registerAsComponentForPublishing(configuration)
    }
}
