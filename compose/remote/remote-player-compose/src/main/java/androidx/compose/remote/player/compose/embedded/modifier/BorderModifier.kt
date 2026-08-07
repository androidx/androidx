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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.BorderModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.readDataReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteColorAsState
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity

@get:Composable
private val BorderModifierOperation.color: Color
    get() {
        val data = readDataReflection()
        if (data.useColorId) {
            return rememberRemoteColorAsState(data.colorId).value
        }
        return Color(data.r, data.g, data.b, data.a)
    }

@Composable
internal fun Modifier.border(op: BorderModifierOperation): Modifier {
    // mBorderWidth and mRoundedCorner may be NaN-encoded variable/expression ids (e.g. dp values
    // recorded against the density variable), so resolve them reactively before scaling —
    // remote-core
    // applies the density behavior afterwards (see rawDimensionDp). The shape mirrors
    // BorderModifierOperation.paint: a plain rectangle, a circle, or a rounded rectangle.
    val density = LocalDensity.current.density
    val behavior = LocalCoreDocument.current.densityBehavior
    val data = op.readDataReflection()
    val width = rememberRemoteFloatAsState(data.borderWidth).value
    val corner = rememberRemoteFloatAsState(data.roundedCorner).value
    val shape: Shape =
        when (data.shapeType) {
            ShapeType.RECTANGLE -> RectangleShape
            ShapeType.CIRCLE -> CircleShape
            else -> RoundedCornerShape(rawDimensionDp(corner, behavior, density))
        }

    return this.border(rawDimensionDp(width, behavior, density), op.color, shape)
}
