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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Playing card
 */
public class Card {
    private static final Set<Character> SUITS = unmodifiableSet(new LinkedHashSet<>(
            asList('♦', /* diamonds*/ '♣', /*, clubs*/ '♥', /* hearts*/ '♠' /*spades*/)));
    private static final Set<Character> VALUES = unmodifiableSet(new LinkedHashSet<>(
            asList('2', '3', '4', '5', '6', '7', '8', '9', '⒑', 'J', 'Q', 'K', 'A')));

    private final char mSuit;
    private final char mValue;

    public Card(char suit, char value) {
        this.mSuit = checkValidValue(suit, SUITS);
        this.mValue = checkValidValue(value, VALUES);
    }

    public char getSuit() {
        return mSuit;
    }

    public String getCornerLabel() {
        return mValue + "\n" + mSuit;
    }

    private static char checkValidValue(char value, Set<Character> allowed) {
        if (allowed.contains(value)) {
            return value;
        }
        throw new IllegalArgumentException("Illegal argument: " + value);
    }
}
