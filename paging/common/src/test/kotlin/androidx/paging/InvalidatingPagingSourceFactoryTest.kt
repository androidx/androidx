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

import org.junit.Test
import kotlin.test.assertEquals


class InvalidatingPagingSourceFactoryTest {

    @Test
    fun getPagingSource() {

        val testFactory = object : InvalidatingPagingSourceFactory<Int, Int>() {
            override fun create(): PagingSource<Int, Int> = TestPagingSource();
        }

        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()

        assertEquals(testFactory.listOfPagingSources.size, 4)
    }

    @Test
    fun invalidateRemoveFromList() {

        val testFactory = object : InvalidatingPagingSourceFactory<Int, Int>() {
            override fun create(): PagingSource<Int, Int> = TestPagingSource();
        }

        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()

        assertEquals(testFactory.listOfPagingSources.size, 4)

        testFactory.invalidate()

        assertEquals(testFactory.listOfPagingSources.size, 0)

    }

    @Test
    fun invalidatePagingSource() {

        val testFactory = object : InvalidatingPagingSourceFactory<Int, Int>() {
            override fun create(): PagingSource<Int, Int> = TestPagingSource();
        }

        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()
        testFactory.invoke()

        testFactory.listOfPagingSources.forEach {
            it.registerInvalidatedCallback {
                assertEquals(it.invalid, true)
            }
        }

        testFactory.invalidate()
    }
}

