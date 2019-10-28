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

package androidx.browser.trusted;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Holds an {@link Intent} and other data necessary to start a Trusted Web Activity.
 */
public final class TrustedWebActivityIntent {
    @NonNull
    private final Intent mIntent;

    @NonNull
    private final List<Uri> mSharedFileUris;

    TrustedWebActivityIntent(@NonNull Intent intent, @NonNull List<Uri> sharedFileUris) {
        mIntent = intent;
        mSharedFileUris = sharedFileUris;
    }

    /**
     * Launches a Trusted Web Activity.
     */
    public void launchTrustedWebActivity(@NonNull Context context) {
        grantUriPermissionToProvider(context);
        ContextCompat.startActivity(context, mIntent, null);
    }

    private void grantUriPermissionToProvider(Context context) {
        for (Uri uri : mSharedFileUris) {
            context.grantUriPermission(mIntent.getPackage(), uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    /**
     * Returns the held {@link Intent}. For launching a Trusted Web Activity prefer using
     * {@link #launchTrustedWebActivity}.
     */
    @NonNull
    public Intent getIntent() {
        return mIntent;
    }
}
