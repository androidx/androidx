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
import android.content.res.TypedArray
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.emoji2.emojipicker.utils.FileCache
import androidx.emoji2.emojipicker.utils.UnicodeRenderableManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * A data loader that loads the following objects either from file based caches or from resources.
 *
 * categorizedEmojiData: a list that holds bundled emoji separated by category, filtered
 * by renderability check. This is the data source for EmojiPickerView.
 *
 * emojiVariantsLookup: a map of emoji variants in bundled emoji, keyed by the base
 * emoji. This allows faster variants lookup.
 *
 * primaryEmojiLookup: a map of base emoji to its variants in bundled emoji. This allows faster
 * variants lookup.
 */
internal object BundledEmojiListLoader {
    private var categorizedEmojiData: List<EmojiDataCategory>? = null
    private var emojiVariantsLookup: Map<String, List<String>>? = null

    internal suspend fun load(context: Context) {
        val categoryNames = context.resources.getStringArray(R.array.category_names)
        val categoryHeaderIconIds =
            context.resources.obtainTypedArray(R.array.emoji_categories_icons).use { typedArray ->
                IntArray(typedArray.length()) { typedArray.getResourceId(it, 0) }
            }
        val resources = if (UnicodeRenderableManager.isEmoji12Supported())
            R.array.emoji_by_category_raw_resources_gender_inclusive
        else
            R.array.emoji_by_category_raw_resources
        val emojiFileCache = FileCache.getInstance(context)

        categorizedEmojiData = context.resources
            .obtainTypedArray(resources)
            .use { ta ->
                loadEmoji(
                    ta,
                    categoryHeaderIconIds,
                    categoryNames,
                    emojiFileCache,
                    context
                )
            }
        emojiVariantsLookup = categorizedEmojiData!!
            .flatMap { it.emojiDataList }
            .filter { it.variants.isNotEmpty() }
            .flatMap { it.variants.map { variant -> EmojiViewItem(variant, it.variants) } }
            .associate { it.emoji to it.variants }
            .also { emojiVariantsLookup = it }
    }

    internal fun getCategorizedEmojiData() = categorizedEmojiData
        ?: throw IllegalStateException("BundledEmojiListLoader.load is not called or complete")

    internal fun getEmojiVariantsLookup() = emojiVariantsLookup
        ?: throw IllegalStateException("BundledEmojiListLoader.load is not called or complete")

    private suspend fun loadEmoji(
        ta: TypedArray,
        @DrawableRes categoryHeaderIconIds: IntArray,
        categoryNames: Array<String>,
        emojiFileCache: FileCache,
        context: Context
    ): List<EmojiDataCategory> = coroutineScope {
        (0 until ta.length()).map {
            async {
                emojiFileCache.getOrPut(getCacheFileName(it)) {
                    loadSingleCategory(context, ta.getResourceId(it, 0))
                }.let { data ->
                    EmojiDataCategory(
                        categoryHeaderIconIds[it],
                        categoryNames[it],
                        data
                    )
                }
            }
        }.awaitAll()
    }

    private fun loadSingleCategory(
        context: Context,
        resId: Int,
    ): List<EmojiViewItem> =
        context.resources
            .openRawResource(resId)
            .bufferedReader()
            .useLines { it.toList() }
            .map { filterRenderableEmojis(it.split(",")) }
            .filter { it.isNotEmpty() }
            .map { EmojiViewItem(it.first(), it.drop(1)) }

    private fun getCacheFileName(categoryIndex: Int) =
        StringBuilder().append("emoji.v1.")
            .append(if (EmojiPickerView.emojiCompatLoaded) 1 else 0)
            .append(".")
            .append(categoryIndex)
            .append(".")
            .append(if (UnicodeRenderableManager.isEmoji12Supported()) 1 else 0)
            .toString()

    /**
     * To eliminate 'Tofu' (the fallback glyph when an emoji is not renderable), check the
     * renderability of emojis and keep only when they are renderable on the current device.
     */
    private fun filterRenderableEmojis(emojiList: List<String>) =
        emojiList.filter {
            UnicodeRenderableManager.isEmojiRenderable(it)
        }.toList()

    internal data class EmojiDataCategory(
        @DrawableRes val headerIconId: Int,
        val categoryName: String,
        val emojiDataList: List<EmojiViewItem>
    )
}
