/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;

import org.chromium.support_lib_boundary.WebViewCookieManagerBoundaryInterface;

import java.util.List;

/**
 * Adapter between CookieManagerCompat and
 * {@link org.chromium.support_lib_boundary.CookieManagerBoundaryInterface} (the
 * corresponding interface shared with the support library glue in the WebView APK).
 */
public class CookieManagerAdapter {
    private final WebViewCookieManagerBoundaryInterface mBoundaryInterface;

    public CookieManagerAdapter(@NonNull WebViewCookieManagerBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Adapter method for {@link androidx.webkit.CookieManagerCompat#getCookieInfo}.
     */
    public @NonNull List<String> getCookieInfo(@NonNull String url) {
        return mBoundaryInterface.getCookieInfo(url);
    }
}
