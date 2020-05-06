/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.paint
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.core.ContentScale
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.unit.Dp

/**
 * Vector graphics object that is generated as a result of [VectorAssetBuilder]]
 * It can be composed and rendered by passing it as an argument to [VectorPainter]
 */
data class VectorAsset internal constructor(

    /**
     * Name of the Vector asset
     */
    val name: String,

    /**
     * Intrinsic width of the vector asset in [Dp]
     */
    val defaultWidth: Dp,

    /**
     * Intrinsic height of the vector asset in [Dp]
     */
    val defaultHeight: Dp,

    /**
     *  Used to define the width of the viewport space. Viewport is basically the virtual canvas
     *  where the paths are drawn on.
     */
    val viewportWidth: Float,

    /**
     * Used to define the height of the viewport space. Viewport is basically the virtual canvas
     * where the paths are drawn on.
     */
    val viewportHeight: Float,

    /**
     * Root group of the vector asset that contains all the child groups and paths
     */
    val root: VectorGroup
)

sealed class VectorNode

/**
 * Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * The transformations are applied in the order of scale, rotate then translate.
 */
class VectorGroup(
    /**
     * Name of the corresponding group
     */
    val name: String = DefaultGroupName,

    /**
     * Rotation of the group in degrees
     */
    val rotation: Float = DefaultRotation,

    /**
     * X coordinate of the pivot point to rotate or scale the group
     */
    val pivotX: Float = DefaultPivotX,

    /**
     * Y coordinate of the pivot point to rotate or scale the group
     */
    val pivotY: Float = DefaultPivotY,

    /**
     * Scale factor in the X-axis to apply to the group
     */
    val scaleX: Float = DefaultScaleX,

    /**
     * Scale factor in the Y-axis to apply to the group
     */
    val scaleY: Float = DefaultScaleY,

    /**
     * Translation in virtual pixels to apply along the x-axis
     */
    val translationX: Float = DefaultTranslationX,

    /**
     * Translation in virtual pixels to apply along the y-axis
     */
    val translationY: Float = DefaultTranslationY,

    /**
     * Path information used to clip the content within the group
     */
    val clipPathData: List<PathNode> = EmptyPath

) : VectorNode(), Iterable<VectorNode> {

    private val children = ArrayList<VectorNode>()

    internal fun addNode(node: VectorNode) {
        children.add(node)
    }

    val size: Int
        get() = children.size

    operator fun get(index: Int): VectorNode {
        return children[index]
    }

    override fun iterator(): Iterator<VectorNode> {
        return object : Iterator<VectorNode> {

            val it = children.iterator()

            override fun hasNext(): Boolean = it.hasNext()

            override fun next(): VectorNode = it.next()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VectorGroup

        if (name != other.name) return false
        if (rotation != other.rotation) return false
        if (pivotX != other.pivotX) return false
        if (pivotY != other.pivotY) return false
        if (scaleX != other.scaleX) return false
        if (scaleY != other.scaleY) return false
        if (translationX != other.translationX) return false
        if (translationY != other.translationY) return false
        if (clipPathData != other.clipPathData) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + pivotX.hashCode()
        result = 31 * result + pivotY.hashCode()
        result = 31 * result + scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + translationX.hashCode()
        result = 31 * result + translationY.hashCode()
        result = 31 * result + clipPathData.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}

/**
 * Leaf node of a Vector graphics tree. This specifies a path shape and parameters
 * to color and style the the shape itself
 */
data class VectorPath(
    /**
     * Name of the corresponding path
     */
    val name: String = DefaultPathName,

    /**
     * Path information to render the shape of the path
     */
    val pathData: List<PathNode>,

    /**
     *  Specifies the color or gradient used to fill the path
     */
    val fill: Brush? = null,

    /**
     * Opacity to fill the path
     */
    val fillAlpha: Float = DefaultAlpha,

    /**
     * Specifies the color or gradient used to fill the stroke
     */
    val stroke: Brush? = null,

    /**
     * Opacity to stroke the path
     */
    val strokeAlpha: Float = DefaultAlpha,

    /**
     * Width of the line to stroke the path
     */
    val strokeLineWidth: Float = DefaultStrokeLineWidth,

    /**
     * Specifies the linecap for a stroked path, either butt, round, or square. The default is butt.
     */
    val strokeLineCap: StrokeCap = DefaultStrokeLineCap,

    /**
     * Specifies the linejoin for a stroked path, either miter, round or bevel. The default is miter
     */
    val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,

    /**
     * Specifies the miter limit for a stroked path, the default is 4
     */
    val strokeLineMiter: Float = DefaultStrokeLineMiter
) : VectorNode()

/**
 * Composes a vector graphic into the composition tree based on the specification
 * provided by given [VectorAsset]
 * @param[tintColor] Optional color used to tint this vector graphic
 * @param[tintBlendMode] Optional blend mode used with [tintColor], default is [BlendMode.srcIn]
 */
@Deprecated("Use VectorPainter and Modifier.paint instead")
@Composable
fun drawVector(
    vectorImage: VectorAsset,
    tintColor: Color = Color.Transparent,
    tintBlendMode: BlendMode = DefaultTintBlendMode,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside
) = Modifier.paint(
    VectorPainter(vectorImage),
    alignment = alignment,
    contentScale = contentScale,
    colorFilter = if (tintColor != Color.Transparent) {
        ColorFilter(tintColor, tintBlendMode)
    } else {
        null
    }
)
