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
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_TAG_KEY;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_URL_KEY;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;


/**
 * Activity used to receives shortcut intents sent from Google, extracts its shortcut url, and
 * launches it in the scope of the app.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class TrampolineActivity extends Activity {
    private static final String TAG = "TrampolineActivity";

    private static volatile KeysetHandle sKeysetHandle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (sKeysetHandle == null) {
            sKeysetHandle = ShortcutUtils.getOrCreateShortcutKeysetHandle(this);
        }

        if (sKeysetHandle != null) {
            Intent intent = getIntent();
            String shortcutUrl = intent.getStringExtra(SHORTCUT_URL_KEY);
            String tag = intent.getStringExtra(SHORTCUT_TAG_KEY);

            if (shortcutUrl != null && tag != null) {
                try {
                    Mac mac = sKeysetHandle.getPrimitive(Mac.class);
                    // Will throw GeneralSecurityException when verifyMac fails.
                    mac.verifyMac(Base64.decode(tag, Base64.DEFAULT),
                            shortcutUrl.getBytes(Charset.forName("UTF-8")));

                    Intent shortcutIntent = Intent.parseUri(shortcutUrl, Intent.URI_INTENT_SCHEME);
                    startActivity(shortcutIntent);
                } catch (GeneralSecurityException | URISyntaxException e) {
                    Log.w(TAG, "failed to open shortcut url", e);
                }
            }
        }

        finish();
    }
}
