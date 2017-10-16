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

import android.arch.core.executor.AppToolkitTaskExecutor;
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
                        AppToolkitTaskExecutor.getInstance().executeOnMainThread(new Runnable() {
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
                        AppToolkitTaskExecutor.getInstance().executeOnMainThread(new Runnable() {
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
     */
    public static <T> LiveData<T> fromPublisher(final Publisher<T> publisher) {
        MutableLiveData<T> liveData = new MutableLiveData<>();
        // Since we don't have a way to directly observe cancels, weakly hold the live data.
        final WeakReference<MutableLiveData<T>> liveDataRef = new WeakReference<>(liveData);

        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                // Don't worry about backpressure. If the stream is too noisy then backpressure can
                // be handled upstream.
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final T t) {
                final LiveData<T> liveData = liveDataRef.get();
                if (liveData != null) {
                    liveData.postValue(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Errors should be handled upstream, so propagate as a crash.
                throw new RuntimeException(t);
            }

            @Override
            public void onComplete() {
            }
        });

        return liveData;
    }

}
