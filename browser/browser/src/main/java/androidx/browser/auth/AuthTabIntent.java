/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.auth;

import static android.app.Activity.RESULT_OK;

import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Class holding an {@link Intent} and other data necessary to start an Auth Tab Activity.
 *
 * <p> After creating an instance of this class, you can call {@link #launch} to present the user
 * with the authentication page. You should create an {@link ActivityResultLauncher} using
 * {@link #registerActivityResultLauncher} unconditionally before every fragment or activity
 * creation. Once the user completes the authentication flow, or cancels it, the
 * {@link ActivityResultCallback} provided when creating the {@link ActivityResultLauncher} will be
 * called with the result. The returned {@link Uri} will be null if the user closes the Auth Tab
 * without completing the authentication.
 *
 * <p>Note: The constants below are public for the browser implementation's benefit. You are
 * strongly encouraged to use {@link AuthTabIntent.Builder}.
 */
@ExperimentalAuthTab
public class AuthTabIntent {
    /** Boolean extra that triggers an Auth Tab launch. */
    public static final String EXTRA_LAUNCH_AUTH_TAB =
            "androidx.browser.auth.extra.LAUNCH_AUTH_TAB";
    /** String extra that determines the redirect scheme. */
    public static final String EXTRA_REDIRECT_SCHEME =
            "androidx.browser.auth.extra.REDIRECT_SCHEME";

    private final Intent mIntent;

    /**
     * Launches an Auth Tab Activity. Should be used for flows that result in a redirect with a
     * custom scheme.
     *
     * @param launcher       The {@link ActivityResultLauncher} used to launch the Auth Tab. Use
     *                       {@link #registerActivityResultLauncher} to create this. See the class
     *                       documentation for more details.
     * @param url            The url to load in the Auth Tab.
     * @param redirectScheme The scheme of the resulting redirect.
     */
    public void launch(@NonNull ActivityResultLauncher<Intent> launcher, @NonNull Uri url,
            @NonNull String redirectScheme) {
        mIntent.setData(url);
        mIntent.putExtra(EXTRA_REDIRECT_SCHEME, redirectScheme);
        launcher.launch(mIntent);
    }

    private AuthTabIntent(@NonNull Intent intent) {
        mIntent = intent;
    }

    /**
     * Builder class for {@link AuthTabIntent} objects.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(Intent.ACTION_VIEW);

        public Builder() {
        }

        /**
         * Combines all the options that have been set and returns a new {@link AuthTabIntent}
         * object.
         */
        @NonNull
        public AuthTabIntent build() {
            mIntent.putExtra(EXTRA_LAUNCH_AUTH_TAB, true);

            // Put a null EXTRA_SESSION as a fallback so that this is interpreted as a Custom Tab
            // intent by browser implementations that don't support Auth Tab.
            Bundle bundle = new Bundle();
            bundle.putBinder(EXTRA_SESSION, null);
            mIntent.putExtras(bundle);

            return new AuthTabIntent(mIntent);
        }
    }

    /**
     * Registers a request to launch an Auth Tab and returns an {@link ActivityResultLauncher} that
     * can be used to launch it. Should be called unconditionally before the fragment or activity is
     * created.
     *
     * @param caller   An {@link ActivityResultCaller}, e.g. a
     *                 {@link androidx.activity.ComponentActivity} or a
     *                 {@link androidx.fragment.app.Fragment}.
     * @param callback An {@link ActivityResultCallback} to be called with the auth result.
     * @return An {@link ActivityResultLauncher} to be passed to {@link #launch}.
     */
    @NonNull
    public static ActivityResultLauncher<Intent> registerActivityResultLauncher(
            @NonNull ActivityResultCaller caller, @NonNull ActivityResultCallback<Uri> callback) {
        return caller.registerForActivityResult(new AuthenticateUserResultContract(), callback);
    }

    private static class AuthenticateUserResultContract extends
            ActivityResultContract<Intent, Uri> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Intent input) {
            return input;
        }

        @NonNull
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            if (resultCode == RESULT_OK && intent != null) {
                return Objects.requireNonNullElse(intent.getData(), Uri.EMPTY);
            }
            return Uri.EMPTY;
        }
    }
}
