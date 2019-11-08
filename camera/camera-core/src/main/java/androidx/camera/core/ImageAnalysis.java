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

import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A use case providing CPU accessible images for an app to perform image analysis on.
 *
 * <p>ImageAnalysis acquires images from the camera via an {@link ImageReader}. Each image
 * is provided to an {@link ImageAnalysis.Analyzer} function which can be implemented by application
 * code, where it can access image data for application analysis via an {@link ImageProxy}.
 *
 * <p>After the analyzer function returns, the {@link ImageProxy} will be closed and the
 * corresponding {@link android.media.Image} is released back to the {@link ImageReader}.
 */
public final class ImageAnalysis extends UseCase {
    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "ImageAnalysis";
    // ImageReader depth for KEEP_ONLY_LATEST mode.
    private static final int NON_BLOCKING_IMAGE_DEPTH = 4;

    private final ImageAnalysisConfig.Builder mUseCaseConfigBuilder;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ImageAnalysisAbstractAnalyzer mImageAnalysisAbstractAnalyzer;
    @GuardedBy("mAnalysisLock")
    private ImageAnalysis.Analyzer mSubscribedAnalyzer;

    @Nullable
    ImageReaderProxy mImageReader;
    @Nullable
    private DeferrableSurface mDeferrableSurface;

    private final Object mAnalysisLock = new Object();

    /**
     * Creates a new image analysis use case from the given configuration.
     *
     * @param config for this use case instance
     */
    public ImageAnalysis(@NonNull ImageAnalysisConfig config) {
        super(config);
        mUseCaseConfigBuilder = ImageAnalysisConfig.Builder.fromConfig(config);

        // Get the combined configuration with defaults
        ImageAnalysisConfig combinedConfig = (ImageAnalysisConfig) getUseCaseConfig();
        setImageFormat(ImageReaderFormatRecommender.chooseCombo().imageAnalysisFormat());

        if (combinedConfig.getBackpressureStrategy() == BackpressureStrategy.BLOCK_PRODUCER) {
            mImageAnalysisAbstractAnalyzer = new ImageAnalysisBlockingAnalyzer();
        } else {
            mImageAnalysisAbstractAnalyzer = new ImageAnalysisNonBlockingAnalyzer(
                    config.getBackgroundExecutor(CameraXExecutors.highPriorityExecutor()));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SessionConfig.Builder createPipeline(@NonNull String cameraId,
            @NonNull ImageAnalysisConfig config, @NonNull Size resolution) {
        Threads.checkMainThread();

        Executor backgroundExecutor = config.getBackgroundExecutor(
                CameraXExecutors.highPriorityExecutor());

        int imageQueueDepth =
                config.getBackpressureStrategy() == BackpressureStrategy.BLOCK_PRODUCER
                        ? config.getImageQueueDepth() : NON_BLOCKING_IMAGE_DEPTH;

        mImageReader =
                ImageReaderProxys.createCompatibleReader(
                        CameraX.getSurfaceManager(),
                        cameraId,
                        resolution.getWidth(),
                        resolution.getHeight(),
                        getImageFormat(),
                        imageQueueDepth,
                        backgroundExecutor);

        tryUpdateRelativeRotation(cameraId);

        mImageAnalysisAbstractAnalyzer.open();
        mImageReader.setOnImageAvailableListener(mImageAnalysisAbstractAnalyzer,
                backgroundExecutor);

        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);

        mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());

        sessionConfigBuilder.addSurface(mDeferrableSurface);

        sessionConfigBuilder.addErrorListener(new SessionConfig.ErrorListener() {
            @Override
            public void onError(@NonNull SessionConfig sessionConfig,
                    @NonNull SessionConfig.SessionError error) {
                clearPipeline();

                // Ensure the bound camera has not changed before resetting.
                // TODO(b/143915543): Ensure this never gets called by a camera that is not bound
                //  to this use case so we don't need to do this check.
                if (isCurrentlyBoundCamera(cameraId)) {
                    SessionConfig.Builder sessionConfigBuilder = createPipeline(cameraId, config,
                            resolution);
                    attachToCamera(cameraId, sessionConfigBuilder.build());

                    notifyReset();
                }
            }
        });

        return sessionConfigBuilder;
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    void clearPipeline() {
        Threads.checkMainThread();
        mImageAnalysisAbstractAnalyzer.close();

        final DeferrableSurface deferrableSurface = mDeferrableSurface;
        mDeferrableSurface = null;
        final ImageReaderProxy imageReaderProxy = mImageReader;
        mImageReader = null;
        if (deferrableSurface != null) {
            deferrableSurface.setOnSurfaceDetachedListener(
                    CameraXExecutors.mainThreadExecutor(),
                    new DeferrableSurface.OnSurfaceDetachedListener() {
                        @Override
                        public void onSurfaceDetached() {
                            if (imageReaderProxy != null) {
                                imageReaderProxy.close();
                            }
                        }
                    });
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
     * Sets the target rotation.
     *
     * <p>This informs the use case so it can adjust the rotation value sent to
     * {@link Analyzer#analyze(ImageProxy, int)}.
     *
     * <p>In most cases this should be set to the current rotation returned by {@link
     * Display#getRotation()}.  In that case, the rotation parameter sent to the analyzer will be
     * the rotation, which if applied to the output image, will make the image match the display
     * orientation.
     *
     * <p>While rotation can also be set via
     * {@link ImageAnalysisConfig.Builder#setTargetRotation(int)}, using
     * {@link ImageAnalysis#setTargetRotation(int)} allows the target rotation to be set
     * dynamically. This can be useful if an app locks itself to portrait, and uses the orientation
     * sensor to set rotation, to process landscape images when the device is rotated by examining
     * the rotation received by the Analyzer function.
     *
     * <p>If not set here or by configuration, the target rotation will default to the value of
     * {@link Display#getRotation()} of the default display at the time the
     * use case is created.
     *
     * @param rotation Target rotation of the output image, expressed as one of
     *                 {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                 {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        ImageAnalysisConfig oldConfig = (ImageAnalysisConfig) getUseCaseConfig();
        int oldRotation = oldConfig.getTargetRotation(ImageOutputConfig.INVALID_ROTATION);
        if (oldRotation == ImageOutputConfig.INVALID_ROTATION || oldRotation != rotation) {
            mUseCaseConfigBuilder.setTargetRotation(rotation);
            updateUseCaseConfig(mUseCaseConfigBuilder.build());

            // TODO(b/122846516): Update session configuration and possibly reconfigure session.
            // For now we'll just update the relative rotation value.
            // Attempt to get the camera ID and update the relative rotation. If we can't, we
            // probably
            // don't yet have permission, so we will try again in onSuggestedResolutionUpdated().
            // Old
            // configuration lens facing should match new configuration.
            try {
                String cameraId = CameraX.getCameraWithCameraDeviceConfig(oldConfig);
                tryUpdateRelativeRotation(cameraId);
            } catch (CameraInfoUnavailableException e) {
                // Likely don't yet have permissions. This is expected if this method is called
                // before
                // this use case becomes active. That's OK though since we've updated the use case
                // configuration. We'll try to update relative rotation again in
                // onSuggestedResolutionUpdated().
                Log.w(TAG, "Unable to get camera id for the camera device config.");
            }
        }
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
     *                 {@link ImageAnalysis.Analyzer#analyze(ImageProxy, int)} will be run.
     * @param analyzer of the images.
     */
    public void setAnalyzer(@NonNull Executor executor, @NonNull Analyzer analyzer) {
        synchronized (mAnalysisLock) {
            mImageAnalysisAbstractAnalyzer.setAnalyzer(executor, analyzer);
            if (mSubscribedAnalyzer == null) {
                notifyActive();
            }
            mSubscribedAnalyzer = analyzer;
        }
    }

    @Override
    @NonNull
    public String toString() {
        return TAG + ":" + getName();
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
        super.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(LensFacing lensFacing) {
        ImageAnalysisConfig defaults = CameraX.getDefaultUseCaseConfig(
                ImageAnalysisConfig.class, lensFacing);
        if (defaults != null) {
            return ImageAnalysisConfig.Builder.fromConfig(defaults);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected Map<String, Size> onSuggestedResolutionUpdated(
            @NonNull Map<String, Size> suggestedResolutionMap) {
        final ImageAnalysisConfig config = (ImageAnalysisConfig) getUseCaseConfig();

        String cameraId = getBoundCameraId();

        Size resolution = suggestedResolutionMap.get(cameraId);
        if (resolution == null) {
            throw new IllegalArgumentException(
                    "Suggested resolution map missing resolution for camera " + cameraId);
        }

        if (mImageReader != null) {
            mImageReader.close();
        }

        SessionConfig.Builder sessionConfigBuilder = createPipeline(cameraId, config, resolution);
        attachToCamera(cameraId, sessionConfigBuilder.build());

        return suggestedResolutionMap;
    }

    private void tryUpdateRelativeRotation(String cameraId) {
        ImageOutputConfig config = (ImageOutputConfig) getUseCaseConfig();
        // Get the relative rotation or default to 0 if the camera info is unavailable
        try {
            CameraInfoInternal cameraInfoInternal = CameraX.getCameraInfo(cameraId);
            mImageAnalysisAbstractAnalyzer.setRelativeRotation(
                    cameraInfoInternal.getSensorRotationDegrees(
                            config.getTargetRotation(Surface.ROTATION_0)));
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to retrieve camera sensor orientation.", e);
        }
    }

    /**
     * How to apply backpressure to the source producing images for analysis.
     *
     * <p>Sometimes, images may be produced faster than they can be analyzed. Since images
     * generally reserve a large portion of the device's memory, they cannot be buffered
     * unbounded and indefinitely. The backpressure strategy defines how to deal with this scenario.
     *
     * @see ImageAnalysisConfig.Builder#setBackpressureStrategy(int)
     */
    @IntDef({BackpressureStrategy.KEEP_ONLY_LATEST, BackpressureStrategy.BLOCK_PRODUCER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BackpressureStrategy {
        /**
         * Only deliver the latest image to the analyzer, dropping images as they arrive.
         *
         * <p>This strategy ignores the value set by
         * {@link ImageAnalysisConfig.Builder#setImageQueueDepth(int)}. Only one image will be
         * delivered for analysis at a time. If more images are produced while that image is
         * being analyzed, they will be dropped and not queued for delivery. Once the image being
         * analyzed is closed, the next latest image will be delivered.
         *
         * <p>Internally this strategy may make use of an internal {@link Executor} to receive
         * and drop images from the producer. A performance-tuned executor will be created
         * internally unless one is explicitly provided by
         * {@link ImageAnalysisConfig.Builder#setBackgroundExecutor(Executor)}. In order to
         * ensure smooth operation of this backpressure strategy, any user supplied
         * {@link Executor} must be able to quickly respond to tasks posted to it, so setting
         * the executor manually should only be considered in advanced use cases.
         *
         * @see ImageAnalysisConfig.Builder#setBackgroundExecutor(Executor)
         */
        int KEEP_ONLY_LATEST = 0;
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
         * {@link ImageAnalysisConfig.Builder#setImageQueueDepth(int)}.
         *
         * @see ImageAnalysisConfig.Builder#setImageQueueDepth(int)
         */
        int BLOCK_PRODUCER = 1;
    }

    /**
     * Interface for analyzing images.
     *
     * <p>Implement Analyzer and pass it to {@link ImageAnalysis#setAnalyzer(Executor, Analyzer)}
     * to receive images and perform custom processing by implementing the
     * {@link ImageAnalysis.Analyzer#analyze(ImageProxy, int)} function.
     */
    public interface Analyzer {
        /**
         * Analyzes an image to produce a result.
         *
         * <p>This method is called once for each image from the camera, and called at the
         * frame rate of the camera.  Each analyze call is executed sequentially.
         *
         * <p>The image passed to this method becomes invalid and is closed after this method
         * returns. The implementation should not close, nor store external references to this
         * image, as these references will become invalid.
         *
         * <p>When processing takes longer than a single frame of latency, the
         * {@link ImageAnalysis.BackpressureStrategy} will determine the next image delivered and
         * stalling behavior.
         *
         * <p>Applications can "skip" analyzing a frame by having the analyzer return immediately.
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
         * @param image           The image to analyze
         * @param rotationDegrees The rotation which if applied to the image would make it match
         *                        the current target rotation of {@link ImageAnalysis}, expressed in
         *                        degrees in the range {@code [0..360)}.
         */
        void analyze(@NonNull ImageProxy image, int rotationDegrees);
    }

    /**
     * Provides a base static default configuration for the ImageAnalysis
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<ImageAnalysisConfig> {
        @BackpressureStrategy
        private static final int DEFAULT_BACKPRESSURE_STRATEGY =
                BackpressureStrategy.KEEP_ONLY_LATEST;
        private static final int DEFAULT_IMAGE_QUEUE_DEPTH = 6;
        private static final Size DEFAULT_TARGET_RESOLUTION = new Size(640, 480);
        private static final Size DEFAULT_MAX_RESOLUTION = new Size(1920, 1080);
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 1;

        private static final ImageAnalysisConfig DEFAULT_CONFIG;

        static {
            ImageAnalysisConfig.Builder builder =
                    new ImageAnalysisConfig.Builder()
                            .setBackpressureStrategy(DEFAULT_BACKPRESSURE_STRATEGY)
                            .setImageQueueDepth(DEFAULT_IMAGE_QUEUE_DEPTH)
                            .setDefaultResolution(DEFAULT_TARGET_RESOLUTION)
                            .setMaxResolution(DEFAULT_MAX_RESOLUTION)
                            .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY);

            DEFAULT_CONFIG = builder.build();
        }

        @Override
        public ImageAnalysisConfig getConfig(LensFacing lensFacing) {
            return DEFAULT_CONFIG;
        }
    }
}
