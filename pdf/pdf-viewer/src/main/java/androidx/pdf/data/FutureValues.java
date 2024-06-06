/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/** Basic {@link FutureValue}s and related helpers. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FutureValues {
    private FutureValues() {
    }

    /** Creates a {@link FutureValue} with the given value. */
    @NonNull
    public static <T> FutureValue<T> newImmediateValue(T value) {
        return new ImmediateValue<T>(value);
    }

    /**
     * Creates a {@link FutureValue} with an immediate failure (the passed value).
     *
     * @param error the exception to be delivered on fail.
     */
    @NonNull
    public static <T> FutureValue<T> immediateFail(final @NonNull Exception error) {
        return callback -> callback.failed(error);
    }

    /** Creates a {@link FutureValue} that decouples setting and reading progress and results. */
    @NonNull
    public static <T> SettableFutureValue<T> newSettableValue() {
        return new SettableFutureValue<T>();
    }

    /** A {@link FutureValue.Callback} wrapper interface around a {@link SettableFutureValue}. */
    @NonNull
    public static <T> FutureValue.Callback<T> setterCallback(
            final @NonNull SettableFutureValue<T> targetFuture) {
        return new FutureValue.Callback<T>() {

            @Override
            public void available(T value) {
                targetFuture.set(value);
            }

            @Override
            public void failed(@NonNull Throwable thrown) {
                targetFuture.fail(thrown);
            }

            @Override
            public void progress(float progress) {
                targetFuture.progress(progress);
            }
        };
    }

    /**
     * Creates a {@link FutureValue} that converts the results of a source {@link FutureValue} on
     * the fly. If the conversion fails, the resulting future will report a failure with the
     * corresponding exception.
     *
     * @param sourceFuture the source {@link FutureValue}.
     * @param converter    the {@link Converter} used to convert results from source to returned
     */
    @NonNull
    public static <F, T> FutureValue<T> convert(final @NonNull FutureValue<F> sourceFuture,
            final @NonNull Converter<F, T> converter) {
        return new FutureValue<T>() {
            @Override
            public void get(final FutureValue.Callback<T> target) {
                final FutureValue.Callback<F> convertCallback = new FutureValue.Callback<F>() {
                    @Override
                    public void progress(float progress) {
                        target.progress(progress);
                    }

                    @Override
                    public void failed(@NonNull Throwable thrown) {
                        target.failed(thrown);
                    }

                    @Override
                    public void available(F sourceValue) {
                        try {
                            target.available(converter.convert(sourceValue));
                        } catch (Exception e) {
                            target.failed(e);
                        }
                    }
                };

                sourceFuture.get(convertCallback);
            }
        };
    }

    /** Creates a new Converter that chains 2 other converters. */
    @NonNull
    public static <F, T, V> Converter<F, T> combine(final @NonNull Converter<F, V> converter1,
            final @NonNull Converter<V, T> converter2) {
        return new Converter<F, T>() {
            @Override
            public T convert(F from) {
                return converter2.convert(converter1.convert(from));
            }
        };
    }

    /**
     * Observe an {@link ObservableValue} as a {@link FutureValue}: wait for it to next hit one of
     * the given target values. It will start observing the observable value after
     * {@link FutureValue#get} is called. The Future might complete immediately (if the observable
     * value already equals the target), in the future (when it changes to it), or never. It never
     * fails (Note that there is not time-out to trigger an automatic fail in case the condition
     * never materializes).
     *
     * <p>This {@link FutureValue} cannot be used more than once (i.e. a second call to {@link
     * FutureValue#get} is illegal).
     *
     * @param obs    the observable value.
     * @param target the value(s) that will trigger the {@link FutureValue}'s completion.
     * @return A {@link FutureValue} that completes when the observable value changes to target.
     */
    @SafeVarargs
    @NonNull
    public static <T> FutureValue<T> observeAsFuture(final @NonNull ObservableValue<T> obs,
            @NonNull T... target) {
        Preconditions.checkNotNull(obs);
        final Set<T> targetSet = new HashSet<>(Arrays.asList(target));
        return new FutureValue<T>() {

            /**
             * key is null as long as this Future hasn't been used (before
             * {@link FutureValue#get} is called). It becomes non-null after that (holds the
             * {@link ValueObserver}'s registration key if applicable).
             */
            @Nullable
            private Object mKey;

            @Override
            public void get(final FutureValue.Callback<T> callback) {
                Preconditions.checkState(mKey == null, "Can't reuse this future twice: " + mKey);
                if (targetSet.contains(obs.get())) {
                    mKey = Boolean.TRUE;
                    callback.available(obs.get());
                } else {
                    mKey = obs.addObserver((oldValue, newValue) -> {
                        if (targetSet.contains(newValue)) {
                            obs.removeObserver(mKey);
                            mKey = Boolean.FALSE;
                            callback.available(newValue);
                        }
                    });
                }
            }
        };
    }

    /**
     * An object that can be used for converting an object of type F to one of type T.
     *
     * @param <F> original type of the object
     * @param <T> type to be converted to
     */
    public interface Converter<F, T> {

        /** Converts object of type F to type T */
        T convert(F from);
    }

    /**
     * A convenient base implementation of {@link FutureValue.Callback} that does nothing. It
     * assumes no failures are reported and thus handles them.
     * Subclasses should override {@link #failed} if failures are expected.
     *
     * @param <T> the type of result
     */
    public static class SimpleCallback<T> implements FutureValue.Callback<T> {

        @Override
        public void available(T value) {
        }

        @Override
        public void failed(@NonNull Throwable thrown) {
        }

        @Override
        public void progress(float progress) {
        }

        @NonNull
        @Override
        public String toString() {
            return "SimpleCallback (unspecified)";
        }
    }

    /**
     * Allows for a blocking get.
     *
     * <p>Useful for when waiting for an execution to complete.
     *
     * @param <T> the type of result
     */
    public static class BlockingCallback<T> extends SimpleCallback<T> {
        Semaphore mSemaphore = new Semaphore(0);
        T mSuccess = null;
        Throwable mFail = null;

        @Override
        public void available(T value) {
            mSuccess = value;
            mSemaphore.release();
        }

        @Override
        public void failed(@NonNull Throwable thrown) {
            mFail = thrown;
            mSemaphore.release();
        }

        /** Acquires semaphore until data comes through callback. */
        @WorkerThread
        public T getBlocking() {
            try {
                mSemaphore.acquire(); // wait until data comes through the callback.
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } finally {
                // Release as someone else is probably now waiting
                mSemaphore.release();
            }
            if (mFail != null) {
                throw new IllegalStateException(mFail);
            }
            return mSuccess;
        }
    }

    /**
     * A simple {@link FutureValue} that is constructed with a value and returns it immediately when
     * {@link #get(Callback)} is called.
     */
    private static class ImmediateValue<T> implements FutureValue<T> {
        private final T mValue;

        ImmediateValue(T value) {
            this.mValue = value;
        }

        @Override
        public void get(Callback<T> callback) {
            callback.available(mValue);
        }
    }

    /**
     * A {@link FutureValue} which wraps a {@link Supplier} for another {@link FutureValue}. This
     * is designed so that a computation which returns a {@link FutureValue} is not started until
     * {@link #get(Callback)} is called for the first time (a "lazy" future).
     *
     * @param <T> the type of the value
     */
    public static class DeferredFutureValue<T> implements FutureValue<T> {

        /**
         * Represents an operation which should not be started until {@link #get(Callback)} is
         * called for the first time.
         */
        private final Supplier<FutureValue<T>> mComputation;

        public DeferredFutureValue(@NonNull Supplier<FutureValue<T>> computation) {
            this.mComputation = computation;
        }

        @Override
        public void get(@Nullable Callback<T> callback) {
            try {
                mComputation.supply(progress -> {
                }).get(callback);
            } catch (Exception e) {
                // Most errors should be piped through the computation's Future to the callback.
                // This captures errors thrown during the supply.
                callback.failed(e);
            }
        }
    }

    /**
     * A {@link FutureValue} that accepts multiple {@link FutureValue.Callback}s and can be set.
     *
     * @param <T> the type of the value to be returned
     */
    public static class SettableFutureValue<T> implements FutureValue<T> {
        private final Collection<Callback<T>> mCallbacks = new ArrayList<>(2);
        private T mValue;
        private Throwable mThrown;

        /**
         * Set the successful value resulting from the asynchronous operation. Any callbacks will be
         * called immediately. The value will be retained to call any new callbacks. All callback
         * references will be removed.
         *
         * @param value The result of the asynchronous operation.
         */
        public void set(T value) {
            Preconditions.checkNotNull(value);
            checkNotSet(value.toString());
            this.mValue = value;
            for (Callback<T> callback : mCallbacks) {
                callback.available(value);
            }
            mCallbacks.clear();
        }

        /**
         * Notify all the callbacks of a progress update. This implementation will not store
         * intermediate progress values. So if a callback is registered after the last progress
         * value has been sent, it will not get the previous progress values. Progress is no longer
         * reported after this future completes (i.e. if {@link #isSet()}).
         *
         * @param progress is a measure of how much progress has been done [0-1].
         */
        public void progress(float progress) {
            if (!isSet()) {
                for (Callback<T> callback : mCallbacks) {
                    callback.progress(progress);
                }
            }
        }

        /**
         * Set the exception thrown while getting the value. Any callbacks will be called
         * immediately. The exception will be retained to call any new callbacks. All callback
         * references will be removed.
         *
         * @param thrown The problem encountered while getting the result of the asynchronous
         *               operation.
         */
        public void fail(@NonNull Throwable thrown) {
            checkNotSet(thrown.toString());
            this.mThrown = Preconditions.checkNotNull(thrown);
            for (Callback<T> callback : mCallbacks) {
                callback.failed(thrown);
            }
            mCallbacks.clear();
        }

        /**
         * Wraps a {@link String} error message in an {@link Exception}. Otherwise, it has the same
         * behaviour as {@link SettableFutureValue#fail(Throwable)}.
         */
        public void fail(@NonNull String errorMessage) {
            fail(new Exception(errorMessage));
        }

        @Override
        public void get(@Nullable Callback<T> callback) {
            if (mValue != null) {
                callback.available(mValue);
            } else if (mThrown != null) {
                callback.failed(mThrown);
            } else {
                Preconditions.checkNotNull(callback);
                mCallbacks.add(callback);
            }
        }

        public boolean isSet() {
            return (mValue != null || mThrown != null);
        }

        private void checkNotSet(String newValue) {
            Preconditions.checkState(mValue == null,
                    String.format("Value has already been set (%s) : %s", mValue, newValue));
            Preconditions.checkState(mThrown == null,
                    String.format("Exception was already set (%s) : %s", mThrown, newValue));
        }
    }
}
