/*
 * Copyright 2025 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import java.util.Objects
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public class SerializableUpdateInfo(
    private val component: String,
    private val securityPatchLevel: String,
    private val publishedDateMillis: Long,
    private val lastCheckTimeMillis: Long,
) {
    public fun toUpdateInfo(): UpdateInfo {
        val spl =
            try {
                SecurityPatchState.getComponentSecurityPatchLevel(component, securityPatchLevel)
            } catch (e: IllegalArgumentException) {
                SecurityPatchState.GenericStringSecurityPatchLevel(securityPatchLevel)
            }
        return UpdateInfo(component, spl, publishedDateMillis, lastCheckTimeMillis)
    }
}

/** Represents information about an available update for a component. */
// BanParcelableUsage is suppressed because this class manually delegates to Bundle in
// writeToParcel and createFromParcel. This approach ensures robust forward compatibility
// (as Bundle ignores unknown keys) and avoids the versioning fragility that standard
// Parcelable implementations suffer from, allowing safe evolution of the API.
@SuppressLint("BanParcelableUsage")
public class UpdateInfo(
    /**
     * Component for which the update information is provided.
     *
     * The value should be one of the constants allowed by [SecurityPatchState.Component], such as
     * [SecurityPatchState.COMPONENT_SYSTEM].
     */
    @get:SecurityPatchState.Component public val component: String,

    /**
     * Security patch level of the available update ready to be applied by the reporting client.
     *
     * This is represented as a [SecurityPatchState.SecurityPatchLevel] object (e.g.,
     * [SecurityPatchState.DateBasedSecurityPatchLevel] or
     * [SecurityPatchState.VersionedSecurityPatchLevel]) appropriate for the component.
     */
    public val securityPatchLevel: SecurityPatchState.SecurityPatchLevel,

    /** Timestamp when the available update was published, in milliseconds since the epoch. */
    public val publishedDateMillis: Long,

    /**
     * The timestamp when this specific update was checked or discovered, in milliseconds since the
     * epoch.
     *
     * This timestamp allows the system to track the freshness of individual update records, which
     * is useful when different components are checked on different schedules.
     */
    public val lastCheckTimeMillis: Long,
) : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toSerializableUpdateInfo(): SerializableUpdateInfo =
        SerializableUpdateInfo(
            component,
            securityPatchLevel.toString(),
            publishedDateMillis,
            lastCheckTimeMillis,
        )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Delegate to Bundle for robust forward-compatibility.
        // This ensures that if we add fields in the future, older clients can still
        // read the parcel without crashing (they will just ignore the new keys).
        val bundle =
            Bundle().apply {
                putString(KEY_COMPONENT, component)
                putString(KEY_SECURITY_PATCH_LEVEL, securityPatchLevel.toString())
                putLong(KEY_PUBLISHED_DATE_MILLIS, publishedDateMillis)
                putLong(KEY_LAST_CHECK_TIME_MILLIS, lastCheckTimeMillis)
            }
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int = 0

    public companion object {
        // Keys for Bundle delegation
        private const val KEY_COMPONENT = "component"
        private const val KEY_SECURITY_PATCH_LEVEL = "securityPatchLevel"
        private const val KEY_PUBLISHED_DATE_MILLIS = "publishedDateMillis"
        private const val KEY_LAST_CHECK_TIME_MILLIS = "lastCheckTimeMillis"

        @JvmField
        public val CREATOR: Parcelable.Creator<UpdateInfo> =
            object : Parcelable.Creator<UpdateInfo> {
                override fun createFromParcel(source: Parcel): UpdateInfo {
                    // Read as a Bundle to handle version skew
                    val bundle = source.readBundle(UpdateInfo::class.java.classLoader)
                    // Set the class loader to ensure we can unparcel any custom types if added
                    // later
                    bundle?.classLoader = UpdateInfo::class.java.classLoader

                    val component = bundle?.getString(KEY_COMPONENT) ?: ""
                    val splString = bundle?.getString(KEY_SECURITY_PATCH_LEVEL) ?: ""
                    val spl =
                        try {
                            SecurityPatchState.getComponentSecurityPatchLevel(component, splString)
                        } catch (e: IllegalArgumentException) {
                            SecurityPatchState.GenericStringSecurityPatchLevel(splString)
                        }

                    return UpdateInfo(
                        component = component,
                        securityPatchLevel = spl,
                        publishedDateMillis = bundle?.getLong(KEY_PUBLISHED_DATE_MILLIS) ?: 0L,
                        lastCheckTimeMillis = bundle?.getLong(KEY_LAST_CHECK_TIME_MILLIS) ?: 0L,
                    )
                }

                override fun newArray(size: Int): Array<UpdateInfo?> {
                    return arrayOfNulls(size)
                }
            }
    }

    /**
     * Returns a string representation of the update information.
     *
     * @return A string that describes the update details.
     */
    public override fun toString(): String =
        "UpdateInfo(" +
            "component=$component, securityPatchLevel=$securityPatchLevel, publishedDateMillis=$publishedDateMillis, lastCheckTimeMillis=$lastCheckTimeMillis)"

    /**
     * Compares this UpdateInfo with another object for equality.
     *
     * @param other The object to compare with this instance.
     * @return true if the other object is an instance of UpdateInfo and all properties match, false
     *   otherwise.
     */
    public override fun equals(other: Any?): Boolean =
        other is UpdateInfo &&
            component == other.component &&
            securityPatchLevel.toString() == other.securityPatchLevel.toString() &&
            publishedDateMillis == other.publishedDateMillis &&
            lastCheckTimeMillis == other.lastCheckTimeMillis

    /**
     * Provides a hash code for an UpdateInfo object.
     *
     * @return A hash code produced by the properties of the update info.
     */
    public override fun hashCode(): Int =
        Objects.hash(
            component,
            securityPatchLevel.toString(),
            publishedDateMillis,
            lastCheckTimeMillis,
        )

    /** Builder class for creating an instance of UpdateInfo. */
    public class Builder {
        @set:JvmSynthetic private var component: String = ""
        @set:JvmSynthetic
        private var securityPatchLevel: SecurityPatchState.SecurityPatchLevel =
            SecurityPatchState.GenericStringSecurityPatchLevel("")
        @set:JvmSynthetic private var publishedDateMillis: Long = 0L
        @set:JvmSynthetic private var lastCheckTimeMillis: Long = 0L

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
         * @param securityPatchLevel The [SecurityPatchState.SecurityPatchLevel] to set. Depending
         *   on the component, this should be a concrete subclass such as
         *   [SecurityPatchState.DateBasedSecurityPatchLevel] (for SYSTEM and VENDOR) or
         *   [SecurityPatchState.VersionedSecurityPatchLevel] (for KERNEL).
         * @return The builder instance for chaining.
         */
        public fun setSecurityPatchLevel(
            securityPatchLevel: SecurityPatchState.SecurityPatchLevel
        ): Builder = apply { this.securityPatchLevel = securityPatchLevel }

        /**
         * Sets the publication date of the update.
         *
         * @param publishedDateMillis The timestamp in milliseconds since the epoch.
         * @return The builder instance for chaining.
         */
        public fun setPublishedDateMillis(publishedDateMillis: Long): Builder = apply {
            this.publishedDateMillis = publishedDateMillis
        }

        /**
         * Sets the timestamp when this update was checked or discovered.
         *
         * @param lastCheckTimeMillis The timestamp in milliseconds since the epoch.
         * @return The builder instance for chaining.
         */
        public fun setLastCheckTimeMillis(lastCheckTimeMillis: Long): Builder = apply {
            this.lastCheckTimeMillis = lastCheckTimeMillis
        }

        /**
         * Builds and returns an UpdateInfo object.
         *
         * @return The constructed UpdateInfo.
         */
        public fun build(): UpdateInfo =
            UpdateInfo(component, securityPatchLevel, publishedDateMillis, lastCheckTimeMillis)
    }
}
