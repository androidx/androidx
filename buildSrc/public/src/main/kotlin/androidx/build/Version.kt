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
    val buildMetadata: String? = null, // Used in JetBrains fork
) : Comparable<Version>, java.io.Serializable {

    constructor(
        versionString: String
    ) : this(
        major = Integer.parseInt(checkedMatcher(versionString).group(1)),
        minor = Integer.parseInt(checkedMatcher(versionString).group(2)),
        patch = Integer.parseInt(checkedMatcher(versionString).group(3)),
        preRelease = checkedMatcher(versionString).group(4)?.ifEmpty { null },
        buildMetadata = checkedMatcher(versionString).group(5)?.ifEmpty { null },
    )

    fun isSnapshot(): Boolean = "-SNAPSHOT" == preRelease

    fun isPrereleasePrefix(prefix: String): Boolean =
        preRelease?.lowercase(Locale.getDefault())?.startsWith(prefix) ?: false

    fun isAlpha(): Boolean = isPrereleasePrefix("alpha")

    fun isBeta(): Boolean = isPrereleasePrefix("beta")

    fun isDev(): Boolean = isPrereleasePrefix("dev")

    fun isRC(): Boolean = isPrereleasePrefix("rc")

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
        private const val serialVersionUID = 345435634563L

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
            return if (matcher.matches()) Version(versionString) else null
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
