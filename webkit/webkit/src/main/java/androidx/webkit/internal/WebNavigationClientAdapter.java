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


import androidx.webkit.WebNavigationClient;

import org.chromium.support_lib_boundary.WebViewNavigationBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewNavigationClientBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewPageBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter between {@link WebNavigationClient} and
 * {@link WebViewNavigationClientBoundaryInterface}. It handles the delegation for callback
 * triggers.
 */
@WebNavigationClient.ExperimentalNavigationCallback
public class WebNavigationClientAdapter implements
        WebViewNavigationClientBoundaryInterface {
    WebNavigationClient mWebNavigationClient;

    public WebNavigationClientAdapter(@NonNull WebNavigationClient client) {
        mWebNavigationClient = client;
    }

    public @Nullable WebNavigationClient getWebNavigationClient() {
        return mWebNavigationClient;
    }

    @Override
    public void onNavigationStarted(@NonNull InvocationHandler navigation) {
        WebViewNavigationBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, navigation);

        mWebNavigationClient.onNavigationStarted(
                (NavigationAdapter) boundaryInterface.getOrCreatePeer(
                        () -> new NavigationAdapter(boundaryInterface)));
    }

    @Override
    public void onNavigationRedirected(@NonNull InvocationHandler navigation) {
        WebViewNavigationBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, navigation);

        mWebNavigationClient.onNavigationRedirected(
                (NavigationAdapter) boundaryInterface.getOrCreatePeer(
                        () -> new NavigationAdapter(boundaryInterface)));
    }

    @WebNavigationClient.ExperimentalNavigationCallback
    @Override
    public void onNavigationCompleted(@NonNull InvocationHandler navigation) {
        WebViewNavigationBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewNavigationBoundaryInterface.class, navigation);

        mWebNavigationClient.onNavigationCompleted(
                (NavigationAdapter) boundaryInterface.getOrCreatePeer(
                        () -> new NavigationAdapter(boundaryInterface)));
    }

    @Override
    public void onPageDeleted(@NonNull InvocationHandler page) {
        final WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, page);
        mWebNavigationClient.onPageDeleted((PageImpl) boundaryInterface.getOrCreatePeer(
                () -> new PageImpl(boundaryInterface)));
    }

    @Override
    public void onPageLoadEventFired(@NonNull InvocationHandler page) {
        final WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, page);
        mWebNavigationClient.onPageLoadEventFired((PageImpl) boundaryInterface.getOrCreatePeer(
                () -> new PageImpl(boundaryInterface)));
    }

    @Override
    public void onPageDOMContentLoadedEventFired(@NonNull InvocationHandler page) {
        final WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, page);
        mWebNavigationClient.onPageDomContentLoadedEventFired(
                (PageImpl) boundaryInterface.getOrCreatePeer(
                        () -> new PageImpl(boundaryInterface)));
    }

    @Override
    public void onFirstContentfulPaint(@NonNull InvocationHandler page) {
        final WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, page);
        mWebNavigationClient.onFirstContentfulPaint((PageImpl) boundaryInterface.getOrCreatePeer(
                () -> new PageImpl(boundaryInterface)));
    }

    @NonNull
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{Features.WEB_VIEW_NAVIGATION_CLIENT_BASIC_USAGE};
    }
}
