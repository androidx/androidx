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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.util.List;

/**
 * Helper for accessing features in {@link PackageManager}.
 */
public final class PackageManagerCompat {
    private PackageManagerCompat() {
        /* Hide constructor */
    }

    /**
     * Activity action: creates an intent to redirect the user to UI to turn on/off their
     * permission revocation settings.
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_PERMISSION_REVOCATION_SETTINGS =
            "android.intent.action.AUTO_REVOKE_PERMISSIONS";

    /** The status of Unused App Restrictions features is unknown for this app. */
    public static final int UNUSED_APP_RESTRICTION_STATUS_UNKNOWN = 0;

    /** There are no available Unused App Restrictions features for this app. */
    public static final int UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE = 1;

    /**
     * Permission revocation is enabled for this app (i.e. permissions will be automatically
     * reset if the app is unused).
     *
     * Note: this also means that app hibernation is not available for this app.
     */
    public static final int PERMISSION_REVOCATION_ENABLED = 2;

    /**
     * Permission revocation is disabled for this app (i.e. this app is exempt from having
     * its permissions automatically removed).
     *
     * Note: this also means that app hibernation is not available for this app.
     */
    public static final int PERMISSION_REVOCATION_DISABLED = 3;

    /**
     * App hibernation is enabled for this app (i.e. this app will be hibernated and have its
     * permissions revoked if the app is unused).
     *
     * Note: this also means that permission revocation is enabled for this app.
     */
    public static final int APP_HIBERNATION_ENABLED = 4;

    /**
     * App hibernation is disabled for this app (i.e. this app is exempt from being hibernated).
     *
     * Note: this also means that permission revocation is disabled for this app.
     */
    public static final int APP_HIBERNATION_DISABLED = 5;

    /**
     * The status of Unused App Restrictions features for this app.
     * @hide
     */
    @IntDef({UNUSED_APP_RESTRICTION_STATUS_UNKNOWN, UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE,
            PERMISSION_REVOCATION_ENABLED, PERMISSION_REVOCATION_DISABLED,
            APP_HIBERNATION_ENABLED, APP_HIBERNATION_DISABLED})
    @Retention(SOURCE)
    @RestrictTo(LIBRARY)
    public @interface UnusedAppRestrictionsStatus {
    }

    /**
     * Returns the status of Unused App Restriction features for the current application, i.e.
     * whether the features are available and if so, enabled for the application.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 31 and above, if {@link PackageManager#isAutoRevokeWhitelisted()} is true, this
     * will return {@link #APP_HIBERNATION_ENABLED}. Else, it will return
     * {@link #APP_HIBERNATION_DISABLED}.</li>
     * <li>SDK 30, if {@link PackageManager#isAutoRevokeWhitelisted()} is true, this will return
     * {@link #PERMISSION_REVOCATION_ENABLED}. Else, it will return
     * {@link #PERMISSION_REVOCATION_DISABLED}.</li>
     * <li>SDK 23 through 29, if there exists an app with the Verifier role that can resolve the
     * {@code Intent.ACTION_AUTO_REVOKE_PERMISSIONS} action.
     * <li>SDK 22 and below, this method always returns
     * {@link #UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE} as runtime permissions did not exist
     * yet.
     * </ul>
     */
    public static @UnusedAppRestrictionsStatus int getUnusedAppRestrictionsStatus(
            @NonNull Context context) {
        // Return false if the Android OS version is before M, because Android M introduced runtime
        // permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE;
        }

        // TODO: replace with VERSION_CODES.S once it's defined
        if (Build.VERSION.SDK_INT >= 31) {
            return Api30Impl.areUnusedAppRestrictionsEnabled(context)
                    ? APP_HIBERNATION_ENABLED
                    : APP_HIBERNATION_DISABLED;
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            return Api30Impl.areUnusedAppRestrictionsEnabled(context)
                    ? PERMISSION_REVOCATION_ENABLED
                    : PERMISSION_REVOCATION_DISABLED;
        }

        // Else, check for an app with the verifier role that can resolve the intent
        String verifierPackageName = getVerifierRolePackageName(context.getPackageManager());
        // Check that we were able to get the one Verifier's package name. If no Verifier or
        // more than one Verifier exists on the device, unused app restrictions are not available
        // on the device.
        return (verifierPackageName == null)
                ? UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE
                // TODO(b/177234481): Implement the backport behavior of this API
                : UNUSED_APP_RESTRICTION_STATUS_UNKNOWN;
    }

    /**
     * Returns the package name of the one and only Verifier on the device. If none exist, this
     * will return {@code null}. Likewise, if multiple Verifiers exist, this method will return
     * the first Verifier's package name.
     */
    @Nullable
    static String getVerifierRolePackageName(@NonNull PackageManager packageManager) {
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
            // If the app is allowlisted, that means that it is exempt from unused app restriction
            // features, and thus the features are _disabled_.
            return !context.getPackageManager().isAutoRevokeWhitelisted();
        }
    }
}
