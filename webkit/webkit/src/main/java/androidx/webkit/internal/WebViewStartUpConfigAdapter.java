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

package androidx.webkit.internal;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewStartUpConfig;

import org.chromium.support_lib_boundary.WebViewStartUpConfigBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Adapter between WebViewStartUpConfig and WebViewStartUpConfigBoundaryInterface (the
 * corresponding interface shared with the support library glue in the WebView APK).
 */
@WebViewCompat.ExperimentalAsyncStartUp
public class WebViewStartUpConfigAdapter implements WebViewStartUpConfigBoundaryInterface {
    private final WebViewStartUpConfig mWebViewStartUpConfig;

    public WebViewStartUpConfigAdapter(
            @NonNull WebViewStartUpConfig webViewStartUpConfig) {
        mWebViewStartUpConfig = webViewStartUpConfig;
    }

    /**
     * Adapter method for {@link WebViewStartUpConfig#getBackgroundExecutor()}.
     */
    @Override
    @NonNull
    public Executor getBackgroundExecutor() {
        return mWebViewStartUpConfig.getBackgroundExecutor();
    }

    /**
     * Adapter method for {@link WebViewStartUpConfig#shouldRunUiThreadStartUpTasks()}.
     */
    @Override
    public boolean shouldRunUiThreadStartUpTasks() {
        return mWebViewStartUpConfig.shouldRunUiThreadStartUpTasks();
    }

    /**
     * Adapter method for {@link WebViewStartUpConfig#getProfilesToLoadDuringStartup()}.
     */
    @Override
    @Nullable
    public Set<String> getProfileNamesToLoad() {
        return mWebViewStartUpConfig.getProfilesToLoadDuringStartup();
    }
}
