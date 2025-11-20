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

import android.webkit.WebResourceError;

import androidx.webkit.WebResourceErrorCompat;

import org.chromium.support_lib_boundary.WebResourceErrorBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Implementation of {@link WebResourceErrorCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link WebResourceErrorCompat} functionality.
 *
 */
public class WebResourceErrorImpl extends WebResourceErrorCompat {
    /**
     * Frameworks implementation - do not use this directly, instead use
     * {@link #getFrameworksImpl()} to ensure this variable has been instantiated correctly.
     */
    private WebResourceError mFrameworksImpl;

    /**
     * Support library glue implementation - do not use this directly, instead use
     * {@link #getBoundaryInterface()} to ensure this variable has been instantiated correctly.
     */
    private WebResourceErrorBoundaryInterface mBoundaryInterface;

    public WebResourceErrorImpl(@NonNull InvocationHandler invocationHandler) {
        mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebResourceErrorBoundaryInterface.class, invocationHandler);
    }

    public WebResourceErrorImpl(@NonNull WebResourceError error) {
        mFrameworksImpl = error;
    }

    private WebResourceError getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl = WebViewGlueCommunicator.getCompatConverter().convertWebResourceError(
                    Proxy.getInvocationHandler(mBoundaryInterface));
        }
        return mFrameworksImpl;
    }

    @Override
    public int getErrorCode() {
        return getFrameworksImpl().getErrorCode();
    }

    @Override
    public @NonNull CharSequence getDescription() {
        return getFrameworksImpl().getDescription();
    }
}
