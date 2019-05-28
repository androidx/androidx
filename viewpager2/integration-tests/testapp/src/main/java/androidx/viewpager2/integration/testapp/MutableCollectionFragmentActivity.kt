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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

private const val KEY_ITEM_TEXT = "androidx.viewpager2.integration.testapp.KEY_ITEM_TEXT"
private const val KEY_CLICK_COUNT = "androidx.viewpager2.integration.testapp.KEY_CLICK_COUNT"

/**
 * Shows how to use [FragmentStateAdapter.notifyDataSetChanged] with [ViewPager2]. Here [ViewPager2]
 * represents pages as [Fragment]s.
 */
class MutableCollectionFragmentActivity : MutableCollectionBaseActivity() {
    override fun createViewPagerAdapter(): RecyclerView.Adapter<*> {
        val items = items // avoids resolving the ViewModel multiple times
        return object : FragmentStateAdapter(this) {
            override fun getItem(position: Int): PageFragment {
                val itemId = items.itemId(position)
                val itemText = items.getItemById(itemId)
                return PageFragment.create(itemText)
            }
            override fun getItemCount(): Int = items.size
            override fun getItemId(position: Int): Long = items.itemId(position)
            override fun containsItem(itemId: Long): Boolean = items.contains(itemId)
        }
    }
}

class PageFragment : Fragment() {
    private lateinit var textViewItemText: TextView
    private lateinit var textViewCount: TextView
    private lateinit var buttonCountIncrease: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.item_mutable_collection, container, false)

        textViewItemText = view.findViewById(R.id.textViewItemText)
        textViewCount = view.findViewById(R.id.textViewCount)
        buttonCountIncrease = view.findViewById(R.id.buttonCountIncrease)

        textViewItemText.text = arguments?.getString(KEY_ITEM_TEXT) ?: throw IllegalStateException()

        fun updateCountText(count: Int) {
            textViewCount.text = "$count"
        }
        updateCountText(savedInstanceState?.getInt(KEY_CLICK_COUNT) ?: 0)

        buttonCountIncrease.setOnClickListener {
            updateCountText(clickCount() + 1)
        }

        return view
    }

    /**
     * [FragmentStateAdapter] minimizes the number of [Fragment]s kept in memory by saving state of
     [Fragment]s that are no longer near the viewport. Here we demonstrate this behavior by relying
     on it to persist click counts through configuration changes (rotation) and data-set changes
     (when items are added or removed).
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_CLICK_COUNT, clickCount())
    }

    private fun clickCount(): Int {
        return "${textViewCount.text}".toInt()
    }

    companion object {
        fun create(itemText: String) =
            PageFragment().apply {
                arguments = Bundle(1).apply {
                    putString(KEY_ITEM_TEXT, itemText)
                }
            }
    }
}
