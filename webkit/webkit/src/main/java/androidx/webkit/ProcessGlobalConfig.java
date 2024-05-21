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

import android.content.Context;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.webkit.internal.ApiHelperForP;
import androidx.webkit.internal.StartupApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.ProcessGlobalConfigConstants;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process Global Configuration for WebView.
 * <p>
 * WebView has some process-global configuration parameters that cannot be changed once WebView has
 * been loaded. This class allows apps to set these parameters.
 * <p>
 * If it is used, the configuration should be set and
 * {@link #apply(androidx.webkit.ProcessGlobalConfig)} should be called prior to
 * loading WebView into the calling process. Most of the methods in
 * {@link android.webkit} and {@link androidx.webkit} packages load WebView, so the
 * configuration should be applied before calling any of these methods.
 * <p>
 * The following code configures the data directory suffix that WebView
 * uses and then applies the configuration. WebView uses this configuration when it is loaded.
 * <pre class="prettyprint">
 * ProcessGlobalConfig config = new ProcessGlobalConfig();
 * config.setDataDirectorySuffix("random_suffix")
 * ProcessGlobalConfig.apply(config);
 * </pre>
 * <p>
 * {@link ProcessGlobalConfig#apply(androidx.webkit.ProcessGlobalConfig)} can only be called once.
 * <p>
 * Only a single thread should access this class at a given time.
 * <p>
 * The configuration should be set up as early as possible during application startup, to ensure
 * that it happens before any other thread can call a method that loads WebView.
 */
public class ProcessGlobalConfig {
    private static final AtomicReference<HashMap<String, Object>> sProcessGlobalConfig =
            new AtomicReference<>();
    private static final Object sLock = new Object();
    @GuardedBy("sLock")
    private static boolean sApplyCalled = false;
    String mDataDirectorySuffix;
    String mDataDirectoryBasePath;
    String mCacheDirectoryBasePath;

    /**
     * Creates a {@link ProcessGlobalConfig} object.
     */
    public ProcessGlobalConfig() {
    }

    /**
     * Define the directory used to store WebView data for the current process.
     * <p>
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
     * @param context a Context to access application assets This value cannot be null.
     * @param suffix The directory name suffix to be used for the current
     *               process. Must not contain a path separator and should not be empty.
     * @return the ProcessGlobalConfig that has the value set to allow chaining of setters
     * @throws UnsupportedOperationException if underlying WebView does not support the use of
     *                                       the method.
     * @throws IllegalArgumentException if the suffix contains a path separator or is empty.
     */
    @RequiresFeature(name = WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
            enforcement =
                    "androidx.webkit.WebViewFeature#isConfigFeatureSupported(String, Context)")
    @NonNull
    public ProcessGlobalConfig setDataDirectorySuffix(@NonNull Context context,
            @NonNull String suffix) {
        final StartupApiFeature.P feature =
                WebViewFeatureInternal.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX;
        if (!feature.isSupported(context)) {
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

    /**
     * Set the base directories that WebView will use for the current process.
     * <p>
     * If this method is not used, WebView uses the default base paths defined by the Android
     * framework.
     * <p>
     * WebView will create and use a subdirectory under each of the base paths supplied to this
     * method.
     * <p>
     * This method can be used in conjunction with {@link #setDataDirectorySuffix(Context, String)}.
     * A different subdirectory is created for each suffix.
     * <p>
     * The base paths must be absolute paths.
     * <p>
     * The data directory must not be under the Android cache directory, as Android may delete
     * cache files when disk space is low and WebView may not function properly if this occurs.
     * Refer to
     * <a href="https://developer.android.com/training/data-storage/app-specific#internal-remove-cache">this</a>
     *  link.
     * <p>
     * If the specified directories already exist then they must be readable and writable by the
     * current process. If they do not already exist, WebView will attempt to create them during
     * initialization, along with any missing parent directories. In such a case, the directory
     * in which WebView creates missing directories must be readable and writable by the
     * current process.
     *
     * @param context a Context to access application assets. This value cannot be null.
     * @param dataDirectoryBasePath the absolute base path for the WebView data directory.
     * @param cacheDirectoryBasePath the absolute base path for the WebView cache directory.
     * @return the ProcessGlobalConfig that has the value set to allow chaining of setters
     * @throws UnsupportedOperationException if underlying WebView does not support the use of
     *                                       the method.
     * @throws IllegalArgumentException if the paths supplied do not have the right permissions
     */
    @SuppressWarnings("StreamFiles")
    @RequiresFeature(name =
            WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
            enforcement =
                    "androidx.webkit.WebViewFeature#isConfigFeatureSupported(String, Context)")
    @NonNull
    public ProcessGlobalConfig setDirectoryBasePaths(@NonNull Context context,
            @NonNull File dataDirectoryBasePath, @NonNull File cacheDirectoryBasePath) {
        final StartupApiFeature.NoFramework feature =
                WebViewFeatureInternal.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATH;
        if (!feature.isSupported(context)) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        if (!dataDirectoryBasePath.isAbsolute()) {
            throw new IllegalArgumentException("dataDirectoryBasePath must be a non-empty absolute"
                    + " path");
        }
        if (!cacheDirectoryBasePath.isAbsolute()) {
            throw new IllegalArgumentException("cacheDirectoryBasePath must be a non-empty absolute"
                    + " path");
        }
        mDataDirectoryBasePath = dataDirectoryBasePath.getAbsolutePath();
        mCacheDirectoryBasePath = cacheDirectoryBasePath.getAbsolutePath();
        return this;
    }

    /**
     * Applies the configuration to be used by WebView on loading.
     * <p>
     * This method can only be called once.
     * <p>
     * Calling this method will not cause WebView to be loaded and will not block the calling
     * thread.
     *
     * @param config the config to be applied
     * @throws IllegalStateException if WebView has already been initialized
     *                               in the current process or if this method was called before
     */
    public static void apply(@NonNull ProcessGlobalConfig config) {
        // TODO(crbug.com/1355297): We can check if we are storing the config in the place that
        //  WebView is going to look for it, and throw if they are not the same.
        //  For this, we would need to reflect into Android Framework internals to get
        //  ActivityThread.currentApplication().getClassLoader() and see if it is the same as
        //  this.getClass().getClassLoader(). This would add reflection that we might not add a
        //  framework API for. Once we know what framework path we will take for
        //  ProcessGlobalConfig, revisit this.
        synchronized (sLock) {
            if (sApplyCalled) {
                throw new IllegalStateException("ProcessGlobalConfig#apply was "
                        + "called more than once, which is an illegal operation. The configuration "
                        + "settings provided by ProcessGlobalConfig take effect only once, when "
                        + "WebView is first loaded into the current process. Every process should "
                        + "only ever create a single instance of ProcessGlobalConfig and apply it "
                        + "once, before any calls to android.webkit APIs, such as during early app "
                        + "startup."
                );
            }
            sApplyCalled = true;
        }
        HashMap<String, Object> configMap = new HashMap<>();
        if (webViewCurrentlyLoaded()) {
            throw new IllegalStateException("WebView has already been loaded in the current "
                    + "process, so any attempt to apply the settings in ProcessGlobalConfig will "
                    + "have no effect. ProcessGlobalConfig#apply needs to be called before any "
                    + "calls to android.webkit APIs, such as during early app startup.");
        }
        if (config.mDataDirectorySuffix != null) {
            final StartupApiFeature.P feature =
                    WebViewFeatureInternal.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX;
            if (feature.isSupportedByFramework()) {
                ApiHelperForP.setDataDirectorySuffix(config.mDataDirectorySuffix);
            } else {
                configMap.put(ProcessGlobalConfigConstants.DATA_DIRECTORY_SUFFIX,
                        config.mDataDirectorySuffix);
            }
        }
        if (config.mDataDirectoryBasePath != null) {
            configMap.put(ProcessGlobalConfigConstants.DATA_DIRECTORY_BASE_PATH,
                    config.mDataDirectoryBasePath);
        }
        if (config.mCacheDirectoryBasePath != null) {
            configMap.put(ProcessGlobalConfigConstants.CACHE_DIRECTORY_BASE_PATH,
                    config.mCacheDirectoryBasePath);
        }
        if (!sProcessGlobalConfig.compareAndSet(null, configMap)) {
            throw new RuntimeException("Attempting to set ProcessGlobalConfig"
                    + "#sProcessGlobalConfig when it was already set");
        }
    }

    private static boolean webViewCurrentlyLoaded() {
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
