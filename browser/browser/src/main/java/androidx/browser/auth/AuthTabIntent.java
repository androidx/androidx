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

import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holding an {@link Intent} and other data necessary to start an Auth Tab Activity.
 *
 * <p> Create an instance of this class using the {@link Builder} and then call {@link #launch} to
 * present the auth page to the user.
 *
 * <p> Upon completion of the auth flow, the webpage should redirect to a URL with the custom scheme
 * or an HTTPS URL with the host and path provided to the {@link #launch} method.
 *
 * <p> Before starting the auth flow, you should create an {@link ActivityResultLauncher}
 * using {@link #registerActivityResultLauncher}. This launcher should be created unconditionally
 * before every fragment or activity creation. The {@link ActivityResultCallback} provided to the
 * launcher will be called with the result of the authentication flow, indicating success or
 * failure.
 *
 * <p> If using an HTTPS redirect URL, you need to establish that your app and the redirect URL
 * are owned by the same organization using Digital Asset Links. If the verification fails, the Auth
 * Tab will return an {@link AuthResult} with the result code {@link RESULT_VERIFICATION_FAILED}.
 *
 * <p> Code sample:
 * <pre><code>
 * // In your activity
 * private final ActivityResultLauncher&lt;Intent&gt; mLauncher =
 *             AuthTabIntent.registerActivityResultLauncher(this, this::handleAuthResult);
 *
 * private void handleAuthResult(AuthTabIntent.AuthResult result) {
 *     // Check the result code
 *     boolean success = result.resultCode == AuthTabIntent.RESULT_OK;
 *     String message =
 *             getResources()
 *                     .getString(success ? R.string.auth_tab_success : R.string.auth_tab_failure);
 *     // Retrieve the result Uri
 *     message += " uri: " + result.resultUri;
 *     Toast.makeText(this, message, Toast.LENGTH_LONG).show();
 * }
 *
 * ...
 *
 * private void launchAuthTab() {
 *     AuthTabIntent authTabIntent = new AuthTabIntent.Builder().build();
 *     authTabIntent.launch(mLauncher, Uri.parse("https://www.example.com/auth"), "myscheme");
 * }
 *
 * ...
 * </code></pre>
 *
 * <p> Note: The constants below are public for the browser implementation's benefit. You are
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
    /** String extra that determines the host of the https redirect. */
    public static final String EXTRA_HTTPS_REDIRECT_HOST =
            "androidx.browser.auth.extra.HTTPS_REDIRECT_HOST";
    /** String extra that determines the path of the https redirect. */
    public static final String EXTRA_HTTPS_REDIRECT_PATH =
            "androidx.browser.auth.extra.HTTPS_REDIRECT_PATH";

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({RESULT_CANCELED, RESULT_OK, RESULT_VERIFICATION_FAILED, RESULT_VERIFICATION_TIMED_OUT,
            RESULT_UNKNOWN_CODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {
    }

    /**
     * Result code for when the Auth Tab is closed without the user completing the auth flow, e.g.
     * the user clicked the close button.
     */
    public static final int RESULT_CANCELED = Activity.RESULT_CANCELED;
    /**
     * Result code for when the Auth Tab is closed as a result of the expected redirect, implying
     * the auth flow was completed.
     */
    public static final int RESULT_OK = Activity.RESULT_OK;
    /**
     * Result code for when the Auth Tab is closed because the verification of the ownership of the
     * HTTPS redirect URL failed.
     */
    public static final int RESULT_VERIFICATION_FAILED = 2;
    /**
     * Result code for when the Auth Tab is closed because the verification of the ownership of the
     * HTTPS redirect URL couldn't be completed in a reasonable amount of time.
     */
    public static final int RESULT_VERIFICATION_TIMED_OUT = 3;
    /**
     * Result code for when the Auth Tab implementation returns an invalid or unknown result.
     */
    public static final int RESULT_UNKNOWN_CODE = -2;

    /** An {@link Intent} used to start the Auth Tab Activity. */
    @NonNull public final Intent intent;

    /**
     * Launches an Auth Tab Activity. Must be used for flows that result in a redirect with a custom
     * scheme.
     *
     * @param launcher       The {@link ActivityResultLauncher} used to launch the Auth Tab. Use
     *                       {@link #registerActivityResultLauncher} to create this. See the class
     *                       documentation for more details.
     * @param url            The url to load in the Auth Tab.
     * @param redirectScheme The scheme of the resulting redirect.
     */
    public void launch(@NonNull ActivityResultLauncher<Intent> launcher, @NonNull Uri url,
            @NonNull String redirectScheme) {
        intent.setData(url);
        intent.putExtra(EXTRA_REDIRECT_SCHEME, redirectScheme);
        launcher.launch(intent);
    }

    /**
     * Launches an Auth Tab Activity. Must be used for flows that result in a redirect with the
     * HTTPS scheme.
     *
     * @param launcher     The {@link ActivityResultLauncher} used to launch the Auth Tab. Use
     *                     {@link #registerActivityResultLauncher} to create this. See the class
     *                     documentation for more details.
     * @param url          The url to load in the Auth Tab.
     * @param redirectHost The host portion of the resulting https redirect.
     * @param redirectPath The path portion of the resulting https redirect.
     */
    public void launch(@NonNull ActivityResultLauncher<Intent> launcher, @NonNull Uri url,
            @NonNull String redirectHost, @NonNull String redirectPath) {
        intent.setData(url);
        intent.putExtra(EXTRA_HTTPS_REDIRECT_HOST, redirectHost);
        intent.putExtra(EXTRA_HTTPS_REDIRECT_PATH, redirectPath);
        launcher.launch(intent);
    }

    private AuthTabIntent(@NonNull Intent intent) {
        this.intent = intent;
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
            @NonNull ActivityResultCaller caller,
            @NonNull ActivityResultCallback<AuthResult> callback) {
        return caller.registerForActivityResult(new AuthenticateUserResultContract(), callback);
    }

    /**
     * Class containing Auth Tab result data. This class must be the result type of the
     * {@link ActivityResultCallback} passed to {@link #registerActivityResultLauncher}.
     *
     * <p> Valid `resultCode`s are {@link RESULT_OK}, {@link RESULT_CANCELED},
     * {@link RESULT_VERIFICATION_FAILED} and {@link RESULT_VERIFICATION_TIMED_OUT}.
     */
    public static final class AuthResult {
        /**
         * Result code of the Auth Tab. If an invalid or unknown code was returned by the Auth Tab,
         * this will be {@link RESULT_UNKNOWN_CODE}.
         */
        @ResultCode
        public final int resultCode;
        /**
         * The {@link Uri} containing the Auth Tab result data. Null if the `resultCode` isn't
         * {@link RESULT_OK}.
         */
        @Nullable
        public final Uri resultUri;

        AuthResult(@ResultCode int resultCode, @Nullable Uri resultUri) {
            this.resultCode = resultCode;
            this.resultUri = resultUri;
        }
    }

    static class AuthenticateUserResultContract extends ActivityResultContract<Intent, AuthResult> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Intent input) {
            return input;
        }

        @NonNull
        @Override
        public AuthResult parseResult(int resultCode, @Nullable Intent intent) {
            Uri resultUri = null;
            switch (resultCode) {
                case RESULT_OK:
                    resultUri = intent != null ? intent.getData() : null;
                    break;
                case RESULT_CANCELED:
                case RESULT_VERIFICATION_FAILED:
                case RESULT_VERIFICATION_TIMED_OUT:
                    break;
                default:
                    resultCode = RESULT_UNKNOWN_CODE;
            }
            return new AuthResult(resultCode, resultUri);
        }
    }
}
