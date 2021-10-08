/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.benchmark.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.BufferedReader
import java.io.InputStreamReader

const val POLARBEAR = "\uD83D\uDC3B\u200D❄️"
private val loadedEmojiStrings: List<String> by lazy { loadEmojiStrings() }

fun emojisString(size: Int) = emojisList(size).joinToString("")
fun emojisList(size: Int) = loadedEmojiStrings.take(size)

fun loadEmojiStrings(): List<String> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inputStream = context.assets.open("emojis.txt")
    val result = mutableListOf<String>()
    return inputStream.use {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        reader.forEachLine {
            val line = it.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachLine

            stringBuilder.setLength(0)
            line.split(" ")
                .toTypedArray()
                .map { intVal -> Character.toChars(intVal.toInt(16)) }
                .forEach { charArray -> stringBuilder.append(charArray) }
            result.add(stringBuilder.toString())
        }
        result
    }
}