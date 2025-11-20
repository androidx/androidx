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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.sharing.ShareData;
import androidx.browser.trusted.sharing.ShareTarget;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Constructs instances of {@link TrustedWebActivityIntent} that can be used to start Trusted Web
 * Activities (see {@link TrustedWebUtils} for more details).
 */
public class TrustedWebActivityIntentBuilder {
    /**
     * Extra for the Trusted Web Activity launch Intent to specify a {@link Bundle} of parameters
     * for the browser to use in constructing a splash screen.
     *
     * It is recommended to use {@link TrustedWebActivityIntentBuilder} instead of manually piecing
     * the Intent together.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_SPLASH_SCREEN_PARAMS =
            "androidx.browser.trusted.EXTRA_SPLASH_SCREEN_PARAMS";

    /**
     * Extra for the Trusted Web Activity launch Intent to specify a list of origins for the
     * browser to treat as trusted, in addition to the origin of the launching URL.
     *
     * It is recommended to use {@link TrustedWebActivityIntentBuilder} instead of manually piecing
     * the Intent together.
     */
    @SuppressLint("ActionValue")
    public static final String EXTRA_ADDITIONAL_TRUSTED_ORIGINS =
            "android.support.customtabs.extra.ADDITIONAL_TRUSTED_ORIGINS";

    /** Extra for the share target, see {@link #setShareParams}. */
    public static final String EXTRA_SHARE_TARGET = "androidx.browser.trusted.extra.SHARE_TARGET";

    /** Extra for the share data, see {@link #setShareParams}. */
    public static final String EXTRA_SHARE_DATA = "androidx.browser.trusted.extra.SHARE_DATA";

    /** Extra for the opened files data, see {@link #setHandledFiles}. */
    public static final String EXTRA_FILE_HANDLING_DATA =
            "androidx.browser.trusted.extra.FILE_HANDLING_DATA";

    /** Extra for the {@link TrustedWebActivityDisplayMode}, see {@link #setDisplayMode}. */
    public static final String EXTRA_DISPLAY_MODE = "androidx.browser.trusted.extra.DISPLAY_MODE";

    /** Extra for the display override list, see {@link #setDisplayOverrideList} */
    public static final String EXTRA_DISPLAY_OVERRIDE =
            "androidx.browser.trusted.extra.DISPLAY_OVERRIDE";

    /** Extra for the screenOrientation, see {@link #setScreenOrientation}. */
    public static final String EXTRA_SCREEN_ORIENTATION =
            "androidx.browser.trusted.extra.SCREEN_ORIENTATION";

    /**
     * If the TWA is being launched using a Protocol Handler, the original URL (with the custom
     * scheme) is provided here, set using {@link #setOriginalLaunchUrl(Uri)}.
     *
     * @see TrustedWebActivityIntent#getOriginalLaunchUrl()
     */
    public static final String EXTRA_ORIGINAL_LAUNCH_URL =
            "androidx.browser.trusted.extra.ORIGINAL_LAUNCH_URL";

    /**
     * If a TWA is being launched using Launch Handler API, its client mode value is provided,
     * see {@link #setLaunchHandlerClientMode}.
     */
    public static final String EXTRA_LAUNCH_HANDLER_CLIENT_MODE =
            "androidx.browser.trusted.extra.LAUNCH_HANDLER_CLIENT_MODE";

    private final @NonNull Uri mUri;
    private final CustomTabsIntent.@NonNull Builder mIntentBuilder = new CustomTabsIntent.Builder();

    private @Nullable List<String> mAdditionalTrustedOrigins;
    private @Nullable Bundle mSplashScreenParams;

    private @Nullable ShareData mShareData;
    private @Nullable ShareTarget mShareTarget;

    private @Nullable FileHandlingData mFileHandlingData;

    private @Nullable Uri mOriginalLaunchUrl;

    @LaunchHandlerClientMode.ClientMode
    private int mLaunchHandlerClientMode = LaunchHandlerClientMode.AUTO;

    private @NonNull TrustedWebActivityDisplayMode mDisplayMode =
            new TrustedWebActivityDisplayMode.DefaultMode();
    private @Nullable List<TrustedWebActivityDisplayMode> mDisplayOverrideList;

    @ScreenOrientation.LockType
    private int mScreenOrientation = ScreenOrientation.DEFAULT;

    /**
     * Creates a Builder given the required parameters.
     *
     * @param uri The web page to launch as Trusted Web Activity.
     */
    public TrustedWebActivityIntentBuilder(@NonNull Uri uri) {
        mUri = uri;
    }

    /**
     * Sets the color applied to the toolbar and the status bar, see
     * {@link CustomTabsIntent.Builder#setToolbarColor}.
     *
     * When a Trusted Web Activity is on the verified origin, the toolbar is hidden, so the color
     * applies only to the status bar. When it's on an unverified origin, the toolbar is shown, and
     * the color applies to both toolbar and status bar.
     *
     * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
     */
    @Deprecated
    public @NonNull TrustedWebActivityIntentBuilder setToolbarColor(@ColorInt int color) {
        mIntentBuilder.setToolbarColor(color);
        return this;
    }

    /**
     * Sets the navigation bar color, see {@link CustomTabsIntent.Builder#setNavigationBarColor}.
     *
     * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
     */
    @Deprecated
    public @NonNull TrustedWebActivityIntentBuilder setNavigationBarColor(@ColorInt int color) {
        mIntentBuilder.setNavigationBarColor(color);
        return this;
    }

    /**
     * Sets the navigation bar divider color, see
     * {@link CustomTabsIntent.Builder#setNavigationBarDividerColor}.
     *
     * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
     */
    @Deprecated
    public @NonNull TrustedWebActivityIntentBuilder setNavigationBarDividerColor(
            @ColorInt int color) {
        mIntentBuilder.setNavigationBarDividerColor(color);
        return this;
    }

    /**
     * Sets the color scheme, see {@link CustomTabsIntent.Builder#setColorScheme}.
     * In Trusted Web Activities color scheme may affect such UI elements as info bars and context
     * menus.
     *
     * @param colorScheme Must be one of {@link CustomTabsIntent#COLOR_SCHEME_SYSTEM},
     *                    {@link CustomTabsIntent#COLOR_SCHEME_LIGHT}, and
     *                    {@link CustomTabsIntent#COLOR_SCHEME_DARK}.
     */
    public @NonNull TrustedWebActivityIntentBuilder setColorScheme(
            @CustomTabsIntent.ColorScheme int colorScheme) {
        mIntentBuilder.setColorScheme(colorScheme);
        return this;
    }

    /**
     * Sets {@link CustomTabColorSchemeParams} for the given color scheme.
     * This allows, for example, to set two navigation bar colors - for light and dark scheme.
     * Trusted Web Activity will automatically apply the correct color according to current system
     * settings. For more details see {@link CustomTabsIntent.Builder#setColorSchemeParams}.
     */
    public @NonNull TrustedWebActivityIntentBuilder setColorSchemeParams(
            @CustomTabsIntent.ColorScheme int colorScheme,
            @NonNull CustomTabColorSchemeParams params) {
        mIntentBuilder.setColorSchemeParams(colorScheme, params);
        return this;
    }

    /**
     * Sets the default {@link CustomTabColorSchemeParams}.
     *
     * This will set a default color scheme that applies when no CustomTabColorSchemeParams
     * specified for current color scheme via {@link #setColorSchemeParams}.
     *
     * @param params An instance of {@link CustomTabColorSchemeParams}.
     */
    public @NonNull TrustedWebActivityIntentBuilder setDefaultColorSchemeParams(
            @NonNull CustomTabColorSchemeParams params) {
        mIntentBuilder.setDefaultColorSchemeParams(params);
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
    public @NonNull TrustedWebActivityIntentBuilder setAdditionalTrustedOrigins(
            @NonNull List<String> origins) {
        mAdditionalTrustedOrigins = origins;
        return this;
    }

    /**
     * Sets the parameters of a splash screen shown while the web page is loading, such as
     * background color. See {@link SplashScreenParamKey} for a list of supported parameters.
     *
     * To provide the image for the splash screen, use {@link TrustedWebUtils#transferSplashImage},
     * prior to launching the intent.
     *
     * It is recommended to also show the same splash screen in the app as soon as possible,
     * prior to establishing a CustomTabConnection. The Trusted Web Activity provider should
     * ensure seamless transition of the splash screen from the app onto the top of webpage
     * being loaded.
     *
     * The splash screen will be removed on the first paint of the page, or when the page load
     * fails.
     */
    public @NonNull TrustedWebActivityIntentBuilder setSplashScreenParams(
            @NonNull Bundle splashScreenParams) {
        mSplashScreenParams = splashScreenParams;
        return this;
    }

    /**
     * Sets the parameters for delivering data to a Web Share Target via a Trusted Web Activity.
     *
     * @param shareTarget A {@link ShareTarget} object describing the Web Share Target.
     * @param shareData   A {@link ShareData} object containing the data to be sent to the Web Share
     *                    Target.
     */
    public @NonNull TrustedWebActivityIntentBuilder setShareParams(@NonNull ShareTarget shareTarget,
            @NonNull ShareData shareData) {
        mShareTarget = shareTarget;
        mShareData = shareData;
        return this;
    }

    /**
     * Sets the parameters for delivering launch files to the launch queue.
     *
     * A web application can declare "file_handlers" in its web manifest, enabling it to handle
     * file opening requests from users. The opened files are then made available by the browser
     * to the web app through the LaunchQueue interface of the Launch Handler API. LaunchQueue
     * implementation depends on a browser receiving a TrustedWebActivityIntent.
     *
     * This method provides the support for file handling in Trusted Web Activities.
     * {@link FileHandlingData} should contain the URIs list of files opened by a user and captured
     * by an intent filter declared in the app manifest. API doesn't perform URI validation and it
     * entirely dependents on a browser receiving a TrustedWebActivityIntent.
     *
     * The API passes read/write permissions for the files to the browser to allow the web
     * application to read and write a file as it required by LaunchQueue interface specification.
     *
     * @param fileHandlingData A {@link FileHandlingData} object containing the data to be sent
     * to browser launch queue.
     */
    public @NonNull TrustedWebActivityIntentBuilder setFileHandlingData(
            @NonNull FileHandlingData fileHandlingData) {
        mFileHandlingData = fileHandlingData;
        return this;
    }

    /**
     * Sets a {@link TrustedWebActivityDisplayMode}. This can be used e.g. to enable immersive mode
     * (see {@link TrustedWebActivityDisplayMode.ImmersiveMode}. Not setting it means
     * {@link TrustedWebActivityDisplayMode.DefaultMode} will be used.
     */
    public @NonNull TrustedWebActivityIntentBuilder setDisplayMode(
            @NonNull TrustedWebActivityDisplayMode displayMode) {
        mDisplayMode = displayMode;
        return this;
    }

    /**
     * Sets the display override fallback list for a Trusted Web Activity.
     *
     * Web applications can declare a "display_override" list to specify the preferred order of
     * display modes. This is useful for more advanced display modes that may have limited browser
     * support ({@see https://wicg.github.io/manifest-incubations/#display_override-member}). The
     * browser will use the first display mode in the list that it supports, and fall back to the
     * `display` manifest field if none are supported.
     *
     * @param displayOverrideList A list of {@link TrustedWebActivityDisplayMode} that represents
     * the fallback order of display modes.
     */
    public @NonNull TrustedWebActivityIntentBuilder setDisplayOverrideList(
            @Nullable List<TrustedWebActivityDisplayMode> displayOverrideList) {
        if (displayOverrideList == null) {
            mDisplayOverrideList = new ArrayList<>();
        } else {
            mDisplayOverrideList = displayOverrideList;
        }
        return this;
    }

    /**
     * Sets a screenOrientation. This can be used e.g. to enable the locking of an orientation
     * lock type {@link ScreenOrientation}.
     *
     * @param orientation A {@link ScreenOrientation} lock type for a Trusted Web Activity.
     *                    Not setting it means {@link ScreenOrientation#DEFAULT} will be used.
     */
    public @NonNull TrustedWebActivityIntentBuilder setScreenOrientation(
            @ScreenOrientation.LockType int orientation) {
        mScreenOrientation = orientation;
        return this;
    }

    /**
     * Sets the original launch URL. This is used by Protocol Handlers to let the browser see
     * the URL that was opened before being processed an modified (the original URL will usually
     * have a custom data scheme and no host, as opposed to the full http/https location in the
     * Intent data).
     *
     * @see TrustedWebActivityIntent#getOriginalLaunchUrl()
     *
     * @param originalLaunchUrl The URL that was originally opened, before being processed and
     *                          modified by a Protocol Handler.
     */
    public @NonNull TrustedWebActivityIntentBuilder setOriginalLaunchUrl(
            @NonNull Uri originalLaunchUrl) {
        mOriginalLaunchUrl = originalLaunchUrl;
        return this;
    }

    /**
     * Sets the parameter for delivering Launch Handler API client mode via a Trusted Web Activity.
     *
     * @param launchHandlerClientMode A string describing the client mode.
     */
    public @NonNull TrustedWebActivityIntentBuilder setLaunchHandlerClientMode(
            @LaunchHandlerClientMode.ClientMode int launchHandlerClientMode) {
        mLaunchHandlerClientMode = launchHandlerClientMode;
        return this;
    }

    /**
     * Builds an instance of {@link TrustedWebActivityIntent}.
     *
     * @param session The {@link CustomTabsSession} to use for launching a Trusted Web Activity.
     */
    public @NonNull TrustedWebActivityIntent build(@NonNull CustomTabsSession session) {
        if (session == null) {
            throw new NullPointerException("CustomTabsSession is required for launching a TWA");
        }

        mIntentBuilder.setSession(session);
        Intent intent = mIntentBuilder.build().intent;
        intent.setData(mUri);
        intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        if (mAdditionalTrustedOrigins != null) {
            intent.putExtra(EXTRA_ADDITIONAL_TRUSTED_ORIGINS,
                    new ArrayList<>(mAdditionalTrustedOrigins));
        }

        if (mSplashScreenParams != null) {
            intent.putExtra(EXTRA_SPLASH_SCREEN_PARAMS, mSplashScreenParams);
        }
        List<Uri> sharedUris = Collections.emptyList();
        if (mShareTarget != null && mShareData != null) {
            intent.putExtra(EXTRA_SHARE_TARGET, mShareTarget.toBundle());
            intent.putExtra(EXTRA_SHARE_DATA, mShareData.toBundle());
            if (mShareData.uris != null) {
                sharedUris = mShareData.uris;
            }
        }
        List<Uri> fileHandlingUris = Collections.emptyList();
        if (mFileHandlingData != null) {
            intent.putExtra(EXTRA_FILE_HANDLING_DATA, mFileHandlingData.toBundle());
            if (mFileHandlingData.uris != null) {
                fileHandlingUris = mFileHandlingData.uris;
            }
        }
        intent.putExtra(EXTRA_DISPLAY_MODE, mDisplayMode.toBundle());
        if (mDisplayOverrideList != null) {
            ArrayList<Bundle> bundles = new ArrayList<>();
            for (TrustedWebActivityDisplayMode displayMode : mDisplayOverrideList) {
                bundles.add(displayMode.toBundle());
            }
            intent.putExtra(EXTRA_DISPLAY_OVERRIDE, bundles);
        }
        intent.putExtra(EXTRA_SCREEN_ORIENTATION, mScreenOrientation);
        if (mOriginalLaunchUrl != null) {
            intent.putExtra(EXTRA_ORIGINAL_LAUNCH_URL, mOriginalLaunchUrl);
        }

        intent.putExtra(EXTRA_LAUNCH_HANDLER_CLIENT_MODE, mLaunchHandlerClientMode);

        return new TrustedWebActivityIntent(intent, sharedUris, fileHandlingUris);
    }

    /**
     * Builds a {@link CustomTabsIntent} based on provided parameters.
     * Can be useful for falling back to Custom Tabs when Trusted Web Activity providers are
     * unavailable.
     */
    public @NonNull CustomTabsIntent buildCustomTabsIntent() {
        return mIntentBuilder.build();
    }

    /**
     * Returns the {@link Uri} to be launched with this Builder.
     */
    public @NonNull Uri getUri() {
        return mUri;
    }

    /**
     * Returns {@link TrustedWebActivityDisplayMode} set on this Builder.
     */
    public @NonNull TrustedWebActivityDisplayMode getDisplayMode() {
        return mDisplayMode;
    }
}
