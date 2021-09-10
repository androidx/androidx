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

package androidx.ads.identifier.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import androidx.ads.identifier.AdvertisingIdUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * The AdvertisingIdProviderManager will be used by an Advertising ID Provider to register the
 * provider implementation or retrieve all the Advertising ID Providers on the device.
 *
 * This package contains an implementation of the Advertising ID Provider Service that supports
 * IAdvertisingIdService.aidl for communication with the developer library, allowing you to
 * easily make your own Advertising ID Provider. Simply do the following:
 * <ol>
 * <li>Implement the {@link AdvertisingIdProvider} interface in the provider library. Developer apps
 * will be interacting with the provider through this programmatic interface.
 * <li>Register the implementation by calling {@link #registerProviderCallable} within the
 * provider’s {@link android.app.Application#onCreate} callback.
 * <li>Register the Advertising Id settings UI with the intent filter
 * "androidx.ads.identifier.provider.OPEN_SETTINGS".
 * </ol>
 *
 * @deprecated Use the
 * <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient">
 * Advertising ID API that's available as part of Google Play Services</a> instead of this library.
 */
@Deprecated
public class AdvertisingIdProviderManager {

    @VisibleForTesting
    static final String OPEN_SETTINGS_ACTION = "androidx.ads.identifier.provider.OPEN_SETTINGS";

    private static Callable<AdvertisingIdProvider> sProviderCallable = null;

    private AdvertisingIdProviderManager() {
    }

    /**
     * Registers the {@link Callable} to create an instance of {@link AdvertisingIdProvider}.
     *
     * <p>This is used to lazy load the {@link AdvertisingIdProvider} when the Service is started.
     * <p>This {@link Callable} will be called within the library's built-in Advertising ID
     * Service's {@link android.app.Service#onCreate} method.
     * <p>Provider could call this method to register the implementation in
     * {@link android.app.Application#onCreate}, which is before
     * {@link android.app.Service#onCreate} has been called.
     */
    public static void registerProviderCallable(
            @NonNull Callable<AdvertisingIdProvider> providerCallable) {
        sProviderCallable = Preconditions.checkNotNull(providerCallable);
    }

    /**
     * Gets the {@link Callable} to create the Advertising ID Provider.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public static Callable<AdvertisingIdProvider> getProviderCallable() {
        return sProviderCallable;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting
    public static void clearProviderCallable() {
        sProviderCallable = null;
    }

    /**
     * Retrieves a list of all the Advertising ID Providers' information on this device, including
     * self and other providers which is based on the AndroidX Advertising ID Provider library.
     *
     * <p>This method helps one Advertising ID Provider find other providers. One usage of this is
     * to link to other providers' settings activity from one provider's settings activity, so the
     * user of the device can manager all the providers' settings together.
     */
    @NonNull
    @SuppressWarnings("MixedMutabilityReturnType")
    public static List<AdvertisingIdProviderInfo> getAdvertisingIdProviders(
            @NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ServiceInfo> serviceInfos =
                AdvertisingIdUtils.getAdvertisingIdProviderServices(packageManager);
        if (serviceInfos.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> activityMap = getOpenSettingsActivities(packageManager);
        ServiceInfo highestPriorityServiceInfo =
                AdvertisingIdUtils.selectServiceByPriority(serviceInfos, packageManager);

        List<AdvertisingIdProviderInfo> providerInfos = new ArrayList<>();
        for (ServiceInfo serviceInfo : serviceInfos) {
            String packageName = serviceInfo.packageName;

            AdvertisingIdProviderInfo.Builder builder =
                    AdvertisingIdProviderInfo.builder()
                            .setPackageName(packageName)
                            .setHighestPriority(serviceInfo == highestPriorityServiceInfo);
            String activityName = activityMap.get(packageName);
            if (activityName != null) {
                builder.setSettingsIntent(
                        new Intent(OPEN_SETTINGS_ACTION).setClassName(packageName, activityName));
            }
            providerInfos.add(builder.build());
        }
        return providerInfos;
    }

    /**
     * Retrieves a {@link Map} from package name to settings activity name.
     *
     * <p>This is achieved by looking up which activities can handle {@link #OPEN_SETTINGS_ACTION}
     * intent action.
     */
    @SuppressWarnings("MixedMutabilityReturnType")
    private static Map<String, String> getOpenSettingsActivities(PackageManager packageManager) {
        Intent settingsIntent = new Intent(OPEN_SETTINGS_ACTION);
        List<ResolveInfo> settingsResolveInfos = packageManager.queryIntentActivities(
                settingsIntent, 0);
        if (settingsResolveInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> activityMap = new HashMap<>();
        for (ResolveInfo settingsResolveInfo : settingsResolveInfos) {
            ActivityInfo settingsActivityInfo = settingsResolveInfo.activityInfo;
            activityMap.put(settingsActivityInfo.packageName, settingsActivityInfo.name);
        }
        return activityMap;
    }
}
