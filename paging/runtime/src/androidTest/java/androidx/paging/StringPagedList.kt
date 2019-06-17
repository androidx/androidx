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

import androidx.testutils.TestExecutor

class StringPagedList constructor(
    leadingNulls: Int,
    trailingNulls: Int,
    vararg items: String
) : PagedList<String>(
    PagedStorage(),
    TestExecutor(),
    TestExecutor(),
    null,
    PagedList.Config.Builder().setPageSize(1).build()
), PagedStorage.Callback {
    val list = items.toList()
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

    override val isContiguous = true

    override val lastKey: Any? = null

    override val isDetached
        get() = detached

    override fun detach() {
        detached = true
    }

    override fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<String>, callback: Callback) {}

    override fun dispatchCurrentLoadState(callback: LoadStateListener) {}

    override fun loadAroundInternal(index: Int) {}

    override fun onInitialized(count: Int) {}

    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {}

    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {}

    override fun onPagePlaceholderInserted(pageIndex: Int) {}

    override fun onPageInserted(start: Int, count: Int) {}

    override val dataSource = ListDataSource(list)

    override fun onPagesRemoved(startOfDrops: Int, count: Int) = notifyRemoved(startOfDrops, count)

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) =
        notifyChanged(startOfDrops, count)
}
