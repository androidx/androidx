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

package androidx.wear.protolayout.expression

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.StateInt32Source
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.expression.PlatformEventSources.LAYOUT_UPDATE_IDLE
import androidx.wear.protolayout.expression.PlatformEventSources.LAYOUT_UPDATE_IDLE_ERROR
import androidx.wear.protolayout.expression.PlatformEventSources.LAYOUT_UPDATE_WAITING

/** Dynamic types for platform events */
public object PlatformEventSources {

    /** The update status of the layout. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LAYOUT_UPDATE_IDLE, LAYOUT_UPDATE_WAITING, LAYOUT_UPDATE_IDLE_ERROR)
    public annotation class LayoutUpdateStatus

    /** There are no pending layout updates. */
    public const val LAYOUT_UPDATE_IDLE: Int = 0

    /** A new layout has been requested. */
    public const val LAYOUT_UPDATE_WAITING: Int = 1

    /**
     * A new layout has been requested, but there was an error. The system will retry the update.
     */
    public const val LAYOUT_UPDATE_IDLE_ERROR: Int = 2

    /** A [DynamicBool] which receives the current visibility status from platform. */
    @RequiresSchemaVersion(major = 1, minor = 500)
    private val isLayoutVisible: DynamicBool = DynamicBool.from(Keys.LAYOUT_VISIBILITY)

    /** A [DynamicLayoutUpdateStatus] which receives the current layout update state. */
    @RequiresSchemaVersion(major = 1, minor = 600)
    private val layoutUpdateStatus: DynamicLayoutUpdateStatus =
        DynamicLayoutUpdateStatus(
            StateInt32Source.Builder()
                .setSourceKey(Keys.LAYOUT_UPDATE_STATUS.key)
                .setSourceNamespace(Keys.LAYOUT_UPDATE_STATUS.namespace)
                .build()
        )

    /**
     * Returns a [DynamicBool] which receives the current visibility status from platform.
     *
     * The visibility status value is `true` when layout is visible, and `false` when invisible.
     */
    @JvmStatic
    @RequiresSchemaVersion(major = 1, minor = 500)
    public fun isLayoutVisible(): DynamicBool = isLayoutVisible

    /**
     * Returns a [DynamicLayoutUpdateStatus] representing the current status of the layout update.
     *
     * The platform requests a new layout when explicitly requested by the tile provider, or when
     * the current layout becomes invalidated.
     *
     * The layout update status can be one of the following:
     * * [LAYOUT_UPDATE_WAITING]: The layout update is pending, starting from when a new layout is
     *   requested until it is successfully received and inflated.
     * * [LAYOUT_UPDATE_IDLE_ERROR]: The latest layout request failed.
     * * [LAYOUT_UPDATE_IDLE]: All other times, indicating no pending or failed layout update.
     */
    @JvmStatic
    @RequiresSchemaVersion(major = 1, minor = 600)
    public fun layoutUpdateStatus(): DynamicLayoutUpdateStatus = layoutUpdateStatus

    /** Data sources keys for platform event. */
    public object Keys {
        /**
         * The data source key for visibility status from platform sources. The visibility status
         * value is `true` when layout is visible, and `false` when invisible.
         */
        @JvmField
        public val LAYOUT_VISIBILITY: PlatformDataKey<DynamicBool> =
            PlatformDataKey<DynamicBool>("VisibilityStatus")

        /**
         * The data source key for layout update status from platform sources.
         *
         * The platform requests a new layout when explicitly requested by the tile provider, or
         * when the current layout becomes invalidated.
         *
         * The layout update status can be one of the following:
         * * [LAYOUT_UPDATE_WAITING]: The layout update is pending, starting from when a new layout
         *   is requested until it is successfully received and inflated.
         * * [LAYOUT_UPDATE_IDLE_ERROR]: The latest layout request failed.
         * * [LAYOUT_UPDATE_IDLE]: All other times, indicating no pending or failed layout update.
         */
        @JvmField
        public val LAYOUT_UPDATE_STATUS: PlatformDataKey<DynamicLayoutUpdateStatus> =
            PlatformDataKey<DynamicLayoutUpdateStatus>("LayoutUpdateStatus")
    }

    /** Dynamic layout update status value. */
    @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
    public class DynamicLayoutUpdateStatus
    @RestrictTo(Scope.LIBRARY)
    constructor(private val impl: DynamicInt32) : DynamicInt32 by impl {
        public companion object {
            /** Creates a constant-valued [DynamicLayoutUpdateStatus]. */
            @JvmStatic
            @RequiresSchemaVersion(major = 1, minor = 600)
            public fun constant(@LayoutUpdateStatus value: Int): DynamicLayoutUpdateStatus =
                DynamicLayoutUpdateStatus(DynamicInt32.constant(value))

            /** Creates a value to be provided from a `PlatformDataProvider`. */
            @JvmStatic
            @Suppress("UNCHECKED_CAST") // DynamicLayoutUpdateStatus acts like DynamicInt32.
            @RequiresSchemaVersion(major = 1, minor = 600)
            public fun dynamicDataValueOf(
                @LayoutUpdateStatus value: Int
            ): DynamicDataValue<DynamicLayoutUpdateStatus> =
                DynamicDataValue.fromInt(value) as DynamicDataValue<DynamicLayoutUpdateStatus>
        }
    }
}
