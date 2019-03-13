/*
 * Copyright 2017 The Android Open Source Project
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

import androidx.paging.futures.DirectExecutor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("DEPRECATION")
@RunWith(JUnit4::class)
class TiledDataSourceTest {

    fun TiledDataSource<String>.loadInitial(
        startPosition: Int,
        count: Int,
        pageSize: Int
    ): List<String> {
        initExecutor(DirectExecutor.INSTANCE)
        return loadInitial(PositionalDataSource.LoadInitialParams(
            startPosition, count, pageSize, true)).get().data
    }

    @Test
    fun loadInitialEmpty() {
        class EmptyDataSource : TiledDataSource<String>() {
            override fun countItems(): Int {
                return 0
            }

            override fun loadRange(startPosition: Int, count: Int): List<String> {
                return emptyList()
            }
        }

        assertEquals(emptyList<String>(), EmptyDataSource().loadInitial(0, 1, 5))
    }

    @Test
    fun loadInitialTooLong() {
        val list = List(26) { "" + 'a' + it }
        class AlphabetDataSource : TiledDataSource<String>() {
            override fun countItems(): Int {
                return list.size
            }

            override fun loadRange(startPosition: Int, count: Int): List<String> {
                return list.subList(startPosition, startPosition + count)
            }
        }
        // baseline behavior
        assertEquals(list, AlphabetDataSource().loadInitial(0, 26, 10))
        assertEquals(list, AlphabetDataSource().loadInitial(50, 26, 10))
    }
}
