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

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_SYSTEM;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_CLOSE_BUTTON_ICON;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_COLOR_SCHEME;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_COLOR_SCHEME_PARAMS;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_ENABLE_EPHEMERAL_BROWSING;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION_ID;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.ExperimentalEphemeralBrowsing;
import androidx.browser.customtabs.ExperimentalPendingSession;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    public final @NonNull Intent intent;

    private final @Nullable AuthTabSession mSession;
    private final AuthTabSession.@Nullable PendingSession mPendingSession;

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

    /**
     * Returns whether ephemeral browsing is enabled.
     */
    @ExperimentalEphemeralBrowsing
    public boolean isEphemeralBrowsingEnabled() {
        return intent.getBooleanExtra(EXTRA_ENABLE_EPHEMERAL_BROWSING, false);
    }

    /**
     * Retrieves the instance of {@link AuthTabColorSchemeParams} from an {@link Intent} for a given
     * color scheme.
     *
     * @param intent      {@link Intent} to retrieve the color scheme params from.
     * @param colorScheme A constant representing a color scheme. Must not be
     *                    {@link #COLOR_SCHEME_SYSTEM}.
     * @return An instance of {@link AuthTabColorSchemeParams} with retrieved params.
     */
    public static @NonNull AuthTabColorSchemeParams getColorSchemeParams(@NonNull Intent intent,
            @CustomTabsIntent.ColorScheme @IntRange(from = COLOR_SCHEME_LIGHT, to =
                    COLOR_SCHEME_DARK) int colorScheme) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return AuthTabColorSchemeParams.fromBundle(null);
        }

        AuthTabColorSchemeParams defaults = AuthTabColorSchemeParams.fromBundle(extras);
        SparseArray<Bundle> paramBundles = BundleCompat.getSparseParcelableArray(extras,
                EXTRA_COLOR_SCHEME_PARAMS, Bundle.class);
        if (paramBundles != null) {
            Bundle bundleForScheme = paramBundles.get(colorScheme);
            if (bundleForScheme != null) {
                return AuthTabColorSchemeParams.fromBundle(bundleForScheme).withDefaults(defaults);
            }
        }
        return defaults;
    }

    private AuthTabIntent(@NonNull Intent intent, @Nullable AuthTabSession session,
            AuthTabSession.@Nullable PendingSession pendingSession) {
        this.intent = intent;
        mSession = session;
        mPendingSession = pendingSession;
    }

    @Nullable
    public AuthTabSession getSession() {
        return mSession;
    }

    @ExperimentalPendingSession
    public AuthTabSession.@Nullable PendingSession getPendingSession() {
        return mPendingSession;
    }

    @Nullable
    public Bitmap getCloseButtonIcon() {
        return IntentCompat.getParcelableExtra(intent, EXTRA_CLOSE_BUTTON_ICON, Bitmap.class);
    }

    /**
     * Builder class for {@link AuthTabIntent} objects.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(Intent.ACTION_VIEW);
        private final AuthTabColorSchemeParams.Builder mDefaultColorSchemeBuilder =
                new AuthTabColorSchemeParams.Builder();
        private @Nullable SparseArray<Bundle> mColorSchemeParamBundles;
        private @Nullable Bundle mDefaultColorSchemeBundle;
        private @Nullable AuthTabSession mSession;
        private AuthTabSession.@Nullable PendingSession mPendingSession;

        public Builder() {
        }

        /**
         * Associates the {@link Intent} with the given {@link AuthTabSession}.
         *
         * Guarantees that the {@link Intent} will be sent to the same component as the one the
         * session is associated with.
         *
         * @param session The {@link AuthTabSession} to associate the intent with.
         */
        public @NonNull Builder setSession(@NonNull AuthTabSession session) {
            mSession = session;
            mIntent.setPackage(session.getComponentName().getPackageName());
            setSessionParameters(session.getBinder(), session.getId());
            return this;
        }

        /**
         * Associates the {@link Intent} with the given {@link AuthTabSession.PendingSession}.
         * Overrides the effect of {@link #setSession}.
         *
         * @param session The {@link AuthTabSession.PendingSession} to associate the intent with.
         */
        @ExperimentalPendingSession
        public @NonNull Builder setPendingSession(AuthTabSession.@NonNull PendingSession session) {
            mPendingSession = session;
            setSessionParameters(null, session.getId());
            return this;
        }

        private void setSessionParameters(@Nullable IBinder binder,
                @Nullable PendingIntent sessionId) {
            Bundle bundle = new Bundle();
            bundle.putBinder(EXTRA_SESSION, binder);
            if (sessionId != null) {
                bundle.putParcelable(EXTRA_SESSION_ID, sessionId);
            }

            mIntent.putExtras(bundle);
        }

        /**
         * Sets whether to enable ephemeral browsing within the Auth Tab. If ephemeral browsing is
         * enabled, and the browser supports it, the Auth Tab does not share cookies or other data
         * with the browser that handles the auth session.
         *
         * @param enabled Whether ephemeral browsing is enabled.
         * @see CustomTabsIntent#EXTRA_ENABLE_EPHEMERAL_BROWSING
         */
        @ExperimentalEphemeralBrowsing
        public AuthTabIntent.@NonNull Builder setEphemeralBrowsingEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_ENABLE_EPHEMERAL_BROWSING, enabled);
            return this;
        }

        /**
         * Sets the color scheme that should be applied to the user interface in the Auth Tab.
         *
         * @param colorScheme Desired color scheme.
         * @see CustomTabsIntent#COLOR_SCHEME_SYSTEM
         * @see CustomTabsIntent#COLOR_SCHEME_LIGHT
         * @see CustomTabsIntent#COLOR_SCHEME_DARK
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public @NonNull Builder setColorScheme(
                @CustomTabsIntent.ColorScheme @IntRange(from = COLOR_SCHEME_SYSTEM, to =
                        COLOR_SCHEME_DARK) int colorScheme) {
            mIntent.putExtra(EXTRA_COLOR_SCHEME, colorScheme);
            return this;
        }

        /**
         * Sets {@link AuthTabColorSchemeParams} for the given color scheme.
         *
         * This allows specifying two different toolbar colors for light and dark schemes.
         * It can be useful if {@link CustomTabsIntent#COLOR_SCHEME_SYSTEM} is set: the Auth Tab
         * will follow the system settings and apply the corresponding
         * {@link AuthTabColorSchemeParams} "on the fly" when the settings change.
         *
         * If there is no {@link AuthTabColorSchemeParams} for the current scheme, or a particular
         * field of it is null, the Auth Tab will fall back to the defaults provided via
         * {@link #setDefaultColorSchemeParams}.
         *
         * Example:
         * <pre><code>
         *     AuthTabColorSchemeParams darkParams = new AuthTabColorSchemeParams.Builder()
         *             .setToolbarColor(darkColor)
         *             .build();
         *     AuthTabColorSchemeParams otherParams = new AuthTabColorSchemeParams.Builder()
         *             .setNavigationBarColor(otherColor)
         *             .build();
         *     AuthTabIntent intent = new AuthTabIntent.Builder()
         *             .setColorScheme(COLOR_SCHEME_SYSTEM)
         *             .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
         *             .setDefaultColorSchemeParams(otherParams)
         *             .build();
         *
         *     // Setting colors independently of color scheme
         *     AuthTabColorSchemeParams params = new AuthTabColorSchemeParams.Builder()
         *             .setToolbarColor(color)
         *             .setNavigationBarColor(color)
         *             .build();
         *     AuthTabIntent intent = new AuthTabIntent.Builder()
         *             .setDefaultColorSchemeParams(params)
         *             .build();
         * </code></pre>
         *
         * @param colorScheme A constant representing a color scheme (see {@link #setColorScheme}).
         *                    It should not be {@link #COLOR_SCHEME_SYSTEM}, because that represents
         *                    a behavior rather than a particular color scheme.
         * @param params      An instance of {@link AuthTabColorSchemeParams}.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public AuthTabIntent.@NonNull Builder setColorSchemeParams(
                @CustomTabsIntent.ColorScheme @IntRange(from = COLOR_SCHEME_LIGHT, to =
                        COLOR_SCHEME_DARK) int colorScheme,
                @NonNull AuthTabColorSchemeParams params) {
            if (mColorSchemeParamBundles == null) {
                mColorSchemeParamBundles = new SparseArray<>();
            }
            mColorSchemeParamBundles.put(colorScheme, params.toBundle());
            return this;
        }

        /**
         * Sets the default {@link AuthTabColorSchemeParams}.
         *
         * This will set a default color scheme that applies when no
         * {@link AuthTabColorSchemeParams} specified for current color scheme via
         * {@link #setColorSchemeParams}.
         *
         * @param params An instance of {@link AuthTabColorSchemeParams}.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public AuthTabIntent.@NonNull Builder setDefaultColorSchemeParams(
                @NonNull AuthTabColorSchemeParams params) {
            mDefaultColorSchemeBundle = params.toBundle();
            return this;
        }

        /**
         * Sets the close button icon for the Auth Tab.
         *
         * A 24x24dp icon is recommended, though it may be scaled to fit the toolbar. The icon will
         * be tinted according to the toolbar's color scheme; its original color is ignored, but the
         * alpha channel is preserved.
         *
         * @param icon The icon {@link Bitmap}.
         */
        public @NonNull Builder setCloseButtonIcon(@NonNull Bitmap icon) {
            mIntent.putExtra(EXTRA_CLOSE_BUTTON_ICON, icon);
            return this;
        }

        /**
         * Combines all the options that have been set and returns a new {@link AuthTabIntent}
         * object.
         */
        public @NonNull AuthTabIntent build() {
            mIntent.putExtra(EXTRA_LAUNCH_AUTH_TAB, true);

            // Put a null EXTRA_SESSION as a fallback so that this is interpreted as a Custom Tab
            // intent by browser implementations that don't support Auth Tab.
            if (!mIntent.hasExtra(EXTRA_SESSION)) {
                setSessionParameters(null, null);
            }

            mIntent.putExtras(mDefaultColorSchemeBuilder.build().toBundle());
            if (mDefaultColorSchemeBundle != null) {
                mIntent.putExtras(mDefaultColorSchemeBundle);
            }

            if (mColorSchemeParamBundles != null) {
                Bundle bundle = new Bundle();
                bundle.putSparseParcelableArray(EXTRA_COLOR_SCHEME_PARAMS,
                        mColorSchemeParamBundles);
                mIntent.putExtras(bundle);
            }

            return new AuthTabIntent(mIntent, mSession, mPendingSession);
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
    public static @NonNull ActivityResultLauncher<Intent> registerActivityResultLauncher(
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
        public final @Nullable Uri resultUri;

        AuthResult(@ResultCode int resultCode, @Nullable Uri resultUri) {
            this.resultCode = resultCode;
            this.resultUri = resultUri;
        }
    }

    static class AuthenticateUserResultContract extends ActivityResultContract<Intent, AuthResult> {
        @Override
        public @NonNull Intent createIntent(@NonNull Context context, @NonNull Intent input) {
            return input;
        }

        @Override
        public @NonNull AuthResult parseResult(int resultCode, @Nullable Intent intent) {
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
