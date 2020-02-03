/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.ui.graphics.Brush
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
