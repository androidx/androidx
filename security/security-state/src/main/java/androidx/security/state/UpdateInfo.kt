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

package androidx.security.state

import java.util.Date
import java.util.Objects

/** Represents information about an available update for a component. */
public class UpdateInfo(
    /** Uri of the content provider from OTA update client serving update information data. */
    public val uri: String,
    /** Component for which the update information is provided. */
    public val component: String,
    /**
     * Security patch level of the available update ready to be applied by the reporting client. Use
     * [SecurityPatchState.getComponentSecurityPatchLevel] method to get encapsulated value.
     */
    public val securityPatchLevel: String,
    /** Date when the available update was published. */
    public val publishedDate: Date
) {
    /**
     * Returns a string representation of the update information.
     *
     * @return A string that describes the update details.
     */
    public override fun toString(): String =
        "UpdateInfo(" +
            "uri=$uri, component=$component, SPL=$securityPatchLevel, date=$publishedDate)"

    /**
     * Compares this UpdateInfo with another object for equality.
     *
     * @param other The object to compare with this instance.
     * @return true if the other object is an instance of UpdateInfo and all properties match, false
     *   otherwise.
     */
    public override fun equals(other: Any?): Boolean =
        other is UpdateInfo &&
            uri == other.uri &&
            component == other.component &&
            securityPatchLevel == other.securityPatchLevel &&
            publishedDate == other.publishedDate

    /**
     * Provides a hash code for an UpdateInfo object.
     *
     * @return A hash code produced by the properties of the update info.
     */
    public override fun hashCode(): Int =
        Objects.hash(uri, component, securityPatchLevel, publishedDate)

    /** Builder class for creating an instance of UpdateInfo. */
    public class Builder {
        @set:JvmSynthetic private var uri: String = ""
        @set:JvmSynthetic private var component: String = ""
        @set:JvmSynthetic private var securityPatchLevel: String = ""
        @set:JvmSynthetic private var publishedDate: Date = Date(0) // 1970-01-01

        /**
         * Sets the URI of the update.
         *
         * @param uri The URI to set.
         * @return The builder instance for chaining.
         */
        public fun setUri(uri: String): Builder = apply { this.uri = uri }

        /**
         * Sets the component associated with the update.
         *
         * @param component The component to set.
         * @return The builder instance for chaining.
         */
        public fun setComponent(component: String): Builder = apply { this.component = component }

        /**
         * Sets the security patch level of the update.
         *
         * @param securityPatchLevel The security patch level to set.
         * @return The builder instance for chaining.
         */
        public fun setSecurityPatchLevel(securityPatchLevel: String): Builder = apply {
            this.securityPatchLevel = securityPatchLevel
        }

        /**
         * Sets the publication date of the update.
         *
         * @param publishedDate The date to set.
         * @return The builder instance for chaining.
         */
        public fun setPublishedDate(publishedDate: Date): Builder = apply {
            this.publishedDate = publishedDate
        }

        /**
         * Builds and returns an UpdateInfo object.
         *
         * @return The constructed UpdateInfo.
         */
        public fun build(): UpdateInfo =
            UpdateInfo(uri, component, securityPatchLevel, publishedDate)
    }
}
