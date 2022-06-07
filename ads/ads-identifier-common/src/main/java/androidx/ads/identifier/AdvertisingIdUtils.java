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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal utilities for Advertising ID.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AdvertisingIdUtils {

    /**
     * The Intent action used to identify an Advertising ID Provider. The Advertising ID Provider
     * Service should declare this as an intent-filter, so that clients can find it.
     */
    public static final String GET_AD_ID_ACTION = "androidx.ads.identifier.provider.GET_AD_ID";

    /**
     * The permission used to indicate which Advertising ID Provider should be used in case there
     * are multiple Advertising ID Providers on the device. Device manufacturer (OEM) should only
     * grant this permission to the designated Advertising ID Provider.
     */
    @VisibleForTesting
    static final String HIGH_PRIORITY_PERMISSION = "androidx.ads.identifier.provider.HIGH_PRIORITY";

    AdvertisingIdUtils() {
    }

    /**
     * Retrieves a list of all Advertising ID Providers' services on this device.
     *
     * <p>This is achieved by looking up which services can handle {@link #GET_AD_ID_ACTION}
     * intent action.
     * <p>Only system-level providers will be returned.
     */
    @NonNull
    @SuppressWarnings({"MixedMutabilityReturnType", "deprecation"})
    public static List<ServiceInfo> getAdvertisingIdProviderServices(
            @NonNull PackageManager packageManager) {
        Intent intent = new Intent(GET_AD_ID_ACTION);

        List<ResolveInfo> resolveInfos =
                packageManager.queryIntentServices(intent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                                ? PackageManager.MATCH_SYSTEM_ONLY : 0);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceInfo> systemLevelServiceInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    || isSystemByApplicationInfo(serviceInfo.packageName, packageManager)) {
                systemLevelServiceInfos.add(serviceInfo);
            }
        }
        return systemLevelServiceInfos;
    }

    @SuppressWarnings("deprecation")
    private static boolean isSystemByApplicationInfo(
            @NonNull String packageName, @NonNull PackageManager packageManager) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException ignored) {
            // Ignore this provider if name not found.
            return false;
        }
    }

    /**
     * Selects the Service of an Advertising ID Provider which should be used by developer
     * library when requesting an Advertising ID.
     *
     * <p>Note: This method should only be used with the {@link ServiceInfo}s from
     * {@link #getAdvertisingIdProviderServices} method, this currently means that only
     * system-level Providers will be selected.
     * <p>It will return the same Advertising ID Provider for all apps which use the developer
     * library, using this priority:
     * <ol>
     * <li>Providers with {@link #HIGH_PRIORITY_PERMISSION} permission
     * <li>Other Providers
     * </ol>
     * <p>If there are ties in any of the above categories, it will use this priority:
     * <ol>
     * <li>First app by earliest install time ({@link PackageInfo#firstInstallTime})
     * <li>First app by package name alphabetically sorted
     * </ol>
     *
     * @return null if the input {@code serviceInfos} is null or empty, or non of the input
     * package is found.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static ServiceInfo selectServiceByPriority(
            @NonNull List<ServiceInfo> serviceInfos, @NonNull PackageManager packageManager) {
        if (serviceInfos.isEmpty()) {
            return null;
        }
        ServiceInfo selectedServiceInfo = null;
        PackageInfo selectedPackageInfo = null;
        for (ServiceInfo serviceInfo : serviceInfos) {
            PackageInfo packageInfo;
            try {
                packageInfo =
                        packageManager.getPackageInfo(
                                serviceInfo.packageName, PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Ignore this provider if name not found.
                continue;
            }
            if (selectedPackageInfo == null
                    || hasHigherPriority(packageInfo, selectedPackageInfo)) {
                selectedServiceInfo = serviceInfo;
                selectedPackageInfo = packageInfo;
            }
        }
        return selectedServiceInfo;
    }

    private static boolean hasHigherPriority(PackageInfo candidate, PackageInfo currentHighest) {
        boolean isCandidateRequestHighPriority = isRequestHighPriority(candidate);
        boolean isCurrentHighestRequestHighPriority = isRequestHighPriority(currentHighest);
        if (isCandidateRequestHighPriority != isCurrentHighestRequestHighPriority) {
            return isCandidateRequestHighPriority;
        }
        if (candidate.firstInstallTime != currentHighest.firstInstallTime) {
            return candidate.firstInstallTime < currentHighest.firstInstallTime;
        }
        return candidate.packageName.compareTo(currentHighest.packageName) < 0;
    }

    private static boolean isRequestHighPriority(PackageInfo packageInfo) {
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        for (String permission : packageInfo.requestedPermissions) {
            if (HIGH_PRIORITY_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
