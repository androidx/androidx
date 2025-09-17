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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.Q
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
import android.os.Looper
import android.os.StrictMode
import android.os.SystemClock
import android.util.LongSparseArray
import android.util.SparseArray
import android.view.FocusFinder
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.ScrollCaptureTarget
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AnimationUtils
import android.view.autofill.AutofillManager as PlatformAndroidManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.translation.ViewTranslationCallback
import android.view.translation.ViewTranslationRequest
import android.view.translation.ViewTranslationResponse
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectList
import androidx.collection.ScatterMap
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.ForgetfulRetainScope
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RetainScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ComposeUiFlags.isAdaptiveRefreshRateEnabled
import androidx.compose.ui.ComposeUiFlags.isCanScrollUsingLastDownEventFixEnabled
import androidx.compose.ui.ComposeUiFlags.isIndirectTouchNavigationGestureDetectorEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.autofill.AndroidAutofill
import androidx.compose.ui.autofill.AndroidAutofillManager
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillCallback
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.autofill.PlatformAutofillManagerImpl
import androidx.compose.ui.autofill.performAutofill
import androidx.compose.ui.autofill.populateViewStructure
import androidx.compose.ui.contentcapture.AndroidContentCaptureManager
import androidx.compose.ui.contentcapture.ContentCaptureSessionWrapper
import androidx.compose.ui.draganddrop.AndroidDragAndDropManager
import androidx.compose.ui.draganddrop.ComposeDragShadowBuilder
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusOwnerImpl
import androidx.compose.ui.focus.FocusTargetNode
import androidx.compose.ui.focus.PlatformFocusOwner
import androidx.compose.ui.focus.calculateFocusRectRelativeTo
import androidx.compose.ui.focus.focusRect
import androidx.compose.ui.focus.is1dFocusSearch
import androidx.compose.ui.focus.isBetterCandidate
import androidx.compose.ui.focus.requestInteropFocus
import androidx.compose.ui.focus.toAndroidFocusDirection
import androidx.compose.ui.focus.toFocusDirection
import androidx.compose.ui.focus.toLayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.PlatformHapticFeedback
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.indirect.AndroidIndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.convertActionToIndirectTouchEventType
import androidx.compose.ui.input.indirect.indirectPrimaryDirectionalScrollAxis
import androidx.compose.ui.input.indirect.nativeEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AndroidPointerIcon
import androidx.compose.ui.input.pointer.AndroidPointerIconType
import androidx.compose.ui.input.pointer.MatrixPositionCalculator
import androidx.compose.ui.input.pointer.MotionEventAdapter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.InsetsListener
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.layout.RulerKey
import androidx.compose.ui.layout.RulerScope
import androidx.compose.ui.layout.WindowInsetsRulerProvider
import androidx.compose.ui.layout.WindowWindowInsetsAnimationValues
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.provideWindowInsetsRulers
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.OutOfFrameExecutor
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.visitSubtree
import androidx.compose.ui.platform.MotionEventVerifierApi29.isValidMotionEvent
import androidx.compose.ui.platform.coreshims.ViewCompatShims
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.scrollcapture.ScrollCapture
import androidx.compose.ui.semantics.EmptySemanticsModifier
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.findClosestParentNode
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastLastOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.compose.ui.viewinterop.InteropView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.core.view.MotionEventCompat.AXIS_SCROLL
import androidx.core.view.ViewCompat
import androidx.core.view.ViewConfigurationCompat.getScaledHorizontalScrollFactor
import androidx.core.view.ViewConfigurationCompat.getScaledVerticalScrollFactor
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.get
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import java.lang.reflect.Method
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

/** Allows tests to inject a custom [PlatformTextInputService]. */
internal var platformTextInputServiceInterceptor:
    (PlatformTextInputService) -> PlatformTextInputService =
    {
        it
    }

private const val ONE_FRAME_120_HERTZ_IN_MILLISECONDS = 8L

@Suppress("ViewConstructor", "VisibleForTests", "NullAnnotationGroup")
@OptIn(InternalComposeUiApi::class)
internal class AndroidComposeView(context: Context, coroutineContext: CoroutineContext) :
    ViewGroup(context),
    Owner,
    PlatformFocusOwner,
    ViewRootForTest,
    MatrixPositionCalculator,
    DefaultLifecycleObserver,
    OutOfFrameExecutor,
    ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnScrollChangedListener,
    ViewTreeObserver.OnTouchModeChangeListener {

    /**
     * Remembers the position of the last pointer input event that was down. This position will be
     * used to calculate whether this view is considered scrollable via [canScrollHorizontally]/
     * [canScrollVertically].
     */
    private var lastDownPointerPosition: Offset = Offset.Unspecified

    /**
     * Signal that AndroidComposeView's superclass constructors have finished running. If this is
     * false, it's because the runtime's default uninitialized value is currently visible and
     * AndroidComposeView's constructor hasn't started running yet. In this state other expected
     * invariants do not hold, e.g. property delegates may not be initialized. View/ViewGroup have a
     * history of calling non-final methods in their constructors that can lead to this case, e.g.
     * [onRtlPropertiesChanged].
     */
    private var superclassInitComplete = true

    // Allows tests to override the calculated primaryDirectionalMotionAxis from a MotionEvent (see
    // [IndirectTouchEventNavigationSystemTests] for more details).
    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    @VisibleForTesting
    internal var primaryDirectionalMotionAxisOverride:
        IndirectTouchEventPrimaryDirectionalMotionAxis? =
        null

    override val sharedDrawScope = LayoutNodeDrawScope()

    override val view: View
        get() = this

    internal var frameEndScheduler: LifecycleRetainScopeOwner.FrameEndScheduler? = null
    private var lifecycleRetainScopeOwnerEntry: LifecycleRetainScopeOwner.RetainScopeEntry? = null
    override var retainScope: RetainScope = ForgetfulRetainScope
        private set

    override var density by mutableStateOf(Density(context), referentialEqualityPolicy())
        private set

    private lateinit var frameRateCategoryView: View

    internal val isArrEnabled =
        @OptIn(ExperimentalComposeUiApi::class) isAdaptiveRefreshRateEnabled &&
            SDK_INT >= VANILLA_ICE_CREAM

    private val rootSemanticsNode = EmptySemanticsModifier()

    override val focusOwner: FocusOwner = FocusOwnerImpl(this, this)

    override fun getImportantForAutofill(): Int {
        return IMPORTANT_FOR_AUTOFILL_YES
    }

    override var coroutineContext: CoroutineContext = coroutineContext
        // In some rare cases, the CoroutineContext is cancelled (because the parent
        // CompositionContext containing the CoroutineContext is no longer associated with this
        // class). Changing this CoroutineContext to the new CompositionContext's CoroutineContext
        // needs to cancel all Pointer Input Nodes relying on the old CoroutineContext.
        // See [Wrapper.android.kt] for more details.
        set(value) {
            field = value

            val headModifierNode = root.nodes.head

            // Reset head Modifier.Node's pointer input handler (that is, the underlying
            // coroutine used to run the handler for input pointer events).
            if (headModifierNode is SuspendingPointerInputModifierNode) {
                headModifierNode.resetPointerInputHandler()
            }

            // Reset all other Modifier.Node's pointer input handler in the chain.
            headModifierNode.visitSubtree(Nodes.PointerInput) {
                if (it is SuspendingPointerInputModifierNode) {
                    it.resetPointerInputHandler()
                }
            }
        }

    override val dragAndDropManager = AndroidDragAndDropManager(::startDrag)

    private val _windowInfo: LazyWindowInfo = LazyWindowInfo()
    override val windowInfo: WindowInfo
        get() = _windowInfo

    /**
     * Because AndroidComposeView always accepts focus, we have to divert focus to another View if
     * there is nothing focusable within. However, if there are only non focusable ComposeViews,
     * then the redirection can recurse infinitely. This makes sure that if that happens, then it
     * can bail when it is detected.
     *
     * Note: Used only when ComposeUiFlags.isViewFocusFixEnabled is true.
     */
    private var processingRequestFocusForNextNonChildView = false

    private fun moveFocusInChildrenCurrent(focusDirection: FocusDirection): Boolean {
        // The view system does not have an API corresponding to Enter/Exit.
        if (focusDirection == Enter || focusDirection == Exit) return false

        val direction =
            checkPreconditionNotNull(focusDirection.toAndroidFocusDirection()) {
                "Invalid focus direction"
            }
        val focusedRect = getEmbeddedViewFocusRect()?.toAndroidRect()

        val nextView =
            FocusFinderCompat.instance.let {
                if (focusedRect == null) {
                    it.findNextFocus(this, findFocus(), direction)
                } else {
                    it.findNextFocusFromRect(this, focusedRect, direction)
                }
            }
        return nextView?.requestInteropFocus(direction, focusedRect) ?: false
    }

    private fun moveFocusInChildrenViewFocusFix(focusDirection: FocusDirection): Boolean {
        // The view system does not have an API corresponding to Enter/Exit.
        if (focusDirection == Enter || focusDirection == Exit || !hasFocus()) return false

        val androidViewsHandler = _androidViewsHandler ?: return false

        val direction =
            checkPreconditionNotNull(focusDirection.toAndroidFocusDirection()) {
                "Invalid focus direction"
            }

        val root = rootView as ViewGroup

        val currentFocus = root.findFocus() ?: error("view hasFocus but root can't find it")

        val focusFinder = FocusFinderCompat.instance
        val nextView = focusFinder.findNextFocus(root, currentFocus, direction)
        val focusedRect: Rect?
        if (focusDirection.is1dFocusSearch() && androidViewsHandler.hasFocus()) {
            focusedRect = null
        } else {
            focusedRect = getEmbeddedViewFocusRect()?.toAndroidRect()
            if (nextView != null && focusedRect != null) {
                root.offsetDescendantRectToMyCoords(this, focusedRect)
                root.offsetRectIntoDescendantCoords(nextView, focusedRect)
            }
        }

        // is it part of the compose hierarchy?
        if (nextView == null || nextView === currentFocus) {
            return false
        }

        val focusedChild = androidViewsHandler.focusedChild
        var nextParent = nextView.parent
        while (nextParent != null && nextParent !== focusedChild) {
            nextParent = nextParent.parent
        }
        if (nextParent == null) {
            return false // not a part of the compose hierarchy
        }
        return nextView.requestInteropFocus(direction, focusedRect)
    }

    private fun moveFocusInChildrenBypassUnfocusableComposeView(
        focusDirection: FocusDirection
    ): Boolean {
        // The view system does not have an API corresponding to Enter/Exit.
        if (focusDirection == Enter || focusDirection == Exit) return false

        val direction =
            checkPreconditionNotNull(focusDirection.toAndroidFocusDirection()) {
                "Invalid focus direction"
            }
        val nextView = findNextViewInEmbeddedView(focusDirection)
        return nextView?.requestInteropFocus(direction, null) ?: false
    }

    // If an embedded view has focus, we try to move focus within it first.
    @OptIn(ExperimentalComposeUiApi::class)
    override fun moveFocusInChildren(focusDirection: FocusDirection): Boolean =
        when {
            ComposeUiFlags.isViewFocusFixEnabled -> moveFocusInChildrenViewFocusFix(focusDirection)
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled ->
                moveFocusInChildrenBypassUnfocusableComposeView(focusDirection)
            else -> moveFocusInChildrenCurrent(focusDirection)
        }

    private fun findNextViewInEmbeddedView(focusDirection: FocusDirection): View? {
        val activeFocusTargetNode =
            focusOwner.activeFocusTargetNode
                ?: error(
                    "findNextViewInEmbeddedView called when owner does not have anything focused."
                )
        val direction =
            checkPreconditionNotNull(focusDirection.toAndroidFocusDirection()) {
                "Invalid focus direction"
            }
        val interopView = activeFocusTargetNode.requireLayoutNode().getInteropView()
        val currentlyFocusedView = findFocus()

        @OptIn(ExperimentalComposeUiApi::class)
        val nextView =
            if (SDK_INT < 26 && ComposeUiFlags.isPre26FocusFinderFixEnabled) {
                FocusFinderCompat.instance.findNextFocus(
                    rootView as ViewGroup,
                    currentlyFocusedView,
                    direction,
                )
            } else {
                FocusFinder.getInstance()
                    .findNextFocus(rootView as ViewGroup, currentlyFocusedView, direction)
            }

        if (nextView != null && interopView?.containsDescendant(nextView) == true) {
            return nextView
        }
        return null
    }

    // If this root view is focused, we can get the focus rect from focusOwner. But if a sub-view
    // has focus, the rect returned by focusOwner would be the bounds of the focus target
    // surrounding the embedded view. For a more accurate focus rect, we use the bounds of the
    // focused sub-view.
    override fun getEmbeddedViewFocusRect(): androidx.compose.ui.geometry.Rect? =
        // TODO(b/378570682): This function should only fetch the bounds of the embedded focused
        //  view. Move the focusOwner.getFocusRect() logic to all call sites of this function.
        if (isFocused) {
            focusOwner.getFocusRect()
        } else {
            findFocus()?.calculateFocusRectRelativeTo(this)
        }

    // Lets the owner know that a new focus target is available.
    override fun focusTargetAvailable() {
        // This signal is used to assign default focus, we don't need to report anything if some
        // component already has focus.
        if (focusOwner.rootState.hasFocus) return

        focusableViewAvailable(this@AndroidComposeView)
    }

    // Used only when ComposeUiFlags.isViewFocusFixEnabled is true.
    private fun findNextNonChildView(direction: Int): View? {
        var currentView: View? = this
        val focusFinder = FocusFinderCompat.instance
        while (currentView != null) {
            currentView = focusFinder.findNextFocus(rootView as ViewGroup, currentView, direction)
            if (currentView != null && !containsDescendant(currentView)) return currentView
        }
        return null
    }

    private val canvasHolder = CanvasHolder()

    override val viewConfiguration: ViewConfiguration =
        AndroidViewConfiguration(android.view.ViewConfiguration.get(context))

    val insetsListener = InsetsListener(this)

    @OptIn(ExperimentalComposeUiApi::class)
    override val root =
        LayoutNode().also {
            it.measurePolicy = RootMeasurePolicy
            it.density = density
            it.viewConfiguration = viewConfiguration
            // Composed modifiers cannot be added here directly
            it.modifier =
                object : ModifierNodeElement<RootModifierNode>() {
                        override fun create() = RootModifierNode()

                        override fun update(node: RootModifierNode) {}

                        override fun InspectorInfo.inspectableProperties() {
                            name = "rootModifier"
                        }

                        override fun hashCode(): Int = this@AndroidComposeView.hashCode()

                        override fun equals(other: Any?): Boolean = other === this
                    }
                    .then(focusOwner.modifier)
                    .then(dragAndDropManager.modifier)
        }

    override val layoutNodes: MutableIntObjectMap<LayoutNode> = mutableIntObjectMapOf()

    override val rectManager = RectManager(layoutNodes)

    override val rootForTest: RootForTest = this
    internal var uncaughtExceptionHandler: RootForTest.UncaughtExceptionHandler? = null

    override val semanticsOwner: SemanticsOwner =
        SemanticsOwner(root, rootSemanticsNode, layoutNodes)
    private val composeAccessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(this)
    internal var contentCaptureManager =
        AndroidContentCaptureManager(
            view = this,
            onContentCaptureSession = ::getContentCaptureSessionCompat,
        )

    /**
     * Provide accessibility manager to the user. Use the Android version of accessibility manager.
     */
    override val accessibilityManager = AndroidAccessibilityManager(context)

    /**
     * Provide access to a GraphicsContext instance used to create GraphicsLayers for providing
     * isolation boundaries for rendering portions of a Composition hierarchy as well as for
     * achieving certain visual effects like masks and blurs
     */
    override val graphicsContext = GraphicsContext(this)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    override val autofillTree = AutofillTree()

    // OwnedLayers that are dirty and should be redrawn.
    private val dirtyLayers = mutableObjectListOf<OwnedLayer>()

    // OwnerLayers that invalidated themselves during their last draw. They will be redrawn
    // during the next AndroidComposeView dispatchDraw pass.
    private var postponedDirtyLayers: MutableObjectList<OwnedLayer>? = null

    private var isDrawingContent = false
    private var isPendingInteropViewLayoutChangeDispatch = false

    private val motionEventAdapter = MotionEventAdapter()
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    /**
     * Used for updating LocalConfiguration when configuration changes - consume LocalConfiguration
     * instead of changing this observer if you are writing a component that adapts to configuration
     * changes.
     */
    var configurationChangeObserver: (Configuration) -> Unit = {}

    private val _autofill = if (autofillSupported()) AndroidAutofill(this, autofillTree) else null

    internal val _autofillManager =
        if (autofillSupported()) {
            val platformAutofill = context.getSystemService(PlatformAndroidManager::class.java)
            checkPreconditionNotNull(platformAutofill) { "Autofill service could not be located." }
            AndroidAutofillManager(
                platformAutofillManager = PlatformAutofillManagerImpl(platformAutofill),
                semanticsOwner = semanticsOwner,
                view = this,
                rectManager = rectManager,
                packageName = context.packageName,
            )
        } else {
            null
        }

    // Used as a CompositionLocal for performing autofill.
    override val autofill: Autofill?
        get() = _autofill

    // Used as a CompositionLocal for performing semantic autofill.
    override val autofillManager: AutofillManager?
        get() = _autofillManager

    private var observationClearRequested = false

    /** Provide clipboard manager to the user. Use the Android version of clipboard manager. */
    override val clipboardManager = AndroidClipboardManager(context)

    override val clipboard = AndroidClipboard(clipboardManager)

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        val exceptionHandler = uncaughtExceptionHandler
        var command =
            if (exceptionHandler != null) {
                {
                    try {
                        command()
                    } catch (e: Exception) {
                        exceptionHandler.onUncaughtException(e)
                    }
                }
            } else {
                command
            }

        if (handler?.looper === Looper.myLooper()) {
            command()
        } else {
            handler?.post(command)
        }
    }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(InternalCoreApi::class)
    override var showLayoutBounds = false
        get() {
            return if (SDK_INT >= 30) Api30Impl.isShowingLayoutBounds(this) else field
        }

    private var _androidViewsHandler: AndroidViewsHandler? = null
    internal val androidViewsHandler: AndroidViewsHandler
        get() {
            if (_androidViewsHandler == null) {
                _androidViewsHandler = AndroidViewsHandler(context)
                addView(_androidViewsHandler)
                // Ensure that AndroidViewsHandler is measured and laid out after creation, so that
                // it can report correct bounds on screen (for semantics, etc).
                // Normally this is done by addView, but here we disabled it for optimization
                // purposes.
                requestLayout()
            }
            return _androidViewsHandler!!
        }

    private var viewLayersContainer: DrawChildContainer? = null

    // The constraints being used by the last onMeasure. It is set to null in onLayout. It allows
    // us to detect the case when the View was measured twice with different constraints within
    // the same measure pass.
    private var onMeasureConstraints: Constraints? = null

    // Will be set to true when we were measured twice with different constraints during the last
    // measure pass.
    private var wasMeasuredWithMultipleConstraints = false

    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    override val measureIteration: Long
        get() = measureAndLayoutDelegate.measureIteration

    override val hasPendingMeasureOrLayout
        get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

    private var globalPosition: IntOffset = IntOffset(Int.MAX_VALUE, Int.MAX_VALUE)

    private val tmpPositionArray = intArrayOf(0, 0)
    private val tmpMatrix = Matrix()
    private val viewToWindowMatrix = Matrix()
    private val windowToViewMatrix = Matrix()

    @VisibleForTesting internal var lastMatrixRecalculationAnimationTime = -1L
    private var forceUseMatrixCache = false

    /**
     * On some devices, the `getLocationOnScreen()` returns `(0, 0)` even when the Window is offset
     * in special circumstances. This contains the screen coordinates of the containing Window the
     * last time the [viewToWindowMatrix] and [windowToViewMatrix] were recalculated.
     */
    private var windowPosition = Offset.Infinite

    // Used to track whether or not there was an exception while creating an MRenderNode
    // so that we don't have to continue using try/catch after fails once.
    private var isRenderNodeCompatible = true

    private var _viewTreeOwners: ViewTreeOwners? by mutableStateOf(null)

    // Having an extra derived state here (instead of directly using _viewTreeOwners) is a
    // workaround for b/271579465 to avoid unnecessary extra recompositions when this is mutated
    // before setContent is called.
    /**
     * Current [ViewTreeOwners]. Use [setOnViewTreeOwnersAvailable] if you want to execute your code
     * when the object will be created.
     */
    val viewTreeOwners: ViewTreeOwners? by derivedStateOf { _viewTreeOwners }

    private var onViewTreeOwnersAvailable: ((ViewTreeOwners) -> Unit)? = null

    private val legacyTextInputServiceAndroid = TextInputServiceAndroid(view, this)

    /**
     * The legacy text input service. This is only used for new text input sessions if
     * [textInputSessionMutex] is null.
     */
    @Deprecated("Use PlatformTextInputModifierNode instead.")
    override val textInputService =
        TextInputService(platformTextInputServiceInterceptor(legacyTextInputServiceAndroid))

    private val textInputSessionMutex = SessionMutex<AndroidPlatformTextInputSession>()

    override val softwareKeyboardController: SoftwareKeyboardController =
        DelegatingSoftwareKeyboardController(textInputService)

    override val placementScope: Placeable.PlacementScope
        get() = PlacementScope(this)

    override suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing =
        textInputSessionMutex.withSessionCancellingPrevious(
            sessionInitializer = {
                AndroidPlatformTextInputSession(
                    view = this,
                    textInputService = textInputService,
                    coroutineScope = it,
                )
            },
            session = session,
        )

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver"),
    )
    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    // Backed by mutableStateOf so that the local provider recomposes when it changes
    // FontFamily.Resolver is not guaranteed to be stable or immutable, hence referential check
    override var fontFamilyResolver: FontFamily.Resolver by
        mutableStateOf(createFontFamilyResolver(context), referentialEqualityPolicy())
        private set

    // keeps track of changes in font weight adjustment to update fontFamilyResolver
    private var currentFontWeightAdjustment: Int =
        context.resources.configuration.fontWeightAdjustmentCompat

    private val Configuration.fontWeightAdjustmentCompat: Int
        get() = if (SDK_INT >= S) fontWeightAdjustment else 0

    // Backed by mutableStateOf so that the ambient provider recomposes when it changes
    override var layoutDirection by
        mutableStateOf(
            // We don't use the attached View's layout direction here since that layout direction
            // may not be resolved since composables may be composed without attaching to the
            // RootViewImpl.
            // In Jetpack Compose, use the locale layout direction (i.e. layoutDirection came from
            // configuration) as a default layout direction.
            toLayoutDirection(context.resources.configuration.layoutDirection)
                ?: LayoutDirection.Ltr
        )
        private set

    /** Provide haptic feedback to the user. Use the Android version of haptic feedback. */
    override val hapticFeedBack: HapticFeedback = PlatformHapticFeedback(this)

    /** Provide an instance of [InputModeManager] which is available as a CompositionLocal. */
    private val _inputModeManager =
        InputModeManagerImpl(
            initialInputMode = if (isInTouchMode) Touch else Keyboard,
            onRequestInputModeChange = {
                when (it) {
                    // Android doesn't support programmatically switching to touch mode, so we
                    // don't do anything, but just return true if we are already in touch mode.
                    Touch -> isInTouchMode

                    // If we are already in keyboard mode, we return true, otherwise, we call
                    // requestFocusFromTouch, which puts the system in non-touch mode.
                    Keyboard -> if (isInTouchMode) requestFocusFromTouch() else true
                    else -> false
                }
            },
        )
    override val inputModeManager: InputModeManager
        get() = _inputModeManager

    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)

    /**
     * Provide textToolbar to the user, for text-related operation. Use the Android version of
     * floating toolbar(post-M) and primary toolbar(pre-M).
     */
    override val textToolbar: TextToolbar = AndroidTextToolbar(this)

    /**
     * When the first event for a mouse is ACTION_DOWN, an ACTION_HOVER_ENTER is never sent. This
     * means that we won't receive an `Enter` event for the first mouse. In order to prevent this
     * problem, we track whether or not the previous event was with the mouse inside and if not, we
     * can create a simulated mouse enter event to force an enter.
     */
    private var previousMotionEvent: MotionEvent? = null

    /** The time of the last layout. This is used to send a synthetic MotionEvent. */
    private var relayoutTime = 0L

    /**
     * A cache for OwnedLayers. Recreating ViewLayers is expensive, so we avoid it as much as
     * possible. This also helps a little with RenderNodeLayers as well.
     */
    private val layerCache = WeakCache<OwnedLayer>()

    /** List of lambdas to be called when [onEndApplyChanges] is called. */
    private val endApplyChangesListeners = mutableObjectListOf<(() -> Unit)?>()

    private var currentFrameRate = 0f
    private var currentFrameRateCategory = 0f

    /**
     * Runnable used to update the pointer position after layout. If another pointer event comes in
     * before this runs, this Runnable will be removed and not executed.
     */
    private val resendMotionEventRunnable =
        object : Runnable {
            override fun run() {
                removeCallbacks(this)
                val lastMotionEvent = previousMotionEvent
                if (lastMotionEvent != null) {
                    val wasMouseEvent = lastMotionEvent.getToolType(0) == TOOL_TYPE_MOUSE
                    val action = lastMotionEvent.actionMasked
                    val resend =
                        if (wasMouseEvent) {
                            action != ACTION_HOVER_EXIT && action != ACTION_UP
                        } else {
                            action != ACTION_UP
                        }
                    if (resend) {
                        val newAction =
                            if (action == ACTION_HOVER_MOVE || action == ACTION_HOVER_ENTER) {
                                ACTION_HOVER_MOVE
                            } else {
                                ACTION_MOVE
                            }
                        sendSimulatedEvent(
                            lastMotionEvent,
                            newAction,
                            relayoutTime,
                            forceHover = false,
                        )
                    }
                }
            }
        }

    /**
     * If an [ACTION_HOVER_EXIT] event is received, it could be because an [ACTION_DOWN] is coming
     * from a mouse or stylus. We can't know for certain until the next event is sent. This message
     * is posted after receiving the [ACTION_HOVER_EXIT] to send the event if nothing else is
     * received before that.
     */
    private val sendHoverExitEvent = Runnable {
        hoverExitReceived = false
        val lastEvent = previousMotionEvent!!
        check(lastEvent.actionMasked == ACTION_HOVER_EXIT) {
            "The ACTION_HOVER_EXIT event was not cleared."
        }
        sendMotionEvent(lastEvent)
    }

    /** Set to `true` when [sendHoverExitEvent] has been posted. */
    private var hoverExitReceived = false

    // Enables event stream tracking for indirect touch navigation gestures.
    private var indirectTouchNavigationGestureDetectorActiveForEventStream = false
    // Determines scroll/swipe to next or previous focusable element for indirect touch events.
    private val indirectTouchNavigationGestureDetector =
        IndirectTouchNavigationGestureDetector(context) {
            focusOwner.moveFocus(focusDirection = it, wrapAroundForOneDimensionalFocus = false)
        }

    /** Callback for [measureAndLayout] to update the pointer position 150ms after layout. */
    private val resendMotionEventOnLayout: () -> Unit = {
        val lastEvent = previousMotionEvent
        if (lastEvent != null) {
            when (lastEvent.actionMasked) {
                // We currently only care about hover events being updated when layout changes
                ACTION_HOVER_ENTER,
                ACTION_HOVER_MOVE -> {
                    relayoutTime = SystemClock.uptimeMillis()
                    post(resendMotionEventRunnable)
                }
            }
        }
    }

    private val matrixToWindow =
        if (SDK_INT < Q) CalculateMatrixToWindowApi21(tmpMatrix) else CalculateMatrixToWindowApi29()

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events. This flag is set when window focus changes. Then we can rely on it
     * when handling the first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used to
     * update windowInfo.keyboardModifiers. See [onWindowFocusChanged] and [sendMotionEvent]
     */
    private var keyboardModifiersRequireUpdate = false

    init {
        addOnAttachStateChangeListener(contentCaptureManager)
        setWillNotDraw(false)
        isFocusable = true
        if (SDK_INT >= O) {
            AndroidComposeViewVerificationHelperMethodsO.focusable(
                this,
                focusable = FOCUSABLE,
                defaultFocusHighlightEnabled = false,
            )
        }
        isFocusableInTouchMode = true
        clipChildren = false
        ViewCompat.setAccessibilityDelegate(this, composeAccessibilityDelegate)
        ViewRootForTest.onViewCreatedCallback?.invoke(this)
        setOnDragListener(dragAndDropManager)
        root.attach(this)

        // Support for this feature in Compose is tracked here: b/207654434
        if (SDK_INT >= Q) AndroidComposeViewForceDarkModeQ.disallowForceDark(this)

        if (isArrEnabled) {
            frameRateCategoryView =
                View(context).apply {
                    layoutParams = LayoutParams(1, 1)
                    // hide this View from layout inspector
                    setTag(R.id.hide_in_inspector_tag, true)
                }
            addView(frameRateCategoryView)
        }
    }

    /**
     * Since this view has its own concept of internal focus, it needs to report that to the view
     * system for accurate focus searching and so ViewRootImpl will scroll correctly.
     */
    override fun getFocusedRect(rect: Rect) {
        val focusRect = getEmbeddedViewFocusRect()
        if (focusRect != null) {
            rect.left = focusRect.left.fastRoundToInt()
            rect.top = focusRect.top.fastRoundToInt()
            rect.right = focusRect.right.fastRoundToInt()
            rect.bottom = focusRect.bottom.fastRoundToInt()
        } else {
            if (focusOwner.focusSearch(Down, null) { true } != true) {
                rect.set(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
            } else {
                super.getFocusedRect(rect)
            }
        }
    }

    override fun addFocusables(views: ArrayList<View?>?, direction: Int, focusableMode: Int) {
        @OptIn(ExperimentalComposeUiApi::class)
        when {
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled -> {
                if (focusOwner.hasFocusableContent()) {
                    super.addFocusables(views, direction, focusableMode)

                    // If we don't have focusables in compose, but only embedded views that are
                    // focusable, the view framework's focus search will find these embedded
                    // views directly.
                    if (!focusOwner.hasNonInteropFocusableContent()) {
                        views?.remove(this)
                    }
                }
            }
            else -> super.addFocusables(views, direction, focusableMode)
        }
    }

    /**
     * Avoid crash by not traversing assist structure. Autofill assistStructure will be dispatched
     * via `dispatchProvideAutofillStructure` from Android 8 and on. See b/251152083 and b/320768586
     * more details.
     */
    override fun dispatchProvideStructure(structure: ViewStructure) {
        if (SDK_INT in 23..27) {
            AndroidComposeViewAssistHelperMethodsO.setClassName(structure, view)
        } else {
            super.dispatchProvideStructure(structure)
        }
    }

    private val scrollCapture = if (SDK_INT >= 31) ScrollCapture() else null
    internal val scrollCaptureInProgress: Boolean
        get() =
            if (SDK_INT >= 31) {
                scrollCapture?.scrollCaptureInProgress ?: false
            } else {
                false
            }

    override fun onScrollCaptureSearch(
        localVisibleRect: Rect,
        windowOffset: Point,
        targets: Consumer<ScrollCaptureTarget>,
    ) {
        if (SDK_INT >= 31) {
            scrollCapture?.onScrollCaptureSearch(
                view = this,
                semanticsOwner = semanticsOwner,
                coroutineContext = coroutineContext,
                targets = targets,
            )
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // Refresh in onResume in case the value has changed.
        if (SDK_INT < 30) {
            showLayoutBounds = getIsShowingLayoutBounds()
        }
        lifecycleRetainScopeOwnerEntry?.stopKeepingExitedValues(frameEndScheduler!!)
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleRetainScopeOwnerEntry?.startKeepingExitedValues()
    }

    override fun focusSearch(focused: View?, direction: Int): View? {
        // do not propagate search if a measurement is happening
        if (focused == null || measureAndLayoutDelegate.duringMeasureLayout) {
            return super.focusSearch(focused, direction)
        }

        // Find the next subview if any using FocusFinder.
        // Note: We use don't use this as the root for search because it can end up looping around
        // the children. So we use rootView instead, and then check if the view returned by
        // findNextFocus is a descendant of this view.
        val root = rootView as ViewGroup
        @OptIn(ExperimentalComposeUiApi::class)
        val nextView =
            if (SDK_INT < 26 && ComposeUiFlags.isPre26FocusFinderFixEnabled) {
                    FocusFinderCompat.instance.findNextFocus(root, focused, direction)
                } else {
                    FocusFinder.getInstance().findNextFocus(root, focused, direction)
                }
                ?.takeIf { containsDescendant(it) }

        // Find the next composable using FocusOwner.
        val focusedBounds =
            if (focused === this) {
                focusOwner.getFocusRect() ?: focused.calculateFocusRectRelativeTo(this)
            } else {
                focused.calculateFocusRectRelativeTo(this)
            }
        val focusDirection = toFocusDirection(direction) ?: Down
        var focusTarget: FocusTargetNode? = null
        val searchResult =
            focusOwner.focusSearch(focusDirection, focusedBounds) {
                focusTarget = it
                true
            }

        return when {
            searchResult == null -> focused // Focus Search Cancelled.
            focusTarget == null ->
                nextView ?: super.focusSearch(focused, direction) // No compose focus item.
            nextView == null -> this // No view found, so go to the focus target.
            focusDirection.is1dFocusSearch() -> {
                @OptIn(ExperimentalComposeUiApi::class)
                if (ComposeUiFlags.isBypassUnfocusableComposeViewEnabled) {
                    // We bypass non focusable compose views, so if FocusFinder's focus search
                    // reached this view, we have something focusable in compose. Embedded views
                    // have associated focus targets, so they will be found using a compose focus
                    // search. we don't need to call super.focusSearch().
                    // Note: View.focusSearch is a public API. So when this is called directly,
                    // returning this is still valid, because it sets isFocusable = true and
                    // isFocusableInTouchMode = true.
                    this
                } else {
                    super.focusSearch(focused, direction)
                }
            }
            isBetterCandidate(
                focusTarget.focusRect(),
                nextView.calculateFocusRectRelativeTo(this),
                focusedBounds,
                focusDirection,
            ) -> this // Compose focus is better than View focus.
            else -> nextView // View focus is better than Compose focus.
        }
    }

    fun requestFocusCurrent(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        // This view is already focused.
        if (isFocused) return true

        // If the root has focus, it means a sub-view is focused,
        // and is trying to move focus within itself.
        if (focusOwner.rootState.hasFocus) {
            return super.requestFocus(direction, previouslyFocusedRect)
        }

        val focusDirection = toFocusDirection(direction) ?: Enter
        return focusOwner.focusSearch(
            focusDirection = focusDirection,
            focusedRect = previouslyFocusedRect?.toComposeRect(),
        ) {
            it.requestFocus(focusDirection)
        } == true
    }

    fun requestFocusViewFocusFix(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        // This view is already focused.
        if (isFocused) return true

        // There is nothing focusable and we've looped around all Views back to this one, so
        // we just return false to indicate that nothing can be focused.
        if (processingRequestFocusForNextNonChildView) return false

        val focusDirection = toFocusDirection(direction) ?: Enter

        // If the root has focus, it means a sub-view is focused,
        // and is trying to move focus within itself.
        if (hasFocus() && moveFocusInChildren(focusDirection)) return true

        var foundFocusable = false
        val focusSearchResult =
            focusOwner.focusSearch(
                focusDirection = focusDirection,
                focusedRect = previouslyFocusedRect?.toComposeRect(),
            ) {
                foundFocusable = true
                it.requestFocus(focusDirection)
            }
        if (focusSearchResult == null) {
            return false // The focus search was canceled
        }
        if (focusSearchResult) {
            return true // We found something to focus on
        }
        if (foundFocusable) {
            return false // The requestFocus() from within the focusSearch was canceled
        }

        if (previouslyFocusedRect != null && !hasFocus()) {
            // try searching, ignoring the previously focused rect. We've had a request to focus on
            // this specific View
            val altFocus =
                focusOwner.focusSearch(focusDirection = focusDirection, focusedRect = null) {
                    it.requestFocus(focusDirection)
                }
            if (altFocus == true) {
                // found alternative focus
                return true
            }
        }

        // We advertised ourselves as focusable, but we aren't. Try to just move the focus to the
        // next item.
        val nextFocusedView = findNextNonChildView(direction)

        // Can crash if we return false when we've advertised ourselves as focusable and we aren't
        // b/369256395
        if (nextFocusedView == null || nextFocusedView === this) {
            // There is no next View, so just return true so we don't cause a crash
            return true
        }

        // Try to focus on the next focusable View
        processingRequestFocusForNextNonChildView = true
        val requestFocusResult = nextFocusedView.requestFocus(direction)
        processingRequestFocusForNextNonChildView = false
        return requestFocusResult
    }

    fun requestFocusBypassUnfocusableComposeView(
        direction: Int,
        previouslyFocusedRect: Rect?,
    ): Boolean {
        // This view is already focused.
        if (isFocused) return true

        // If the root has focus, it means a sub-view is focused, but focus search returned back to
        // this view, which means we have to grant focus to a composable for 1D focus search.
        // and is trying to move focus within itself.

        val focusDirection = toFocusDirection(direction) ?: Enter

        // Grant focus to a focus target in the Compose hierarchy.
        val requestFocusWithPrevRect =
            focusOwner.focusSearch(
                focusDirection = focusDirection,
                focusedRect = previouslyFocusedRect?.toComposeRect(),
            ) {
                it.requestFocus(focusDirection)
            }
        if (requestFocusWithPrevRect == true) return true

        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isIgnoreInvalidPrevFocusRectEnabled) {
            val requestFocusWithoutPrevRect =
                focusOwner.focusSearch(focusDirection = focusDirection, focusedRect = null) {
                    it.requestFocus(focusDirection)
                }
            if (requestFocusWithoutPrevRect == true) return true
        }

        // If we landed on this view and a sub-view already has focus, it means that FocusFinder
        // could not find something else to focus on, and rolled over and returned back to this
        // view. Just to verify that this is the case, we also check if this is a 1-D focus search,
        // as only 1-D focus searches should roll over.
        if (hasFocus() && focusDirection.is1dFocusSearch()) {
            return focusOwner.resetFocus(focusDirection)
        }
        return false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean =
        when {
            ComposeUiFlags.isViewFocusFixEnabled ->
                requestFocusViewFocusFix(direction, previouslyFocusedRect)
            ComposeUiFlags.isBypassUnfocusableComposeViewEnabled ->
                requestFocusBypassUnfocusableComposeView(direction, previouslyFocusedRect)
            else -> requestFocusCurrent(direction, previouslyFocusedRect)
        }

    override fun requestOwnerFocus(
        focusDirection: FocusDirection?,
        previouslyFocusedRect: androidx.compose.ui.geometry.Rect?,
    ): Boolean {

        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isBypassUnfocusableComposeViewEnabled) {
            // We don't request focus if the view is already focused.
            if (isFocused) return true
        } else {
            // We don't request focus if the view is already focused, or if an embedded view is
            // focused,
            // because this would cause the embedded view to lose focus.
            if (isFocused || hasFocus()) return true
        }

        return super.requestFocus(
            focusDirection?.toAndroidFocusDirection() ?: FOCUS_DOWN,
            @Suppress("DEPRECATION") previouslyFocusedRect?.toAndroidRect(),
        )
    }

    override fun clearOwnerFocus() {
        @OptIn(ExperimentalComposeUiApi::class)
        if (isFocused || (!ComposeUiFlags.isViewFocusFixEnabled && hasFocus())) {
            super.clearFocus()
        } else if (hasFocus()) {
            // Call clearFocus() on the child that has focus
            findFocus()?.clearFocus()
            super.clearFocus()
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (!gainFocus && !hasFocus()) {
            focusOwner.releaseFocus()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        _windowInfo.isWindowFocused = hasWindowFocus
        keyboardModifiersRequireUpdate = true
        super.onWindowFocusChanged(hasWindowFocus)

        if (hasWindowFocus && SDK_INT < 30) {
            // Refresh in onResume in case the value has changed from the quick settings tile, in
            // which case the activity won't be paused/resumed (b/225937688).
            getIsShowingLayoutBounds().also { newShowLayoutBounds ->
                if (showLayoutBounds != newShowLayoutBounds) {
                    showLayoutBounds = newShowLayoutBounds
                    // Unlike in onResume, getting window focus doesn't automatically trigger a new
                    // draw pass, so we have to do that manually.
                    invalidateDescendants()
                }
            }
        }
    }

    /** This function is used by the testing framework to send key events. */
    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
        // First dispatch the key event to mimic the event being intercepted before it is sent to
        // the soft keyboard.
        focusOwner.dispatchInterceptedSoftKeyboardEvent(keyEvent) ||
            // Next, send the key event to the Soft Keyboard.
            // TODO(b/272600716): Send the key event to the IME.

            // Finally, dispatch the key event to onPreKeyEvent/onKeyEvent listeners.
            focusOwner.dispatchKeyEvent(keyEvent)

    /** This function is used by the testing framework to send indirect touch events. */
    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    override fun sendIndirectTouchEvent(indirectTouchEvent: IndirectTouchEvent): Boolean {
        return handleIndirectTouchEvent(indirectTouchEvent)
    }

    override fun dispatchKeyEvent(event: AndroidKeyEvent): Boolean =
        if (isFocused) {
            // Focus lies within the Compose hierarchy, so we dispatch the key event to the
            // appropriate place.
            _windowInfo.keyboardModifiers = PointerKeyboardModifiers(event.metaState)
            // If the event is not consumed, use the default implementation.
            focusOwner.dispatchKeyEvent(KeyEvent(event)) || super.dispatchKeyEvent(event)
        } else {
            // This Owner has a focused child view, which is a view interoperability use case,
            // so we use the default ViewGroup behavior which will route tke key event to the
            // focused child view.
            focusOwner.dispatchKeyEvent(
                keyEvent = KeyEvent(event),
                onFocusedItem = {
                    // TODO(b/320510084): Add tests to verify that embedded views receive key
                    // events.
                    super.dispatchKeyEvent(event)
                },
            )
        }

    override fun dispatchKeyEventPreIme(event: AndroidKeyEvent): Boolean {
        return (isFocused && focusOwner.dispatchInterceptedSoftKeyboardEvent(KeyEvent(event))) ||
            // If this view is not focused, and it received a key event, it means this is a view
            // interoperability use case and we need to route the event to the embedded child view.
            // Also, if this event wasn't consumed by the compose hierarchy, we need to send it back
            // to the parent view. Both these cases are handles by the default view implementation.
            super.dispatchKeyEventPreIme(event)
    }

    /**
     * This function is used by the delegate file to enable accessibility frameworks for testing.
     */
    override fun forceAccessibilityForTesting(enable: Boolean) {
        composeAccessibilityDelegate.accessibilityForceEnabledForTesting = enable
    }

    /**
     * This function is used by the delegate file to set the time interval between sending
     * accessibility events in milliseconds.
     */
    override fun setAccessibilityEventBatchIntervalMillis(intervalMillis: Long) {
        composeAccessibilityDelegate.SendRecurringAccessibilityEventsIntervalMillis = intervalMillis
    }

    override fun onPreAttach(node: LayoutNode) {
        layoutNodes[node.semanticsId] = node
    }

    override fun onPostAttach(node: LayoutNode) {
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.onPostAttach(node)
        }
    }

    override fun onDetach(node: LayoutNode) {
        layoutNodes.remove(node.semanticsId)
        measureAndLayoutDelegate.onNodeDetached(node)
        requestClearInvalidObservations()
        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isRectTrackingEnabled) {
            rectManager.remove(node)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.onDetach(node)
        }
    }

    override fun requestAutofill(node: LayoutNode) {
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.requestAutofill(node)
        }
    }

    fun requestClearInvalidObservations() {
        observationClearRequested = true
    }

    override fun onEndApplyChanges() {
        if (observationClearRequested) {
            snapshotObserver.clearInvalidObservations()
            observationClearRequested = false
        }
        val childAndroidViews = _androidViewsHandler
        if (childAndroidViews != null) {
            clearChildInvalidObservations(childAndroidViews)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.onEndApplyChanges()
        }
        // Listeners can add more items to the list and we want to ensure that they
        // are executed after being added, so loop until the list is empty
        while (endApplyChangesListeners.isNotEmpty() && endApplyChangesListeners[0] != null) {
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

    private fun startDrag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean {
        val density =
            with(context.resources) {
                Density(density = displayMetrics.density, fontScale = configuration.fontScale)
            }
        val shadowBuilder =
            ComposeDragShadowBuilder(
                density = density,
                decorationSize = decorationSize,
                drawDragDecoration = drawDragDecoration,
            )
        @Suppress("DEPRECATION")
        return if (SDK_INT >= N) {
            AndroidComposeViewStartDragAndDropN.startDragAndDrop(
                view = this,
                transferData = transferData,
                dragShadowBuilder = shadowBuilder,
            )
        } else {
            startDrag(
                transferData.clipData,
                shadowBuilder,
                transferData.localState,
                transferData.flags,
            )
        }
    }

    private fun clearChildInvalidObservations(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AndroidComposeView) {
                child.onEndApplyChanges()
            } else if (child is ViewGroup) {
                clearChildInvalidObservations(child)
            }
        }
    }

    private fun addExtraDataToAccessibilityNodeInfoHelper(
        virtualViewId: Int,
        info: AccessibilityNodeInfo,
        extraDataKey: String,
    ) {
        // This extra is just for testing: needed a way to retrieve `traversalBefore` and
        // `traversalAfter` from a non-sealed instance of an ANI
        when (extraDataKey) {
            composeAccessibilityDelegate.ExtraDataTestTraversalBeforeVal -> {
                composeAccessibilityDelegate.idToBeforeMap.getOrDefault(virtualViewId, -1).let {
                    if (it != -1) {
                        info.extras.putInt(extraDataKey, it)
                    }
                }
            }
            composeAccessibilityDelegate.ExtraDataTestTraversalAfterVal -> {
                composeAccessibilityDelegate.idToAfterMap.getOrDefault(virtualViewId, -1).let {
                    if (it != -1) {
                        info.extras.putInt(extraDataKey, it)
                    }
                }
            }
            else -> {}
        }
    }

    override fun addView(child: View?) = addView(child, -1)

    override fun addView(child: View?, index: Int) =
        addView(child, index, child!!.layoutParams ?: generateDefaultLayoutParams())

    override fun addView(child: View?, width: Int, height: Int) =
        addView(
            child,
            -1,
            generateDefaultLayoutParams().also {
                it.width = width
                it.height = height
            },
        )

    override fun addView(child: View?, params: LayoutParams?) = addView(child, -1, params)

    /**
     * Directly adding _real_ [View]s to this view is not supported for external consumers, so we
     * can use the non-layout-invalidating [addViewInLayout] for when we need to add utility
     * container views, such as [viewLayersContainer].
     */
    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        addViewInLayout(child, index, params, /* preventRequestLayout= */ true)
    }

    /**
     * Called to inform the owner that a new Android [View] was [attached][Owner.onPreAttach] to the
     * hierarchy.
     */
    fun addAndroidView(view: AndroidViewHolder, layoutNode: LayoutNode) {
        androidViewsHandler.holderToLayoutNode[view] = layoutNode
        androidViewsHandler.addView(view)
        androidViewsHandler.layoutNodeToHolder[layoutNode] = view
        // Fetching AccessibilityNodeInfo from a View which is not set to
        // IMPORTANT_FOR_ACCESSIBILITY_YES will return null.
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
        val thisView = this
        ViewCompat.setAccessibilityDelegate(
            view,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)

                    // Prevent TalkBack from trying to focus the AndroidViewHolder.
                    // This also prevents UIAutomator from finding nodes, so don't
                    // do it if there are no enabled a11y services (which implies that
                    // UIAutomator is the one requesting an AccessibilityNodeInfo).
                    if (composeAccessibilityDelegate.isEnabled) {
                        info.isVisibleToUser = false
                    }

                    var parentId =
                        layoutNode
                            .findClosestParentNode { it.nodes.has(Nodes.Semantics) }
                            ?.semanticsId
                    if (
                        parentId == null || parentId == semanticsOwner.unmergedRootSemanticsNode.id
                    ) {
                        parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    }
                    info.setParent(thisView, parentId)
                    val semanticsId = layoutNode.semanticsId

                    val beforeId =
                        composeAccessibilityDelegate.idToBeforeMap.getOrDefault(semanticsId, -1)
                    if (beforeId != -1) {
                        val beforeView = androidViewsHandler.semanticsIdToView(beforeId)
                        if (beforeView != null) {
                            // If the node that should come before this one is a view, we want to
                            // pass in the "before" view itself, which is retrieved
                            // from `androidViewsHandler.idToViewMap`.
                            info.setTraversalBefore(beforeView)
                        } else {
                            // Otherwise, we'll just set the "before" value by passing in
                            // the semanticsId.
                            info.setTraversalBefore(thisView, beforeId)
                        }
                        addExtraDataToAccessibilityNodeInfoHelper(
                            semanticsId,
                            info.unwrap(),
                            composeAccessibilityDelegate.ExtraDataTestTraversalBeforeVal,
                        )
                    }

                    val afterId =
                        composeAccessibilityDelegate.idToAfterMap.getOrDefault(semanticsId, -1)
                    if (afterId != -1) {
                        val afterView = androidViewsHandler.semanticsIdToView(afterId)
                        if (afterView != null) {
                            info.setTraversalAfter(afterView)
                        } else {
                            info.setTraversalAfter(thisView, afterId)
                        }
                        addExtraDataToAccessibilityNodeInfoHelper(
                            semanticsId,
                            info.unwrap(),
                            composeAccessibilityDelegate.ExtraDataTestTraversalAfterVal,
                        )
                    }
                }
            },
        )
    }

    /**
     * Called to inform the owner that an Android [View] was [detached][Owner.onDetach] from the
     * hierarchy.
     */
    fun removeAndroidView(view: AndroidViewHolder) {
        androidViewsHandler.removeViewInLayout(view)
        androidViewsHandler.layoutNodeToHolder.remove(
            androidViewsHandler.holderToLayoutNode.remove(view)
        )
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    }

    /** Called to ask the owner to draw a child Android [View] to [canvas]. */
    fun drawAndroidView(view: AndroidViewHolder, canvas: android.graphics.Canvas) {
        androidViewsHandler.drawView(view, canvas)
    }

    private fun scheduleMeasureAndLayout(nodeToRemeasure: LayoutNode? = null) {
        if (!isLayoutRequested && isAttachedToWindow) {
            if (nodeToRemeasure != null) {
                // if [nodeToRemeasure] can potentially resize the root we should call
                // requestLayout() so our parent View can react on this change on the same frame.
                // if instead we just call invalidate() and remeasure inside dispatchDraw()
                // this will cause inconsistency as the Compose content will already have the
                // new size, but the View hierarchy will react only on the next frame.
                var node = nodeToRemeasure
                while (
                    node != null &&
                        node.measuredByParent == UsageByParent.InMeasureBlock &&
                        node.childSizeCanAffectParentSize()
                ) {
                    node = node.parent
                }
                if (node === root) {
                    requestLayout()
                    return
                }
            }
            if (width == 0 || height == 0) {
                // if the view has no size calling invalidate() will be skipped
                requestLayout()
            } else {
                invalidate()
            }
        }
    }

    private fun LayoutNode.childSizeCanAffectParentSize(): Boolean {
        // if the view was measured twice with different constraints last time it means the
        // constraints we have could be not the final constraints and in fact our parent
        // ViewGroup can remeasure us with different constraints if we call requestLayout().
        return wasMeasuredWithMultipleConstraints ||
            // when parent's [hasFixedInnerContentConstraints] is true the child size change
            // can't affect parent size as the size is fixed. for example it happens when parent
            // has Modifier.fillMaxSize() set on it.
            parent?.hasFixedInnerContentConstraints == false
    }

    internal var isSendPendingContentCaptureEventsScheduled = false
    private val cachedLambdaForSendPendingContentCaptureEvents = {
        contentCaptureManager.sendPendingContentCaptureEvents()
        isSendPendingContentCaptureEventsScheduled = false
    }

    private fun scheduleSendPendingContentCaptureEvents(): Unit {
        if (
            isAttachedToWindow &&
                contentCaptureManager.isEnabled &&
                contentCaptureManager.hasPendingEvents &&
                !isSendPendingContentCaptureEventsScheduled
        ) {
            schedule(cachedLambdaForSendPendingContentCaptureEvents)
            isSendPendingContentCaptureEventsScheduled = true
        }
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        // only run the logic when we have something pending
        if (
            measureAndLayoutDelegate.hasPendingMeasureOrLayout ||
                measureAndLayoutDelegate.hasPendingOnPositionedCallbacks
        ) {
            trace("AndroidOwner:measureAndLayout") {
                val resend = if (sendPointerUpdate) resendMotionEventOnLayout else null
                val rootNodeResized = measureAndLayoutDelegate.measureAndLayout(resend)
                if (rootNodeResized) {
                    requestLayout()
                }
                measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
                dispatchPendingInteropLayoutCallbacks()
            }
        }
        scheduleSendPendingContentCaptureEvents()
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        trace("AndroidOwner:measureAndLayout") {
            measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
            // only dispatch the callbacks if we don't have other nodes to process as otherwise
            // we will have one more measureAndLayout() pass anyway in the same frame.
            // it allows us to not traverse the hierarchy twice.
            if (!measureAndLayoutDelegate.hasPendingMeasureOrLayout) {
                measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
                dispatchPendingInteropLayoutCallbacks()
            }
            @OptIn(ExperimentalComposeUiApi::class)
            if (ComposeUiFlags.isRectTrackingEnabled) {
                rectManager.dispatchCallbacks()
            }
            scheduleSendPendingContentCaptureEvents()
        }
    }

    private fun dispatchPendingInteropLayoutCallbacks() {
        if (isPendingInteropViewLayoutChangeDispatch) {
            viewTreeObserver.dispatchOnGlobalLayout()
            isPendingInteropViewLayoutChangeDispatch = false
        }
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {
        measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode, affectsLookahead)
    }

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean,
        scheduleMeasureAndLayout: Boolean,
    ) {
        if (affectsLookahead) {
            if (
                measureAndLayoutDelegate.requestLookaheadRemeasure(layoutNode, forceRequest) &&
                    scheduleMeasureAndLayout
            ) {
                scheduleMeasureAndLayout(layoutNode)
            }
        } else if (
            measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
                scheduleMeasureAndLayout
        ) {
            scheduleMeasureAndLayout(layoutNode)
        }
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean,
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                scheduleMeasureAndLayout()
            }
        } else {
            if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
                scheduleMeasureAndLayout()
            }
        }
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
        scheduleMeasureAndLayout()
    }

    override fun measureAndLayoutForTest() {
        measureAndLayout()
    }

    override fun setUncaughtExceptionHandler(handler: RootForTest.UncaughtExceptionHandler?) {
        uncaughtExceptionHandler = handler
        measureAndLayoutDelegate.uncaughtExceptionHandler = handler
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        trace("AndroidOwner:onMeasure") {
            if (!isAttachedToWindow) {
                invalidateLayoutNodeMeasurement(root)
            }
            val (minWidth, maxWidth) = convertMeasureSpec(widthMeasureSpec)
            val (minHeight, maxHeight) = convertMeasureSpec(heightMeasureSpec)

            val constraints =
                Constraints.fitPrioritizingHeight(
                    minWidth = minWidth,
                    maxWidth = maxWidth,
                    minHeight = minHeight,
                    maxHeight = maxHeight,
                )
            if (onMeasureConstraints == null) {
                // first onMeasure after last onLayout
                onMeasureConstraints = constraints
                wasMeasuredWithMultipleConstraints = false
            } else if (onMeasureConstraints != constraints) {
                // we were remeasured twice with different constraints after last onLayout
                wasMeasuredWithMultipleConstraints = true
            }
            measureAndLayoutDelegate.updateRootConstraints(constraints)
            measureAndLayoutDelegate.measureOnly()
            setMeasuredDimension(root.width, root.height)

            if (_androidViewsHandler != null) {
                androidViewsHandler.measure(
                    MeasureSpec.makeMeasureSpec(root.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(root.height, MeasureSpec.EXACTLY),
                )
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun ULong.component1() = (this shr 32).toInt()

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun ULong.component2() = (this and 0xFFFFFFFFUL).toInt()

    private fun pack(a: Int, b: Int) = (a.toULong() shl 32 or b.toULong())

    private fun convertMeasureSpec(measureSpec: Int): ULong {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> pack(size, size)
            MeasureSpec.UNSPECIFIED -> pack(0, Constraints.Infinity)
            MeasureSpec.AT_MOST -> pack(0, size)
            else -> throw IllegalStateException()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        lastMatrixRecalculationAnimationTime = 0L // reset it so that we're sure to have a new value
        measureAndLayoutDelegate.measureAndLayout(resendMotionEventOnLayout)
        onMeasureConstraints = null
        // we postpone onPositioned callbacks until onLayout as LayoutCoordinates
        // are currently wrong if you try to get the global(activity) coordinates -
        // View is not yet laid out.
        updatePositionCacheAndDispatch()
        if (_androidViewsHandler != null) {
            // Even if we laid out during onMeasure, we want to set the bounds of the
            // AndroidViewsHandler for accessibility and for Views making assumptions based on
            // the size of their ancestors. Usually the Views in the hierarchy will not
            // be relaid out, as they have not requested layout in the meantime.
            // However, there is also chance for the AndroidViewsHandler and the children to be
            // isLayoutRequested at this point, in case the Views hierarchy receives forceLayout().
            // In case of a forceLayout(), calling layout here will traverse the entire subtree
            // and replace the Views at the same position, which is needed to clean up their
            // layout state, which otherwise might cause further requestLayout()s to be blocked.
            androidViewsHandler.layout(0, 0, r - l, b - t)
        }
    }

    private var _rootView: View? = null

    private fun updatePositionCacheAndDispatch() {
        var positionChanged = false
        getLocationOnScreen(tmpPositionArray)
        val (globalX, globalY) = globalPosition
        if (
            globalX != tmpPositionArray[0] ||
                globalY != tmpPositionArray[1] ||
                // -1 means it has never been set, 0 means it has been "reset". We only want to
                // catch the "never been set" case
                lastMatrixRecalculationAnimationTime < 0L
        ) {
            globalPosition = IntOffset(tmpPositionArray[0], tmpPositionArray[1])
            if (globalX != Int.MAX_VALUE && globalY != Int.MAX_VALUE) {
                positionChanged = true
                root.layoutDelegate.measurePassDelegate.notifyChildrenUsingCoordinatesWhilePlacing()
            }
        }
        recalculateWindowPosition()

        val rootView: View =
            _rootView
                ?: rootView.let {
                    _rootView = it
                    it
                }

        rectManager.updateOffsets(
            globalPosition,
            windowPosition.round(),
            viewToWindowMatrix,
            rootView.width,
            rootView.height,
        )
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = positionChanged)
        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isRectTrackingEnabled) {
            rectManager.dispatchCallbacks()
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {}

    override fun createLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?,
    ): OwnedLayer {
        if (explicitLayer != null) {
            return GraphicsLayerOwnerLayer(
                graphicsLayer = explicitLayer,
                context = null,
                ownerView = this,
                drawBlock = drawBlock,
                invalidateParentLayer = invalidateParentLayer,
            )
        }
        // First try the layer cache
        val layer = layerCache.pop()
        if (layer !== null) {
            layer.reuseLayer(drawBlock, invalidateParentLayer)
            return layer
        }

        // Prior to M ViewLayer implementation might be doing extra drawing in order
        // to support the software rendering. This extra drawing is breaking some of tests
        // and we can't fully migrate to it until we figure out how to solve it.
        if (SDK_INT >= M) {
            return GraphicsLayerOwnerLayer(
                graphicsLayer = graphicsContext.createGraphicsLayer(),
                context = graphicsContext,
                ownerView = this,
                drawBlock = drawBlock,
                invalidateParentLayer = invalidateParentLayer,
            )
        }

        // RenderNode is supported on Q+ for certain, but may also be supported on M-O.
        // We can't be confident that RenderNode is supported, so we try and fail over to
        // the ViewLayer implementation. We'll try even on on P devices, but it will fail
        // until ART allows things on the unsupported list on P.
        if (isHardwareAccelerated && SDK_INT >= M && isRenderNodeCompatible) {
            try {
                return RenderNodeLayer(this, drawBlock, invalidateParentLayer)
            } catch (_: Throwable) {
                isRenderNodeCompatible = false
            }
        }
        if (viewLayersContainer == null) {
            if (!ViewLayer.hasRetrievedMethod) {
                // Test to see if updateDisplayList() can be called. If this fails then
                // ViewLayer.shouldUseDispatchDraw will be true.
                ViewLayer.updateDisplayList(View(context))
            }
            viewLayersContainer =
                if (ViewLayer.shouldUseDispatchDraw) {
                    DrawChildContainer(context)
                } else {
                    ViewLayerContainer(context)
                }
            addView(viewLayersContainer)
        }
        return ViewLayer(this, viewLayersContainer!!, drawBlock, invalidateParentLayer)
    }

    /**
     * Return [layer] to the layer cache. It can be reused in [createLayer] after this. Returns
     * `true` if it was recycled or `false` if it will be discarded.
     */
    internal fun recycle(layer: OwnedLayer): Boolean {
        val cacheValue =
            viewLayersContainer == null ||
                ViewLayer.shouldUseDispatchDraw ||
                SDK_INT >= M // L throws during RenderThread when reusing the Views.
        if (cacheValue) {
            layerCache.push(layer)
        }
        dirtyLayers -= layer
        return cacheValue
    }

    override fun onSemanticsChange() {
        composeAccessibilityDelegate.onSemanticsChange()
        contentCaptureManager.onSemanticsChange()
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        composeAccessibilityDelegate.onLayoutChange(layoutNode)
        contentCaptureManager.onLayoutChange()
    }

    override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {
        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isRectTrackingEnabled) {
            rectManager.remove(layoutNode)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.onLayoutNodeDeactivated(layoutNode)
        }
    }

    override fun onPreLayoutNodeReused(layoutNode: LayoutNode, oldSemanticsId: Int) {
        // Keep the mapping up to date when the semanticsId changes
        layoutNodes.remove(oldSemanticsId)
        layoutNodes[layoutNode.semanticsId] = layoutNode
    }

    override fun onPostLayoutNodeReused(layoutNode: LayoutNode, oldSemanticsId: Int) {
        @OptIn(ExperimentalComposeUiApi::class)
        if (autofillSupported() && ComposeUiFlags.isSemanticAutofillEnabled) {
            _autofillManager?.onPostLayoutNodeReused(layoutNode, oldSemanticsId)
        }
        // Sometimes, while scrolling with reuse, a child LayoutNode, might not
        // require measure or layout at all, but at a minimum we need to update RectManager with
        // the correct information.
        rectManager.onLayoutPositionChanged(layoutNode, true)
    }

    override fun onInteropViewLayoutChange(view: InteropView) {
        isPendingInteropViewLayoutChangeDispatch = true
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
        scheduleMeasureAndLayout()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        if (!isAttachedToWindow) {
            invalidateLayers(root)
        }
        measureAndLayout()
        Snapshot.notifyObjectsInitialized()

        isDrawingContent = true
        // we don't have to observe here because the root has a layer modifier
        // that will observe all children. The AndroidComposeView has only the
        // root, so it doesn't have to invalidate itself based on model changes.
        try {
            canvasHolder.drawInto(canvas) {
                root.draw(
                    canvas = this,
                    graphicsLayer = null, // the root node will provide the root graphics layer
                )
            }

            if (dirtyLayers.isNotEmpty()) {
                for (i in 0 until dirtyLayers.size) {
                    val layer = dirtyLayers[i]
                    layer.updateDisplayList()
                }
            }

            if (ViewLayer.shouldUseDispatchDraw) {
                // We must update the display list of all children using dispatchDraw()
                // instead of updateDisplayList(). But since we don't want to actually draw
                // the contents, we will clip out everything from the canvas.
                val saveCount = canvas.save()
                canvas.clipRect(0f, 0f, 0f, 0f)

                super.dispatchDraw(canvas)
                canvas.restoreToCount(saveCount)
            }

            dirtyLayers.clear()
            isDrawingContent = false
        } catch (t: Throwable) {
            uncaughtExceptionHandler?.onUncaughtException(t) ?: throw t
        }

        // updateDisplayList operations performed above (during root.draw and during the explicit
        // layer.updateDisplayList() calls) can result in the same layers being invalidated. These
        // layers have been added to postponedDirtyLayers and will be redrawn during the next
        // dispatchDraw.
        if (postponedDirtyLayers != null) {
            val postponed = postponedDirtyLayers!!
            dirtyLayers.addAll(postponed)
            postponed.clear()
        }

        // Used to handle frame rate information
        if (isArrEnabled) {
            Api35Impl.setRequestedFrameRate(this, currentFrameRate)
            Api35Impl.setRequestedFrameRate(frameRateCategoryView, currentFrameRateCategory)

            if (!currentFrameRateCategory.isNaN()) {
                frameRateCategoryView.invalidate()
                drawChild(canvas, frameRateCategoryView, drawingTime)
            }

            currentFrameRate = Float.NaN
            currentFrameRateCategory = Float.NaN
        }
        if (ComposeUiFlags.isRectTrackingEnabled) {
            rectManager.dispatchCallbacks()
        }
    }

    internal fun notifyLayerIsDirty(layer: OwnedLayer, isDirty: Boolean) {
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
                    ?: mutableObjectListOf<OwnedLayer>().also { postponedDirtyLayers = it }
            postponed += layer
        }
    }

    /**
     * The callback to be executed when [viewTreeOwners] is created and not-null anymore. Note that
     * this callback will be fired inline when it is already available
     */
    fun setOnViewTreeOwnersAvailable(callback: (ViewTreeOwners) -> Unit) {
        val viewTreeOwners = viewTreeOwners
        if (viewTreeOwners != null) {
            callback(viewTreeOwners)
        }
        if (!isAttachedToWindow) {
            onViewTreeOwnersAvailable = callback
        }
    }

    // TODO(mnuzen): combine both event loops into one larger one
    suspend fun boundsUpdatesContentCaptureEventLoop() {
        contentCaptureManager.boundsUpdatesEventLoop()
    }

    suspend fun boundsUpdatesAccessibilityEventLoop() {
        composeAccessibilityDelegate.boundsUpdatesEventLoop()
    }

    /** Walks the entire LayoutNode sub-hierarchy and marks all nodes as needing measurement. */
    private fun invalidateLayoutNodeMeasurement(node: LayoutNode) {
        measureAndLayoutDelegate.requestRemeasure(node)
        node.forEachChild { invalidateLayoutNodeMeasurement(it) }
    }

    /** Walks the entire LayoutNode sub-hierarchy and marks all layers as needing to be redrawn. */
    private fun invalidateLayers(node: LayoutNode) {
        node.invalidateLayers()
        node.forEachChild { invalidateLayers(it) }
    }

    override fun invalidateDescendants() {
        invalidateLayers(root)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (SDK_INT < 30) {
            showLayoutBounds = getIsShowingLayoutBounds()
        }
        if (ComposeUiFlags.areWindowInsetsRulersEnabled) {
            insetsListener.onViewAttachedToWindow(this)
        }
        addNotificationForSysPropsChange(this)
        _windowInfo.isWindowFocused = hasWindowFocus()
        _windowInfo.setOnInitializeContainerSize { calculateWindowSize(this) }
        updateWindowMetrics()
        invalidateLayoutNodeMeasurement(root)
        invalidateLayers(root)
        snapshotObserver.startObserving()
        ifDebug {
            if (autofillSupported()) {
                // TODO(b/333102566): Use _semanticAutofill after switching to the newer Autofill
                // system.
                _autofill?.let { AutofillCallback.register(it) }
            }
        }

        val lifecycleOwner = findViewTreeLifecycleOwner()
        val savedStateRegistryOwner = findViewTreeSavedStateRegistryOwner()
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()

        retainScope =
            installLocalRetainScope(lifecycleOwner, viewModelStoreOwner) ?: ForgetfulRetainScope

        val oldViewTreeOwners = viewTreeOwners
        // We need to change the ViewTreeOwner if there isn't one yet (null)
        // or if either the lifecycleOwner, savedStateRegistryOwner, viewModelStoreOwner has
        // changed.
        val resetViewTreeOwner =
            oldViewTreeOwners == null ||
                ((lifecycleOwner != null && savedStateRegistryOwner != null) &&
                    (lifecycleOwner !== oldViewTreeOwners.lifecycleOwner ||
                        savedStateRegistryOwner !== oldViewTreeOwners.savedStateRegistryOwner ||
                        viewModelStoreOwner !== oldViewTreeOwners.viewModelStoreOwner))
        if (resetViewTreeOwner) {
            if (lifecycleOwner == null) {
                throw IllegalStateException(
                    "Composed into the View which doesn't propagate ViewTreeLifecycleOwner!"
                )
            }
            if (savedStateRegistryOwner == null) {
                throw IllegalStateException(
                    "Composed into the View which doesn't propagate" +
                        "ViewTreeSavedStateRegistryOwner!"
                )
            }
            oldViewTreeOwners?.lifecycleOwner?.lifecycle?.removeObserver(this)
            lifecycleOwner.lifecycle.addObserver(this)
            val viewTreeOwners =
                ViewTreeOwners(
                    lifecycleOwner = lifecycleOwner,
                    savedStateRegistryOwner = savedStateRegistryOwner,
                    viewModelStoreOwner = viewModelStoreOwner,
                )
            _viewTreeOwners = viewTreeOwners
            onViewTreeOwnersAvailable?.invoke(viewTreeOwners)
            onViewTreeOwnersAvailable = null
        }

        _inputModeManager.inputMode = if (isInTouchMode) Touch else Keyboard

        val lifecycle =
            checkPreconditionNotNull(viewTreeOwners?.lifecycleOwner?.lifecycle) {
                "No lifecycle owner exists"
            }
        lifecycle.addObserver(this)
        lifecycle.addObserver(contentCaptureManager)
        viewTreeObserver.addOnGlobalLayoutListener(this)
        viewTreeObserver.addOnScrollChangedListener(this)
        viewTreeObserver.addOnTouchModeChangeListener(this)

        if (SDK_INT >= S) AndroidComposeViewTranslationCallbackS.setViewTranslationCallback(this)
        _autofillManager?.let {
            focusOwner.listeners += it
            semanticsOwner.listeners += it
        }

        if (ComposeUiFlags.isContentCaptureOptimizationEnabled) {
            contentCaptureManager.let { semanticsOwner.listeners += it }
        }
    }

    private fun installLocalRetainScope(
        lifecycleOwner: LifecycleOwner?,
        viewModelStoreOwner: ViewModelStoreOwner?,
    ): RetainScope? {
        val frameEndScheduler = frameEndScheduler
        if (lifecycleOwner == null || viewModelStoreOwner == null || frameEndScheduler == null)
            return null

        val retainScopeOwner =
            ViewModelProvider.create(
                    store = viewModelStoreOwner.viewModelStore,
                    factory = ViewModelProvider.NewInstanceFactory(),
                )
                .get<LifecycleRetainScopeOwner>()

        // If we have a unique View Id, its on our parent ComposeView, not the AndroidComposeView
        // implementation child view.
        val viewId = (parent as View).id
        val retainScopeEntry = retainScopeOwner.getOrCreateRetainScopeEntry(viewId)
        lifecycleRetainScopeOwnerEntry = retainScopeEntry
        return retainScopeEntry.retainScope
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (ComposeUiFlags.areWindowInsetsRulersEnabled) {
            insetsListener.onViewDetachedFromWindow(this)
        }
        if (isArrEnabled) {
            removeView(frameRateCategoryView)
        }

        removeNotificationForSysPropsChange(this)
        snapshotObserver.stopObserving()
        _windowInfo.setOnInitializeContainerSize(null)
        val lifecycle =
            checkPreconditionNotNull(viewTreeOwners?.lifecycleOwner?.lifecycle) {
                "No lifecycle owner exists"
            }
        lifecycle.removeObserver(contentCaptureManager)
        lifecycle.removeObserver(this)
        ifDebug {
            if (autofillSupported()) {
                // TODO(b/333102566): Use _semanticAutofill after switching to the newer Autofill
                // system.
                _autofill?.let { AutofillCallback.unregister(it) }
            }
        }
        viewTreeObserver.removeOnGlobalLayoutListener(this)
        viewTreeObserver.removeOnScrollChangedListener(this)
        viewTreeObserver.removeOnTouchModeChangeListener(this)

        lifecycleRetainScopeOwnerEntry?.release()
        lifecycleRetainScopeOwnerEntry = null

        if (SDK_INT >= S) AndroidComposeViewTranslationCallbackS.clearViewTranslationCallback(this)
        _autofillManager?.let {
            semanticsOwner.listeners -= it
            focusOwner.listeners -= it
        }
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure?, flags: Int) {
        if (autofillSupported() && structure != null) {
            if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isSemanticAutofillEnabled) {
                _autofillManager?.populateViewStructure(structure)
            }
            _autofill?.populateViewStructure(structure)
        }
    }

    override fun autofill(values: SparseArray<AutofillValue>) {
        if (autofillSupported()) {
            if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isSemanticAutofillEnabled) {
                _autofillManager?.performAutofill(values)
            }
            _autofill?.performAutofill(values)
        }
    }

    @RequiresApi(S)
    override fun onCreateVirtualViewTranslationRequests(
        virtualIds: LongArray,
        supportedFormats: IntArray,
        requestsCollector: Consumer<ViewTranslationRequest?>,
    ) {
        contentCaptureManager.onCreateVirtualViewTranslationRequests(
            virtualIds,
            supportedFormats,
            requestsCollector,
        )
    }

    @RequiresApi(S)
    override fun onVirtualViewTranslationResponses(
        response: LongSparseArray<ViewTranslationResponse?>
    ) {
        contentCaptureManager.onVirtualViewTranslationResponses(contentCaptureManager, response)
    }

    override fun dispatchGenericMotionEvent(motionEvent: MotionEvent): Boolean {
        if (hoverExitReceived) {
            removeCallbacks(sendHoverExitEvent)
            // Ignore ACTION_HOVER_EXIT if it is directly followed by an ACTION_SCROLL.
            // Note: In some versions of Android Studio with screen mirroring, studio will
            // incorrectly add an ACTION_HOVER_EXIT during a scroll event which causes
            // issues (b/314269723), so we ignore the exit in that case.
            if (motionEvent.actionMasked == ACTION_SCROLL) {
                hoverExitReceived = false
            } else {
                sendHoverExitEvent.run()
            }
        }
        if (isBadMotionEvent(motionEvent) || !isAttachedToWindow) {
            return super.dispatchGenericMotionEvent(motionEvent)
        }

        return when (motionEvent.actionMasked) {
            ACTION_SCROLL ->
                if (motionEvent.isFromSource(SOURCE_ROTARY_ENCODER)) {
                    handleRotaryEvent(motionEvent)
                } else {
                    handleMotionEvent(motionEvent).dispatchedToAPointerInputModifier
                }
            else -> {
                @OptIn(ExperimentalIndirectTouchTypeApi::class)
                if (motionEvent.isFromSource(SOURCE_TOUCH_NAVIGATION)) {
                    val primaryDirectionalMotionAxis =
                        primaryDirectionalMotionAxisOverride
                            ?: indirectPrimaryDirectionalScrollAxis(motionEvent)
                    val indirectTouchEvent =
                        AndroidIndirectTouchEvent(
                            position = Offset(motionEvent.x, motionEvent.y),
                            uptimeMillis = motionEvent.eventTime,
                            type = convertActionToIndirectTouchEventType(motionEvent.actionMasked),
                            primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
                            nativeEvent = motionEvent,
                        )

                    if (handleIndirectTouchEvent(indirectTouchEvent)) {
                        return true
                    }
                }

                // If focus owner did not handle, rely on ViewGroup to handle.
                super.dispatchGenericMotionEvent(motionEvent)
            }
        }
    }

    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    private fun handleIndirectTouchEvent(indirectTouchEvent: IndirectTouchEvent): Boolean {
        val motionEvent = indirectTouchEvent.nativeEvent

        val handled =
            focusOwner.dispatchIndirectTouchEvent(indirectTouchEvent) {
                super.dispatchGenericMotionEvent(motionEvent)
            }

        if (handled) {
            // Turns off all navigation gestures for this event stream since an app is
            // handling the event stream and also resets the preferred Axis.
            indirectTouchNavigationGestureDetectorActiveForEventStream = false
            indirectTouchNavigationGestureDetector.primaryDirectionalMotionAxis =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            return true
        } else {
            @OptIn(ExperimentalComposeUiApi::class)
            if (isIndirectTouchNavigationGestureDetectorEnabled) { // Flag for feature
                if (motionEvent.action == ACTION_DOWN) {
                    // Starts tracking only with ACTION_DOWN (start of event stream).
                    indirectTouchNavigationGestureDetectorActiveForEventStream = true
                    indirectTouchNavigationGestureDetector.primaryDirectionalMotionAxis =
                        indirectTouchEvent.primaryDirectionalMotionAxis
                }

                if (indirectTouchNavigationGestureDetectorActiveForEventStream) {
                    indirectTouchNavigationGestureDetector.onTouchEvent(motionEvent)
                }
                // If the isIndirectTouchNavigationGestureDetectorEnabled flag is
                // enabled, it means that we don't want to pass the event up to the
                // platform's handler for SOURCE_TOUCH_NAVIGATION, so we return true.
                return true
            }
        }
        return false
    }

    // TODO(shepshapard): Test this method.
    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (hoverExitReceived) {
            // Go ahead and send ACTION_HOVER_EXIT if this isn't an ACTION_DOWN for the same
            // pointer
            removeCallbacks(sendHoverExitEvent)
            val lastEvent = previousMotionEvent!!
            if (
                motionEvent.actionMasked != ACTION_DOWN || hasChangedDevices(motionEvent, lastEvent)
            ) {
                sendHoverExitEvent.run()
            } else {
                hoverExitReceived = false
            }
        }
        if (isBadMotionEvent(motionEvent) || !isAttachedToWindow) {
            return false // Bad MotionEvent. Don't handle it.
        }

        if (motionEvent.actionMasked == ACTION_MOVE && !isPositionChanged(motionEvent)) {
            // There was no movement from previous MotionEvent, so we don't need to dispatch this.
            // This could be a scroll event or some other non-touch event that results in an
            // ACTION_MOVE without any movement.
            return false
        }

        val processResult = handleMotionEvent(motionEvent)

        if (processResult.anyMovementConsumed) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        // Implement tap-to-clear focus _after_ handling this motion event
        // This allows focus to cleanly move from A to B if this motion event causes B to take
        // focus, instead of clearing focus entirely first, and then requesting focus on B after
        // this doesn't consume the motion event
        val isDown =
            motionEvent.actionMasked == ACTION_DOWN ||
                motionEvent.actionMasked == ACTION_POINTER_DOWN
        val isFromMouseOrTouchpad =
            motionEvent.isFromSource(InputDevice.SOURCE_MOUSE) ||
                motionEvent.isFromSource(InputDevice.SOURCE_TOUCHPAD)
        if (isDown && isFromMouseOrTouchpad) {
            if ((parent as? AbstractComposeView)?.isClearFocusOnPointerDownEnabled == true) {
                val activeFocusTargetNode = focusOwner.activeFocusTargetNode
                if (activeFocusTargetNode != null) {
                    val focusedNodeBounds =
                        activeFocusTargetNode.requireLayoutCoordinates().boundsInRoot()
                    // Only clear focus if the motion event doesn't fall inside the bounds
                    // of the node that is currently focused
                    // Note that we don't use the current focusRect which can be different
                    // than the bounds of the currently focused node.
                    // If a focusable node is choosing to have the focusRect be different than
                    // its own bounds, we shouldn't clear focus from it if the down event
                    // occurs over that node.
                    if (!focusedNodeBounds.contains(Offset(motionEvent.x, motionEvent.y))) {
                        focusOwner.clearFocus()
                    }
                }
            }
        }

        return processResult.dispatchedToAPointerInputModifier
    }

    private fun handleRotaryEvent(event: MotionEvent): Boolean {
        val config = android.view.ViewConfiguration.get(context)
        val axisValue = -event.getAxisValue(AXIS_SCROLL)
        val rotaryEvent =
            RotaryScrollEvent(
                verticalScrollPixels = axisValue * getScaledVerticalScrollFactor(config, context),
                horizontalScrollPixels =
                    axisValue * getScaledHorizontalScrollFactor(config, context),
                uptimeMillis = event.eventTime,
                inputDeviceId = event.deviceId,
            )
        return focusOwner.dispatchRotaryEvent(rotaryEvent) {
            super.dispatchGenericMotionEvent(event)
        }
    }

    private fun handleMotionEvent(motionEvent: MotionEvent): ProcessResult {
        removeCallbacks(resendMotionEventRunnable)
        try {
            recalculateWindowPosition(motionEvent)
            forceUseMatrixCache = true
            measureAndLayout(sendPointerUpdate = false)
            val result =
                trace("AndroidOwner:onTouch") {
                    val action = motionEvent.actionMasked
                    val lastEvent = previousMotionEvent

                    val wasMouseEvent = lastEvent?.getToolType(0) == TOOL_TYPE_MOUSE
                    if (lastEvent != null && hasChangedDevices(motionEvent, lastEvent)) {
                        if (isDevicePressEvent(lastEvent)) {
                            // Send a cancel event
                            pointerInputEventProcessor.processCancel()
                        } else if (lastEvent.actionMasked != ACTION_HOVER_EXIT && wasMouseEvent) {
                            // The mouse cursor disappeared without sending an ACTION_HOVER_EXIT, so
                            // we have to send that event.
                            sendSimulatedEvent(lastEvent, ACTION_HOVER_EXIT, lastEvent.eventTime)
                        }
                    }

                    val isMouseEvent = motionEvent.getToolType(0) == TOOL_TYPE_MOUSE

                    if (
                        !wasMouseEvent &&
                            isMouseEvent &&
                            action != ACTION_CANCEL &&
                            action != ACTION_HOVER_ENTER &&
                            isInBounds(motionEvent)
                    ) {
                        // We didn't previously have an enter event and we're getting our first
                        // mouse event. Send a simulated enter event so that we have a consistent
                        // enter/exit.
                        sendSimulatedEvent(motionEvent, ACTION_HOVER_ENTER, motionEvent.eventTime)
                    }
                    lastEvent?.recycle()

                    // If the previous MotionEvent was an ACTION_HOVER_EXIT, we need to check if it
                    // was a synthetic MotionEvent generated by the platform for an ACTION_DOWN
                    // event
                    // or not.
                    //
                    // If it was synthetic, we do nothing, because we want to keep the existing
                    // cache
                    // of "Hit" Modifier.Node(s) from the previous hover events, so we can reuse
                    // them
                    // once an ACTION_UP event is triggered and we return to the same hover state
                    // (cache improves performance for this frequent event sequence with a mouse).
                    //
                    // If it was NOT synthetic, we end the event stream in MotionEventAdapter and
                    // clear
                    // the hit cache used in PointerInputEventProcessor (specifically, the
                    // HitPathTracker cache inside PointerInputEventProcessor), so events in this
                    // new
                    // stream do not trigger Modifier.Node(s) hit by the previous stream.
                    if (previousMotionEvent?.action == ACTION_HOVER_EXIT) {
                        val previousEventDefaultPointerId =
                            previousMotionEvent?.getPointerId(0) ?: -1

                        // New ACTION_HOVER_ENTER, so this should be considered a new stream
                        if (
                            motionEvent.action == ACTION_HOVER_ENTER && motionEvent.historySize == 0
                        ) {
                            if (previousEventDefaultPointerId >= 0) {
                                motionEventAdapter.endStream(previousEventDefaultPointerId)
                            }
                        } else if (
                            motionEvent.action == ACTION_DOWN && motionEvent.historySize == 0
                        ) {
                            val previousX = previousMotionEvent?.x ?: Float.NaN
                            val previousY = previousMotionEvent?.y ?: Float.NaN

                            val currentX = motionEvent.x
                            val currentY = motionEvent.y

                            val previousAndCurrentCoordinatesDoNotMatch =
                                (previousX != currentX || previousY != currentY)

                            val previousEventTime = previousMotionEvent?.eventTime ?: -1L

                            val previousAndCurrentEventTimesDoNotMatch =
                                previousEventTime != motionEvent.eventTime

                            // A synthetically created Hover Exit event will always have the same x,
                            // y, and timestamp as the down event it proceeds.
                            val previousHoverEventWasNotSyntheticallyProducedFromADownEvent =
                                previousAndCurrentCoordinatesDoNotMatch ||
                                    previousAndCurrentEventTimesDoNotMatch

                            if (previousHoverEventWasNotSyntheticallyProducedFromADownEvent) {
                                // This should be considered a new stream, and we should
                                // reset everything.
                                if (previousEventDefaultPointerId >= 0) {
                                    motionEventAdapter.endStream(previousEventDefaultPointerId)
                                }
                                pointerInputEventProcessor.clearPreviouslyHitModifierNodes()
                            }
                        }
                    }

                    previousMotionEvent = MotionEvent.obtainNoHistory(motionEvent)

                    sendMotionEvent(motionEvent)
                }
            return result
        } finally {
            forceUseMatrixCache = false
        }
    }

    private fun hasChangedDevices(event: MotionEvent, lastEvent: MotionEvent): Boolean {
        return lastEvent.source != event.source || lastEvent.getToolType(0) != event.getToolType(0)
    }

    private fun isDevicePressEvent(event: MotionEvent): Boolean {
        if (event.buttonState != 0) {
            return true
        }
        return when (event.actionMasked) {
            ACTION_POINTER_UP, // means that there is at least one remaining pointer
            ACTION_DOWN,
            ACTION_MOVE -> true
            //            ACTION_SCROLL, // We've already checked for buttonState, so it must not be
            // down
            //            ACTION_HOVER_ENTER,
            //            ACTION_HOVER_MOVE,
            //            ACTION_HOVER_EXIT,
            //            ACTION_UP,
            //            ACTION_CANCEL,
            else -> false
        }
    }

    @OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
    private fun sendMotionEvent(motionEvent: MotionEvent): ProcessResult {
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            _windowInfo.keyboardModifiers = PointerKeyboardModifiers(motionEvent.metaState)
        }
        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent, this)
        val action = motionEvent.actionMasked
        return if (pointerInputEvent != null) {
            // Cache the last position of the last pointer to go down so we can check if
            // it's in a scrollable region in canScroll{Vertically|Horizontally}. Those
            // methods use semantics data, and because semantics coordinates are local to
            // this view, the pointer _position_, not _positionOnScreen_, is the offset that
            // needs to be cached.
            pointerInputEvent.pointers
                .fastLastOrNull {
                    it.down &&
                        (action == ACTION_DOWN ||
                            action == ACTION_POINTER_DOWN ||
                            !isCanScrollUsingLastDownEventFixEnabled)
                }
                ?.position
                ?.let { lastDownPointerPosition = it }

            val result =
                pointerInputEventProcessor.process(pointerInputEvent, this, isInBounds(motionEvent))
            // Clear the MotionEvent reference after dispatching it.
            pointerInputEvent.motionEvent = null

            if (
                (action == ACTION_DOWN || action == ACTION_POINTER_DOWN) &&
                    !result.dispatchedToAPointerInputModifier
            ) {
                // We aren't handling the pointer, so the event stream has ended for us.
                // The next time we receive a pointer event, it should be considered a new
                // pointer.
                motionEventAdapter.endStream(motionEvent.getPointerId(motionEvent.actionIndex))
            }
            result
        } else {
            pointerInputEventProcessor.processCancel()
            ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false,
                anyChangeConsumed = false,
            )
        }
    }

    @OptIn(InternalCoreApi::class)
    private fun sendSimulatedEvent(
        motionEvent: MotionEvent,
        action: Int,
        eventTime: Long,
        forceHover: Boolean = true,
    ) {
        // don't send any events for pointers that are "up" unless they support hover
        val upIndex =
            when (motionEvent.actionMasked) {
                ACTION_UP ->
                    if (action == ACTION_HOVER_ENTER || action == ACTION_HOVER_EXIT) -1 else 0
                ACTION_POINTER_UP -> motionEvent.actionIndex
                else -> -1
            }
        val pointerCount = motionEvent.pointerCount - if (upIndex >= 0) 1 else 0
        if (pointerCount == 0) {
            return
        }
        val pointerProperties = Array(pointerCount) { MotionEvent.PointerProperties() }
        val pointerCoords = Array(pointerCount) { MotionEvent.PointerCoords() }
        for (i in 0 until pointerCount) {
            val sourceIndex = i + if (upIndex < 0 || i < upIndex) 0 else 1
            motionEvent.getPointerProperties(sourceIndex, pointerProperties[i])
            val coords = pointerCoords[i]
            motionEvent.getPointerCoords(sourceIndex, coords)
            val localPosition = Offset(coords.x, coords.y)
            val screenPosition = localToScreen(localPosition)
            coords.x = screenPosition.x
            coords.y = screenPosition.y
        }
        val buttonState = if (forceHover) 0 else motionEvent.buttonState

        val downTime =
            if (motionEvent.downTime == motionEvent.eventTime) {
                eventTime
            } else {
                motionEvent.downTime
            }
        val event =
            MotionEvent.obtain(
                /* downTime */ downTime,
                /* eventTime */ eventTime,
                /* action */ action,
                /* pointerCount */ pointerCount,
                /* pointerProperties */ pointerProperties,
                /* pointerCoords */ pointerCoords,
                /* metaState */ motionEvent.metaState,
                /* buttonState */ buttonState,
                /* xPrecision */ motionEvent.xPrecision,
                /* yPrecision */ motionEvent.yPrecision,
                /* deviceId */ motionEvent.deviceId,
                /* edgeFlags */ motionEvent.edgeFlags,
                /* source */ motionEvent.source,
                /* flags */ motionEvent.flags,
            )
        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(event, this)!!

        pointerInputEventProcessor.process(pointerInputEvent, this, true)
        event.recycle()
    }

    /**
     * This method is required to correctly support swipe-to-dismiss layouts on WearOS, which search
     * their children for scrollable views to determine whether or not to intercept touch events – a
     * sort of simplified nested scrolling mechanism.
     *
     * Because a composition may contain many scrollable and non-scrollable areas, and this method
     * doesn't know which part of the view the caller cares about, it uses the
     * [lastDownPointerPosition] as the location to check.
     */
    override fun canScrollHorizontally(direction: Int): Boolean =
        composeAccessibilityDelegate.canScroll(vertical = false, direction, lastDownPointerPosition)

    /** See [canScrollHorizontally]. */
    override fun canScrollVertically(direction: Int): Boolean =
        composeAccessibilityDelegate.canScroll(vertical = true, direction, lastDownPointerPosition)

    private fun isInBounds(motionEvent: MotionEvent): Boolean {
        val x = motionEvent.x
        val y = motionEvent.y
        return (x in 0f..width.toFloat() && y in 0f..height.toFloat())
    }

    override fun localToScreen(localPosition: Offset): Offset {
        recalculateWindowPosition()
        val local = viewToWindowMatrix.map(localPosition)
        return Offset(local.x + windowPosition.x, local.y + windowPosition.y)
    }

    override fun localToScreen(localTransform: Matrix) {
        recalculateWindowPosition()
        localTransform.timesAssign(viewToWindowMatrix)
        localTransform.preTranslate(windowPosition.x, windowPosition.y, tmpMatrix)
    }

    override fun screenToLocal(positionOnScreen: Offset): Offset {
        recalculateWindowPosition()
        val x = positionOnScreen.x - windowPosition.x
        val y = positionOnScreen.y - windowPosition.y
        return windowToViewMatrix.map(Offset(x, y))
    }

    private fun recalculateWindowPosition() {
        if (!forceUseMatrixCache) {
            val animationTime = AnimationUtils.currentAnimationTimeMillis()
            if (animationTime != lastMatrixRecalculationAnimationTime) {
                lastMatrixRecalculationAnimationTime = animationTime
                recalculateWindowViewTransforms()
                var viewParent = parent
                var view: View = this
                while (viewParent is ViewGroup) {
                    view = viewParent
                    viewParent = view.parent
                }
                view.getLocationOnScreen(tmpPositionArray)
                val screenX = tmpPositionArray[0].toFloat()
                val screenY = tmpPositionArray[1].toFloat()
                view.getLocationInWindow(tmpPositionArray)
                val windowX = tmpPositionArray[0].toFloat()
                val windowY = tmpPositionArray[1].toFloat()
                windowPosition = Offset(screenX - windowX, screenY - windowY)
            }
        }
    }

    /**
     * Recalculates the window position based on the [motionEvent]'s coordinates and screen
     * coordinates. Some devices give false positions for [getLocationOnScreen] in some unusual
     * circumstances, so a different mechanism must be used to determine the actual position.
     */
    private fun recalculateWindowPosition(motionEvent: MotionEvent) {
        lastMatrixRecalculationAnimationTime = AnimationUtils.currentAnimationTimeMillis()
        recalculateWindowViewTransforms()
        val positionInWindow = viewToWindowMatrix.map(Offset(motionEvent.x, motionEvent.y))

        windowPosition =
            Offset(motionEvent.rawX - positionInWindow.x, motionEvent.rawY - positionInWindow.y)
    }

    private fun recalculateWindowViewTransforms() {
        matrixToWindow.calculateMatrixToWindow(this, viewToWindowMatrix)
        viewToWindowMatrix.invertTo(windowToViewMatrix)
    }

    private fun updateWindowMetrics() {
        _windowInfo.updateContainerSizeIfObserved { calculateWindowSize(this) }
    }

    override fun onCheckIsTextEditor(): Boolean {
        val parentSession =
            textInputSessionMutex.currentSession
                ?: return legacyTextInputServiceAndroid.isEditorFocused()
        // Don't bring this up before the ?: – establishTextInputSession has been called, but
        // startInputMethod has not, we're not a text editor until the session is cancelled or
        // startInputMethod is called.
        return parentSession.isReadyForConnection
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val parentSession =
            textInputSessionMutex.currentSession
                ?: return legacyTextInputServiceAndroid.createInputConnection(outAttrs)
        // Don't bring this up before the ?: - if this returns null, we SHOULD NOT fall back to
        // the legacy input system.
        return parentSession.createInputConnection(outAttrs)
    }

    override fun calculateLocalPosition(positionInWindow: Offset): Offset {
        recalculateWindowPosition()
        return windowToViewMatrix.map(positionInWindow)
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset {
        recalculateWindowPosition()
        return viewToWindowMatrix.map(localPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        updateWindowMetrics()
        if (newConfig.fontWeightAdjustmentCompat != currentFontWeightAdjustment) {
            currentFontWeightAdjustment = newConfig.fontWeightAdjustmentCompat
            fontFamilyResolver = createFontFamilyResolver(context)
        }
        configurationChangeObserver(newConfig)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // This method can be called while View's constructor is running
        // by way of resolving padding in response to initScrollbars.
        // If we get such a call, don't try to write to a property delegate
        // that hasn't been initialized yet.
        if (superclassInitComplete) {
            this.layoutDirection = toLayoutDirection(layoutDirection) ?: LayoutDirection.Ltr
        }
    }

    private fun autofillSupported() = SDK_INT >= O

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (hoverExitReceived) {
            // Go ahead and send it now
            removeCallbacks(sendHoverExitEvent)
            sendHoverExitEvent.run()
        }
        if (isBadMotionEvent(event) || !isAttachedToWindow) {
            return false // Bad MotionEvent. Don't handle it.
        }

        // Always call accessibilityDelegate dispatchHoverEvent (since accessibilityDelegate's
        // dispatchHoverEvent only runs if touch exploration is enabled)
        composeAccessibilityDelegate.dispatchHoverEvent(event)

        when (event.actionMasked) {
            ACTION_HOVER_EXIT -> {
                if (isInBounds(event)) {
                    if (event.getToolType(0) == TOOL_TYPE_MOUSE && event.buttonState != 0) {
                        // We know that this is caused by a mouse button press, so we can ignore it
                        return false
                    }

                    // This may be caused by a press (e.g. stylus pressed on the screen), but
                    // we can't be sure until the ACTION_DOWN is received. Let's delay this
                    // message and see if the ACTION_DOWN comes.
                    previousMotionEvent?.recycle()
                    previousMotionEvent = MotionEvent.obtainNoHistory(event)
                    hoverExitReceived = true
                    // There are cases where the hover exit will incorrectly trigger because this
                    // post is called right before the end of the frame and the new frame checks for
                    // a press/down event (which hasn't occurred yet). Therefore, we delay the post
                    // call a small amount to account for that.
                    postDelayed(sendHoverExitEvent, ONE_FRAME_120_HERTZ_IN_MILLISECONDS)
                    return false
                }
            }
            ACTION_HOVER_MOVE ->
                // Check if we're receiving this when we've already handled it elsewhere
                if (!isPositionChanged(event)) {
                    return false
                }
        }
        val result = handleMotionEvent(event)
        return result.dispatchedToAPointerInputModifier
    }

    private fun isBadMotionEvent(event: MotionEvent): Boolean {
        var eventInvalid =
            !event.x.fastIsFinite() ||
                !event.y.fastIsFinite() ||
                !event.rawX.fastIsFinite() ||
                !event.rawY.fastIsFinite()

        if (!eventInvalid) {
            // First event x,y is checked above if block, so we can skip index 0.
            for (index in 1 until event.pointerCount) {
                eventInvalid =
                    !event.getX(index).fastIsFinite() ||
                        !event.getY(index).fastIsFinite() ||
                        (SDK_INT >= Q && !isValidMotionEvent(event, index))

                if (eventInvalid) break
            }
        }

        return eventInvalid
    }

    private fun isPositionChanged(event: MotionEvent): Boolean {
        if (event.pointerCount != 1) {
            return true
        }
        val lastEvent = previousMotionEvent
        return lastEvent == null ||
            lastEvent.pointerCount != event.pointerCount ||
            event.rawX != lastEvent.rawX ||
            event.rawY != lastEvent.rawY
    }

    private fun findViewByAccessibilityIdRootedAtCurrentView(
        accessibilityId: Int,
        currentView: View,
    ): View? {
        if (SDK_INT < Q) {
            val getAccessibilityViewIdMethod =
                Class.forName("android.view.View").getDeclaredMethod("getAccessibilityViewId")
            getAccessibilityViewIdMethod.isAccessible = true
            if (getAccessibilityViewIdMethod.invoke(currentView) == accessibilityId) {
                return currentView
            }
            if (currentView is ViewGroup) {
                for (i in 0 until currentView.childCount) {
                    val foundView =
                        findViewByAccessibilityIdRootedAtCurrentView(
                            accessibilityId,
                            currentView.getChildAt(i),
                        )
                    if (foundView != null) {
                        return foundView
                    }
                }
            }
        }
        return null
    }

    @RequiresApi(N)
    override fun onResolvePointerIcon(
        event: MotionEvent,
        pointerIndex: Int,
    ): android.view.PointerIcon {
        val toolType = event.getToolType(pointerIndex)
        if (
            !event.isFromSource(InputDevice.SOURCE_MOUSE) &&
                event.isFromSource(InputDevice.SOURCE_STYLUS) &&
                (toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER)
        ) {
            val icon = pointerIconService.getStylusHoverIcon()
            if (icon != null) {
                return AndroidComposeViewVerificationHelperMethodsN.toAndroidPointerIcon(
                    context,
                    icon,
                )
            }
        }
        return super.onResolvePointerIcon(event, pointerIndex)
    }

    override val pointerIconService: PointerIconService =
        object : PointerIconService {
            private var currentMouseCursorIcon: PointerIcon = PointerIcon.Default
            private var currentStylusHoverIcon: PointerIcon? = null

            override fun getIcon(): PointerIcon {
                return currentMouseCursorIcon
            }

            override fun setIcon(value: PointerIcon?) {
                currentMouseCursorIcon = value ?: PointerIcon.Default
                if (SDK_INT >= N) {
                    AndroidComposeViewVerificationHelperMethodsN.setPointerIcon(
                        this@AndroidComposeView,
                        currentMouseCursorIcon,
                    )
                }
            }

            override fun getStylusHoverIcon(): PointerIcon? {
                return currentStylusHoverIcon
            }

            override fun setStylusHoverIcon(value: PointerIcon?) {
                currentStylusHoverIcon = value
            }
        }

    /**
     * This overrides an @hide method in ViewGroup. Because of the @hide, the override keyword
     * cannot be used, but the override works anyway because the ViewGroup method is not final. In
     * Android P and earlier, the call path is
     * AccessibilityInteractionController#findViewByAccessibilityId ->
     * View#findViewByAccessibilityId -> ViewGroup#findViewByAccessibilityIdTraversal. In Android Q
     * and later, AccessibilityInteractionController#findViewByAccessibilityId uses
     * AccessibilityNodeIdManager and findViewByAccessibilityIdTraversal is only used by autofill.
     */
    @Suppress("BanHideTag")
    fun findViewByAccessibilityIdTraversal(accessibilityId: Int): View? {
        try {
            // AccessibilityInteractionController#findViewByAccessibilityId doesn't call this
            // method in Android Q and later. Ideally, we should only define this method in
            // Android P and earlier, but since we don't have a way to do so, we can simply
            // invoke the hidden parent method after Android P. If in new android, the hidden method
            // ViewGroup#findViewByAccessibilityIdTraversal signature is changed or removed, we can
            // simply return null here because there will be no call to this method.
            return if (SDK_INT >= Q) {
                val findViewByAccessibilityIdTraversalMethod =
                    Class.forName("android.view.View")
                        .getDeclaredMethod("findViewByAccessibilityIdTraversal", Int::class.java)
                findViewByAccessibilityIdTraversalMethod.isAccessible = true
                findViewByAccessibilityIdTraversalMethod.invoke(this, accessibilityId) as? View
            } else {
                findViewByAccessibilityIdRootedAtCurrentView(accessibilityId, this)
            }
        } catch (_: NoSuchMethodException) {
            return null
        }
    }

    override val isLifecycleInResumedState: Boolean
        get() = viewTreeOwners?.lifecycleOwner?.lifecycle?.currentState == Lifecycle.State.RESUMED

    override fun shouldDelayChildPressedState(): Boolean = false

    // Track sensitive composable visible in this view
    private var sensitiveComponentCount = 0

    override fun incrementSensitiveComponentCount() {
        if (SDK_INT >= 35) {
            if (sensitiveComponentCount == 0) {
                AndroidComposeViewSensitiveContent35.setContentSensitivity(view, true)
            }
            sensitiveComponentCount += 1
        }
    }

    override fun decrementSensitiveComponentCount() {
        if (SDK_INT >= 35) {
            if (sensitiveComponentCount == 1) {
                AndroidComposeViewSensitiveContent35.setContentSensitivity(view, false)
            }
            sensitiveComponentCount -= 1
        }
    }

    private var keepScreenOnCount = 0

    override fun incrementKeepScreenOnCount() {
        keepScreenOnCount++
        view.keepScreenOn = keepScreenOnCount > 0
    }

    override fun decrementKeepScreenOnCount() {
        keepScreenOnCount--
        view.keepScreenOn = keepScreenOnCount > 0
    }

    override val outOfFrameExecutor
        get() = if (isAttachedToWindow) this else null

    override fun schedule(block: () -> Unit) {
        val handler =
            requireNotNull(handler) {
                "schedule is called when outOfFrameExecutor is not available (view is detached)"
            }
        handler.postAtFrontOfQueue { trace("AndroidOwner:outOfFrameExecutor", block) }
    }

    @RequiresApi(VANILLA_ICE_CREAM)
    override fun voteFrameRate(frameRate: Float) {
        if (isArrEnabled) {
            if (frameRate > 0) {
                if (currentFrameRate.isNaN() || frameRate > currentFrameRate) {
                    currentFrameRate = frameRate // set frame rate
                }
            } else if (frameRate < 0) {
                if (currentFrameRateCategory.isNaN() || frameRate < currentFrameRateCategory) {
                    currentFrameRateCategory = frameRate // set frame rate category
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchOnScrollChanged(delta: Offset) {
        // TODO(levima) b/402138549: Use viewTreeObserver.dispatchOnScrollChanged()
        dispatchOnScrollChanged(viewTreeObserver)
    }

    // executed when the layout pass has been finished. as a result of it our view could be
    // moved
    // inside the window (we are interested not only in the event when our parent positioned
    // us
    // on a different position, but also in the position of each of the grandparents as all
    // these
    // positions add up to final global position)
    override fun onGlobalLayout() {
        // make sure that we use an updated window position and matrix
        lastMatrixRecalculationAnimationTime = 0
        updatePositionCacheAndDispatch()
    }

    // executed when a scrolling container like ScrollView of RecyclerView performed the
    // scroll,
    // this could affect our global position
    override fun onScrollChanged() {
        updatePositionCacheAndDispatch()
    }

    // executed whenever the touch mode changes.
    override fun onTouchModeChanged(isInTouchMode: Boolean) {
        _inputModeManager.inputMode = if (isInTouchMode) Touch else Keyboard
    }

    companion object {
        private var systemPropertiesClass: Class<*>? = null
        private var getBooleanMethod: Method? = null
        private var addChangeCallbackMethod: Method? = null
        private val composeViews = mutableObjectListOf<AndroidComposeView>()
        private var systemPropertiesChangedRunnable: Runnable? = null
        private var dispatchOnScrollChangedMethod: Method? = null

        @Suppress("BanUncheckedReflection")
        private fun getIsShowingLayoutBounds(): Boolean =
            try {
                if (systemPropertiesClass == null) {
                    systemPropertiesClass = Class.forName("android.os.SystemProperties")
                }
                if (getBooleanMethod == null) {
                    getBooleanMethod =
                        systemPropertiesClass?.getDeclaredMethod(
                            "getBoolean",
                            String::class.java,
                            Boolean::class.java,
                        )
                }
                getBooleanMethod?.invoke(null, "debug.layout", false) as? Boolean == true
            } catch (_: Exception) {
                false
            }

        @Suppress("BanUncheckedReflection")
        private fun addNotificationForSysPropsChange(composeView: AndroidComposeView) {
            if (SDK_INT > 28) {
                // Removing the callback is prohibited on newer versions, so we should only add one
                // callback and use it for all AndroidComposeViews
                if (systemPropertiesChangedRunnable == null) {
                    val runnable = Runnable {
                        synchronized(composeViews) {
                            if (SDK_INT < 30) {
                                composeViews.forEach {
                                    val oldValue = it.showLayoutBounds
                                    it.showLayoutBounds = getIsShowingLayoutBounds()
                                    if (oldValue != it.showLayoutBounds) {
                                        it.invalidateDescendants()
                                    }
                                }
                            } else {
                                composeViews.forEach { it.invalidateDescendants() }
                            }
                        }
                    }
                    systemPropertiesChangedRunnable = runnable
                    val origPolicy = StrictMode.getVmPolicy()
                    try {
                        if (systemPropertiesClass == null) {
                            systemPropertiesClass = Class.forName("android.os.SystemProperties")
                        }
                        if (addChangeCallbackMethod == null) {
                            StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
                            addChangeCallbackMethod =
                                systemPropertiesClass?.getDeclaredMethod(
                                    "addChangeCallback",
                                    Runnable::class.java,
                                )
                        }
                        addChangeCallbackMethod?.invoke(null, runnable)
                    } catch (_: Throwable) {} finally {
                        StrictMode.setVmPolicy(origPolicy)
                    }
                }
                synchronized(composeViews) { composeViews += composeView }
            }
        }

        private fun removeNotificationForSysPropsChange(composeView: AndroidComposeView) {
            if (SDK_INT > 28) {
                synchronized(composeViews) { composeViews -= composeView }
            }
        }

        // Back compat implementation
        @SuppressLint("BanUncheckedReflection") // suppress for now, the API is available in MIN_SDK
        fun dispatchOnScrollChanged(viewTreeObserver: ViewTreeObserver) {
            try {
                if (dispatchOnScrollChangedMethod == null) {
                    dispatchOnScrollChangedMethod =
                        viewTreeObserver.javaClass
                            .getDeclaredMethod("dispatchOnScrollChanged")
                            .also { it.isAccessible = true }
                }
                dispatchOnScrollChangedMethod?.invoke(viewTreeObserver)
            } catch (_: Exception) {}
        }
    }

    /** Combines objects populated via ViewTree*Owner */
    class ViewTreeOwners(
        /** The [LifecycleOwner] associated with this owner. */
        val lifecycleOwner: LifecycleOwner,
        /** The [SavedStateRegistryOwner] associated with this owner. */
        val savedStateRegistryOwner: SavedStateRegistryOwner,
        /** The [ViewModelStoreOwner] associated with this owner. */
        val viewModelStoreOwner: ViewModelStoreOwner?,
    )

    private inner class RootModifierNode :
        Modifier.Node(),
        BringIntoViewModifierNode,
        SemanticsModifierNode,
        RotaryInputModifierNode,
        KeyInputModifierNode,
        LayoutModifierNode,
        TraversableNode,
        WindowInsetsRulerProvider {
        override val insetsValues: ScatterMap<Any, WindowWindowInsetsAnimationValues>
            get() = insetsListener.insetsValues

        val generation: MutableIntState
            get() = insetsListener.generation

        var previousGeneration = -1

        override val cutoutRects: MutableObjectList<MutableState<Rect>>
            get() = insetsListener.displayCutouts

        override val cutoutRulers: List<RectRulers>
            get() = insetsListener.displayCutoutRulers

        override val insetsListener: InsetsListener
            get() = this@AndroidComposeView.insetsListener

        @OptIn(ExperimentalComposeUiApi::class)
        val rulerLambda: RulerScope.() -> Unit = {
            previousGeneration = generation.intValue // just read the value so it is observed
            // When generation is 0, no updateInsets() has been called yet, so we don't need to
            // provide any insets.
            if (previousGeneration > 0 && ComposeUiFlags.areWindowInsetsRulersEnabled) {
                provideWindowInsetsRulers(this@RootModifierNode)
            }
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            val width = placeable.width
            val height = placeable.height
            return layout(width, height, rulers = rulerLambda) { placeable.place(0, 0) }
        }

        override val traverseKey: Any
            get() = RulerKey

        override fun SemanticsPropertyReceiver.applySemantics() {}

        override suspend fun bringIntoView(
            childCoordinates: LayoutCoordinates,
            boundsProvider: () -> androidx.compose.ui.geometry.Rect?,
        ) {
            val childOffset = childCoordinates.positionInRoot()
            val rootRect = boundsProvider()?.translate(childOffset)
            if (rootRect != null) {
                requestRectangleOnScreen(rootRect.toAndroidRect(), false)
            }
        }

        // TODO(b/210748692): call focusManager.moveFocus() in response to rotary events.
        override fun onRotaryScrollEvent(event: RotaryScrollEvent) = false

        override fun onPreRotaryScrollEvent(event: RotaryScrollEvent) = false

        override fun onPreKeyEvent(event: KeyEvent): Boolean = false

        // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
        //  that this common logic can be used by all owners.
        override fun onKeyEvent(event: KeyEvent): Boolean {
            val focusDirection = event.toFocusDirection()
            if (focusDirection == null || event.type != KeyDown) return false

            @OptIn(ExperimentalComposeUiApi::class)
            if (ComposeUiFlags.isBypassUnfocusableComposeViewEnabled) {
                // If an embedded view has focus, attempt to move focus within it.
                if (
                    focusOwner.activeFocusTargetNode?.isInteropViewHost == true &&
                        moveFocusInChildren(focusDirection)
                ) {
                    return true
                }

                val focusedRect = getEmbeddedViewFocusRect()
                val focusWasMovedOrCancelled =
                    focusOwner.focusSearch(focusDirection, focusedRect) {
                        it.requestFocus(focusDirection)
                    } ?: true

                // Consume the key event if we moved focus or if focus search or requestFocus is
                // cancelled.
                if (focusWasMovedOrCancelled) return true

                // We ideally don't consume the key event, and let the framework handle it. This
                // will move to embedded views or sibling views as needed. However for 1D focus
                // search focus might rollover and return back to this view. In that case we need
                // to reset focus in Compose.
                if (focusDirection.is1dFocusSearch()) {
                    val direction = focusDirection.toAndroidFocusDirection() ?: FOCUS_FORWARD
                    val nextView =
                        FocusFinder.getInstance()
                            .findNextFocus(rootView as ViewGroup, view, direction)
                    if (nextView == null || nextView == this) {
                        return focusOwner.resetFocus(focusDirection)
                    }
                }

                return false
            }

            val androidDirection = focusDirection.toAndroidFocusDirection()

            @OptIn(ExperimentalComposeUiApi::class)
            if (ComposeUiFlags.isViewFocusFixEnabled) {
                if (hasFocus() && androidDirection != null) {
                    // A child AndroidView is focused. See if the view has a child that should be
                    // focused next.
                    if (moveFocusInChildren(focusDirection)) return true
                }
            }
            val focusedRect = getEmbeddedViewFocusRect()

            // Consume the key event if we moved focus or if focus search or requestFocus is
            // cancelled.
            val focusWasMovedOrCancelled =
                focusOwner.focusSearch(focusDirection, focusedRect) {
                    it.requestFocus(focusDirection)
                } ?: true
            if (focusWasMovedOrCancelled) return true

            // For 2D focus search, we don't need to wrap around, so we just return false. If there
            // are
            // items after this view that haven't been visited, they will be visited when the
            // unconsumed key event triggers a focus search.
            if (!focusDirection.is1dFocusSearch()) return false

            // For 1D focus search, we use FocusFinder to find the next view that is not a child of
            // this view. We don't return false because we don't want to re-visit sub-views. They
            // will
            // instead be visited when the AndroidView around them gets a moveFocus(Enter)).
            if (androidDirection != null) {
                val nextView = findNextNonChildView(androidDirection).takeIf { it != this }
                if (nextView != null) {
                    val androidRect =
                        checkPreconditionNotNull(focusedRect?.toAndroidRect()) { "Invalid rect" }
                    val rootView = rootView as ViewGroup
                    rootView.offsetDescendantRectToMyCoords(view, androidRect)
                    rootView.offsetRectIntoDescendantCoords(nextView, androidRect)
                    if (nextView.requestInteropFocus(androidDirection, androidRect)) {
                        return true
                    }
                }
            }

            // Focus finder couldn't find another view. We manually wrap around since focus remained
            // on this view.
            val clearedFocusSuccessfully =
                focusOwner.clearFocus(
                    force = false,
                    refreshFocusEvents = true,
                    clearOwnerFocus = false,
                    focusDirection = focusDirection,
                )

            // Consume the key event if clearFocus was cancelled.
            if (!clearedFocusSuccessfully) return true

            // Perform wrap-around focus search by running a focus search after clearing focus.
            return focusOwner.focusSearch(focusDirection, null) { it.requestFocus(focusDirection) }
                ?: true
        }
    }
}

@RequiresApi(S)
private object AndroidComposeViewTranslationCallback : ViewTranslationCallback {
    override fun onShowTranslation(view: View): Boolean {
        val androidComposeView = view as AndroidComposeView
        androidComposeView.contentCaptureManager.onShowTranslation()
        return true
    }

    override fun onHideTranslation(view: View): Boolean {
        val androidComposeView = view as AndroidComposeView
        androidComposeView.contentCaptureManager.onHideTranslation()
        return true
    }

    override fun onClearTranslation(view: View): Boolean {
        val androidComposeView = view as AndroidComposeView
        androidComposeView.contentCaptureManager.onClearTranslation()
        return true
    }
}

/**
 * These classes are here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes which
 * use this method will pass.
 */
@RequiresApi(O)
private object AndroidComposeViewVerificationHelperMethodsO {
    @RequiresApi(O)
    @DoNotInline
    fun focusable(view: View, focusable: Int, defaultFocusHighlightEnabled: Boolean) {
        view.focusable = focusable
        // not to add the default focus highlight to the whole compose view
        view.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
    }
}

@RequiresApi(M)
private object AndroidComposeViewAssistHelperMethodsO {
    @RequiresApi(M)
    @DoNotInline
    fun setClassName(structure: ViewStructure, view: View) {
        structure.setClassName(view.accessibilityClassName.toString())
    }
}

@RequiresApi(N)
private object AndroidComposeViewVerificationHelperMethodsN {
    @RequiresApi(N)
    fun toAndroidPointerIcon(context: Context, icon: PointerIcon?): android.view.PointerIcon =
        when (icon) {
            is AndroidPointerIcon -> icon.pointerIcon
            is AndroidPointerIconType -> android.view.PointerIcon.getSystemIcon(context, icon.type)
            else ->
                android.view.PointerIcon.getSystemIcon(
                    context,
                    android.view.PointerIcon.TYPE_DEFAULT,
                )
        }

    @DoNotInline
    @RequiresApi(N)
    fun setPointerIcon(view: View, icon: PointerIcon?) {
        val iconToSet = toAndroidPointerIcon(view.context, icon)

        if (view.pointerIcon != iconToSet) {
            view.pointerIcon = iconToSet
        }
    }
}

@RequiresApi(Q)
private object AndroidComposeViewForceDarkModeQ {
    @DoNotInline
    @RequiresApi(Q)
    fun disallowForceDark(view: View) {
        view.isForceDarkAllowed = false
    }
}

@RequiresApi(S)
internal object AndroidComposeViewTranslationCallbackS {
    @DoNotInline
    @RequiresApi(S)
    fun setViewTranslationCallback(view: View) {
        view.setViewTranslationCallback(AndroidComposeViewTranslationCallback)
    }

    @DoNotInline
    @RequiresApi(S)
    fun clearViewTranslationCallback(view: View) {
        view.clearViewTranslationCallback()
    }
}

/** Sets this [Matrix] to be the result of this * [other] */
private fun Matrix.preTransform(other: Matrix) {
    val v00 = dot(other, 0, this, 0)
    val v01 = dot(other, 0, this, 1)
    val v02 = dot(other, 0, this, 2)
    val v03 = dot(other, 0, this, 3)
    val v10 = dot(other, 1, this, 0)
    val v11 = dot(other, 1, this, 1)
    val v12 = dot(other, 1, this, 2)
    val v13 = dot(other, 1, this, 3)
    val v20 = dot(other, 2, this, 0)
    val v21 = dot(other, 2, this, 1)
    val v22 = dot(other, 2, this, 2)
    val v23 = dot(other, 2, this, 3)
    val v30 = dot(other, 3, this, 0)
    val v31 = dot(other, 3, this, 1)
    val v32 = dot(other, 3, this, 2)
    val v33 = dot(other, 3, this, 3)
    this[0, 0] = v00
    this[0, 1] = v01
    this[0, 2] = v02
    this[0, 3] = v03
    this[1, 0] = v10
    this[1, 1] = v11
    this[1, 2] = v12
    this[1, 3] = v13
    this[2, 0] = v20
    this[2, 1] = v21
    this[2, 2] = v22
    this[2, 3] = v23
    this[3, 0] = v30
    this[3, 1] = v31
    this[3, 2] = v32
    this[3, 3] = v33
}

/** Like [android.graphics.Matrix.preTranslate], for a Compose [Matrix] */
private fun Matrix.preTranslate(x: Float, y: Float, tmpMatrix: Matrix) {
    tmpMatrix.reset()
    tmpMatrix.translate(x, y)
    preTransform(tmpMatrix)
}

// Taken from Matrix.kt
private fun dot(m1: Matrix, row: Int, m2: Matrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
        m1[row, 1] * m2[1, column] +
        m1[row, 2] * m2[2, column] +
        m1[row, 3] * m2[3, column]
}

private interface CalculateMatrixToWindow {
    /**
     * Calculates the matrix from [view] to screen coordinates and returns the value in [matrix].
     */
    fun calculateMatrixToWindow(view: View, matrix: Matrix)
}

@RequiresApi(35)
private object AndroidComposeViewSensitiveContent35 {
    @DoNotInline
    @RequiresApi(35)
    fun setContentSensitivity(view: View, isSensitiveContent: Boolean) {
        if (isSensitiveContent) {
            view.setContentSensitivity(View.CONTENT_SENSITIVITY_SENSITIVE)
        } else {
            view.setContentSensitivity(View.CONTENT_SENSITIVITY_AUTO)
        }
    }
}

@RequiresApi(Q)
private class CalculateMatrixToWindowApi29 : CalculateMatrixToWindow {
    private val tmpMatrix = android.graphics.Matrix()
    private val tmpPosition = IntArray(2)

    @DoNotInline
    override fun calculateMatrixToWindow(view: View, matrix: Matrix) {
        tmpMatrix.reset()
        view.transformMatrixToGlobal(tmpMatrix)
        var parent = view.parent
        var root = view
        while (parent is View) {
            root = parent
            parent = root.parent
        }
        root.getLocationOnScreen(tmpPosition)
        val (screenX, screenY) = tmpPosition
        root.getLocationInWindow(tmpPosition)
        val (windowX, windowY) = tmpPosition
        tmpMatrix.postTranslate((windowX - screenX).toFloat(), (windowY - screenY).toFloat())
        matrix.setFrom(tmpMatrix)
    }
}

private class CalculateMatrixToWindowApi21(private val tmpMatrix: Matrix) :
    CalculateMatrixToWindow {
    private val tmpLocation = IntArray(2)

    override fun calculateMatrixToWindow(view: View, matrix: Matrix) {
        matrix.reset()
        transformMatrixToWindow(view, matrix)
    }

    private fun transformMatrixToWindow(view: View, matrix: Matrix) {
        val parentView = view.parent
        if (parentView is View) {
            transformMatrixToWindow(parentView, matrix)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
        } else {
            val pos = tmpLocation
            view.getLocationInWindow(pos)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(pos[0].toFloat(), pos[1].toFloat())
        }

        val viewMatrix = view.matrix
        if (!viewMatrix.isIdentity) {
            matrix.preConcat(viewMatrix)
        }
    }

    /**
     * Like [android.graphics.Matrix.preConcat], for a Compose [Matrix] that accepts an [other]
     * [android.graphics.Matrix].
     */
    private fun Matrix.preConcat(other: android.graphics.Matrix) {
        tmpMatrix.setFrom(other)
        preTransform(tmpMatrix)
    }

    /** Like [android.graphics.Matrix.preTranslate], for a Compose [Matrix] */
    private fun Matrix.preTranslate(x: Float, y: Float) {
        preTranslate(x, y, tmpMatrix)
    }
}

@RequiresApi(29)
private object MotionEventVerifierApi29 {
    @DoNotInline
    fun isValidMotionEvent(event: MotionEvent, index: Int): Boolean {
        return event.getRawX(index).fastIsFinite() && event.getRawY(index).fastIsFinite()
    }
}

@RequiresApi(N)
private object AndroidComposeViewStartDragAndDropN {
    @DoNotInline
    @RequiresApi(N)
    fun startDragAndDrop(
        view: View,
        transferData: DragAndDropTransferData,
        dragShadowBuilder: ComposeDragShadowBuilder,
    ): Boolean =
        view.startDragAndDrop(
            transferData.clipData,
            dragShadowBuilder,
            transferData.localState,
            transferData.flags,
        )
}

private fun View.containsDescendant(other: View): Boolean {
    if (other == this) return false
    var viewParent = other.parent
    while (viewParent != null) {
        if (viewParent === this) return true
        viewParent = viewParent.parent
    }
    return false
}

private fun View.getContentCaptureSessionCompat(): ContentCaptureSessionWrapper? {
    ViewCompatShims.setImportantForContentCapture(
        this,
        ViewCompatShims.IMPORTANT_FOR_CONTENT_CAPTURE_YES,
    )
    return ViewCompatShims.getContentCaptureSession(this)
}

private class BringIntoViewOnScreenResponderNode(var view: ViewGroup) :
    Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> androidx.compose.ui.geometry.Rect?,
    ) {
        val childOffset = childCoordinates.positionInRoot()
        val rootRect = boundsProvider()?.translate(childOffset)
        if (rootRect != null) {
            view.requestRectangleOnScreen(rootRect.toAndroidRect(), false)
        }
    }
}

/** Split out to avoid class verification errors. This class will only be loaded when SDK >= 30. */
@RequiresApi(30)
private object Api30Impl {
    @DoNotInline fun isShowingLayoutBounds(view: View) = view.isShowingLayoutBounds
}

@RequiresApi(35)
private object Api35Impl {
    @JvmStatic
    @DoNotInline
    fun setRequestedFrameRate(view: View, frameRate: Float) {
        view.requestedFrameRate = frameRate
    }
}

internal class IndirectTouchNavigationGestureDetector(
    context: Context,
    private val onMoveFocus: (FocusDirection) -> Unit,
) {
    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    var primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.None

    private val gestureDetector: GestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.OnGestureListener {
                override fun onDown(e: MotionEvent) = true

                override fun onShowPress(e: MotionEvent) {}

                override fun onSingleTapUp(e: MotionEvent): Boolean = true

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ) = true

                override fun onLongPress(e: MotionEvent) {}

                @OptIn(ExperimentalIndirectTouchTypeApi::class)
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (
                        primaryDirectionalMotionAxis ==
                            IndirectTouchEventPrimaryDirectionalMotionAxis.X
                    ) {
                        if (abs(velocityX) > abs(velocityY)) {
                            val direction =
                                if (velocityX > 0f) FocusDirection.Next else FocusDirection.Previous
                            onMoveFocus(direction)
                        }
                    } else if (
                        primaryDirectionalMotionAxis ==
                            IndirectTouchEventPrimaryDirectionalMotionAxis.Y
                    ) {
                        if (abs(velocityY) > abs(velocityX)) {
                            val direction =
                                if (velocityY > 0f) FocusDirection.Next else FocusDirection.Previous
                            onMoveFocus(direction)
                        }
                    }
                    // If it gets here, it means there isn't a primary axis specified, which means
                    // the event will be translated by system to key up, down, left, and right.

                    return true
                }
            },
        )

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
