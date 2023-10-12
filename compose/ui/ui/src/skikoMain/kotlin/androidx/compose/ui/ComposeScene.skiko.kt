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

import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputElement
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile
import kotlinx.coroutines.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.currentNanoTime

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
 * To draw content on [Canvas], you can use [render] method.
 *
 * To specify available size for the content, you should use [constraints].
 *
 * After [ComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise there can be a memory leak.
 *
 * [ComposeScene] doesn't support concurrent read/write access from different threads. Except:
 * - [hasInvalidations] can be called from any thread
 * - [invalidate] callback can be called from any thread
 */
class ComposeScene internal constructor(
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    internal val platform: Platform,
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    private val invalidate: () -> Unit = {}
) {
    /**
     * Constructs [ComposeScene]
     *
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param layoutDirection Initial layout direction of the content.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * rerendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    @ExperimentalComposeUiApi
    constructor(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        invalidate: () -> Unit = {}
    ) : this(
        coroutineContext,
        Platform.Empty,
        density,
        layoutDirection,
        invalidate
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
     * rerendered. If you draw your content using [render] method, in this callback you should
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
        coroutineContext,
        object : Platform by Platform.Empty {
            override val textInputService: PlatformTextInputService get() = textInputService
        },
        density,
        layoutDirection,
        invalidate,
    )

    /**
     * Constructs [ComposeScene]
     *
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * rerendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    constructor(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        invalidate: () -> Unit = {}
    ) : this(
        coroutineContext,
        density,
        LayoutDirection.Ltr,
        invalidate
    )

    /**
     * Constructs [ComposeScene]
     *
     * @param textInputService Platform specific text input service
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * rerendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    constructor(
        textInputService: PlatformTextInputService,
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        invalidate: () -> Unit = {}
    ) : this(
        textInputService,
        coroutineContext,
        density,
        LayoutDirection.Ltr,
        invalidate,
    )

    private var isInvalidationDisabled = false

    @Volatile
    private var hasPendingDraws = true
    private inline fun <T> postponeInvalidation(crossinline block: () -> T): T {
        check(!isClosed) { "ComposeScene is closed" }
        isInvalidationDisabled = true
        val result = try {
            // Try to get see the up-to-date state before running block
            // Note that this doesn't guarantee it, if sendApplyNotifications is called concurrently
            // in a different thread than this code.
            sendAndPerformSnapshotChanges()
            block()
        } finally {
            isInvalidationDisabled = false
        }
        invalidateIfNeeded()
        return result
    }

    private fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters || needLayout || needDraw ||
            snapshotChanges.hasCommands || syntheticEventSender.needUpdatePointerPosition
        if (hasPendingDraws && !isInvalidationDisabled && !isClosed) {
            invalidate()
        }
    }

    private var needLayout = true
    private var needDraw = true

    private fun requestLayout() {
        needLayout = true
        invalidateIfNeeded()
    }

    private fun requestDraw() {
        needDraw = true
        invalidateIfNeeded()
    }

    private fun requestUpdatePointer() {
        syntheticEventSender.needUpdatePointerPosition = true
        invalidateIfNeeded()
    }

    internal fun onPointerUpdate() {
        requestUpdatePointer()
    }

    /**
     * Contains all registered [SkiaBasedOwner] (main frame, popups, etc.) in order of registration.
     * So that Popup opened from main owner will have bigger index.
     * This logic is used by accessibility.
     */
    internal val owners = mutableListOf<SkiaBasedOwner>()
    private val listCopy = mutableListOf<SkiaBasedOwner>()

    private inline fun forEachOwner(action: (SkiaBasedOwner) -> Unit) {
        listCopy.addAll(owners)
        try {
            listCopy.fastForEach(action)
        } finally {
            listCopy.clear()
        }
    }

    private inline fun forEachOwnerReversed(action: (SkiaBasedOwner) -> Unit) {
        listCopy.addAll(owners)
        try {
            listCopy.fastForEachReversed(action)
        } finally {
            listCopy.clear()
        }
    }

    private val childScenes = mutableSetOf<ComposeScene>()

    /**
     * Adds a child [ComposeScene], so that nodes in it can be found in tests.
     */
    internal fun addChildScene(scene: ComposeScene) {
        check(!isClosed) { "ComposeScene is closed" }
        childScenes.add(scene)
    }

    /**
     * Removes a child [ComposeScene].
     */
    internal fun removeChildScene(scene: ComposeScene) {
        check(!isClosed) { "ComposeScene is closed" }
        childScenes.remove(scene)
    }

    /**
     * Adds this scene's [RootForTest]s, including any of its child scenes, into the given set.
     */
    private fun addRootsForTestTo(target: MutableSet<RootForTest>) {
        target.addAll(owners)
        for (child in childScenes) {
            child.addRootsForTestTo(target)
        }
    }

    /**
     * All currently registered [RootForTest]s. After calling [setContent] the first root
     * will be added. If there is an any [Popup] is present in the content, it will be added as
     * another [RootForTest]
     */
    val roots: Set<RootForTest>
        get() = buildSet(owners.size) {
            addRootsForTestTo(this)
        }

    private val defaultPointerStateTracker = DefaultPointerStateTracker()

    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    // We use FlushCoroutineDispatcher for effectDispatcher not because we need `flush` for
    // LaunchEffect tasks, but because we need to know if it is idle (hasn't scheduled tasks)
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)
    private val syntheticEventSender = SyntheticEventSender(::processPointerInput)

    internal var mainOwner: SkiaBasedOwner? = null
    private var composition: Composition? = null

    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density = density
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner?.density = value
        }

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    @ExperimentalComposeUiApi
    var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner?.layoutDirection = value
        }

    private var isClosed = false

    /**
     * Provides mapping pointer id -> current pointer position inside [ComposeScene]
     */
    internal val pointerPositions = mutableMapOf<PointerId, Offset>()

    init {
        GlobalSnapshotManager.ensureStarted()
        coroutineScope.launch(
            recomposeDispatcher + frameClock,
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
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
        composition?.dispose()
        mainOwner?.dispose()
        recomposer.cancel()
        job.cancel()
        isClosed = true
    }

    private val snapshotChanges = CommandList(::invalidateIfNeeded)

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations() = hasPendingDraws ||
        recomposer.hasPendingWork ||
        effectDispatcher.hasTasks() ||
        recomposeDispatcher.hasTasks()

    internal fun attach(owner: SkiaBasedOwner) {
        check(!isClosed) { "ComposeScene is closed" }
        owners.add(owner)
        owner.requestLayout = ::requestLayout
        owner.requestDraw = ::requestDraw
        owner.dispatchSnapshotChanges = snapshotChanges::add
        owner.constraints = constraints
        if (owner.focusable) {
            focusedOwner = owner

            // Exit event to lastHoverOwner will be sent via synthetic event on next frame
        }
        if (isFocused) {
            owner.focusOwner.takeFocus()
        } else {
            owner.focusOwner.releaseFocus()
        }
        requestUpdatePointer()
        invalidateIfNeeded()
    }

    internal fun detach(owner: SkiaBasedOwner) {
        check(!isClosed) { "ComposeScene is closed" }
        owners.remove(owner)
        owner.dispatchSnapshotChanges = null
        owner.requestDraw = null
        owner.requestLayout = null
        if (owner == focusedOwner) {
            focusedOwner = owners.lastOrNull { it.focusable }

            // Enter event to new focusedOwner will be sent via synthetic event on next frame
        }
        if (owner == lastHoverOwner) {
            lastHoverOwner = null
        }
        if (owner == gestureOwner) {
            gestureOwner = null
        }
        requestUpdatePointer()
        invalidateIfNeeded()
    }

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext? by mutableStateOf(null)

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(
        content: @Composable () -> Unit
    ) = setContent(
        parentComposition = null,
        content = content
    )

    // TODO(demin): We should configure routing of key events if there
    //  are any popups/root present:
    //   - ComposeScene.sendKeyEvent
    //   - ComposeScene.onPreviewKeyEvent (or Window.onPreviewKeyEvent)
    //   - Popup.onPreviewKeyEvent
    //   - NestedPopup.onPreviewKeyEvent
    //   - NestedPopup.onKeyEvent
    //   - Popup.onKeyEvent
    //   - ComposeScene.onKeyEvent
    //  Currently we have this routing:
    //   - [active Popup or the main content].onPreviewKeyEvent
    //   - [active Popup or the main content].onKeyEvent
    //   After we change routing, we can remove onPreviewKeyEvent/onKeyEvent from this method
    internal fun setContent(
        parentComposition: CompositionContext? = null,
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        check(!isClosed) { "ComposeScene is closed" }
        syntheticEventSender.reset()
        composition?.dispose()
        mainOwner?.dispose()
        val mainOwner = SkiaBasedOwner(
            this,
            platform,
            platform.focusManager,
            initDensity = density,
            initLayoutDirection = layoutDirection,
            coroutineContext = recomposer.effectCoroutineContext,
            bounds = IntSize(constraints.maxWidth, constraints.maxHeight).toIntRect(),
            onPointerUpdate = ::onPointerUpdate,
            modifier = KeyInputElement(onKeyEvent = onKeyEvent, onPreKeyEvent = onPreviewKeyEvent)
        )
        attach(mainOwner)
        composition = mainOwner.setContent(
            parentComposition ?: recomposer,
            { compositionLocalContext }
        ) {
            CompositionLocalProvider(
                LocalComposeScene provides this,
                content = content
            )
        }
        this.mainOwner = mainOwner

        // to perform all pending work synchronously
        recomposeDispatcher.flush()
    }

    /**
     * Constraints used to measure and layout content.
     */
    var constraints: Constraints = Constraints()
        set(value) {
            field = value
            forEachOwner {
                it.constraints = constraints
            }
            mainOwner?.bounds = IntSize(constraints.maxWidth, constraints.maxHeight).toIntRect()
        }

    /**
     * Returns the current content size
     */
    val contentSize: IntSize
        get() {
            check(!isClosed) { "ComposeScene is closed" }
            val mainOwner = mainOwner ?: return IntSize.Zero
            mainOwner.measureAndLayout()
            return mainOwner.contentSize
        }

    /**
     * Sends any pending apply notifications and performs the changes they cause.
     */
    private fun sendAndPerformSnapshotChanges() {
        Snapshot.sendApplyNotifications()
        snapshotChanges.perform()
    }

    internal fun hitTestInteropView(position: Offset): Boolean {
        // TODO:
        //  Temporary solution copying control flow from [processPress].
        //  A proper solution is to send touches to scene as black box
        //  and handle only that ones that were received in interop view
        //  instead of using [pointInside].
        owners.fastForEachReversed { owner ->
            if (owner.isInBounds(position)) {
                return owner.hitInteropView(
                    pointerPosition = position,
                    isTouchEvent = true,
                )
            } else if (owner == focusedOwner) {
                return false
            }
        }

        // We didn't pass isInBounds check for any owner 🤷
        return false
    }

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: Canvas, nanoTime: Long): Unit = postponeInvalidation {
        recomposeDispatcher.flush()
        frameClock.sendFrame(nanoTime) // Recomposition
        sendAndPerformSnapshotChanges() // Apply changes from recomposition phase to layout phase
        needLayout = false
        forEachOwner { it.measureAndLayout() }
        syntheticEventSender.updatePointerPosition()
        sendAndPerformSnapshotChanges()  // Apply changes from layout phase to draw phase
        needDraw = false
        forEachOwner { it.draw(canvas) }
        forEachOwner { it.clearInvalidObservations() }
    }

    private var focusedOwner: SkiaBasedOwner? = null
    private var gestureOwner: SkiaBasedOwner? = null
    private var lastHoverOwner: SkiaBasedOwner? = null

    /**
     * Find hovered owner for position of first pointer.
     */
    private fun hoveredOwner(event: PointerInputEvent): SkiaBasedOwner? {
        val position = event.pointers.first().position
        return owners.lastOrNull { it.isInBounds(position) }
    }

    /**
     * Check if [focusedOwner] blocks input for this owner.
     */
    private fun isInteractive(owner: SkiaBasedOwner?) =
        focusedOwner == null || owner == null ||
            owners.indexOf(focusedOwner) <= owners.indexOf(owner)


    // TODO(demin): return Boolean (when it is consumed)
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
    @OptIn(ExperimentalComposeUiApi::class)
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
        defaultPointerStateTracker.onPointerEvent(button, eventType)

        val actualButtons = buttons ?: defaultPointerStateTracker.buttons
        val actualKeyboardModifiers =
            keyboardModifiers ?: defaultPointerStateTracker.keyboardModifiers

        sendPointerEvent(
            eventType,
            listOf(Pointer(PointerId(0), position, actualButtons.areAnyPressed, type)),
            actualButtons,
            actualKeyboardModifiers,
            scrollDelta,
            timeMillis,
            nativeEvent,
            button
        )
    }

    // TODO(demin): return Boolean (when it is consumed)
    // TODO(demin) verify that pressure is the same on Android and iOS
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
        pointers: List<Pointer>,
        buttons: PointerButtons = PointerButtons(),
        keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(),
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
    ): Unit = postponeInvalidation {
        val event = pointerInputEvent(
            eventType,
            pointers,
            timeMillis,
            nativeEvent,
            scrollDelta,
            buttons,
            keyboardModifiers,
            button,
        )
        needLayout = false
        forEachOwner { it.measureAndLayout() }
        syntheticEventSender.updatePointerPosition()
        syntheticEventSender.send(event)
        updatePointerPositions(event)
    }

    private fun updatePointerPositions(event: PointerInputEvent) {
        // update positions for pointers that are down + mouse (if it is not Exit event)
        for (pointer in event.pointers) {
            if ((pointer.type == PointerType.Mouse && event.eventType != PointerEventType.Exit) ||
                pointer.down
            ) {
                pointerPositions[pointer.id] = pointer.position
            }
        }
        // touches/styluses positions should be removed from [pointerPositions] if they are not down anymore
        // also, mouse exited ComposeScene should be removed
        val iterator = pointerPositions.iterator()
        while (iterator.hasNext()) {
            val pointerId = iterator.next().key
            val pointer = event.pointers.find { it.id == pointerId } ?: continue
            if ((pointer.type != PointerType.Mouse && !pointer.down) ||
                (pointer.type == PointerType.Mouse && event.eventType == PointerEventType.Exit)
            ) {
                iterator.remove()
            }
        }
    }

    private fun processPointerInput(event: PointerInputEvent) {
        when (event.eventType) {
            PointerEventType.Press -> processPress(event)
            PointerEventType.Release -> processRelease(event)
            PointerEventType.Move -> processMove(event)
            PointerEventType.Enter -> processMove(event)
            PointerEventType.Exit -> processMove(event)
            PointerEventType.Scroll -> processScroll(event)
        }

        // Clean gestureOwner when there is no pressed pointers/buttons
        if (!event.isGestureInProgress) {
            gestureOwner = null
        }
    }

    private fun processPress(event: PointerInputEvent) {
        val currentGestureOwner = gestureOwner
        if (currentGestureOwner != null) {
            currentGestureOwner.processPointerInput(event)
            return
        }
        val position = event.pointers.first().position
        forEachOwnerReversed { owner ->

            // If the position of in bounds of the owner - send event to it and stop processing
            if (owner.isInBounds(position)) {
                owner.processPointerInput(event)
                gestureOwner = owner
                return
            }

            // Input event is out of bounds - send click outside notification
            owner.onOutsidePointerEvent?.invoke(event)

            // if the owner is in focus, do not pass the event to underlying owners
            if (owner == focusedOwner) {
                return
            }
        }
    }

    private fun processRelease(event: PointerInputEvent) {
        // Send Release to gestureOwner even if is not hovered or under focusedOwner
        gestureOwner?.processPointerInput(event)
        if (!event.isGestureInProgress) {
            val owner = hoveredOwner(event)
            if (isInteractive(owner)) {
                processHover(event, owner)
            } else if (gestureOwner == null) {
                // If hovered owner is not interactive, then it means that
                // - It's not focusedOwner
                // - It placed under focusedOwner or not exist at all
                // In all these cases the even happened outside focused owner bounds
                focusedOwner?.onOutsidePointerEvent?.invoke(event)
            }
        }
    }

    private fun processMove(event: PointerInputEvent) {
        var owner = when {
            // All touch events or mouse with pressed button(s)
            event.isGestureInProgress -> gestureOwner

            // Do not generate Enter and Move
            event.eventType == PointerEventType.Exit -> null

            // Find owner under mouse position
            else -> hoveredOwner(event)
        }

        // Even if the owner is not interactive, hover state still need to be updated
        if (!isInteractive(owner)) {
            owner = null
        }
        if (processHover(event, owner)) {
            return
        }
        owner?.processPointerInput(
            event.copy(eventType = PointerEventType.Move)
        )
    }

    /**
     * Updates hover state and generates [PointerEventType.Enter] and [PointerEventType.Exit]
     * events. Returns true if [event] is consumed.
     */
    private fun processHover(event: PointerInputEvent, owner: SkiaBasedOwner?): Boolean {
        if (event.pointers.fastAny { it.type != PointerType.Mouse }) {
            // Track hover only for mouse
            return false
        }
        // Cases:
        // - move from outside to the window (owner != null, lastMoveOwner == null): Enter
        // - move from the window to outside (owner == null, lastMoveOwner != null): Exit
        // - move from one point of the window to another (owner == lastMoveOwner): Move
        // - move from one popup to another (owner != lastMoveOwner): [Popup 1] Exit, [Popup 2] Enter
        if (owner == lastHoverOwner) {
            // Owner wasn't changed
            return false
        }
        lastHoverOwner?.processPointerInput(
            event.copy(eventType = PointerEventType.Exit),
            isInBounds = false
        )
        owner?.processPointerInput(
            event.copy(eventType = PointerEventType.Enter)
        )
        lastHoverOwner = owner

        // Changing hovering state replaces Move event, so treat it as consumed
        return true
    }

    private fun processScroll(event: PointerInputEvent) {
        val owner = hoveredOwner(event)
        if (isInteractive(owner)) {
            owner?.processPointerInput(event)
        }
    }

    /**
     * Send [KeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(event: ComposeKeyEvent): Boolean = postponeInvalidation {
        defaultPointerStateTracker.onKeyEvent(event)
        focusedOwner?.sendKeyEvent(event) == true
    }

    private var isFocused = true

    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     */
    @ExperimentalComposeUiApi
    fun releaseFocus() {
        owners.forEach {
            it.focusOwner.releaseFocus()
        }
        isFocused = false
    }

    @ExperimentalComposeUiApi
    fun requestFocus() {
        owners.findLast { it.focusable }?.focusOwner?.takeFocus()
        isFocused = true
    }

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusProperties()][focusProperties].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    @ExperimentalComposeUiApi
    fun moveFocus(focusDirection: FocusDirection): Boolean =
        owners.lastOrNull()?.focusOwner?.moveFocus(focusDirection) ?: false

    /**
     * Represents pointer such as mouse cursor, or touch/stylus press.
     * There can be multiple pointers on the screen at the same time.
     */
    @ExperimentalComposeUiApi
    class Pointer(
        /**
         * Unique id associated with the pointer. Used to distinguish between multiple pointers that can exist
         * at the same time (i.e. multiple pressed touches).
         */
        val id: PointerId,

        /**
         * The [Offset] of the pointer.
         */
        val position: Offset,

        /**
         * `true` if the pointer event is considered "pressed". For example,
         * a finger touches the screen or any mouse button is pressed.
         *  During the up event, pointer is considered not pressed.
         */
        val pressed: Boolean,

        /**
         * The device type associated with the pointer, such as [mouse][PointerType.Mouse],
         * or [touch][PointerType.Touch].
         */
        val type: PointerType = PointerType.Mouse,

        /**
         * Pressure of the pointer. 0.0 - no pressure, 1.0 - average pressure
         */
        val pressure: Float = 1.0f,

        /**
         * High-frequency pointer moves in between the current event and the last event.
         * can be used for extra accuracy when touchscreen rate exceeds framerate.
         *
         * Can be empty, if a platform doesn't provide any.
         *
         * For example, on iOS this list is populated using the data of.
         * https://developer.apple.com/documentation/uikit/uievent/1613808-coalescedtouchesfortouch?language=objc
         */
        val historical: List<HistoricalChange> = emptyList()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Pointer

            if (position != other.position) return false
            if (pressed != other.pressed) return false
            if (type != other.type) return false
            if (id != other.id) return false
            if (pressure != other.pressure) return false

            return true
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + pressed.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + pressure.hashCode()
            return result
        }

        override fun toString(): String {
            return "Pointer(position=$position, pressed=$pressed, type=$type, id=$id, pressure=$pressure)"
        }
    }
}

private class DefaultPointerStateTracker {
    fun onPointerEvent(button: PointerButton?, eventType: PointerEventType) {
        when (eventType) {
            PointerEventType.Press -> buttons = buttons.copyFor(button ?: PointerButton.Primary, pressed = true)
            PointerEventType.Release -> buttons = buttons.copyFor(button ?: PointerButton.Primary, pressed = false)
        }
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        keyboardModifiers = keyEvent.nativeKeyEvent.toPointerKeyboardModifiers()
    }

    var buttons = PointerButtons()
        private set

    var keyboardModifiers = PointerKeyboardModifiers()
        private set
}

@OptIn(ExperimentalComposeUiApi::class)
private fun pointerInputEvent(
    eventType: PointerEventType,
    pointers: List<ComposeScene.Pointer>,
    timeMillis: Long,
    nativeEvent: Any?,
    scrollDelta: Offset,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
    changedButton: PointerButton?
): PointerInputEvent {
    return PointerInputEvent(
        eventType,
        timeMillis,
        pointers.map {
            PointerInputEventData(
                it.id,
                timeMillis,
                it.position,
                it.position,
                it.pressed,
                it.pressure,
                it.type,
                issuesEnterExit = it.type == PointerType.Mouse,
                historical = it.historical,
                scrollDelta = scrollDelta
            )
        },
        buttons,
        keyboardModifiers,
        nativeEvent,
        changedButton
    )
}

internal expect fun createSkiaLayer(): SkiaLayer

internal expect fun NativeKeyEvent.toPointerKeyboardModifiers(): PointerKeyboardModifiers

private val PointerInputEvent.isGestureInProgress get() = pointers.fastAny { it.down }