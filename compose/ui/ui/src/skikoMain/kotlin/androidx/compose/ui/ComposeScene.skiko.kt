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

package androidx.compose.ui

import org.jetbrains.skia.Canvas as SkCanvas
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.*
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import org.jetbrains.skiko.currentNanoTime

/**
 * A virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 * To draw content on [SkCanvas], you can use [render] method.
 *
 * To specify available size for the content, you should use [constraints].
 *
 * After [ComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * [ComposeScene] doesn't support concurrent read/write access from different threads. Except:
 * - [hasInvalidations] can be called from any thread
 * - [invalidate] callback can be called from any thread
 */
@Deprecated(
    "Replaced with interface in scene package",
    replaceWith = ReplaceWith("androidx.compose.ui.scene.ComposeScene"),
    level = DeprecationLevel.ERROR
)
class ComposeScene internal constructor(
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    density: Density,
    layoutDirection: LayoutDirection,
    invalidate: () -> Unit
) {
    /**
     * Constructs [ComposeScene]
     *
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param layoutDirection Initial layout direction of the content.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * re-rendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    @ExperimentalComposeUiApi
    constructor(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        invalidate: () -> Unit = {}
    ) : this(
        textInputService = PlatformContext.Empty.textInputService,
        coroutineContext = coroutineContext,
        density = density,
        layoutDirection = layoutDirection,
        invalidate = invalidate
    )

    /**
     * Constructs [ComposeScene]
     *
     * @param textInputService Platform specific text input service
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param layoutDirection Initial layout direction of the content.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * re-rendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    @ExperimentalComposeUiApi
    constructor(
        textInputService: PlatformTextInputService,
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        invalidate: () -> Unit = {}
    ) : this(
        coroutineContext = coroutineContext,
        composeSceneContext = object : ComposeSceneContext {
            override val platformContext = object : PlatformContext by PlatformContext.Empty {
                override val textInputService get() = textInputService
            }
        },
        density = density,
        layoutDirection = layoutDirection,
        invalidate = invalidate,
    )

    /**
     * Constructs [ComposeScene]
     *
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * re-rendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    constructor(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        invalidate: () -> Unit = {}
    ) : this(
        textInputService = PlatformContext.Empty.textInputService,
        coroutineContext = coroutineContext,
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        invalidate = invalidate
    )

    /**
     * Constructs [ComposeScene]
     *
     * @param textInputService Platform specific text input service
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * re-rendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    constructor(
        textInputService: PlatformTextInputService,
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        invalidate: () -> Unit = {}
    ) : this(
        textInputService = textInputService,
        coroutineContext = coroutineContext,
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        invalidate = invalidate
    )

    private val replacement = androidx.compose.ui.scene.CanvasLayersComposeScene(
        density = density,
        layoutDirection = layoutDirection,
        coroutineContext = coroutineContext,
        composeSceneContext = composeSceneContext,
        invalidate = invalidate,
    )

    @Suppress("unused")
    @Deprecated(
        message = "The scene isn't tracking list of roots anymore",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("PlatformContext.RootForTestListener")
    )
    val roots: Set<RootForTest>
        get() = throw NotImplementedError()

    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density by replacement::density

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    @ExperimentalComposeUiApi
    var layoutDirection: LayoutDirection by replacement::layoutDirection

    /**
     * Constraints used to measure and layout content.
     */
    var constraints: Constraints
        get() = replacement.size?.toConstraints() ?: Constraints()
        set(value) { replacement.size = value.toIntSize() }

    /**
     * Returns the current content size
     */
    @Deprecated("Use calculateContentSize() instead", replaceWith = ReplaceWith("calculateContentSize()"))
    val contentSize: IntSize
        get() = replacement.calculateContentSize()

    /**
     * Returns the current content size in infinity constraints.
     *
     * @throws IllegalStateException when [ComposeScene] content has lazy layouts without maximum size bounds
     * (e.g. LazyColumn without maximum height).
     */
    @ExperimentalComposeUiApi
    fun calculateContentSize(): IntSize {
        return replacement.calculateContentSize()
    }

    /**
     * Close all resources and subscriptions. Not calling this method when [ComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ComposeScene].
     */
    fun close() {
        replacement.close()
    }

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations() = replacement.hasInvalidations()

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext? by replacement::compositionLocalContext

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(content: @Composable () -> Unit) {
        replacement.setContent(content)
    }

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: SkCanvas, nanoTime: Long) {
        replacement.render(canvas.asComposeCanvas(), nanoTime)
    }

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
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
        button: PointerButton? = null
    ) {
        replacement.sendPointerEvent(
            eventType, position, scrollDelta, timeMillis, type, buttons, keyboardModifiers, nativeEvent, button
        )
    }

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
    @ExperimentalComposeUiApi
    fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ) {
        replacement.sendPointerEvent(
            eventType, pointers, buttons, keyboardModifiers, scrollDelta, timeMillis, nativeEvent, button
        )
    }

    /**
     * Send [KeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(event: KeyEvent): Boolean {
        return replacement.sendKeyEvent(event)
    }
}

private fun Constraints.toIntSize() =
    if (maxWidth != Constraints.Infinity || maxHeight != Constraints.Infinity) {
        IntSize(width = maxWidth, height = maxHeight)
    } else {
        null
    }

private fun IntSize.toConstraints() = Constraints(maxWidth = width, maxHeight = height)
