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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs and launches an intent to start a Trusted Web Activity (see {@link TrustedWebUtils}
 * for more details).
 */
public class TrustedWebActivityBuilder {
    /**
     * Extra for the Trusted Web Activity launch Intent to specify a {@link Bundle} of parameters
     * for the browser to use in constructing a splash screen.
     *
     * It is recommended to use {@link TrustedWebActivityBuilder} instead of manually piecing the
     * Intent together.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_SPLASH_SCREEN_PARAMS =
            "androidx.browser.trusted.EXTRA_SPLASH_SCREEN_PARAMS";

    /**
     * Extra for the Trusted Web Activity launch Intent to specify a list of origins for the
     * browser to treat as trusted, in addition to the origin of the launching URL.
     *
     * It is recommended to use {@link TrustedWebActivityBuilder} instead of manually piecing the
     * Intent together.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_ADDITIONAL_TRUSTED_ORIGINS =
            "android.support.customtabs.extra.ADDITIONAL_TRUSTED_ORIGINS";

    private final Context mContext;
    private final Uri mUri;

    @Nullable private Integer mStatusBarColor;
    @Nullable private List<String> mAdditionalTrustedOrigins;
    @Nullable private Bundle mSplashScreenParams;

    /**
     * Creates a Builder given the required parameters.
     * @param context {@link Context} to use.
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public TrustedWebActivityBuilder(@NonNull Context context, @NonNull Uri uri) {
        mContext = context;
        mUri = uri;
    }

    /**
     * Sets the status bar color to be seen while the Trusted Web Activity is running.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public TrustedWebActivityBuilder setStatusBarColor(@ColorInt int color) {
        mStatusBarColor = color;
        return this;
    }

    /**
     * Sets a list of additional trusted origins that the user may navigate or be redirected to
     * from the starting uri.
     *
     * For example, if the user starts at https://www.example.com/page1 and is redirected to
     * https://m.example.com/page2, and both origins are associated with the calling application
     * via the Digital Asset Links, then pass "https://www.example.com/page1" as uri and
     * Arrays.asList("https://m.example.com") as additionalTrustedOrigins.
     *
     * Alternatively, use {@link CustomTabsSession#validateRelationship} to validate additional
     * origins asynchronously, but that would delay launching the Trusted Web Activity.
     */
    @NonNull
    public TrustedWebActivityBuilder setAdditionalTrustedOrigins(
            @NonNull List<String> origins) {
        mAdditionalTrustedOrigins = origins;
        return this;
    }

    /**
     * Sets the parameters of a splash screen shown while the web page is loading, such as
     * background color. See {@link SplashScreenParamKey} for a list of supported parameters.
     *
     * To provide the image for the splash screen, use {@link TrustedWebUtils#transferSplashImage},
     * prior to calling {@link #launchActivity} on the builder.
     *
     * It is recommended to also show the same splash screen in the app as soon as possible,
     * prior to establishing a CustomTabConnection. The Trusted Web Activity provider should
     * ensure seamless transition of the splash screen from the app onto the top of webpage
     * being loaded.
     *
     * The splash screen will be removed on the first paint of the page, or when the page load
     * fails.
     */
    @NonNull
    public TrustedWebActivityBuilder setSplashScreenParams(@NonNull Bundle splashScreenParams) {
        mSplashScreenParams = splashScreenParams;
        return this;
    }

    /**
     * Launches a Trusted Web Activity. Once it is launched, browser side implementations may
     * have their own fallback behavior (e.g. showing the page in a custom tab UI with toolbar).
     *
     * @param session The {@link CustomTabsSession} to use for launching a Trusted Web Activity.
     */
    public void launchActivity(@NonNull CustomTabsSession session) {
        if (session == null) {
            throw new NullPointerException("CustomTabsSession is required for launching a TWA");
        }

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(session);
        if (mStatusBarColor != null) {
            // Toolbar color applies also to the status bar.
            intentBuilder.setToolbarColor(mStatusBarColor);
        }

        Intent intent = intentBuilder.build().intent;
        intent.setData(mUri);
        intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        if (mAdditionalTrustedOrigins != null) {
            intent.putExtra(EXTRA_ADDITIONAL_TRUSTED_ORIGINS,
                    new ArrayList<>(mAdditionalTrustedOrigins));
        }

        if (mSplashScreenParams != null) {
            intent.putExtra(EXTRA_SPLASH_SCREEN_PARAMS, mSplashScreenParams);
        }
        ContextCompat.startActivity(mContext, intent, null);
    }

    /**
     * Returns the {@link Uri} to be launched with this Builder.
     */
    @NonNull
    public Uri getUrl() {
        return mUri;
    }

    /**
     * Returns the color set via {@link #setStatusBarColor(int)} or {@code null} if not set.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    @ColorInt
    public Integer getStatusBarColor() {
        return mStatusBarColor;
    }
}
