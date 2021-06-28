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
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
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
 */
public final class ExtensionsManager {
    private static final String TAG = "ExtensionsManager";

    /**
     * The effect mode options applied on the bound use cases
     *
     * @deprecated Use {@link ExtensionMode} to call the new
     * {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)} and
     * {@link #getExtensionEnabledCameraSelector(CameraProvider, CameraSelector, int)} APIs.
     */
    @Deprecated
    public enum EffectMode {
        /** Normal mode without any specific effect applied. */
        NORMAL,
        /** Bokeh mode that is often applied as portrait mode for people pictures. */
        BOKEH,
        /**
         * HDR mode that may get source pictures with different AE settings to generate a best
         * result.
         */
        HDR,
        /**
         * Night mode is used for taking better still capture images under low-light situations,
         * typically at night time.
         */
        NIGHT,
        /**
         * Beauty mode is used for taking still capture images that incorporate facial changes
         * like skin tone, geometry, or retouching.
         */
        BEAUTY,
        /**
         * Auto mode is used for taking still capture images that automatically adjust to the
         * surrounding scenery.
         */
        AUTO
    }

    public enum ExtensionsAvailability {
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

    private static final Object ERROR_LOCK = new Object();

    @GuardedBy("ERROR_LOCK")
    private static final Handler DEFAULT_HANDLER = new Handler(Looper.getMainLooper());
    @SuppressWarnings("deprecation")
    @GuardedBy("ERROR_LOCK")
    private static volatile ExtensionsErrorListener sExtensionsErrorListener = null;

    // Singleton instance of the Extensions object
    private static final Object EXTENSIONS_LOCK = new Object();

    @GuardedBy("EXTENSIONS_LOCK")
    static boolean sInitialized = false;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<ExtensionsAvailability> sAvailabilityFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<Void> sDeinitFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<ExtensionsManager> sInitializeFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ListenableFuture<Void> sDeinitializeFuture;

    @GuardedBy("EXTENSIONS_LOCK")
    private static ExtensionsManager sExtensionsManager;

    private final ExtensionsAvailability mExtensionsAvailability;

    /**
     * Initialize the extensions asynchronously.
     *
     * <p>This should be the first call to the extensions module. An application must wait until the
     * {@link ListenableFuture} completes before making any other calls to the extensions module.
     *
     * @deprecated Use {@link #getInstance(Context)} to obtain an {@link ExtensionsManager}
     * instance to access the extensions functions.
     */
    @Deprecated
    @NonNull
    public static ListenableFuture<ExtensionsAvailability> init(@NonNull Context context) {
        synchronized (EXTENSIONS_LOCK) {
            if (sDeinitFuture != null && !sDeinitFuture.isDone()) {
                throw new IllegalStateException("Not yet done deinitializing extensions");
            }
            sDeinitFuture = null;

            // Will be initialized, with an empty implementation which will report all extensions
            // as unavailable
            if (ExtensionVersion.getRuntimeVersion() == null) {
                setInitialized(true);
                return Futures.immediateFuture(ExtensionsAvailability.NONE);
            }

            // Prior to 1.1 no additional initialization logic required
            if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) < 0) {
                setInitialized(true);
                return Futures.immediateFuture(
                        ExtensionsAvailability.LIBRARY_AVAILABLE);
            }

            if (sAvailabilityFuture == null) {
                sAvailabilityFuture = CallbackToFutureAdapter.getFuture(completer -> {
                    try {
                        InitializerImpl.init(VersionName.getCurrentVersion().toVersionString(),
                                context,
                                new InitializerImpl.OnExtensionsInitializedCallback() {
                                @Override
                                public void onSuccess() {
                                    Logger.d(TAG, "Successfully initialized extensions");
                                    setInitialized(true);
                                    completer.set(ExtensionsAvailability.LIBRARY_AVAILABLE);
                                }

                                @Override
                                public void onFailure(int error) {
                                    Logger.d(TAG, "Failed to initialize extensions");
                                    completer.set(ExtensionsAvailability
                                            .LIBRARY_UNAVAILABLE_ERROR_LOADING);
                                }
                                },
                                CameraXExecutors.mainThreadExecutor());
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        completer.set(
                                ExtensionsAvailability.LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION);
                    }

                    return "Initialize extensions";
                });
            }

            return sAvailabilityFuture;
        }
    }

    // protected method so that EXTENSIONS_LOCK can be kept private
    static void setInitialized(boolean initialized) {
        synchronized (EXTENSIONS_LOCK) {
            sInitialized = initialized;
        }
    }

    /**
     * Deinitialize the extensions.
     *
     * <p> For the moment only used for testing to deinitialize the extensions. Immediately after
     * this has been called, the extensions functions will no longer work. Calling the extensions
     * functions in the situation will cause IllegalStateExceptions. The deinitialization process
     * is asynchronous. Tests should wait until the returned future is complete..
     *
     * @hide
     */
    // TODO: Will need to be rewritten to be threadsafe with use in conjunction with
    //  ExtensionsManager.init(...) if this is to be released for use outside of testing.
    @RestrictTo(RestrictTo.Scope.TESTS)
    @NonNull
    public static ListenableFuture<Void> deinit() {
        synchronized (EXTENSIONS_LOCK) {
            setInitialized(false);
            if (ExtensionVersion.getRuntimeVersion() == null) {
                return Futures.immediateFuture(null);
            }

            // If initialization not yet attempted then deinit should succeed immediately.
            if (sAvailabilityFuture == null) {
                return Futures.immediateFuture(null);
            }

            // If already in progress of deinit then return the future
            if (sDeinitFuture != null) {
                return sDeinitFuture;
            }

            // Wait for the extension to be initialized before deinitializing. Block since
            // this is only used for testing.
            ExtensionsAvailability availability;
            try {
                availability = sAvailabilityFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                sDeinitFuture = Futures.immediateFailedFuture(e);
                return sDeinitFuture;
            }

            sAvailabilityFuture = null;

            // Once extension has been initialized start the deinit call
            if (availability == ExtensionsAvailability.LIBRARY_AVAILABLE) {
                sDeinitFuture = CallbackToFutureAdapter.getFuture(completer -> {
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
                                CameraXExecutors.mainThreadExecutor());
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        completer.setException(e);
                    }
                    return null;
                });
            } else {
                sDeinitFuture = Futures.immediateFuture(null);
            }
            return sDeinitFuture;
        }
    }

    /**
     * Indicates whether the camera device with the lensFacing can support the specific
     * extension function.
     *
     * @param effectMode The extension function to be checked.
     * @param lensFacing The lensFacing of the camera device to be checked.
     * @return True if the specific extension function is supported for the camera device.
     * @deprecated Use {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)} to
     * check whether extension function can support with the given {@link CameraSelector}.
     */
    @Deprecated
    public static boolean isExtensionAvailable(@NonNull EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        boolean isImageCaptureAvailable = checkImageCaptureExtensionCapability(effectMode,
                lensFacing);
        boolean isPreviewAvailable = checkPreviewExtensionCapability(effectMode, lensFacing);

        if (isImageCaptureAvailable != isPreviewAvailable) {
            Logger.e(TAG, "ImageCapture and Preview are not available simultaneously for "
                    + effectMode.name());
        }

        return isImageCaptureAvailable && isPreviewAvailable;
    }

    /**
     * Indicates whether the camera device with the lensFacing can support the specific
     * extension function for specific use case.
     *
     * @param klass      The {@link ImageCapture} or {@link Preview} class to be checked.
     * @param effectMode The extension function to be checked.
     * @param lensFacing The lensFacing of the camera device to be checked.
     * @return True if the specific extension function is supported for the camera device.
     * @deprecated Use {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)} to
     * check whether extension function can support with the given {@link CameraSelector}.
     */
    @Deprecated
    public static boolean isExtensionAvailable(@NonNull Class<?> klass,
            @NonNull EffectMode effectMode, @CameraSelector.LensFacing int lensFacing) {
        boolean isAvailable = false;

        if (klass == ImageCapture.class) {
            isAvailable = checkImageCaptureExtensionCapability(effectMode, lensFacing);
        } else if (klass.equals(Preview.class)) {
            isAvailable = checkPreviewExtensionCapability(effectMode, lensFacing);
        }

        return isAvailable;
    }

    /**
     * Retrieves the {@link ExtensionsManager} associated with the current process.
     *
     * <p>An application must wait until the {@link ListenableFuture} completes to get an
     * {@link ExtensionsManager} instance. The {@link ExtensionsManager} instance can be used to
     * access the extensions related functions.
     */
    @NonNull
    public static ListenableFuture<ExtensionsManager> getInstance(@NonNull Context context) {
        return getInstance(context, VersionName.getCurrentVersion());
    }

    static ListenableFuture<ExtensionsManager> getInstance(@NonNull Context context,
            @NonNull VersionName versionName) {
        synchronized (EXTENSIONS_LOCK) {
            if (sDeinitializeFuture != null && !sDeinitializeFuture.isDone()) {
                throw new IllegalStateException("Not yet done deinitializing extensions");
            }
            sDeinitializeFuture = null;

            // Will be initialized, with an empty implementation which will report all extensions
            // as unavailable
            if (ExtensionVersion.getRuntimeVersion() == null) {
                return Futures.immediateFuture(
                        getOrCreateExtensionsManager(ExtensionsAvailability.NONE));
            }

            // Prior to 1.1 no additional initialization logic required
            if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) < 0) {
                return Futures.immediateFuture(
                        getOrCreateExtensionsManager(ExtensionsAvailability.LIBRARY_AVAILABLE));
            }

            if (sInitializeFuture == null) {
                sInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
                    try {
                        InitializerImpl.init(versionName.toVersionString(),
                                context,
                                new InitializerImpl.OnExtensionsInitializedCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Logger.d(TAG, "Successfully initialized extensions");
                                        completer.set(getOrCreateExtensionsManager(
                                                ExtensionsAvailability.LIBRARY_AVAILABLE));
                                    }

                                    @Override
                                    public void onFailure(int error) {
                                        Logger.e(TAG, "Failed to initialize extensions");
                                        completer.set(getOrCreateExtensionsManager(
                                                ExtensionsAvailability
                                                        .LIBRARY_UNAVAILABLE_ERROR_LOADING));
                                    }
                                },
                                CameraXExecutors.directExecutor());
                    } catch (NoSuchMethodError | NoClassDefFoundError e) {
                        Logger.e(TAG, "Failed to initialize extensions. Some classes or methods "
                                + "are missed in the vendor library. " + e);
                        completer.set(getOrCreateExtensionsManager(
                                ExtensionsAvailability.LIBRARY_UNAVAILABLE_MISSING_IMPLEMENTATION));
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
     * complete. Then, tests can call the {@link ExtensionsManager#getInstance(Context)} function
     * again to initialize a new {@link ExtensionsManager} instance.
     *
     * @hide
     */
    // TODO: Will need to be rewritten to be threadsafe with use in conjunction with
    //  ExtensionsManager.init(...) if this is to be released for use outside of testing.
    @RestrictTo(RestrictTo.Scope.TESTS)
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
            ExtensionsAvailability extensionsAvailability) {
        synchronized (EXTENSIONS_LOCK) {
            if (sExtensionsManager != null) {
                return sExtensionsManager;
            }

            sExtensionsManager = new ExtensionsManager(extensionsAvailability);

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
     * @param cameraProvider     A {@link CameraProvider} which will be used to bind use cases. The
     *                           {@link CameraProvider} can be the
     *                           {@link androidx.camera.lifecycle.ProcessCameraProvider}
     *                           which is obtained by
     *                 {@link androidx.camera.lifecycle.ProcessCameraProvider#getInstance(Context)}.
     * @param baseCameraSelector The base {@link CameraSelector} on top of which the extension
     *                           config is applied.
     *                           {@link #isExtensionAvailable(CameraProvider, CameraSelector, int)}
     *                           can be used to check whether any camera can support the specified
     *                           extension mode for the base camera selector.
     * @param mode               The target extension mode.
     * @return a {@link CameraSelector} for the specified Extensions mode.
     * @throws IllegalArgumentException If this device doesn't support extensions function, no
     *                                  camera can be found to support the specified extension
     *                                  mode, or the base {@link CameraSelector} has contained
     *                                  extension related configuration in it.
     */
    @NonNull
    public CameraSelector getExtensionEnabledCameraSelector(@NonNull CameraProvider cameraProvider,
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

        return ExtensionsInfo.getExtensionCameraSelectorAndInjectCameraConfig(cameraProvider,
                baseCameraSelector, mode);
    }

    /**
     * Returns true if the particular extension mode is available for the specified
     * {@link CameraSelector}.
     *
     * @param cameraProvider     A {@link CameraProvider} which will be used to bind use cases. The
     *                           {@link CameraProvider} can be the
     *                           {@link androidx.camera.lifecycle.ProcessCameraProvider}
     *                           which is obtained by
     *                 {@link androidx.camera.lifecycle.ProcessCameraProvider#getInstance(Context)}.
     * @param baseCameraSelector The base {@link CameraSelector} to find a camera to use.
     * @param mode               The target extension mode to support.
     */
    public boolean isExtensionAvailable(@NonNull CameraProvider cameraProvider,
            @NonNull CameraSelector baseCameraSelector, @ExtensionMode.Mode int mode) {
        if (mode == ExtensionMode.NONE) {
            return true;
        }

        if (mExtensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            // Returns false if extensions are not available.
            return false;
        }

        return ExtensionsInfo.isExtensionAvailable(cameraProvider, baseCameraSelector, mode);
    }

    /**
     * Returns the estimated capture latency range in milliseconds for the target capture
     * resolution.
     *
     * <p>This includes the time spent processing the multi-frame capture request along with any
     * additional time for encoding of the processed buffer in the framework if necessary.
     *
     * @param cameraProvider    A {@link CameraProvider} which will be used to bind use cases. The
     *                          {@link CameraProvider} can be the
     *                          {@link androidx.camera.lifecycle.ProcessCameraProvider}
     *                          which is obtained by
     *                 {@link androidx.camera.lifecycle.ProcessCameraProvider#getInstance(Context)}.
     * @param cameraSelector    The {@link CameraSelector} to find a camera which supports the
     *                          specified extension mode.
     * @param mode              The extension mode to check.
     * @param surfaceResolution the surface resolution of the {@link ImageCapture} which will be
     *                          used to take a picture. If the input value of this parameter is
     *                          null or it is not included in the supported output sizes, the
     *                          maximum capture output size is used to get the estimated range
     *                          information.
     * @return the range of estimated minimal and maximal capture latency in milliseconds.
     * Returns null if no capture latency info can be provided.
     * @throws IllegalArgumentException If this device doesn't support extensions function, or no
     *                                  camera can be found to support the specified extension mode.
     */
    @Nullable
    public Range<Long> getEstimatedCaptureLatencyRange(@NonNull CameraProvider cameraProvider,
            @NonNull CameraSelector cameraSelector, @ExtensionMode.Mode int mode,
            @Nullable Size surfaceResolution) {
        if (mode == ExtensionMode.NONE
                || mExtensionsAvailability != ExtensionsAvailability.LIBRARY_AVAILABLE) {
            throw new IllegalArgumentException(
                    "No camera can be found to support the specified extensions mode! "
                            + "isExtensionAvailable should be checked first before calling "
                            + "getEstimatedCaptureLatencyRange.");
        }

        return ExtensionsInfo.getEstimatedCaptureLatencyRange(cameraProvider, cameraSelector, mode,
                surfaceResolution);
    }

    @VisibleForTesting
    @NonNull
    ExtensionsAvailability getExtensionsAvailability() {
        return mExtensionsAvailability;
    }

    @SuppressWarnings("deprecation")
    private static boolean checkImageCaptureExtensionCapability(EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        ImageCaptureExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = BokehImageCaptureExtender.create(builder);
                break;
            case HDR:
                extender = HdrImageCaptureExtender.create(builder);
                break;
            case NIGHT:
                extender = NightImageCaptureExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyImageCaptureExtender.create(builder);
                break;
            case AUTO:
                extender = AutoImageCaptureExtender.create(builder);
                break;
            case NORMAL:
                return true;
            default:
                return false;
        }

        return extender.isExtensionAvailable(selector);
    }

    /**
     * Sets an {@link ExtensionsErrorListener} which will get called any time an
     * extensions error is encountered.
     *
     * @param listener The {@link ExtensionsErrorListener} listener that will be run.
     *
     * @deprecated Currently, this is only used to monitor whether a {@link Preview} or
     * {@link ImageCapture} is lacking when enabling extension modes. CameraX will automatically
     * add an extra {@link Preview} or {@link ImageCapture} to make the extension functions work
     *  well. After that, no error will be reported via this interface.
     */
    @Deprecated
    public static void setExtensionsErrorListener(@Nullable ExtensionsErrorListener listener) {
        synchronized (ERROR_LOCK) {
            sExtensionsErrorListener = listener;
        }
    }

    /**
     * Posts extension error to the listener.
     *
     * @hide
     */
    @SuppressWarnings("deprecation")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void postExtensionsError(
            @NonNull ExtensionsErrorListener.ExtensionsErrorCode errorCode) {
        synchronized (ERROR_LOCK) {
            final ExtensionsErrorListener listenerReference = sExtensionsErrorListener;
            if (listenerReference != null) {
                DEFAULT_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listenerReference.onError(errorCode);
                    }
                });
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean checkPreviewExtensionCapability(EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        Preview.Builder builder = new Preview.Builder();
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        PreviewExtender extender;

        switch (effectMode) {
            case BOKEH:
                extender = BokehPreviewExtender.create(builder);
                break;
            case HDR:
                extender = HdrPreviewExtender.create(builder);
                break;
            case NIGHT:
                extender = NightPreviewExtender.create(builder);
                break;
            case BEAUTY:
                extender = BeautyPreviewExtender.create(builder);
                break;
            case AUTO:
                extender = AutoPreviewExtender.create(builder);
                break;
            case NORMAL:
                return true;
            default:
                return false;
        }

        return extender.isExtensionAvailable(cameraSelector);
    }

    private ExtensionsManager(@NonNull ExtensionsAvailability extensionsAvailability) {
        mExtensionsAvailability = extensionsAvailability;
    }
}
