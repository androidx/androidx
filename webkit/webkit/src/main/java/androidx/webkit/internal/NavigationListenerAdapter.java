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
import androidx.webkit.NavigationRedirectParameters;
import androidx.webkit.NavigationState;
import androidx.webkit.PageState;

import org.chromium.support_lib_boundary.WebViewNavigationListenerBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter between {@link NavigationListener} and
 * {@link WebViewNavigationListenerBoundaryInterface}.
 */
@SuppressWarnings("MissingOverride")
public class NavigationListenerAdapter implements WebViewNavigationListenerBoundaryInterface {

    private static final String[] SUPPORTED_FEATURES = {Features.WEB_VIEW_NAVIGATION_LISTENER_V1,
            Features.WEB_VIEW_NAVIGATION_LISTENER_V2,
            Features.NAVIGATION_LISTENER_NAVIGATION_STATE};

    private final NavigationListener mImpl;

    public NavigationListenerAdapter(@NonNull NavigationListener impl) {
        mImpl = impl;
    }

    @Override
    public void onNavigationStarted(@NonNull InvocationHandler navigation) {
        // on older WebView versions we still want to support the NavigationState callback,
        // so we create a 'fake' NavigationState, which is thread-unsafe, and pass it into
        // the NavigationState callback.
        // This applies to all the below callbacks which aren't deprecated.
        //
        // TODO(b/571964854): If this is called and the user is still using the old
        //  onNavigationStarted(Navigation) callbacks, we are doing unnecessary work by wrapping a
        //  CopiedNavigationState and not directly calling the old callback.
        mImpl.onNavigationStarted(new NavigationState(new CopiedNavigationState(navigation)));
    }

    @Override
    public void onNavigationStartedThreadsafe(@NonNull InvocationHandler navigationState) {
        mImpl.onNavigationStarted(NavigationState.forInvocationHandler(navigationState));
    }

    @Override
    public void onNavigationRedirected(@NonNull InvocationHandler navigation) {
        mImpl.onNavigationRedirected(
                new NavigationState(new CopiedNavigationState(navigation)),
                NavigationRedirectParameters.forInvocationHandler(
                        new NotImplementedInvocationHandler()));
    }

    @Override
    public void onNavigationRedirectedThreadsafe(
            @NonNull InvocationHandler navigationState,
            @NonNull InvocationHandler redirectParameters) {
        mImpl.onNavigationRedirected(
                NavigationState.forInvocationHandler(navigationState),
                NavigationRedirectParameters.forInvocationHandler(redirectParameters));
    }

    @Override
    public void onNavigationCompleted(@NonNull InvocationHandler navigation) {
        mImpl.onNavigationCompleted(new NavigationState(new CopiedNavigationState(navigation)));
    }

    @Override
    public void onNavigationCompletedThreadsafe(@NonNull InvocationHandler navigationState) {
        mImpl.onNavigationCompleted(
                NavigationState.forInvocationHandler(navigationState));
    }

    @Override
    public void onPageDeleted(@NonNull InvocationHandler page) {
        mImpl.onPageDeleted(new PageState(new CopiedPageState(page)));
    }

    @Override
    public void onPageDeletedThreadsafe(@NonNull InvocationHandler pageState) {
        mImpl.onPageDeleted(PageState.forInvocationHandler(pageState));
    }

    @Override
    public void onPageLoadEventFired(@NonNull InvocationHandler page) {
        mImpl.onPageLoadEvent(new PageState(new CopiedPageState(page)));
    }

    @Override
    public void onPageLoadEventFiredThreadsafe(@NonNull InvocationHandler pageState) {
        mImpl.onPageLoadEvent(PageState.forInvocationHandler(pageState));
    }

    @Override
    public void onPageDOMContentLoadedEventFired(@NonNull InvocationHandler page) {
        mImpl.onPageDomContentLoadedEvent(new PageState(new CopiedPageState(page)));
    }

    @Override
    public void onPageDOMContentLoadedEventFiredThreadsafe(@NonNull InvocationHandler pageState) {
        mImpl.onPageDomContentLoadedEvent(PageState.forInvocationHandler(pageState));
    }

    /**
     * @deprecated Use {@link #onFirstContentfulPaintMillis} instead.
     */
    @Deprecated
    public void onFirstContentfulPaint(@NonNull InvocationHandler page, long loadTimeUs) {}

    @Override
    public void onFirstContentfulPaintMillis(@NonNull InvocationHandler page, long durationMillis) {
        mImpl.onFirstContentfulPaintMillis(
                new PageState(new CopiedPageState(page)), durationMillis);
    }

    @Override
    public void onFirstContentfulPaintMillisThreadsafe(
            @NonNull InvocationHandler pageState, long durationMillis) {
        mImpl.onFirstContentfulPaintMillis(
                PageState.forInvocationHandler(pageState), durationMillis);
    }

    @Override
    public void onLargestContentfulPaintMillis(@NonNull InvocationHandler page,
            long durationMillis) {
        mImpl.onLargestContentfulPaintMillis(
                new PageState(new CopiedPageState(page)), durationMillis);
    }

    @Override
    public void onLargestContentfulPaintMillisThreadsafe(@NonNull InvocationHandler pageState,
            long durationMillis) {
        mImpl.onLargestContentfulPaintMillis(
                PageState.forInvocationHandler(pageState), durationMillis);
    }

    @Override
    public void onPerformanceMarkMillis(@NonNull InvocationHandler page,
            @NonNull String markName, long markTimeMillis) {
        mImpl.onPerformanceMarkMillis(new PageState(new CopiedPageState(page)),
                markName, markTimeMillis);
    }

    @Override
    public void onPerformanceMarkMillisThreadsafe(@NonNull InvocationHandler pageState,
            @NonNull String markName, long markTimeMillis) {
        mImpl.onPerformanceMarkMillis(PageState.forInvocationHandler(pageState),
                markName, markTimeMillis);
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
