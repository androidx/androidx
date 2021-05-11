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
package androidx.window

import java.math.BigInteger
import java.util.regex.Pattern

/**
 * Class encapsulating a version with major, minor, patch and description values.
 */
internal class Version private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val description: String
) : Comparable<Version> {

    // Cached BigInteger value of the version.
    private val bigInteger: BigInteger by lazy {
        BigInteger.valueOf(major.toLong()).shiftLeft(32)
            .or(BigInteger.valueOf(minor.toLong()))
            .shiftLeft(32)
            .or(BigInteger.valueOf(patch.toLong()))
    }

    override fun toString(): String {
        val postfix = if (description.isNotBlank()) {
            "-$description"
        } else {
            ""
        }
        return "$major.$minor.$patch$postfix"
    }

    /**
     * To compare the major, minor and patch version with another.
     *
     * @param other The version to compare to this one.
     * @return 0 if it have the same major minor and patch version; less than 0 if this version
     * sorts ahead of <var>other</var>; greater than 0 if this version sorts after <var>other</var>.
     */
    override fun compareTo(other: Version): Int {
        return bigInteger.compareTo(other.bigInteger)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Version) {
            return false
        }

        // The equals checking ignores the description.
        return major == other.major && minor == other.minor && patch == other.patch
    }

    override fun hashCode(): Int {
        // The hash code ignores the description.
        var result = 17
        result = result * 31 + major
        result = result * 31 + minor
        result = result * 31 + patch
        return result
    }

    companion object {
        val UNKNOWN = Version(0, 0, 0, "")
        @JvmField
        val VERSION_0_1 = Version(0, 1, 0, "")
        val VERSION_1_0 = Version(1, 0, 0, "")
        @JvmField
        val CURRENT = VERSION_1_0
        private const val VERSION_PATTERN_STRING = "(\\d+)(?:\\.(\\d+))(?:\\.(\\d+))(?:-(.+))?"

        /**
         * Parses a string to a version object.
         *
         * @param versionString string in the format "1.2.3" or "1.2.3-Description"
         * (major.minor.patch[-description])
         * @return the parsed Version object or `null`> if the versionString format is invalid.
         */
        @JvmStatic
        fun parse(versionString: String?): Version? {
            if (versionString == null || versionString.isBlank()) {
                return null
            }
            val matcher = Pattern.compile(VERSION_PATTERN_STRING).matcher(versionString)
            if (!matcher.matches()) {
                return null
            }
            val major = matcher.group(1)?.toInt() ?: return null
            val minor = matcher.group(2)?.toInt() ?: return null
            val patch = matcher.group(3)?.toInt() ?: return null
            val description = if (matcher.group(4) != null) matcher.group(4) else ""
            return Version(major, minor, patch, description)
        }
    }
}