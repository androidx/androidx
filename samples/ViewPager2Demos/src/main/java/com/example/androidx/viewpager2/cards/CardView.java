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

package com.example.androidx.viewpager2.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidx.viewpager2.R;

/** Inflates and populates a {@link View} representing a {@link Card} */
public class CardView {
    private final View mView;
    private final TextView mTextSuite;
    private final TextView mTextCorner1;
    private final TextView mTextCorner2;

    public CardView(LayoutInflater layoutInflater, ViewGroup container) {
        mView = layoutInflater.inflate(R.layout.item_card_layout, container, false);
        mTextSuite = mView.findViewById(R.id.label_center);
        mTextCorner1 = mView.findViewById(R.id.label_top);
        mTextCorner2 = mView.findViewById(R.id.label_bottom);
    }

    /**
     * Updates the view to represent the passed in card
     */
    public void bind(Card card) {
        mTextSuite.setText(Character.toString(card.getSuit()));

        String cornerLabel = card.getCornerLabel();
        mTextCorner1.setText(cornerLabel);
        mTextCorner2.setText(cornerLabel);
        mTextCorner2.setRotation(180);
    }

    public View getView() {
        return mView;
    }
}
