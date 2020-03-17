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

package androidx.webkit.internal;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewRenderProcessClient;

/**
 * Adapter class to pass a {@link android.webkit.WebViewRenderProcessClient} over to chromium.
 */
@RequiresApi(29)
public class WebViewRenderProcessClientFrameworkAdapter extends
            android.webkit.WebViewRenderProcessClient {
    private WebViewRenderProcessClient mWebViewRenderProcessClient;

    public WebViewRenderProcessClientFrameworkAdapter(@NonNull WebViewRenderProcessClient client) {
        mWebViewRenderProcessClient = client;
    }

    @Override
    public void onRenderProcessUnresponsive(@NonNull WebView view,
                @Nullable final android.webkit.WebViewRenderProcess renderer) {
        mWebViewRenderProcessClient.onRenderProcessUnresponsive(view,
                WebViewRenderProcessImpl.forFrameworkObject(renderer));
    }

    @Override
    public void onRenderProcessResponsive(@NonNull WebView view,
                @Nullable final android.webkit.WebViewRenderProcess renderer) {
        mWebViewRenderProcessClient.onRenderProcessResponsive(view,
                WebViewRenderProcessImpl.forFrameworkObject(renderer));
    }

    @Nullable
    public WebViewRenderProcessClient getFrameworkRenderProcessClient() {
        return mWebViewRenderProcessClient;
    }
}
