/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink.view

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.HandlerCompat
import androidx.core.view.isVisible
import androidx.pdf.ink.R
import androidx.pdf.ink.view.brush.BrushSizeSelectorView
import androidx.pdf.ink.view.brush.model.BrushSizes.highlightBrushSizes
import androidx.pdf.ink.view.brush.model.BrushSizes.penBrushSizes
import androidx.pdf.ink.view.colorpalette.ColorPaletteView
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.colorpalette.model.Emoji
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.pdf.ink.view.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.pdf.ink.view.draganddrop.AnnotationToolbarTouchHandler
import androidx.pdf.ink.view.draganddrop.ToolbarDockState
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.pdf.ink.view.draganddrop.ToolbarDragListener
import androidx.pdf.ink.view.layout.AnnotationToolbarConstraintSet
import androidx.pdf.ink.view.layout.ToolTrayScrollerManager
import androidx.pdf.ink.view.state.AnnotationToolbarState
import androidx.pdf.ink.view.state.ToolbarEffect
import androidx.pdf.ink.view.state.ToolbarInitializer
import androidx.pdf.ink.view.state.ToolbarIntent
import androidx.pdf.ink.view.state.ToolbarIntent.ClearToolSelection
import androidx.pdf.ink.view.tool.AnnotationToolInfo
import androidx.pdf.ink.view.tool.AnnotationToolView
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.ERASER
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.HIGHLIGHTER
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.PEN
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

/**
 * A toolbar that hosts a set of annotation tools for interacting with a PDF document.
 *
 * This custom [android.view.ViewGroup] contains a predefined set of [AnnotationToolView] buttons
 * such as pen, highlighter, eraser, etc. aligned based on the [dockState] set.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationToolbar
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ConstraintLayout(context, attrs, defStyle), ToolbarDockState {

    private val viewModel =
        AnnotationToolbarViewModel(ToolbarInitializer.createInitialState(context))

    /**
     * A [android.view.ViewGroup] containing all the annotation tools button.
     *
     * Custom tools can be dynamically added to this container using [ViewGroup.addView].
     */
    public val toolTray: ViewGroup

    /**
     * Controls the enabled state of the undo button.
     *
     * This property should be updated by an external controller that manages the annotation
     * undo/redo stack.
     */
    public var canUndo: Boolean = false
        set(value) {
            field = value
            viewModel.onAction(ToolbarIntent.UndoAvailabilityChanged(value))
        }

    /**
     * Controls the enabled state of the redo button.
     *
     * This property should be updated by an external controller that manages the annotation
     * undo/redo stack.
     */
    public var canRedo: Boolean = false
        set(value) {
            field = value
            viewModel.onAction(ToolbarIntent.RedoAvailabilityChanged(value))
        }

    /**
     * Returns true if any of the configuration popups (color palette or brush size slider) are
     * currently visible.
     */
    public val isConfigPopupVisible: Boolean
        get() = with(viewModel.state.value) { showColorPalette || showBrushSizeSlider }

    /** Dismisses any currently visible popups (such as the color palette or brush size slider). */
    public fun dismissPopups() {
        viewModel.onAction(ToolbarIntent.DismissPopups)
    }

    private var annotationToolbarListener: AnnotationToolbarListener? = null

    /** Set the listener for [AnnotationToolbar] events. */
    public fun setAnnotationToolbarListener(listener: AnnotationToolbarListener?) {
        annotationToolbarListener = listener
    }

    /** Clears any selection of tools on [AnnotationToolbar]. No-op if no tool is selected. */
    public fun clearToolSelection() {
        viewModel.onAction(ClearToolSelection)
    }

    /** Reset the [AnnotationToolbar] to its initial state. */
    public fun reset() {
        viewModel.updateState(ToolbarInitializer.createInitialState(context = context))
    }

    override var dockState: Int
        get() = viewModel.state.value.dockedState
        set(value) {
            if (viewModel.state.value.dockedState == value) return

            viewModel.onAction(ToolbarIntent.DockStateChanged(value))
        }

    /**
     * Sets a listener to receive drag events when the toolbar is being moved and docked.
     *
     * @param dragListener The [ToolbarDragListener] to be notified of drag lifecycle events.
     */
    public fun setOnToolbarDragListener(dragListener: ToolbarDragListener) {
        toolbarTouchHandler.setOnDragListener(dragListener)
    }

    private val constraintSet = AnnotationToolbarConstraintSet(this.context)

    private val pen: AnnotationToolView
    private val highlighter: AnnotationToolView
    private val eraser: AnnotationToolView
    private val colorPaletteButton: AnnotationToolView
    private val undo: AnnotationToolView
    private val redo: AnnotationToolView
    private val toggleAnnotation: AnnotationToolView
    private val collapsedIcon: AnnotationToolView
    private val undoRedoContainer: LinearLayout

    private val brushSizeSelectorView: BrushSizeSelectorView

    private val colorPaletteView: ColorPaletteView

    private var whileAttachedToVisibleWindowJob: Job? = null

    private val penPaletteItems = getPenPaletteItems(context)
    private val highlighterPaletteItems = getHighlightPaletteItems(context)

    private val iconFillDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.color_palette_icon_fill)?.mutate()
    }
    private val iconStrokeDrawable: Drawable? by lazy {
        ContextCompat.getDrawable(context, R.drawable.color_palette_icon_stroke)?.mutate()
    }

    private val toolbarScroller: ToolTrayScrollerManager

    private val toolbarTouchHandler: AnnotationToolbarTouchHandler by lazy {
        AnnotationToolbarTouchHandler(this) { event ->
            // Intercepting a long press during a slide on the brush size selector is unintended.
            // Ignore long press detection when the touch target is the brush size selector.
            (brushSizeSelectorView.isVisible && brushSizeSelectorView.isTouchInView(event))
        }
    }

    // Required to disable any animation while performing espresso/screenshot tests
    @VisibleForTesting
    internal var areAnimationsEnabled: Boolean = true
        set(value) {
            field = value
            toolbarTouchHandler.areAnimationsEnabled = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.annotation_toolbar, this, true)
        background = context.getDrawable(R.drawable.annotation_toolbar_background)

        toolTray = findViewById(R.id.tool_tray)
        toolbarScroller = ToolTrayScrollerManager(this, toolTray)
        brushSizeSelectorView = findViewById(R.id.brush_size_selector)
        colorPaletteView = findViewById(R.id.color_palette)
        pen = findViewById(R.id.pen_button)
        highlighter = findViewById(R.id.highlighter_button)
        eraser = findViewById(R.id.eraser_button)
        colorPaletteButton = findViewById(R.id.color_palette_button)
        undo = findViewById(R.id.undo_button)
        redo = findViewById(R.id.redo_button)
        toggleAnnotation = findViewById(R.id.toggle_annotation_button)
        collapsedIcon = findViewById(R.id.collapsed_tool)
        undoRedoContainer = findViewById(R.id.undo_redo_container)

        setupChildViews()
    }

    /**
     * Expands the toolbar to show the full set of tools.
     *
     * The visual change is animated if animations are enabled.
     */
    public fun expandToolbar() {
        viewModel.onAction(ToolbarIntent.ExpandToolbar)
    }

    /**
     * Collapses the toolbar to a single icon.
     *
     * The visual change is animated if animations are enabled.
     */
    public fun collapseToolbar() {
        viewModel.onAction(ToolbarIntent.CollapseToolbar)
    }

    private fun setupChildViews() {
        setupToolTray()
        setupBrushSizeSlider()
        setupColorPalette()
    }

    private fun setupToolTray() {
        // Set click listeners for all tool views
        pen.setOnClickListener { viewModel.onAction(ToolbarIntent.PenToolClicked) }
        highlighter.setOnClickListener { viewModel.onAction(ToolbarIntent.HighlighterToolClicked) }
        eraser.setOnClickListener { viewModel.onAction(ToolbarIntent.EraserToolClicked) }
        colorPaletteButton.setOnClickListener {
            viewModel.onAction(ToolbarIntent.ToggleColorPalette)
        }
        undo.setOnClickListener { viewModel.onAction(ToolbarIntent.UndoClicked) }
        redo.setOnClickListener { viewModel.onAction(ToolbarIntent.RedoClicked) }
        toggleAnnotation.setOnClickListener {
            viewModel.onAction(ToolbarIntent.ToggleAnnotationVisibility)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        whileAttachedToVisibleWindowJob?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        whileAttachedToVisibleWindowJob?.cancel()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        val toJoin = whileAttachedToVisibleWindowJob?.apply { cancel() }
        if (visibility != VISIBLE) {
            whileAttachedToVisibleWindowJob = null
        } else {
            whileAttachedToVisibleWindowJob =
                CoroutineScope(HandlerCompat.createAsync(handler.looper).asCoroutineDispatcher())
                    .launch(start = CoroutineStart.UNDISPATCHED) {
                        // Don't let two copies of this run concurrently
                        toJoin?.join()
                        collectUiStates()
                    }
        }
    }

    private suspend fun collectUiStates() = coroutineScope {
        var lastDockedState: Int? = null
        launch {
            viewModel.state.collect { state ->
                addAnimatorIfRequired(state)

                if (state.dockedState != lastDockedState) {
                    updateDockState(state.dockedState)
                    lastDockedState = state.dockedState
                }

                updateExpandedState(state.isExpanded)

                pen.isSelected = state.selectedTool == PEN
                highlighter.isSelected = state.selectedTool == HIGHLIGHTER
                eraser.isSelected = state.selectedTool == ERASER

                updateColorPaletteIcon(state)
                updateBrushSlider(state)
                updateColorPalette(state)

                // Enable tools only if annotations are enabled on toolbar
                pen.isEnabled = state.isAnnotationVisible
                highlighter.isEnabled = state.isAnnotationVisible
                eraser.isEnabled = state.isAnnotationVisible
                // Undo/Redo will be enabled based on current stack of annotations
                undo.isEnabled = state.canUndo && state.isAnnotationVisible
                redo.isEnabled = state.canRedo && state.isAnnotationVisible

                toggleAnnotation.isSelected = !state.isAnnotationVisible
            }
        }

        launch {
            viewModel.effects.collect {
                when (it) {
                    is ToolbarEffect.ToolUpdated -> {
                        annotationToolbarListener?.onToolChanged(it.toolInfo)
                    }
                    is ToolbarEffect.Undo -> {
                        annotationToolbarListener?.onUndo()
                    }

                    is ToolbarEffect.Redo -> {
                        annotationToolbarListener?.onRedo()
                    }
                    is ToolbarEffect.AnnotationVisibilityChanged -> {
                        annotationToolbarListener?.onAnnotationVisibilityChanged(it.isVisible)
                    }
                }
            }
        }
    }

    private fun addAnimatorIfRequired(state: AnnotationToolbarState) {
        if (!areAnimationsEnabled) return

        val isToolbarCurrentlyExpanded = toolTray.isVisible
        val isColorPaletteCurrentlyVisible = colorPaletteView.isVisible
        val isBrushSizeSliderVisible = brushSizeSelectorView.isVisible

        if (
            isToolbarCurrentlyExpanded != state.isExpanded ||
                isColorPaletteCurrentlyVisible != state.showColorPalette ||
                isBrushSizeSliderVisible != state.showBrushSizeSlider
        )
            addTransitionAnimation()
    }

    private fun setupBrushSizeSlider() {
        brushSizeSelectorView.brushSizeSlider.addOnChangeListener { _, value, _ ->
            viewModel.onAction(ToolbarIntent.BrushSizeChanged(value.roundToInt()))
        }
    }

    private fun setupColorPalette() {
        colorPaletteView.setPaletteItemSelectedListener(
            object : ColorPaletteView.PaletteItemSelectedListener {
                override fun onItemSelected(index: Int, paletteItem: PaletteItem) {
                    viewModel.onAction(ToolbarIntent.ColorSelected(index, paletteItem))
                }
            }
        )
    }

    private fun updateBrushSlider(state: AnnotationToolbarState) {
        if (state.showBrushSizeSlider) {
            var selectedBrushSizeIndex = 0
            var brushPreviewSize = 0f
            when (state.selectedTool) {
                PEN -> {
                    selectedBrushSizeIndex = viewModel.state.value.penState.selectedBrushSizeIndex
                    brushPreviewSize = penBrushSizes[selectedBrushSizeIndex].toPx(context)
                }

                HIGHLIGHTER -> {
                    selectedBrushSizeIndex =
                        viewModel.state.value.highlighterState.selectedBrushSizeIndex
                    brushPreviewSize = highlightBrushSizes[selectedBrushSizeIndex].toPx(context)
                }
            }

            with(brushSizeSelectorView) {
                brushSizeSlider.value = selectedBrushSizeIndex.toFloat()
                brushPreviewView.brushSize = brushPreviewSize
            }
        }

        brushSizeSelectorView.isVisible = state.showBrushSizeSlider
    }

    private fun updateColorPalette(state: AnnotationToolbarState) {
        // Update palette items regardless of visibility to prevent stale content flash.
        when (state.selectedTool) {
            PEN -> {
                val currentSelectedIndex = state.penState.selectedColorIndex
                colorPaletteView.updatePaletteItems(
                    paletteItems = penPaletteItems,
                    currentSelectedIndex = currentSelectedIndex,
                )
            }

            HIGHLIGHTER -> {
                val selectedItemIndex = state.highlighterState.selectedColorIndex
                colorPaletteView.updatePaletteItems(
                    paletteItems = highlighterPaletteItems,
                    currentSelectedIndex = selectedItemIndex,
                )
            }
        }

        colorPaletteView.isVisible = state.showColorPalette
    }

    private fun updateColorPaletteIcon(state: AnnotationToolbarState) {
        colorPaletteButton.isEnabled = state.isAnnotationVisible && state.isColorPaletteEnabled
        colorPaletteButton.isChecked = state.showColorPalette

        val paletteItem =
            when (state.selectedTool) {
                PEN -> state.penState.paletteItem
                HIGHLIGHTER -> state.highlighterState.paletteItem
                else -> null
            }

        when (paletteItem) {
            is Color -> {
                updateIconColor(paletteItem.color)
            }

            is Emoji -> {
                colorPaletteButton.icon = ContextCompat.getDrawable(context, paletteItem.emoji)
                colorPaletteButton.iconTint = null
            }
        }
    }

    private fun updateIconColor(dynamicColor: Int) {
        val fill = iconFillDrawable ?: return
        val stroke = iconStrokeDrawable ?: return

        DrawableCompat.setTint(fill, dynamicColor)

        val layers = arrayOf(fill, stroke)
        val colorPaletteVector = LayerDrawable(layers)

        colorPaletteButton.apply {
            // By converting the LayerDrawable to a Bitmap, we "flatten" it into a static image.
            // This prevents the MaterialButton from overriding our custom colors with its own
            // theme-based tinting logic.
            icon = BitmapDrawable(resources, colorPaletteVector.toBitmap())
            iconTint = null
        }
    }

    private fun addTransitionAnimation() {
        val transition =
            TransitionSet()
                .apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    duration = TRANSITION_ANIMATION_DURATION
                    interpolator = DecelerateInterpolator()

                    addTransition(
                        Fade().apply {
                            addTarget(collapsedIcon)
                            addTarget(toolTray)
                        }
                    )

                    addTransition(
                        Fade(Fade.IN).apply {
                            addTarget(colorPaletteView)
                            addTarget(brushSizeSelectorView)
                            // Delay the appearance of popups to allow the toolbar background
                            // to expand first, ensuring a smoother entrance.
                            startDelay = FADE_IN_TRANSITION_DELAY
                        }
                    )

                    addTransition(
                        ChangeBounds().apply {
                            // Animate bound changes for the parent background
                            addTarget(this@AnnotationToolbar)

                            // Target the tool tray to lock its position and prevent drifting
                            // during the parent container's resize animation.
                            addTarget(R.id.scrollable_tool_tray_container)
                        }
                    )
                }
                .setListener(
                    onEnd = {
                        if (brushSizeSelectorView.isVisible) {
                            val slider = brushSizeSelectorView.brushSizeSlider
                            slider.post { slider.requestFocus() }
                        }
                        if (colorPaletteView.isVisible) {
                            colorPaletteView.requestFocusOnSelectedItem()
                        }
                    },
                    onCancel = {
                        /**
                         * If a new animation starts on the [AnnotationToolbar] before a previous
                         * one finishes, the existing transition is canceled. As a defensive
                         * mechanism, update views to their final visibility.
                         */
                        with(viewModel.state.value) {
                            colorPaletteView.isVisible = showColorPalette
                            brushSizeSelectorView.isVisible = showBrushSizeSlider
                            toolTray.isVisible = isExpanded
                            collapsedIcon.isVisible = !isExpanded
                        }
                    },
                )

        TransitionManager.beginDelayedTransition(this@AnnotationToolbar, transition)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = ToolbarSavedState(superState)
        savedState.toolbarState = viewModel.state.value
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is ToolbarSavedState) {
            super.onRestoreInstanceState(state.superState)
            viewModel.updateState(state.toolbarState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return toolbarTouchHandler.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        return toolbarTouchHandler.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun updateExpandedState(isExpanded: Boolean) {
        toolTray.isVisible = isExpanded
        collapsedIcon.isVisible = !isExpanded
    }

    private fun updateDockState(dockedState: Int) {
        when (dockedState) {
            DOCK_STATE_START -> {
                toolbarScroller.setOrientation(VERTICAL)
                undoRedoContainer.orientation = VERTICAL
                brushSizeSelectorView.orientation = VERTICAL
                constraintSet.dockStateStart.applyTo(this)
            }

            DOCK_STATE_BOTTOM -> {
                toolbarScroller.setOrientation(HORIZONTAL)
                undoRedoContainer.orientation = HORIZONTAL
                brushSizeSelectorView.orientation = HORIZONTAL
                constraintSet.dockStateBottom.applyTo(this)
            }

            DOCK_STATE_END -> {
                toolbarScroller.setOrientation(VERTICAL)
                undoRedoContainer.orientation = VERTICAL
                brushSizeSelectorView.orientation = VERTICAL
                constraintSet.dockStateEnd.applyTo(this)
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when interaction occurs with the
     * [AnnotationToolbar].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface AnnotationToolbarListener {
        /**
         * Called every time the selected tool or its attributes (e.g., color, size) are changed.
         *
         * @param toolInfo An [AnnotationToolInfo] object containing the state of the currently
         *   selected tool.
         */
        public fun onToolChanged(toolInfo: AnnotationToolInfo)

        /** Called when an undo button is clicked if [canUndo] is set to enable. */
        public fun onUndo()

        /** Called when a redo button is clicked if [canRedo] is set to enable. */
        public fun onRedo()

        /**
         * Called when the annotation visibility toggle button is clicked.
         *
         * @param isVisible `true` if annotations are now set to be visible, `false` otherwise.
         */
        public fun onAnnotationVisibilityChanged(isVisible: Boolean)
    }

    private fun Int.toPx(context: Context): Float {
        return this.toFloat() * context.resources.displayMetrics.density
    }

    private inline fun Transition.setListener(
        crossinline onEnd: (transition: Transition) -> Unit = {},
        crossinline onCancel: (transition: Transition) -> Unit = {},
    ): Transition {
        addListener(
            object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) = onEnd(transition)

                override fun onTransitionCancel(transition: Transition) = onCancel(transition)

                override fun onTransitionStart(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionResume(transition: Transition) {}
            }
        )

        return this
    }

    internal companion object {
        private const val TRANSITION_ANIMATION_DURATION = 300L
        private const val FADE_IN_TRANSITION_DELAY = 150L
    }
}

/** Helper function to check if a touch event is within the bounds of a given view. */
internal fun View.isTouchInView(event: MotionEvent): Boolean {
    val viewRect = Rect()
    getHitRect(viewRect)
    return viewRect.contains(event.x.toInt(), event.y.toInt())
}
