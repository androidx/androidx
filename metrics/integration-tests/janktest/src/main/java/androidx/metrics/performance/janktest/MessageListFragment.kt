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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.metrics.performance.JankStats
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MessageListFragment : Fragment() {

    val messageList: Array<String> = Array<String>(100) {
        "Message #" + it
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.MessageList)
        recyclerView.adapter = MessageListAdapter(messageList)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val jankStats = activity?.let { JankStats.getInstance(view) }
                if (jankStats != null) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        jankStats.addState("RecyclerView", "Dragging")
                    } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        jankStats.addState("RecyclerView", "Settling")
                    } else {
                        jankStats.removeState("RecyclerView")
                    }
                }
            }
        })
    }
}