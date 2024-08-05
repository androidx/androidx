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
import androidx.build.version
import java.io.File
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

private const val BCV_DIR_NAME = "bcv"

/**
 * Contains information about the files used to record a library's API surfaces. This class may
 * represent a versioned API txt file or the "current" API txt file.
 *
 * <p>
 * This class is responsible for understanding the naming pattern used by various types of API
 * files:
 * <ul>
 * <li>public
 * <li>restricted
 * <li>resource
 * </ul>
 */
data class ApiLocation(
    // Directory where the library's API files are stored
    val apiFileDirectory: File,
    // File where the library's public API surface is recorded
    val publicApiFile: File,
    // File where the library's public plus restricted (see @RestrictTo) API surfaces are recorded
    val restrictedApiFile: File,
    // File where the library's public resources are recorded
    val resourceFile: File,
    // Directory where the library's stable AIDL surface is recorded
    val aidlApiDirectory: File,
    // File where the API version history is recorded, for use in docs
    val apiLevelsFile: File
) : Serializable {

    /**
     * Returns the library version represented by this API location, or {@code null} if this is a
     * current API file.
     */
    fun version(): Version? {
        val baseName = publicApiFile.nameWithoutExtension
        if (baseName == CURRENT) {
            return null
        }
        return Version(baseName)
    }

    companion object {
        fun fromPublicApiFile(f: File): ApiLocation {
            return fromBaseName(f.parentFile, f.nameWithoutExtension)
        }

        fun fromVersion(apiFileDir: File, version: Version): ApiLocation {
            return fromBaseName(apiFileDir, version.toApiFileBaseName())
        }

        fun fromCurrent(apiFileDir: File): ApiLocation {
            return fromBaseName(apiFileDir, CURRENT)
        }

        fun isResourceApiFilename(filename: String): Boolean {
            return filename.startsWith(PREFIX_RESOURCE)
        }

        private fun fromBaseName(apiFileDir: File, baseName: String): ApiLocation {
            return ApiLocation(
                apiFileDirectory = apiFileDir,
                publicApiFile = File(apiFileDir, "$baseName$EXTENSION"),
                restrictedApiFile = File(apiFileDir, "$PREFIX_RESTRICTED$baseName$EXTENSION"),
                resourceFile = File(apiFileDir, "$PREFIX_RESOURCE$baseName$EXTENSION"),
                aidlApiDirectory = File(apiFileDir, AIDL_API_DIRECTORY_NAME).resolve(baseName),
                apiLevelsFile = File(apiFileDir, API_LEVELS)
            )
        }

        /** File name extension used by API files. */
        private const val EXTENSION = ".txt"

        /** Base file name used by current API files. */
        private const val CURRENT = "current"

        /** Prefix used for restricted API surface files. */
        private const val PREFIX_RESTRICTED = "restricted_"

        /** Prefix used for resource-type API files. */
        private const val PREFIX_RESOURCE = "res-"

        /** Directory name for location of AIDL API files */
        private const val AIDL_API_DIRECTORY_NAME = "aidl"

        /** File name for API version history file. */
        private const val API_LEVELS = "apiLevels.json"
    }
}

/** Converts the version to a valid API file base name. */
private fun Version.toApiFileBaseName(): String {
    return getApiFileVersion(this).toString()
}

/** Returns the directory containing the project's versioned and current ABI files. */
fun Project.getBcvFileDirectory(): Directory = project.layout.projectDirectory.dir(BCV_DIR_NAME)

/** Returns the directory containing the project's versioned and current API files. */
fun Project.getApiFileDirectory(): File {
    return File(project.projectDir, "api")
}

/** Returns whether the project's API file directory exists. */
fun Project.hasApiFileDirectory(): Boolean {
    return project.getApiFileDirectory().exists()
}

/** Returns the directory containing the project's built current API file. */
private fun Project.getBuiltApiFileDirectory(): File {
    @Suppress("DEPRECATION") return File(project.buildDir, "api")
}

/** Returns the directory containing the project's built current ABI file. */
fun Project.getBuiltBcvFileDirectory(): Provider<Directory> =
    project.layout.buildDirectory.dir(BCV_DIR_NAME)

/**
 * Returns an ApiLocation with the given version, or with the project's current version if not
 * specified. This method is guaranteed to return an ApiLocation that represents a versioned API txt
 * and not a current API txt.
 *
 * @param version the project version for which an API file should be returned
 * @return an ApiLocation representing a versioned API file
 */
fun Project.getVersionedApiLocation(version: Version = project.version()): ApiLocation {
    return ApiLocation.fromVersion(project.getApiFileDirectory(), version)
}

/**
 * Returns an ApiLocation for the current version. This method is guaranteed to return an
 * ApiLocation that represents a current API txt and not a versioned API txt.
 */
fun Project.getCurrentApiLocation(): ApiLocation {
    return ApiLocation.fromCurrent(project.getApiFileDirectory())
}

/**
 * Returns an ApiLocation for the "work-in-progress" current version which is built from tip-of-tree
 * and lives in the build output directory.
 */
fun Project.getBuiltApiLocation(): ApiLocation {
    return ApiLocation.fromCurrent(project.getBuiltApiFileDirectory())
}

/**
 * Contains information about the files used to record a library's API compatibility and lint
 * violation baselines.
 *
 * <p>
 * This class is responsible for understanding the naming pattern used by various types of API
 * compatibility and linting violation baseline files:
 * <ul>
 * <li>public API compatibility
 * <li>restricted API compatibility
 * <li>API lint
 * </ul>
 */
data class ApiBaselinesLocation(
    val ignoreFileDirectory: File,
    val publicApiFile: File,
    val restrictedApiFile: File,
    val apiLintFile: File
) : Serializable {

    companion object {
        fun fromApiLocation(apiLocation: ApiLocation): ApiBaselinesLocation {
            val ignoreFileDirectory = apiLocation.apiFileDirectory
            return ApiBaselinesLocation(
                ignoreFileDirectory = ignoreFileDirectory,
                publicApiFile =
                    File(
                        ignoreFileDirectory,
                        apiLocation.publicApiFile.nameWithoutExtension + EXTENSION
                    ),
                restrictedApiFile =
                    File(
                        ignoreFileDirectory,
                        apiLocation.restrictedApiFile.nameWithoutExtension + EXTENSION
                    ),
                apiLintFile = File(ignoreFileDirectory, "api_lint$EXTENSION")
            )
        }

        private const val EXTENSION = ".ignore"
    }
}
