/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

class StringPagedList constructor(
    leadingNulls: Int,
    trailingNulls: Int,
    vararg items: String,
    list: List<String> = items.toList()
) : PagedList<String>(
    PagedSourceWrapper(ListDataSource(list)),
    PagedStorage(),
    Config.Builder().setPageSize(1).build()
), PagedStorage.Callback {
    var detached = false

    init {
        val keyedStorage = getStorage()
        keyedStorage.init(
            leadingNulls,
            list,
            trailingNulls,
            0,
            this
        )
    }

    override val lastKey: Any? = null

    override val isDetached
        get() = detached

    override fun detach() {
        detached = true
    }

    override fun dispatchCurrentLoadState(callback: LoadStateListener) {}

    override fun loadAroundInternal(index: Int) {}

    override fun onInitialized(count: Int) {}

    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {}

    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {}

    override fun onPagesRemoved(startOfDrops: Int, count: Int) = notifyRemoved(startOfDrops, count)

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) =
        notifyChanged(startOfDrops, count)
}
