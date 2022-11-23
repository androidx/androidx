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
internal interface RecentEmojiProvider {
    /**
     * Inserts an emoji into recent emoji list. Called by emoji picker when an emoji is shared.
     */
    fun insert(emoji: String)

    /** Returns a list of recent items.  */
    suspend fun getRecentItemList(): List<String>
}