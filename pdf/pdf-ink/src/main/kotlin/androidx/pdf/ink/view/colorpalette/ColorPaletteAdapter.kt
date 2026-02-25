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

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.annotations.VisibleForTesting

/**
 * An adapter for displaying a list of [PaletteItem]s in a [RecyclerView].
 *
 * @param onItemClicked A lambda function to be invoked when an item in the palette is clicked.
 */
internal class ColorPaletteAdapter(private var onItemClicked: (Int, PaletteItem) -> Unit) :
    ListAdapter<PaletteItem, ColorPaletteAdapter.PaletteItemViewHolder>(PaletteItemDiffCallback) {

    @VisibleForTesting var areAnimationsEnabled: Boolean = true

    // Store the position of the selected item. Initialize to no selection.
    var selectedPosition = RecyclerView.NO_POSITION
        private set

    /** Sets the currently selected item in the palette. */
    fun setSelection(itemPos: Int) {
        notifyItemSelectionChanged(itemPos)
    }

    private fun handleItemClick(position: Int) {
        val item = getItem(position)
        onItemClicked(position, item)

        notifyItemSelectionChanged(position)
    }

    private fun notifyItemSelectionChanged(position: Int) {
        // Optimizes notifying item changed, if same item is selected
        if (position == selectedPosition) return

        val previousPosition = selectedPosition
        selectedPosition = position

        // Notify the adapter to redraw the old and new selected items.
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition, SELECTION_UPDATED)
        }
        notifyItemChanged(selectedPosition, SELECTION_UPDATED)
    }

    /**
     * Creates a new [PaletteItemViewHolder]. The ViewHolder is initialized with a lambda that will
     * handle click events by notifying the adapter.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteItemViewHolder {
        val view = PaletteItemView(context = parent.context)
        return PaletteItemViewHolder(view, this::handleItemClick)
    }

    /**
     * Binds the data from a [PaletteItem] to a [PaletteItemViewHolder]. This is the single source
     * of truth for the view's appearance, based on the current selection state.
     */
    override fun onBindViewHolder(holder: PaletteItemViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition, animate = areAnimationsEnabled)
    }

    override fun onBindViewHolder(
        holder: PaletteItemViewHolder,
        position: Int,
        payloads: List<Any?>,
    ) {
        // Optimizes re-binding a view holder, if only selection is updated
        if (payloads.contains(SELECTION_UPDATED)) {
            holder.updateSelection(
                isSelected = position == selectedPosition,
                animate = areAnimationsEnabled,
            )
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** ViewHolder for a single item in the color palette. */
    internal class PaletteItemViewHolder(
        private val paletteItemView: PaletteItemView,
        onItemClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(paletteItemView) {

        init {
            paletteItemView.setOnClickListener {
                // When clicked, if we have a valid adapter position, call the lambda.
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(bindingAdapterPosition)
                }
            }
        }

        /**
         * Binds a [PaletteItem] to this ViewHolder, updating the view's appearance and selection
         * state.
         */
        fun bind(item: PaletteItem, isSelected: Boolean, animate: Boolean) {
            paletteItemView.setPaletteItem(item)
            paletteItemView.setSelected(selected = isSelected, animate = animate)
            paletteItemView.contentDescription = item.contentDescription
        }

        fun updateSelection(isSelected: Boolean, animate: Boolean) {
            paletteItemView.setSelected(selected = isSelected, animate = animate)
        }

        fun cleanup() {
            paletteItemView.reset()
        }
    }

    override fun onViewRecycled(holder: PaletteItemViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    companion object {
        private const val SELECTION_UPDATED = "SelectionUpdated"
    }
}

/**
 * A [DiffUtil.ItemCallback] for calculating the difference between two [PaletteItem] lists. Since
 * [PaletteItem] subclasses are data classes, we can rely on their structural equality.
 */
private object PaletteItemDiffCallback : DiffUtil.ItemCallback<PaletteItem>() {
    override fun areItemsTheSame(oldItem: PaletteItem, newItem: PaletteItem): Boolean =
        oldItem == newItem

    // The subclasses of PaletteItem are data class that implements .equals() by default.
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: PaletteItem, newItem: PaletteItem): Boolean =
        oldItem == newItem
}
