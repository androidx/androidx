/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build;
import android.os.Looper;
import android.webkit.TracingController;
import android.webkit.WebView;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.TracingConfig;

import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * Utility class to use new APIs that were added in P (API level 28).
 * These need to exist in a separate class so that Android framework can successfully verify
 * classes without encountering the new APIs.
 */
@RequiresApi(Build.VERSION_CODES.P)
public class ApiHelperForP {
    private ApiHelperForP() {
    }

    /**
     * @see TracingController#getInstance()
     */
    @DoNotInline
    @NonNull
    public static TracingController getTracingControllerInstance() {
        return TracingController.getInstance();
    }

    /**
     * @see TracingController#isTracing()
     */
    @DoNotInline
    public static boolean isTracing(@NonNull TracingController tracingController) {
        return tracingController.isTracing();
    }

    /**
     * Converts the passed {@link TracingConfig} to {@link android.webkit.TracingConfig} to
     * isolate new types in this class.
     * @see TracingController#start(android.webkit.TracingConfig)
     */
    @DoNotInline
    public static void start(@NonNull TracingController tracingController,
            @NonNull TracingConfig tracingConfig) {
        android.webkit.TracingConfig config =
                new android.webkit.TracingConfig.Builder()
                        .addCategories(tracingConfig.getPredefinedCategories())
                        .addCategories(tracingConfig.getCustomIncludedCategories())
                        .setTracingMode(tracingConfig.getTracingMode())
                        .build();
        tracingController.start(config);
    }

    /**
     * @see TracingController#stop(OutputStream, Executor)
     */
    @DoNotInline
    public static boolean stop(@NonNull TracingController tracingController,
            @Nullable OutputStream os, @NonNull Executor ex) {
        return tracingController.stop(os, ex);
    }

    /**
     * @see WebView#getWebViewClassLoader()
     */
    @DoNotInline
    @NonNull
    public static ClassLoader getWebViewClassLoader() {
        return WebView.getWebViewClassLoader();
    }

    /**
     * @see WebView#getWebViewLooper()
     */
    @DoNotInline
    @NonNull
    public static Looper getWebViewLooper(@NonNull WebView webView) {
        return webView.getWebViewLooper();
    }


    /**
     * @see WebView#setDataDirectorySuffix(String)
     */
    @DoNotInline
    public static void setDataDirectorySuffix(@NonNull String suffix) {
        WebView.setDataDirectorySuffix(suffix);
    }
}
