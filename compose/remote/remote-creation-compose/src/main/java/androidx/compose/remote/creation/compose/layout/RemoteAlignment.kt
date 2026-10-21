/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.Bottom
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.CenterHorizontally
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.CenterVertically
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.End
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.Start
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.Top

/**
 * A remote equivalent of [androidx.compose.ui.Alignment]. It is used to define how a layout's
 * children should be positioned.
 *
 * Pre-defined alignment objects are available through the companion object:
 * - Horizontal: [Start], [CenterHorizontally], [End]
 * - Vertical: [Top], [CenterVertically], [Bottom]
 */
public interface RemoteAlignment {
    @get:Suppress("HiddenAbstractMethodInInterface")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val horizontal: Horizontal
    @get:Suppress("HiddenAbstractMethodInInterface")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val vertical: Vertical

    /**
     * A remote equivalent of [androidx.compose.ui.Alignment.Horizontal]. It is used to define how a
     * layout's children should be positioned horizontally.
     *
     * Pre-defined alignment objects are available: [Start], [CenterHorizontally], and [End].
     */
    public sealed interface Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toRemote(layoutDirection: RemoteLayoutDirection): Int
    }

    /**
     * An alignment that defines how to place a child vertically inside a parent layout. This
     * corresponds to [androidx.compose.ui.Alignment.Vertical].
     *
     * @see Top
     * @see CenterVertically
     * @see Bottom
     */
    public sealed interface Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun toRemote(): Int
    }

    /** A collection of common [RemoteAlignment]s aware of layout direction. */
    public companion object {
        // 2D Alignments.

        /** Aligns the child at the top-start corner of the parent. */
        public val TopStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.TOP)

        /** Aligns the child at the top-center edge of the parent. */
        public val TopCenter: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.TOP)

        /** Aligns the child at the top-end corner of the parent. */
        public val TopEnd: RemoteAlignment = RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.TOP)

        /** Aligns the child at the center-start edge of the parent. */
        public val CenterStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.CENTER)

        /** Aligns the child at the center of the parent both horizontally and vertically. */
        public val Center: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.CENTER)

        /** Aligns the child at the center-end edge of the parent. */
        public val CenterEnd: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.CENTER)

        /** Aligns the child at the bottom-start corner of the parent. */
        public val BottomStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.BOTTOM)

        /** Aligns the child at the bottom-center edge of the parent. */
        public val BottomCenter: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.BOTTOM)

        /** Aligns the child at the bottom-end corner of the parent. */
        public val BottomEnd: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.BOTTOM)

        // 1D Alignment.Verticals.

        /** Aligns the child at the top edge of the parent. */
        public val Top: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.TOP)

        /** Aligns the child vertically in the center of the parent. */
        public val CenterVertically: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.CENTER)

        /** Aligns the child at the bottom edge of the parent. */
        public val Bottom: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.BOTTOM)

        // 1D Alignment.Horizontals.

        /** Aligns the child at the start edge of the parent. */
        public val Start: Horizontal = RemoteBiasAlignment.Horizontal(ColumnLayout.START)

        /** Aligns the child horizontally in the center of the parent. */
        public val CenterHorizontally: Horizontal =
            RemoteBiasAlignment.Horizontal(ColumnLayout.CENTER)

        /** Aligns the child at the end edge of the parent. */
        public val End: Horizontal = RemoteBiasAlignment.Horizontal(ColumnLayout.END)
    }
}

/** A collection of common [RemoteAlignment]s unaware of the layout direction. */
public object RemoteAbsoluteAlignment {
    // 2D AbsoluteAlignments.

    /** The child is positioned at the top-left corner of the parent. */
    public val TopLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.TOP)

    /** The child is positioned at the top-right corner of the parent. */
    public val TopRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.TOP)

    /** The child is positioned at the center-left edge of the parent. */
    public val CenterLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.CENTER)

    /** The child is positioned at the center-right edge of the parent. */
    public val CenterRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.CENTER)

    /** The child is positioned at the bottom-left corner of the parent. */
    public val BottomLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.BOTTOM)

    /** The child is positioned at the bottom-right corner of the parent. */
    public val BottomRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.BOTTOM)

    // 1D RemoteBiasAbsoluteAlignment.Horizontals.

    /** The child is positioned at the left edge of the parent. */
    public val Left: RemoteAlignment.Horizontal =
        RemoteBiasAbsoluteAlignment.Horizontal(ColumnLayout.START)

    /** The child is positioned at the right edge of the parent. */
    public val Right: RemoteAlignment.Horizontal =
        RemoteBiasAbsoluteAlignment.Horizontal(ColumnLayout.END)
}

/**
 * An [RemoteAlignment] specified by bias: [ColumnLayout.START] / [ColumnLayout.TOP],
 * [ColumnLayout.CENTER], [ColumnLayout.END] / [ColumnLayout.BOTTOM].
 *
 * @see RemoteAlignment
 */
internal data class RemoteBiasAlignment(val horizontalBias: Int, val verticalBias: Int) :
    RemoteAlignment {
    override val horizontal: RemoteAlignment.Horizontal =
        RemoteBiasAlignment.Horizontal(horizontalBias)
    override val vertical: RemoteAlignment.Vertical = RemoteBiasAlignment.Vertical(verticalBias)

    /**
     * An [RemoteAlignment.Horizontal] specified by bias: [ColumnLayout.START],
     * [ColumnLayout.CENTER], [ColumnLayout.END].
     *
     * @see Vertical
     */
    internal data class Horizontal(val type: Int) : RemoteAlignment.Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toRemote(layoutDirection: RemoteLayoutDirection): Int =
            when (type) {
                ColumnLayout.START ->
                    if (layoutDirection == RemoteLayoutDirection.Ltr) type else ColumnLayout.END
                ColumnLayout.END ->
                    if (layoutDirection == RemoteLayoutDirection.Ltr) type else ColumnLayout.START
                else -> type
            }
    }

    /**
     * An [RemoteAlignment.Vertical] specified by bias: [ColumnLayout.TOP], [ColumnLayout.CENTER],
     * [ColumnLayout.BOTTOM].
     *
     * @see Horizontal
     */
    internal data class Vertical(val type: Int) : RemoteAlignment.Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int = type
    }
}

/**
 * An [RemoteAlignment] specified by bias: [ColumnLayout.START] / [ColumnLayout.TOP],
 * * [ColumnLayout.CENTER], [ColumnLayout.END] / [ColumnLayout.BOTTOM].
 *
 * @see RemoteAbsoluteAlignment
 * @see RemoteAlignment
 */
internal data class RemoteBiasAbsoluteAlignment(val horizontalBias: Int, val verticalBias: Int) :
    RemoteAlignment {
    override val horizontal: RemoteAlignment.Horizontal =
        RemoteBiasAbsoluteAlignment.Horizontal(horizontalBias)
    override val vertical: RemoteAlignment.Vertical =
        RemoteBiasAbsoluteAlignment.Vertical(verticalBias)

    /**
     * An [RemoteAlignment.Horizontal] specified by bias: [ColumnLayout.START],
     * [ColumnLayout.CENTER], [ColumnLayout.END].
     *
     * @see RemoteBiasAlignment.Horizontal
     */
    internal data class Horizontal(val bias: Int) : RemoteAlignment.Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toRemote(layoutDirection: RemoteLayoutDirection): Int = bias
    }

    /**
     * An [RemoteAlignment.Vertical] specified by bias: [ColumnLayout.TOP], [ColumnLayout.CENTER],
     * [ColumnLayout.BOTTOM].
     *
     * @see RemoteBiasAlignment.Horizontal
     */
    internal data class Vertical(val bias: Int) : RemoteAlignment.Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int = bias
    }
}
