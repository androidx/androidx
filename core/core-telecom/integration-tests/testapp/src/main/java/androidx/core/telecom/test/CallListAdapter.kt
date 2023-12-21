/*
 * Copyright 2023 The Android Open Source Project
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

import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(34)
class CallListAdapter(private var mList: ArrayList<CallRow>?) :
    RecyclerView.Adapter<CallListAdapter.ViewHolder>() {

    var mCallIdToViewHolder: MutableMap<String, ViewHolder> = mutableMapOf()

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        // TextViews
        val callCount: TextView = itemView.findViewById(R.id.callNumber)
        val callIdTextView: TextView = itemView.findViewById(R.id.callIdTextView)
        val currentState: TextView = itemView.findViewById(R.id.callStateTextView)
        val currentEndpoint: TextView = itemView.findViewById(R.id.endpointStateTextView)

        // Call State Buttons
        val activeButton: Button = itemView.findViewById(R.id.activeButton)
        val holdButton: Button = itemView.findViewById(R.id.holdButton)
        val disconnectButton: Button = itemView.findViewById(R.id.disconnectButton)

        // Call Audio Buttons
        val earpieceButton: Button = itemView.findViewById(R.id.earpieceButton)
        val speakerButton: Button = itemView.findViewById(R.id.speakerButton)
        val bluetoothButton: Button = itemView.findViewById(R.id.bluetoothButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.call_row, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList?.size ?: 0
    }

    // Set the data for the user
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ItemsViewModel = mList?.get(position)

        // sets the text to the textview from our itemHolder class
        if (ItemsViewModel != null) {
            mCallIdToViewHolder[ItemsViewModel.callObject.mTelecomCallId] = holder

            holder.callCount.text = "Call # " + ItemsViewModel.callNumber.toString() + "; "
            holder.callIdTextView.text = "ID=[" + ItemsViewModel.callObject.mTelecomCallId + "]"

            holder.activeButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    if (ItemsViewModel.callObject.mCallControl!!.setActive()) {
                        holder.currentState.text = "CurrentState=[active]"
                    }
                }
            }

            holder.holdButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    if (ItemsViewModel.callObject.mCallControl!!.setInactive()) {
                        holder.currentState.text = "CurrentState=[onHold]"
                    }
                }
            }

            holder.disconnectButton.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    ItemsViewModel.callObject.mCallControl?.disconnect(
                        DisconnectCause(
                            DisconnectCause.LOCAL
                        )
                    )
                }
                holder.currentState.text = "CurrentState=[null]"
                mList?.remove(ItemsViewModel)
                this.notifyDataSetChanged()
            }

            holder.earpieceButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val earpieceEndpoint =
                        ItemsViewModel.callObject.getEndpointType(CallEndpoint.TYPE_EARPIECE)
                    if (earpieceEndpoint != null) {
                        ItemsViewModel.callObject.mCallControl?.requestEndpointChange(
                            earpieceEndpoint
                        )
                    }
                }
            }
            holder.speakerButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val speakerEndpoint = ItemsViewModel.callObject
                        .getEndpointType(CallEndpoint.TYPE_SPEAKER)
                    if (speakerEndpoint != null) {
                        val success = ItemsViewModel.callObject.mCallControl?.requestEndpointChange(
                            speakerEndpoint
                        )
                        if (success == true) {
                            holder.currentEndpoint.text = "currentEndpoint=[speaker]"
                        }
                    }
                }
            }

            holder.bluetoothButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val bluetoothEndpoint = ItemsViewModel.callObject
                        .getEndpointType(CallEndpoint.TYPE_BLUETOOTH)
                    if (bluetoothEndpoint != null) {
                        val success = ItemsViewModel.callObject.mCallControl?.requestEndpointChange(
                            bluetoothEndpoint
                        )
                        if (success == true) {
                            holder.currentEndpoint.text =
                                "currentEndpoint=[BT:${bluetoothEndpoint.name}]"
                        }
                    }
                }
            }
        }
    }

    fun updateCallState(callId: String, state: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val holder = mCallIdToViewHolder[callId]
            holder?.callIdTextView?.text = "currentState=[$state]"
        }
    }

    fun updateEndpoint(callId: String, endpoint: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val holder = mCallIdToViewHolder[callId]
            holder?.currentEndpoint?.text = "currentEndpoint=[$endpoint]"
        }
    }
}