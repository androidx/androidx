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

import android.media.AudioManager.AudioRecordingCallback
import android.media.AudioRecord
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(34)
class CallListAdapter(
    private var mList: ArrayList<CallRow>?,
    private var mAudioRecord: AudioRecord? = null
) : RecyclerView.Adapter<CallListAdapter.ViewHolder>() {
    var mCallIdToViewHolder: MutableMap<String, ViewHolder> = mutableMapOf()
    private val CONTROL_ACTION_FAILED_MSG = "CurrentState=[FAILED-T]"
    internal var mAudioRecordingCallback: AudioRecordingCallback? = null

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        // TextViews
        val callCount: TextView = itemView.findViewById(R.id.callNumber)
        val callIdTextView: TextView = itemView.findViewById(R.id.callIdTextView)
        val currentState: TextView = itemView.findViewById(R.id.callStateTextView)
        val currentEndpoint: TextView = itemView.findViewById(R.id.endpointStateTextView)
        val participants: TextView = itemView.findViewById(R.id.participantsTextView)

        // Call State Buttons
        val activeButton: Button = itemView.findViewById(R.id.activeButton)
        val holdButton: Button = itemView.findViewById(R.id.holdButton)
        val disconnectButton: Button = itemView.findViewById(R.id.disconnectButton)

        // Call Audio Buttons
        val earpieceButton: Button = itemView.findViewById(R.id.selectEndpointButton)
        val speakerButton: Button = itemView.findViewById(R.id.speakerButton)
        val bluetoothButton: Button = itemView.findViewById(R.id.bluetoothButton)

        // Participant Buttons
        val addParticipantButton: Button = itemView.findViewById(R.id.addParticipantButton)
        val removeParticipantButton: Button = itemView.findViewById(R.id.removeParticipantButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view that is used to hold list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.call_row, parent, false)

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
                    // If the audio is not already recording, start it up (i.e. if call was set
                    // to inactive just before).
                    if (mAudioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        mAudioRecord?.startRecording()
                    }
                    when (ItemsViewModel.callObject.mCallControl!!.setActive()) {
                        is CallControlResult.Success -> {
                            holder.currentState.text = "CurrentState=[active]"
                        }
                        is CallControlResult.Error -> {
                            holder.currentState.text = CONTROL_ACTION_FAILED_MSG
                        }
                    }
                }
            }

            holder.holdButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    // Pause recording but don't clear callback
                    mAudioRecord?.stop()
                    when (ItemsViewModel.callObject.mCallControl!!.setInactive()) {
                        is CallControlResult.Success -> {
                            holder.currentState.text = "CurrentState=[onHold]"
                        }
                        is CallControlResult.Error -> {
                            holder.currentState.text = CONTROL_ACTION_FAILED_MSG
                        }
                    }
                }
            }

            holder.disconnectButton.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    endAudioRecording()
                    ItemsViewModel.callObject.mCallControl?.disconnect(
                        DisconnectCause(DisconnectCause.LOCAL)
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
                    val speakerEndpoint =
                        ItemsViewModel.callObject.getEndpointType(CallEndpoint.TYPE_SPEAKER)
                    if (speakerEndpoint != null) {
                        when (
                            ItemsViewModel.callObject.mCallControl!!.requestEndpointChange(
                                speakerEndpoint
                            )
                        ) {
                            is CallControlResult.Success -> {
                                holder.currentState.text = "CurrentState=[speaker]"
                            }
                            is CallControlResult.Error -> {
                                holder.currentState.text = CONTROL_ACTION_FAILED_MSG
                            }
                        }
                    }
                }
            }

            holder.bluetoothButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val bluetoothEndpoint =
                        ItemsViewModel.callObject.getEndpointType(CallEndpoint.TYPE_BLUETOOTH)
                    if (bluetoothEndpoint != null) {
                        when (
                            ItemsViewModel.callObject.mCallControl!!.requestEndpointChange(
                                bluetoothEndpoint
                            )
                        ) {
                            is CallControlResult.Success -> {
                                holder.currentEndpoint.text =
                                    "currentEndpoint=[BT:${bluetoothEndpoint.name}]"
                            }
                            is CallControlResult.Error -> {
                                // e.g. tear down call and
                                holder.currentState.text = CONTROL_ACTION_FAILED_MSG
                            }
                        }
                    }
                }
            }

            holder.addParticipantButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    ItemsViewModel.callObject.mParticipantControl?.onParticipantAdded?.invoke()
                }
            }

            holder.removeParticipantButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    ItemsViewModel.callObject.mParticipantControl?.onParticipantRemoved?.invoke()
                }
            }
        }
    }

    fun updateParticipants(callId: String, participants: List<ParticipantState>) {
        CoroutineScope(Dispatchers.Main).launch {
            val holder = mCallIdToViewHolder[callId]
            holder?.participants?.text = "participants=[${printParticipants(participants)}]"
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

    private fun printParticipants(participants: List<ParticipantState>): String {
        if (participants.isEmpty()) return "<NONE>"
        val builder = StringBuilder()
        val iterator = participants.iterator()
        while (iterator.hasNext()) {
            val participant = iterator.next()
            builder.append("<")
            if (participant.isActive) {
                builder.append(" * ")
            }
            builder.append(participant.name)
            if (participant.isSelf) {
                builder.append("(me)")
            }
            if (participant.isHandRaised) {
                builder.append(" ")
                builder.append("(RH)")
            }
            builder.append(">")
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
        return builder.toString()
    }

    private fun endAudioRecording() {
        try {
            // Stop audio recording
            mAudioRecord?.stop()
            mAudioRecord?.unregisterAudioRecordingCallback(mAudioRecordingCallback!!)
        } catch (e: java.lang.Exception) {
            // pass through
        } finally {
            mAudioRecordingCallback = null
        }
    }
}
