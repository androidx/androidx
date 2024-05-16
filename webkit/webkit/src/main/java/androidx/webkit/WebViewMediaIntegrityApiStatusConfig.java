/*
 * Copyright 2023 The Android Open Source Project
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

import android.webkit.WebView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebSettingsBoundaryInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration to set API enablement status for site origins through override rules.
 *
 * <p>Websites will follow the default status supplied in the builder constructor,
 * unless the site origin matches one of the origin patterns supplied in the override rules.
 *
 * <p>The override rules are a map from origin patterns to the desired
 * {@link WebViewMediaIntegrityApiStatus}.
 */
@RequiresFeature(name = WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
public class WebViewMediaIntegrityApiStatusConfig {
    @Target(ElementType.TYPE_USE)
    @IntDef({WEBVIEW_MEDIA_INTEGRITY_API_DISABLED,
            WEBVIEW_MEDIA_INTEGRITY_API_ENABLED_WITHOUT_APP_IDENTITY,
            WEBVIEW_MEDIA_INTEGRITY_API_ENABLED})
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @interface WebViewMediaIntegrityApiStatus {
    }

    /**
     * Enables the WebView Media Integrity API and allows sharing of the app package name with
     * the JavaScript caller.
     *
     * <p>This is the default value.
     */
    public static final int WEBVIEW_MEDIA_INTEGRITY_API_ENABLED =
            WebSettingsBoundaryInterface.WebViewMediaIntegrityApiStatus.ENABLED;

    /**
     * Enables the WebView Media Integrity API for JavaScript callers but disables sharing app
     * package name in generated tokens.
     */
    public static final int WEBVIEW_MEDIA_INTEGRITY_API_ENABLED_WITHOUT_APP_IDENTITY =
            WebSettingsBoundaryInterface.WebViewMediaIntegrityApiStatus
                    .ENABLED_WITHOUT_APP_IDENTITY;

    /**
     * Disables the WebView Media Integrity API and causes it to return an
     * error code to the JavaScript callers indicating that the app has disabled it.
     */
    public static final int WEBVIEW_MEDIA_INTEGRITY_API_DISABLED =
            WebSettingsBoundaryInterface.WebViewMediaIntegrityApiStatus.DISABLED;

    private final @WebViewMediaIntegrityApiStatus int mDefaultStatus;
    private final Map<String, @WebViewMediaIntegrityApiStatus Integer> mOverrideRules;

    public WebViewMediaIntegrityApiStatusConfig(@NonNull Builder builder) {
        this.mDefaultStatus = builder.mDefaultStatus;
        this.mOverrideRules = builder.mOverrideRules;
    }

    /**
     * Builds a {@link WebViewMediaIntegrityApiStatusConfig} having a default API status and
     * a map of origin pattern rules to their respective API status.
     *
     * <p>
     * Example:
     * <pre class="prettyprint">
     *     // Create a config with default API status being DISABLED and API status is ENABLED for
     *     // Uris matching origin pattern "http://*.example.com"
     *     new WebViewMediaIntegrityApiStatusConfig.Builder(WEBVIEW_MEDIA_INTEGRITY_API_DISABLED)
     *         .addOverrideRule("http://*.example.com", WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
     *         .build();
     * </pre>
     */
    public static final class Builder {
        private final @WebViewMediaIntegrityApiStatus int mDefaultStatus;
        private Map<String, @WebViewMediaIntegrityApiStatus Integer> mOverrideRules;

        /**
         * @param defaultStatus Default API status that will be used for URIs that don't match
         *                      any origin pattern rule.
         */
        public Builder(@WebViewMediaIntegrityApiStatus int defaultStatus) {
            this.mDefaultStatus = defaultStatus;
            this.mOverrideRules = new HashMap<>();
        }

        /**
         * Add an override rule to set a specific API status for origin sites matching the origin
         * pattern stated in the rule. Origin patterns should be supplied in the same format as
         * those in
         * {@link androidx.webkit.WebViewCompat.WebMessageListener#addWebMessageListener(WebView, String, Set, WebViewCompat.WebMessageListener)}
         *
         * If two or more origin patterns match a given origin site, the least permissive option
         * will be chosen.
         */

        @NonNull
        public Builder addOverrideRule(@NonNull String originPattern,
                @WebViewMediaIntegrityApiStatus int permission) {
            mOverrideRules.put(originPattern, permission);
            return this;
        }

        /**
         * Set all required override rules at once using a map of origin patterns to
         * desired API statuses. This overwrites existing rules.
         * <p>
         * If two or more origin patterns match a given origin site, the least permissive option
         * will be chosen.
         * <p>
         * This is only meant for internal use within the library.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setOverrideRules(@NonNull Map<String,
                @WebViewMediaIntegrityApiStatus Integer> overrideRules) {
            mOverrideRules = overrideRules;
            return this;
        }

        /**
         * Build the config.
         */
        @NonNull
        public WebViewMediaIntegrityApiStatusConfig build() {
            return new WebViewMediaIntegrityApiStatusConfig(this);
        }
    }

    /**
     * Returns the default value for origins that don't match any override rules.
     */
    public @WebViewMediaIntegrityApiStatus int getDefaultStatus() {
        return mDefaultStatus;
    }

    /**
     * Get the explicitly set override rules.
     * <p> This is a map from origin patterns to their desired WebView Media Integrity API statuses.
     *
     */
    @NonNull
    public Map<String, @WebViewMediaIntegrityApiStatus Integer> getOverrideRules() {
        return mOverrideRules;
    }
}
