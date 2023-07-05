/*
 * Copyright 2020 The Android Open Source Project
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

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

class TestPagingDataDiffer<T : Any>(mainContext: CoroutineContext = Dispatchers.Main) :
    PagingDataDiffer<T>(noopDifferCallback, mainContext) {

    val currentList: List<T> get() = List(size) { i -> get(i)!! }

    override suspend fun presentNewList(
        previousList: NullPaddedList<T>,
        newList: NullPaddedList<T>,
        lastAccessedIndex: Int,
        onListPresentable: () -> Unit,
    ): Int? {
        onListPresentable()
        return null
    }

    companion object {
        private val noopDifferCallback = object : DifferCallback {
            override fun onChanged(position: Int, count: Int) {}
            override fun onInserted(position: Int, count: Int) {}
            override fun onRemoved(position: Int, count: Int) {}
        }
    }
}
