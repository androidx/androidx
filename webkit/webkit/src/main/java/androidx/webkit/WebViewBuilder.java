/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webkit;

import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RequiresOptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import org.chromium.support_lib_boundary.WebViewBuilderBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * WebViewBuilder can be used in place of {@link android.webkit.WebView}'s constructor.
 *
 * <p>This API allows you to declare how the WebView will be used via APIs like
 * {@link RestrictionAllowlist}.
 *
 * <p>WebView instances constructed by this builder can be used as direct drop-in replacements for
 * WebViews created by {@link WebView#WebView(Context)} with no additional code changes.
 */
@WebViewBuilder.Experimental
// WebView is a framework class that cannot be readily evolved. This builder in AndroidX can only be
// implemented at the top level.
@SuppressWarnings("TopLevelBuilder")
public final class WebViewBuilder {
    private boolean mRestrictJavascriptInterface;
    private final @NonNull List<@NonNull RestrictionAllowlist> mAllowLists =
            new ArrayList<@NonNull RestrictionAllowlist>();
    private @Nullable String mProfileName;

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    public @interface Experimental {}

    /**
     * Matches the configuration of a WebView created via the {@link WebView#WebView(Context)}
     * constructor.
     */
    public static final int PRESET_LEGACY = 0;

    /**
     * Common configuration presets for WebView.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        PRESET_LEGACY,
    })
    public @interface Preset {}

    @Nullable WebViewBuilderBoundaryInterface mBuilderStateBoundary;

    /**
     * Create a new builder with settings initialized to the given preset Preset.
     *
     * <p>Currently, only the {@link PRESET_LEGACY} preset is supported.
     */
    public WebViewBuilder(@Preset int preset) {
        if (preset != PRESET_LEGACY) {
            throw new IllegalArgumentException("Invalid preset: " + preset);
        }
        // TODO(crbug.com/419726203): We only have the no-op LEGACY preset right now, so no logic
        // consumes this argument, yet.
    }

    /**
     * Restrict {@link WebView#addJavascriptInterface(Object, String)} and
     * {@link WebView#removeJavascriptInterface(String)} from being callable.
     *
     * <p>Opting into this restriction makes these methods throw a RuntimeException if called on the
     * built WebView.
     *
     * <p>This needs to be called in order to allow specific origin patterns to inject JavaScript
     * interfaces via {@link RestrictionAllowlist#addJavaScriptInterface(Object, String)}.
     */
    // We prefer a one-directional switch in order to improve app code auditability.
    @SuppressWarnings("BuilderSetStyle")
    public @NonNull WebViewBuilder restrictJavaScriptInterfaces() {
        mRestrictJavascriptInterface = true;
        return this;
    }

    /**
     * Set the profile for the WebView.
     *
     * <p>If the profile does not exist, it will be created when {@link WebViewBuilder#build} is
     * called, as per {@link ProfileStore#getOrCreateProfile(String)}.
     *
     * @param profileName The name of the profile to use.
     */
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    // Corresponding getter is in WebViewCompat (AndroidX), not WebView (framework).
    // Similar to WebViewCompat, the setter uses String and the getter uses Profile.
    @SuppressWarnings("MissingGetterMatchingBuilder")
    public @NonNull WebViewBuilder setProfile(@NonNull String profileName) {
        mProfileName = profileName;
        return this;
    }

    /**
     * Add an allowlist of behaviors for a list of origin patterns. All allowlists will be merged
     * together. A WebViewBuilderException will be thrown from {@link WebViewBuilder#build(Context)}
     * if a behavior is allowlisted that has not been restricted via the WebViewBuilder.
     *
     * @param allowList An allowlist that will allow behaviors for the origin patterns provided.
     */
    // This input data is somewhat ephemeral and reprocessed. There's no direct use for a getter,
    // and the RestrictionAllowlist object itself is somewhat opaque.
    @SuppressWarnings("MissingGetterMatchingBuilder")
    public @NonNull WebViewBuilder addAllowlist(@NonNull RestrictionAllowlist allowList) {
        mAllowLists.add(allowList);
        return this;
    }

    /**
     * Constructs a new WebView with all the properties defined.
     *
     * @param context The Activity Context for the WebView.
     * @throws WebViewBuilderException if there was an issue with validation or constructing the
     *                                 WebView.
     */
    @UiThread
    @RequiresFeature(
            name = WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @NonNull WebView build(@NonNull Context context) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.WEBVIEW_BUILDER_V1;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

        WebViewBuilderBoundaryInterface builder = getBuilderStateBoundary();
        // makeConfig must be called every time in case the builder state changes.
        WebViewBuilderBoundaryInterface.Config config = makeConfig();

        try {
            return builder.build(context, config);
        } catch (RuntimeException e) {
            throw new WebViewBuilderException(e);
        }
    }

    /**
     * Applies a builder config to an existing but unused WebView.
     *
     * <p>This allows the builder to be used in cases where {@link WebViewBuilder#build(Context)} is
     * not practical, including cases where WebView has been inflated from an XML layout or
     * subclassed.
     *
     * <p>It is not permitted to call any other WebView APIs on the WebView before this. A WebView
     * may only have a builder configuration applied at most once. This API may not be used with
     * WebViews that were built with {@link WebViewBuilder#build(Context)}.
     *
     * @param webview The WebView to apply the config to.
     * @throws WebViewBuilderException if there was an issue with validation or constructing the
     *                                 WebView.
     * @throws IllegalStateException if the WebView has already been used or configured in some way.
     */
    @UiThread
    @RequiresFeature(
            name = WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @SuppressWarnings("BuilderSetStyle")
    public <T extends WebView> @NonNull T applyTo(@NonNull T webview) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.WEBVIEW_BUILDER_V2;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

        WebViewBuilderBoundaryInterface builder = getBuilderStateBoundary();
        // makeConfig must be called every time in case the builder state changes.
        WebViewBuilderBoundaryInterface.Config config = makeConfig();

        try {
            builder.applyTo(webview, config);
        } catch (IllegalStateException e) {
            // Special case IllegalStateException from any other RuntimeExceptions handled below.
            // IllegalStateException probably indicates we were passed a bad WebView, rather than a
            // bad config, so simply rethrow it.
            throw e;
        } catch (RuntimeException e) {
            throw new WebViewBuilderException(e);
        }

        return webview;
    }

    private @NonNull WebViewBuilderBoundaryInterface getBuilderStateBoundary() {
        // The boundary interface is lazy loaded but it is built with the
        // assumption that on every call to build, we can re-use the same instance.
        if (mBuilderStateBoundary == null) {
            mBuilderStateBoundary = WebViewGlueCommunicator.getFactory().getWebViewBuilder();
        }

        return mBuilderStateBoundary;
    }

    private WebViewBuilderBoundaryInterface.@NonNull Config makeConfig() {
        WebViewBuilderBoundaryInterface.Config config =
                new WebViewBuilderBoundaryInterface.Config();

        config.restrictJavascriptInterface = mRestrictJavascriptInterface;
        config.profileName = mProfileName;

        try {
            for (RestrictionAllowlist allowList : mAllowLists) {
                allowList.configure(config);
            }
        } catch (RuntimeException e) {
            throw new WebViewBuilderException(e);
        }

        return config;
    }
}
