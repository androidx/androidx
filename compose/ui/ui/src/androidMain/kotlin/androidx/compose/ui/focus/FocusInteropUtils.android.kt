/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.focus

import android.view.ViewGroup
import androidx.compose.ui.unit.LayoutDirection

/**
 * Converts an android focus direction to a compose [focus direction][FocusDirection].
 */
internal fun toFocusDirection(androidDirection: Int): FocusDirection? = when (androidDirection) {
    ViewGroup.FOCUS_UP -> FocusDirection.Up
    ViewGroup.FOCUS_DOWN -> FocusDirection.Down
    ViewGroup.FOCUS_LEFT -> FocusDirection.Left
    ViewGroup.FOCUS_RIGHT -> FocusDirection.Right
    ViewGroup.FOCUS_FORWARD -> FocusDirection.Next
    ViewGroup.FOCUS_BACKWARD -> FocusDirection.Previous
    else -> null
}

/**
 * Converts a compose [focus direction][FocusDirection] to an android focus direction.
 */
internal fun FocusDirection.toAndroidFocusDirection(): Int? = when (this) {
        FocusDirection.Up -> ViewGroup.FOCUS_UP
        FocusDirection.Down -> ViewGroup.FOCUS_DOWN
        FocusDirection.Left -> ViewGroup.FOCUS_LEFT
        FocusDirection.Right -> ViewGroup.FOCUS_RIGHT
        FocusDirection.Next -> ViewGroup.FOCUS_FORWARD
        FocusDirection.Previous -> ViewGroup.FOCUS_BACKWARD
        else -> null
    }

/**
 * Convert an Android layout direction to a compose [layout direction][LayoutDirection].
 */
internal fun toLayoutDirection(androidLayoutDirection: Int): LayoutDirection? {
    return when (androidLayoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        else -> null
    }
}

/**
 * focus search in the Android framework wraps around for 1D focus search, but not for 2D focus
 * search. This is a helper function that can be used to determine whether we should wrap around.
 */
internal fun supportsWrapAroundFocus(androidDirection: Int): Boolean = when (androidDirection) {
    ViewGroup.FOCUS_FORWARD, ViewGroup.FOCUS_BACKWARD -> true
    else -> false
}
