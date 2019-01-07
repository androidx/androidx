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

package com.example.androidx.viewpager2

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_mutable_collection.buttonAddAfter
import kotlinx.android.synthetic.main.activity_mutable_collection.buttonAddBefore
import kotlinx.android.synthetic.main.activity_mutable_collection.buttonGoTo
import kotlinx.android.synthetic.main.activity_mutable_collection.buttonRemove
import kotlinx.android.synthetic.main.activity_mutable_collection.itemSpinner
import kotlinx.android.synthetic.main.activity_mutable_collection.viewPager

/**
 * Shows how to use notifyDataSetChanged with [ViewPager2]
 */
abstract class MutableCollectionBaseActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mutable_collection)

        viewPager.adapter = createViewPagerAdapter()

        itemSpinner.adapter = object : BaseAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                ((convertView as TextView?) ?: TextView(parent.context)).apply {
                    text = getItem(position)
                }

            override fun getItem(position: Int): String = items[getItemId(position)]
            override fun getItemId(position: Int): Long = items.itemId(position)
            override fun getCount(): Int = items.size
        }

        buttonGoTo.setOnClickListener {
            viewPager.setCurrentItem(itemSpinner.selectedItemPosition, true)
        }

        buttonRemove.setOnClickListener {
            items.removeAt(itemSpinner.selectedItemPosition)
            notifyDataSetChanged()
        }

        buttonAddBefore.setOnClickListener {
            items.addNewAt(itemSpinner.selectedItemPosition)
            notifyDataSetChanged()
        }

        buttonAddAfter.setOnClickListener {
            items.addNewAt(itemSpinner.selectedItemPosition + 1)
            notifyDataSetChanged()
        }
    }

    abstract fun createViewPagerAdapter(): RecyclerView.Adapter<*>

    val items: Items get() = ViewModelProviders.of(this)[Items::class.java]

    private fun notifyDataSetChanged() {
        viewPager.adapter.notifyDataSetChanged()
        (itemSpinner.adapter as BaseAdapter).notifyDataSetChanged()
    }
}

/** A very simple collection of items. Optimized for simplicity (i.e. not performance). */
class Items : ViewModel() {
    private var nextValue = 1L

    private val items = (1..9).map { longToItem(nextValue++) }.toMutableList()
    private val clickCount = mutableMapOf<Long, Int>()

    operator fun get(id: Long): String = items.first { itemToLong(it) == id }
    fun itemId(position: Int): Long = itemToLong(items[position])
    fun contains(itemId: Long): Boolean = items.any { itemToLong(it) == itemId }
    fun addNewAt(position: Int) = items.add(position, longToItem(nextValue++))
    fun removeAt(position: Int) = items.removeAt(position)
    fun clickCount(itemId: Long): Int = clickCount[itemId] ?: 0
    fun registerClick(itemId: Long) = clickCount.set(itemId, 1 + clickCount(itemId))
    val size: Int get() = items.size

    private fun longToItem(value: Long): String = "item#$value"
    private fun itemToLong(value: String): Long = value.split("#")[1].toLong()
}
