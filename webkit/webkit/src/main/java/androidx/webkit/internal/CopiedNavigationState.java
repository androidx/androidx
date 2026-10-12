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

package androidx.webkit.internal;

import androidx.annotation.RequiresFeature;
import androidx.webkit.NavigationState;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.WebViewNavigationBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewNavigationStateBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.Objects;

/**
 * CopiedNavigationState acts as a fake WebViewNavigationStateBoundaryInterface to be used in
 * {@link NavigationState}, it copies values from the Navigation object passed by the old
 * NavigationListener callbacks when {@link WebViewFeature#NAVIGATION_LISTENER_NAVIGATION_STATE} is
 * not supported. This means it's not threadsafe like a real {@link NavigationState}, it exists for
 * easy backwards compatibility.
 */
public class CopiedNavigationState implements WebViewNavigationStateBoundaryInterface {

    private final @Nullable InvocationHandler mPageState;
    private final String mUrl;
    private final boolean mWasInitiatedByPage;
    private final boolean mIsSameDocument;
    private final boolean mIsReload;
    private final boolean mIsHistory;
    private final boolean mIsRestore;
    private final boolean mIsBack;
    private final boolean mIsForward;
    private final boolean mDidCommit;
    private final boolean mDidCommitErrorPage;
    private final int mStatusCode;
    private final @Nullable InvocationHandler mWebResourceError;
    private final @NonNull InvocationHandler mNavigation;

    /*package*/ CopiedNavigationState(@NonNull /*Navigation*/ InvocationHandler invocationHandler) {
        WebViewNavigationBoundaryInterface boundaryInterface = Objects.requireNonNull(
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, invocationHandler));
        InvocationHandler page = boundaryInterface.getPage();
        mNavigation = invocationHandler;
        mPageState = page == null
                ? null
                : BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new CopiedPageState(boundaryInterface.getPage()));
        mUrl = boundaryInterface.getUrl();
        mWasInitiatedByPage = boundaryInterface.wasInitiatedByPage();
        mIsSameDocument = boundaryInterface.isSameDocument();
        mIsReload = boundaryInterface.isReload();
        mIsHistory = boundaryInterface.isHistory();
        mIsRestore = boundaryInterface.isRestore();
        mIsBack = boundaryInterface.isBack();
        mIsForward = boundaryInterface.isForward();
        mDidCommit = boundaryInterface.didCommit();
        mDidCommitErrorPage = boundaryInterface.didCommitErrorPage();
        mStatusCode = boundaryInterface.getStatusCode();
        mWebResourceError =
                WebViewFeatureInternal.NAVIGATION_GET_WEB_RESOURCE_ERROR.isSupportedByWebView()
                    ? boundaryInterface.getWebResourceError()
                    : null;
    }

    @NonNull
    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public boolean wasInitiatedByPage() {
        return mWasInitiatedByPage;
    }

    @Override
    public boolean isSameDocument() {
        return mIsSameDocument;
    }

    @Override
    public boolean isReload() {
        return mIsReload;
    }

    @Override
    public boolean isHistory() {
        return mIsHistory;
    }

    @Override
    public boolean isRestore() {
        return mIsRestore;
    }

    @Override
    public boolean isBack() {
        return mIsBack;
    }

    @Override
    public boolean isForward() {
        return mIsForward;
    }

    @Override
    public boolean didCommit() {
        return mDidCommit;
    }

    @Override
    public boolean didCommitErrorPage() {
        return mDidCommitErrorPage;
    }

    @Override
    public int getStatusCode() {
        return mStatusCode;
    }

    @Override
    public long getNavigationStartUptimeMillis() {
        // Not available when the NavigationState is copied from a Navigation
        throw WebViewFeatureInternal.getUnsupportedOperationException();
    }

    @Override
    public @Nullable /*PageState*/ InvocationHandler getPageState() {
        return mPageState;
    }

    @Override
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_WEB_RESOURCE_ERROR,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @Nullable /*WebResourceErrorCompat*/ InvocationHandler getWebResourceError() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_WEB_RESOURCE_ERROR;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        return mWebResourceError;
    }

    @Override
    public @Nullable Map<String, String> getResponseHeaders() {
        // Not available when the NavigationState is copied from a Navigation
        throw WebViewFeatureInternal.getUnsupportedOperationException();
    }

    @NonNull
    @Override
    public /*Navigation*/ InvocationHandler getNavigation() {
        return mNavigation;
    }
}
