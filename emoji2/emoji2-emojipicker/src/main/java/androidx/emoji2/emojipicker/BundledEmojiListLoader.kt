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
import androidx.core.content.res.use

/**
 * A data loader that loads the following objects either from file based caches or from resources.
 *
 * @property categorizedEmojiData: a list that holds bundled emoji separated by category, filtered
 * by renderability check. This is the data source for EmojiPickerView.
 *
 * @property emojiVariantsLookup: a map of emoji variants in bundled emoji, keyed by the primary
 * emoji. This allows faster variants lookup.
 */
internal object BundledEmojiListLoader {

    private var _categorizedEmojiData: List<EmojiDataCategory>? = null
    private var _emojiVariantsLookup: Map<String, List<String>>? = null

    internal fun load(context: Context, emojiCompatMetadata: EmojiPickerView.EmojiCompatMetadata) {
        // TODO(chelseahao): load from cache.
        val categoryNames = context.resources.getStringArray(R.array.category_names)

        _categorizedEmojiData = context.resources
            .obtainTypedArray(R.array.emoji_by_category_raw_resources)
            .use { ta ->
                (0 until ta.length()).map {
                    EmojiDataCategory(
                        categoryNames[it],
                        loadSingleCategory(
                            context,
                            emojiCompatMetadata,
                            ta.getResourceId(it, 0)
                        )
                    )
                }.toList()
            }

        _emojiVariantsLookup =
            _categorizedEmojiData!!
                .map { it.emojiDataList }
                .flatten()
                .filter { it.variants.isNotEmpty() }
                .associate { it.primary to it.variants }
    }

    internal val categorizedEmojiData: List<EmojiDataCategory>
        get() = _categorizedEmojiData
            ?: throw IllegalStateException("BundledEmojiListLoader.load is not called")

    internal val emojiVariantsLookup: Map<String, List<String>>
        get() = _emojiVariantsLookup
            ?: throw IllegalStateException("BundledEmojiListLoader.load is not called")

    private fun loadSingleCategory(
        context: Context,
        emojiCompatMetadata: EmojiPickerView.EmojiCompatMetadata,
        resId: Int,
    ): List<EmojiData> =
        context.resources
            .openRawResource(resId)
            .bufferedReader()
            .useLines { it.toList() }
            .map { filterRenderableEmojis(it.split(","), emojiCompatMetadata) }
            .filter { it.isNotEmpty() }
            .map { EmojiData(it.first(), it.drop(1)) }

    /**
     * To eliminate 'Tofu' (the fallback glyph when an emoji is not renderable), check the
     * renderability of emojis and keep only when they are renderable on the current device.
     */
    @Suppress("UNUSED_PARAMETER")
    // TODO(chelseahao): implementation.
    private fun filterRenderableEmojis(
        emojiList: List<String>,
        emojiCompatMetadata: EmojiPickerView.EmojiCompatMetadata,
    ): List<String> = emojiList

    internal data class EmojiData(val primary: String, val variants: List<String>)

    internal data class EmojiDataCategory(
        val categoryName: String,
        val emojiDataList: List<EmojiData>
    )
}