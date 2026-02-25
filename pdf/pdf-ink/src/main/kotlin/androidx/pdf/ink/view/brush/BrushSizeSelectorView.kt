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

package androidx.pdf.ink.view.brush

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.pdf.ink.R
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.SliderOrientation

/**
 * A composite [android.view.ViewGroup] for selecting a brush size.
 *
 * It features a Material Slider with discrete steps and a BrushPreviewView that visually represents
 * the selected brush's thickness.
 */
internal class BrushSizeSelectorView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {

    val brushSizeSlider: Slider
    val brushPreviewView: BrushPreviewView

    init {
        setupContainerView()

        brushSizeSlider = createSliderView()
        brushPreviewView = createBrushPreviewView()

        addView(brushSizeSlider)
        addView(brushPreviewView)
    }

    override fun setOrientation(orientation: Int) {
        if (this.orientation == orientation) return
        super.setOrientation(orientation)

        brushSizeSlider.applyOrientation(orientation)

        // Reorder views based on orientation
        removeAllViews()
        if (orientation == VERTICAL) {
            addView(brushPreviewView)
            addView(brushSizeSlider)
        } else {
            addView(brushSizeSlider)
            addView(brushPreviewView)
        }
    }

    private fun setupContainerView() {
        orientation = HORIZONTAL
        background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                color =
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            this@BrushSizeSelectorView,
                            MaterialR.attr.colorSurfaceContainerLowest,
                            ContextCompat.getColor(
                                context,
                                R.color.default_brush_size_selector_background_color,
                            ),
                        )
                    )
                cornerRadius = context.resources.getDimension(R.dimen.corner_radius_20dp)
            }

        val defaultPadding = context.resources.getDimensionPixelSize(R.dimen.padding_8dp)
        setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun createSliderView(): Slider =
        Slider(context).apply {
            applyOrientation(orientation)
            // By default, there are only 5 steps sizes
            valueFrom = 0f
            valueTo = 4f
            stepSize = 1f
            labelBehavior = LabelFormatter.LABEL_GONE
            contentDescription = context.getString(R.string.pdf_brush_slider_content_description)
            isFocusableInTouchMode = true
        }

    private fun createBrushPreviewView(): BrushPreviewView =
        BrushPreviewView(context).apply {
            layoutParams =
                LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.brush_preview_width),
                    resources.getDimensionPixelSize(R.dimen.brush_preview_height),
                )
            // Brush preview is for static visual appearance; mark it unfocusable
            focusable = NOT_FOCUSABLE
            gravity = Gravity.CENTER
        }

    private fun Slider.applyOrientation(orientation: Int) {
        when (orientation) {
            VERTICAL -> {
                layoutParams =
                    LayoutParams(LayoutParams.WRAP_CONTENT, 0).also {
                        // Let slider occupy the remaining height of the container
                        it.weight = 1f
                    }
                setOrientation(SliderOrientation.VERTICAL)
            }
            HORIZONTAL -> {
                layoutParams =
                    LayoutParams(0, LayoutParams.WRAP_CONTENT).also {
                        // Let slider occupy the remaining width of the container
                        it.weight = 1f
                    }
                setOrientation(SliderOrientation.HORIZONTAL)
            }
        }
    }
}
