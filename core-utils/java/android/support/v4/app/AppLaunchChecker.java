/*
 * Copyright (C) 2016 The Android Open Source Project
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


package android.support.v4.app;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v4.content.SharedPreferencesCompat;

/**
 * This class provides APIs for determining how an app has been launched.
 * This can be useful if you want to confirm that a user has launched your
 * app through its front door activity from their launcher/home screen, rather
 * than just if the app has been opened in the past in order to view a link,
 * open a document or perform some other service for other apps on the device.
 */
public class AppLaunchChecker {
    private static final String SHARED_PREFS_NAME = "android.support.AppLaunchChecker";
    private static final String KEY_STARTED_FROM_LAUNCHER = "startedFromLauncher";

    /**
     * Checks if this app has been launched by the user from their launcher or home screen
     * since it was installed.
     *
     * <p>To track this state properly you must call {@link #onActivityCreate(Activity)}
     * in your launcher activity's {@link Activity#onCreate(Bundle)} method.</p>
     *
     * @param context Context to check
     * @return true if this app has been started by the user from the launcher at least once
     */
    public static boolean hasStartedFromLauncher(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, 0)
                .getBoolean(KEY_STARTED_FROM_LAUNCHER, false);
    }

    /**
     * Records the parameters of an activity's launch for later use by the other
     * methods available on this class.
     *
     * <p>Your app should call this method in your launcher activity's
     * {@link Activity#onCreate(Bundle)} method to track launch state.
     * If the app targets API 23 (Android 6.0 Marshmallow) or later, this state will be
     * eligible for full data backup and may be restored to the user's device automatically.</p>     *
     *
     * @param activity the Activity currently running onCreate
     */
    public static void onActivityCreate(Activity activity) {
        final SharedPreferences sp = activity.getSharedPreferences(SHARED_PREFS_NAME, 0);
        if (sp.getBoolean(KEY_STARTED_FROM_LAUNCHER, false)) {
            return;
        }

        final Intent launchIntent = activity.getIntent();
        if (launchIntent == null) {
            return;
        }

        if (Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && (launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)
                || launchIntent.hasCategory(IntentCompat.CATEGORY_LEANBACK_LAUNCHER))) {
            SharedPreferencesCompat.EditorCompat.getInstance().apply(
                    sp.edit().putBoolean(KEY_STARTED_FROM_LAUNCHER, true));
        }
    }
}
