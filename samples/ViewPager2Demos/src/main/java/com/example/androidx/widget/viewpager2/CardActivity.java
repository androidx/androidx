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

package com.example.androidx.widget.viewpager2;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import android.app.Activity;
import android.os.Bundle;

import com.example.androidx.widget.viewpager2.cards.Card;
import com.example.androidx.widget.viewpager2.cards.CardDataAdapter;

import java.util.List;

import androidx.widget.ViewPager2;

/** @inheritDoc */
public class CardActivity extends Activity {
    private static final List<Card> sCards = unmodifiableList(asList(
            new Card('♦', 'A'),
            new Card('♣', 'K'),
            new Card('♥', 'J'),
            new Card('♠', '9'),
            new Card('♦', '2')));

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_layout);

        this.<ViewPager2>findViewById(R.id.view_pager).setAdapter(
                new CardDataAdapter(getLayoutInflater(), sCards));
    }
}
