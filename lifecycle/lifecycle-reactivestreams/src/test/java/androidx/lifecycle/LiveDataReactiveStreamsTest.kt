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
package androidx.lifecycle

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.testing.TestLifecycleOwner
import io.reactivex.Flowable.fromPublisher
import io.reactivex.Flowable.just
import io.reactivex.processors.PublishProcessor.create
import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.AsyncSubject
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class LiveDataReactiveStreamsTest {

    @JvmField
    @Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private lateinit var lifecycleOwner: TestLifecycleOwner
    private val liveDataOutput = ArrayList<String>()
    private val observer = Observer<String> { s -> liveDataOutput.add(s) }
    private val outputProcessor = ReplayProcessor.create<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun init() {
        lifecycleOwner = TestLifecycleOwner(
            Lifecycle.State.RESUMED,
            UnconfinedTestDispatcher(null, null)
        )
    }

    @Test
    fun convertsFromPublisher() {
        val processor = create<String>()
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("foo")
        processor.onNext("bar")
        processor.onNext("baz")
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "bar", "baz")))
    }

    @Test
    fun convertsFromPublisherSubscribeWithDelay() {
        val processor = create<String>()
        processor.delaySubscription(100, SECONDS, backgroundScheduler)
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("foo")
        liveData.removeObserver(observer)
        backgroundScheduler.triggerActions()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("bar")
        processor.onNext("baz")
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "foo", "bar", "baz")))
    }

    @Test
    fun convertsFromPublisherThrowsException() {
        val processor = create<String>()
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        val exception = IllegalStateException("test exception")
        try {
            processor.onError(exception)
            fail("Runtime Exception expected")
        } catch (ex: RuntimeException) {
            assertEquals(ex.cause, exception)
        }
    }

    @Test
    fun convertsFromPublisherWithMultipleObservers() {
        val output2 = ArrayList<String>()
        val processor = create<String>()
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("foo")
        processor.onNext("bar")

        // The second observer should only get the newest value and any later values.
        liveData.observe(lifecycleOwner) { s -> output2.add(s) }
        processor.onNext("baz")
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "bar", "baz")))
        assertThat(output2, `is`(mutableListOf("bar", "baz")))
    }

    @Test
    fun convertsFromPublisherWithMultipleObserversAfterInactive() {
        val output2 = ArrayList<String>()
        val processor = create<String>()
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("foo")
        processor.onNext("bar")

        // The second observer should only get the newest value and any later values.
        liveData.observe(lifecycleOwner) { s -> output2.add(s) }
        liveData.removeObserver(observer)
        processor.onNext("baz")
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "bar")))
        assertThat(output2, `is`(mutableListOf("bar", "baz")))
    }

    @Test
    fun convertsFromPublisherAfterInactive() {
        val processor = create<String>()
        val liveData = processor.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("foo")
        liveData.removeObserver(observer)
        processor.onNext("bar")
        liveData.observe(lifecycleOwner, observer)
        processor.onNext("baz")
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "foo", "baz")))
    }

    @Test
    fun convertsFromPublisherManagesSubscriptions() {
        val processor = create<String>()
        val liveData = processor.toLiveData()
        assertThat(processor.hasSubscribers(), `is`(false))
        liveData.observe(lifecycleOwner, observer)

        // once the live data is active, there's a subscriber
        assertThat(processor.hasSubscribers(), `is`(true))
        liveData.removeObserver(observer)
        // once the live data is inactive, the subscriber is removed
        assertThat(processor.hasSubscribers(), `is`(false))
    }

    @Test
    fun convertsFromAsyncPublisher() {
        val input = just("foo").concatWith(just("bar", "baz")
                .observeOn(backgroundScheduler))
        val liveData = input.toLiveData()
        liveData.observe(lifecycleOwner, observer)
        assertThat(liveDataOutput, `is`(listOf("foo")))
        backgroundScheduler.triggerActions()
        assertThat(liveDataOutput, `is`(mutableListOf("foo", "bar", "baz")))
    }

    @Test
    fun convertsToPublisherWithSyncData() {
        val liveData = MutableLiveData<String>()
        liveData.value = "foo"
        assertThat(liveData.value, `is`("foo"))
        fromPublisher(toPublisher(lifecycleOwner, liveData))
            .subscribe(outputProcessor)
        liveData.value = "bar"
        liveData.value = "baz"
        assertThat(
            outputProcessor.getValues(arrayOf()),
            `is`(arrayOf("foo", "bar", "baz"))
        )
    }

    @Test
    fun convertingToPublisherIsCancelable() {
        val liveData = MutableLiveData<String>()
        liveData.value = "foo"
        assertThat(liveData.value, `is`("foo"))
        val disposable = fromPublisher(toPublisher(lifecycleOwner, liveData))
            .subscribe { s -> liveDataOutput.add(s) }
        liveData.value = "bar"
        liveData.value = "baz"
        assertThat(liveData.hasObservers(), `is`(true))
        disposable.dispose()
        liveData.value = "fizz"
        liveData.value = "buzz"
        assertThat(liveDataOutput, `is`(mutableListOf<String?>("foo", "bar", "baz")))
        // Canceling disposable should also remove livedata mObserver.
        assertThat(liveData.hasObservers(), `is`(false))
    }

    @Test
    fun convertsToPublisherWithBackpressure() {
        val liveData = MutableLiveData<String>()
        val subscriptionSubject = AsyncSubject.create<Subscription>()
        fromPublisher(toPublisher<String>(lifecycleOwner, liveData))
            .subscribe(object : Subscriber<String> {
                override fun onSubscribe(s: Subscription) {
                    subscriptionSubject.onNext(s)
                    subscriptionSubject.onComplete()
                }

                override fun onNext(s: String) {
                    outputProcessor.onNext(s)
                }

                override fun onError(t: Throwable) {
                    throw RuntimeException(t)
                }

                override fun onComplete() {}
            })

        // Subscription should have happened synchronously. If it didn't, this will deadlock.
        val subscription = subscriptionSubject.blockingSingle()
        subscription.request(1)
        assertThat(outputProcessor.getValues(arrayOf()), `is`(arrayOf()))
        liveData.value = "foo"
        assertThat(outputProcessor.getValues(arrayOf()), `is`(arrayOf("foo")))
        subscription.request(2)
        liveData.value = "baz"
        liveData.value = "fizz"
        assertThat(
            outputProcessor.getValues(arrayOf()),
            `is`(arrayOf("foo", "baz", "fizz"))
        )

        // 'nyan' will be dropped as there is nothing currently requesting a stream.
        liveData.value = "nyan"
        liveData.value = "cat"
        assertThat(
            outputProcessor.getValues(arrayOf()),
            `is`(arrayOf("foo", "baz", "fizz"))
        )

        // When a new request comes in, the latest value will be pushed.
        subscription.request(1)
        assertThat(
            outputProcessor.getValues(arrayOf()),
            `is`(arrayOf("foo", "baz", "fizz", "cat"))
        )
    }

    @Test
    fun convertsToPublisherWithAsyncData() {
        val liveData = MutableLiveData<String>()
        fromPublisher(toPublisher(lifecycleOwner, liveData))
            .observeOn(backgroundScheduler)
            .subscribe(outputProcessor)
        liveData.value = "foo"
        assertThat(outputProcessor.getValues(arrayOf()), `is`(arrayOf()))
        backgroundScheduler.triggerActions()
        assertThat(outputProcessor.getValues(arrayOf()), `is`(arrayOf("foo")))
        liveData.value = "bar"
        liveData.value = "baz"
        assertThat(outputProcessor.getValues(arrayOf()), `is`(arrayOf("foo")))
        backgroundScheduler.triggerActions()
        assertThat(
            outputProcessor.getValues(arrayOf()),
            `is`(arrayOf("foo", "bar", "baz"))
        )
    }

    companion object {
        private val backgroundScheduler = TestScheduler()
    }
}
