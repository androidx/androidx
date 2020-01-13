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
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.unit.Dp
import java.util.Stack

/**
 * Builder used to construct a Vector graphic tree.
 * This is useful for caching the result of expensive operations used to construct
 * a vector graphic for compose.
 * For example, the vector graphic could be serialized and downloaded from a server and represented
 * internally in a VectorAsset before it is composed through [DrawVector]
 * The generated VectorAsset is recommended to be memoized across composition calls to avoid
 * doing redundant work
 **/
class VectorAssetBuilder(

    /**
     * Name of the vector asset
     */
    val name: String = DefaultGroupName,

    /**
     * Intrinsic width of the Vector in [Dp]
     */
    val defaultWidth: Dp,

    /**
     * Intrinsic height of the Vector in [Dp]
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
    val viewportHeight: Float
) {

    private val nodes = Stack<VectorGroup>()

    private var root = VectorGroup()
    private var isConsumed = false

    private val currentGroup: VectorGroup
        get() = nodes.peek()

    init {
        nodes.add(root)
    }

    /**
     * Create a new group and push it to the front of the stack of VectorAsset nodes
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun pushGroup(
        name: String = DefaultGroupName,
        rotate: Float = DefaultRotation,
        pivotX: Float = DefaultPivotX,
        pivotY: Float = DefaultPivotY,
        scaleX: Float = DefaultScaleX,
        scaleY: Float = DefaultScaleY,
        translationX: Float = DefaultTranslationX,
        translationY: Float = DefaultTranslationY,
        clipPathData: List<PathNode> = EmptyPath
    ): VectorAssetBuilder {
        ensureNotConsumed()
        val group = VectorGroup(
            name,
            rotate,
            pivotX,
            pivotY,
            scaleX,
            scaleY,
            translationX,
            translationY,
            clipPathData
        )
        currentGroup.addNode(group)
        nodes.add(group)
        return this
    }

    /**
     * Pops the topmost VectorGroup from this VectorAssetBuilder. This is used to indicate
     * that no additional VectorAsset nodes will be added to the current VectorGroup
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun popGroup(): VectorAssetBuilder {
        ensureNotConsumed()
        nodes.pop()
        return this
    }

    /**
     * Add a path to the VectorAsset graphic. This represents a leaf node in the VectorAsset graphics
     * tree structure
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun addPath(
        pathData: List<PathNode>,
        name: String = DefaultPathName,
        fill: Brush? = null,
        fillAlpha: Float = DefaultAlpha,
        stroke: Brush? = null,
        strokeAlpha: Float = DefaultAlpha,
        strokeLineWidth: Float = DefaultStrokeLineWidth,
        strokeLineCap: StrokeCap = DefaultStrokeLineCap,
        strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
        strokeLineMiter: Float = DefaultStrokeLineMiter
    ): VectorAssetBuilder {
        ensureNotConsumed()
        currentGroup.addNode(
            VectorPath(
                name,
                pathData,
                fill,
                fillAlpha,
                stroke,
                strokeAlpha,
                strokeLineWidth,
                strokeLineCap,
                strokeLineJoin,
                strokeLineMiter
            )
        )
        return this
    }

    /**
     * Construct a VectorAsset. This concludes the creation process of a VectorAsset graphic
     * This builder cannot be re-used to create additional VectorAsset instances
     * @return Thew newly created VectorAsset instance
     */
    fun build(): VectorAsset {
        ensureNotConsumed()
        val vectorImage = VectorAsset(
            name,
            defaultWidth,
            defaultHeight,
            viewportWidth,
            viewportHeight,
            root
        )

        // reset state in case this builder is used again
        nodes.clear()
        root = VectorGroup()
        nodes.add(root)

        isConsumed = true

        return vectorImage
    }

    /**
     * Throws IllegalStateException if the VectorAssetBuilder is already been consumed
     */
    fun ensureNotConsumed() {
        if (isConsumed) {
            throw IllegalStateException("VectorAssetBuilder is single use, create " +
                    "a new instance to create a new VectorAsset")
        }
    }
}

sealed class VectorNode

/**
 * Vector graphics object that is generated as a result of [VectorAssetBuilder]]
 * It can be composed and rendered by passing it as an argument to [DrawVector]
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

/**
 * Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * The transformations are applied in the order of scale, rotate then translate.
 */
data class VectorGroup(
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
@Composable
fun DrawVector(
    vectorImage: VectorAsset,
    tintColor: Color = Color.Transparent,
    tintBlendMode: BlendMode = DefaultTintBlendMode,
    alignment: Alignment = Alignment.Center,
    fit: ScaleFit = ScaleFit.Fit
) {
    DrawVector(
        name = vectorImage.name,
        viewportWidth = vectorImage.viewportWidth,
        viewportHeight = vectorImage.viewportHeight,
        defaultWidth = vectorImage.defaultWidth,
        defaultHeight = vectorImage.defaultHeight,
        tintColor = tintColor,
        tintBlendMode = tintBlendMode,
        alignment = alignment,
        scaleFit = fit
    ) { _, _ ->
        RenderVectorGroup(group = vectorImage.root)
    }
}

/**
 * Recursive method for creating the vector graphic composition by traversing
 * the tree structure
 */
@Composable
private fun VectorScope.RenderVectorGroup(group: VectorGroup) {
    for (vectorNode in group) {
        if (vectorNode is VectorPath) {
            Path(
                pathData = vectorNode.pathData,
                name = vectorNode.name,
                fill = vectorNode.fill,
                fillAlpha = vectorNode.fillAlpha,
                stroke = vectorNode.stroke,
                strokeAlpha = vectorNode.strokeAlpha,
                strokeLineWidth = vectorNode.strokeLineWidth,
                strokeLineCap = vectorNode.strokeLineCap,
                strokeLineJoin = vectorNode.strokeLineJoin,
                strokeLineMiter = vectorNode.strokeLineMiter
            )
        } else if (vectorNode is VectorGroup) {
            Group(
                name = vectorNode.name,
                rotation = vectorNode.rotation,
                scaleX = vectorNode.scaleX,
                scaleY = vectorNode.scaleY,
                translationX = vectorNode.translationX,
                translationY = vectorNode.translationY,
                pivotX = vectorNode.pivotX,
                pivotY = vectorNode.pivotY,
                clipPathData = vectorNode.clipPathData
            ) {
                RenderVectorGroup(group = vectorNode)
            }
        }
    }
}