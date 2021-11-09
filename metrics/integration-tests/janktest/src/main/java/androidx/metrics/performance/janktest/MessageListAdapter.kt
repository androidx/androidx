/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.metrics.performance.janktest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView

class MessageListAdapter(val messageList: Array<String>) :
    RecyclerView.Adapter<MessageListAdapter.MessageHeaderViewHolder>() {

    class MessageHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageHeaderTV =
            itemView.findViewById<android.widget.TextView>(R.id.messageHeader)
        fun bind(headerText: String) {
            itemView.setOnClickListener {
                val bundle = bundleOf("title" to headerText)
                Navigation.findNavController(it).navigate(
                    R.id.action_MessageList_to_messageContent, bundle
                )
            }
            messageHeaderTV.text = headerText
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHeaderViewHolder {
        return MessageHeaderViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MessageHeaderViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}