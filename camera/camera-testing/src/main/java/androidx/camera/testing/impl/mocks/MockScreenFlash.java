/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.mocks;

import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.ScreenFlash;
import androidx.camera.core.ImageCapture.ScreenFlashListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementations of {@link ScreenFlash} for testing purpose.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MockScreenFlash implements ScreenFlash {
    /**
     * Represents {@link ImageCapture.ScreenFlash#apply} event.
     */
    public static final int APPLY = 0;
    /**
     * Represents {@link ScreenFlash#clear()} event.
     */
    public static final int CLEAR = 1;

    /**
     * The event types in {@link ScreenFlash}.
     */
    @IntDef({APPLY, CLEAR})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    public @interface ScreenFlashEvent {
    }

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final List<@ScreenFlashEvent Integer> mEventList = new ArrayList<>();
    private final CountDownLatch mClearLatch = new CountDownLatch(1);
    private boolean mIsApplyCompletedInstantly = true;

    @GuardedBy("mLock")
    private long mLastApplyExpirationTimeMillis;

    @GuardedBy("mLock")
    @Nullable
    private Looper mLastApplyThreadLooper;

    @GuardedBy("mLock")
    @Nullable
    private ScreenFlashListener mLastApplyListener;

    @GuardedBy("mLock")
    @Nullable
    private Looper mLastClearThreadLooper;

    /**
     * Returns a list of {@link ScreenFlashEvent} in the same order as invoked.
     */
    @NonNull
    public List<@ScreenFlashEvent Integer> getScreenFlashEvents() {
        synchronized (mLock) {
            return new ArrayList<>(mEventList);
        }
    }

    /**
     * Waits for {@link #clear} to be invoked once.
     *
     * @param timeoutInMillis The timeout of waiting in milliseconds.
     * @return True if {@link #clear} was invoked, false if timed out.
     */
    public boolean awaitClear(long timeoutInMillis) {
        try {
            return mClearLatch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables or disables the {@link ScreenFlashListener} being completed instantly when
     * {@link ScreenFlash#apply(long, ScreenFlashListener)} is invoked.
     */
    public void setApplyCompletedInstantly(boolean completedInstantly) {
        mIsApplyCompletedInstantly = completedInstantly;
    }

    /**
     * Gets the expiration time argument of the last
     * {@link ScreenFlash#apply(long, ScreenFlashListener)} invocation, or zero in case of no
     * invocation.f<
     */
    public long getLastApplyExpirationTimeMillis() {
        synchronized (mLock) {
            return mLastApplyExpirationTimeMillis;
        }
    }

    /**
     * Gets the {@link Looper} of calling thread for
     * {@link ScreenFlash#apply(long, ScreenFlashListener)} invocation, or null in case of no
     * invocation.
     */
    @Nullable
    public Looper getLastApplyThreadLooper() {
        synchronized (mLock) {
            return mLastApplyThreadLooper;
        }
    }

    /**
     * Gets the {@link ScreenFlashListener} instance of the last
     * {@link ScreenFlash#apply(long, ScreenFlashListener)} invocation, or null in case of no
     * invocation.
     */
    @Nullable
    public ScreenFlashListener getLastApplyListener() {
        synchronized (mLock) {
            return mLastApplyListener;
        }
    }

    /**
     * Gets the {@link Looper} of calling thread for {@link ScreenFlash#clear()} invocation, or
     * null in case of no invocation.
     */
    @Nullable
    public Looper getLastClearThreadLooper() {
        synchronized (mLock) {
            return mLastClearThreadLooper;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void apply(long expirationTimeMillis,
            @NonNull ScreenFlashListener screenFlashListener) {
        synchronized (mLock) {
            mEventList.add(APPLY);
            mLastApplyExpirationTimeMillis = expirationTimeMillis;
            mLastApplyListener = screenFlashListener;
            mLastApplyThreadLooper = Looper.myLooper();
            if (mIsApplyCompletedInstantly) {
                screenFlashListener.onCompleted();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        synchronized (mLock) {
            mEventList.add(CLEAR);
            mClearLatch.countDown();
            mLastClearThreadLooper = Looper.myLooper();
        }
    }
}
