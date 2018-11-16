/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.example.androidx.viewpager2.cards.Card
import com.example.androidx.viewpager2.cards.CardView

/**
 * Shows how to use a [androidx.viewpager2.widget.ViewPager2] with Fragments, via a
 * [androidx.viewpager2.adapter.FragmentStateAdapter]
 *
 * @see CardViewActivity for an example of using {@link androidx.viewpager2.widget.ViewPager2} with
 * Views.
 */
class CardFragmentActivity : BaseCardActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewPager.adapter = object : FragmentStateAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return CardFragment.create(BaseCardActivity.cards[position])
            }

            override fun getItemCount(): Int {
                return BaseCardActivity.cards.size
            }
        }
    }

    class CardFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val cardView = CardView(layoutInflater, container!!)
            cardView.bind(Card.fromBundle(arguments!!))
            return cardView.view
        }

        companion object {

            /** Creates a Fragment for a given [Card]  */
            fun create(card: Card): CardFragment {
                val fragment = CardFragment()
                fragment.arguments = card.toBundle()
                return fragment
            }
        }
    }
}
