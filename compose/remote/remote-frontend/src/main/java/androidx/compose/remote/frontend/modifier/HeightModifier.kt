/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.remote.frontend.modifier

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.RemoteDp
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HeightModifier(public val type: Type, public val value: RemoteFloat) :
    RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.HeightModifier(
            type,
            value.internalAsFloat(),
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return if (type == Type.EXACT) {
            // TODO how to get the value of a RemoteFloat in preview mode
            val valueDp = with(LocalDensity.current) { value.toFloat().toDp() }
            height(valueDp)
        } else if (type == Type.FILL) {
            fillMaxHeight(value.toFloat())
            //        } else if (type == Type.WEIGHT) {
            //            @Suppress("INVISIBLE_REFERENCE")
            //            with(androidx.compose.foundation.layout.ColumnScopeInstance as
            // ColumnScope) {
            //                weight(value.toFloat(), true)
            //            }
        } else if (type == Type.INTRINSIC_MIN) {
            height(IntrinsicSize.Min)
        } else if (type == Type.INTRINSIC_MAX) {
            height(IntrinsicSize.Max)
        } else {
            System.err.println("Not handled height modifier $type")
            this
        }
    }
}

public fun RemoteModifier.height(width: RemoteDp): RemoteModifier =
    then(HeightModifier(Type.EXACT_DP, width.value))

public fun RemoteModifier.height(height: RemoteFloat): RemoteModifier =
    then(HeightModifier(Type.EXACT, height))

public fun RemoteModifier.fillMaxHeight(height: RemoteFloat = RemoteFloat(1f)): RemoteModifier =
    then(HeightModifier(Type.FILL, height))

public fun RemoteModifier.fillMaxHeight(height: Float): RemoteModifier =
    then(HeightModifier(Type.FILL, RemoteFloat(height)))

@Composable
public fun RemoteModifier.height(value: Dp): RemoteModifier {
    val valuePx = with(LocalDensity.current) { value.toPx() }
    return then(HeightModifier(Type.EXACT, RemoteFloat(valuePx)))
}

@Composable
public fun RemoteModifier.height(value: Int): RemoteModifier =
    then(HeightModifier(Type.EXACT, RemoteFloat(value.toFloat())))

@Composable
public fun RemoteModifier.height(value: IntrinsicSize): RemoteModifier {
    if (value == IntrinsicSize.Min) {
        return then(HeightModifier(Type.INTRINSIC_MIN, RemoteFloat(0f)))
    } else {
        return then(HeightModifier(Type.INTRINSIC_MAX, RemoteFloat(0f)))
    }
}
