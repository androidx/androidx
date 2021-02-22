/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.util

/**
 * Class representing a simple Java version.
 *
 * NOTE: This class is greatly simplified to be used by the
 * [androidx.room.RoomProcessor.methodParametersVisibleInClassFiles] check only. If you want to use
 * this class, consider expanding the implementation or use a different library.
 */
data class SimpleJavaVersion(
    val major: Int,
    val minor: Int,
    val update: Int? = null
) : Comparable<SimpleJavaVersion> {

    override fun compareTo(other: SimpleJavaVersion): Int {
        return compareValuesBy(
            this,
            other,
            compareBy(SimpleJavaVersion::major)
                .thenBy(SimpleJavaVersion::minor)
                .thenBy(nullsFirst(), SimpleJavaVersion::update)
        ) { it }
    }

    companion object {

        val VERSION_11_0_0: SimpleJavaVersion = parse("11.0.0")
        val VERSION_1_8_0_202: SimpleJavaVersion = parse("1.8.0_202")

        /**
         * Returns the current Java version being used or `null` if it can't be parsed successfully.
         */
        fun getCurrentVersion(): SimpleJavaVersion? {
            return tryParse(System.getProperty("java.runtime.version"))
        }

        /**
         * Parses the Java version from the given string (e.g.,
         * "1.8.0_202-release-1483-b39-5396753"). Returns `null` if it can't be parsed successfully.
         */
        fun tryParse(version: String?): SimpleJavaVersion? {
            if (version == null) {
                return null
            }

            val parts = version.split('.')

            // There are valid JDK version strings with no parts split by dots.
            // For example: "15+36".
            if (parts.size == 1) {
                return try {
                    val major = parts[0].substringBeforeNonDigitChar()
                    SimpleJavaVersion(major.toInt(), 0)
                } catch (e: NumberFormatException) {
                    null
                }
            }

            if (parts[0] == "1") {
                // All 3 parts are needed when JDK versions strings where major version is 1.
                // For example: "1.8.0_202-release-1483-b39-5396753"
                if (parts.size < 3) {
                    return null
                }
                val major = parts[1]
                val minorAndUpdate = parts[2].substringBefore('-').split('_')
                if (minorAndUpdate.size != 2) {
                    return null
                }
                return try {
                    SimpleJavaVersion(
                        major.toInt(),
                        minorAndUpdate[0].toInt(),
                        minorAndUpdate[1].toInt()
                    )
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                return try {
                    val minor = parts[1].substringBeforeNonDigitChar()
                    SimpleJavaVersion(parts[0].toInt(), minor.toInt())
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        private fun String.substringBeforeNonDigitChar(): String {
            val nonDigitIndex = indexOfFirst { !it.isDigit() }
            return if (nonDigitIndex == -1) {
                this
            } else {
                substring(0, nonDigitIndex)
            }
        }

        /**
         * Parses the Java version from the given string (e.g.,
         * "1.8.0_202-release-1483-b39-5396753"), throwing [IllegalArgumentException] if it
         * can't be parsed successfully.
         */
        fun parse(version: String): SimpleJavaVersion {
            return tryParse(version)
                ?: throw IllegalArgumentException("Unable to parse Java version $version")
        }
    }
}