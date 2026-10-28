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

@file:JvmName("RemoteVectorPainterKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.vector

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.DefaultIconSize
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.RemoteVectorGroup
import androidx.compose.remote.creation.compose.capture.RemoteVectorPath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.asConstantColorOrNull
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/** A [RemotePainter] that draws a [RemoteImageVector] into the provided [RemoteCanvas]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteVectorPainter : RemotePainter() {

    internal var root: RemoteGroupComponent
        get() = RemoteGroupComponent()
        set(value) {
            vector = RemoteVectorComponent(value)
        }

    internal var vector = RemoteVectorComponent(root)

    internal var autoMirror = false

    internal var defaultSize: RemoteSize = RemoteSize(DefaultIconSize.rf, DefaultIconSize.rf)

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
            val shouldMirror = autoMirror && remoteLayoutDirection == RemoteLayoutDirection.Rtl
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
        get() = defaultSize
}

/**
 * Creates a [RemoteVectorPainter] from a [RemoteImageVector].
 *
 * @param vector The [RemoteImageVector] to create the painter for.
 * @param tintColor the tint color to apply to the image.
 */
public fun painterRemoteVector(
    vector: RemoteImageVector,
    tintColor: RemoteColor = vector.tintColor,
): RemotePainter {
    return createVectorPainterFromRemoteImageVector(vector, tintColor, vector.remoteTintBlendMode)
}

/** Helper method to configure the properties of a VectorPainter that maybe re-used */
internal fun RemoteVectorPainter.configureRemoteVectorPainter(
    root: RemoteGroupComponent,
    viewportSize: RemoteSize,
    name: String,
    intrinsicColorFilter: RemoteColorFilter?,
    autoMirror: Boolean = false,
    defaultSize: RemoteSize = RemoteSize(DefaultIconSize.rf, DefaultIconSize.rf),
): RemoteVectorPainter = apply {
    this.root = root
    this.autoMirror = autoMirror
    this.intrinsicColorFilter = intrinsicColorFilter
    this.viewportSize = viewportSize
    this.name = name
    this.defaultSize = defaultSize
}

internal fun createColorFilter(
    tintColor: RemoteColor,
    tintBlendMode: RemoteBlendMode,
): RemoteColorFilter? {
    val constantColor = tintColor.asConstantColorOrNull()
    return if (constantColor != null) {
        if (constantColor.isSpecified && constantColor != Color.Transparent) {
            RemoteBlendModeColorFilter(tintColor, tintBlendMode)
        } else {
            null
        }
    } else {
        RemoteBlendModeColorFilter(tintColor, tintBlendMode)
    }
}

/** Helper method to create a VectorPainter instance from a RemoteImageVector */
internal fun createVectorPainterFromRemoteImageVector(
    imageVector: RemoteImageVector,
    tintColor: RemoteColor,
    blendMode: RemoteBlendMode,
): RemoteVectorPainter {
    val root = RemoteGroupComponent().createGroupComponent(imageVector.root)
    val viewport = RemoteSize(imageVector.viewportWidth, imageVector.viewportHeight)
    val defaultSize = RemoteSize(imageVector.defaultWidth, imageVector.defaultHeight)
    return RemoteVectorPainter()
        .configureRemoteVectorPainter(
            root = root,
            viewportSize = viewport,
            name = imageVector.name,
            intrinsicColorFilter = createColorFilter(tintColor, blendMode),
            autoMirror = imageVector.autoMirror,
            defaultSize = defaultSize,
        )
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
                    pathFillType = vectorNode.pathFillType
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
                    clipPathData = vectorNode.clipPathData
                    createGroupComponent(vectorNode)
                }
            insertAt(index, remoteGroupComponent)
        }
    }
    return this
}

/**
 * Draws the vector painter into the provided [canvas].
 *
 * @param canvas The [RemoteCanvas] to draw onto.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteVectorPainter.draw(canvas: RemoteCanvas) {
    with(canvas.drawScope) {
        onDraw()
    }
}

/**
 * Draws the [RemoteImageVector] directly into the provided [canvas].
 *
 * @param canvas The [RemoteCanvas] to draw onto.
 * @param tintColor The tint color to apply to the vector.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteImageVector.draw(
    canvas: RemoteCanvas,
    tintColor: RemoteColor = this.tintColor,
) {
    val painter = painterRemoteVector(this, tintColor) as RemoteVectorPainter
    painter.draw(canvas)
}
