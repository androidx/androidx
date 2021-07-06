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

import static androidx.core.content.PackageManagerCompat.ACTION_PERMISSION_REVOCATION_SETTINGS;
import static androidx.core.content.PackageManagerCompat.UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE;
import static androidx.core.content.PackageManagerCompat.getUnusedAppRestrictionsStatus;
import static androidx.core.content.PackageManagerCompat.getVerifierRolePackageName;
import static androidx.core.util.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

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
     * Note: developers must first call
     * {@link PackageManagerCompat#getUnusedAppRestrictionsStatus(Context)} to make
     * sure that unused app restriction features are available on the device before attempting to
     * create an intent using this method. Any return value of this method besides
     * {@link PackageManagerCompat#UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE} indicates that at
     * least one unused app restriction feature is available on the device. If the return value _is_
     * {@link PackageManagerCompat#UNUSED_APP_RESTRICTION_FEATURE_NOT_AVAILABLE}, this method will
     * throw an {@link UnsupportedOperationException}.
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

        Intent permissionRevocationSettingsIntent =
                new Intent(ACTION_PERMISSION_REVOCATION_SETTINGS)
                        .setData(Uri.fromParts(
                                "package", packageName, /* fragment= */ null));

        // If the OS version is R, then no need to add any other data or flags, since we're
        // relying on the Android R system feature.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return permissionRevocationSettingsIntent;
        } else {
            // Only allow apps with the Verifier role to resolve the intent.
            String verifierPackageName = getVerifierRolePackageName(context.getPackageManager());
            // The Verifier package name shouldn't be null since we've already checked that there
            // exists a Verifier on the device, but nonetheless we double-check here.
            return permissionRevocationSettingsIntent
                    .setPackage(checkNotNull(verifierPackageName));
        }
    }
}
