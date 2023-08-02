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

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.work.impl.utils.toMillisCompat
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * A specification of the requirements that need to be met before a [WorkRequest] can run.  By
 * default, WorkRequests do not have any requirements and can run immediately.  By adding
 * requirements, you can make sure that work only runs in certain situations - for example, when you
 * have an unmetered network and are charging.
 *
 * @property requiredNetworkType The type of network required for the work to run.
 * The default value is [NetworkType.NOT_REQUIRED].
 * @param requiresCharging whether device should be charging for the [WorkRequest] to run. The
 * default value is `false`.
 * @param requiresDeviceIdle whether device should be idle for the [WorkRequest] to run. The
 * default value is `false`.
 * @param requiresBatteryNotLow whether device battery should be at an acceptable level for the
 * [WorkRequest] to run. The default value is `false`.
 * @param requiresStorageNotLow whether the device's available storage should be at an acceptable
 * level for the [WorkRequest] to run. The default value is `false`.
 * @property contentTriggerUpdateDelayMillis the delay in milliseconds that is allowed from the
 * time a `content:` [Uri] change is detected to the time when the [WorkRequest] is scheduled.
 * If there are more changes during this time, the delay will be reset to the start of the most
 * recent change. This functionality is identical to the one found in `JobScheduler` and
 * is described in [android.app.job.JobInfo.Builder.setTriggerContentUpdateDelay]
 * @property contentTriggerMaxDelayMillis the maximum delay in milliseconds that is allowed
 * from the first time a `content:` [Uri] change is detected to the time when the [WorkRequest]
 * is scheduled. This functionality is identical to the one found in `JobScheduler` and is described
 * in [android.app.job.JobInfo.Builder.setTriggerContentMaxDelay].
 * @property contentUriTriggers set of [ContentUriTrigger]. [WorkRequest] will run when a local
 * `content:` [Uri] of one of the triggers in the set is updated.
 * This functionality is identical to the one found in `JobScheduler` and is described in
 * [android.app.job.JobInfo.Builder.addTriggerContentUri].
 */
class Constraints(
    @ColumnInfo(name = "required_network_type")
    val requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
    @ColumnInfo(name = "requires_charging")
    private val requiresCharging: Boolean = false,
    @ColumnInfo(name = "requires_device_idle")
    private val requiresDeviceIdle: Boolean = false,
    @ColumnInfo(name = "requires_battery_not_low")
    private val requiresBatteryNotLow: Boolean = false,
    @ColumnInfo(name = "requires_storage_not_low")
    private val requiresStorageNotLow: Boolean = false,
    @ColumnInfo(name = "trigger_content_update_delay")
    val contentTriggerUpdateDelayMillis: Long = -1,
    @ColumnInfo(name = "trigger_max_content_delay")
    val contentTriggerMaxDelayMillis: Long = -1,
    @ColumnInfo(name = "content_uri_triggers")
    val contentUriTriggers: Set<ContentUriTrigger> = setOf(),
) {
    constructor(other: Constraints) : this(
        requiresCharging = other.requiresCharging,
        requiresDeviceIdle = other.requiresDeviceIdle,
        requiredNetworkType = other.requiredNetworkType,
        requiresBatteryNotLow = other.requiresBatteryNotLow,
        requiresStorageNotLow = other.requiresStorageNotLow,
        contentUriTriggers = other.contentUriTriggers,
        contentTriggerUpdateDelayMillis = other.contentTriggerUpdateDelayMillis,
        contentTriggerMaxDelayMillis = other.contentTriggerMaxDelayMillis,
    )

    /**
     * @return `true` if the work should only execute while the device is charging
     */
    fun requiresCharging(): Boolean {
        return requiresCharging
    }

    /**
     * @return `true` if the work should only execute while the device is idle
     */
    @RequiresApi(23)
    fun requiresDeviceIdle(): Boolean {
        return requiresDeviceIdle
    }

    /**
     * @return `true` if the work should only execute when the battery isn't low
     */
    fun requiresBatteryNotLow(): Boolean {
        return requiresBatteryNotLow
    }

    /**
     * @return `true` if the work should only execute when the storage isn't low
     */
    fun requiresStorageNotLow(): Boolean {
        return requiresStorageNotLow
    }

    /**
     * @return `true` if [ContentUriTrigger] is not empty
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun hasContentUriTriggers(): Boolean {
        return contentUriTriggers.isNotEmpty()
    }

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
        return if (requiredNetworkType != that.requiredNetworkType) false
        else contentUriTriggers == that.contentUriTriggers
    }

    override fun hashCode(): Int {
        var result = requiredNetworkType.hashCode()
        result = 31 * result + if (requiresCharging) 1 else 0
        result = 31 * result + if (requiresDeviceIdle) 1 else 0
        result = 31 * result + if (requiresBatteryNotLow) 1 else 0
        result = 31 * result + if (requiresStorageNotLow) 1 else 0
        result = 31 * result +
            (contentTriggerUpdateDelayMillis xor (contentTriggerUpdateDelayMillis ushr 32)).toInt()
        result = 31 * result +
            (contentTriggerMaxDelayMillis xor (contentTriggerMaxDelayMillis ushr 32)).toInt()
        result = 31 * result + contentUriTriggers.hashCode()
        return result
    }

    /**
     * A Builder for a [Constraints] object.
     */
    class Builder {
        private var requiresCharging = false
        private var requiresDeviceIdle = false
        private var requiredNetworkType = NetworkType.NOT_REQUIRED
        private var requiresBatteryNotLow = false
        private var requiresStorageNotLow = false
        // Same defaults as JobInfo
        private var triggerContentUpdateDelay: Long = -1
        private var triggerContentMaxDelay: Long = -1
        private var contentUriTriggers = mutableSetOf<ContentUriTrigger>()

        constructor() {
            // default public constructor
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(constraints: Constraints) {
            requiresCharging = constraints.requiresCharging()
            requiresDeviceIdle = Build.VERSION.SDK_INT >= 23 && constraints.requiresDeviceIdle()
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
         * Sets whether device should be charging for the [WorkRequest] to run.  The
         * default value is `false`.
         *
         * @param requiresCharging `true` if device must be charging for the work to run
         * @return The current [Builder]
         */
        fun setRequiresCharging(requiresCharging: Boolean): Builder {
            this.requiresCharging = requiresCharging
            return this
        }

        /**
         * Sets whether device should be idle for the [WorkRequest] to run. The default
         * value is `false`.
         *
         * @param requiresDeviceIdle `true` if device must be idle for the work to run
         * @return The current [Builder]
         */
        @RequiresApi(23)
        fun setRequiresDeviceIdle(requiresDeviceIdle: Boolean): Builder {
            this.requiresDeviceIdle = requiresDeviceIdle
            return this
        }

        /**
         * Sets whether device should have a particular [NetworkType] for the
         * [WorkRequest] to run. The default value is [NetworkType.NOT_REQUIRED].
         *
         * @param networkType The type of network required for the work to run
         * @return The current [Builder]
         */
        fun setRequiredNetworkType(networkType: NetworkType): Builder {
            requiredNetworkType = networkType
            return this
        }

        /**
         * Sets whether device battery should be at an acceptable level for the
         * [WorkRequest] to run. The default value is `false`.
         *
         * @param requiresBatteryNotLow `true` if the battery should be at an acceptable level
         * for the work to run
         * @return The current [Builder]
         */
        fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean): Builder {
            this.requiresBatteryNotLow = requiresBatteryNotLow
            return this
        }

        /**
         * Sets whether the device's available storage should be at an acceptable level for the
         * [WorkRequest] to run. The default value is `false`.
         *
         * @param requiresStorageNotLow `true` if the available storage should not be below a
         * a critical threshold for the work to run
         * @return The current [Builder]
         */
        fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean): Builder {
            this.requiresStorageNotLow = requiresStorageNotLow
            return this
        }

        /**
         * Sets whether the [WorkRequest] should run when a local `content:` [Uri]
         * is updated.  This functionality is identical to the one found in `JobScheduler` and
         * is described in
         * `JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)`.
         *
         * @param uri The local `content:` Uri to observe
         * @param triggerForDescendants `true` if any changes in descendants cause this
         * [WorkRequest] to run
         * @return The current [Builder]
         */
        @RequiresApi(24)
        fun addContentUriTrigger(uri: Uri, triggerForDescendants: Boolean): Builder {
            contentUriTriggers.add(ContentUriTrigger(uri, triggerForDescendants))
            return this
        }

        /**
         * Sets the delay that is allowed from the time a `content:` [Uri]
         * change is detected to the time when the [WorkRequest] is scheduled.  If there are
         * more changes during this time, the delay will be reset to the start of the most recent
         * change. This functionality is identical to the one found in `JobScheduler` and
         * is described in `JobInfo.Builder#setTriggerContentUpdateDelay(long)`.
         *
         * @param duration The length of the delay in `timeUnit` units
         * @param timeUnit The units of time for `duration`
         * @return The current [Builder]
         */
        @RequiresApi(24)
        fun setTriggerContentUpdateDelay(duration: Long, timeUnit: TimeUnit): Builder {
            triggerContentUpdateDelay = timeUnit.toMillis(duration)
            return this
        }

        /**
         * Sets the delay that is allowed from the time a `content:` [Uri] change
         * is detected to the time when the [WorkRequest] is scheduled.  If there are more
         * changes during this time, the delay will be reset to the start of the most recent change.
         * This functionality is identical to the one found in `JobScheduler` and
         * is described in `JobInfo.Builder#setTriggerContentUpdateDelay(long)`.
         *
         * @param duration The length of the delay
         * @return The current [Builder]
         */
        @RequiresApi(26)
        fun setTriggerContentUpdateDelay(duration: Duration): Builder {
            triggerContentUpdateDelay = duration.toMillisCompat()
            return this
        }

        /**
         * Sets the maximum delay that is allowed from the first time a `content:`
         * [Uri] change is detected to the time when the [WorkRequest] is scheduled.
         * This functionality is identical to the one found in `JobScheduler` and
         * is described in `JobInfo.Builder#setTriggerContentMaxDelay(long)`.
         *
         * @param duration The length of the delay in `timeUnit` units
         * @param timeUnit The units of time for `duration`
         * @return The current [Builder]
         */
        @RequiresApi(24)
        fun setTriggerContentMaxDelay(duration: Long, timeUnit: TimeUnit): Builder {
            triggerContentMaxDelay = timeUnit.toMillis(duration)
            return this
        }

        /**
         * Sets the maximum delay that is allowed from the first time a `content:` [Uri]
         * change is detected to the time when the [WorkRequest] is scheduled. This
         * functionality is identical to the one found in `JobScheduler` and is described
         * in `JobInfo.Builder#setTriggerContentMaxDelay(long)`.
         *
         * @param duration The length of the delay
         * @return The current [Builder]
         */
        @RequiresApi(26)
        fun setTriggerContentMaxDelay(duration: Duration): Builder {
            triggerContentMaxDelay = duration.toMillisCompat()
            return this
        }

        /**
         * Generates the [Constraints] from this Builder.
         *
         * @return The [Constraints] specified by this Builder
         */
        fun build(): Constraints {
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
                requiresCharging = requiresCharging,
                requiresDeviceIdle = Build.VERSION.SDK_INT >= 23 && requiresDeviceIdle,
                requiredNetworkType = requiredNetworkType,
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
     * `content:` [Uri] is updated.  This functionality is identical to the one found
     * in `JobScheduler` and is described in
     * `JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)`.
     *
     * @property uri The local `content:` Uri to observe
     * @property isTriggeredForDescendants `true` if trigger also applies to descendants of the [Uri]
     */
    class ContentUriTrigger(val uri: Uri, val isTriggeredForDescendants: Boolean) {
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

    companion object {
        /**
         * Represents a Constraints object with no requirements.
         */
        @JvmField
        val NONE = Constraints()
    }
}
