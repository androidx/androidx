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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.androidx.widget.viewpager2.R;

/** @inheritDoc */
public class CardViewHolder extends RecyclerView.ViewHolder {
    private final TextView mTextSuite;
    private final TextView mTextCorner1;
    private final TextView mTextCorner2;

    public CardViewHolder(View itemView) {
        super(itemView);
        mTextSuite = itemView.findViewById(R.id.label_center);
        mTextCorner1 = itemView.findViewById(R.id.label_top);
        mTextCorner2 = itemView.findViewById(R.id.label_bottom);
    }

    /**
     * Updates the view to represent the passed in card
     */
    public void apply(Card card) {
        mTextSuite.setText(Character.toString(card.getSuit()));

        String cornerLabel = card.getCornerLabel();
        mTextCorner1.setText(cornerLabel);
        mTextCorner2.setText(cornerLabel);
        mTextCorner2.setRotation(180);
    }
}
