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

package androidx.webkit;

import android.webkit.WebView;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * Receives the downloads started in a {@link WebView}, which WebView then carries out on the
 * app's behalf.
 *
 * <p>Unlike {@link android.webkit.DownloadListener}, which only receives the metadata of a
 * download and leaves the app to request the file again, a {@link DownloadHandler} receives a
 * {@link WebDownload} that WebView downloads itself, using the request it already made. Downloads
 * that depend on the state of that request, such as POST requests, cookies, authentication or
 * {@code blob:} URLs, therefore work as expected. The app decides where the file is saved with
 * {@link WebDownload#saveTo}, or rejects the download with {@link WebDownload#cancel()}.
 *
 * <p>While a {@link DownloadHandler} is set on a {@link WebView}, the
 * {@link android.webkit.DownloadListener} set with
 * {@link WebView#setDownloadListener(android.webkit.DownloadListener)} is not called for that
 * {@link WebView}. Other {@link WebView} instances are not affected.
 *
 * @see WebViewCompat#setDownloadHandler(WebView, DownloadHandler)
 * @see WebViewCompat#setDownloadHandler(WebView, Executor, DownloadHandler)
 * @see WebViewCompat#clearDownloadHandler(WebView)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WebViewCompat.ExperimentalDownloadHandler
public interface DownloadHandler {
    /**
     * Called once for each download started in {@code webView}.
     *
     * <p>This method is called on the {@link Executor} passed to
     * {@link WebViewCompat#setDownloadHandler(WebView, Executor, DownloadHandler)}, or on the UI
     * thread if the handler was set with
     * {@link WebViewCompat#setDownloadHandler(WebView, DownloadHandler)}. As usual,
     * {@code webView} must only be accessed on the UI thread.
     *
     * <p>The app must eventually call either {@link WebDownload#saveTo} or
     * {@link WebDownload#cancel()} on {@code download}. It may return from this method first and
     * decide later, for example after asking the user where to save the file.
     * Until then, WebView keeps downloading into a temporary file that it manages and deletes
     * itself. If {@code download} becomes unreachable before either method is called, the
     * download is eventually cancelled.
     *
     * @param webView  the {@link WebView} that started the download.
     * @param download the download.
     */
    void onDownloadRequested(@NonNull WebView webView, @NonNull WebDownload download);
}
