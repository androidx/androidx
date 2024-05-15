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
@file:JvmName("LiveDataReactiveStreams")

package androidx.lifecycle

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import java.util.concurrent.atomic.AtomicReference
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Adapts the given [LiveData] stream to a ReactiveStreams [Publisher].
 *
 * By using a good publisher implementation such as RxJava 2.x Flowables, most consumers will
 * be able to let the library deal with backpressure using operators and not need to worry about
 * ever manually calling [Subscription.request].
 *
 * On subscription to the publisher, the observer will attach to the given [LiveData].
 * Once [Subscription.request] is called on the subscription object, an observer will be
 * connected to the data stream. Calling `request(Long.MAX_VALUE)` is equivalent to creating an
 * unbounded stream with no backpressure. If request with a finite count reaches 0, the observer
 * will buffer the latest item and emit it to the subscriber when data is again requested. Any
 * other items emitted during the time there was no backpressure requested will be dropped.
 */
@SuppressLint("LambdaLast")
@Deprecated(
    message = "Use the extension method `liveData.toPublisher(lifecycleOwner)` instead.",
    replaceWith = ReplaceWith(
        expression = "liveData.toPublisher(lifecycleOwner)",
        imports = arrayOf("androidx.lifecycle.toPublisher"),
    )
)
fun <T> toPublisher(lifecycle: LifecycleOwner, liveData: LiveData<T>): Publisher<T> {
    return LiveDataPublisher(lifecycle, liveData)
}

/**
 * Adapts the given [LiveData] stream to a ReactiveStreams [Publisher].
 *
 * By using a good publisher implementation such as RxJava 2.x Flowables, most consumers will
 * be able to let the library deal with backpressure using operators and not need to worry about
 * ever manually calling [Subscription.request].
 *
 * On subscription to the publisher, the observer will attach to the given [LiveData].
 * Once [Subscription.request] is called on the subscription object, an observer will be
 * connected to the data stream. Calling `request(Long.MAX_VALUE)` is equivalent to creating an
 * unbounded stream with no backpressure. If request with a finite count reaches 0, the observer
 * will buffer the latest item and emit it to the subscriber when data is again requested. Any
 * other items emitted during the time there was no backpressure requested will be dropped.
 */
@SuppressLint("LambdaLast")
@JvmName("toPublisher")
fun <T> LiveData<T>.toPublisher(lifecycle: LifecycleOwner): Publisher<T> {
    return LiveDataPublisher(lifecycle, this)
}

private class LiveDataPublisher<T>(
    val lifecycle: LifecycleOwner,
    val liveData: LiveData<T>
) : Publisher<T> {
    override fun subscribe(subscriber: Subscriber<in T>) {
        subscriber.onSubscribe(LiveDataSubscription(subscriber, lifecycle, liveData))
    }

    class LiveDataSubscription<T> constructor(
        val subscriber: Subscriber<in T>,
        val lifecycle: LifecycleOwner,
        val liveData: LiveData<T>
    ) : Subscription, Observer<T?> {
        @Volatile
        var canceled = false

        // used on main thread only
        var observing = false
        var requested: Long = 0

        // used on main thread only
        var latest: T? = null

        override fun onChanged(value: T?) {
            if (canceled) {
                return
            }
            if (requested > 0) {
                latest = null
                subscriber.onNext(value)
                if (requested != Long.MAX_VALUE) {
                    requested--
                }
            } else {
                latest = value
            }
        }

        override fun request(n: Long) {
            if (canceled) {
                return
            }
            ArchTaskExecutor.getInstance().executeOnMainThread(
                Runnable {
                    if (canceled) {
                        return@Runnable
                    }
                    if (n <= 0L) {
                        canceled = true
                        if (observing) {
                            liveData.removeObserver(this@LiveDataSubscription)
                            observing = false
                        }
                        latest = null
                        subscriber.onError(
                            IllegalArgumentException("Non-positive request")
                        )
                        return@Runnable
                    }

                    // Prevent overflowage.
                    requested =
                        if (requested + n >= requested) requested + n else Long.MAX_VALUE
                    if (!observing) {
                        observing = true
                        liveData.observe(lifecycle, this@LiveDataSubscription)
                    } else if (latest != null) {
                        onChanged(latest)
                        latest = null
                    }
                })
        }

        override fun cancel() {
            if (canceled) {
                return
            }
            canceled = true
            ArchTaskExecutor.getInstance().executeOnMainThread {
                if (observing) {
                    liveData.removeObserver(this@LiveDataSubscription)
                    observing = false
                }
                latest = null
            }
        }
    }
}

/**
 * Creates an observable [LiveData] stream from a ReactiveStreams [Publisher]}.
 *
 * When the LiveData becomes active, it subscribes to the emissions from the Publisher.
 *
 * When the LiveData becomes inactive, the subscription is cleared.
 * LiveData holds the last value emitted by the Publisher when the LiveData was active.
 *
 * Therefore, in the case of a hot RxJava Observable, when a new LiveData [Observer] is
 * added, it will automatically notify with the last value held in LiveData,
 * which might not be the last value emitted by the Publisher.
 *
 * Note that LiveData does NOT handle errors and it expects that errors are treated as states
 * in the data that's held. In case of an error being emitted by the publisher, an error will
 * be propagated to the main thread and the app will crash.
 */
@JvmName("fromPublisher")
fun <T> Publisher<T>.toLiveData(): LiveData<T> = PublisherLiveData(this)

/**
 * Defines a [LiveData] object that wraps a [Publisher].
 *
 * When the LiveData becomes active, it subscribes to the emissions from the Publisher.
 *
 * When the LiveData becomes inactive, the subscription is cleared.
 * LiveData holds the last value emitted by the Publisher when the LiveData was active.
 *
 * Therefore, in the case of a hot RxJava Observable, when a new LiveData [Observer] is
 * added, it will automatically notify with the last value held in LiveData,
 * which might not be the last value emitted by the Publisher.
 *
 * Note that LiveData does NOT handle errors and it expects that errors are treated as states
 * in the data that's held. In case of an error being emitted by the publisher, an error will
 * be propagated to the main thread and the app will crash.
 */
private class PublisherLiveData<T>(
    private val publisher: Publisher<T>
) : LiveData<T>() {
    val subscriber: AtomicReference<LiveDataSubscriber> = AtomicReference()

    override fun onActive() {
        super.onActive()
        val s = LiveDataSubscriber()
        subscriber.set(s)
        publisher.subscribe(s)
    }

    override fun onInactive() {
        super.onInactive()
        val s = subscriber.getAndSet(null)
        s?.cancelSubscription()
    }

    inner class LiveDataSubscriber : AtomicReference<Subscription>(), Subscriber<T> {
        override fun onSubscribe(s: Subscription) {
            if (compareAndSet(null, s)) {
                s.request(Long.MAX_VALUE)
            } else {
                s.cancel()
            }
        }

        override fun onNext(item: T) {
            postValue(item)
        }

        override fun onError(ex: Throwable) {
            subscriber.compareAndSet(this, null)
            ArchTaskExecutor.getInstance()
                .executeOnMainThread { // Errors should be handled upstream, so propagate as a crash.
                    throw RuntimeException(
                        "LiveData does not handle errors. Errors from " +
                            "publishers should be handled upstream and propagated as " +
                            "state",
                        ex
                    )
                }
        }

        override fun onComplete() {
            subscriber.compareAndSet(this, null)
        }

        fun cancelSubscription() {
            val s = get()
            s?.cancel()
        }
    }
}
