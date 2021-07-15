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

import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_BUFFER_FORMAT;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_CAPTURE_BUNDLE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_CAPTURE_PROCESSOR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_FLASH_MODE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IMAGE_READER_PROXY_PROVIDER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IO_EXECUTOR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_MAX_CAPTURE_STAGES;
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
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ATTACHED_USE_CASES_UPDATE_LISTENER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;
import static androidx.camera.core.internal.utils.ImageUtil.min;
import static androidx.camera.core.internal.utils.ImageUtil.sizeToVertexes;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy.OnImageCloseListener;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureMetaData.AeState;
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode;
import androidx.camera.core.impl.CameraCaptureMetaData.AfState;
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResult.EmptyCameraCaptureResult;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
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
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.IoConfig;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.YuvToJpegProcessor;
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk;
import androidx.camera.core.internal.compat.quirk.UseTorchAsFlashQuirk;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
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
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "ImageCapture";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private static final long CHECK_3A_TIMEOUT_IN_MS = 1000L;
    private static final int MAX_IMAGES = 2;
    // TODO(b/149336664) Move the quality to a compatibility class when there is a per device case.
    private static final byte JPEG_QUALITY_MAXIMIZE_QUALITY_MODE = 100;
    private static final byte JPEG_QUALITY_MINIMIZE_LATENCY_MODE = 95;
    @CaptureMode
    private static final int DEFAULT_CAPTURE_MODE = CAPTURE_MODE_MINIMIZE_LATENCY;
    @FlashMode
    private static final int DEFAULT_FLASH_MODE = FLASH_MODE_OFF;

    private final CaptureCallbackChecker mSessionCallbackChecker = new CaptureCallbackChecker();

    private final ImageReaderProxy.OnImageAvailableListener mClosingListener = (imageReader -> {
        try (ImageProxy image = imageReader.acquireLatestImage()) {
            Log.d(TAG, "Discarding ImageProxy which was inadvertently acquired: " + image);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to acquire latest image.", e);
        }
    });

    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mIoExecutor;
    @CaptureMode
    private final int mCaptureMode;

    /**
     * A flag to check 3A converged or not.
     *
     * <p>In order to speed up the taking picture process, trigger AF / AE should be skipped when
     * the flag is disabled. Set it to be enabled in the maximum quality mode and disabled in the
     * minimum latency mode.
     */
    private final boolean mEnableCheck3AConverged;

    @GuardedBy("mLockedFlashMode")
    private final AtomicReference<Integer> mLockedFlashMode = new AtomicReference<>(null);

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

    private ExecutorService mExecutor;

    private CaptureConfig mCaptureConfig;

    /** The set of requests that will be sent to the camera for the final captured image. */
    private CaptureBundle mCaptureBundle;
    private int mMaxCaptureStages;

    /**
     * Processing that gets done to the mCaptureBundle to produce the final image that is produced
     * by {@link #takePicture(Executor, OnImageCapturedCallback)}
     */
    private CaptureProcessor mCaptureProcessor;

    /**
     * Whether the software JPEG pipeline will be used.
     */
    private boolean mUseSoftwareJpeg = false;

    /**
     * Whether the torch flash will be used.
     *
     * <p>When the flag is set, torch will be opened and closed to replace the flash fired by flash
     * mode.
     */
    private boolean mUseTorchFlash = false;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SessionConfig.Builder mSessionConfigBuilder;

    /** synthetic accessor */
    @SuppressWarnings("WeakerAccess")
    SafeCloseImageReaderProxy mImageReader;

    @SuppressWarnings("WeakerAccess")
    ProcessingImageReader mProcessingImageReader;

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

        mIoExecutor = Preconditions.checkNotNull(
                useCaseConfig.getIoExecutor(CameraXExecutors.ioExecutor()));
        mSequentialIoExecutor = CameraXExecutors.newSequentialExecutor(mIoExecutor);

        if (mCaptureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
            mEnableCheck3AConverged = true; // check 3A convergence in MAX_QUALITY mode
        } else {
            mEnableCheck3AConverged = false; // skip 3A convergence in MIN_LATENCY mode
        }
    }

    @UiThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageCaptureConfig config, @NonNull Size resolution) {
        Threads.checkMainThread();
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);
        sessionConfigBuilder.addRepeatingCameraCaptureCallback(mSessionCallbackChecker);

        // Setup the ImageReader to do processing
        if (config.getImageReaderProxyProvider() != null) {
            mImageReader =
                    new SafeCloseImageReaderProxy(
                            config.getImageReaderProxyProvider().newInstance(resolution.getWidth(),
                                    resolution.getHeight(), getImageFormat(), MAX_IMAGES, 0));
            mMetadataMatchingCaptureCallback = new CameraCaptureCallback() {
            };
        } else if (mCaptureProcessor != null || mUseSoftwareJpeg) {
            // Capture processor set from configuration takes precedence over software JPEG.
            YuvToJpegProcessor softwareJpegProcessor = null;
            CaptureProcessorPipeline captureProcessorPipeline = null;
            CaptureProcessor captureProcessor = mCaptureProcessor;
            int inputFormat = getImageFormat();
            int outputFormat = getImageFormat();
            if (mUseSoftwareJpeg) {
                // API check to satisfy linter
                if (Build.VERSION.SDK_INT >= 26) {
                    Logger.i(TAG, "Using software JPEG encoder.");

                    if (mCaptureProcessor != null) {
                        softwareJpegProcessor = new YuvToJpegProcessor(getJpegQuality(),
                                mMaxCaptureStages);
                        captureProcessor = captureProcessorPipeline = new CaptureProcessorPipeline(
                                mCaptureProcessor, mMaxCaptureStages, softwareJpegProcessor,
                                mExecutor);
                    } else {
                        captureProcessor = softwareJpegProcessor =
                                new YuvToJpegProcessor(getJpegQuality(), mMaxCaptureStages);
                    }

                    outputFormat = ImageFormat.JPEG;
                } else {
                    // Note: This should never be hit due to SDK_INT check before setting
                    // useSoftwareJpeg.
                    throw new IllegalStateException("Software JPEG only supported on API 26+");
                }
            }

            // TODO: To allow user to use an Executor for the image processing.
            mProcessingImageReader = new ProcessingImageReader.Builder(resolution.getWidth(),
                    resolution.getHeight(), inputFormat, mMaxCaptureStages,
                    getCaptureBundle(CaptureBundles.singleDefaultCaptureBundle()),
                    captureProcessor).setPostProcessExecutor(mExecutor).setOutputFormat(
                    outputFormat).build();

            mMetadataMatchingCaptureCallback = mProcessingImageReader.getCameraCaptureCallback();
            mImageReader = new SafeCloseImageReaderProxy(mProcessingImageReader);
            if (softwareJpegProcessor != null) {
                // Close the JPEG processor once ProcessingImageReader is done.
                // Processor is assigned to an effectively final variable here for the lambda.
                YuvToJpegProcessor processorToClose = softwareJpegProcessor;
                CaptureProcessorPipeline captureProcessorPipelineToClose = captureProcessorPipeline;
                mProcessingImageReader.getCloseFuture().addListener(() -> {
                    // API check to satisfy linter
                    if (Build.VERSION.SDK_INT >= 26) {
                        processorToClose.close();
                        captureProcessorPipelineToClose.close();
                    }
                }, CameraXExecutors.directExecutor());
            }
        } else {
            MetadataImageReader metadataImageReader = new MetadataImageReader(resolution.getWidth(),
                    resolution.getHeight(), getImageFormat(), MAX_IMAGES);
            mMetadataMatchingCaptureCallback = metadataImageReader.getCameraCaptureCallback();
            mImageReader = new SafeCloseImageReaderProxy(metadataImageReader);
        }
        mImageCaptureRequestProcessor = new ImageCaptureRequestProcessor(MAX_IMAGES,
                request -> takePictureInternal(request));

        // By default close images that come from the listener.
        mImageReader.setOnImageAvailableListener(mClosingListener,
                CameraXExecutors.mainThreadExecutor());

        SafeCloseImageReaderProxy imageReaderProxy = mImageReader;
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = new ImmediateSurface(
                mImageReader.getSurface(), new Size(mImageReader.getWidth(),
                mImageReader.getHeight()), mImageReader.getImageFormat());
        mDeferrableSurface.getTerminationFuture().addListener(
                imageReaderProxy::safeClose, CameraXExecutors.mainThreadExecutor());
        sessionConfigBuilder.addNonRepeatingSurface(mDeferrableSurface);

        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            clearPipeline();
            // Ensure the attached camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                mSessionConfigBuilder = createPipeline(cameraId, config, resolution);
                updateSessionConfig(mSessionConfigBuilder.build());
                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    @UiThread
    @SuppressWarnings("WeakerAccess")
    void clearPipeline() {
        Threads.checkMainThread();
        DeferrableSurface deferrableSurface = mDeferrableSurface;
        mDeferrableSurface = null;
        mImageReader = null;
        mProcessingImageReader = null;

        if (deferrableSurface != null) {
            deferrableSurface.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return Builder.fromConfig(config);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        if (builder.getUseCaseConfig().retrieveOption(OPTION_CAPTURE_PROCESSOR, null)
                != null && Build.VERSION.SDK_INT >= 29) {
            // TODO: The API level check can be removed if the ImageWriterCompat issue on API
            //  level 28 devices (b182363220/) can be resolved.
            Logger.i(TAG, "Requesting software JPEG due to a CaptureProcessor is set.");
            builder.getMutableConfig().insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, true);
        } else if (cameraInfo.getCameraQuirks().contains(
                SoftwareJpegEncodingPreferredQuirk.class)) {
            // Request software JPEG encoder if quirk exists on this device and the software JPEG
            // option has not already been explicitly set.
            if (!builder.getMutableConfig().retrieveOption(OPTION_USE_SOFTWARE_JPEG_ENCODER,
                    true)) {
                Logger.w(TAG, "Device quirk suggests software JPEG encoder, but it has been "
                        + "explicitly disabled.");
            } else {
                Logger.i(TAG, "Requesting software JPEG due to device quirk.");
                builder.getMutableConfig().insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, true);
            }
        }

        // If software JPEG is requested, disable if it can't be supported on current API level.
        boolean useSoftwareJpeg = enforceSoftwareJpegConstraints(builder.getMutableConfig());

        // Update the input format base on the other options set (mainly whether processing
        // is done)
        Integer bufferFormat = builder.getMutableConfig().retrieveOption(OPTION_BUFFER_FORMAT,
                null);
        if (bufferFormat != null) {
            Preconditions.checkArgument(
                    builder.getMutableConfig().retrieveOption(OPTION_CAPTURE_PROCESSOR, null)
                            == null,
                    "Cannot set buffer format with CaptureProcessor defined.");
            builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                    useSoftwareJpeg ? ImageFormat.YUV_420_888 : bufferFormat);
        } else {
            if (builder.getMutableConfig().retrieveOption(OPTION_CAPTURE_PROCESSOR, null) != null
                    || useSoftwareJpeg) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                        ImageFormat.YUV_420_888);
            } else {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.JPEG);
            }
        }

        Preconditions.checkArgument(
                builder.getMutableConfig().retrieveOption(OPTION_MAX_CAPTURE_STAGES, MAX_IMAGES)
                        >= 1,
                "Maximum outstanding image count must be at least 1");
        return builder.getUseCaseConfig();
    }

    /**
     * Configures flash mode to CameraControlInternal once it is ready.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    protected void onCameraControlReady() {
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
     * the torch is disabled, flash will function as specified by {@link #setFlashMode(int)}.
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
     * {@link ImageCapture#setTargetRotation(int)}. The rotation of an image taken is determined
     * by the rotation value set at the time image capture is initiated, such as when calling
     * {@link #takePicture(Executor, OnImageCapturedCallback)}.
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
     * {@link ImageCapture#setTargetRotation(int)} allows the target rotation to be set dynamically.
     *
     * <p>In general, it is best to use an {@link android.view.OrientationEventListener} to
     * set the target rotation.  This way, the rotation output will indicate which way is down for
     * a given image.  This is important since display orientation may be locked by device
     * default, user setting, or app configuration, and some devices may not transition to a
     * reverse-portrait display orientation. In these cases,
     * use {@link ImageCapture#setTargetRotation} to set target rotation dynamically according to
     * the {@link android.view.OrientationEventListener}, without re-creating the use case.  Note
     * the OrientationEventListener output of degrees in the range [0..359] should be converted to
     * a surface rotation. The mapping values are listed as the following.
     * <p>{@link android.view.OrientationEventListener#ORIENTATION_UNKNOWN}: orientation == -1
     * <p>{@link Surface#ROTATION_0}: orientation >= 315 || orientation < 45
     * <p>{@link Surface#ROTATION_90}: orientation >= 225 && orientation < 315
     * <p>{@link Surface#ROTATION_180}: orientation >= 135 && orientation < 225
     * <p>{@link Surface#ROTATION_270}: orientation >= 45 && orientation < 135
     *
     * <p>When this function is called, value set by
     * {@link ImageCapture.Builder#setTargetResolution(Size)} will be updated automatically to make
     * sure the suitable resolution can be selected when the use case is bound. Value set by
     * {@link ImageCapture#setCropAspectRatio(Rational)} will also be updated automatically to
     * make sure the output image is cropped into expected aspect ratio.
     *
     * <p>If no target rotation is set by the application, it is set to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is created. The
     * use case is fully created once it has been attached to a camera.
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
     * The application needs to call {@link #getResolutionInfo()} again to get the latest
     * {@link ResolutionInfo} for the changes.
     *
     * @return the resolution information if the use case has been bound by the
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner
     * , CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     */
    @Nullable
    @Override
    public ResolutionInfo getResolutionInfo() {
        return super.getResolutionInfo();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
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

        return ResolutionInfo.create(resolution, cropRect, rotationDegrees);
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

        sendImageCaptureRequest(executor, callback);
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
                        switch (error) {
                            case FILE_IO_FAILED:
                                imageCaptureError = ERROR_FILE_IO;
                                break;
                            default:
                                // Keep the imageCaptureError as UNKNOWN_ERROR
                                break;
                        }

                        imageSavedCallback.onError(
                                new ImageCaptureException(imageCaptureError, message, cause));
                    }
                };

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
        sendImageCaptureRequest(CameraXExecutors.mainThreadExecutor(), imageCaptureCallbackWrapper);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    @Override
    public void onStateDetached() {
        abortImageCaptureRequests();
    }

    private void abortImageCaptureRequests() {
        Throwable throwable = new CameraClosedException("Camera is closed.");
        mImageCaptureRequestProcessor.cancelRequests(throwable);
    }

    @UiThread
    private void sendImageCaptureRequest(
            @NonNull Executor callbackExecutor, @NonNull OnImageCapturedCallback callback) {

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

        mImageCaptureRequestProcessor.sendRequest(new ImageCaptureRequest(
                getRelativeRotation(attachedCamera), getJpegQuality(), mCropAspectRatio,
                getViewPortCropRect(), callbackExecutor, callback));
    }

    private void lockFlashMode() {
        synchronized (mLockedFlashMode) {
            if (mLockedFlashMode.get() != null) {
                // FlashMode is locked.
                return;
            }
            mLockedFlashMode.set(getFlashMode());
        }
    }

    private void unlockFlashMode() {
        synchronized (mLockedFlashMode) {
            Integer lockedFlashMode = mLockedFlashMode.getAndSet(null);
            if (lockedFlashMode == null) {
                // FlashMode is not locked yet.
                return;
            }
            if (lockedFlashMode.intValue() != getFlashMode()) {
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
    private int getJpegQuality() {
        switch (mCaptureMode) {
            case CAPTURE_MODE_MAXIMIZE_QUALITY:
                return JPEG_QUALITY_MAXIMIZE_QUALITY_MODE;
            case CAPTURE_MODE_MINIMIZE_LATENCY:
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

                    TakePictureState state = new TakePictureState();
                    ListenableFuture<Void> future = FutureChain.from(preTakePicture(state))
                            .transformAsync(v -> issueTakePicture(imageCaptureRequest), mExecutor);

                    Futures.addCallback(future,
                            new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    postTakePicture(state);
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    postTakePicture(state);

                                    completer.setException(throwable);
                                }
                            },
                            mExecutor);

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

        @Override
        public void onImageClose(ImageProxy image) {
            synchronized (mLock) {
                mOutstandingImages--;
                processNextRequest();
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
                            Preconditions.checkNotNull(image);
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
                    public void onFailure(Throwable t) {
                        synchronized (mLock) {
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
                }, CameraXExecutors.directExecutor());
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
        } else if (throwable instanceof CaptureFailedException) {
            return ERROR_CAPTURE_FAILED;
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
    static boolean enforceSoftwareJpegConstraints(@NonNull MutableConfig mutableConfig) {
        // Software encoder currently only supports API 26+.
        if (mutableConfig.retrieveOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, false)) {
            boolean supported = true;
            if (Build.VERSION.SDK_INT < 26) {
                Logger.w(TAG, "Software JPEG only supported on API 26+, but current API level is "
                        + Build.VERSION.SDK_INT);
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
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onDetached() {
        abortImageCaptureRequests();
        clearPipeline();
        mUseSoftwareJpeg = false;
        mExecutor.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onAttached() {
        ImageCaptureConfig useCaseConfig = (ImageCaptureConfig) getCurrentConfig();

        CaptureConfig.Builder captureBuilder = CaptureConfig.Builder.createFrom(useCaseConfig);
        mCaptureConfig = captureBuilder.build();

        // Retrieve camera specific settings.
        mCaptureProcessor = useCaseConfig.getCaptureProcessor(null);
        mMaxCaptureStages = useCaseConfig.getMaxCaptureStages(MAX_IMAGES);
        mCaptureBundle = useCaseConfig.getCaptureBundle(
                CaptureBundles.singleDefaultCaptureBundle());

        // This will only be set to true if software JPEG was requested and
        // enforceSoftwareJpegConstraints() hasn't removed the request.
        mUseSoftwareJpeg = useCaseConfig.isSoftwareJpegEncoderRequested();

        CameraInternal camera = getCamera();
        Preconditions.checkNotNull(camera, "Attached camera cannot be null");
        mUseTorchFlash = camera.getCameraInfoInternal().getCameraQuirks()
                .contains(UseTorchAsFlashQuirk.class);
        if (mUseTorchFlash) {
            Logger.d(TAG, "Open and close torch to replace the flash fired by flash mode.");
        }

        mExecutor =
                Executors.newFixedThreadPool(
                        1,
                        new ThreadFactory() {
                            private final AtomicInteger mId = new AtomicInteger(0);

                            @Override
                            public Thread newThread(@NonNull Runnable r) {
                                return new Thread(
                                        r,
                                        CameraXThreads.TAG + "image_capture_"
                                                + mId.getAndIncrement());
                            }
                        });
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
        mSessionConfigBuilder = createPipeline(getCameraId(),
                (ImageCaptureConfig) getCurrentConfig(), suggestedResolution);

        updateSessionConfig(mSessionConfigBuilder.build());

        // In order to speed up the take picture process, notifyActive at an early stage to
        // attach the session capture callback to repeating and get capture result all the time.
        notifyActive();
        return suggestedResolution;
    }

    /**
     * Routine before taking picture.
     *
     * <p>For example, trigger 3A scan, open torch and check 3A converged if necessary.
     */
    private ListenableFuture<Void> preTakePicture(final TakePictureState state) {
        lockFlashMode();
        return FutureChain.from(getPreCaptureStateIfNeeded())
                .transformAsync(captureResult -> {
                    state.mPreCaptureState = captureResult;
                    triggerAfIfNeeded(state);
                    if (isFlashRequired(state)) {
                        if (mUseTorchFlash) {
                            return openTorch(state);
                        } else {
                            return triggerAePrecapture(state);
                        }
                    }
                    return Futures.immediateFuture(null);
                }, mExecutor)
                .transformAsync(v -> check3AConverged(state), mExecutor)
                // Ignore the 3A convergence result.
                .transform(is3AConverged -> null, mExecutor);
    }

    /**
     * Routine after picture was taken.
     *
     * <p>For example, cancel 3A scan, close torch if necessary.
     */
    void postTakePicture(final TakePictureState state) {
        closeTorch(state);
        cancelAfAeTrigger(state);
        unlockFlashMode();
    }

    @NonNull
    private ListenableFuture<Void> openTorch(@NonNull TakePictureState state) {
        CameraInternal camera = getCamera();
        if (camera != null && camera.getCameraInfo().getTorchState().getValue() == TorchState.ON) {
            // Torch is already opened.
            return Futures.immediateFuture(null);
        }

        Logger.d(TAG, "openTorch");

        // Create a new future in order to ignore any fail from CameraControl.enableTorch().
        return CallbackToFutureAdapter.getFuture(completer -> {
            getCameraControl().enableTorch(state.mIsTorchOpened = true).addListener(
                    () -> completer.set(null), CameraXExecutors.directExecutor());
            return "openTorch";
        });
    }

    private void closeTorch(@NonNull TakePictureState state) {
        if (state.mIsTorchOpened) {
            // Add listener to avoid FutureReturnValueIgnored error.
            getCameraControl().enableTorch(state.mIsTorchOpened = false).addListener(() -> {
            }, CameraXExecutors.directExecutor());
        }
    }

    /**
     * Gets a capture result or not according to current configuration.
     *
     * <p>Conditions to get a capture result.
     *
     * <p>(1) The enableCheck3AConverged is enabled because it needs to know current AF mode and
     * state.
     *
     * <p>(2) The flashMode is AUTO because it needs to know the current AE state.
     */
    // Currently this method is used to prevent there is no repeating surface to get capture result.
    // If app is in min-latency mode and flash ALWAYS/OFF mode, it can still take picture without
    // checking the capture result. Remove this check once no repeating surface issue is fixed.
    private ListenableFuture<CameraCaptureResult> getPreCaptureStateIfNeeded() {
        if (mEnableCheck3AConverged || getFlashMode() == FLASH_MODE_AUTO) {
            return mSessionCallbackChecker.checkCaptureResult(
                    new CaptureCallbackChecker.CaptureResultChecker<CameraCaptureResult>() {
                        @Override
                        public CameraCaptureResult check(
                                @NonNull CameraCaptureResult captureResult) {
                            if (Logger.isDebugEnabled(TAG)) {
                                Logger.d(TAG, "preCaptureState, AE=" + captureResult.getAeState()
                                        + " AF =" + captureResult.getAfState()
                                        + " AWB=" + captureResult.getAwbState());
                            }
                            return captureResult;
                        }
                    });
        }
        return Futures.immediateFuture(null);
    }

    boolean isFlashRequired(@NonNull TakePictureState state) {
        switch (getFlashMode()) {
            case FLASH_MODE_ON:
                return true;
            case FLASH_MODE_AUTO:
                return state.mPreCaptureState.getAeState() == AeState.FLASH_REQUIRED;
            case FLASH_MODE_OFF:
                return false;
        }
        throw new AssertionError(getFlashMode());
    }

    ListenableFuture<Boolean> check3AConverged(TakePictureState state) {
        if (!mEnableCheck3AConverged && !state.mIsAePrecaptureTriggered && !state.mIsTorchOpened) {
            return Futures.immediateFuture(false);
        }

        return mSessionCallbackChecker.checkCaptureResult(
                new CaptureCallbackChecker.CaptureResultChecker<Boolean>() {
                    @Override
                    public Boolean check(@NonNull CameraCaptureResult captureResult) {
                        if (Logger.isDebugEnabled(TAG)) {
                            Logger.d(TAG, "checkCaptureResult, AE=" + captureResult.getAeState()
                                    + " AF =" + captureResult.getAfState()
                                    + " AWB=" + captureResult.getAwbState());
                        }

                        if (is3AConverged(captureResult)) {
                            return true;
                        }
                        // Return null to continue check.
                        return null;
                    }
                },
                CHECK_3A_TIMEOUT_IN_MS,
                false);
    }

    boolean is3AConverged(CameraCaptureResult captureResult) {
        if (captureResult == null) {
            return false;
        }

        // If afMode is CAF, don't check af locked to speed up.
        // If afMode is OFF or UNKNOWN , no need for waiting.
        // otherwise wait until af is locked or focused.
        boolean isAfReady = (captureResult.getAfMode() == AfMode.ON_CONTINUOUS_AUTO
                || captureResult.getAfMode() == AfMode.OFF
                || captureResult.getAfMode() == AfMode.UNKNOWN
                || captureResult.getAfState() == AfState.FOCUSED
                || captureResult.getAfState() == AfState.LOCKED_FOCUSED
                || captureResult.getAfState() == AfState.LOCKED_NOT_FOCUSED);

        // Unknown means cannot get valid state from CaptureResult
        boolean isAeReady = captureResult.getAeState() == AeState.CONVERGED
                || captureResult.getAeState() == AeState.FLASH_REQUIRED
                || captureResult.getAeState() == AeState.UNKNOWN;

        // Unknown means cannot get valid state from CaptureResult
        boolean isAwbReady = captureResult.getAwbState() == AwbState.CONVERGED
                || captureResult.getAwbState() == AwbState.UNKNOWN;

        return (isAfReady && isAeReady && isAwbReady);
    }

    /**
     * Issues the AF scan if needed.
     *
     * <p>If enableCheck3AConverged is disabled or it is in CAF mode, AF scan should not be
     * triggered. Trigger AF scan only in {@link AfMode#ON_MANUAL_AUTO} and current AF state is
     * {@link AfState#INACTIVE}. If the AF mode is {@link AfMode#ON_MANUAL_AUTO} and AF state is not
     * inactive, it means that a manual or auto focus request may be in progress or completed.
     */
    void triggerAfIfNeeded(TakePictureState state) {
        if (mEnableCheck3AConverged
                && state.mPreCaptureState.getAfMode() == AfMode.ON_MANUAL_AUTO
                && state.mPreCaptureState.getAfState() == AfState.INACTIVE) {
            triggerAf(state);
        }
    }

    /** Issues a request to start auto focus scan. */
    private void triggerAf(TakePictureState state) {
        Logger.d(TAG, "triggerAf");
        state.mIsAfTriggered = true;
        ListenableFuture<CameraCaptureResult> future = getCameraControl().triggerAf();
        // Add listener to avoid FutureReturnValueIgnored error.
        future.addListener(() -> {
        }, CameraXExecutors.directExecutor());
    }

    /** Issues a request to start auto exposure scan. */
    ListenableFuture<Void> triggerAePrecapture(TakePictureState state) {
        Logger.d(TAG, "triggerAePrecapture");
        state.mIsAePrecaptureTriggered = true;
        // Transform type from CameraCaptureResult to Void
        return Futures.transform(getCameraControl().triggerAePrecapture(), captureResult -> null,
                CameraXExecutors.directExecutor());
    }

    /** Issues a request to cancel auto focus and/or auto exposure scan. */
    void cancelAfAeTrigger(TakePictureState state) {
        if (!state.mIsAfTriggered && !state.mIsAePrecaptureTriggered) {
            return;
        }
        getCameraControl()
                .cancelAfAeTrigger(state.mIsAfTriggered, state.mIsAePrecaptureTriggered);
        state.mIsAfTriggered = false;
        state.mIsAePrecaptureTriggered = false;
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

        final List<ListenableFuture<Void>> futureList = new ArrayList<>();
        final List<CaptureConfig> captureConfigs = new ArrayList<>();
        String tagBundleKey = null;

        CaptureBundle captureBundle;
        if (mProcessingImageReader != null) {
            // If the Processor is provided, check if we have valid CaptureBundle and update
            // ProcessingImageReader before actually issuing a take picture request.
            captureBundle = getCaptureBundle(CaptureBundles.singleDefaultCaptureBundle());

            if (captureBundle == null) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "ImageCapture cannot set empty CaptureBundle."));
            }

            if (mCaptureProcessor == null && captureBundle.getCaptureStages().size() > 1) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "No CaptureProcessor can be found to process the images captured for "
                                + "multiple CaptureStages."));
            }

            if (captureBundle.getCaptureStages().size() > mMaxCaptureStages) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "ImageCapture has CaptureStages > Max CaptureStage size"));
            }

            mProcessingImageReader.setCaptureBundle(captureBundle);
            tagBundleKey = mProcessingImageReader.getTagBundleKey();
        } else {
            captureBundle = getCaptureBundle(CaptureBundles.singleDefaultCaptureBundle());
            if (captureBundle.getCaptureStages().size() > 1) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "ImageCapture have no CaptureProcess set with CaptureBundle size > 1."));
            }
        }

        for (final CaptureStage captureStage : captureBundle.getCaptureStages()) {
            final CaptureConfig.Builder builder = new CaptureConfig.Builder();
            builder.setTemplateType(mCaptureConfig.getTemplateType());

            // Add the default implementation options of ImageCapture
            builder.addImplementationOptions(mCaptureConfig.getImplementationOptions());
            builder.addAllCameraCaptureCallbacks(
                    mSessionConfigBuilder.getSingleCameraCaptureCallbacks());

            builder.addSurface(mDeferrableSurface);

            // Add the dynamic implementation options of ImageCapture
            if (new ExifRotationAvailability().isRotationOptionSupported()) {
                builder.addImplementationOption(CaptureConfig.OPTION_ROTATION,
                        imageCaptureRequest.mRotationDegrees);
            }
            builder.addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY,
                    imageCaptureRequest.mJpegQuality);

            // Add the implementation options required by the CaptureStage
            builder.addImplementationOptions(
                    captureStage.getCaptureConfig().getImplementationOptions());

            // Use CaptureBundle object as the key for TagBundle
            if (tagBundleKey != null) {
                builder.addTag(tagBundleKey, captureStage.getId());
            }
            builder.addCameraCaptureCallback(mMetadataMatchingCaptureCallback);

            ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        CameraCaptureCallback completerCallback = new CameraCaptureCallback() {
                            @Override
                            public void onCaptureCompleted(
                                    @NonNull CameraCaptureResult result) {
                                completer.set(null);
                            }

                            @Override
                            public void onCaptureFailed(
                                    @NonNull CameraCaptureFailure failure) {
                                String msg = "Capture request failed with reason "
                                        + failure.getReason();
                                completer.setException(new CaptureFailedException(msg));
                            }

                            @Override
                            public void onCaptureCancelled() {
                                String msg = "Capture request is cancelled because "
                                        + "camera is closed";
                                completer.setException(new CameraClosedException(msg));
                            }
                        };
                        builder.addCameraCaptureCallback(completerCallback);

                        captureConfigs.add(builder.build());
                        return "issueTakePicture[stage=" + captureStage.getId() + "]";
                    });
            futureList.add(future);

        }

        getCameraControl().submitCaptureRequests(captureConfigs);
        return Futures.transform(Futures.allAsList(futureList),
                input -> null, CameraXExecutors.directExecutor());
    }

    /** This exception is thrown when request is failed (reported by framework) */
    static final class CaptureFailedException extends RuntimeException {
        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        CaptureFailedException(String s, Throwable e) {
            super(s, e);
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        CaptureFailedException(String s) {
            super(s);
        }
    }


    private CaptureBundle getCaptureBundle(CaptureBundle defaultCaptureBundle) {
        List<CaptureStage> captureStages = mCaptureBundle.getCaptureStages();
        if (captureStages == null || captureStages.isEmpty()) {
            return defaultCaptureBundle;
        }

        return CaptureBundles.createCaptureBundle(captureStages);
    }

    /**
     * Describes the error that occurred during an image capture operation (such as {@link
     * ImageCapture#takePicture(Executor, OnImageCapturedCallback)}).
     *
     * <p>This is a parameter sent to the error callback functions set in listeners such as {@link
     * ImageCapture.OnImageSavedCallback#onError(ImageCaptureException)}.
     *
     * @hide
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
     *
     * @hide
     */
    @IntDef({CAPTURE_MODE_MAXIMIZE_QUALITY, CAPTURE_MODE_MINIMIZE_LATENCY})
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
     *
     * @hide
     */
    @IntDef({FLASH_MODE_UNKNOWN, FLASH_MODE_AUTO, FLASH_MODE_ON, FLASH_MODE_OFF})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface FlashMode {
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
         * <p>See also {@link Builder#setTargetRotation(int)} and
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
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults
            implements ConfigProvider<ImageCaptureConfig> {
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 4;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;

        private static final ImageCaptureConfig DEFAULT_CONFIG;

        static {
            Builder builder = new Builder().setSurfaceOccupancyPriority(
                    DEFAULT_SURFACE_OCCUPANCY_PRIORITY).setTargetAspectRatio(DEFAULT_ASPECT_RATIO);

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

        @Nullable
        File getFile() {
            return mFile;
        }

        @Nullable
        ContentResolver getContentResolver() {
            return mContentResolver;
        }

        @Nullable
        Uri getSaveCollection() {
            return mSaveCollection;
        }

        @Nullable
        ContentValues getContentValues() {
            return mContentValues;
        }

        @Nullable
        OutputStream getOutputStream() {
            return mOutputStream;
        }

        /**
         * Exposed internally so that CameraController can overwrite the flip horizontal flag for
         * front camera. External core API users shouldn't need this because they are the ones who
         * created the {@link Metadata}.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Metadata getMetadata() {
            return mMetadata;
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
        private Uri mSavedUri;

        OutputFileResults(@Nullable Uri savedUri) {
            mSavedUri = savedUri;
        }

        /**
         * Returns the {@link Uri} of the saved file.
         *
         * <p> This field is only returned if the {@link OutputFileOptions} is backed by
         * {@link MediaStore} constructed with
         *
         * {@link androidx.camera.core.ImageCapture.OutputFileOptions.Builder
         * #Builder(ContentResolver, Uri, ContentValues)}.
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
         *
         * @hide
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
    }

    /**
     * An intermediate action recorder while taking picture. It is used to restore certain states.
     * For example, cancel AF/AE scan, and close flash light.
     */
    static final class TakePictureState {
        CameraCaptureResult mPreCaptureState = EmptyCameraCaptureResult.create();
        boolean mIsTorchOpened = false;
        boolean mIsAfTriggered = false;
        boolean mIsAePrecaptureTriggered = false;
    }

    /**
     * A helper class to check camera capture result.
     *
     * <p>CaptureCallbackChecker is an implementation of {@link CameraCaptureCallback} that checks a
     * specified list of condition and sets a ListenableFuture when the conditions have been met. It
     * is mainly used to continuously capture callbacks to detect specific conditions. It also
     * handles the timeout condition if the check condition does not satisfy the given timeout, and
     * returns the given default value if the timeout is met.
     */
    static final class CaptureCallbackChecker extends CameraCaptureCallback {
        private static final long NO_TIMEOUT = 0L;

        /** Capture listeners. */
        private final Set<CaptureResultListener> mCaptureResultListeners = new HashSet<>();

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            deliverCaptureResultToListeners(cameraCaptureResult);
        }

        /**
         * Check the capture results of current session capture callback by giving a {@link
         * CaptureResultChecker}.
         *
         * @param checker a CaptureResult checker that returns an object with type T if the check is
         *                complete, returning null to continue the check process.
         * @param <T>     the type parameter for CaptureResult checker.
         * @return a listenable future for capture result check process.
         */
        <T> ListenableFuture<T> checkCaptureResult(CaptureResultChecker<T> checker) {
            return checkCaptureResult(checker, NO_TIMEOUT, null);
        }

        /**
         * Check the capture results of current session capture callback with timeout limit by
         * giving a {@link CaptureResultChecker}.
         *
         * @param checker     a CaptureResult checker that returns an object with type T if the
         *                    check is
         *                    complete, returning null to continue the check process.
         * @param timeoutInMs used to force stop checking.
         * @param defValue    the default return value if timeout occur.
         * @param <T>         the type parameter for CaptureResult checker.
         * @return a listenable future for capture result check process.
         */
        <T> ListenableFuture<T> checkCaptureResult(
                final CaptureResultChecker<T> checker, final long timeoutInMs, final T defValue) {
            if (timeoutInMs < NO_TIMEOUT) {
                throw new IllegalArgumentException("Invalid timeout value: " + timeoutInMs);
            }
            final long startTimeInMs =
                    (timeoutInMs != NO_TIMEOUT) ? SystemClock.elapsedRealtime() : 0L;

            return CallbackToFutureAdapter.getFuture(
                    completer -> {
                        addListener(
                                new CaptureResultListener() {
                                    @Override
                                    public boolean onCaptureResult(
                                            @NonNull CameraCaptureResult captureResult) {
                                        T result = checker.check(captureResult);
                                        if (result != null) {
                                            completer.set(result);
                                            return true;
                                        } else if (startTimeInMs > 0
                                                && SystemClock.elapsedRealtime() - startTimeInMs
                                                > timeoutInMs) {
                                            completer.set(defValue);
                                            return true;
                                        }
                                        // Return false to continue check.
                                        return false;
                                    }
                                });
                        return "checkCaptureResult";
                    });
        }

        /**
         * Delivers camera capture result to {@link CaptureCallbackChecker#mCaptureResultListeners}.
         */
        private void deliverCaptureResultToListeners(@NonNull CameraCaptureResult captureResult) {
            synchronized (mCaptureResultListeners) {
                Set<CaptureResultListener> removeSet = null;
                for (CaptureResultListener listener : new HashSet<>(mCaptureResultListeners)) {
                    // Remove listener if the callback return true
                    if (listener.onCaptureResult(captureResult)) {
                        if (removeSet == null) {
                            removeSet = new HashSet<>();
                        }
                        removeSet.add(listener);
                    }
                }
                if (removeSet != null) {
                    mCaptureResultListeners.removeAll(removeSet);
                }
            }
        }

        /** Add capture result listener. */
        void addListener(CaptureResultListener listener) {
            synchronized (mCaptureResultListeners) {
                mCaptureResultListeners.add(listener);
            }
        }

        /** An interface to check camera capture result. */
        public interface CaptureResultChecker<T> {

            /**
             * The callback to check camera capture result.
             *
             * @param captureResult the camera capture result.
             * @return the check result, return null to continue checking.
             */
            @Nullable
            T check(@NonNull CameraCaptureResult captureResult);
        }

        /** An interface to listen to camera capture results. */
        private interface CaptureResultListener {

            /**
             * Callback to handle camera capture results.
             *
             * @param captureResult camera capture result.
             * @return true to finish listening, false to continue listening.
             */
            boolean onCaptureResult(@NonNull CameraCaptureResult captureResult);
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

        /**
         * @param rotationDegrees The degrees to rotate the image buffer from sensor
         *                        coordinates into the final output coordinate space.
         * @param targetRatio     The aspect ratio of the image in final output coordinate space.
         *                        This must be a non-negative, non-zero value.
         * @throws IllegalArgumentException If targetRatio is not a valid value.
         */
        ImageCaptureRequest(
                @RotationValue int rotationDegrees,
                @IntRange(from = 1, to = 100) int jpegQuality,
                Rational targetRatio,
                @Nullable Rect viewPortCropRect,
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
            int dispatchRotationDegrees = 0;

            // Retrieve the dimension and rotation values from the embedded EXIF data in the
            // captured image only if those information is available.
            if (new ExifRotationAvailability().shouldUseExifOrientation(image)) {
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
                    image.getImageInfo().getTimestamp(), dispatchRotationDegrees);

            final ImageProxy dispatchedImageProxy = new SettableImageProxy(image,
                    dispatchResolution,
                    imageInfo);

            // Update the crop rect aspect ratio after it has been rotated into the buffer
            // orientation
            if (mViewPortCropRect != null) {
                // If Viewport is present, use the Viewport-based crop rect.
                dispatchedImageProxy.setCropRect(getDispatchCropRect(mViewPortCropRect,
                        mRotationDegrees, dispatchResolution, dispatchRotationDegrees));
            } else if (mTargetRatio != null) {
                // Fall back to crop aspect ratio if view port is not available.
                Rational dispatchRatio = mTargetRatio;
                if ((dispatchRotationDegrees % 180) != 0) {
                    dispatchRatio = new Rational(
                            /* invert the ratio numerator=*/ mTargetRatio.getDenominator(),
                            /* invert the ratio denominator=*/ mTargetRatio.getNumerator());
                }
                Size sourceSize = new Size(dispatchedImageProxy.getWidth(),
                        dispatchedImageProxy.getHeight());
                if (ImageUtil.isAspectRatioValid(sourceSize, dispatchRatio)) {
                    dispatchedImageProxy.setCropRect(
                            ImageUtil.computeCropRectFromAspectRatio(sourceSize,
                                    dispatchRatio));
                }
            }

            try {
                mListenerExecutor.execute(() -> {
                    mCallback.onCaptureSuccess(dispatchedImageProxy);
                });
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "Unable to post to the supplied executor.");

                // Unable to execute on the supplied executor, close the image.
                image.close();
            }
        }

        /**
         * Corrects crop rect based on JPEG exif rotation.
         *
         * <p> The original crop rect is calculated based on camera sensor buffer. On some devices,
         * the buffer is rotated before being passed to users, in which case the crop rect also
         * needs additional transformations.
         *
         * <p> There are two most common scenarios: 1) exif rotation is 0, or 2) exif rotation
         * equals output rotation. 1) means the HAL rotated the buffer based on target
         * rotation. 2) means HAL no-oped on the rotation. Theoretically only 1) needs
         * additional transformations, but this method is also generic enough to handle all possible
         * HAL rotations.
         */
        @NonNull
        static Rect getDispatchCropRect(@NonNull Rect surfaceCropRect, int surfaceToOutputDegrees,
                @NonNull Size dispatchResolution, int dispatchToOutputDegrees) {
            // There are 3 coordinate systems: surface, dispatch and output. Surface is where
            // the original crop rect is defined. We need to figure out what HAL
            // has done to the buffer (the surface->dispatch mapping) and apply the same
            // transformation to the crop rect.
            // The surface->dispatch mapping is calculated by inverting a dispatch->surface mapping.

            Matrix matrix = new Matrix();
            // Apply the dispatch->surface rotation.
            matrix.setRotate(dispatchToOutputDegrees - surfaceToOutputDegrees);
            // Apply the dispatch->surface translation. The translation is calculated by
            // compensating for the offset caused by the dispatch->surface rotation.
            float[] vertexes = sizeToVertexes(dispatchResolution);
            matrix.mapPoints(vertexes);
            float left = min(vertexes[0], vertexes[2], vertexes[4], vertexes[6]);
            float top = min(vertexes[1], vertexes[3], vertexes[5], vertexes[7]);
            matrix.postTranslate(-left, -top);
            // Inverting the dispatch->surface mapping to get the surface->dispatch mapping.
            matrix.invert(matrix);

            // Apply the surface->dispatch mapping to surface crop rect.
            RectF dispatchCropRectF = new RectF();
            matrix.mapRect(dispatchCropRectF, new RectF(surfaceCropRect));
            dispatchCropRectF.sort();
            Rect dispatchCropRect = new Rect();
            dispatchCropRectF.round(dispatchCropRect);
            return dispatchCropRect;
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
    @SuppressWarnings("ObjectToString")
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
         * @hide
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
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static Builder fromConfig(@NonNull ImageCaptureConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /**
         * {@inheritDoc}
         *
         * @hide
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
            // Error at runtime for using both setTargetResolution and setTargetAspectRatio on
            // the same config.
            if (getMutableConfig().retrieveOption(OPTION_TARGET_ASPECT_RATIO, null) != null
                    && getMutableConfig().retrieveOption(OPTION_TARGET_RESOLUTION, null) != null) {
                throw new IllegalArgumentException(
                        "Cannot use both setTargetResolution and setTargetAspectRatio on the same "
                                + "config.");
            }

            // Update the input format base on the other options set (mainly whether processing
            // is done)
            Integer bufferFormat = getMutableConfig().retrieveOption(OPTION_BUFFER_FORMAT, null);
            if (bufferFormat != null) {
                Preconditions.checkArgument(
                        getMutableConfig().retrieveOption(OPTION_CAPTURE_PROCESSOR, null) == null,
                        "Cannot set buffer format with CaptureProcessor defined.");
                getMutableConfig().insertOption(OPTION_INPUT_FORMAT, bufferFormat);
            } else {
                if (getMutableConfig().retrieveOption(OPTION_CAPTURE_PROCESSOR, null) != null) {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.YUV_420_888);
                } else {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, ImageFormat.JPEG);
                }
            }

            ImageCapture imageCapture = new ImageCapture(getUseCaseConfig());

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

            Preconditions.checkArgument(
                    getMutableConfig().retrieveOption(OPTION_MAX_CAPTURE_STAGES, MAX_IMAGES) >= 1,
                    "Maximum outstanding image count must be at least 1");

            Preconditions.checkNotNull(getMutableConfig().retrieveOption(OPTION_IO_EXECUTOR,
                    CameraXExecutors.ioExecutor()), "The IO executor can't be null");

            if (getMutableConfig().containsOption(OPTION_FLASH_MODE)) {
                int flashMode = getMutableConfig().retrieveOption(OPTION_FLASH_MODE);

                if (flashMode != FLASH_MODE_AUTO && flashMode != FLASH_MODE_ON
                        && flashMode != FLASH_MODE_OFF) {
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
         * Sets the {@link CaptureBundle}.
         *
         * @param captureBundle The requested capture bundle for extension.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCaptureBundle(@NonNull CaptureBundle captureBundle) {
            getMutableConfig().insertOption(OPTION_CAPTURE_BUNDLE, captureBundle);
            return this;
        }

        /**
         * Sets the {@link CaptureProcessor}.
         *
         * @param captureProcessor The requested capture processor for extension.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCaptureProcessor(@NonNull CaptureProcessor captureProcessor) {
            getMutableConfig().insertOption(OPTION_CAPTURE_PROCESSOR, captureProcessor);
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
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setBufferFormat(int bufferImageFormat) {
            getMutableConfig().insertOption(OPTION_BUFFER_FORMAT, bufferImageFormat);
            return this;
        }

        /**
         * Sets the max number of {@link CaptureStage}.
         *
         * @param maxCaptureStages The max CaptureStage number.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setMaxCaptureStages(int maxCaptureStages) {
            getMutableConfig().insertOption(OPTION_MAX_CAPTURE_STAGES, maxCaptureStages);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
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
         * <p>If not set, resolutions with aspect ratio 4:3 will be considered in higher
         * priority.
         *
         * @param aspectRatio The desired ImageCapture {@link AspectRatio}
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio) {
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
         * case.  See {@link androidx.camera.core.ImageCapture#setTargetRotation(int)} for
         * additional documentation.
         *
         * <p>If not set, the target rotation will default to the value of
         * {@link android.view.Display#getRotation()} of the default display at the time the use
         * case is created. The use case is fully created once it has been attached to a camera.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see androidx.camera.core.ImageCapture#setTargetRotation(int)
         * @see android.view.OrientationEventListener
         */
        @NonNull
        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
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
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a> section.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @hide
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(ImageOutputConfig.OPTION_DEFAULT_RESOLUTION,
                    resolution);
            return this;
        }

        /** @hide */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        /** @hide */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setImageReaderProxyProvider(
                @NonNull ImageReaderProxyProvider imageReaderProxyProvider) {
            getMutableConfig().insertOption(OPTION_IMAGE_READER_PROXY_PROVIDER,
                    imageReaderProxyProvider);
            return this;
        }

        /** @hide */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setSoftwareJpegEncoderRequested(boolean requestSoftwareJpeg) {
            getMutableConfig().insertOption(OPTION_USE_SOFTWARE_JPEG_ENCODER,
                    requestSoftwareJpeg);
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

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setUseCaseEventCallback(
                @NonNull UseCase.EventCallback useCaseEventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, useCaseEventCallback);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setAttachedUseCasesUpdateListener(
                @NonNull Consumer<Collection<UseCase>> attachedUseCasesUpdateListener) {
            getMutableConfig().insertOption(OPTION_ATTACHED_USE_CASES_UPDATE_LISTENER,
                    attachedUseCasesUpdateListener);
            return this;
        }
    }
}
