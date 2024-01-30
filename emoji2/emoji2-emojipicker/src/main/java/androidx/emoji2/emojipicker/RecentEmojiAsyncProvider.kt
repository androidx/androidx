/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.guava.await

/**
 * A interface equivalent to [RecentEmojiProvider] that allows java clients to override the
 * [ListenableFuture] based function [getRecentEmojiListAsync] in order to provide recent emojis.
 */
interface RecentEmojiAsyncProvider {
    fun recordSelection(emoji: String)

    fun getRecentEmojiListAsync(): ListenableFuture<List<String>>
}

/**
 * An adapter for the [RecentEmojiAsyncProvider].
 */
class RecentEmojiProviderAdapter(private val recentEmojiAsyncProvider: RecentEmojiAsyncProvider) :
    RecentEmojiProvider {
    override fun recordSelection(emoji: String) {
        recentEmojiAsyncProvider.recordSelection(emoji)
    }

    override suspend fun getRecentEmojiList() =
        recentEmojiAsyncProvider.getRecentEmojiListAsync().await()
}
