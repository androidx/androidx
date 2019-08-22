/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import android.app.Activity
import androidx.compose.composer
import androidx.test.rule.ActivityTestRule
import java.lang.StringBuilder
import kotlin.random.Random

fun <T : Activity> ActivityTestRule<T>.runOnUiThreadSync(action: () -> Unit) {
    // Workaround for lambda bug in IR
    runOnUiThread(object : Runnable {
        override fun run() {
            action.invoke()
        }
    })
}

fun Activity.runOnUiThreadSync(action: () -> Unit) {
    // Workaround for lambda bug in IR
    runOnUiThread(object : Runnable {
        override fun run() {
            action.invoke()
        }
    })
}

class RandomTextGenerator(
    private val space: String = " ",
    private val charRanges: List<IntRange> = alphabets
) {
    private val random = Random(0)

    private fun nextWord(length: Int): String =
        List(length) {
            charRanges.random(random).random(random).toChar()
        }.joinToString(separator = "")

    fun nextParagraph(
        length: Int,
        wordLength: Int = 9
    ): String {
        return if (length == 0) {
            ""
        } else {
            StringBuilder().apply {
                while (this.length < length) {
                    append(nextWord(wordLength))
                    append(space)
                }
            }.substring(0, length)
        }
    }

    companion object {
        val alphabets = listOf(
            IntRange('a'.toInt(), 'z'.toInt()),
            IntRange('A'.toInt(), 'Z'.toInt())
        )
    }
}