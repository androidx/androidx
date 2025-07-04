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

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Holds an {@link Intent} and other data necessary to start a Trusted Web Activity.
 */
public final class TrustedWebActivityIntent {
    private final @NonNull Intent mIntent;

    private final @NonNull List<Uri> mSharedFileUris;
    private final @NonNull List<Uri> mFileHandlingUris;

    TrustedWebActivityIntent(
            @NonNull Intent intent,
            @NonNull List<Uri> sharedFileUris,
            @NonNull List<Uri> fileHandlingUris) {
        mIntent = intent;
        mSharedFileUris = sharedFileUris;
        mFileHandlingUris = fileHandlingUris;
    }

    /**
     * Used by Protocol Handlers to provide context for the browser. When a custom data scheme
     * link (e.g. web+coffee://latte) is being processed by a WebAPK/TWA, it will get replaced
     * with an actual http/https location (e.g. https://coffee.com/?type=latte) and that URL gets
     * sent to the browser. This extra will then store the original link in case the browser needs
     * different logic for Protocol Handlers and regular links.
     *
     * {@see https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest/Reference/protocol_handlers}
     *
     * @return The original URL before being processed by a Protocol Handler, or null if this was
     *         never a custom data scheme link.
     */
    public @Nullable Uri getOriginalLaunchUrl() {
        return IntentCompat.getParcelableExtra(getIntent(),
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL, Uri.class);
    }

    public @Nullable FileHandlingData getFileHandlingData() {
        return FileHandlingData.fromBundle(
            getIntent().getBundleExtra(TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA));
    }

    /**
     * Used for Launch Handler API to provide client mode to a browser.
     *
     * {@see https://developer.mozilla.org/en-US/docs/Web/API/Launch_Handler_API}
     *
     * @return An integer that represents Launch Handler API client mode.
     */
    public @LaunchHandlerClientMode.ClientMode int getLaunchHandlerClientMode() {
        return getIntent().getIntExtra(
                TrustedWebActivityIntentBuilder.EXTRA_LAUNCH_HANDLER_CLIENT_MODE,
                LaunchHandlerClientMode.AUTO);
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
        for (Uri uri : mFileHandlingUris) {
            context.grantUriPermission(mIntent.getPackage(), uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    /**
     * Returns the held {@link Intent}. For launching a Trusted Web Activity prefer using
     * {@link #launchTrustedWebActivity}.
     */
    public @NonNull Intent getIntent() {
        return mIntent;
    }
}
