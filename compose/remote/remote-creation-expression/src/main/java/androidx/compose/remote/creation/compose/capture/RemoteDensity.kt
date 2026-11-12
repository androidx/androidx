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
import androidx.compose.remote.creation.compose.state.RemoteConfiguration
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.runtime.Immutable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteDensity(
    public val density: RemoteFloat,
    public val fontScale: RemoteFloat,
) {
    public fun toPx(dp: RemoteDp): RemoteFloat = dp.toPx(this)

    public fun toPx(textUnit: RemoteTextUnit): RemoteFloat = textUnit.toPx(this)

    public companion object {
        private const val DEFAULT_FONT_SIZE = 14f

        public val Host: RemoteDensity
            get() {
                val density = RemoteConfiguration.density
                val fontScale = RemoteConfiguration.fontSizePx / DEFAULT_FONT_SIZE / density
                return RemoteDensity(density, fontScale)
            }
    }
}
