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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf

/**
 * Represents the screen density and font scale factor used for unit conversions in a remote
 * composition context. Similar to Compose Density.
 *
 * @property density The logical density of the display, used to convert DP to pixels.
 * @property fontScale The current user preference for the scaling factor for fonts.
 */
public class RemoteDensity(public val density: RemoteFloat, public val fontScale: RemoteFloat) {
    public companion object {
        private const val DEFAULT_FONT_SIZE = 14f

        /**
         * Creates a [RemoteDensity] instance from the provided [CreationDisplayInfo] and [Context].
         *
         * @param creationDisplayInfo The display information containing the screen density.
         * @param context optional context to get font scale from local configuration, if not
         *   provided it would default to 1.
         * @return A [RemoteDensity] instance with the density from the display info and the local
         *   font scale.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun from(
            creationDisplayInfo: CreationDisplayInfo,
            context: Context? = null,
        ): RemoteDensity {
            val localScale = if (context == null) 1f else context.resources.configuration.fontScale
            return RemoteDensity(creationDisplayInfo.density.rf, localScale.rf)
        }

        /**
         * A [RemoteDensity] instance that represents the host's screen density, with font scale
         * derived from the host's system font size and density settings.
         */
        public val Host: RemoteDensity
            get() {
                val density = RemoteFloat(RemoteContext.FLOAT_DENSITY)
                val fontScale = RemoteFloat(Rc.System.FONT_SIZE) / DEFAULT_FONT_SIZE / density
                return RemoteDensity(density, fontScale)
            }
    }
}
