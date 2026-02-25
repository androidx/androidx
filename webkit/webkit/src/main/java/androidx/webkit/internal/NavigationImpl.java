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

package androidx.webkit.internal;

import androidx.webkit.Navigation;
import androidx.webkit.Page;
import androidx.webkit.WebNavigationClient;
import androidx.webkit.WebResourceErrorCompat;

import org.chromium.support_lib_boundary.WebViewNavigationBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * Adapter for {@link WebViewNavigationBoundaryInterface} instances.
 *
 * <p>Adapters are isomorphic, and should be obtained through
 * {@link #forInvocationHandler(InvocationHandler)}.
 */
@WebNavigationClient.ExperimentalNavigationCallback
public class NavigationImpl implements Navigation {
    WebViewNavigationBoundaryInterface mImpl;
    Page mPage;

    /**
     * Factory method that returns the NavigationImpl associated with the given invocationHandler.
     */
    public static @NonNull Navigation forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        WebViewNavigationBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return (Navigation) Objects.requireNonNull(boundaryInterface.getOrCreatePeer(
                () -> new NavigationImpl(boundaryInterface)));

    }

    private NavigationImpl(@NonNull WebViewNavigationBoundaryInterface impl) {
        mImpl = impl;
    }

    @Override
    public @Nullable Page getPage() {
        if (mImpl.getPage() == null) return null;
        // Once the Page is non-null, it won't change so there's no need to do an extra casting.
        if (mPage == null) {
            mPage = PageImpl.forInvocationHandler(mImpl.getPage());
        }
        return mPage;
    }

    @Override
    public @NonNull String getUrl() {
        return mImpl.getUrl();
    }

    @Override
    public boolean wasInitiatedByPage() {
        return mImpl.wasInitiatedByPage();
    }

    @Override
    public boolean isSameDocument() {
        return mImpl.isSameDocument();
    }

    @Override
    public boolean isReload() {
        return mImpl.isReload();
    }

    @Override
    public boolean isHistory() {
        return mImpl.isHistory();
    }

    @Override
    public boolean isRestore() {
        return mImpl.isRestore();
    }

    @Override
    public boolean isBack() {
        return mImpl.isBack();
    }

    @Override
    public boolean isForward() {
        return mImpl.isForward();
    }

    @Override
    public boolean didCommit() {
        return mImpl.didCommit();
    }

    @Override
    public boolean didCommitErrorPage() {
        return mImpl.didCommitErrorPage();
    }

    @Override
    public int getStatusCode() {
        return mImpl.getStatusCode();
    }

    @Override
    public @Nullable WebResourceErrorCompat getWebResourceError() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_WEB_RESOURCE_ERROR;
        if (feature.isSupportedByWebView()) {
            if (mImpl.getWebResourceError() == null) return null;
            return new WebResourceErrorImpl(mImpl.getWebResourceError());
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
