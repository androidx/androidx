/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import androidx.annotation.RestrictTo

/** Describes an overlay [ActivityStack] associated with [OverlayCreateParams.tag]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverlayInfo
internal constructor(
    /** The unique identifier associated with the overlay [ActivityStack]. */
    val overlayTag: String,
    /**
     * The [OverlayAttributes] of the overlay [ActivityStack] if it exists, or `null` if there's no
     * such a [ActivityStack]
     */
    val currentOverlayAttributes: OverlayAttributes?,
    /**
     * The overlay [ActivityStack] associated with [overlayTag], or `null` if there's no such a
     * [ActivityStack].
     */
    val activityStack: ActivityStack?
) {
    operator fun contains(activity: Activity): Boolean = activityStack?.contains(activity) ?: false

    override fun toString(): String =
        "${OverlayInfo::class.java.simpleName}: {" +
            "tag=$overlayTag" +
            ", currentOverlayAttrs=$currentOverlayAttributes" +
            ", activityStack=$activityStack" +
            "}"
}
