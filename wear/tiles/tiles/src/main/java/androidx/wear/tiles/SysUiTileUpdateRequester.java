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
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Variant of {@link TileUpdateRequester} which requests an update from the Wear SysUI app. */
// TODO(b/173688156): Renovate this whole class, and especially work around all the locks.
class SysUiTileUpdateRequester implements TileUpdateRequester {
    private static final String TAG = "HTUpdateRequester";

    private static final String DEFAULT_TARGET_SYSUI = "com.google.android.wearable.app";
    private static final String SYSUI_SETTINGS_KEY = "clockwork_sysui_package";

    public static final String ACTION_BIND_UPDATE_REQUESTER =
            "androidx.wear.tiles.action.BIND_UPDATE_REQUESTER";

    final Context mAppContext;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    boolean mBindInProgress = false;

    @GuardedBy("mLock")
    final Set<Class<? extends Service>> mPendingServices = new HashSet<>();

    public SysUiTileUpdateRequester(@NonNull Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends TileProviderService> tileProvider) {
        synchronized (mLock) {
            mPendingServices.add(tileProvider);

            if (mBindInProgress) {
                // Something else kicked off the bind; let that carry on binding.
                return;
            } else {
                mBindInProgress = true;
            }
        }

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

    private String getSysUiPackageName() {
        String sysUiPackageName =
                Settings.Global.getString(mAppContext.getContentResolver(), SYSUI_SETTINGS_KEY);

        if (sysUiPackageName == null || sysUiPackageName.isEmpty()) {
            return DEFAULT_TARGET_SYSUI;
        } else {
            return sysUiPackageName;
        }
    }

    @Nullable
    private Intent buildUpdateBindIntent() {
        Intent bindIntent = new Intent(ACTION_BIND_UPDATE_REQUESTER);
        bindIntent.setPackage(getSysUiPackageName());

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

    private void bindAndUpdate(Intent i) {
        mAppContext.bindService(
                i,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        // Copy so we can shorten the lock duration.
                        List<Class<? extends Service>> pendingServicesCopy;

                        synchronized (mLock) {
                            pendingServicesCopy = new ArrayList<>(mPendingServices);
                            mPendingServices.clear();
                            mBindInProgress = false;
                        }

                        // This is a little suboptimal, as if an update is requested in this lock,
                        // we'll unbind, then immediately rebind. That said, this class should be
                        // used pretty rarely
                        // (and it'll be rare to have two in-flight update requests at once
                        // regardless), so it's probably fine.
                        TileUpdateRequesterService updateRequesterService =
                                TileUpdateRequesterService.Stub.asInterface(service);

                        for (Class<? extends Service> tileProvider : pendingServicesCopy) {
                            sendTileUpdateRequest(tileProvider, updateRequesterService);
                        }

                        mAppContext.unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {}
                },
                Context.BIND_AUTO_CREATE);
    }

    void sendTileUpdateRequest(
            Class<? extends Service> tileProvider,
            TileUpdateRequesterService updateRequesterService) {
        try {
            ComponentName cn = new ComponentName(mAppContext, tileProvider);
            updateRequesterService.requestUpdate(cn, new TileUpdateRequestData());
        } catch (RemoteException ex) {
            Log.w(TAG, "RemoteException while requesting tile update");
        }
    }
}
