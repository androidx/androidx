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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.DefaultPointerButtons
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.PrimaryPressedPointerButtons
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusOwnerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.layer.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.pointer.TestPointerInputEventData
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.EmptySemanticsElement
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext

private typealias Command = () -> Unit

@OptIn(
    ExperimentalComposeUiApi::class,
    InternalCoreApi::class,
    InternalComposeUiApi::class
)
internal class SkiaBasedOwner(
    private val platformInputService: PlatformInput,
    private val component: PlatformComponent,
    density: Density = Density(1f, 1f),
    override val coroutineContext: CoroutineContext,
    val isPopup: Boolean = false,
    val isFocusable: Boolean = true,
    val onDismissRequest: (() -> Unit)? = null,
    private val onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    private val onKeyEvent: (KeyEvent) -> Boolean = { false },
) : Owner, RootForTest, SkiaRootForTest, PositionCalculator {

    internal fun isHovered(point: Offset): Boolean {
        val intOffset = IntOffset(point.x.toInt(), point.y.toInt())
        return bounds.contains(intOffset)
    }

    internal var accessibilityController: AccessibilityController? = null

    internal var bounds by mutableStateOf(IntRect.Zero)

    override var density by mutableStateOf(density)

    // TODO(demin): support RTL
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override val sharedDrawScope = LayoutNodeDrawScope()

    private val semanticsModifier = EmptySemanticsElement

    override val focusOwner: FocusOwner = FocusOwnerImpl(
        onRequestApplyChangesListener = ::registerOnEndApplyChangesListener,
        onRequestFocusForOwner = { _, _ -> true }, // TODO request focus from framework.
        onMoveFocusInterop = { _ -> true },
        onClearFocusForOwner = {}, // TODO clear focus from framework.
        onFocusRectInterop = { null },
        onLayoutDirection = { layoutDirection } // TODO(demin): RTL [onRtlPropertiesChanged].
    )

    // TODO: Set the input mode. For now we don't support touch mode, (always in Key mode).
    private val _inputModeManager = InputModeManagerImpl(
        initialInputMode = Keyboard,
        onRequestInputModeChange = {
            // TODO: Change the input mode programmatically. For now we just return true if the
            //  requested input mode is Keyboard mode.
            it == Keyboard
        }
    )
    override val inputModeManager: InputModeManager
        get() = _inputModeManager

    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)

    // TODO: set/clear _windowInfo.isWindowFocused when the window gains/loses focus.
    private val _windowInfo: WindowInfoImpl = WindowInfoImpl()
    override val windowInfo: WindowInfo
        get() = _windowInfo

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier = Modifier.onKeyEvent {
        val focusDirection = getFocusDirection(it)
        if (focusDirection == null || it.type != KeyDown) return@onKeyEvent false

        // Consume the key event if we moved focus.
        focusOwner.moveFocus(focusDirection)
    }

    @Suppress("unused") // to be used in JB fork (not all prerequisite changes added yet)
    internal fun setCurrentKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        _windowInfo.keyboardModifiers = modifiers
    }

    var constraints: Constraints = Constraints()
        set(value) {
            field = value

            if (!isPopup) {
                this.bounds = IntRect(
                    IntOffset(bounds.left, bounds.top),
                    IntSize(constraints.maxWidth, constraints.maxHeight)
                )
            }
        }

    override val root = LayoutNode().also {
        it.measurePolicy = RootMeasurePolicy
        it.modifier = semanticsModifier
            .then(focusOwner.modifier)
            .then(keyInputModifier)
            .onPreviewKeyEvent(onPreviewKeyEvent)
            .onKeyEvent(onKeyEvent)
    }

    override val rootForTest = this

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        onDispatchCommand?.invoke(command)
    }
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

    init {
        snapshotObserver.startObserving()
        root.attach(this)
        focusOwner.focusTransactionManager.withNewTransaction {
            // TODO instead of taking focus here, call this when the owner gets focused.
            focusOwner.takeFocus(Enter, previouslyFocusedRect = null)
        }
    }

    fun dispose() {
        snapshotObserver.stopObserving()
        // we don't need to call root.detach() because root will be garbage collected
    }

    override val textInputService = TextInputService(platformInputService)

    override val softwareKeyboardController: SoftwareKeyboardController =
        DelegatingSoftwareKeyboardController(textInputService)

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    override val fontLoader = FontLoader()

    override val fontFamilyResolver = createFontFamilyResolver()

    override val hapticFeedBack = DefaultHapticFeedback()

    override val clipboardManager = PlatformClipboardManager()

    override val accessibilityManager = DefaultAccessibilityManager()
    override val graphicsContext: GraphicsContext = GraphicsContext()

    override val textToolbar = DefaultTextToolbar()

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)

    override val dragAndDropManager: DragAndDropManager get() = TODO("Not yet implemented")

    override val autofillTree = AutofillTree()

    override val autofill: Autofill? get() = null

    override val viewConfiguration: ViewConfiguration = DefaultViewConfiguration(density)

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
        sendKeyEvent(platformInputService, focusOwner, keyEvent)

    override fun forceAccessibilityForTesting() = TODO("Not yet implemented")

    override fun setAccessibilityEventBatchIntervalMillis(accessibilityInterval: Long) =
        TODO("Not yet implemented")

    override var showLayoutBounds = false

    override fun requestFocus() = true

    override fun onAttach(node: LayoutNode) = Unit

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
        needClearObservations = true
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    private var needLayout = true
    private var needDraw = true

    val needRender get() = needLayout || needDraw || needSendSyntheticEvents
    var onNeedRender: (() -> Unit)? = null
    var onDispatchCommand: ((Command) -> Unit)? = null

    fun render(canvas: org.jetbrains.skia.Canvas) {
        needLayout = false
        measureAndLayout()
        sendSyntheticEvents()
        needDraw = false
        draw(canvas)
        clearInvalidObservations()
    }

    private var needClearObservations = false

    private fun clearInvalidObservations() {
        if (needClearObservations) {
            snapshotObserver.clearInvalidObservations()
            needClearObservations = false
        }
    }

    private fun requestLayout() {
        needLayout = true
        needDraw = true
        onNeedRender?.invoke()
    }

    private fun requestDraw() {
        needDraw = true
        onNeedRender?.invoke()
    }

    var contentSize = IntSize.Zero
        private set

    override val placementScope: Placeable.PlacementScope = PlacementScope(this)

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        measureAndLayoutDelegate.updateRootConstraints(constraints)
        if (
            measureAndLayoutDelegate.measureAndLayout(
                scheduleSyntheticEvents.takeIf { sendPointerUpdate }
            )
        ) {
            requestDraw()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()

        // Don't use mainOwner.root.width here, as it strictly coerced by [constraints]
        contentSize = IntSize(
            root.children.maxOfOrNull { it.outerCoordinator.measuredWidth } ?: 0,
            root.children.maxOfOrNull { it.outerCoordinator.measuredHeight } ?: 0,
        )
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
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
                requestLayout()
            }
        } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
            scheduleMeasureAndLayout
        ) {
            requestLayout()
        }
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                requestLayout()
            }
        } else if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
            requestLayout()
        }
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
        requestLayout()
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?
    ) = SkiaLayer(
        density,
        invalidateParentLayer = {
            invalidateParentLayer()
            requestDraw()
        },
        drawBlock = drawBlock,
        onDestroy = { needClearObservations = true }
    )

    override fun onSemanticsChange() {
        accessibilityController?.onSemanticsChange()
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        accessibilityController?.onLayoutChange(layoutNode)
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        return when (keyEvent.key) {
            Tab -> if (keyEvent.isShiftPressed) Previous else Next
            DirectionCenter -> Enter
            Back -> Exit
            else -> null
        }
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset = localPosition

    override fun calculateLocalPosition(positionInWindow: Offset): Offset = positionInWindow

    override fun localToScreen(localPosition: Offset): Offset = localPosition

    override fun localToScreen(localTransform: Matrix) {}

    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

    fun draw(canvas: org.jetbrains.skia.Canvas) {
        root.draw(canvas.asComposeCanvas(), null)
    }

    private var desiredPointerIcon: PointerIcon? = null

    private var needSendSyntheticEvents = false
    private var lastPointerEvent: PointerInputEvent? = null

    private val scheduleSyntheticEvents: () -> Unit = {
        // we can't send event synchronously, as we can have call of `measureAndLayout`
        // inside the event handler. So we can have a situation when we call event handler inside
        // event handler. And that can lead to unpredictable behaviour.
        // Nature of synthetic events doesn't require that they should be fired
        // synchronously on layout change.
        needSendSyntheticEvents = true
        onNeedRender?.invoke()
    }

    // TODO(demin) should we repeat all events, or only which are make sense?
    //  For example, touch Move after touch Release doesn't make sense,
    //  and an application can handle it in a wrong way
    //  Desktop doesn't support touch at the moment, but when it will, we should resolve this.
    private fun sendSyntheticEvents() {
        if (needSendSyntheticEvents) {
            needSendSyntheticEvents = false
            val lastPointerEvent = lastPointerEvent
            if (lastPointerEvent != null) {
                doProcessPointerInput(
                    PointerInputEvent(
                        PointerEventType.Move,
                        lastPointerEvent.uptime,
                        lastPointerEvent.pointers,
                        lastPointerEvent.buttons,
                        lastPointerEvent.keyboardModifiers,
                        lastPointerEvent.mouseEvent
                    )
                )
            }
        }
    }

    internal fun processPointerInput(event: PointerInputEvent): ProcessResult {
        measureAndLayout()
        sendSyntheticEvents()
        desiredPointerIcon = null
        lastPointerEvent = event
        return doProcessPointerInput(event)
    }

    private fun doProcessPointerInput(event: PointerInputEvent): ProcessResult {
        return pointerInputEventProcessor.process(
            event,
            this,
            isInBounds = event.pointers.all {
                it.position.x in 0f..root.width.toFloat() &&
                    it.position.y in 0f..root.height.toFloat()
            }
        ).also {
            if (it.dispatchedToAPointerInputModifier) {
                setPointerIcon(component, desiredPointerIcon)
            }
        }
    }

    override fun processPointerInput(timeMillis: Long, pointers: List<TestPointerInputEventData>) {
        val isPressed = pointers.any { it.down }
        processPointerInput(
            PointerInputEvent(
                PointerEventType.Unknown,
                timeMillis,
                pointers.map { it.toPointerInputEventData() },
                if (isPressed) PrimaryPressedPointerButtons else DefaultPointerButtons
            )
        )
    }

    override fun onEndApplyChanges() {
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
        requestLayout()
    }

    override suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        component.textInputSession(session)
    }

    // A Stub for the PointerIconService required in Owner.kt
    override val pointerIconService: PointerIconService =
        object : PointerIconService {
            override fun getIcon(): PointerIcon {
                return desiredPointerIcon ?: PointerIcon.Default
            }

            override fun setIcon(value: PointerIcon?) {
                desiredPointerIcon = value
            }
        }
}

internal expect fun sendKeyEvent(
    platformInputService: PlatformInput,
    focusOwner: FocusOwner,
    keyEvent: KeyEvent
): Boolean

internal expect fun setPointerIcon(
    containerCursor: PlatformComponentWithCursor?,
    icon: PointerIcon?
)
