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

import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter between {@link WebNavigationClient} and
 * {@link org.chromium.support_lib_boundary.WebViewNavigationClientBoundaryInterface}.
 * It handles the delegation for callback triggers.
 */
@WebNavigationClient.ExperimentalNavigationCallback
@SuppressWarnings("deprecation")
public class WebNavigationClientAdapter implements
        org.chromium.support_lib_boundary.WebViewNavigationClientBoundaryInterface {
    WebNavigationClient mWebNavigationClient;

    public WebNavigationClientAdapter(@NonNull WebNavigationClient client) {
        mWebNavigationClient = client;
    }

    public @Nullable WebNavigationClient getWebNavigationClient() {
        return mWebNavigationClient;
    }

    @Override
    public void onNavigationStarted(@NonNull InvocationHandler navigation) {
        mWebNavigationClient.onNavigationStarted(
                NavigationImpl.forInvocationHandler(navigation));
    }

    @Override
    public void onNavigationRedirected(@NonNull InvocationHandler navigation) {
        mWebNavigationClient.onNavigationRedirected(
                NavigationImpl.forInvocationHandler(navigation));
    }

    @WebNavigationClient.ExperimentalNavigationCallback
    @Override
    public void onNavigationCompleted(@NonNull InvocationHandler navigation) {
        mWebNavigationClient.onNavigationCompleted(
                NavigationImpl.forInvocationHandler(navigation));
    }

    @Override
    public void onPageDeleted(@NonNull InvocationHandler page) {
        mWebNavigationClient.onPageDeleted(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onPageLoadEventFired(@NonNull InvocationHandler page) {
        mWebNavigationClient.onPageLoadEventFired(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onPageDOMContentLoadedEventFired(@NonNull InvocationHandler page) {
        mWebNavigationClient.onPageDomContentLoadedEventFired(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onFirstContentfulPaint(@NonNull InvocationHandler page) {
        mWebNavigationClient.onFirstContentfulPaint(PageImpl.forInvocationHandler(page));
    }

    @NonNull
    @Override
    public String[] getSupportedFeatures() {
        return new String[]{Features.WEB_VIEW_NAVIGATION_CLIENT_BASIC_USAGE};
    }
}
