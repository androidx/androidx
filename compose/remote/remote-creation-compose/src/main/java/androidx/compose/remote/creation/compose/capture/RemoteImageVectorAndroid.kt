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

@file:JvmName("RemoteImageVectorKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.DrawableRes
import androidx.compose.remote.creation.compose.layout.toRemotePathFillType
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeCap
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathData
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import androidx.compose.remote.creation.compose.vector.RemotePathScope
import androidx.compose.remote.creation.compose.vector.toRemotePathNodes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.DefaultPathName
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.isSpecified

/**
 * DSL extension for adding a [RemoteVectorPath] to [this].
 *
 * @param name the name for this path
 * @param fill specifies the [Brush] used to fill the path
 * @param fillAlpha the alpha to fill the path
 * @param stroke specifies the [Brush] used to fill the stroke
 * @param strokeAlpha the alpha to stroke the path
 * @param strokeLineWidth the width of the line to stroke the path
 * @param strokeLineCap specifies the linecap for a stroked path
 * @param strokeLineJoin specifies the linejoin for a stroked path
 * @param strokeLineMiter specifies the miter limit for a stroked path
 * @param pathFillType specifies the winding rule that decides how the interior of a
 *   [androidx.compose.remote.creation.RemotePath] is calculated.
 * @param pathBuilder [RemotePathScope] lambda for adding [RemotePathNode]s to this path.
 */
public fun RemoteImageVector.Builder.path(
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: RemoteFloat = 1.0f.rf,
    stroke: Brush? = null,
    strokeAlpha: RemoteFloat = 1.0f.rf,
    strokeLineWidth: RemoteFloat = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = StrokeCap.Butt,
    strokeLineJoin: StrokeJoin = StrokeJoin.Miter,
    strokeLineMiter: RemoteFloat = DefaultStrokeLineMiter,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: RemotePathScope.() -> Unit,
): RemoteImageVector.Builder =
    addPath(
        RemotePathData(pathBuilder),
        pathFillType.toRemotePathFillType(),
        name,
        fill,
        fillAlpha,
        stroke,
        strokeAlpha,
        strokeLineWidth,
        strokeLineCap.toRemoteStrokeCap(),
        strokeLineJoin.toRemoteStrokeJoin(),
        strokeLineMiter,
    )

/**
 * Load a [RemoteImageVector] from an Android vector resource.
 *
 * @param id the drawable resource identifier of the vector drawable
 * @return the [RemoteImageVector] associated with the resource
 */
@Composable
public fun RemoteImageVector.Companion.vectorResource(@DrawableRes id: Int): RemoteImageVector {
    val imageVector = ImageVector.vectorResource(id)
    return remember(imageVector) { imageVector.toRemoteImageVector() }
}

/** Converts a Compose UI [ImageVector] to a [RemoteImageVector]. */
public fun ImageVector.toRemoteImageVector(): RemoteImageVector {
    val defWidth = if (defaultWidth.isSpecified) defaultWidth.asRdp() else DefaultIconSize.rdp
    val defHeight = if (defaultHeight.isSpecified) defaultHeight.asRdp() else DefaultIconSize.rdp
    val vWidth = if (viewportWidth.isNaN()) defWidth.value else viewportWidth.rf
    val vHeight = if (viewportHeight.isNaN()) defHeight.value else viewportHeight.rf

    val builder =
        RemoteImageVector.Builder(
            defaultWidth = defWidth,
            defaultHeight = defHeight,
            viewportWidth = vWidth,
            viewportHeight = vHeight,
            tintColor = RemoteColor(tintColor),
            name = name,
            tintBlendMode = tintBlendMode,
            autoMirror = autoMirror,
        )

    fun addGroupToBuilder(group: VectorGroup) {
        for (i in 0 until group.size) {
            val node = group[i]
            if (node is VectorPath) {
                builder.addPath(
                    pathData = node.pathData.toRemotePathNodes(),
                    pathFillType = node.pathFillType.toRemotePathFillType(),
                    name = node.name,
                    fill = node.fill,
                    fillAlpha = node.fillAlpha.rf,
                    stroke = node.stroke,
                    strokeAlpha = node.strokeAlpha.rf,
                    strokeLineWidth = node.strokeLineWidth.rf,
                    strokeLineCap = node.strokeLineCap.toRemoteStrokeCap(),
                    strokeLineJoin = node.strokeLineJoin.toRemoteStrokeJoin(),
                    strokeLineMiter = node.strokeLineMiter.rf,
                    trimPathStart = node.trimPathStart.rf,
                    trimPathEnd = node.trimPathEnd.rf,
                    trimPathOffset = node.trimPathOffset.rf,
                )
            } else if (node is VectorGroup) {
                builder.addGroup(
                    name = node.name,
                    rotate = node.rotation.rf,
                    pivotX = node.pivotX.rf,
                    pivotY = node.pivotY.rf,
                    scaleX = node.scaleX.rf,
                    scaleY = node.scaleY.rf,
                    translationX = node.translationX.rf,
                    translationY = node.translationY.rf,
                    clipPathData = node.clipPathData.toRemotePathNodes(),
                )
                addGroupToBuilder(node)
                builder.clearGroup()
            }
        }
    }

    addGroupToBuilder(root)
    return builder.build()
}
