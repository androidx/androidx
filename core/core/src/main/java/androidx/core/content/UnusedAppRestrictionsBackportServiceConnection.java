/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.content;

import static androidx.core.content.PackageManagerCompat.getPermissionRevocationVerifierApp;
import static androidx.core.content.UnusedAppRestrictionsBackportService.ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_30_BACKPORT;
import static androidx.core.content.UnusedAppRestrictionsConstants.DISABLED;
import static androidx.core.content.UnusedAppRestrictionsConstants.ERROR;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportService;

/**
 * {@link ServiceConnection} to use while binding to a
 * {@link IUnusedAppRestrictionsBackportService}.
 *
 * The lifecycle of this object is as follows:
 * <ul>
 *     <li>Call {@link #connectAndFetchResult(ResolvableFuture)}}
 *     <li>This method binds the service and fetches the data from it
 *     <li>It is recommended that the caller disconnects the connection after serving the request,
 *     and creates a new connection if a new request is necessary
 * </ul>
 *
 * The data flow is as follows:
 * <ol>
 *     <li>3P app calls {@link PackageManagerCompat#getUnusedAppRestrictionsStatus(Context)}
 *     <li>Jetpack calls {@link #connectAndFetchResult(ResolvableFuture)}
 *     <li>This method triggers binding of the {@link UnusedAppRestrictionsBackportService}.
 *     Once the service is bound, isPermissionRevocationEnabledForApp is called with a
 *     {@link UnusedAppRestrictionsBackportCallback}
 *     <li>This callback is defined in Jetpack, which maps the results of
 *     isPermissionRevocationEnabledForApp to an
 *     {@link PackageManagerCompat.UnusedAppRestrictionsStatus} and sets it as the
 *     {@link ResolvableFuture}'s value
 *     <li>The 3P app attaches a listener to the returned {@link ResolvableFuture} that utilizes
 *     the calculated UnusedAppRestrictionsStatus
 * </ol>
 */
class UnusedAppRestrictionsBackportServiceConnection implements ServiceConnection  {

    @VisibleForTesting @Nullable IUnusedAppRestrictionsBackportService
            mUnusedAppRestrictionsService = null;
    @NonNull ResolvableFuture<Integer> mResultFuture;

    private final Context mContext;
    private boolean mHasBoundService = false;

    UnusedAppRestrictionsBackportServiceConnection(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mUnusedAppRestrictionsService =
                IUnusedAppRestrictionsBackportService.Stub.asInterface(service);

        try {
            mUnusedAppRestrictionsService.isPermissionRevocationEnabledForApp(
                    getBackportCallback());
        } catch (RemoteException e) {
            // Unable to call bound service's isPermissionRevocationEnabledForApp().
            mResultFuture.set(ERROR);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mUnusedAppRestrictionsService = null;
    }

    public void connectAndFetchResult(@NonNull ResolvableFuture<Integer> resultFuture) {
        if (mHasBoundService) {
            throw new IllegalStateException("Each UnusedAppRestrictionsBackportServiceConnection "
                    + "can only be bound once.");
        }
        mHasBoundService = true;
        mResultFuture = resultFuture;

        Intent intent = new Intent(ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION)
                .setPackage(getPermissionRevocationVerifierApp(
                        mContext.getPackageManager()));
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void disconnectFromService() {
        if (!mHasBoundService) {
            throw new IllegalStateException("bindService must be called before unbind");
        }
        mHasBoundService = false;
        mContext.unbindService(this);
    }

    private IUnusedAppRestrictionsBackportCallback getBackportCallback() {
        return new IUnusedAppRestrictionsBackportCallback.Stub() {
            @Override
            public void onIsPermissionRevocationEnabledForAppResult(boolean success,
                    boolean isEnabled) throws RemoteException {
                if (success) {
                    if (isEnabled) {
                        mResultFuture.set(API_30_BACKPORT);
                    } else {
                        mResultFuture.set(DISABLED);
                    }
                } else {
                    // Unable to retrieve the permission revocation setting
                    mResultFuture.set(ERROR);
                    Log.e(PackageManagerCompat.LOG_TAG, "Unable to retrieve the permission "
                            + "revocation setting from the backport");
                }
            }
        };
    }
}

