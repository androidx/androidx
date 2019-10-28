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

package androidx.ads.identifier.internal;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A one-time use ServiceConnection that facilitates waiting for the bind to complete and the
 * passing of the IBinder from the callback thread to the waiting thread.
 */
public class BlockingServiceConnection implements ServiceConnection {

    // Facilitates passing of the IBinder across threads
    private final BlockingQueue<IBinder> mBlockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void onServiceConnected(@NonNull ComponentName name, @NonNull IBinder service) {
        mBlockingQueue.add(service);
    }

    @Override
    public void onServiceDisconnected(@NonNull ComponentName name) {
        // Don't worry about clearing the returned binder in this case. If it does
        // happen a RemoteException will be thrown, which is already handled.
    }

    /**
     * Blocks until the bind is complete with a timeout and returns the bound IBinder. This must
     * only be called once.
     *
     * @return the IBinder of the bound service
     * @throws InterruptedException  if the current thread is interrupted while waiting for the bind
     * @throws IllegalStateException if called more than once
     * @throws TimeoutException      if the timeout period has elapsed
     */
    @NonNull
    public IBinder getServiceWithTimeout(long timeout, @NonNull TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {
        IBinder binder = mBlockingQueue.poll(timeout, timeUnit);
        if (binder == null) {
            throw new TimeoutException("Timed out waiting for the service connection");
        } else {
            return binder;
        }
    }
}
