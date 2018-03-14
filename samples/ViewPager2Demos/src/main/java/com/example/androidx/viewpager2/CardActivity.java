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

package com.example.androidx.viewpager2;

import static java.util.Collections.unmodifiableList;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.androidx.viewpager2.cards.Card;
import com.example.androidx.viewpager2.cards.CardView;

import java.util.List;

/**
 * Shows how to use {@link ViewPager2#setAdapter(RecyclerView.Adapter)}
 *
 * @see CardFragmentActivity
 */
public class CardActivity extends Activity {
    private static final List<Card> sCards = unmodifiableList(Card.createDeck52());

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_layout);

        this.<ViewPager2>findViewById(R.id.view_pager).setAdapter(
                new RecyclerView.Adapter<CardViewHolder>() {
                    @NonNull
                    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        return new CardViewHolder(new CardView(getLayoutInflater(), parent));
                    }

                    @Override
                    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
                        holder.bind(sCards.get(position));
                    }

                    @Override
                    public int getItemCount() {
                        return sCards.size();
                    }
                });
    }

    /** @inheritDoc */
    public static class CardViewHolder extends RecyclerView.ViewHolder {
        private final CardView mCardView;

        /** {@inheritDoc} */
        public CardViewHolder(CardView cardView) {
            super(cardView.getView());
            mCardView = cardView;
        }

        /** @see CardView#bind(Card) */
        public void bind(Card card) {
            mCardView.bind(card);
        }
    }
}
