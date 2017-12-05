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

package com.example.androidx.widget.viewpager2.cards;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.androidx.widget.viewpager2.R;

import java.util.List;

/** @inheritDoc */
public class CardDataAdapter extends RecyclerView.Adapter<CardViewHolder> {
    private final List<Card> mCards;
    private final LayoutInflater mLayoutInflater;

    public CardDataAdapter(LayoutInflater layoutInflater, List<Card> cards) {
        mLayoutInflater = layoutInflater;
        mCards = cards;
    }

    /**
     * @inheritDoc
     */
    @NonNull
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardViewHolder(
                mLayoutInflater.inflate(R.layout.item_card_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.apply(mCards.get(position));
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }
}
