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
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private const val LEFT = 101
private const val RIGHT = 102

/**
 * In remote-compose, an arrangement is a contract for how to lay out children in a container that
 * allows for more than one child. This is a mirror of
 * [androidx.compose.foundation.layout.Arrangement]
 */
public object RemoteArrangement {

    /** A contract for laying out children horizontally. */
    public sealed interface Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toRemote(layoutDirection: LayoutDirection): Int
    }

    /** A contract for laying out children vertically. */
    public sealed interface Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun toRemote(): Int
    }

    /** A contract for laying out children horizontally or vertically. */
    public sealed interface HorizontalOrVertical : Horizontal, Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toComposeUi():
            androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toRemote(layoutDirection: LayoutDirection): Int

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int
    }

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
     */
    public fun spacedBy(space: RemoteDp): RemoteArrangement.HorizontalOrVertical =
        RemoteSpacedArrangement(space.toPx())

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children can
     * occupy.
     *
     * @param space The space between adjacent children.
     */
    public fun spacedBy(space: RemoteFloat): RemoteArrangement.HorizontalOrVertical =
        RemoteSpacedArrangement(space)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children can
     * occupy. An [alignment] can be specified to align the spaced children horizontally inside the
     * parent, in case there is empty width remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    public fun spacedBy(
        space: RemoteDp,
        alignment: RemoteAlignment.Horizontal,
    ): RemoteArrangement.Horizontal = RemoteSpacedHorizontalArrangement(space.toPx(), alignment)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children can
     * occupy. An [alignment] can be specified to align the spaced children horizontally inside the
     * parent, in case there is empty width remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    public fun spacedBy(
        space: RemoteFloat,
        alignment: RemoteAlignment.Horizontal,
    ): RemoteArrangement.Horizontal = RemoteSpacedHorizontalArrangement(space, alignment)

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children can
     * occupy. An [alignment] can be specified to align the spaced children vertically inside the
     * parent, in case there is empty height remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    public fun spacedBy(
        space: RemoteDp,
        alignment: RemoteAlignment.Vertical,
    ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space.toPx(), alignment)

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children can
     * occupy. An [alignment] can be specified to align the spaced children vertically inside the
     * parent, in case there is empty height remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    public fun spacedBy(
        space: RemoteFloat,
        alignment: RemoteAlignment.Vertical,
    ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space, alignment)

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
        public val Center: RemoteArrangement.Horizontal = HorizontalArrangement(ColumnLayout.CENTER)

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
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(space: RemoteDp): RemoteArrangement.HorizontalOrVertical =
            RemoteSpacedArrangement(space.toPx())

        /**
         * Place children such that each two adjacent ones are spaced by a fixed [space] distance
         * across the main axis. The spacing will be subtracted from the available space that the
         * children can occupy.
         *
         * @param space The space between adjacent children.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(space: RemoteFloat): RemoteArrangement.HorizontalOrVertical =
            RemoteSpacedArrangement(space)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteDp,
            alignment: RemoteAlignment.Horizontal,
        ): RemoteArrangement.Horizontal =
            RemoteSpacedAbsoluteHorizontalArrangement(space.toPx(), alignment)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteFloat,
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
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteDp,
            alignment: RemoteAlignment.Vertical,
        ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space.toPx(), alignment)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed
         * [space] distance. The spacing will be subtracted from the available width that the
         * children can occupy. An [alignment] can be specified to align the spaced children
         * horizontally inside the parent, in case there is empty width remaining.
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun spacedBy(
            space: RemoteFloat,
            alignment: RemoteAlignment.Vertical,
        ): RemoteArrangement.Vertical = RemoteSpacedVerticalArrangement(space, alignment)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class HorizontalArrangement(var type: Int) : RemoteArrangement.Horizontal {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal =
        when (type) {
            ColumnLayout.START -> androidx.compose.foundation.layout.Arrangement.Start
            ColumnLayout.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
            ColumnLayout.END -> androidx.compose.foundation.layout.Arrangement.End
            ColumnLayout.SPACE_BETWEEN ->
                androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ColumnLayout.SPACE_EVENLY -> androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ColumnLayout.SPACE_AROUND -> androidx.compose.foundation.layout.Arrangement.SpaceAround
            LEFT -> androidx.compose.foundation.layout.Arrangement.Absolute.Left
            RIGHT -> androidx.compose.foundation.layout.Arrangement.Absolute.Right
            else -> androidx.compose.foundation.layout.Arrangement.Start
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(layoutDirection: LayoutDirection): Int =
        when (type) {
            ColumnLayout.START ->
                if (layoutDirection == LayoutDirection.Ltr) ColumnLayout.START else ColumnLayout.END
            ColumnLayout.CENTER -> ColumnLayout.CENTER
            ColumnLayout.END ->
                if (layoutDirection == LayoutDirection.Ltr) ColumnLayout.END else ColumnLayout.START
            ColumnLayout.SPACE_BETWEEN -> ColumnLayout.SPACE_BETWEEN
            ColumnLayout.SPACE_EVENLY -> ColumnLayout.SPACE_EVENLY
            ColumnLayout.SPACE_AROUND -> ColumnLayout.SPACE_AROUND
            LEFT -> ColumnLayout.START
            RIGHT -> ColumnLayout.END
            else ->
                if (layoutDirection == LayoutDirection.Ltr) ColumnLayout.START else ColumnLayout.END
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class VerticalArrangement(var type: Int) : RemoteArrangement.Vertical {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical =
        when (type) {
            ColumnLayout.TOP -> androidx.compose.foundation.layout.Arrangement.Top
            ColumnLayout.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
            ColumnLayout.BOTTOM -> androidx.compose.foundation.layout.Arrangement.Bottom
            else -> androidx.compose.foundation.layout.Arrangement.Top
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int = type
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class HorizontalOrVerticalArrangement(var type: Int) :
    RemoteArrangement.HorizontalOrVertical {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi():
        androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical =
        when (type) {
            ColumnLayout.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
            ColumnLayout.SPACE_BETWEEN ->
                androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ColumnLayout.SPACE_EVENLY -> androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ColumnLayout.SPACE_AROUND -> androidx.compose.foundation.layout.Arrangement.SpaceAround
            else -> androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(layoutDirection: LayoutDirection): Int =
        when (type) {
            ColumnLayout.CENTER,
            ColumnLayout.SPACE_BETWEEN,
            ColumnLayout.SPACE_EVENLY,
            ColumnLayout.SPACE_AROUND -> type
            else ->
                if (layoutDirection == LayoutDirection.Ltr) ColumnLayout.START else ColumnLayout.END
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int =
        when (type) {
            ColumnLayout.CENTER,
            ColumnLayout.SPACE_BETWEEN,
            ColumnLayout.SPACE_EVENLY,
            ColumnLayout.SPACE_AROUND -> type
            else -> ColumnLayout.START
        }
}

internal interface RemoteSpaced {
    public val space: RemoteFloat
}

internal data class RemoteSpacedArrangement(override val space: RemoteFloat) :
    RemoteArrangement.HorizontalOrVertical, RemoteSpaced {
    override fun toComposeUi():
        androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical =
        androidx.compose.foundation.layout.Arrangement.spacedBy(space.toDp())

    override fun toRemote(layoutDirection: LayoutDirection): Int = ColumnLayout.START

    override fun toRemote(): Int = ColumnLayout.TOP
}

internal data class RemoteSpacedHorizontalArrangement(
    override val space: RemoteFloat,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal =
        androidx.compose.foundation.layout.Arrangement.spacedBy(
            space.toDp(),
            alignment.toComposeUi(),
        )

    override fun toRemote(layoutDirection: LayoutDirection): Int =
        alignment.toRemote(layoutDirection)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteSpacedAbsoluteHorizontalArrangement(
    override val space: RemoteFloat,
    val alignment: RemoteAlignment.Horizontal,
) : RemoteArrangement.Horizontal, RemoteSpaced {
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal =
        if (alignment is RemoteBiasAbsoluteAlignment.Horizontal) {
            androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy(
                space.toDp(),
                alignment.toComposeUi(),
            )
        } else {
            androidx.compose.foundation.layout.Arrangement.spacedBy(
                space.toDp(),
                alignment.toComposeUi(),
            )
        }

    override fun toRemote(layoutDirection: LayoutDirection): Int =
        alignment.toRemote(layoutDirection)
}

internal data class RemoteSpacedVerticalArrangement(
    override val space: RemoteFloat,
    val alignment: RemoteAlignment.Vertical,
) : RemoteArrangement.Vertical, RemoteSpaced {
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical =
        androidx.compose.foundation.layout.Arrangement.spacedBy(
            space.toDp(),
            alignment.toComposeUi(),
        )

    override fun toRemote(): Int = alignment.toRemote()
}

private fun RemoteFloat.toDp(): Dp {
    return this.constantValueOrNull?.dp ?: 0.dp
}
