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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.TracingConfig;
import androidx.webkit.TracingController;

import org.chromium.support_lib_boundary.TracingControllerBoundaryInterface;

import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link TracingController}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link TracingController} functionality.
 */
public class TracingControllerImpl extends TracingController {
    private android.webkit.TracingController mFrameworksImpl;
    private TracingControllerBoundaryInterface mBoundaryInterface;

    @SuppressLint("NewApi")
    public TracingControllerImpl() {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.TRACING_CONTROLLER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            mFrameworksImpl = android.webkit.TracingController.getInstance();
            mBoundaryInterface = null;
        } else if (feature.isSupportedByWebView()) {
            mFrameworksImpl = null;
            mBoundaryInterface = WebViewGlueCommunicator.getFactory().getTracingController();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @RequiresApi(28)
    private android.webkit.TracingController getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl = android.webkit.TracingController.getInstance();
        }
        return mFrameworksImpl;
    }

    private TracingControllerBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            mBoundaryInterface = WebViewGlueCommunicator.getFactory().getTracingController();
        }
        return mBoundaryInterface;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean isTracing() {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.TRACING_CONTROLLER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().isTracing();
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().isTracing();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void start(@NonNull TracingConfig tracingConfig) {
        if (tracingConfig == null) {
            throw new IllegalArgumentException("Tracing config must be non null");
        }

        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.TRACING_CONTROLLER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            android.webkit.TracingConfig config = new android.webkit.TracingConfig.Builder()
                    .addCategories(tracingConfig.getPredefinedCategories())
                    .addCategories(tracingConfig.getCustomIncludedCategories())
                    .setTracingMode(tracingConfig.getTracingMode())
                    .build();
            getFrameworksImpl().start(config);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().start(tracingConfig.getPredefinedCategories(),
                    tracingConfig.getCustomIncludedCategories(), tracingConfig.getTracingMode());
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean stop(OutputStream outputStream, Executor executor) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.TRACING_CONTROLLER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().stop(outputStream, executor);
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().stop(outputStream, executor);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
