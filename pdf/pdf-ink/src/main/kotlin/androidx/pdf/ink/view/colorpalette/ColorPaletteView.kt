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

package androidx.pdf.ink.view.colorpalette

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.pdf.ink.R
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import kotlin.math.max
import org.jetbrains.annotations.VisibleForTesting

/**
 * A custom [RecyclerView] that displays a grid of selectable palette items (colors or emojis) for
 * annotation. The view uses a [GridLayoutManager] to create a dynamic grid of items.
 */
internal class ColorPaletteView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    RecyclerView(context, attrs, defStyleAttr) {

    @VisibleForTesting
    internal var areAnimationsEnabled: Boolean = true
        set(value) {
            field = value
            colorPaletteAdapter.areAnimationsEnabled = value
        }

    private val defaultSpanCount = 7

    private var paletteItemSelectedListener: PaletteItemSelectedListener? = null

    private val colorPaletteAdapter = ColorPaletteAdapter { index, paletteItem ->
        paletteItemSelectedListener?.onItemSelected(index = index, paletteItem = paletteItem)
    }

    init {
        setupRecyclerView()
    }

    /**
     * Updates the grid with a new list of palette items.
     *
     * @param paletteItems The new list of [PaletteItem]s to be displayed.
     * @param currentSelectedIndex The index of the currently selected item in the palette.
     */
    fun updatePaletteItems(paletteItems: List<PaletteItem>, currentSelectedIndex: Int? = null) {
        colorPaletteAdapter.submitList(paletteItems) {
            if (currentSelectedIndex != null) colorPaletteAdapter.setSelection(currentSelectedIndex)
        }
    }

    /**
     * Sets the listener that will be notified when a palette item is selected.
     *
     * @param listener The [PaletteItemSelectedListener] to be invoked upon item selection.
     */
    fun setPaletteItemSelectedListener(listener: PaletteItemSelectedListener) {
        paletteItemSelectedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Recalculate span count when the width changes.
        if (w > 0 && w != oldw) {
            val itemWidth = resources.getDimensionPixelSize(R.dimen.color_palette_item_size)
            val itemSpacing = resources.getDimensionPixelSize(R.dimen.padding_4dp)

            if (itemWidth > 0) {
                val totalItemSpace = itemWidth + itemSpacing

                // To calculate the number of items that can fit, we adjust the available width
                // by adding back one spacing unit because a grid of 'n' items has 'n-1' gaps
                val availableWidth = w - paddingLeft - paddingRight + itemSpacing
                val newSpanCount = max(1, availableWidth / totalItemSpace)
                val lm = layoutManager as? GridLayoutManager ?: return
                if (newSpanCount != lm.spanCount) lm.spanCount = newSpanCount
            }
        }
    }

    fun requestFocusOnSelectedItem() {
        post {
            val selectedIndex = colorPaletteAdapter.selectedPosition
            val viewHolder = findViewHolderForAdapterPosition(selectedIndex)
            viewHolder?.itemView?.let {
                it.isFocusableInTouchMode = true
                it.requestFocus()
            }
        }
    }

    private fun setupRecyclerView() {
        setHasFixedSize(true)

        layoutManager = GridLayoutManager(context, defaultSpanCount)
        adapter = colorPaletteAdapter

        background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                color =
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            this@ColorPaletteView,
                            MaterialR.attr.colorSurfaceContainerLowest,
                            ContextCompat.getColor(
                                context,
                                R.color.default_brush_size_selector_background_color,
                            ),
                        )
                    )
                cornerRadius = context.resources.getDimension(R.dimen.corner_radius_20dp)
            }

        val itemSpacing = resources.getDimensionPixelSize(R.dimen.padding_4dp)
        addItemDecoration(GridSpacingItemDecoration(itemSpacing))
    }

    /** Interface for a callback to be invoked when a palette item is selected. */
    internal interface PaletteItemSelectedListener {
        /**
         * Called when a new item has been selected from the palette.
         *
         * @param index The index of the newly selected item in the palette.
         * @param paletteItem The [PaletteItem] that was selected by the user.
         */
        fun onItemSelected(index: Int, paletteItem: PaletteItem)
    }

    /** An [ItemDecoration] to add equal spacing around all items in a [GridLayoutManager]. */
    private class GridSpacingItemDecoration(private val spacing: Int) : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            val halfSpacing = spacing / 2
            outRect.left = halfSpacing
            outRect.right = halfSpacing
            outRect.top = halfSpacing
            outRect.bottom = halfSpacing
        }
    }
}
