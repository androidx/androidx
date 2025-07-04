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

package androidx.webkit;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebStorage;

import androidx.annotation.RequiresFeature;
import androidx.annotation.UiThread;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebStorageAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Compatibility class for {@link android.webkit.WebStorage} that introduces
 * new methods to completely clear data.
 * <p>
 * WebStorage is tied to WebView {@link Profile} instances. If your app uses multiple profiles,
 * then you should ensure that you clear data for all relevant profiles.
 * <p>
 * A {@link android.webkit.WebStorage} instance can be obtained from
 * {@link android.webkit.WebStorage#getInstance()}, or from
 * {@link androidx.webkit.Profile#getWebStorage()} if your app is using multiple
 * WebView profiles.
 */
public final class WebStorageCompat {

    /**
     * Class is not intended to be instantiated.
     */
    private WebStorageCompat() {}

    /**
     * Delete all data stored by websites in the given WebStorage instance.
     * This includes network cache, cookies, and any JavaScript-readable storage.
     * <p>
     * This method will delete all data that was stored before being invoked.
     * Deletion is not an atomic operation, so it may also delete data that is stored
     * during deletion, but this is not guaranteed.
     *
     * @param instance     WebStorage instance to delete all data in.
     * @param executor     Executor to run the {@code doneCallback}.
     * @param doneCallback callback that will be invoked when deletion is complete.
     */
    @RequiresFeature(name = WebViewFeature.DELETE_BROWSING_DATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    public static void deleteBrowsingData(
            @NonNull WebStorage instance, @NonNull Executor executor,
            @NonNull Runnable doneCallback) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.DELETE_BROWSING_DATA;
        if (feature.isSupportedByWebView()) {
            getAdapter(instance).deleteBrowsingData(executor, doneCallback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

    }

    /**
     * Delete all data stored by websites in the given WebStorage instance.
     * <p>
     * This method functions as {@link #deleteBrowsingData(WebStorage, Executor, Runnable)},
     * but invokes the {@code doneCallback} on the UI thread.
     *
     * @see #deleteBrowsingData(WebStorage, Executor, Runnable)
     */
    @RequiresFeature(name = WebViewFeature.DELETE_BROWSING_DATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    public static void deleteBrowsingData(
            @NonNull WebStorage instance, @NonNull Runnable doneCallback) {
        deleteBrowsingData(instance, r -> new Handler(Looper.getMainLooper()).post(r),
                doneCallback);
    }

    /**
     * Delete data stored by web storage APIs for the specified domain in the
     * given WebStorage instance.
     * This includes network cache, cookies, and any JavaScript-readable storage.
     * <p>
     * For partitioned storage, this method will remove all data owned by the site,
     * as well as all storage for content embedded in the specified site, e.g. iframed content.
     * <p>
     * This method deletes storage for the <a
     * href="https://developer.mozilla.org/en-US/docs/Glossary/Site">site</a> part of the
     * input argument.
     * This means that if the input argument contains a subdomain (for example
     * {@code www.example.com}), then this method will delete everything belonging to
     * {@code example.com}.
     * <p>
     * This method will delete all data that was stored before being invoked.
     * Deletion is not an atomic operation, so it may also delete data that is stored
     * during deletion, but this is not guaranteed.
     *
     * @param instance     WebStorage instance to delete all data in.
     * @param site         The site to delete for. This can be a domain name, or a full URL.
     * @param executor     Executor to run the {@code doneCallback}.
     * @param doneCallback callback that will be invoked when deletion is complete.
     * @return The domain that was used for deletion. This will be the top-level domain part
     * of the {@code domain} parameter.
     * @throws IllegalArgumentException if unable to parse the {@code domain} as a domain name.
     */
    @RequiresFeature(name = WebViewFeature.DELETE_BROWSING_DATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    public static @NonNull String deleteBrowsingDataForSite(
            @NonNull WebStorage instance, @NonNull String site, @NonNull Executor executor,
            @NonNull Runnable doneCallback) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.DELETE_BROWSING_DATA;
        if (feature.isSupportedByWebView()) {
            return getAdapter(instance).deleteBrowsingDataForSite(site, executor, doneCallback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Delete data stored by web storage APIs for the specified domain in the
     * given WebStorage instance.
     * <p>
     * This method functions as
     * {@link #deleteBrowsingDataForSite(WebStorage, String, Executor, Runnable)},
     * but invokes the {@code doneCallback} on the UI thread.
     *
     * @see #deleteBrowsingDataForSite(WebStorage, String, Executor, Runnable)
     */
    @RequiresFeature(name = WebViewFeature.DELETE_BROWSING_DATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    public static @NonNull String deleteBrowsingDataForSite(
            @NonNull WebStorage instance, @NonNull String site, @NonNull Runnable doneCallback) {
        return deleteBrowsingDataForSite(instance, site,
                r -> new Handler(Looper.getMainLooper()).post(r), doneCallback);
    }


    private static WebStorageAdapter getAdapter(WebStorage webStorage) {
        return WebViewGlueCommunicator.getCompatConverter().convertWebStorage(webStorage);
    }
}
