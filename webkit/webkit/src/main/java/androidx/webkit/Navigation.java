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

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebResourceErrorImpl;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.WebViewNavigationBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * The Navigation instance passed by {@link NavigationListener}.
 * <p>
 * The same object will be used by the relevant callbacks for the same navigation,
 * allowing the instance itself to be used as a key/ID to connect the callbacks for
 * the same navigation through {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * <p>
 * The return values of {@link #wasInitiatedByPage()}, {@link #isReload()}, {@link #isHistory()},
 * {@link #isBack()}, {@link #isForward()} and {@link #isRestore()} are constant for a given
 * Navigation. For the other methods:
 * <ul>
 *     <li>{@link #getPage()}, {@link #didCommit()} and {@link #didCommitErrorPage()} will only
 *     change when {@link NavigationListener#onNavigationCompleted(Navigation)} is called.</li>
 *     <li>{@link #getUrl()} will only change when
 *     {@link NavigationListener#onNavigationRedirected(Navigation)} is called.</li>
 * </ul>
 */
public final class Navigation {
    private final @NonNull WebViewNavigationBoundaryInterface mNavigationImpl;
    Page mPage;

    private Navigation(@NonNull WebViewNavigationBoundaryInterface navigationImpl) {
        mNavigationImpl = navigationImpl;
    }

    /**
     * Factory method that returns the Navigation associated with the given invocationHandler.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull Navigation forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        WebViewNavigationBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return (Navigation) Objects.requireNonNull(boundaryInterface.getOrCreatePeer(
                () -> new Navigation(boundaryInterface)));
    }

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
    public @Nullable Page getPage() {
        if (mNavigationImpl.getPage() == null) return null;
        // Once the Page is non-null, it won't change so there's no need to do an extra casting.
        if (mPage == null) {
            mPage = Page.forInvocationHandler(mNavigationImpl.getPage());
        }
        return mPage;
    }

    /**
     * Returns the URL of this navigation.
     *
     * @return The URL of this navigation as a String.
     */
    public @NonNull String getUrl() {
        return mNavigationImpl.getUrl();
    }

    /**
     * Indicates whether the navigation is initiated by the page/renderer (e.g., link clicks, JS
     * script)
     * instead of the browser/app (e.g., loadUrl calls).
     *
     * @return True if page-initiated, false otherwise.
     */
    public boolean wasInitiatedByPage() {
        return mNavigationImpl.wasInitiatedByPage();
    }

    /**
     * Indicates whether the navigation is a same-document navigation.
     *
     * @return True if same-document, false otherwise.
     */
    public boolean isSameDocument() {
        return mNavigationImpl.isSameDocument();
    }

    /**
     * Indicates whether the navigation is a reload navigation.
     *
     * @return True if reload, false otherwise.
     */
    public boolean isReload() {
        return mNavigationImpl.isReload();
    }

    /**
     * Indicates whether the navigation is a history navigation.
     *
     * @return True if history, false otherwise.
     */
    public boolean isHistory() {
        return mNavigationImpl.isHistory();
    }

    /**
     * Indicates whether the navigation is a history back navigation.
     *
     * @return True if back navigation, false otherwise.
     */
    public boolean isBack() {
        return mNavigationImpl.isBack();
    }

    /**
     * Indicates whether the navigation is a history forward navigation.
     *
     * @return True if forward navigation, false otherwise.
     */
    public boolean isForward() {
        return mNavigationImpl.isForward();
    }

    /**
     * Indicates whether the navigation committed (i.e., did not get aborted/return 204/etc).
     *
     * @return True if committed, false otherwise.
     */
    public boolean didCommit() {
        return mNavigationImpl.didCommit();
    }

    /**
     * Indicates whether the navigation committed an error page.
     *
     * @return True if an error page was committed, false otherwise.
     */
    public boolean didCommitErrorPage() {
        return mNavigationImpl.didCommitErrorPage();
    }

    /**
     * Returns the status code received by the navigation.
     *
     * @return The HTTP status code.
     */
    public int getStatusCode() {
        return mNavigationImpl.getStatusCode();
    }

    /**
     * Indicates whether the navigation is a restore navigation after calling
     * {@link WebView#restoreState(Bundle)}.
     *
     * @return True if session restore, false otherwise.
     */
    public boolean isRestore() {
        return mNavigationImpl.isRestore();
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
            if (mNavigationImpl.getWebResourceError() == null) return null;
            return new WebResourceErrorImpl(mNavigationImpl.getWebResourceError());
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
