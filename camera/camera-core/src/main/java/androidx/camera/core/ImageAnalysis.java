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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A use case providing CPU accessible images for an app to perform image analysis on.
 *
 * <p>ImageAnalysis acquires images from the camera via an {@link ImageReader}. Each image
 * is provided to an {@link ImageAnalysis.Analyzer} function which can be implemented by application
 * code, where it can access image data for application analysis via an {@link ImageProxy}.
 *
 * <p>After the analyzer function returns, the {@link ImageProxy} will be closed and the
 * corresponding {@link android.media.Image} is released back to the {@link ImageReader}.
 *
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
    final AtomicReference<Analyzer> mSubscribedAnalyzer;
    final AtomicInteger mRelativeRotation = new AtomicInteger();
    private final Handler mHandler;
    private final ImageAnalysisConfig.Builder mUseCaseConfigBuilder;
    @Nullable
    ImageReaderProxy mImageReader;
    @Nullable
    private DeferrableSurface mDeferrableSurface;

    /**
     * Creates a new image analysis use case from the given configuration.
     *
     * @param config for this use case instance
     */
    public ImageAnalysis(ImageAnalysisConfig config) {
        super(config);
        mUseCaseConfigBuilder = ImageAnalysisConfig.Builder.fromConfig(config);

        // Get the combined configuration with defaults
        ImageAnalysisConfig combinedConfig = (ImageAnalysisConfig) getUseCaseConfig();
        mSubscribedAnalyzer = new AtomicReference<>();
        mHandler = combinedConfig.getCallbackHandler(null);
        if (mHandler == null) {
            throw new IllegalStateException("No default mHandler specified.");
        }
        setImageFormat(ImageReaderFormatRecommender.chooseCombo().imageAnalysisFormat());
    }

    /**
     * Removes a previously set analyzer.
     *
     * <p>This is equivalent to calling {@code setAnalyzer(null)}.  This will stop data from
     * streaming to the {@link ImageAnalysis}.
     */
    @UiThread
    public void removeAnalyzer() {
        setAnalyzer(null);
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
     * <p>If no target rotation is set by the application, it is set to the value of
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
     * Retrieves a previously set analyzer.
     *
     * @return The last set analyzer or {@code null} if no analyzer is set.
     */
    @UiThread
    @Nullable
    public Analyzer getAnalyzer() {
        return mSubscribedAnalyzer.get();
    }

    /**
     * Sets an analyzer to receive and analyze images.
     *
     * <p>Setting an analyzer will signal to the camera that it should begin sending data. The
     * stream of data can be stopped by setting the analyzer to {@code null} or by calling {@link
     * #removeAnalyzer()}.
     *
     * <p>Applications can process or copy the image by implementing the {@link Analyzer}.  If
     * frames should be skipped (no analysis), the analyzer function should return, instead of
     * disconnecting the analyzer function completely.
     *
     * <p>Setting an analyzer function replaces any previous analyzer.  Only one analyzer can be
     * set at any time.
     *
     * @param analyzer of the images or {@code null} to stop data streaming to
     *                 {@link ImageAnalysis}.
     */
    @UiThread
    public void setAnalyzer(@Nullable Analyzer analyzer) {
        Analyzer previousAnalyzer = mSubscribedAnalyzer.getAndSet(analyzer);
        if (previousAnalyzer == null && analyzer != null) {
            notifyActive();
        } else if (previousAnalyzer != null && analyzer == null) {
            notifyInactive();
        }
    }

    @Override
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
        if (mDeferrableSurface != null) {
            mDeferrableSurface.setOnSurfaceDetachedListener(
                    CameraXExecutors.mainThreadExecutor(),
                    new DeferrableSurface.OnSurfaceDetachedListener() {
                        @Override
                        public void onSurfaceDetached() {
                            if (mImageReader != null) {
                                mImageReader.close();
                                mImageReader = null;
                            }
                        }
                    });
        }
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
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        final ImageAnalysisConfig config = (ImageAnalysisConfig) getUseCaseConfig();

        String cameraId = getCameraIdUnchecked(config);

        Size resolution = suggestedResolutionMap.get(cameraId);
        if (resolution == null) {
            throw new IllegalArgumentException(
                    "Suggested resolution map missing resolution for camera " + cameraId);
        }

        if (mImageReader != null) {
            mImageReader.close();
        }

        mImageReader =
                ImageReaderProxys.createCompatibleReader(
                        cameraId,
                        resolution.getWidth(),
                        resolution.getHeight(),
                        getImageFormat(),
                        config.getImageQueueDepth(),
                        mHandler);

        tryUpdateRelativeRotation(cameraId);
        mImageReader.setOnImageAvailableListener(
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        Analyzer analyzer = mSubscribedAnalyzer.get();
                        try (ImageProxy image =
                                     config
                                             .getImageReaderMode(config.getImageReaderMode())
                                             .equals(ImageReaderMode.ACQUIRE_NEXT_IMAGE)
                                             ? imageReader.acquireNextImage()
                                             : imageReader.acquireLatestImage()) {
                            // Do not analyze if unable to acquire an ImageProxy
                            if (image == null) {
                                return;
                            }

                            if (analyzer != null) {
                                analyzer.analyze(image, mRelativeRotation.get());
                            }
                        }
                    }
                },
                mHandler);

        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);

        mDeferrableSurface = new ImmediateSurface(mImageReader.getSurface());

        sessionConfigBuilder.addSurface(mDeferrableSurface);

        attachToCamera(cameraId, sessionConfigBuilder.build());

        return suggestedResolutionMap;
    }

    private void tryUpdateRelativeRotation(String cameraId) {
        ImageOutputConfig config = (ImageOutputConfig) getUseCaseConfig();
        // Get the relative rotation or default to 0 if the camera info is unavailable
        try {
            CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
            mRelativeRotation.set(
                    cameraInfo.getSensorRotationDegrees(
                            config.getTargetRotation(Surface.ROTATION_0)));
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to retrieve camera sensor orientation.", e);
        }
    }

    /**
     * The different ways that the image sent to the analyzer is acquired from the underlying {@link
     * ImageReader}. This corresponds to acquireLatestImage or acquireNextImage in {@link
     * ImageReader}.
     *
     * @see android.media.ImageReader
     */
    public enum ImageReaderMode {
        /** Acquires the latest image in the queue, discarding any images older than the latest. */
        ACQUIRE_LATEST_IMAGE,
        /** Acquires the next image in the queue. */
        ACQUIRE_NEXT_IMAGE,
    }

    /** Interface for analyzing images.
     *
     * <p>Implement Analyzer and pass it to {@link ImageAnalysis#setAnalyzer(Analyzer)} to receive
     * images and perform custom processing by implementing the
     * {@link ImageAnalysis.Analyzer#analyze(ImageProxy, int)} function.
     *
     */
    public interface Analyzer {
        /**
         * Analyzes an image to produce a result.
         *
         * <p>This method is called once for each image from the camera, and called at the
         * frame rate of the camera.  Each analyze call is executed sequentially.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * <p>Processing should complete within a single frame time of latency, or the image data
         * should be copied out for longer processing.  Applications can be skip analyzing a frame
         * by having the analyzer return immediately.
         *
         * @param image           The image to analyze
         * @param rotationDegrees The rotation which if applied to the image would make it match
         *                        the current target rotation of {@link ImageAnalysis}, expressed in
         *                        degrees in the range {@code [0..360)}.
         *
         */
        void analyze(ImageProxy image, int rotationDegrees);
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
        private static final ImageReaderMode DEFAULT_IMAGE_READER_MODE =
                ImageReaderMode.ACQUIRE_NEXT_IMAGE;
        private static final Handler DEFAULT_HANDLER = new Handler(Looper.getMainLooper());
        private static final int DEFAULT_IMAGE_QUEUE_DEPTH = 6;
        private static final Size DEFAULT_TARGET_RESOLUTION = new Size(640, 480);
        private static final Size DEFAULT_MAX_RESOLUTION = new Size(1920, 1080);
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 1;

        private static final ImageAnalysisConfig DEFAULT_CONFIG;

        static {
            ImageAnalysisConfig.Builder builder =
                    new ImageAnalysisConfig.Builder()
                            .setImageReaderMode(DEFAULT_IMAGE_READER_MODE)
                            .setCallbackHandler(DEFAULT_HANDLER)
                            .setImageQueueDepth(DEFAULT_IMAGE_QUEUE_DEPTH)
                            .setTargetResolution(DEFAULT_TARGET_RESOLUTION)
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
