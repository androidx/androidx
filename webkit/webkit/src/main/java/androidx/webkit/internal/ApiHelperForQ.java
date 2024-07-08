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

import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewRenderProcess;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewRenderProcessClient;

import java.util.concurrent.Executor;

/**
 * Utility class to use new APIs that were added in Q (API level 29).
 * These need to exist in a separate class so that Android framework can successfully verify
 * classes without encountering the new APIs.
 */
@RequiresApi(Build.VERSION_CODES.Q)
public class ApiHelperForQ {
    private ApiHelperForQ() {
    }

    /**
     * @see WebSettings#setForceDark(int)
     * @deprecated in 31
     */
    @Deprecated
    public static void setForceDark(@NonNull WebSettings webSettings, int forceDark) {
        webSettings.setForceDark(forceDark);
    }

    /**
     * @see WebSettings#getForceDark()
     * @deprecated in 31
     */
    @Deprecated
    public static int getForceDark(@NonNull WebSettings webSettings) {
        return webSettings.getForceDark();
    }

    /**
     * @see WebView#getWebViewRenderProcess()
     */
    @Nullable
    public static WebViewRenderProcess getWebViewRenderProcess(@NonNull WebView webView) {
        return webView.getWebViewRenderProcess();
    }

    /**
     * @see WebViewRenderProcess#terminate()
     */
    public static boolean terminate(@NonNull WebViewRenderProcess webViewRenderProcess) {
        return webViewRenderProcess.terminate();
    }

    /**
     * @see WebView#setWebViewRenderProcessClient(Executor,
     *          android.webkit.WebViewRenderProcessClient)
     */
    public static void setWebViewRenderProcessClient(@NonNull WebView webView,
            @NonNull Executor executor,
            @Nullable WebViewRenderProcessClient client) {
        WebViewRenderProcessClientFrameworkAdapter clientAdapter =
                client != null ? new WebViewRenderProcessClientFrameworkAdapter(client) : null;
        webView.setWebViewRenderProcessClient(executor, clientAdapter);
    }

    /**
     * @see WebView#setWebViewRenderProcessClient(android.webkit.WebViewRenderProcessClient)
     */
    public static void setWebViewRenderProcessClient(@NonNull WebView webView,
            @Nullable WebViewRenderProcessClient client) {
        WebViewRenderProcessClientFrameworkAdapter clientAdapter =
                client != null ? new WebViewRenderProcessClientFrameworkAdapter(client) : null;
        webView.setWebViewRenderProcessClient(clientAdapter);
    }

    /**
     * @see WebView#getWebViewRenderProcessClient()
     */
    @Nullable
    public static android.webkit.WebViewRenderProcessClient getWebViewRenderProcessClient(
            @NonNull WebView webView) {
        return webView.getWebViewRenderProcessClient();
    }
}
