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
import androidx.compose.ui.unit.dp

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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun toRemote(): Int
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override fun toRemote(): Int
    }

    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     */
    public val Top: RemoteArrangement.Vertical = VerticalArrangement(0)
    /** Place children vertically such that they are centered on the main axis. */
    public val Center: RemoteArrangement.Vertical = VerticalArrangement(1)
    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     */
    public val Bottom: RemoteArrangement.Vertical = VerticalArrangement(2)
    /**
     * Place children horizontally such that they are as close as possible to the start of the main
     * axis.
     */
    public val Start: RemoteArrangement.Horizontal = HorizontalArrangement(3)
    /** Place children horizontally such that they are centered on the main axis. */
    public val CenterHorizontally: RemoteArrangement.Horizontal = HorizontalArrangement(4)
    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     */
    public val End: RemoteArrangement.Horizontal = HorizontalArrangement(5)
    /** Place children with equal space between them, including the edges. */
    public val SpaceBetween: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(6)
    /** Place children with equal space between them, but not on the edges. */
    public val SpaceEvenly: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(7)
    /** Place children with equal space around them. */
    public val SpaceAround: RemoteArrangement.HorizontalOrVertical =
        HorizontalOrVerticalArrangement(8)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class VerticalArrangement(var type: Int) : RemoteArrangement.Vertical {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical {
        when (type) {
            0 -> return androidx.compose.foundation.layout.Arrangement.Top
            1 -> return androidx.compose.foundation.layout.Arrangement.Center
            2 -> return androidx.compose.foundation.layout.Arrangement.Bottom
        }
        return androidx.compose.foundation.layout.Arrangement.Top
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int {
        when (type) {
            0 -> return ColumnLayout.TOP
            1 -> return ColumnLayout.CENTER
            2 -> return ColumnLayout.BOTTOM
        }
        return ColumnLayout.TOP
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class HorizontalOrVerticalArrangement(var type: Int) :
    RemoteArrangement.HorizontalOrVertical {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi():
        androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical {
        when (type) {
            6 -> return androidx.compose.foundation.layout.Arrangement.SpaceBetween
            7 -> return androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            8 -> return androidx.compose.foundation.layout.Arrangement.SpaceAround
        }
        return androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int {
        when (type) {
            6 -> return ColumnLayout.SPACE_BETWEEN
            7 -> return ColumnLayout.SPACE_EVENLY
            8 -> return ColumnLayout.SPACE_AROUND
        }
        return ColumnLayout.START
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class HorizontalArrangement(var type: Int) : RemoteArrangement.Horizontal {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal {
        when (type) {
            3 -> return androidx.compose.foundation.layout.Arrangement.Start
            4 -> return androidx.compose.foundation.layout.Arrangement.Center
            5 -> return androidx.compose.foundation.layout.Arrangement.End
            6 -> return androidx.compose.foundation.layout.Arrangement.SpaceBetween
            7 -> return androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            8 -> return androidx.compose.foundation.layout.Arrangement.SpaceAround
        }
        return androidx.compose.foundation.layout.Arrangement.Start
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toRemote(): Int {
        when (type) {
            3 -> return ColumnLayout.START
            4 -> return ColumnLayout.CENTER
            5 -> return ColumnLayout.END
            6 -> return ColumnLayout.SPACE_BETWEEN
            7 -> return ColumnLayout.SPACE_EVENLY
            8 -> return ColumnLayout.SPACE_AROUND
        }
        return ColumnLayout.START
    }
}
