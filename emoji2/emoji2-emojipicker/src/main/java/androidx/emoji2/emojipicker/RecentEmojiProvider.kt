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

/** An interface to provide recent emoji list.  */
interface RecentEmojiProvider {
    /**
     * Records an emoji into recent emoji list.
     * This fun will be called when an emoji is selected.
     * Clients could specify the behavior to record recently used emojis.(e.g. click frequency).
     */
    fun recordSelection(emoji: String)

    /**
     * Returns a list of recent emojis.
     * Default behavior: The most recently used emojis will be displayed first.
     * Clients could also specify the behavior such as displaying the emojis from high click
     * frequency to low click frequency.
     */
    suspend fun getRecentEmojiList(): List<String>
}
