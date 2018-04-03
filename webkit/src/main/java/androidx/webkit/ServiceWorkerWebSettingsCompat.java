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

import android.webkit.WebSettings;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Manages settings state for all Service Workers. These settings are not tied to
 * the lifetime of any WebView because service workers can outlive WebView instances.
 * The settings are similar to {@link WebSettings} but only settings relevant to
 * Service Workers are supported.
 */
public abstract class ServiceWorkerWebSettingsCompat {
    /**
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ServiceWorkerWebSettingsCompat() {}

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            WebSettings.LOAD_DEFAULT,
            WebSettings.LOAD_CACHE_ELSE_NETWORK,
            WebSettings.LOAD_NO_CACHE,
            WebSettings.LOAD_CACHE_ONLY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheMode {}

    /**
     *
     * Overrides the way the cache is used, see {@link WebSettings#setCacheMode}.
     *
     * @param mode the mode to use. One of {@link WebSettings#LOAD_DEFAULT},
     * {@link WebSettings#LOAD_CACHE_ELSE_NETWORK}, {@link WebSettings#LOAD_NO_CACHE}
     * or {@link WebSettings#LOAD_CACHE_ONLY}. The default value is
     * {@link WebSettings#LOAD_DEFAULT}.
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_CACHE_MODE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void setCacheMode(@CacheMode int mode);

    /**
     *
     * Gets the current setting for overriding the cache mode.
     *
     * @return the current setting for overriding the cache mode
     * @see #setCacheMode
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_CACHE_MODE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract @CacheMode int getCacheMode();

    /**
     *
     * Enables or disables content URL access from Service Workers, see
     * {@link WebSettings#setAllowContentAccess}.
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void setAllowContentAccess(boolean allow);

    /**
     *
     * Gets whether Service Workers support content URL access.
     *
     * @see #setAllowContentAccess
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract boolean getAllowContentAccess();

    /**
     *
     * Enables or disables file access within Service Workers, see
     * {@link WebSettings#setAllowFileAccess}.
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_FILE_ACCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void setAllowFileAccess(boolean allow);

    /**
     *
     * Gets whether Service Workers support file access.
     *
     * @see #setAllowFileAccess
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_FILE_ACCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract boolean getAllowFileAccess();

    /**
     *
     * Sets whether Service Workers should not load resources from the network,
     * see {@link WebSettings#setBlockNetworkLoads}.
     *
     * @param flag {@code true} means block network loads by the Service Workers
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void setBlockNetworkLoads(boolean flag);

    /**
     *
     * Gets whether Service Workers are prohibited from loading any resources from the network.
     *
     * @return {@code true} if the Service Workers are not allowed to load any resources from the
     * network
     * @see #setBlockNetworkLoads
     *
     */
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract boolean getBlockNetworkLoads();
}
