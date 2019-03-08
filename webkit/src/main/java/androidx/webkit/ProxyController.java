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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ProxyControllerImpl;

import java.util.concurrent.Executor;

/**
 * Manages setting and clearing a process-specific override for the Android system-wide proxy
 * settings that govern network requests made by {@link android.webkit.WebView}.
 * <p>
 * WebView may make network requests in order to fetch content that is not otherwise read from
 * the file system or provided directly by application code. In this case by default the
 * system-wide Android network proxy settings are used to redirect requests to appropriate proxy
 * servers.
 * <p>
 * In the rare case that it is necessary for an application to explicitly specify its proxy
 * configuration, this API may be used to explicitly specify the proxy rules that govern WebView
 * initiated network requests.
 *
 * <p>
 * Example usage:
 * <pre class="prettyprint">
 * ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule("myproxy.com")
 *                                                    .addBypassRule("www.excluded.*")
 *                                                    .build();
 * Executor executor = ...
 * Runnable listener = ...
 * ProxyController.getInstance().setProxyOverride(proxyConfig, executor, listener);
 * ...
 * ProxyController.getInstance().clearProxyOverride(executor, listener);
 * </pre>
 */
public abstract class ProxyController {
    /**
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public ProxyController() {}

    /**
     * Returns the {@link ProxyController} instance.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#PROXY_OVERRIDE}.
     */
    @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    public static ProxyController getInstance() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            throw new UnsupportedOperationException("Proxy override not supported");
        }
        return LAZY_HOLDER.INSTANCE;
    }

    private static class LAZY_HOLDER {
        static final ProxyController INSTANCE = new ProxyControllerImpl();
    }

    /**
     * Sets {@link ProxyConfig} which will be used by all WebViews in the app. URLs that match
     * patterns in the bypass list will not be directed to any proxy. Instead, the request will be
     * made directly to the origin specified by the URL. Network connections are not guaranteed to
     * immediately use the new proxy setting; wait for the listener before loading a page. This
     * listener will be called in the provided executor.
     *
     * <p class="note"><b>Note:</b> calling setProxyOverride will cause any existing system wide
     * setting to be ignored.
     *
     * @param proxyConfig Proxy config to be applied
     * @param executor Executor for the listener to be executed in
     * @param listener Listener called when the proxy setting change has been applied
     *
     * @throws IllegalArgumentException If the proxyConfig is invalid
     */
    public abstract void setProxyOverride(@NonNull ProxyConfig proxyConfig,
            @NonNull Executor executor, @NonNull Runnable listener);

    /**
     * Clears the proxy settings. Network connections are not guaranteed to immediately use the
     * new proxy setting; wait for the listener before loading a page. This listener will be called
     * in the provided executor.
     *
     * @param executor Executor for the listener to be executed in
     * @param listener Listener called when the proxy setting change has been applied
     */
    public abstract void clearProxyOverride(@NonNull Executor executor,
            @NonNull Runnable listener);
}
