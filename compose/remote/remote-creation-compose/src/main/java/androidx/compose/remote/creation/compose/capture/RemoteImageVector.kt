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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathBuilder
import androidx.compose.remote.creation.compose.vector.RemotePathData
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.VectorGroup

/**
 * A base class for defining vector graphics that can be drawn in a remote compose. It could be
 * rendered by passing it as an argument to
 * [androidx.compose.remote.creation.compose.vector.painterRemoteVector]
 */
public class RemoteImageVector
/**
 * Create a RemoteImageVector.
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
internal constructor(
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
    public class Builder(

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

        /** Name of the vector asset */
        private val name: String = DefaultGroupName,

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
        internal fun addGroup(
            name: String = DefaultGroupName,
            rotate: RemoteFloat = DefaultRotation,
            pivotX: RemoteFloat = DefaultPivotX,
            pivotY: RemoteFloat = DefaultPivotY,
            scaleX: RemoteFloat = DefaultScaleX,
            scaleY: RemoteFloat = DefaultScaleY,
            translationX: RemoteFloat = DefaultTranslationX,
            translationY: RemoteFloat = DefaultTranslationY,
            clipPathData: List<RemotePathNode> = EmptyPath,
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
        internal fun clearGroup(): Builder {
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
        internal fun addPath(
            pathData: List<RemotePathNode>,
            pathFillType: PathFillType = DefaultFillType,
            name: String = DefaultPathName,
            fill: Brush? = null,
            fillAlpha: RemoteFloat = 1.0f.rf,
            stroke: Brush? = null,
            strokeAlpha: RemoteFloat = 1.0f.rf,
            strokeLineWidth: RemoteFloat = DefaultStrokeLineWidth,
            strokeLineCap: StrokeCap = DefaultStrokeLineCap,
            strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
            strokeLineMiter: RemoteFloat = DefaultStrokeLineMiter,
            trimPathStart: RemoteFloat = DefaultTrimPathStart,
            trimPathEnd: RemoteFloat = DefaultTrimPathEnd,
            trimPathOffset: RemoteFloat = DefaultTrimPathOffset,
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
            var rotate: RemoteFloat = DefaultRotation,
            var pivotX: RemoteFloat = DefaultPivotX,
            var pivotY: RemoteFloat = DefaultPivotY,
            var scaleX: RemoteFloat = DefaultScaleX,
            var scaleY: RemoteFloat = DefaultScaleY,
            var translationX: RemoteFloat = DefaultTranslationX,
            var translationY: RemoteFloat = DefaultTranslationY,
            var clipPathData: List<RemotePathNode> = EmptyPath,
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
    val rotation: RemoteFloat = DefaultRotation,

    /** X coordinate of the pivot point to rotate or scale the group */
    val pivotX: RemoteFloat = DefaultPivotX,

    /** Y coordinate of the pivot point to rotate or scale the group */
    val pivotY: RemoteFloat = DefaultPivotY,

    /** Scale factor in the X-axis to apply to the group */
    val scaleX: RemoteFloat = DefaultScaleX,

    /** Scale factor in the Y-axis to apply to the group */
    val scaleY: RemoteFloat = DefaultScaleY,

    /** Translation in virtual pixels to apply along the x-axis */
    val translationX: RemoteFloat = DefaultTranslationX,

    /** Translation in virtual pixels to apply along the y-axis */
    val translationY: RemoteFloat = DefaultTranslationY,

    /** Path information used to clip the content within the group */
    val clipPathData: List<RemotePathNode> = EmptyPath,

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
    val pathData: List<RemotePathNode>,

    /** Rule to determine how the interior of the path is to be calculated */
    val pathFillType: PathFillType,

    /** Specifies the color or gradient used to fill the path */
    val fill: Brush? = null,

    /** Opacity to fill the path */
    val fillAlpha: RemoteFloat = 1.0f.rf,

    /** Specifies the color or gradient used to fill the stroke */
    val stroke: Brush? = null,

    /** Opacity to stroke the path */
    val strokeAlpha: RemoteFloat = 1.0f.rf,

    /** Width of the line to stroke the path */
    val strokeLineWidth: RemoteFloat = DefaultStrokeLineWidth,

    /**
     * Specifies the linecap for a stroked path, either butt, round, or square. The default is butt.
     */
    val strokeLineCap: StrokeCap = DefaultStrokeLineCap,

    /**
     * Specifies the linejoin for a stroked path, either miter, round or bevel. The default is miter
     */
    val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,

    /** Specifies the miter limit for a stroked path, the default is 4 */
    val strokeLineMiter: RemoteFloat = DefaultStrokeLineMiter,

    /**
     * Specifies the fraction of the path to trim from the start, in the range from 0 to 1. The
     * default is 0.
     */
    val trimPathStart: RemoteFloat = DefaultTrimPathStart,

    /**
     * Specifies the fraction of the path to trim from the end, in the range from 0 to 1. The
     * default is 1.
     */
    val trimPathEnd: RemoteFloat = DefaultTrimPathEnd,

    /**
     * Specifies the offset of the trim region (allows showed region to include the start and end),
     * in the range from 0 to 1. The default is 0.
     */
    val trimPathOffset: RemoteFloat = DefaultTrimPathOffset,
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
 * @param pathBuilder [RemotePathBuilder] lambda for adding [RemotePathNode]s to this path.
 */
public fun RemoteImageVector.Builder.path(
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: RemoteFloat = 1.0f.rf,
    stroke: Brush? = null,
    strokeAlpha: RemoteFloat = 1.0f.rf,
    strokeLineWidth: RemoteFloat = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: RemoteFloat = DefaultStrokeLineMiter,
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteImageVector.Builder.group(
    name: String = DefaultGroupName,
    rotate: RemoteFloat = DefaultRotation,
    pivotX: RemoteFloat = DefaultPivotX,
    pivotY: RemoteFloat = DefaultPivotY,
    scaleX: RemoteFloat = DefaultScaleX,
    scaleY: RemoteFloat = DefaultScaleY,
    translationX: RemoteFloat = DefaultTranslationX,
    translationY: RemoteFloat = DefaultTranslationY,
    clipPathData: List<RemotePathNode> = EmptyPath,
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

internal const val DefaultGroupName = ""
internal val DefaultRotation = 0.0f.rf
internal val DefaultPivotX = 0.0f.rf
internal val DefaultPivotY = 0.0f.rf
internal val DefaultScaleX = 1.0f.rf
internal val DefaultScaleY = 1.0f.rf
internal val DefaultTranslationX = 0.0f.rf
internal val DefaultTranslationY = 0.0f.rf

internal val EmptyPath = emptyList<RemotePathNode>()

internal const val DefaultPathName = ""
internal val DefaultStrokeLineWidth = 0.0f.rf
internal val DefaultStrokeLineMiter = 4.0f.rf
internal val DefaultTrimPathStart = 0.0f.rf
internal val DefaultTrimPathEnd = 1.0f.rf
internal val DefaultTrimPathOffset = 0.0f.rf

internal val DefaultStrokeLineCap = StrokeCap.Butt
internal val DefaultStrokeLineJoin = StrokeJoin.Miter
internal val DefaultTintBlendMode = BlendMode.SrcIn
internal val DefaultTintColor = Color.Transparent
internal val DefaultFillType = PathFillType.NonZero
