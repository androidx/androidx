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

import androidx.annotation.ColorRes;

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
        mView.setBackgroundResource(getColorRes(card));

        String cornerLabel = card.getCornerLabel();
        mTextCorner1.setText(cornerLabel);
        mTextCorner2.setText(cornerLabel);
        mTextCorner2.setRotation(180);
    }

    public View getView() {
        return mView;
    }

    @ColorRes
    private int getColorRes(Card card) {
        int shade = getShade(card);
        int color = getColor(card);
        return COLOR_MAP[color][shade];
    }

    private int getShade(Card card) {
        switch (card.getValue()) {
            case '2':
            case '6':
            case '⒑':
            case 'A':
                return 2;
            case '3':
            case '7':
            case 'J':
                return 3;
            case '4':
            case '8':
            case 'Q':
                return 0;
            case '5':
            case '9':
            case 'K':
                return 1;
        }
        throw new IllegalStateException(String.format("Card value cannot be %c (0x%X)",
                card.getValue(), (int) card.getValue()));
    }

    private int getColor(Card card) {
        switch (card.getSuit()) {
            case '♣':
                return 0;
            case '♦':
                return 1;
            case '♥':
                return 2;
            case '♠':
                return 3;
        }
        throw new IllegalStateException(String.format("Card suit cannot be %c (0x%X)",
                card.getSuit(), (int) card.getSuit()));
    }

    private static final int[][] COLOR_MAP = {
            {R.color.red_100, R.color.red_300, R.color.red_500, R.color.red_700},
            {R.color.blue_100, R.color.blue_300, R.color.blue_500, R.color.blue_700},
            {R.color.green_100, R.color.green_300, R.color.green_500, R.color.green_700},
            {R.color.yellow_100, R.color.yellow_300, R.color.yellow_500, R.color.yellow_700},
    };
}
