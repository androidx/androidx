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
import androidx.emoji2.emojipicker.utils.FileCache
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class BundledEmojiListLoaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val emojiCompatMetadata = EmojiPickerView.EmojiCompatMetadata(null, false)

    @Test
    fun testGetCategorizedEmojiData_loaded_writeToCache() {
        // delete cache dir first
        val fileCache = FileCache.getInstance(context)
        fileCache.emojiPickerCacheDir.deleteRecursively()
        assertFalse(fileCache.emojiPickerCacheDir.exists())

        BundledEmojiListLoader.load(context, emojiCompatMetadata)
        assertTrue(BundledEmojiListLoader.categorizedEmojiData.isNotEmpty())

        // emoji_picker/osVersion|appVersion/ folder should be created
        val propertyFolder = fileCache.emojiPickerCacheDir.listFiles()!![0]
        assertTrue(propertyFolder!!.isDirectory)

        // Number of cache files should match the size of categorizedEmojiData
        val cacheFiles = propertyFolder.listFiles()
        assertTrue(
            cacheFiles!!.size == BundledEmojiListLoader.categorizedEmojiData.size
        )
    }

    @Test
    fun testGetCategorizedEmojiData_loaded_readFromCache() {
        // delete cache and load again
        val fileCache = FileCache.getInstance(context)
        fileCache.emojiPickerCacheDir.deleteRecursively()
        BundledEmojiListLoader.load(context, emojiCompatMetadata)

        val cacheFileName = fileCache.emojiPickerCacheDir.listFiles()!![0].listFiles()!![0].name
        val emptyDefaultValue = listOf<BundledEmojiListLoader.EmojiData>()
        // Read from cache instead of using default value
        var output = fileCache.getOrPut(cacheFileName) { emptyDefaultValue }
        assertTrue(output.isNotEmpty())

        // Remove cache, write default value to cache
        fileCache.emojiPickerCacheDir.deleteRecursively()
        output = fileCache.getOrPut(cacheFileName) { emptyDefaultValue }
        assertTrue(output.isEmpty())
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    fun testGetEmojiVariantsLookup_loaded() {
        // delete cache and load again
        FileCache.getInstance(context).emojiPickerCacheDir.deleteRecursively()
        BundledEmojiListLoader.load(context, emojiCompatMetadata)

        // 👃 has variants (👃,👃,👃🏻,👃🏼,👃🏽,👃🏾,👃🏿)
        assertTrue(
            BundledEmojiListLoader
                .emojiVariantsLookup["\uD83D\uDC43"]
            !!.contains("\uD83D\uDC43\uD83C\uDFFD")
        )
        // 😀 has no variant
        assertFalse(
            BundledEmojiListLoader.emojiVariantsLookup.containsKey("\uD83D\uDE00")
        )
    }
}
