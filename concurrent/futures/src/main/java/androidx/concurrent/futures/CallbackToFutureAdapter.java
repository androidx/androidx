/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.concurrent.futures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A utility that provides safety checks as an alternative to {@link
 * androidx.concurrent.futures.ResolvableFuture}, failing the future if it will never complete.
 * Useful for adapting interfaces that take callbacks into interfaces that return {@link
 * ListenableFuture}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * return CallbackToFutureAdapter.getFuture(
 *    completer -> {
 *      Callback myCallback = foo -> completer.set(foo);
 *      someObject.getFoo(myCallback);
 *      return myCallback;
 *    });
 * }</pre>
 *
 * Try to avoid creating references from listeners on the returned {@code Future} to the {@link
 * Completer} or the passed-in {@code tag} object, as this will defeat the best-effort early failure
 * detection based on garbage collection.
 */
public final class CallbackToFutureAdapter {

    private CallbackToFutureAdapter() {
    }

    /*
     * Returns a Future that will be completed by the {@link Completer} provided in from
     * {@link Callback#getCompleter}.
     *
     * <p>The provided callback is invoked immediately inline. Any exceptions thrown by it will
     * fail the returned {@code Future}.
     */
    @NonNull
    public static <T> ListenableFuture<T> getFuture(@NonNull Resolver<T> callback) {
        Completer<T> completer = new Completer<>();
        SafeFuture<T> safeFuture = new SafeFuture<>(completer);
        completer.future = safeFuture;
        // Set something as the tag, so that we can hopefully identify the call site from the
        // toString()
        // of the future. Retaining the instance could potentially cause a leak (if it's an inner
        // class)
        // and it's probably a lambda anyway so retaining the class provides just as much
        // information.
        completer.tag = callback.getClass();
        // Start timeout before invoking the callback
        final Object tag;
        try {
            tag = callback.attachCompleter(completer);
            if (tag != null) {
                completer.tag = tag;
            }
        } catch (Exception e) {
            safeFuture.setException(e);
        }
        return safeFuture;
    }

    /** Called by {@link #getFuture}. */
    public interface Resolver<T> {
        /**
         * Create your callback object and start whatever operations are required to trigger it
         * here.
         *
         * @param completer Call one of the set methods on this object to complete the returned
         *                  Future.
         * @return an object to use as the human-readable description of what is expected to
         * complete
         * this future. In error cases, its toString() will be included in the message.
         */
        @Nullable
        Object attachCompleter(@NonNull Completer<T> completer) throws Exception;
    }

    // TODO(b/119308748): Implement InternalFutureFailureAccess
    private static final class SafeFuture<T> implements ListenableFuture<T> {
        final WeakReference<Completer<T>> completerWeakReference;

        SafeFuture(Completer<T> completer) {
            this.completerWeakReference = new WeakReference<>(completer);
        }

        private final AbstractResolvableFuture<T> delegate = new AbstractResolvableFuture<T>() {

            @Override
            protected String pendingToString() {
                Completer<T> completer = completerWeakReference.get();
                if (completer == null) {
                    return "Completer object has been garbage collected, future will fail soon";
                } else {
                    return "tag=[" + completer.tag + "]";
                }
            }
        };

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            // Obtain reference to completer before setting; a listener might make completer weakly
            // weakly reachable.
            Completer<T> completer = completerWeakReference.get();
            boolean cancelled = delegate.cancel(mayInterruptIfRunning);
            if (cancelled && completer != null) {
                // If the completer was null here, that means it will be finalized in the future.
                completer.fireCancellationListeners();
            }
            return cancelled;
        }

        boolean cancelWithoutNotifyingCompleter(boolean shouldInterrupt) {
            return delegate.cancel(shouldInterrupt);
        }

        // setFuture intentionally omitted, because it interacts badly with timeouts

        boolean set(T value) {
            return delegate.set(value);
        }

        boolean setException(Throwable t) {
            return delegate.setException(t);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public T get(long timeout, @NonNull TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }

        @Override
        public void addListener(@NonNull Runnable listener, @NonNull Executor executor) {
            delegate.addListener(listener, executor);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /** Used to complete the future returned by {@link #getFuture} */
    public static final class Completer<T> {
        // synthetic access
        Object tag;
        // synthetic access
        SafeFuture<T> future;
        private ResolvableFuture<Void> cancellationFuture = ResolvableFuture.create();

        /**
         * Tracks whether a caller ever attempted to complete the future. If they did, we won't
         * invoke
         * cancellation listeners if this object is GCed.
         */
        private boolean attemptedSetting;

        Completer() {
        }

        /**
         * Sets the result of the {@code Future} unless the {@code Future} has already been
         * cancelled or
         * set. When a call to this method returns, the {@code Future} is guaranteed to be done.
         *
         * @param value the value to be used as the result
         * @return true if this attempt completed the {@code Future}, false if it was already
         * complete
         */
        public boolean set(T value) {
            attemptedSetting = true;
            SafeFuture<T> localFuture = future;
            boolean wasSet = localFuture != null && localFuture.set(value);
            if (wasSet) {
                setCompletedNormally();
            }
            return wasSet;
        }

        /**
         * Sets the failed result of the {@code Future} unless the {@code Future} has already been
         * cancelled or set. When a call to this method returns, the {@code Future} is guaranteed
         * to be
         * done.
         *
         * @param t the exception to be used as the failed result
         * @return true if this attempt completed the {@code Future}, false if it was already
         * complete
         */
        public boolean setException(@NonNull Throwable t) {
            attemptedSetting = true;
            SafeFuture<T> localFuture = future;
            boolean wasSet = localFuture != null && localFuture.setException(t);
            if (wasSet) {
                setCompletedNormally();
            }
            return wasSet;
        }

        /**
         * Cancels {@code Future} unless the {@code Future} has already been cancelled or set.
         * When a
         * call to this method returns, the {@code Future} is guaranteed to be done.
         *
         * @return true if this attempt completed the {@code Future}, false if it was already
         * complete
         */
        public boolean setCancelled() {
            attemptedSetting = true;
            SafeFuture<T> localFuture = future;
            boolean wasSet = localFuture != null && localFuture.cancelWithoutNotifyingCompleter(
                    true);
            if (wasSet) {
                setCompletedNormally();
            }
            return wasSet;
        }

        /**
         * Use to propagate cancellation from the future to whatever operation is using this
         * Completer.
         * <p>
         * Will be called when the returned Future is cancelled by
         * {@link Future#cancel(boolean)} or this {@code Completer} object is garbage collected
         * before the future completes.
         * Not triggered by {@link #setCancelled}.
         */
        public void addCancellationListener(@NonNull Runnable runnable,
                @NonNull Executor executor) {
            ListenableFuture<?> localCancellationFuture = cancellationFuture;
            if (localCancellationFuture != null) {
                localCancellationFuture.addListener(runnable, executor);
            }
        }

        void fireCancellationListeners() {
            tag = null;
            future = null;
            cancellationFuture.set(null);
        }

        private void setCompletedNormally() {
            // Null out, so that GC does not retain the future and its value even if the callback
            // retains
            // the completer object
            tag = null;
            future = null;
            cancellationFuture = null;
        }

        // toString intentionally left omitted, so that if the tag object (which holds this object
        // as a field) includes it in its toString, we won't infinitely recurse.

        @Override
        protected void finalize() {
            SafeFuture<T> localFuture = future;
            // Complete the future with an error before any cancellation listeners try to set the
            // future.
            // Also avoid allocating the exception if we know we won't actually be able to set it.
            if (localFuture != null && !localFuture.isDone()) {
                localFuture.setException(
                        new FutureGarbageCollectedException(
                                "The completer object was garbage collected - this future would "
                                        + "otherwise never "
                                        + "complete. The tag was: "
                                        + tag));
            }
            if (!attemptedSetting) {
                ResolvableFuture<Void> localCancellationFuture = cancellationFuture;
                if (localCancellationFuture != null) {
                    // set is idempotent, so even if this was already invoked it won't run
                    // listeners twice
                    localCancellationFuture.set(null);
                }
            }
        }
    }

    static final class FutureGarbageCollectedException extends Throwable {
        FutureGarbageCollectedException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; // no stack trace, wouldn't be useful anyway
        }
    }
}
