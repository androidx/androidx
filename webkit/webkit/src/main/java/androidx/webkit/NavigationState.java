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

import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.AnyThread;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebResourceErrorImpl;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.WebViewNavigationStateBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.Collections;
import java.util.Map;

/**
 * This class serves as an immutable snapshot of a {@link Navigation} instance at a certain point
 * in time, captured every time a callback (e.g. {@link NavigationListener#onNavigationStarted}) is
 * called.
 * <p>
 * Note: Do not use {@link NavigationState} as a key to connect callbacks, use {@link Navigation}
 * instead from {@link NavigationState#getNavigation()}.
 */
@AnyThread
public final class NavigationState {

    private final @NonNull WebViewNavigationStateBoundaryInterface mBoundaryInterface;

    /**
     * Factory method that returns the PageState associated with the given invocationHandler.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull NavigationState forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        WebViewNavigationStateBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationStateBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return new NavigationState(boundaryInterface);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public NavigationState(@NonNull WebViewNavigationStateBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Returns the URL of the Navigation's state at a point in time
     *
     * @return The URL of this NavigationState as a String.
     */
    public @NonNull String getUrl() {
        return mBoundaryInterface.getUrl();
    }

    /**
     * Indicates whether the navigation is initiated by the page/renderer (e.g., link clicks, JS
     * script)
     * instead of the browser/app (e.g., loadUrl calls).
     *
     * @return True if page-initiated, false otherwise.
     */
    public boolean wasInitiatedByPage() {
        return mBoundaryInterface.wasInitiatedByPage();
    }

    /**
     * Indicates whether the navigation is a same-document navigation.
     *
     * @return True if same-document, false otherwise.
     */
    public boolean isSameDocument() {
        return mBoundaryInterface.isSameDocument();
    }

    /**
     * Indicates whether the navigation is a reload navigation.
     *
     * @return True if reload, false otherwise.
     */
    public boolean isReload() {
        return mBoundaryInterface.isReload();
    }

    /**
     * Indicates whether the navigation is a history navigation.
     *
     * @return True if history, false otherwise.
     */
    public boolean isHistory() {
        return mBoundaryInterface.isHistory();
    }

    /**
     * Indicates whether the navigation is a restore navigation after calling
     * {@link WebView#restoreState(Bundle)}.
     *
     * @return True if session restore, false otherwise.
     */
    public boolean isRestore() {
        return mBoundaryInterface.isRestore();
    }

    /**
     * Indicates whether the navigation is a history back navigation.
     *
     * @return True if back navigation, false otherwise.
     */
    public boolean isBack() {
        return mBoundaryInterface.isBack();
    }

    /**
     * Indicates whether the navigation is a history forward navigation.
     *
     * @return True if forward navigation, false otherwise.
     */
    public boolean isForward() {
        return mBoundaryInterface.isForward();
    }

    /**
     * Indicates whether the navigation committed (i.e., did not get aborted/return 204/etc).
     * <p>
     * Note: This will return true if {@link #didCommitErrorPage()} returns true.
     *
     * @return True if committed, false otherwise.
     */
    public boolean didCommit() {
        return mBoundaryInterface.didCommit();
    }

    /**
     * Indicates whether the navigation committed an error page.
     *
     * @return True if an error page was committed, false otherwise.
     */
    public boolean didCommitErrorPage() {
        return mBoundaryInterface.didCommitErrorPage();
    }

    /**
     * Returns the status code received by the navigation.
     *
     * @return The HTTP status code.
     */
    public int getStatusCode() {
        return mBoundaryInterface.getStatusCode();
    }

    /**
     * Returns the timestamp when this navigation was initiated.
     * <p>
     * The returned timestamp represents the time elapsed since device boot in milliseconds,
     * excluding time spent in deep sleep. This timebase is equivalent to
     * {@link android.os.SystemClock#uptimeMillis()}.
     *
     * @return The start time of the navigation in uptime milliseconds.
     */
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_NAVIGATION_START_UPTIME_MILLIS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public long getNavigationStartUptimeMillis() {
        ApiFeature.NoFramework feature =
                WebViewFeatureInternal.NAVIGATION_GET_NAVIGATION_START_UPTIME_MILLIS;
        if (feature.isSupportedByWebView()) {
            return mBoundaryInterface.getNavigationStartUptimeMillis();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns the PageState that the navigation commits into.
     * <p>
     * Note: This method will return {@code null} if the navigation had not committed a page when
     * this snapshot was taken, or never commits one (204/download).
     * If the navigation successfully commits a page, this method will return the corresponding
     * {@link PageState} object for the page. This could wrap a newly created {@link Page} or a
     * previously seen {@link Page} in the case of BFCache (Back/Forward Cache).
     * <p>
     * Note: Once this method returns a non-null {@link PageState} object for a
     * specific navigation's {@link Page}, {@link PageState#getPage} will always return the same
     * {@link Page} object for that navigation.
     *
     * @return The {@link PageState} object, or {@code null} if the navigation had not committed a
     * Page when the snapshot was taken.
     * (e.g., <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/204">204</a>
     * /download).
     */
    @Nullable
    public PageState getPageState() {
        InvocationHandler pageStateInvocationHandler = mBoundaryInterface.getPageState();
        if (pageStateInvocationHandler == null) {
            return null;
        }
        return PageState.forInvocationHandler(pageStateInvocationHandler);
    }

    /**
     * Navigation error information for the navigation load.
     *
     * @return The {@link WebResourceErrorCompat} object, or {@code null} if there is no
     * error for this navigation.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#NAVIGATION_GET_WEB_RESOURCE_ERROR} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_WEB_RESOURCE_ERROR,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @Nullable WebResourceErrorCompat getWebResourceError() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_WEB_RESOURCE_ERROR;
        if (feature.isSupportedByWebView()) {
            InvocationHandler webResourceErrorInvocationHandler =
                    mBoundaryInterface.getWebResourceError();
            if (webResourceErrorInvocationHandler == null) {
                return null;
            }
            return new WebResourceErrorImpl(webResourceErrorInvocationHandler);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns the HTTP response headers received for this navigation.
     * <p>
     * The response headers are not available until the navigation has completed, so this method
     * will return {@code null} until
     * {@link NavigationListener#onNavigationCompleted(NavigationState)} has been called for this
     * navigation.
     * <p>
     * The returned map will be empty if the navigation did not receive an HTTP response, for
     * example for same-document navigations, navigations to non-HTTP URLs (such as
     * {@code file://} or {@code about:blank}) or navigations that failed before a response was
     * received.
     * <p>
     * The {@code Set-Cookie} header is never included. Only the headers of the final response
     * are returned, the headers of any redirects are exposed via
     * {@link NavigationRedirectParameters#getResponseHeaders()} in
     * {@link NavigationListener#onNavigationRedirected(NavigationState, NavigationRedirectParameters)}
     *
     * @return An unmodifiable {@link Map} from header name to header value, or {@code null} if
     * the navigation has not completed yet.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#NAVIGATION_GET_RESPONSE_HEADERS} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_RESPONSE_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @SuppressWarnings("NullableCollection")
    public @Nullable Map<String, String> getResponseHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_RESPONSE_HEADERS;
        if (feature.isSupportedByWebView()) {
            Map<String, String> responseHeaders = mBoundaryInterface.getResponseHeaders();
            if (responseHeaders == null) return null;
            return Collections.unmodifiableMap(responseHeaders);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns the navigation linked to this NavigationState snapshot
     */
    public @NonNull Navigation getNavigation() {
        return Navigation.forInvocationHandler(mBoundaryInterface.getNavigation());
    }

}
