/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.backported.fixes

/** List of all known issue reportable by [BackportedFixManager] */
internal class KnownIssue
private constructor(

    /**
     * The public id of this issue in the [Google Issue Tracker](https://issuetracker.google.com)
     */
    val id: Long,
    /**
     * The alias for this issue, if one exists.
     *
     * Known issues can have at most one alias.
     *
     * The value 0 indicates there is no alias for this issue. Non-zero alias values are unique
     * across all known issues.
     */
    val alias: Int,
) {
    // TODO b/381266031 - Make public
    // TODO b/381267367 - Add link to public list issues

    /**
     * The url to the [Google Issue Tracker](https://issuetracker.google.com) for this known issue.
     */
    internal val url = "https://issuetracker.google.com/issues/$id"

    override fun equals(other: Any?) = other is KnownIssue && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return if (alias == 0) {
            "$id without alias"
        } else {
            "$id with alias $alias"
        }
    }

    companion object {
        // keep-sorted start newline_separated=yes sticky_prefixes=/*
        /** Sample known issue that is always fixed on a device. */
        @JvmField val KI_350037023 = KnownIssue(350037023L, 1)

        /** Sample known issue that is never fixed on a device. */
        @JvmField val KI_350037348 = KnownIssue(350037348L, 3)

        /** Sample known issue that is only applies to robolectric devices */
        @JvmField val KI_372917199 = KnownIssue(372917199L, 2)

        // keep-sorted end

        private val values by lazy {
            listOf(
                // keep-sorted start
                KI_350037023,
                KI_350037348,
                KI_372917199,
                // keep-sorted end
            )
        }

        /** List of all known issues */
        @JvmStatic
        internal fun values(): List<KnownIssue> {
            return values
        }
    }
}
