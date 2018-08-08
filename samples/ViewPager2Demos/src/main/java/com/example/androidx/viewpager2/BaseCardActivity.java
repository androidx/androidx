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

package com.example.androidx.viewpager2;

import static java.util.Collections.unmodifiableList;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.androidx.viewpager2.cards.Card;

import java.util.List;
import java.util.Set;

/**
 * Base class for the two activities in the demo. Sets up the list of cards and implements UI to
 * jump to arbitrary cards using setCurrentItem, either with or without smooth scrolling.
 */
public abstract class BaseCardActivity extends FragmentActivity {

    protected static final List<Card> sCards = unmodifiableList(Card.createDeck52());

    protected ViewPager2 mViewPager;
    private Spinner mValueSelector;
    private Spinner mSuitSelector;
    private CheckBox mSmoothScrollCheckBox;
    private Button mGotoPage;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_layout);

        mViewPager = findViewById(R.id.view_pager);
        mValueSelector = findViewById(R.id.value_spinner);
        mSuitSelector = findViewById(R.id.suit_spinner);
        mSmoothScrollCheckBox = findViewById(R.id.smooth_scroll_checkbox);
        mGotoPage = findViewById(R.id.jump_button);

        mValueSelector.setAdapter(createAdapter(Card.VALUES));
        mSuitSelector.setAdapter(createAdapter(Card.SUITS));

        mGotoPage.setOnClickListener(view -> {
            int suit = mSuitSelector.getSelectedItemPosition();
            int value = mValueSelector.getSelectedItemPosition();
            int targetPosition = suit * Card.VALUES.size() + value;
            boolean smoothScroll = mSmoothScrollCheckBox.isChecked();
            mViewPager.setCurrentItem(targetPosition, smoothScroll);
        });
    }

    private SpinnerAdapter createAdapter(Set<Character> values) {
        ArrayAdapter<Character> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values.toArray(new Character[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

}
