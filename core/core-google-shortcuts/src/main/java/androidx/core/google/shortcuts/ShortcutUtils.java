/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.google.shortcuts;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utility methods and constants used by the google shortcuts library.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
class ShortcutUtils {
    public static final String SHORTCUT_LABEL_KEY = "shortcutLabel";
    public static final String SHORTCUT_DESCRIPTION_KEY = "shortcutDescription";
    public static final String SHORTCUT_URL_KEY = "shortcutUrl";
    public static final String ID_KEY = "id";
    public static final String CAPABILITY_PARAM_SEPARATOR = "/";

    private static final String APP_ACTION_CAPABILITY_PREFIX = "actions.intent.";

    /**
     * Generate value for Indexable url field. The url field will not be used for anything other
     * than referencing the Indexable object. But since it requires that it's openable by the
     * app, we generate it as an intent that opens the Trampoline Activity.
     *
     * @param context the app's context.
     * @param shortcutId the shortcut id used to generate the url.
     * @return the indexable url.
     */
    public static String getIndexableUrl(@NonNull Context context, @NonNull String shortcutId) {
        Intent intent = new Intent(context, TrampolineActivity.class);
        intent.putExtra(ID_KEY, shortcutId);

        return intent.toUri(Intent.URI_INTENT_SCHEME);
    }

    /**
     * Generate value for Indexable shortcutUrl field. This field will be used by Google
     * Assistant to open shortcuts.
     *
     * @param context the app's context.
     * @param shortcutIntent the intent that the shortcut opens.
     * @return the shortcut url.
     */
    public static String getIndexableShortcutUrl(@NonNull Context context,
            @NonNull Intent shortcutIntent) {
        // TODO (b/182599835): support private shortcut intents by wrapping it inside an intent
        //  that launches the trampoline activity.
        return shortcutIntent.toUri(0);
    }

    public static boolean isAppActionCapability(@NonNull final String capability) {
        return capability.startsWith(APP_ACTION_CAPABILITY_PREFIX);
    }

    private ShortcutUtils() {}
}
