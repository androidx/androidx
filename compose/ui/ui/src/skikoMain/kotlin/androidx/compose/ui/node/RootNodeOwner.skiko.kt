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

package androidx.compose.ui.node

import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusOwnerImpl
import androidx.compose.ui.focus.PlatformFocusOwner
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.layout.RulerProviderModifierElement
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.platform.DefaultAccessibilityManager
import androidx.compose.ui.platform.DefaultHapticFeedback
import androidx.compose.ui.platform.DelegatingSoftwareKeyboardController
import androidx.compose.ui.platform.GraphicsLayerOwnerLayer
import androidx.compose.ui.platform.OwnedLayerManager
import androidx.compose.ui.platform.PlatformClipboardManager
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.LegacyRenderNodeLayer
import androidx.compose.ui.platform.createPlatformClipboard
import androidx.compose.ui.platform.setLightingInfo
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneInputHandler
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.semantics.EmptySemanticsModifier
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.useLegacyRenderNodeLayers
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMaxOfOrDefault
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.InteropPointerInputModifier
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.pointerInteropFilter
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Owner of root [LayoutNode].
 *
 * It hides [Owner]/[RootForTest] implementations, but provides everything that
 * [ComposeScene] need.
 */
internal class RootNodeOwner(
    density: Density,
    layoutDirection: LayoutDirection,
    size: IntSize?,
    coroutineContext: CoroutineContext,
    val platformContext: PlatformContext,
    private val snapshotInvalidationTracker: SnapshotInvalidationTracker,
    private val inputHandler: ComposeSceneInputHandler,
) {
    val focusOwner: FocusOwner get() = _owner.focusOwner
    val dragAndDropOwner = DragAndDropOwner(platformContext.dragAndDropManager)

    private val rootSemanticsNode = EmptySemanticsModifier()
    private val snapshotObserver = snapshotInvalidationTracker.snapshotObserver()
    private val graphicsContext = SkiaGraphicsContext(platformContext.measureDrawLayerBounds)
    private val coroutineScope = CoroutineScope(coroutineContext + Job(parent = coroutineContext[Job]))

    private val _owner = OwnerImpl(layoutDirection, coroutineContext)
    val owner: Owner get() = _owner

    val semanticsOwner get() = owner.semanticsOwner
    var size: IntSize? = size
        set(value) {
            field = value
            onRootConstrainsChanged(value?.toConstraints())
        }
    var density by mutableStateOf(density)

    private var _layoutDirection by mutableStateOf(layoutDirection)
    var layoutDirection: LayoutDirection
        get() = _layoutDirection
        set(value) {
            _layoutDirection = value
            owner.root.layoutDirection = value
        }

    private val rootForTest = PlatformRootForTestImpl()
    private val ownedLayerManager = OwnedLayerManagerImpl()
    private val pointerInputEventProcessor = PointerInputEventProcessor(owner.root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(owner.root)
    private var isDisposed = false

    private var positionInWindow: Offset? = null
    private var positionOnScreen: Offset? = null

    // TODO: Android assumes matrix for some APIs, so we need store something to avoid extra
    //  allocations. Clean up this APIs and remove it.
    private val identityMatrix = Matrix()

    init {
        snapshotObserver.startObserving()
        owner.root.attach(owner)
        platformContext.rootForTestListener?.onRootForTestCreated(rootForTest)
        onRootConstrainsChanged(size?.toConstraints())
        updatePositionCacheAndDispatch()
        coroutineScope.launch {
            snapshotFlow { platformContext.windowInfo.containerSize }
                .collect { updatePositionCacheAndDispatch(hasContainerSizeChanged = true) }
        }
    }

    fun dispose() {
        check(!isDisposed) { "RootNodeOwner is already disposed" }
        coroutineScope.cancel()
        platformContext.rootForTestListener?.onRootForTestDisposed(rootForTest)
        snapshotObserver.stopObserving()
        graphicsContext.dispose()
        // we don't need to call root.detach() because root will be garbage collected
        isDisposed = true
    }

    private var needClearObservations = false
    private fun clearInvalidObservations() {
        if (needClearObservations) {
            snapshotObserver.clearInvalidObservations()
            needClearObservations = false
        }
    }

    /**
     * Provides a way to measure Owner's content in given [constraints]
     * Draw/pointer and other callbacks won't be called here like in [measureAndLayout] functions
     */
    fun measureInConstraints(constraints: Constraints): IntSize {
        try {
            // TODO: is it possible to measure without reassigning root constraints?
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
            measureAndLayoutDelegate.measureOnly()

            // Don't use mainOwner.root.width here, as it strictly coerced by [constraints]
            val children = owner.root.children
            return IntSize(
                width = children.fastMaxOfOrDefault(0) { it.outerCoordinator.measuredWidth },
                height = children.fastMaxOfOrDefault(0) { it.outerCoordinator.measuredHeight },
            )
        } finally {
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
        }
    }

    fun measureAndLayout() {
        owner.measureAndLayout(sendPointerUpdate = true)
        updatePositionCacheAndDispatch()
    }

    private fun updatePositionCacheAndDispatch(hasContainerSizeChanged: Boolean = false) {
        val positionOnScreen = platformContext.convertLocalToScreenPosition(Offset.Zero)
        val hasPositionOnScreenChanged = if (platformContext.hasNonTranslationComponents) {
            this.positionOnScreen = null
            true // Always invalidate in case of rotation, skew, etc.
        } else if (positionOnScreen != this.positionOnScreen) {
            this.positionOnScreen = positionOnScreen
            true
        } else false

        val positionInWindow = platformContext.convertLocalToWindowPosition(Offset.Zero)
        val hasPositionInWindowChanged = if (platformContext.hasNonTranslationComponents) {
            this.positionInWindow = null
            true // Always invalidate in case of rotation, skew, etc.
        } else if (positionInWindow != this.positionInWindow) {
            this.positionInWindow = positionInWindow
            true
        } else false

        if (hasPositionOnScreenChanged || hasPositionInWindowChanged) {
            owner.root.layoutDelegate.measurePassDelegate.notifyChildrenUsingCoordinatesWhilePlacing()
        }
        val containerSize = platformContext.windowInfo.containerSize
        owner.rectManager.updateOffsets(
            screenOffset = positionOnScreen.round(),
            windowOffset = positionInWindow.round(),
            viewToWindowMatrix = identityMatrix, // TODO: Replace viewToWindowMatrix to delegates
            windowWidth = containerSize.width,
            windowHeight = containerSize.height,
        )
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(
            forceDispatch = hasPositionOnScreenChanged || hasPositionInWindowChanged
        )
        owner.rectManager.dispatchCallbacks()
        if (hasPositionInWindowChanged || hasContainerSizeChanged) {
            graphicsContext.setLightingInfo(
                canvasOffset = positionInWindow,
                density = density,
                containerSize = containerSize
            )
        }
    }

    fun invalidatePositionInWindow() {
        updatePositionCacheAndDispatch()
    }

    fun invalidatePositionOnScreen() {
        updatePositionCacheAndDispatch()
    }

    fun draw(canvas: Canvas) = trace("RootNodeOwner:draw") {
        ownedLayerManager.draw(canvas)
        clearInvalidObservations()
        owner.rectManager.dispatchCallbacks()
    }

    fun setRootModifier(modifier: Modifier) {
        owner.root.modifier = _owner.rootModifier then modifier
    }

    private fun onRootConstrainsChanged(constraints: Constraints?) {
        measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
        if (measureAndLayoutDelegate.hasPendingMeasureOrLayout) {
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }
    }

    fun onCancelPointerInput() {
        pointerInputEventProcessor.processCancel()
    }

    @OptIn(InternalCoreApi::class)
    fun onPointerInput(event: PointerInputEvent): PointerEventResult {
        if (event.button != null) {
            platformContext.inputModeManager.requestInputMode(InputMode.Touch)
        }
        val isInBounds = event.eventType != PointerEventType.Exit &&
            event.pointers.fastAll { isInBounds(it.position) }
        val result = pointerInputEventProcessor.process(
            event,
            IdentityPositionCalculator,
            isInBounds = isInBounds
        )
        return PointerEventResult(value = result.value)
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        return focusOwner.dispatchKeyEvent(keyEvent) || handleFocusKeys(keyEvent)
    }

    private fun handleFocusKeys(keyEvent: KeyEvent): Boolean {
        // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
        //  that this common logic can be used by all owners.
        val focusDirection = getFocusDirection(keyEvent)
        if (focusDirection == null || keyEvent.type != KeyEventType.KeyDown) return false

        platformContext.inputModeManager.requestInputMode(InputMode.Keyboard)
        // Consume the key event if we moved focus.
        return focusOwner.moveFocus(focusDirection)
    }

    private fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        return when (keyEvent.key) {
            Key.Tab -> if (keyEvent.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
            Key.DirectionCenter -> FocusDirection.Enter
            Key.Back -> FocusDirection.Exit
            else -> null
        }
    }

    fun onRotaryEvent(event: RotaryScrollEvent): Boolean {
        return _owner.focusOwner.dispatchRotaryEvent(event)
    }

    /**
     * Perform hit test and return the [InteropView] associated with the resulting
     * [PointerInputModifierNode] node in case it is a [Modifier.pointerInteropFilter],
     * otherwise null.
     */
    fun hitTestInteropView(position: Offset): InteropView? {
        val result = HitTestResult()
        owner.root.hitTest(position, result, isInLayer = true)

        val last = result.lastOrNull() as? BackwardsCompatNode
        val node = last?.element as? InteropPointerInputModifier
        return node?.interopView
    }

    private fun isInBounds(localPosition: Offset): Boolean =
        size?.toIntRect()?.toRect()?.contains(localPosition) ?: true

    private fun calculateBoundsInWindow(): Rect? {
        val rect = size?.toIntRect()?.toRect() ?: return null
        val p0 = platformContext.convertLocalToWindowPosition(Offset(rect.left, rect.top))
        val p1 = platformContext.convertLocalToWindowPosition(Offset(rect.left, rect.bottom))
        val p3 = platformContext.convertLocalToWindowPosition(Offset(rect.right, rect.top))
        val p4 = platformContext.convertLocalToWindowPosition(Offset(rect.right, rect.bottom))

        val left = min(min(p0.x, p1.x), min(p3.x, p4.x))
        val top = min(min(p0.y, p1.y), min(p3.y, p4.y))
        val right = max(max(p0.x, p1.x), max(p3.x, p4.x))
        val bottom = max(max(p0.y, p1.y), max(p3.y, p4.y))
        return Rect(left, top, right, bottom)
    }

    private inner class OwnerImpl(
        layoutDirection: LayoutDirection,
        override val coroutineContext: CoroutineContext,
    ) : Owner {
        private val platformFocusOwner = object : PlatformFocusOwner {
            override fun requestOwnerFocus(
                focusDirection: FocusDirection?,
                previouslyFocusedRect: Rect?
            ): Boolean {
                return platformContext.requestFocus()
            }

            override fun clearOwnerFocus() {
                platformContext.parentFocusManager.clearFocus(true)
            }

            // onMoveFocusInterop's purpose is to move focus inside embed interop views.
            // Another logic is used in our child-interop views (SwingPanel, etc)
            override fun moveFocusInChildren(focusDirection: FocusDirection) = false

            override fun getEmbeddedViewFocusRect(): Rect? = null
        }

        override val focusOwner: FocusOwner = FocusOwnerImpl(platformFocusOwner, this)

        val rootModifier = if (ComposeUiFlags.areWindowInsetsRulersEnabled) {
                RulerProviderModifierElement(platformContext.windowInsets)
            } else {
                Modifier
            }
            .then(EmptySemanticsElement(rootSemanticsNode))
            .focusProperties {
                onExit = {
                    // if focusDirection is forward/backward,
                    // it will move the focus after/before ComposePanel
                    if (platformContext.parentFocusManager.moveFocus(requestedFocusDirection)) {
                        cancelFocusChange()
                    }
                }
            }
            .then(focusOwner.modifier)
            .then(dragAndDropOwner.modifier)
            .semantics {
                // This makes the reported role of the root node "PANEL", which is ignored by VoiceOver
                // (which is what we want).
                isTraversalGroup = true
            }

        override val root = LayoutNode().also {
            it.layoutDirection = layoutDirection
            it.measurePolicy = RootMeasurePolicy
            it.modifier = rootModifier
        }

        override val sharedDrawScope = LayoutNodeDrawScope()
        override val layoutNodes: MutableIntObjectMap<LayoutNode> = mutableIntObjectMapOf()
        override val rootForTest get() = this@RootNodeOwner.rootForTest
        override val hapticFeedBack = DefaultHapticFeedback()
        override val inputModeManager get() = platformContext.inputModeManager
        override val clipboardManager = PlatformClipboardManager()
        override val clipboard = createPlatformClipboard()
        override val accessibilityManager = DefaultAccessibilityManager()
        override val graphicsContext get() = this@RootNodeOwner.graphicsContext
        override val textToolbar get() = platformContext.textToolbar
        @Suppress("DEPRECATION")
        override val autofillTree = androidx.compose.ui.autofill.AutofillTree()
        @Suppress("DEPRECATION")
        override val autofill: androidx.compose.ui.autofill.Autofill?
            get() = null
        // TODO https://youtrack.jetbrains.com/issue/CMP-1572
        override val autofillManager: AutofillManager? get() = null
        override val density get() = this@RootNodeOwner.density
        override val textInputService =
            TextInputService(platformContext.textInputService)
        override val softwareKeyboardController =
            DelegatingSoftwareKeyboardController(textInputService)

        private val textInputSessionMutex = SessionMutex<TextInputSession>()
        private inner class TextInputSession(
            coroutineScope: CoroutineScope,
        ) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {
            private val innerSessionMutex = SessionMutex<Nothing?>()

            @OptIn(InternalTextApi::class)
            override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
                innerSessionMutex.withSessionCancellingPrevious<Nothing>(
                    sessionInitializer = { null }
                ) {
                    coroutineScope {
                        // Currently TextInputService is used for keyboard show/hide actions and for
                        // backward compatibility by the LocalTextInputService.
                        // startInput and stopInput calls are required to properly configure the service
                        // and allow it to pass keyboard show/hide calls to the PlatformTextInputService.
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            suspendCancellableCoroutine<Nothing> {
                                textInputService.startInput()
                                it.invokeOnCancellation {
                                    textInputService.stopInput()
                                }
                            }
                        }
                        platformContext.startInputMethod(request)
                    }
                }
            }
        }

        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ) : Nothing {
            textInputSessionMutex.withSessionCancellingPrevious<Nothing>(
                sessionInitializer = ::TextInputSession,
                session = session
            )
        }

        override val dragAndDropManager = this@RootNodeOwner.dragAndDropOwner
        override val pointerIconService = PointerIconServiceImpl()
        override val semanticsOwner = SemanticsOwner(root, rootSemanticsNode, layoutNodes)
        override val windowInfo get() = platformContext.windowInfo
        override val retainedValuesStore: RetainedValuesStore get() = ForgetfulRetainedValuesStore
        override val rectManager = RectManager()

        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override val fontLoader = androidx.compose.ui.text.platform.FontLoader()
        override val fontFamilyResolver = createFontFamilyResolver()
        override val layoutDirection get() = _layoutDirection
        override var showLayoutBounds by mutableStateOf(false)
            @InternalCoreApi
            set

        override val modifierLocalManager = ModifierLocalManager(this)
        override val snapshotObserver get() = this@RootNodeOwner.snapshotObserver
        override val viewConfiguration get() = platformContext.viewConfiguration
        override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

        override fun requestAutofill(node: LayoutNode) {
            // TODO: 1.8.0-beta01 Adopt requestAutofill API
            //  https://youtrack.jetbrains.com/issue/CMP-7485
        }

        override fun onPreAttach(node: LayoutNode) {
            layoutNodes[node.semanticsId] = node
        }

        override fun onPostAttach(node: LayoutNode) {
        }

        override fun onDetach(node: LayoutNode) {
            val existing = layoutNodes.remove(node.semanticsId)
            checkNotNull(existing) {
                "Invalid usage of Owner.onDetach: layoutNode was not previously attached"
            }
            measureAndLayoutDelegate.onNodeDetached(node)
            snapshotObserver.clear(node)
            needClearObservations = true
            rectManager.remove(node)
        }

        override fun measureAndLayout(sendPointerUpdate: Boolean) {
            // only run the logic when we have something pending
            if (measureAndLayoutDelegate.hasPendingMeasureOrLayout ||
                measureAndLayoutDelegate.hasPendingOnPositionedCallbacks
            ) {
                trace("RootNodeOwner:measureAndLayout") {
                    val resend = if (sendPointerUpdate) inputHandler::onPointerUpdate else null
                    val rootNodeResized = measureAndLayoutDelegate.measureAndLayout(resend)
                    if (rootNodeResized) {
                        snapshotInvalidationTracker.requestDraw()
                    }
                    measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
                    rectManager.dispatchCallbacks()
                }
            }
        }

        override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
            trace("RootNodeOwner:measureAndLayout") {
                measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
                inputHandler.onPointerUpdate()
                // only dispatch the callbacks if we don't have other nodes to process as otherwise
                // we will have one more measureAndLayout() pass anyway in the same frame.
                // it allows us to not traverse the hierarchy twice.
                if (!measureAndLayoutDelegate.hasPendingMeasureOrLayout) {
                    measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
                }
                rectManager.dispatchCallbacks()
            }
        }

        override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {
            measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode, affectsLookahead)
        }

        override fun onRequestMeasure(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean,
            scheduleMeasureAndLayout: Boolean
        ) {
            if (affectsLookahead) {
                if (measureAndLayoutDelegate.requestLookaheadRemeasure(layoutNode, forceRequest) &&
                    scheduleMeasureAndLayout
                ) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
                scheduleMeasureAndLayout
            ) {
                snapshotInvalidationTracker.requestMeasureAndLayout()
            }
        }

        override fun onRequestRelayout(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean
        ) {
            if (affectsLookahead) {
                if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            } else {
                if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            }
        }

        override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
            measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }

        override fun createLayer(
            drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
            invalidateParentLayer: () -> Unit,
            explicitLayer: GraphicsLayer?,
        ) = ownedLayerManager.createLayer(
            drawBlock = drawBlock,
            invalidateParentLayer = invalidateParentLayer,
            explicitLayer = explicitLayer
        )

        override fun onSemanticsChange() {
            platformContext.semanticsOwnerListener?.onSemanticsChange(semanticsOwner)
        }

        override fun onLayoutChange(layoutNode: LayoutNode) {
            platformContext.semanticsOwnerListener?.onLayoutChange(
                semanticsOwner = semanticsOwner,
                semanticsNodeId = layoutNode.semanticsId
            )
        }

        override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {
            rectManager.remove(layoutNode)
        }

        override fun onPreLayoutNodeReused(layoutNode: LayoutNode, oldSemanticsId: Int) {
            // Keep the mapping up to date when the semanticsId changes
            val existing = layoutNodes.remove(oldSemanticsId)
            checkNotNull(existing) {
                "Invalid usage of Owner.onPreLayoutNodeReused: layoutNode is not found"
            }
            check(existing == layoutNode) {
                "Invalid usage of Owner.onPreLayoutNodeReused: previous semanticsId refers another layoutNode"
            }
            layoutNodes[layoutNode.semanticsId] = layoutNode
        }

        @InternalComposeUiApi
        override fun onInteropViewLayoutChange(view: InteropView) {
            // TODO dispatch platform re-layout
        }

        override fun calculatePositionInWindow(localPosition: Offset): Offset =
            platformContext.convertLocalToWindowPosition(localPosition)

        override fun calculateLocalPosition(positionInWindow: Offset): Offset =
            platformContext.convertWindowToLocalPosition(positionInWindow)

        override fun screenToLocal(positionOnScreen: Offset): Offset =
            platformContext.convertScreenToLocalPosition(positionOnScreen)

        override fun localToScreen(localPosition: Offset): Offset =
            platformContext.convertLocalToScreenPosition(localPosition)

        // TODO: reverted in https://r.android.com/3208275
//        override fun localToScreen(localTransform: Matrix) {
//            throw UnsupportedOperationException(
//                "Construction of local-to-screen matrix is not supported, " +
//                    "use direct conversion instead"
//            )
//        }

        private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

        override fun onEndApplyChanges() {
            clearInvalidObservations()

            // Listeners can add more items to the list and we want to ensure that they
            // are executed after being added, so loop until the list is empty
            while (endApplyChangesListeners.isNotEmpty()) {
                val size = endApplyChangesListeners.size
                for (i in 0 until size) {
                    val listener = endApplyChangesListeners[i]
                    // null out the item so that if the listener is re-added then we execute it again.
                    endApplyChangesListeners[i] = null
                    listener?.invoke()
                }
                // Remove all the items that were visited. Removing items shifts all items after
                // to the front of the list, so removing in a chunk is cheaper than removing one-by-one
                endApplyChangesListeners.removeRange(0, size)
            }
        }

        override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
            if (listener !in endApplyChangesListeners) {
                endApplyChangesListeners += listener
            }
        }

        override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
            measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }

        override fun voteFrameRate(frameRate: Float) {
            ownedLayerManager.voteFrameRate(frameRate)
        }

        private var keepScreenOnCount = 0

        override fun incrementKeepScreenOnCount() {
            keepScreenOnCount++
            platformContext.isKeepScreenOnEnabled = keepScreenOnCount > 0
        }

        override fun decrementKeepScreenOnCount() {
            keepScreenOnCount--
            platformContext.isKeepScreenOnEnabled = keepScreenOnCount > 0
        }
    }

    private inner class PlatformRootForTestImpl : PlatformRootForTest {
        override val density get() = this@RootNodeOwner.density
        @Suppress("OVERRIDE_DEPRECATION")
        override val textInputService get() = owner.textInputService
        override val semanticsOwner get() = owner.semanticsOwner
        override val visibleBounds: Rect
            get() {
                val windowRect = platformContext.windowInfo.containerSize.toIntRect().toRect()
                val ownerRect = calculateBoundsInWindow()
                return ownerRect?.intersect(windowRect) ?: windowRect
            }

        override val hasPendingMeasureOrLayout: Boolean
            get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

        override fun measureAndLayoutForTest() {
            owner.measureAndLayout(sendPointerUpdate = true)
        }

        /**
         * Handles the input initiated by tests.
         */
        override fun sendPointerEvent(
            eventType: PointerEventType,
            position: Offset,
            scrollDelta: Offset,
            timeMillis: Long,
            type: PointerType,
            buttons: PointerButtons?,
            keyboardModifiers: PointerKeyboardModifiers?,
            nativeEvent: Any?,
            button: PointerButton?
        ) {
            inputHandler.onPointerEvent(
                eventType = eventType,
                position = position,
                scrollDelta = scrollDelta,
                timeMillis = timeMillis,
                type = type,
                buttons = buttons,
                keyboardModifiers = keyboardModifiers,
                nativeEvent = nativeEvent,
                button = button
            )
        }

        /**
         * Handles the input initiated by tests.
         */
        override fun sendPointerEvent(
            eventType: PointerEventType,
            pointers: List<ComposeScenePointer>,
            buttons: PointerButtons,
            keyboardModifiers: PointerKeyboardModifiers,
            scrollDelta: Offset,
            timeMillis: Long,
            nativeEvent: Any?,
            button: PointerButton?,
        ) {
            inputHandler.onPointerEvent(
                eventType = eventType,
                pointers = pointers,
                buttons = buttons,
                keyboardModifiers = keyboardModifiers,
                scrollDelta = scrollDelta,
                timeMillis = timeMillis,
                nativeEvent = nativeEvent,
                button = button
            )
        }

        /**
         * Handles the input initiated by tests or accessibility.
         */
        override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
            inputHandler.onKeyEvent(keyEvent)

        // TODO https://youtrack.jetbrains.com/issue/COMPOSE-1258/Implement-PlatformRootForTest.accessitiblity-functions

        @ExperimentalComposeUiApi
        override fun forceAccessibilityForTesting(enable: Boolean) {
        }

        @ExperimentalComposeUiApi
        override fun setAccessibilityEventBatchIntervalMillis(intervalMillis: Long) {
        }
    }

    private inner class PointerIconServiceImpl : PointerIconService {
        private var desiredPointerIcon: PointerIcon? = null
        override fun getIcon(): PointerIcon = desiredPointerIcon ?: PointerIcon.Default
        override fun setIcon(value: PointerIcon?) {
            desiredPointerIcon = value
            platformContext.setPointerIcon(desiredPointerIcon ?: PointerIcon.Default)
        }

        // TODO https://youtrack.jetbrains.com/issue/CMP-7145/Properly-adopt-stylus-handwriting-hover-icon
        override fun getStylusHoverIcon(): PointerIcon? = null
        override fun setStylusHoverIcon(value: PointerIcon?) {}
    }

    private inner class OwnedLayerManagerImpl : OwnedLayerManager {
        // OwnedLayers that are dirty and should be redrawn.
        private val dirtyLayers = mutableListOf<OwnedLayer>()

        // OwnerLayers that invalidated themselves during their last draw. They will be redrawn
        // during the next AndroidComposeView dispatchDraw pass.
        private var postponedDirtyLayers: MutableList<OwnedLayer>? = null

        private var isDrawingContent = false

        override fun createLayer(
            drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
            invalidateParentLayer: () -> Unit,
            explicitLayer: GraphicsLayer?
        ) = if (explicitLayer != null || !ComposeUiFlags.useLegacyRenderNodeLayers) {
            GraphicsLayerOwnerLayer(
                graphicsLayer = explicitLayer ?: graphicsContext.createGraphicsLayer(),
                context = if (explicitLayer != null) null else graphicsContext,
                layerManager = this,
                drawBlock = drawBlock,
                invalidateParentLayer = invalidateParentLayer,
            )
        } else {
            LegacyRenderNodeLayer(
                density = Snapshot.withoutReadObservation {
                    // density is a mutable state that is observed whenever layer is created. the layer
                    // is updated manually on draw, so not observing the density changes here helps with
                    // performance in layout.
                    density
                },
                measureDrawBounds = platformContext.measureDrawLayerBounds,
                layerManager = this,
                requiresStateWorkaround = { graphicsContext.activeGraphicsLayersCount > 0 },
                invalidateParentLayer = invalidateParentLayer,
                drawBlock = drawBlock,
            )
        }

        override fun recycle(layer: OwnedLayer): Boolean {
            needClearObservations = true
            dirtyLayers -= layer
            return false
        }

        override fun notifyLayerIsDirty(layer: OwnedLayer, isDirty: Boolean) {
            if (!isDirty) {
                // It is correct to remove the layer here regardless of this if, but for performance
                // we are hackily not doing the removal here in order to just do clear() a bit later.
                if (!isDrawingContent) {
                    dirtyLayers.remove(layer)
                    postponedDirtyLayers?.remove(layer)
                }
            } else if (!isDrawingContent) {
                dirtyLayers += layer
            } else {
                val postponed =
                    postponedDirtyLayers
                        ?: mutableListOf<OwnedLayer>().also { postponedDirtyLayers = it }
                postponed += layer
            }
        }

        override fun invalidate() {
            snapshotInvalidationTracker.requestDraw()
        }

        private var currentFrameRate = Float.NaN
        private var currentFrameRateCategory = 0f

        override fun voteFrameRate(frameRate: Float) {
            if (!ComposeUiFlags.isAdaptiveRefreshRateEnabled) return

            val isCurrentFrameRateUnset = currentFrameRate.isNaN()
            val isCurrentFrameRateCategoryUnset = currentFrameRateCategory == 0f

            if (frameRate > 0) {
                if (isCurrentFrameRateUnset || frameRate > currentFrameRate) {
                    currentFrameRate = frameRate
                }
            } else if (frameRate.isNaN() && isCurrentFrameRateCategoryUnset) {
                currentFrameRateCategory = frameRate
            } else if (!frameRate.isNaN() && frameRate < 0 && (currentFrameRateCategory.isNaN() || frameRate < currentFrameRateCategory)) {
                currentFrameRateCategory = frameRate
            }
        }

        fun draw(canvas: Canvas) {
            isDrawingContent = true

            // Unlike Android, "draw" forms actual render commands sequence, so updating
            // display lists after that won't affect current frame result.
            // So, we applying it before drawing to reflect the changes from previous phases.
            // Changes that requires another round of invalidation will be scheduled to next frame.
            if (dirtyLayers.isNotEmpty()) {
                for (i in 0 until dirtyLayers.size) {
                    val layer = dirtyLayers[i]
                    layer.updateDisplayList()
                }
            }
            dirtyLayers.clear()

            // Draw root node
            owner.root.draw(
                canvas = canvas,
                graphicsLayer = null // the root node will provide the root graphics layer
            )

            // updateDisplayList operations performed above (during root.draw and during the explicit
            // layer.updateDisplayList() calls) can result in the same layers being invalidated. These
            // layers have been added to postponedDirtyLayers and will be redrawn during the next
            // dispatchDraw.
            if (postponedDirtyLayers != null) {
                val postponed = postponedDirtyLayers!!
                dirtyLayers.addAll(postponed)
                postponed.clear()
            }

            val isAnyCurrentFrameRateSet = !currentFrameRate.isNaN() || currentFrameRateCategory != 0f
            if (ComposeUiFlags.isAdaptiveRefreshRateEnabled && isAnyCurrentFrameRateSet) {
                platformContext.voteFrameRate(currentFrameRate, currentFrameRateCategory)
                currentFrameRate = Float.NaN
                currentFrameRateCategory = 0f
            }

            isDrawingContent = false
        }
    }
}

// TODO a proper way is to provide API in Constraints to get this value
/**
 * Equals [Constraints.MinNonFocusMask]
 */
private const val ConstraintsMinNonFocusMask = 0x7FFF // 32767

/**
 * The max value that can be passed as Constraints(0, LargeDimension, 0, LargeDimension)
 *
 * Greater values cause "Can't represent a width of".
 * See [Constraints.createConstraints] and [Constraints.bitsNeedForSize]:
 *  - it fails if `widthBits + heightBits > 31`
 *  - widthBits/heightBits are greater than 15 if we pass size >= [Constraints.MinNonFocusMask]
 */
internal const val LargeDimension = ConstraintsMinNonFocusMask - 1

/**
 * After https://android-review.googlesource.com/c/platform/frameworks/support/+/2901556
 * Compose core doesn't allow measuring in infinity constraints,
 * but RootNodeOwner and ComposeScene allow passing Infinity constraints by contract
 * (Android on the other hand doesn't have public API for that and don't have such an issue).
 *
 * This method adds additional check on Infinity constraints,
 * and pass constraint large enough instead
 */
private fun MeasureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(
    constraints: Constraints?
) {
    updateRootConstraints(
        constraints = Constraints(
            minWidth = constraints?.minWidth ?: 0,
            maxWidth = if (constraints != null && constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                LargeDimension
            },
            minHeight = constraints?.minHeight ?: 0,
            maxHeight = if (constraints != null && constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                LargeDimension
            }
        )
    )
}

private fun IntSize.toConstraints() = Constraints(maxWidth = width, maxHeight = height)

private object IdentityPositionCalculator: PositionCalculator {
    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen
    override fun localToScreen(localPosition: Offset): Offset = localPosition
}
