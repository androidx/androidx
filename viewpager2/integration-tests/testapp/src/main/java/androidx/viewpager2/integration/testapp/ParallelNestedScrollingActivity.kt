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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL

class ParallelNestedScrollingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewPager = ViewPager2(this).apply {
            layoutParams = matchParent()
            orientation = ORIENTATION_HORIZONTAL
            adapter = VpAdapter()
        }
        setContentView(viewPager)
    }

    class VpAdapter : RecyclerView.Adapter<VpAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return 4
        }

        @SuppressLint("ResourceType")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val root = inflater.inflate(R.layout.item_nested_recyclerviews, parent, false)
            return ViewHolder(root).apply {
                rv1.setUpRecyclerView(RecyclerView.HORIZONTAL)
                rv2.setUpRecyclerView(RecyclerView.VERTICAL)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder) {
                title.text =
                    title.context.getString(R.string.page_position, absoluteAdapterPosition)
                itemView.setBackgroundResource(PAGE_COLORS[position % PAGE_COLORS.size])
            }
        }

        private fun RecyclerView.setUpRecyclerView(orientation: Int) {
            layoutManager = LinearLayoutManager(context, orientation, false)
            adapter = RvAdapter(orientation)
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.page_title)
            val rv1: RecyclerView = itemView.findViewById(R.id.first_rv)
            val rv2: RecyclerView = itemView.findViewById(R.id.second_rv)
        }
    }

    class RvAdapter(private val orientation: Int) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return 40
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context)
            tv.layoutParams = matchParent().apply {
                if (orientation == RecyclerView.HORIZONTAL) {
                    width = WRAP_CONTENT
                } else {
                    height = WRAP_CONTENT
                }
            }
            tv.textSize = 20f
            tv.gravity = Gravity.CENTER
            tv.setPadding(20, 55, 20, 55)
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder) {
                tv.text =
                    tv.context.getString(R.string.item_position, absoluteAdapterPosition)
                tv.setBackgroundResource(CELL_COLORS[position % CELL_COLORS.size])
            }
        }

        class ViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    }
}

internal fun matchParent(): LayoutParams {
    return LayoutParams(MATCH_PARENT, MATCH_PARENT)
}

internal val PAGE_COLORS = listOf(
    R.color.yellow_300,
    R.color.green_300,
    R.color.teal_300,
    R.color.blue_300
)

internal val CELL_COLORS = listOf(
    R.color.grey_100,
    R.color.grey_300
)
