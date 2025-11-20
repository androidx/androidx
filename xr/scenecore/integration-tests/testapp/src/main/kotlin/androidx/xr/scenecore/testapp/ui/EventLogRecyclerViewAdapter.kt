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

package androidx.xr.scenecore.testapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.SpatialEventLog

class EventLogRecyclerViewAdapter(private var dataSet: MutableList<SpatialEventLog>) :
    RecyclerView.Adapter<EventLogRecyclerViewAdapter.EventLogViewHolder>() {

    class EventLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestampTextView: TextView = view.findViewById(R.id.event_timestamp)
        val eventTextView: TextView = view.findViewById(R.id.event_name)
        val logTextView: TextView = view.findViewById(R.id.event_log)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EventLogViewHolder {
        val view =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.event_log_table_item, viewGroup, false)

        return EventLogViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: EventLogViewHolder, position: Int) {
        viewHolder.timestampTextView.text = dataSet[position].timestamp
        viewHolder.eventTextView.text = dataSet[position].eventName
        viewHolder.logTextView.text = dataSet[position].eventLog
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}
