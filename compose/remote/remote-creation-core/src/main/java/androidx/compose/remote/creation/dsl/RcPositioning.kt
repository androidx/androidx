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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo

/** Base class for child positioning in layouts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcPositioning {
    public companion object {
        /** Centers children along the axis. */
        @get:JvmStatic public val Center: RcPositioning = CenterPositioning
    }
}

private object CenterPositioning :
    RcPositioning,
    RcHorizontalPositioning,
    RcVerticalPositioning,
    RcRowHorizontalPositioning,
    RcColumnVerticalPositioning

/** Horizontal child positioning. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcHorizontalPositioning : RcPositioning {
    public companion object {
        @get:JvmStatic public val Start: RcHorizontalPositioning = StartPositioning
        @get:JvmStatic public val Center: RcHorizontalPositioning = CenterPositioning
        @get:JvmStatic public val End: RcHorizontalPositioning = EndPositioning
    }
}

/** Vertical child positioning. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcVerticalPositioning : RcPositioning {
    public companion object {
        @get:JvmStatic public val Top: RcVerticalPositioning = TopPositioning
        @get:JvmStatic public val Center: RcVerticalPositioning = CenterPositioning
        @get:JvmStatic public val Bottom: RcVerticalPositioning = BottomPositioning
    }
}

/** Horizontal child positioning specific to Row. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcRowHorizontalPositioning : RcHorizontalPositioning {
    public companion object {
        @get:JvmStatic public val Start: RcRowHorizontalPositioning = StartPositioning
        @get:JvmStatic public val Center: RcRowHorizontalPositioning = CenterPositioning
        @get:JvmStatic public val End: RcRowHorizontalPositioning = EndPositioning
        @get:JvmStatic public val SpaceBetween: RcRowHorizontalPositioning = SpaceBetweenPositioning
        @get:JvmStatic public val SpaceEvenly: RcRowHorizontalPositioning = SpaceEvenlyPositioning
        @get:JvmStatic public val SpaceAround: RcRowHorizontalPositioning = SpaceAroundPositioning
    }
}

/** Vertical child positioning specific to Column. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcColumnVerticalPositioning : RcVerticalPositioning {
    public companion object {
        @get:JvmStatic public val Top: RcColumnVerticalPositioning = TopPositioning
        @get:JvmStatic public val Center: RcColumnVerticalPositioning = CenterPositioning
        @get:JvmStatic public val Bottom: RcColumnVerticalPositioning = BottomPositioning
        @get:JvmStatic
        public val SpaceBetween: RcColumnVerticalPositioning = SpaceBetweenPositioning
        @get:JvmStatic public val SpaceEvenly: RcColumnVerticalPositioning = SpaceEvenlyPositioning
        @get:JvmStatic public val SpaceAround: RcColumnVerticalPositioning = SpaceAroundPositioning
    }
}

private object StartPositioning : RcHorizontalPositioning, RcRowHorizontalPositioning

private object EndPositioning : RcHorizontalPositioning, RcRowHorizontalPositioning

private object TopPositioning : RcVerticalPositioning, RcColumnVerticalPositioning

private object BottomPositioning : RcVerticalPositioning, RcColumnVerticalPositioning

private object SpaceBetweenPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning

private object SpaceEvenlyPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning

private object SpaceAroundPositioning : RcRowHorizontalPositioning, RcColumnVerticalPositioning

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
