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

import androidx.annotation.Nullable;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewRenderProcess;

import org.chromium.support_lib_boundary.WebViewRendererBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Callable;

/**
 * Implementation of {@link WebViewRenderProcess}.
 * This class uses the WebView APK to implement
 * {@link WebViewRenderProcess} functionality.
 */
public class WebViewRenderProcessImpl extends WebViewRenderProcess {
    private WebViewRendererBoundaryInterface mBoundaryInterface;

    public WebViewRenderProcessImpl(WebViewRendererBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Get a support library WebViewRenderProcess object that is 1:1 with the webview object.
     */
    public static @Nullable WebViewRenderProcessImpl forInvocationHandler(
            InvocationHandler invocationHandler) {
        // Make a possibly temporary proxy object in order to call into WebView.
        final WebViewRendererBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewRendererBoundaryInterface.class,
                        invocationHandler);

        // Ask WebView to either call us back to create the wrapper object, or
        // to return a previously created wrapper object.
        return (WebViewRenderProcessImpl) boundaryInterface.getOrCreatePeer(
                new Callable<Object>() {
                    @Override
                    public Object call() {
                        return new WebViewRenderProcessImpl(boundaryInterface);
                    }
                });
    }

    @Override
    public boolean terminate() {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.getFeature(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE);
        if (feature.isSupportedByWebView()) {
            return mBoundaryInterface.terminate();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
