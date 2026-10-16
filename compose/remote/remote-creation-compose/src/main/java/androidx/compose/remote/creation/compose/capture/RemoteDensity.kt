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

import androidx.compose.remote.creation.compose.state.RemoteConfiguration
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTextUnit

/**
 * Represents the screen density and font scale factor used for unit conversions in a remote
 * composition context. Similar to Compose Density.
 *
 * @property density The logical density of the display, used to convert DP to pixels.
 * @property fontScale The current user preference for the scaling factor for fonts.
 */
public class RemoteDensity(
    public val density: RemoteFloat,
    public val fontScale: RemoteFloat,
) {
    /**
     * Converts a [RemoteDp] to pixels using this [RemoteDensity].
     *
     * @param dp The [RemoteDp] value to convert.
     */
    public fun toPx(dp: RemoteDp): RemoteFloat = dp.toPx(this)

    /**
     * Converts a [RemoteTextUnit] to pixels using this [RemoteDensity].
     *
     * @param textUnit The [RemoteTextUnit] value to convert.
     */
    public fun toPx(textUnit: RemoteTextUnit): RemoteFloat = textUnit.toPx(this)

    public companion object {
        private const val DEFAULT_FONT_SIZE = 14f

        /**
         * A [RemoteDensity] instance that represents the host's screen density, with font scale
         * derived from the host's system font size and density settings.
         */
        public val Host: RemoteDensity
            get() {
                val density = RemoteConfiguration.density
                val fontScale = RemoteConfiguration.fontSizePx / DEFAULT_FONT_SIZE / density
                return RemoteDensity(density, fontScale)
            }
    }
}
