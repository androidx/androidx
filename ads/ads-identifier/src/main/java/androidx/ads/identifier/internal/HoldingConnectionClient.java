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

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.ads.identifier.AdvertisingIdUtils;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/** A client which keeps the ServiceConnection to the {@link IAdvertisingIdService}. */
@SuppressWarnings("deprecation")
public class HoldingConnectionClient {

    private static final long SERVICE_CONNECTION_TIMEOUT_SECONDS = 10;

    private final Context mContext;

    @NonNull
    private final BlockingServiceConnection mConnection;

    @NonNull
    private final String mPackageName;

    @NonNull
    private final IAdvertisingIdService mIdService;

    /**
     * The last connection ID which assign to the users of this client.
     *
     * <p>This also indicates the connection status, >= 0 indicates this client is connected,
     * otherwise this client has already been disconnected.
     * <p>It helps to synchronize between the usages of this client and auto disconnection task by
     * using this single atomic, which supports 3 kinds of atomic operations:
     * <ul>
     *     <li>Checks whether this client is connected, if yes, increment and get a connection ID.
     *     <li>When an auto disconnect task is due, it compares its connection ID to this value, if
     *     same, unbind the service and sets this atomic to {@link Long#MIN_VALUE}.
     *     <li>When this client's connection has lost and
     *     {@link BlockingServiceConnection#onServiceDisconnected} is called, unbind the service
     *     and sets this atomic to {@link Long#MIN_VALUE}.
     * </ul>
     * <p>This ID is monotonically increasing, except when this client is disconnected, this ID
     * sets to {@link Long#MIN_VALUE}.
     */
    private final AtomicLong mLastConnectionId = new AtomicLong(0);

    @WorkerThread
    public HoldingConnectionClient(@NonNull Context context)
            throws androidx.ads.identifier.AdvertisingIdNotAvailableException, IOException,
            TimeoutException,
            InterruptedException {
        mContext = context;
        ComponentName componentName = getProviderComponentName(mContext);
        mConnection = getServiceConnection(componentName);
        mIdService = getIdServiceFromConnection();
        mPackageName = componentName.getPackageName();
    }

    /** Gets the connected {@link IAdvertisingIdService}. */
    @NonNull
    public IAdvertisingIdService getIdService() {
        return mIdService;
    }

    /** Gets the connected service's package name. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Gets whether the client is connected to the {@link IAdvertisingIdService}. */
    public boolean isConnected() {
        return mLastConnectionId.get() >= 0;
    }

    /**
     * Gets a connection ID before using this client which prevents race condition with the auto
     * disconnection task.
     *
     * @return connection ID, >= 0 indicates this client is connected, otherwise this client has
     * already been disconnected.
     */
    public long askConnectionId() {
        return mLastConnectionId.incrementAndGet();
    }

    /**
     * Closes the connection to the Advertising ID Provider Service.
     *
     * <p>Note: If the connection has already been closed, does nothing.
     */
    void finish() {
        if (mLastConnectionId.getAndSet(Long.MIN_VALUE) >= 0) {
            mContext.unbindService(mConnection);
        }
    }

    /**
     * Tries to close the connection to the Advertising ID Provider Service if no one is using the
     * client.
     *
     * @return true if this client is disconnected after this method returns.
     */
    public boolean tryFinish(long connectionId) {
        if (mLastConnectionId.compareAndSet(connectionId, Long.MIN_VALUE)) {
            mContext.unbindService(mConnection);
            return true;
        }
        return !isConnected();
    }

    private static ComponentName getProviderComponentName(Context context)
            throws androidx.ads.identifier.AdvertisingIdNotAvailableException {
        PackageManager packageManager = context.getPackageManager();
        List<ServiceInfo> serviceInfos =
                AdvertisingIdUtils.getAdvertisingIdProviderServices(packageManager);
        ServiceInfo serviceInfo =
                AdvertisingIdUtils.selectServiceByPriority(serviceInfos, packageManager);
        if (serviceInfo == null) {
            throw new androidx.ads.identifier.AdvertisingIdNotAvailableException(
                    "No compatible AndroidX Advertising ID Provider available.");
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    /**
     * Retrieves BlockingServiceConnection which must be unbound after use.
     *
     * @throws IOException when unable to bind service successfully.
     */
    @VisibleForTesting
    BlockingServiceConnection getServiceConnection(ComponentName componentName) throws IOException {
        Intent intent = new Intent(GET_AD_ID_ACTION);
        intent.setComponent(componentName);

        BlockingServiceConnection bsc = new BlockingServiceConnection();
        if (mContext.bindService(intent, bsc, Service.BIND_AUTO_CREATE)) {
            return bsc;
        } else {
            throw new IOException("Connection failure");
        }
    }

    /**
     * Gets the {@link IAdvertisingIdService} from the blocking queue. This should wait until
     * {@link ServiceConnection#onServiceConnected} event with a
     * {@link #SERVICE_CONNECTION_TIMEOUT_SECONDS} second timeout.
     *
     * @throws TimeoutException     if connection timeout period has expired.
     * @throws InterruptedException if connection has been interrupted before connected.
     */
    @VisibleForTesting
    @WorkerThread
    IAdvertisingIdService getIdServiceFromConnection()
            throws TimeoutException, InterruptedException {
        // Block until the bind is complete, or timeout period is over.
        return IAdvertisingIdService.Stub.asInterface(mConnection.getServiceWithTimeout());
    }

    /**
     * A one-time use ServiceConnection that facilitates waiting for the bind to complete and the
     * passing of the IBinder from the callback thread to the waiting thread.
     */
    class BlockingServiceConnection implements ServiceConnection {
        // Facilitates passing of the IBinder across threads
        private final BlockingQueue<IBinder> mBlockingQueue = new LinkedBlockingQueue<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBlockingQueue.add(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            finish();
        }

        /**
         * Blocks until the bind is complete with a timeout and returns the bound IBinder. This must
         * only be called once.
         *
         * @return the IBinder of the bound service
         * @throws InterruptedException if the current thread is interrupted while waiting for
         *                              the bind
         * @throws TimeoutException     if the timeout period has elapsed
         */
        @WorkerThread
        @NonNull
        IBinder getServiceWithTimeout() throws InterruptedException, TimeoutException {
            IBinder binder =
                    mBlockingQueue.poll(SERVICE_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (binder == null) {
                throw new TimeoutException("Timed out waiting for the service connection");
            }
            return binder;
        }
    }
}
