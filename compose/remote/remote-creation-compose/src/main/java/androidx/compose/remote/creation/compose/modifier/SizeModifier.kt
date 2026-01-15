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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.size(width: RemoteDp, height: RemoteDp): RemoteModifier =
    width(width).height(height)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.size(size: RemoteDp): RemoteModifier = width(size).height(size)

public fun RemoteModifier.fillMaxSize(fraction: Float = 1f): RemoteModifier =
    fillMaxWidth(fraction).fillMaxHeight(fraction)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.fillMaxSize(fraction: RemoteFloat): RemoteModifier =
    fillMaxWidth(fraction).fillMaxHeight(fraction)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.wrapContentSize(): RemoteModifier =
    then(WidthModifier(Type.WRAP, RemoteFloat(1f))).then(HeightModifier(Type.WRAP, RemoteFloat(1f)))
