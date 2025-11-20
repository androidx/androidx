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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidthModifier(public val type: Type, public val value: RemoteFloat) :
    RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.WidthModifier(
            type,
            value.internalAsFloat(),
        )
    }
}

public fun RemoteModifier.width(width: RemoteDp): RemoteModifier =
    then(WidthModifier(Type.EXACT_DP, width.value))

public fun RemoteModifier.width(width: RemoteFloat): RemoteModifier =
    then(WidthModifier(Type.EXACT, width))

public fun RemoteModifier.fillMaxWidth(fraction: RemoteFloat = RemoteFloat(1f)): RemoteModifier =
    then(WidthModifier(Type.FILL, fraction))

public fun RemoteModifier.fillMaxWidth(fraction: Float): RemoteModifier =
    then(WidthModifier(Type.FILL, RemoteFloat(fraction)))

public fun RemoteModifier.width(width: Int): RemoteModifier =
    then(WidthModifier(Type.EXACT, RemoteFloat(width.toFloat())))

@Composable
public fun RemoteModifier.width(width: IntrinsicSize): RemoteModifier {
    return if (width == IntrinsicSize.Min) {
        then(WidthModifier(Type.INTRINSIC_MIN, RemoteFloat(0f)))
    } else {
        then(WidthModifier(Type.INTRINSIC_MAX, RemoteFloat(0f)))
    }
}
