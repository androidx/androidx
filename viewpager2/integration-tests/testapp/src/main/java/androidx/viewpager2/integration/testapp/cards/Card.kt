/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp.cards

import android.os.Bundle
import androidx.core.text.BidiFormatter

/**
 * Playing card
 */
class Card private constructor(val suit: String, val value: String) {

    val cornerLabel: String
        get() = value + "\n" + suit

    /** Use in conjunction with [Card.fromBundle]  */
    fun toBundle(): Bundle {
        val args = Bundle(1)
        args.putStringArray(ARGS_BUNDLE, arrayOf(suit, value))
        return args
    }

    override fun toString(): String {
        val bidi = BidiFormatter.getInstance()
        if (!bidi.isRtlContext) {
            return bidi.unicodeWrap("$value $suit")
        } else {
            return bidi.unicodeWrap("$suit $value")
        }
    }

    companion object {
        internal val ARGS_BUNDLE = Card::class.java.name + ":Bundle"

        val SUITS = setOf("♣" /* clubs*/, "♦" /* diamonds*/, "♥" /* hearts*/, "♠" /*spades*/)
        val VALUES = setOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
        val DECK = SUITS.flatMap { suit ->
            VALUES.map { value -> Card(suit, value) }
        }

        fun List<Card>.find(value: String, suit: String): Card? {
            return find { it.value == value && it.suit == suit }
        }

        /** Use in conjunction with [Card.toBundle]  */
        fun fromBundle(bundle: Bundle): Card {
            val spec = bundle.getStringArray(ARGS_BUNDLE)
            return Card(spec!![0], spec[1])
        }
    }
}
