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
import androidx.core.os.BundleCompat
import java.util.Collections
import java.util.Objects

/**
 * Container for the result of a security update check from a specific provider.
 *
 * This class encapsulates the list of available updates along with metadata about the source and
 * freshness of the data. It is returned by the [IUpdateInfoService.listAvailableUpdates] method.
 *
 * @param providerPackageName package name of the application that provided these update results
 * @param updates available updates found during this check
 * @param lastCheckTimeMillis timestamp of the last successful synchronization with the backend, in
 *   milliseconds since the epoch
 * @param componentLastCheckTimesMillis last successful check time of each component in milliseconds
 *   since the epoch, keyed by component identifier; the map is copied, and entries with an empty
 *   key or a non-positive time are ignored
 */
// BanParcelableUsage is suppressed because this class manually delegates to Bundle in
// writeToParcel and createFromParcel. This approach ensures robust forward compatibility
// (as Bundle ignores unknown keys) and avoids the versioning fragility that standard
// Parcelable implementations suffer from, allowing safe evolution of the API.
@SuppressLint("BanParcelableUsage")
public class UpdateCheckResult
@JvmOverloads
constructor(
    /**
     * The package name of the application that provided these update results.
     *
     * This field identifies the authoritative source of the update data (e.g.,
     * "com.google.android.gms" for System Updates).
     */
    public val providerPackageName: String,

    /** The list of [UpdateInfo] objects representing available updates found during this check. */
    public val updates: List<UpdateInfo>,

    /**
     * The timestamp of the last successful synchronization with the backend, in milliseconds since
     * the epoch.
     *
     * This field is critical for clients to understand the freshness of the result, especially when
     * [updates] is empty. It allows the UI to display messages like "Last checked: 5 minutes ago"
     * versus "Last checked: Yesterday".
     *
     * This is a single time for the whole provider. If the provider checks more than one component,
     * it does not show which of them was checked; use [getLastCheckTimeMillis] to read the check
     * time of one component.
     */
    public val lastCheckTimeMillis: Long,
    componentLastCheckTimesMillis: Map<String, Long> = emptyMap(),
) : Parcelable {

    /**
     * The last successful check time of each component reported by this provider, in milliseconds
     * since the epoch, keyed by component identifier such as [SecurityPatchState.COMPONENT_SYSTEM].
     *
     * A provider that checks more than one component can report a time for each, so that clients
     * can tell which components were checked when [updates] is empty. A component has no entry if
     * the provider reported no check time specific to it: either the component has not been
     * checked, or the provider reports only the provider-wide [lastCheckTimeMillis]. Every value is
     * greater than 0. Use [getLastCheckTimeMillis] to read the time for a single component.
     *
     * The map is unmodifiable; attempts to modify it throw [UnsupportedOperationException].
     */
    public val componentLastCheckTimesMillis: Map<String, Long> =
        Collections.unmodifiableMap(
            componentLastCheckTimesMillis.filter { (key, value) -> key.isNotEmpty() && value > 0L }
        )

    internal constructor(
        parcel: Parcel
    ) : this(
        providerPackageName = parcel.readString() ?: "",
        updates = parcel.createTypedArrayList(UpdateInfo.CREATOR) ?: emptyList(),
        lastCheckTimeMillis = parcel.readLong(),
    )

    /**
     * Returns the last successful check time reported for [component], in milliseconds since the
     * epoch.
     *
     * The result is the later of the entry for [component] in [componentLastCheckTimesMillis] and
     * the [UpdateInfo.lastCheckTimeMillis] of any update for [component] in [updates]. It is 0 if
     * this provider reported neither, which means either that [component] has not been checked or
     * that the provider reports only the provider-wide [lastCheckTimeMillis].
     *
     * Do not use the provider-wide [lastCheckTimeMillis] in place of a 0 result unless this
     * provider is known to check only [component]. A provider that checks several components can
     * report a [lastCheckTimeMillis] that comes from a check of a different component.
     *
     * @param component component identifier, such as [SecurityPatchState.COMPONENT_SYSTEM] or
     *   [SecurityPatchState.COMPONENT_SYSTEM_MODULES]
     * @return last check time reported for [component] in milliseconds since the epoch, or 0 if
     *   none was reported
     */
    public fun getLastCheckTimeMillis(@SecurityPatchState.Component component: String): Long {
        val recordedCheckTime = componentLastCheckTimesMillis[component] ?: 0L
        val updateCheckTime =
            updates
                .filter { it.component == component && it.lastCheckTimeMillis > 0L }
                .maxOfOrNull { it.lastCheckTimeMillis } ?: 0L
        return maxOf(recordedCheckTime, updateCheckTime)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Delegate to Bundle for robust forward compatibility.
        // By wrapping the data in a Bundle, we ensure that if we add new fields in future
        // versions of this library, older clients can still read the Parcel without crashing,
        // as Bundle automatically ignores unknown keys.
        val bundle =
            Bundle().apply {
                putString(KEY_PROVIDER_PACKAGE_NAME, providerPackageName)
                putLong(KEY_LAST_CHECK_TIME_MILLIS, lastCheckTimeMillis)
                // Bundle requires ArrayList implementation for Parcelable lists
                putParcelableArrayList(KEY_UPDATES, ArrayList(updates))
                if (componentLastCheckTimesMillis.isNotEmpty()) {
                    val componentTimesBundle =
                        Bundle().apply {
                            for ((component, timestampMillis) in componentLastCheckTimesMillis) {
                                putLong(component, timestampMillis)
                            }
                        }
                    putBundle(KEY_COMPONENT_LAST_CHECK_TIMES_MILLIS, componentTimesBundle)
                }
            }
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int = 0

    public companion object {
        // Keys for Bundle delegation
        private const val KEY_PROVIDER_PACKAGE_NAME = "providerPackageName"
        private const val KEY_UPDATES = "updates"
        private const val KEY_LAST_CHECK_TIME_MILLIS = "lastCheckTimeMillis"
        private const val KEY_COMPONENT_LAST_CHECK_TIMES_MILLIS = "componentLastCheckTimesMillis"

        @JvmField
        public val CREATOR: Parcelable.Creator<UpdateCheckResult> =
            object : Parcelable.Creator<UpdateCheckResult> {
                override fun createFromParcel(source: Parcel): UpdateCheckResult {
                    // Read the data as a Bundle to safely handle version skew.
                    // If the source is an older library version, some keys might be missing;
                    // we default them to safe values (empty strings/lists) to prevent runtime
                    // crashes.
                    val bundle = source.readBundle(UpdateCheckResult::class.java.classLoader)

                    // Explicitly set the ClassLoader to ensure the inner Parcelable ArrayList
                    // (updates) can be unparceled correctly.
                    bundle?.classLoader = UpdateCheckResult::class.java.classLoader

                    // Safe reading of the list using BundleCompat.
                    // This handles the type-safe API on Android U+ (API 33) and falls back
                    // to the legacy API on older versions.
                    val updatesList =
                        bundle?.let {
                            BundleCompat.getParcelableArrayList(
                                it,
                                KEY_UPDATES,
                                UpdateInfo::class.java,
                            )
                        } ?: emptyList()

                    // getLong returns the default for a missing or non-Long value; the constructor
                    // then drops non-positive timestamps.
                    val componentCheckTimes =
                        bundle?.getBundle(KEY_COMPONENT_LAST_CHECK_TIMES_MILLIS)?.let { times ->
                            buildMap {
                                for (key in times.keySet()) {
                                    if (key.isNullOrEmpty()) continue
                                    put(key, times.getLong(key, 0L))
                                }
                            }
                        } ?: emptyMap()

                    return UpdateCheckResult(
                        providerPackageName = bundle?.getString(KEY_PROVIDER_PACKAGE_NAME) ?: "",
                        updates = updatesList,
                        lastCheckTimeMillis = bundle?.getLong(KEY_LAST_CHECK_TIME_MILLIS) ?: 0L,
                        componentLastCheckTimesMillis = componentCheckTimes,
                    )
                }

                override fun newArray(size: Int): Array<UpdateCheckResult?> = arrayOfNulls(size)
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateCheckResult

        if (providerPackageName != other.providerPackageName) return false
        if (updates != other.updates) return false
        if (lastCheckTimeMillis != other.lastCheckTimeMillis) return false
        if (componentLastCheckTimesMillis != other.componentLastCheckTimesMillis) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            providerPackageName,
            updates,
            lastCheckTimeMillis,
            componentLastCheckTimesMillis,
        )
    }

    override fun toString(): String {
        return "UpdateCheckResult(providerPackageName=$providerPackageName, updates=$updates, lastCheckTimeMillis=$lastCheckTimeMillis, componentLastCheckTimesMillis=$componentLastCheckTimesMillis)"
    }
}
