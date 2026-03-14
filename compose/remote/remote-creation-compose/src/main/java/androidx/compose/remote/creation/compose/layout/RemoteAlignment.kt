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
import androidx.compose.ui.unit.LayoutDirection

/**
 * A remote equivalent of [androidx.compose.ui.Alignment]. It is used to define how a layout's
 * children should be positioned.
 *
 * Pre-defined alignment objects are available through the companion object:
 * - Horizontal: [Start], [CenterHorizontally], [End]
 * - Vertical: [Top], [CenterVertically], [Bottom]
 */
public interface RemoteAlignment {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val horizontal: Horizontal

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val vertical: Vertical

    /**
     * A remote equivalent of [androidx.compose.ui.Alignment.Horizontal]. It is used to define how a
     * layout's children should be positioned horizontally.
     *
     * Pre-defined alignment objects are available: [Start], [CenterHorizontally], and [End].
     */
    public sealed interface Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toRemote(layoutDirection: LayoutDirection): Int
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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toComposeUi(): androidx.compose.ui.Alignment.Vertical

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun toRemote(): Int
    }

    /** A collection of common [RemoteAlignment]s aware of layout direction. */
    public companion object {
        // 2D Alignments.
        public val TopStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.TOP)
        public val TopCenter: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.TOP)
        public val TopEnd: RemoteAlignment = RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.TOP)
        public val CenterStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.CENTER)
        public val Center: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.CENTER)
        public val CenterEnd: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.CENTER)
        public val BottomStart: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.START, ColumnLayout.BOTTOM)
        public val BottomCenter: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.CENTER, ColumnLayout.BOTTOM)
        public val BottomEnd: RemoteAlignment =
            RemoteBiasAlignment(ColumnLayout.END, ColumnLayout.BOTTOM)

        // 1D Alignment.Verticals.
        public val Top: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.TOP)
        public val CenterVertically: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.CENTER)
        public val Bottom: Vertical = RemoteBiasAlignment.Vertical(ColumnLayout.BOTTOM)

        // 1D Alignment.Horizontals.
        public val Start: Horizontal = RemoteBiasAlignment.Horizontal(ColumnLayout.START)
        public val CenterHorizontally: Horizontal =
            RemoteBiasAlignment.Horizontal(ColumnLayout.CENTER)
        public val End: Horizontal = RemoteBiasAlignment.Horizontal(ColumnLayout.END)
    }
}

/** A collection of common [RemoteAlignment]s unaware of the layout direction. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteAbsoluteAlignment {
    // 2D AbsoluteAlignments.
    public val TopLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.TOP)
    public val TopRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.TOP)
    public val CenterLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.CENTER)
    public val CenterRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.CENTER)
    public val BottomLeft: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.START, ColumnLayout.BOTTOM)
    public val BottomRight: RemoteAlignment =
        RemoteBiasAbsoluteAlignment(ColumnLayout.END, ColumnLayout.BOTTOM)

    // 1D RemoteBiasAbsoluteAlignment.Horizontals.
    public val Left: RemoteAlignment.Horizontal =
        RemoteBiasAbsoluteAlignment.Horizontal(ColumnLayout.START)
    public val Right: RemoteAlignment.Horizontal =
        RemoteBiasAbsoluteAlignment.Horizontal(ColumnLayout.END)
}

/**
 * An [RemoteAlignment] specified by bias: [ColumnLayout.START] / [ColumnLayout.TOP],
 * [ColumnLayout.CENTER], [ColumnLayout.END] / [ColumnLayout.BOTTOM].
 *
 * @see RemoteAlignment
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteBiasAlignment(val horizontalBias: Int, val verticalBias: Int) :
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Horizontal(val type: Int) : RemoteAlignment.Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal =
            when (type) {
                ColumnLayout.START -> androidx.compose.ui.Alignment.Start
                ColumnLayout.CENTER -> androidx.compose.ui.Alignment.CenterHorizontally
                ColumnLayout.END -> androidx.compose.ui.Alignment.End
                else -> androidx.compose.ui.Alignment.Start
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toRemote(layoutDirection: LayoutDirection): Int =
            when (type) {
                ColumnLayout.START ->
                    if (layoutDirection == LayoutDirection.Ltr) type else ColumnLayout.END
                ColumnLayout.END ->
                    if (layoutDirection == LayoutDirection.Ltr) type else ColumnLayout.START
                else -> type
            }
    }

    /**
     * An [RemoteAlignment.Vertical] specified by bias: [ColumnLayout.TOP], [ColumnLayout.CENTER],
     * [ColumnLayout.BOTTOM].
     *
     * @see Horizontal
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Vertical(var type: Int) : RemoteAlignment.Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toComposeUi(): androidx.compose.ui.Alignment.Vertical =
            when (type) {
                ColumnLayout.TOP -> androidx.compose.ui.Alignment.Top
                ColumnLayout.CENTER -> androidx.compose.ui.Alignment.CenterVertically
                ColumnLayout.BOTTOM -> androidx.compose.ui.Alignment.Bottom
                else -> androidx.compose.ui.Alignment.Top
            }

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteBiasAbsoluteAlignment(val horizontalBias: Int, val verticalBias: Int) :
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Horizontal(val bias: Int) : RemoteAlignment.Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal =
            when (bias) {
                ColumnLayout.START -> androidx.compose.ui.AbsoluteAlignment.Left
                ColumnLayout.CENTER -> androidx.compose.ui.Alignment.CenterHorizontally
                ColumnLayout.END -> androidx.compose.ui.AbsoluteAlignment.Right
                else -> androidx.compose.ui.AbsoluteAlignment.Left
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toRemote(layoutDirection: LayoutDirection): Int = bias
    }

    /**
     * An [RemoteAlignment.Vertical] specified by bias: [ColumnLayout.TOP], [ColumnLayout.CENTER],
     * [ColumnLayout.BOTTOM].
     *
     * @see RemoteBiasAlignment.Horizontal
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public data class Vertical(val bias: Int) : RemoteAlignment.Vertical {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        override fun toComposeUi(): androidx.compose.ui.Alignment.Vertical =
            when (bias) {
                ColumnLayout.TOP -> androidx.compose.ui.Alignment.Top
                ColumnLayout.CENTER -> androidx.compose.ui.Alignment.CenterVertically
                ColumnLayout.BOTTOM -> androidx.compose.ui.Alignment.Bottom
                else -> androidx.compose.ui.Alignment.Top
            }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int = bias
    }
}
