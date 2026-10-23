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
@file:JvmName("RemoteDensityKt")

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * Creates a [RemoteDensity] instance from the provided [RemoteCreationDisplayInfo].
 *
 * @param creationDisplayInfo The display information containing the screen density and font scale.
 * @return A [RemoteDensity] instance with the density and font scale from the display info.
 */
internal fun RemoteDensity.Companion.from(
    creationDisplayInfo: RemoteCreationDisplayInfo
): RemoteDensity {
    return RemoteDensity(
        creationDisplayInfo.density.density.rf,
        creationDisplayInfo.density.fontScale.rf,
    )
}

/**
 * Converts a [TextUnit] to pixels using the provided [RemoteDensity].
 *
 * @param density The [RemoteDensity] used to convert this [TextUnit] to pixels.
 */
public fun TextUnit.toPx(density: RemoteDensity): RemoteFloat = asRemoteTextUnit().toPx(density)

/**
 * Converts a [Dp] to pixels using the provided [RemoteDensity].
 *
 * @param density The [RemoteDensity] used to convert this [Dp] to pixels.
 */
public fun Dp.toPx(density: RemoteDensity): RemoteFloat = asRdp().toPx(density)

/**
 * Converts a [TextUnit] to pixels using this [RemoteDensity].
 *
 * @param textUnit The [TextUnit] value to convert.
 */
public fun RemoteDensity.toPx(textUnit: TextUnit): RemoteFloat =
    textUnit.asRemoteTextUnit().toPx(this)

/**
 * Converts a [Dp] to pixels using this [RemoteDensity].
 *
 * @param dp The [Dp] value to convert.
 */
public fun RemoteDensity.toPx(dp: Dp): RemoteFloat = dp.asRdp().toPx(this)
