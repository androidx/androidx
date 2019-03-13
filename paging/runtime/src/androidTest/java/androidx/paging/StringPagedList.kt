/*
 * Copyright (C) 2017 The Android Open Source Project
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
    vararg items: String
) : PagedList<String>(
        PagedStorage<String>(),
        TestExecutor(),
        TestExecutor(),
        null,
        PagedList.Config.Builder().setPageSize(1).build()
), PagedStorage.Callback {
    val list = items.toList()
    var detached = false
    init {
        @Suppress("UNCHECKED_CAST")
        val keyedStorage = mStorage as PagedStorage<String>
        keyedStorage.init(leadingNulls,
                list,
                trailingNulls,
                0,
                this)
    }

    internal override fun isContiguous(): Boolean {
        return true
    }

    override fun getLastKey(): Any? {
        return null
    }

    override fun isDetached(): Boolean = detached

    override fun detach() {
        detached = true
    }

    override fun dispatchUpdatesSinceSnapshot(
        storageSnapshot: PagedList<String>,
        callback: PagedList.Callback
    ) {
    }

    override fun dispatchCurrentLoadState(listener: LoadStateListener?) {}

    override fun loadAroundInternal(index: Int) {}

    override fun onInitialized(count: Int) {}

    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {}

    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {}

    override fun onPagePlaceholderInserted(pageIndex: Int) {}

    override fun onPageInserted(start: Int, count: Int) {}

    override fun getDataSource(): DataSource<*, String> {
        return ListDataSource<String>(list)
    }

    override fun onPagesRemoved(startOfDrops: Int, count: Int) {
        notifyRemoved(startOfDrops, count)
    }

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) {
        notifyChanged(startOfDrops, count)
    }
}
