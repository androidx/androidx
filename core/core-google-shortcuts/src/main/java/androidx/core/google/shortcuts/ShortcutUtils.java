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
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.mac.HmacKeyManager;
import com.google.crypto.tink.mac.MacConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

/**
 * Utility methods and constants used by the Google shortcuts library.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
class ShortcutUtils {
    public static final String ID_KEY = "id";
    public static final String SHORTCUT_URL_KEY = "shortcutUrl";
    public static final String CAPABILITY_PARAM_SEPARATOR = "/";
    public static final String SHORTCUT_TAG_KEY = "shortcutTag";
    public static final String SHORTCUT_LISTENER_INTENT_FILTER_ACTION = "androidx.core.content.pm"
            + ".SHORTCUT_LISTENER";

    private static final String TAG = "ShortcutUtils";
    private static final String APP_ACTION_CAPABILITY_PREFIX = "actions.intent.";
    private static final String MASTER_KEY_ALIAS = "core-google-shortcuts.MASTER_KEY";
    private static final String TINK_KEYSET_NAME = "core-google-shortcuts.TINK_KEYSET";
    private static final String PREF_FILE_NAME = "core-google-shortcuts.PREF_FILE_NAME";

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
        intent.setAction(SHORTCUT_LISTENER_INTENT_FILTER_ACTION);
        intent.putExtra(ID_KEY, shortcutId);

        return intent.toUri(Intent.URI_INTENT_SCHEME);
    }

    /**
     * Generate value for Indexable shortcutUrl field. This field will be used by Google
     * Assistant to open shortcuts.
     *
     * @param context the app's context.
     * @param shortcutIntent the intent that the shortcut opens.
     * @param keysetHandle the keyset handle used to sign the shortcut.
     * @return the shortcut url wrapped inside an intent that opens the Trampoline Activity if
     * the shortcut can be signed. Otherwise return just the shortcut url.
     */
    public static String getIndexableShortcutUrl(@NonNull Context context,
            @NonNull Intent shortcutIntent, @Nullable KeysetHandle keysetHandle) {
        String shortcutUrl = shortcutIntent.toUri(Intent.URI_INTENT_SCHEME);
        if (keysetHandle == null) {
            // If keyset handle is null, then create the shortcut without using the Trampoline
            // Activity. This means that only shortcuts with exported intent will work. Those with
            // non-exported intents cannot be opened without the Trampoline Activity since the
            // caller will invoke the shortcut url directly.
            return shortcutUrl;
        }

        try {
            // Compute the tag for the shortcut url. Only the app who has the keyset used to
            // create the tag can verify it. This ensures that:
            // 1. only the app that created the shortcut can open the shortcut
            // 2. only the shortcut that was indexed using this library can be opened. You cannot
            // use the Trampoline Activity to open arbitrary shortcuts.
            Mac mac = keysetHandle.getPrimitive(Mac.class);
            byte[] tag = mac.computeMac(shortcutUrl.getBytes(Charset.forName("UTF-8")));
            String tagString = Base64.encodeToString(tag, Base64.DEFAULT);

            Intent trampolineIntent = new Intent(context, TrampolineActivity.class);
            trampolineIntent.setPackage(context.getPackageName());
            trampolineIntent.setAction(SHORTCUT_LISTENER_INTENT_FILTER_ACTION);
            trampolineIntent.putExtra(SHORTCUT_URL_KEY, shortcutUrl);
            trampolineIntent.putExtra(SHORTCUT_TAG_KEY, tagString);

            return trampolineIntent.toUri(Intent.URI_INTENT_SCHEME);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "failed to generate tag for shortcut.", e);
            // Could not sign the shortcut, so just return the raw url without Trampoline Activity.
            return shortcutUrl;
        }
    }

    public static boolean isAppActionCapability(@NonNull final String capability) {
        return capability.startsWith(APP_ACTION_CAPABILITY_PREFIX);
    }

    @Nullable
    public static KeysetHandle getOrCreateShortcutKeysetHandle(@NonNull Context context) {
        try {
            MacConfig.register();

            // Android keystore and shared pref are not shared across apps.
            return new AndroidKeysetManager.Builder()
                    .withSharedPref(context, TINK_KEYSET_NAME, PREF_FILE_NAME)
                    .withKeyTemplate(HmacKeyManager.hmacSha256HalfDigestTemplate())
                    .withMasterKeyUri(String.format("android-keystore://%s", MASTER_KEY_ALIAS))
                    .build()
                    .getKeysetHandle();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "could not get or create keyset handle.", e);
            return null;
        }
    }

    private ShortcutUtils() {}
}
