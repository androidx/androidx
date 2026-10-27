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

@file:JvmName("RemoteVectorPainterKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.vector

import androidx.compose.remote.creation.compose.capture.DefaultIconSize
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.toRemotePathFillType
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeCap
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeJoin
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.isSpecified

/**
 * Creates a [RemoteVectorPainter] from an [ImageVector].
 *
 * @param image the [ImageVector] to create the painter for.
 * @param tintColor the tint color to apply to the image.
 */
public fun painterRemoteVector(
    image: ImageVector,
    tintColor: RemoteColor = RemoteColor(image.tintColor),
): RemotePainter {
    return createVectorPainterFromImageVector(image, tintColor)
}

internal fun RemoteGroupComponent.createGroupComponent(
    currentGroup: VectorGroup
): RemoteGroupComponent {
    for (index in 0 until currentGroup.size) {
        val vectorNode = currentGroup[index]
        if (vectorNode is VectorPath) {
            val remotePathComponent =
                RemotePathComponent().apply {
                    pathData = vectorNode.pathData.toRemotePathNodes()
                    name = vectorNode.name
                    pathFillType = vectorNode.pathFillType.toRemotePathFillType()
                    fill = vectorNode.fill?.toRemoteColor()
                    fillAlpha = vectorNode.fillAlpha.rf
                    stroke = vectorNode.stroke?.toRemoteColor()
                    strokeAlpha = vectorNode.strokeAlpha.rf
                    strokeLineWidth = vectorNode.strokeLineWidth.rf
                    strokeLineCap = vectorNode.strokeLineCap.toRemoteStrokeCap()
                    strokeLineJoin = vectorNode.strokeLineJoin.toRemoteStrokeJoin()
                    strokeLineMiter = vectorNode.strokeLineMiter.rf
                    trimPathStart = vectorNode.trimPathStart.rf
                    trimPathEnd = vectorNode.trimPathEnd.rf
                    trimPathOffset = vectorNode.trimPathOffset.rf
                }
            insertAt(index, remotePathComponent)
        } else if (vectorNode is VectorGroup) {
            val remoteGroupComponent =
                RemoteGroupComponent().apply {
                    name = vectorNode.name
                    rotation = vectorNode.rotation.rf
                    scaleX = vectorNode.scaleX.rf
                    scaleY = vectorNode.scaleY.rf
                    translationX = vectorNode.translationX.rf
                    translationY = vectorNode.translationY.rf
                    pivotX = vectorNode.pivotX.rf
                    pivotY = vectorNode.pivotY.rf
                    clipPathData = vectorNode.clipPathData.toRemotePathNodes()
                    createGroupComponent(vectorNode)
                }
            insertAt(index, remoteGroupComponent)
        }
    }
    return this
}

/** Helper method to create a VectorPainter instance from an ImageVector */
private fun createVectorPainterFromImageVector(
    imageVector: ImageVector,
    tintColor: RemoteColor = RemoteColor(imageVector.tintColor),
): RemoteVectorPainter {
    val root = RemoteGroupComponent().createGroupComponent(imageVector.root)

    val defaultWidth =
        if (imageVector.defaultWidth.isSpecified) imageVector.defaultWidth.asRdp().value
        else DefaultIconSize.rf
    val defaultHeight =
        if (imageVector.defaultHeight.isSpecified) imageVector.defaultHeight.asRdp().value
        else DefaultIconSize.rf
    val defaultSize = RemoteSize(defaultWidth, defaultHeight)

    val viewportWidth =
        if (imageVector.viewportWidth.isNaN()) defaultSize.width else imageVector.viewportWidth.rf
    val viewportHeight =
        if (imageVector.viewportHeight.isNaN()) defaultSize.height
        else imageVector.viewportHeight.rf
    val viewport = RemoteSize(viewportWidth, viewportHeight)
    return RemoteVectorPainter()
        .configureRemoteVectorPainter(
            root = root,
            viewportSize = viewport,
            name = imageVector.name,
            intrinsicColorFilter = createColorFilter(tintColor, RemoteBlendMode.SrcIn),
            autoMirror = imageVector.autoMirror,
            defaultSize = defaultSize,
        )
}
