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

package androidx.wear.compose.foundation.lazy.layout

/**
 * A key-index mapping used inside the
 * [androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider]. It might not contain all items
 * in the lazy layout as optimization, but it must cover items the provider is requesting during
 * layout pass. See [androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap] as sample
 * implementation that samples items near current viewport.
 */
internal interface LazyLayoutKeyIndexMap {
    /** @return current index for given [key] or `-1` if not found. */
    fun getIndex(key: Any): Int

    /** @return key for a given [index] if it is known, or null otherwise. */
    fun getKey(index: Int): Any?

    /** Empty map implementation, always returning `-1` for any key. */
    companion object Empty : LazyLayoutKeyIndexMap {
        @Suppress("AutoBoxing") override fun getIndex(key: Any): Int = -1

        override fun getKey(index: Int) = null
    }
}
