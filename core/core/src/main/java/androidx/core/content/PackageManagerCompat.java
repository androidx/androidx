/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_30;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_30_BACKPORT;
import static androidx.core.content.UnusedAppRestrictionsConstants.API_31;
import static androidx.core.content.UnusedAppRestrictionsConstants.DISABLED;
import static androidx.core.content.UnusedAppRestrictionsConstants.ERROR;
import static androidx.core.content.UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.os.UserManagerCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Helper for accessing features in {@link PackageManager}.
 */
public final class PackageManagerCompat {
    private PackageManagerCompat() {
        /* Hide constructor */
    }

    @RestrictTo(LIBRARY)
    public static final String LOG_TAG = "PackageManagerCompat";

    /**
     * Activity action: creates an intent to redirect the user to UI to turn on/off their
     * permission revocation settings.
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_PERMISSION_REVOCATION_SETTINGS =
            "android.intent.action.AUTO_REVOKE_PERMISSIONS";

    /**
     * The status of Unused App Restrictions for this app.
     */
    @IntDef({ERROR, FEATURE_NOT_AVAILABLE, DISABLED, API_30_BACKPORT, API_30, API_31})
    @Retention(SOURCE)
    @RestrictTo(LIBRARY)
    public @interface UnusedAppRestrictionsStatus {
    }

    /**
     * Returns the status of Unused App Restrictions for the current application.
     * In other words, whether the features are available and if so, enabled for the application.
     *
     * The returned value is a ListenableFuture with an Integer corresponding to a value in
     * {@link UnusedAppRestrictionsConstants}.
     *
     * The possible values are as follows:
     * <ul>
     *     <li>{@link UnusedAppRestrictionsConstants#ERROR}: an error occurred when fetching
     *     the availability and status of Unused App Restrictions. Check the logs for
     *     the reason (e.g. if the app's target SDK version < 30 or the user is in locked device
     *     boot mode).</li>
     *     <li>{@link UnusedAppRestrictionsConstants#FEATURE_NOT_AVAILABLE}: there are no
     *     available Unused App Restrictions for this app.</li>
     *     <li>{@link UnusedAppRestrictionsConstants#DISABLED}: any available Unused App
     *     Restrictions on the device are disabled for this app.</li>
     *     <li>{@link UnusedAppRestrictionsConstants#API_30_BACKPORT}: Unused App Restrictions
     *     introduced by Android API 30, and since made available on earlier (API 23-29) devices
     *     are enabled for this app:
     *     <a href="https://developer.android.com/training/permissions/requesting#auto-reset-permissions-unused-apps"
     *     >permission auto-reset</a>.</li>
     *     <li>{@link UnusedAppRestrictionsConstants#API_30}: Unused App Restrictions introduced
     *     by Android API 30 are enabled for this app:
     *     <a href="https://developer.android.com/training/permissions/requesting#auto-reset-permissions-unused-apps"
     *     >permission auto-reset</a>.</li>
     *     <li>{@link UnusedAppRestrictionsConstants#API_31}: Unused App Restrictions introduced
     *     by Android API 31 are enabled for this app:
     *     <a href="https://developer.android.com/training/permissions/requesting#auto-reset-permissions-unused-apps"
     *     >permission auto-reset</a> and
     *     <a href="https://developer.android.com/about/versions/12/behavior-changes-12#app-hibernation"
     *     >app hibernation</a>.</li>
     * </ul>
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 31 and above, if {@link PackageManager#isAutoRevokeWhitelisted()} is true, this API
     * will return {@link UnusedAppRestrictionsConstants#DISABLED}. Else, it will return
     * {@link UnusedAppRestrictionsConstants#API_31}.</li>
     * <li>SDK 30, if {@link PackageManager#isAutoRevokeWhitelisted()} is true, this API will return
     * {@link UnusedAppRestrictionsConstants#DISABLED}. Else, it will return
     * {@link UnusedAppRestrictionsConstants#API_30}.</li>
     * <li>SDK 23 through 29, if there exists an app with the Verifier role that can resolve the
     * {@code Intent.ACTION_AUTO_REVOKE_PERMISSIONS} action, then this API will return
     * {@link UnusedAppRestrictionsConstants#API_30_BACKPORT} if Unused App Restrictions are
     * enabled and {@link UnusedAppRestrictionsConstants#DISABLED} if disabled. Else, it will
     * return {@link UnusedAppRestrictionsConstants#FEATURE_NOT_AVAILABLE}.
     * <li>SDK 22 and below, this method always returns
     * {@link UnusedAppRestrictionsConstants#FEATURE_NOT_AVAILABLE} as runtime permissions did
     * not exist yet.
     * </ul>
     */
    public static @NonNull ListenableFuture<Integer> getUnusedAppRestrictionsStatus(
            @NonNull Context context) {
        ResolvableFuture<Integer> resultFuture = ResolvableFuture.create();
        // If the user is in locked direct boot mode, return error as we cannot access the
        // unused app restriction settings.
        if (!UserManagerCompat.isUserUnlocked(context)) {
            resultFuture.set(ERROR);
            Log.e(LOG_TAG, "User is in locked direct boot mode");
            return resultFuture;
        }

        if (!areUnusedAppRestrictionsAvailable(context.getPackageManager())) {
            resultFuture.set(FEATURE_NOT_AVAILABLE);
            return resultFuture;
        }

        int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;

        if (targetSdkVersion < Build.VERSION_CODES.R) {
            resultFuture.set(ERROR);
            Log.e(LOG_TAG, "Target SDK version below API 30");
            return resultFuture;
        }

        // TODO: replace with VERSION_CODES.S once it's defined
        if (Build.VERSION.SDK_INT >= 31) {
            if (Api30Impl.areUnusedAppRestrictionsEnabled(context)) {
                // API 31 unused app restrictions are only available for apps targeting API 31+.
                // For apps targeting API 30-, API 30 unused app restrictions will be used instead.
                resultFuture.set(targetSdkVersion >= 31 ? API_31 : API_30);
            } else {
                resultFuture.set(DISABLED);
            }
            return resultFuture;
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            resultFuture.set(
                    Api30Impl.areUnusedAppRestrictionsEnabled(context)
                            ? API_30
                            : DISABLED);
            return resultFuture;
        }

        UnusedAppRestrictionsBackportServiceConnection backportServiceConnection =
                new UnusedAppRestrictionsBackportServiceConnection(context);

        // Keep the connection object alive until the async operation completes, and then
        // disconnect it.
        resultFuture.addListener(
                backportServiceConnection::disconnectFromService,
                Executors.newSingleThreadExecutor());

        // Start binding the service and fetch the result
        backportServiceConnection.connectAndFetchResult(resultFuture);

        return resultFuture;
    }

    /**
     * Returns whether any Unused App Restrictions are available on the device.
     *
     */
    @RestrictTo(LIBRARY)
    public static boolean areUnusedAppRestrictionsAvailable(
            @NonNull PackageManager packageManager) {
        boolean restrictionsBuiltIntoOs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
        boolean hasBackportFeature = getPermissionRevocationVerifierApp(packageManager) != null;

        return restrictionsBuiltIntoOs || hasBackportFeature;
    }

    /**
     * Returns the package name of the one and only Verifier on the device that can support
     * permission revocation. If none exist, this will return {@code null}. Likewise, if multiple
     * Verifiers exist, this method will return the first Verifier's package name.
     *
     */
    @RestrictTo(LIBRARY)
    @SuppressWarnings("deprecation")
    public static @Nullable String getPermissionRevocationVerifierApp(
            @NonNull PackageManager packageManager) {
        Intent permissionRevocationSettingsIntent =
                new Intent(ACTION_PERMISSION_REVOCATION_SETTINGS)
                        .setData(Uri.fromParts(
                                "package", "com.example", /* fragment= */ null));
        List<ResolveInfo> intentResolvers =
                packageManager.queryIntentActivities(
                        permissionRevocationSettingsIntent, /* flags= */ 0);

        String verifierPackageName = null;

        for (ResolveInfo intentResolver: intentResolvers) {
            String packageName = intentResolver.activityInfo.packageName;
            if (packageManager.checkPermission("android.permission.PACKAGE_VERIFICATION_AGENT",
                    packageName) != PackageManager.PERMISSION_GRANTED) {
                continue;
            }

            if (verifierPackageName != null) {
                // This shouldn't happen, but we fail gracefully nonetheless and avoid throwing an
                // exception, instead returning the first package name with the Verifier role
                // that we found.
                return verifierPackageName;
            }
            verifierPackageName = packageName;
        }

        return verifierPackageName;
    }

    /**
     * We create this static class to avoid Class Verification Failures from referencing a method
     * only added in Android R.
     *
     * <p>Gating references on SDK checks does not address class verification failures, hence the
     * need for this inner class.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        private Api30Impl() {}
        static boolean areUnusedAppRestrictionsEnabled(@NonNull Context context) {
            // If the app is allowlisted, that means that it is exempt from Unused App Restrictions,
            // and thus the features are _disabled_.
            return !context.getPackageManager().isAutoRevokeWhitelisted();
        }
    }
}
