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

package androidx.viewpager2.integration.testapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * Shows how to use notifyDataSetChanged with [ViewPager2]
 */
abstract class MutableCollectionBaseActivity : FragmentActivity() {
    private lateinit var buttonAddAfter: Button
    private lateinit var buttonAddBefore: Button
    private lateinit var buttonGoTo: Button
    private lateinit var buttonRemove: Button
    private lateinit var itemSpinner: Spinner
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mutable_collection)

        buttonAddAfter = findViewById(R.id.buttonAddAfter)
        buttonAddBefore = findViewById(R.id.buttonAddBefore)
        buttonGoTo = findViewById(R.id.buttonGoTo)
        buttonRemove = findViewById(R.id.buttonRemove)
        itemSpinner = findViewById(R.id.itemSpinner)
        viewPager = findViewById(R.id.viewPager)

        viewPager.adapter = createViewPagerAdapter()

        itemSpinner.adapter = object : BaseAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                ((convertView as TextView?) ?: TextView(parent.context)).apply {
                    text = getItem(position)
                }

            override fun getItem(position: Int): String = items.getItemById(getItemId(position))
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

    val items: ItemsViewModel get() = ViewModelProviders.of(this)[ItemsViewModel::class.java]

    private fun notifyDataSetChanged() {
        viewPager.adapter!!.notifyDataSetChanged()
        (itemSpinner.adapter as BaseAdapter).notifyDataSetChanged()
    }
}

/** A very simple collection of items. Optimized for simplicity (i.e. not performance). */
class ItemsViewModel : ViewModel() {
    private var nextValue = 1L

    private val items = (1..9).map { longToItem(nextValue++) }.toMutableList()

    fun getItemById(id: Long): String = items.first { itemToLong(it) == id }
    fun itemId(position: Int): Long = itemToLong(items[position])
    fun contains(itemId: Long): Boolean = items.any { itemToLong(it) == itemId }
    fun addNewAt(position: Int) = items.add(position, longToItem(nextValue++))
    fun removeAt(position: Int) = items.removeAt(position)
    val size: Int get() = items.size

    private fun longToItem(value: Long): String = "item#$value"
    private fun itemToLong(value: String): Long = value.split("#")[1].toLong()
}
