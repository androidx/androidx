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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.pdf.ink.R
import androidx.pdf.ink.view.brush.BrushSizeSelectorView
import androidx.pdf.ink.view.colorpalette.ColorPaletteView
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.pdf.ink.view.state.ToolbarInitializer
import androidx.pdf.ink.view.tool.AnnotationToolInfo
import androidx.pdf.ink.view.tool.AnnotationToolView

/**
 * A toolbar that hosts a set of annotation tools for interacting with a PDF document.
 *
 * This custom [android.view.ViewGroup] contains a predefined set of [AnnotationToolView] buttons
 * such as pen, highlighter, eraser, etc. aligned based on the [LinearLayout.orientation] set.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationToolbar
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ConstraintLayout(context, attrs, defStyle) {

    /**
     * A [android.view.ViewGroup] containing all the annotation tools button.
     *
     * Custom tools can be dynamically added to this container using [LinearLayout.addView].
     */
    public val toolTray: LinearLayout

    /**
     * Controls the enabled state of the undo button.
     *
     * This property should be updated by an external controller that manages the annotation
     * undo/redo stack.
     */
    public var canUndo: Boolean = false
        set(value) {
            field = value
            // TODO(b/448244684) Connect when state holder is implemented
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
            // TODO(b/448244684) Connect when state holder is implemented
        }

    private var annotationToolbarListener: AnnotationToolbarListener? = null

    /** Set the listener for [AnnotationToolbar] events. */
    public fun setAnnotationToolbarListener(listener: AnnotationToolbarListener) {
        annotationToolbarListener = listener
    }

    private val viewmodel =
        AnnotationToolbarViewModel(ToolbarInitializer.createInitialState(context))

    private val pen: AnnotationToolView
    private val highlighter: AnnotationToolView
    private val eraser: AnnotationToolView
    private val colorPaletteButton: AnnotationToolView
    private val undo: AnnotationToolView
    private val redo: AnnotationToolView
    private val toggleAnnotation: AnnotationToolView

    private val brushSizeSelectorView: BrushSizeSelectorView

    private val colorPaletteView: ColorPaletteView

    init {
        LayoutInflater.from(context).inflate(R.layout.annotation_toolbar, this, true)
        background = context.getDrawable(R.drawable.annotation_toolbar_background)

        toolTray = findViewById(R.id.toolbar)
        brushSizeSelectorView = findViewById(R.id.brush_size_selector)
        colorPaletteView = findViewById(R.id.color_palette)
        pen = findViewById(R.id.pen_button)
        highlighter = findViewById(R.id.highlighter_button)
        eraser = findViewById(R.id.eraser_button)
        colorPaletteButton = findViewById(R.id.color_palette_button)
        undo = findViewById(R.id.undo_button)
        redo = findViewById(R.id.redo_button)
        toggleAnnotation = findViewById(R.id.toggle_annotation_button)

        // TODO: Add click listeners for the tools

        setupChildViews()
    }

    private fun setupChildViews() {
        setupToolTray()
        setupBrushSizeSlider()
        setupColorPalette()
    }

    private fun setupToolTray() {
        // default orientation
        toolTray.orientation = HORIZONTAL
    }

    private fun setupBrushSizeSlider() {
        brushSizeSelectorView.brushSizeSlider.addOnChangeListener { _, value, _ ->
            // TODO(b/448244684) Connect when state holder is implemented
        }
    }

    private fun setupColorPalette() {
        colorPaletteView.setPaletteItemSelectedListener(
            object : ColorPaletteView.PaletteItemSelectedListener {
                override fun onItemSelected(index: Int, paletteItem: PaletteItem) {
                    // TODO(b/448244684) Connect when state holder is implemented
                }
            }
        )
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

        /** Called when a undo button is clicked if [canUndo] is set to enabled. */
        public fun onUndo()

        /** Called when a redo button is clicked if [canRedo] is set to enabled. */
        public fun onRedo()

        /**
         * Called when the annotation visibility toggle button is clicked.
         *
         * @param isVisible `true` if annotations are now set to be visible, `false` otherwise.
         */
        public fun onAnnotationVisibilityChanged(isVisible: Boolean)
    }
}
