/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardViewAdapter : RecyclerView.Adapter<CardViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardViewHolder(CardView(LayoutInflater.from(parent.context), parent))
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(Card.DECK[position])
    }

    override fun getItemCount(): Int {
        return Card.DECK.size
    }
}

class CardViewHolder internal constructor(private val cardView: CardView) :
    RecyclerView.ViewHolder(cardView.view) {
    internal fun bind(card: Card) {
        cardView.bind(card)
    }
}
