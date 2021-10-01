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

package androidx.room;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all the communication from {@link RoomDatabase} and {@link InvalidationTracker} to
 * {@link MultiInstanceInvalidationService}.
 */
class MultiInstanceInvalidationClient {

    /**
     * The application context.
     */
    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final Context mAppContext;

    /**
     * The name of the database file.
     */
    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final String mName;

    /**
     * The client ID assigned by {@link MultiInstanceInvalidationService}.
     */
    // synthetic access
    @SuppressWarnings("WeakerAccess")
    int mClientId;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final InvalidationTracker mInvalidationTracker;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final InvalidationTracker.Observer mObserver;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    IMultiInstanceInvalidationService mService;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final Executor mExecutor;

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final IMultiInstanceInvalidationCallback mCallback =
            new IMultiInstanceInvalidationCallback.Stub() {
                @Override
                public void onInvalidation(final String[] tables) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mInvalidationTracker.notifyObserversByTableNames(tables);
                        }
                    });
                }
            };

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mStopped = new AtomicBoolean(false);

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IMultiInstanceInvalidationService.Stub.asInterface(service);
            mExecutor.execute(mSetUpRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mExecutor.execute(mRemoveObserverRunnable);
            mService = null;
        }

    };

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final Runnable mSetUpRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                final IMultiInstanceInvalidationService service = mService;
                if (service != null) {
                    mClientId = service.registerCallback(mCallback, mName);
                    mInvalidationTracker.addObserver(mObserver);
                }
            } catch (RemoteException e) {
                Log.w(Room.LOG_TAG, "Cannot register multi-instance invalidation callback", e);
            }
        }
    };

    // synthetic access
    @SuppressWarnings("WeakerAccess")
    final Runnable mRemoveObserverRunnable = new Runnable() {
        @Override
        public void run() {
            mInvalidationTracker.removeObserver(mObserver);
        }
    };

    /**
     * @param context             The Context to be used for binding
     *                            {@link IMultiInstanceInvalidationService}.
     * @param name                The name of the database file.
     * @param serviceIntent       The {@link Intent} used for binding
     *                            {@link IMultiInstanceInvalidationService}.
     * @param invalidationTracker The {@link InvalidationTracker}
     * @param executor            The background executor.
     */
    MultiInstanceInvalidationClient(Context context, String name, Intent serviceIntent,
            InvalidationTracker invalidationTracker, Executor executor) {
        mAppContext = context.getApplicationContext();
        mName = name;
        mInvalidationTracker = invalidationTracker;
        mExecutor = executor;
        // Use all tables names for observer.
        final Set<String> tableNames = invalidationTracker.mTableIdLookup.keySet();
        mObserver = new InvalidationTracker.Observer(tableNames.toArray(new String[0])) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                if (mStopped.get()) {
                    return;
                }
                try {
                    final IMultiInstanceInvalidationService service = mService;
                    if (service != null) {
                        service.broadcastInvalidation(mClientId, tables.toArray(new String[0]));
                    }
                } catch (RemoteException e) {
                    Log.w(Room.LOG_TAG, "Cannot broadcast invalidation", e);
                }
            }

            @Override
            boolean isRemote() {
                return true;
            }
        };
        mAppContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    void stop() {
        if (mStopped.compareAndSet(false, true)) {
            mInvalidationTracker.removeObserver(mObserver);
            try {
                final IMultiInstanceInvalidationService service = mService;
                if (service != null) {
                    service.unregisterCallback(mCallback, mClientId);
                }
            } catch (RemoteException e) {
                Log.w(Room.LOG_TAG, "Cannot unregister multi-instance invalidation callback", e);
            }
            mAppContext.unbindService(mServiceConnection);
        }
    }
}
