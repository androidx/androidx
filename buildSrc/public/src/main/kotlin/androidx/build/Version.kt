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

package androidx.build

import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.gradle.api.Project

/** Utility class which represents a version */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val preReleaseIteration: Int? = null,
    val buildMetadata: String? = null, // Used in JetBrains fork
) : Comparable<Version>, java.io.Serializable {

    fun isSnapshot(): Boolean = "-SNAPSHOT" == preRelease

    fun isPrereleasePrefix(prefix: String): Boolean =
        preRelease?.lowercase(Locale.getDefault())?.startsWith(prefix) ?: false

    fun isAlpha(): Boolean = isPrereleasePrefix(ALPHA)

    fun isBeta(): Boolean = isPrereleasePrefix(BETA)

    fun isDev(): Boolean = isPrereleasePrefix(DEV)

    fun isRC(): Boolean = isPrereleasePrefix(RC)

    fun isStable(): Boolean = (preRelease == null)

    // Returns whether the API surface is allowed to change within the current revision (see
    // go/androidx/versioning for policy definition)
    fun isFinalApi(): Boolean = !(isSnapshot() || isAlpha() || isDev())

    override fun compareTo(other: Version) =
        compareValuesBy(
            this,
            other,
            { it.major },
            { it.minor },
            { it.patch },
            { it.preRelease == null }, // False (no extra) sorts above true (has extra)
            { it.preRelease }, // gradle uses lexicographic ordering
            // Comparing shouldn'r involve [buildMetadata]
        )

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (preRelease != null) {
            append("-$preRelease")
        }
        if (buildMetadata != null) {
            append("+$buildMetadata")
        }
    }

    companion object {
        fun parse(versionString: String): Version {
            val matched = checkedMatcher(versionString)
            val major = Integer.parseInt(matched.group(1))
            val minor = Integer.parseInt(matched.group(2))
            val patch = Integer.parseInt(matched.group(3))
            // This includes both the alpha/beta/dev/rc prefix and number (e.g. "rc01" or "alpha02")
            val preRelease = matched.group(4)?.ifEmpty { null }
            val buildMetadata = matched.group(5)?.ifEmpty { null }
            val preReleaseIteration =
                when {
                    preRelease == null -> null
                    preRelease.startsWith(ALPHA) -> preRelease.substring(ALPHA.length).toIntOrNull()
                    preRelease.startsWith(BETA) -> preRelease.substring(BETA.length).toIntOrNull()
                    preRelease.startsWith(DEV) -> preRelease.substring(DEV.length).toIntOrNull()
                    preRelease.startsWith(RC) -> preRelease.substring(RC.length).toIntOrNull()
                    else -> null
                }
            return Version(
                major = major,
                minor = minor,
                patch = patch,
                preRelease = preRelease,
                preReleaseIteration = preReleaseIteration,
                buildMetadata = buildMetadata,
            )
        }

        private const val serialVersionUID = 345435634563L

        private const val ALPHA = "alpha"
        private const val BETA = "beta"
        private const val DEV = "dev"
        private const val RC = "rc"

        private val VERSION_FILE_REGEX = Pattern.compile("^(res-)?(.*).txt$")
        private val SEMVER_VERSION_REGEX =
            Pattern.compile(
                // This expressions is taken from
                // https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
                "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$"
            )

        private fun checkedMatcher(versionString: String): Matcher {
            val matcher = SEMVER_VERSION_REGEX.matcher(versionString)
            if (!matcher.matches()) {
                throw IllegalArgumentException("Can not parse version: $versionString")
            }
            return matcher
        }

        /** @return Version or null, if a name of the given file doesn't match */
        fun parseFilenameOrNull(filename: String): Version? {
            val matcher = VERSION_FILE_REGEX.matcher(filename)
            return if (matcher.matches()) parseOrNull(matcher.group(2)) else null
        }

        /** @return Version or null, if the given string doesn't match */
        fun parseOrNull(versionString: String): Version? {
            val matcher = SEMVER_VERSION_REGEX.matcher(versionString)
            return if (matcher.matches()) parse(versionString) else null
        }

        /** Tells whether a version string would refer to a dependency range */
        fun isDependencyRange(version: String): Boolean {
            if (
                (version.startsWith("[") || version.startsWith("(")) &&
                    version.contains(",") &&
                    (version.endsWith("]") || version.endsWith(")"))
            ) {
                return true
            }
            if (version.endsWith("+")) {
                return true
            }
            return false
        }
    }
}

fun Project.version(): Version {
    return if (project.version is Version) {
        project.version as Version
    } else {
        throw IllegalStateException("Tried to use project version for $name that was never set.")
    }
}
