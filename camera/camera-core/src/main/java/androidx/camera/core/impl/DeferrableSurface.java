/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.graphics.ImageFormat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class for creating and tracking use of a {@link Surface} in an asynchronous manner.
 *
 * <p>Once the deferrable surface has been closed via {@link #close()} and is no longer in
 * use ({@link #decrementUseCount()} has been called equal to the number of times to
 * {@link #incrementUseCount()}), then the surface is considered terminated.
 *
 * <p>Resources managed by this class can be safely cleaned up upon completion of the
 * {@link ListenableFuture} returned by {@link #getTerminationFuture()}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class DeferrableSurface {

    /**
     * The exception that is returned by the ListenableFuture of {@link #getSurface()} if the
     * deferrable surface is unable to produce a {@link Surface}.
     */
    public static final class SurfaceUnavailableException extends Exception {
        public SurfaceUnavailableException(@NonNull String message) {
            super(message);
        }
    }

    /**
     * The exception that is returned by the ListenableFuture of {@link #getSurface()} if the
     * {@link Surface} backing the DeferrableSurface has already been closed.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class SurfaceClosedException extends Exception {
        DeferrableSurface mDeferrableSurface;

        public SurfaceClosedException(@NonNull String s, @NonNull DeferrableSurface surface) {
            super(s);
            mDeferrableSurface = surface;
        }

        /**
         * Returns the {@link DeferrableSurface} that generated the exception.
         *
         * <p>The deferrable surface will already be closed.
         */
        @NonNull
        public DeferrableSurface getDeferrableSurface() {
            return mDeferrableSurface;
        }
    }

    // The size of the surface is not defined.
    public static final Size SIZE_UNDEFINED = new Size(0, 0);

    private static final String TAG = "DeferrableSurface";
    private static final boolean DEBUG = Logger.isDebugEnabled(TAG);

    // Debug only, used to track total count of surfaces in use.
    private static final AtomicInteger USED_COUNT = new AtomicInteger(0);
    // Debug only, used to track total count of surfaces, including those not in use. Will be
    // decremented once surface is cleaned.
    private static final AtomicInteger TOTAL_COUNT = new AtomicInteger(0);

    // Lock used for accessing states.
    private final Object mLock = new Object();

    // The use count.
    @GuardedBy("mLock")
    private int mUseCount = 0;

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @GuardedBy("mLock")
    private CallbackToFutureAdapter.Completer<Void> mTerminationCompleter;
    private final ListenableFuture<Void> mTerminationFuture;

    @NonNull
    private final Size mPrescribedSize;
    private final int mPrescribedStreamFormat;
    @Nullable
    Class<?> mContainerClass;

    /**
     * Creates a new DeferrableSurface which has no use count.
     */
    public DeferrableSurface() {
        this(SIZE_UNDEFINED, ImageFormat.UNKNOWN);
    }

    /**
     * Creates a new DeferrableSurface which has no use count.
     *
     * @param size   the {@link Size} of the surface
     * @param format the stream configuration format that the provided Surface will be used on.
     */
    public DeferrableSurface(@NonNull Size size, int format) {
        mPrescribedSize = size;
        mPrescribedStreamFormat = format;
        mTerminationFuture = CallbackToFutureAdapter.getFuture(completer -> {
            synchronized (mLock) {
                mTerminationCompleter = completer;
            }
            return "DeferrableSurface-termination(" + DeferrableSurface.this + ")";
        });

        if (Logger.isDebugEnabled(TAG)) {
            printGlobalDebugCounts("Surface created", TOTAL_COUNT.incrementAndGet(),
                    USED_COUNT.get());

            String creationStackTrace = Log.getStackTraceString(new Exception());
            mTerminationFuture.addListener(() -> {
                try {
                    mTerminationFuture.get();
                    printGlobalDebugCounts("Surface terminated", TOTAL_COUNT.decrementAndGet(),
                            USED_COUNT.get());
                } catch (Exception e) {
                    Logger.e(TAG, "Unexpected surface termination for " + DeferrableSurface.this
                            + "\nStack Trace:\n" + creationStackTrace);
                    synchronized (mLock) {
                        throw new IllegalArgumentException(String.format(
                                "DeferrableSurface %s [closed: %b, use_count: %s] terminated with"
                                        + " unexpected exception.",
                                DeferrableSurface.this, mClosed, mUseCount), e);
                    }
                }
            }, CameraXExecutors.directExecutor());
        }
    }

    private void printGlobalDebugCounts(@NonNull String prefix, int totalCount, int useCount) {
        //  If debug logging was not enabled at static initialization time but is now enabled,
        //  sUsedCount and sTotalCount may be inaccurate.
        if (!DEBUG && Logger.isDebugEnabled(TAG)) {
            Logger.d(TAG,
                    "DeferrableSurface usage statistics may be inaccurate since debug logging was"
                            + " not enabled at static initialization time. App restart may be "
                            + "required to enable accurate usage statistics.");
        }
        Logger.d(TAG, prefix + "[total_surfaces=" + totalCount + ", used_surfaces=" + useCount
                + "](" + this + "}");
    }

    /**
     * Returns a {@link Surface} that is wrapped in a {@link ListenableFuture}.
     *
     * @return Will return a {@link ListenableFuture} with an exception if the DeferrableSurface
     * is already closed.
     */
    @NonNull
    public final ListenableFuture<Surface> getSurface() {
        synchronized (mLock) {
            if (mClosed) {
                return Futures.immediateFailedFuture(
                        new SurfaceClosedException("DeferrableSurface already closed.", this));
            }
            return provideSurface();
        }
    }

    /**
     * Returns a {@link Surface} that is wrapped in a {@link ListenableFuture} when the
     * DeferrableSurface has not yet been closed.
     */
    @NonNull
    protected abstract ListenableFuture<Surface> provideSurface();

    /**
     * Returns a future which completes when the deferrable surface is terminated.
     *
     * <p>A deferrable surface is considered terminated once it has been closed by
     * {@link #close()} and it is marked as no longer in use via {@link #decrementUseCount()}.
     *
     * <p>Once a deferrable surface has been terminated, it is safe to release all resources
     * which may have been created for the surface.
     *
     * @return A future signalling the deferrable surface has terminated. Cancellation of this
     * future is a no-op.
     */
    @NonNull
    public ListenableFuture<Void> getTerminationFuture() {
        return Futures.nonCancellationPropagating(mTerminationFuture);
    }

    /**
     * Increments the use count of the surface.
     *
     * <p>If the surface has been closed and was not previously in use, this will fail and throw a
     * {@link SurfaceClosedException} and the use count will not be incremented.
     *
     * @throws SurfaceClosedException if the surface has been closed.
     */
    public void incrementUseCount() throws SurfaceClosedException {
        synchronized (mLock) {
            if (mUseCount == 0 && mClosed) {
                throw new SurfaceClosedException("Cannot begin use on a closed surface.", this);
            }
            mUseCount++;

            if (Logger.isDebugEnabled(TAG)) {
                if (mUseCount == 1) {
                    printGlobalDebugCounts("New surface in use", TOTAL_COUNT.get(),
                            USED_COUNT.incrementAndGet());
                }
                Logger.d(TAG, "use count+1, useCount=" + mUseCount + " " + this);
            }
        }
    }

    /**
     * Close the surface.
     *
     * <p>After closing, {@link #getSurface()} and {@link #incrementUseCount()} will return a
     * {@link SurfaceClosedException}.
     *
     * <p>If the surface is not being used, then this will also complete the future returned by
     * {@link #getTerminationFuture()}. If the surface is in use, then the future not be completed
     * until {@link #decrementUseCount()} has bee called the appropriate number of times.
     *
     * <p>This method is idempotent. Subsequent calls after the first invocation will have no
     * effect.
     */
    public void close() {
        // If this gets set, then the surface will terminate
        CallbackToFutureAdapter.Completer<Void> terminationCompleter = null;
        synchronized (mLock) {
            if (!mClosed) {
                mClosed = true;

                if (mUseCount == 0) {
                    terminationCompleter = mTerminationCompleter;
                    mTerminationCompleter = null;
                }

                if (Logger.isDebugEnabled(TAG)) {
                    Logger.d(TAG,
                            "surface closed,  useCount=" + mUseCount + " closed=true " + this);
                }
            }
        }

        if (terminationCompleter != null) {
            terminationCompleter.set(null);
        }
    }

    /**
     * Decrements the use count.
     *
     * <p>If this causes the use count to go to zero and the surface has been closed, this will
     * complete the future returned by {@link #getTerminationFuture()}.
     */
    public void decrementUseCount() {
        // If this gets set, then the surface will terminate
        CallbackToFutureAdapter.Completer<Void> terminationCompleter = null;
        synchronized (mLock) {
            if (mUseCount == 0) {
                throw new IllegalStateException("Decrementing use count occurs more times than "
                        + "incrementing");
            }

            mUseCount--;
            if (mUseCount == 0 && mClosed) {
                terminationCompleter = mTerminationCompleter;
                mTerminationCompleter = null;
            }

            if (Logger.isDebugEnabled(TAG)) {
                Logger.d(TAG, "use count-1,  useCount=" + mUseCount + " closed=" + mClosed
                        + " " + this);

                if (mUseCount == 0) {
                    printGlobalDebugCounts("Surface no longer in use", TOTAL_COUNT.get(),
                            USED_COUNT.decrementAndGet());
                }
            }
        }

        if (terminationCompleter != null) {
            terminationCompleter.set(null);
        }
    }

    /**
     * @return the {@link Size} of the surface
     */
    @NonNull
    public Size getPrescribedSize() {
        return mPrescribedSize;
    }

    /**
     * @return the stream configuration format that the provided Surface will be used on.
     */
    public int getPrescribedStreamFormat() {
        return mPrescribedStreamFormat;
    }

    @VisibleForTesting
    public int getUseCount() {
        synchronized (mLock) {
            return mUseCount;
        }
    }

    /**
     * Checks if the {@link DeferrableSurface} is closed
     */
    public boolean isClosed() {
        synchronized (mLock) {
            return mClosed;
        }
    }

    /**
     * Returns the {@link Class} that contains this {@link DeferrableSurface} to provide more
     * context about it.
     */
    @Nullable
    public Class<?> getContainerClass() {
        return mContainerClass;
    }

    /**
     * Set the {@link Class} that contains this {@link DeferrableSurface} to provide more
     * context about it.
     */
    public void setContainerClass(@NonNull Class<?> containerClass) {
        mContainerClass = containerClass;
    }
}
