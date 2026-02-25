/*
 * Copyright 2025 The Android Open Source Project
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

import org.jspecify.annotations.NonNull;

/**
 * Page identification and lifecycle APIs.
 *
 * <p>This class provides callbacks to identify the different stages of navigation.
 * For more information about the navigation lifecycle, please see the
 * <a href="https://docs.google.com/presentation/d/1YVqDmbXI0cllpfXD7TuewiexDNZYfwk6fRdmoXJbBlM">Life of a Navigation Presentation</a>.
 *
 * <p>Note: These navigation callbacks only fire for navigations happening on the main frame.
 *
 * <p>Navigation lifecycle events:
 * <ul>
 *   <li>{@link #onNavigationStarted(Navigation)}</li>
 *   <li>Potentially zero or more {@link #onNavigationRedirected(Navigation)} events</li>
 *   <li>{@link #onNavigationCompleted(Navigation)}</li>
 *   <li>If the navigation commits and is not a same-document navigation, potentially any
 * combination and order of zero or one of each of:
 *     <ul>
 *       <li>{@link #onPageLoadEvent(Page)}</li>
 *       <li>{@link #onPageDomContentLoadedEvent(Page)}</li>
 *       <li>{@link #onFirstContentfulPaint(Page, long)}</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@WebNavigationClient.ExperimentalNavigationCallback
public interface NavigationListener {
    /**
     * Called when a navigation starts, including same-document navigations.
     * <p>
     * Note: These navigation callbacks only fire for navigations happening on the main frame.
     *
     * @param navigation The Navigation object representing the started navigation.
     */
    default void onNavigationStarted(@NonNull Navigation navigation) {

    }

    /**
     * Called when a navigation is redirected.
     *
     * @param navigation The Navigation object representing the redirected navigation.
     */
    default void onNavigationRedirected(@NonNull Navigation navigation) {

    }

    /**
     * Called when a navigation completes.
     * <p>
     * The navigation might not have actually committed (e.g., results in 204/download/cancelled),
     * but those that do they will have committed by this point.
     *
     * @param navigation The Navigation object representing the completed navigation.
     */
    default void onNavigationCompleted(@NonNull Navigation navigation) {

    }

    /**
     * Called when any Page is evicted/destroyed. This can occur immediately
     * on navigation, or later if the page is BFCached and subsequently evicted.
     *
     * @param page The Page that was evicted or destroyed.
     */
    default void onPageDeleted(@NonNull Page page) {

    }

    /**
     * Called when the {@code window.load} event is fired for the current page.
     *
     * @param page The Page for which the {@code window.load} event fired.
     */
    default void onPageLoadEvent(@NonNull Page page) {

    }

    /**
     * Called when the {@code DOMContentLoaded} event is fired for the current page.
     *
     * @param page The Page for which the {@code DOMContentLoaded} event fired.
     */
    default void onPageDomContentLoadedEvent(@NonNull Page page) {

    }

    /**
     * Called when the page achieves "First Contentful Paint".
     *
     * <p>See <a href="https://web.dev/articles/fcp">First Contentful Paint (FCP)</a>
     * for a definition.</p>
     *
     * @param page       The Page for which the First Contentful Paint occurred.
     * @param loadTimeUs Navigation to First Contentful Paint load time in microseconds.
     */
    @SuppressLint("LambdaLast")
    default void onFirstContentfulPaint(@NonNull Page page, long loadTimeUs) {

    }

    /**
     * Called when the page achieves "First Contentful Paint".
     *
     * <p>See <a href="https://web.dev/articles/fcp">First Contentful Paint (FCP)</a>
     * for a definition.</p>
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER_V2} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the First Contentful Paint occurred.
     * @param durationMillis Navigation to First Contentful Paint duration in milliseconds.
     */
    @SuppressLint("LambdaLast")
    default void onFirstContentfulPaintMillis(@NonNull Page page, long durationMillis) {

    }

    /**
     * Called when the page fires a "Largest Contentful Paint" event.
     *
     * <p>See <a href="https://web.dev/articles/lcp">Largest Contentful Paint (LCP)</a>
     * for a definition.</p>
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER_V2} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the Largest Contentful Paint occurred.
     * @param durationMillis Navigation to Largest Contentful Paint duration in milliseconds.
     */
    @SuppressLint("LambdaLast")
    default void onLargestContentfulPaintMillis(@NonNull Page page, long durationMillis) {

    }

    /**
     * Called when the page registers a performance mark.
     *
     * <p>See <a href="https://developer.mozilla.org/en-US/docs/Web/API/Performance/mark">
     *     Performance: mark() method</a> for a definition.</p>
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER_V2} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the performance mark was registered.
     * @param markName       The name of the performance mark.
     * @param markTimeMillis The time of the performance mark in milliseconds since
     *                       Performance.timeOrigin.
     */
    @SuppressLint("LambdaLast")
    default void onPerformanceMarkMillis(@NonNull Page page,
            @NonNull String markName, long markTimeMillis) {

    }
}
