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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf

/**
 * Sets the width and height of the content.
 *
 * @param width The width to apply.
 * @param height The height to apply.
 */
public fun RemoteModifier.size(width: RemoteDp, height: RemoteDp): RemoteModifier =
    width(width).height(height)

/** Sets both the width and height of the content to [size]. */
public fun RemoteModifier.size(size: RemoteDp): RemoteModifier = width(size).height(size)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillMaxSize(fraction: Float): RemoteModifier =
    fillMaxWidth(fraction).fillMaxHeight(fraction)

/**
 * Fills the parent's maximum available width and height.
 *
 * @param fraction The fraction of the parent's maximum size to use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillParentMaxSize(fraction: Float): RemoteModifier =
    fillParentMaxWidth(fraction).fillParentMaxHeight(fraction)

/**
 * Fills the parent's maximum available width and height.
 *
 * @param fraction The fraction of the parent's maximum size to use.
 */
public fun RemoteModifier.fillMaxSize(fraction: RemoteFloat = 1f.rf): RemoteModifier =
    fillMaxWidth(fraction).fillMaxHeight(fraction)

/** Wraps the content size to its intrinsic dimensions. */
public fun RemoteModifier.wrapContentSize(): RemoteModifier =
    then(WidthModifier(Type.WRAP, RemoteFloat(1f))).then(HeightModifier(Type.WRAP, RemoteFloat(1f)))
