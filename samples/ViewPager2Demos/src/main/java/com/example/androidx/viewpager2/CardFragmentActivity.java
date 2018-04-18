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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.FragmentProvider;

import com.example.androidx.viewpager2.cards.Card;
import com.example.androidx.viewpager2.cards.CardView;

import java.util.List;

/**
 * Shows how to use {@link ViewPager2#setAdapter(FragmentManager, FragmentProvider, int)}
 *
 * @see CardActivity
 */
public class CardFragmentActivity extends FragmentActivity {
    private static final List<Card> sCards = unmodifiableList(Card.createDeck52());

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_layout);

        this.<ViewPager2>findViewById(R.id.view_pager).setAdapter(getSupportFragmentManager(),
                new FragmentProvider() {
                    @Override
                    public Fragment getItem(int position) {
                        return CardFragment.create(sCards.get(position));
                    }

                    @Override
                    public int getCount() {
                        return sCards.size();
                    }
                },
                ViewPager2.FragmentRetentionPolicy.SAVE_STATE);
    }

        /** {@inheritDoc} */
    public static class CardFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            CardView cardView = new CardView(getLayoutInflater(), container);
            cardView.bind(Card.fromBundle(getArguments()));
            return cardView.getView();
        }

        /** Creates a Fragment for a given {@link Card} */
        public static CardFragment create(Card card) {
            CardFragment fragment = new CardFragment();
            fragment.setArguments(card.toBundle());
            return fragment;
        }
    }
}
