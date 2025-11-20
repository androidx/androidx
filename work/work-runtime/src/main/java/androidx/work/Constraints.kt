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
package androidx.work

import android.annotation.SuppressLint
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.work.impl.utils.NetworkRequest30
import androidx.work.impl.utils.NetworkRequestCompat
import androidx.work.impl.utils.toMillisCompat
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * A specification of the requirements that need to be met before a [WorkRequest] can run. By
 * default, WorkRequests do not have any requirements and can run immediately. By adding
 * requirements, you can make sure that work only runs in certain situations - for example, when you
 * have an unmetered network and are charging.
 */
public class Constraints {
    /** The type of network required for the work to run. */
    @ColumnInfo(name = "required_network_type") public val requiredNetworkType: NetworkType

    /**
     * [NetworkRequest] required for work to run on. It is used only the API levels >= 28 (Android
     * P). For the older API levels, [requiredNetworkType] will be used instead on the older
     * platforms and this property will be `null`.
     *
     * `NetworkRequest`-s with `NetworkSpecifier` set aren't supported, as well as `NetworkRequest`
     * with `setIncludeOtherUidNetworks` set. passed.
     */
    public val requiredNetworkRequest: NetworkRequest?
        get() = requiredNetworkRequestCompat.networkRequest

    @ColumnInfo(name = "required_network_request", defaultValue = "x''")
    internal val requiredNetworkRequestCompat: NetworkRequestCompat

    @ColumnInfo(name = "requires_charging") private val requiresCharging: Boolean

    @ColumnInfo(name = "requires_device_idle") private val requiresDeviceIdle: Boolean

    @ColumnInfo(name = "requires_battery_not_low") private val requiresBatteryNotLow: Boolean

    @ColumnInfo(name = "requires_storage_not_low") private val requiresStorageNotLow: Boolean

    /**
     * The delay in milliseconds that is allowed from the time a `content:` [Uri] change is detected
     * to the time when the [WorkRequest] is scheduled. If there are more changes during this time,
     * the delay will be reset to the start of the most recent change. This functionality is
     * identical to the one found in `JobScheduler` and is described in
     * [android.app.job.JobInfo.Builder.setTriggerContentUpdateDelay]
     */
    @get:RequiresApi(24)
    @ColumnInfo(name = "trigger_content_update_delay")
    public val contentTriggerUpdateDelayMillis: Long

    /**
     * The maximum delay in milliseconds that is allowed from the first time a `content:` [Uri]
     * change is detected to the time when the [WorkRequest] is scheduled. This functionality is
     * identical to the one found in `JobScheduler` and is described in
     * [android.app.job.JobInfo.Builder.setTriggerContentMaxDelay].
     */
    @get:RequiresApi(24)
    @ColumnInfo(name = "trigger_max_content_delay")
    public val contentTriggerMaxDelayMillis: Long

    /**
     * Set of [ContentUriTrigger]. [WorkRequest] will run when a local `content:` [Uri] of one of
     * the triggers in the set is updated. This functionality is identical to the one found in
     * `JobScheduler` and is described in [android.app.job.JobInfo.Builder.addTriggerContentUri].
     */
    @ColumnInfo(name = "content_uri_triggers")
    @get:RequiresApi(24)
    public val contentUriTriggers: Set<ContentUriTrigger>

    /**
     * Constructs [Constraints].
     *
     * @param requiredNetworkType The type of network required for the work to run. The default
     *   value is [NetworkType.NOT_REQUIRED].
     * @param requiresCharging whether device should be charging for the [WorkRequest] to run. The
     *   default value is `false`.
     * @param requiresBatteryNotLow whether device battery should be at an acceptable level for the
     *   [WorkRequest] to run. The default value is `false`.
     * @param requiresStorageNotLow whether the device's available storage should be at an
     *   acceptable level for the [WorkRequest] to run. The default value is `false`.
     */
    @Ignore
    @SuppressLint("NewApi")
    public constructor(
        requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
        requiresCharging: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        requiresStorageNotLow: Boolean = false,
    ) : this(
        requiredNetworkType = requiredNetworkType,
        requiresCharging = requiresCharging,
        requiresStorageNotLow = requiresStorageNotLow,
        requiresBatteryNotLow = requiresBatteryNotLow,
        requiresDeviceIdle = false,
    )

    /**
     * Constructs [Constraints].
     *
     * @param requiredNetworkType The type of network required for the work to run. The default
     *   value is [NetworkType.NOT_REQUIRED].
     * @param requiresCharging whether device should be charging for the [WorkRequest] to run. The
     *   default value is `false`.
     * @param requiresDeviceIdle whether device should be idle for the [WorkRequest] to run. The
     *   default value is `false`.
     * @param requiresBatteryNotLow whether device battery should be at an acceptable level for the
     *   [WorkRequest] to run. The default value is `false`.
     * @param requiresStorageNotLow whether the device's available storage should be at an
     *   acceptable level for the [WorkRequest] to run. The default value is `false`.
     */
    @Ignore
    public constructor(
        requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        requiresStorageNotLow: Boolean = false,
    ) : this(
        requiredNetworkType = requiredNetworkType,
        requiresCharging = requiresCharging,
        requiresDeviceIdle = requiresDeviceIdle,
        requiresBatteryNotLow = requiresBatteryNotLow,
        requiresStorageNotLow = requiresStorageNotLow,
        contentTriggerUpdateDelayMillis = -1,
    )

    /**
     * Constructs [Constraints].
     *
     * @param requiredNetworkType The type of network required for the work to run. The default
     *   value is [NetworkType.NOT_REQUIRED].
     * @param requiresCharging whether device should be charging for the [WorkRequest] to run. The
     *   default value is `false`.
     * @param requiresDeviceIdle whether device should be idle for the [WorkRequest] to run. The
     *   default value is `false`.
     * @param requiresBatteryNotLow whether device battery should be at an acceptable level for the
     *   [WorkRequest] to run. The default value is `false`.
     * @param requiresStorageNotLow whether the device's available storage should be at an
     *   acceptable level for the [WorkRequest] to run. The default value is `false`.
     * @param contentTriggerUpdateDelayMillis the delay in milliseconds that is allowed from the
     *   time a `content:` [Uri] change is detected to the time when the [WorkRequest] is scheduled.
     *   If there are more changes during this time, the delay will be reset to the start of the
     *   most recent change. This functionality is identical to the one found in `JobScheduler` and
     *   is described in [android.app.job.JobInfo.Builder.setTriggerContentUpdateDelay]
     * @param contentTriggerMaxDelayMillis the maximum delay in milliseconds that is allowed from
     *   the first time a `content:` [Uri] change is detected to the time when the [WorkRequest] is
     *   scheduled. This functionality is identical to the one found in `JobScheduler` and is
     *   described in [android.app.job.JobInfo.Builder.setTriggerContentMaxDelay].
     * @param contentUriTriggers set of [ContentUriTrigger]. [WorkRequest] will run when a local
     *   `content:` [Uri] of one of the triggers in the set is updated. This functionality is
     *   identical to the one found in `JobScheduler` and is described in
     *   [android.app.job.JobInfo.Builder.addTriggerContentUri].
     */
    @Ignore
    @RequiresApi(24)
    public constructor(
        requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        requiresStorageNotLow: Boolean = false,
        contentTriggerUpdateDelayMillis: Long = -1,
        contentTriggerMaxDelayMillis: Long = -1,
        contentUriTriggers: Set<ContentUriTrigger> = setOf(),
    ) {
        this.requiredNetworkRequestCompat = NetworkRequestCompat()
        this.requiredNetworkType = requiredNetworkType
        this.requiresCharging = requiresCharging
        this.requiresDeviceIdle = requiresDeviceIdle
        this.requiresBatteryNotLow = requiresBatteryNotLow
        this.requiresStorageNotLow = requiresStorageNotLow
        this.contentTriggerUpdateDelayMillis = contentTriggerUpdateDelayMillis
        this.contentTriggerMaxDelayMillis = contentTriggerMaxDelayMillis
        this.contentUriTriggers = contentUriTriggers
    }

    internal constructor(
        requiredNetworkRequestCompat: NetworkRequestCompat,
        requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false,
        requiresBatteryNotLow: Boolean = false,
        requiresStorageNotLow: Boolean = false,
        contentTriggerUpdateDelayMillis: Long = -1,
        contentTriggerMaxDelayMillis: Long = -1,
        contentUriTriggers: Set<ContentUriTrigger> = setOf(),
    ) {
        this.requiredNetworkRequestCompat = requiredNetworkRequestCompat
        this.requiredNetworkType = requiredNetworkType
        this.requiresCharging = requiresCharging
        this.requiresDeviceIdle = requiresDeviceIdle
        this.requiresBatteryNotLow = requiresBatteryNotLow
        this.requiresStorageNotLow = requiresStorageNotLow
        this.contentTriggerUpdateDelayMillis = contentTriggerUpdateDelayMillis
        this.contentTriggerMaxDelayMillis = contentTriggerMaxDelayMillis
        this.contentUriTriggers = contentUriTriggers
    }

    @SuppressLint("NewApi") // just copy everything
    public constructor(other: Constraints) {
        requiresCharging = other.requiresCharging
        requiresDeviceIdle = other.requiresDeviceIdle
        requiredNetworkRequestCompat = other.requiredNetworkRequestCompat
        requiredNetworkType = other.requiredNetworkType
        requiresBatteryNotLow = other.requiresBatteryNotLow
        requiresStorageNotLow = other.requiresStorageNotLow
        contentUriTriggers = other.contentUriTriggers
        contentTriggerUpdateDelayMillis = other.contentTriggerUpdateDelayMillis
        contentTriggerMaxDelayMillis = other.contentTriggerMaxDelayMillis
    }

    /** @return `true` if the work should only execute while the device is charging */
    public fun requiresCharging(): Boolean {
        return requiresCharging
    }

    /** @return `true` if the work should only execute while the device is idle */
    public fun requiresDeviceIdle(): Boolean {
        return requiresDeviceIdle
    }

    /** @return `true` if the work should only execute when the battery isn't low */
    public fun requiresBatteryNotLow(): Boolean {
        return requiresBatteryNotLow
    }

    /** @return `true` if the work should only execute when the storage isn't low */
    public fun requiresStorageNotLow(): Boolean {
        return requiresStorageNotLow
    }

    /** @return `true` if [ContentUriTrigger] is not empty */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun hasContentUriTriggers(): Boolean {
        return Build.VERSION.SDK_INT < 24 || contentUriTriggers.isNotEmpty()
    }

    // just use all properties in equals, no actual harm in accessing properties annotated by
    // RequiresApi(...)
    @SuppressLint("NewApi")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Constraints
        if (requiresCharging != that.requiresCharging) return false
        if (requiresDeviceIdle != that.requiresDeviceIdle) return false
        if (requiresBatteryNotLow != that.requiresBatteryNotLow) return false
        if (requiresStorageNotLow != that.requiresStorageNotLow) return false
        if (contentTriggerUpdateDelayMillis != that.contentTriggerUpdateDelayMillis) return false
        if (contentTriggerMaxDelayMillis != that.contentTriggerMaxDelayMillis) return false
        if (requiredNetworkRequest != that.requiredNetworkRequest) return false
        return if (requiredNetworkType != that.requiredNetworkType) false
        else contentUriTriggers == that.contentUriTriggers
    }

    // just use all properties in hashCode, no actual harm in accessing properties annotated by
    // RequiresApi(...)
    @SuppressLint("NewApi")
    override fun hashCode(): Int {
        var result = requiredNetworkType.hashCode()
        result = 31 * result + if (requiresCharging) 1 else 0
        result = 31 * result + if (requiresDeviceIdle) 1 else 0
        result = 31 * result + if (requiresBatteryNotLow) 1 else 0
        result = 31 * result + if (requiresStorageNotLow) 1 else 0
        result =
            31 * result +
                (contentTriggerUpdateDelayMillis xor (contentTriggerUpdateDelayMillis ushr 32))
                    .toInt()
        result =
            31 * result +
                (contentTriggerMaxDelayMillis xor (contentTriggerMaxDelayMillis ushr 32)).toInt()
        result = 31 * result + contentUriTriggers.hashCode()
        result = 31 * result + requiredNetworkRequest.hashCode()
        return result
    }

    // just use all properties in toString, no actual harm in accessing properties annotated by
    // RequiresApi(...)
    @SuppressLint("NewApi")
    override fun toString(): String {
        return "Constraints{" +
            "requiredNetworkType=$requiredNetworkType, " +
            "requiresCharging=$requiresCharging, " +
            "requiresDeviceIdle=$requiresDeviceIdle, " +
            "requiresBatteryNotLow=$requiresBatteryNotLow, " +
            "requiresStorageNotLow=$requiresStorageNotLow, " +
            "contentTriggerUpdateDelayMillis=$contentTriggerUpdateDelayMillis, " +
            "contentTriggerMaxDelayMillis=$contentTriggerMaxDelayMillis, " +
            "contentUriTriggers=$contentUriTriggers, " +
            "}"
    }

    /** A Builder for a [Constraints] object. */
    public class Builder {
        private var requiresCharging = false
        private var requiresDeviceIdle = false
        private var requiredNetworkRequest: NetworkRequestCompat = NetworkRequestCompat()
        private var requiredNetworkType = NetworkType.NOT_REQUIRED
        private var requiresBatteryNotLow = false
        private var requiresStorageNotLow = false

        // Same defaults as JobInfo
        private var triggerContentUpdateDelay: Long = -1
        private var triggerContentMaxDelay: Long = -1
        private var contentUriTriggers = mutableSetOf<ContentUriTrigger>()

        public constructor() {
            // default public constructor
        }

        /**  */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor(constraints: Constraints) {
            requiresCharging = constraints.requiresCharging()
            requiresDeviceIdle = constraints.requiresDeviceIdle()
            requiredNetworkType = constraints.requiredNetworkType
            requiresBatteryNotLow = constraints.requiresBatteryNotLow()
            requiresStorageNotLow = constraints.requiresStorageNotLow()
            if (Build.VERSION.SDK_INT >= 24) {
                triggerContentUpdateDelay = constraints.contentTriggerUpdateDelayMillis
                triggerContentMaxDelay = constraints.contentTriggerMaxDelayMillis
                contentUriTriggers = constraints.contentUriTriggers.toMutableSet()
            }
        }

        /**
         * Sets whether device should be charging for the [WorkRequest] to run. The default value is
         * `false`.
         *
         * @param requiresCharging `true` if device must be charging for the work to run
         * @return The current [Builder]
         */
        public fun setRequiresCharging(requiresCharging: Boolean): Builder {
            this.requiresCharging = requiresCharging
            return this
        }

        /**
         * Sets whether device should be idle for the [WorkRequest] to run. The default value is
         * `false`.
         *
         * @param requiresDeviceIdle `true` if device must be idle for the work to run
         * @return The current [Builder]
         */
        public fun setRequiresDeviceIdle(requiresDeviceIdle: Boolean): Builder {
            this.requiresDeviceIdle = requiresDeviceIdle
            return this
        }

        /**
         * Sets whether device should have a particular [NetworkType] for the [WorkRequest] to run.
         * The default value is [NetworkType.NOT_REQUIRED].
         *
         * @param networkType The type of network required for the work to run
         * @return The current [Builder]
         */
        public fun setRequiredNetworkType(networkType: NetworkType): Builder {
            requiredNetworkType = networkType
            requiredNetworkRequest = NetworkRequestCompat()
            return this
        }

        /**
         * Sets whether device should have a particular [NetworkRequest] for the [WorkRequest] to
         * run on the API levels >= 28 (Android P). For the older API levels, `networkType` will be
         * used instead on the older platforms.
         *
         * `NetworkRequest` with `NetworkSpecifier` set aren't supported, as well as
         * `NetworkRequest` with `setIncludeOtherUidNetworks` set. [IllegalArgumentException] will
         * be thrown if such requests are passed.
         *
         * @param networkRequest
         * @param networkType The type of network required for t
         * @return The current [Builder]
         */
        public fun setRequiredNetworkRequest(
            networkRequest: NetworkRequest,
            networkType: NetworkType,
        ): Builder {
            if (Build.VERSION.SDK_INT >= 28) {
                if (
                    Build.VERSION.SDK_INT >= 31 &&
                        NetworkRequest30.getNetworkSpecifier(networkRequest) != null
                ) {
                    throw IllegalArgumentException(
                        "NetworkRequests with NetworkSpecifiers set aren't supported."
                    )
                }
                requiredNetworkRequest = NetworkRequestCompat(networkRequest)
                requiredNetworkType = NetworkType.NOT_REQUIRED
            } else {
                requiredNetworkType = networkType
            }
            return this
        }

        /**
         * Sets whether device battery should be at an acceptable level for the [WorkRequest] to
         * run. The default value is `false`.
         *
         * @param requiresBatteryNotLow `true` if the battery should be at an acceptable level for
         *   the work to run
         * @return The current [Builder]
         */
        public fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean): Builder {
            this.requiresBatteryNotLow = requiresBatteryNotLow
            return this
        }

        /**
         * Sets whether the device's available storage should be at an acceptable level for the
         * [WorkRequest] to run. The default value is `false`.
         *
         * @param requiresStorageNotLow `true` if the available storage should not be below a a
         *   critical threshold for the work to run
         * @return The current [Builder]
         */
        public fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean): Builder {
            this.requiresStorageNotLow = requiresStorageNotLow
            return this
        }

        /**
         * Sets whether the [WorkRequest] should run when a local `content:` [Uri] is updated. This
         * functionality is identical to the one found in `JobScheduler` and is described in
         * `JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)`.
         *
         * @param uri The local `content:` Uri to observe
         * @param triggerForDescendants `true` if any changes in descendants cause this
         *   [WorkRequest] to run
         * @return The current [Builder]
         */
        @RequiresApi(24)
        public fun addContentUriTrigger(uri: Uri, triggerForDescendants: Boolean): Builder {
            contentUriTriggers.add(ContentUriTrigger(uri, triggerForDescendants))
            return this
        }

        /**
         * Sets the delay that is allowed from the time a `content:` [Uri] change is detected to the
         * time when the [WorkRequest] is scheduled. If there are more changes during this time, the
         * delay will be reset to the start of the most recent change. This functionality is
         * identical to the one found in `JobScheduler` and is described in
         * `JobInfo.Builder#setTriggerContentUpdateDelay(long)`.
         *
         * @param duration The length of the delay in `timeUnit` units
         * @param timeUnit The units of time for `duration`
         * @return The current [Builder]
         */
        @RequiresApi(24)
        public fun setTriggerContentUpdateDelay(duration: Long, timeUnit: TimeUnit): Builder {
            triggerContentUpdateDelay = timeUnit.toMillis(duration)
            return this
        }

        /**
         * Sets the delay that is allowed from the time a `content:` [Uri] change is detected to the
         * time when the [WorkRequest] is scheduled. If there are more changes during this time, the
         * delay will be reset to the start of the most recent change. This functionality is
         * identical to the one found in `JobScheduler` and is described in
         * `JobInfo.Builder#setTriggerContentUpdateDelay(long)`.
         *
         * @param duration The length of the delay
         * @return The current [Builder]
         */
        @RequiresApi(26)
        public fun setTriggerContentUpdateDelay(duration: Duration): Builder {
            triggerContentUpdateDelay = duration.toMillisCompat()
            return this
        }

        /**
         * Sets the maximum delay that is allowed from the first time a `content:` [Uri] change is
         * detected to the time when the [WorkRequest] is scheduled. This functionality is identical
         * to the one found in `JobScheduler` and is described in
         * `JobInfo.Builder#setTriggerContentMaxDelay(long)`.
         *
         * @param duration The length of the delay in `timeUnit` units
         * @param timeUnit The units of time for `duration`
         * @return The current [Builder]
         */
        @RequiresApi(24)
        public fun setTriggerContentMaxDelay(duration: Long, timeUnit: TimeUnit): Builder {
            triggerContentMaxDelay = timeUnit.toMillis(duration)
            return this
        }

        /**
         * Sets the maximum delay that is allowed from the first time a `content:` [Uri] change is
         * detected to the time when the [WorkRequest] is scheduled. This functionality is identical
         * to the one found in `JobScheduler` and is described in
         * `JobInfo.Builder#setTriggerContentMaxDelay(long)`.
         *
         * @param duration The length of the delay
         * @return The current [Builder]
         */
        @RequiresApi(26)
        public fun setTriggerContentMaxDelay(duration: Duration): Builder {
            triggerContentMaxDelay = duration.toMillisCompat()
            return this
        }

        /**
         * Generates the [Constraints] from this Builder.
         *
         * @return The [Constraints] specified by this Builder
         */
        public fun build(): Constraints {
            val contentUriTriggers: Set<ContentUriTrigger>
            val triggerContentUpdateDelay: Long
            val triggerMaxContentDelay: Long
            if (Build.VERSION.SDK_INT >= 24) {
                contentUriTriggers = this.contentUriTriggers.toSet()
                triggerContentUpdateDelay = this.triggerContentUpdateDelay
                triggerMaxContentDelay = triggerContentMaxDelay
            } else {
                contentUriTriggers = emptySet()
                triggerContentUpdateDelay = -1
                triggerMaxContentDelay = -1
            }

            return Constraints(
                requiredNetworkRequestCompat = requiredNetworkRequest,
                requiredNetworkType = requiredNetworkType,
                requiresCharging = requiresCharging,
                requiresDeviceIdle = requiresDeviceIdle,
                requiresBatteryNotLow = requiresBatteryNotLow,
                requiresStorageNotLow = requiresStorageNotLow,
                contentTriggerMaxDelayMillis = triggerMaxContentDelay,
                contentTriggerUpdateDelayMillis = triggerContentUpdateDelay,
                contentUriTriggers = contentUriTriggers,
            )
        }
    }

    /**
     * This class describes a content uri trigger on the [WorkRequest]: it should run when a local
     * `content:` [Uri] is updated. This functionality is identical to the one found in
     * `JobScheduler` and is described in
     * `JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)`.
     *
     * @property uri The local `content:` Uri to observe
     * @property isTriggeredForDescendants `true` if trigger also applies to descendants of the
     *   [Uri]
     */
    public class ContentUriTrigger(
        public val uri: Uri,
        public val isTriggeredForDescendants: Boolean,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ContentUriTrigger

            if (uri != other.uri) return false
            if (isTriggeredForDescendants != other.isTriggeredForDescendants) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uri.hashCode()
            result = 31 * result + isTriggeredForDescendants.hashCode()
            return result
        }
    }

    public companion object {
        /** Represents a Constraints object with no requirements. */
        @JvmField public val NONE: Constraints = Constraints()
    }
}

internal const val CONSTRAINTS_COLUMNS =
    "required_network_type, required_network_request, requires_charging, " +
        "requires_device_idle, requires_battery_not_low, requires_storage_not_low, " +
        "trigger_content_update_delay, trigger_max_content_delay, content_uri_triggers"
