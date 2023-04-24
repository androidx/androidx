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

import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_BACKPRESSURE_STRATEGY;
import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_IMAGE_QUEUE_DEPTH;
import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_IMAGE_READER_PROXY_PROVIDER;
import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_ONE_PIXEL_SHIFT_ENABLED;
import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_OUTPUT_IMAGE_FORMAT;
import static androidx.camera.core.impl.ImageAnalysisConfig.OPTION_OUTPUT_IMAGE_ROTATION_ENABLED;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_DEFAULT_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAMERA_SELECTOR;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_HIGH_RESOLUTION_DISABLED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_USE_CASE_EVENT_CALLBACK;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;
import static androidx.camera.core.internal.ThreadConfig.OPTION_BACKGROUND_EXECUTOR;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.core.internal.compat.quirk.OnePixelShiftQuirk;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LifecycleOwner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A use case providing CPU accessible images for an app to perform image analysis on.
 *
 * <p>ImageAnalysis acquires images from the camera via an {@link ImageReader}. Each image
 * is provided to an {@link ImageAnalysis.Analyzer} function which can be implemented by application
 * code, where it can access image data for application analysis via an {@link ImageProxy}.
 *
 * <p>The application is responsible for calling {@link ImageProxy#close()} to close the image.
 * Failing to close the image will cause future images to be stalled or dropped depending on the
 * backpressure strategy.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ImageAnalysis extends UseCase {

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime constant] - Stays constant for the lifetime of the UseCase. Which means
    // they could be created in the constructor.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Only deliver the latest image to the analyzer, dropping images as they arrive.
     *
     * <p>This strategy ignores the value set by {@link Builder#setImageQueueDepth(int)}.
     * Only one image will be delivered for analysis at a time. If more images are produced
     * while that image is being analyzed, they will be dropped and not queued for delivery.
     * Once the image being analyzed is closed by calling {@link ImageProxy#close()}, the
     * next latest image will be delivered.
     *
     * <p>Internally this strategy may make use of an internal {@link Executor} to receive
     * and drop images from the producer. A performance-tuned executor will be created
     * internally unless one is explicitly provided by
     * {@link Builder#setBackgroundExecutor(Executor)}. In order to
     * ensure smooth operation of this backpressure strategy, any user supplied
     * {@link Executor} must be able to quickly respond to tasks posted to it, so setting
     * the executor manually should only be considered in advanced use cases.
     *
     * @see Builder#setBackgroundExecutor(Executor)
     */
    public static final int STRATEGY_KEEP_ONLY_LATEST = 0;
    /**
     * Block the producer from generating new images.
     *
     * <p>Once the producer has produced the number of images equal to the image queue depth,
     * and none have been closed, the producer will stop producing images. Note that images
     * may be queued internally and not be delivered to the analyzer until the last delivered
     * image has been closed with {@link ImageProxy#close()}. These internally queued images
     * will count towards the total number of images that the producer can provide at any one
     * time.
     *
     * <p>When the producer stops producing images, it may also stop producing images for
     * other use cases, such as {@link Preview}, so it is important for the analyzer to keep
     * up with frame rate, <i>on average</i>. Failure to keep up with frame rate may lead to
     * jank in the frame stream and a diminished user experience. If more time is needed for
     * analysis on <i>some</i> frames, consider increasing the image queue depth with
     * {@link Builder#setImageQueueDepth(int)}.
     *
     * @see Builder#setImageQueueDepth(int)
     */
    public static final int STRATEGY_BLOCK_PRODUCER = 1;

    /**
     * Images sent to the analyzer will have YUV format.
     *
     * <p>All {@link ImageProxy} sent to {@link Analyzer#analyze(ImageProxy)} will have
     * format {@link android.graphics.ImageFormat#YUV_420_888}
     *
     * @see Builder#setOutputImageFormat(int)
     */
    public static final int OUTPUT_IMAGE_FORMAT_YUV_420_888 = 1;

    /**
     * Images sent to the analyzer will have RGBA format.
     *
     * <p>All {@link ImageProxy} sent to {@link Analyzer#analyze(ImageProxy)} will have
     * format {@link android.graphics.PixelFormat#RGBA_8888}
     *
     * <p>The output order is a single-plane with the order of R, G, B, A in increasing byte index
     * in the {@link java.nio.ByteBuffer}. The {@link java.nio.ByteBuffer} is retrieved from
     * {@link ImageProxy.PlaneProxy#getBuffer()}.
     *
     * @see Builder#setOutputImageFormat(int)
     */
    public static final int OUTPUT_IMAGE_FORMAT_RGBA_8888 = 2;

    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "ImageAnalysis";
    // ImageReader depth for KEEP_ONLY_LATEST mode.
    private static final int NON_BLOCKING_IMAGE_DEPTH = 4;
    @BackpressureStrategy
    private static final int DEFAULT_BACKPRESSURE_STRATEGY = STRATEGY_KEEP_ONLY_LATEST;
    private static final int DEFAULT_IMAGE_QUEUE_DEPTH = 6;
    // Default to YUV_420_888 format for output.
    private static final int DEFAULT_OUTPUT_IMAGE_FORMAT = OUTPUT_IMAGE_FORMAT_YUV_420_888;
    // One pixel shift for YUV.
    private static final Boolean DEFAULT_ONE_PIXEL_SHIFT_ENABLED = null;
    // Default to disabled for rotation.
    private static final boolean DEFAULT_OUTPUT_IMAGE_ROTATION_ENABLED = false;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ImageAnalysisAbstractAnalyzer mImageAnalysisAbstractAnalyzer;
    private final Object mAnalysisLock = new Object();

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime dynamic] - Dynamic variables which could change during anytime during
    // the UseCase lifetime.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @GuardedBy("mAnalysisLock")
    private ImageAnalysis.Analyzer mSubscribedAnalyzer;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    private DeferrableSurface mDeferrableSurface;

    /**
     * Creates a new image analysis use case from the given configuration.
     *
     * @param config for this use case instance
     */
    @SuppressWarnings("WeakerAccess")
    ImageAnalysis(@NonNull ImageAnalysisConfig config) {
        super(config);

        // Get the combined configuration with defaults
        ImageAnalysisConfig combinedConfig = (ImageAnalysisConfig) getCurrentConfig();

        if (combinedConfig.getBackpressureStrategy(DEFAULT_BACKPRESSURE_STRATEGY)
                == STRATEGY_BLOCK_PRODUCER) {
            mImageAnalysisAbstractAnalyzer = new ImageAnalysisBlockingAnalyzer();
        } else {
            mImageAnalysisAbstractAnalyzer = new ImageAnalysisNonBlockingAnalyzer(
                    config.getBackgroundExecutor(CameraXExecutors.highPriorityExecutor()));
        }
        mImageAnalysisAbstractAnalyzer.setOutputImageFormat(getOutputImageFormat());
        mImageAnalysisAbstractAnalyzer.setOutputImageRotationEnabled(
                isOutputImageRotationEnabled());
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {

        // Flag to enable or disable one pixel shift. It will override the flag set by device info.
        // If enabled, the workaround will be applied for all devices.
        // If disabled, the workaround will be disabled for all devices.
        // If not configured, the workaround will be applied to the problem devices only.
        Boolean isOnePixelShiftEnabled = getOnePixelShiftEnabled();
        boolean isOnePixelShiftIssueDevice = cameraInfo.getCameraQuirks().contains(
                OnePixelShiftQuirk.class) ? true : false;
        mImageAnalysisAbstractAnalyzer.setOnePixelShiftEnabled(
                isOnePixelShiftEnabled == null ? isOnePixelShiftIssueDevice
                        : isOnePixelShiftEnabled);

        // Override the target resolution with the value provided by the analyzer.
        Size analyzerResolution;
        synchronized (mAnalysisLock) {
            analyzerResolution = mSubscribedAnalyzer != null
                    ? mSubscribedAnalyzer.getDefaultTargetResolution() : null;
        }

        if (analyzerResolution == null) {
            return builder.getUseCaseConfig();
        }

        int targetRotation = builder.getMutableConfig().retrieveOption(
                OPTION_TARGET_ROTATION, Surface.ROTATION_0);
        // analyzerResolution is a size in the sensor coordinate system, but the legacy
        // target resolution setting is in the view coordinate system. Flips the
        // analyzerResolution according to the sensor rotation degrees.
        if (cameraInfo.getSensorRotationDegrees(targetRotation) % 180 == 90) {
            analyzerResolution = new Size(/* width= */ analyzerResolution.getHeight(),
                    /* height= */ analyzerResolution.getWidth());
        }

        // Merges the analyzerResolution as legacy target resolution setting so that it can take
        // effect when running the legacy resolution selection logic flow.
        if (!builder.getUseCaseConfig().containsOption(OPTION_TARGET_RESOLUTION)) {
            builder.getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION,
                    analyzerResolution);
        }

        // Merges the analyzerResolution to ResolutionSelector.
        ResolutionSelector resolutionSelector =
                builder.getMutableConfig().retrieveOption(OPTION_RESOLUTION_SELECTOR, null);
        if (resolutionSelector != null && resolutionSelector.getResolutionStrategy() == null) {
            ResolutionSelector.Builder resolutionSelectorBuilder =
                    ResolutionSelector.Builder.fromResolutionSelector(resolutionSelector);
            resolutionSelectorBuilder.setResolutionStrategy(
                    new ResolutionStrategy(analyzerResolution,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER));
            builder.getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR,
                    resolutionSelectorBuilder.build());
        }

        return builder.getUseCaseConfig();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageAnalysisConfig config, @NonNull StreamSpec streamSpec) {
        Threads.checkMainThread();
        Size resolution = streamSpec.getResolution();

        Executor backgroundExecutor = Preconditions.checkNotNull(config.getBackgroundExecutor(
                CameraXExecutors.highPriorityExecutor()));

        int imageQueueDepth =
                getBackpressureStrategy() == STRATEGY_BLOCK_PRODUCER ? getImageQueueDepth()
                        : NON_BLOCKING_IMAGE_DEPTH;
        SafeCloseImageReaderProxy imageReaderProxy;
        if (config.getImageReaderProxyProvider() != null) {
            imageReaderProxy = new SafeCloseImageReaderProxy(
                    config.getImageReaderProxyProvider().newInstance(
                            resolution.getWidth(), resolution.getHeight(), getImageFormat(),
                            imageQueueDepth, 0));
        } else {
            imageReaderProxy =
                    new SafeCloseImageReaderProxy(ImageReaderProxys.createIsolatedReader(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            getImageFormat(),
                            imageQueueDepth));
        }

        boolean flipWH = getCamera() != null ? isFlipWH(getCamera()) : false;
        int width = flipWH ? resolution.getHeight() : resolution.getWidth();
        int height = flipWH ? resolution.getWidth() : resolution.getHeight();
        int format = getOutputImageFormat() == OUTPUT_IMAGE_FORMAT_RGBA_8888
                ? PixelFormat.RGBA_8888 : ImageFormat.YUV_420_888;

        boolean isYuv2Rgb = getImageFormat() == ImageFormat.YUV_420_888
                && getOutputImageFormat() == OUTPUT_IMAGE_FORMAT_RGBA_8888;
        boolean isYuvRotationOrPixelShift = getImageFormat() == ImageFormat.YUV_420_888
                && ((getCamera() != null && getRelativeRotation(getCamera()) != 0)
                || Boolean.TRUE.equals(getOnePixelShiftEnabled()));

        // TODO(b/195021586): to support RGB format input for image analysis for devices already
        // supporting RGB natively. The logic here will check if the specific configured size is
        // available in RGB and if not, fall back to YUV-RGB conversion.
        final SafeCloseImageReaderProxy processedImageReaderProxy =
                (isYuv2Rgb || isYuvRotationOrPixelShift)
                        ? new SafeCloseImageReaderProxy(
                        ImageReaderProxys.createIsolatedReader(
                                width,
                                height,
                                format,
                                imageReaderProxy.getMaxImages())) : null;
        if (processedImageReaderProxy != null) {
            mImageAnalysisAbstractAnalyzer.setProcessedImageReaderProxy(processedImageReaderProxy);
        }

        tryUpdateRelativeRotation();

        imageReaderProxy.setOnImageAvailableListener(mImageAnalysisAbstractAnalyzer,
                backgroundExecutor);

        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());

        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = new ImmediateSurface(imageReaderProxy.getSurface(), resolution,
                getImageFormat());
        mDeferrableSurface.getTerminationFuture().addListener(
                () -> {
                    imageReaderProxy.safeClose();
                    if (processedImageReaderProxy != null) {
                        processedImageReaderProxy.safeClose();
                    }
                },
                CameraXExecutors.mainThreadExecutor());

        sessionConfigBuilder.setExpectedFrameRateRange(streamSpec.getExpectedFrameRateRange());

        sessionConfigBuilder.addSurface(mDeferrableSurface);


        sessionConfigBuilder.addErrorListener((sessionConfig, error) -> {
            clearPipeline();
            // Clear cache so app won't get a outdated image.
            mImageAnalysisAbstractAnalyzer.clearCache();
            // Ensure the attached camera has not changed before resetting.
            // TODO(b/143915543): Ensure this never gets called by a camera that is not attached
            //  to this use case so we don't need to do this check.
            if (isCurrentCamera(cameraId)) {
                // Only reset the pipeline when the bound camera is the same.
                SessionConfig.Builder errorSessionConfigBuilder = createPipeline(cameraId, config,
                        streamSpec);
                updateSessionConfig(errorSessionConfigBuilder.build());

                notifyReset();
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void clearPipeline() {
        Threads.checkMainThread();
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
            mDeferrableSurface = null;
        }
    }

    /**
     * Removes a previously set analyzer.
     *
     * <p>This will stop data from streaming to the {@link ImageAnalysis}.
     */
    public void clearAnalyzer() {
        synchronized (mAnalysisLock) {
            mImageAnalysisAbstractAnalyzer.setAnalyzer(null, null);
            if (mSubscribedAnalyzer != null) {
                notifyInactive();
            }
            mSubscribedAnalyzer = null;
        }
    }

    /**
     * Returns the rotation of the intended target for images.
     *
     * <p>
     * The rotation can be set when constructing an {@link ImageAnalysis} instance using
     * {@link ImageAnalysis.Builder#setTargetRotation(int)}, or dynamically by calling
     * {@link ImageAnalysis#setTargetRotation(int)} or
     * {@link ImageAnalysis#setTargetRotationDegrees(int)}. If not set, the target rotation
     * defaults to the value of {@link Display#getRotation()} of the default display at the time
     * the use case is created. The use case is fully created once it has been attached to a camera.
     * </p>
     *
     * @return The rotation of the intended target for images.
     * @see ImageAnalysis#setTargetRotation(int)
     */
    @RotationValue
    public int getTargetRotation() {
        return getTargetRotationInternal();
    }

    /**
     * Sets the target rotation.
     *
     * <p>This adjust the {@link ImageInfo#getRotationDegrees()} of the {@link ImageProxy} passed
     * to {@link Analyzer#analyze(ImageProxy)}. The rotation value of ImageInfo will be the
     * rotation, which if applied to the output image, will make the image match target rotation
     * specified here.
     *
     * <p>While rotation can also be set via {@link Builder#setTargetRotation(int)}, using
     * {@link ImageAnalysis#setTargetRotation(int)} allows the target rotation to be set
     * dynamically.
     *
     * <p>In general, it is best to use an {@link android.view.OrientationEventListener} to
     * set the target rotation.  This way, the rotation output to the Analyzer will indicate
     * which way is down for a given image.  This is important since display orientation may be
     * locked by device default, user setting, or app configuration, and some devices may not
     * transition to a reverse-portrait display orientation.  In these cases, use
     * {@link ImageAnalysis#setTargetRotationDegrees(int)} to set target rotation dynamically
     * according to the {@link android.view.OrientationEventListener}, without re-creating the
     * use case. See {@link #setTargetRotationDegrees} for more information.
     *
     * <p>When this function is called, value set by
     * {@link ImageAnalysis.Builder#setTargetResolution(Size)} will be updated automatically to
     * make sure the suitable resolution can be selected when the use case is bound.
     *
     * <p>If not set here or by configuration, the target rotation will default to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is bound. To
     * return to the default value, set the value to
     * <pre>{@code
     * context.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
     * }</pre>
     *
     * @param rotation Target rotation of the output image, expressed as one of
     *                 {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                 {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        if (setTargetRotationInternal(rotation)) {
            tryUpdateRelativeRotation();
        }
    }

    /**
     * Sets the target rotation in degrees.
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
     *                     mImageAnalysis.setTargetRotationDegrees(orientation);
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
     * {@link ImageAnalysis.Builder#setTargetResolution(Size)} will be updated automatically to
     * make sure the suitable resolution can be selected when the use case is bound.
     *
     * @param degrees Desired rotation degree of the output image.
     * @see #setTargetRotation(int)
     * @see #getTargetRotation()
     */
    public void setTargetRotationDegrees(int degrees) {
        setTargetRotation(orientationDegreesToSurfaceRotation(degrees));
    }

    /**
     * Sets an analyzer to receive and analyze images.
     *
     * <p>Setting an analyzer will signal to the camera that it should begin sending data. The
     * stream of data can be stopped by calling {@link #clearAnalyzer()}.
     *
     * <p>Applications can process or copy the image by implementing the {@link Analyzer}.  If
     * frames should be skipped (no analysis), the analyzer function should return, instead of
     * disconnecting the analyzer function completely.
     *
     * <p>Setting an analyzer function replaces any previous analyzer.  Only one analyzer can be
     * set at any time.
     *
     * @param executor The executor in which the
     *                 {@link ImageAnalysis.Analyzer#analyze(ImageProxy)} will be run.
     * @param analyzer of the images.
     */
    public void setAnalyzer(@NonNull Executor executor, @NonNull Analyzer analyzer) {
        synchronized (mAnalysisLock) {
            mImageAnalysisAbstractAnalyzer.setAnalyzer(executor, image -> analyzer.analyze(image));
            if (mSubscribedAnalyzer == null) {
                notifyActive();
            }
            mSubscribedAnalyzer = analyzer;
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        super.setViewPortCropRect(viewPortCropRect);
        mImageAnalysisAbstractAnalyzer.setViewPortCropRect(viewPortCropRect);
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void setSensorToBufferTransformMatrix(@NonNull Matrix matrix) {
        super.setSensorToBufferTransformMatrix(matrix);
        mImageAnalysisAbstractAnalyzer.setSensorToBufferTransformMatrix(matrix);
    }

    private boolean isFlipWH(@NonNull CameraInternal cameraInternal) {
        return isOutputImageRotationEnabled()
                ? ((getRelativeRotation(cameraInternal) % 180) != 0) : false;
    }

    /**
     * Returns the mode with which images are acquired from the {@linkplain ImageReader image
     * producer}.
     *
     * <p>
     * The backpressure strategy is set when constructing an {@link ImageAnalysis} instance using
     * {@link ImageAnalysis.Builder#setBackpressureStrategy(int)}. If not set, it defaults to
     * {@link ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST}.
     * </p>
     *
     * @return The backpressure strategy applied to the image producer.
     * @see ImageAnalysis.Builder#setBackpressureStrategy(int)
     */
    @BackpressureStrategy
    public int getBackpressureStrategy() {
        return ((ImageAnalysisConfig) getCurrentConfig()).getBackpressureStrategy(
                DEFAULT_BACKPRESSURE_STRATEGY);
    }

    /**
     * Returns the executor that will be used for background tasks.
     *
     * @return The {@link Executor} provided to
     * {@link ImageAnalysis.Builder#setBackgroundExecutor(Executor)}.
     * If no Executor has been provided, then returns {@code null}
     */
    @Nullable
    @ExperimentalUseCaseApi
    public Executor getBackgroundExecutor() {
        return ((ImageAnalysisConfig) getCurrentConfig())
                .getBackgroundExecutor(null);
    }

    /**
     * Returns the number of images available to the camera pipeline, including the image being
     * analyzed, for the {@link #STRATEGY_BLOCK_PRODUCER} backpressure mode.
     *
     * <p>
     * The image queue depth is set when constructing an {@link ImageAnalysis} instance using
     * {@link ImageAnalysis.Builder#setImageQueueDepth(int)}. If not set, and this option is used
     * by the backpressure strategy, the default will be a queue depth of 6 images.
     * </p>
     *
     * @return The image queue depth for the {@link #STRATEGY_BLOCK_PRODUCER} backpressure mode.
     * @see ImageAnalysis.Builder#setImageQueueDepth(int)
     * @see ImageAnalysis.Builder#setBackpressureStrategy(int)
     */
    public int getImageQueueDepth() {
        return ((ImageAnalysisConfig) getCurrentConfig()).getImageQueueDepth(
                DEFAULT_IMAGE_QUEUE_DEPTH);
    }

    /**
     * Gets output image format.
     *
     * <p>The returned image format will be
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_YUV_420_888} or
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_RGBA_8888}.
     *
     * @return output image format.
     * @see ImageAnalysis.Builder#setOutputImageFormat(int)
     */
    @ImageAnalysis.OutputImageFormat
    public int getOutputImageFormat() {
        return ((ImageAnalysisConfig) getCurrentConfig()).getOutputImageFormat(
                DEFAULT_OUTPUT_IMAGE_FORMAT);
    }

    /**
     * Checks if output image rotation is enabled. It returns false by default.
     *
     * @return true if enabled, false otherwise.
     * @see ImageAnalysis.Builder#setOutputImageRotationEnabled(boolean)
     */
    public boolean isOutputImageRotationEnabled() {
        return ((ImageAnalysisConfig) getCurrentConfig()).isOutputImageRotationEnabled(
                DEFAULT_OUTPUT_IMAGE_ROTATION_ENABLED);
    }

    /**
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Boolean getOnePixelShiftEnabled() {
        return ((ImageAnalysisConfig) getCurrentConfig()).getOnePixelShiftEnabled(
                DEFAULT_ONE_PIXEL_SHIFT_ENABLED);
    }

    /**
     * Gets resolution related information of the {@link ImageAnalysis}.
     *
     * <p>The returned {@link ResolutionInfo} will be expressed in the coordinates of the camera
     * sensor. It will be the same as the resolution of the {@link ImageProxy} received from
     * {@link ImageAnalysis.Analyzer#analyze}.
     *
     * <p>The resolution information might change if the use case is unbound and then rebound or
     * {@link #setTargetRotation(int)} is called to change the target rotation setting. The
     * application needs to call {@link #getResolutionInfo()} again to get the latest
     * {@link ResolutionInfo} for the changes.
     *
     * @return the resolution information if the use case has been bound by the
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner,
     * CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     */
    @Nullable
    public ResolutionInfo getResolutionInfo() {
        return getResolutionInfoInternal();
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

    @Override
    @NonNull
    public String toString() {
        return TAG + ":" + getName();
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onUnbind() {
        clearPipeline();
        mImageAnalysisAbstractAnalyzer.detach();
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config captureConfig = factory.getConfig(
                UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS,
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onBind() {
        mImageAnalysisAbstractAnalyzer.attach();
    }

    /**
     * {@inheritDoc}
     *
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return ImageAnalysis.Builder.fromConfig(config);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected StreamSpec onSuggestedStreamSpecUpdated(@NonNull StreamSpec suggestedStreamSpec) {
        final ImageAnalysisConfig config = (ImageAnalysisConfig) getCurrentConfig();

        SessionConfig.Builder sessionConfigBuilder = createPipeline(getCameraId(), config,
                suggestedStreamSpec);
        updateSessionConfig(sessionConfigBuilder.build());

        return suggestedStreamSpec;
    }

    /**
     * Updates relative rotation if attached to a camera. No-op otherwise.
     */
    private void tryUpdateRelativeRotation() {
        CameraInternal cameraInternal = getCamera();
        if (cameraInternal != null) {
            mImageAnalysisAbstractAnalyzer.setRelativeRotation(getRelativeRotation(cameraInternal));
        }
    }

    /**
     * How to apply backpressure to the source producing images for analysis.
     *
     * <p>Sometimes, images may be produced faster than they can be analyzed. Since images
     * generally reserve a large portion of the device's memory, they cannot be buffered
     * unbounded and indefinitely. The backpressure strategy defines how to deal with this scenario.
     *
     * <p>The receiver of the {@link ImageProxy} is responsible for explicitly closing the image
     * by calling {@link ImageProxy#close()}. However, the image will only be valid when the
     * ImageAnalysis instance is bound to a camera.
     *
     * @see Builder#setBackpressureStrategy(int)
     */
    @IntDef({STRATEGY_KEEP_ONLY_LATEST, STRATEGY_BLOCK_PRODUCER})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @interface BackpressureStrategy {
    }

    /**
     * Supported output image format for image analysis.
     *
     * <p>The supported output image format
     * is {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_YUV_420_888} and
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_RGBA_8888}.
     *
     * <p>By default, {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_YUV_420_888} will be used.
     *
     * @see Builder#setOutputImageFormat(int)
     */
    @IntDef({OUTPUT_IMAGE_FORMAT_YUV_420_888, OUTPUT_IMAGE_FORMAT_RGBA_8888})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @interface OutputImageFormat {
    }

    /**
     * Interface for analyzing images.
     *
     * <p>Implement Analyzer and pass it to {@link ImageAnalysis#setAnalyzer(Executor, Analyzer)}
     * to receive images and perform custom processing by implementing the
     * {@link ImageAnalysis.Analyzer#analyze(ImageProxy)} function.
     */
    public interface Analyzer {
        /**
         * Analyzes an image to produce a result.
         *
         * <p>This method is called once for each image from the camera, and called at the
         * frame rate of the camera. Each analyze call is executed sequentially.
         *
         * <p>It is the responsibility of the application to close the image once done with it.
         * If the images are not closed then it may block further images from being produced
         * (causing the preview to stall) or drop images as determined by the configured
         * backpressure strategy. The exact behavior is configurable via
         * {@link ImageAnalysis.Builder#setBackpressureStrategy(int)}.
         *
         * <p>Images produced here will no longer be valid after the {@link ImageAnalysis}
         * instance that produced it has been unbound from the camera.
         *
         * <p>The image provided has format {@link android.graphics.ImageFormat#YUV_420_888}.
         *
         * <p>The provided image is typically in the orientation of the sensor, meaning CameraX
         * does not perform an internal rotation of the data.  The rotationDegrees parameter allows
         * the analysis to understand the image orientation when processing or to apply a rotation.
         * For example, if the
         * {@linkplain ImageAnalysis#setTargetRotation(int) target rotation}) is natural
         * orientation, rotationDegrees would be the rotation which would align the buffer
         * data ordering to natural orientation.
         *
         * <p>Timestamps are in nanoseconds and monotonic and can be compared to timestamps from
         * images produced from UseCases bound to the same camera instance.  More detail is
         * available depending on the implementation.  For example with CameraX using a
         * {@link androidx.camera.camera2} implementation additional detail can be found in
         * {@link android.hardware.camera2.CameraDevice} documentation.
         *
         * @param image The image to analyze
         * @see android.media.Image#getTimestamp()
         * @see android.hardware.camera2.CaptureResult#SENSOR_TIMESTAMP
         */
        void analyze(@NonNull ImageProxy image);

        /**
         * Implement this method to set a default target resolution for the {@link ImageAnalysis}.
         *
         * <p> Implement this method if the {@link Analyzer} requires a specific resolution to
         * work. The return value will be used as the default target resolution for the
         * {@link ImageAnalysis}. Return {@code null} if no falling back is needed. By default,
         * this method returns {@code null}.
         *
         * <p> If the app does not set a target resolution for {@link ImageAnalysis}, then this
         * value will be used as the target resolution. If the {@link ImageAnalysis} has set a
         * target resolution, e.g. if {@link ImageAnalysis.Builder#setTargetResolution(Size)} is
         * called, then the {@link ImageAnalysis} will use the app value over this value.
         *
         * <p> Note that this method is invoked by CameraX at the time of binding to lifecycle. In
         * order for this value to be effective, the {@link Analyzer} has to be set before
         * {@link ImageAnalysis} is bound to a lifecycle. Otherwise, the value will be ignored.
         *
         * @return the default resolution of {@link ImageAnalysis}, or {@code null} if no specific
         * resolution is needed.
         */
        @Nullable
        default Size getDefaultTargetResolution() {
            return null;
        }

        /**
         * Implement this method to return the target coordinate system.
         *
         * <p>The coordinates detected by analyzing camera frame usually needs to be transformed.
         * For example, in order to highlight a detected face, the app needs to transform the
         * bounding box from the {@link ImageAnalysis}'s coordinate system to the View's coordinate
         * system. This method allows the implementer to set a target coordinate system.
         *
         * <p>The value will be used by CameraX to calculate the transformation {@link Matrix} and
         * forward it to the {@link Analyzer} via {@link #updateTransform}. By default, this
         * method returns {@link ImageAnalysis#COORDINATE_SYSTEM_ORIGINAL}.
         *
         * <p>For now, camera-core only supports {@link ImageAnalysis#COORDINATE_SYSTEM_ORIGINAL},
         * please see libraries derived from camera-core, for example, camera-view.
         *
         * @see #updateTransform(Matrix)
         */
        default int getTargetCoordinateSystem() {
            return COORDINATE_SYSTEM_ORIGINAL;
        }

        /**
         * Implement this method to receive the {@link Matrix} for coordinate transformation.
         *
         * <p>The value represents the transformation from the camera sensor to the target
         * coordinate system defined in {@link #getTargetCoordinateSystem()}. It should be used
         * by the implementation to transform the coordinates detected in the camera frame. For
         * example, the coordinates of the detected face.
         *
         * <p>If the value is {@code null}, it means that no valid transformation is available.
         * It could have different causes depending on the value of
         * {@link #getTargetCoordinateSystem()}:
         * <ul>
         *     <li> If the target coordinate system is {@link #COORDINATE_SYSTEM_ORIGINAL}, it is
         *     always invalid because in that case, the coordinate system depends on how the
         *     analysis algorithm processes the {@link ImageProxy}.
         *     <li> It is also invalid if the target coordinate system is not available, for example
         *     if the analyzer targets the viewfinder and the view finder is not visible in UI.
         * </ul>
         *
         * <p>This method is invoked whenever a new transformation is ready. For example, when
         * the view finder is first a launched as well as when it's resized.
         *
         * @see #getTargetCoordinateSystem()
         */
        default void updateTransform(@Nullable Matrix matrix) {
            // no-op
        }
    }

    /**
     * {@link ImageAnalysis.Analyzer} option for returning the original coordinates.
     *
     * <p> Use this option if no additional transformation is needed by the {@link Analyzer}
     * implementation. By using this option, CameraX will pass {@code null} to
     * {@link Analyzer#updateTransform(Matrix)}.
     */
    public static final int COORDINATE_SYSTEM_ORIGINAL = 0;

    /**
     * Provides a base static default configuration for the ImageAnalysis.
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<ImageAnalysisConfig> {
        private static final Size DEFAULT_TARGET_RESOLUTION = new Size(640, 480);
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 1;
        private static final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;

        private static final ResolutionSelector DEFAULT_RESOLUTION_SELECTOR =
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).setResolutionStrategy(
                        new ResolutionStrategy(SizeUtil.RESOLUTION_VGA,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

        private static final ImageAnalysisConfig DEFAULT_CONFIG;

        static {
            Builder builder = new Builder()
                    .setDefaultResolution(DEFAULT_TARGET_RESOLUTION)
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setTargetAspectRatio(DEFAULT_ASPECT_RATIO)
                    .setResolutionSelector(DEFAULT_RESOLUTION_SELECTOR);

            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public ImageAnalysisConfig getConfig() {
            return DEFAULT_CONFIG;
        }
    }

    /** Builder for a {@link ImageAnalysis}. */
    @SuppressWarnings("ObjectToString")
    public static final class Builder
            implements ImageOutputConfig.Builder<Builder>,
            ThreadConfig.Builder<Builder>,
            UseCaseConfig.Builder<ImageAnalysis, ImageAnalysisConfig, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageAnalysis.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageAnalysis.class);
        }

        /**
         * Generates a Builder from another Config object.
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static Builder fromConfig(@NonNull Config configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Generates a Builder from another Config object.
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromConfig(@NonNull ImageAnalysisConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Sets the backpressure strategy to apply to the image producer to deal with scenarios
         * where images may be produced faster than they can be analyzed.
         *
         * <p>The available values are {@link #STRATEGY_BLOCK_PRODUCER} and
         * {@link #STRATEGY_KEEP_ONLY_LATEST}.
         *
         * <p>If not set, the backpressure strategy will default to
         * {@link #STRATEGY_KEEP_ONLY_LATEST}.
         *
         * @param strategy The strategy to use.
         * @return The current Builder.
         */
        @NonNull
        public Builder setBackpressureStrategy(@BackpressureStrategy int strategy) {
            getMutableConfig().insertOption(OPTION_BACKPRESSURE_STRATEGY, strategy);
            return this;
        }

        /**
         * Sets the number of images available to the camera pipeline for
         * {@link #STRATEGY_BLOCK_PRODUCER} mode.
         *
         * <p>The image queue depth is the number of images available to the camera to fill with
         * data. This includes the image currently being analyzed by {@link
         * ImageAnalysis.Analyzer#analyze(ImageProxy)}. Increasing the image queue depth
         * may make camera operation smoother, depending on the backpressure strategy, at
         * the cost of increased memory usage.
         *
         * <p>When the backpressure strategy is set to {@link #STRATEGY_BLOCK_PRODUCER},
         * increasing the image queue depth may make the camera pipeline run smoother on systems
         * under high load. However, the time spent analyzing an image should still be kept under
         * a single frame period for the current frame rate, <i>on average</i>, to avoid stalling
         * the camera pipeline.
         *
         * <p>The value only applies to {@link #STRATEGY_BLOCK_PRODUCER} mode.
         * For {@link #STRATEGY_KEEP_ONLY_LATEST} the value is ignored.
         *
         * <p>If not set, and this option is used by the selected backpressure strategy,
         * the default will be a queue depth of 6 images.
         *
         * @param depth The total number of images available to the camera.
         * @return The current Builder.
         */
        @NonNull
        public Builder setImageQueueDepth(int depth) {
            getMutableConfig().insertOption(OPTION_IMAGE_QUEUE_DEPTH, depth);
            return this;
        }

        /**
         * Sets output image format.
         *
         * <p>The supported output image format
         * is {@link OutputImageFormat#OUTPUT_IMAGE_FORMAT_YUV_420_888} and
         * {@link OutputImageFormat#OUTPUT_IMAGE_FORMAT_RGBA_8888}.
         *
         * <p>If not set, {@link OutputImageFormat#OUTPUT_IMAGE_FORMAT_YUV_420_888} will be used.
         *
         * Requesting {@link OutputImageFormat#OUTPUT_IMAGE_FORMAT_RGBA_8888} will have extra
         * overhead because format conversion takes time.
         *
         * @param outputImageFormat The output image format.
         * @return The current Builder.
         */
        @NonNull
        public Builder setOutputImageFormat(@OutputImageFormat int outputImageFormat) {
            getMutableConfig().insertOption(OPTION_OUTPUT_IMAGE_FORMAT, outputImageFormat);
            return this;
        }

        /**
         * Enable or disable output image rotation.
         *
         * <p>On API 22 and below, this API has no effect. User needs to handle the image rotation
         * based on the {@link ImageInfo#getRotationDegrees()}.
         *
         * <p>{@link ImageAnalysis#setTargetRotation(int)} is to adjust the rotation
         * degree information returned by {@link ImageInfo#getRotationDegrees()} based on
         * sensor rotation and user still needs to rotate the output image to achieve the target
         * rotation. Once this is enabled, user doesn't need to handle the rotation, the output
         * image will be a rotated {@link ImageProxy} and {@link ImageInfo#getRotationDegrees()}
         * will return 0.
         *
         * <p>Turning this on will add more processing overhead to every image analysis
         * frame. The average processing time is about 10-15ms for 640x480 image on a mid-range
         * device.
         *
         * By default, the rotation is disabled.
         *
         * @param outputImageRotationEnabled flag to enable or disable.
         * @return The current Builder.
         *
         * @see
         * <a href="https://developer.android.com/training/camerax/orientation-rotation#imageanalysis">ImageAnalysis</a>
         */
        @RequiresApi(23)
        @NonNull
        public Builder setOutputImageRotationEnabled(boolean outputImageRotationEnabled) {
            getMutableConfig().insertOption(OPTION_OUTPUT_IMAGE_ROTATION_ENABLED,
                    outputImageRotationEnabled);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setOnePixelShiftEnabled(boolean onePixelShiftEnabled) {
            getMutableConfig().insertOption(OPTION_ONE_PIXEL_SHIFT_ENABLED,
                    Boolean.valueOf(onePixelShiftEnabled));
            return this;
        }

        /**
         * {@inheritDoc}
         *
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
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public ImageAnalysisConfig getUseCaseConfig() {
            return new ImageAnalysisConfig(OptionsBundle.from(mMutableConfig));
        }

        /**
         * Builds an {@link ImageAnalysis} from the current state.
         *
         * @return A {@link ImageAnalysis} populated with the current state.
         * @throws IllegalArgumentException if attempting to set both target aspect ratio and
         *                                  target resolution.
         */
        @Override
        @NonNull
        public ImageAnalysis build() {
            ImageAnalysisConfig imageAnalysisConfig = getUseCaseConfig();
            ImageOutputConfig.validateConfig(imageAnalysisConfig);
            return new ImageAnalysis(imageAnalysisConfig);
        }

        // Implementations of TargetConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<ImageAnalysis> targetClass) {
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
         * @param aspectRatio The desired ImageAnalysis {@link AspectRatio}
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
         * <p>This adjust the {@link ImageInfo#getRotationDegrees()} of the {@link ImageProxy}
         * passed to {@link Analyzer#analyze(ImageProxy)}. The rotation value of ImageInfo will
         * be the rotation, which if applied to the output image, will make the image match
         * target rotation specified here.
         *
         * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link
         * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
         *
         * <p>In general, it is best to additionally set the target rotation dynamically on the use
         * case.  See
         * {@link androidx.camera.core.ImageAnalysis#setTargetRotationDegrees(int)} for additional
         * documentation.
         *
         * <p>If not set, the target rotation will default to the value of
         * {@link android.view.Display#getRotation()} of the default display at the time the
         * use case is created. The use case is fully created once it has been attached to a camera.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see androidx.camera.core.ImageAnalysis#setTargetRotation(int)
         * @see androidx.camera.core.ImageAnalysis#setTargetRotationDegrees(int)
         * @see android.view.OrientationEventListener
         */
        @NonNull
        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * setMirrorMode is not supported on ImageAnalysis.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            throw new UnsupportedOperationException("setMirrorMode is not supported.");
        }

        /**
         * Sets the resolution of the intended target from this configuration.
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
         * <p>If not set, resolution of 640x480 will be selected to use in priority.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, which resolution
         * will be finally selected will depend on the camera device's hardware level and the
         * bound use cases combination. The device hardware level information can be retrieved by
         * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL}
         * from the interop class
         * {@link androidx.camera.camera2.interop.Camera2CameraInfo#getCameraCharacteristic(CameraCharacteristics.Key)}.
         * A <code>LIMITED-level</code> above device can support a <code>RECORD</code> size
         * resolution for {@link ImageAnalysis} when it is bound together with {@link Preview}
         * and {@link ImageCapture}. The trade-off is the selected resolution for the
         * {@link ImageCapture} will also be restricted by the <code>RECORD</code> size. To
         * successfully select a <code>RECORD</code> size resolution for {@link ImageAnalysis}, a
         * <code>RECORD</code> size target resolution should be set on both {@link ImageCapture}
         * and {@link ImageAnalysis}. This indicates that the application clearly understand the
         * trade-off and prefer the {@link ImageAnalysis} to have a larger resolution rather than
         * the {@link ImageCapture} to have a <code>MAXIMUM</code> size resolution. For the
         * definitions of <code>RECORD</code>, <code>MAXIMUM</code> sizes and more details see the
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a>
         * section in {@link android.hardware.camera2.CameraDevice}'s. The <code>RECORD</code>
         * size refers to the camera device's maximum supported recording resolution, as
         * determined by {@link CamcorderProfile}. The <code>MAXIMUM</code> size refers to the
         * camera device's maximum output resolution for that format or target from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes}.
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
            getMutableConfig()
                    .insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
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
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION,
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

        /**
         * Sets the resolution selector to select the preferred supported resolution.
         *
         * <p>ImageAnalysis has a default {@link ResolutionStrategy} with bound size as 640x480
         * and fallback rule of {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER}.
         * Applications can override this default strategy with a different resolution strategy.
         *
         * <p>When using the {@code camera-camera2} CameraX implementation, which resolution is
         * finally selected depends on the camera device's hardware level, capabilities and the
         * bound use cases combination. The device hardware level and capabilities information
         * can be retrieved via the interop class
         * {@link androidx.camera.camera2.interop.Camera2CameraInfo#getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.Key)}
         * with
         * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} and
         * {@link android.hardware.camera2.CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES}.
         *
         * <p>A {@code LIMITED-level} above device can support a {@code RECORD} size resolution
         * for {@link ImageAnalysis} when it is bound together with {@link Preview} and
         * {@link ImageCapture}. The trade-off is the selected resolution for the
         * {@link ImageCapture} is also restricted by the {@code RECORD} size. To successfully
         * select a {@code RECORD} size resolution for {@link ImageAnalysis}, a
         * {@link ResolutionStrategy} of selecting {@code RECORD} size resolution should be set
         * on both {@link ImageCapture} and {@link ImageAnalysis}. This indicates that the
         * application clearly understand the trade-off and prefer the {@link ImageAnalysis} to
         * have a larger resolution rather than the {@link ImageCapture} to have a {@code MAXIMUM
         * } size resolution. For the definitions of {@code RECORD}, {@code MAXIMUM} sizes and
         * more details see the
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a>
         * section in {@link android.hardware.camera2.CameraDevice}'s. The {@code RECORD} size
         * refers to the camera device's maximum supported recording resolution, as determined by
         * {@link CamcorderProfile}. The {@code MAXIMUM} size refers to the camera device's
         * maximum output resolution for that format or target from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes}.
         *
         * <p>The existing {@link #setTargetResolution(Size)} and
         * {@link #setTargetAspectRatio(int)} APIs are deprecated and are not compatible with
         * {@link #setResolutionSelector(ResolutionSelector)}. Calling either of these APIs
         * together with {@link #setResolutionSelector(ResolutionSelector)} will result in an
         * {@link IllegalArgumentException} being thrown when you attempt to build the
         * {@link ImageAnalysis} instance.
         *
         * @return The current Builder.
         */
        @Override
        @NonNull
        public Builder setResolutionSelector(@NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
            return this;
        }

        // Implementations of ThreadConfig.Builder default methods

        /**
         * Sets the default executor that will be used for background tasks.
         *
         * <p>If not set, the background executor will default to an automatically generated
         * {@link Executor}.
         *
         * @param executor The executor which will be used for background tasks.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setBackgroundExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_BACKGROUND_EXECUTOR, executor);
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
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @RestrictTo(Scope.LIBRARY)
        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
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

        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setImageReaderProxyProvider(
                @NonNull ImageReaderProxyProvider imageReaderProxyProvider) {
            getMutableConfig().insertOption(OPTION_IMAGE_READER_PROXY_PROVIDER,
                    imageReaderProxyProvider);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }
    }
}
