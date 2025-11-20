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

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.scenecore.testapp.R
import java.text.SimpleDateFormat
import java.util.Locale

class BuildInfoRecyclerViewAdapter(private val dataSet: Array<String>) :
    RecyclerView.Adapter<BuildInfoRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Define click listener for the ViewHolder's View
        val labelTextView: TextView = view.findViewById(R.id.build_info_label)
        val infoTextView: TextView = view.findViewById(R.id.build_info_text)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.build_info_row_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val buildDateString =
            SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Build.TIME)
        val cLString = "Unavailable"

        viewHolder.labelTextView.text = dataSet[position]
        when (position) {
            0 -> viewHolder.infoTextView.text = Build.FINGERPRINT
            1 -> viewHolder.infoTextView.text = Build.DEVICE
            2 -> viewHolder.infoTextView.text = buildDateString
            3 -> viewHolder.infoTextView.text = cLString
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}
