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

import android.os.Bundle;
import android.webkit.WebView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The Navigation instance passed by the navigation callbacks.
 * <p>
 * The same object will be used by the relevant callbacks for the same navigation,
 * allowing the instance itself to be used as a key/ID to connect the callbacks for
 * the same navigations.
 */
@WebNavigationClient.ExperimentalNavigationCallback
public interface Navigation {
    /**
     * Returns the Page that the navigation commits into.
     * <p>
     * Note: This method will initially return {@code null} when navigation begins.
     * If the navigation successfully commits a page, this method will return the corresponding
     * {@link Page} object. This could be a newly created {@link Page} or a previously seen
     * {@link Page} in the case of BFCache (Back/Forward Cache).
     * <p>
     * Note: Once this method returns a non-null {@link Page} object for a
     * specific navigation, it will always return the same {@link Page} object for that navigation.
     * <p>
     *
     * @return The {@link Page} object, or {@code null} if the navigation does not commit or
     * result in a Page
     * (e.g., <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/204">204</a>
     * /download).
     */
    @Nullable
    Page getPage();

    /**
     * Returns the URL of this navigation.
     *
     * @return The URL of this navigation as a String.
     */
    @NonNull
    String getUrl();

    /**
     * Indicates whether the navigation is initiated by the page/renderer (e.g., link clicks, JS
     * script)
     * instead of the browser/app (e.g., loadUrl calls).
     *
     * @return True if page-initiated, false otherwise.
     */
    boolean wasInitiatedByPage();

    /**
     * Indicates whether the navigation is a same-document navigation.
     *
     * @return True if same-document, false otherwise.
     */
    boolean isSameDocument();

    /**
     * Indicates whether the navigation is a reload navigation.
     *
     * @return True if reload, false otherwise.
     */
    boolean isReload();

    /**
     * Indicates whether the navigation is a history navigation.
     *
     * @return True if history, false otherwise.
     */
    boolean isHistory();

    /**
     * Indicates whether the navigation is a history back navigation.
     *
     * @return True if back navigation, false otherwise.
     */
    boolean isBack();

    /**
     * Indicates whether the navigation is a history forward navigation.
     *
     * @return True if forward navigation, false otherwise.
     */
    boolean isForward();

    /**
     * Indicates whether the navigation committed (i.e., did not get aborted/return 204/etc).
     *
     * @return True if committed, false otherwise.
     */
    boolean didCommit();

    /**
     * Indicates whether the navigation committed an error page.
     *
     * @return True if an error page was committed, false otherwise.
     */
    boolean didCommitErrorPage();

    /**
     * Returns the status code received by the navigation.
     *
     * @return The HTTP status code.
     */
    int getStatusCode();

    /**
     * Indicates whether the navigation is a restore navigation after calling
     * {@link WebView#restoreState(Bundle)}.
     *
     * @return True if session restore, false otherwise.
     */
    boolean isRestore();

}
