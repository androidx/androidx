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

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.data.FutureValue.Callback;
import androidx.pdf.data.FutureValues.Converter;
import androidx.pdf.data.FutureValues.SettableFutureValue;
import androidx.pdf.util.ThreadUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helpers to create {@link FutureValue}s that are ready to be used for UI operations: their
 * callbacks will always execute on the UI thread.
 * <p>
 * {@link #execute(Supplier)}:<br>
 * Creates a {@link FutureValue} and gets its value using a {@link Supplier} in a background thread.
 * The progress and result of {@link Supplier#supply(Progress)} are passed to {@link Callback} on
 * the main thread.
 * <pre>
 * {@code
 *   FutureValue<Cheese> FutureValueTask.execute(new Supplier() {
 *      public Cheese supply(Progress progress) {
 *          Cheese cheese = Cheese.createFresh();
 *          while (!cheese.isMature()) {
 *              progress.report(cheese.age() / Cheese.MATURE_AGE);
 *          }
 *          return cheese;
 *      }
 *   });
 * }
 * </pre>
 * <p>
 * {@link #immediateValue(Object)} / {@link #immediateFail(Exception)}:
 * Creates a {@link FutureValue} that will always run its callbacks on the UI thread, returning the
 * value that was passed to it (an actual value or an Exception).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class UiFutureValues {
    private static final String TAG = UiFutureValues.class.getSimpleName();
    private static final Executor DEFAULT_EXECUTOR = Executors.newFixedThreadPool(4,
            new ThreadFactoryBuilder().setNameFormat("PdfViewer-" + TAG + "-%d").build());
    private static Executor sExecutor = DEFAULT_EXECUTOR;
    private static boolean sDebug;

    private UiFutureValues() {
    }

    @VisibleForTesting
    static void overrideExecutor(Executor executor) {
        UiFutureValues.sExecutor = executor;
    }

    @VisibleForTesting
    static void resetExecutor() {
        sExecutor = DEFAULT_EXECUTOR;
    }

    /**
     * Creates a {@link FutureValue} with an immediate result (the passed value) that will always
     * deliver it (in {@link Callback#available}) on the UI thread.
     *
     * @param value the value to be returned. If it is an {@link Exception}, this will trigger
     *              {@link
     *              Callback#failed(Throwable)} to be called instead.
     */
    public static <T> FutureValue<T> immediateValue(final T value) {
        return callback -> ThreadUtils.runOnUiThread(() -> callback.available(value));
    }

    /**
     * Creates a {@link FutureValue} with an immediate failure (the passed value) that will always
     * deliver it (in {@link Callback#failed}) on the UI thread.
     *
     * <p>Do not use this method in testing as a background thread flushing can never complete.
     *
     * @param error the exception to be delivered on fail.
     */
    public static <T> FutureValue<T> immediateFail(final Exception error) {
        return callback -> ThreadUtils.runOnUiThread(() -> callback.failed(error));
    }

    /** Log the calling threads stack trace when exceptions occur in the background thread. */
    public static void setDebugTrace(boolean debug) {
        Log.v(TAG, "Setting debug trace to " + debug);
        UiFutureValues.sDebug = debug;
    }

    /** Wraps up a {@link Supplier} to supply a converted value. */
    public static <F, T> Supplier<T> postConvert(final Supplier<F> supplier,
            final Converter<F, T> converter) {
        return new Supplier<T>() {

            @Override
            public T supply(Progress progress) throws Exception {
                return converter.convert(supplier.supply(progress));
            }
        };
    }

    /**
     * Calls through to {@link #execute(Supplier)} in order to disambigute it from {@link
     * #execute(FutureValue)}.
     */
    public static <T> FutureValue<T> executeAsync(Supplier<T> supplier) {
        return execute(supplier);
    }

    /**
     * Offers an asynchronous interface (a {@link FutureValue}) on top of a synchronous one (a
     * {@link Supplier}). The callbacks of the {@link FutureValue} are run on the main thread.
     *
     * @return The value to be supplied at some point in the future.
     */
    @SuppressWarnings("deprecation")
    public static <T> FutureValue<T> execute(Supplier<T> supplier) {
        SettableFutureValue<T> future = FutureValues.newSettableValue();
        new FutureAsyncTask<>(supplier, future).executeOnExecutor(sExecutor);
        return future;
    }

    /**
     * Converts an asynchronous interface into another one (using {@link FutureValue}s) so that it
     * gets executed the same way an {@link android.os.AsyncTask} is: the body on a background
     * thread and the callbacks on the main thread.
     *
     * @return a {@link FutureValue} that reports progress and result on the main thread.
     */
    public static <T> FutureValue<T> execute(final FutureValue<T> sourceFuture) {
        final SettableFutureValue<T> future = FutureValues.newSettableValue();
        sExecutor.execute(() -> pipe(sourceFuture, future));
        return future;
    }

    /**
     * Pipes the results of one {@link FutureValue} into a {@link SettableFutureValue}, making sure
     * each callback call is run on the UI thread.
     */
    public static <T> void pipe(FutureValue<T> sourceFuture, SettableFutureValue<T> targetFuture) {
        FutureValue.Callback<T> pipeCallback = runOnUi(FutureValues.setterCallback(targetFuture));
        sourceFuture.get(pipeCallback);
    }

    /**
     * A {@link FutureValue.Callback} wrapper interface around another
     * {@link FutureValue.Callback} that ensures that each
     * callback call is run on the UI thread.
     */
    private static <T> FutureValue.Callback<T> runOnUi(
            final FutureValue.Callback<T> targetCallback) {

        return new FutureValue.Callback<T>() {

            @Override
            public void progress(final float progress) {
                if (ThreadUtils.isUiThread()) {
                    targetCallback.progress(progress);
                } else {
                    ThreadUtils.runOnUiThread(() -> targetCallback.progress(progress));
                }
            }

            @Override
            public void failed(final Throwable thrown) {
                if (ThreadUtils.isUiThread()) {
                    targetCallback.failed(thrown);
                } else {
                    ThreadUtils.runOnUiThread(() -> targetCallback.failed(thrown));
                }
            }

            @Override
            public void available(final T value) {
                if (ThreadUtils.isUiThread()) {
                    targetCallback.available(value);
                } else {
                    ThreadUtils.runOnUiThread(() -> targetCallback.available(value));
                }
            }
        };
    }

    /**
     * Using a {@link Converter}, convert the result of one {@link FutureValue}, and pipe that
     * result
     * into another destination {@link SettableFutureValue}.
     */
    public static <F, T> void convert(FutureValue<F> sourceFuture, Converter<F, T> converter,
            final SettableFutureValue<T> destFuture) {
        FutureValue<T> convertedValue = FutureValues.convert(sourceFuture, converter);

        convertedValue.get(new FutureValue.Callback<T>() {
            @Override
            public void available(final T value) {
                ThreadUtils.runOnUiThread(() -> destFuture.set(value));
            }

            @Override
            public void failed(final Throwable thrown) {
                ThreadUtils.runOnUiThread(() -> destFuture.fail(thrown));
            }

            @Override
            public void progress(final float progress) {
                ThreadUtils.runOnUiThread(() -> destFuture.progress(progress));
            }
        });
    }

    /**
     * Execute a {@link Supplier} and give its result to a {@link SettableFutureValue}.
     *
     * @param <T> The type of the value being supplied.
     */
    @SuppressWarnings("deprecation")
    private static class FutureAsyncTask<T> extends android.os.AsyncTask<Void, Float, T> {

        private final Supplier<T> mSupplier;
        private final FutureValues.SettableFutureValue<T> mFuture;
        // Any throwable caught during the call to supply() is kept here for onPostExecute.
        private Throwable mCaught;
        // Debug exception to trace the calling threads stack.
        private Exception mDebugTraceException;

        FutureAsyncTask(Supplier<T> supplier, SettableFutureValue<T> settable) {
            this.mSupplier = supplier;
            this.mFuture = settable;
            if (sDebug) {
                this.mDebugTraceException = new Exception("A debug stack trace");
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        protected T doInBackground(Void... params) {
            try {
                Progress progress = new Progress() {
                    @Override
                    public void report(float progress) {
                        publishProgress(progress);
                    }
                };
                return mSupplier.supply(progress);
            } catch (Throwable e) {
                if (mDebugTraceException != null) {
                    Log.d(TAG, "Exception during background processing: ", e);
                    Log.d(TAG, "Problem during background called from:", mDebugTraceException);
                } else {
                    Log.d(TAG, "Exception during background processing: " + e);
                }
                mCaught = e;
                return null;
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void onProgressUpdate(Float... values) {
            mFuture.progress(values[0]);
        }

        @Override
        @SuppressWarnings("deprecation")
        protected void onPostExecute(T result) {
            try {
                if (mCaught != null) {
                    mFuture.fail(mCaught);
                } else {
                    mFuture.set(result);
                }
            } catch (Exception e) {
                if (mDebugTraceException != null) {
                    Log.e(TAG, "Exception during post processing: ", e);
                    Log.e(TAG, "Problem in post-execute called from:", mDebugTraceException);
                } else {
                    Log.w(TAG, "Exception during post processing: ", e);
                }
            }
        }
    }
}
