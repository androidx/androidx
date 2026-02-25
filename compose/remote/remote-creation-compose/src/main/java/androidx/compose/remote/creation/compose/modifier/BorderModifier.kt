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
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.compose.layout.RemoteFloatContext
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.BorderModifier as CreationBorderModifier
import androidx.compose.remote.creation.modifiers.DynamicBorderModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.graphics.toArgb

internal class BorderModifier(
    public val width: RemoteFloat,
    public val color: RemoteColor,
    public val shape: RemoteShape = RemoteRectangleShape,
) : RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        var shapeType = ShapeType.RECTANGLE
        var roundedCorner = 0f

        if (shape === RemoteCircleShape) {
            shapeType = ShapeType.CIRCLE
        } else if (shape == RemoteRectangleShape) {
            shapeType = ShapeType.RECTANGLE
        } else if (shape is RemoteRoundedCornerShape) {
            val context = RemoteFloatContext(this)
            shapeType = ShapeType.ROUNDED_RECTANGLE

            val remoteSize = RemoteSize(context.componentWidth(), context.componentHeight())
            roundedCorner = shape.topStart.toPx(remoteSize, remoteDensity).floatId
        }

        val constantColor = color.constantValueOrNull
        return if (constantColor != null) {
            CreationBorderModifier(width.floatId, roundedCorner, constantColor.toArgb(), shapeType)
        } else {
            DynamicBorderModifier(width.floatId, roundedCorner, color.id.toShort(), shapeType)
        }
    }
}

/**
 * Draws a border around the element.
 *
 * @param width The width of the border.
 * @param color The color of the border.
 * @param shape The shape of the border. When [RemoteRoundedCornerShape] is used, only the
 *   [RemoteRoundedCornerShape.topStart] corner size is currently taken into consideration for all
 *   corners.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.border(
    width: RemoteDp,
    color: RemoteColor,
    shape: RemoteShape = RemoteRectangleShape,
): RemoteModifier {
    return then(BorderModifier(width.toPx(), color, shape))
}
