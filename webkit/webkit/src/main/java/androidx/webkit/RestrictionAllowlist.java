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

import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebViewBuilderBoundaryInterface;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * RestrictionAllowlist can be used to scope WebView behaviors to particular origin patterns.
 *
 * <p>Add a RestrictionAllowlist via {@link WebViewBuilder#addAllowlist}
 */
@WebViewBuilder.Experimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RestrictionAllowlist {
    private final @NonNull List<ConfigTask> mConfigTasks;

    private RestrictionAllowlist(@NonNull List<ConfigTask> configTasks) {
        mConfigTasks = configTasks;
    }

    void configure(WebViewBuilderBoundaryInterface.Config config) {
        for (ConfigTask configTask : mConfigTasks) {
            configTask.configure(config);
        }
    }

    /**
     * RestrictionAllowlist builder. Add the RestrictionAllowlist produced to a WebView via {@link
     * WebViewBuilder#addAllowlist}.
     */
    public static final class Builder {
        final @NonNull List<String> mOriginPatterns;
        final @NonNull List<ConfigTask> mConfigTasks = new ArrayList<>();

        public Builder(@NonNull List<String> originPatterns) {
            mOriginPatterns = originPatterns;
        }

        /**
         * This API is the same as {@link WebView#addJavascriptInterface(Object, String)} expect it
         * will only inject the interface into the origin patterns allowed.
         */
        public @NonNull Builder javascriptInterface(@NonNull Object object, @NonNull String name) {
            mConfigTasks.add(
                    config -> config.addJavascriptInterface(object, name, mOriginPatterns));
            return this;
        }

        /**
         * Constructs a new RestrictionAllowlist that can be attached via {@link
         * WebViewBuilder#setRestrictionAllowlist(RestrictionAllowlist)}.
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
