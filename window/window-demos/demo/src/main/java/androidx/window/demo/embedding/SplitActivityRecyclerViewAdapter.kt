/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.demo.embedding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.window.demo.databinding.ActivitySplitActivityItemButtonLayoutBinding
import androidx.window.demo.databinding.ActivitySplitActivityItemCheckboxLayoutBinding
import androidx.window.demo.databinding.ActivitySplitActivityItemTextviewLayoutBinding
import androidx.window.demo.embedding.SplitActivityRecyclerViewBindingData.Item

/**
 * SplitActivityRecyclerViewAdapter is a custom adapter for binding data to the Split demo's main UI
 * RecyclerView. This class provides the necessary methods to handle view creation, data binding,
 * and item action interactions within the RecyclerView.
 */
class SplitActivityRecyclerViewAdapter(private val items: List<Item>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            Item.TYPE_TEXT -> {
                val binding =
                    ActivitySplitActivityItemTextviewLayoutBinding.inflate(
                        layoutInflater,
                        parent,
                        false,
                    )
                TextViewHolder(binding)
            }
            Item.TYPE_CHECKBOX -> {
                val binding =
                    ActivitySplitActivityItemCheckboxLayoutBinding.inflate(
                        layoutInflater,
                        parent,
                        false,
                    )
                CheckboxViewHolder(binding)
            }
            Item.TYPE_BUTTON -> {
                val binding =
                    ActivitySplitActivityItemButtonLayoutBinding.inflate(
                        layoutInflater,
                        parent,
                        false,
                    )
                ButtonViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        item.listener =
            object : Item.OnItemChangedListener {
                override fun onItemChangedListener() {
                    val index = items.indexOf(item)
                    notifyItemChanged(index)
                }
            }

        when (holder) {
            is TextViewHolder -> holder.bind(item)
            is CheckboxViewHolder -> holder.bind(item)
            is ButtonViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class TextViewHolder(private val binding: ActivitySplitActivityItemTextviewLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.textView.visibility = if (item.isVisible) View.VISIBLE else View.GONE
            binding.textView.text = item.text
            binding.view.visibility = if (item.withDivider) View.VISIBLE else View.GONE
        }
    }

    class CheckboxViewHolder(private val binding: ActivitySplitActivityItemCheckboxLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.itemCheckbox.visibility = if (item.isVisible) View.VISIBLE else View.GONE
            binding.itemCheckbox.isChecked = item.isChecked
            binding.itemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                item.onTriggered()
            }
            binding.itemCheckbox.isEnabled = item.isEnabled
            binding.itemCheckbox.text = item.text
            binding.view.visibility = if (item.withDivider) View.VISIBLE else View.GONE
        }
    }

    class ButtonViewHolder(private val binding: ActivitySplitActivityItemButtonLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.itemButton.visibility = if (item.isVisible) View.VISIBLE else View.GONE
            binding.itemButton.text = item.text
            binding.itemButton.setOnClickListener { item.onTriggered() }
            binding.view.visibility = if (item.withDivider) View.VISIBLE else View.GONE
        }
    }
}
