/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import android.app.Activity
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.Context
import androidx.compose.Immutable
import androidx.compose.Providers
import androidx.compose.TestOnly
import androidx.compose.ambientOf
import androidx.compose.disposeComposition
import androidx.compose.escapeCompose
import androidx.compose.remember
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.round

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.framework.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup.
 * @param popupProperties Provides extended set of properties to configure the popup.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopLeft,
    offset: IntPxPosition = IntPxPosition(IntPx.Zero, IntPx.Zero),
    popupProperties: PopupProperties = PopupProperties(),
    children: @Composable() () -> Unit
) {
    // Memoize the object, but change the value of the properties if a recomposition happens
    val popupPositionProperties = remember {
        PopupPositionProperties(
            offset = offset
        )
    }
    popupPositionProperties.offset = offset

    Popup(
        popupProperties = popupProperties,
        popupPositionProperties = popupPositionProperties,
        calculatePopupPosition = { calculatePopupGlobalPosition(it, alignment) },
        children = children
    )
}

/**
 * Opens a popup with the given content.
 *
 * The dropdown popup is positioned below its parent, using the [dropDownAlignment] and [offset].
 * The dropdown popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.framework.samples.DropdownPopupSample
 *
 * @param dropDownAlignment The left or right alignment below the parent.
 * @param offset An offset from the original aligned position of the popup.
 * @param popupProperties Provides extended set of properties to configure the popup.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun DropdownPopup(
    dropDownAlignment: DropDownAlignment = DropDownAlignment.Left,
    offset: IntPxPosition = IntPxPosition(IntPx.Zero, IntPx.Zero),
    popupProperties: PopupProperties = PopupProperties(),
    children: @Composable() () -> Unit
) {
    // Memoize the object, but change the value of the properties if a recomposition happens
    val popupPositionProperties = remember {
        PopupPositionProperties(
            offset = offset
        )
    }
    popupPositionProperties.offset = offset

    Popup(
        popupProperties = popupProperties,
        popupPositionProperties = popupPositionProperties,
        calculatePopupPosition = { calculateDropdownPopupPosition(it, dropDownAlignment) },
        children = children
    )
}

private val DefaultTestTag = "DEFAULT_TEST_TAG"

// TODO(b/142431825): This is a hack to work around Popups not using Semantics for test tags
//  We should either remove it, or come up with an abstracted general solution that isn't specific
//  to Popup
private val PopupTestTagAmbient = ambientOf { DefaultTestTag }

@Composable
internal fun PopupTestTag(tag: String, children: @Composable() () -> Unit) {
    Providers(PopupTestTagAmbient provides tag, children = children)
}

@Composable
private fun Popup(
    popupProperties: PopupProperties,
    popupPositionProperties: PopupPositionProperties,
    calculatePopupPosition: ((PopupPositionProperties) -> IntPxPosition),
    children: @Composable() () -> Unit
) {
    val context = ContextAmbient.current
    // TODO(b/139866476): Decide if we want to expose the AndroidComposeView
    val composeView = AndroidComposeViewAmbient.current
    val providedTestTag = PopupTestTagAmbient.current

    val popupLayout = remember(popupProperties) {
        escapeCompose { PopupLayout(
            context = context,
            composeView = composeView,
            popupProperties = popupProperties,
            popupPositionProperties = popupPositionProperties,
            calculatePopupPosition = calculatePopupPosition,
            testTag = providedTestTag
        ) }
    }
    popupLayout.calculatePopupPosition = calculatePopupPosition

    // Get the parent's global position and size
    OnPositioned { coordinates ->
        // Get the global position of the parent
        val layoutPosition = coordinates.localToGlobal(PxPosition.Origin)
        val layoutSize = coordinates.size

        popupLayout.popupPositionProperties.parentPosition = layoutPosition
        popupLayout.popupPositionProperties.parentSize = layoutSize

        // Update the popup's position
        popupLayout.updatePosition()
    }

    onCommit {
        popupLayout.setContent {
            OnChildPositioned({
                // Get the size of the content
                popupLayout.popupPositionProperties.childrenSize = it.size

                // Update the popup's position
                popupLayout.updatePosition()
            }, children)
        }
    }

    onDispose {
        popupLayout.disposeComposition()
        // Remove the window
        popupLayout.dismiss()
    }
}

/**
 * The layout the popup uses to display its content.
 *
 * @param context The application context.
 * @param composeView The parent view of the popup which is the AndroidComposeView.
 * @param popupProperties Properties of the popup.
 * @param calculatePopupPosition The logic of positioning the popup relative to its parent.
 */
private class PopupLayout(
    context: Context,
    val composeView: View,
    val popupProperties: PopupProperties,
    var popupPositionProperties: PopupPositionProperties,
    var calculatePopupPosition: ((PopupPositionProperties) -> IntPxPosition),
    var testTag: String
) : FrameLayout(context) {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val params = createLayoutParams()
    var viewAdded: Boolean = false

    init {
        id = android.R.id.content
        updateLayoutParams()
    }

    /**
     * Shows the popup at a position given by the method which calculates the coordinates
     * relative to its parent.
     */
    fun updatePosition() {
        val popupGlobalPosition = calculatePopupPosition(popupPositionProperties)

        params.x = popupGlobalPosition.x.value
        params.y = popupGlobalPosition.y.value

        if (!viewAdded) {
            windowManager.addView(this, params)
            viewAdded = true
        } else {
            windowManager.updateViewLayout(this, params)
        }
    }

    /**
     * Update the LayoutParams using the popup's properties.
     */
    fun updateLayoutParams() {
        if (!popupProperties.isFocusable) {
            this.params.flags = this.params.flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [PopupProperties.onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if ((event?.action == MotionEvent.ACTION_DOWN) &&
            ((event.x < 0) || (event.x >= width) || (event.y < 0) || (event.y >= height))) {
            popupProperties.onDismissRequest?.invoke()

            return true
        } else if (event?.action == MotionEvent.ACTION_OUTSIDE) {
            popupProperties.onDismissRequest?.invoke()

            return true
        }

        return super.onTouchEvent(event)
    }

    /**
     * Initialize the LayoutParams specific to [android.widget.PopupWindow].
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            // Start to position the popup in the top left corner, a new position will be calculated
            gravity = Gravity.START or Gravity.TOP

            // Flags specific to android.widget.PopupWindow
            flags = flags and (WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH).inv()

            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT
        }
    }
}

// TODO(b/139800142): Add other PopupWindow properties which may be needed
@Immutable
data class PopupProperties(
    /**
     * Indicates if the popup can grab the focus.
     */
    val isFocusable: Boolean = false,
    /**
     * Executes when the popup tries to dismiss itself.
     * This happens when the popup is focusable and the user clicks outside.
     */
    val onDismissRequest: (() -> Unit)? = null
)

internal data class PopupPositionProperties(
    var offset: IntPxPosition
) {
    var parentPosition = PxPosition.Origin
    var parentSize = PxSize.Zero
    var childrenSize = PxSize.Zero
}

/**
 * The [DropdownPopup] is aligned below its parent relative to its left or right corner.
 * [DropDownAlignment] is used to specify how should [DropdownPopup] be aligned.
 */
enum class DropDownAlignment {
    Left,
    Right
}

internal fun calculatePopupGlobalPosition(
    popupPositionProperties: PopupPositionProperties,
    alignment: Alignment
): IntPxPosition {
    // TODO: Decide which is the best way to round to result without reimplementing Alignment.align
    var popupGlobalPosition = IntPxPosition(IntPx.Zero, IntPx.Zero)

    // Get the aligned point inside the parent
    val parentAlignmentPoint = alignment.align(
        IntPxSize(
            popupPositionProperties.parentSize.width.round(),
            popupPositionProperties.parentSize.height.round()
        )
    )
    // Get the aligned point inside the child
    val relativePopupPos = alignment.align(
        IntPxSize(
            popupPositionProperties.childrenSize.width.round(),
            popupPositionProperties.childrenSize.height.round()
        )
    )

    // Add the global position of the parent
    popupGlobalPosition += IntPxPosition(
        popupPositionProperties.parentPosition.x.round(),
        popupPositionProperties.parentPosition.y.round()
    )

    // Add the distance between the parent's top left corner and the alignment point
    popupGlobalPosition += parentAlignmentPoint

    // Subtract the distance between the children's top left corner and the alignment point
    popupGlobalPosition -= IntPxPosition(relativePopupPos.x, relativePopupPos.y)

    // Add the user offset
    popupGlobalPosition += popupPositionProperties.offset

    return popupGlobalPosition
}

internal fun calculateDropdownPopupPosition(
    popupPositionProperties: PopupPositionProperties,
    dropDownAlignment: DropDownAlignment
): IntPxPosition {
    var popupGlobalPosition = IntPxPosition(IntPx.Zero, IntPx.Zero)

    // Add the global position of the parent
    popupGlobalPosition += IntPxPosition(
        popupPositionProperties.parentPosition.x.round(),
        popupPositionProperties.parentPosition.y.round()
    )

    // The X coordinate of the popup relative to the parent is equal to the parent's width if
    // aligned to the END or it is 0 otherwise
    val alignmentPositionX =
        if (dropDownAlignment == DropDownAlignment.Right) {
            popupPositionProperties.parentSize.width.round()
        } else {
            IntPx.Zero
        }

    // The popup's position relative to the parent's top left corner
    val dropdownAlignmentPosition = IntPxPosition(
        alignmentPositionX,
        popupPositionProperties.parentSize.height.round()
    )

    popupGlobalPosition += dropdownAlignmentPosition

    // Add the user offset
    popupGlobalPosition += popupPositionProperties.offset

    return popupGlobalPosition
}

// TODO(b/140396932): Remove once Activity.disposeComposition() is working properly
/**
 * Disposes the root view of the Activity.
 */
fun disposeActivityComposition(activity: Activity) {
    val composeView = activity.window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? AndroidComposeView
        ?: error("No root view found")

    Compose.disposeComposition(composeView.root, activity, null)
}

/**
 * Returns whether the given view is an underlying decor view of a popup. If the given testTag is
 * supplied it also verifies that the popup has such tag assigned.
 *
 * @param view View to verify.
 * @param testTag If provided, tests that the given tag in defined on the popup.
 */
// TODO(b/139861182): Move this functionality to ComposeTestRule
@TestOnly
fun isPopupLayout(view: View, testTag: String? = null): Boolean =
    view is PopupLayout && (testTag == null || testTag == view.testTag)