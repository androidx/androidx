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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.Orientation;

import com.example.androidx.viewpager2.cards.Card;

import java.util.List;
import java.util.Set;

/**
 * Base class for the two activities in the demo. Sets up the list of cards and implements UI to
 * jump to arbitrary cards using setCurrentItem, either with or without smooth scrolling.
 */
public abstract class BaseCardActivity extends FragmentActivity {

    protected static final List<Card> sCards = unmodifiableList(Card.createDeck52());
    private static final String HORIZONTAL = "horizontal";
    private static final String VERTICAL = "vertical";

    protected ViewPager2 mViewPager;
    private Spinner mOrientationSelector;
    private Spinner mValueSelector;
    private Spinner mSuitSelector;
    private CheckBox mSmoothScrollCheckBox;
    private CheckBox mRotateCheckBox;
    private CheckBox mTranslateCheckBox;
    private CheckBox mScaleCheckBox;
    private Button mGotoPage;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_layout);

        mViewPager = findViewById(R.id.view_pager);
        mOrientationSelector = findViewById(R.id.orientation_spinner);
        mValueSelector = findViewById(R.id.value_spinner);
        mSuitSelector = findViewById(R.id.suit_spinner);
        mSmoothScrollCheckBox = findViewById(R.id.smooth_scroll_checkbox);
        mRotateCheckBox = findViewById(R.id.rotate_checkbox);
        mTranslateCheckBox = findViewById(R.id.translate_checkbox);
        mScaleCheckBox = findViewById(R.id.scale_checkbox);
        mGotoPage = findViewById(R.id.jump_button);

        mOrientationSelector.setAdapter(createOrientationAdapter());
        mValueSelector.setAdapter(createAdapter(Card.VALUES));
        mSuitSelector.setAdapter(createAdapter(Card.SUITS));

        mViewPager.setPageTransformer(mAnimator);

        mOrientationSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (parent.getSelectedItem().toString()) {
                    case HORIZONTAL:
                        mViewPager.setOrientation(Orientation.HORIZONTAL);
                        break;
                    case VERTICAL:
                        mViewPager.setOrientation(Orientation.VERTICAL);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

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

    private SpinnerAdapter createOrientationAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[] {HORIZONTAL, VERTICAL});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private final ViewPager2.PageTransformer mAnimator = (page, position) -> {
        float absPos = Math.abs(position);
        if (mRotateCheckBox.isChecked()) {
            page.setRotation(position * 360);
        } else {
            page.setRotation(0);
        }
        if (mTranslateCheckBox.isChecked()) {
            page.setTranslationY(absPos * 500);
        } else {
            page.setTranslationY(0);
        }
        if (mScaleCheckBox.isChecked()) {
            float scale = absPos > 1 ? 0 : 1 - absPos;
            page.setScaleX(scale);
            page.setScaleY(scale);
        } else {
            page.setScaleX(1);
            page.setScaleY(1);
        }
    };

}
