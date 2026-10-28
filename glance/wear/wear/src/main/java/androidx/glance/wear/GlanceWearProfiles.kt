/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.collection.IntSet
import androidx.collection.buildIntSet
import androidx.compose.remote.creation.compose.capture.createProfile
import androidx.compose.remote.creation.profile.Profile
import androidx.glance.wear.core.RcWearOperations.CORE_TEXT
import androidx.glance.wear.core.RcWearOperations.DATA_FONT
import androidx.glance.wear.core.RcWearOperations.DYNAMIC_FLOAT_LIST
import androidx.glance.wear.core.RcWearOperations.ID_LOOKUP
import androidx.glance.wear.core.RcWearOperations.MODIFIER_GRAPHICS_LAYER
import androidx.glance.wear.core.RcWearOperations.PATH_COMBINE
import androidx.glance.wear.core.RcWearOperations.PATH_EXPRESSION
import androidx.glance.wear.core.RcWearOperations.TEXT_TRANSFORM
import androidx.glance.wear.core.RcWearOperations.UPDATE_DYNAMIC_FLOAT_LIST
import androidx.glance.wear.core.RendererVersion
import androidx.glance.wear.core.WearWidgetParams

/** Defines profiles for Glance Wear. */
public object GlanceWearProfiles {
    // Forked from androidx.compose.remote.core.RcProfiles.PROFILE_WEAR_WIDGETS
    private const val PROFILE_WEAR_WIDGETS: Int = 0x800

    /**
     * The set of operations that are allowed to be used on Wear Widgets. The actual support is
     * Host-dependent.
     */
    @VisibleForTesting
    internal val WEAR_WIDGETS_ALLOWED_OPERATIONS: IntSet = buildIntSet {
        addAll(RendererVersion.SAFE_FALLBACK_SUPPORTED_OPERATIONS)
        add(CORE_TEXT)
        add(DATA_FONT)
        add(DYNAMIC_FLOAT_LIST)
        add(ID_LOOKUP)
        add(MODIFIER_GRAPHICS_LAYER)
        add(PATH_COMBINE)
        add(PATH_EXPRESSION)
        add(TEXT_TRANSFORM)
        add(UPDATE_DYNAMIC_FLOAT_LIST)
    }

    /** A profile for Wear Widgets, based on the allowed operations for widgets. */
    public val wearWidgets: Profile by
        lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
            createWearWidgetsProfile(supportedOperations = null)
        }

    /**
     * Creates a [Profile] for Wear Widgets using the host-supported operations from [params].
     *
     * @param params the [WearWidgetParams] for the widget request
     * @return a [Profile] configured for the host's supported operations
     */
    public fun createWearWidgetsProfile(params: WearWidgetParams): Profile =
        createWearWidgetsProfile(params.rendererVersion.supportedOperations)

    /**
     * Creates a Profile for Wear Widgets, based on the allowed operations for widgets using only
     * the supported Host operations from the list.
     *
     * If `supportedOperations` is not specified, null or empty, all allowed operations are used.
     *
     * @param supportedOperations The set of operations that are supported by the host. If null, all
     *   allowed operations can be used.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun createWearWidgetsProfile(supportedOperations: IntSet? = null): Profile {
        val operationsToUse =
            if (supportedOperations != null && supportedOperations.isNotEmpty()) {
                buildIntSet {
                    supportedOperations.forEach {
                        if (WEAR_WIDGETS_ALLOWED_OPERATIONS.contains(it)) {
                            add(it)
                        }
                    }
                }
            } else {
                WEAR_WIDGETS_ALLOWED_OPERATIONS
            }
        return createProfile(
            profileFlags = PROFILE_WEAR_WIDGETS,
            supportedOperations = operationsToUse,
        )
    }
}
