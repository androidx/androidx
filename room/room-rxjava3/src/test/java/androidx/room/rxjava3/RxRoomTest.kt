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
package androidx.room.rxjava3

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class RxRoomTest {
    @get:Rule var mExecutor = CountingTaskExecutorRule()
    private lateinit var mDatabase: RoomDatabase
    private lateinit var mInvalidationTracker: InvalidationTracker
    private val mAddedObservers: MutableList<InvalidationTracker.Observer> = ArrayList()

    @Before
    fun init() {
        mDatabase = mock()
        mInvalidationTracker = mock()
        whenever(mDatabase.invalidationTracker).thenReturn(mInvalidationTracker)
        whenever(mDatabase.queryExecutor).thenReturn(ArchTaskExecutor.getIOThreadExecutor())
        doAnswer { invocation: InvocationOnMock ->
                mAddedObservers.add(invocation.arguments[0] as InvalidationTracker.Observer)
                null
            }
            .whenever(mInvalidationTracker)
            .addObserver(any())
    }

    @Test
    fun basicAddRemove_Flowable() {
        val flowable = createFlowable(mDatabase, "a", "b")
        verify(mInvalidationTracker, never()).addObserver(any())
        var disposable = flowable.subscribe()
        verify(mInvalidationTracker).addObserver(any())
        assertThat(mAddedObservers.size).isEqualTo(1)
        val observer = mAddedObservers[0]
        disposable.dispose()
        verify(mInvalidationTracker).removeObserver(observer)
        disposable = flowable.subscribe()
        verify(mInvalidationTracker, times(2)).addObserver(any())
        assertThat(mAddedObservers.size).isEqualTo(2)
        assertThat(mAddedObservers[1]).isNotSameInstanceAs(observer)

        val observer2 = mAddedObservers[1]
        disposable.dispose()
        verify(mInvalidationTracker).removeObserver(observer2)
    }

    @Test
    fun basicAddRemove_Observable() {
        val observable = createObservable(mDatabase, "a", "b")
        verify(mInvalidationTracker, never()).addObserver(any())
        var disposable = observable.subscribe()
        verify(mInvalidationTracker).addObserver(any())
        assertThat(mAddedObservers.size).isEqualTo(1)
        val observer = mAddedObservers[0]
        disposable.dispose()
        verify(mInvalidationTracker).removeObserver(observer)
        disposable = observable.subscribe()
        verify(mInvalidationTracker, times(2)).addObserver(any())
        assertThat(mAddedObservers.size).isEqualTo(2)
        assertThat(mAddedObservers[1]).isNotSameInstanceAs(observer)

        val observer2 = mAddedObservers[1]
        disposable.dispose()
        verify(mInvalidationTracker).removeObserver(observer2)
    }

    @Test
    fun basicNotify_Flowable() {
        val tables = arrayOf("a", "b")
        val tableSet: Set<String> = HashSet(listOf(*tables))
        val flowable = createFlowable(mDatabase, *tables)
        val consumer = CountingConsumer()
        val disposable = flowable.subscribe(consumer)
        assertThat(mAddedObservers.size).isEqualTo(1)
        val observer = mAddedObservers[0]
        assertThat(consumer.mCount).isEqualTo(1)
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(2)
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(3)
        disposable.dispose()
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(3)
    }

    @Test
    fun basicNotify_Observable() {
        val tables = arrayOf("a", "b")
        val tableSet: Set<String> = HashSet(listOf(*tables))
        val observable = createObservable(mDatabase, *tables)
        val consumer = CountingConsumer()
        val disposable = observable.subscribe(consumer)
        assertThat(mAddedObservers.size).isEqualTo(1)
        val observer = mAddedObservers[0]
        assertThat(consumer.mCount).isEqualTo(1)
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(2)
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(3)
        disposable.dispose()
        observer.onInvalidated(tableSet)
        assertThat(consumer.mCount).isEqualTo(3)
    }

    @Test
    @Suppress("DEPRECATION")
    fun internalCallable_Flowable() {
        val value = AtomicReference<Any>(null)
        val tables = arrayOf("a", "b")
        val tableSet: Set<String> = HashSet(listOf(*tables))
        val flowable = createFlowable(mDatabase, false, tables, Callable { value.get() })
        val consumer = CountingConsumer()
        val disposable = flowable.subscribe(consumer)
        drain()
        val observer = mAddedObservers[0]
        // no value because it is null
        assertThat(consumer.mCount).isEqualTo(0)
        value.set("bla")
        observer.onInvalidated(tableSet)
        drain()
        // get value
        assertThat(consumer.mCount).isEqualTo(1)
        observer.onInvalidated(tableSet)
        drain()
        // get value
        assertThat(consumer.mCount).isEqualTo(2)
        value.set(null)
        observer.onInvalidated(tableSet)
        drain()
        // no value
        assertThat(consumer.mCount).isEqualTo(2)
        disposable.dispose()
    }

    @Test
    @Suppress("DEPRECATION")
    fun internalCallable_Observable() {
        val value = AtomicReference<Any>(null)
        val tables = arrayOf("a", "b")
        val tableSet: Set<String> = HashSet(listOf(*tables))
        val flowable = createObservable(mDatabase, false, tables, Callable { value.get() })
        val consumer = CountingConsumer()
        val disposable = flowable.subscribe(consumer)
        drain()
        val observer = mAddedObservers[0]
        // no value because it is null
        assertThat(consumer.mCount).isEqualTo(0)
        value.set("bla")
        observer.onInvalidated(tableSet)
        drain()
        // get value
        assertThat(consumer.mCount).isEqualTo(1)
        observer.onInvalidated(tableSet)
        drain()
        // get value
        assertThat(consumer.mCount).isEqualTo(2)
        value.set(null)
        observer.onInvalidated(tableSet)
        drain()
        // no value
        assertThat(consumer.mCount).isEqualTo(2)
        disposable.dispose()
    }

    @Test
    @Suppress("DEPRECATION")
    fun exception_Flowable() {
        val flowable =
            createFlowable<String>(
                mDatabase,
                false,
                arrayOf("a"),
                Callable { throw Exception("i want exception") }
            )
        val subscriber = TestSubscriber<String>()
        flowable.subscribe(subscriber)
        drain()
        subscriber.assertError { throwable: Throwable -> throwable.message == "i want exception" }
    }

    @Test
    @Suppress("DEPRECATION")
    fun exception_Observable() {
        val flowable =
            createObservable<String>(
                mDatabase,
                false,
                arrayOf("a"),
                Callable { throw Exception("i want exception") }
            )
        val observer = TestObserver<String>()
        flowable.subscribe(observer)
        drain()
        observer.assertError { throwable: Throwable -> throwable.message == "i want exception" }
    }

    @Throws(Exception::class)
    private fun drain() {
        mExecutor.drainTasks(10, TimeUnit.SECONDS)
    }

    private class CountingConsumer : Consumer<Any> {
        var mCount = 0

        override fun accept(o: Any) {
            mCount++
        }
    }
}
