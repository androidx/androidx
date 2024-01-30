/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3room

import android.graphics.Color
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.paging.integration.testapp.R
import androidx.paging.integration.testapp.room.Customer
import androidx.recyclerview.widget.RecyclerView

class V3RoomAdapter : PagingDataAdapter<Customer, RecyclerView.ViewHolder>(
    diffCallback = Customer.DIFF_CALLBACK
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(TextView(parent.context)) {}
            .apply<RecyclerView.ViewHolder> {
                itemView.minimumHeight = 150
                itemView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            (holder.itemView as TextView).text = item.name
            holder.itemView.setBackgroundColor(Color.BLUE)
        } else {
            (holder.itemView as TextView).setText(R.string.loading)
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}
