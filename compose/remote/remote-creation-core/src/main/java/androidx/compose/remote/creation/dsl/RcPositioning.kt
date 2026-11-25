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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RcPositionings")

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo

/** Base class for child positioning in layouts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcPositioning {
    public companion object {
        /** Centers children along the axis. */
        public val Center: RcPositioning = CenterPositioning
    }
}

private object CenterPositioning :
    RcPositioning,
    RcHorizontalPositioning,
    RcVerticalPositioning,
    RcRowHorizontalPositioning,
    RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.Center"
}

/** Horizontal child positioning. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcHorizontalPositioning : RcPositioning {
    public companion object {
        public val Start: RcHorizontalPositioning = StartPositioning
        public val Center: RcHorizontalPositioning = CenterPositioning
        public val End: RcHorizontalPositioning = EndPositioning
    }
}

/** Vertical child positioning. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcVerticalPositioning : RcPositioning {
    public companion object {
        public val Top: RcVerticalPositioning = TopPositioning
        public val Center: RcVerticalPositioning = CenterPositioning
        public val Bottom: RcVerticalPositioning = BottomPositioning
    }
}

/** Horizontal child positioning specific to Row. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcRowHorizontalPositioning : RcHorizontalPositioning {
    public companion object {
        public val Start: RcRowHorizontalPositioning = StartPositioning
        public val Center: RcRowHorizontalPositioning = CenterPositioning
        public val End: RcRowHorizontalPositioning = EndPositioning
        public val SpaceBetween: RcRowHorizontalPositioning = SpaceBetweenPositioning
        public val SpaceEvenly: RcRowHorizontalPositioning = SpaceEvenlyPositioning
        public val SpaceAround: RcRowHorizontalPositioning = SpaceAroundPositioning
    }
}

/** Vertical child positioning specific to Column. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcColumnVerticalPositioning : RcVerticalPositioning {
    public companion object {
        public val Top: RcColumnVerticalPositioning = TopPositioning
        public val Center: RcColumnVerticalPositioning = CenterPositioning
        public val Bottom: RcColumnVerticalPositioning = BottomPositioning
        public val SpaceBetween: RcColumnVerticalPositioning = SpaceBetweenPositioning
        public val SpaceEvenly: RcColumnVerticalPositioning = SpaceEvenlyPositioning
        public val SpaceAround: RcColumnVerticalPositioning = SpaceAroundPositioning
    }
}

private object StartPositioning : RcHorizontalPositioning, RcRowHorizontalPositioning {
    override fun toString(): String = "RcPositioning.Start"
}

private object EndPositioning : RcHorizontalPositioning, RcRowHorizontalPositioning {
    override fun toString(): String = "RcPositioning.End"
}

private object TopPositioning : RcVerticalPositioning, RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.Top"
}

private object BottomPositioning : RcVerticalPositioning, RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.Bottom"
}

private object SpaceBetweenPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.SpaceBetween"
}

private object SpaceEvenlyPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.SpaceEvenly"
}

private object SpaceAroundPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning {
    override fun toString(): String = "RcPositioning.SpaceAround"
}

internal val RcPositioning.value: Int
    get() =
        when (this) {
            StartPositioning -> 1 // START
            CenterPositioning -> 2 // CENTER
            EndPositioning -> 3 // END
            TopPositioning -> 4 // TOP
            BottomPositioning -> 5 // BOTTOM
            SpaceBetweenPositioning -> 6 // SPACE_BETWEEN
            SpaceEvenlyPositioning -> 7 // SPACE_EVENLY
            SpaceAroundPositioning -> 8 // SPACE_AROUND
        }
