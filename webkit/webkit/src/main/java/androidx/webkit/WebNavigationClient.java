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

import androidx.annotation.RequiresOptIn;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Page identification and lifecycle APIs. This class provides callbacks to identify the
 * different stages of navigation.
 * For more information about the navigation lifecycle, please see the
 * <a href="https://docs.google.com/presentation/d/1YVqDmbXI0cllpfXD7TuewiexDNZYfwk6fRdmoXJbBlM">Life of a Navigation Presentation</a>.
 * <p>
 * Note: These navigation callbacks only fire for navigations happening on the main frame.
 * <p>
 * Navigation lifecycle events:
 * <ul>
 * <li>{@code onNavigationStarted}</li>
 * <li>Potentially zero or more {@code onNavigationRedirected} events</li>
 * <li>{@code onNavigationCompleted}</li>
 * <li>If the navigation commits and is not a same-document navigation, potentially any
 * combination and order of zero or one of each of:
 * <ul>
 * <li>{@code onPageLoadEvent}</li>
 * <li>{@code onPageDomContentLoaded}</li>
 * <li>{@code onFirstContentfulPaint}</li>
 * </ul>
 * </li>
 * </ul>
 */
@WebNavigationClient.ExperimentalNavigationCallback
public interface WebNavigationClient {
    /**
     * Fired when a navigation starts, including same-document navigations.
     * <p>
     * Note: These navigation callbacks only fire for navigations happening on the main frame.
     *
     * @param navigation The Navigation object representing the started navigation.
     */
    void onNavigationStarted(@NonNull Navigation navigation);

    /**
     * Fired when a navigation is redirected.
     *
     * @param navigation The Navigation object representing the redirected navigation.
     */
    void onNavigationRedirected(@NonNull Navigation navigation);

    /**
     * Fired when a navigation completes.
     * <p>
     * The navigation might not have actually committed (e.g., results in 204/download/cancelled).
     *
     * @param navigation The Navigation object representing the completed navigation.
     */
    void onNavigationCompleted(@NonNull Navigation navigation);

    /**
     * Fired when any Page is evicted/destroyed. This can occur immediately
     * on navigation, or later if the page is BFCached and subsequently evicted.
     *
     * @param page The Page that was evicted or destroyed.
     */
    void onPageDeleted(@NonNull Page page);

    /**
     * Fired when the `window.load` event is fired for the current page.
     *
     * @param page The Page for which the `window.load` event fired.
     */
    void onPageLoadEventFired(@NonNull Page page);

    /**
     * Fired when the `DOMContentLoaded` event is fired for the current page.
     *
     * @param page The Page for which the `DOMContentLoaded` event fired.
     */
    void onPageDomContentLoadedEventFired(@NonNull Page page);

    /**
     * Fired when the page achieves "First Contentful Paint".
     *
     * <p>See <a href="https://web.dev/articles/fcp">First Contentful Paint (FCP)</a>
     * for a definition.</p>
     *
     * @param page The Page for which the First Contentful Paint occurred.
     */
    void onFirstContentfulPaint(@NonNull Page page);

    /**
     * Denotes {@link Navigation}, {@link Page} and {@link WebNavigationClient} API surfaces are
     * experimental.
     * <p>
     * It may change without warning and should not be relied upon for non-experimental purposes.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalNavigationCallback {
    }
}
