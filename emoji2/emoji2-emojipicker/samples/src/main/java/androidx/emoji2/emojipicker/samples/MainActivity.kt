/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker.samples

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val view = findViewById<EmojiPickerView>(R.id.emoji_picker)
        view.setOnEmojiPickedListener {
            findViewById<EditText>(R.id.edit_text).append(it.emoji)
        }
        view.setRecentEmojiProvider(CustomRecentEmojiProvider(applicationContext))
    }
}

/**
 * Define a custom recent emoji provider which only provides emoji with clicks >= 2
 */
internal class CustomRecentEmojiProvider(
    context: Context
) : RecentEmojiProvider {

    companion object {
        private const val PREF_KEY_CUSTOM_RECENT_EMOJI = "pref_key_custom_recent_emoji"
        private const val PREF_KEY_CUSTOM_EMOJI_FREQ = "pref_key_custom_emoji_freq"
        private const val RECENT_EMOJI_LIST_FILE_NAME =
            "androidx.emoji2.emojipicker.sample.preferences"
        private const val SPLIT_CHAR = ","
        private const val KEY_VALUE_DELIMITER = "="
    }

    private val sharedPreferences =
        context.getSharedPreferences(RECENT_EMOJI_LIST_FILE_NAME, Context.MODE_PRIVATE)
    private val recentEmojiList =
        sharedPreferences.getString(PREF_KEY_CUSTOM_RECENT_EMOJI, null)
            ?.split(SPLIT_CHAR)
            ?.toMutableList()
            ?: mutableListOf()

    private val emoji2Frequency: MutableMap<String, Int> = mutableMapOf()

    init {
        for (entry in sharedPreferences.getString(PREF_KEY_CUSTOM_EMOJI_FREQ, null)
            ?.split(SPLIT_CHAR) ?: listOf()) {
            val kv = entry.split(KEY_VALUE_DELIMITER)
            emoji2Frequency[kv[0]] = kv[1].toInt()
        }
    }

    override suspend fun getRecentEmojiList(): List<String> {
        val recentList = mutableListOf<String>()
        emoji2Frequency.keys.filter { (emoji2Frequency[it] ?: 0) >= 2 }
            .map { recentList.add(it) }
        return recentList
    }

    override fun recordSelection(emoji: String) {
        recentEmojiList.add(0, emoji)
        emoji2Frequency[emoji] = (emoji2Frequency[emoji] ?: 0) + 1
        saveToPreferences()
    }

    private fun saveToPreferences() {
        sharedPreferences
            .edit()
            .putString(PREF_KEY_CUSTOM_RECENT_EMOJI, recentEmojiList.joinToString(SPLIT_CHAR))
            .putString(PREF_KEY_CUSTOM_EMOJI_FREQ, emoji2Frequency.entries.joinToString(SPLIT_CHAR))
            .commit()
    }
}
