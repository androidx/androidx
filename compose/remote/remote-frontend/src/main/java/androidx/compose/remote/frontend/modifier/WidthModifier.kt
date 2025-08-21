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
package androidx.compose.remote.frontend.modifier

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.RemoteDp
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class WidthModifier(val type: Type, val value: RemoteFloat) : RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.WidthModifier(
            type,
            value.internalAsFloat(),
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return if (type == Type.EXACT) {
            // TODO how to get the value of a RemoteFloat safely in preview mode
            val valueDp = with(LocalDensity.current) { value.toFloat().toDp() }
            width(valueDp)
        } else if (type == Type.EXACT_DP) {
            width(value.toFloat().dp)
        } else if (type == Type.FILL) {
            fillMaxWidth(value.toFloat())
        } else if (type == Type.WEIGHT) {
            @Suppress("INVISIBLE_REFERENCE")
            with(androidx.compose.foundation.layout.RowScopeInstance as RowScope) {
                weight(value.toFloat(), true)
            }
        } else {
            System.err.println("Not handled width modifier $type")
            this
        }
    }
}

@Composable
fun RemoteModifier.size(width: Dp, height: Dp): RemoteModifier = width(width).height(height)

@Composable fun RemoteModifier.size(value: Dp): RemoteModifier = width(value).height(value)

fun RemoteModifier.fillMaxSize(weight: RemoteFloat = RemoteFloat(1f)): RemoteModifier =
    fillMaxWidth(weight).fillMaxHeight(weight)

fun RemoteModifier.width(width: RemoteDp): RemoteModifier =
    then(WidthModifier(Type.EXACT_DP, width.value))

fun RemoteModifier.width(width: RemoteFloat): RemoteModifier =
    then(WidthModifier(Type.EXACT, width))

fun RemoteModifier.fillMaxWidth(width: RemoteFloat = RemoteFloat(1f)): RemoteModifier =
    then(WidthModifier(Type.FILL, width))

fun RemoteModifier.fillMaxWidth(width: Float): RemoteModifier =
    then(WidthModifier(Type.FILL, RemoteFloat(width)))

fun RemoteModifier.wrapContentSize(): RemoteModifier =
    then(WidthModifier(Type.WRAP, RemoteFloat(1f))).then(HeightModifier(Type.WRAP, RemoteFloat(1f)))

@Composable
fun RemoteModifier.width(value: Dp): RemoteModifier {
    val valuePx = with(LocalDensity.current) { value.toPx() }

    return then(WidthModifier(Type.EXACT, RemoteFloat(valuePx)))
}

fun RemoteModifier.width(value: Int): RemoteModifier =
    then(WidthModifier(Type.EXACT, RemoteFloat(value.toFloat())))

@Composable
fun RemoteModifier.width(value: IntrinsicSize): RemoteModifier {
    if (value == IntrinsicSize.Min) {
        return then(WidthModifier(Type.INTRINSIC_MIN, RemoteFloat(0f)))
    } else {
        return then(WidthModifier(Type.INTRINSIC_MAX, RemoteFloat(0f)))
    }
}
