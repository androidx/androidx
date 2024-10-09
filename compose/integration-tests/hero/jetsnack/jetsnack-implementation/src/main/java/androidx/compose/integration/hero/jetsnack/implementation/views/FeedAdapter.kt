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

package androidx.compose.integration.hero.jetsnack.implementation.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.integration.hero.jetsnack.implementation.CollectionType
import androidx.compose.integration.hero.jetsnack.implementation.R
import androidx.compose.integration.hero.jetsnack.implementation.SnackCollection
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class FeedAdapter(private val snackCollection: List<SnackCollection>) :
    Adapter<FeedAdapter.FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.snack_feed, parent, false)
        return FeedViewHolder(root)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(snackCollection[position])
    }

    override fun getItemCount(): Int {
        return snackCollection.size
    }

    class FeedViewHolder(rootView: View) : ViewHolder(rootView) {
        private val nameTextView = rootView.findViewById<TextView>(R.id.nameTextView)
        private val feedRecyclerView = rootView.findViewById<RecyclerView>(R.id.feedRecyclerView)

        fun bind(snackCollection: SnackCollection) {
            nameTextView.text = snackCollection.name
            val feedAdapter =
                if (snackCollection.type == CollectionType.Highlight)
                    DessertAdapter(snackCollection.snacks)
                else SnackAdapter(snackCollection.snacks)
            feedRecyclerView.adapter = feedAdapter
        }
    }
}
