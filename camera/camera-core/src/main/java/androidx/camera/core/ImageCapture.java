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

package androidx.camera.core;

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_BUFFER_FORMAT;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_FLASH_MODE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_FLASH_TYPE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IMAGE_READER_PROXY_PROVIDER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IO_EXECUTOR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_JPEG_COMPRESSION_QUALITY;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_RESOLUTION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_USE_CASE_EVENT_CALLBACK;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_HIGH_RESOLUTION_DISABLED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.internal.utils.ImageUtil.computeCropRectFromAspectRatio;
import static androidx.camera.core.internal.utils.ImageUtil.isAspectRatioValid;
import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy.OnImageCloseListener;
import androidx.camera.core.imagecapture.ImageCaptureControl;
import androidx.camera.core.imagecapture.ImagePipeline;
import androidx.camera.core.imagecapture.TakePictureManager;
import androidx.camera.core.imagecapture.TakePictureRequest;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.IoConfig;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A use case for taking a picture.
 *
 * <p>This class is designed for basic picture taking. It provides takePicture() functions to take
 * a picture to memory or save to a file, and provides image metadata.  Pictures are taken in
 * automatic mode after focus has converged. The flash mode can additionally be set by the
 * application.
 *
 * <p>TakePicture returns immediately and a listener is called to provide the results after the
 * capture completes. Multiple calls to takePicture will take pictures sequentially starting
 * after the previous picture is captured.
 *
 * <p>Note that focus and exposure metering regions can be controlled via {@link Preview}.
 *
 * <p>When capturing to memory, the captured image is made available through an {@link ImageProxy}
 * via an {@link ImageCapture.OnImageCapturedCallback}.
 */
@SuppressWarnings("unused")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageCapture extends UseCase {

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime constant] - Stays constant for the lifetime of the UseCase. Which means
    // they could be created in the constructor.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * An unknown error occurred.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    public static final int ERROR_UNKNOWN = 0;
    /**
     * An error occurred while attempting to read or write a file, such as when saving an image
     * to a File.
     */
    public static final int ERROR_FILE_IO = 1;

    /**
     * An error reported by camera framework indicating the capture request is failed.
     */
    public static final int ERROR_CAPTURE_FAILED = 2;

    /**
     * An error indicating the request cannot be done due to camera is closed.
     */
    public static final int ERROR_CAMERA_CLOSED = 3;

    /**
     * An error indicating this ImageCapture is not bound to a valid camera.
     */
    public static final int ERROR_INVALID_CAMERA = 4;

    /**
     * Optimizes capture pipeline to prioritize image quality over latency. When the capture
     * mode is set to MAX_QUALITY, images may take longer to capture.
     */
    public static final int CAPTURE_MODE_MAXIMIZE_QUALITY = 0;
    /**
     * Optimizes capture pipeline to prioritize latency over image quality. When the capture
     * mode is set to MIN_LATENCY, images may capture faster but the image quality may be
     * reduced.
     */
    public static final int CAPTURE_MODE_MINIMIZE_LATENCY = 1;
    /**
     * Optimizes capture pipeline to have better latency while keeping good image quality. When
     * the capture mode is set to ZERO_SHUTTER_LAG, the latency between the shutter button is
     * clicked and the picture is taken is expected to be minimized, compared with other capture
     * modes.
     *
     * <p> ZERO_SHUTTER_LAG mode is aiming to provide the minimum latency for instant capture. It
     * caches intermediate results and deliver the one with the closest timestamp when
     * {@link ImageCapture#takePicture(OutputFileOptions, Executor, OnImageSavedCallback)}
     * is invoked.
     *
     * <p> {@link CameraInfo#isZslSupported()} can be used to query the device capability to
     * support this mode or not. However, this mode also depends on use cases configuration and
     * flash mode settings. If VideoCapture is bound or flash mode is not OFF or
     * OEM Extension is ON, this mode will be disabled automatically.
     */
    @ExperimentalZeroShutterLag
    public static final int CAPTURE_MODE_ZERO_SHUTTER_LAG = 2;

    /**
     * Auto flash. The flash will be used according to the camera system's determination when taking
     * a picture.
     */
    private static final int FLASH_MODE_UNKNOWN = -1;
    public static final int FLASH_MODE_AUTO = 0;
    /** Always flash. The flash will always be used when taking a picture. */
    public static final int FLASH_MODE_ON = 1;
    /** No flash. The flash will never be used when taking a picture. */
    public static final int FLASH_MODE_OFF = 2;

    /**
     * When flash is required for taking a picture, a normal one shot flash will be used.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int FLASH_TYPE_ONE_SHOT_FLASH = 0;
    /**
     * When flash is required for taking a picture, torch will be used as flash.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int FLASH_TYPE_USE_TORCH_AS_FLASH = 1;

    /**
     * Provides a static configuration with implementation-agnostic options.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "ImageCapture";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private static final int MAX_IMAGES = 2;
    // TODO(b/149336664) Move the quality to a compatibility class when there is a per device case.
    private static final byte JPEG_QUALITY_MAXIMIZE_QUALITY_MODE = 100;
    private static final byte JPEG_QUALITY_MINIMIZE_LATENCY_MODE = 95;
    @CaptureMode
    private static final int DEFAULT_CAPTURE_MODE = CAPTURE_MODE_MINIMIZE_LATENCY;
    @FlashMode
    private static final int DEFAULT_FLASH_MODE = FLASH_MODE_OFF;

    boolean mUseProcessingPipeline = true;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static final ExifRotationAvailability EXIF_ROTATION_AVAILABILITY =
            new ExifRotationAvailability();

    private final ImageReaderProxy.OnImageAvailableListener mClosingListener = imageReader -> {
        try (ImageProxy image = imageReader.acquireLatestImage()) {
            Log.d(TAG, "Discarding ImageProxy which was inadvertently acquired: " + image);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to acquire latest image.", e);
        }
    };

    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mIoExecutor;
    @CaptureMode
    private final int mCaptureMode;

    @GuardedBy("mLockedFlashMode")
    private final AtomicReference<Integer> mLockedFlashMode = new AtomicReference<>(null);

    @FlashType
    private final int mFlashType;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime dynamic] - Dynamic variables which could change during anytime during
    // the UseCase lifetime.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /** Current flash mode. */
    @GuardedBy("mLockedFlashMode")
    @FlashMode
    private int mFlashMode = FLASH_MODE_UNKNOWN;
    private Rational mCropAspectRatio = null;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached constant] - Is only valid when the UseCase is attached to a camera.
    ////////////////////////////////////////////////////////////////////////////////////////////
    private CaptureConfig mCaptureConfig;

    /**
     * Whether the software JPEG pipeline will be used.
     */
    private boolean mUseSoftwareJpeg = false;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SessionConfig.Builder mSessionConfigBuilder;

    /** synthetic accessor */
    @SuppressWarnings("WeakerAccess")
    SafeCloseImageReaderProxy mImageReader;

    /** Callback used to match the {@link ImageProxy} with the {@link ImageInfo}. */
    private CameraCaptureCallback mMetadataMatchingCaptureCallback;

    private DeferrableSurface mDeferrableSurface;

    private ImageCaptureRequestProcessor mImageCaptureRequestProcessor;
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final Executor mSequentialIoExecutor;

    /**
     * Creates a new image capture use case from the given configuration.
     *
     * @param userConfig for this use case instance
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    ImageCapture(@NonNull ImageCaptureConfig userConfig) {
        super(userConfig);

        ImageCaptureConfig useCaseConfig = (ImageCaptureConfig) getCurrentConfig();

        if (useCaseConfig.containsOption(OPTION_IMAGE_CAPTURE_MODE)) {
            mCaptureMode = useCaseConfig.getCaptureMode();
        } else {
            mCaptureMode = DEFAULT_CAPTURE_MODE;
        }

        mFlashType = useCaseConfig.getFlashType(FLASH_TYPE_ONE_SHOT_FLASH);

        mIoExecutor = checkNotNull(
                useCaseConfig.getIoExecutor(CameraXExecutors.ioExecutor()));
        mSequentialIoExecutor = CameraXExecutors.newSequentialExecutor(mIoExecutor);

    }

    @UiThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageCaptureConfig config, @NonNull StreamSpec streamSpec) {
        checkMainThread();
        if (isNodeEnabled()) {
            return createPipelineWithNode(cameraId, config, streamSpec);
        }
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());

        if (Build.VERSION.SDK_INT >= 23 && getCaptureMode() == CAPTURE_MODE_ZERO_SHUTTER_LAG) {
            getCameraControl().addZslConfig(sessionConfigBuilder);
        }

        // Setup the ImageReader to do processing
        Size resolution = streamSpec.getResolution();
        if (config.getImageReaderProxyProvider() != null) {
            mImageReader =
                    new SafeCloseImageReaderProxy(
                            config.getImageReaderProxyProvider().newInstance(resolution.getWidth(),
                                    resolution.getHeight(), getImageFormat(), MAX_IMAGES, 0));
            mMetadataMatchingCaptureCallback = new CameraCaptureCallback() {
            };
        } else if (isSessionProcessorEnabledInCurrentCamera()) {
            ImageReaderProxy imageReader;
            // SessionProcessor only outputs JPEG format.
            if (getImageFormat() == ImageFormat.JPEG) {
                // SessionProcessor can't guarantee that image and capture result have the same
                // time stamp. Thus we can't use MetadataImageReader
                imageReader = ImageReaderProxys.createIsolatedReader(resolution.getWidth(),
                        resolution.getHeight(), ImageFormat.JPEG, MAX_IMAGES);
                mMetadataMatchingCaptureCallback = new CameraCaptureCallback() {
                };
            } else {
                throw new IllegalArgumentException("Unsupported image format:" + getImageFormat());
            }
            mImageReader = new SafeCloseImageReaderProxy(imageReader);
        } else {
            MetadataImageReader metadataImageReader = new MetadataImageReader(resolution.getWidth(),
                    resolution.getHeight(), getImageFormat(), MAX_IMAGES);
            mMetadataMatchingCaptureCallback = metadataImageReader.getCameraCaptureCallback();
            mImageReader = new SafeCloseImageReaderProxy(metadataImageReader);
        }

        if (mImageCaptureRequestProcessor != null) {
            mImageCaptureRequestProcessor.cancelRequests(
                    new CancellationException("Request is canceled."));
        }

        mImageCaptureRequestProcessor = new ImageCaptureRequestProcessor(MAX_IMAGES,
                this::takePictureInternal);

        // By default close images that come from the listener.
        mImageReader.setOnImageAvailableListener(mClosingListener,
                CameraXExecutors.mainThreadExecutor());

        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }

        mDeferrableSurface = new ImmediateSurface(
                requireNonNull(mImageReader.getSurface()),
                new Size(mImageReader.getWidth(),
                        mImageReader.getHeight()),
                /* get the surface image format using getImageFormat */
                getImageFormat());

        mDeferrableSurface.getTerminationFuture().addListener(mImageReader::safeClose,
                CameraXExecutors.mainThreadExecutor());

        sessionConfigBuilder.addNonRepeatingSurface(mDeferrableSurface);

        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            // Get the unfinished requests before re-create the pipeline
            List<ImageCaptureRequest> pendingRequests = (mImageCaptureRequestProcessor != null)
                    ? mImageCaptureRequestProcessor.pullOutUnfinishedRequests()
                    : Collections.emptyList();

            clearPipeline();
            // Ensure the attached camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                mSessionConfigBuilder = createPipeline(cameraId, config, streamSpec);

                if (mImageCaptureRequestProcessor != null) {
                    // Restore the unfinished requests to the created pipeline
                    for (ImageCaptureRequest request : pendingRequests) {
                        mImageCaptureRequestProcessor.sendRequest(request);
                    }
                }

                updateSessionConfig(mSessionConfigBuilder.build());
                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    private boolean isSessionProcessorEnabledInCurrentCamera() {
        if (getCamera() == null) {
            return false;
        }

        CameraConfig cameraConfig = getCamera().getExtendedConfig();
        return cameraConfig.getSessionProcessor(null) != null;
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    @UiThread
    @SuppressWarnings("WeakerAccess")
    void clearPipeline() {
        checkMainThread();
        if (isNodeEnabled()) {
            clearPipelineWithNode();
            return;
        }
        if (mImageCaptureRequestProcessor != null) {
            mImageCaptureRequestProcessor.cancelRequests(
                    new CancellationException("Request is canceled."));
            mImageCaptureRequestProcessor = null;
        }
        DeferrableSurface deferrableSurface = mDeferrableSurface;
        mDeferrableSurface = null;
        mImageReader = null;

        if (deferrableSurface != null) {
            deferrableSurface.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                getCaptureMode());

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return Builder.fromConfig(config);
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        if (cameraInfo.getCameraQuirks().contains(SoftwareJpegEncodingPreferredQuirk.class)) {
            // Request software JPEG encoder if quirk exists on this device, and the software JPEG
            // option has not already been explicitly set.
            if (Boolean.FALSE.equals(builder.getMutableConfig().retrieveOption(
                    OPTION_USE_SOFTWARE_JPEG_ENCODER, true))) {
                Logger.w(TAG, "Device quirk suggests software JPEG encoder, but it has been "
                        + "explicitly disabled.");
            } else {
                Logger.i(TAG, "Requesting software JPEG due to device quirk.");
                builder.getMutableConfig().insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, true);
            }
        }

        // If software JPEG is requested, disable if it is incompatible.
        boolean useSoftwareJpeg = enforceSoftwareJpegConstraints(builder.getMutableConfig());

        // Update the input format base on the other options set (mainly whether processing
        // is done)
        Integer bufferFormat = builder.getMutableConfig().retrieveOption(OPTION_BUFFER_FORMAT,
                null);
        if (bufferFormat != null) {
            Preconditions.checkArgument(!(isSessionProcessorEnabledInCurrentCamera()
                            && bufferFormat != ImageFormat.JPEG),
                    "Cannot set non-JPEG buffer format with Extensions enabled.");
            builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                    useSoftwareJpeg ? ImageFormat.YUV_420_888 : bufferFormat);
        } else {
            if (useSoftwareJpeg) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                        ImageFormat.YUV_420_888);
            } else {
                List<Pair<Integer, Size[]>> supportedSizes =
                        builder.getMutableConfig().retrieveOption(OPTION_SUPPORTED_RESOLUTIONS,
                                null);
                if (supportedSizes == null) {
                    builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.JPEG);
                } else {
                    // Use Jpeg first if supported.
                    if (isImageFormatSupported(supportedSizes, ImageFormat.JPEG)) {
                        builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                                ImageFormat.JPEG);
                    } else if (isImageFormatSupported(supportedSizes, ImageFormat.YUV_420_888)) {
                        builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                                ImageFormat.YUV_420_888);
                    }
                }
            }
        }
        return builder.getUseCaseConfig();
    }

    private static boolean isImageFormatSupported(List<Pair<Integer, Size[]>> supportedSizes,
            int imageFormat) {
        if (supportedSizes == null) {
            return false;
        }
        for (Pair<Integer, Size[]> supportedSize : supportedSizes) {
            if (supportedSize.first.equals(imageFormat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configures flash mode to CameraControlInternal once it is ready.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onCameraControlReady() {
        trySetFlashModeToCameraControl();
    }

    /**
     * Get the flash mode.
     *
     * @return the flashMode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON}, or
     * {@link #FLASH_MODE_OFF}.
     */
    @FlashMode
    public int getFlashMode() {
        synchronized (mLockedFlashMode) {
            return mFlashMode != FLASH_MODE_UNKNOWN ? mFlashMode
                    : ((ImageCaptureConfig) getCurrentConfig()).getFlashMode(DEFAULT_FLASH_MODE);
        }
    }

    /**
     * Set the flash mode.
     *
     * <p>The flash control for the subsequent photo capture requests. Applications can check if
     * there is a flash unit via {@link CameraInfo#hasFlashUnit()} and update UI component if
     * necessary. If there is no flash unit, then calling this API will take no effect for the
     * subsequent photo capture requests and they will act like {@link #FLASH_MODE_OFF}.
     *
     * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
     * will remain enabled during photo capture regardless of flashMode setting. When
     * the torch is disabled, flash will function as specified by {@code setFlashMode(int)}.
     *
     * <p>On some LEGACY devices like Samsung A3, taking pictures with {@link #FLASH_MODE_AUTO}
     * mode could cause a crash. To workaround this CameraX will disable the auto flash behavior
     * internally on devices that have this issue.
     *
     * @param flashMode the flash mode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON},
     *                  or {@link #FLASH_MODE_OFF}.
     */
    public void setFlashMode(@FlashMode int flashMode) {
        if (flashMode != FLASH_MODE_AUTO && flashMode != FLASH_MODE_ON
                && flashMode != FLASH_MODE_OFF) {
            throw new IllegalArgumentException("Invalid flash mode: " + flashMode);
        }

        synchronized (mLockedFlashMode) {
            mFlashMode = flashMode;
            trySetFlashModeToCameraControl();
        }
    }

    /**
     * Sets target cropping aspect ratio for output image.
     *
     * <p>This aspect ratio is orientation-dependent. It should be expressed in the coordinate
     * frame after rotating the image by the target rotation.
     *
     * <p>This sets the cropping rectangle returned by {@link ImageProxy#getCropRect()} returned
     * from {@link ImageCapture#takePicture(Executor, OnImageCapturedCallback)}.
     *
     * <p>For example, assume the {@code aspectRatio} of 3x4. If an image has a resolution of
     * 480x640 after applying the target rotation, then the output {@link ImageProxy} of
     * {@link ImageCapture#takePicture(Executor, OnImageCapturedCallback)} would have a cropping
     * rectangle of 480x640 after applying the rotation degrees. However, if an image has a
     * resolution of 640x480 after applying the target rotation, then the cropping rectangle
     * of the output {@link ImageProxy} would be 360x480 after applying the rotation degrees.
     *
     * <p>This crops the saved image when calling
     * {@link ImageCapture#takePicture(OutputFileOptions, Executor, OnImageSavedCallback)}. Note
     * that the cropping will introduce an additional latency.
     *
     * <p>Cropping occurs around the center of the image and as though it were in the target
     * rotation. For example, assume the {@code aspectRatio} of 3x4. If an image has a resolution
     * of 480x640 after applying the target rotation, then the saved output image would be
     * 480x640 after applying the EXIF orientation value. However, if an image has a resolution
     * of 640x480 after applying the target rotation, then the saved output image would be
     * 360x480 after applying the EXIF orientation value.
     *
     * <p>This setting value will be automatically updated to match the new target rotation value
     * when {@link ImageCapture#setTargetRotation(int)} is called.
     *
     * @param aspectRatio New target aspect ratio.
     */
    public void setCropAspectRatio(@NonNull Rational aspectRatio) {
        mCropAspectRatio = aspectRatio;
    }

    /**
     * Returns the desired rotation of the output image.
     *
     * <p>The rotation can be set prior to constructing an ImageCapture using
     * {@link ImageCapture.Builder#setTargetRotation(int)} or dynamically by calling
     * {@link ImageCapture#setTargetRotation(int)} or
     * {@link ImageCapture#setTargetRotationDegrees(int)}. The rotation of an image taken is
     * determined by the rotation value set at the time image capture is initiated, such as when
     * calling {@link #takePicture(Executor, OnImageCapturedCallback)}.
     *
     * <p>If no target rotation is set by the application, it is set to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is
     * created. The use case is fully created once it has been attached to a camera.
     *
     * @return The rotation of the intended target.
     */
    @RotationValue
    public int getTargetRotation() {
        return getTargetRotationInternal();
    }

    /**
     * Sets the desired rotation of the output image.
     *
     * <p>This will affect the EXIF rotation metadata in images saved by takePicture calls and the
     * {@link ImageInfo#getRotationDegrees()} value of the {@link ImageProxy} returned by
     * {@link OnImageCapturedCallback}. These will be set to be the rotation, which if applied to
     * the output image data, will make the image match target rotation specified here.
     *
     * <p>While rotation can also be set via {@link Builder#setTargetRotation(int)}, using
     * {@code setTargetRotation(int)} allows the target rotation to be set dynamically.
     *
     * <p>In general, it is best to use an {@link android.view.OrientationEventListener} to
     * set the target rotation.  This way, the rotation output will indicate which way is down for
     * a given image.  This is important since display orientation may be locked by device
     * default, user setting, or app configuration, and some devices may not transition to a
     * reverse-portrait display orientation. In these cases,
     * use {@link #setTargetRotationDegrees} to set target rotation dynamically according to the
     * {@link android.view.OrientationEventListener}, without re-creating the use case.
     * See {@link #setTargetRotationDegrees} for more information.
     *
     * <p>When this function is called, value set by
     * {@link ImageCapture.Builder#setTargetResolution(Size)} will be updated automatically to make
     * sure the suitable resolution can be selected when the use case is bound. Value set by
     * {@link ImageCapture#setCropAspectRatio(Rational)} will also be updated automatically to
     * make sure the output image is cropped into expected aspect ratio.
     *
     * <p>If no target rotation is set by the application, it is set to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is bound. To
     * return to the default value, set the value to
     * <pre>{@code
     * context.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
     * }</pre>
     *
     * <p>takePicture uses the target rotation at the time it begins executing (which may be delayed
     * waiting on a previous takePicture call to complete).
     *
     * @param rotation Target rotation of the output image, expressed as one of
     *                 {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                 {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        int oldRotation = getTargetRotation();

        if (setTargetRotationInternal(rotation)) {
            // For the crop aspect ratio value, the numerator and denominator of original setting
            // value will be swapped then set back. It is an orientation-dependent value that will
            // be used to crop ImageCapture's output image.
            if (mCropAspectRatio != null) {
                int oldRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(
                        oldRotation);
                int newRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(rotation);
                mCropAspectRatio = ImageUtil.getRotatedAspectRatio(
                        Math.abs(newRotationDegrees - oldRotationDegrees), mCropAspectRatio);
            }

            // TODO(b/122846516): Update session configuration and possibly reconfigure session.
        }
    }

    /**
     * Sets the desired rotation of the output image in degrees.
     *
     * <p>In general, it is best to use an {@link  android.view.OrientationEventListener} to set
     * the target rotation. This way, the rotation output will indicate which way is down for a
     * given image. This is important since display orientation may be locked by device default,
     * user setting, or app configuration, and some devices may not transition to a
     * reverse-portrait display orientation. In these cases, use
     * {@code setTargetRotationDegrees()} to set target rotation dynamically according
     * to the {@link  android.view.OrientationEventListener}, without re-creating the use case.
     * The sample code is as below:
     * <pre>{@code
     * public class CameraXActivity extends AppCompatActivity {
     *
     *     private OrientationEventListener mOrientationEventListener;
     *
     *     @Override
     *     protected void onStart() {
     *         super.onStart();
     *         if (mOrientationEventListener == null) {
     *             mOrientationEventListener = new OrientationEventListener(this) {
     *                 @Override
     *                 public void onOrientationChanged(int orientation) {
     *                     if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
     *                         return;
     *                     }
     *                     mImageCapture.setTargetRotationDegrees(orientation);
     *                 }
     *             };
     *         }
     *         mOrientationEventListener.enable();
     *     }
     *
     *     @Override
     *     protected void onStop() {
     *         super.onStop();
     *         mOrientationEventListener.disable();
     *     }
     * }
     * }</pre>
     *
     * <p>{@code setTargetRotationDegrees()} cannot rotate the camera image to an arbitrary angle,
     * instead it maps the angle to one of {@link Surface#ROTATION_0},
     * {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180} and {@link Surface#ROTATION_270}
     * as the input of {@link #setTargetRotation(int)}. The rule is as follows:
     * <p>If the input degrees is not in the range [0..359], it will be converted to the equivalent
     * degrees in the range [0..359]. And then take the following mapping based on the input
     * degrees.
     * <p>degrees >= 315 || degrees < 45 -> {@link Surface#ROTATION_0}
     * <p>degrees >= 225 && degrees < 315 -> {@link Surface#ROTATION_90}
     * <p>degrees >= 135 && degrees < 225 -> {@link Surface#ROTATION_180}
     * <p>degrees >= 45 && degrees < 135 -> {@link Surface#ROTATION_270}
     * <p>The rotation value can be obtained by {@link #getTargetRotation()}. This means the
     * rotation previously set by {@link #setTargetRotation(int)} will be overridden by
     * {@code setTargetRotationDegrees(int)}, and vice versa.
     *
     * <p>When this function is called, value set by
     * {@link ImageCapture.Builder#setTargetResolution(Size)} will be updated automatically to make
     * sure the suitable resolution can be selected when the use case is bound. Value set by
     * {@link ImageCapture#setCropAspectRatio(Rational)} will also be updated automatically to
     * make sure the output image is cropped into expected aspect ratio.
     *
     * <p>takePicture uses the target rotation at the time it begins executing (which may be delayed
     * waiting on a previous takePicture call to complete).
     *
     * @param degrees Desired rotation degree of the output image.
     * @see #setTargetRotation(int)
     * @see #getTargetRotation()
     */
    public void setTargetRotationDegrees(int degrees) {
        setTargetRotation(orientationDegreesToSurfaceRotation(degrees));
    }

    /**
     * Returns the set capture mode.
     *
     * <p>This is set when constructing an ImageCapture using
     * {@link ImageCapture.Builder#setCaptureMode(int)}. This is static for an instance of
     * ImageCapture.
     */
    @CaptureMode
    public int getCaptureMode() {
        return mCaptureMode;
    }

    /**
     * Returns the JPEG quality setting.
     *
     * <p>This is set when constructing an ImageCapture using
     * {@link ImageCapture.Builder#setJpegQuality(int)}. If not set, a default value will be set
     * according to the capture mode setting. JPEG compression quality 95 is set for
     * {@link #CAPTURE_MODE_MINIMIZE_LATENCY} and 100 is set for
     * {@link #CAPTURE_MODE_MAXIMIZE_QUALITY}. This is static for an instance of ImageCapture.
     */
    @IntRange(from = 1, to = 100)
    public int getJpegQuality() {
        return getJpegQualityInternal();
    }

    /**
     * Gets selected resolution information of the {@link ImageCapture}.
     *
     * <p>The returned {@link ResolutionInfo} will be expressed in the coordinates of the camera
     * sensor. Note that the resolution might not be the same as the resolution of the received
     * image by calling {@link #takePicture} because the received image might have been rotated
     * to the upright orientation using the target rotation setting by the device.
     *
     * <p>The resolution information might change if the use case is unbound and then rebound,
     * {@link #setTargetRotation(int)} is called to change the target rotation setting, or
     * {@link #setCropAspectRatio(Rational)} is called to change the crop aspect ratio setting.
     * The application needs to call {@code getResolutionInfo()} again to get the latest
     * {@link ResolutionInfo} for the changes.
     *
     * @return the resolution information if the use case has been bound by the
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner
     *, CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     */
    @Nullable
    @Override
    public ResolutionInfo getResolutionInfo() {
        return super.getResolutionInfo();
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    protected ResolutionInfo getResolutionInfoInternal() {
        CameraInternal camera = getCamera();
        Size resolution = getAttachedSurfaceResolution();

        if (camera == null || resolution == null) {
            return null;
        }

        Rect cropRect = getViewPortCropRect();

        Rational cropAspectRatio = mCropAspectRatio;

        if (cropRect == null) {
            if (cropAspectRatio != null) {
                cropRect = ImageUtil.computeCropRectFromAspectRatio(resolution, cropAspectRatio);
            } else {
                cropRect = new Rect(0, 0, resolution.getWidth(), resolution.getHeight());
            }
        }

        int rotationDegrees = getRelativeRotation(camera);

        return ResolutionInfo.create(resolution, requireNonNull(cropRect), rotationDegrees);
    }

    /**
     * Returns the resolution selector setting.
     *
     * <p>This setting is set when constructing an ImageCapture using
     * {@link Builder#setResolutionSelector(ResolutionSelector)}.
     */
    @Nullable
    public ResolutionSelector getResolutionSelector() {
        return ((ImageOutputConfig) getCurrentConfig()).getResolutionSelector(null);
    }

    /**
     * Captures a new still image for in memory access.
     *
     * <p>The callback will be called only once for every invocation of this method. The listener
     * is responsible for calling {@link Image#close()} on the returned image.
     *
     * @param executor The executor in which the callback methods will be run.
     * @param callback Callback to be invoked for the newly captured image
     */
    public void takePicture(@NonNull Executor executor,
            final @NonNull OnImageCapturedCallback callback) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            CameraXExecutors.mainThreadExecutor().execute(() -> takePicture(executor, callback));
            return;
        }

        if (isNodeEnabled()) {
            takePictureWithNode(executor, callback, /*onDiskCallback=*/null,
                    /*outputFileOptions=*/null);
            return;
        }

        sendImageCaptureRequest(executor, callback, /*saveImage=*/false);
    }

    /**
     * Captures a new still image and saves to a file along with application specified metadata.
     *
     * <p> The callback will be called only once for every invocation of this method.
     *
     * <p> If the {@link ImageCapture} is in a {@link UseCaseGroup} where {@link ViewPort} is
     * set, or {@link #setCropAspectRatio} is used, the image may be cropped before saving to
     * disk which causes an additional latency.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     * @see ViewPort
     */
    public void takePicture(
            final @NonNull OutputFileOptions outputFileOptions,
            final @NonNull Executor executor,
            final @NonNull OnImageSavedCallback imageSavedCallback) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            CameraXExecutors.mainThreadExecutor().execute(
                    () -> takePicture(outputFileOptions, executor, imageSavedCallback));
            return;
        }
        if (isNodeEnabled()) {
            takePictureWithNode(executor, /*inMemoryCallback=*/null, imageSavedCallback,
                    outputFileOptions);
            return;
        }

        /*
         * We need to chain the following callbacks to save the image to disk:
         *
         * +-----------------------+
         * |                       |
         * |ImageCapture.          |
         * |OnImageCapturedCallback|
         * |                       |
         * +-----------+-----------+
         *             |
         *             |
         * +-----------v-----------+      +----------------------+
         * |                       |      |                      |
         * | ImageSaver.           |      | ImageCapture.        |
         * | OnImageSavedCallback  +------> OnImageSavedCallback |
         * |                       |      |                      |
         * +-----------------------+      +----------------------+
         */

        // Convert the ImageSaver.OnImageSavedCallback to ImageCapture.OnImageSavedCallback
        final ImageSaver.OnImageSavedCallback imageSavedCallbackWrapper =
                new ImageSaver.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull OutputFileResults outputFileResults) {
                        imageSavedCallback.onImageSaved(outputFileResults);
                    }

                    @Override
                    public void onError(@NonNull ImageSaver.SaveError error,
                            @NonNull String message,
                            @Nullable Throwable cause) {
                        @ImageCaptureError int imageCaptureError = ERROR_UNKNOWN;
                        // Keep the imageCaptureError as UNKNOWN_ERROR
                        if (error == ImageSaver.SaveError.FILE_IO_FAILED) {
                            imageCaptureError = ERROR_FILE_IO;
                        }

                        imageSavedCallback.onError(
                                new ImageCaptureException(imageCaptureError, message, cause));
                    }
                };

        int outputJpegQuality = getJpegQualityInternal();

        // Wrap the ImageCapture.OnImageSavedCallback with an OnImageCapturedCallback so it can
        // be put into the capture request queue
        OnImageCapturedCallback imageCaptureCallbackWrapper =
                new OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        mIoExecutor.execute(
                                new ImageSaver(
                                        image,
                                        outputFileOptions,
                                        image.getImageInfo().getRotationDegrees(),
                                        outputJpegQuality,
                                        executor,
                                        mSequentialIoExecutor,
                                        imageSavedCallbackWrapper));
                    }

                    @Override
                    public void onError(@NonNull final ImageCaptureException exception) {
                        imageSavedCallback.onError(exception);
                    }
                };

        // Always use the mainThreadExecutor for the initial callback so we don't need to double
        // post to another thread
        sendImageCaptureRequest(CameraXExecutors.mainThreadExecutor(),
                imageCaptureCallbackWrapper, /*saveImage=*/true);
    }

    @NonNull
    static Rect computeDispatchCropRect(@Nullable Rect viewPortCropRect,
            @Nullable Rational cropAspectRatio, int rotationDegrees,
            @NonNull Size dispatchResolution, int dispatchRotationDegrees) {
        if (viewPortCropRect != null) {
            return ImageUtil.computeCropRectFromDispatchInfo(viewPortCropRect, rotationDegrees,
                    dispatchResolution, dispatchRotationDegrees);
        } else if (cropAspectRatio != null) {
            // Fall back to crop aspect ratio if view port is not available.
            Rational aspectRatio = cropAspectRatio;
            if ((dispatchRotationDegrees % 180) != 0) {
                aspectRatio = new Rational(
                        /* invert the ratio numerator=*/ cropAspectRatio.getDenominator(),
                        /* invert the ratio denominator=*/ cropAspectRatio.getNumerator());
            }
            if (ImageUtil.isAspectRatioValid(dispatchResolution, aspectRatio)) {
                return requireNonNull(
                        ImageUtil.computeCropRectFromAspectRatio(dispatchResolution, aspectRatio));
            }
        }

        return new Rect(0, 0, dispatchResolution.getWidth(), dispatchResolution.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    @Override
    public void onStateDetached() {
        abortImageCaptureRequests();
    }

    @UiThread
    private void abortImageCaptureRequests() {
        if (mTakePictureManager != null) {
            mTakePictureManager.abortRequests();
        } else if (mImageCaptureRequestProcessor != null) {
            Throwable throwable = new CameraClosedException("Camera is closed.");
            mImageCaptureRequestProcessor.cancelRequests(throwable);
        }
    }

    @UiThread
    private void sendImageCaptureRequest(@NonNull Executor callbackExecutor,
            @NonNull OnImageCapturedCallback callback, boolean saveImage) {

        // TODO(b/143734846): From here on, the image capture request should be
        //  self-contained and use this camera for everything. Currently the pre-capture
        //  sequence does not follow this approach and could fail if this use case is unbound
        //  or reattached to a different camera in the middle of pre-capture.
        CameraInternal attachedCamera = getCamera();
        if (attachedCamera == null) {
            // Not bound. Notify callback.
            callbackExecutor.execute(
                    () -> callback.onError(new ImageCaptureException(ERROR_INVALID_CAMERA,
                            "Not bound to a valid Camera [" + ImageCapture.this + "]", null)));
            return;
        }

        if (mImageCaptureRequestProcessor == null) {
            callbackExecutor.execute(
                    () -> callback.onError(
                            new ImageCaptureException(ERROR_UNKNOWN, "Request is canceled", null)));
            return;
        }

        mImageCaptureRequestProcessor.sendRequest(new ImageCaptureRequest(
                getRelativeRotation(attachedCamera),
                getJpegQualityForImageCaptureRequest(attachedCamera, saveImage),
                mCropAspectRatio,
                getViewPortCropRect(),
                getSensorToBufferTransformMatrix(),
                callbackExecutor,
                callback));
    }

    @UiThread
    private int getJpegQualityForImageCaptureRequest(@NonNull CameraInternal cameraInternal,
            boolean saveImage) {
        int jpegQuality;
        if (saveImage) {
            int rotationDegrees = getRelativeRotation(cameraInternal);
            Size dispatchResolution = requireNonNull(getAttachedSurfaceResolution());
            // At this point, we can't know whether HAL will rotate the captured image or not. No
            // matter HAL will rotate the image byte array or not, it won't affect whether the final
            // image needs cropping or not. Therefore, we can still use the attached surface
            // resolution and its relative rotation degrees against to the target rotation
            // setting to calculate the possible crop rectangle and then use it to determine
            // whether the final image will need cropping or not.
            Rect cropRect = computeDispatchCropRect(getViewPortCropRect(), mCropAspectRatio,
                    rotationDegrees, dispatchResolution, rotationDegrees);
            boolean shouldCropImage = ImageUtil.shouldCropImage(dispatchResolution.getWidth(),
                    dispatchResolution.getHeight(), cropRect.width(), cropRect.height());
            if (shouldCropImage) {
                // When cropping is required, jpeg compression will occur twice:
                // 1. Jpeg quality set to camera HAL by camera capture request.
                // 2. Bitmap compression during cropping process in ImageSaver.
                // Here we need to define the first compression value and be careful to lose too
                // much quality due to double compression.
                // Setting 100 for the first compression can minimize quality loss, but will result
                // in poor performance during cropping than setting 95 (see b/206348741 for more
                // detail). As a trade-off, max quality mode is set to 100, and the others are set
                // to 95.
                jpegQuality = mCaptureMode == CAPTURE_MODE_MAXIMIZE_QUALITY ? 100 : 95;
            } else {
                jpegQuality = getJpegQualityInternal();
            }
        } else {
            // The captured image will be directly provided to the app via the
            // OnImageCapturedCallback callback. It won't be uncompressed and compressed again
            // after the image is captured. The JPEG quality setting will be directly provided to
            // the HAL to compress the output JPEG image.
            jpegQuality = getJpegQualityInternal();
        }
        return jpegQuality;
    }

    void lockFlashMode() {
        synchronized (mLockedFlashMode) {
            if (mLockedFlashMode.get() != null) {
                // FlashMode is locked.
                return;
            }
            mLockedFlashMode.set(getFlashMode());
        }
    }

    void unlockFlashMode() {
        synchronized (mLockedFlashMode) {
            Integer lockedFlashMode = mLockedFlashMode.getAndSet(null);
            if (lockedFlashMode == null) {
                // FlashMode is not locked yet.
                return;
            }
            if (lockedFlashMode != getFlashMode()) {
                // Flash Mode is changed during lock session.
                trySetFlashModeToCameraControl();
            }
        }
    }

    private void trySetFlashModeToCameraControl() {
        synchronized (mLockedFlashMode) {
            if (mLockedFlashMode.get() != null) {
                // Flash Mode is locked.
                return;
            }
            getCameraControl().setFlashMode(getFlashMode());
        }
    }

    /**
     * Gets the JPEG quality based on {@link #mCaptureMode}.
     *
     * <p> Range is 1-100; larger is higher quality.
     *
     * @return Compression quality of the captured JPEG image.
     */
    @IntRange(from = 1, to = 100)
    private int getJpegQualityInternal() {
        ImageCaptureConfig imageCaptureConfig = (ImageCaptureConfig) getCurrentConfig();

        if (imageCaptureConfig.containsOption(OPTION_JPEG_COMPRESSION_QUALITY)) {
            return imageCaptureConfig.getJpegQuality();
        }

        switch (mCaptureMode) {
            case CAPTURE_MODE_MAXIMIZE_QUALITY:
                return JPEG_QUALITY_MAXIMIZE_QUALITY_MODE;
            case CAPTURE_MODE_MINIMIZE_LATENCY:
            case CAPTURE_MODE_ZERO_SHUTTER_LAG:
                return JPEG_QUALITY_MINIMIZE_LATENCY_MODE;
            default:
                throw new IllegalStateException("CaptureMode " + mCaptureMode + " is invalid");
        }
    }

    /**
     * The take picture flow.
     *
     * <p>There are three steps to take a picture.
     *
     * <p>(1) Pre-take picture, which will trigger af/ae scan or open torch if necessary. Then check
     * 3A converged if necessary.
     *
     * <p>(2) Issue take picture single request.
     *
     * <p>(3) Post-take picture, which will cancel af/ae scan or close torch if necessary.
     */
    @NonNull
    private ListenableFuture<ImageProxy> takePictureInternal(
            @NonNull ImageCaptureRequest imageCaptureRequest) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    mImageReader.setOnImageAvailableListener(
                            (imageReader) -> {
                                try {
                                    ImageProxy image = imageReader.acquireLatestImage();
                                    if (image != null) {
                                        if (!completer.set(image)) {
                                            // If the future is already complete (probably be
                                            // cancelled), then close the image.
                                            image.close();
                                        }
                                    } else {
                                        completer.setException(new IllegalStateException(
                                                "Unable to acquire image"));
                                    }
                                } catch (IllegalStateException e) {
                                    completer.setException(e);
                                }
                            },
                            CameraXExecutors.mainThreadExecutor());

                    lockFlashMode();
                    ListenableFuture<Void> future = issueTakePicture(imageCaptureRequest);

                    Futures.addCallback(future,
                            new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    unlockFlashMode();
                                }

                                @Override
                                public void onFailure(@NonNull Throwable throwable) {
                                    unlockFlashMode();

                                    completer.setException(throwable);
                                }
                            },
                            CameraXExecutors.mainThreadExecutor());

                    completer.addCancellationListener(() -> future.cancel(true),
                            CameraXExecutors.directExecutor());
                    return "takePictureInternal";
                });
    }

    /**
     * A processor that manages and issues the pending {@link ImageCaptureRequest}s.
     *
     * <p>It ensures that only one single {@link ImageCaptureRequest} is in progress at a time
     * and is able to process next request only when there is not over the maximum number of
     * dispatched image.
     */
    @VisibleForTesting
    static class ImageCaptureRequestProcessor implements OnImageCloseListener {
        @GuardedBy("mLock")
        private final Deque<ImageCaptureRequest> mPendingRequests = new ArrayDeque<>();

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        @GuardedBy("mLock")
        ImageCaptureRequest mCurrentRequest = null;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        @GuardedBy("mLock")
        ListenableFuture<ImageProxy> mCurrentRequestFuture = null;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        @GuardedBy("mLock")
        int mOutstandingImages = 0;

        @GuardedBy("mLock")
        private final ImageCaptor mImageCaptor;
        private final int mMaxImages;
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        final Object mLock = new Object();

        ImageCaptureRequestProcessor(int maxImages, @NonNull ImageCaptor imageCaptor) {
            mMaxImages = maxImages;
            mImageCaptor = imageCaptor;
        }

        /**
         * Sends an {@link ImageCaptureRequest} to queue.
         *
         * @param imageCaptureRequest the image request
         */
        public void sendRequest(@NonNull ImageCaptureRequest imageCaptureRequest) {
            synchronized (mLock) {
                mPendingRequests.offer(imageCaptureRequest);
                Logger.d(TAG, String.format(Locale.US,
                        "Send image capture request [current, pending] = [%d, %d]",
                        mCurrentRequest != null ? 1 : 0, mPendingRequests.size()));
                processNextRequest();
            }
        }

        /** Cancels current processing and pending requests. */
        public void cancelRequests(@NonNull Throwable throwable) {
            ImageCaptureRequest currentRequest;
            ListenableFuture<ImageProxy> currentRequestFuture;
            List<ImageCaptureRequest> pendingRequests;
            synchronized (mLock) {
                currentRequest = mCurrentRequest;
                mCurrentRequest = null;
                currentRequestFuture = mCurrentRequestFuture;
                mCurrentRequestFuture = null;
                pendingRequests = new ArrayList<>(mPendingRequests);
                mPendingRequests.clear();
            }
            if (currentRequest != null && currentRequestFuture != null) {
                currentRequest.notifyCallbackError(getError(throwable), throwable.getMessage(),
                        throwable);
                currentRequestFuture.cancel(true);
            }
            for (ImageCaptureRequest request : pendingRequests) {
                request.notifyCallbackError(getError(throwable), throwable.getMessage(), throwable);
            }
        }

        /**
         * Removes and returns all unfinished requests.
         *
         * <p>The unfinished requests include:
         * <ul>
         *     <li>Current running request if it is not complete yet.</li>
         *     <li>All pending requests.</li>
         * </ul>
         *
         * @return list of the remaining requests
         */
        @NonNull
        public List<ImageCaptureRequest> pullOutUnfinishedRequests() {
            List<ImageCaptureRequest> remainingRequests;
            synchronized (mLock) {
                remainingRequests = new ArrayList<>(mPendingRequests);
                // Clear the pending requests before canceling the mCurrentRequestFuture.
                mPendingRequests.clear();

                ImageCaptureRequest currentRequest = mCurrentRequest;
                mCurrentRequest = null;
                if (currentRequest != null && mCurrentRequestFuture != null
                        && mCurrentRequestFuture.cancel(true)) {
                    remainingRequests.add(0, currentRequest);
                }
            }

            return remainingRequests;
        }

        @Override
        public void onImageClose(@NonNull ImageProxy image) {
            synchronized (mLock) {
                // TODO: mLock can be removed if all methods and callbacks in
                //  ImageCaptureRequestProcessor are used in the main thread.
                //  Side note: TakePictureManager already handles the requests in the main thread.
                mOutstandingImages--;
                CameraXExecutors.mainThreadExecutor().execute(this::processNextRequest);
            }
        }

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        void processNextRequest() {
            synchronized (mLock) {
                // Unable to issue request if there is currently a request in flight
                if (mCurrentRequest != null) {
                    return;
                }

                // Unable to issue request if the ImageReader has no available image buffer left.
                if (mOutstandingImages >= mMaxImages) {
                    Logger.w(TAG,
                            "Too many acquire images. Close image to be able to process next.");
                    return;
                }

                ImageCaptureRequest imageCaptureRequest = mPendingRequests.poll();
                if (imageCaptureRequest == null) {
                    return;
                }

                mCurrentRequest = imageCaptureRequest;
                mCurrentRequestFuture = mImageCaptor.capture(imageCaptureRequest);
                Futures.addCallback(mCurrentRequestFuture, new FutureCallback<ImageProxy>() {
                    @Override
                    public void onSuccess(@Nullable ImageProxy image) {
                        synchronized (mLock) {
                            checkNotNull(image);
                            SingleCloseImageProxy wrappedImage = new SingleCloseImageProxy(image);
                            wrappedImage.addOnImageCloseListener(ImageCaptureRequestProcessor.this);
                            mOutstandingImages++;
                            imageCaptureRequest.dispatchImage(wrappedImage);

                            mCurrentRequest = null;
                            mCurrentRequestFuture = null;
                            processNextRequest();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        synchronized (mLock) {
                            //noinspection StatementWithEmptyBody
                            if (t instanceof CancellationException) {
                                // Do not trigger callback which should be done in cancelRequests()
                                // with a given throwable.
                            } else {
                                imageCaptureRequest.notifyCallbackError(getError(t),
                                        t != null ? t.getMessage() : "Unknown error", t);
                            }

                            mCurrentRequest = null;
                            mCurrentRequestFuture = null;
                            processNextRequest();
                        }
                    }
                }, CameraXExecutors.mainThreadExecutor());
            }
        }

        /** An interface of an {@link ImageProxy} captor. */
        interface ImageCaptor {
            /**
             * Captures an {@link ImageProxy} by giving a {@link ImageCaptureRequest}.
             *
             * @param imageCaptureRequest an {@link ImageCaptureRequest} contains required
             *                            parameters for this capture.
             * @return a {@link ListenableFuture represents the capture result. Cancellation to
             * the future should cancel the capture task.
             */
            @NonNull
            ListenableFuture<ImageProxy> capture(@NonNull ImageCaptureRequest imageCaptureRequest);
        }

        /**
         * An interface to provide callbacks when processing each capture request.
         */
        interface RequestProcessCallback {
            /**
             * This will be called before starting to process the
             * ImageCaptureRequest.
             */
            void onPreProcessRequest(@NonNull ImageCaptureRequest imageCaptureRequest);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + ":" + getName();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ImageCaptureError
    static int getError(Throwable throwable) {
        if (throwable instanceof CameraClosedException) {
            return ERROR_CAMERA_CLOSED;
        } else if (throwable instanceof ImageCaptureException) {
            return ((ImageCaptureException) throwable).getImageCaptureError();
        } else {
            return ERROR_UNKNOWN;
        }
    }

    /**
     * Disables software JPEG if it is requested and the provided config and/or device
     * characteristics are incompatible.
     *
     * @return {@code true} if software JPEG will be used after applying constraints.
     */
    boolean enforceSoftwareJpegConstraints(@NonNull MutableConfig mutableConfig) {
        // Software encoder currently only supports API 26+.
        if (Boolean.TRUE.equals(
                mutableConfig.retrieveOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, false))) {
            boolean supported = true;
            if (isSessionProcessorEnabledInCurrentCamera()) {
                // SessionProcessor requires JPEG input format so it is incompatible with SW Jpeg.
                Logger.w(TAG, "Software JPEG cannot be used with Extensions.");
                supported = false;
            }
            Integer bufferFormat = mutableConfig.retrieveOption(OPTION_BUFFER_FORMAT, null);
            if (bufferFormat != null && bufferFormat != ImageFormat.JPEG) {
                Logger.w(TAG, "Software JPEG cannot be used with non-JPEG output buffer format.");
                supported = false;
            }

            if (!supported) {
                Logger.w(TAG, "Unable to support software JPEG. Disabling.");
                mutableConfig.insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, false);
            }

            return supported;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onUnbind() {
        abortImageCaptureRequests();
        clearPipeline();
        mUseSoftwareJpeg = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onBind() {
        ImageCaptureConfig useCaseConfig = (ImageCaptureConfig) getCurrentConfig();

        CaptureConfig.Builder captureBuilder = CaptureConfig.Builder.createFrom(useCaseConfig);
        mCaptureConfig = captureBuilder.build();

        // This will only be set to true if software JPEG was requested and
        // enforceSoftwareJpegConstraints() hasn't removed the request.
        mUseSoftwareJpeg = useCaseConfig.isSoftwareJpegEncoderRequested();

        CameraInternal camera = getCamera();
        checkNotNull(camera, "Attached camera cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecUpdated(@NonNull StreamSpec suggestedStreamSpec) {
        mSessionConfigBuilder = createPipeline(getCameraId(),
                (ImageCaptureConfig) getCurrentConfig(), suggestedStreamSpec);

        updateSessionConfig(mSessionConfigBuilder.build());

        // In order to speed up the take picture process, notifyActive at an early stage to
        // attach the session capture callback to repeating and get capture result all the time.
        notifyActive();
        return suggestedStreamSpec;
    }

    /**
     * Initiates a set of captures that will be used to create the output of
     * {@link #takePicture(OutputFileOptions, Executor, OnImageSavedCallback)} and its variants.
     *
     * <p> This returns a {@link ListenableFuture} whose completion indicates that the
     * captures are finished. Before the future is complete, any modification to the camera state
     * such as 3A could affect the result of the captures. After the future is complete, then it
     * is safe to reset or modify the 3A state.
     */
    ListenableFuture<Void> issueTakePicture(@NonNull ImageCaptureRequest imageCaptureRequest) {
        Logger.d(TAG, "issueTakePicture");

        final CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setTemplateType(mCaptureConfig.getTemplateType());

        // Add the default implementation options of ImageCapture
        builder.addImplementationOptions(mCaptureConfig.getImplementationOptions());
        builder.addAllCameraCaptureCallbacks(
                mSessionConfigBuilder.getSingleCameraCaptureCallbacks());

        builder.addSurface(mDeferrableSurface);

        // Only sets the JPEG rotation and quality capture request options when capturing
        // images in JPEG format. Some devices do not handle these CaptureRequest key values
        // when capturing a non-JPEG image. Setting these capture requests and checking the
        // returned capture results for specific purpose might cause problems. See b/204375890.
        if (getImageFormat() == ImageFormat.JPEG) {
            // Add the dynamic implementation options of ImageCapture
            if (EXIF_ROTATION_AVAILABILITY.isRotationOptionSupported()) {
                builder.addImplementationOption(CaptureConfig.OPTION_ROTATION,
                        imageCaptureRequest.mRotationDegrees);
            }
            builder.addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY,
                    imageCaptureRequest.mJpegQuality);
        }

        builder.addCameraCaptureCallback(mMetadataMatchingCaptureCallback);

        return submitStillCaptureRequest(Arrays.asList(builder.build()));
    }

    /**
     * ===== New architecture start =====
     *
     * TODO(b/229629844): unit test the interactions between {@link ImageCapture} and
     *  {@link ImagePipeline}/{@link TakePictureManager}.
     */

    @Nullable
    private ImagePipeline mImagePipeline;
    @Nullable
    private TakePictureManager mTakePictureManager;

    /**
     * An {@link ImageCaptureControl} implementation to control this {@link ImageCapture} instance.
     */
    private final ImageCaptureControl mImageCaptureControl = new ImageCaptureControl() {

        @MainThread
        @Override
        public void lockFlashMode() {
            ImageCapture.this.lockFlashMode();
        }

        @MainThread
        @Override
        public void unlockFlashMode() {
            ImageCapture.this.unlockFlashMode();
        }

        @MainThread
        @NonNull
        @Override
        public ListenableFuture<Void> submitStillCaptureRequests(
                @NonNull List<CaptureConfig> captureConfigs) {
            return ImageCapture.this.submitStillCaptureRequest(captureConfigs);
        }
    };

    /**
     * Checks if the node is enabled given the current configuration.
     *
     * <p>This method checks if the new architecture can be enabled based on the current
     * configuration.
     *
     * <p>DO NOT turn on the new architecture until the 1.3 alpha branch is cut. We don't want
     * the quality of 1.2 beta to be affected by the refactoring.
     */
    @MainThread
    private boolean isNodeEnabled() {
        checkMainThread();
        ImageCaptureConfig config = (ImageCaptureConfig) getCurrentConfig();
        if (config.getImageReaderProxyProvider() != null) {
            // Use old pipeline for custom ImageReader.
            return false;
        }
        if (isSessionProcessorEnabledInCurrentCamera()) {
            // Use old pipeline when extension is enabled.
            return false;
        }

        if (config.getBufferFormat(ImageFormat.JPEG) != ImageFormat.JPEG) {
            // Use old pipeline for non-JPEG output format.
            return false;
        }
        return mUseProcessingPipeline;
    }

    /**
     * Creates the pipeline for both capture request configuration and image post-processing.
     *
     * <p> This is the new {@link #createPipeline}.
     */
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    @MainThread
    private SessionConfig.Builder createPipelineWithNode(@NonNull String cameraId,
            @NonNull ImageCaptureConfig config, @NonNull StreamSpec streamSpec) {
        checkMainThread();
        Log.d(TAG, String.format("createPipelineWithNode(cameraId: %s, streamSpec: %s)",
                cameraId, streamSpec));
        Size resolution = streamSpec.getResolution();

        checkState(mImagePipeline == null);
        mImagePipeline = new ImagePipeline(config, resolution, getEffect(),
                !requireNonNull(getCamera()).getHasTransform());

        if (mTakePictureManager == null) {
            // mTakePictureManager is reused when the Surface is reset.
            mTakePictureManager = new TakePictureManager(mImageCaptureControl);
        }
        mTakePictureManager.setImagePipeline(mImagePipeline);

        SessionConfig.Builder sessionConfigBuilder =
                mImagePipeline.createSessionConfigBuilder(streamSpec.getResolution());
        if (Build.VERSION.SDK_INT >= 23 && getCaptureMode() == CAPTURE_MODE_ZERO_SHUTTER_LAG) {
            getCameraControl().addZslConfig(sessionConfigBuilder);
        }
        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                mTakePictureManager.pause();
                clearPipelineWithNode(/*keepTakePictureManager=*/ true);
                mSessionConfigBuilder = createPipeline(cameraId, config, streamSpec);
                updateSessionConfig(mSessionConfigBuilder.build());
                notifyReset();
                mTakePictureManager.resume();
            } else {
                clearPipelineWithNode();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Takes a picture with the new architecture.
     */
    @MainThread
    private void takePictureWithNode(@NonNull Executor executor,
            @Nullable OnImageCapturedCallback inMemoryCallback,
            @Nullable ImageCapture.OnImageSavedCallback onDiskCallback,
            @Nullable OutputFileOptions outputFileOptions) {
        checkMainThread();
        Log.d(TAG, "takePictureWithNode");
        CameraInternal camera = getCamera();
        if (camera == null) {
            sendInvalidCameraError(executor, inMemoryCallback, onDiskCallback);
            return;
        }
        mTakePictureManager.offerRequest(TakePictureRequest.of(
                executor,
                inMemoryCallback,
                onDiskCallback,
                outputFileOptions,
                getTakePictureCropRect(),
                getSensorToBufferTransformMatrix(),
                getRelativeRotation(camera),
                getJpegQualityInternal(),
                getCaptureMode(),
                mSessionConfigBuilder.getSingleCameraCaptureCallbacks()));
    }

    private void sendInvalidCameraError(@NonNull Executor executor,
            @Nullable OnImageCapturedCallback inMemoryCallback,
            @Nullable ImageCapture.OnImageSavedCallback onDiskCallback) {
        ImageCaptureException exception = new ImageCaptureException(ERROR_INVALID_CAMERA,
                "Not bound to a valid Camera [" + ImageCapture.this + "]", null);
        if (inMemoryCallback != null) {
            inMemoryCallback.onError(exception);
        } else if (onDiskCallback != null) {
            onDiskCallback.onError(exception);
        } else {
            throw new IllegalArgumentException("Must have either in-memory or on-disk callback.");
        }
    }

    /**
     * Calculates a snapshot of crop rect when app calls {@link #takePicture}.
     */
    @NonNull
    private Rect getTakePictureCropRect() {
        Rect rect = getViewPortCropRect();
        Size resolution = requireNonNull(getAttachedSurfaceResolution());
        if (rect != null) {
            return rect;
        } else if (isAspectRatioValid(mCropAspectRatio)) {
            int rotationDegrees = getRelativeRotation(requireNonNull(getCamera()));
            Rational rotatedAspectRatio = new Rational(
                    /* numerator= */ mCropAspectRatio.getDenominator(),
                    /* denominator= */ mCropAspectRatio.getNumerator());
            Rational sensorCropRatio = is90or270(rotationDegrees)
                    ? rotatedAspectRatio : mCropAspectRatio;
            return requireNonNull(computeCropRectFromAspectRatio(resolution, sensorCropRatio));
        }
        return new Rect(0, 0, resolution.getWidth(), resolution.getHeight());
    }


    /**
     * Clears the pipeline without keeping the {@link TakePictureManager}.
     */
    @MainThread
    private void clearPipelineWithNode() {
        clearPipelineWithNode(/*keepTakePictureManager=*/false);
    }

    /**
     * Clears the pipeline.
     *
     * <p>Similar to {@link #clearPipeline()}, this cancels unfinished requests and release
     * resources.
     */
    @MainThread
    private void clearPipelineWithNode(boolean keepTakePictureManager) {
        Log.d(TAG, "clearPipelineWithNode");
        checkMainThread();
        if (mImagePipeline != null) {
            mImagePipeline.close();
            mImagePipeline = null;
        }
        // TODO: no need to abort requests when UseCase unbinds. Clean this up when the old
        //  pipeline is removed.
        if (!keepTakePictureManager && mTakePictureManager != null) {
            mTakePictureManager.abortRequests();
            mTakePictureManager = null;
        }
    }

    /**
     * Submits still capture requests with the current configurations.
     */
    @MainThread
    ListenableFuture<Void> submitStillCaptureRequest(
            @NonNull List<CaptureConfig> captureConfigs) {
        checkMainThread();
        return Futures.transform(getCameraControl().submitStillCaptureRequests(
                        captureConfigs, mCaptureMode, mFlashType),
                input -> null, CameraXExecutors.directExecutor());
    }

    @VisibleForTesting
    boolean isProcessingPipelineEnabled() {
        return mImagePipeline != null && mTakePictureManager != null;
    }

    @Nullable
    @VisibleForTesting
    ImagePipeline getImagePipeline() {
        return mImagePipeline;
    }

    @VisibleForTesting
    @NonNull
    TakePictureManager getTakePictureManager() {
        return requireNonNull(mTakePictureManager);
    }

    // ===== New architecture end =====

    /**
     * @inheritDoc
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Set<Integer> getSupportedEffectTargets() {
        Set<Integer> targets = new HashSet<>();
        targets.add(IMAGE_CAPTURE);
        return targets;
    }

    /**
     * Describes the error that occurred during an image capture operation (such as {@link
     * ImageCapture#takePicture(Executor, OnImageCapturedCallback)}).
     *
     * <p>This is a parameter sent to the error callback functions set in listeners such as {@link
     * ImageCapture.OnImageSavedCallback#onError(ImageCaptureException)}.
     */
    @IntDef({ERROR_UNKNOWN, ERROR_FILE_IO, ERROR_CAPTURE_FAILED, ERROR_CAMERA_CLOSED,
            ERROR_INVALID_CAMERA})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @interface ImageCaptureError {
    }

    /**
     * Capture mode options for ImageCapture. A picture will always be taken regardless of
     * mode, and the mode will be used on devices that support it.
     */
    @IntDef({CAPTURE_MODE_MAXIMIZE_QUALITY, CAPTURE_MODE_MINIMIZE_LATENCY,
            CAPTURE_MODE_ZERO_SHUTTER_LAG})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @interface CaptureMode {
    }

    /**
     * The flash mode options when taking a picture using ImageCapture.
     *
     * <p>Applications can check if there is a flash unit via {@link CameraInfo#hasFlashUnit()} and
     * update UI component if necessary. If there is no flash unit, then the FlashMode set to
     * {@link #setFlashMode(int)} will take no effect for the subsequent photo capture requests
     * and they will act like {@link #FLASH_MODE_OFF}.
     *
     * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
     * will remain enabled during photo capture regardless of flash mode setting. When
     * the torch is disabled, flash will function as specified by
     * {@link #setFlashMode(int)}.
     */
    @IntDef({FLASH_MODE_UNKNOWN, FLASH_MODE_AUTO, FLASH_MODE_ON, FLASH_MODE_OFF})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface FlashMode {
    }

    /**
     * The flash type options when flash is required for taking a picture.
     */
    @IntDef({FLASH_TYPE_ONE_SHOT_FLASH, FLASH_TYPE_USE_TORCH_AS_FLASH})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface FlashType {
    }

    /** Listener containing callbacks for image file I/O events. */
    public interface OnImageSavedCallback {
        /** Called when an image has been successfully saved. */
        void onImageSaved(@NonNull OutputFileResults outputFileResults);

        /**
         * Called when an error occurs while attempting to save an image.
         *
         * @param exception An {@link ImageCaptureException} that contains the type of error, the
         *                  error message and the throwable that caused it.
         */
        void onError(@NonNull ImageCaptureException exception);
    }

    /**
     * Callback for when an image capture has completed.
     */
    public abstract static class OnImageCapturedCallback {
        /**
         * Callback for when the image has been captured.
         *
         * <p>The application is responsible for calling {@link ImageProxy#close()} to close the
         * image.
         *
         * <p>The image is of format {@link ImageFormat#JPEG}, queryable via
         * {@link ImageProxy#getFormat()}.
         *
         * <p>The image is provided as captured by the underlying {@link ImageReader} without
         * rotation applied. The value in {@code image.getImageInfo().getRotationDegrees()}
         * describes the magnitude of clockwise rotation, which if applied to the image will make
         * it match the currently configured target rotation.
         *
         * <p>For example, if the current target rotation is set to the display rotation,
         * rotationDegrees is the rotation to apply to the image to match the display orientation.
         * A rotation of 90 degrees would mean rotating the image 90 degrees clockwise produces an
         * image that will match the display orientation.
         *
         * <p>See also {@link ImageCapture.Builder#setTargetRotation(int)} and
         * {@link #setTargetRotation(int)}.
         *
         * <p>Timestamps are in nanoseconds and monotonic and can be compared to timestamps from
         * images produced from UseCases bound to the same camera instance.  More detail is
         * available depending on the implementation.  For example with CameraX using a
         * {@link androidx.camera.camera2} implementation additional detail can be found in
         * {@link android.hardware.camera2.CameraDevice} documentation.
         *
         * @param image The captured image
         */
        public void onCaptureSuccess(@NonNull ImageProxy image) {
        }

        /**
         * Callback for when an error occurred during image capture.
         *
         * @param exception An {@link ImageCaptureException} that contains the type of error, the
         *                  error message and the throwable that caused it.
         */
        public void onError(@NonNull final ImageCaptureException exception) {
        }
    }

    /**
     * Provides a base static default configuration for the ImageCapture
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults
            implements ConfigProvider<ImageCaptureConfig> {
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 4;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;

        private static final ResolutionSelector DEFAULT_RESOLUTION_SELECTOR =
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).setResolutionStrategy(
                        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build();

        private static final ImageCaptureConfig DEFAULT_CONFIG;

        static {
            Builder builder = new Builder()
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                    .setResolutionSelector(DEFAULT_RESOLUTION_SELECTOR);

            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public ImageCaptureConfig getConfig() {
            return DEFAULT_CONFIG;
        }
    }

    /**
     * Options for saving newly captured image.
     *
     * <p> this class is used to configure save location and metadata. Save location can be
     * either a {@link File}, {@link MediaStore} or a {@link OutputStream}. The metadata will be
     * stored with the saved image. For JPEG this will be included in the EXIF.
     */
    public static final class OutputFileOptions {

        @Nullable
        private final File mFile;
        @Nullable
        private final ContentResolver mContentResolver;
        @Nullable
        private final Uri mSaveCollection;
        @Nullable
        private final ContentValues mContentValues;
        @Nullable
        private final OutputStream mOutputStream;
        @NonNull
        private final Metadata mMetadata;

        OutputFileOptions(@Nullable File file,
                @Nullable ContentResolver contentResolver,
                @Nullable Uri saveCollection,
                @Nullable ContentValues contentValues,
                @Nullable OutputStream outputStream,
                @Nullable Metadata metadata) {
            mFile = file;
            mContentResolver = contentResolver;
            mSaveCollection = saveCollection;
            mContentValues = contentValues;
            mOutputStream = outputStream;
            mMetadata = metadata == null ? new Metadata() : metadata;
        }

        /**
         *
         */
        @Nullable
        @RestrictTo(Scope.LIBRARY_GROUP)
        public File getFile() {
            return mFile;
        }

        /**
         *
         */
        @Nullable
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ContentResolver getContentResolver() {
            return mContentResolver;
        }

        /**
         *
         */
        @Nullable
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Uri getSaveCollection() {
            return mSaveCollection;
        }

        /**
         *
         */
        @Nullable
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ContentValues getContentValues() {
            return mContentValues;
        }

        /**
         *
         */
        @Nullable
        @RestrictTo(Scope.LIBRARY_GROUP)
        public OutputStream getOutputStream() {
            return mOutputStream;
        }

        /**
         * Exposed internally so that CameraController can overwrite the flip horizontal flag for
         * front camera. External core API users shouldn't need this because they are the ones who
         * created the {@link Metadata}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Metadata getMetadata() {
            return mMetadata;
        }

        @NonNull
        @Override
        public String toString() {
            return "OutputFileOptions{"
                    + "mFile=" + mFile + ", "
                    + "mContentResolver=" + mContentResolver + ", "
                    + "mSaveCollection=" + mSaveCollection + ", "
                    + "mContentValues=" + mContentValues + ", "
                    + "mOutputStream=" + mOutputStream + ", "
                    + "mMetadata=" + mMetadata
                    + "}";
        }

        /**
         * Builder class for {@link OutputFileOptions}.
         */
        public static final class Builder {
            @Nullable
            private File mFile;
            @Nullable
            private ContentResolver mContentResolver;
            @Nullable
            private Uri mSaveCollection;
            @Nullable
            private ContentValues mContentValues;
            @Nullable
            private OutputStream mOutputStream;
            @Nullable
            private Metadata mMetadata;

            /**
             * Creates options to write captured image to a {@link File}.
             *
             * @param file save location of the image.
             */
            public Builder(@NonNull File file) {
                mFile = file;
            }

            /**
             * Creates options to write captured image to {@link MediaStore}.
             *
             * Example:
             *
             * <pre>{@code
             *
             * ContentValues contentValues = new ContentValues();
             * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_IMAGE");
             * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
             *
             * ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
             *         getContentResolver(),
             *         MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
             *         contentValues).build();
             *
             * }</pre>
             *
             * @param contentResolver to access {@link MediaStore}
             * @param saveCollection  The URL of the table to insert into.
             * @param contentValues   to be included in the created image file.
             */
            public Builder(@NonNull ContentResolver contentResolver,
                    @NonNull Uri saveCollection,
                    @NonNull ContentValues contentValues) {
                mContentResolver = contentResolver;
                mSaveCollection = saveCollection;
                mContentValues = contentValues;
            }

            /**
             * Creates options that write captured image to a {@link OutputStream}.
             *
             * @param outputStream save location of the image.
             */
            public Builder(@NonNull OutputStream outputStream) {
                mOutputStream = outputStream;
            }

            /**
             * Sets the metadata to be stored with the saved image.
             *
             * <p> For JPEG this will be included in the EXIF.
             *
             * @param metadata Metadata to be stored with the saved image. For JPEG this will
             *                 be included in the EXIF.
             */
            @NonNull
            public Builder setMetadata(@NonNull Metadata metadata) {
                mMetadata = metadata;
                return this;
            }

            /**
             * Builds {@link OutputFileOptions}.
             */
            @NonNull
            public OutputFileOptions build() {
                return new OutputFileOptions(mFile, mContentResolver, mSaveCollection,
                        mContentValues, mOutputStream, mMetadata);
            }
        }
    }

    /**
     * Info about the saved image file.
     */
    public static class OutputFileResults {
        @Nullable
        private final Uri mSavedUri;

        /**
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public OutputFileResults(@Nullable Uri savedUri) {
            mSavedUri = savedUri;
        }

        /**
         * Returns the {@link Uri} of the saved file.
         *
         * <p> Returns null if the {@link OutputFileOptions} is constructed with
         * {@link androidx.camera.core.ImageCapture.OutputFileOptions.Builder
         * #Builder(OutputStream)}.
         */
        @Nullable
        public Uri getSavedUri() {
            return mSavedUri;
        }
    }

    /** Holder class for metadata that will be saved with captured images. */
    public static final class Metadata {
        /**
         * Indicates a left-right mirroring (reflection).
         *
         * <p>The reflection is meant to be applied to the upright image (after rotation to the
         * target orientation). When saving the image to file, it is combined with the rotation
         * degrees, to generate the corresponding EXIF orientation value.
         */
        private boolean mIsReversedHorizontal;

        /**
         * Whether the mIsReversedHorizontal has been set by the app explicitly.
         */
        private boolean mIsReversedHorizontalSet = false;

        /**
         * Indicates an upside down mirroring, equivalent to a horizontal mirroring (reflection)
         * followed by a 180 degree rotation.
         *
         * <p>The reflection is meant to be applied to the upright image (after rotation to the
         * target orientation). When saving the image to file, it is combined with the rotation
         * degrees, to generate the corresponding EXIF orientation value.
         */
        private boolean mIsReversedVertical;
        /** Data representing a geographic location. */
        @Nullable
        private Location mLocation;

        /**
         * Gets left-right mirroring of the capture.
         *
         * @return true if the capture is left-right mirrored.
         */
        public boolean isReversedHorizontal() {
            return mIsReversedHorizontal;
        }

        /**
         * Returns true if {@link #setReversedHorizontal} has been called.
         *
         * <p> CameraController's default behavior is mirroring the picture when front camera is
         * used. This method is used to check if reverseHorizontal is set explicitly by the app.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public boolean isReversedHorizontalSet() {
            return mIsReversedHorizontalSet;
        }

        /**
         * Sets left-right mirroring of the capture.
         *
         * @param isReversedHorizontal true if the capture is left-right mirrored.
         */
        public void setReversedHorizontal(boolean isReversedHorizontal) {
            mIsReversedHorizontal = isReversedHorizontal;
            mIsReversedHorizontalSet = true;
        }

        /**
         * Gets upside-down mirroring of the capture.
         *
         * @return true if the capture is upside-down.
         */
        public boolean isReversedVertical() {
            return mIsReversedVertical;
        }

        /**
         * Sets upside-down mirroring of the capture.
         *
         * @param isReversedVertical true if the capture is upside-down.
         */
        public void setReversedVertical(boolean isReversedVertical) {
            mIsReversedVertical = isReversedVertical;
        }

        /**
         * Gets the geographic location of the capture.
         *
         * @return the geographic location.
         */
        @Nullable
        public Location getLocation() {
            return mLocation;
        }

        /**
         * Sets the geographic location of the capture.
         *
         * @param location the geographic location.
         */
        public void setLocation(@Nullable Location location) {
            mLocation = location;
        }

        @NonNull
        @Override
        public String toString() {
            return "Metadata{"
                    + "mIsReversedHorizontal=" + mIsReversedHorizontal + ", "
                    + "mIsReversedVertical=" + mIsReversedVertical + ", "
                    + "mLocation=" + mLocation
                    + "}";
        }
    }

    @VisibleForTesting
    static class ImageCaptureRequest {
        @RotationValue
        final int mRotationDegrees;
        @IntRange(from = 1, to = 100)
        final int mJpegQuality;

        private final Rational mTargetRatio;
        @NonNull
        private final Executor mListenerExecutor;
        @NonNull
        private final OnImageCapturedCallback mCallback;

        AtomicBoolean mDispatched = new AtomicBoolean(false);

        private final Rect mViewPortCropRect;

        @NonNull
        private final Matrix mSensorToBufferTransformMatrix;

        /**
         * @param rotationDegrees               The degrees to rotate the image buffer from sensor
         *                                      coordinates into the final output coordinate space.
         * @param jpegQuality                   The requested output JPEG image compression
         *                                      quality. The value must
         *                                      be in range [1..100] which larger is higher quality.
         * @param targetRatio                   The aspect ratio of the image in final output
         *                                      coordinate space.
         *                                      This must be a non-negative, non-zero value.
         * @param viewPortCropRect              The cropped rect of the field of view.
         * @param sensorToBufferTransformMatrix The sensor to buffer transform matrix.
         * @param executor                      The {@link Executor} which will be used for the
         *                                      listener.
         * @param callback                      The {@link OnImageCapturedCallback} for the quest.
         * @throws IllegalArgumentException If targetRatio is not a valid value.
         */
        ImageCaptureRequest(
                @RotationValue int rotationDegrees,
                @IntRange(from = 1, to = 100) int jpegQuality,
                Rational targetRatio,
                @Nullable Rect viewPortCropRect,
                @NonNull Matrix sensorToBufferTransformMatrix,
                @NonNull Executor executor,
                @NonNull OnImageCapturedCallback callback) {
            mRotationDegrees = rotationDegrees;
            mJpegQuality = jpegQuality;
            if (targetRatio != null) {
                Preconditions.checkArgument(!targetRatio.isZero(), "Target ratio cannot be zero");
                Preconditions.checkArgument(targetRatio.floatValue() > 0, "Target ratio must be "
                        + "positive");
            }
            mTargetRatio = targetRatio;
            mViewPortCropRect = viewPortCropRect;
            mSensorToBufferTransformMatrix = sensorToBufferTransformMatrix;
            mListenerExecutor = executor;
            mCallback = callback;
        }

        void dispatchImage(final ImageProxy image) {
            // Check to make sure image hasn't been already dispatched or error has been notified
            if (!mDispatched.compareAndSet(false, true)) {
                image.close();
                return;
            }

            Size dispatchResolution;
            int dispatchRotationDegrees;

            // Retrieve the dimension and rotation values from the embedded EXIF data in the
            // captured image only if those information is available.
            if (EXIF_ROTATION_AVAILABILITY.shouldUseExifOrientation(image)) {
                // JPEG needs to have rotation/crop based on the EXIF
                try {
                    ImageProxy.PlaneProxy[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    Exif exif;

                    buffer.rewind();

                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    exif = Exif.createFromInputStream(new ByteArrayInputStream(data));
                    buffer.rewind();

                    dispatchResolution = new Size(exif.getWidth(), exif.getHeight());
                    dispatchRotationDegrees = exif.getRotation();
                } catch (IOException e) {
                    notifyCallbackError(ERROR_FILE_IO, "Unable to parse JPEG exif", e);
                    image.close();
                    return;
                }
            } else {
                // All other formats take the rotation based simply on the target rotation
                dispatchResolution = new Size(image.getWidth(), image.getHeight());
                dispatchRotationDegrees = mRotationDegrees;
            }

            // Construct the ImageProxy with the updated rotation & crop for the output
            ImageInfo imageInfo = ImmutableImageInfo.create(
                    image.getImageInfo().getTagBundle(),
                    image.getImageInfo().getTimestamp(),
                    dispatchRotationDegrees,
                    mSensorToBufferTransformMatrix);

            final ImageProxy dispatchedImageProxy = new SettableImageProxy(image,
                    dispatchResolution, imageInfo);

            // Update the crop rect aspect ratio after it has been rotated into the buffer
            // orientation
            Rect cropRect = computeDispatchCropRect(mViewPortCropRect, mTargetRatio,
                    mRotationDegrees, dispatchResolution, dispatchRotationDegrees);
            dispatchedImageProxy.setCropRect(cropRect);

            try {
                mListenerExecutor.execute(() -> mCallback.onCaptureSuccess(dispatchedImageProxy));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "Unable to post to the supplied executor.");

                // Unable to execute on the supplied executor, close the image.
                image.close();
            }
        }

        void notifyCallbackError(final @ImageCaptureError int imageCaptureError,
                final String message, final Throwable cause) {
            // Check to make sure image hasn't been already dispatched or error has been notified
            if (!mDispatched.compareAndSet(false, true)) {
                return;
            }

            try {
                mListenerExecutor.execute(() -> mCallback.onError(
                        new ImageCaptureException(imageCaptureError, message, cause)));
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "Unable to post to the supplied executor.");
            }
        }
    }

    /** Builder for an {@link ImageCapture}. */
    @SuppressWarnings({"ObjectToString", "unused"})
    public static final class Builder implements
            UseCaseConfig.Builder<ImageCapture, ImageCaptureConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            IoConfig.Builder<Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageCapture.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageCapture.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromConfig(@NonNull Config configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static Builder fromConfig(@NonNull ImageCaptureConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /**
         * {@inheritDoc}
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public ImageCaptureConfig getUseCaseConfig() {
            return new ImageCaptureConfig(OptionsBundle.from(mMutableConfig));
        }

        /**
         * Builds an immutable {@link ImageCapture} from the current state.
         *
         * @return A {@link ImageCapture} populated with the current state.
         * @throws IllegalArgumentException if attempting to set both target aspect ratio and
         *                                  target resolution.
         */
        @Override
        @NonNull
        public ImageCapture build() {
            // Update the input format base on the other options set (mainly whether processing
            // is done)
            Integer bufferFormat = getMutableConfig().retrieveOption(OPTION_BUFFER_FORMAT, null);
            if (bufferFormat != null) {
                getMutableConfig().insertOption(OPTION_INPUT_FORMAT, bufferFormat);
            } else {
                getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.JPEG);
            }

            ImageCaptureConfig imageCaptureConfig = getUseCaseConfig();
            ImageOutputConfig.validateConfig(imageCaptureConfig);
            ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);

            // Makes the crop aspect ratio match the target resolution setting as what mentioned
            // in javadoc of setTargetResolution(). When the target resolution is set, {@link
            // ImageCapture#setCropAspectRatio(Rational)} will be automatically called to set
            // corresponding value.
            Size targetResolution = getMutableConfig().retrieveOption(OPTION_TARGET_RESOLUTION,
                    null);
            if (targetResolution != null) {
                imageCapture.setCropAspectRatio(new Rational(targetResolution.getWidth(),
                        targetResolution.getHeight()));
            }

            checkNotNull(getMutableConfig().retrieveOption(OPTION_IO_EXECUTOR,
                    CameraXExecutors.ioExecutor()), "The IO executor can't be null");

            if (getMutableConfig().containsOption(OPTION_FLASH_MODE)) {
                Integer flashMode = getMutableConfig().retrieveOption(OPTION_FLASH_MODE);

                if (flashMode == null || (flashMode != FLASH_MODE_AUTO && flashMode != FLASH_MODE_ON
                        && flashMode != FLASH_MODE_OFF)) {
                    throw new IllegalArgumentException(
                            "The flash mode is not allowed to set: " + flashMode);
                }
            }

            return imageCapture;
        }

        /**
         * Sets the image capture mode.
         *
         * <p>Valid capture modes are {@link CaptureMode#CAPTURE_MODE_MINIMIZE_LATENCY}, which
         * prioritizes
         * latency over image quality, or {@link CaptureMode#CAPTURE_MODE_MAXIMIZE_QUALITY},
         * which prioritizes
         * image quality over latency.
         *
         * <p>If not set, the capture mode will default to
         * {@link CaptureMode#CAPTURE_MODE_MINIMIZE_LATENCY}.
         *
         * @param captureMode The requested image capture mode.
         * @return The current Builder.
         */
        @NonNull
        public Builder setCaptureMode(@CaptureMode int captureMode) {
            getMutableConfig().insertOption(OPTION_IMAGE_CAPTURE_MODE, captureMode);
            return this;
        }

        /**
         * Sets the flashMode.
         *
         * <p>If not set, the flash mode will default to {@link #FLASH_MODE_OFF}.
         *
         * <p>See {@link ImageCapture#setFlashMode(int)} for more information.
         *
         * @param flashMode The requested flash mode. Value is {@link #FLASH_MODE_AUTO},
         *                  {@link #FLASH_MODE_ON}, or {@link #FLASH_MODE_OFF}.
         * @return The current Builder.
         */
        @NonNull
        public Builder setFlashMode(@FlashMode int flashMode) {
            getMutableConfig().insertOption(OPTION_FLASH_MODE, flashMode);
            return this;
        }

        /**
         * Sets the {@link ImageFormat} of the {@link ImageProxy} returned by the
         * {@link ImageCapture.OnImageCapturedCallback}.
         *
         * <p>Warning. This could lead to an invalid configuration as image format support is per
         * device. Also, setting the buffer format in conjuncture with image capture extensions will
         * result in an invalid configuration. In this case {@link
         * ImageCapture#ImageCapture(ImageCaptureConfig)} will throw an
         * {@link IllegalArgumentException}.
         *
         * @param bufferImageFormat The image format for captured images.
         * @return The current Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setBufferFormat(int bufferImageFormat) {
            getMutableConfig().insertOption(OPTION_BUFFER_FORMAT, bufferImageFormat);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setCustomOrderedResolutions(@NonNull List<Size> resolutions) {
            getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, resolutions);
            return this;
        }

        // Implementations of TargetConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<ImageCapture> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        /**
         * Sets the name of the target object being configured, used only for debug logging.
         *
         * <p>The name should be a value that can uniquely identify an instance of the object being
         * configured.
         *
         * <p>If not set, the target name will default to a unique name automatically generated
         * with the class canonical name and random UUID.
         *
         * @param targetName A unique string identifier for the instance of the class being
         *                   configured.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of ImageOutputConfig.Builder default methods

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>The aspect ratio is the ratio of width to height in the sensor orientation.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case. Attempting so will throw an IllegalArgumentException when building the Config.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution and the resulting
         * aspect ratio may not be exactly as requested.
         *
         * <p>If not set, or {@link AspectRatio#RATIO_DEFAULT} is supplied, resolutions with
         * aspect ratio 4:3 will be considered in higher priority.
         *
         * @param aspectRatio The desired ImageCapture {@link AspectRatio}
         * @return The current Builder.
         * @deprecated use {@link ResolutionSelector} with {@link AspectRatioStrategy} to specify
         * the preferred aspect ratio settings instead.
         */
        @NonNull
        @Override
        @Deprecated
        public Builder setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio) {
            if (aspectRatio == AspectRatio.RATIO_DEFAULT) {
                aspectRatio = Defaults.DEFAULT_ASPECT_RATIO;
            }
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return this;
        }

        /**
         * Sets the rotation of the intended target for images from this configuration.
         *
         * <p>This will affect the EXIF rotation metadata in images saved by takePicture calls and
         * the {@link ImageInfo#getRotationDegrees()} value of the {@link ImageProxy} returned by
         * {@link OnImageCapturedCallback}. These will be set to be the rotation, which if
         * applied to the output image data, will make the image match the target rotation
         * specified here.
         *
         * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link
         * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
         *
         * <p>In general, it is best to additionally set the target rotation dynamically on the use
         * case.  See {@link androidx.camera.core.ImageCapture#setTargetRotationDegrees(int)} for
         * additional documentation.
         *
         * <p>If not set, the target rotation will default to the value of
         * {@link android.view.Display#getRotation()} of the default display at the time the use
         * case is created. The use case is fully created once it has been attached to a camera.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see androidx.camera.core.ImageCapture#setTargetRotation(int)
         * @see androidx.camera.core.ImageCapture#setTargetRotationDegrees(int)
         * @see android.view.OrientationEventListener
         */
        @NonNull
        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * setMirrorMode is not supported on ImageCapture.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            throw new UnsupportedOperationException("setMirrorMode is not supported.");
        }

        /**
         * Sets the intended output target resolution.
         *
         * <p>The target resolution attempts to establish a minimum bound for the image resolution.
         * The actual image resolution will be the closest available resolution in size that is not
         * smaller than the target resolution, as determined by the Camera implementation. However,
         * if no resolution exists that is equal to or larger than the target resolution, the
         * nearest available resolution smaller than the target resolution will be chosen.
         * Resolutions with the same aspect ratio of the provided {@link Size} will be considered in
         * higher priority before resolutions of different aspect ratios.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case. Attempting so will throw an IllegalArgumentException when building the Config.
         *
         * <p>The resolution {@link Size} should be expressed in the coordinate frame after
         * rotating the supported sizes by the target rotation. For example, a device with
         * portrait natural orientation in natural target rotation requesting a portrait image
         * may specify 480x640, and the same device, rotated 90 degrees and targeting landscape
         * orientation may specify 640x480.
         *
         * <p>When the target resolution is set,
         * {@link ImageCapture#setCropAspectRatio(Rational)} will be automatically called to set
         * corresponding value. Such that the output image will be cropped into the desired
         * aspect ratio.
         *
         * <p>The maximum available resolution that could be selected for an {@link ImageCapture}
         * will depend on the camera device's capability.
         *
         * <p>If not set, the largest available resolution will be selected to use. Usually,
         * users will intend to get the largest still image that the camera device can support.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, which resolution
         * will be finally selected will depend on the camera device's hardware level and the
         * bound use cases combination. For more details see the guaranteed supported
         * configurations tables in {@link android.hardware.camera2.CameraDevice}'s
         * href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice
         * #regular-capture">Regular capture</a> section.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @deprecated use {@link ResolutionSelector} with {@link ResolutionStrategy} to specify
         * the preferred resolution settings instead.
         */
        @NonNull
        @Override
        @Deprecated
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(ImageOutputConfig.OPTION_DEFAULT_RESOLUTION,
                    resolution);
            return this;
        }

        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        /**
         * Sets the resolution selector to select the preferred supported resolution.
         *
         * <p>The default resolution strategy for ImageCapture is
         * {@link ResolutionStrategy#HIGHEST_AVAILABLE_STRATEGY}, which will select the largest
         * available resolution to use. Applications can override this default strategy with a
         * different resolution strategy.
         *
         * <p>The existing {@link #setTargetResolution(Size)} and
         * {@link #setTargetAspectRatio(int)} APIs are deprecated and are not compatible with
         * {@link #setResolutionSelector(ResolutionSelector)}. Calling either of these APIs
         * together with {@link #setResolutionSelector(ResolutionSelector)} will result in an
         * {@link IllegalArgumentException} being thrown when you attempt to build the
         * {@link ImageCapture} instance.
         *
         * @return The current Builder.
         */
        @Override
        @NonNull
        public Builder setResolutionSelector(@NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
            return this;
        }

        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setImageReaderProxyProvider(
                @NonNull ImageReaderProxyProvider imageReaderProxyProvider) {
            getMutableConfig().insertOption(OPTION_IMAGE_READER_PROXY_PROVIDER,
                    imageReaderProxyProvider);
            return this;
        }

        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setSoftwareJpegEncoderRequested(boolean requestSoftwareJpeg) {
            getMutableConfig().insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER,
                    requestSoftwareJpeg);
            return this;
        }

        /**
         * Sets the flashType.
         *
         * <p>If not set, the flash type will default to {@link #FLASH_TYPE_ONE_SHOT_FLASH}.
         *
         * @param flashType The requested flash mode. Value is {@link #FLASH_TYPE_ONE_SHOT_FLASH}
         *                  or {@link #FLASH_TYPE_USE_TORCH_AS_FLASH}.
         * @return The current Builder.
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setFlashType(@FlashType int flashType) {
            getMutableConfig().insertOption(OPTION_FLASH_TYPE, flashType);
            return this;
        }

        /**
         * Sets the output JPEG image compression quality.
         *
         * <p>This is used for the {@link ImageProxy} which is returned by
         * {@link #takePicture(Executor, OnImageCapturedCallback)} or the output JPEG image which
         * is saved by {@link #takePicture(OutputFileOptions, Executor, OnImageSavedCallback)}.
         * The saved JPEG image might be cropped according to the {@link ViewPort} setting or
         * the crop aspect ratio set by {@link #setCropAspectRatio(Rational)}. The JPEG quality
         * setting will also be used to compress the cropped output image.
         *
         * <p>If not set, a default value will be used according to the capture mode setting.
         * JPEG compression quality 95 is used for {@link #CAPTURE_MODE_MINIMIZE_LATENCY} and 100
         * is used for {@link #CAPTURE_MODE_MAXIMIZE_QUALITY}.
         *
         * @param jpegQuality The requested output JPEG image compression quality. The value must
         *                    be in range [1..100] which larger is higher quality.
         * @return The current Builder.
         * @throws IllegalArgumentException if the input value is not in range [1..100].
         */
        @NonNull
        public Builder setJpegQuality(@IntRange(from = 1, to = 100) int jpegQuality) {
            Preconditions.checkArgumentInRange(jpegQuality, 1, 100, "jpegQuality");
            getMutableConfig().insertOption(OPTION_JPEG_COMPRESSION_QUALITY,
                    jpegQuality);
            return this;
        }

        // Implementations of IoConfig.Builder default methods

        /**
         * Sets the default executor that will be used for IO tasks.
         *
         * <p> This executor will be used for any IO tasks specifically for ImageCapture, such as
         * {@link ImageCapture#takePicture(OutputFileOptions, Executor,
         * ImageCapture.OnImageSavedCallback)}. If no executor is set, then a default Executor
         * specifically for IO will be used instead.
         *
         * @param executor The executor which will be used for IO tasks.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setIoExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_IO_EXECUTOR, executor);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setUseCaseEventCallback(
                @NonNull UseCase.EventCallback useCaseEventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, useCaseEventCallback);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }
    }
}
