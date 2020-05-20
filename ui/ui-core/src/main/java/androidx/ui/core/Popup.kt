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
import androidx.compose.Immutable
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.compose.currentComposer
import androidx.compose.emptyContent
import androidx.compose.escapeCompose
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.popup
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.round
import org.jetbrains.annotations.TestOnly

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.core.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntPxPosition = IntPxPosition(IntPx.Zero, IntPx.Zero),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(alignment, offset)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        isFocusable = isFocusable,
        onDismissRequest = onDismissRequest,
        children = children
    )
}

/**
 * Opens a popup with the given content.
 *
 * The dropdown popup is positioned below its parent, using the [dropDownAlignment] and [offset].
 * The dropdown popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.core.samples.DropdownPopupSample
 *
 * @param dropDownAlignment The start or end alignment below the parent.
 * @param offset An offset from the original aligned position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun DropdownPopup(
    dropDownAlignment: DropDownAlignment = DropDownAlignment.Start,
    offset: IntPxPosition = IntPxPosition(IntPx.Zero, IntPx.Zero),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val popupPositioner = remember(dropDownAlignment, offset) {
        DropdownPositionProvider(dropDownAlignment, offset)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        isFocusable = isFocusable,
        onDismissRequest = onDismissRequest,
        children = children
    )
}

// TODO(b/142431825): This is a hack to work around Popups not using Semantics for test tags
//  We should either remove it, or come up with an abstracted general solution that isn't specific
//  to Popup
private val PopupTestTagAmbient = ambientOf { "DEFAULT_TEST_TAG" }

@Composable
internal fun PopupTestTag(tag: String, children: @Composable () -> Unit) {
    Providers(PopupTestTagAmbient provides tag, children = children)
}

internal class PopupPositionProperties {
    var parentPosition = IntPxPosition.Origin
    var parentSize = IntPxSize.Zero
    var childrenSize = IntPxSize.Zero
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
}

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
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val context = ContextAmbient.current
    // TODO(b/139866476): Decide if we want to expose the AndroidComposeView
    @Suppress("DEPRECATION")
    val owner = OwnerAmbient.current
    val providedTestTag = PopupTestTagAmbient.current

    val popupPositionProperties = remember { PopupPositionProperties() }
    val popupLayout = remember(isFocusable) {
        escapeCompose {
            PopupLayout(
                context = context,
                composeView = owner as View,
                popupIsFocusable = isFocusable,
                onDismissRequest = onDismissRequest,
                popupPositionProperties = popupPositionProperties,
                popupPositionProvider = popupPositionProvider,
                testTag = providedTestTag
            )
        }
    }
    popupLayout.popupPositionProvider = popupPositionProvider

    var composition: Composition? = null

    // TODO(soboleva): Look at module arrangement so that Box can be
    // used instead of this custom Layout
    // Get the parent's global position, size and layout direction
    Layout(children = emptyContent(), modifier = Modifier.onPositioned { childCoordinates ->
        val coordinates = childCoordinates.parentCoordinates!!
        // Get the global position of the parent
        val layoutPosition = coordinates.localToGlobal(PxPosition.Origin).round()
        val layoutSize = coordinates.size

        popupLayout.popupPositionProperties.parentPosition = layoutPosition
        popupLayout.popupPositionProperties.parentSize = layoutSize

        // Update the popup's position
        popupLayout.updatePosition()
    }) { _, _, layoutDirection ->
        popupLayout.popupPositionProperties.parentLayoutDirection = layoutDirection
        layout(0.ipx, 0.ipx) {}
    }

    val recomposer = currentComposer.recomposer
    onCommit {
        composition = popupLayout.setContent(recomposer) {
            Semantics(container = true, properties = { this.popup = true }) {
                SimpleStack(Modifier.onPositioned {
                    // Get the size of the content
                    popupLayout.popupPositionProperties.childrenSize = it.size

                    // Update the popup's position
                    popupLayout.updatePosition()
                }, children = children)
            }
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
    Layout(children = children, modifier = modifier) { measurables, constraints, _ ->
        when (measurables.size) {
            0 -> layout(0.ipx, 0.ipx) {}
            1 -> {
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0.ipx, 0.ipx)
                }
            }
            else -> {
                val placeables = measurables.map { it.measure(constraints) }
                var width = 0.ipx
                var height = 0.ipx
                for (i in 0..placeables.lastIndex) {
                    val p = placeables[i]
                    width = max(width, p.width)
                    height = max(height, p.height)
                }
                layout(width, height) {
                    for (i in 0..placeables.lastIndex) {
                        val p = placeables[i]
                        p.place(0.ipx, 0.ipx)
                    }
                }
            }
        }
    }
}

/**
 * The layout the popup uses to display its content.
 *
 * @param context The application context.
 * @param composeView The parent view of the popup which is the AndroidComposeView.
 * @param popupIsFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executed when the popup tries to dismiss itself.
 * @param popupPositionProvider The logic of positioning the popup relative to its parent.
 */
@SuppressLint("ViewConstructor")
private class PopupLayout(
    context: Context,
    val composeView: View,
    val popupIsFocusable: Boolean,
    val onDismissRequest: (() -> Unit)? = null,
    var popupPositionProperties: PopupPositionProperties,
    var popupPositionProvider: PopupPositionProvider,
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
        val popupGlobalPosition = popupPositionProvider.calculatePosition(
            popupPositionProperties.parentPosition,
            popupPositionProperties.parentSize,
            popupPositionProperties.parentLayoutDirection,
            popupPositionProperties.childrenSize
        )

        params.x = popupGlobalPosition.x.value
        params.y = popupGlobalPosition.y.value

        if (!viewAdded) {
            windowManager.addView(this, params)
            ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
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
 * Calculates the position of a [Popup] on screen.
 */
@Immutable
interface PopupPositionProvider {
    /**
     * Calculates the position of a [Popup] on screen.
     *
     * @param parentLayoutPosition The position of the parent wrapper layout on screen.
     * @param parentLayoutSize The size of the parent wrapper layout.
     * @param layoutDirection The layout direction of the parent wrapper layout.
     * @param popupSize The size of the popup to be positioned.
     */
    fun calculatePosition(
        parentLayoutPosition: IntPxPosition,
        parentLayoutSize: IntPxSize,
        layoutDirection: LayoutDirection,
        popupSize: IntPxSize
    ): IntPxPosition
}

/**
 * The [DropdownPopup] is aligned below its parent relative to its left or right corner.
 * [DropDownAlignment] is used to specify how should [DropdownPopup] be aligned.
 */
enum class DropDownAlignment {
    Start,
    End
}

internal class AlignmentOffsetPositionProvider(
    val alignment: Alignment,
    val offset: IntPxPosition
) : PopupPositionProvider {
    override fun calculatePosition(
        parentLayoutPosition: IntPxPosition,
        parentLayoutSize: IntPxSize,
        layoutDirection: LayoutDirection,
        popupSize: IntPxSize
    ): IntPxPosition {
        // TODO: Decide which is the best way to round to result without reimplementing Alignment.align
        var popupGlobalPosition = IntPxPosition(IntPx.Zero, IntPx.Zero)

        // Get the aligned point inside the parent
        val parentAlignmentPoint = alignment.align(
            IntPxSize(parentLayoutSize.width, parentLayoutSize.height),
            layoutDirection
        )
        // Get the aligned point inside the child
        val relativePopupPos = alignment.align(
            IntPxSize(popupSize.width, popupSize.height),
            layoutDirection
        )

        // Add the global position of the parent
        popupGlobalPosition += IntPxPosition(parentLayoutPosition.x, parentLayoutPosition.y)

        // Add the distance between the parent's top left corner and the alignment point
        popupGlobalPosition += parentAlignmentPoint

        // Subtract the distance between the children's top left corner and the alignment point
        popupGlobalPosition -= IntPxPosition(relativePopupPos.x, relativePopupPos.y)

        // Add the user offset
        val resolvedOffset = IntPxPosition(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )
        popupGlobalPosition += resolvedOffset

        return popupGlobalPosition
    }
}

internal class DropdownPositionProvider(
    val dropDownAlignment: DropDownAlignment,
    val offset: IntPxPosition
) : PopupPositionProvider {
    override fun calculatePosition(
        parentLayoutPosition: IntPxPosition,
        parentLayoutSize: IntPxSize,
        layoutDirection: LayoutDirection,
        popupSize: IntPxSize
    ): IntPxPosition {
        var popupGlobalPosition = IntPxPosition(IntPx.Zero, IntPx.Zero)

        // Add the global position of the parent
        popupGlobalPosition += IntPxPosition(parentLayoutPosition.x, parentLayoutPosition.y)

        /*
        * In LTR context aligns popup's left edge with the parent's left edge for Start alignment and
        * parent's right edge for End alignment.
        * In RTL context aligns popup's right edge with the parent's right edge for Start alignment and
        * parent's left edge for End alignment.
        */
        val alignmentPositionX =
            if (dropDownAlignment == DropDownAlignment.Start) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    0.ipx
                } else {
                    parentLayoutSize.width - popupSize.width
                }
            } else {
                if (layoutDirection == LayoutDirection.Ltr) {
                    parentLayoutSize.width
                } else {
                    -popupSize.width
                }
            }

        // The popup's position relative to the parent's top left corner
        val dropdownAlignmentPosition = IntPxPosition(alignmentPositionX, parentLayoutSize.height)

        popupGlobalPosition += dropdownAlignmentPosition

        // Add the user offset
        val resolvedOffset = IntPxPosition(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )
        popupGlobalPosition += resolvedOffset

        return popupGlobalPosition
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