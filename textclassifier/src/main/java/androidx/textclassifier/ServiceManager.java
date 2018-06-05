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

package androidx.textclassifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Takes care of service binding and its lifecycle.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class ServiceManager {
    private static final String TAG = TextClassifier.DEFAULT_LOG_TAG;
    private static final int BIND_TIMEOUT_SECOND = 3;
    // How long after the last interaction with the service we would unbind
    private static final long TIMEOUT_IDLE_BIND_MILLIS = TimeUnit.MINUTES.toMillis(1);

    @NonNull
    private final Object mLock = new Object();

    @Nullable
    @GuardedBy("mLock")
    private ITextClassifierService mService;

    @GuardedBy("mLock")
    private boolean mBinding;

    @NonNull
    private final Context mContext;

    @NonNull
    private final Intent mServiceIntent;

    @NonNull
    private final Handler mMainThreadHandler;

    @NonNull
    private final TextClassifierServiceConnection mServiceConnection =
            new TextClassifierServiceConnection();

    @NonNull
    private final Runnable mUnbindRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (!isBound()) {
                    return;
                }

                Log.d(TAG, "Unbinding from " + mServiceIntent.getComponent());
                mServiceConnection.cleanupService();
            }
        }
    };

    ServiceManager(@NonNull Context context, @NonNull ComponentName serviceComponent) {
        this(context, serviceComponent, new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    ServiceManager(
            @NonNull Context context, @NonNull ComponentName serviceComponent,
            @NonNull Handler mainThreadHandler) {
        mContext = Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(serviceComponent);
        mServiceIntent = new Intent(TextClassifierService.SERVICE_INTERFACE)
                .setComponent(serviceComponent);
        mMainThreadHandler = mainThreadHandler;
    }

    /**
     * Binds the target {@link TextClassifierService} synchronously.
     * @return the remote service if bound successfully, {@code null} otherwise.
     */
    @Nullable
    @WorkerThread
    ITextClassifierService bindAndAwait() {
        TextClassifier.ensureNotOnMainThread();

        if (isBound()) {
            return getService();
        }
        CountDownLatch latch = mServiceConnection.newLatch();
        boolean binding = bind();
        if (!binding) {
            return null;
        }
        try {
            latch.await(BIND_TIMEOUT_SECOND, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "bindAndAwait: interrupted", e);
        }
        return getService();
    }

    void scheduleUnbind() {
        mMainThreadHandler.removeCallbacks(mUnbindRunnable);
        mMainThreadHandler.postDelayed(mUnbindRunnable, TIMEOUT_IDLE_BIND_MILLIS);
    }

    private ITextClassifierService getService() {
        synchronized (mLock) {
            return mService;
        }
    }

    @VisibleForTesting
    boolean isBound() {
        synchronized (mLock) {
            return mService != null;
        }
    }

    /**
     * @return true if the service is bound or in the process of being bound.
     * Returns false otherwise.
     */
    private boolean bind() {
        synchronized (mLock) {
            if (isBound() || mBinding) {
                return true;
            }
            mBinding = mContext.bindService(
                    mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "Binding to " + mServiceIntent.getComponent());
            return mBinding;
        }
    }

    private final class TextClassifierServiceConnection implements ServiceConnection {
        @NonNull
        private volatile CountDownLatch mLatch = new CountDownLatch(1);

        private TextClassifierServiceConnection() {}

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            init(ITextClassifierService.Stub.asInterface(service));
            mLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            cleanupService();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            cleanupService();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            cleanupService();
        }

        /**
         * Returns a newly created latch to await service connection.
         */
        private CountDownLatch newLatch() {
            mLatch = new CountDownLatch(1);
            return mLatch;
        }

        private void cleanupService() {
            synchronized (mLock) {
                mContext.unbindService(this);
                init(null);
            }
        }

        private void init(@Nullable ITextClassifierService service) {
            synchronized (mLock) {
                mService = service;
                mBinding = false;
            }
        }
    }
}
