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

import androidx.webkit.NavigationListener;
import androidx.webkit.WebNavigationClient;

import org.chromium.support_lib_boundary.WebViewNavigationListenerBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

@WebNavigationClient.ExperimentalNavigationCallback
public class NavigationListenerAdapter implements WebViewNavigationListenerBoundaryInterface {

    private static final String[] SUPPORTED_FEATURES = {Features.WEB_VIEW_NAVIGATION_LISTENER_V1};

    private final NavigationListener mImpl;

    public NavigationListenerAdapter(@NonNull NavigationListener impl) {
        mImpl = impl;
    }

    @Override
    public void onNavigationStarted(@NonNull InvocationHandler navigation) {
        mImpl.onNavigationStarted(NavigationImpl.forInvocationHandler(navigation));
    }

    @Override
    public void onNavigationRedirected(@NonNull InvocationHandler navigation) {
        mImpl.onNavigationRedirected(NavigationImpl.forInvocationHandler(navigation));
    }

    @Override
    public void onNavigationCompleted(@NonNull InvocationHandler navigation) {
        mImpl.onNavigationCompleted(NavigationImpl.forInvocationHandler(navigation));
    }

    @Override
    public void onPageDeleted(@NonNull InvocationHandler page) {
        mImpl.onPageDeleted(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onPageLoadEventFired(@NonNull InvocationHandler page) {
        mImpl.onPageLoadEvent(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onPageDOMContentLoadedEventFired(@NonNull InvocationHandler page) {
        mImpl.onPageDomContentLoadedEvent(PageImpl.forInvocationHandler(page));
    }

    @Override
    public void onFirstContentfulPaint(@NonNull InvocationHandler page, long loadTimeUs) {
        mImpl.onFirstContentfulPaint(PageImpl.forInvocationHandler(page), loadTimeUs);
    }

    @SuppressWarnings("NullableProblems")
    @NonNull
    @Override
    public String[] getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof NavigationListenerAdapter) {
            NavigationListenerAdapter other = (NavigationListenerAdapter) obj;
            return mImpl.equals(other.mImpl);
        }
        return false;
    }
}
