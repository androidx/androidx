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

package androidx.ads.identifier;

import android.content.Context;
import android.os.RemoteException;

import androidx.ads.identifier.internal.HoldingConnectionClient;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client for retrieving Advertising ID related info from an AndroidX ID Provider installed on
 * the device.
 *
 * <p>Typical usage would be:
 * <ol>
 * <li>Call {@link #isAdvertisingIdProviderAvailable} to make sure there is an Advertising ID
 * Provider available.
 * <li>Call {@link #getAdvertisingIdInfo} to get Advertising ID info (the Advertising ID and LAT
 * setting).
 * </ol>
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
public class AdvertisingIdClient {

    /**
     * Amount of time to wait before timing out when trying to get the ID info from the
     * Provider. Including the binding service time and the remote calling time.
     */
    private static final long TIMEOUT_SECONDS = 20;

    private static final long AUTO_DISCONNECT_SECONDS = 30;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static final ExecutorService QUERY_EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private static final Object sLock = new Object();

    /**
     * The client holding connection which can be reused if connected.
     *
     * <p>This value will only be set at 2 places at production when setup new connection or auto
     * disconnect timeout happen, and 1 place at testing when clear connection.
     * <p>There could be multiple connection clients in corner cases, but each of them will be
     * auto disconnect eventually.
     * <p>Each connection client has a last connection ID field, which ties to the connection
     * client and also indicates the status of connection. See {@link HoldingConnectionClient}'s
     * mLastConnectionId filed for details.
     * <p>Each get ID instance will get a pair of connection client and connection ID (which ties
     * to the connection client) first, then use this pair to schedule an auto disconnection at
     * {@link #AUTO_DISCONNECT_SECONDS} later.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    static final AtomicReference<HoldingConnectionClient> sConnectionClient =
            new AtomicReference<>(null);

    @AutoValue
    @AutoValue.CopyAnnotations
    @SuppressWarnings("deprecation")
    abstract static class ConnectionPair {
        @NonNull
        abstract HoldingConnectionClient getConnectionClient();

        abstract long getConnectionId();

        @NonNull
        static ConnectionPair of(HoldingConnectionClient connectionClient, long connectionId) {
            return new AutoValue_AdvertisingIdClient_ConnectionPair(connectionClient, connectionId);
        }
    }

    private AdvertisingIdClient() {
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    @NonNull
    static ConnectionPair getConnection(Context context)
            throws IOException, AdvertisingIdNotAvailableException, TimeoutException,
            InterruptedException {
        ConnectionPair connectionPair = tryConnect();
        if (connectionPair == null) {
            synchronized (sLock) {
                connectionPair = tryConnect();
                if (connectionPair == null) {
                    HoldingConnectionClient connectionClient = new HoldingConnectionClient(context);
                    sConnectionClient.set(connectionClient);
                    connectionPair = ConnectionPair.of(connectionClient, 0);
                }
            }
        }
        return connectionPair;
    }

    @Nullable
    private static ConnectionPair tryConnect() {
        HoldingConnectionClient connectionClient = sConnectionClient.get();
        if (connectionClient != null) {
            long connectionId = connectionClient.askConnectionId();
            if (connectionId >= 0) {
                return ConnectionPair.of(connectionClient, connectionId);
            }
        }
        return null;
    }

    /** Returns the Advertising ID info as {@link AdvertisingIdInfo}. */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @VisibleForTesting
    @WorkerThread
    @NonNull
    static AdvertisingIdInfo getIdInfo(HoldingConnectionClient connectionClient)
            throws IOException, AdvertisingIdNotAvailableException {
        IAdvertisingIdService service = connectionClient.getIdService();

        try {
            String id = service.getId();
            if (id == null || id.trim().isEmpty()) {
                throw new AdvertisingIdNotAvailableException(
                        "Advertising ID Provider does not returns an Advertising ID.");
            }
            return AdvertisingIdInfo.builder()
                    .setId(id)
                    .setProviderPackageName(connectionClient.getPackageName())
                    .setLimitAdTrackingEnabled(service.isLimitAdTrackingEnabled())
                    .build();
        } catch (RemoteException e) {
            throw new IOException("Remote exception", e);
        } catch (RuntimeException e) {
            throw new AdvertisingIdNotAvailableException(
                    "Advertising ID Provider throws a exception.", e);
        }
    }

    @VisibleForTesting
    static void clearConnectionClient() {
        sConnectionClient.set(null);
    }

    @VisibleForTesting
    static boolean isConnected() {
        HoldingConnectionClient connectionClient = sConnectionClient.get();
        return connectionClient != null && connectionClient.isConnected();
    }

    /**
     * Checks whether there is any Advertising ID Provider installed on the device.
     *
     * <p>This method does a quick check for the Advertising ID providers.
     * <p>Note: Even if this method returns true, there is still a possibility that the
     * {@link #getAdvertisingIdInfo(Context)} method throws an exception for some reason.
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return whether there is an Advertising ID Provider available on the device.
     */
    public static boolean isAdvertisingIdProviderAvailable(@NonNull Context context) {
        return !AdvertisingIdUtils.getAdvertisingIdProviderServices(context.getPackageManager())
                .isEmpty();
    }

    /**
     * Retrieves the user's Advertising ID info.
     *
     * <p>When multiple Advertising ID Providers are installed on the device, this method will
     * always return the Advertising ID information from same Advertising ID Provider for all
     * apps which use this library, using following priority:
     * <ol>
     * <li>System-level providers with "androidx.ads.identifier.provider.HIGH_PRIORITY" permission
     * <li>Other system-level providers
     * </ol>
     * <p>If there are ties in any of the above categories, it will use this priority:
     * <ol>
     * <li>First app by earliest install time
     * ({@link android.content.pm.PackageInfo#firstInstallTime})
     * <li>First app by package name alphabetically sorted
     * </ol>
     *
     * @param context Current {@link Context} (such as the current {@link android.app.Activity}).
     * @return A {@link ListenableFuture} that will be fulfilled with a {@link AdvertisingIdInfo}
     * which contains the user's Advertising ID info, or rejected with the following exceptions,
     * <ul>
     * <li><b>IOException</b> signaling connection to Advertising ID Providers failed.
     * <li><b>AdvertisingIdNotAvailableException</b> indicating Advertising ID is not available,
     * like no Advertising ID Provider found or provider does not return an Advertising ID.
     * <li><b>TimeoutException</b> indicating timeout period (20s) has expired.
     * <li><b>InterruptedException</b> indicating the current thread has been interrupted.
     * </ul>
     */
    @NonNull
    public static ListenableFuture<AdvertisingIdInfo> getAdvertisingIdInfo(
            @NonNull Context context) {
        final Context applicationContext = context.getApplicationContext();

        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<AdvertisingIdInfo>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull CallbackToFutureAdapter.Completer<AdvertisingIdInfo>
                                    completer) {
                        submitAdvertisingIdInfoTask(applicationContext, completer);
                        return "getAdvertisingIdInfo";
                    }
                });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static void submitAdvertisingIdInfoTask(
            final Context applicationContext,
            @NonNull final CallbackToFutureAdapter.Completer<AdvertisingIdInfo> completer) {
        final Future<?> getIdInfoFuture = QUERY_EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ConnectionPair connectionPair = getConnection(applicationContext);
                    scheduleAutoDisconnect(connectionPair);
                    completer.set(getIdInfo(connectionPair.getConnectionClient()));
                } catch (IOException | AdvertisingIdNotAvailableException | TimeoutException
                        | InterruptedException e) {
                    completer.setException(e);
                }
            }
        });
        scheduleTimeoutCheck(getIdInfoFuture, completer);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private static void scheduleTimeoutCheck(
            final Future<?> getIdInfoFuture,
            @NonNull final CallbackToFutureAdapter.Completer<AdvertisingIdInfo> completer) {
        SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                if (!getIdInfoFuture.isDone()) {
                    completer.setException(new TimeoutException());
                    getIdInfoFuture.cancel(true);
                }
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @SuppressWarnings({"WeakerAccess", "FutureReturnValueIgnored"}) /* synthetic accessor */
    static void scheduleAutoDisconnect(final ConnectionPair connectionPair) {
        SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                HoldingConnectionClient connectionClient = connectionPair.getConnectionClient();
                if (connectionClient.tryFinish(connectionPair.getConnectionId())) {
                    sConnectionClient.compareAndSet(connectionClient, null);
                }
            }
        }, AUTO_DISCONNECT_SECONDS, TimeUnit.SECONDS);
    }
}
