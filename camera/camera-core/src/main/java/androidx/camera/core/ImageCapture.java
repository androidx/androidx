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

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.JPEG_R;
import static android.graphics.ImageFormat.RAW_SENSOR;

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
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_OUTPUT_FORMAT;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_POSTVIEW_ENABLED;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_POSTVIEW_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SCREEN_FLASH;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_RESOLUTION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_SECONDARY_INPUT_FORMAT;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
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
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.imagecapture.ImageCaptureControl;
import androidx.camera.core.imagecapture.ImagePipeline;
import androidx.camera.core.imagecapture.TakePictureManager;
import androidx.camera.core.imagecapture.TakePictureRequest;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.ImageInputConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.IoConfig;
import androidx.camera.core.internal.ScreenFlashWrapper;
import androidx.camera.core.internal.SupportedOutputSizesSorter;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
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

    private static final int FLASH_MODE_UNKNOWN = -1;
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
     * Screen flash. Display screen brightness will be used as alternative to flash when taking
     * a picture with front camera.
     *
     * <p> This flash mode can be set via {@link #setFlashMode(int)} after setting a non-null
     * {@link ScreenFlash} instance with {@link #setScreenFlash(ScreenFlash)}.
     * This mode will always invoke all the necessary operations for a screen flash image capture,
     * i.e. it is similar to {@link #FLASH_MODE_ON}, not {@link #FLASH_MODE_AUTO}.
     *
     * <p> The following code snippet shows an example implementation of how this flash mode can be
     * set to an {@link ImageCapture} instance.
     * <pre>{@code
     * imageCapture.setScreenFlash(new ImageCapture.ScreenFlash() {
     *     @Override
     *     public void apply(long expirationTimeMillis,
     *             @NonNull ScreenFlashListener screenFlashListener) {
     *         whiteColorOverlayView.setVisibility(View.VISIBLE);
     *         maximizeScreenBrightness();
     *         screenFlashListener.onCompleted();
     *     }
     *
     *     @Override
     *     public void clear() {
     *         restoreScreenBrightness();
     *         whiteColorOverlayView.setVisibility(View.INVISIBLE);
     *     }
     * });
     *
     * imageCapture.setFlashMode(ImageCapture.FLASH_MODE_SCREEN);
     * }</pre>
     *
     * @see #setFlashMode(int)
     */
    public static final int FLASH_MODE_SCREEN = 3;

    /** The timeout in seconds within which screen flash UI changes have to be completed. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final long SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS = 3;

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
     * Captures 8-bit standard dynamic range (SDR) images using the {@link ImageFormat#JPEG}
     * image format.
     */
    @ExperimentalImageCaptureOutputFormat
    public static final int OUTPUT_FORMAT_JPEG = 0;

    /**
     * Captures Ultra HDR compressed images using the {@link ImageFormat#JPEG_R} image format.
     *
     * <p>This format is backward compatible with SDR JPEG images and supports HDR rendering of
     * content. This means that on older apps or devices, images appear seamlessly as regular JPEG;
     * on apps and devices that have been updated to fully support the format, images appear as HDR.
     *
     * <p>For more information see
     * <a href="https://developer.android.com/media/grow/ultra-hdr">Support Ultra HDR</a>.
     */
    @ExperimentalImageCaptureOutputFormat
    public static final int OUTPUT_FORMAT_JPEG_ULTRA_HDR = 1;

    /**
     * Captures raw images in the {@link ImageFormat#RAW_SENSOR} image format.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int OUTPUT_FORMAT_RAW = 2;

    /**
     * Captures raw images in the {@link ImageFormat#RAW_SENSOR} and {@link ImageFormat#JPEG}
     * image formats.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int OUTPUT_FORMAT_RAW_JPEG = 3;

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
    @NonNull
    private ScreenFlashWrapper mScreenFlashWrapper;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            SessionConfig.Builder mSessionConfigBuilder;

    @Nullable
    private ImagePipeline mImagePipeline;
    @Nullable
    private TakePictureManager mTakePictureManager;
    @Nullable
    private SessionConfig.CloseableErrorListener mCloseableErrorListener;

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
        mScreenFlashWrapper = ScreenFlashWrapper.from(useCaseConfig.getScreenFlash());
    }

    private boolean isSessionProcessorEnabledInCurrentCamera() {
        if (getCamera() == null) {
            return false;
        }

        CameraConfig cameraConfig = getCamera().getExtendedConfig();
        return cameraConfig.getSessionProcessor(null) != null;
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
                DEFAULT_CONFIG.getConfig().getCaptureType(),
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
                            && bufferFormat != JPEG),
                    "Cannot set non-JPEG buffer format with Extensions enabled.");
            builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                    useSoftwareJpeg ? ImageFormat.YUV_420_888 : bufferFormat);
        } else {
            if (isOutputFormatRaw(builder.getMutableConfig())) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, RAW_SENSOR);
            } else if (isOutputFormatRawJpeg(builder.getMutableConfig())) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, RAW_SENSOR);
                builder.getMutableConfig().insertOption(OPTION_SECONDARY_INPUT_FORMAT, JPEG);
            } else if (isOutputFormatUltraHdr(builder.getMutableConfig())) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, JPEG_R);
                builder.getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE,
                        DynamicRange.UNSPECIFIED);
            } else if (useSoftwareJpeg) {
                builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                        ImageFormat.YUV_420_888);
            } else {
                List<Pair<Integer, Size[]>> supportedSizes =
                        builder.getMutableConfig().retrieveOption(OPTION_SUPPORTED_RESOLUTIONS,
                                null);
                if (supportedSizes == null) {
                    builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT, JPEG);
                } else {
                    // Use Jpeg first if supported.
                    if (isImageFormatSupported(supportedSizes, JPEG)) {
                        builder.getMutableConfig().insertOption(OPTION_INPUT_FORMAT,
                                JPEG);
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

    @OptIn(markerClass = ExperimentalImageCaptureOutputFormat.class)
    private static boolean isOutputFormatUltraHdr(@NonNull MutableConfig config) {
        return Objects.equals(config.retrieveOption(OPTION_OUTPUT_FORMAT, null),
                OUTPUT_FORMAT_JPEG_ULTRA_HDR);
    }

    private static boolean isOutputFormatRaw(@NonNull MutableConfig config) {
        return Objects.equals(config.retrieveOption(OPTION_OUTPUT_FORMAT, null),
                OUTPUT_FORMAT_RAW);
    }

    private static boolean isOutputFormatRawJpeg(@NonNull MutableConfig config) {
        return Objects.equals(config.retrieveOption(OPTION_OUTPUT_FORMAT, null),
                OUTPUT_FORMAT_RAW_JPEG);
    }

    /**
     * Configures flash mode to CameraControlInternal once it is ready.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onCameraControlReady() {
        trySetFlashModeToCameraControl();
        setScreenFlashToCameraControl();
    }

    private @CameraSelector.LensFacing int getCameraLens() {
        Camera camera = getCamera();
        if (camera != null) {
            return camera.getCameraInfo().getLensFacing();
        }
        return CameraSelector.LENS_FACING_UNKNOWN;
    }

    /**
     * Get the flash mode.
     *
     * @return the flashMode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON},
     * {@link #FLASH_MODE_SCREEN}, or {@link #FLASH_MODE_OFF}.
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
     * necessary. If there is no flash unit and {@code flashMode} is not {@link #FLASH_MODE_SCREEN},
     * then calling this API will take no effect for the subsequent photo capture requests and
     * they will act like {@link #FLASH_MODE_OFF}.
     *
     * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
     * will remain enabled during photo capture regardless of flashMode setting. When
     * the torch is disabled, flash will function as specified by {@code setFlashMode(int)}.
     *
     * <p>On some LEGACY devices like Samsung A3, taking pictures with {@link #FLASH_MODE_AUTO}
     * mode could cause a crash. To workaround this CameraX will disable the auto flash behavior
     * internally on devices that have this issue.
     *
     * <p>If {@link #FLASH_MODE_SCREEN} is set, a {@link ScreenFlash} implementation
     * must be set via {@link #setScreenFlash(ScreenFlash)} before calling this
     * API. Trying to use {@link #FLASH_MODE_SCREEN} without a {@code ScreenFlash} instance set or
     * with a non-front camera will result in an {@link IllegalArgumentException}. It is the
     * application's responsibility to change flashMode while switching the camera in case it
     * leads to a non-supported case (e.g. switching to rear camera while FLASH_MODE_SCREEN is
     * still on).
     *
     * @param flashMode the flash mode. Value is {@link #FLASH_MODE_AUTO}, {@link #FLASH_MODE_ON},
     *                  {@link #FLASH_MODE_SCREEN} or {@link #FLASH_MODE_OFF}.
     *
     * @throws IllegalArgumentException If flash mode is invalid or FLASH_MODE_SCREEN is used
     *                                  without a {@code ScreenFlash} instance or front camera.
     */
    public void setFlashMode(@FlashMode int flashMode) {
        if (flashMode != FLASH_MODE_AUTO && flashMode != FLASH_MODE_ON
                && flashMode != FLASH_MODE_OFF) {
            if (flashMode == FLASH_MODE_SCREEN) {
                if (mScreenFlashWrapper.getBaseScreenFlash() == null) {
                    throw new IllegalArgumentException("ScreenFlash not set for FLASH_MODE_SCREEN");
                }

                if (getCamera() != null && getCameraLens() != CameraSelector.LENS_FACING_FRONT) {
                    throw new IllegalArgumentException(
                            "Not a front camera despite setting FLASH_MODE_SCREEN");
                }
            } else {
                throw new IllegalArgumentException("Invalid flash mode: " + flashMode);
            }
        }

        synchronized (mLockedFlashMode) {
            mFlashMode = flashMode;
            trySetFlashModeToCameraControl();
        }
    }

    /**
     * Sets {@link ScreenFlash} for subsequent photo capture requests.
     *
     * <p>The calling of this API will take effect for {@link #FLASH_MODE_SCREEN} only
     * and the {@code screenFlash} instance will be ignored for other flash modes.
     *
     * <p>If the implementation provided by the user is no longer valid (e.g. due to any
     * {@link android.app.Activity} or {@link android.view.View} reference used in the
     * implementation becoming invalid), user needs to re-set a new valid {@code ScreenFlash} or
     * clear the previous one with {@code setScreenFlash(null)}, whichever appropriate.
     *
     * @param screenFlash A {@link ScreenFlash} implementation that is used to
     *                             notify API users when app side changes need to be done. This
     *                             will replace the previous {@code ScreenFlash} instance set
     *                             with this method.
     */
    public void setScreenFlash(@Nullable ScreenFlash screenFlash) {
        mScreenFlashWrapper = ScreenFlashWrapper.from(screenFlash);
        setScreenFlashToCameraControl();
    }

    /**
     * Returns the {@link ScreenFlash} instance currently set, null if none.
     */
    @Nullable
    public ScreenFlash getScreenFlash() {
        return mScreenFlashWrapper.getBaseScreenFlash();
    }

    private void setScreenFlashToCameraControl() {
        setScreenFlashToCameraControl(mScreenFlashWrapper);
    }

    private void setScreenFlashToCameraControl(@Nullable ImageCapture.ScreenFlash screenFlash) {
        getCameraControl().setScreenFlash(screenFlash);
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
     * {@link ImageCapture#setTargetRotation(int)}. The rotation of an image taken is
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
     * reverse-portrait display orientation. In these cases, set target rotation dynamically
     * according to the {@link android.view.OrientationEventListener}, without re-creating the
     * use case. {@link UseCase#snapToSurfaceRotation(int)} is a helper function to convert the
     * orientation of the {@link android.view.OrientationEventListener} to a rotation value.
     * See {@link UseCase#snapToSurfaceRotation(int)} for more information and sample code.
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
    public ResolutionInfo getResolutionInfo() {
        return getResolutionInfoInternal();
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

        return new ResolutionInfo(resolution, requireNonNull(cropRect), rotationDegrees);
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
     * Returns the output format setting.
     *
     * <p>If the output format was not provided to
     * {@link ImageCapture.Builder#setOutputFormat(int)}, this will return the default of
     * {@link #OUTPUT_FORMAT_JPEG}.
     *
     * @return the output format set for this {@code ImageCapture} use case.
     *
     * @see ImageCapture.Builder#setOutputFormat(int)
     */
    @ExperimentalImageCaptureOutputFormat
    @OutputFormat
    public int getOutputFormat() {
        return checkNotNull(getCurrentConfig().retrieveOption(OPTION_OUTPUT_FORMAT,
                Defaults.DEFAULT_OUTPUT_FORMAT));
    }

    /**
     * Captures a new still image for in memory access.
     *
     * <p>The listener is responsible for calling {@link Image#close()} on the returned image.
     *
     * @param executor The executor in which the callback methods will be run.
     * @param callback Callback to be invoked for the newly captured image.
     *
     * @throws IllegalArgumentException If {@link ImageCapture#FLASH_MODE_SCREEN} is used without a
     *                                  non-null {@code ScreenFlash} instance set.
     */
    public void takePicture(@NonNull Executor executor,
            final @NonNull OnImageCapturedCallback callback) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            CameraXExecutors.mainThreadExecutor().execute(() -> takePicture(executor, callback));
            return;
        }

        takePictureInternal(executor, callback, /*onDiskCallback=*/null,
                /*outputFileOptions=*/null);
    }

    /**
     * Captures a new still image and saves to a file along with application specified metadata.
     *
     * <p> If the {@link ImageCapture} is in a {@link UseCaseGroup} where {@link ViewPort} is
     * set, or {@link #setCropAspectRatio} is used, the image may be cropped before saving to
     * disk which causes an additional latency.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     *
     * @throws IllegalArgumentException If {@link ImageCapture#FLASH_MODE_SCREEN} is used without a
     *                                  a non-null {@code ScreenFlash} instance set.
     * @see ViewPort
     */
    public void takePicture(
            final @NonNull OutputFileOptions outputFileOptions,
            final @NonNull Executor executor,
            final @NonNull OnImageSavedCallback imageSavedCallback) {
        takePicture(List.of(outputFileOptions), executor, imageSavedCallback);
    }

    /**
     * Captures two still images simultaneously and saves to a file along with application
     * specified metadata.
     *
     * <p>Currently only {@link #OUTPUT_FORMAT_RAW_JPEG} is supporting simultaneous image capture.
     *
     * @param outputFileOptions  List of options to store the newly captured images.
     * @param executor           The executor in which the callback methods will be run.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     *
     * @throws IllegalArgumentException If {@link ImageCapture#FLASH_MODE_SCREEN} is used without a
     *                                  a non-null {@code ScreenFlash} instance set.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void takePicture(
            final @NonNull List<OutputFileOptions> outputFileOptions,
            final @NonNull Executor executor,
            final @NonNull OnImageSavedCallback imageSavedCallback) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            CameraXExecutors.mainThreadExecutor().execute(
                    () -> takePicture(outputFileOptions,
                            executor, imageSavedCallback));
            return;
        }
        takePictureInternal(executor, /*inMemoryCallback=*/null, imageSavedCallback,
                outputFileOptions);
    }

    /**
     * Returns {@link ImageCaptureCapabilities} to query ImageCapture capability of the given
     * {@link CameraInfo}.
     *
     * <p>Some capabilities are only exposed on Extensions-enabled cameras. To get the correct
     * capabilities when Extensions are enabled, you need to pass the {@link CameraInfo} from the
     * Extensions-enabled {@link Camera} instance. To do this, use the {@link CameraSelector}
     * instance retrieved from
     * {@link androidx.camera.extensions.ExtensionsManager#getExtensionEnabledCameraSelector(CameraSelector, int)}
     * to invoke {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle} where
     * you can skip use cases arguments if you'd like to query it before opening the camera. Then,
     * use the returned {@link Camera} to get the {@link CameraInfo} instance.
     *
     * <p>>The following code snippet demonstrates how to enable postview:
     *
     * <pre>{@code
     * CameraSelector extensionCameraSelector =
     *     extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.NIGHT);
     * Camera camera = cameraProvider.bindToLifecycle(activity, extensionCameraSelector);
     * ImageCaptureCapabilities capabilities =
     *     ImageCapture.getImageCaptureCapabilities(camera.getCameraInfo());
     * ImageCapture imageCapture = new ImageCapture.Builder()
     *     .setPostviewEnabled(capabilities.isPostviewSupported())
     *     .build();
     * }}</pre>
     *
     * @return {@link ImageCaptureCapabilities}
     */
    @NonNull
    public static ImageCaptureCapabilities getImageCaptureCapabilities(
            @NonNull CameraInfo cameraInfo) {
        return new ImageCaptureCapabilitiesImpl(cameraInfo);
    }

    private static class ImageCaptureCapabilitiesImpl implements ImageCaptureCapabilities {
        private final CameraInfo mCameraInfo;
        ImageCaptureCapabilitiesImpl(@NonNull CameraInfo cameraInfo) {
            mCameraInfo = cameraInfo;
        }

        @Override
        public boolean isPostviewSupported() {
            if (mCameraInfo instanceof CameraInfoInternal) {
                return ((CameraInfoInternal) mCameraInfo).isPostviewSupported();
            }
            return false;
        }

        @Override
        public boolean isCaptureProcessProgressSupported() {
            if (mCameraInfo instanceof CameraInfoInternal) {
                return ((CameraInfoInternal) mCameraInfo).isCaptureProcessProgressSupported();
            }
            return false;
        }

        @ExperimentalImageCaptureOutputFormat
        @NonNull
        @Override
        public Set<@OutputFormat Integer> getSupportedOutputFormats() {
            Set<Integer> formats = new HashSet<>();
            formats.add(OUTPUT_FORMAT_JPEG);
            if (isUltraHdrSupported()) {
                formats.add(OUTPUT_FORMAT_JPEG_ULTRA_HDR);
            }

            if (isRawSupported()) {
                formats.add(OUTPUT_FORMAT_RAW);
                formats.add(OUTPUT_FORMAT_RAW_JPEG);
            }

            return formats;
        }

        private boolean isUltraHdrSupported() {
            if (mCameraInfo instanceof CameraInfoInternal) {
                CameraInfoInternal cameraInfoInternal = (CameraInfoInternal) mCameraInfo;
                return cameraInfoInternal.getSupportedOutputFormats().contains(JPEG_R);
            }

            return false;
        }

        private boolean isRawSupported() {
            if (mCameraInfo instanceof CameraInfoInternal) {
                CameraInfoInternal cameraInfoInternal = (CameraInfoInternal) mCameraInfo;
                return cameraInfoInternal.getSupportedOutputFormats().contains(RAW_SENSOR);
            }

            return false;
        }
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
        // Camera2CapturePipeline ScreenFlash#clear event may come a bit later due to
        // thread-hopping or listener invocation delay. When all requests are aborted anyway, we can
        // complete all pending tasks earlier and ignore any that comes from user/camera-camera2.
        mScreenFlashWrapper.completePendingTasks();

        if (mTakePictureManager != null) {
            mTakePictureManager.abortRequests();
        }
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
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
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
        if (Boolean.TRUE.equals(
                mutableConfig.retrieveOption(OPTION_USE_SOFTWARE_JPEG_ENCODER, false))) {
            boolean supported = true;
            if (isSessionProcessorEnabledInCurrentCamera()) {
                // SessionProcessor requires JPEG input format so it is incompatible with SW Jpeg.
                Logger.w(TAG, "Software JPEG cannot be used with Extensions.");
                supported = false;
            }
            Integer bufferFormat = mutableConfig.retrieveOption(OPTION_BUFFER_FORMAT, null);
            if (bufferFormat != null && bufferFormat != JPEG) {
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
        setScreenFlashToCameraControl(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onBind() {
        CameraInternal camera = getCamera();
        checkNotNull(camera, "Attached camera cannot be null");

        if (getFlashMode() == FLASH_MODE_SCREEN
                && getCameraLens() != CameraSelector.LENS_FACING_FRONT) {
            throw new IllegalArgumentException(
                    "Not a front camera despite setting FLASH_MODE_SCREEN in ImageCapture");
        }
    }

    @Nullable
    private SessionProcessor getSessionProcessor() {
        CameraConfig cameraConfig = getCamera().getExtendedConfig();
        return cameraConfig.getSessionProcessor(null);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecUpdated(
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        mSessionConfigBuilder = createPipeline(getCameraId(),
                (ImageCaptureConfig) getCurrentConfig(), primaryStreamSpec);

        updateSessionConfig(List.of(mSessionConfigBuilder.build()));

        // In order to speed up the take picture process, notifyActive at an early stage to
        // attach the session capture callback to repeating and get capture result all the time.
        notifyActive();
        return primaryStreamSpec;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected StreamSpec onSuggestedStreamSpecImplementationOptionsUpdated(@NonNull Config config) {
        mSessionConfigBuilder.addImplementationOptions(config);
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
        return getAttachedStreamSpec().toBuilder().setImplementationOptions(config).build();
    }

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
     * Creates the pipeline for both capture request configuration and image post-processing.
     */
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    @MainThread
    private SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageCaptureConfig config, @NonNull StreamSpec streamSpec) {
        checkMainThread();
        Log.d(TAG, String.format("createPipeline(cameraId: %s, streamSpec: %s)",
                cameraId, streamSpec));
        Size resolution = streamSpec.getResolution();
        boolean isVirtualCamera = !requireNonNull(getCamera()).getHasTransform();
        if (mImagePipeline != null) {
            checkState(isVirtualCamera);
            // On LEGACY devices, when the app is backgrounded, it will trigger StreamSharing's
            // SessionConfig error callback and recreate children pipeline.
            mImagePipeline.close();
        }

        boolean isPostviewEnabled =
                getCurrentConfig().retrieveOption(OPTION_POSTVIEW_ENABLED, false);
        Size postViewSize = null;
        int postviewFormat = ImageFormat.YUV_420_888;

        if (isPostviewEnabled) {
            SessionProcessor sessionProcessor = getSessionProcessor();
            if (sessionProcessor != null) {
                ResolutionSelector postviewSizeSelector =
                        getCurrentConfig().retrieveOption(OPTION_POSTVIEW_RESOLUTION_SELECTOR,
                                null);
                Map<Integer, List<Size>> map =
                        sessionProcessor.getSupportedPostviewSize(resolution);
                // Prefer YUV because it takes less time to decode to bitmap.
                List<Size> sizes = map.get(ImageFormat.YUV_420_888);
                if (sizes == null || sizes.isEmpty()) {
                    sizes = map.get(JPEG);
                    postviewFormat = JPEG;
                }

                if (sizes != null && !sizes.isEmpty()) {
                    if (postviewSizeSelector != null) {
                        Collections.sort(sizes, new CompareSizesByArea(true));
                        CameraInternal camera = getCamera();
                        Rect sensorRect = camera.getCameraControlInternal().getSensorRect();
                        CameraInfoInternal cameraInfo = camera.getCameraInfoInternal();
                        Rational fullFov = new Rational(sensorRect.width(), sensorRect.height());
                        List<Size> result =
                                SupportedOutputSizesSorter
                                        .sortSupportedOutputSizesByResolutionSelector(
                                                postviewSizeSelector,
                                                sizes,
                                                null,
                                                getTargetRotation(),
                                                fullFov,
                                                cameraInfo.getSensorRotationDegrees(),
                                                cameraInfo.getLensFacing());
                        if (result.isEmpty()) {
                            throw new IllegalArgumentException("The postview ResolutionSelector "
                                    + "cannot select a valid size for the postview.");
                        }
                        postViewSize = result.get(0);
                    } else {
                        postViewSize = Collections.max(sizes, new CompareSizesByArea());
                    }
                }
            }
        }

        CameraCharacteristics cameraCharacteristics = null;
        if (getCamera() != null) {
            try {
                Object obj = getCamera().getCameraInfoInternal().getCameraCharacteristics();
                if (obj instanceof CameraCharacteristics) {
                    cameraCharacteristics = (CameraCharacteristics) obj;
                }
            } catch (Exception e) {
                Log.e(TAG, "getCameraCharacteristics failed", e);
            }
        }

        mImagePipeline = new ImagePipeline(config, resolution,
                cameraCharacteristics,
                getEffect(), isVirtualCamera,
                postViewSize, postviewFormat);

        if (mTakePictureManager == null) {
            // mTakePictureManager is reused when the Surface is reset.
            mTakePictureManager = getCurrentConfig().getTakePictureManagerProvider().newInstance(
                    mImageCaptureControl);
        }
        mTakePictureManager.setImagePipeline(mImagePipeline);

        SessionConfig.Builder sessionConfigBuilder =
                mImagePipeline.createSessionConfigBuilder(streamSpec.getResolution());
        if (Build.VERSION.SDK_INT >= 23
                && getCaptureMode() == CAPTURE_MODE_ZERO_SHUTTER_LAG
                && !streamSpec.getZslDisabled()) {
            getCameraControl().addZslConfig(sessionConfigBuilder);
        }
        if (streamSpec.getImplementationOptions() != null) {
            sessionConfigBuilder.addImplementationOptions(streamSpec.getImplementationOptions());
        }
        // Close the old error listener
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
        }
        mCloseableErrorListener = new SessionConfig.CloseableErrorListener(
                (sessionConfig, error) -> {
                    // Do nothing when the use case has been unbound.
                    if (getCamera() == null) {
                        return;
                    }

                    mTakePictureManager.pause();
                    clearPipeline(/*keepTakePictureManager=*/ true);
                    mSessionConfigBuilder = createPipeline(getCameraId(),
                            (ImageCaptureConfig) getCurrentConfig(),
                            Preconditions.checkNotNull(getAttachedStreamSpec()));
                    updateSessionConfig(List.of(mSessionConfigBuilder.build()));
                    notifyReset();
                    mTakePictureManager.resume();
                });
        sessionConfigBuilder.setErrorListener(mCloseableErrorListener);
        return sessionConfigBuilder;
    }

    /**
     * Takes a picture with the new architecture.
     *
     * @throws IllegalArgumentException If {@link ImageCapture#FLASH_MODE_SCREEN} is used without a
     *                                  non-null {@code ScreenFlash} instance set.
     */
    @MainThread
    private void takePictureInternal(@NonNull Executor executor,
            @Nullable OnImageCapturedCallback inMemoryCallback,
            @Nullable ImageCapture.OnImageSavedCallback onDiskCallback,
            @Nullable List<OutputFileOptions> outputFileOptions) {
        checkMainThread();
        if (getFlashMode() == ImageCapture.FLASH_MODE_SCREEN
                && mScreenFlashWrapper.getBaseScreenFlash() == null) {
            throw new IllegalArgumentException("ScreenFlash not set for FLASH_MODE_SCREEN");
        }
        Log.d(TAG, "takePictureInternal");
        CameraInternal camera = getCamera();
        if (camera == null) {
            sendInvalidCameraError(executor, inMemoryCallback, onDiskCallback);
            return;
        }
        requireNonNull(mTakePictureManager).offerRequest(TakePictureRequest.of(
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
    private void clearPipeline() {
        clearPipeline(/*keepTakePictureManager=*/false);
    }

    /**
     * Clears the pipeline.
     */
    @MainThread
    private void clearPipeline(boolean keepTakePictureManager) {
        Log.d(TAG, "clearPipeline");
        checkMainThread();

        // Close the old error listener
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
            mCloseableErrorListener = null;
        }

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
     * Returns an estimate of the capture and processing sequence duration based on the current
     * camera configuration and scene conditions. The value will vary as the scene and/or camera
     * configuration change.
     *
     * <p>The processing estimate can vary based on device processing load.
     */
    @NonNull
    public ImageCaptureLatencyEstimate getRealtimeCaptureLatencyEstimate() {
        final CameraInternal camera = getCamera();
        if (camera == null) {
            return ImageCaptureLatencyEstimate.UNDEFINED_IMAGE_CAPTURE_LATENCY;
        }

        final CameraConfig config = camera.getExtendedConfig();
        final SessionProcessor sessionProcessor = config.getSessionProcessor();
        final Pair<Long, Long> latencyEstimate = sessionProcessor.getRealtimeCaptureLatency();
        if (latencyEstimate == null) {
            return ImageCaptureLatencyEstimate.UNDEFINED_IMAGE_CAPTURE_LATENCY;
        }
        return new ImageCaptureLatencyEstimate(latencyEstimate.first, latencyEstimate.second);
    }

    /**
     * Returns if postview is enabled or not.
     *
     * @see Builder#setPostviewEnabled(boolean)
     */
    public boolean isPostviewEnabled() {
        return getCurrentConfig().retrieveOption(OPTION_POSTVIEW_ENABLED, false);
    }

    /**
     * Returns the {@link ResolutionSelector} used to select the postview size.
     *
     * @see Builder#setPostviewResolutionSelector(ResolutionSelector)
     */
    @Nullable
    public ResolutionSelector getPostviewResolutionSelector() {
        return getCurrentConfig().retrieveOption(OPTION_POSTVIEW_RESOLUTION_SELECTOR,
                null);
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
    @OptIn(markerClass = androidx.camera.core.ExperimentalZeroShutterLag.class)
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
    @IntDef({FLASH_MODE_UNKNOWN, FLASH_MODE_AUTO, FLASH_MODE_ON, FLASH_MODE_SCREEN, FLASH_MODE_OFF})
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

    /**
     * The output format of the captured image.
     */
    @OptIn(markerClass = androidx.camera.core.ExperimentalImageCaptureOutputFormat.class)
    @Target({ElementType.TYPE_USE})
    @IntDef({OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_JPEG_ULTRA_HDR,
            OUTPUT_FORMAT_RAW, OUTPUT_FORMAT_RAW_JPEG})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface OutputFormat {
    }

    /** Listener containing callbacks for image file I/O events. */
    public interface OnImageSavedCallback {
        /**
         * Called when the capture is started.
         *
         * <p>This callback is guaranteed to be called once and before
         * {@link #onImageSaved(OutputFileResults)} for the same invocation of
         * {@link #takePicture(OutputFileOptions, Executor, OnImageSavedCallback)}.
         *
         * <p>It's recommended to play shutter sound or trigger UI indicators of
         * capture when receiving this callback.
         */
        default void onCaptureStarted() {
        }

        /** Called when an image has been successfully saved. */
        void onImageSaved(@NonNull OutputFileResults outputFileResults);

        /**
         * Called when an error occurs while attempting to save an image.
         *
         * @param exception An {@link ImageCaptureException} that contains the type of error, the
         *                  error message and the throwable that caused it.
         */
        void onError(@NonNull ImageCaptureException exception);

        /**
         * Callback to report the progress of the capture's processing.
         *
         * <p>To know in advanced if this callback will be invoked or not, check the
         * capabilities by {@link #getImageCaptureCapabilities(CameraInfo)} and
         * {@link ImageCaptureCapabilities#isCaptureProcessProgressSupported()}. If supported,
         * this callback will be called multiple times with monotonically increasing
         * values. At the minimum the callback will be called once with value 100 to
         * indicate the processing is finished. This callback will always be called before
         * {@link #onImageSaved(OutputFileResults)}.
         *
         * @param progress the progress ranging from 0 to 100.
         */
        default void onCaptureProcessProgressed(int progress) {
        }

        /**
         * Callback to notify that the postview bitmap is available. The postview is intended to be
         * shown on UI before the long-processing capture is completed in order to provide a
         * better UX.
         *
         * <p>The postview is only available when the
         * {@link ImageCaptureCapabilities#isPostviewSupported()} returns true for the specified
         * {@link CameraInfo} and applications must explicitly enable the postview using the
         * {@link Builder#setPostviewEnabled(boolean)}. This callback will be called before
         * {@link #onImageSaved(OutputFileResults)}. But if something goes wrong when processing
         * the postview, this callback method could be skipped.
         *
         * <p>The bitmap is rotated according to the target rotation set to the {@link ImageCapture}
         * to make it upright. If target rotation is not set, the display rotation is used.
         *
         * <p>See also {@link ImageCapture.Builder#setTargetRotation(int)} and
         * {@link #setTargetRotation(int)}.
         *
         * @param bitmap the postview bitmap.
         */
        default void onPostviewBitmapAvailable(@NonNull Bitmap bitmap) {
        }
    }

    /**
     * Callback for image capture events.
     */
    public abstract static class OnImageCapturedCallback {
        /**
         * Callback for when the camera has started exposing a frame.
         *
         * <p>This callback is guaranteed to be called once and before
         * {@link #onCaptureSuccess(ImageProxy)} for the same invocation of
         * {@link #takePicture(Executor, OnImageCapturedCallback)}.
         *
         * <p>It's recommended to play shutter sound or trigger UI indicators of
         * capture when receiving this callback.
         */
        public void onCaptureStarted() {
        }

        /**
         * Callback for when the image has been captured.
         *
         * <p>The application is responsible for calling {@link ImageProxy#close()} to close the
         * image.
         *
         * <p>The image is of format {@link ImageFormat#JPEG} or {@link ImageFormat#JPEG_R},
         * queryable via {@link ImageProxy#getFormat()}.
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

        /**
         * Callback to report the progress of the capture's processing.
         *
         * <p>To know in advanced if this callback will be invoked or not, check the
         * capabilities by {@link #getImageCaptureCapabilities(CameraInfo)} and
         * {@link ImageCaptureCapabilities#isCaptureProcessProgressSupported()}. If supported,
         * this callback will be called multiple times with monotonically increasing
         * values. At the minimum the callback will be called once with value 100 to
         * indicate the processing is finished. This callback will always be called before
         * {@link #onCaptureSuccess(ImageProxy)}.
         *
         * @param progress the progress ranging from 0 to 100.
         */
        public void onCaptureProcessProgressed(int progress) {
        }

        /**
         * Callback to notify that the postview bitmap is available. The postview is intended to be
         * shown on UI before the long-processing capture is completed in order to provide a
         * better UX.
         *
         * <p>The postview is only available when the
         * {@link ImageCaptureCapabilities#isPostviewSupported()} returns true for the specified
         * {@link CameraInfo} and applications must explicitly enable the postview using the
         * {@link Builder#setPostviewEnabled(boolean)}. This callback will be called before
         * {@link #onCaptureSuccess(ImageProxy)}. But if something goes wrong when processing the
         * postview, this callback method could be skipped.
         *
         * <p>The bitmap is rotated according to the target rotation set to the {@link ImageCapture}
         * to make it upright. If target rotation is not set, the display rotation is used.
         *
         * <p>See also {@link ImageCapture.Builder#setTargetRotation(int)} and
         * {@link #setTargetRotation(int)}.
         *
         * @param bitmap the postview bitmap.

         */
        public void onPostviewBitmapAvailable(@NonNull Bitmap bitmap) {
        }
    }

    /**
     * Callback listener for discovering when the application has completed its changes for a
     * screen flash image capture.
     *
     * <p> For example, an application may change its UI to a full white screen with maximum
     * brightness for a proper screen flash capture.
     *
     * @see ScreenFlash#apply(long, ScreenFlashListener)
     */
    public interface ScreenFlashListener {
        /**
         * Invoked by the application when it has completed its changes due to a screen flash
         * image capture.
         *
         * @see ScreenFlash#apply
         */
        void onCompleted();
    }

    /**
     * Interface to do the application changes required for screen flash operations.
     *
     * <p> Each {@link #apply} invocation will be followed up with a corresponding {@link #clear}
     * invocation. For each image capture, {@code #apply} and {@code #clear} will be invoked only
     * once.
     */
    public interface ScreenFlash {
        /**
         * Applies the necessary application changes for a screen flash photo capture.
         *
         * <p>When the application UI needs to be changed for a successful photo capture with
         * screen flash feature, CameraX will invoke this method and wait for the application to
         * complete its changes. When this API is invoked, the application UI should utilize the
         * screen to provide extra light as an alternative to physical flash. For example, the
         * screen brightness can be maximized and screen color can be covered with some bright
         * color like white.
         *
         * <p>The parameter {@code expirationTimeMillis} is based on
         * {@link System#currentTimeMillis()}. It is at least 3 seconds later from the start of a
         * screen flash image capture operation. Until the timestamp of {@code expirationTimeMillis}
         * parameter, CameraX will wait for the application to notify the completion of the
         * application-side changes using the {@link ScreenFlashListener} parameter of this
         * method. Applications must call {@link ScreenFlashListener#onCompleted()} after their
         * UI changes are done so that CameraX is not unnecessarily waiting. If the application
         * does not call {@code ScreenFlashListener#onCompleted} before {@code
         * expirationTimeMillis}, CameraX will stop waiting and move forward with the subsequent
         * operations regardless. In such case, the application no longer needs to call {@code
         * ScreenFlashListener#onCompleted()}. If {@link #clear} has also been invoked while the
         * application is still doing the changes, it is the application's responsibility to
         * clear any UI change done after {@link #clear} has been invoked.
         *
         * <p>The following code snippet shows an example implementation of this API.
         * <pre>{@code
         * @Override
         * public void apply(long expirationTimeMillis,
         *         @NonNull ScreenFlashListener screenFlashListener) {
         *     // Enable top overlay to make screen color white
         *     whiteColorOverlay.setVisible(true);
         *     // Maximize screen brightness
         *     maximizeScreenBrightness();
         *     screenFlashListener.onCompleted();
         * }}</pre>
         *
         * @param expirationTimeMillis The timestamp after which CameraX will no longer listen
         *                             to {@code screenFlashListener}.
         * @param screenFlashListener  Used to notify when UI changes have been applied.
         */
        // ExecutorRegistration lint suppressed since this is called by app and CameraX supports
        // receiving the call on any thread. Adding executor will make it harder for apps.
        @SuppressWarnings("ExecutorRegistration")
        @UiThread
        void apply(long expirationTimeMillis, @NonNull ScreenFlashListener screenFlashListener);

        /**
         * Clears any application change done for screen flash operation, if required.
         *
         * <p>CameraX will invoke this method when a screen flash photo capture has been completed
         * and the application screen can be safely changed to a state not conforming to screen
         * flash photo capture.
         */
        @UiThread
        void clear();
    }

    /**
     * Provides a base static default configuration for the ImageCapture
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     */
    @OptIn(markerClass = androidx.camera.core.ExperimentalImageCaptureOutputFormat.class)
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults
            implements ConfigProvider<ImageCaptureConfig> {
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 4;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;
        private static final int DEFAULT_OUTPUT_FORMAT = OUTPUT_FORMAT_JPEG;

        private static final ResolutionSelector DEFAULT_RESOLUTION_SELECTOR =
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).setResolutionStrategy(
                        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build();

        private static final ImageCaptureConfig DEFAULT_CONFIG;
        // ImageCapture does not yet support HDR so we must default to SDR. This ensures it won't
        // choose an HDR format when other use cases have selected HDR.
        private static final DynamicRange DEFAULT_DYNAMIC_RANGE = DynamicRange.SDR;

        static {
            Builder builder = new Builder()
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                    .setResolutionSelector(DEFAULT_RESOLUTION_SELECTOR)
                    .setOutputFormat(DEFAULT_OUTPUT_FORMAT)
                    .setDynamicRange(DEFAULT_DYNAMIC_RANGE);

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

    /** Builder for an {@link ImageCapture}. */
    @SuppressWarnings({"ObjectToString", "unused", "HiddenSuperclass"})
    public static final class Builder implements
            UseCaseConfig.Builder<ImageCapture, ImageCaptureConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            IoConfig.Builder<Builder>,
            ImageInputConfig.Builder<Builder> {

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

            setCaptureType(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE);
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
         *                                  target resolution, or attempting to set
         *                                  {@link ImageCapture#FLASH_MODE_SCREEN} without
         *                                  setting a non-null {@link ScreenFlash} instance.
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
                if (isOutputFormatRaw(getMutableConfig())) {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, RAW_SENSOR);
                } else if (isOutputFormatRawJpeg(getMutableConfig())) {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, RAW_SENSOR);
                    getMutableConfig().insertOption(OPTION_SECONDARY_INPUT_FORMAT, JPEG);
                } else if (isOutputFormatUltraHdr(getMutableConfig())) {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, JPEG_R);
                    getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE,
                            DynamicRange.UNSPECIFIED);
                } else {
                    getMutableConfig().insertOption(OPTION_INPUT_FORMAT, JPEG);
                }
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
                        && flashMode != FLASH_MODE_SCREEN && flashMode != FLASH_MODE_OFF)) {
                    throw new IllegalArgumentException(
                            "The flash mode is not allowed to set: " + flashMode);
                }

                if (flashMode == FLASH_MODE_SCREEN) {
                    if (getMutableConfig().retrieveOption(OPTION_SCREEN_FLASH, null)
                            == null) {
                        throw new IllegalArgumentException(
                                "The flash mode is not allowed to set to FLASH_MODE_SCREEN "
                                        + "without setting ScreenFlash");
                    }
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
         * <p>If set to {@link #FLASH_MODE_SCREEN}, a non-null {@link ScreenFlash} instance must
         * also be set with {@link #setScreenFlash(ScreenFlash)}. Otherwise, an
         * {@link IllegalArgumentException} will be thrown when {@link #build()} is invoked.
         *
         * <p>See {@link ImageCapture#setFlashMode(int)} for more information.
         *
         * @param flashMode The requested flash mode. Value is {@link #FLASH_MODE_AUTO},
         *                  {@link #FLASH_MODE_ON}, {@link #FLASH_MODE_SCREEN}, or
         *                  {@link #FLASH_MODE_OFF}.
         * @return The current Builder.
         */
        @NonNull
        public Builder setFlashMode(@FlashMode int flashMode) {
            getMutableConfig().insertOption(OPTION_FLASH_MODE, flashMode);
            return this;
        }

        /**
         * Sets the {@link ScreenFlash} instance necessary for screen flash operations.
         *
         * <p>If not set, the instance will be set to null and users will need to set it later
         * before calling {@link #setFlashMode(int)} with {@link #FLASH_MODE_SCREEN}.
         *
         * <p>See {@link ImageCapture#setScreenFlash(ScreenFlash)} for more
         * information.
         *
         * @param screenFlash The {@link ScreenFlash} to notify caller for the
         *                             UI side changes required for photo capture with
         *                             {@link #FLASH_MODE_SCREEN}.
         * @return The current Builder.
         */
        @NonNull
        public Builder setScreenFlash(@NonNull ScreenFlash screenFlash) {
            getMutableConfig().insertOption(OPTION_SCREEN_FLASH, screenFlash);
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
         * case. See {@link androidx.camera.core.ImageCapture#setTargetRotation(int)} for
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

        /**
         * Enables postview image generation. A postview image is a low-quality image
         * that's produced earlier during image capture than the final high-quality image,
         * and can be used as a thumbnail or placeholder until the final image is ready.
         *
         * <p>When the postview is available,
         * {@link OnImageCapturedCallback#onPostviewBitmapAvailable(Bitmap)} or
         * {@link OnImageSavedCallback#onPostviewBitmapAvailable(Bitmap)} will be called.
         *
         * <p>By default the largest available postview size that is smaller or equal to the
         * ImagaeCapture size will be used to configure the postview. The {@link ResolutionSelector}
         * can also be used to select a specific size via
         * {@link #setPostviewResolutionSelector(ResolutionSelector)}.
         *
         * <p>You can query the postview capability by invoking
         * {@link #getImageCaptureCapabilities(CameraInfo)}. If
         * {@link ImageCaptureCapabilities#isPostviewSupported()} returns false and you still
         * enable the postview, the postview image won't be generated.
         *
         * @param postviewEnabled whether postview is enabled or not
         * @return the current Builder.
         */
        @NonNull
        public Builder setPostviewEnabled(boolean postviewEnabled) {
            getMutableConfig().insertOption(OPTION_POSTVIEW_ENABLED,
                    postviewEnabled);
            return this;
        }

        /**
         * Set the {@link ResolutionSelector} to select the postview size from the available
         * postview sizes. These available postview sizes are smaller or equal to the
         * ImageCapture size. You can implement the
         * {@link androidx.camera.core.resolutionselector.ResolutionFilter} and set it to the
         * {@link ResolutionSelector} to get the list of available sizes and determine which size
         * to use.
         *
         * <p>If no sizes can be selected using the given {@link ResolutionSelector}, it will throw
         * an {@link IllegalArgumentException} when {@code bindToLifecycle()} is invoked.
         *
         * @return the current Builder.
         */
        @NonNull
        public Builder setPostviewResolutionSelector(
                @NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_POSTVIEW_RESOLUTION_SELECTOR,
                    resolutionSelector);
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

        /**
         * Sets the output format of the captured image.
         *
         * <p>The supported output formats for capturing image depend on the capabilities of the
         * camera. The supported output formats of the camera can be queried using
         * {@link ImageCaptureCapabilities#getSupportedOutputFormats()}.
         *
         * <p>If not set, the output format will default to {@link #OUTPUT_FORMAT_JPEG}.
         *
         * @param outputFormat The output image format. Value is {@link #OUTPUT_FORMAT_JPEG} or
         *                     {@link #OUTPUT_FORMAT_JPEG_ULTRA_HDR} or {@link #OUTPUT_FORMAT_RAW}.
         * @return The current Builder.
         *
         * @see OutputFormat
         * @see ImageCaptureCapabilities#getSupportedOutputFormats()
         */
        @ExperimentalImageCaptureOutputFormat
        @NonNull
        public Builder setOutputFormat(@OutputFormat int outputFormat) {
            getMutableConfig().insertOption(OPTION_OUTPUT_FORMAT, outputFormat);
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
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
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

        /**
         * {@inheritDoc}
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setCaptureType(@NonNull UseCaseConfigFactory.CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }

        // Implementations of ImageInputConfig.Builder default methods

        /**
         * Sets the {@link DynamicRange}.
         *
         * <p>This is currently only exposed to internally set the dynamic range to SDR.
         *
         * @return The current Builder.
         * @see DynamicRange
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        @Override
        public Builder setDynamicRange(@NonNull DynamicRange dynamicRange) {
            getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE, dynamicRange);
            return this;
        }
    }
}
