/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.support.test.filters.SmallTest
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RxPagedListBuilderTest {

    /**
     * Creates a data source that will sequentially supply the passed lists
     */
    private fun testDataSourceSequence(data: List<List<String>>): DataSource.Factory<Int, String> {
        return object : DataSource.Factory<Int, String>() {
            var localData = data
            override fun create(): DataSource<Int, String> {
                val currentList = localData.first()
                localData = localData.drop(1)
                return ListDataSource<String>(currentList)
            }
        }
    }

    @Test
    fun basic() {
        val factory = testDataSourceSequence(listOf(listOf("a", "b"), listOf("c", "d")))
        val scheduler = TestScheduler()
        val observable = RxPagedListBuilder(factory, 10)
                .setFetchScheduler(scheduler)
                .setNotifyScheduler(scheduler)
                .buildObservable()
        val observer = TestObserver<PagedList<String>>()

        observable.subscribe(observer)

        // initial state
        observer.assertNotComplete()
        observer.assertValueCount(0)

        // load first item
        scheduler.triggerActions()
        observer.assertValueCount(1)
        assertEquals(listOf("a", "b"), observer.values().first())

        // invalidate triggers second load
        observer.values().first().dataSource.invalidate()
        scheduler.triggerActions()
        observer.assertValueCount(2)
        assertEquals(listOf("c", "d"), observer.values().last())
    }

    @Test
    fun checkSchedulers() {
        val factory = testDataSourceSequence(listOf(listOf("a", "b"), listOf("c", "d")))
        val notifyScheduler = TestScheduler()
        val fetchScheduler = TestScheduler()
        val observable: Observable<PagedList<String>> = RxPagedListBuilder(
                factory, 10)
                .setFetchScheduler(fetchScheduler)
                .setNotifyScheduler(notifyScheduler)
                .buildObservable()
        val observer = TestObserver<PagedList<String>>()
        observable.subscribe(observer)

        // notify has nothing to do
        notifyScheduler.triggerActions()
        observer.assertValueCount(0)

        // fetch creates list, but observer doesn't see
        fetchScheduler.triggerActions()
        observer.assertValueCount(0)

        // now notify reveals item
        notifyScheduler.triggerActions()
        observer.assertValueCount(1)
    }
}
