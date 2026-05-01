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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat

/**
 * Scale the contents of the composable by the following scale factors along the horizontal and
 * vertical axis respectively. Negative scale factors can be used to mirror content across the
 * corresponding horizontal or vertical axis.
 *
 * @param scaleX Multiplier to scale content along the horizontal axis
 * @param scaleY Multiplier to scale content along the vertical axis
 * @sample androidx.compose.remote.creation.compose.samples.ScaleNonUniformSample
 * @see graphicsLayer
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.scale(scaleX: RemoteFloat, scaleY: RemoteFloat): RemoteModifier =
    graphicsLayer(scaleX = scaleX, scaleY = scaleY)

/**
 * Scale the contents of both the horizontal and vertical axis uniformly by the same scale factor.
 *
 * @param scale Multiplier to scale content along the horizontal and vertical axis
 * @sample androidx.compose.remote.creation.compose.samples.ScaleUniformSample
 * @see graphicsLayer
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.scale(scale: RemoteFloat): RemoteModifier =
    scale(scaleX = scale, scaleY = scale)
