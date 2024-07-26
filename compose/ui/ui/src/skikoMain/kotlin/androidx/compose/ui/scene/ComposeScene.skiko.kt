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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.pointerInteropFilter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import org.jetbrains.skiko.currentNanoTime

/**
 * Represents a static [CompositionLocal] key for a [ComposeScene] in Jetpack Compose.
 *
 * @see ComposeScene
 */
internal val LocalComposeScene = staticCompositionLocalOf<ComposeScene?> { null }

/**
 * The local [ComposeScene] is typically not-null. This extension can be used in these cases.
 */
@Composable
internal fun CompositionLocal<ComposeScene?>.requireCurrent(): ComposeScene {
    return current ?: error("CompositionLocal LocalComposeScene not provided")
}

/**
 * A virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 *
 * After [ComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * It is marked as [InternalComposeUiApi] and used by default Compose entry points
 * (such as application, runComposeUiTest, ComposeWindow). While it can be used by
 * third-party users for integrating Compose into other platforms, it does not come
 * with any guarantee of stability.
 */
@InternalComposeUiApi
interface ComposeScene {
    /**
     * Density of the content which will be used to convert [Dp] units.
     */
    var density: Density

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    var layoutDirection: LayoutDirection

    /**
     * The size (in pixels) is used to impose constraints on the content. If the size is undefined,
     * it can be set to `null`. In such a case, the content will be laid out without any
     * restrictions and the window size will be utilized to bounds verification.
     */
    var size: IntSize?

    /**
     * Top-level composition locals, which will be provided for the Composable content,
     * which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext?

    /**
     * The interface to manages focus within a [ComposeScene].
     *
     * This interface extends the [FocusManager] interface and provides additional functions
     * specific to managing focus within a [ComposeScene].
     * It can be used to request and release focus, as well as retrieve the coordinates of
     * the currently focused item.
     *
     * @see ComposeSceneFocusManager
     * @see FocusManager
     */
    val focusManager: ComposeSceneFocusManager

    /**
     * Close all resources and subscriptions. Not calling this method when [ComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ComposeScene].
     */
    fun close()

    /**
     * Returns the current content size (in pixels) in infinity constraints.
     *
     * @throws IllegalStateException when [ComposeScene] content has lazy layouts without maximum
     * size bounds (e.g. LazyColumn without maximum height).
     */
    fun calculateContentSize(): IntSize

    /**
     * Invalidates position of [ComposeScene] in window. It will trigger callbacks like
     * [Modifier.onGloballyPositioned] so they can recalculate actual position in the window.
     *
     * @see PlatformContext.convertLocalToWindowPosition
     * @see PlatformContext.convertWindowToLocalPosition
     */
    fun invalidatePositionInWindow()

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations(): Boolean

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: Canvas, nanoTime: Long)

    /**
     * Send pointer event to the content.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param position The [Offset] of the current pointer event, relative to the content.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset.Zero,
        timeMillis: Long = currentTimeForEvent(),
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
        button: PointerButton? = null
    )

    /**
     * Send pointer event to the content. The more detailed version of [sendPointerEvent] that can accept
     * multiple pointers.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param pointers The current pointers with position relative to the content.
     * There can be multiple pointers, for example, if we use Touch and touch screen with multiple fingers.
     * Contains only the state of the active pointers.
     * Touch that is released still considered as active on PointerEventType.Release event (but with pressed=false). It
     * is no longer active after that, and shouldn't be passed to the scene.
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset.Zero,
        timeMillis: Long = currentTimeForEvent(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    )

    /**
     * Send [KeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean

    /**
     * Perform hit test and return the [InteropView] associated with the resulting node
     * in case it has a [Modifier.pointerInteropFilter], otherwise return null.
     * @param position The position of the hit test.
     * @return The [InteropView] associated with the resulting node in case there is any, or null.
     */
    fun hitTestInteropView(position: Offset): InteropView?

    /**
     * Creates a new [ComposeSceneLayer] with the specified parameters.
     * It's used to create a new layer for [Popup] or [Dialog].
     *
     * @see rememberComposeSceneLayer
     *
     * @param density The density of the layer.
     * @param layoutDirection The layout direction of the layer.
     * @param focusable Indicates whether the layer is focusable.
     * @param compositionContext The composition context for the layer.
     * @return The created [ComposeSceneLayer].
     */
    fun createLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext,
    ): ComposeSceneLayer
}

private fun currentTimeForEvent(): Long =
    (currentNanoTime() / 1E6).toLong()
