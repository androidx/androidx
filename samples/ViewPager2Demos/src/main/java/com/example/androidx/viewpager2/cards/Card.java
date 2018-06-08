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

package com.example.androidx.viewpager2.cards;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Playing card
 */
public class Card {
    private static final String ARGS_BUNDLE = Card.class.getName() + ":Bundle";

    private static final Set<Character> SUITS = unmodifiableSet(new LinkedHashSet<>(
            asList('♣' /* clubs*/, '♦' /* diamonds*/, '♥' /* hearts*/, '♠' /*spades*/)));
    private static final Set<Character> VALUES = unmodifiableSet(new LinkedHashSet<>(
            asList('2', '3', '4', '5', '6', '7', '8', '9', '⒑', 'J', 'Q', 'K', 'A')));

    private final char mSuit;
    private final char mValue;

    public Card(char suit, char value) {
        this.mSuit = checkValidValue(suit, SUITS);
        this.mValue = checkValidValue(value, VALUES);
    }

    char getSuit() {
        return mSuit;
    }

    String getCornerLabel() {
        return mValue + "\n" + mSuit;
    }

    /** Use in conjunction with {@link Card#fromBundle(Bundle)} */
    public Bundle toBundle() {
        Bundle args = new Bundle(1);
        args.putCharArray(ARGS_BUNDLE, new char[]{mSuit, mValue});
        return args;
    }

    /** Use in conjunction with {@link Card#toBundle()} */
    public static Card fromBundle(Bundle bundle) {
        char[] spec = bundle.getCharArray(ARGS_BUNDLE);
        return new Card(spec[0], spec[1]);
    }

    private static char checkValidValue(char value, Set<Character> allowed) {
        if (allowed.contains(value)) {
            return value;
        }
        throw new IllegalArgumentException("Illegal argument: " + value);
    }

    /**
     * Creates a deck of all allowed cards
     */
    public static List<Card> createDeck52() {
        List<Card> result = new ArrayList<>(52);
        for (Character suit : SUITS) {
            for (Character value : VALUES) {
                result.add(new Card(suit, value));
            }
        }
        if (result.size() != 52) {
            throw new IllegalStateException();
        }
        return result;
    }
}
