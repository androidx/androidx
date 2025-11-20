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

import org.chromium.support_lib_boundary.WebViewBuilderBoundaryInterface;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RestrictionAllowlist can be used to scope WebView behaviors to particular origin patterns.
 *
 * <p>Add a RestrictionAllowlist via {@link WebViewBuilder#addAllowlist}. For example:
 * <pre class="prettyprint">
 * WebView webview = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY)
 *         .restrictJavaScriptInterfaces()
 *         .addAllowlist(new RestrictionAllowlist.Builder(Set.of("https://example.com"))
 *                 .addJavaScriptInterface(someJavaScriptInterface, "myInterface")
 *                 .build())
 *         .build();
 * </pre>
 * <p>This example creates a WebView where the
 * {@link android.webkit.WebView#addJavascriptInterface(Object, String)} and
 * {@link android.webkit.WebView#removeJavascriptInterface(String)} APIs are disabled, but adds an
 * allowlist that injects the {@code someJavaScriptInterface} object as {@code window.myInterface}
 * into JavaScript only for frames with the {@code https://example.com} origin.
 */
@WebViewBuilder.Experimental
public final class RestrictionAllowlist {
    private final @NonNull List<@NonNull ConfigTask> mConfigTasks;

    private RestrictionAllowlist(@NonNull List<@NonNull ConfigTask> configTasks) {
        mConfigTasks = configTasks;
    }

    void configure(WebViewBuilderBoundaryInterface.Config config) {
        for (ConfigTask configTask : mConfigTasks) {
            configTask.configure(config);
        }
    }

    /**
     * RestrictionAllowlist builder. Add the RestrictionAllowlist produced to a WebView via
     * {@link WebViewBuilder#addAllowlist}.
     */
    public static final class Builder {
        private final @NonNull List<@NonNull String> mOriginPatterns;
        private final @NonNull List<@NonNull ConfigTask> mConfigTasks = new ArrayList<>();

        /**
         * Construct a RestrictionAllowlist Builder
         *
         * <p>See {@link #addWebMessageListener(WebView, String, Set, WebMessageListener)} for the
         * rules of the {@code originPatterns} parameter.
         *
         * @param originPatterns List of origin patterns to allow the selected behaviors on.
         */
        public Builder(@NonNull Set<@NonNull String> originPatterns) {
            mOriginPatterns = new ArrayList<@NonNull String>(originPatterns);
        }

        /**
         * This API is the same as {@link WebView#addJavascriptInterface(Object, String)} except it
         * will only inject the interface into the origin patterns allowed.
         *
         * <p>A {@code name} value may only be used once per WebViewBuilder, regardless of whether
         * it's the same, a different, or an equivalent allowlist.
         */
        // There's no practical need for a getter with the intended use of RestrictionAllowlist.
        // This data is only stored via variable capture, and adding getters may only complicate
        // future changes.
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public @NonNull Builder addJavaScriptInterface(@NonNull Object object,
                @NonNull String name) {
            mConfigTasks.add(
                    config -> config.addJavascriptInterface(object, name, mOriginPatterns));
            return this;
        }

        /**
         * Constructs a new RestrictionAllowlist that can be attached via
         * {@link WebViewBuilder#setRestrictionAllowlist(RestrictionAllowlist)}.
         *
         * @see WebViewBuilder
         */
        public @NonNull RestrictionAllowlist build() {
            return new RestrictionAllowlist(mConfigTasks);
        }
    }

    private interface ConfigTask {
        void configure(WebViewBuilderBoundaryInterface.Config config);
    }
}
