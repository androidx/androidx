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

package androidx.compose.remote.creation.compose.vector

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.DefaultIconSize
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.RemoteVectorGroup
import androidx.compose.remote.creation.compose.capture.RemoteVectorPath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RootGroupName
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [RemotePainter] that support drawing either a Compose [ImageVector] or a [RemoteImageVector]
 * into the provided RemoteCanvas.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteVectorPainter : RemotePainter() {

    internal var root: RemoteGroupComponent
        get() = RemoteGroupComponent()
        set(value) {
            vector = RemoteVectorComponent(value)
        }

    internal var vector = RemoteVectorComponent(root)

    internal var autoMirror = false

    /** configures the intrinsic tint that may be defined on a VectorPainter */
    internal var intrinsicColorFilter: RemoteColorFilter?
        get() = vector.intrinsicColorFilter
        set(value) {
            vector.intrinsicColorFilter = value
        }

    internal var viewportSize: RemoteSize
        get() = (vector.viewportSize)
        set(value) {
            vector.viewportSize = value
        }

    internal var name: String
        get() = vector.name
        set(value) {
            vector.name = value
        }

    override fun RemoteDrawScope.onDraw() {
        with(vector) {
            val shouldMirror = autoMirror && layoutDirection == LayoutDirection.Rtl
            if (shouldMirror) {
                withTransform({
                    translate(width, 0f.rf)
                    scale(-1f.rf, 1f.rf, RemoteOffset.Zero)
                }) {
                    draw(null)
                }
            } else {
                draw(null)
            }
        }
    }

    override val intrinsicSize: RemoteSize
        get() = RemoteSize(DefaultIconSize.rf, DefaultIconSize.rf)
}

/**
 * Creates a [RemoteVectorPainter] from a [RemoteImageVector].
 *
 * @param vector The [RemoteImageVector] to create the painter for.
 * @param tintColor the tine color to apply to the image.
 */
public fun painterRemoteVector(
    vector: RemoteImageVector,
    tintColor: RemoteColor = RemoteColor(Color.Black),
): RemotePainter {
    return createVectorPainterFromRemoteImageVector(vector, tintColor, vector.tintBlendMode)
}

/**
 * Creates a [RemoteVectorPainter] from a [RemoteImageVector].
 *
 * @param image the [ImageVector] to create the painter for.
 * @param tintColor the tine color to apply to the image.
 */
public fun painterRemoteVector(
    image: ImageVector,
    tintColor: RemoteColor = RemoteColor(Color.Black),
): RemotePainter {
    return createVectorPainterFromImageVector(image, tintColor)
}

/** Helper method to configure the properties of a VectorPainter that maybe re-used */
internal fun RemoteVectorPainter.configureRemoteVectorPainter(
    root: RemoteGroupComponent,
    viewportSize: RemoteSize,
    name: String = RootGroupName,
    intrinsicColorFilter: RemoteColorFilter?,
    autoMirror: Boolean = false,
): RemoteVectorPainter = apply {
    this.root = root
    this.autoMirror = autoMirror
    this.intrinsicColorFilter = intrinsicColorFilter
    this.viewportSize = viewportSize
    this.name = name
}

/** Helper method to create a VectorPainter instance from a RemoteImageVector */
internal fun createVectorPainterFromRemoteImageVector(
    imageVector: RemoteImageVector,
    tintColor: RemoteColor,
    blendMode: BlendMode,
): RemoteVectorPainter {
    val root = RemoteGroupComponent().createGroupComponent(imageVector.root)
    val viewport = RemoteSize(imageVector.viewportWidth, imageVector.viewportHeight)
    return RemoteVectorPainter()
        .configureRemoteVectorPainter(
            root = root,
            viewportSize = viewport,
            name = imageVector.name,
            intrinsicColorFilter = RemoteBlendModeColorFilter(tintColor, blendMode),
            autoMirror = imageVector.autoMirror,
        )
}

/** Helper method to create a VectorPainter instance from an ImageVector */
internal fun createVectorPainterFromImageVector(
    imageVector: ImageVector,
    tintColor: RemoteColor = RemoteColor(imageVector.tintColor),
): RemoteVectorPainter {
    val root = RemoteGroupComponent().createGroupComponent(imageVector.root)

    val defaultSize =
        RemoteSize(imageVector.defaultWidth.asRdp().toPx(), imageVector.defaultWidth.asRdp().toPx())
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
            intrinsicColorFilter = RemoteBlendModeColorFilter(tintColor, BlendMode.SrcIn),
            autoMirror = imageVector.autoMirror,
        )
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
                    fill = vectorNode.fill
                    fillAlpha = vectorNode.fillAlpha.rf
                    stroke = vectorNode.stroke
                    strokeAlpha = vectorNode.strokeAlpha.rf
                    strokeLineWidth = vectorNode.strokeLineWidth.rf
                    strokeLineCap = vectorNode.strokeLineCap
                    strokeLineJoin = vectorNode.strokeLineJoin
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
                    createGroupComponent(vectorNode)
                }
            insertAt(index, remoteGroupComponent)
        }
    }
    return this
}

internal fun RemoteGroupComponent.createGroupComponent(
    currentGroup: RemoteVectorGroup
): RemoteGroupComponent {
    for (index in 0 until currentGroup.size) {
        val vectorNode = currentGroup[index]
        if (vectorNode is RemoteVectorPath) {
            val remotePathComponent =
                RemotePathComponent().apply {
                    pathData = vectorNode.pathData
                    name = vectorNode.name
                    fill = vectorNode.fill
                    fillAlpha = vectorNode.fillAlpha
                    stroke = vectorNode.stroke
                    strokeAlpha = vectorNode.strokeAlpha
                    strokeLineWidth = vectorNode.strokeLineWidth
                    strokeLineCap = vectorNode.strokeLineCap
                    strokeLineJoin = vectorNode.strokeLineJoin
                    strokeLineMiter = vectorNode.strokeLineMiter
                    trimPathStart = vectorNode.trimPathStart
                    trimPathEnd = vectorNode.trimPathEnd
                    trimPathOffset = vectorNode.trimPathOffset
                }
            insertAt(index, remotePathComponent)
        } else if (vectorNode is RemoteVectorGroup) {
            val remoteGroupComponent =
                RemoteGroupComponent().apply {
                    name = vectorNode.name
                    rotation = vectorNode.rotation
                    scaleX = vectorNode.scaleX
                    scaleY = vectorNode.scaleY
                    translationX = vectorNode.translationX
                    translationY = vectorNode.translationY
                    pivotX = vectorNode.pivotX
                    pivotY = vectorNode.pivotY
                    createGroupComponent(vectorNode)
                }
            insertAt(index, remoteGroupComponent)
        }
    }
    return this
}
