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

import android.graphics.Rect
import android.view.FocusFinder
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.focus.FocusInteropUtils.Companion.tempCoordinates
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.unit.LayoutDirection

private class FocusInteropUtils {
    companion object {
        val tempCoordinates = IntArray(2)
    }
}

/** Converts an android focus direction to a compose [focus direction][FocusDirection]. */
internal fun toFocusDirection(androidDirection: Int): FocusDirection? =
    when (androidDirection) {
        ViewGroup.FOCUS_UP -> FocusDirection.Up
        ViewGroup.FOCUS_DOWN -> FocusDirection.Down
        ViewGroup.FOCUS_LEFT -> FocusDirection.Left
        ViewGroup.FOCUS_RIGHT -> FocusDirection.Right
        ViewGroup.FOCUS_FORWARD -> FocusDirection.Next
        ViewGroup.FOCUS_BACKWARD -> FocusDirection.Previous
        else -> null
    }

/** Converts a compose [focus direction][FocusDirection] to an android focus direction. */
internal fun FocusDirection.toAndroidFocusDirection(): Int? =
    when (this) {
        FocusDirection.Up -> ViewGroup.FOCUS_UP
        FocusDirection.Down -> ViewGroup.FOCUS_DOWN
        FocusDirection.Left -> ViewGroup.FOCUS_LEFT
        FocusDirection.Right -> ViewGroup.FOCUS_RIGHT
        FocusDirection.Next -> ViewGroup.FOCUS_FORWARD
        FocusDirection.Previous -> ViewGroup.FOCUS_BACKWARD
        else -> null
    }

/** Convert an Android layout direction to a compose [layout direction][LayoutDirection]. */
internal fun toLayoutDirection(androidLayoutDirection: Int): LayoutDirection? {
    return when (androidLayoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        else -> null
    }
}

/** Returns the bounding rect of the view in the current window. */
internal fun View.calculateBoundingRectRelativeTo(view: View): androidx.compose.ui.geometry.Rect {
    getLocationInWindow(tempCoordinates)
    val xInWindow = tempCoordinates[0]
    val yInWindow = tempCoordinates[1]
    view.getLocationInWindow(tempCoordinates)
    val targetX = tempCoordinates[0]
    val targetY = tempCoordinates[1]
    val x = (xInWindow - targetX).toFloat()
    val y = (yInWindow - targetY).toFloat()
    return androidx.compose.ui.geometry.Rect(x, y, x + width, y + height)
}

internal fun View.requestInteropFocus(direction: Int?, rect: Rect?): Boolean {
    return when {
        direction == null -> requestFocus()
        this !is ViewGroup -> requestFocus(direction, rect)
        isFocused -> true
        isFocusable && !hasFocus() -> requestFocus(direction, rect)
        this is AndroidComposeView -> requestFocus(direction, rect)
        rect != null ->
            FocusFinder.getInstance()
                .findNextFocusFromRect(this, rect, direction)
                ?.requestFocus(direction, rect) ?: requestFocus(direction, rect)
        else -> {
            val focusedView = if (hasFocus()) findFocus() else null
            FocusFinder.getInstance()
                .findNextFocus(this, focusedView, direction)
                ?.requestFocus(direction) ?: requestFocus(direction)
        }
    }
}
