/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A reference to a {@link Surface} whose creation can be deferred to a later time.
 *
 * <p>A {@link OnSurfaceDetachedListener} can also be set to be notified of surface detach event. It
 * can be used to safely close the surface.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public abstract class DeferrableSurface {
    /**
     * The exception that is returned by the ListenableFuture of {@link #getSurface()} if the
     * {@link Surface} backing the DeferrableSurface has already been closed.
     */
    final class SurfaceClosedException extends Exception {
        SurfaceClosedException(String s, Throwable e) {
            super(s, e);
        }

        SurfaceClosedException(String s) {
            super(s);
        }
    }

    private static final boolean DEBUG = false;
    protected static final String TAG = "DeferrableSurface";

    // Debug only, used to track total count of attached surfaces.
    private static AtomicInteger sSurfaceCount = new AtomicInteger(0);

    // The count of attachment.
    @GuardedBy("mLock")
    private int mAttachedCount = 0;

    // Listener to be called when surface is detached totally.
    @Nullable
    @GuardedBy("mLock")
    private OnSurfaceDetachedListener mOnSurfaceDetachedListener = null;

    @Nullable
    @GuardedBy("mLock")
    private Executor mListenerExecutor = null;

    // Lock used for accessing states.
    private final Object mLock = new Object();

    /** Returns a {@link Surface} that is wrapped in a {@link ListenableFuture}. */
    @Nullable
    public abstract ListenableFuture<Surface> getSurface();

    /** Refreshes the {@link DeferrableSurface} before attach if needed. */
    public void refresh() {
    }

    /** Notifies this surface is attached */
    public void notifySurfaceAttached() {
        synchronized (mLock) {
            mAttachedCount++;

            if (DEBUG) {
                if (mAttachedCount == 1) {
                    Log.d(TAG, "Surface attached, count = " + sSurfaceCount.incrementAndGet() + " "
                            + this);
                }
                Log.d(TAG, "attach count+1, attachedCount=" + mAttachedCount + " " + this);
            }
        }
    }

    /**
     * Notifies this surface is detached. OnSurfaceDetachedListener will be called if it is detached
     * totally
     */
    public void notifySurfaceDetached() {
        OnSurfaceDetachedListener listener = null;
        Executor listenerExecutor = null;

        synchronized (mLock) {
            if (mAttachedCount == 0) {
                throw new IllegalStateException("Detaching occurs more times than attaching");
            }

            mAttachedCount--;
            if (mAttachedCount == 0) {
                listener = mOnSurfaceDetachedListener;
                listenerExecutor = mListenerExecutor;
            }

            if (DEBUG) {
                Log.d(TAG, "attach count-1,  attachedCount=" + getAttachedCount() + " " + this);

                if (mAttachedCount == 0) {
                    Log.d(TAG, "Surface detached, count = " + sSurfaceCount.decrementAndGet() + " "
                            + this);
                }
            }
        }

        if (listener != null && listenerExecutor != null) {
            callOnSurfaceDetachedListener(listener, listenerExecutor);
        }
    }

    /**
     * Sets the listener to be called when surface is detached totally.
     *
     * <p>If the surface is currently not attached, the listener will be called immediately. When
     * clearing resource like ImageReader, to close it safely you can call this method and close the
     * resources in the listener. This can ensure the surface is closed after it is no longer held
     * in camera.
     */
    public void setOnSurfaceDetachedListener(@NonNull Executor executor,
            @NonNull OnSurfaceDetachedListener listener) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(listener);
        boolean shouldCallListenerNow = false;
        synchronized (mLock) {
            mOnSurfaceDetachedListener = listener;
            mListenerExecutor = executor;
            // Calls the listener immediately if the surface is not attached right now.
            if (mAttachedCount == 0) {
                shouldCallListenerNow = true;
            }
        }

        if (shouldCallListenerNow) {
            callOnSurfaceDetachedListener(listener, executor);
        }
    }

    private static void callOnSurfaceDetachedListener(
            @NonNull final OnSurfaceDetachedListener listener, @NonNull Executor executor) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(listener);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onSurfaceDetached();
            }
        });
    }

    @VisibleForTesting
    public int getAttachedCount() {
        synchronized (mLock) {
            return mAttachedCount;
        }
    }

    /**
     * The listener to be called when surface is detached totally.
     */
    public interface OnSurfaceDetachedListener {
        /**
         * Called when surface is totally detached.
         */
        void onSurfaceDetached();
    }
}
