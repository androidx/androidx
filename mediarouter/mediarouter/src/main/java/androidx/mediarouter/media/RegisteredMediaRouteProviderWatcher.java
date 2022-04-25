/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.MediaRoute2ProviderService;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Watches for media route provider services to be installed.
 * Adds a provider to the media router for each registered service.
 *
 * @see RegisteredMediaRouteProvider
 */
final class RegisteredMediaRouteProviderWatcher {
    private final Context mContext;
    final Callback mCallback;
    private final Handler mHandler;
    private final PackageManager mPackageManager;

    private final ArrayList<RegisteredMediaRouteProvider> mProviders = new ArrayList<>();
    private boolean mRunning;

    @SuppressWarnings("deprecation")
    RegisteredMediaRouteProviderWatcher(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
        mHandler = new Handler();
        mPackageManager = context.getPackageManager();
    }

    public void start() {
        if (!mRunning) {
            mRunning = true;

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
            filter.addDataScheme("package");
            mContext.registerReceiver(mScanPackagesReceiver, filter, null, mHandler);

            // Scan packages.
            // Also has the side-effect of restarting providers if needed.
            mHandler.post(mScanPackagesRunnable);
        }
    }

    public void rescan() {
        mHandler.post(mScanPackagesRunnable);
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;

            mContext.unregisterReceiver(mScanPackagesReceiver);
            mHandler.removeCallbacks(mScanPackagesRunnable);

            // Stop all providers.
            for (int i = mProviders.size() - 1; i >= 0; i--) {
                mProviders.get(i).stop();
            }
        }
    }

    @SuppressWarnings("deprecation")
    void scanPackages() {
        if (!mRunning) {
            return;
        }

        List<ServiceInfo> mediaRoute2ProviderServices = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaRoute2ProviderServices = getMediaRoute2ProviderServices();
        }

        // Add providers for all new services.
        // Reorder the list so that providers left at the end will be the ones to remove.
        int targetIndex = 0;
        Intent intent = new Intent(MediaRouteProviderService.SERVICE_INTERFACE);
        for (ResolveInfo resolveInfo : mPackageManager.queryIntentServices(intent, 0)) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo == null) {
                continue;
            }
            if (MediaRouter.isMediaTransferEnabled()
                    && listContainsServiceInfo(mediaRoute2ProviderServices, serviceInfo)) {
                // Do not register services which supports MediaRoute2ProviderService,
                // since we will communicate with them via MediaRouter2.
                continue;
            }
            int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
            if (sourceIndex < 0) {
                RegisteredMediaRouteProvider provider = new RegisteredMediaRouteProvider(
                        mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name));
                provider.setControllerCallback(
                        controller -> mCallback.releaseProviderController(provider, controller));
                provider.start();
                mProviders.add(targetIndex++, provider);
                mCallback.addProvider(provider);
            } else if (sourceIndex >= targetIndex) {
                RegisteredMediaRouteProvider provider = mProviders.get(sourceIndex);
                provider.start(); // restart the provider if needed
                provider.rebindIfDisconnected();
                Collections.swap(mProviders, sourceIndex, targetIndex++);
            }
        }

        // Remove providers for missing services.
        if (targetIndex < mProviders.size()) {
            for (int i = mProviders.size() - 1; i >= targetIndex; i--) {
                RegisteredMediaRouteProvider provider = mProviders.get(i);
                mCallback.removeProvider(provider);
                mProviders.remove(provider);
                provider.setControllerCallback(null);
                provider.stop();
            }
        }
    }

    static boolean listContainsServiceInfo(List<ServiceInfo> serviceList, ServiceInfo target) {
        if (target == null || serviceList == null || serviceList.isEmpty()) {
            return false;
        }
        for (ServiceInfo serviceInfo : serviceList) {
            if (target.packageName.equals(serviceInfo.packageName)
                    && target.name.equals(serviceInfo.name)) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @NonNull
    @SuppressWarnings("deprecation")
    List<ServiceInfo> getMediaRoute2ProviderServices() {
        Intent intent = new Intent(MediaRoute2ProviderService.SERVICE_INTERFACE);

        List<ServiceInfo> serviceInfoList = new ArrayList<>();
        for (ResolveInfo resolveInfo : mPackageManager.queryIntentServices(intent, 0)) {
            serviceInfoList.add(resolveInfo.serviceInfo);
        }
        return serviceInfoList;
    }

    private int findProvider(String packageName, String className) {
        int count = mProviders.size();
        for (int i = 0; i < count; i++) {
            RegisteredMediaRouteProvider provider = mProviders.get(i);
            if (provider.hasComponentName(packageName, className)) {
                return i;
            }
        }
        return -1;
    }

    private final BroadcastReceiver mScanPackagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanPackages();
        }
    };

    private final Runnable mScanPackagesRunnable = new Runnable() {
        @Override
        public void run() {
            scanPackages();
        }
    };

    public interface Callback {
        void addProvider(@NonNull MediaRouteProvider provider);
        void removeProvider(@NonNull MediaRouteProvider provider);
        void releaseProviderController(@NonNull RegisteredMediaRouteProvider provider,
                @NonNull MediaRouteProvider.RouteController controller);
    }
}
