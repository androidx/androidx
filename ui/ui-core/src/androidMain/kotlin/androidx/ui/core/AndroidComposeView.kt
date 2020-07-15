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

@file:OptIn(ExperimentalComposeApi::class)

// TODO(b/160821157): Replace FocusDetailedState with FocusState2
@file:Suppress("DEPRECATION")

package androidx.ui.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.ViewTreeObserver
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ExperimentalComposeApi
import androidx.compose.snapshots.SnapshotStateObserver
import androidx.core.os.HandlerCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.autofill.performAutofill
import androidx.ui.autofill.populateViewStructure
import androidx.ui.autofill.registerCallback
import androidx.ui.autofill.unregisterCallback
import androidx.ui.core.AndroidOwner.ViewTreeOwners
import androidx.ui.core.clipboard.AndroidClipboardManager
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.focus.ExperimentalFocus
import androidx.ui.core.focus.FOCUS_TAG
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.core.focus.FocusModifierImpl
import androidx.ui.core.hapticfeedback.AndroidHapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.keyinput.ExperimentalKeyInput
import androidx.ui.core.keyinput.KeyEvent2
import androidx.ui.core.keyinput.KeyEventAndroid
import androidx.ui.core.keyinput.KeyInputModifier
import androidx.ui.core.pointerinput.MotionEventAdapter
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.ProcessResult
import androidx.ui.core.semantics.SemanticsModifierCore
import androidx.ui.core.semantics.SemanticsOwner
import androidx.ui.core.text.AndroidFontResourceLoader
import androidx.ui.core.texttoolbar.AndroidTextToolbar
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.CanvasHolder
import androidx.ui.input.TextInputServiceAndroid
import androidx.ui.input.textInputServiceFactory
import androidx.ui.savedinstancestate.UiSavedStateRegistry
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import androidx.ui.unit.IntOffset
import androidx.ui.util.fastForEach
import androidx.ui.util.trace
import java.lang.reflect.Method
import kotlin.math.max
import android.view.KeyEvent as AndroidKeyEvent

/***
 * This function creates an instance of [AndroidOwner]
 *
 * @param context Context to use to create a View
 * @param lifecycleOwner Current [LifecycleOwner]. When it is not provided we will try to get the
 * owner using [ViewTreeLifecycleOwner] when we will be attached.
 */
fun AndroidOwner(
    context: Context,
    lifecycleOwner: LifecycleOwner? = null,
    viewModelStoreOwner: ViewModelStoreOwner? = null
): AndroidOwner = AndroidComposeView(
    context,
    lifecycleOwner,
    viewModelStoreOwner
)

@OptIn(
    ExperimentalFocus::class,
    ExperimentalKeyInput::class,
    ExperimentalLayoutNodeApi::class
)
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class AndroidComposeView constructor(
    context: Context,
    initialLifecycleOwner: LifecycleOwner?,
    initialViewModelStoreOwner: ViewModelStoreOwner?
) : ViewGroup(context), AndroidOwner {

    override val view: View = this

    override var density = Density(context)
        private set

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeAllDescendants = false,
        properties = {}
    )

    // TODO(b/160821157): Replace FocusDetailedState with FocusState2.
    @Suppress("DEPRECATION")
    private val focusModifier = FocusModifierImpl(Inactive)
    private val keyInputModifier = KeyInputModifier(null, null)

    private val canvasHolder = CanvasHolder()

    override val root = LayoutNode().also {
        it.measureBlocks = RootMeasureBlocks
        it.modifier = Modifier.drawLayer() + semanticsModifier + focusModifier + keyInputModifier
    }

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)
    private val accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(this)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    override val autofillTree = AutofillTree()

    // OwnedLayers that are dirty and should be redrawn.
    internal val dirtyLayers = mutableListOf<OwnedLayer>()

    private val motionEventAdapter = MotionEventAdapter()
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    // TODO(mount): reinstate when coroutines are supported by IR compiler
    // private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    // Used for updating the ConfigurationAmbient when configuration changes - consume the
    // configuration ambient instead of changing this observer if you are writing a component that
    // adapts to configuration changes.
    override var configurationChangeObserver: () -> Unit = {}

    private val _autofill = if (autofillSupported()) AndroidAutofill(this, autofillTree) else null

    // Used as an ambient for performing autofill.
    override val autofill: Autofill? get() = _autofill

    // TODO(b/160821157): Replace FocusDetailedState with FocusState2.
    @Suppress("DEPRECATION")
    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        Log.d(FOCUS_TAG, "Owner FocusChanged($gainFocus)")
        if (gainFocus) {
            // If the focus state is not Inactive, it indicates that the focus state is already
            // set (possibly by dispatchWindowFocusChanged). So we don't update the state.
            if (focusModifier.focusDetailedState == Inactive) {
                focusModifier.focusDetailedState = Active
                // TODO(b/152535715): propagate focus to children based on child focusability.
            }
        } else {
            // If this view lost focus, clear focus from the children.
            with(focusModifier) {
                focusNode?.clearFocus(forcedClear = true)
                focusDetailedState = Inactive
            }
        }
    }

    override fun sendKeyEvent(keyEvent: KeyEvent2): Boolean {
        return keyInputModifier.processKeyInput(keyEvent)
    }

    override fun dispatchKeyEvent(event: AndroidKeyEvent): Boolean {
        return sendKeyEvent(KeyEventAndroid(event))
    }

    private val snapshotObserver = SnapshotStateObserver { command ->
        if (handler.looper === Looper.myLooper()) {
            command()
        } else {
            handler.post(command)
        }
    }

    private val onCommitAffectingMeasure: (LayoutNode) -> Unit = { layoutNode ->
        onRequestMeasure(layoutNode)
    }

    private val onCommitAffectingLayout: (LayoutNode) -> Unit = { layoutNode ->
        if (measureAndLayoutDelegate.requestRelayout(layoutNode)) {
            scheduleMeasureAndLayout()
        }
    }

    internal val onCommitAffectingLayer: (OwnedLayer) -> Unit = { layer ->
        layer.invalidate()
    }

    private val onCommitAffectingLayerParams: (OwnedLayer) -> Unit = { layer ->
        handler.postAtFrontOfQueue {
            updateLayerProperties(layer)
        }
    }

    @OptIn(InternalCoreApi::class)
    override var showLayoutBounds = false

    override fun pauseModelReadObserveration(block: () -> Unit) =
        @OptIn(ExperimentalComposeApi::class)
        snapshotObserver.pauseObservingReads(block)

    init {
        setWillNotDraw(false)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusable = View.FOCUSABLE
            // not to add the default focus highlight to the whole compose view
            defaultFocusHighlightEnabled = false
        }
        isFocusableInTouchMode = true
        clipChildren = false
        root.isPlaced = true
        ViewCompat.setAccessibilityDelegate(this, accessibilityDelegate)
        AndroidOwner.onAndroidOwnerCreatedCallback?.invoke(this)
    }

    override fun onInvalidate(layoutNode: LayoutNode) {
        invalidate(layoutNode)
    }

    private fun invalidate(node: LayoutNode) {
        val layer = node.findLayer()
        if (layer == null) {
            invalidate()
        } else {
            layer.invalidate()
        }
    }

    override fun onAttach(node: LayoutNode) {
    }

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
    }

    private var _androidViewsHandler: AndroidViewsHandler? = null
    private val androidViewsHandler: AndroidViewsHandler
        get() {
            if (_androidViewsHandler == null) {
                _androidViewsHandler = AndroidViewsHandler(context)
                addView(_androidViewsHandler)
            }
            return _androidViewsHandler!!
        }
    private val viewLayersContainer by lazy(LazyThreadSafetyMode.NONE) {
        ViewLayerContainer(context).also { addView(it) }
    }

    override fun addAndroidView(view: View, layoutNode: LayoutNode) {
        androidViewsHandler.layoutNode[view] = layoutNode
        androidViewsHandler.addView(view)
    }

    override fun removeAndroidView(view: View) {
        androidViewsHandler.removeView(view)
        androidViewsHandler.layoutNode.remove(view)
    }

    // [ Layout block start ]

    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    private var measureAndLayoutScheduled = false

    private val measureAndLayoutHandler: Handler =
        HandlerCompat.createAsync(Looper.getMainLooper()) {
            measureAndLayoutScheduled = false
            measureAndLayout()
            true
        }

    private fun scheduleMeasureAndLayout() {
        if (!isLayoutRequested && !measureAndLayoutScheduled) {
            measureAndLayoutScheduled = true
            measureAndLayoutHandler.sendEmptyMessage(0)
        }
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    override fun measureAndLayout() {
        val rootNodeResized = measureAndLayoutDelegate.measureAndLayout()
        if (rootNodeResized) {
            requestLayout()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun onRequestRelayout(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRelayout(layoutNode)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        savedStateDelegate.stopWaitingForStateRestoration()
        trace("AndroidOwner:onMeasure") {
            val (minWidth, maxWidth) = convertMeasureSpec(widthMeasureSpec)
            val (minHeight, maxHeight) = convertMeasureSpec(heightMeasureSpec)

            measureAndLayoutDelegate.updateRootParams(
                Constraints(minWidth, maxWidth, minHeight, maxHeight),
                resources.configuration.localeLayoutDirection
            )
            measureAndLayoutDelegate.measureAndLayout()
            setMeasuredDimension(root.width, root.height)
        }
    }

    private fun convertMeasureSpec(measureSpec: Int): Pair<Int, Int> {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> size to size
            MeasureSpec.UNSPECIFIED -> 0 to Constraints.Infinity
            MeasureSpec.AT_MOST -> 0 to size
            else -> throw IllegalStateException()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // we postpone onPositioned callbacks until onLayout as LayoutCoordinates
        // are currently wrong if you try to get the global(activity) coordinates -
        // View is not yet laid out.
        dispatchOnPositioned()
        if (_androidViewsHandler != null && androidViewsHandler.isLayoutRequested) {
            // Even if we laid out during onMeasure, this can happen when the Views hierarchy
            // receives forceLayout(). We need to relayout to clear the isLayoutRequested info
            // on the Views, as otherwise further layout requests will be discarded.
            androidViewsHandler.layout(0, 0, r - l, b - t)
        }
    }

    private var globalPosition: IntOffset = IntOffset.Origin

    private val tmpPositionArray = intArrayOf(0, 0)

    private fun dispatchOnPositioned() {
        var positionChanged = false
        getLocationOnScreen(tmpPositionArray)
        if (globalPosition.x != tmpPositionArray[0] || globalPosition.y != tmpPositionArray[1]) {
            globalPosition = IntOffset(tmpPositionArray[0], tmpPositionArray[1])
            positionChanged = true
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = positionChanged)
    }

    // [ Layout block end ]

    override fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit) {
        snapshotObserver.observeReads(node, onCommitAffectingLayout, block)
    }

    override fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit) {
        snapshotObserver.observeReads(node, onCommitAffectingMeasure, block)
    }

    fun observeLayerModelReads(layer: OwnedLayer, block: () -> Unit) {
        snapshotObserver.observeReads(layer, onCommitAffectingLayer, block)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    override fun createLayer(
        drawLayerModifier: DrawLayerModifier,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        val layer = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || isInEditMode()) {
            ViewLayer(
                this, viewLayersContainer, drawLayerModifier, drawBlock,
                invalidateParentLayer
            )
        } else {
            RenderNodeLayer(this, drawLayerModifier, drawBlock, invalidateParentLayer)
        }

        updateLayerProperties(layer)

        return layer
    }

    override fun onSemanticsChange() {
        accessibilityDelegate.onSemanticsChange()
    }

    private fun updateLayerProperties(layer: OwnedLayer) {
        snapshotObserver.observeReads(layer, onCommitAffectingLayerParams) {
            layer.updateLayerProperties()
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        measureAndLayout()
        // we don't have to observe here because the root has a layer modifier
        // that will observe all children. The AndroidComposeView has only the
        // root, so it doesn't have to invalidate itself based on model changes.
        canvasHolder.drawInto(canvas) { root.draw(this) }

        if (dirtyLayers.isNotEmpty()) {
            for (i in 0 until dirtyLayers.size) {
                val layer = dirtyLayers[i]
                layer.updateDisplayList()
            }
            dirtyLayers.clear()
        }
    }

    override var viewTreeOwners: ViewTreeOwners? =
        if (initialLifecycleOwner != null && initialViewModelStoreOwner != null) {
            ViewTreeOwners(
                initialLifecycleOwner,
                initialViewModelStoreOwner
            )
        } else {
            null
        }
        private set

    override fun setOnViewTreeOwnersAvailable(callback: (ViewTreeOwners) -> Unit) {
        val viewTreeOwners = viewTreeOwners
        if (viewTreeOwners != null) {
            callback(viewTreeOwners)
        } else {
            onViewTreeOwnersAvailable = callback
        }
    }

    private var onViewTreeOwnersAvailable: ((ViewTreeOwners) -> Unit)? = null

    // executed when the layout pass has been finished. as a result of it our view could be moved
    // inside the window (we are interested not only in the event when our parent positioned us
    // on a different position, but also in the position of each of the grandparents as all these
    // positions add up to final global position)
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        dispatchOnPositioned()
    }
    // executed when a scrolling container like ScrollView of RecyclerView performed the scroll,
    // this could affect our global position
    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        dispatchOnPositioned()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showLayoutBounds = getIsShowingLayoutBounds()
        snapshotObserver.enableStateUpdatesObserving(true)
        ifDebug { if (autofillSupported()) _autofill?.registerCallback() }
        root.attach(this)

        if (viewTreeOwners == null) {
            val lifecycleOwner = ViewTreeLifecycleOwner.get(this) ?: throw IllegalStateException(
                "Composed into the View which doesn't propagate ViewTreeLifecycleOwner!"
            )
            val viewModelStoreOwner =
                ViewTreeViewModelStoreOwner.get(this) ?: throw IllegalStateException(
                    "Composed into the View which doesn't propagate ViewTreeViewModelStoreOwner!"
                )
            val viewTreeOwners = ViewTreeOwners(
                lifecycleOwner = lifecycleOwner,
                viewModelStoreOwner = viewModelStoreOwner
            )
            this.viewTreeOwners = viewTreeOwners
            onViewTreeOwnersAvailable?.invoke(viewTreeOwners)
        }
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotObserver.enableStateUpdatesObserving(false)
        ifDebug { if (autofillSupported()) _autofill?.unregisterCallback() }
        if (measureAndLayoutScheduled) {
            measureAndLayoutHandler.removeMessages(0)
        }
        root.detach()
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure?, flags: Int) {
        if (autofillSupported() && structure != null) _autofill?.populateViewStructure(structure)
    }

    override fun autofill(values: SparseArray<AutofillValue>) {
        if (autofillSupported()) _autofill?.performAutofill(values)
    }

    // TODO(shepshapard): Test this method.
    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        measureAndLayout()
        val processResult = trace("AndroidOwner:onTouch") {
            val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
            if (pointerInputEvent != null) {
                pointerInputEventProcessor.process(pointerInputEvent)
            } else {
                pointerInputEventProcessor.processCancel()
                ProcessResult(
                    dispatchedToAPointerInputModifier = false,
                    anyMovementConsumed = false
                )
            }
        }

        if (processResult.anyMovementConsumed) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return processResult.dispatchedToAPointerInputModifier
    }

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    override val textInputService =
        @Suppress("DEPRECATION_ERROR")
        textInputServiceFactory(textInputServiceAndroid)

    override val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    override val hapticFeedBack: HapticFeedback =
        AndroidHapticFeedback(this)

    /**
     * Provide clipboard manager to the user. Use the Android version of clipboard manager.
     */
    override val clipboardManager: ClipboardManager = AndroidClipboardManager(context)

    /**
     * Provide textToolbar to the user, for text-related operation. Use the Android version of
     * floating toolbar(post-M) and primary toolbar(pre-M).
     */
    override val textToolbar: TextToolbar = AndroidTextToolbar(this)

    override fun onCheckIsTextEditor(): Boolean = textInputServiceAndroid.isEditorFocused()

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        textInputServiceAndroid.createInputConnection(outAttrs)

    override fun calculatePosition(): IntOffset = globalPosition

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        configurationChangeObserver()
    }

    private val savedStateDelegate = SavedStateDelegate {
        // When AndroidComposeView is composed into some ViewGroup we just add ourself as a child
        // for this ViewGroup. And we don't have any id on AndroidComposeView as we can't make it
        // unique, but we require this parent ViewGroup to have an unique id for the saved
        // instance state mechanism to work (similarly to how it works without Compose).
        // When we composed into Activity our parent is the ViewGroup with android.R.id.content.
        (parent as? View)?.id ?: View.NO_ID
    }

    /**
     * The current instance of [UiSavedStateRegistry]. If it's null you can wait for it to became
     * available using [setOnSavedStateRegistryAvailable].
     */
    override val savedStateRegistry: UiSavedStateRegistry?
        get() = savedStateDelegate.savedStateRegistry

    /**
     * Allows other components to be notified when the [UiSavedStateRegistry] became available.
     */
    override fun setOnSavedStateRegistryAvailable(callback: (UiSavedStateRegistry) -> Unit) {
        savedStateDelegate.setOnSaveRegistryAvailable(callback)
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        val superState = super.onSaveInstanceState()!!
        savedStateDelegate.dispatchSaveInstanceState(container, superState)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        val superState = savedStateDelegate.dispatchRestoreInstanceState(container)
        onRestoreInstanceState(superState)
    }

    private fun autofillSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityDelegate.dispatchHoverEvent(event)
    }

    companion object {
        private var systemPropertiesClass: Class<*>? = null
        private var getBooleanMethod: Method? = null

        // TODO(mount): replace with ViewCompat.isShowingLayoutBounds() when it becomes available.
        @SuppressLint("PrivateApi")
        private fun getIsShowingLayoutBounds(): Boolean {
            try {
                if (systemPropertiesClass == null) {
                    systemPropertiesClass = Class.forName("android.os.SystemProperties")
                    getBooleanMethod = systemPropertiesClass?.getDeclaredMethod(
                        "getBoolean",
                        String::class.java,
                        Boolean::class.java
                    )
                }

                return getBooleanMethod?.invoke(null, "debug.layout", false) as? Boolean ?: false
            } catch (e: Exception) {
                return false
            }
        }

        private val RootMeasureBlocks = object : LayoutNode.MeasureBlocks {
            override fun measure(
                measureScope: MeasureScope,
                measurables: List<Measurable>,
                constraints: Constraints,
                layoutDirection: LayoutDirection
            ): MeasureScope.MeasureResult {
                return when {
                    measurables.isEmpty() -> measureScope.layout(0, 0) {}
                    measurables.size == 1 -> {
                        val placeable = measurables[0].measure(constraints, layoutDirection)
                        measureScope.layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
                    else -> {
                        val placeables = measurables.map {
                            it.measure(constraints, layoutDirection)
                        }
                        var maxWidth = 0
                        var maxHeight = 0
                        placeables.fastForEach { placeable ->
                            maxWidth = max(placeable.width, maxWidth)
                            maxHeight = max(placeable.height, maxHeight)
                        }
                        measureScope.layout(maxWidth, maxHeight) {
                            placeables.fastForEach { placeable ->
                                placeable.place(0, 0)
                            }
                        }
                    }
                }
            }

            override fun minIntrinsicWidth(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                h: Int,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun minIntrinsicHeight(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                w: Int,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicWidth(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                h: Int,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicHeight(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                w: Int,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")
        }
    }
}

/**
 * Return the layout direction set by the [Locale][java.util.Locale].
 *
 * A convenience getter that translates [Configuration.getLayoutDirection] result into
 * [LayoutDirection] instance.
 */
val Configuration.localeLayoutDirection: LayoutDirection
    get() = when (layoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        // API doc says Configuration#getLayoutDirection only returns LTR or RTL.
        // Fallback to LTR for unexpected return value.
        else -> LayoutDirection.Ltr
    }
