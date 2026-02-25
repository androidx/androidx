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

/**
 * A remote equivalent of [androidx.compose.ui.Alignment]. It is used to define how a layout's
 * children should be positioned.
 *
 * Pre-defined alignment objects are available through the companion object:
 * - Horizontal: [Start], [CenterHorizontally], [End]
 * - Vertical: [Top], [CenterVertically], [Bottom]
 */
public object RemoteAlignment {

    /**
     * A remote equivalent of [androidx.compose.ui.Alignment.Horizontal]. It is used to define how a
     * layout's children should be positioned horizontally.
     *
     * Pre-defined alignment objects are available: [Start], [CenterHorizontally], and [End].
     */
    public sealed interface Horizontal {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun toRemote(): Int
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

    /**
     * A [RemoteAlignment.Horizontal] that aligns the child to the start of the parent's horizontal
     * axis.
     */
    public val Start: RemoteAlignment.Horizontal = RemoteHorizontalAlignment(0)
    /**
     * A [RemoteAlignment.Horizontal] that aligns the child to the center of the parent's horizontal
     * axis.
     */
    public val CenterHorizontally: RemoteAlignment.Horizontal = RemoteHorizontalAlignment(1)
    /**
     * A [RemoteAlignment.Horizontal] that aligns the child to the end of the parent's horizontal
     * axis.
     */
    public val End: RemoteAlignment.Horizontal = RemoteHorizontalAlignment(2)
    /**
     * A [RemoteAlignment.Vertical] that aligns the child to the top of the parent's vertical axis.
     */
    public val Top: RemoteAlignment.Vertical = RemoteVerticalAlignment(3)
    /**
     * A [RemoteAlignment.Vertical] that aligns the child to the center of the parent's vertical
     * axis.
     */
    public val CenterVertically: RemoteAlignment.Vertical = RemoteVerticalAlignment(4)
    /**
     * A [RemoteAlignment.Vertical] that aligns the child to the bottom of the parent's vertical
     * axis.
     */
    public val Bottom: RemoteAlignment.Vertical = RemoteVerticalAlignment(5)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteHorizontalAlignment(var type: Int) : RemoteAlignment.Horizontal {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal {
        when (type) {
            0 -> return androidx.compose.ui.Alignment.Start
            1 -> return androidx.compose.ui.Alignment.CenterHorizontally
            2 -> return androidx.compose.ui.Alignment.End
        }
        return androidx.compose.ui.Alignment.Start
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int {
        when (type) {
            0 -> return ColumnLayout.START
            1 -> return ColumnLayout.CENTER
            2 -> return ColumnLayout.END
        }
        return ColumnLayout.START
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class RemoteVerticalAlignment(var type: Int) : RemoteAlignment.Vertical {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.ui.Alignment.Vertical {
        when (type) {
            3 -> return androidx.compose.ui.Alignment.Top
            4 -> return androidx.compose.ui.Alignment.CenterVertically
            5 -> return androidx.compose.ui.Alignment.Bottom
        }
        return androidx.compose.ui.Alignment.Top
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int {
        when (type) {
            3 -> return ColumnLayout.TOP
            4 -> return ColumnLayout.CENTER
            5 -> return ColumnLayout.BOTTOM
        }
        return ColumnLayout.TOP
    }
}
