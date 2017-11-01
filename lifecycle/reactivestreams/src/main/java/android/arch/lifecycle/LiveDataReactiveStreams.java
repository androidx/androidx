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

package android.arch.lifecycle;

import android.arch.core.executor.ArchTaskExecutor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.ref.WeakReference;

/**
 * Adapts {@link LiveData} input and output to the ReactiveStreams spec.
 */
@SuppressWarnings("WeakerAccess")
public final class LiveDataReactiveStreams {
    private LiveDataReactiveStreams() {
    }

    /**
     * Adapts the given {@link LiveData} stream to a ReactiveStreams {@link Publisher}.
     *
     * <p>
     * By using a good publisher implementation such as RxJava 2.x Flowables, most consumers will
     * be able to let the library deal with backpressure using operators and not need to worry about
     * ever manually calling {@link Subscription#request}.
     *
     * <p>
     * On subscription to the publisher, the observer will attach to the given {@link LiveData}.
     * Once {@link Subscription#request) is called on the subscription object, an observer will be
     * connected to the data stream. Calling request(Long.MAX_VALUE) is equivalent to creating an
     * unbounded stream with no backpressure. If request with a finite count reaches 0, the observer
     * will buffer the latest item and emit it to the subscriber when data is again requested. Any
     * other items emitted during the time there was no backpressure requested will be dropped.
     */
    public static <T> Publisher<T> toPublisher(
            final LifecycleOwner lifecycle, final LiveData<T> liveData) {

        return new Publisher<T>() {
            boolean mObserving;
            boolean mCanceled;
            long mRequested;
            @Nullable
            T mLatest;

            @Override
            public void subscribe(final Subscriber<? super T> subscriber) {
                final Observer<T> observer = new Observer<T>() {
                    @Override
                    public void onChanged(@Nullable T t) {
                        if (mCanceled) {
                            return;
                        }
                        if (mRequested > 0) {
                            mLatest = null;
                            subscriber.onNext(t);
                            if (mRequested != Long.MAX_VALUE) {
                                mRequested--;
                            }
                        } else {
                            mLatest = t;
                        }
                    }
                };

                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(final long n) {
                        if (n < 0 || mCanceled) {
                            return;
                        }
                        ArchTaskExecutor.getInstance().executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCanceled) {
                                    return;
                                }
                                // Prevent overflowage.
                                mRequested = mRequested + n >= mRequested
                                        ? mRequested + n : Long.MAX_VALUE;
                                if (!mObserving) {
                                    mObserving = true;
                                    liveData.observe(lifecycle, observer);
                                } else if (mLatest != null) {
                                    observer.onChanged(mLatest);
                                    mLatest = null;
                                }
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                        if (mCanceled) {
                            return;
                        }
                        ArchTaskExecutor.getInstance().executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mCanceled) {
                                    return;
                                }
                                if (mObserving) {
                                    liveData.removeObserver(observer);
                                    mObserving = false;
                                }
                                mLatest = null;
                                mCanceled = true;
                            }
                        });
                    }
                });
            }

        };
    }

    /**
     * Creates an Observable {@link LiveData} stream from a ReactiveStreams publisher.
     *
     * <p>
     * When the LiveData becomes active, it subscribes to the emissions from the Publisher.
     *
     * <p>
     * When the LiveData becomes inactive, the subscription is cleared.
     * LiveData holds the last value emitted by the Publisher when the LiveData was active.
     * <p>
     * Therefore, in the case of a hot RxJava Observable, when a new LiveData {@link Observer} is
     * added, it will automatically notify with the last value held in LiveData,
     * which might not be the last value emitted by the Publisher.
     *
     * @param <T> The type of data hold by this instance.
     */
    public static <T> LiveData<T> fromPublisher(final Publisher<T> publisher) {
        return new PublisherLiveData<>(publisher);
    }

    /**
     * Defines a {@link LiveData} object that wraps a {@link Publisher}.
     *
     * <p>
     * When the LiveData becomes active, it subscribes to the emissions from the Publisher.
     *
     * <p>
     * When the LiveData becomes inactive, the subscription is cleared.
     * LiveData holds the last value emitted by the Publisher when the LiveData was active.
     * <p>
     * Therefore, in the case of a hot RxJava Observable, when a new LiveData {@link Observer} is
     * added, it will automatically notify with the last value held in LiveData,
     * which might not be the last value emitted by the Publisher.
     *
     * @param <T> The type of data hold by this instance.
     */
    private static class PublisherLiveData<T> extends LiveData<T> {
        private WeakReference<Subscription> mSubscriptionRef;
        private final Publisher mPublisher;
        private final Object mLock = new Object();

        PublisherLiveData(@NonNull final Publisher publisher) {
            mPublisher = publisher;
        }

        @Override
        protected void onActive() {
            super.onActive();

            mPublisher.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription s) {
                    // Don't worry about backpressure. If the stream is too noisy then
                    // backpressure can be handled upstream.
                    synchronized (mLock) {
                        s.request(Long.MAX_VALUE);
                        mSubscriptionRef = new WeakReference<>(s);
                    }
                }

                @Override
                public void onNext(final T t) {
                    postValue(t);
                }

                @Override
                public void onError(Throwable t) {
                    synchronized (mLock) {
                        mSubscriptionRef = null;
                    }
                    // Errors should be handled upstream, so propagate as a crash.
                    throw new RuntimeException(t);
                }

                @Override
                public void onComplete() {
                    synchronized (mLock) {
                        mSubscriptionRef = null;
                    }
                }
            });

        }

        @Override
        protected void onInactive() {
            super.onInactive();
            synchronized (mLock) {
                WeakReference<Subscription> subscriptionRef = mSubscriptionRef;
                if (subscriptionRef != null) {
                    Subscription subscription = subscriptionRef.get();
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    mSubscriptionRef = null;
                }
            }
        }
    }
}
