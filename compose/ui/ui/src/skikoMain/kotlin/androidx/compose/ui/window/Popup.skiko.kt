/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

/**
 * Properties used to customize the behavior of a [Popup].
 *
 * @property focusable Whether the popup is focusable. When true, the popup will receive IME
 * events and key presses, such as when the back button is pressed.
 * @property dismissOnBackPress Whether the popup can be dismissed by pressing the back button
 * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest. Note that [focusable] must be
 * set to true in order to receive key events such as the back button - if the popup is not
 * focusable then this property does nothing.
 * @property dismissOnClickOutside Whether the popup can be dismissed by clicking outside the
 * popup's bounds. If true, clicking outside the popup will call onDismissRequest.
 * @property clippingEnabled Whether to allow the popup window to extend beyond the bounds of the
 * screen. By default, the window is clipped to the screen boundaries. Setting this to false will
 * allow windows to be accurately positioned.
 * The default value is true.
 * @property usePlatformDefaultWidth Whether the width of the popup's content should be limited to
 * the platform default, which is smaller than the screen width.
 * @property usePlatformInsets Whether the width of the popup's content should be limited by
 * platform insets.
 */
@Immutable
actual class PopupProperties @ExperimentalComposeUiApi constructor(
    actual val focusable: Boolean = false,
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val clippingEnabled: Boolean = true,
    val usePlatformDefaultWidth: Boolean = false,
    val usePlatformInsets: Boolean = true,
) {
    // Constructor with all non-experimental arguments.
    @OptIn(ExperimentalComposeUiApi::class)
    actual constructor(
        focusable: Boolean,
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
        clippingEnabled: Boolean,
    ) : this(
        focusable = focusable,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        clippingEnabled = clippingEnabled,
        usePlatformDefaultWidth = false,
        usePlatformInsets = true,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PopupProperties) return false

        if (focusable != other.focusable) return false
        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (clippingEnabled != other.clippingEnabled) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false

        return true
    }

    override fun hashCode(): Int {
        var result = focusable.hashCode()
        result = 31 * result + dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + clippingEnabled.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        return result
    }
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param focusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Deprecated(
    "Replaced by Popup with properties parameter",
    ReplaceWith("Popup(alignment, offset, onDismissRequest, " +
        "androidx.compose.ui.window.PopupProperties(focusable = focusable), " +
        "onPreviewKeyEvent, onKeyEvent, content)")
)
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    focusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable () -> Unit
) = Popup(
    alignment = alignment,
    offset = offset,
    onDismissRequest = onDismissRequest,
    properties = PopupProperties(
        focusable = focusable,
        dismissOnBackPress = true,
        dismissOnClickOutside = focusable

    ),
    onPreviewKeyEvent = onPreviewKeyEvent,
    onKeyEvent = onKeyEvent,
    content = content
)

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param focusable Indicates if the popup can grab the focus.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Deprecated(
    "Replaced by Popup with properties parameter",
    ReplaceWith("Popup(popupPositionProvider, onDismissRequest, " +
        "androidx.compose.ui.window.PopupProperties(focusable = focusable), " +
        "onPreviewKeyEvent, onKeyEvent, content)")
)
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    focusable: Boolean = false,
    content: @Composable () -> Unit
) = Popup(
    popupPositionProvider = popupPositionProvider,
    onDismissRequest = onDismissRequest,
    properties = PopupProperties(
        focusable = focusable,
        dismissOnBackPress = true,
        dismissOnClickOutside = focusable

    ),
    onPreviewKeyEvent = onPreviewKeyEvent,
    onKeyEvent = onKeyEvent,
    content = content
)

/**
 * Opens a popup with the given content.
 *
 * A popup is a floating container that appears on top of the current activity.
 * It is especially useful for non-modal UI surfaces that remain hidden until they
 * are needed, for example floating menus like Cut/Copy/Paste.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
actual fun Popup(
    alignment: Alignment,
    offset: IntOffset,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit
): Unit = Popup(
    alignment = alignment,
    offset = offset,
    onDismissRequest = onDismissRequest,
    properties = properties,
    onPreviewKeyEvent = null,
    onKeyEvent = null,
    content = content
)

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
actual fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit
): Unit = Popup(
    popupPositionProvider = popupPositionProvider,
    onDismissRequest = onDismissRequest,
    properties = properties,
    onPreviewKeyEvent = null,
    onKeyEvent = null,
    content = content
)

/**
 * Opens a popup with the given content.
 *
 * A popup is a floating container that appears on top of the current activity.
 * It is especially useful for non-modal UI surfaces that remain hidden until they
 * are needed, for example floating menus like Cut/Copy/Paste.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(alignment, offset)
    }
    Popup(
        popupPositionProvider = popupPositioner,
        onDismissRequest = onDismissRequest,
        properties = properties,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnKeyEvent by rememberUpdatedState(onKeyEvent)

    val overriddenOnKeyEvent = if (properties.dismissOnBackPress && onDismissRequest != null) {
        // No need to remember this lambda, as it doesn't capture any values that can change.
        { event: KeyEvent ->
            val consumed = currentOnKeyEvent?.invoke(event) ?: false
            if (!consumed && event.isDismissRequest()) {
                currentOnDismissRequest?.invoke()
                true
            } else {
                consumed
            }
        }
    } else {
        onKeyEvent
    }
    PopupLayout(
        popupPositionProvider = popupPositionProvider,
        focusable = properties.focusable,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.semantics { popup() },
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = overriddenOnKeyEvent,
        content = content,
    )
}

@Composable
internal fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    focusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
    onKeyEvent: ((KeyEvent) -> Boolean)?,
    content: @Composable () -> Unit
) {
    // TODO: Upstream ComposeScene refactor
    val scene = LocalComposeScene.current
    val density = LocalDensity.current

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    var popupBounds by remember { mutableStateOf(IntRect.Zero) }

    // getting parent bounds
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            parentBounds = IntRect(
                coordinates.localToWindow(Offset.Zero).round(),
                coordinates.size
            )
        },
        measurePolicy = { _, _ ->
            layout(0, 0) {}
        }
    )

    val parentComposition = rememberCompositionContext()
    val (owner, composition) = remember {
        val owner = SkiaBasedOwner(
            platformInputService = scene.platformInputService,
            component = scene.component,
            density = density,
            coroutineContext = parentComposition.effectCoroutineContext,
            isPopup = true,
            isFocusable = focusable,
            onDismissRequest = onDismissRequest,
            onPreviewKeyEvent = onPreviewKeyEvent ?: { false },
            onKeyEvent = onKeyEvent ?: { false }
        )
        scene.attach(owner)
        val composition = owner.setContent(parent = parentComposition) {
            Layout(
                content = content,
                modifier = modifier,
                measurePolicy = { measurables, constraints ->
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach {
                            val placeable = it.measure(constraints)
                            val position = popupPositionProvider.calculatePosition(
                                anchorBounds = parentBounds,
                                windowSize = IntSize(width, height),
                                layoutDirection = layoutDirection,
                                popupContentSize = IntSize(placeable.width, placeable.height)
                            )

                            popupBounds = IntRect(
                                position,
                                IntSize(placeable.width, placeable.height)
                            )
                            owner.bounds = popupBounds
                            placeable.place(position.x, position.y)
                        }
                    }
                }
            )
        }
        owner to composition
    }
    owner.density = density
    DisposableEffect(Unit) {
        onDispose {
            scene.detach(owner)
            composition.dispose()
            owner.dispose()
        }
    }
}

private fun KeyEvent.isDismissRequest() =
    type == KeyEventType.KeyDown && key == Key.Escape
