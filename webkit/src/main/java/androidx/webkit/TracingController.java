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

package androidx.webkit;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.TracingControllerImpl;

import java.io.OutputStream;
import java.util.concurrent.Executor;

/**
 * Manages tracing of WebViews. In particular provides functionality for the app
 * to enable/disable tracing of parts of code and to collect tracing data.
 * This is useful for profiling performance issues, debugging and memory usage
 * analysis in production and real life scenarios.
 * <p>
 * The resulting trace data is sent back as a byte sequence in json format. This
 * file can be loaded in "chrome://tracing" for further analysis.
 * <p>
 * Example usage:
 * <pre class="prettyprint">
 * TracingController tracingController = TracingController.getInstance();
 * tracingController.start(new TracingConfig.Builder()
 *                  .addCategories(CATEGORIES_WEB_DEVELOPER).build());
 * ...
 * tracingController.stop(new FileOutputStream("trace.json"),
 *                        Executors.newSingleThreadExecutor());
 * </pre>
 */
public abstract class TracingController {
    /**
     *
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingController() {}

    /**
     * Returns the default {@link TracingController} instance. At present there is
     * only one TracingController instance for all WebView instances.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static TracingController getInstance() {
        return LAZY_HOLDER.INSTANCE;
    }

    private static class LAZY_HOLDER {
        static final TracingController INSTANCE = new TracingControllerImpl();
    }

    /**
     * Starts tracing all webviews. Depending on the trace mode in traceConfig
     * specifies how the trace events are recorded.
     *
     * <p>
     * For tracing modes {@link android.webkit.TracingConfig#RECORD_UNTIL_FULL} and
     * {@link android.webkit.TracingConfig#RECORD_CONTINUOUSLY} the events are recorded
     * using an internal buffer and flushed to the outputStream when
     * {@link #stop(OutputStream, Executor)} is called.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     * @param tracingConfig Configuration options to use for tracing.
     *
     * @throws IllegalStateException If the system is already tracing.
     * @throws IllegalArgumentException If the configuration is invalid (e.g.
     *         invalid category pattern or invalid tracing mode).
     */
    public abstract void start(@NonNull TracingConfig tracingConfig);

    /**
     * Stops tracing and flushes tracing data to the specified outputStream.
     *
     * The data is sent to the specified output stream in json format typically in chunks
     * by invoking {@link OutputStream#write(byte[])}.
     * On completion the {@link OutputStream#close()} method is called.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns {@code true} for {@link WebViewFeature#TRACING_CONTROLLER_BASIC_USAGE}.
     *
     * @param outputStream The output stream the tracing data will be sent to.
     *                     If {@code null} the tracing data will be discarded.
     * @param executor The Executor on which the outputStream {@link OutputStream#write(byte[])} and
     *                 {@link OutputStream#close()} methods will be invoked.
     *
     *                 Callback and listener events are dispatched through this Executor,
     *                 providing an easy way to control which thread is used.
     *                 To dispatch events through the main thread of your application,
     *                 you can use {@link Context#getMainExecutor()}.
     *                 To dispatch events through a shared thread pool,
     *                 you can use {@link AsyncTask#THREAD_POOL_EXECUTOR}.
     *
     * @return {@code false} if the WebView framework was not tracing at the time of the call,
     * {@code true} otherwise.
     */
    public abstract boolean stop(@Nullable OutputStream outputStream, @NonNull Executor executor);

    /**
     * Returns whether the WebView framework is tracing.
     *
     * @return True if tracing is enabled.
     */
    public abstract boolean isTracing();
}
