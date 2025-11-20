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

import android.annotation.SuppressLint;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Result object associated with
 * {@link androidx.webkit.WebViewCompat.WebViewStartUpCallback#onSuccess(WebViewStartUpResult)}.
 *
 */
@WebViewCompat.ExperimentalAsyncStartUp
public interface WebViewStartUpResult {
    /**
     * The total time WebView startup took on the UI thread.
     * <p>
     * The return value will be {@code null} if the underlying WebView version doesn't support this
     * method.
     */
    @Nullable
    @SuppressLint("AutoBoxing")
    Long getTotalTimeInUiThreadMillis();

    /**
     * The maximum time taken by a task among all the tasks associated with WebView startup in the
     * UI thread.
     * <p>
     * The return value will be {@code null} if the underlying WebView version doesn't support this
     * method.
     */
    @SuppressLint("AutoBoxing")
    @Nullable Long getMaxTimePerTaskInUiThreadMillis();

    /**
     * Code locations where WebView startup completely blocked the UI thread.
     * <p>
     * This is as a debug tool to enable apps to catch code locations where WebView is suboptimally
     * started up even when
     * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
     * is used.
     * <p>
     * Example code location: A `new WebView()` call on the Android main looper before calling
     * any other API that triggers WebView startup.
     * <p>
     * The list will be chronologically ordered based on the time of creation of the stacktrace.
     * <p>
     * The return value will be {@code null} if the underlying WebView version doesn't support this
     * method.
     */
    @SuppressLint("NullableCollection")
    @Nullable List<StartUpLocation> getUiThreadBlockingStartUpLocations();

    /**
     * Code locations where WebView startup blocked a non-UI thread.
     * <p>
     * This is as a debug tool to enable apps to catch code locations where WebView is started up or
     * is about to be started up such that it blocks a non-UI thread.
     * <p>
     * Example code location: A `WebSettings.getDefaultUserAgent()` call on a background thread
     * before calling any other API that triggers WebView startup.
     * <p>
     * The list will be chronologically ordered based on the time of creation of the stacktrace.
     * <p>
     * The return value will be {@code null} if the underlying WebView version doesn't support this
     * method.
     */
    @SuppressLint("NullableCollection")
    @Nullable List<StartUpLocation> getNonUiThreadBlockingStartUpLocations();
}

