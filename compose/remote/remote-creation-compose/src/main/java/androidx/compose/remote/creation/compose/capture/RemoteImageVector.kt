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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

import android.graphics.Path
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.vector.RemotePathBuilder
import androidx.compose.remote.creation.compose.vector.RemotePathData
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.DefaultGroupName
import androidx.compose.ui.graphics.vector.DefaultPathName
import androidx.compose.ui.graphics.vector.DefaultPivotX
import androidx.compose.ui.graphics.vector.DefaultPivotY
import androidx.compose.ui.graphics.vector.DefaultRotation
import androidx.compose.ui.graphics.vector.DefaultScaleX
import androidx.compose.ui.graphics.vector.DefaultScaleY
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.graphics.vector.DefaultStrokeLineJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.graphics.vector.DefaultStrokeLineWidth
import androidx.compose.ui.graphics.vector.DefaultTranslationX
import androidx.compose.ui.graphics.vector.DefaultTranslationY
import androidx.compose.ui.graphics.vector.DefaultTrimPathEnd
import androidx.compose.ui.graphics.vector.DefaultTrimPathOffset
import androidx.compose.ui.graphics.vector.DefaultTrimPathStart
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup

/**
 * A base class for defining vector graphics that can be drawn in a remote compose. It could be
 * rendered by passing it as an argument to
 * [androidx.compose.remote.creation.compose.vector.painterRemoteVector]
 *
 * @param name Name of the Vector asset
 * @param viewportWidth Used to define the height of the viewport space. Viewport is basically the
 *   virtual canvas where the paths are drawn on.
 * @param viewportHeight Used to define the width of the viewport space. Viewport is basically the
 *   virtual canvas where the paths are drawn on.
 * @param root Root group of the vector asset that contains all the child groups and paths
 * @param tintColor Optional tint color to be applied to the vector graphic
 * @param tintBlendMode Blend mode used to apply [tintColor]
 * @param autoMirror Determines if the vector asset should automatically be mirrored for right to
 *   left locales
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteImageVector(
    internal val name: String,
    internal val viewportWidth: RemoteFloat,
    internal val viewportHeight: RemoteFloat,
    internal val tintColor: RemoteColor = RemoteColor(Color.Black),
    internal val tintBlendMode: BlendMode = BlendMode.SrcIn,
    internal val autoMirror: Boolean = false,
) {
    internal lateinit var root: RemoteVectorGroup

    /**
     * Builder used to construct a Vector graphic tree. This is useful for caching the result of
     * expensive operations used to construct a vector graphic for compose. For example, the vector
     * graphic could be serialized and downloaded from a server and represented internally in a
     * ImageVector before it is composed through The generated ImageVector is recommended to be
     * memoized across composition calls to avoid doing redundant work
     */
    @Suppress("MissingGetterMatchingBuilder")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Builder(

        /** Name of the vector asset */
        private val name: String = DefaultGroupName,

        /**
         * Used to define the width of the viewport space. Viewport is basically the virtual canvas
         * where the paths are drawn on.
         */
        private val viewportWidth: RemoteFloat,

        /**
         * Used to define the height of the viewport space. Viewport is basically the virtual canvas
         * where the paths are drawn on.
         */
        private val viewportHeight: RemoteFloat,

        /** Optional color used to tint the entire vector image */
        private val tintColor: RemoteColor,

        /** Blend mode used to apply the tint color */
        private val tintBlendMode: BlendMode = BlendMode.SrcIn,

        /**
         * Determines if the vector asset should automatically be mirrored for right to left locales
         */
        private val autoMirror: Boolean = false,
    ) {

        private val nodes = ArrayList<RemoteGroupParams>()

        private var root = RemoteGroupParams()
        private var isConsumed = false

        private val currentGroup: RemoteGroupParams
            get() = nodes.peek()

        init {
            nodes.push(root)
        }

        /**
         * Create a new group and push it to the front of the stack of ImageVector nodes
         *
         * @param name the name of the group
         * @param rotate the rotation of the group in degrees
         * @param pivotX the x coordinate of the pivot point to rotate or scale the group
         * @param pivotY the y coordinate of the pivot point to rotate or scale the group
         * @param scaleX the scale factor in the X-axis to apply to the group
         * @param scaleY the scale factor in the Y-axis to apply to the group
         * @param translationX the translation in virtual pixels to apply along the x-axis
         * @param translationY the translation in virtual pixels to apply along the y-axis
         * @param clipPathData the path information used to clip the content within the group
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun addGroup(
            name: String = DefaultGroupName,
            rotate: Float = DefaultRotation,
            pivotX: Float = DefaultPivotX,
            pivotY: Float = DefaultPivotY,
            scaleX: Float = DefaultScaleX,
            scaleY: Float = DefaultScaleY,
            translationX: Float = DefaultTranslationX,
            translationY: Float = DefaultTranslationY,
            clipPathData: List<PathNode> = EmptyPath,
        ): Builder {
            ensureNotConsumed()
            val group =
                RemoteGroupParams(
                    name,
                    rotate,
                    pivotX,
                    pivotY,
                    scaleX,
                    scaleY,
                    translationX,
                    translationY,
                    clipPathData,
                )
            nodes.push(group)
            return this
        }

        /**
         * Pops the topmost VectorGroup from this ImageVector.Builder. This is used to indicate that
         * no additional ImageVector nodes will be added to the current VectorGroup
         *
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun clearGroup(): Builder {
            ensureNotConsumed()
            val popped = nodes.pop()
            currentGroup.children.add(popped.asVectorGroup())
            return this
        }

        /**
         * Add a path to the ImageVector graphic. This represents a leaf node in the ImageVector
         * graphics tree structure
         *
         * @param pathData path information to render the shape of the path
         * @param pathFillType rule to determine how the interior of the path is to be calculated
         * @param name the name of the path
         * @param fill specifies the [Brush] used to fill the path
         * @param fillAlpha the alpha to fill the path
         * @param stroke specifies the [Brush] used to fill the stroke
         * @param strokeAlpha the alpha to stroke the path
         * @param strokeLineWidth the width of the line to stroke the path
         * @param strokeLineCap specifies the linecap for a stroked path
         * @param strokeLineJoin specifies the linejoin for a stroked path
         * @param strokeLineMiter specifies the miter limit for a stroked path
         * @param trimPathStart specifies the fraction of the path to trim from the start in the
         *   range from 0 to 1. Values outside the range will wrap around the length of the path.
         *   Default is 0.
         * @param trimPathEnd specifies the fraction of the path to trim from the end in the range
         *   from 0 to 1. Values outside the range will wrap around the length of the path. Default
         *   is 1.
         * @param trimPathOffset specifies the fraction to shift the path trim region in the range
         *   from 0 to 1. Values outside the range will wrap around the length of the path. Default
         *   is 0.
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        @Suppress("MissingGetterMatchingBuilder")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun addPath(
            pathData: List<PathNode>,
            pathFillType: PathFillType = DefaultFillType,
            name: String = DefaultPathName,
            fill: Brush? = null,
            fillAlpha: Float = 1.0f,
            stroke: Brush? = null,
            strokeAlpha: Float = 1.0f,
            strokeLineWidth: Float = DefaultStrokeLineWidth,
            strokeLineCap: StrokeCap = DefaultStrokeLineCap,
            strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
            strokeLineMiter: Float = DefaultStrokeLineMiter,
            trimPathStart: Float = DefaultTrimPathStart,
            trimPathEnd: Float = DefaultTrimPathEnd,
            trimPathOffset: Float = DefaultTrimPathOffset,
        ): Builder {
            ensureNotConsumed()
            currentGroup.children.add(
                RemoteVectorPath(
                    name,
                    pathData,
                    pathFillType,
                    fill,
                    fillAlpha,
                    stroke,
                    strokeAlpha,
                    strokeLineWidth,
                    strokeLineCap,
                    strokeLineJoin,
                    strokeLineMiter,
                    trimPathStart,
                    trimPathEnd,
                    trimPathOffset,
                )
            )
            return this
        }

        /**
         * Construct a ImageVector. This concludes the creation process of a ImageVector graphic
         * This builder cannot be re-used to create additional ImageVector instances
         *
         * @return The newly created ImageVector instance
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun build(): RemoteImageVector {
            ensureNotConsumed()
            // pop all groups except for the root
            while (nodes.size > 1) {
                clearGroup()
            }

            val vectorImage =
                RemoteImageVector(
                        name = name,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        tintColor = tintColor,
                        tintBlendMode = tintBlendMode,
                        autoMirror = autoMirror,
                    )
                    .apply { root = this@Builder.root.asVectorGroup() }

            isConsumed = true

            return vectorImage
        }

        /** Throws IllegalStateException if the ImageVector.Builder has already been consumed */
        private fun ensureNotConsumed() {
            if (isConsumed) {
                throw IllegalStateException(
                    "RemoteImageVector.Builder is single use, create a new instance " +
                        "to create a new ImageVector"
                )
            }
        }

        /**
         * Helper method to create an immutable VectorGroup object from an set of GroupParams which
         * represent a group that is in the middle of being constructed
         */
        private fun RemoteGroupParams.asVectorGroup(): RemoteVectorGroup =
            RemoteVectorGroup(
                name,
                rotate,
                pivotX,
                pivotY,
                scaleX,
                scaleY,
                translationX,
                translationY,
                clipPathData,
                children,
            )

        /**
         * Internal helper class to help assist with in progress creation of a vector group before
         * creating the immutable result
         */
        private class RemoteGroupParams(
            var name: String = DefaultGroupName,
            var rotate: Float = DefaultRotation,
            var pivotX: Float = DefaultPivotX,
            var pivotY: Float = DefaultPivotY,
            var scaleX: Float = DefaultScaleX,
            var scaleY: Float = DefaultScaleY,
            var translationX: Float = DefaultTranslationX,
            var translationY: Float = DefaultTranslationY,
            var clipPathData: List<PathNode> = EmptyPath,
            var children: MutableList<RemoteVectorNode> = mutableListOf(),
        )
    }
}

internal sealed class RemoteVectorNode

/**
 * Defines a group of paths or subgroups, plus transformation information. The transformations are
 * defined in the same coordinates as the viewport. The transformations are applied in the order of
 * scale, rotate then translate.
 */
internal class RemoteVectorGroup
internal constructor(
    /** Name of the corresponding group */
    val name: String = DefaultGroupName,

    /** Rotation of the group in degrees */
    val rotation: Float = DefaultRotation,

    /** X coordinate of the pivot point to rotate or scale the group */
    val pivotX: Float = DefaultPivotX,

    /** Y coordinate of the pivot point to rotate or scale the group */
    val pivotY: Float = DefaultPivotY,

    /** Scale factor in the X-axis to apply to the group */
    val scaleX: Float = DefaultScaleX,

    /** Scale factor in the Y-axis to apply to the group */
    val scaleY: Float = DefaultScaleY,

    /** Translation in virtual pixels to apply along the x-axis */
    val translationX: Float = DefaultTranslationX,

    /** Translation in virtual pixels to apply along the y-axis */
    val translationY: Float = DefaultTranslationY,

    /** Path information used to clip the content within the group */
    val clipPathData: List<PathNode> = EmptyPath,

    /** Child Vector nodes that are part of this group, this can contain paths or other groups */
    private val children: List<RemoteVectorNode> = emptyList(),
) : RemoteVectorNode(), Iterable<RemoteVectorNode> {

    val size: Int
        get() = children.size

    operator fun get(index: Int): RemoteVectorNode {
        return children[index]
    }

    override fun iterator(): Iterator<RemoteVectorNode> {
        return object : Iterator<RemoteVectorNode> {

            val it = children.iterator()

            override fun hasNext(): Boolean = it.hasNext()

            override fun next(): RemoteVectorNode = it.next()
        }
    }
}

/**
 * Leaf node of a Vector graphics tree. This specifies a path shape and parameters to color and
 * style the shape itself
 */
internal class RemoteVectorPath
internal constructor(
    /** Name of the corresponding path */
    val name: String = DefaultPathName,

    /** Path information to render the shape of the path */
    val pathData: List<PathNode>,

    /** Rule to determine how the interior of the path is to be calculated */
    val pathFillType: PathFillType,

    /** Specifies the color or gradient used to fill the path */
    val fill: Brush? = null,

    /** Opacity to fill the path */
    val fillAlpha: Float = 1.0f,

    /** Specifies the color or gradient used to fill the stroke */
    val stroke: Brush? = null,

    /** Opacity to stroke the path */
    val strokeAlpha: Float = 1.0f,

    /** Width of the line to stroke the path */
    val strokeLineWidth: Float = DefaultStrokeLineWidth,

    /**
     * Specifies the linecap for a stroked path, either butt, round, or square. The default is butt.
     */
    val strokeLineCap: StrokeCap = DefaultStrokeLineCap,

    /**
     * Specifies the linejoin for a stroked path, either miter, round or bevel. The default is miter
     */
    val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,

    /** Specifies the miter limit for a stroked path, the default is 4 */
    val strokeLineMiter: Float = DefaultStrokeLineMiter,

    /**
     * Specifies the fraction of the path to trim from the start, in the range from 0 to 1. The
     * default is 0.
     */
    val trimPathStart: Float = DefaultTrimPathStart,

    /**
     * Specifies the fraction of the path to trim from the end, in the range from 0 to 1. The
     * default is 1.
     */
    val trimPathEnd: Float = DefaultTrimPathEnd,

    /**
     * Specifies the offset of the trim region (allows showed region to include the start and end),
     * in the range from 0 to 1. The default is 0.
     */
    val trimPathOffset: Float = DefaultTrimPathOffset,
) : RemoteVectorNode() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RemoteVectorPath

        if (name != other.name) return false
        if (fill != other.fill) return false
        if (fillAlpha != other.fillAlpha) return false
        if (stroke != other.stroke) return false
        if (strokeAlpha != other.strokeAlpha) return false
        if (strokeLineWidth != other.strokeLineWidth) return false
        if (strokeLineCap != other.strokeLineCap) return false
        if (strokeLineJoin != other.strokeLineJoin) return false
        if (strokeLineMiter != other.strokeLineMiter) return false
        if (trimPathStart != other.trimPathStart) return false
        if (trimPathEnd != other.trimPathEnd) return false
        if (trimPathOffset != other.trimPathOffset) return false
        if (pathFillType != other.pathFillType) return false
        if (pathData != other.pathData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + pathData.hashCode()
        result = 31 * result + (fill?.hashCode() ?: 0)
        result = 31 * result + fillAlpha.hashCode()
        result = 31 * result + (stroke?.hashCode() ?: 0)
        result = 31 * result + strokeAlpha.hashCode()
        result = 31 * result + strokeLineWidth.hashCode()
        result = 31 * result + strokeLineCap.hashCode()
        result = 31 * result + strokeLineJoin.hashCode()
        result = 31 * result + strokeLineMiter.hashCode()
        result = 31 * result + trimPathStart.hashCode()
        result = 31 * result + trimPathEnd.hashCode()
        result = 31 * result + trimPathOffset.hashCode()
        result = 31 * result + pathFillType.hashCode()
        return result
    }
}

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
 * @param pathFillType specifies the winding rule that decides how the interior of a [Path] is
 *   calculated.
 * @param pathBuilder [RemotePathBuilder] lambda for adding [PathNode]s to this path.
 */
public inline fun RemoteImageVector.Builder.path(
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: Float = 1.0f,
    stroke: Brush? = null,
    strokeAlpha: Float = 1.0f,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: RemotePathBuilder.() -> Unit,
): RemoteImageVector.Builder =
    addPath(
        RemotePathData(pathBuilder),
        pathFillType,
        name,
        fill,
        fillAlpha,
        stroke,
        strokeAlpha,
        strokeLineWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeLineMiter,
    )

/**
 * DSL extension for adding a [VectorGroup] to [this].
 *
 * @param name the name of the group
 * @param rotate the rotation of the group in degrees
 * @param pivotX the x coordinate of the pivot point to rotate or scale the group
 * @param pivotY the y coordinate of the pivot point to rotate or scale the group
 * @param scaleX the scale factor in the X-axis to apply to the group
 * @param scaleY the scale factor in the Y-axis to apply to the group
 * @param translationX the translation in virtual pixels to apply along the x-axis
 * @param translationY the translation in virtual pixels to apply along the y-axis
 * @param clipPathData the path information used to clip the content within the group
 * @param block builder lambda to add children to this group
 */
public inline fun RemoteImageVector.Builder.group(
    name: String = DefaultGroupName,
    rotate: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    block: RemoteImageVector.Builder.() -> Unit,
): RemoteImageVector.Builder = apply {
    addGroup(name, rotate, pivotX, pivotY, scaleX, scaleY, translationX, translationY, clipPathData)
    block()
    clearGroup()
}

private fun <T> ArrayList<T>.push(value: T): Boolean = add(value)

private fun <T> ArrayList<T>.pop(): T = this.removeAt(size - 1)

private fun <T> ArrayList<T>.peek(): T = this[size - 1]

/** For image vectors that don't have an intrinsic size. */
internal const val DefaultIconSize = 24f
