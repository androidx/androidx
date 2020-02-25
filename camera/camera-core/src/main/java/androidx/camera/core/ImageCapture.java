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
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_IO_EXECUTOR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_MAX_CAPTURE_STAGES;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ASPECT_RATIO_CUSTOM;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_RESOLUTION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_USE_CASE_EVENT_CALLBACK;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
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
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.IoConfig;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.utils.UseCaseConfigUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
@SuppressWarnings("ClassCanBeStatic") // TODO(b/141958189): Suppressed during upgrade to AGP 3.6.
public final class ImageCapture extends UseCase {

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
    private static final long CHECK_3A_TIMEOUT_IN_MS = 1000L;
    private static final int MAX_IMAGES = 2;
    // TODO(b/149336664) Move the quality to a compatibility class when there is a per device case.
    private static final byte JPEG_QUALITY_MAXIMIZE_QUALITY_MODE = 100;
    private static final byte JPEG_QUALITY_MINIMIZE_LATENCY_MODE = 95;

    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final TakePictureLock mTakePictureLock = new TakePictureLock(MAX_IMAGES, this);

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Deque<ImageCaptureRequest> mPendingImageCaptureRequests = new ConcurrentLinkedDeque<>();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SessionConfig.Builder mSessionConfigBuilder;
    private final CaptureConfig mCaptureConfig;
    private final ExecutorService mExecutor =
            Executors.newFixedThreadPool(
                    1,
                    new ThreadFactory() {
                        private final AtomicInteger mId = new AtomicInteger(0);

                        @Override
                        public Thread newThread(@NonNull Runnable r) {
                            return new Thread(
                                    r,
                                    CameraXThreads.TAG + "image_capture_" + mId.getAndIncrement());
                        }
                    });
    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mIoExecutor;
    private final CaptureCallbackChecker mSessionCallbackChecker = new CaptureCallbackChecker();
    @CaptureMode
    private final int mCaptureMode;

    /** The set of requests that will be sent to the camera for the final captured image. */
    private final CaptureBundle mCaptureBundle;
    private final int mMaxCaptureStages;

    /**
     * Processing that gets done to the mCaptureBundle to produce the final image that is produced
     * by {@link #takePicture(Executor, OnImageCapturedCallback)}
     */
    private final CaptureProcessor mCaptureProcessor;
    /** synthetic accessor */
    @SuppressWarnings("WeakerAccess")
    ImageReaderProxy mImageReader;
    /** Callback used to match the {@link ImageProxy} with the {@link ImageInfo}. */
    private CameraCaptureCallback mMetadataMatchingCaptureCallback;
    private ImageCaptureConfig mConfig;
    private DeferrableSurface mDeferrableSurface;

    private final ImageReaderProxy.OnImageAvailableListener mClosingListener = (imageReader -> {
        try (ImageProxy image = imageReader.acquireLatestImage()) {
            Log.d(TAG, "Discarding ImageProxy which was inadvertently acquired: " + image);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to acquire latest image.", e);
        }
    });

    /**
     * A flag to check 3A converged or not.
     *
     * <p>In order to speed up the taking picture process, trigger AF / AE should be skipped when
     * the flag is disabled. Set it to be enabled in the maximum quality mode and disabled in the
     * minimum latency mode.
     */
    private boolean mEnableCheck3AConverged;
    /** Current flash mode. */
    @FlashMode
    private int mFlashMode;

    /**
     * Creates a new image capture use case from the given configuration.
     *
     * @param userConfig for this use case instance
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    ImageCapture(@NonNull ImageCaptureConfig userConfig) {
        super(userConfig);
        // Ensure we're using the combined configuration (user config + defaults)
        mConfig = (ImageCaptureConfig) getUseCaseConfig();
        mCaptureMode = mConfig.getCaptureMode();
        mFlashMode = mConfig.getFlashMode();

        mCaptureProcessor = mConfig.getCaptureProcessor(null);
        mMaxCaptureStages = mConfig.getMaxCaptureStages(MAX_IMAGES);
        Preconditions.checkArgument(mMaxCaptureStages >= 1,
                "Maximum outstanding image count must be at least 1");

        Integer bufferFormat = mConfig.getBufferFormat(null);
        if (bufferFormat != null) {
            Preconditions.checkArgument(mCaptureProcessor == null,
                    "Cannot set buffer format with CaptureProcessor defined.");
            setImageFormat(bufferFormat);
        } else {
            if (mCaptureProcessor != null) {
                setImageFormat(ImageFormat.YUV_420_888);
            } else {
                setImageFormat(ImageReaderFormatRecommender.chooseCombo().imageCaptureFormat());
            }
        }

        mCaptureBundle = mConfig.getCaptureBundle(CaptureBundles.singleDefaultCaptureBundle());

        mIoExecutor =
                Preconditions.checkNotNull(mConfig.getIoExecutor(CameraXExecutors.ioExecutor()));

        if (mCaptureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
            mEnableCheck3AConverged = true; // check 3A convergence in MAX_QUALITY mode
        } else if (mCaptureMode == CAPTURE_MODE_MINIMIZE_LATENCY) {
            mEnableCheck3AConverged = false; // skip 3A convergence in MIN_LATENCY mode
        }

        CaptureConfig.Builder captureBuilder = CaptureConfig.Builder.createFrom(mConfig);
        mCaptureConfig = captureBuilder.build();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageCaptureConfig config, @NonNull Size resolution) {
        Threads.checkMainThread();
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);
        sessionConfigBuilder.addRepeatingCameraCaptureCallback(mSessionCallbackChecker);

        HandlerThread processingImageResultThread = new HandlerThread(
                "OnImageAvailableHandlerThread");
        processingImageResultThread.start();
        Handler processingImageResultHandler = new Handler(
                processingImageResultThread.getLooper());

        // Setup the ImageReader to do processing
        if (mCaptureProcessor != null) {
            // TODO: To allow user to use an Executor for the image processing.
            ProcessingImageReader processingImageReader =
                    new ProcessingImageReader(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            getImageFormat(), mMaxCaptureStages,
                            processingImageResultHandler,
                            getCaptureBundle(CaptureBundles.singleDefaultCaptureBundle()),
                            mCaptureProcessor);
            mMetadataMatchingCaptureCallback = processingImageReader.getCameraCaptureCallback();
            mImageReader = processingImageReader;
        } else {
            MetadataImageReader metadataImageReader = new MetadataImageReader(resolution.getWidth(),
                    resolution.getHeight(), getImageFormat(), MAX_IMAGES,
                    processingImageResultHandler);
            mMetadataMatchingCaptureCallback = metadataImageReader.getCameraCaptureCallback();
            mImageReader = metadataImageReader;
        }

        // By default close images that come from the listener.
        mImageReader.setOnImageAvailableListener(mClosingListener,
                CameraXExecutors.mainThreadExecutor());

        ImageReaderProxy imageReaderProxy = mImageReader;
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());
        mDeferrableSurface.getTerminationFuture().addListener(
                () -> {
                    imageReaderProxy.close();

                    // Close the handlerThread after the ImageReader was closed.
                    processingImageResultThread.quitSafely();
                }, CameraXExecutors.mainThreadExecutor());
        sessionConfigBuilder.addNonRepeatingSurface(mDeferrableSurface);

        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            clearPipeline();
            // Ensure the bound camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not bound
            //  to this use case so we don't need to do this check.
            if (isCurrentlyBoundCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                mSessionConfigBuilder = createPipeline(cameraId, config, resolution);
                attachToCamera(mSessionConfigBuilder.build());
                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    @SuppressWarnings("WeakerAccess")
    void clearPipeline() {
        Threads.checkMainThread();
        DeferrableSurface deferrableSurface = mDeferrableSurface;
        mDeferrableSurface = null;
        mImageReader = null;

        if (deferrableSurface != null) {
            deferrableSurface.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(@Nullable CameraInfo cameraInfo) {
        ImageCaptureConfig defaults = CameraX.getDefaultUseCaseConfig(ImageCaptureConfig.class,
                cameraInfo);
        if (defaults != null) {
            return Builder.fromConfig(defaults);
        }

        return null;
    }

    /**
     * Configures flash mode to CameraControlInternal once it is ready.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    protected void onCameraControlReady() {
        getCameraControl().setFlashMode(mFlashMode);
    }

    /**
     * Get the flash mode.
     *
     * @return the flashMode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON}, or
     * {@link #FLASH_MODE_OFF}.
     */
    @FlashMode
    public int getFlashMode() {
        return mFlashMode;
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
     * @param flashMode the flash mode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON},
     *                  or {@link #FLASH_MODE_OFF}.
     */
    public void setFlashMode(@FlashMode int flashMode) {
        this.mFlashMode = flashMode;
        // The camera control will be ready after the use case is attached. The {@link
        // CameraSelector} containing camera id info is also generated at meanwhile. Developers
        // may update flash mode before the use case is bound. If the camera control has been
        // ready, directly updating the flash mode into camera control. If the camera control has
        // been not ready yet, just saving the flash mode and updating into camera control when
        // camera control ready callback is called.
        if (getBoundCamera() != null) {
            getCameraControl().setFlashMode(flashMode);
        }
    }

    /**
     * Sets target cropping aspect ratio for output image.
     *
     * <p>This aspect ratio is in the coordinate frame after rotating the image by the target
     * rotation.
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
     * {@link ImageCapture#takePicture(OutputFileOptions, Executor, OnImageSavedCallback)}.
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
        ImageCaptureConfig oldConfig = (ImageCaptureConfig) getUseCaseConfig();
        Builder builder = Builder.fromConfig(oldConfig);
        Rational oldRatio = oldConfig.getTargetAspectRatioCustom(null);
        if (!aspectRatio.equals(oldRatio)) {
            builder.setTargetAspectRatioCustom(aspectRatio);
            updateUseCaseConfig(builder.getUseCaseConfig());
            mConfig = (ImageCaptureConfig) getUseCaseConfig();

            // TODO(b/122846516): Reconfigure capture session if the ratio is changed drastically.
        }
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
     * created.
     */
    @RotationValue
    public int getTargetRotation() {
        return ((ImageOutputConfig) getUseCaseConfig()).getTargetRotation();
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
     * a surface rotation, i.e. one of {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     *
     * <p>When this function is called, value set by
     * {@link ImageCapture.Builder#setTargetResolution(Size)} will be updated automatically to make
     * sure the suitable resolution can be selected when the use case is bound. Value set by
     * {@link ImageCapture#setCropAspectRatio(Rational)} will also be updated automatically to
     * make sure the output image is cropped into expected aspect ratio.
     *
     * <p>If no target rotation is set by the application, it is set to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is created.
     *
     * <p>takePicture uses the target rotation at the time it begins executing (which may be delayed
     * waiting on a previous takePicture call to complete).
     *
     * @param rotation Target rotation of the output image, expressed as one of
     *                 {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                 {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        ImageCaptureConfig oldConfig = (ImageCaptureConfig) getUseCaseConfig();
        Builder builder = Builder.fromConfig(oldConfig);
        int oldRotation = oldConfig.getTargetRotation(ImageOutputConfig.INVALID_ROTATION);
        if (oldRotation == ImageOutputConfig.INVALID_ROTATION || oldRotation != rotation) {
            UseCaseConfigUtil.updateTargetRotationAndRelatedConfigs(builder, rotation);
            updateUseCaseConfig(builder.getUseCaseConfig());
            mConfig = (ImageCaptureConfig) getUseCaseConfig();

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
     * <p>The callback will be called only once for every invocation of this method.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
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
                    public void onError(ImageSaver.SaveError error, String message,
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
    public void onStateOffline() {
        abortImageCaptureRequests();
    }

    private void abortImageCaptureRequests() {
        Throwable throwable = new CameraClosedException("Camera is closed.");
        for (ImageCaptureRequest captureRequest : mPendingImageCaptureRequests) {
            captureRequest.notifyCallbackError(
                    getError(throwable),
                    throwable.getMessage(), throwable);
        }
        mPendingImageCaptureRequests.clear();

        mTakePictureLock.cancelTakePicture(throwable);
    }

    @UiThread
    private void sendImageCaptureRequest(
            @Nullable Executor listenerExecutor, OnImageCapturedCallback callback) {

        // TODO(b/143734846): From here on, the image capture request should be
        //  self-contained and use this camera ID for everything. Currently the pre-capture
        //  sequence does not follow this approach and could fail if this use case is unbound
        //  or unbound to a different camera in the middle of pre-capture.
        CameraInternal boundCamera = getBoundCamera();
        if (boundCamera == null) {
            // Not bound. Notify callback.
            callback.onError(new ImageCaptureException(ERROR_INVALID_CAMERA,
                    "Not bound to a valid Camera [" + ImageCapture.this + "]", null));
            return;
        }

        CameraInfoInternal cameraInfoInternal = boundCamera.getCameraInfoInternal();
        int relativeRotation = cameraInfoInternal.getSensorRotationDegrees(
                mConfig.getTargetRotation(Surface.ROTATION_0));

        Rational targetRatio = mConfig.getTargetAspectRatioCustom(null);
        targetRatio = ImageUtil.rotate(targetRatio, relativeRotation);

        mPendingImageCaptureRequests.offer(
                new ImageCaptureRequest(relativeRotation, getJpegQuality(), targetRatio,
                        listenerExecutor, callback));

        issueImageCaptureRequests();
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

    /** Issues saved ImageCaptureRequest. */
    @UiThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void issueImageCaptureRequests() {
        ImageCaptureRequest imageCaptureRequest = mPendingImageCaptureRequests.poll();
        if (imageCaptureRequest == null) {
            return;
        }

        if (!takePictureInternal(imageCaptureRequest)) {
            // Was not able to successfully take picture. Retry again later when no current
            // picture being taken.
            Log.d(TAG, "Unable to issue take picture. Re-queuing image capture request");
            mPendingImageCaptureRequests.offerFirst(imageCaptureRequest);
        }
        Log.d(TAG, "Size of image capture request queue: " + mPendingImageCaptureRequests.size());

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
    private boolean takePictureInternal(@NonNull ImageCaptureRequest imageCaptureRequest) {
        if (!mTakePictureLock.lockTakePicture(imageCaptureRequest)) {
            // A ImageCaptureRequest in progress. Need to wait until current request completed
            // before issuing another one.
            return false;
        }

        mImageReader.setOnImageAvailableListener(
                (imageReader) -> {
                    ImageProxy image = mTakePictureLock.tryAcquireImage(imageReader,
                            imageCaptureRequest);
                    if (image != null) {
                        imageCaptureRequest.dispatchImage(image);
                    }
                    if (!mTakePictureLock.unlockTakePicture(imageCaptureRequest)) {
                        Log.d(TAG, "Error unlocking after dispatch");
                    }
                },
                CameraXExecutors.mainThreadExecutor());

        final TakePictureState state = new TakePictureState();

        FutureChain.from(preTakePicture(state))
                .transformAsync(v -> issueTakePicture(imageCaptureRequest), mExecutor)
                .addCallback(
                        new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                postTakePicture(state);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.e(TAG, "takePictureInternal onFailure", throwable);
                                postTakePicture(state);

                                // To handle the error and issue the next capture request
                                // when the capture stages have any fail.
                                CameraXExecutors.mainThreadExecutor().execute(() -> {
                                    imageCaptureRequest.notifyCallbackError(getError(throwable),
                                            (throwable != null) ? throwable.getMessage()
                                                    : "Unknown error", throwable);
                                    if (!mTakePictureLock.unlockTakePicture(imageCaptureRequest)) {
                                        Log.d(TAG, "Error unlocking wrong request");
                                    }
                                });
                            }
                        },
                        mExecutor);

        return true;
    }

    /**
     * A lock ensure that only one single {@link ImageCaptureRequest} is in progress at a time.
     */
    private static class TakePictureLock implements OnImageCloseListener {
        @GuardedBy("mLock")
        private ImageCaptureRequest mCurrentRequest = null;
        @GuardedBy("mLock")
        private int mOutstandingImages = 0;
        @GuardedBy("mLock")
        private final ImageCapture mImageCapture;

        private final int mMaxImages;
        private final Object mLock = new Object();

        TakePictureLock(int maxImages, ImageCapture imageCapture) {
            mMaxImages = maxImages;
            mImageCapture = imageCapture;
        }

        boolean lockTakePicture(ImageCaptureRequest imageCaptureRequest) {
            synchronized (mLock) {
                // Unable to lock if too many outstanding images or if there is currently a
                // request in flight
                if (mOutstandingImages >= mMaxImages || mCurrentRequest != null) {
                    return false;
                }

                mCurrentRequest = imageCaptureRequest;
                return true;
            }
        }

        @Nullable
        ImageProxy tryAcquireImage(ImageReaderProxy imageReaderProxy,
                ImageCaptureRequest imageCaptureRequest) {
            synchronized (mLock) {
                if (mCurrentRequest != imageCaptureRequest) {
                    Log.e(TAG, "Attempting to acquire image with incorrect request");
                    return null;
                }
                SingleCloseImageProxy imageProxy = null;
                try {
                    ImageProxy image = imageReaderProxy.acquireLatestImage();
                    if (image != null) {
                        imageProxy = new SingleCloseImageProxy(image);
                        imageProxy.addOnImageCloseListener(this);
                        mOutstandingImages++;
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to acquire latest image.", e);
                }

                return imageProxy;
            }
        }

        @Override
        public void onImageClose(ImageProxy image) {
            synchronized (mLock) {
                mOutstandingImages--;
                CameraXExecutors.mainThreadExecutor().execute(
                        mImageCapture::issueImageCaptureRequests);
            }
        }

        boolean unlockTakePicture(ImageCaptureRequest imageCaptureRequest) {
            synchronized (mLock) {
                if (mCurrentRequest != imageCaptureRequest) {
                    return false;
                }

                mCurrentRequest = null;
                CameraXExecutors.mainThreadExecutor().execute(
                        mImageCapture::issueImageCaptureRequests);
                return true;
            }
        }

        void cancelTakePicture(Throwable throwable) {
            synchronized (mLock) {
                if (mCurrentRequest != null) {
                    mCurrentRequest.notifyCallbackError(ImageCapture.getError(throwable),
                            throwable.getMessage(), throwable);
                }
                mCurrentRequest = null;
            }
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
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void clear() {
        clearPipeline();
        mExecutor.shutdown();
        super.clear();
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
        mSessionConfigBuilder = createPipeline(getBoundCameraId(), mConfig, suggestedResolution);

        attachToCamera(mSessionConfigBuilder.build());

        // In order to speed up the take picture process, notifyActive at an early stage to
        // attach the session capture callback to repeating and get capture result all the time.
        notifyActive();

        return suggestedResolution;
    }

    final OnImageCloseListener mOnImageCloseListener = new OnImageCloseListener() {
        /**
         * {@inheritDoc}
         *
         * <p>Issues next image capture request when dispatched image is closed, which can ensure
         * the image buffer in ImageReader is always available.
         */
        @Override
        public void onImageClose(ImageProxy image) {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                CameraXExecutors.mainThreadExecutor().execute(() -> onImageClose(image));
                return;
            }
            issueImageCaptureRequests();
        }
    };

    /**
     * Routine before taking picture.
     *
     * <p>For example, trigger 3A scan, open torch and check 3A converged if necessary.
     */
    private ListenableFuture<Void> preTakePicture(final TakePictureState state) {
        return FutureChain.from(getPreCaptureStateIfNeeded())
                .transformAsync(captureResult -> {
                    state.mPreCaptureState = captureResult;
                    triggerAfIfNeeded(state);

                    if (isFlashRequired(state)) {
                        state.mIsFlashTriggered = true;
                        triggerAePrecapture(state);
                    }
                    return check3AConverged(state);
                }, mExecutor)
                // Ignore the 3A convergence result.
                .transform(is3AConverged -> null, mExecutor);
    }

    /**
     * Routine after picture was taken.
     *
     * <p>For example, cancel 3A scan, close torch if necessary.
     */
    void postTakePicture(final TakePictureState state) {
        mExecutor.execute(() -> ImageCapture.this.cancelAfAeTrigger(state));
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
                            return captureResult;
                        }
                    });
        }
        return Futures.immediateFuture(null);
    }

    boolean isFlashRequired(TakePictureState state) {
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
        // Besides enableCheck3AConverged == true (MAX_QUALITY), if flash is triggered we also need
        // to
        // wait for 3A convergence.
        if (!mEnableCheck3AConverged && !state.mIsFlashTriggered) {
            return Futures.immediateFuture(false);
        }

        // if current capture result shows 3A is converged, no need to check upcoming capture
        // result.
        if (is3AConverged(state.mPreCaptureState)) {
            return Futures.immediateFuture(true);
        }

        return mSessionCallbackChecker.checkCaptureResult(
                new CaptureCallbackChecker.CaptureResultChecker<Boolean>() {
                    @Override
                    public Boolean check(@NonNull CameraCaptureResult captureResult) {
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
        state.mIsAfTriggered = true;
        getCameraControl().triggerAf();
    }

    /** Issues a request to start auto exposure scan. */
    void triggerAePrecapture(TakePictureState state) {
        state.mIsAePrecaptureTriggered = true;
        getCameraControl().triggerAePrecapture();
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
        final List<ListenableFuture<Void>> futureList = new ArrayList<>();
        final List<CaptureConfig> captureConfigs = new ArrayList<>();

        CaptureBundle captureBundle;
        if (mCaptureProcessor != null) {
            // If the Processor is provided, check if we have valid CaptureBundle and update
            // ProcessingImageReader before actually issuing a take picture request.
            captureBundle = getCaptureBundle(null);

            if (captureBundle == null) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "ImageCapture cannot set empty CaptureBundle."));
            }

            if (captureBundle.getCaptureStages().size() > mMaxCaptureStages) {
                return Futures.immediateFailedFuture(new IllegalArgumentException(
                        "ImageCapture has CaptureStages > Max CaptureStage size"));
            }

            ((ProcessingImageReader) mImageReader).setCaptureBundle(captureBundle);
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
            builder.addImplementationOption(CaptureConfig.OPTION_ROTATION,
                    imageCaptureRequest.mRotationDegrees);
            builder.addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY,
                    imageCaptureRequest.mJpegQuality);

            // Add the implementation options required by the CaptureStage
            builder.addImplementationOptions(
                    captureStage.getCaptureConfig().getImplementationOptions());
            builder.setTag(captureStage.getCaptureConfig().getTag());
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
    @IntDef({FLASH_MODE_AUTO, FLASH_MODE_ON, FLASH_MODE_OFF})
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
         * rotation applied.  rotationDegrees describes the magnitude of clockwise rotation, which
         * if applied to the image will make it match the currently configured target rotation.
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
            image.close();
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
        @CaptureMode
        private static final int DEFAULT_CAPTURE_MODE = CAPTURE_MODE_MINIMIZE_LATENCY;
        @FlashMode
        private static final int DEFAULT_FLASH_MODE = FLASH_MODE_OFF;
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 4;

        private static final ImageCaptureConfig DEFAULT_CONFIG;

        static {
            Builder builder =
                    new Builder()
                            .setCaptureMode(DEFAULT_CAPTURE_MODE)
                            .setFlashMode(DEFAULT_FLASH_MODE)
                            .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY);

            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public ImageCaptureConfig getConfig(@Nullable CameraInfo cameraInfo) {
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

        // Empty metadata object used as a placeholder for no user-supplied metadata.
        // Should be initialized to all default values.
        private static final Metadata EMPTY_METADATA = new Metadata();

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
            mMetadata = metadata == null ? EMPTY_METADATA : metadata;
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

        @NonNull
        Metadata getMetadata() {
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
             * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
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
         * Sets left-right mirroring of the capture.
         *
         * @param isReversedHorizontal true if the capture is left-right mirrored.
         */
        public void setReversedHorizontal(boolean isReversedHorizontal) {
            mIsReversedHorizontal = isReversedHorizontal;
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
        boolean mIsAfTriggered = false;
        boolean mIsAePrecaptureTriggered = false;
        boolean mIsFlashTriggered = false;
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

    private static final class ImageCaptureRequest {
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

        ImageCaptureRequest(
                @RotationValue int rotationDegrees,
                @IntRange(from = 1, to = 100)
                int jpegQuality,
                Rational targetRatio,
                @NonNull Executor executor,
                @NonNull OnImageCapturedCallback callback) {
            mRotationDegrees = rotationDegrees;
            mJpegQuality = jpegQuality;
            mTargetRatio = targetRatio;
            mListenerExecutor = executor;
            mCallback = callback;
        }

        void dispatchImage(final ImageProxy image) {
            // Check to make sure image hasn't been already dispatched or error has been notified
            if (!mDispatched.compareAndSet(false, true)) {
                return;
            }

            try {
                mListenerExecutor.execute(() -> {
                    Size sourceSize = new Size(image.getWidth(), image.getHeight());
                    if (ImageUtil.isAspectRatioValid(sourceSize, mTargetRatio)) {
                        image.setCropRect(
                                ImageUtil.computeCropRectFromAspectRatio(sourceSize,
                                        mTargetRatio));
                    }

                    ImageInfo imageInfo = ImmutableImageInfo.create(image.getImageInfo().getTag(),
                            image.getImageInfo().getTimestamp(), mRotationDegrees);

                    mCallback.onCaptureSuccess(new SettableImageProxy(image, imageInfo));
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Unable to post to the supplied executor.");

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
                Log.e(TAG, "Unable to post to the supplied executor.");
            }
        }
    }

    /** Builder for an {@link ImageCapture}. */
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
        public static Builder fromConfig(@NonNull ImageCaptureConfig configuration) {
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
            return new ImageCapture(getUseCaseConfig());
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
         * <p>This is the ratio of the target's width to the image's height, where the numerator of
         * the provided {@link Rational} corresponds to the width, and the denominator corresponds
         * to the height.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution.
         *
         * <p>This method can be used to request an aspect ratio that is not from the standard set
         * of aspect ratios defined in the {@link AspectRatio}.
         *
         * <p>This method will remove any value set by setTargetAspectRatio().
         *
         * <p>setTargetAspectRatioCustom will crop the file outputs from takePicture methods
         * as opposed to {@link #setTargetAspectRatio(int)} which does not apply a crop.  The source
         * (pre-crop) resolution will an automatically selected resolution suitable for still
         * images.
         *
         * <p>For ImageCapture, the outputs are the {@link ImageProxy} or the File passed to image
         * capture listeners.
         *
         * @param aspectRatio A {@link Rational} representing the ratio of the target's width and
         *                    height.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setTargetAspectRatioCustom(@NonNull Rational aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM, aspectRatio);
            getMutableConfig().removeOption(OPTION_TARGET_ASPECT_RATIO);
            return this;
        }

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>The aspect ratio is the ratio of width to height in the sensor orientation.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case.  Attempting so will throw an IllegalArgumentException when building the
         * Config.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution.
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
         * This will affect the EXIF rotation metadata in images saved by takePicture calls and the
         * {@link ImageInfo#getRotationDegrees()} value of the {@link ImageProxy} returned by
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
         * case is created.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see androidx.camera.core.ImageCapture#setTargetRotation(int)
         * * @see android.view.OrientationEventListener
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
         * use case.  Attempting so will throw an IllegalArgumentException when building the
         * Config.
         *
         * <p>The resolution {@link Size} should be expressed at the use cases's target rotation.
         * For example, a device with portrait natural orientation in natural target rotation
         * requesting a portrait image may specify 480x640, and the same device, rotated 90 degrees
         * and targeting landscape orientation may specify 640x480.
         *
         * <p>When the target resolution is set,
         * {@link ImageCapture.Builder#setCropAspectRatio(Rational)} will be automatically called
         * to set corresponding value. Such that the output image will be cropped into the
         * desired aspect ratio.
         *
         * <p>The maximum available resolution that could be selected for an {@link ImageCapture}
         * will depend on the camera device's capability.
         *
         * <p>If not set, the largest available resolution will be selected to use. Usually,
         * users will intend to get the largest still image that the camera device can support.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION, resolution);
            if (resolution != null) {
                getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM,
                        new Rational(resolution.getWidth(), resolution.getHeight()));
            }
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
    }
}
