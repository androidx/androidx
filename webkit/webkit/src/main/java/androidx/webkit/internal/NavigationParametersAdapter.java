/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.webkit.NavigationParameters;
import androidx.webkit.WebViewCompat;

import org.chromium.support_lib_boundary.NavigationParametersBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Adapter between {@link NavigationParameters} and {@link NavigationParametersBoundaryInterface}.
 */
@WebViewCompat.ExperimentalNavigate
public class NavigationParametersAdapter implements NavigationParametersBoundaryInterface {
    private static final String[] SUPPORTED_FEATURES = {Features.WEBVIEW_NAVIGATE_V1};
    private final NavigationParameters mParams;

    public NavigationParametersAdapter(@NonNull NavigationParameters params) {
        mParams = params;
    }

    @Override
    public boolean getShouldReplaceCurrentEntry() {
        return mParams.shouldReplaceCurrentEntry();
    }

    @Override
    public @Nullable Map<String, String> getAdditionalHeaders() {
        return mParams.getAdditionalHeaders();
    }

    @Override
    public @NonNull String[] getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }
}
