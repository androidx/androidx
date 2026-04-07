/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.ListView

/**
 * A {@link ListView} which serves as a menu of elements firing {@link Intent}s to other Activities.
 */
class MenuListView(context: Context, attrs: AttributeSet) : ListView(context, attrs) {

    /** An item in the {@link MenuListView}. */
    class MenuItem(private val name: String, private val intentToLaunch: Intent) {

        override fun toString(): String = name

        /**
         * Starts the {@link Intent} for this MenuItem. This accepts the {@link Context} for the
         * current Activity on the stack, which will be used as the {@code this} argument to call
         * {@link Context#startActivity(Intent)}.
         *
         * @param activityContext the Activity Context of the current Activity on the stack.
         */
        fun start(activityContext: Context) {
            activityContext.startActivity(intentToLaunch)
        }
    }

    /** Sets the menu items for this {@link MenuListView}. */
    fun setItems(items: List<MenuItem>) {
        val featureArrayAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
        adapter = featureArrayAdapter
        onItemClickListener = OnItemClickListener { _, _, position: Int, _ ->
            (adapter.getItem(position) as MenuItem).start(context)
        }
    }
}
