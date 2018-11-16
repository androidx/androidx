/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.androidx.viewpager2

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

import com.example.androidx.viewpager2.cards.Card
import com.example.androidx.viewpager2.cards.CardView

/**
 * Shows how to use [ViewPager2.setAdapter] with Views.
 *
 * @see CardFragmentActivity for an example of using {@link ViewPager2} with Fragments.
 */
class CardViewActivity : BaseCardActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewPager.adapter = object : RecyclerView.Adapter<CardViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): CardViewHolder {
                return CardViewHolder(CardView(layoutInflater, parent))
            }

            override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
                holder.bind(BaseCardActivity.cards[position])
            }

            override fun getItemCount(): Int {
                return BaseCardActivity.cards.size
            }
        }
    }

    class CardViewHolder internal constructor(
        private val cardView: CardView
    ) : RecyclerView.ViewHolder(cardView.view) {

        internal fun bind(card: Card) {
            cardView.bind(card)
        }
    }
}
