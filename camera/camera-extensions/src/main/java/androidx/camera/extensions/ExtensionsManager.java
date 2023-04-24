/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.camera.extensions;

import android.content.Context;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.ContextUtil;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.extensions.impl.InitializerImpl;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.VersionName;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Provides interfaces for third party app developers to get capabilities info of extension
 * functions.
 *
 * <p>Many Android devices contain powerful cameras, with manufacturers devoting a lot of effort
 * to build many cutting-edge features, or special effects, into these camera devices.
 * <code>CameraX Extensions</code> allows third party apps to enable the available extension
 * modes on the supported devices. The extension modes which might be supported via <code>CameraX
 * Extensions</code> are {@link ExtensionMode#BOKEH}, {@link ExtensionMode#HDR},
 * {@link ExtensionMode#NIGHT}, {@link ExtensionMode#FACE_RETOUCH} and {@link ExtensionMode#AUTO}
 * . The known supported devices are listed in the
 * <a href="https://developer.android.com/training/camera/supported-devices">Supported devices</a>
 * page.
 *
 * <p><code>CameraX Extensions</code> are built on the top of <code>CameraX Core</code> libraries
 * . To enable an extension mode, an {@link ExtensionsManager} instance needs to be retrieved
 * first with {@link #getInstanceAsync(Context, CameraProvider)}. Only a single
 * {@link ExtensionsManager} instance can exist within a process. After retrieving the
 * {@link ExtensionsManager} instance, the availability of a specific extension mode can be
 * checked by {@link #isExtensionAvailable(CameraSelector, int)}. For an available extension
 * mode, an extension enabled {@link CameraSelector} can be obtained by calling
 * {@link #getExtensionEnabledCameraSelector(CameraSelector, int)}. After binding use cases by
 * the extension enabled {@link CameraSelector}, the extension mode will be applied to the bound
 * {@link Preview} and {@link ImageCapture}. The following sample code describes how to enable an
 * extension mode for use cases.
 * </p>
 * <pre>
 * void bindUseCasesWithBokehMode() {
 *     // Create a camera provider
 *     ProcessCameraProvider cameraProvider = ... // Get the provider instance
 *     // Call the getInstance function to retrieve a ListenableFuture object
 *     ListenableFuture future = ExtensionsManager.getInstance(context, cameraProvider);
 *
 *     // Obtain the ExtensionsManager instance from the returned ListenableFuture object
 *     future.addListener(() -> {
 *         try {
 *             ExtensionsManager extensionsManager = future.get()
 *
 *             // Query if extension is available.
 *             if (mExtensionsManager.isExtensionAvailable(DEFAULT_BACK_CAMERA,
 *                        ExtensionMode.BOKEH)) {
 *                 // Needs to unbind all use cases before enabling different extension mode.
 *                 cameraProvider.unbindAll();
 *
 *                 // Retrieve extension enabled camera selector
 *                 CameraSelector extensionCameraSelector;
 *                 extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
 *                         DEFAULT_BACK_CAMERA, ExtensionMode.BOKEH);
 *
 *                 // Bind image capture and preview use cases with the extension enabled camera
 *                 // selector.
 *                 ImageCapture imageCapture = new ImageCapture.Builder().build();
 *                 Preview preview = new Preview.Builder().build();
 *                 cameraProvider.bindToLifecycle(lifecycleOwner, extensionCameraSelector,
 *                         imageCapture, preview);
 *             }
 *         } catch (ExecutionException | InterruptedException e) {
 *             // This should not happen unless the future is cancelled or the thread is
 *             // interrupted by applications.
 *         }
 *     }, ContextCompact.getMainExecutor(context));
 * }
 * </pre>
 *
 * <p>Without enabling <code>CameraX Extensions</code>, any device should be able to support the
 * use cases combination of {@link ImageCapture}, {@link Preview} and {@link ImageAnalysis}. To
 * support the <code>CameraX Extensions</code> functionality, the {@link ImageCapture} or
 * {@link Preview} might need to occupy a different format of stream. This might restrict the app
 * to not be able to bind {@link ImageCapture}, {@link Preview} and {@link ImageAnalysis} at the
 * same time if the device's hardware level is not
 * {@link CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_FULL} or above. If enabling an extension
 * mode is more important and the {@link ImageAnalysis} could be optional to the app design, the
 * extension mode can be enabled successfully when only binding {@link ImageCapture},
 * {@link Preview} even if the device's hardware level is
 * {@link CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED}.
 *
 * <p><code>CameraX Extensions</code> currently can only support {@link ImageCapture} and
 * {@link Preview}. The {@linkplain androidx.camera.video.VideoCapture} can't be supported yet.
 * If the app binds {@linkplain androidx.camera.video.VideoCapture} and
 * enables any extension mode, an {@link IllegalArgumentException} will be thrown.
 *
 * <p>For some devices, the vendor library implementation might only support a subset of the all
 * supported sizes retrieved by {@link StreamConfigurationMap#getOutputSizes(int)}. <code>CameraX
 * </code> will select the supported sizes for the use cases according to the use cases'
 * configuration and combination.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExtensionsManager {
    private static final String TAG = "ExtensionsManager";

    enum ExtensionsAvailability {
        /**
         * The device extensions library exists and has been correctly loaded.
         */
        LIBRARY_AVAILABLE,
        /**
         * The device extensions library exists. However, there was some error loading the library.
         */
        LIBRARY_UNAVAILABLE_ERROR_LOADING,
        /**
         * The device extensions library exists. However, the library is missing implementations.
         */
        LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION,
        /**
         * There are no extensions available on this device.
         */
        NONE
    }

    // Singleton instance of the Extensions object
    private static final Object EXTENSIONS_LOCK = new Object();

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<ExtensionsManager> sInitializeFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<Void> sDeinitializeFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ExtensionsManager sExtensionsManager;

    private final ExtensionsAvailability mExtensionsAvailability;

    private final ExtensionsInfo mExtensionsInfo;

    /**
     * Retrieves the {@link ExtensionsManager} associated with the current process.
     *
     * <p>An application must wait until the {@link ListenableFuture} completes to get an
     * {@link ExtensionsManager} instance. The {@link ExtensionsManager} instance can be used to
     * access the extensions related functions.
     *
     * @param context The context to initialize the extensions library.
     * @param cameraProvider     A {@link CameraProvider} will be used to query the information
     *                           of cameras on the device. The {@link CameraProvider} can be the
     *                           {@link androidx.camera.lifecycle.ProcessCameraProvider}
     *                           which is obtained by
     *                 {@link androidx.camera.lifecycle.ProcessCameraProvider#getInstance(Context)}.
     */
    @NonNull
    public static ListenableFuture<ExtensionsManager> getInstanceAsync(@NonNull Context context,
            @NonNull CameraProvider cameraProvider) {
        return getInstanceAsync(context, cameraProvider, VersionName.getCurrentVersion());
    }

    static ListenableFuture<ExtensionsManager> getInstanceAsync(@NonNull Context context,
            @NonNull CameraProvider cameraProvider, @NonNull VersionName versionName) {
        synchronized (EXTENSIONS_LOCK) {
            if (sDeinitializeFuture != null && !sDeinitializeFuture.isDone()) {
                throw new IllegalStateException("Not yet done deinitializing extensions");
            }
            sDeinitializeFuture = null;

            // Will be initialized, with an empty implementation which will report all extensions
            // as unavailable
            if (ExtensionVersion.getRuntimeVersion() == null) {
                return Futures.immediateFuture(
                        getOrCreateExtensionsManager(ExtensionsAvailability.NONE, cameraProvider));
            }

            // Prior to 1.1 no additional initialization logic required
            if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) < 0) {
                return Futures.immediateFuture(
                        getOrCreateExtensionsManager(ExtensionsAvailability.LIBRARY_AVAILABLE,
                                cameraProvider));
            }

            if (sInitializeFuture == null) {
                sInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
                    try {
                        InitializerImpl.init(versionName.toVersionString(),
                                ContextUtil.getApplicationContext(context),
                                new InitializerImpl.OnExtensionsInitializedCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Logger.d(TAG, "Successfully initialized extensions");
                                        completer.set(getOrCreateExtensionsManager(
                                                ExtensionsAvailability.LIBRARY_AVAILABLE,
                                                cameraProvider));
                                    }

                                    @Override
                                    public void onFailure(int error) {
                                        Logger.e(TAG, "Failed to initialize extensions");
                                        completer.set(getOrCreateExtensionsManager(
                                                ExtensionsAvailability
                                                        .LIBRARY_UNAVAILABLE_ERROR_LOADING,
                                                cameraProvider));
                                    }
                                },
                                CameraXExecutors.directExecutor());
                    } catch (NoSuchMethodError | NoClassDefFoundError | AbstractMethodError e) {
                        Logger.e(TAG, "Failed to initialize extensions. Some classes or methods "
                                + "are missed in the vendor library. " + e);
                        completer.set(getOrCreateExtensionsManager(
                                ExtensionsAvailability.LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION,
                                cameraProvider));
                    } catch (RuntimeException e) {
                        // Catches all unexpected runtime exceptions and still returns an
                        // ExtensionsManager instance which performs default behavior.
                        Logger.e(TAG,
                                "Failed to initialize extensions. Something wents wrong when "
                                        + "initializing the vendor library. "
                                        + e);
                        completer.set(getOrCreateExtensionsManager(
                                ExtensionsAvailability.LIBRARY_UNAVAILABLE_ERROR_LOADING,
                                cameraProvider));
                    }

                    return "Initialize extensions";
                });
            }

            return sInitializeFuture;
        }
    }

    /**
     * Shutdown the extensions.
     *
     * <p> For the moment only used for testing to shutdown the extensions. Calling this function
     * can deinitialize the extensions vendor library and release the created
     * {@link ExtensionsManager} instance. Tests should wait until the returned future is
     * complete. Then, tests can call the
     * {@link ExtensionsManager#getInstanceAsync(Context, CameraProvider)} function again to
     * initialize a new {@link ExtensionsManager} instance.
     *
     * @hide
     */
    // TODO: Will need to be rewritten to be threadsafe with use in conjunction with
    //  ExtensionsManager.init(...) if this is to be released for use outside of testing.
    @VisibleForTesting
    @NonNull
    public ListenableFuture<Void> shutdown() {
        synchronized (EXTENSIONS_LOCK) {
            if (ExtensionVersion.getRuntimeVersion() == null) {
                sInitializeFuture = null;
                sExtensionsManager = null;
                return Futures.immediateFuture(null);
            }

            // If initialization not yet attempted then deinit should succeed immediately.
            if (sInitializeFuture == null) {
                return Futures.immediateFuture(null);
            }

            // If already in progress of deinit then return the future
            if (sDeinitializeFuture != null) {
                return sDeinitializeFuture;
            }

            ExtensionsAvailability availability;

            // Wait for the extension to be initialized before deinitializing. Block since
            // this is only used for testing.
            try {
                sInitializeFuture.get();
                sInitializeFuture = null;
                availability = sExtensionsManager.mExtensionsAvailability;
                sExtensionsManager = null;
            } catch (ExecutionException | InterruptedException e) {
                sDeinitializeFuture = Futures.immediateFailedFuture(e);
                return sDeinitializeFuture;
            }

            // Once extension has been initialized start the deinit call
            if (availability == ExtensionsAvailability.LIBRARY_AVAILABLE) {
                sDeinitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
                    try {
                        InitializerImpl.deinit(
                                new InitializerImpl.OnExtensionsDeinitializedCallback() {
                                    @Override
                                    public void onSuccess() {
                                        completer.set(null);
                                    }

                                    @Override
                                    public void onFailure(int error) {
                                        completer.setException(new Exception("Failed to "
                                                + "deinitialize extensions."));
                                    }
                                },
                                CameraXExecutors.directExecutor());
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        completer.setException(e);
                    }
                    return null;
                });
            } else {
                sDeinitializeFuture = Futures.immediateFuture(null);
            }
            return sDeinitializeFuture;
        }
    }

    static ExtensionsManager getOrCreateExtensionsManager(
            @NonNull ExtensionsAvailability extensionsAvailability,
            @NonNull CameraProvider cameraProvider) {
        synchronized (EXTENSIONS_LOCK) {
            if (sExtensionsManager != null) {
                return sExtensionsManager;
            }

            sExtensionsManager = new ExtensionsManager(extensionsAvailability, cameraProvider);

            return sExtensionsManager;
        }
    }

    /**
     * Returns a modified {@link CameraSelector} that will enable the specified extension mode.
     *
     * <p>The returned extension {@link CameraSelector} can be used to bind use cases to a
     * desired {@link LifecycleOwner} and then the specified extension mode will be enabled on
     * the camera.
     *
     * @param baseCameraSelector The base {@link CameraSelector} on top of which the extension
     *                           config is applied.
     *                           {@link #isExtensionAvailable(CameraSelector, int)} can be used
     *                           to check whether any camera can support the specified extension
     *                           mode for the base camera selector.
     * @param mode               The target extension mode.
     * @return a {@link CameraSelector} for the specified Extensions mode.
     * @throws IllegalArgumentException If this device doesn't support extensions function, no
     *                                  camera can be found to support the specified extension
     *                                  mode, or the base {@link CameraSelector} has contained
     *                                  extension related configuration in it.
     */
    @NonNull
    public CameraSelector getExtensionEnabledCameraSelector(
            @NonNull CameraSelector baseCameraSelector, @ExtensionMode.Mode int mode) {
        // Directly return the input baseCameraSelector if the target extension mode is NONE.
        if (mode == ExtensionMode.NONE) {
            return baseCameraSelector;
        }

        if (mExtensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            throw new IllegalArgumentException("This device doesn't support extensions function! "
                    + "isExtensionAvailable should be checked first before calling "
                    + "getExtensionEnabledCameraSelector.");
        }

        return mExtensionsInfo.getExtensionCameraSelectorAndInjectCameraConfig(baseCameraSelector,
                mode);
    }

    /**
     * Returns true if the particular extension mode is available for the specified
     * {@link CameraSelector}.
     *
     * @param baseCameraSelector The base {@link CameraSelector} to find a camera to use.
     * @param mode               The target extension mode to support.
     */
    public boolean isExtensionAvailable(@NonNull CameraSelector baseCameraSelector,
            @ExtensionMode.Mode int mode) {
        if (mode == ExtensionMode.NONE) {
            return true;
        }

        if (mExtensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            // Returns false if extensions are not available.
            return false;
        }

        return mExtensionsInfo.isExtensionAvailable(baseCameraSelector, mode);
    }

    /**
     * Returns the estimated capture latency range in milliseconds for the target camera and
     * extension mode.
     *
     * <p>This includes the time spent processing the multi-frame capture request along with any
     * additional time for encoding of the processed buffer in the framework if necessary.
     *
     * @param cameraSelector    The {@link CameraSelector} to find a camera which supports the
     *                          specified extension mode.
     * @param mode              The extension mode to check.
     * @return the range of estimated minimal and maximal capture latency in milliseconds.
     * Returns null if no capture latency info can be provided.
     * @throws IllegalArgumentException If this device doesn't support extensions function, or no
     *                                  camera can be found to support the specified extension mode.
     */
    @Nullable
    public Range<Long> getEstimatedCaptureLatencyRange(@NonNull CameraSelector cameraSelector,
            @ExtensionMode.Mode int mode) {
        if (mode == ExtensionMode.NONE
                || mExtensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            throw new IllegalArgumentException(
                    "No camera can be found to support the specified extensions mode! "
                            + "isExtensionAvailable should be checked first before calling "
                            + "getEstimatedCaptureLatencyRange.");
        }

        return mExtensionsInfo.getEstimatedCaptureLatencyRange(cameraSelector, mode, null);
    }

    @VisibleForTesting
    @NonNull
    ExtensionsAvailability getExtensionsAvailability() {
        return mExtensionsAvailability;
    }

    private ExtensionsManager(@NonNull ExtensionsAvailability extensionsAvailability,
            @NonNull CameraProvider cameraProvider) {
        mExtensionsAvailability = extensionsAvailability;
        mExtensionsInfo = new ExtensionsInfo(cameraProvider);
    }
}
