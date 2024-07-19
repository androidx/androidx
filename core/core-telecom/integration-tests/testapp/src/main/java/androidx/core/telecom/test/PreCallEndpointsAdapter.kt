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

package androidx.core.telecom.test

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.recyclerview.widget.RecyclerView

@RequiresApi(26)
class PreCallEndpointsAdapter(private var mCurrentEndpoints: ArrayList<CallEndpointCompat>?) :
    RecyclerView.Adapter<PreCallEndpointsAdapter.ViewHolder>() {
    var mSelectedCallEndpoint: CallEndpointCompat? = null

    companion object {
        val TAG: String = PreCallEndpointsAdapter::class.java.simpleName
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        // TextViews
        val endpointName: TextView = itemView.findViewById(R.id.endpoint_name)
        val endpointType: TextView = itemView.findViewById(R.id.endpoint_type_id)
        val endpointUuid: TextView = itemView.findViewById(R.id.endpoint_uuid_id)
        // Call State Buttons
        val selectButton: Button = itemView.findViewById(R.id.select_endpoint_id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view that is used to hold list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.endpoint_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mCurrentEndpoints?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val callEndpointCompat = mCurrentEndpoints?.get(position)
        if (callEndpointCompat != null) {
            holder.endpointName.text = callEndpointCompat.name
            holder.endpointType.text = callEndpointCompat.type.toString()
            holder.endpointUuid.text = callEndpointCompat.identifier.toString()

            holder.selectButton.setOnClickListener {
                Log.i(TAG, "selected: preCallEndpoint=[${callEndpointCompat}]")
                mSelectedCallEndpoint = callEndpointCompat
            }
        }
    }
}
