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

import androidx.annotation.IntRange

/**
 * A known issue is a public issue from the [Google Issue Tracker](https://issuetracker.google.com)
 * which has a fix that might be backported to released devices, instead of only in the next Android
 * release.
 *
 * Some known issues are specific to certain form factors or even particular devices, not merely a
 * function of the OS version or the security patch level.
 *
 * [BackportedFixManager.isFixed] will report if the issue is fixed on a device.
 *
 * [KnownIssues] contains constants for all known issues at the time the library was built.
 *
 * @property id The public id of this issue in the
 *   [Google Issue Tracker](https://issuetracker.google.com)
 */
public class KnownIssue
internal constructor(

    /**
     * The public id of this issue in the [Google Issue Tracker](https://issuetracker.google.com)
     *
     * Details of the issue can be found at the Google issue tracker using this id.
     *
     * Ids `1` to `1023` are reserved so an 'id' never overlaps with an internal `alias`.
     */
    @IntRange(from = 1024) public val id: Long,

    /**
     * The alias for this issue, if one exists.
     *
     * The alias is used by the on device data store for known issues.
     *
     * A non null alias is unique across all known issues.
     */
    @IntRange(from = 1, to = 1023) internal val alias: Int?,

    /**
     * Set of manually tested build fingerprints of where the known issues is fixed.
     *
     * If a device has a fingerprint in this set then [BackportedFixManager.getStatus] will always
     * return [Status.Fixed].
     *
     * **NOTE** This should only be used when a proper test for a KnownIssue is delayed and there is
     * small number of build that have been manually tested to verify that the issue is fixed on
     * those builds.
     *
     * Defaults to an empty set.
     */
    internal val manuallyTestedFingerprints: Set<String> = emptySet(),

    /**
     * A function that returns true if the issue applies to a device.
     *
     * Override if the issue should only apply to specific brands, models or other static
     * properties.
     *
     * When the precondition is false. [BackportedFixManager.getStatus] will return
     * [Status.NotApplicable].
     *
     * Defaults to true for all devices.
     */
    internal val precondition: () -> Boolean = { true },
) {
    // TODO b/381267367 - Add link to public list issues

    /**
     * The url to the [Google Issue Tracker](https://issuetracker.google.com) for this known issue.
     */
    public val url: String = "https://issuetracker.google.com/issues/$id"

    override fun equals(other: Any?): Boolean = other is KnownIssue && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return if (alias == null) {
            "$id without alias"
        } else {
            "$id with alias $alias"
        }
    }
}
