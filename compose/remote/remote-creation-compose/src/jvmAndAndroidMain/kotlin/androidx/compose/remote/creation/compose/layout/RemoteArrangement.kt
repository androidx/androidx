/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

private const val LEFT = 101
private const val CENTER = 102
private const val RIGHT = 103

/**
 * In remote-compose, an arrangement is a contract for how to lay out children in a container that
 * allows for more than one child. This is a mirror of
 * [androidx.compose.foundation.layout.Arrangement]
 */
@Immutable
public object RemoteArrangement {

    /** A contract for laying out children horizontally. */
    @Stable public sealed interface Horizontal

    /** A contract for laying out children vertically. */
    @Stable public sealed interface Vertical

    /** A contract for laying out children horizontally or vertically. */
    @Stable public sealed interface HorizontalOrVertical : Horizontal, Vertical

    /**
     * Place children horizontally such that they are as close as possible to the start of the main
     * axis.
     */
    public val Start: RemoteArrangement.Horizontal = HorizontalArrangement(ColumnLayout.START)
    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     */
    public val End: RemoteArrangement.Horizontal = HorizontalArrangement(ColumnLayout.END)
    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     */
    public val Top: RemoteArrangement.Vertical = VerticalArrangement(ColumnLayout.TOP)
    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     */
    public val Bottom: RemoteArrangement.Vertical = VerticalArrangement(ColumnLayout.BOTTOM)
    /** Place children vertically such that they are centered on the main axis. */
    public val Center: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(ColumnLayout.CENTER)

    /** Place children with equal space between them, but not on the edges. */
    public val SpaceEvenly: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(ColumnLayout.SPACE_EVENLY)
    /** Place children with equal space between them, including the edges. */
    public val SpaceBetween: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(ColumnLayout.SPACE_BETWEEN)
    /** Place children with equal space around them. */
    public val SpaceAround: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(ColumnLayout.SPACE_AROUND)

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children can
     * occupy.
     *
     * @param space The space between adjacent children.
     * @return a [RemoteArrangement.HorizontalOrVertical] that spaces adjacent children by [space]
     */
    public fun spacedBy(space: RemoteDp): RemoteArrangement.HorizontalOrVertical =
        RemoteSpacedArrangement(space)

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children can
     * occupy.
     *
     * @param space The space between adjacent children.
     * @return a [RemoteArrangement.HorizontalOrVertical] that spaces adjacent children by [space]
     */
    public fun spacedBy(space: RemoteFloat): RemoteArrangement.HorizontalOrVertical =
        RemoteFloatSpacedArrangement(space)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children can
     * occupy. An [alignment] can be specified to align the spaced children horizontally inside the
     * parent, in case there is empty width remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     * @return a [RemoteArrangement.Horizontal] that spaces adjacent children by [space] aligned by
     *   [alignment]
     */
    public fun spacedBy(
        space: RemoteDp,
        alignment: RemoteAlignment.Horizontal,
    ): RemoteArrangement.Horizontal = RemoteSpacedHorizontalArrangement(space, alignment)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children can
     * occupy. An [alignment] can be specified to align the spaced children horizontally inside the
     * parent, in case there is empty width remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     * @return a [RemoteArrangement.Horizontal] that spaces adjacent children by [space] aligned by
     *   [alignment]
     */
    public fun spacedBy(
        space: RemoteFloat,
        alignment: RemoteAlignment.Horizontal,
    ): RemoteArrangement.Horizontal = RemoteFloatSpacedHorizontalArrangement(space, alignment)

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children can
     * occupy. An [alignment] can be specified to align the spaced children vertically inside the
     * parent, in case there is empty height remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     * @return a [RemoteArrangement.Vertical] that spaces adjacent children by [space] aligned by
     *   [alignment]
     */
    public fun spacedBy(
        space: RemoteDp,
        alignment: RemoteAlignment.Vertical,
    ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space, alignment)

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children can
     * occupy. An [alignment] can be specified to align the spaced children vertically inside the
     * parent, in case there is empty height remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     * @return a [RemoteArrangement.Vertical] that spaces adjacent children by [space] aligned by
     *   [alignment]
     */
    public fun spacedBy(
        space: RemoteFloat,
        alignment: RemoteAlignment.Vertical,
    ): RemoteArrangement.Vertical = RemoteFloatSpacedVerticalArrangement(space, alignment)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public object Absolute {
        /**
         * Place children horizontally such that they are as close as possible to the left edge of
         * the [RemoteRow].
         */
        public val Left: RemoteArrangement.Horizontal = HorizontalArrangement(LEFT)

        /**
         * Place children such that they are as close as possible to the middle of the [RemoteRow].
         */
        public val Center: RemoteArrangement.Horizontal = HorizontalArrangement(CENTER)

        /**
         * Place children horizontally such that they are as close as possible to the right edge of
         * the [RemoteRow].
         */
        public val Right: RemoteArrangement.Horizontal = HorizontalArrangement(RIGHT)

        /**
         * Place children such that they are spaced evenly across the main axis, without free space
         * before the first child or after the last child.
         */
        public val SpaceBetween: RemoteArrangement.Horizontal =
            HorizontalArrangement(ColumnLayout.SPACE_BETWEEN)

        /**
         * Place children such that they are spaced evenly across the main axis, including free
         * space before the first child and after the last child.
         */
        public val SpaceEvenly: RemoteArrangement.Horizontal =
            HorizontalArrangement(ColumnLayout.SPACE_EVENLY)

        /**
         * Place children such that they are spaced evenly horizontally, including free space before
         * the first child and after the last child, but half the amount of space existing otherwise
         * between two consecutive children.
         */
        public val SpaceAround: RemoteArrangement.Horizontal =
            HorizontalArrangement(ColumnLayout.SPACE_AROUND)

        /**
         * Place children such that each two adjacent ones are spaced by a fixed [space] distance
         * across the main axis. The spacing will be subtracted from the available space that the
         * children can occupy.
         *
         * @param space The space between adjacent children.
         * @return a [RemoteArrangement.HorizontalOrVertical] that spaces adjacent children by
         *   [space]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(space: RemoteDp): RemoteArrangement.HorizontalOrVertical =
            RemoteSpacedArrangement(space)

        /**
         * Place children such that each two adjacent ones are spaced by a fixed [space] distance
         * across the main axis. The spacing will be subtracted from the available space that the
         * children can occupy.
         *
         * @param space The space between adjacent children.
         * @return a [RemoteArrangement.HorizontalOrVertical] that spaces adjacent children by
         *   [space]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(space: RemoteFloat): RemoteArrangement.HorizontalOrVertical =
            RemoteFloatSpacedArrangement(space)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         * @return a [RemoteArrangement.Horizontal] that spaces adjacent children by [space],
         *   aligned by [alignment]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteDp,
            alignment: RemoteAlignment.Horizontal,
        ): RemoteArrangement.Horizontal =
            RemoteSpacedAbsoluteHorizontalArrangement(space, alignment)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         * @return a [RemoteArrangement.Horizontal] that spaces adjacent children by [space],
         *   aligned by [alignment]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteFloat,
            alignment: RemoteAlignment.Horizontal,
        ): RemoteArrangement.Horizontal =
            RemoteFloatSpacedAbsoluteHorizontalArrangement(space, alignment)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         * @return a [RemoteArrangement.Vertical] that spaces adjacent children by [space] aligned
         *   by [alignment]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteDp,
            alignment: RemoteAlignment.Vertical,
        ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space, alignment)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         * @return a [RemoteArrangement.Vertical] that spaces adjacent children by [space] aligned
         *   by [alignment]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteFloat,
            alignment: RemoteAlignment.Vertical,
        ): RemoteArrangement.Vertical = RemoteFloatSpacedVerticalArrangement(space, alignment)
    }
}

internal fun RemoteArrangement.Horizontal.toRemote(layoutDirection: RemoteLayoutDirection): Int =
    when (this) {
        is HorizontalArrangement -> toRemote(layoutDirection)
        is HorizontalOrVerticalArrangement -> toRemote(layoutDirection)
        is RemoteSpacedArrangement -> toRemoteHorizontal()
        is RemoteFloatSpacedArrangement -> toRemoteHorizontal()
        is RemoteSpacedHorizontalArrangement -> toRemote(layoutDirection)
        is RemoteFloatSpacedHorizontalArrangement -> toRemote(layoutDirection)
        is RemoteSpacedAbsoluteHorizontalArrangement -> toRemote(layoutDirection)
        is RemoteFloatSpacedAbsoluteHorizontalArrangement -> toRemote(layoutDirection)
    }

internal fun RemoteArrangement.Vertical.toRemote(): Int =
    when (this) {
        is VerticalArrangement -> toRemote()
        is HorizontalOrVerticalArrangement -> toRemote()
        is RemoteSpacedArrangement -> toRemoteVertical()
        is RemoteFloatSpacedArrangement -> toRemoteVertical()
        is RemoteSpacedVerticalArrangement -> toRemote()
        is RemoteFloatSpacedVerticalArrangement -> toRemote()
    }

internal data class HorizontalArrangement(val type: Int) : RemoteArrangement.Horizontal {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int =
        when (type) {
            ColumnLayout.START ->
                if (layoutDirection == RemoteLayoutDirection.Ltr) ColumnLayout.START
                else ColumnLayout.END
            ColumnLayout.CENTER -> ColumnLayout.CENTER
            ColumnLayout.END ->
                if (layoutDirection == RemoteLayoutDirection.Ltr) ColumnLayout.END
                else ColumnLayout.START
            ColumnLayout.SPACE_BETWEEN -> ColumnLayout.SPACE_BETWEEN
            ColumnLayout.SPACE_EVENLY -> ColumnLayout.SPACE_EVENLY
            ColumnLayout.SPACE_AROUND -> ColumnLayout.SPACE_AROUND
            LEFT -> ColumnLayout.START
            CENTER -> ColumnLayout.CENTER
            RIGHT -> ColumnLayout.END
            else ->
                if (layoutDirection == RemoteLayoutDirection.Ltr) ColumnLayout.START
                else ColumnLayout.END
        }

    internal fun isAbsolute() = type == LEFT || type == CENTER || type == RIGHT
}

internal data class VerticalArrangement(val type: Int) : RemoteArrangement.Vertical {

    fun toRemote(): Int = type
}

internal data class HorizontalOrVerticalArrangement(val type: Int) :
    RemoteArrangement.HorizontalOrVertical {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int =
        when (type) {
            ColumnLayout.CENTER,
            ColumnLayout.SPACE_BETWEEN,
            ColumnLayout.SPACE_EVENLY,
            ColumnLayout.SPACE_AROUND -> type
            else ->
                if (layoutDirection == RemoteLayoutDirection.Ltr) ColumnLayout.START
                else ColumnLayout.END
        }

    fun toRemote(): Int =
        when (type) {
            ColumnLayout.CENTER,
            ColumnLayout.SPACE_BETWEEN,
            ColumnLayout.SPACE_EVENLY,
            ColumnLayout.SPACE_AROUND -> type
            else -> ColumnLayout.START
        }
}

internal interface RemoteSpaced {
    fun getSpacingFloatId(scope: RemoteStateScope): Float
}

internal data class RemoteSpacedArrangement(val space: RemoteDp) :
    RemoteArrangement.HorizontalOrVertical, RemoteSpaced {

    fun toRemoteHorizontal(): Int = ColumnLayout.START

    fun toRemoteVertical(): Int = ColumnLayout.TOP

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                space.value.floatId
            } else {
                space.toPx().floatId
            }
        }
}

internal data class RemoteFloatSpacedArrangement(val space: RemoteFloat) :
    RemoteArrangement.HorizontalOrVertical, RemoteSpaced {

    fun toRemoteHorizontal(): Int = ColumnLayout.START

    fun toRemoteVertical(): Int = ColumnLayout.TOP

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                (space / remoteDensity.density).floatId
            } else {
                space.floatId
            }
        }
}

internal data class RemoteSpacedHorizontalArrangement(
    val space: RemoteDp,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int = alignment.toRemote(layoutDirection)

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                space.value.floatId
            } else {
                space.toPx().floatId
            }
        }
}

internal data class RemoteFloatSpacedHorizontalArrangement(
    val space: RemoteFloat,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int = alignment.toRemote(layoutDirection)

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                (space / remoteDensity.density).floatId
            } else {
                space.floatId
            }
        }
}

internal data class RemoteSpacedAbsoluteHorizontalArrangement(
    val space: RemoteDp,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int = alignment.toRemote(layoutDirection)

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                space.value.floatId
            } else {
                space.toPx().floatId
            }
        }
}

internal data class RemoteFloatSpacedAbsoluteHorizontalArrangement(
    val space: RemoteFloat,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {

    fun toRemote(layoutDirection: RemoteLayoutDirection): Int = alignment.toRemote(layoutDirection)

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                (space / remoteDensity.density).floatId
            } else {
                space.floatId
            }
        }
}

internal data class RemoteSpacedVerticalArrangement(
    val space: RemoteDp,
    val alignment: RemoteAlignment.Vertical,
) : RemoteArrangement.Vertical, RemoteSpaced {

    fun toRemote(): Int = alignment.toRemote()

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                space.value.floatId
            } else {
                space.toPx().floatId
            }
        }
}

internal data class RemoteFloatSpacedVerticalArrangement(
    val space: RemoteFloat,
    val alignment: RemoteAlignment.Vertical,
) : RemoteArrangement.Vertical, RemoteSpaced {

    fun toRemote(): Int = alignment.toRemote()

    override fun getSpacingFloatId(scope: RemoteStateScope): Float =
        with(scope) {
            if (densityBehavior == RemoteDensityBehavior.Dp) {
                (space / remoteDensity.density).floatId
            } else {
                space.floatId
            }
        }
}
