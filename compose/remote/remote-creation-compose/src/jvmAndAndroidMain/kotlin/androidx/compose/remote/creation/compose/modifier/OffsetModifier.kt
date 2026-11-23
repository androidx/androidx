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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.state.InternalRemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.densityBehavior
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.modifiers.OffsetModifier as CreationOffsetModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Stable

internal data class OffsetModifier(
    public val x: RemoteFloat? = null,
    public val y: RemoteFloat? = null,
    private val xDp: RemoteDp? = null,
    private val yDp: RemoteDp? = null,
) : RemoteModifier.Element {

    public constructor(x: RemoteDp, y: RemoteDp) : this(xDp = x, yDp = y)

    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element =
        with(this as InternalRemoteStateScope) {
            val resolvedX =
                if (densityBehavior == RemoteDensityBehavior.Dp) {
                    xDp?.value ?: (x!! / density.density)
                } else {
                    xDp?.toPx(density) ?: x!!
                }
            val resolvedY =
                if (densityBehavior == RemoteDensityBehavior.Dp) {
                    yDp?.value ?: (y!! / density.density)
                } else {
                    yDp?.toPx(density) ?: y!!
                }
            CreationOffsetModifier(resolvedX.floatId, resolvedY.floatId)
        }
}

/**
 * Offsets the content by ([x], [y]).
 *
 * @param x The horizontal offset in [RemoteDp].
 * @param y The vertical offset in [RemoteDp].
 * @return a [RemoteModifier] that offsets the content by ([x], [y])
 */
@Stable
public fun RemoteModifier.offset(x: RemoteDp = 0.rdp, y: RemoteDp = 0.rdp): RemoteModifier =
    then(OffsetModifier(x, y))
