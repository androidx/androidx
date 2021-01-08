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

package androidx.wear.tiles;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Variant of {@link TileUpdateRequester} which requests an update from the Wear SysUI app. */
// TODO(b/173688156): Renovate this whole class, and especially work around all the locks.
public class SysUiTileUpdateRequester implements TileUpdateRequester {
    private static final String TAG = "HTUpdateRequester";

    // TODO(b/174002885): Stop hardcoding Home package name.
    private static final String TARGET_SYSUI = "com.google.android.wearable.app";

    public static final String ACTION_BIND_UPDATE_REQUESTER =
            "androidx.wear.tiles.action.BIND_UPDATE_REQUESTER";

    private final Context mAppContext;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    TileUpdateRequesterService mUpdateRequestService;

    @GuardedBy("mLock")
    boolean mBindInProgress = false;

    @GuardedBy("mLock")
    final Set<Class<? extends Service>> mPendingServices = new HashSet<>();

    public SysUiTileUpdateRequester(@NonNull Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends Service> tileProvider) {
        synchronized (mLock) {
            if (mUpdateRequestService != null && mUpdateRequestService.asBinder().isBinderAlive()) {
                sendTileUpdateRequest(tileProvider);
                return;
            } else if (mBindInProgress) {
                // Update scheduled anyway, skip.
                mPendingServices.add(tileProvider);
                return;
            } else {
                // Can release the lock after this
                mPendingServices.add(tileProvider);
                mBindInProgress = true;
            }
        }

        // Something was wrong with the binder, trigger a request.
        Intent bindIntent = buildUpdateBindIntent();

        if (bindIntent == null) {
            Log.e(TAG, "Could not build bind intent");
            synchronized (mLock) {
                mBindInProgress = false;
            }
            return;
        }

        bindAndUpdate(bindIntent);
    }

    @Nullable
    private Intent buildUpdateBindIntent() {
        Intent bindIntent = new Intent(ACTION_BIND_UPDATE_REQUESTER);
        bindIntent.setPackage(TARGET_SYSUI);

        // Find the concrete ComponentName of the service that implements what we need.
        PackageManager pm = mAppContext.getPackageManager();

        List<ResolveInfo> services =
                pm.queryIntentServices(
                        bindIntent,
                        PackageManager.GET_META_DATA | PackageManager.GET_RESOLVED_FILTER);

        if (services.isEmpty()) {
            Log.w(TAG, "Couldn't find any services filtering on " + ACTION_BIND_UPDATE_REQUESTER);
            return null;
        }

        ServiceInfo serviceInfo = services.get(0).serviceInfo;

        bindIntent.setClassName(serviceInfo.packageName, serviceInfo.name);

        return bindIntent;
    }

    // TODO(b/174002003): Make this unbind from the service.
    private void bindAndUpdate(Intent i) {
        mAppContext.bindService(
                i,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        synchronized (mLock) {
                            mUpdateRequestService =
                                    TileUpdateRequesterService.Stub.asInterface(service);
                            mBindInProgress = false;

                            for (Class<? extends Service> tileProvider : mPendingServices) {
                                sendTileUpdateRequest(tileProvider);
                            }

                            mPendingServices.clear();
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        synchronized (mLock) {
                            mUpdateRequestService = null;
                        }
                    }
                },
                Context.BIND_AUTO_CREATE);
    }

    @GuardedBy("mLock")
    void sendTileUpdateRequest(Class<? extends Service> tileProvider) {
        try {
            ComponentName cn = new ComponentName(mAppContext, tileProvider);
            mUpdateRequestService.requestUpdate(cn, new TileUpdateRequestData());
        } catch (RemoteException ex) {
            Log.w(TAG, "RemoteException while requesting tile update");
        }
    }
}
