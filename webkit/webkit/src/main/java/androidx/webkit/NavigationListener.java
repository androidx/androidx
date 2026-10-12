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

import androidx.annotation.RestrictTo;

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
 *       <li>{@link #onFirstContentfulPaintMillis(Page, long)}</li>
 *       <li>{@link #onLargestContentfulPaintMillis(Page, long)}</li>
 *       <li>{@link #onPerformanceMarkMillis(Page, String, long)}</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public interface NavigationListener {
    /**
     * Called when a navigation starts, including same-document navigations.
     * <p>
     * Note: These navigation callbacks only fire for navigations happening on the main frame.
     *
     * @param navigation The Navigation object representing the started navigation.
     * @deprecated Use {@link NavigationListener#onNavigationStarted(NavigationState)} instead.
     */
    @Deprecated
    default void onNavigationStarted(@NonNull Navigation navigation) {

    }

    /**
     * Called when a navigation starts, including same-document navigations.
     * <p>
     * Note: These navigation callbacks only fire for navigations happening on the main frame.
     * <p>
     * Note: Calling super will call the old
     * {@link NavigationListener#onNavigationStarted(Navigation)}
     *
     * @param navigationState An immutable snapshot of the Navigation's state at the point when the
     * Navigation is started.
     */
    default void onNavigationStarted(@NonNull NavigationState navigationState) {
        onNavigationStarted(navigationState.getNavigation());
    }

    /**
     * Called when a navigation is redirected.
     *
     * @param navigation The Navigation object representing the redirected navigation.
     * @deprecated Use {@link
     * NavigationListener#onNavigationRedirected(NavigationState, NavigationRedirectParameters)}
     * instead.
     */
    @Deprecated
    default void onNavigationRedirected(@NonNull Navigation navigation) {

    }

    /**
     * Called when a navigation is redirected.
     *
     * @param navigationState An immutable snapshot of the Navigation's state at the point the
     * Navigation is redirected.
     * @param navigationRedirectParameters An object which exposes response headers for the callback
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default void onNavigationRedirected(
            @NonNull NavigationState navigationState,
            @NonNull NavigationRedirectParameters navigationRedirectParameters) {
        onNavigationRedirected(navigationState.getNavigation());
    }

    /**
     * Called when a navigation completes.
     * <p>
     * The navigation might not have actually committed (e.g., results in 204/download/cancelled),
     * but those that do they will have committed by this point.
     *
     * @param navigation The Navigation object representing the completed navigation.
     * @deprecated Use {@link NavigationListener#onNavigationCompleted(NavigationState)} instead.
     */
    @Deprecated
    default void onNavigationCompleted(@NonNull Navigation navigation) {

    }

    /**
     * Called when a navigation completes.
     * <p>
     * The navigation might not have actually committed (e.g., results in 204/download/cancelled),
     * but those that do they will have committed by this point.
     *
     * @param navigationState An immutable snapshot of the Navigation's state at the point the
     * Navigation is completed.
     */
    default void onNavigationCompleted(@NonNull NavigationState navigationState) {
        onNavigationCompleted(navigationState.getNavigation());
    }

    /**
     * Called when any Page is evicted/destroyed. This can occur immediately
     * on navigation, or later if the page is BFCached and subsequently evicted.
     *
     * @param page The Page that was evicted or destroyed.
     * @deprecated Use {@link NavigationListener#onPageDeleted(PageState)} instead.
     */
    @Deprecated
    default void onPageDeleted(@NonNull Page page) {

    }

    /**
     * Called when any Page is evicted/destroyed. This can occur immediately
     * on navigation, or later if the page is BFCached and subsequently evicted.
     *
     * @param pageState An immutable snapshot of the Page's state at the point the Page is deleted.
     */
    default void onPageDeleted(@NonNull PageState pageState) {
        onPageDeleted(pageState.getPage());
    }

    /**
     * Called when the {@code window.load} event is fired for the current page.
     *
     * @param page The Page for which the {@code window.load} event fired.
     * @deprecated Use {@link NavigationListener#onPageLoadEvent(PageState)} instead.
     */
    @Deprecated
    default void onPageLoadEvent(@NonNull Page page) {

    }

    /**
     * Called when the {@code window.load} event is fired for the current page.
     *
     * @param pageState An immutable snapshot of the Page's state at the point the Page is loaded.
     */
    default void onPageLoadEvent(@NonNull PageState pageState) {
        onPageLoadEvent(pageState.getPage());
    }

    /**
     * Called when the {@code DOMContentLoaded} event is fired for the current page.
     *
     * @param page The Page for which the {@code DOMContentLoaded} event fired.
     * @deprecated Use {@link NavigationListener#onPageDomContentLoadedEvent(PageState)} instead.
     */
    @Deprecated
    default void onPageDomContentLoadedEvent(@NonNull Page page) {

    }

    /**
     * Called when the {@code DOMContentLoaded} event is fired for the current page.
     *
     * @param pageState An immutable snapshot of the Page's state at the point the DOMContentLoaded
     * event is fired.
     */
    default void onPageDomContentLoadedEvent(@NonNull PageState pageState) {
        onPageDomContentLoadedEvent(pageState.getPage());
    }

    /**
     * Called when the page achieves "First Contentful Paint".
     *
     * <p>See <a href="https://web.dev/articles/fcp">First Contentful Paint (FCP)</a>
     * for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the First Contentful Paint occurred.
     * @param durationMillis Navigation to First Contentful Paint duration in milliseconds.
     * @deprecated Use {@link NavigationListener#onFirstContentfulPaintMillis(PageState, long)}
     * instead.
     */
    @Deprecated
    default void onFirstContentfulPaintMillis(@NonNull Page page, long durationMillis) {

    }

    /**
     * Called when the page achieves "First Contentful Paint".
     *
     * <p>See <a href="https://web.dev/articles/fcp">First Contentful Paint (FCP)</a>
     * for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param pageState           The Page's state at which the First Contentful Paint occurred.
     * @param durationMillis Navigation to First Contentful Paint duration in milliseconds.
     */
    default void onFirstContentfulPaintMillis(@NonNull PageState pageState, long durationMillis) {
        onFirstContentfulPaintMillis(pageState.getPage(), durationMillis);
    }

    /**
     * Called when the page fires a "Largest Contentful Paint" event.
     *
     * <p>See <a href="https://web.dev/articles/lcp">Largest Contentful Paint (LCP)</a>
     * for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the Largest Contentful Paint occurred.
     * @param durationMillis Navigation to Largest Contentful Paint duration in milliseconds.
     * @deprecated Use {@link NavigationListener#onLargestContentfulPaintMillis(PageState, long)}
     * instead.
     */
    @Deprecated
    default void onLargestContentfulPaintMillis(@NonNull Page page, long durationMillis) {

    }

    /**
     * Called when the page fires a "Largest Contentful Paint" event.
     *
     * <p>See <a href="https://web.dev/articles/lcp">Largest Contentful Paint (LCP)</a>
     * for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param pageState           The Page's state at which the Largest Contentful Paint occurred.
     * @param durationMillis Navigation to Largest Contentful Paint duration in milliseconds.
     */
    default void onLargestContentfulPaintMillis(@NonNull PageState pageState, long durationMillis) {
        onLargestContentfulPaintMillis(pageState.getPage(), durationMillis);
    }

    /**
     * Called when the page registers a performance mark.
     *
     * <p>See <a href="https://developer.mozilla.org/en-US/docs/Web/API/Performance/mark">
     *     Performance: mark() method</a> for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param page           The Page for which the performance mark was registered.
     * @param markName       The name of the performance mark.
     * @param markTimeMillis The time of the performance mark in milliseconds since
     *                       Performance.timeOrigin.
     * @deprecated Use {@link NavigationListener#onPerformanceMarkMillis(PageState, String, long)}
     * instead.
     */
    @Deprecated
    default void onPerformanceMarkMillis(@NonNull Page page,
            @NonNull String markName, long markTimeMillis) {

    }

    /**
     * Called when the page registers a performance mark.
     *
     * <p>See <a href="https://developer.mozilla.org/en-US/docs/Web/API/Performance/mark">
     *     Performance: mark() method</a> for a definition.</p>
     *
     * Note: Delivery of this event may be delayed due to batching with other events,
     * so it may not be sent in real-time.
     *
     * <p>This method is called only if
     * {@link WebViewFeature#NAVIGATION_LISTENER} is supported.
     * You can check whether that flag is supported using
     * {@link WebViewFeature#isFeatureSupported(String)}.
     *
     * @param pageState      The Page's state at which the performance mark was registered.
     * @param markName       The name of the performance mark.
     * @param markTimeMillis The time of the performance mark in milliseconds since
     *                       Performance.timeOrigin.
     */
    default void onPerformanceMarkMillis(
            @NonNull PageState pageState, @NonNull String markName, long markTimeMillis) {
        onPerformanceMarkMillis(pageState.getPage(), markName, markTimeMillis);
    }
}
