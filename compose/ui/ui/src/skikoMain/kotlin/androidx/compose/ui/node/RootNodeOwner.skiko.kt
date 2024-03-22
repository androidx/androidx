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

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusOwnerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.InteropViewCatchPointerModifier
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
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.platform.DefaultAccessibilityManager
import androidx.compose.ui.platform.DefaultHapticFeedback
import androidx.compose.ui.platform.DelegatingSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformClipboardManager
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.RenderNodeLayer
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneInputHandler
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.semantics.EmptySemanticsElement
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.trace
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.awaitCancellation

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
    // TODO(https://github.com/JetBrains/compose-multiplatform/issues/2944)
    //  Check if ComposePanel/SwingPanel focus interop work correctly with new features of
    //  the focus system (it works with the old features like moveFocus/clearFocus)
    val focusOwner: FocusOwner = FocusOwnerImpl(
        parent = platformContext.parentFocusManager
    ) {
        owner.registerOnEndApplyChangesListener(it)
    }.also {
        it.layoutDirection = layoutDirection
    }
    private val rootModifier = EmptySemanticsElement
        .then(focusOwner.modifier)
        .onKeyEvent {
            // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
            //  that this common logic can be used by all owners.
            val focusDirection = owner.getFocusDirection(it)
            if (focusDirection == null || it.type != KeyEventType.KeyDown) return@onKeyEvent false

            platformContext.inputModeManager.requestInputMode(InputMode.Keyboard)
            // Consume the key event if we moved focus.
            focusOwner.moveFocus(focusDirection)
        }
    val owner: Owner = OwnerImpl(layoutDirection, coroutineContext)
    val semanticsOwner = SemanticsOwner(owner.root)
    var size by mutableStateOf(size)
    var density by mutableStateOf(density)

    private val constraints
        get() = size?.toConstraints() ?: Constraints()

    private var _layoutDirection by mutableStateOf(layoutDirection)
    var layoutDirection: LayoutDirection
        get() = _layoutDirection
        set(value) {
            _layoutDirection = value
            focusOwner.layoutDirection = value
            owner.root.layoutDirection = value
        }

    private val rootForTest = PlatformRootForTestImpl()
    private val snapshotObserver = snapshotInvalidationTracker.snapshotObserver()
    private val pointerInputEventProcessor = PointerInputEventProcessor(owner.root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(owner.root)
    private var isDisposed = false

    init {
        snapshotObserver.startObserving()
        owner.root.attach(owner)
        platformContext.rootForTestListener?.onRootForTestCreated(rootForTest)
    }

    fun dispose() {
        check(!isDisposed) { "RootNodeOwner is already disposed" }
        platformContext.rootForTestListener?.onRootForTestDisposed(rootForTest)
        snapshotObserver.stopObserving()
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
                width = children.maxOfOrNull { it.outerCoordinator.measuredWidth } ?: 0,
                height = children.maxOfOrNull { it.outerCoordinator.measuredHeight } ?: 0,
            )
        } finally {
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
        }
    }

    fun measureAndLayout() {
        owner.measureAndLayout(sendPointerUpdate = true)
    }

    fun invalidatePositionInWindow() {
        owner.root.layoutDelegate.measurePassDelegate.notifyChildrenUsingCoordinatesWhilePlacing()
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = true)
    }

    fun draw(canvas: Canvas) = trace("RootNodeOwner:draw") {
        owner.root.draw(canvas)
        clearInvalidObservations()
    }

    fun setRootModifier(modifier: Modifier) {
        owner.root.modifier = rootModifier then modifier
    }

    @OptIn(InternalCoreApi::class)
    fun onPointerInput(event: PointerInputEvent) {
        if (event.button != null) {
            platformContext.inputModeManager.requestInputMode(InputMode.Touch)
        }
        val isInBounds = event.eventType != PointerEventType.Exit &&
            event.pointers.fastAll { isInBounds(it.position) }
        pointerInputEventProcessor.process(
            event,
            IdentityPositionCalculator,
            isInBounds = isInBounds
        )
    }

    fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        return focusOwner.dispatchKeyEvent(keyEvent)
    }

    /**
     * If pointerPosition is inside UIKitView, then Compose skip touches. And touches goes to UIKit.
     */
    fun hitTestInteropView(position: Offset): Boolean {
        val result = HitTestResult()
        owner.root.hitTest(position, result, true)
        val last = result.lastOrNull()
        return (last as? BackwardsCompatNode)?.element is InteropViewCatchPointerModifier
    }

    private fun isInBounds(localPosition: Offset): Boolean =
        size?.toIntRect()?.toRect()?.contains(localPosition) ?: true

    private fun calculateBoundsInWindow(): Rect? {
        val rect = size?.toIntRect()?.toRect() ?: return null
        val p0 = platformContext.calculatePositionInWindow(Offset(rect.left, rect.top))
        val p1 = platformContext.calculatePositionInWindow(Offset(rect.left, rect.bottom))
        val p3 = platformContext.calculatePositionInWindow(Offset(rect.right, rect.top))
        val p4 = platformContext.calculatePositionInWindow(Offset(rect.right, rect.bottom))

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

        override val root = LayoutNode().also {
            it.layoutDirection = layoutDirection
            it.measurePolicy = RootMeasurePolicy
            it.modifier = rootModifier
        }

        override val sharedDrawScope = LayoutNodeDrawScope()
        override val rootForTest get() = this@RootNodeOwner.rootForTest
        override val hapticFeedBack = DefaultHapticFeedback()
        override val inputModeManager get() = platformContext.inputModeManager
        override val clipboardManager = PlatformClipboardManager()
        override val accessibilityManager = DefaultAccessibilityManager()
        override val textToolbar get() = platformContext.textToolbar
        override val autofillTree = AutofillTree()
        override val autofill: Autofill?  get() = null
        override val density get() = this@RootNodeOwner.density
        override val textInputService = TextInputService(platformContext.textInputService)
        override val softwareKeyboardController =
            DelegatingSoftwareKeyboardController(textInputService)

        // TODO https://youtrack.jetbrains.com/issue/COMPOSE-733/Merge-1.6.-Apply-changes-for-the-new-text-input
        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ): Nothing {
            awaitCancellation()
        }
        // TODO https://youtrack.jetbrains.com/issue/COMPOSE-743/Implement-commonMain-Dragdrop-developed-in-AOSP
        override val dragAndDropManager: DragAndDropManager get() = TODO("Not yet implemented")
        override val pointerIconService = PointerIconServiceImpl()
        override val focusOwner get() = this@RootNodeOwner.focusOwner
        override val windowInfo get() = platformContext.windowInfo

        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override val fontLoader = androidx.compose.ui.text.platform.FontLoader()
        override val fontFamilyResolver = createFontFamilyResolver()
        override val layoutDirection get() = _layoutDirection
        override var showLayoutBounds = false
            @InternalCoreApi
            set

        override val modifierLocalManager = ModifierLocalManager(this)
        override val snapshotObserver get() = this@RootNodeOwner.snapshotObserver
        override val viewConfiguration get() = platformContext.viewConfiguration
        override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

        override fun requestFocus() = platformContext.requestFocus()
        override fun onAttach(node: LayoutNode) = Unit
        override fun onDetach(node: LayoutNode) {
            measureAndLayoutDelegate.onNodeDetached(node)
            snapshotObserver.clear(node)
            needClearObservations = true
        }

        override fun measureAndLayout(sendPointerUpdate: Boolean) {
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
            val rootNodeResized = measureAndLayoutDelegate.measureAndLayout {
                if (sendPointerUpdate) {
                    inputHandler.onPointerUpdate()
                }
            }
            if (rootNodeResized) {
                snapshotInvalidationTracker.requestDraw()
            }
            measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
        }

        override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
            measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
            inputHandler.onPointerUpdate()
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
                    snapshotInvalidationTracker.requestLayout()
                }
            } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
                scheduleMeasureAndLayout
            ) {
                snapshotInvalidationTracker.requestLayout()
            }
        }

        override fun onRequestRelayout(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean
        ) {
            this.onRequestMeasure(layoutNode, affectsLookahead, forceRequest, scheduleMeasureAndLayout = true)
        }

        override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
            measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
            snapshotInvalidationTracker.requestLayout()
        }

        override fun createLayer(
            drawBlock: (Canvas) -> Unit,
            invalidateParentLayer: () -> Unit
        ) = RenderNodeLayer(
            density = Snapshot.withoutReadObservation {
                // density is a mutable state that is observed whenever layer is created. the layer
                // is updated manually on draw, so not observing the density changes here helps with
                // performance in layout.
                density
            },
            measureDrawBounds = platformContext.measureDrawLayerBounds,
            invalidateParentLayer = {
                invalidateParentLayer()
                snapshotInvalidationTracker.requestDraw()
            },
            drawBlock = drawBlock,
            onDestroy = { needClearObservations = true }
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

        override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
            return when (keyEvent.key) {
                Key.Tab -> if (keyEvent.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                Key.DirectionCenter -> FocusDirection.Enter
                Key.Back -> FocusDirection.Exit
                else -> null
            }
        }

        override fun calculatePositionInWindow(localPosition: Offset): Offset =
            platformContext.calculatePositionInWindow(localPosition)

        override fun calculateLocalPosition(positionInWindow: Offset): Offset =
            platformContext.calculateLocalPosition(positionInWindow)

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
            snapshotInvalidationTracker.requestLayout()
        }
    }

    private inner class PlatformRootForTestImpl : PlatformRootForTest {
        override val density get() = this@RootNodeOwner.density
        override val textInputService get() = owner.textInputService
        override val semanticsOwner get() = this@RootNodeOwner.semanticsOwner
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
        ) = inputHandler.onPointerEvent(
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
        ) = inputHandler.onPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            nativeEvent = nativeEvent,
            button = button
        )

        /**
         * Handles the input initiated by tests or accessibility.
         */
        override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
            inputHandler.onKeyEvent(keyEvent)
    }

    private inner class PointerIconServiceImpl : PointerIconService {
        private var desiredPointerIcon: PointerIcon? = null
        override fun getIcon(): PointerIcon = desiredPointerIcon ?: PointerIcon.Default
        override fun setIcon(value: PointerIcon?) {
            desiredPointerIcon = value
            platformContext.setPointerIcon(desiredPointerIcon ?: PointerIcon.Default)
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
    constraints: Constraints
) {
    val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else LargeDimension
    val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else LargeDimension
    updateRootConstraints(
        Constraints(constraints.minWidth, maxWidth, constraints.minHeight, maxHeight)
    )
}

private fun IntSize.toConstraints() = Constraints(maxWidth = width, maxHeight = height)

private object IdentityPositionCalculator: PositionCalculator {
    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen
    override fun localToScreen(localPosition: Offset): Offset = localPosition
    override fun localToScreen(localTransform: Matrix) = Unit
}
