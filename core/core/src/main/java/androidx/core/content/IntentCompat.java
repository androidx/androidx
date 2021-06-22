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

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.util.Preconditions.checkNotNull;

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
 * Helper for accessing features in {@link android.content.Intent}.
 */
public final class IntentCompat {
    private IntentCompat() {
        /* Hide constructor */
    }

    /**
     * Activity Action: Creates a reminder.
     * <p>Input: {@link android.content.Intent#EXTRA_TITLE} The title of the
     * reminder that will be shown to the user.
     * {@link android.content.Intent#EXTRA_TEXT} The reminder text that will be
     * shown to the user. The intent should at least specify a title or a text.
     * {@link #EXTRA_TIME} The time when the reminder will
     * be shown to the user. The time is specified in milliseconds since the
     * Epoch (optional).
     * </p>
     * <p>Output: Nothing.</p>
     *
     * @see android.content.Intent#EXTRA_TITLE
     * @see android.content.Intent#EXTRA_TEXT
     * @see #EXTRA_TIME
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_CREATE_REMINDER = "android.intent.action.CREATE_REMINDER";

    /**
     * Activity action: creates an intent to redirect the user to UI to turn on/off their
     * unused app restriction settings.
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_UNUSED_APP_RESTRICTIONS =
            "android.intent.action.AUTO_REVOKE_PERMISSIONS";

    /**
     * A constant String that is associated with the Intent, used with
     * {@link android.content.Intent#ACTION_SEND} to supply an alternative to
     * {@link android.content.Intent#EXTRA_TEXT}
     * as HTML formatted text.  Note that you <em>must</em> also supply
     * {@link android.content.Intent#EXTRA_TEXT}.
     */
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";

    /**
     * Used as a boolean extra field in {@link android.content.Intent#ACTION_VIEW} intents to
     * indicate that content should immediately be played without any intermediate screens that
     * require additional user input, e.g. a profile selection screen or a details page.
     */
    public static final String EXTRA_START_PLAYBACK = "android.intent.extra.START_PLAYBACK";

    /**
     * Optional extra specifying a time in milliseconds since the Epoch. The value must be
     * non-negative.
     * <p>
     * Type: long
     * </p>
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_TIME = "android.intent.extra.TIME";

    /**
     * Indicates an activity optimized for Leanback mode, and that should
     * be displayed in the Leanback launcher.
     */
    public static final String CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER";

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
     * Make an Intent for the main activity of an application, without
     * specifying a specific activity to run but giving a selector to find
     * the activity.  This results in a final Intent that is structured
     * the same as when the application is launched from
     * Home.  For anything else that wants to launch an application in the
     * same way, it is important that they use an Intent structured the same
     * way, and can use this function to ensure this is the case.
     *
     * <p>The returned Intent has {@link Intent#ACTION_MAIN} as its action, and includes the
     * category {@link Intent#CATEGORY_LAUNCHER}.  This does <em>not</em> have
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} set, though typically you will want
     * to do that through {@link Intent#addFlags(int)} on the returned Intent.
     *
     * @param selectorAction The action name of the Intent's selector.
     * @param selectorCategory The name of a category to add to the Intent's
     * selector.
     * @return Returns a newly created Intent that can be used to launch the
     * activity as a main application entry.
     */
    @NonNull
    public static Intent makeMainSelectorActivity(@NonNull String selectorAction,
            @NonNull String selectorCategory) {
        if (Build.VERSION.SDK_INT >= 15) {
            return Intent.makeMainSelectorActivity(selectorAction, selectorCategory);
        } else {
            // Before api 15 you couldn't set a selector intent.
            // Fall back and just return an intent with the requested action/category,
            // even though it won't be a proper "main" intent.
            Intent intent = new Intent(selectorAction);
            intent.addCategory(selectorCategory);
            return intent;
        }
    }

    /**
     * Make an Intent to redirect the user to UI to manage their unused app restriction settings
     * for a particular app (e.g. permission revocation, app hibernation).
     *
     * Note: developers must first call {@link #getUnusedAppRestrictionsStatus(Context)} to make
     * sure that unused app restriction features are available on the device before attempting to
     * create an intent using this method. Any return value of this method besides
     * {@link #UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE} indicates that at least one
     * unused app restriction feature is available on the device. If the return value _is_
     * {@link #UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE}, this method will throw an
     * {@link UnsupportedOperationException}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 31 and above, this method generates an intent with action {@code Intent
     * .ACTION_APPLICATION_DETAILS_SETTINGS} and {@code packageName} as data.
     * <li>SDK 30, this method generates an intent with action {@code Intent
     * .ACTION_AUTO_REVOKE_PERMISSIONS} and {@code packageName} as data.
     * <li>SDK 23 through 29, this method will generate an intent with action
     * {@link Intent#ACTION_AUTO_REVOKE_PERMISSIONS} and the package as the app with the Verifier
     * role that can resolve the intent.
     * <li>SDK 22 and below, this method will throw an {@link UnsupportedOperationException}
     * </ul>
     *
     * @param context The {@link Context} of the calling application.
     * @param packageName The package name of the calling application.
     *
     * @return Returns a newly created Intent that can be used to launch an activity where users
     * can manage unused app restrictions for a specific app.
     */
    @NonNull
    public static Intent createManageUnusedAppRestrictionsIntent(@NonNull Context context,
            @NonNull String packageName) {
        if (getUnusedAppRestrictionsStatus(context)
                == UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE) {
            throw new UnsupportedOperationException(
                    "Unused App Restriction features are not available on this device");
        }

        // If the OS version is S+, generate the intent using the Application Details Settings
        // intent action to support compatibility with the App Hibernation feature
        // TODO: replace with VERSION_CODES.S once it's defined
        if (Build.VERSION.SDK_INT >= 31) {
            return new Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", packageName, /* fragment= */ null));
        }

        Intent unusedAppRestrictionsIntent =
                new Intent(ACTION_UNUSED_APP_RESTRICTIONS)
                        .setData(Uri.fromParts(
                                "package", packageName, /* fragment= */ null));

        // If the OS version is R, then no need to add any other data or flags, since we're
        // relying on the Android R system feature.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return unusedAppRestrictionsIntent;
        } else {
            // Only allow apps with the Verifier role to resolve the intent.
            String verifierPackageName = getVerifierRolePackageName(context.getPackageManager());
            // The Verifier package name shouldn't be null since we've already checked that there
            // exists a Verifier on the device, but nonetheless we double-check here.
            return unusedAppRestrictionsIntent
                    .setPackage(checkNotNull(verifierPackageName));
        }
    }

    /**
     * Returns the package name of the one and only Verifier on the device. If none exist, this
     * will return {@code null}. Likewise, if multiple Verifiers exist, this method will return
     * the first Verifier's package name.
     */
    @Nullable
    private static String getVerifierRolePackageName(PackageManager packageManager) {
        Intent unusedAppRestrictionsIntent =
                new Intent(ACTION_UNUSED_APP_RESTRICTIONS)
                        .setData(Uri.fromParts(
                                "package", "com.example", /* fragment= */ null));
        List<ResolveInfo> intentResolvers =
                packageManager.queryIntentActivities(unusedAppRestrictionsIntent, /* flags= */ 0);

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
