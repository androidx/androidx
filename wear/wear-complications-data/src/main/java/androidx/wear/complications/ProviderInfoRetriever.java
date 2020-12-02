/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.IProviderInfoService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Retrieves {@link ComplicationProviderInfo} for a watch face's complications.
 *
 * <p>To use construct an instance and call {@link #retrieveProviderInfo} which returns a {@link
 * ListenableFuture}.
 *
 * <p>Further calls to {@link #retrieveProviderInfo} may be made using the same instance of this
 * class, but {@link #close} must be called when it is no longer needed. Once release has been
 * called, further retrieval attempts will fail.
 */
public class ProviderInfoRetriever implements AutoCloseable {

    /** Results for {@link #retrieveProviderInfo}. */
    public static class ProviderInfo {
        /** The id for the complication, as provided to {@link #retrieveProviderInfo}. */
        private final int mWatchFaceComplicationId;

        /**
         * Details of the provider for that complication, or {@code null} if no provider is
         * currently configured.
         */
        @Nullable
        private final ComplicationProviderInfo mInfo;

        ProviderInfo(int watchFaceComplicationId, @Nullable ComplicationProviderInfo info) {
            mWatchFaceComplicationId = watchFaceComplicationId;
            mInfo = info;
        }

        /**
         * Returns the id for the complication, as provided to {@link #retrieveProviderInfo}.
         */
        public int getWatchFaceComplicationId() {
            return mWatchFaceComplicationId;
        }

        /**
         * Return details of the provider for that complication, or {@code null} if no provider is
         * currently configured.
         */
        @Nullable
        public ComplicationProviderInfo getInfo() {
            return mInfo;
        }
    }

    private final class ProviderInfoServiceConnection implements ServiceConnection {
        @Override
        @SuppressLint("SyntheticAccessor")
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceFuture.set(IProviderInfoService.Stub.asInterface(service));
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public void onServiceDisconnected(ComponentName name) {
            mServiceFuture.cancel(false);
        }
    }

    /** The package of the service that supplies provider info. */
    private static final String PROVIDER_INFO_SERVICE_PACKAGE = "com.google.android.wearable.app";

    private static final String ACTION_GET_COMPLICATION_CONFIG =
            "android.support.wearable.complications.ACTION_GET_COMPLICATION_CONFIG";

    @SuppressLint("SyntheticAccessor")
    private final ServiceConnection mConn = new ProviderInfoServiceConnection();

    @NonNull private final Context mContext;

    private final ResolvableFuture<IProviderInfoService> mServiceFuture = ResolvableFuture.create();

    /**
     * @param context  the current context
     */
    public ProviderInfoRetriever(@NonNull Context context) {
        mContext = context;

        Intent intent = new Intent(ACTION_GET_COMPLICATION_CONFIG);
        intent.setPackage(PROVIDER_INFO_SERVICE_PACKAGE);
        mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * Requests {@link ComplicationProviderInfo} for the specified complication ids on the specified
     * watch face. When the info is received, the listener will receive a callback for each id.
     * These callbacks will occur on the main thread.
     *
     * <p>This will only work if the package of the current app is the same as the package of the
     * specified watch face.
     *
     * @param watchFaceComponent the ComponentName of the WatchFaceService for which info is
     *     being requested
     * @param watchFaceComplicationIds ids of the complications that info is being requested for
     * @return A {@link ListenableFuture} for the requested provider info. If the look up fails
     *     null will be returned
     */
    @NonNull
    public ListenableFuture<ProviderInfo> retrieveProviderInfo(
            @NonNull final ComponentName watchFaceComponent,
            @NonNull final int[] watchFaceComplicationIds) {
        final ResolvableFuture<ProviderInfo> mResultFuture = ResolvableFuture.create();
        mServiceFuture.addListener(
                () -> {
                    try {
                        if (mServiceFuture.isCancelled())  {
                            mResultFuture.set(null);
                            return;
                        }
                        ComplicationProviderInfo[] infos =
                                mServiceFuture.get().getProviderInfos(watchFaceComponent,
                                        watchFaceComplicationIds);
                        if (infos != null) {
                            for (int i = 0; i < infos.length; i++) {
                                final int watchFaceComplicationId =
                                        watchFaceComplicationIds[i];
                                final ComplicationProviderInfo info = infos[i];
                                mResultFuture.set(
                                        new ProviderInfo(watchFaceComplicationId, info));
                            }
                        } else {
                            mResultFuture.set(null);
                        }
                    } catch (RemoteException e) {
                        mResultFuture.setException(e);
                    } catch (InterruptedException e) {
                        mResultFuture.setException(e);
                    } catch (ExecutionException e) {
                        mResultFuture.setException(e);
                    }
                },
                runnable -> runnable.run()
        );
        return mResultFuture;
    }

    /**
     * Releases the connection to the complication system used by this class. This must
     * be called when the retriever is no longer needed.
     *
     * <p>Any outstanding or subsequent futures returned by {@link #retrieveProviderInfo} will
     * resolve with null.
     *
     * This class implements the Java {@code AutoClosable} interface and
     * may be used with try-with-resources.
     */
    @Override
    public void close() {
        mContext.unbindService(mConn);
    }
}
