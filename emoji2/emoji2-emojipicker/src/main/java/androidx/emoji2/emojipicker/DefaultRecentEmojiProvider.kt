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

package androidx.emoji2.emojipicker

import android.content.Context
import android.content.Context.MODE_PRIVATE

/**
 * Provides recently shared emoji. This is the default recent emoji list provider. Clients could
 * specify the provider by their own.
 */
internal class DefaultRecentEmojiProvider(context: Context) : RecentEmojiProvider {

    companion object {
        private const val PREF_KEY_RECENT_EMOJI = "pref_key_recent_emoji"
        private const val RECENT_EMOJI_LIST_FILE_NAME = "androidx.emoji2.emojipicker.preferences"
        private const val SPLIT_CHAR = ","
    }

    private val sharedPreferences =
        context.getSharedPreferences(RECENT_EMOJI_LIST_FILE_NAME, MODE_PRIVATE)
    private val recentEmojiList: MutableList<String> =
        sharedPreferences.getString(PREF_KEY_RECENT_EMOJI, null)?.split(SPLIT_CHAR)?.toMutableList()
            ?: mutableListOf()

    override suspend fun getRecentEmojiList(): List<String> {
        return recentEmojiList
    }

    override fun recordSelection(emoji: String) {
        recentEmojiList.remove(emoji)
        recentEmojiList.add(0, emoji)
        saveToPreferences()
    }

    private fun saveToPreferences() {
        sharedPreferences
            .edit()
            .putString(PREF_KEY_RECENT_EMOJI, recentEmojiList.joinToString(SPLIT_CHAR))
            .commit()
    }
}
