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

package androidx.xr.scenecore

import android.app.Activity
import androidx.xr.runtime.Session

/**
 * Methods used to manage the [Activity]'s main spatial window when it is displayed in Home Space
 * Mode.
 *
 * @see Session
 */
public object SpatialWindow {
    /** Constant to indicate that there are no preferences for the aspect ratio. */
    public const val NO_PREFERRED_ASPECT_RATIO: Float = -1.0f

    /**
     * Sets a preferred main panel aspect ratio for Home Space Mode.
     *
     * The ratio is only applied to the [Activity]. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in Full
     * Space Mode, the preference is ignored.
     *
     * If the activity's current aspect ratio differs from the `preferredRatio`, the panel is
     * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
     * consider specifying the desired aspect ratio in your
     * [application's manifest file](https://developer.android.com/guide/topics/manifest/manifest-intro).
     * This ensures your activity launches with the preferred aspect ratio from the start.
     *
     * The default preference is set to [NO_PREFERRED_ASPECT_RATIO] allowing the user to resize the
     * spatial window to any aspect ratio. The actual initial aspect ratio is determined by
     * system-defined behavior.
     *
     * @param session the session from which to set the preference.
     * @param activity the activity for which to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. Use [NO_PREFERRED_ASPECT_RATIO] to indicate that there are no preferences.
     * @throws IllegalArgumentException if preferredRatio is smaller than 1:12 or greater than 12:1.
     *   A value <= 0.0f indicates no preference and will not throw exception.
     */
    public fun setPreferredAspectRatio(
        session: Session,
        activity: Activity,
        preferredRatio: Float,
    ): Unit = session.sceneRuntime.setPreferredAspectRatio(activity, preferredRatio)
}
