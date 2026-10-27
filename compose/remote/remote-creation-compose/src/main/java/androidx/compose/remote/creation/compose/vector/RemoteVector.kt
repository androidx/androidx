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
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.DefaultGroupName
import androidx.compose.remote.creation.compose.capture.DefaultPivotX
import androidx.compose.remote.creation.compose.capture.DefaultPivotY
import androidx.compose.remote.creation.compose.capture.DefaultRotation
import androidx.compose.remote.creation.compose.capture.DefaultScaleX
import androidx.compose.remote.creation.compose.capture.DefaultScaleY
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineMiter
import androidx.compose.remote.creation.compose.capture.DefaultStrokeLineWidth
import androidx.compose.remote.creation.compose.capture.DefaultTranslationX
import androidx.compose.remote.creation.compose.capture.DefaultTranslationY
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathEnd
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathOffset
import androidx.compose.remote.creation.compose.capture.DefaultTrimPathStart
import androidx.compose.remote.creation.compose.capture.EmptyPath
import androidx.compose.remote.creation.compose.capture.toRemotePath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.creationState
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.fastForEach
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor

/** DSL for building a vector with [RemotePathScope]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemotePathData(block: RemotePathScope.() -> Unit): List<RemotePathNode> =
    with(RemotePathScope()) {
        block()
        nodes
    }

internal sealed class RemoteVNode {
    abstract fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?)
}

internal class RemoteVectorComponent(val root: RemoteGroupComponent) : RemoteVNode() {

    var name: String = DefaultGroupName

    internal var intrinsicColorFilter: RemoteColorFilter? = null
    internal var viewportSize = RemoteSize(24f.rf, 24f.rf)

    private var rootScaleX = 1f.rf
    private var rootScaleY = 1f.rf

    private fun RemoteDrawScope.drawVector(tintFilter: RemoteColorFilter?) {
        with(root) { scale(rootScaleX, rootScaleY) { draw(tintFilter) } }
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        rootScaleX = width / viewportSize.width
        rootScaleY = height / viewportSize.height
        val targetFilter = colorFilter ?: intrinsicColorFilter
        drawVector(targetFilter)
    }

    override fun toString(): String {
        return buildString {
            append("Params: ")
            append("\tname: ").append(name).append("\n")
            append("\tviewportWidth: ").append(viewportSize.width).append("\n")
            append("\tviewportHeight: ").append(viewportSize.height).append("\n")
        }
    }
}

internal class RemotePathComponent : RemoteVNode() {
    var name = DefaultGroupName
    var fill: Brush? = null
    var fillAlpha = 1.0f.rf
    var pathFillType: RemotePathFillType = RemotePathFillType.NonZero
    var pathData: List<RemotePathNode> = EmptyPath
    var targetPathData: List<RemotePathNode>? = null
    var pathTween: RemoteFloat = 0f.rf
    var strokeAlpha = 1.0f.rf
    var strokeLineWidth = DefaultStrokeLineWidth
    var stroke: Brush? = null
    var strokeLineCap: RemoteStrokeCap = RemoteStrokeCap.Butt
    var strokeLineJoin: RemoteStrokeJoin = RemoteStrokeJoin.Miter
    var strokeLineMiter = DefaultStrokeLineMiter
    var trimPathStart = DefaultTrimPathStart
    var trimPathEnd = DefaultTrimPathEnd
    var trimPathOffset = DefaultTrimPathOffset
    var remoteFillColor: RemoteColor? = null
    var remoteStrokeColor: RemoteColor? = null

    private val path = RemotePath()
    private val targetPath = RemotePath()

    private fun RemoteDrawScope.applyBrushToPaint(
        brush: Brush?,
        alpha: RemoteFloat,
        paint: RemotePaint,
    ) {
        if (brush == null) return
        when (brush) {
            is SolidColor -> {
                val c = brush.value
                paint.color =
                    RemoteColor.rgb(
                        red = c.red.rf,
                        green = c.green.rf,
                        blue = c.blue.rf,
                        alpha = c.alpha.rf * alpha,
                    )
            }
            is ShaderBrush -> {
                // Compose ShaderBrush cannot be serialized directly without reflection,
                // degrade to transparent paint
                paint.color = RemoteColor.rgb(0f.rf, 0f.rf, 0f.rf, 0f.rf)
            }
        }
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        // The call below resets the path
        pathData.toRemotePath(path, this.remoteCanvas.creationState)
        val hasTween = targetPathData != null && targetPathData!!.isNotEmpty()
        if (hasTween) {
            targetPathData!!.toRemotePath(targetPath, this.remoteCanvas.creationState)
        }

        val isTrimmed =
            !(trimPathStart.hasConstantValue &&
                trimPathStart.constantValue == 0f &&
                trimPathEnd.hasConstantValue &&
                trimPathEnd.constantValue == 1f &&
                trimPathOffset.hasConstantValue &&
                trimPathOffset.constantValue == 0f)

        val start = if (isTrimmed) (trimPathStart + trimPathOffset) % 1f.rf else 0f.rf
        val stop =
            if (isTrimmed) {
                if (trimPathOffset.hasConstantValue && trimPathOffset.constantValue == 0f) {
                    trimPathEnd
                } else {
                    (trimPathEnd + trimPathOffset) % 1f.rf
                }
            } else {
                1f.rf
            }

        val effectiveFillBrush = fill
        val effectiveFillColor = remoteFillColor

        if (effectiveFillBrush != null || effectiveFillColor != null) {
            val fillPaint = RemotePaint {
                this.colorFilter = colorFilter
                this.remoteStyle = RemotePaintingStyle.Fill
            }
            if (effectiveFillColor != null) {
                val paintColor =
                    if (fillAlpha.hasConstantValue && fillAlpha.constantValue == 1f) {
                        effectiveFillColor
                    } else {
                        effectiveFillColor.copy(alpha = effectiveFillColor.alpha * fillAlpha)
                    }
                fillPaint.color = paintColor
            } else if (effectiveFillBrush != null) {
                applyBrushToPaint(effectiveFillBrush, fillAlpha, fillPaint)
            }
            if (hasTween) {
                drawTweenPath(path, targetPath, pathTween, start, stop, fillPaint)
            } else if (isTrimmed) {
                drawTweenPath(path, path, 0f.rf, start, stop, fillPaint)
            } else {
                drawPath(path, pathFillType, fillPaint)
            }
        }

        val effectiveStrokeBrush = stroke
        val effectiveStrokeColor = remoteStrokeColor

        if (effectiveStrokeBrush != null || effectiveStrokeColor != null) {
            val strokePaint = RemotePaint {
                this.colorFilter = colorFilter
                this.remoteStyle = RemotePaintingStyle.Stroke
                this.strokeWidth = strokeLineWidth
                this.remoteStrokeCap = strokeLineCap
                this.remoteStrokeJoin = strokeLineJoin
            }
            if (effectiveStrokeColor != null) {
                val paintColor =
                    if (strokeAlpha.hasConstantValue && strokeAlpha.constantValue == 1f) {
                        effectiveStrokeColor
                    } else {
                        effectiveStrokeColor.copy(alpha = effectiveStrokeColor.alpha * strokeAlpha)
                    }
                strokePaint.color = paintColor
            } else if (effectiveStrokeBrush != null) {
                applyBrushToPaint(effectiveStrokeBrush, strokeAlpha, strokePaint)
            }
            if (hasTween) {
                drawTweenPath(path, targetPath, pathTween, start, stop, strokePaint)
            } else if (isTrimmed) {
                drawTweenPath(path, path, 0f.rf, start, stop, strokePaint)
            } else {
                drawPath(path, pathFillType, strokePaint)
            }
        }
    }

    override fun toString() = path.toString()
}

internal class RemoteGroupComponent : RemoteVNode() {
    private val children = mutableListOf<RemoteVNode>()

    // If the name changes we should re-draw as individual nodes could
    // be modified based off of this name parameter.
    var name: String = DefaultGroupName

    var rotation: RemoteFloat = DefaultRotation

    var pivotX: RemoteFloat = DefaultPivotX

    var pivotY: RemoteFloat = DefaultPivotY

    var scaleX: RemoteFloat = DefaultScaleX

    var scaleY: RemoteFloat = DefaultScaleY

    var translationX: RemoteFloat = DefaultTranslationX

    var translationY: RemoteFloat = DefaultTranslationY

    var clipPathData: List<RemotePathNode> = EmptyPath

    private val clipPath = RemotePath()

    val numChildren: Int
        get() = children.size

    operator fun get(index: Int): RemoteVNode = children[index]

    fun insertAt(index: Int, instance: RemoteVNode) {
        if (index < numChildren) {
            children[index] = instance
        } else {
            children.add(instance)
        }
    }

    override fun RemoteDrawScope.draw(colorFilter: RemoteColorFilter?) {
        val hasTransform =
            !(translationX.hasConstantValue &&
                translationX.constantValue == 0f &&
                translationY.hasConstantValue &&
                translationY.constantValue == 0f &&
                rotation.hasConstantValue &&
                rotation.constantValue == 0f &&
                scaleX.hasConstantValue &&
                scaleX.constantValue == 1f &&
                scaleY.hasConstantValue &&
                scaleY.constantValue == 1f)
        val hasClip =
            clipPathData.isNotEmpty() &&
                Operations.CLIP_PATH in
                    this@draw.remoteCanvas.creationState.profile.supportedOperations

        if (hasTransform || hasClip) {
            withTransform({
                if (hasTransform) {
                    translate(translationX + pivotX, translationY + pivotY)
                    rotate(rotation)
                    scale(scaleX, scaleY)
                    translate(-pivotX, -pivotY)
                }
                if (hasClip) {
                    clipPathData.toRemotePath(clipPath, this@draw.remoteCanvas.creationState)
                    clipPath(clipPath)
                }
            }) {
                children.fastForEach { node -> with(node) { this@draw.draw(colorFilter) } }
            }
        } else {
            children.fastForEach { node -> with(node) { this@draw.draw(colorFilter) } }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder().append("VGroup: ").append(name)
        children.fastForEach { node -> sb.append("\t").append(node.toString()).append("\n") }
        return sb.toString()
    }
}
