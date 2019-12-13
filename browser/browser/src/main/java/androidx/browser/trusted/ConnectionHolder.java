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

package androidx.browser.trusted;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.customtabs.trusted.ITrustedWebActivityService;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a connection to a TrustedWebActivityService.
 * It should only be used on the UI Thread.
 */
class ConnectionHolder implements ServiceConnection {
    private static final int STATE_AWAITING_CONNECTION = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_DISCONNECTED = 2;
    private static final int STATE_CANCELLED = 3;

    @NonNull private final Runnable mCloseRunnable;
    @NonNull private final WrapperFactory mWrapperFactory;

    private int mState = STATE_AWAITING_CONNECTION;
    @Nullable private TrustedWebActivityServiceConnection mService;
    @NonNull private List<Completer<TrustedWebActivityServiceConnection>> mCompleters =
            new ArrayList<>();
    @Nullable private Exception mCancellationException;

    /** A class that creates the TrustedWebActivityServiceConnection. Allows mocking in tests. */
    static class WrapperFactory {
        @NonNull
        TrustedWebActivityServiceConnection create(ComponentName name, IBinder iBinder) {
            return new TrustedWebActivityServiceConnection(
                    ITrustedWebActivityService.Stub.asInterface(iBinder), name);
        }
    }

    /**
     * Constructor for production use. Takes in a {@link Runnable} that will be called either when
     * the Service disconnects or {@link #cancel} is called.
     */
    @MainThread
    ConnectionHolder(@NonNull Runnable closeRunnable) {
        this(closeRunnable, new WrapperFactory());
    }

    /** Constructor for testing use. */
    @MainThread
    ConnectionHolder(@NonNull Runnable closeRunnable, @NonNull WrapperFactory factory) {
        mCloseRunnable = closeRunnable;
        mWrapperFactory = factory;
    }

    /** This method will be called on the UI Thread by the Android Framework. */
    @Override
    @MainThread
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mService = mWrapperFactory.create(componentName, iBinder);
        for (Completer<TrustedWebActivityServiceConnection> completer : mCompleters) {
            completer.set(mService);
        }
        mCompleters.clear();

        mState = STATE_CONNECTED;
    }

    /** This method will be called on the UI Thread by the Android Framework. */
    @Override
    @MainThread
    public void onServiceDisconnected(ComponentName componentName) {
        mService = null;
        mCloseRunnable.run();
        mState = STATE_DISCONNECTED;
    }

    /**
     * Called to signal that the connection attempt failed and that neither
     * {@link #onServiceConnected(ComponentName, IBinder)} or
     * {@link #onServiceDisconnected(ComponentName)} will be called.
     */
    @MainThread
    public void cancel(@NonNull Exception exception) {
        for (Completer<TrustedWebActivityServiceConnection> completer : mCompleters) {
            completer.setException(exception);
        }
        mCompleters.clear();
        mCloseRunnable.run();
        mState = STATE_CANCELLED;
        mCancellationException = exception;
    }

    /**
     * Returns a future that will:
     * - be unset if a connection is still pending and set once open.
     * - be set to a {@link TrustedWebActivityServiceConnection} if a connection is open.
     * - be set to an exception if the connection failed or has been closed.
     */
    @MainThread
    @NonNull
    public ListenableFuture<TrustedWebActivityServiceConnection> getServiceWrapper() {
        // Using CallbackToFutureAdapter and storing the completers gives us some additional safety
        // checks over using Futures ourselves (such as failing the Future if the completer is
        // garbage collected).
        return CallbackToFutureAdapter.getFuture(completer -> {
            switch (mState) {
                case STATE_AWAITING_CONNECTION:
                    mCompleters.add(completer);
                    break;
                case STATE_CONNECTED:
                    if (mService == null) {
                        throw new IllegalStateException("ConnectionHolder state is incorrect.");
                    }
                    completer.set(mService);
                    break;
                case STATE_DISCONNECTED:
                    throw new IllegalStateException("Service has been disconnected.");
                case STATE_CANCELLED:
                    throw mCancellationException;
                default:
                    throw new IllegalStateException("Connection state is invalid");
            }

            return "ConnectionHolder, state = " + mState;
        });
    }
}
