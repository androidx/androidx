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

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import androidx.wear.tiles.proto.RequestProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Variant of {@link TileUpdateRequester} which requests an update from the Wear SysUI app. */
class SysUiTileUpdateRequester implements TileUpdateRequester {

    private static final String TAG = "HTUpdateRequester";

    private static final String DEFAULT_TARGET_SYSUI = "com.google.android.wearable.app";
    private static final String SYSUI_SETTINGS_KEY = "clockwork_sysui_package";

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};

    public static final String ACTION_BIND_UPDATE_REQUESTER =
            "androidx.wear.tiles.action.BIND_UPDATE_REQUESTER";
    @VisibleForTesting static final String CATEGORY_HOME_MAIN = CATEGORY_HOME + "_MAIN";

    final Context mAppContext;

    final Object mLock = new Object();

    @GuardedBy("mLock")
    boolean mBindInProgress = false;

    @GuardedBy("mLock")
    final Set<PendingRequest> mPendingRequests = new HashSet<>();

    public SysUiTileUpdateRequester(@NonNull Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends TileService> tileService) {
        requestUpdateInternal(new PendingRequest(tileService));
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends TileService> tileService, int tileId) {
        requestUpdateInternal(new PendingRequest(tileService, tileId));
    }

    private void requestUpdateInternal(PendingRequest pendingRequest) {
        synchronized (mLock) {
            mPendingRequests.add(pendingRequest);

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

    private String getSysUiPackageNameOnU() {
        Intent queryIntent = new Intent(ACTION_MAIN);
        queryIntent.addCategory(CATEGORY_HOME_MAIN);

        PackageManager pm = mAppContext.getPackageManager();
        ResolveInfo homeActivity =
                pm.resolveActivity(queryIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (homeActivity == null) {
            Log.e(TAG, "Couldn't find SysUi packageName");
            return DEFAULT_TARGET_SYSUI;
        }

        return homeActivity.activityInfo.packageName;
    }

    private String getSysUiPackageName() {
        if (VERSION.SDK_INT == VERSION_CODES.UPSIDE_DOWN_CAKE
                && mAppContext.getApplicationInfo().targetSdkVersion
                        > VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getSysUiPackageNameOnU();
        }

        String sysUiPackageName =
                Settings.Global.getString(mAppContext.getContentResolver(), SYSUI_SETTINGS_KEY);

        if (sysUiPackageName == null || sysUiPackageName.isEmpty()) {
            return DEFAULT_TARGET_SYSUI;
        } else {
            return sysUiPackageName;
        }
    }

    private @Nullable Intent buildUpdateBindIntent() {
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
                        List<PendingRequest> pendingRequestsCopy;

                        synchronized (mLock) {
                            pendingRequestsCopy = new ArrayList<>(mPendingRequests);
                            mPendingRequests.clear();
                            mBindInProgress = false;
                        }

                        // This is a little suboptimal, as if an update is requested in this lock,
                        // we'll unbind, then immediately rebind. That said, this class should be
                        // used pretty rarely
                        // (and it'll be rare to have two in-flight update requests at once
                        // regardless), so
                        // it's probably fine.
                        TileUpdateRequesterService updateRequesterService =
                                TileUpdateRequesterService.Stub.asInterface(service);

                        for (PendingRequest pendingRequest : pendingRequestsCopy) {
                            sendTileUpdateRequest(pendingRequest, updateRequesterService);
                        }

                        mAppContext.unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {}
                },
                Context.BIND_AUTO_CREATE);
    }

    void sendTileUpdateRequest(
            PendingRequest pendingRequest, TileUpdateRequesterService updateRequesterService) {
        try {
            ComponentName providerName = new ComponentName(mAppContext, pendingRequest.mService);
            byte[] requestBytes =
                    pendingRequest.mTileId == null
                            ? EMPTY_BYTE_ARRAY
                            : RequestProto.TileUpdateRequest.newBuilder()
                                    .setTileId(pendingRequest.mTileId)
                                    .build()
                                    .toByteArray();
            updateRequesterService.requestUpdate(
                    providerName,
                    new TileUpdateRequestData(requestBytes, TileUpdateRequestData.VERSION_1));
        } catch (RemoteException ex) {
            Log.w(TAG, "RemoteException while requesting tile update");
        }
    }

    private static class PendingRequest {
        final Class<? extends Service> mService;
        final @Nullable Integer mTileId;

        PendingRequest(Class<? extends Service> service, @Nullable Integer tileId) {
            this.mService = service;
            this.mTileId = tileId;
        }

        PendingRequest(Class<? extends Service> service) {
            this(service, null);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PendingRequest)) {
                return false;
            }
            PendingRequest that = (PendingRequest) obj;
            return mService.equals(that.mService) && Objects.equals(mTileId, that.mTileId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mService, mTileId);
        }
    }
}
