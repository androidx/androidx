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

package com.example.androidx.viewpager2.cards

import android.os.Bundle
import java.util.ArrayList

/**
 * Playing card
 */
class Card(suit: String, value: String) {

    val suit: String
    val value: String

    val cornerLabel: String
        get() = value + "\n" + suit

    init {
        this.suit = checkValidValue(suit, SUITS)
        this.value = checkValidValue(value, VALUES)
    }

    /** Use in conjunction with [Card.fromBundle]  */
    fun toBundle(): Bundle {
        val args = Bundle(1)
        args.putStringArray(ARGS_BUNDLE, arrayOf(suit, value))
        return args
    }

    private fun checkValidValue(value: String, allowed: Set<String>): String {
        if (allowed.contains(value)) {
            return value
        }
        throw IllegalArgumentException("Illegal argument: $value")
    }

    companion object {
        private val ARGS_BUNDLE = Card::class.java.name + ":Bundle"

        val SUITS = setOf("♣" /* clubs*/, "♦" /* diamonds*/, "♥" /* hearts*/, "♠" /*spades*/)
        val VALUES = setOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

        /** Use in conjunction with [Card.toBundle]  */
        fun fromBundle(bundle: Bundle): Card {
            val spec = bundle.getStringArray(ARGS_BUNDLE)
            return Card(spec[0], spec[1])
        }

        /**
         * Creates a deck of all allowed cards
         */
        fun createDeck52(): List<Card> {
            val result = ArrayList<Card>(52)
            SUITS.forEach { suit -> VALUES.forEach { value -> result.add(Card(suit, value)) } }
            if (result.size != 52) {
                throw IllegalStateException()
            }
            return result
        }
    }
}
