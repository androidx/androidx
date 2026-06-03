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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.draganddrop.UIKitDragAndDropManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.hapticfeedback.CupertinoHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.PointerKeyboardModifiers
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.OffsetToFocusedRect
import androidx.compose.ui.navigationevent.UIKitNavigationEventInput
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.CUPERTINO_TOUCH_SLOP
import androidx.compose.ui.platform.DefaultInputModeManager
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformScreenReader
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.UIKitIdleTimerManager
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.UIKitWindowInsetsManager
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalNativeTextInputContext
import androidx.compose.ui.uikit.LocalUIView
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.toNanoSeconds
import androidx.compose.ui.input.key.internal
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TrackInteropPlacementContainer
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.BackgroundInputView
import androidx.compose.ui.window.ComposeSceneKeyboardOffsetManager
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.KeyboardVisibilityListener
import androidx.compose.ui.window.MetalRedrawer
import androidx.compose.ui.window.OverlayInputView
import androidx.compose.ui.window.TouchesEventKind
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CATransaction
import platform.UIKit.UIEvent
import platform.UIKit.UIEventButtonMaskPrimary
import platform.UIKit.UIEventButtonMaskSecondary
import platform.UIKit.UIPress
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UITouchTypeDirect
import platform.UIKit.UITouchTypeIndirect
import platform.UIKit.UITouchTypeIndirectPointer
import platform.UIKit.UITouchTypePencil
import platform.UIKit.UIView

/**
 * iOS specific-implementation of [PlatformContext.SemanticsOwnerListener] used to track changes in [SemanticsOwner].
 *
 * @property view The UI container associated with the semantics owner.
 * @property coroutineContext The coroutine context to use for handling semantics changes.
 * @property performEscape A lambda to delegate accessibility escape operation. Returns true if the escape was handled, false otherwise.
 */
private class SemanticsOwnerListenerImpl(
    private val view: UIView,
    private val coroutineContext: CoroutineContext,
    private val performEscape: () -> Boolean,
    private val onScreenReaderActive: (Boolean) -> Unit,
) : PlatformContext.SemanticsOwnerListener {

    private var accessibilityMediator: AccessibilityMediator? = null

    var isEnabled: Boolean = false
        set(value) {
            field = value
            accessibilityMediator?.isEnabled = value
        }

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator == null) {
            accessibilityMediator = AccessibilityMediator(
                view,
                semanticsOwner,
                coroutineContext,
                performEscape,
                onScreenReaderActive
            ).also {
                it.isEnabled = isEnabled
            }
        }
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.dispose()
            accessibilityMediator = null
            onScreenReaderActive(false)
        }
    }

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.onSemanticsChange()
        }
    }

    override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
        if (accessibilityMediator?.owner == semanticsOwner) {
            accessibilityMediator?.onLayoutChange(nodeId = semanticsNodeId)
        }
    }

    val hasInvalidations: Boolean get() = accessibilityMediator?.hasPendingInvalidations ?: false

    fun dispose() {
        accessibilityMediator?.dispose()
        accessibilityMediator = null
    }
}

internal class ComposeSceneMediator(
    private val onFocusBehavior: OnFocusBehavior,
    private val isClearFocusOnMouseDownEnabled: Boolean,
    focusedViewsList: FocusedViewsList?,
    private val windowContext: PlatformWindowContext,
    private val architectureComponentsOwner: PlatformArchitectureComponentsOwner,
    private val coroutineContext: CoroutineContext,
    private val redrawer: MetalRedrawer,
    private val navigationEventInput: UIKitNavigationEventInput,
    interfaceOrientationState: State<InterfaceOrientation>,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        frameRecomposer: FrameRecomposer,
    ) -> ComposeScene,
) {
    private var onPreviewKeyEvent: (KeyEvent) -> Boolean = { false }

    private var onKeyEvent: (KeyEvent) -> Boolean = { false }
    private var animateKeyboardOffsetChanges by mutableStateOf(false)
    private var platformScreenReader = object : PlatformScreenReader {
        override var isActive by mutableStateOf(false)
    }

    private val coroutineScope = CoroutineScope(coroutineContext)

    private val isActive get() = coroutineContext.isActive

    private val viewConfiguration: ViewConfiguration =
        object : ViewConfiguration by PlatformContext.DefaultViewConfiguration {
            override val touchSlop: Float
                get() = with(screenDensity) {
                    // this value is originating from iOS 16 drag behavior reverse engineering
                    CUPERTINO_TOUCH_SLOP.dp.toPx()
                }
        }

    // TODO: It must be shared between Compose instances.
    //  It's supposed to be stored in platform's root view or window.
    val frameRecomposer = FrameRecomposer(coroutineContext, redrawer::setNeedsRedraw)

    // TODO: It cannot be used in case of shared [FrameRecomposer], replace this helper with calling
    //  - [frameRecomposer.performFrame] once per frame (across all instances) before platform views layout phase
    //  - [scene.measureAndLayout] during platform views layout phase. Note that it should be triggered
    //    by platform view invalidation (which is triggered by [scene.invalidateLayout] OR by regular platform invalidation)
    //  - [scene.draw] during drawing phase of platform views (which is triggered by [scene.invalidateDraw]).
    //    Note that in case of custom GPU surface/V-Sync handling, it needs to be handled differently.
    private val sceneRenderingScope = SingleComposeSceneRenderingScope(redrawer::setNeedsRedraw)

    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            sceneRenderingScope::onSceneInvalidation,
            PlatformContextImpl(),
            frameRecomposer,
        )
    }

    private var composeSceneSize: IntSize?
        get() = scene.size
        set(value) {
            if (isActive) {
                scene.size = value
                if (value != null) {
                    windowInsetsManager.sceneSize.value = value
                }
            }
        }

    /**
     * Density used by the Compose scene for dp/px conversions within Compose.
     *
     * This value is intentionally separate from [screenDensity] so we can support setting custom
     * composeSceneDensity without regressions (merging [screenDensity] and [composeSceneDensity]
     * into one causes rendering and interaction issues because they are semantically different).
     */
    var composeSceneDensity: Density
        get() = scene.density
        set(value) {
            if (isActive) {
                scene.density = value
            }
        }

    /**
     * Density of the hosting UIKit screen.
     *
     * This value is intentionally separate from [composeSceneDensity] so we can support setting
     * composeSceneDensity without regressions.
     */
    val screenDensity: Density get() = _overlayView.density

    var layoutDirection: LayoutDirection
        get() = scene.layoutDirection
        set(value) {
            if (isActive) {
                scene.layoutDirection = value
            }
        }

    var compositionLocalContext: CompositionLocalContext?
        get() = scene.compositionLocalContext
        set(value) {
            if (isActive) {
                scene.compositionLocalContext = value
            }
        }

    val hasInteropViews: Boolean get() = interopContainer.hasInteropViews

    /**
     * Primary view to handle user input.
     * Also, it is used as a root container view for accessibility and text input.
     */
    private val _overlayView = OverlayInputView(
        hitTestInteropView = ::hitTestInteropView,
        isPointInsideInteractionBounds = ::isPointInsideInteractionBounds,
        onTouchesEvent = ::onTouchesEvent,
        onCancelAllTouches = ::onCancelAllTouches,
        onScrollEvent = ::onScrollEvent,
        onCancelScroll = ::onCancelScroll,
        onHoverEvent = ::onHoverEvent,
        onKeyboardPresses = ::onKeyboardPresses,
        ignoreTouchChanges = navigationEventInput::isBackGestureActive,
        onRemoveSubview = {
            CoroutineScope(coroutineContext).launch {
                finishUnattachedKeysPresses()
            }
        }
    )

    val overlayView: UIView get() = _overlayView

    /**
     * A holder for interop views that located below the Metal canvas.
     * The view handles user touches that occur only over the interop views located on it.
     */
    private val _backgroundView = BackgroundInputView(
        onMovedToWindow = ::focusOverlayViewIfNeeded,
        onLayoutSubviews = ::updateLayout,
        hitTestInteropView = ::hitTestInteropView,
        isPointInsideInteractionBounds = ::isPointInsideInteractionBounds,
        onTouchesEvent = ::onTouchesEvent,
        onCancelAllTouches = ::onCancelAllTouches,
        ignoreTouchChanges = navigationEventInput::isBackGestureActive
    )

    val backgroundView: UIView get() = _backgroundView

    /**
     * Container for managing UIKitView and UIKitViewController
     */
    private val interopContainer = UIKitInteropContainer(
        overlayContainer = _overlayView,
        backgroundContainer = _backgroundView,
        requestRedraw = redrawer::setNeedsRedraw
    )

    private val dragAndDropManager = UIKitDragAndDropManager(
        view = _overlayView,
        getComposeRootDragAndDropNode = { scene.rootDragAndDropNode },
    )

    private val windowInsetsManager = UIKitWindowInsetsManager(
        windowInsetsViews = listOf(
            { _overlayView },
            { windowContext.window?.rootViewController?.view },
        ),
        interfaceOrientation = interfaceOrientationState
    )

    /**
     * A callback to define whether the precondition for the user input view hit test is met.
     *
     * @param point Point in the interaction view coordinate space.
     */
    private fun isPointInsideInteractionBounds(point: CValue<CGPoint>) =
        interactionBounds.contains(point.toDpOffset().toOffset(screenDensity).round())

    private val semanticsOwnerListener by lazy {
        SemanticsOwnerListenerImpl(
            view = _overlayView,
            coroutineContext = coroutineContext,
            performEscape = {
                val down = onKeyboardEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown))
                val up = onKeyboardEvent(KeyEvent(Key.Escape, KeyEventType.KeyUp))

                down || up
            },
            onScreenReaderActive = { platformScreenReader.isActive = it }
        )
    }

    var isFocusEnabled: Boolean
        get() = semanticsOwnerListener.isEnabled
        set(value) {
            semanticsOwnerListener.isEnabled = value
            if (value) {
                focusOverlayViewIfNeeded()
            } else {
                _overlayView.resignFirstResponder()
            }
        }

    private val keyboardManager by lazy {
        ComposeSceneKeyboardOffsetManager(
            view = _overlayView,
            keyboardOverlapHeightChanged = { height ->
                val heightPx = with(screenDensity) { height.roundToPx() }
                if (windowInsetsManager.keyboardOverlapHeight.value != heightPx) {
                    animateKeyboardOffsetChanges = false
                    windowInsetsManager.keyboardOverlapHeight.value = heightPx
                }
            }
        )
    }

    private val textInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                frameRecomposer.performFrame(lastRenderTime)
                scene.measureAndLayout()
                CATransaction.flush()
            },
            view = _overlayView,
            viewConfiguration = viewConfiguration,
            focusedViewsList = focusedViewsList,
            onInputStarted = { animateKeyboardOffsetChanges = true },
            focusManager = { scene.focusManager },
            coroutineContext = coroutineContext,
        ).also {
            KeyboardVisibilityListener.initialize()
        }
    }

    private val textInputServiceAdapter by lazy {
        UIKitTextInputServiceAdapter(
            textInputService,
            coroutineScope
        )
    }

    val hasInvalidations: Boolean
        get() = frameRecomposer.hasPendingWork() ||
            scene.hasInvalidations() ||
            keyboardManager.isAnimating ||
            isLayoutTransitionAnimating ||
            semanticsOwnerListener.hasInvalidations ||
            textInputService.hasInvalidations

    init {
        coroutineContext.job.invokeOnCompletion { dispose() }
    }

    private fun hitTestInteropView(point: CValue<CGPoint>): UIView? =
        point.useContents {
            val position = toDpOffset().toOffset(composeSceneDensity)
            val interopView = scene.hitTestInteropView(position)

            // Find a group of a holder associated with a given interop view or view controller
            interopView?.let {
                interopContainer.groupForInteropView(it)
            }
        }

    private fun onScrollEvent(
        position: DpOffset,
        delta: DpOffset,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ) {
        when (eventKind) {
            TouchesEventKind.BEGAN -> redrawer.ongoingInteractionEventsCount += 1
            TouchesEventKind.MOVED -> {}
            TouchesEventKind.ENDED -> redrawer.ongoingInteractionEventsCount -= 1
        }

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            pointers = listOf(
                ComposeScenePointer(
                    id = PointerId(0),
                    position = position.toOffset(composeSceneDensity),
                    pressed = false,
                    type = PointerType.Mouse,
                )
            ),
            scrollDelta = delta.toOffset(composeSceneDensity) * SCROLL_DELTA_MULTIPLIER,
            timeMillis = event.timeMillis,
            nativeEvent = event,
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        )
    }

    private fun onHoverEvent(
        position: DpOffset,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ) {
        val eventType = when (eventKind) {
            TouchesEventKind.BEGAN -> PointerEventType.Enter
            TouchesEventKind.MOVED -> PointerEventType.Move
            TouchesEventKind.ENDED -> PointerEventType.Exit
        }

        scene.sendPointerEvent(
            eventType = eventType,
            pointers = listOf(
                ComposeScenePointer(
                    id = PointerId(0),
                    position = position.toOffset(composeSceneDensity),
                    pressed = false,
                    type = PointerType.Mouse,
                )
            ),
            timeMillis = event.timeMillis,
            nativeEvent = event,
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        )
    }

    private fun onCancelScroll() {
        redrawer.ongoingInteractionEventsCount -= 1
        scene.cancelPointerInput()
    }

    private fun onCancelAllTouches(touches: Set<*>) {
        redrawer.ongoingInteractionEventsCount -= touches.count()
        scene.cancelPointerInput()
    }

    /**
     * Converts [UITouch] objects from [touches] to [ComposeScenePointer] and dispatches them to the appropriate handlers.
     * @param touches a [Set] of [UITouch] objects. Erasure happens due to K/N not supporting Obj-C lightweight generics.
     * @param event the [UIEvent] associated with the touches
     * @param eventKind the [TouchesEventKind] of the touches
     */
    private fun onTouchesEvent(
        touches: Set<*>,
        event: UIEvent?,
        eventKind: TouchesEventKind
    ): PointerEventResult {
        when (eventKind) {
            TouchesEventKind.BEGAN -> redrawer.ongoingInteractionEventsCount += touches.count()
            TouchesEventKind.ENDED -> redrawer.ongoingInteractionEventsCount -= touches.count()
            TouchesEventKind.MOVED -> {}
        }

        val pointers = touches.mapIndexed { index, touch ->
            touch as UITouch
            val position = touch.offsetInView(_backgroundView, screenDensity.density)
            val pointerType = when (touch.type) {
                UITouchTypeDirect -> PointerType.Touch
                UITouchTypeIndirect, UITouchTypeIndirectPointer -> PointerType.Mouse
                UITouchTypePencil -> PointerType.Stylus
                else -> PointerType.Touch
            }
            val id = touch.hashCode().toLong().takeIf {
                pointerType != PointerType.Mouse
            } ?: index.toLong()
            ComposeScenePointer(
                id = PointerId(id),
                position = position,
                pressed = touch.isPressed,
                type = pointerType,
                pressure = touch.force.toFloat(),
                historical = event?.historicalChangesForTouch(
                    touch,
                    _overlayView,
                    screenDensity.density
                ) ?: emptyList()
            )
        }

        // UIKit sends buttonMask that was before the release action. It should be empty if no
        // pressed pointers left.
        val pointerButtonsMask = event.buttonMaskOrZero.takeIf {
            pointers.any { it.pressed }
        } ?: 0L

        return scene.sendPointerEvent(
            eventType = eventKind.toPointerEventType(),
            pointers = pointers,
            timeMillis = event.timeMillis,
            nativeEvent = event,
            button = event?.getButton(previousButtonMask, eventKind, previousTouchEventKind),
            buttons = PointerButtons(pointerButtonsMask),
            keyboardModifiers = PointerKeyboardModifiers(event.modifierFlagsOrZero)
        ).also {
            previousButtonMask = event.buttonMaskOrZero
            if (eventKind != TouchesEventKind.MOVED) {
                previousTouchEventKind = eventKind
            }
        }
    }
    private var previousButtonMask: Long = 0L
    private var previousTouchEventKind: TouchesEventKind? = null

    private var lastFocusedRect: Rect? = null
    private fun getFocusedRect(): Rect? {
        return scene.focusManager.getFocusRect(afterLayout = false)?.also {
            lastFocusedRect = it
        } ?: lastFocusedRect
    }

    var onOutsidePointerEvent: (PointerEventType) -> Unit by _overlayView::onOutsidePointerEvent
    var isInterceptingOutsideEvents: Boolean by _overlayView::isInterceptingOutsideEvents
    var interactionBounds = IntRect.Zero

    fun setContent(content: @Composable () -> Unit) {
        _backgroundView.runOnceOnAppeared {
            scene.setContent {
                ProvideComposeSceneMediatorCompositionLocals {
                    FocusAboveKeyboardIfNeeded {
                        interopContainer.TrackInteropPlacementContainer(content = content)
                    }
                }
            }
        }
    }

    private var isLayoutTransitionAnimating = false
    fun prepareAndGetSizeTransitionAnimation(withProgress: suspend ((Float) -> Unit) -> Unit): suspend () -> Unit {
        isLayoutTransitionAnimating = true

        val initialWindowInsets = windowInsetsManager.windowInsetsSnapshot()
        val initialSize = scene.size?.toSize() ?: return {}

        return {
            try {
                withProgress { progress ->
                    windowInsetsManager.updateInsetsForAnimation(
                        initialWindowInsets = initialWindowInsets,
                        progress = progress
                    )
                    composeSceneSize = lerp(
                        start = initialSize,
                        stop = currentViewSize,
                        fraction = progress
                    ).roundToIntSize()
                }
            } finally {
                isLayoutTransitionAnimating = false
                updateLayout()
            }
        }
    }

    private var lastRenderTime = CACurrentMediaTime().toNanoSeconds()
    fun render(canvas: Canvas, nanoTime: Long) {
        lastRenderTime = nanoTime
        with(sceneRenderingScope) {
            scene.render(frameRecomposer, canvas, nanoTime)
        }
    }

    fun retrieveInteropTransaction(): UIKitInteropTransaction =
        interopContainer.retrieveTransaction()

    @OptIn(InternalComposeUiApi::class)
    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalInteropContainer provides interopContainer,
            LocalUIView provides _overlayView,
            LocalNativeTextInputContext provides textInputService.nativeTextInputContext,
            content = content
        )

    @Composable
    private fun FocusAboveKeyboardIfNeeded(content: @Composable () -> Unit) {
        if (onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            OffsetToFocusedRect(
                insets = windowInsetsManager.windowInsets.ime,
                getFocusedRect = ::getFocusedRect,
                size = scene.size,
                animationDuration = if (animateKeyboardOffsetChanges) {
                    FOCUS_CHANGE_ANIMATION_DURATION
                } else {
                    0.seconds
                },
                animationCompletion = {
                    animateKeyboardOffsetChanges = false
                },
                content = content
            )
        } else {
            content()
        }
    }

    private fun dispose() {
        onPreviewKeyEvent = { false }
        onKeyEvent = { false }

        _overlayView.dispose()
        keyboardManager.dispose()
        _backgroundView.dispose()

        _overlayView.removeFromSuperview()
        _backgroundView.removeFromSuperview()

        scene.close()
        frameRecomposer.close()
        interopContainer.dispose()
        semanticsOwnerListener.dispose()
    }

    /**
     * Updates the [ComposeScene] with the properties derived from the [_overlayView].
     */
    private fun updateLayout() {
        if (isLayoutTransitionAnimating) {
            return
        }
        windowInsetsManager.updateInsets()
        composeSceneSize = currentViewSize.roundToIntSize()
        interactionBounds = with(screenDensity) {
            _overlayView.bounds.toDpRect().toRect().roundToIntRect()
        }
    }

    private val currentViewSize: Size get() {
        return with(screenDensity) {
            _overlayView.frame.useContents { size.toDpSize() }.toSize()
        }
    }

    fun sceneDidAppear() {
        redrawer.setNeedsRedraw()
        keyboardManager.start()
    }

    fun sceneWillDisappear() {
        keyboardManager.stop()
    }

    // The Overlay View needs to be focused to be able to handle keyboard actions.
    // In general, the iOS system automatically reassigns the first responder focus to the overlay
    // view when other views resign the first responder focus, except at the time of initial appearance.
    private fun focusOverlayViewIfNeeded() {
        if (!isFocusEnabled) {
            return
        }
        val window = _overlayView.window ?: return
        fun findFirstResponder(view: UIView): UIView? {
            if (view.isFirstResponder) {
                return view
            }
            for (subview in view.subviews) {
                subview as UIView
                val firstResponder = findFirstResponder(subview)
                if (firstResponder != null) {
                    return firstResponder
                }
            }
            return null
        }
        if (findFirstResponder(window) == null) {
            _overlayView.becomeFirstResponder()
        }
    }

    fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        this.onPreviewKeyEvent = onPreviewKeyEvent ?: { false }
        this.onKeyEvent = onKeyEvent ?: { false }
    }

    /**
     * Converts [UIPress] objects to [KeyEvent] and dispatches them to the appropriate handlers.
     * @param presses a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    private fun onKeyboardPresses(presses: Set<*>) {
        presses.forEach {
            val press = it as UIPress
            onKeyboardEvent(press.toComposeEvent())
        }
    }

    private data class KeyIdentifier(
        val key: Key,
        val codePoint: Int,
        val modifiers: PointerKeyboardModifiers,
    ) {
        var press: UIPress? = null // Should not be part of the identifier

        val isAttachedToWindow: Boolean get() = (press?.responder as? UIView)?.window != null
    }

    private fun KeyEvent.keyIdentifier(): KeyIdentifier {
        val internalEvent = internal
        return KeyIdentifier(
            key = internalEvent.key,
            codePoint = internalEvent.codePoint,
            modifiers = internalEvent.modifiers,
        ).also {
            it.press = internalEvent.nativeEvent as? UIPress
        }
    }

    private val pressedKeysState = mutableListOf<KeyIdentifier>()

    // iOS does not complete or cancels key events which are attached to a view that is not in
    //  the window hierarchy.
    private fun finishUnattachedKeysPresses() {
        if (pressedKeysState.isEmpty()) {
            return
        }
        pressedKeysState.filter { !it.isAttachedToWindow }.forEach { key ->
            onKeyboardEvent(
                KeyEvent(
                    key = key.key,
                    type = KeyEventType.KeyUp,
                    codePoint = key.codePoint,
                    isCtrlPressed = key.modifiers.isCtrlPressed,
                    isMetaPressed = key.modifiers.isMetaPressed,
                    isAltPressed = key.modifiers.isAltPressed,
                    isShiftPressed = key.modifiers.isShiftPressed,
                    nativeEvent = key.press,
                )
            )
        }
    }

    private fun onKeyboardEvent(keyEvent: KeyEvent): Boolean {
        val result = textInputService.onPreviewKeyEvent(keyEvent)
            || onPreviewKeyEvent(keyEvent)
            || scene.sendKeyEvent(keyEvent)
            || onKeyEvent(keyEvent)
            || navigationEventInput.onKeyEvent(keyEvent)

        val identifier = keyEvent.keyIdentifier()
        if (keyEvent.type == KeyEventType.KeyDown) {
            pressedKeysState.add(identifier)
        } else if (keyEvent.type == KeyEventType.KeyUp) {
            if (pressedKeysState.contains(identifier)) {
                pressedKeysState.removeAll { it == identifier }
            } else {
                // Dirty state - remove all events to prevent further errors
                pressedKeysState.clear()
            }
        }

        return result
    }

    private inner class PlatformContextImpl : PlatformContext {
        override val windowInfo: WindowInfo get() = windowContext.windowInfo
        override val architectureComponentsOwner get() = this@ComposeSceneMediator.architectureComponentsOwner
        override val screenReader: PlatformScreenReader get() = platformScreenReader

        override val hapticFeedback: HapticFeedback by lazy(LazyThreadSafetyMode.NONE) {
            CupertinoHapticFeedback()
        }

        override fun convertLocalToWindowPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToWindowPosition(_overlayView, localPosition)

        override fun convertWindowToLocalPosition(positionInWindow: Offset): Offset =
            windowContext.convertWindowToLocalPosition(_overlayView, positionInWindow)

        override fun convertLocalToScreenPosition(localPosition: Offset): Offset =
            windowContext.convertLocalToScreenPosition(_overlayView, localPosition)

        override fun convertScreenToLocalPosition(positionOnScreen: Offset): Offset =
            windowContext.convertScreenToLocalPosition(_overlayView, positionOnScreen)

        override val viewConfiguration get() = this@ComposeSceneMediator.viewConfiguration

        override val inputModeManager by lazy(LazyThreadSafetyMode.NONE) {
            DefaultInputModeManager(InputMode.Touch)
        }

        override val textInputService get() = this@ComposeSceneMediator.textInputServiceAdapter
        override val textToolbar get() = this@ComposeSceneMediator.textInputService.textToolbar
        override val semanticsOwnerListener get() = this@ComposeSceneMediator.semanticsOwnerListener
        override val dragAndDropManager get() = this@ComposeSceneMediator.dragAndDropManager
        override val windowInsets get() = this@ComposeSceneMediator.windowInsetsManager.windowInsets
        override val isClearFocusOnMouseDownEnabled: Boolean
            get() = this@ComposeSceneMediator.isClearFocusOnMouseDownEnabled

        override var isKeepScreenOnEnabled: Boolean
            get() = UIKitIdleTimerManager.isIdleTimerDisabled
            set(value) { UIKitIdleTimerManager.setIdleTimerState(this@ComposeSceneMediator, value) }

        override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
            redrawer.voteFrameRate(frameRate, frameRateCategory)
        }

        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            this@ComposeSceneMediator.textInputService.startInputMethod(request)
        }
    }
}

private fun UIEvent.getButton(
    previousButtonMask: Long,
    eventKind: TouchesEventKind,
    previousEventKind: TouchesEventKind?
): PointerButton? =
    if (eventKind == TouchesEventKind.MOVED) {
        null
    } else if (buttonMaskOrZero and UIEventButtonMaskPrimary != 0L &&
        (previousButtonMask and UIEventButtonMaskPrimary == 0L ||
            eventKind != previousEventKind)) {
        PointerButton.Primary
    } else if (buttonMaskOrZero and UIEventButtonMaskSecondary != 0L &&
        (previousButtonMask and UIEventButtonMaskSecondary == 0L ||
            eventKind != previousEventKind)) {
        PointerButton.Secondary
    } else {
        null
    }

private val UIEvent?.timeMillis: Long get() {
    // If the touches were cancelled due to gesture failure, the timestamp is not available,
    // because no actual event with touch updates happened. We just use the current time in
    // this case.
    val timestamp = this?.timestamp ?: CACurrentMediaTime()
    return (timestamp * 1e3).toLong()
}

private val FOCUS_CHANGE_ANIMATION_DURATION = 0.15.seconds
private val SCROLL_DELTA_MULTIPLIER = 0.01f

private fun TouchesEventKind.toPointerEventType(): PointerEventType =
    when (this) {
        TouchesEventKind.BEGAN -> PointerEventType.Press
        TouchesEventKind.MOVED -> PointerEventType.Move
        TouchesEventKind.ENDED -> PointerEventType.Release
    }

private fun UIEvent.historicalChangesForTouch(
    touch: UITouch,
    view: UIView,
    density: Float
): List<HistoricalChange> {
    val touches = coalescedTouchesForTouch(touch) ?: return emptyList()

    return if (touches.size > 1) {
        // the last touch is not included because it is the actual touch reported by the event
        touches.dropLast(1).map {
            val historicalTouch = it as UITouch
            val position = historicalTouch.offsetInView(view, density)
            HistoricalChange(
                uptimeMillis = (historicalTouch.timestamp * 1e3).toLong(),
                position = position,
                originalEventPosition = position,
                scaleFactor = 1f,
                panOffset = Offset.Zero,
            )
        }
    } else {
        emptyList()
    }
}

private val UIEvent?.buttonMaskOrZero: Long get() =
    if (available(OS.Ios to OSVersion(13, 4))) {
        this?.buttonMask ?: 0L
    } else {
        0L
    }

private val UIEvent?.modifierFlagsOrZero: Long get() =
    if (available(OS.Ios to OSVersion(13, 4))) {
        this?.modifierFlags ?: 0L
    } else {
        0L
    }

private val UITouch.isPressed
    get() = when (phase) {
        UITouchPhase.UITouchPhaseEnded, UITouchPhase.UITouchPhaseCancelled -> false
        else -> true
    }

private fun UITouch.offsetInView(view: UIView, density: Float): Offset =
    locationInView(view).useContents {
        Offset(x.toFloat() * density, y.toFloat() * density)
    }
