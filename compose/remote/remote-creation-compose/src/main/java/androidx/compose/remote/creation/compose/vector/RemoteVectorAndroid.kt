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

package androidx.compose.remote.creation.compose.vector

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.fastMap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathNode

internal fun Brush.toRemoteColor(): RemoteColor =
    when (this) {
        is SolidColor ->
            RemoteColor.rgb(
                red = value.red.rf,
                green = value.green.rf,
                blue = value.blue.rf,
                alpha = value.alpha.rf,
            )
        is ShaderBrush -> RemoteColor.rgb(0f.rf, 0f.rf, 0f.rf, 0f.rf)
    }

internal fun List<PathNode>.toRemotePathNodes(): List<RemotePathNode> {
    return this.fastMap { it.toRemotePathNode() }
}

private fun PathNode.toRemotePathNode(): RemotePathNode {
    return when (this) {
        is PathNode.MoveTo -> RemotePathNode.MoveTo(this.x.rf, this.y.rf)
        is PathNode.LineTo -> RemotePathNode.LineTo(this.x.rf, this.y.rf)
        is PathNode.CurveTo ->
            RemotePathNode.CurveTo(
                this.x1.rf,
                this.y1.rf,
                this.x2.rf,
                this.y2.rf,
                this.x3.rf,
                this.y3.rf,
            )
        is PathNode.RelativeMoveTo -> RemotePathNode.RelativeMoveTo(this.dx.rf, this.dy.rf)
        is PathNode.QuadTo -> RemotePathNode.QuadTo(this.x1.rf, this.y1.rf, this.x2.rf, this.y2.rf)
        is PathNode.ArcTo ->
            RemotePathNode.ArcTo(
                this.horizontalEllipseRadius.rf,
                this.verticalEllipseRadius.rf,
                this.theta.rf,
                this.isMoreThanHalf.rb,
                this.isPositiveArc.rb,
                this.arcStartX.rf,
                this.arcStartY.rf,
            )
        is PathNode.HorizontalTo -> RemotePathNode.HorizontalTo(this.x.rf)
        is PathNode.VerticalTo -> RemotePathNode.VerticalTo(this.y.rf)
        is PathNode.Close -> RemotePathNode.Close
        is PathNode.RelativeHorizontalTo -> RemotePathNode.RelativeHorizontalTo(this.dx.rf)
        is PathNode.RelativeVerticalTo -> RemotePathNode.RelativeVerticalTo(this.dy.rf)
        is PathNode.ReflectiveQuadTo -> RemotePathNode.ReflectiveQuadTo(this.x.rf, this.y.rf)
        is PathNode.ReflectiveCurveTo ->
            RemotePathNode.ReflectiveCurveTo(this.x1.rf, this.y1.rf, this.x2.rf, this.y2.rf)
        is PathNode.RelativeArcTo ->
            RemotePathNode.RelativeArcTo(
                this.horizontalEllipseRadius.rf,
                this.verticalEllipseRadius.rf,
                this.theta.rf,
                this.isMoreThanHalf.rb,
                this.isPositiveArc.rb,
                this.arcStartDx.rf,
                this.arcStartDy.rf,
            )
        is PathNode.RelativeCurveTo ->
            RemotePathNode.RelativeCurveTo(
                this.dx1.rf,
                this.dy1.rf,
                this.dx2.rf,
                this.dy2.rf,
                this.dx3.rf,
                this.dy3.rf,
            )
        is PathNode.RelativeLineTo -> RemotePathNode.RelativeLineTo(this.dx.rf, this.dy.rf)
        is PathNode.RelativeQuadTo ->
            RemotePathNode.RelativeQuadTo(this.dx1.rf, this.dy1.rf, this.dx2.rf, this.dy2.rf)
        is PathNode.RelativeReflectiveCurveTo ->
            RemotePathNode.RelativeReflectiveCurveTo(
                this.dx1.rf,
                this.dy1.rf,
                this.dx2.rf,
                this.dy2.rf,
            )
        is PathNode.RelativeReflectiveQuadTo ->
            RemotePathNode.RelativeReflectiveQuadTo(this.dx.rf, this.dy.rf)
    }
}
