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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.ProcessGlobalConfigConstants;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process Global Configuration for WebView.
 *
 * WebView has some process-global configuration parameters that cannot be changed once WebView has
 * been loaded. This class allows apps to set these parameters.
 * <p>
 * If it is used, the configuration should be set and {@link #apply()} should be called prior to
 * loading WebView into the calling process. Most of the methods in
 * {@link android.webkit} and {@link androidx.webkit} packages load WebView, so the
 * configuration should be applied before calling any of these methods.
 * <p>
 * The following code configures the data directory suffix that WebView
 * uses and then applies the configuration. WebView uses this configuration when it is loaded.
 * <pre class="prettyprint">
 * ProcessGlobalConfig.createInstance()
 *                    .setDataDirectorySuffix("random_suffix")
 *                    .apply();
 * </pre>
 * <p>
 * Restrictions are in place to ensure that {@link #createInstance()} can only be called once.
 * The setters and {@link #apply()} can also only be called once.
 * <p>
 * Only a single thread should access this class at a given time.
 * <p>
 * The configuration should be set up as early as possible during application startup, to ensure
 * that it happens before any other thread can call a method that loads WebView.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ProcessGlobalConfig {
    private static final AtomicReference<HashMap<String, Object>> sProcessGlobalConfig =
            new AtomicReference<HashMap<String, Object>>();
    private static AtomicBoolean sInstanceCreated = new AtomicBoolean(false);
    private boolean mApplyCalled = false;
    private String mDataDirectorySuffix;

    private ProcessGlobalConfig() {
    }

    /**
     * Creates instance of {@link ProcessGlobalConfig}.
     *
     * This method can only be called once.
     *
     * @return {@link ProcessGlobalConfig} object where configuration can be set and applied
     *
     * @throws IllegalStateException if this method was called before
     */
    @NonNull
    public static ProcessGlobalConfig createInstance() {
        if (!sInstanceCreated.compareAndSet(false, true)) {
            throw new IllegalStateException("ProcessGlobalConfig#createInstance() was already "
                    + "called before");
        }
        return new ProcessGlobalConfig();
    }

    /**
     * Applies the configuration to be used by WebView on loading.
     *
     * If this method is not called, the configuration that is set will not be applied.
     * This method can only be called once.
     * <p>
     * Calling this method will not cause WebView to be loaded and will not block the calling
     * thread.
     *
     * @throws IllegalStateException if WebView has already been initialized
     *                               in the current process or if this method was called before
     */
    public void apply() {
        // TODO(crbug.com/1355297): We can check if we are storing the config in the place that
        //  WebView is going to look for it, and throw if they are not the same.
        //  For this, we would need to reflect into Android Framework internals to get
        //  ActivityThread.currentApplication().getClassLoader() and see if it is the same as
        //  this.getClass().getClassLoader(). This would add reflection that we might not add a
        //  framework API for. Once we know what framework path we will take for
        //  ProcessGlobalConfig, revisit this.
        HashMap<String, Object> configMap = new HashMap<String, Object>();
        if (mApplyCalled) {
            throw new IllegalStateException("ProcessGlobalConfig#apply() was already called "
                    + "before");
        }
        mApplyCalled = true;
        if (webViewCurrentlyLoaded()) {
            throw new IllegalStateException(
                    "WebView has already been initialized in the current process");
        }
        final ApiFeature.P feature = WebViewFeatureInternal.SET_DATA_DIRECTORY_SUFFIX;
        if (feature.isSupportedByFramework()) {
            androidx.webkit.internal.ApiHelperForP.setDataDirectorySuffix(mDataDirectorySuffix);
        } else {
            configMap.put(ProcessGlobalConfigConstants.DATA_DIRECTORY_SUFFIX, mDataDirectorySuffix);
        }
        if (!sProcessGlobalConfig.compareAndSet(null, configMap)) {
            throw new RuntimeException("Attempting to set ProcessGlobalConfig"
                    + "#sProcessGlobalConfig when it was already set");
        }
    }

    /**
     * Define the directory used to store WebView data for the current process.
     *
     * The provided suffix will be used when constructing data and cache
     * directory paths. If this API is not called, no suffix will be used.
     * Each directory can be used by only one process in the application. If more
     * than one process in an app wishes to use WebView, only one process can use
     * the default directory, and other processes must call this API to define
     * a unique suffix.
     * <p>
     * This means that different processes in the same application cannot directly
     * share WebView-related data, since the data directories must be distinct.
     * Applications that use this API may have to explicitly pass data between
     * processes. For example, login cookies may have to be copied from one
     * process's cookie jar to the other using {@link android.webkit.CookieManager} if both
     * processes' WebViews are intended to be logged in.
     * <p>
     * Most applications should simply ensure that all components of the app
     * that rely on WebView are in the same process, to avoid needing multiple
     * data directories. The {@link android.webkit.WebView#disableWebView} method can be used to
     * ensure that the other processes do not use WebView by accident in this case.
     * <p>
     * This is a compatibility method for
     * {@link android.webkit.WebView#setDataDirectorySuffix(String)}
     *
     * @param suffix The directory name suffix to be used for the current
     *               process. Must not contain a path separator and should not be empty.
     * @throws IllegalStateException if WebView has already been initialized
     *                               in the current process or if this method was called before
     * @throws IllegalArgumentException if the suffix contains a path separator or is empty.
     */
    @RequiresFeature(name = WebViewFeature.SET_DATA_DIRECTORY_SUFFIX,
            enforcement =
                    "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    public ProcessGlobalConfig setDataDirectorySuffix(@NonNull String suffix) {
        if (mDataDirectorySuffix != null) {
            throw new IllegalStateException(
                    "ProcessGlobalConfig#setDataDirectorySuffix(String) was already "
                            + "called");
        }
        final ApiFeature.P feature = WebViewFeatureInternal.SET_DATA_DIRECTORY_SUFFIX;
        if (!feature.isSupported()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        if (suffix.equals("")) {
            throw new IllegalArgumentException("Suffix cannot be an empty string");
        }
        if (suffix.indexOf(File.separatorChar) >= 0) {
            throw new IllegalArgumentException("Suffix " + suffix
                    + " contains a path separator");
        }
        mDataDirectorySuffix = suffix;
        return this;
    }

    private boolean webViewCurrentlyLoaded() {
        // TODO(crbug.com/1355297): This is racy but it is the best we can do for now since we can't
        //  access the lock for sProviderInstance in WebView. Evaluate a framework path for
        //  ProcessGlobalConfig.
        try {
            Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
            Field providerInstanceField =
                    webViewFactoryClass.getDeclaredField("sProviderInstance");
            providerInstanceField.setAccessible(true);
            return providerInstanceField.get(null) != null;
        } catch (Exception e) {
            // This means WebViewFactory was not found or sProviderInstance was not found within
            // the class. If that is true, WebView doesn't seem to be loaded.
            return false;
        }
    }
}
