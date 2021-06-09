/*
 * Copyright 2018 The Android Open Source Project
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

import android.webkit.SafeBrowsingResponse;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.SafeBrowsingResponseCompat;

import org.chromium.support_lib_boundary.SafeBrowsingResponseBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Implementation of {@link SafeBrowsingResponseCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link SafeBrowsingResponseCompat} functionality.
 *
 */
public class SafeBrowsingResponseImpl extends SafeBrowsingResponseCompat {
    /**
     * Frameworks implementation - do not use this directly, instead use
     * {@link #getFrameworksImpl()} to ensure this variable has been instantiated correctly.
     */
    private SafeBrowsingResponse mFrameworksImpl;

    /**
     * Support library glue implementation - do not use this directly, instead use
     * {@link #getBoundaryInterface()} to ensure this variable has been instantiated correctly.
     */
    private SafeBrowsingResponseBoundaryInterface mBoundaryInterface;

    public SafeBrowsingResponseImpl(@NonNull InvocationHandler invocationHandler) {
        mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                SafeBrowsingResponseBoundaryInterface.class, invocationHandler);
    }

    public SafeBrowsingResponseImpl(@NonNull SafeBrowsingResponse response) {
        mFrameworksImpl = response;
    }

    @RequiresApi(27)
    private SafeBrowsingResponse getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl =
                    WebViewGlueCommunicator.getCompatConverter().convertSafeBrowsingResponse(
                            Proxy.getInvocationHandler(mBoundaryInterface));
        }
        return mFrameworksImpl;
    }

    private SafeBrowsingResponseBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    SafeBrowsingResponseBoundaryInterface.class,
                    WebViewGlueCommunicator.getCompatConverter().convertSafeBrowsingResponse(
                            mFrameworksImpl));
        }
        return mBoundaryInterface;
    }

    @Override
    public void showInterstitial(boolean allowReporting) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().showInterstitial(allowReporting);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().showInterstitial(allowReporting);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void proceed(boolean report) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.SAFE_BROWSING_RESPONSE_PROCEED;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().proceed(report);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().proceed(report);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void backToSafety(boolean report) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().backToSafety(report);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().backToSafety(report);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
