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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.item_mutable_collection.view.buttonCountIncrease
import kotlinx.android.synthetic.main.item_mutable_collection.view.textViewCount
import kotlinx.android.synthetic.main.item_mutable_collection.view.textViewItemId

/**
 * Shows how to use [RecyclerView.Adapter.notifyDataSetChanged] with [ViewPager2]
 */
class MutableCollectionViewActivity : MutableCollectionBaseActivity() {
    override fun createViewPagerAdapter(): RecyclerView.Adapter<*> {
        val items = items // avoids resolving the ViewModel multiple times
        return object : RecyclerView.Adapter<PageViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, type: Int) = PageViewHolder(parent)
            override fun onBindViewHolder(holder: PageViewHolder, position: Int) = holder.bind()
            override fun getItemCount(): Int = items.size
            override fun getItemId(position: Int): Long = items.itemId(position)
        }.apply { setHasStableIds(true) }
    }
}

class PageViewHolder(parent: ViewGroup) :
    RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_mutable_collection, parent, false)
    ) {

    fun bind() {
        val items = (itemView.context as MutableCollectionBaseActivity).items
        itemView.textViewItemId.text = items[itemId]

        fun updateCountView() {
            itemView.textViewCount.text = "${items.clickCount(itemId)}"
        }
        updateCountView()

        itemView.buttonCountIncrease.setOnClickListener {
            items.registerClick(itemId)
            updateCountView()
        }
    }
}
