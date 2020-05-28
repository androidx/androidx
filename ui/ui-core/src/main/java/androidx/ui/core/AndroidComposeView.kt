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
package androidx.ui.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.SparseArray
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.os.HandlerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.ui.autofill.AndroidAutofill
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.autofill.performAutofill
import androidx.ui.autofill.populateViewStructure
import androidx.ui.autofill.registerCallback
import androidx.ui.autofill.unregisterCallback
import androidx.ui.core.clipboard.AndroidClipboardManager
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.focus.FocusModifierImpl
import androidx.ui.core.hapticfeedback.AndroidHapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.pointerinput.MotionEventAdapter
import androidx.ui.core.pointerinput.PointerInputEventProcessor
import androidx.ui.core.pointerinput.ProcessResult
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.core.semantics.SemanticsModifierCore
import androidx.ui.core.semantics.SemanticsOwner
import androidx.ui.core.semantics.getAllSemanticsNodesToMap
import androidx.ui.core.semantics.getOrNull
import androidx.ui.core.text.AndroidFontResourceLoader
import androidx.ui.core.texttoolbar.AndroidTextToolbar
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.core.focus.FocusDetailedState.Active
import androidx.ui.core.focus.FocusDetailedState.Inactive
import androidx.ui.core.keyinput.Key
import androidx.ui.core.keyinput.KeyEvent
import androidx.ui.core.keyinput.KeyEventType.KeyDown
import androidx.ui.core.keyinput.KeyEventType.KeyUp
import androidx.ui.core.keyinput.KeyInputModifier
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.CanvasHolder
import androidx.ui.input.TextInputServiceAndroid
import androidx.ui.input.textInputServiceFactory
import androidx.ui.savedinstancestate.UiSavedStateRegistry
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.util.fastForEach
import androidx.ui.util.trace
import java.lang.reflect.Method

/***
 * This function creates an instance of [AndroidOwner]
 *
 * @param context Context to use to create a View
 * @param lifecycleOwner Current [LifecycleOwner]. When it is not provided we will try to get the
 * owner using [ViewTreeLifecycleOwner] when we will be attached.
 */
fun AndroidOwner(
    context: Context,
    lifecycleOwner: LifecycleOwner? = null
): AndroidOwner = AndroidComposeView(context, lifecycleOwner).also {
    AndroidOwner.onAndroidOwnerCreatedCallback?.invoke(it)
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class AndroidComposeView constructor(
    context: Context,
    lifecycleOwner: LifecycleOwner?
) : ViewGroup(context), AndroidOwner {

    override val view: View = this
    private val accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(this)

    override var density = Density(context)
        private set

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsNode.generateNewId(),
        applyToChildLayoutNode = false,
        mergeAllDescendants = false,
        properties = null
    )
    private val focusModifier = FocusModifierImpl(Inactive)
    private val keyInputModifier = KeyInputModifier(null, null)

    private val canvasHolder = CanvasHolder()

    override val root = LayoutNode().also {
        it.measureBlocks = RootMeasureBlocks
        it.layoutDirection =
            context.applicationContext.resources.configuration.localeLayoutDirection
        it.modifier = Modifier.drawLayer() + semanticsModifier + focusModifier + keyInputModifier
    }

    private inner class SemanticsNodeCopy(
        semanticsNode: SemanticsNode
    ) {
        val config = semanticsNode.config
        val children: MutableSet<Int> = mutableSetOf()

        init {
            semanticsNode.children.fastForEach { child ->
                children.add(child.id)
            }
        }
    }

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)
    private var semanticsNodes: MutableMap<Int, SemanticsNodeCopy> = mutableMapOf()
    private var semanticsRoot = SemanticsNodeCopy(semanticsOwner.rootSemanticsNode)
    private var checkingForSemanticsChanges = false

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

    override fun dispatchWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            focusModifier.focusDetailedState = Active
            // TODO(b/152535715): propagate focus to children based on child focusability.
        } else {
            // If this view lost focus, clear focus from the children. For now we clear focus
            // from the children by requesting focus on the parent.
            // TODO(b/151335411): use clearFocus() instead.
            focusModifier.apply {
                requestFocus()
                focusDetailedState = Inactive
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: AndroidKeyEvent): Boolean {
        val keyEvent = KeyEvent(Key(event.keyCode), KeyUp)
        return keyInputModifier.processKeyInput(keyEvent)
    }

    override fun onKeyDown(keyCode: Int, event: AndroidKeyEvent): Boolean {
        val keyEvent = KeyEvent(Key(event.keyCode), KeyDown)
        return keyInputModifier.processKeyInput(keyEvent)
    }

    private val modelObserver = ModelObserver { command ->
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

    override var showLayoutBounds = false

    override fun pauseModelReadObserveration(block: () -> Unit) =
        modelObserver.pauseObservingReads(block)

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
        modelObserver.clear(node)
    }

    private val androidViewsHandler by lazy(LazyThreadSafetyMode.NONE) {
        AndroidViewsHandler(context).also { addView(it) }
    }
    private val viewLayersContainer by lazy(LazyThreadSafetyMode.NONE) {
        ViewLayerContainer(context).also { addView(it) }
    }

    override fun addAndroidView(view: View, layoutNode: LayoutNode) {
        androidViewsHandler.addView(view)
        androidViewsHandler.layoutNode[view] = layoutNode
    }

    override fun removeAndroidView(view: View) {
        androidViewsHandler.removeView(view)
        androidViewsHandler.layoutNode.remove(view)
    }

    // [ Layout block start ]

    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    private val measureAndLayoutHandler: Handler =
        HandlerCompat.createAsync(Looper.getMainLooper()) {
            measureAndLayout()
            true
        }

    private fun scheduleMeasureAndLayout() {
        if (root.needsRemeasure) {
            requestLayout()
        } else {
            measureAndLayoutHandler.removeMessages(0)
            measureAndLayoutHandler.sendEmptyMessage(0)
        }
    }

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

    override fun measureAndLayout() {
        measureAndLayoutDelegate.measureAndLayout()
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        savedStateDelegate.stopWaitingForStateRestoration()
        trace("AndroidOwner:onMeasure") {
            val (minWidth, maxWidth) = convertMeasureSpec(widthMeasureSpec)
            val (minHeight, maxHeight) = convertMeasureSpec(heightMeasureSpec)

            measureAndLayoutDelegate.rootConstraints =
                Constraints(minWidth, maxWidth, minHeight, maxHeight)
            measureAndLayoutDelegate.measureAndLayout()
            setMeasuredDimension(root.width.value, root.height.value)
        }
    }

    private fun convertMeasureSpec(measureSpec: Int): Pair<IntPx, IntPx> {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = IntPx(MeasureSpec.getSize(measureSpec))
        return when (mode) {
            MeasureSpec.EXACTLY -> size to size
            MeasureSpec.UNSPECIFIED -> IntPx.Zero to IntPx.Infinity
            MeasureSpec.AT_MOST -> IntPx.Zero to size
            else -> throw IllegalStateException()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // we postpone onPositioned callbacks until onLayout as LayoutCoordinates
        // are currently wrong if you try to get the global(activity) coordinates -
        // View is not yet laid out.
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    // [ Layout block end ]

    override fun observeLayoutModelReads(node: LayoutNode, block: () -> Unit) {
        modelObserver.observeReads(node, onCommitAffectingLayout, block)
    }

    override fun observeMeasureModelReads(node: LayoutNode, block: () -> Unit) {
        modelObserver.observeReads(node, onCommitAffectingMeasure, block)
    }

    fun observeLayerModelReads(layer: OwnedLayer, block: () -> Unit) {
        modelObserver.observeReads(layer, onCommitAffectingLayer, block)
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
        if ((context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                    as AccessibilityManager).isEnabled && !checkingForSemanticsChanges
        ) {
            checkingForSemanticsChanges = true
            Handler(Looper.getMainLooper()).post {
                checkForSemanticsChanges()
                checkingForSemanticsChanges = false
            }
        }
    }

    private fun updateLayerProperties(layer: OwnedLayer) {
        modelObserver.observeReads(layer, onCommitAffectingLayerParams) {
            layer.updateLayerProperties()
        }
    }

    private fun checkForSemanticsChanges() {
        val newSemanticsNodes = semanticsOwner.getAllSemanticsNodesToMap()

        // Structural change
        sendSemanticsStructureChangeEvents(semanticsOwner.rootSemanticsNode, semanticsRoot)

        // Property change
        for (id in newSemanticsNodes.keys) {
            if (semanticsNodes.contains(id)) {
                // We do doing this search because the new configuration is set as a whole, so we
                // can't indicate which property is changed when setting the new configuration.
                var newNode = newSemanticsNodes[id]
                var oldNode = semanticsNodes[id]
                for (entry in newNode!!.config) {
                    if (entry.value == oldNode!!.config.getOrNull(entry.key)) {
                        continue
                    }
                    when (entry.key) {
                        // we are in aosp, so can't use the state description yet.
                        SemanticsProperties.AccessibilityValue,
                        SemanticsProperties.AccessibilityLabel ->
                            accessibilityDelegate.sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION,
                                entry.value as CharSequence
                            )
                        else -> {
                            // TODO(b/151840490) send the correct events when property changes
                        }
                    }
                }
            }
        }

        // Update the cache
        semanticsNodes.clear()
        for (entry in newSemanticsNodes.entries) {
            semanticsNodes[entry.key] = SemanticsNodeCopy(entry.value)
        }
        semanticsRoot = SemanticsNodeCopy(semanticsOwner.rootSemanticsNode)
    }

    private fun sendSemanticsStructureChangeEvents(
        newNode: SemanticsNode,
        oldNode: SemanticsNodeCopy
    ) {
        var newChildren: MutableSet<Int> = mutableSetOf()

        // If any child is added, clear the subtree rooted at this node and return.
        newNode.children.fastForEach { child ->
            if (!oldNode.children.contains(child.id)) {
                accessibilityDelegate.sendEventForVirtualView(
                    semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
                    null
                )
                return
            }
            newChildren.add(child.id)
        }

        // If any child is deleted, clear the subtree rooted at this node and return.
        for (child in oldNode.children) {
            if (!newChildren.contains(child)) {
                accessibilityDelegate.sendEventForVirtualView(
                    semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
                    null
                )
                return
            }
        }

        newNode.children.fastForEach { child ->
            sendSemanticsStructureChangeEvents(child, semanticsNodes[child.id]!!)
        }
    }

    private fun semanticsNodeIdToAccessibilityVirtualNodeId(id: Int): Int {
        if (id == semanticsOwner.rootSemanticsNode.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
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

    override var lifecycleOwner: LifecycleOwner? = lifecycleOwner
        private set

    private var onLifecycleAvailable: ((LifecycleOwner) -> Unit)? = null

    override fun setOnLifecycleOwnerAvailable(callback: (LifecycleOwner) -> Unit) {
        require(lifecycleOwner == null) { "LifecycleOwner is already available" }
        onLifecycleAvailable = callback
    }

    // Workaround for the cases when we don't have a real LifecycleOwner, this happens when
    // ViewTreeLifecycleOwner.get(this) returned null:
    // 1) we are in AppCompatActivity and there is a bug for(should be fixed soon)
    // 2) we are in a regular Activity. once we fix bug in AppCompatActivity we stop support it.
    private val viewLifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle() = lifecycleRegistry
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showLayoutBounds = getIsShowingLayoutBounds()
        modelObserver.enableModelUpdatesObserving(true)
        ifDebug { if (autofillSupported()) _autofill?.registerCallback() }
        root.attach(this)
        if (lifecycleOwner == null) {
            lifecycleOwner = ViewTreeLifecycleOwner.get(this) ?: viewLifecycleOwner
        }
        onLifecycleAvailable?.invoke(lifecycleOwner!!)
        onLifecycleAvailable = null
        viewLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        modelObserver.enableModelUpdatesObserving(false)
        ifDebug { if (autofillSupported()) _autofill?.unregisterCallback() }
        root.detach()
        viewLifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
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

    override val textInputService = textInputServiceFactory(textInputServiceAndroid)

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

    override fun calculatePosition(): IntPxPosition {
        val positionArray = intArrayOf(0, 0)
        getLocationOnScreen(positionArray)
        return IntPxPosition(positionArray[0].ipx, positionArray[1].ipx)
    }

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
                    measurables.isEmpty() -> measureScope.layout(IntPx.Zero, IntPx.Zero) {}
                    measurables.size == 1 -> {
                        val placeable = measurables[0].measure(constraints, layoutDirection)
                        measureScope.layout(placeable.width, placeable.height) {
                            placeable.place(IntPx.Zero, IntPx.Zero)
                        }
                    }
                    else -> {
                        val placeables = measurables.map {
                            it.measure(constraints, layoutDirection)
                        }
                        var maxWidth = IntPx.Zero
                        var maxHeight = IntPx.Zero
                        placeables.fastForEach { placeable ->
                            maxWidth = max(placeable.width, maxWidth)
                            maxHeight = max(placeable.height, maxHeight)
                        }
                        measureScope.layout(maxWidth, maxHeight) {
                            placeables.fastForEach { placeable ->
                                placeable.place(IntPx.Zero, IntPx.Zero)
                            }
                        }
                    }
                }
            }

            override fun minIntrinsicWidth(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun minIntrinsicHeight(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicWidth(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                h: IntPx,
                layoutDirection: LayoutDirection
            ) = error("Undefined intrinsics block and it is required")

            override fun maxIntrinsicHeight(
                intrinsicMeasureScope: IntrinsicMeasureScope,
                measurables: List<IntrinsicMeasurable>,
                w: IntPx,
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
