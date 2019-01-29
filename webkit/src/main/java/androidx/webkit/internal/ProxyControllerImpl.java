/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.ProxyControllerBoundaryInterface;

import java.util.concurrent.Executor;

/**
 * Implementation of {@link ProxyController}.
 */
public class ProxyControllerImpl extends ProxyController {
    private ProxyControllerBoundaryInterface mBoundaryInterface;

    @Override
    public void setProxyOverride(@NonNull ProxyConfig proxyConfig, @NonNull Executor executor,
            @NonNull Runnable listener) {
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webViewFeature.isSupportedByWebView()) {
            getBoundaryInterface().setProxyOverride(
                    proxyConfig.proxyRules().toArray(new String[0][]),
                    proxyConfig.bypassRules().toArray(new String[0]), listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void clearProxyOverride(@NonNull Executor executor, @NonNull Runnable listener) {
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.PROXY_OVERRIDE);
        if (webViewFeature.isSupportedByWebView()) {
            getBoundaryInterface().clearProxyOverride(listener, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private ProxyControllerBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            mBoundaryInterface = WebViewGlueCommunicator.getFactory().getProxyController();
        }
        return mBoundaryInterface;
    }
}
