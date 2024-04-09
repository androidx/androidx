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

package androidx.build.checkapi

import androidx.build.Version
import androidx.build.checkapi.ApiLocation.Companion.isResourceApiFilename
import androidx.build.version
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import org.gradle.api.GradleException
import org.gradle.api.Project

enum class ApiType {
    CLASSAPI,
    RESOURCEAPI
}

/**
 * Returns the API file containing the public API that this library promises to support This is API
 * file that checkApiRelease validates against
 *
 * @return the API file
 */
fun Project.getRequiredCompatibilityApiFile(): File? {
    return getRequiredCompatibilityApiFileFromDir(
        project.getApiFileDirectory(),
        project.version(),
        ApiType.CLASSAPI
    )
}

/*
 * Same as getRequiredCompatibilityApiFile but also contains a restricted API file
 */
fun Project.getRequiredCompatibilityApiLocation(): ApiLocation? {
    val publicFile = project.getRequiredCompatibilityApiFile() ?: return null
    return ApiLocation.fromPublicApiFile(publicFile)
}

/**
 * Sometimes the version of an API file might be not equal to the version of its artifact. This is
 * because under certain circumstances, APIs are not allowed to change, and in those cases we may
 * stop versioning the API. This functions returns the version of API file to use given the version
 * of an artifact
 */
fun getApiFileVersion(version: Version): Version {
    if (!isValidArtifactVersion(version)) {
        val suggestedVersion = Version("${version.major}.${version.minor}.${version.patch}-rc01")
        throw GradleException(
            "Illegal version $version . It is not allowed to have a nonzero " +
                "patch number and be alpha or beta at the same time.\n" +
                "Did you mean $suggestedVersion?"
        )
    }
    val extra = if (version.patch != 0) "" else version.extra ?: ""
    return Version(version.major, version.minor, 0, extra)
}

/** Whether it is allowed for an artifact to have this version */
fun isValidArtifactVersion(version: Version): Boolean {
    if (version.patch != 0 && (version.isAlpha() || version.isBeta() || version.isDev())) {
        return false
    }
    return true
}

/**
 * Returns the api file that version <version> is required to be compatible with. If apiType is
 * RESOURCEAPI, it will return the resource api file and if it is CLASSAPI, it will return the
 * regular api file.
 */
fun getRequiredCompatibilityApiFileFromDir(
    apiDir: File,
    apiVersion: Version,
    apiType: ApiType
): File? {
    var highestPath: Path? = null
    var highestVersion: Version? = null

    // Find the path with highest version that is lower than the current API version.
    Files.newDirectoryStream(apiDir.toPath()).forEach { path ->
        val pathName = path.name
        if (
            (apiType == ApiType.RESOURCEAPI && isResourceApiFilename(pathName)) ||
                (apiType == ApiType.CLASSAPI && !isResourceApiFilename(pathName))
        ) {
            val pathVersion = Version.parseFilenameOrNull(pathName)
            if (
                pathVersion != null &&
                    (highestVersion == null || pathVersion > highestVersion!!) &&
                    pathVersion <= apiVersion &&
                    pathVersion.isFinalApi() &&
                    pathVersion.major == apiVersion.major
            ) {
                highestPath = path
                highestVersion = pathVersion
            }
        }
    }

    return highestPath?.toFile()
}
