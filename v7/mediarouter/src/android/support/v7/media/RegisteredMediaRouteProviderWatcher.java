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

package android.support.v7.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Watches for media route provider services to be installed.
 * Adds a provider to the media router for each registered service.
 *
 * @see RegisteredMediaRouteProvider
 */
final class RegisteredMediaRouteProviderWatcher {
    private final Context mContext;
    private final MediaRouter mRouter;
    private final ArrayList<RegisteredMediaRouteProvider> mProviders =
            new ArrayList<RegisteredMediaRouteProvider>();
    private final PackageManager mPackageManager;

    public RegisteredMediaRouteProviderWatcher(Context context, MediaRouter router) {
        mContext = context;
        mRouter = router;
        mPackageManager = context.getPackageManager();
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanPackages();
            }
        }, filter);

        scanPackages();
    }

    private void scanPackages() {
        // Add providers for all new services.
        // Reorder the list so that providers left at the end will be the ones to remove.
        int targetIndex = 0;
        Intent intent = new Intent(MediaRouteProviderService.SERVICE_INTERFACE);
        for (ResolveInfo resolveInfo : mPackageManager.queryIntentServices(intent, 0)) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo != null) {
                int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
                if (sourceIndex < 0) {
                    RegisteredMediaRouteProvider provider =
                            new RegisteredMediaRouteProvider(mContext,
                            new ComponentName(serviceInfo.packageName, serviceInfo.name));
                    provider.start();
                    mProviders.add(targetIndex++, provider);
                    mRouter.addProvider(provider);
                } else if (sourceIndex >= targetIndex) {
                    RegisteredMediaRouteProvider provider = mProviders.get(sourceIndex);
                    provider.rebindIfDisconnected();
                    Collections.swap(mProviders, sourceIndex, targetIndex++);
                }
            }
        }

        // Remove providers for missing services.
        if (targetIndex < mProviders.size()) {
            for (int i = mProviders.size() - 1; i >= targetIndex; i--) {
                RegisteredMediaRouteProvider provider = mProviders.get(i);
                mRouter.removeProvider(provider);
                mProviders.remove(provider);
                provider.stop();
            }
        }
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
}
