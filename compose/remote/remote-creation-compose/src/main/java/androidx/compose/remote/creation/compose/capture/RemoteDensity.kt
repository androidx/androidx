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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf

/**
 * Represents the screen density and font scale factor used for unit conversions in a remote
 * composition context. Similar to Compose Density.
 *
 * @property density The logical density of the display, used to convert DP to pixels.
 * @property fontScale The current user preference for the scaling factor for fonts.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDensity(public val density: RemoteFloat, public val fontScale: RemoteFloat) {
    public companion object {
        /**
         * Creates a [RemoteDensity] instance from the provided [CreationDisplayInfo].
         *
         * @param creationDisplayInfo The display information containing the screen density.
         * @return A [RemoteDensity] instance with the density from the display info and a default
         *   font scale of 1.0.
         */
        public fun from(creationDisplayInfo: CreationDisplayInfo): RemoteDensity {
            return RemoteDensity(creationDisplayInfo.density.rf, 1.rf)
        }

        /**
         * A [RemoteDensity] instance that represents the host's screen density and a default font
         * scale of 1.0.
         */
        public val HOST: RemoteDensity =
            RemoteDensity(RemoteFloat(RemoteContext.FLOAT_DENSITY), 1.rf)
    }
}
