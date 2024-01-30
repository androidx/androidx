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
import static androidx.core.content.PackageManagerCompat.areUnusedAppRestrictionsAvailable;
import static androidx.core.content.PackageManagerCompat.getPermissionRevocationVerifierApp;
import static androidx.core.util.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

/**
 * Helper for accessing features in {@link Intent}.
 */
public final class IntentCompat {
    private IntentCompat() {
        /* Hide constructor */
    }

    /**
     * Activity Action: Creates a reminder.
     * <p>Input: {@link Intent#EXTRA_TITLE} The title of the
     * reminder that will be shown to the user.
     * {@link Intent#EXTRA_TEXT} The reminder text that will be
     * shown to the user. The intent should at least specify a title or a text.
     * {@link #EXTRA_TIME} The time when the reminder will
     * be shown to the user. The time is specified in milliseconds since the
     * Epoch (optional).
     * </p>
     * <p>Output: Nothing.</p>
     *
     * @see Intent#EXTRA_TITLE
     * @see Intent#EXTRA_TEXT
     * @see #EXTRA_TIME
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_CREATE_REMINDER = "android.intent.action.CREATE_REMINDER";

    /**
     * A constant String that is associated with the Intent, used with
     * {@link Intent#ACTION_SEND} to supply an alternative to
     * {@link Intent#EXTRA_TEXT}
     * as HTML formatted text.  Note that you <em>must</em> also supply
     * {@link Intent#EXTRA_TEXT}.
     */
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";

    /**
     * Used as a boolean extra field in {@link Intent#ACTION_VIEW} intents to
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
            return Api15Impl.makeMainSelectorActivity(selectorAction, selectorCategory);
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
     * <p>Note: developers must first call
     * {@link PackageManagerCompat#getUnusedAppRestrictionsStatus(Context)}
     * to make sure that unused app restriction features are available on the device before
     * attempting to create an intent using this method. Likewise, the returned intent must be sent
     * using {@link Activity#startActivityForResult}, _not_ {@link Activity#startActivity}.
     *
     * <p>Any return value of {@link PackageManagerCompat#getUnusedAppRestrictionsStatus(Context)}
     * besides {@link UnusedAppRestrictionsConstants#FEATURE_NOT_AVAILABLE}
     * indicates that at least one unused app restriction feature is available on the device. If
     * the return value _is_ {@link UnusedAppRestrictionsConstants#FEATURE_NOT_AVAILABLE}, this
     * method will throw an {@link UnsupportedOperationException}.
     *
     * <p>If the return value is {@link UnusedAppRestrictionsConstants#ERROR}, then there was an
     * issue when fetching whether the unused app restriction features on the device are enabled
     * for this application. However, this method will still return an intent to redirect the user.
     *
     * <p>Compatibility behavior:
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
        if (!areUnusedAppRestrictionsAvailable(context.getPackageManager())) {
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
            // Only allow apps with the Verifier role to resolve the permission revocation intent.
            String verifierPackageName =
                    getPermissionRevocationVerifierApp(context.getPackageManager());
            // The Verifier package name shouldn't be null since we've already checked that there
            // exists a Verifier on the device, but nonetheless we double-check here.
            return permissionRevocationSettingsIntent
                    .setPackage(checkNotNull(verifierPackageName));
        }
    }

    /**
     * Retrieve extended data from the intent.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and later, this method matches platform behavior.
     *     <li>SDK 33 and below, the object type is checked after deserialization.
     * </ul>
     *
     * @param in The intent to retrieve from.
     * @param name The name of the desired item.
     * @param clazz The type of the object expected.
     *
     * @return the value of an item previously added with putExtra(),
     * or null if no Parcelable value was found.
     *
     * @see Intent#putExtra(String, Parcelable)
     */
    @Nullable
    @SuppressWarnings({"deprecation", "unchecked"})
    public static <T> T getParcelableExtra(@NonNull Intent in, @Nullable String name,
            @NonNull Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelableExtra(in, name, clazz);
        } else {
            T extra = in.getParcelableExtra(name);
            return clazz.isInstance(extra) ? extra : null;
        }
    }

    /**
     * Retrieve extended data from the intent.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and later, this method matches platform behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The intent to retrieve from.
     * @param name The name of the desired item.
     * @param clazz The type of the items inside the array. This is only verified when unparceling.
     *
     * @return the value of an item previously added with putExtra(),
     * or null if no Parcelable[] value was found.
     *
     * @see Intent#putExtra(String, Parcelable[])
     */
    @Nullable
    @SuppressWarnings({"deprecation"})
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    public static Parcelable[] getParcelableArrayExtra(@NonNull Intent in, @Nullable String name,
            @NonNull Class<? extends Parcelable> clazz) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelableArrayExtra(in, name, clazz);
        } else {
            return in.getParcelableArrayExtra(name);
        }
    }

    /**
     * Retrieve extended data from the intent.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and later, this method matches platform behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The intent to retrieve from.
     * @param name The name of the desired item.
     * @param clazz The type of the items inside the array list. This is only verified when
     *     unparceling.
     *
     * @return the value of an item previously added with
     * putParcelableArrayListExtra(), or null if no
     * ArrayList<Parcelable> value was found.
     *
     * @see Intent#putParcelableArrayListExtra(String, ArrayList)
     */
    @Nullable
    @SuppressWarnings({"deprecation", "unchecked"})
    @SuppressLint({"ConcreteCollection", "NullableCollection"})
    public static <T> ArrayList<T> getParcelableArrayListExtra(
            @NonNull Intent in, @Nullable String name, @NonNull Class<? extends T> clazz) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelableArrayListExtra(in, name, clazz);
        } else {
            return (ArrayList<T>) in.getParcelableArrayListExtra(name);
        }
    }

    @RequiresApi(15)
    static class Api15Impl {
        private Api15Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Intent makeMainSelectorActivity(String selectorAction, String selectorCategory) {
            return Intent.makeMainSelectorActivity(selectorAction, selectorCategory);
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is non-instantiable.
        }

        @DoNotInline
        static <T> T getParcelableExtra(@NonNull Intent in, @Nullable String name,
                @NonNull Class<T> clazz) {
            return in.getParcelableExtra(name, clazz);
        }

        @DoNotInline
        static <T> T[] getParcelableArrayExtra(@NonNull Intent in, @Nullable String name,
                @NonNull Class<T> clazz) {
            return in.getParcelableArrayExtra(name, clazz);
        }

        @DoNotInline
        static <T> ArrayList<T> getParcelableArrayListExtra(@NonNull Intent in,
                @Nullable String name, @NonNull Class<? extends T> clazz) {
            return in.getParcelableArrayListExtra(name, clazz);
        }
    }
}
