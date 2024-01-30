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

package androidx.paging.integration.testapp.v3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView

class LoadStateViewHolder(
    parent: ViewGroup,
    retry: () -> Unit
) : RecyclerView.ViewHolder(inflate(parent)) {
    private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    private val errorMsg: TextView = itemView.findViewById(R.id.error_msg)
    private val retry: Button = itemView.findViewById<Button>(R.id.retry_button)
        .also { it.setOnClickListener { retry.invoke() } }

    fun bind(loadState: LoadState) {
        if (loadState is LoadState.Error) {
            errorMsg.text = loadState.error.localizedMessage
        }
        progressBar.visibility = toVisibility(loadState is LoadState.Loading)
        retry.visibility = toVisibility(loadState !is LoadState.Loading)
        errorMsg.visibility = toVisibility(loadState !is LoadState.Loading)
    }

    private fun toVisibility(constraint: Boolean): Int = if (constraint) {
        View.VISIBLE
    } else {
        View.GONE
    }

    companion object {
        fun inflate(parent: ViewGroup): View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.load_state_item, parent, false)
    }
}

class StateItemAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState) =
        LoadStateViewHolder(parent, retry)

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) =
        holder.bind(loadState)
}
