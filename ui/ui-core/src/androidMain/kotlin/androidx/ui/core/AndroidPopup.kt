/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.ExperimentalComposeApi
import androidx.compose.compositionReference
import androidx.compose.currentComposer
import androidx.compose.emptyContent
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.ui.core.semantics.semantics
import androidx.ui.geometry.Offset
import androidx.ui.semantics.popup
import androidx.ui.unit.IntBounds
import androidx.ui.unit.round
import org.jetbrains.annotations.TestOnly

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.ui.core.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
internal actual fun ActualPopup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    children: @Composable () -> Unit
) {
    val view = ViewAmbient.current
    val providedTestTag = PopupTestTagAmbient.current

    val popupPositionProperties = remember { PopupPositionProperties() }
    val popupLayout = remember(isFocusable) {
        PopupLayout(
            composeView = view,
            popupIsFocusable = isFocusable,
            onDismissRequest = onDismissRequest,
            popupPositionProperties = popupPositionProperties,
            popupPositionProvider = popupPositionProvider,
            testTag = providedTestTag
        )
    }
    popupLayout.popupPositionProvider = popupPositionProvider

    var composition: Composition? = null

    // TODO(soboleva): Look at module arrangement so that Box can be
    // used instead of this custom Layout
    // Get the parent's global position, size and layout direction
    Layout(children = emptyContent(), modifier = Modifier.onPositioned { childCoordinates ->
        val coordinates = childCoordinates.parentCoordinates!!
        // Get the global position of the parent
        val layoutPosition = coordinates.localToGlobal(Offset.Zero).round()
        val layoutSize = coordinates.size

        popupLayout.popupPositionProperties.parentBounds = IntBounds(layoutPosition, layoutSize)

        // Update the popup's position
        popupLayout.updatePosition()
    }) { _, _ ->
        popupLayout.popupPositionProperties.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }

    // TODO(lmr): refactor these APIs so that recomposer isn't necessary
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    val parentComposition = compositionReference()
    onCommit {
        composition = popupLayout.setContent(recomposer, parentComposition) {
            SimpleStack(Modifier.semantics { this.popup() }.onPositioned {
                // Get the size of the content
                popupLayout.popupPositionProperties.childrenSize = it.size

                // Update the popup's position
                popupLayout.updatePosition()
            }, children = children)
        }
    }

    onDispose {
        composition?.dispose()
        // Remove the window
        popupLayout.dismiss()
    }
}

// TODO(soboleva): Look at module dependencies so that we can get code reuse between
// Popup's SimpleStack and Stack.
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun SimpleStack(modifier: Modifier, noinline children: @Composable () -> Unit) {
    Layout(children = children, modifier = modifier) { measurables, constraints ->
        when (measurables.size) {
            0 -> layout(0, 0) {}
            1 -> {
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0, 0)
                }
            }
            else -> {
                val placeables = measurables.map { it.measure(constraints) }
                var width = 0
                var height = 0
                for (i in 0..placeables.lastIndex) {
                    val p = placeables[i]
                    width = maxOf(width, p.width)
                    height = maxOf(height, p.height)
                }
                layout(width, height) {
                    for (i in 0..placeables.lastIndex) {
                        val p = placeables[i]
                        p.place(0, 0)
                    }
                }
            }
        }
    }
}

/**
 * The layout the popup uses to display its content.
 *
 * @param composeView The parent view of the popup which is the AndroidComposeView.
 * @param popupIsFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executed when the popup tries to dismiss itself.
 * @param popupPositionProvider The logic of positioning the popup relative to its parent.
 */
@SuppressLint("ViewConstructor")
private class PopupLayout(
    val composeView: View,
    val popupIsFocusable: Boolean,
    val onDismissRequest: (() -> Unit)? = null,
    var popupPositionProperties: PopupPositionProperties,
    var popupPositionProvider: PopupPositionProvider,
    var testTag: String
) : FrameLayout(composeView.context) {
    val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val params = createLayoutParams()
    var viewAdded: Boolean = false

    init {
        id = android.R.id.content
        updateLayoutParams()
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
    }

    /**
     * Shows the popup at a position given by the method which calculates the coordinates
     * relative to its parent.
     */
    fun updatePosition() {
        val popupGlobalPosition = popupPositionProvider.calculatePosition(
            popupPositionProperties.parentBounds,
            popupPositionProperties.parentLayoutDirection,
            popupPositionProperties.childrenSize
        )

        params.x = popupGlobalPosition.x
        params.y = popupGlobalPosition.y

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
        if (!popupIsFocusable) {
            this.params.flags = this.params.flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        ViewTreeLifecycleOwner.set(this, null)
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if ((event?.action == MotionEvent.ACTION_DOWN) &&
            ((event.x < 0) || (event.x >= width) || (event.y < 0) || (event.y >= height))
        ) {
            onDismissRequest?.invoke()
            return true
        } else if (event?.action == MotionEvent.ACTION_OUTSIDE) {
            onDismissRequest?.invoke()
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