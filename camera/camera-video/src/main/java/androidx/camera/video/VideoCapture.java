/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video;

import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_DEFAULT_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MIRROR_MODE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_HIGH_RESOLUTION_DISABLED;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_TARGET_FRAME_RATE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_VIDEO_STABILIZATION_MODE;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;
import static androidx.camera.core.impl.utils.Threads.isMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.rectToString;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.camera.core.internal.TargetConfig.OPTION_TARGET_CLASS;
import static androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME;
import static androidx.camera.core.internal.ThreadConfig.OPTION_BACKGROUND_EXECUTOR;
import static androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk.workaroundBySurfaceProcessing;
import static androidx.camera.core.internal.utils.SizeUtil.getArea;
import static androidx.camera.video.QualitySelector.getQualityToResolutionMap;
import static androidx.camera.video.StreamInfo.STREAM_ID_ERROR;
import static androidx.camera.video.impl.VideoCaptureConfig.OPTION_FORCE_ENABLE_SURFACE_PROCESSING;
import static androidx.camera.video.impl.VideoCaptureConfig.OPTION_VIDEO_ENCODER_INFO_FINDER;
import static androidx.camera.video.impl.VideoCaptureConfig.OPTION_VIDEO_OUTPUT;
import static androidx.camera.video.internal.config.VideoConfigUtil.resolveVideoEncoderConfig;
import static androidx.camera.video.internal.config.VideoConfigUtil.resolveVideoMimeInfo;
import static androidx.camera.video.internal.utils.DynamicRangeUtil.isHdrSettingsMatched;
import static androidx.camera.video.internal.utils.DynamicRangeUtil.videoProfileBitDepthToDynamicRangeBitDepth;
import static androidx.camera.video.internal.utils.DynamicRangeUtil.videoProfileHdrFormatsToDynamicRangeEncoding;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.os.SystemClock;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.SurfaceRequest.TransformationInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.ImageInputConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.Observable.Observer;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.TransformUtils;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.core.processing.DefaultSurfaceProcessor;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorNode;
import androidx.camera.core.processing.util.OutConfig;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.video.StreamInfo.StreamState;
import androidx.camera.video.impl.VideoCaptureConfig;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.SizeCannotEncodeVideoQuirk;
import androidx.camera.video.internal.config.VideoMimeInfo;
import androidx.camera.video.internal.encoder.SwappedVideoEncoderInfo;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;
import androidx.camera.video.internal.encoder.VideoEncoderInfoImpl;
import androidx.camera.video.internal.workaround.VideoEncoderInfoWrapper;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A use case that provides camera stream suitable for video application.
 *
 * <p>VideoCapture is used to create a camera stream suitable for a video application such as
 * recording a high-quality video to a file. The camera stream is used by the extended classes of
 * {@link VideoOutput}.
 * {@link #withOutput(VideoOutput)} can be used to create a VideoCapture instance associated with
 * the given VideoOutput. Take {@link Recorder} as an example,
 * <pre>{@code
 *         VideoCapture<Recorder> videoCapture
 *                 = VideoCapture.withOutput(new Recorder.Builder().build());
 * }</pre>
 * Then {@link #getOutput()} can retrieve the Recorder instance.
 *
 * @param <T> the type of VideoOutput
 */
public final class VideoCapture<T extends VideoOutput> extends UseCase {
    private static final String TAG = "VideoCapture";
    private static final String SURFACE_UPDATE_KEY =
            "androidx.camera.video.VideoCapture.streamUpdate";
    private static final Defaults DEFAULT_CONFIG = new Defaults();

    @SuppressWarnings("WeakerAccess") // Synthetic access
    DeferrableSurface mDeferrableSurface;
    @Nullable
    private SurfaceEdge mCameraEdge;
    @SuppressWarnings("WeakerAccess") // Synthetic access
    StreamInfo mStreamInfo = StreamInfo.STREAM_INFO_ANY_INACTIVE;
    @SuppressWarnings("WeakerAccess") // Synthetic access
    @NonNull
    SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @SuppressWarnings("WeakerAccess") // Synthetic access
    ListenableFuture<Void> mSurfaceUpdateFuture = null;
    private SurfaceRequest mSurfaceRequest;
    @SuppressWarnings("WeakerAccess") // Synthetic access
    VideoOutput.SourceState mSourceState = VideoOutput.SourceState.INACTIVE;
    @Nullable
    private SurfaceProcessorNode mNode;
    @Nullable
    private Rect mCropRect;
    private int mRotationDegrees;
    private boolean mHasCompensatingTransformation = false;
    @Nullable
    private SourceStreamRequirementObserver mSourceStreamRequirementObserver;
    @Nullable
    private SessionConfig.CloseableErrorListener mCloseableErrorListener;

    /**
     * Create a VideoCapture associated with the given {@link VideoOutput}.
     *
     * @throws NullPointerException if {@code videoOutput} is null.
     */
    @NonNull
    public static <T extends VideoOutput> VideoCapture<T> withOutput(@NonNull T videoOutput) {
        return new VideoCapture.Builder<>(Preconditions.checkNotNull(videoOutput)).build();
    }

    /**
     * Creates a new video capture use case from the given configuration.
     *
     * @param config for this use case instance
     */
    VideoCapture(@NonNull VideoCaptureConfig<T> config) {
        super(config);
    }

    /**
     * Gets the {@link VideoOutput} associated with this VideoCapture.
     *
     * @return the value provided to {@link #withOutput(VideoOutput)} used to create this
     * VideoCapture.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public T getOutput() {
        return ((VideoCaptureConfig<T>) getCurrentConfig()).getVideoOutput();
    }

    /**
     * Returns the desired rotation of the output video.
     *
     * <p>The rotation can be set prior to constructing a VideoCapture using
     * {@link VideoCapture.Builder#setTargetRotation(int)} or dynamically by calling
     * {@link VideoCapture#setTargetRotation(int)}.
     * If not set, the target rotation defaults to the value of {@link Display#getRotation()} of
     * the default display at the time the use case is bound.
     *
     * @return The rotation of the intended target.
     * @see VideoCapture#setTargetRotation(int)
     */
    @RotationValue
    public int getTargetRotation() {
        return getTargetRotationInternal();
    }

    /**
     * Returns the target frame rate range, in frames per second, for the associated VideoCapture
     * use case.
     *
     * <p>The target frame rate can be set prior to constructing a VideoCapture using
     * {@link VideoCapture.Builder#setTargetFrameRate(Range)}
     * If not set, the target frame rate defaults to the value of
     * {@link StreamSpec#FRAME_RATE_RANGE_UNSPECIFIED}
     *
     * @return The target frame rate of the intended target.
     */
    @NonNull
    public Range<Integer> getTargetFrameRate() {
        return getTargetFrameRateInternal();
    }

    /**
     * Returns whether video stabilization is enabled.
     */
    public boolean isVideoStabilizationEnabled() {
        return getCurrentConfig().getVideoStabilizationMode() == StabilizationMode.ON;
    }

    /**
     * Sets the desired rotation of the output video.
     *
     * <p>Valid values include: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
     *
     * <p>While rotation can also be set via {@link Builder#setTargetRotation(int)}, using
     * {@code setTargetRotation(int)} allows the target rotation to be set dynamically.
     *
     * <p>In general, it is best to use an {@link android.view.OrientationEventListener} to set
     * the target rotation. This way, the rotation output will indicate which way is down for a
     * given video. This is important since display orientation may be locked by device default,
     * user setting, or app configuration, and some devices may not transition to a
     * reverse-portrait display orientation. In these cases, set target rotation dynamically
     * according to the {@link android.view.OrientationEventListener}, without re-creating the
     * use case. {@link UseCase#snapToSurfaceRotation(int)} is a helper function to convert the
     * orientation of the {@link android.view.OrientationEventListener} to a rotation value.
     * See {@link UseCase#snapToSurfaceRotation(int)} for more information and sample code.
     *
     * <p>If not set, the target rotation will default to the value of
     * {@link Display#getRotation()} of the default display at the time the use case is bound. To
     * return to the default value, set the value to
     * <pre>{@code
     * context.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
     * }</pre>
     *
     * <p>For a {@link Recorder} output, calling this method has no effect on the ongoing
     * recording, but will affect recordings started after calling this method. The final
     * rotation degrees of the video, including the degrees set by this method and the orientation
     * of the camera sensor, will be reflected by several possibilities, 1) the rotation degrees is
     * written into the video metadata, 2) the video content is directly rotated, 3) both, i.e.
     * rotation metadata and rotated video content which combines to the target rotation. CameraX
     * will choose a strategy according to the use case.
     *
     * @param rotation Desired rotation of the output video, expressed as one of
     *                 {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                 {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        if (setTargetRotationInternal(rotation)) {
            sendTransformationInfoIfReady();
        }
    }

    /**
     * Returns the mirror mode.
     *
     * <p>The mirror mode is set by {@link VideoCapture.Builder#setMirrorMode(int)}. If not set,
     * it defaults to {@link MirrorMode#MIRROR_MODE_OFF}.
     *
     * @return The mirror mode of the intended target.
     */
    @MirrorMode.Mirror
    public int getMirrorMode() {
        int mirrorMode = getMirrorModeInternal();
        if (mirrorMode == MirrorMode.MIRROR_MODE_UNSPECIFIED) {
            return MirrorMode.MIRROR_MODE_OFF;
        }
        return mirrorMode;
    }

    @SuppressWarnings("unchecked")
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected StreamSpec onSuggestedStreamSpecUpdated(
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        Logger.d(TAG, "onSuggestedStreamSpecUpdated: " + primaryStreamSpec);
        VideoCaptureConfig<T> config = (VideoCaptureConfig<T>) getCurrentConfig();
        List<Size> customOrderedResolutions = config.getCustomOrderedResolutions(null);
        if (customOrderedResolutions != null
                && !customOrderedResolutions.contains(primaryStreamSpec.getResolution())) {
            Logger.w(TAG, "suggested resolution " + primaryStreamSpec.getResolution()
                    + " is not in custom ordered resolutions " + customOrderedResolutions);
        }
        return primaryStreamSpec;
    }

    /**
     * Returns the dynamic range.
     *
     * <p>The dynamic range is set by {@link VideoCapture.Builder#setDynamicRange(DynamicRange)}.
     * If the dynamic range set is not a fully defined dynamic range, such as
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}, then it will be returned just as provided,
     * and will not be returned as a fully defined dynamic range.
     *
     * <p>If the dynamic range was not provided to
     * {@link VideoCapture.Builder#setDynamicRange(DynamicRange)}, this will return the default of
     * {@link DynamicRange#SDR}
     *
     * @return the dynamic range set for this {@code VideoCapture} use case.
     */
    // Internal implementation note: this method should not be used to retrieve the dynamic range
    // that will be sent to the VideoOutput. That should always be retrieved from the StreamSpec
    // since that will be the final DynamicRange chosen by the camera based on other use case
    // combinations.
    @NonNull
    public DynamicRange getDynamicRange() {
        return getCurrentConfig().hasDynamicRange() ? getCurrentConfig().getDynamicRange() :
                Defaults.DEFAULT_DYNAMIC_RANGE;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onStateAttached() {
        super.onStateAttached();

        Logger.d(TAG, "VideoCapture#onStateAttached: cameraID = " + getCameraId());

        // For concurrent camera, the surface request might not be null when switching
        // from single to dual camera.
        if (getAttachedStreamSpec() == null || mSurfaceRequest != null) {
            return;
        }
        StreamSpec attachedStreamSpec = Preconditions.checkNotNull(getAttachedStreamSpec());
        mStreamInfo = fetchObservableValue(getOutput().getStreamInfo(),
                StreamInfo.STREAM_INFO_ANY_INACTIVE);
        mSessionConfigBuilder = createPipeline(
                (VideoCaptureConfig<T>) getCurrentConfig(), attachedStreamSpec);
        applyStreamInfoAndStreamSpecToSessionConfigBuilder(mSessionConfigBuilder, mStreamInfo,
                attachedStreamSpec);
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
        // VideoCapture has to be active to apply SessionConfig's template type.
        notifyActive();
        getOutput().getStreamInfo().addObserver(CameraXExecutors.mainThreadExecutor(),
                mStreamInfoObserver);
        if (mSourceStreamRequirementObserver != null) {
            // In case a previous observer was not closed, close it first
            mSourceStreamRequirementObserver.close();
        }
        // Camera should be already bound by now, so calling getCameraControl() is ok
        mSourceStreamRequirementObserver = new SourceStreamRequirementObserver(getCameraControl());
        // Should automatically trigger once for latest data
        getOutput().isSourceStreamRequired().addObserver(CameraXExecutors.mainThreadExecutor(),
                mSourceStreamRequirementObserver);
        setSourceState(VideoOutput.SourceState.ACTIVE_NON_STREAMING);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        super.setViewPortCropRect(viewPortCropRect);
        sendTransformationInfoIfReady();
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void onStateDetached() {
        Logger.d(TAG, "VideoCapture#onStateDetached");

        checkState(isMainThread(), "VideoCapture can only be detached on the main thread.");

        // It's safer to remove and close mSourceStreamRequirementObserver before stopping recorder
        // in case there is some bug leading to double video usage decrement updates (e.g. once for
        // recorder stop and once for observer close)
        if (mSourceStreamRequirementObserver != null) {
            getOutput().isSourceStreamRequired().removeObserver(mSourceStreamRequirementObserver);
            mSourceStreamRequirementObserver.close();
            mSourceStreamRequirementObserver = null;
        }

        setSourceState(VideoOutput.SourceState.INACTIVE);
        getOutput().getStreamInfo().removeObserver(mStreamInfoObserver);

        if (mSurfaceUpdateFuture != null) {
            if (mSurfaceUpdateFuture.cancel(false)) {
                Logger.d(TAG, "VideoCapture is detached from the camera. Surface update "
                        + "cancelled.");
            }
        }
        // Clear the pipeline to close the surface, which releases the codec so that it's
        // available for other applications.
        clearPipeline();
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
        return requireNonNull(getAttachedStreamSpec()).toBuilder()
                .setImplementationOptions(config).build();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + ":" + getName();
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
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);

        if (applyDefaultConfig) {
            captureConfig = Config.mergeConfigs(captureConfig, DEFAULT_CONFIG.getConfig());
        }

        return captureConfig == null ? null :
                getUseCaseConfigBuilder(captureConfig).getUseCaseConfig();
    }

    /**
     * {@inheritDoc}
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {

        updateCustomOrderedResolutionsByQuality(cameraInfo, builder);

        return builder.getUseCaseConfig();
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

    private void sendTransformationInfoIfReady() {
        CameraInternal cameraInternal = getCamera();
        SurfaceEdge cameraEdge = mCameraEdge;
        if (cameraInternal != null && cameraEdge != null) {
            mRotationDegrees = getCompensatedRotation(cameraInternal);
            cameraEdge.updateTransformation(mRotationDegrees, getAppTargetRotation());
        }
    }

    @NonNull
    private Rect adjustCropRectWithInProgressTransformation(@NonNull Rect cropRect,
            int rotationDegrees) {
        Rect adjustedCropRect = cropRect;
        if (shouldCompensateTransformation()) {
            adjustedCropRect = TransformUtils.sizeToRect(TransformUtils.getRotatedSize(
                    Preconditions.checkNotNull(
                            mStreamInfo.getInProgressTransformationInfo()).getCropRect(),
                    rotationDegrees));
        }
        return adjustedCropRect;
    }

    /**
     * Gets the rotation that is compensated by the in-progress transformation.
     *
     * <p>If there's no in-progress recording, the returned rotation degrees will be the same as
     * {@link #getRelativeRotation(CameraInternal)}.
     */
    private int getCompensatedRotation(@NonNull CameraInternal cameraInternal) {
        boolean isMirroringRequired = isMirroringRequired(cameraInternal);
        int rotationDegrees = getRelativeRotation(cameraInternal, isMirroringRequired);
        if (shouldCompensateTransformation()) {
            TransformationInfo transformationInfo =
                    requireNonNull(mStreamInfo.getInProgressTransformationInfo());
            int inProgressDegrees = transformationInfo.getRotationDegrees();
            if (isMirroringRequired != transformationInfo.isMirroring()) {
                // If the mirroring states of the current stream and the existing stream are
                // different, the existing rotation degrees should be inverted.
                inProgressDegrees = -inProgressDegrees;
            }
            rotationDegrees = within360(rotationDegrees - inProgressDegrees);
        }
        return rotationDegrees;
    }

    @NonNull
    private Size adjustResolutionWithInProgressTransformation(@NonNull Size resolution,
            @NonNull Rect originalCropRect, @NonNull Rect targetCropRect) {
        Size nodeResolution = resolution;
        if (shouldCompensateTransformation() && !targetCropRect.equals(originalCropRect)) {
            float targetRatio = ((float) targetCropRect.height()) / originalCropRect.height();
            nodeResolution = new Size((int) Math.ceil(resolution.getWidth() * targetRatio),
                    (int) Math.ceil(resolution.getHeight() * targetRatio));
        }
        return nodeResolution;
    }

    @VisibleForTesting
    @Nullable
    Rect getCropRect() {
        return mCropRect;
    }

    @VisibleForTesting
    int getRotationDegrees() {
        return mRotationDegrees;
    }

    /**
     * Calculates the crop rect.
     *
     * <p>Fall back to the full {@link Surface} rect if {@link ViewPort} crop rect is not
     * available. The returned crop rect is adjusted if it is not valid to the video encoder.
     */
    @NonNull
    private Rect calculateCropRect(@NonNull Size surfaceResolution,
            @Nullable VideoEncoderInfo videoEncoderInfo) {
        Rect cropRect;
        if (getViewPortCropRect() != null) {
            cropRect = getViewPortCropRect();
        } else {
            cropRect = new Rect(0, 0, surfaceResolution.getWidth(), surfaceResolution.getHeight());
        }
        if (videoEncoderInfo == null || videoEncoderInfo.isSizeSupportedAllowSwapping(
                cropRect.width(), cropRect.height())) {
            return cropRect;
        }
        return adjustCropRectToValidSize(cropRect, surfaceResolution, videoEncoderInfo);
    }

    @SuppressLint("WrongConstant")
    @MainThread
    @NonNull
    private SessionConfig.Builder createPipeline(
            @NonNull VideoCaptureConfig<T> config,
            @NonNull StreamSpec streamSpec) {
        Threads.checkMainThread();
        CameraInternal camera = Preconditions.checkNotNull(getCamera());
        Size resolution = streamSpec.getResolution();

        // Currently, VideoCapture uses StreamInfo to handle requests for surface, so
        // handleInvalidate() is not used. But if a different approach is asked in the future,
        // handleInvalidate() can be used as an alternative.
        Runnable onSurfaceInvalidated = this::notifyReset;
        Range<Integer> expectedFrameRate = resolveFrameRate(streamSpec);
        MediaSpec mediaSpec = requireNonNull(getMediaSpec());
        VideoCapabilities videoCapabilities = getVideoCapabilities(camera.getCameraInfo());
        DynamicRange dynamicRange = streamSpec.getDynamicRange();
        VideoValidatedEncoderProfilesProxy encoderProfiles =
                videoCapabilities.findNearestHigherSupportedEncoderProfilesFor(resolution,
                        dynamicRange);
        VideoEncoderInfo videoEncoderInfo = resolveVideoEncoderInfo(
                config.getVideoEncoderInfoFinder(), encoderProfiles, mediaSpec,  resolution,
                dynamicRange, expectedFrameRate);
        mRotationDegrees = getCompensatedRotation(camera);
        Rect originalCropRect = calculateCropRect(resolution, videoEncoderInfo);
        mCropRect = adjustCropRectWithInProgressTransformation(originalCropRect, mRotationDegrees);
        Size nodeResolution = adjustResolutionWithInProgressTransformation(resolution,
                originalCropRect, mCropRect);
        if (shouldCompensateTransformation()) {
            // If this pipeline is created with in-progress transformation, we need to reset the
            // pipeline when the transformation becomes invalid.
            mHasCompensatingTransformation = true;
        }
        mCropRect = adjustCropRectByQuirk(mCropRect, mRotationDegrees,
                isCreateNodeNeeded(camera, config, mCropRect, resolution), videoEncoderInfo);
        mNode = createNodeIfNeeded(camera, config, mCropRect, resolution, dynamicRange);
        Timebase timebase = resolveTimebase(camera, mNode);
        Logger.d(TAG, "camera timebase = " + camera.getCameraInfoInternal().getTimebase()
                + ", processing timebase = " + timebase);
        // Update the StreamSpec with new frame rate range and resolution.
        StreamSpec updatedStreamSpec =
                streamSpec.toBuilder()
                        .setResolution(nodeResolution)
                        .setExpectedFrameRateRange(expectedFrameRate)
                        .build();
        // Make sure the previously created camera edge is cleared before creating a new one.
        checkState(mCameraEdge == null);
        mCameraEdge = new SurfaceEdge(
                VIDEO_CAPTURE,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                updatedStreamSpec,
                getSensorToBufferTransformMatrix(),
                camera.getHasTransform(),
                mCropRect,
                mRotationDegrees,
                getAppTargetRotation(),
                shouldMirror(camera));
        mCameraEdge.addOnInvalidatedListener(onSurfaceInvalidated);
        if (mNode != null) {
            OutConfig outConfig = OutConfig.of(mCameraEdge);
            SurfaceProcessorNode.In nodeInput = SurfaceProcessorNode.In.of(
                    mCameraEdge,
                    singletonList(outConfig));
            SurfaceProcessorNode.Out nodeOutput = mNode.transform(nodeInput);
            SurfaceEdge appEdge = requireNonNull(nodeOutput.get(outConfig));
            appEdge.addOnInvalidatedListener(
                    () -> onAppEdgeInvalidated(appEdge, camera, config, timebase));
            mSurfaceRequest = appEdge.createSurfaceRequest(camera);
            mDeferrableSurface = mCameraEdge.getDeferrableSurface();
            DeferrableSurface latestDeferrableSurface = mDeferrableSurface;
            mDeferrableSurface.getTerminationFuture().addListener(() -> {
                // If camera surface is the latest one, it means this pipeline can be abandoned.
                // Clear the pipeline in order to trigger the surface complete event to appSurface.
                if (latestDeferrableSurface == mDeferrableSurface) {
                    clearPipeline();
                }
            }, CameraXExecutors.mainThreadExecutor());
        } else {
            mSurfaceRequest = mCameraEdge.createSurfaceRequest(camera);
            mDeferrableSurface = mSurfaceRequest.getDeferrableSurface();
        }

        config.getVideoOutput().onSurfaceRequested(mSurfaceRequest, timebase);
        sendTransformationInfoIfReady();
        // Since VideoCapture is in video module and can't be recognized by core module, use
        // MediaCodec class instead.
        mDeferrableSurface.setContainerClass(MediaCodec.class);

        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config,
                streamSpec.getResolution());
        // Use the frame rate range directly from the StreamSpec here (don't resolve it to the
        // default if unresolved).
        // Applies the AE fps range to the session config builder according to the stream spec and
        // quirk values.
        applyExpectedFrameRateRange(sessionConfigBuilder, streamSpec);
        sessionConfigBuilder.setVideoStabilization(config.getVideoStabilizationMode());
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
        }
        mCloseableErrorListener = new SessionConfig.CloseableErrorListener(
                (sessionConfig, error) -> resetPipeline());
        sessionConfigBuilder.setErrorListener(mCloseableErrorListener);
        if (streamSpec.getImplementationOptions() != null) {
            sessionConfigBuilder.addImplementationOptions(streamSpec.getImplementationOptions());
        }

        return sessionConfigBuilder;
    }

    private void onAppEdgeInvalidated(@NonNull SurfaceEdge appEdge, @NonNull CameraInternal camera,
            @NonNull VideoCaptureConfig<T> config, @NonNull Timebase timebase) {
        if (camera == getCamera()) {
            mSurfaceRequest = appEdge.createSurfaceRequest(camera);
            config.getVideoOutput().onSurfaceRequested(mSurfaceRequest, timebase);
            sendTransformationInfoIfReady();
        }
    }

    /**
     * Clear the internal pipeline so that the pipeline can be set up again.
     */
    @MainThread
    private void clearPipeline() {
        Threads.checkMainThread();

        // Closes the old error listener
        if (mCloseableErrorListener != null) {
            mCloseableErrorListener.close();
            mCloseableErrorListener = null;
        }

        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
            mDeferrableSurface = null;
        }
        if (mNode != null) {
            mNode.release();
            mNode = null;
        }
        if (mCameraEdge != null) {
            mCameraEdge.close();
            mCameraEdge = null;
        }
        mCropRect = null;
        mSurfaceRequest = null;
        mStreamInfo = StreamInfo.STREAM_INFO_ANY_INACTIVE;
        mRotationDegrees = 0;
        mHasCompensatingTransformation = false;
    }

    @MainThread
    @SuppressWarnings({"WeakerAccess", "unchecked"}) /* synthetic accessor */
    void resetPipeline() {
        // Do nothing when the use case has been unbound.
        if (getCamera() == null) {
            return;
        }

        clearPipeline();
        mSessionConfigBuilder = createPipeline(
                (VideoCaptureConfig<T>) getCurrentConfig(),
                Preconditions.checkNotNull(getAttachedStreamSpec()));
        applyStreamInfoAndStreamSpecToSessionConfigBuilder(mSessionConfigBuilder, mStreamInfo,
                getAttachedStreamSpec());
        updateSessionConfig(List.of(mSessionConfigBuilder.build()));
        notifyReset();
    }

    /**
     *
     */
    @Nullable
    @VisibleForTesting
    SurfaceEdge getCameraEdge() {
        return mCameraEdge;
    }

    /**
     * Provides a base static default configuration for the VideoCapture
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<VideoCaptureConfig<?>> {
        /** Surface occupancy priority to this use case */
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 5;
        private static final VideoOutput DEFAULT_VIDEO_OUTPUT =
                SurfaceRequest::willNotProvideSurface;
        private static final VideoCaptureConfig<?> DEFAULT_CONFIG;

        private static final Function<VideoEncoderConfig, VideoEncoderInfo>
                DEFAULT_VIDEO_ENCODER_INFO_FINDER = VideoEncoderInfoImpl.FINDER;

        static final Range<Integer> DEFAULT_FPS_RANGE = new Range<>(30, 30);

        /**
         * Explicitly setting the default dynamic range to SDR (rather than UNSPECIFIED) means
         * VideoCapture won't inherit dynamic ranges from other use cases.
         */
        static final DynamicRange DEFAULT_DYNAMIC_RANGE = DynamicRange.SDR;

        static {
            Builder<?> builder = new Builder<>(DEFAULT_VIDEO_OUTPUT)
                    .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                    .setVideoEncoderInfoFinder(DEFAULT_VIDEO_ENCODER_INFO_FINDER)
                    .setDynamicRange(DEFAULT_DYNAMIC_RANGE);

            DEFAULT_CONFIG = builder.getUseCaseConfig();
        }

        @NonNull
        @Override
        public VideoCaptureConfig<?> getConfig() {
            return DEFAULT_CONFIG;
        }
    }

    @Nullable
    private MediaSpec getMediaSpec() {
        return fetchObservableValue(getOutput().getMediaSpec(), null);
    }

    @NonNull
    private VideoCapabilities getVideoCapabilities(@NonNull CameraInfo cameraInfo) {
        return getOutput().getMediaCapabilities(cameraInfo);
    }

    private final Observer<StreamInfo> mStreamInfoObserver = new Observer<StreamInfo>() {
        @SuppressWarnings("unchecked")
        @Override
        public void onNewData(@Nullable StreamInfo streamInfo) {
            if (streamInfo == null) {
                throw new IllegalArgumentException("StreamInfo can't be null");
            }
            if (mSourceState == VideoOutput.SourceState.INACTIVE) {
                // VideoCapture is unbound.
                return;
            }
            Logger.d(TAG, "Stream info update: old: " + mStreamInfo + " new: " + streamInfo);

            StreamInfo currentStreamInfo = mStreamInfo;
            mStreamInfo = streamInfo;

            // Doing resetPipeline() includes notifyReset/notifyUpdated(). Doing NotifyReset()
            // includes notifyUpdated(). So we just take actions on higher order item for
            // optimization.
            StreamSpec attachedStreamSpec = Preconditions.checkNotNull(getAttachedStreamSpec());
            if (isStreamIdChanged(currentStreamInfo.getId(), streamInfo.getId())
                    || shouldResetCompensatingTransformation(currentStreamInfo, streamInfo)) {
                // Reset pipeline if it's one of the following cases:
                // 1. The stream ids are different, which means there's a new surface ready to be
                // requested.
                // 2. The in-progress transformation info becomes null, which means a recording
                // has been finalized, and there's an existing compensating transformation.
                resetPipeline();
            } else if ((currentStreamInfo.getId() != STREAM_ID_ERROR
                    && streamInfo.getId() == STREAM_ID_ERROR)
                    || (currentStreamInfo.getId() == STREAM_ID_ERROR
                    && streamInfo.getId() != STREAM_ID_ERROR)) {
                // If id switch to STREAM_ID_ERROR, it means VideoOutput is failed to setup video
                // stream. The surface should be removed from camera. Vice versa.
                applyStreamInfoAndStreamSpecToSessionConfigBuilder(mSessionConfigBuilder,
                        streamInfo,
                        attachedStreamSpec);
                updateSessionConfig(List.of(mSessionConfigBuilder.build()));
                notifyReset();
            } else if (currentStreamInfo.getStreamState() != streamInfo.getStreamState()) {
                applyStreamInfoAndStreamSpecToSessionConfigBuilder(mSessionConfigBuilder,
                        streamInfo,
                        attachedStreamSpec);
                updateSessionConfig(List.of(mSessionConfigBuilder.build()));
                notifyUpdated();
            }
        }

        @Override
        public void onError(@NonNull Throwable t) {
            Logger.w(TAG, "Receive onError from StreamState observer", t);
        }
    };

    /**
     * Observes whether the source stream is required and updates source i.e. camera layer
     * accordingly.
     */
    static class SourceStreamRequirementObserver implements Observer<Boolean> {
        @Nullable
        private CameraControlInternal mCameraControl;

        private boolean mIsSourceStreamRequired = false;

        SourceStreamRequirementObserver(@NonNull CameraControlInternal cameraControl) {
            mCameraControl = cameraControl;
        }

        @MainThread
        @Override
        public void onNewData(@Nullable Boolean value) {
            checkState(isMainThread(),
                    "SourceStreamRequirementObserver can be updated from main thread only");
            updateVideoUsageInCamera(Boolean.TRUE.equals(value));
        }

        @Override
        public void onError(@NonNull Throwable t) {
            Logger.w(TAG, "SourceStreamRequirementObserver#onError", t);
        }

        private void updateVideoUsageInCamera(boolean isRequired) {
            if (mIsSourceStreamRequired == isRequired) {
                return;
            }
            mIsSourceStreamRequired = isRequired;
            if (mCameraControl != null) {
                if (mIsSourceStreamRequired) {
                    mCameraControl.incrementVideoUsage();
                } else {
                    mCameraControl.decrementVideoUsage();
                }
            } else {
                Logger.d(TAG,
                        "SourceStreamRequirementObserver#isSourceStreamRequired: Received"
                                + " new data despite being closed already");
            }
        }

        /**
         * Closes this object to detach the association with camera and updates recording status if
         * required.
         */
        @MainThread
        public void close() {
            checkState(isMainThread(),
                    "SourceStreamRequirementObserver can be closed from main thread only");

            Logger.d(TAG, "SourceStreamRequirementObserver#close: mIsSourceStreamRequired = "
                    + mIsSourceStreamRequired);

            if (mCameraControl == null) {
                Logger.d(TAG, "SourceStreamRequirementObserver#close: Already closed!");
                return;
            }

            // Before removing the camera, it should be updated about recording status
            updateVideoUsageInCamera(false);
            mCameraControl = null;
        }
    }

    @MainThread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void applyStreamInfoAndStreamSpecToSessionConfigBuilder(
            @NonNull SessionConfig.Builder sessionConfigBuilder,
            @NonNull StreamInfo streamInfo, @NonNull StreamSpec streamSpec) {
        final boolean isStreamError = streamInfo.getId() == StreamInfo.STREAM_ID_ERROR;
        final boolean isStreamActive = streamInfo.getStreamState() == StreamState.ACTIVE;
        if (isStreamError && isStreamActive) {
            throw new IllegalStateException(
                    "Unexpected stream state, stream is error but active");
        }

        sessionConfigBuilder.clearSurfaces();
        DynamicRange dynamicRange = streamSpec.getDynamicRange();
        if (!isStreamError && mDeferrableSurface != null) {
            if (isStreamActive) {
                sessionConfigBuilder.addSurface(mDeferrableSurface,
                        dynamicRange,
                        null,
                        MirrorMode.MIRROR_MODE_UNSPECIFIED);
            } else {
                sessionConfigBuilder.addNonRepeatingSurface(mDeferrableSurface, dynamicRange);
            }
        } // Don't attach surface when stream is invalid.

        setupSurfaceUpdateNotifier(sessionConfigBuilder, isStreamActive);
    }

    private boolean isCreateNodeNeeded(@NonNull CameraInternal camera,
            @NonNull VideoCaptureConfig<?> config,
            @NonNull Rect cropRect,
            @NonNull Size resolution) {
        return getEffect() != null
                || shouldEnableSurfaceProcessingByConfig(camera, config)
                || shouldEnableSurfaceProcessingByQuirk(camera)
                || shouldCrop(cropRect, resolution)
                || shouldMirror(camera)
                || shouldCompensateTransformation();
    }

    @Nullable
    private SurfaceProcessorNode createNodeIfNeeded(@NonNull CameraInternal camera,
            @NonNull VideoCaptureConfig<T> config,
            @NonNull Rect cropRect,
            @NonNull Size resolution,
            @NonNull DynamicRange dynamicRange) {
        if (isCreateNodeNeeded(camera, config, cropRect, resolution)) {
            Logger.d(TAG, "Surface processing is enabled.");
            return new SurfaceProcessorNode(requireNonNull(getCamera()),
                    getEffect() != null ? getEffect().createSurfaceProcessorInternal() :
                            DefaultSurfaceProcessor.Factory.newInstance(dynamicRange));
        }
        return null;
    }

    @VisibleForTesting
    @Nullable
    SurfaceProcessorNode getNode() {
        return mNode;
    }

    /** Adjusts the cropRect if the quirk matches, otherwise returns the original cropRect. */
    @NonNull
    private static Rect adjustCropRectByQuirk(@NonNull Rect cropRect, int rotationDegrees,
            boolean isSurfaceProcessingEnabled, @Nullable VideoEncoderInfo videoEncoderInfo) {
        SizeCannotEncodeVideoQuirk quirk = DeviceQuirks.get(SizeCannotEncodeVideoQuirk.class);
        if (quirk != null) {
            return quirk.adjustCropRectForProblematicEncodeSize(cropRect,
                    isSurfaceProcessingEnabled ? rotationDegrees : 0, videoEncoderInfo);
        }
        return cropRect;
    }

    /**
     * This method resizes the crop rectangle to a valid size.
     *
     * <p>The valid size must fulfill
     * <ul>
     * <li>The multiple of VideoEncoderInfo.getWidthAlignment()/getHeightAlignment() alignment</li>
     * <li>In the scope of Surface resolution and VideoEncoderInfo.getSupportedWidths()
     * /getSupportedHeights().</li>
     * </ul>
     *
     * <p>When the size is not a multiple of the alignment, it seeks to shrink or enlarge the size
     * with the smallest amount of change and ensures that the size is within the surface
     * resolution and supported widths and heights. The new cropping rectangle position (left,
     * right, top, and bottom) is then calculated by extending or indenting from the center of
     * the original cropping rectangle.
     */
    @SuppressWarnings("RedundantIfStatement")
    @NonNull
    private static Rect adjustCropRectToValidSize(@NonNull Rect cropRect, @NonNull Size resolution,
            @NonNull VideoEncoderInfo videoEncoderInfo) {
        Logger.d(TAG, String.format("Adjust cropRect %s by width/height alignment %d/%d and "
                        + "supported widths %s / supported heights %s",
                rectToString(cropRect),
                videoEncoderInfo.getWidthAlignment(),
                videoEncoderInfo.getHeightAlignment(),
                videoEncoderInfo.getSupportedWidths(),
                videoEncoderInfo.getSupportedHeights()
        ));

        boolean swapWidthHeightConstraints;
        if (videoEncoderInfo.getSupportedWidths().contains(cropRect.width())
                && videoEncoderInfo.getSupportedHeights().contains(cropRect.height())) {
            swapWidthHeightConstraints = false;
        } else if (videoEncoderInfo.canSwapWidthHeight()
                && videoEncoderInfo.getSupportedHeights().contains(cropRect.width())
                && videoEncoderInfo.getSupportedWidths().contains(cropRect.height())) {
            swapWidthHeightConstraints = true;
        } else {
            // We may need a strategy when both width and height are not within supported widths
            // and heights. It should be a rare case and for now we leave it no swapping.
            swapWidthHeightConstraints = false;
        }
        if (swapWidthHeightConstraints) {
            videoEncoderInfo = new SwappedVideoEncoderInfo(videoEncoderInfo);
        }

        int widthAlignment = videoEncoderInfo.getWidthAlignment();
        int heightAlignment = videoEncoderInfo.getHeightAlignment();
        Range<Integer> supportedWidths = videoEncoderInfo.getSupportedWidths();
        Range<Integer> supportedHeights = videoEncoderInfo.getSupportedHeights();

        // Construct all up/down alignment combinations.
        int widthAlignedDown = alignDown(cropRect.width(), widthAlignment, supportedWidths);
        int widthAlignedUp = alignUp(cropRect.width(), widthAlignment, supportedWidths);
        int heightAlignedDown = alignDown(cropRect.height(), heightAlignment, supportedHeights);
        int heightAlignedUp = alignUp(cropRect.height(), heightAlignment, supportedHeights);

        // Use Set to filter out duplicates.
        Set<Size> candidateSet = new HashSet<>();
        addBySupportedSize(candidateSet, widthAlignedDown, heightAlignedDown, resolution,
                videoEncoderInfo);
        addBySupportedSize(candidateSet, widthAlignedDown, heightAlignedUp, resolution,
                videoEncoderInfo);
        addBySupportedSize(candidateSet, widthAlignedUp, heightAlignedDown, resolution,
                videoEncoderInfo);
        addBySupportedSize(candidateSet, widthAlignedUp, heightAlignedUp, resolution,
                videoEncoderInfo);
        if (candidateSet.isEmpty()) {
            Logger.w(TAG, "Can't find valid cropped size");
            return cropRect;
        }
        List<Size> candidatesList = new ArrayList<>(candidateSet);
        Logger.d(TAG, "candidatesList = " + candidatesList);

        // Find the smallest change in dimensions.
        //noinspection ComparatorCombinators - Suggestion by Comparator.comparingInt is for API24+
        Collections.sort(candidatesList,
                (s1, s2) -> (Math.abs(s1.getWidth() - cropRect.width()) + Math.abs(
                        s1.getHeight() - cropRect.height()))
                        - (Math.abs(s2.getWidth() - cropRect.width()) + Math.abs(
                        s2.getHeight() - cropRect.height())));
        Logger.d(TAG, "sorted candidatesList = " + candidatesList);
        Size newSize = candidatesList.get(0);
        int newWidth = newSize.getWidth();
        int newHeight = newSize.getHeight();

        if (newWidth == cropRect.width() && newHeight == cropRect.height()) {
            Logger.d(TAG, "No need to adjust cropRect because crop size is valid.");
            return cropRect;
        }

        // New width/height should be multiple of 2 since VideoCapabilities.get*Alignment()
        // returns power of 2. This ensures width/2 and height/2 are not rounded off.
        // New width/height smaller than resolution ensures calculated cropRect never exceeds
        // the resolution.
        checkState(newWidth % 2 == 0 && newHeight % 2 == 0
                && newWidth <= resolution.getWidth() && newHeight <= resolution.getHeight());
        Rect newCropRect = new Rect(cropRect);
        if (newWidth != cropRect.width()) {
            // Note: When the width/height of cropRect is odd number, Rect.centerX/Y() will be
            // offset to the left/top by 0.5.
            newCropRect.left = Math.max(0, cropRect.centerX() - newWidth / 2);
            newCropRect.right = newCropRect.left + newWidth;
            if (newCropRect.right > resolution.getWidth()) {
                newCropRect.right = resolution.getWidth();
                newCropRect.left = newCropRect.right - newWidth;
            }
        }
        if (newHeight != cropRect.height()) {
            newCropRect.top = Math.max(0, cropRect.centerY() - newHeight / 2);
            newCropRect.bottom = newCropRect.top + newHeight;
            if (newCropRect.bottom > resolution.getHeight()) {
                newCropRect.bottom = resolution.getHeight();
                newCropRect.top = newCropRect.bottom - newHeight;
            }
        }
        Logger.d(TAG, String.format("Adjust cropRect from %s to %s", rectToString(cropRect),
                rectToString(newCropRect)));
        return newCropRect;
    }

    private static void addBySupportedSize(@NonNull Set<Size> candidates, int width, int height,
            @NonNull Size resolution, @NonNull VideoEncoderInfo videoEncoderInfo) {
        if (width > resolution.getWidth() || height > resolution.getHeight()) {
            return;
        }
        try {
            Range<Integer> supportedHeights = videoEncoderInfo.getSupportedHeightsFor(width);
            candidates.add(new Size(width, supportedHeights.clamp(height)));
        } catch (IllegalArgumentException e) {
            Logger.w(TAG, "No supportedHeights for width: " + width, e);
        }
        try {
            Range<Integer> supportedWidths = videoEncoderInfo.getSupportedWidthsFor(height);
            candidates.add(new Size(supportedWidths.clamp(width), height));
        } catch (IllegalArgumentException e) {
            Logger.w(TAG, "No supportedWidths for height: " + height, e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean isStreamIdChanged(int currentId, int newId) {
        return !StreamInfo.NON_SURFACE_STREAM_ID.contains(currentId)
                && !StreamInfo.NON_SURFACE_STREAM_ID.contains(newId)
                && currentId != newId;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean shouldResetCompensatingTransformation(@NonNull StreamInfo currentStreamInfo,
            @NonNull StreamInfo streamInfo) {
        return mHasCompensatingTransformation
                && currentStreamInfo.getInProgressTransformationInfo() != null
                && streamInfo.getInProgressTransformationInfo() == null;
    }

    private boolean shouldMirror(@NonNull CameraInternal camera) {
        // Stream is always mirrored during buffer copy. If there has been a buffer copy, it
        // means the input stream is already mirrored. Otherwise, mirror it as needed.
        return camera.getHasTransform() && isMirroringRequired(camera);
    }

    private boolean shouldCompensateTransformation() {
        return mStreamInfo.getInProgressTransformationInfo() != null;
    }

    private static boolean shouldCrop(@NonNull Rect cropRect, @NonNull Size resolution) {
        return resolution.getWidth() != cropRect.width()
                || resolution.getHeight() != cropRect.height();
    }

    private static <T extends VideoOutput> boolean shouldEnableSurfaceProcessingByConfig(
            @NonNull CameraInternal camera, @NonNull VideoCaptureConfig<T> config) {
        // If there has been a buffer copy, it means the surface processing is already enabled on
        // input stream. Otherwise, enable it as needed.
        return camera.getHasTransform() && config.isSurfaceProcessingForceEnabled();
    }

    private static boolean shouldEnableSurfaceProcessingByQuirk(@NonNull CameraInternal camera) {
        // If there has been a buffer copy, it means the surface processing is already enabled on
        // input stream. Otherwise, enable it as needed.
        return camera.getHasTransform() && (workaroundBySurfaceProcessing(DeviceQuirks.getAll())
                || workaroundBySurfaceProcessing(camera.getCameraInfoInternal().getCameraQuirks()));
    }

    private static int alignDown(int length, int alignment,
            @NonNull Range<Integer> supportedLength) {
        return align(true, length, alignment, supportedLength);
    }

    private static int alignUp(int length, int alignment,
            @NonNull Range<Integer> supportedRange) {
        return align(false, length, alignment, supportedRange);
    }

    private static int align(boolean alignDown, int length, int alignment,
            @NonNull Range<Integer> supportedRange) {
        int remainder = length % alignment;
        int newLength;
        if (remainder == 0) {
            newLength = length;
        } else if (alignDown) {
            newLength = length - remainder;
        } else {
            newLength = length + (alignment - remainder);
        }
        // Clamp new length by supportedRange, which is supposed to be valid length.
        return supportedRange.clamp(newLength);
    }

    @NonNull
    private static Timebase resolveTimebase(@NonNull CameraInternal camera,
            @Nullable SurfaceProcessorNode node) {
        // Choose Timebase based on the whether the buffer is copied.
        Timebase timebase;
        if (node != null || !camera.getHasTransform()) {
            timebase = camera.getCameraInfoInternal().getTimebase();
        } else {
            // When camera buffers from a REALTIME device are passed directly to a video encoder
            // from the camera, automatic compensation is done to account for differing timebases
            // of the audio and camera subsystems. See the document of
            // CameraMetadata#SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME. So the timebase is always
            // UPTIME when encoder surface is directly sent to camera.
            timebase = Timebase.UPTIME;
        }
        return timebase;
    }

    @NonNull
    private static Range<Integer> resolveFrameRate(@NonNull StreamSpec streamSpec) {
        // If the expected frame rate range is unspecified, we need to give an educated estimate
        // on what frame rate the camera will be operating at. For most devices this is a
        // constant frame rate of 30fps, but in the future this could probably be queried from
        // the camera.
        Range<Integer> frameRate = streamSpec.getExpectedFrameRateRange();
        if (Objects.equals(frameRate, StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)) {
            frameRate = Defaults.DEFAULT_FPS_RANGE;
        }
        return frameRate;
    }

    @Nullable
    private static VideoEncoderInfo resolveVideoEncoderInfo(
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder,
            @Nullable VideoValidatedEncoderProfilesProxy encoderProfiles,
            @NonNull MediaSpec mediaSpec,
            @NonNull Size resolution,
            @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRate) {
        // Resolve the VideoEncoderConfig
        VideoMimeInfo videoMimeInfo = resolveVideoMimeInfo(mediaSpec, dynamicRange,
                encoderProfiles);
        VideoEncoderConfig videoEncoderConfig = resolveVideoEncoderConfig(
                videoMimeInfo,
                // Timebase won't affect the found EncoderInfo so give a arbitrary one.
                Timebase.UPTIME,
                mediaSpec.getVideoSpec(),
                resolution,
                dynamicRange,
                expectedFrameRate);

        VideoEncoderInfo videoEncoderInfo = videoEncoderInfoFinder.apply(videoEncoderConfig);
        if (videoEncoderInfo == null) {
            // If VideoCapture cannot find videoEncoderInfo, it means that VideoOutput should
            // also not be able to find the encoder. VideoCapture will not handle this situation
            // and leave it to VideoOutput to respond.
            Logger.w(TAG, "Can't find videoEncoderInfo");
            return null;
        }

        Size profileSize = encoderProfiles != null ? new Size(
                encoderProfiles.getDefaultVideoProfile().getWidth(),
                encoderProfiles.getDefaultVideoProfile().getHeight()) : null;
        return VideoEncoderInfoWrapper.from(videoEncoderInfo, profileSize);
    }

    @MainThread
    private void setupSurfaceUpdateNotifier(@NonNull SessionConfig.Builder sessionConfigBuilder,
            boolean isStreamActive) {
        if (mSurfaceUpdateFuture != null) {
            // A newer update is issued before the previous update is completed. Cancel the
            // previous future.
            if (mSurfaceUpdateFuture.cancel(false)) {
                Logger.d(TAG,
                        "A newer surface update is requested. Previous surface update cancelled.");
            }
        }

        ListenableFuture<Void> surfaceUpdateFuture = mSurfaceUpdateFuture =
                CallbackToFutureAdapter.getFuture(completer -> {
                    // Use the completer as the tag to identify the update.
                    sessionConfigBuilder.addTag(SURFACE_UPDATE_KEY, completer.hashCode());
                    AtomicBoolean surfaceUpdateComplete = new AtomicBoolean(false);
                    CameraCaptureCallback cameraCaptureCallback =
                            new CameraCaptureCallback() {
                                private boolean mIsFirstCaptureResult = true;
                                @Override
                                public void onCaptureCompleted(int captureConfigId,
                                        @NonNull CameraCaptureResult cameraCaptureResult) {
                                    super.onCaptureCompleted(captureConfigId, cameraCaptureResult);
                                    // Only print the first result to avoid flooding the log.
                                    if (mIsFirstCaptureResult) {
                                        mIsFirstCaptureResult = false;
                                        Logger.d(TAG, "cameraCaptureResult timestampNs = "
                                                + cameraCaptureResult.getTimestamp()
                                                + ", current system uptimeMs = "
                                                + SystemClock.uptimeMillis()
                                                + ", current system realtimeMs = "
                                                + SystemClock.elapsedRealtime());
                                    }
                                    if (!surfaceUpdateComplete.get()) {
                                        Object tag = cameraCaptureResult.getTagBundle().getTag(
                                                SURFACE_UPDATE_KEY);
                                        if (tag != null
                                                && (int) tag == completer.hashCode()
                                                && completer.set(null)
                                                && !surfaceUpdateComplete.getAndSet(true)) {
                                            // Remove from builder so this callback doesn't get
                                            // added to future SessionConfigs
                                            CameraXExecutors.mainThreadExecutor().execute(() ->
                                                    sessionConfigBuilder
                                                            .removeCameraCaptureCallback(this));
                                        }
                                    }
                                }
                            };
                    completer.addCancellationListener(() -> {
                        checkState(isMainThread(), "Surface update "
                                + "cancellation should only occur on main thread.");
                        surfaceUpdateComplete.set(true);
                        sessionConfigBuilder.removeCameraCaptureCallback(cameraCaptureCallback);
                    }, CameraXExecutors.directExecutor());
                    sessionConfigBuilder.addRepeatingCameraCaptureCallback(cameraCaptureCallback);

                    return String.format("%s[0x%x]", SURFACE_UPDATE_KEY, completer.hashCode());
                });

        Futures.addCallback(surfaceUpdateFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // If there is a new surface update request, we will wait to update the video
                // output until that update is complete.
                // Also, if the source state is inactive, then we are detached and should not tell
                // the video output we're active.
                if (surfaceUpdateFuture == mSurfaceUpdateFuture
                        && mSourceState != VideoOutput.SourceState.INACTIVE) {
                    setSourceState(isStreamActive ? VideoOutput.SourceState.ACTIVE_STREAMING
                            : VideoOutput.SourceState.ACTIVE_NON_STREAMING);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (!(t instanceof CancellationException)) {
                    Logger.e(TAG, "Surface update completed with unexpected exception", t);
                }
            }
        }, CameraXExecutors.mainThreadExecutor());
    }

    /**
     * Set {@link ImageOutputConfig#OPTION_CUSTOM_ORDERED_RESOLUTIONS} according to the resolution
     * found by the {@link QualitySelector} in VideoOutput.
     *
     * @throws IllegalArgumentException if not able to find a resolution by the QualitySelector
     *                                  in VideoOutput.
     */
    @SuppressWarnings("unchecked") // Cast to VideoCaptureConfig<T>
    private void updateCustomOrderedResolutionsByQuality(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) throws IllegalArgumentException {
        MediaSpec mediaSpec = getMediaSpec();

        Preconditions.checkArgument(mediaSpec != null,
                "Unable to update target resolution by null MediaSpec.");

        DynamicRange requestedDynamicRange = getDynamicRange();
        VideoCapabilities videoCapabilities = getVideoCapabilities(cameraInfo);

        // Get supported qualities.
        List<Quality> supportedQualities = videoCapabilities.getSupportedQualities(
                requestedDynamicRange);
        if (supportedQualities.isEmpty()) {
            // When the device does not have any supported quality, even the most flexible
            // QualitySelector such as QualitySelector.from(Quality.HIGHEST), still cannot
            // find any resolution. This should be a rare case but will cause VideoCapture
            // to always fail to bind. The workaround is not set any resolution and leave it to
            // auto resolution mechanism.
            Logger.w(TAG, "Can't find any supported quality on the device.");
            return;
        }

        // Get selected qualities.
        VideoSpec videoSpec = mediaSpec.getVideoSpec();
        QualitySelector qualitySelector = videoSpec.getQualitySelector();
        List<Quality> selectedQualities = qualitySelector.getPrioritizedQualities(
                supportedQualities);
        Logger.d(TAG, "Found selectedQualities " + selectedQualities + " by " + qualitySelector);
        if (selectedQualities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to find supported quality by QualitySelector");
        }

        // Get corresponded resolutions for the target aspect ratio.
        int aspectRatio = videoSpec.getAspectRatio();
        Map<Quality, Size> supportedQualityToSizeMap = getQualityToResolutionMap(videoCapabilities,
                requestedDynamicRange);
        QualityRatioToResolutionsTable qualityRatioTable = new QualityRatioToResolutionsTable(
                cameraInfo.getSupportedResolutions(getImageFormat()), supportedQualityToSizeMap);
        List<Size> customOrderedResolutions = new ArrayList<>();
        for (Quality selectedQuality : selectedQualities) {
            customOrderedResolutions.addAll(
                    qualityRatioTable.getResolutions(selectedQuality, aspectRatio));
        }
        List<Size> filteredCustomOrderedResolutions = filterOutEncoderUnsupportedResolutions(
                (VideoCaptureConfig<T>) builder.getUseCaseConfig(), mediaSpec,
                requestedDynamicRange, videoCapabilities, customOrderedResolutions,
                supportedQualityToSizeMap);
        Logger.d(TAG, "Set custom ordered resolutions = " + filteredCustomOrderedResolutions);
        builder.getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS,
                filteredCustomOrderedResolutions);
    }

    @NonNull
    private static List<Size> filterOutEncoderUnsupportedResolutions(
            @NonNull VideoCaptureConfig<?> config,
            @NonNull MediaSpec mediaSpec,
            @NonNull DynamicRange dynamicRange,
            @NonNull VideoCapabilities videoCapabilities,
            @NonNull List<Size> resolutions,
            @NonNull Map<Quality, Size> supportedQualityToSizeMap
    ) {
        if (resolutions.isEmpty()) {
            return resolutions;
        }

        Iterator<Size> iterator = resolutions.iterator();
        while (iterator.hasNext()) {
            Size resolution = iterator.next();
            // To improve performance, there is no need to check for supported qualities'
            // resolutions because the encoder should support them.
            if (supportedQualityToSizeMap.containsValue(resolution)) {
                continue;
            }
            // We must find EncoderProfiles for each resolution because the EncoderProfiles found
            // by resolution may contain different video mine type which leads to different codec.
            VideoValidatedEncoderProfilesProxy encoderProfiles =
                    videoCapabilities.findNearestHigherSupportedEncoderProfilesFor(resolution,
                            dynamicRange);
            if (encoderProfiles == null) {
                continue;
            }
            // If the user set a non-fully specified target DynamicRange, there could be multiple
            // videoProfiles that matches to the DynamicRange. Find the one with the largest
            // supported size as a workaround.
            // If the suggested StreamSpec(i.e. DynamicRange + resolution) is unfortunately over
            // codec supported size, then rely on surface processing (OpenGL) to resize the
            // camera stream.
            VideoEncoderInfo videoEncoderInfo = findLargestSupportedSizeVideoEncoderInfo(
                    config.getVideoEncoderInfoFinder(), encoderProfiles, dynamicRange,
                    mediaSpec, resolution,
                    requireNonNull(config.getTargetFrameRate(Defaults.DEFAULT_FPS_RANGE)));
            if (videoEncoderInfo != null && !videoEncoderInfo.isSizeSupportedAllowSwapping(
                    resolution.getWidth(), resolution.getHeight())) {
                iterator.remove();
            }
        }
        return resolutions;
    }

    @Nullable
    private static VideoEncoderInfo findLargestSupportedSizeVideoEncoderInfo(
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder,
            @NonNull VideoValidatedEncoderProfilesProxy encoderProfiles,
            @NonNull DynamicRange dynamicRange,
            @NonNull MediaSpec mediaSpec,
            @NonNull Size resolution,
            @NonNull Range<Integer> expectedFrameRate) {
        if (dynamicRange.isFullySpecified()) {
            return resolveVideoEncoderInfo(videoEncoderInfoFinder, encoderProfiles,
                    mediaSpec, resolution, dynamicRange, expectedFrameRate);
        }
        // There could be multiple VideoProfiles that match the non-fully specified DynamicRange.
        // The one with the largest supported size will be returned.
        VideoEncoderInfo sizeLargestVideoEncoderInfo = null;
        int largestArea = Integer.MIN_VALUE;
        for (EncoderProfilesProxy.VideoProfileProxy videoProfile :
                encoderProfiles.getVideoProfiles()) {
            if (isHdrSettingsMatched(videoProfile, dynamicRange)) {
                DynamicRange profileDynamicRange = new DynamicRange(
                        videoProfileHdrFormatsToDynamicRangeEncoding(videoProfile.getHdrFormat()),
                        videoProfileBitDepthToDynamicRangeBitDepth(videoProfile.getBitDepth()));
                VideoEncoderInfo videoEncoderInfo =
                        resolveVideoEncoderInfo(videoEncoderInfoFinder, encoderProfiles,
                                mediaSpec, resolution, profileDynamicRange, expectedFrameRate);
                if (videoEncoderInfo == null) {
                    continue;
                }
                // Compare by area size.
                int area = getArea(videoEncoderInfo.getSupportedWidths().getUpper(),
                        videoEncoderInfo.getSupportedHeights().getUpper());
                if (area > largestArea) {
                    largestArea = area;
                    sizeLargestVideoEncoderInfo = videoEncoderInfo;
                }
            }
        }
        return sizeLargestVideoEncoderInfo;
    }

    /**
     * Gets the snapshot value of the given {@link Observable}.
     *
     * <p>Note: Set {@code valueIfMissing} to a non-{@code null} value doesn't mean the method
     * will never return a {@code null} value. The observable could contain exact {@code null}
     * value.
     *
     * @param observable     the observable
     * @param valueIfMissing if the observable doesn't contain value.
     * @param <T>            the value type
     * @return the snapshot value of the given {@link Observable}.
     */
    @Nullable
    private static <T> T fetchObservableValue(@NonNull Observable<T> observable,
            @Nullable T valueIfMissing) {
        ListenableFuture<T> future = observable.fetchData();
        if (!future.isDone()) {
            return valueIfMissing;
        }
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            // Should not happened
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @MainThread
    void setSourceState(@NonNull VideoOutput.SourceState newState) {
        VideoOutput.SourceState oldState = mSourceState;
        if (newState != oldState) {
            mSourceState = newState;
            getOutput().onSourceStateChanged(newState);
        }
    }

    @VisibleForTesting
    @NonNull
    SurfaceRequest getSurfaceRequest() {
        return requireNonNull(mSurfaceRequest);
    }

    /**
     * @inheritDoc
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Set<Integer> getSupportedEffectTargets() {
        Set<Integer> targets = new HashSet<>();
        targets.add(VIDEO_CAPTURE);
        return targets;
    }

    /**
     * Builder for a {@link VideoCapture}.
     *
     * @param <T> the type of VideoOutput
     */
    @SuppressWarnings("ObjectToString")
    public static final class Builder<T extends VideoOutput> implements
            UseCaseConfig.Builder<VideoCapture<T>, VideoCaptureConfig<T>, Builder<T>>,
            ImageOutputConfig.Builder<Builder<T>>, ImageInputConfig.Builder<Builder<T>>,
            ThreadConfig.Builder<Builder<T>> {
        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder(@NonNull T videoOutput) {
            this(createInitialBundle(videoOutput));
        }

        @SuppressWarnings("unchecked")
        private Builder(@NonNull MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            if (!mMutableConfig.containsOption(OPTION_VIDEO_OUTPUT)) {
                throw new IllegalArgumentException("VideoOutput is required");
            }

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(VideoCapture.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setCaptureType(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
            setTargetClass((Class<VideoCapture<T>>) (Type) VideoCapture.class);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static Builder<? extends VideoOutput> fromConfig(@NonNull Config configuration) {
            return new Builder<>(MutableOptionsBundle.from(configuration));
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static <T extends VideoOutput> Builder<T> fromConfig(
                @NonNull VideoCaptureConfig<T> configuration) {
            return new Builder<>(MutableOptionsBundle.from(configuration));
        }

        @NonNull
        private static <T extends VideoOutput> MutableOptionsBundle createInitialBundle(
                @NonNull T videoOutput) {
            MutableOptionsBundle bundle = MutableOptionsBundle.create();
            bundle.insertOption(OPTION_VIDEO_OUTPUT, videoOutput);
            return bundle;
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
        public VideoCaptureConfig<T> getUseCaseConfig() {
            return new VideoCaptureConfig<>(OptionsBundle.from(mMutableConfig));
        }

        /** Sets the associated {@link VideoOutput}. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder<T> setVideoOutput(@NonNull VideoOutput videoOutput) {
            getMutableConfig().insertOption(OPTION_VIDEO_OUTPUT, videoOutput);
            return this;
        }

        @NonNull
        Builder<T> setVideoEncoderInfoFinder(
                @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
            getMutableConfig().insertOption(OPTION_VIDEO_ENCODER_INFO_FINDER,
                    videoEncoderInfoFinder);
            return this;
        }

        /**
         * Builds a {@link VideoCapture} from the current state.
         *
         * @return A {@link VideoCapture} populated with the current state.
         */
        @Override
        @NonNull
        public VideoCapture<T> build() {
            return new VideoCapture<>(getUseCaseConfig());
        }

        // Implementations of TargetConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setTargetClass(@NonNull Class<VideoCapture<T>> targetClass) {
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
         * <p>If not set, the target name will default to an unique name automatically generated
         * with the class canonical name and random UUID.
         *
         * @param targetName A unique string identifier for the instance of the class being
         *                   configured.
         * @return the current Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of ImageOutputConfig.Builder default methods

        /**
         * setTargetAspectRatio is not supported on VideoCapture
         *
         * <p>To set aspect ratio, see {@link Recorder.Builder#setAspectRatio(int)}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio) {
            throw new UnsupportedOperationException("setTargetAspectRatio is not supported.");
        }

        /**
         * Sets the rotation of the intended target for images from this configuration.
         *
         * <p>Valid values include: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
         * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
         *
         * <p>In general, it is best to additionally set the target rotation dynamically on the
         * use case. See {@link VideoCapture#setTargetRotation(int)} for additional
         * documentation.
         *
         * <p>If not set, the target rotation will default to the value of
         * {@link Display#getRotation()} of the default display at the time the use case is bound.
         *
         * <p>For a {@link Recorder} output, the final rotation degrees of the video, including
         * the degrees set by this method and the orientation of the camera sensor, will be
         * reflected by several possibilities, 1) the rotation degrees is written into the video
         * metadata, 2) the video content is directly rotated, 3) both, i.e. rotation metadata
         * and rotated video content which combines to the target rotation. CameraX will choose a
         * strategy according to the use case.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         * @see VideoCapture#setTargetRotation(int)
         * @see android.view.OrientationEventListener
         */
        @NonNull
        @Override
        public Builder<T> setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * Sets the mirror mode.
         *
         * <p>Valid values include: {@link MirrorMode#MIRROR_MODE_OFF},
         * {@link MirrorMode#MIRROR_MODE_ON} and {@link MirrorMode#MIRROR_MODE_ON_FRONT_ONLY}.
         * If not set, it defaults to {@link MirrorMode#MIRROR_MODE_OFF}.
         *
         * <p>This API only changes the mirroring behavior on VideoCapture, but does not affect
         * other UseCases. If the application wants to be consistent with the default
         * {@link Preview} behavior where the rear camera is not mirrored but the front camera is
         * mirrored, then {@link MirrorMode#MIRROR_MODE_ON_FRONT_ONLY} is recommended.
         *
         * @param mirrorMode The mirror mode of the intended target.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder<T> setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            getMutableConfig().insertOption(OPTION_MIRROR_MODE, mirrorMode);
            return this;
        }

        /**
         * setTargetResolution is not supported on VideoCapture
         *
         * <p>To set resolution, see {@link Recorder.Builder#setQualitySelector(QualitySelector)}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setTargetResolution(@NonNull Size resolution) {
            throw new UnsupportedOperationException("setTargetResolution is not supported.");
        }

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION, resolution);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setSupportedResolutions(
                @NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setCustomOrderedResolutions(@NonNull List<Size> resolutions) {
            getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, resolutions);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setResolutionSelector(@NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
            return this;
        }

        // Implementations of ImageInputConfig.Builder default methods

        /**
         * Sets the {@link DynamicRange}.
         *
         * <p>The dynamic range specifies how the range of colors, highlights and shadows that
         * are captured by the video producer are displayed on a display. Some dynamic ranges will
         * allow the video to make full use of the extended range of brightness of a display when
         * the video is played back.
         *
         * <p>The supported dynamic ranges for video capture depend on the capabilities of the
         * camera and the {@link VideoOutput}. The supported dynamic ranges can normally be
         * queried through the specific video output. For example, the available dynamic
         * ranges for the {@link Recorder} video output can be queried through
         * the {@link androidx.camera.video.VideoCapabilities} returned by
         * {@link Recorder#getVideoCapabilities(CameraInfo)} via
         * {@link androidx.camera.video.VideoCapabilities#getSupportedDynamicRanges()}.
         *
         * <p>It is possible to choose a high dynamic range (HDR) with unspecified encoding by
         * providing {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}.
         *
         * <p>If the dynamic range is not provided, the returned video capture use case will use
         * a default of {@link DynamicRange#SDR}.
         *
         * @return The current Builder.
         * @see DynamicRange
         */
        @NonNull
        @Override
        public Builder<T> setDynamicRange(@NonNull DynamicRange dynamicRange) {
            getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE, dynamicRange);
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
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setBackgroundExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_BACKGROUND_EXECUTOR, executor);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder<T> setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }

        /**
         * Sets the target frame rate range in frames per second for the associated VideoCapture
         * use case.
         *
         * <p>This target will be used as a part of the heuristics for the algorithm that determines
         * the final frame rate range and resolution of all concurrently bound use cases.
         *
         * <p>It is not guaranteed that this target frame rate will be the final range,
         * as other use cases as well as frame rate restrictions of the device may affect the
         * outcome of the algorithm that chooses the actual frame rate.
         *
         * <p>For supported frame rates, see {@link CameraInfo#getSupportedFrameRateRanges()}.
         *
         * @param targetFrameRate the target frame rate range.
         */
        @NonNull
        public Builder<T> setTargetFrameRate(@NonNull Range<Integer> targetFrameRate) {
            getMutableConfig().insertOption(OPTION_TARGET_FRAME_RATE, targetFrameRate);
            return this;
        }

        /**
         * Enable video stabilization.
         *
         * <p>It will enable stabilization for the video capture use case. However, it is not
         * guaranteed the stabilization will be enabled for the preview use case. If you want to
         * enable preview stabilization, use
         * {@link Preview.Builder#setPreviewStabilizationEnabled(boolean)} instead.
         *
         * <p>Preview stabilization, where streams are stabilized with the same quality of
         * stabilization for {@link Preview} and {@link VideoCapture} use cases, is enabled. This
         * mode aims to give clients a 'what you see is what you get' effect. In this mode, the
         * FoV reduction will be a maximum of 20 % both horizontally and vertically (10% from
         * left, right, top, bottom) for the given zoom ratio / crop region. The resultant FoV
         * will also be the same across all use cases (that have the same aspect ratio). This is
         * the tradeoff between video stabilization and preview stabilization.
         *
         * <p>It is recommended to query the device capability via
         * {@link VideoCapabilities#isStabilizationSupported()} before enabling this
         * feature, otherwise HAL error might be thrown.
         *
         * <p> If both preview stabilization and video stabilization are enabled or disabled, the
         * final result will be
         *
         * <p>
         * <table>
         * <tr> <th id="rb">Preview</th> <th id="rb">VideoCapture</th>   <th id="rb">Result</th>
         * </tr>
         * <tr> <td>ON</td> <td>ON</td> <td>Both Preview and VideoCapture will be stabilized,
         * VideoCapture quality might be worse than only VideoCapture stabilized</td>
         * </tr>
         * <tr> <td>ON</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>ON</td> <td>NOT SPECIFIED</td> <td>Both Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>ON</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>OFF</td> <td>NOT SPECIFIED</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * <tr> <td>NOT SPECIFIED</td> <td>ON</td> <td>Only VideoCapture will be stabilized,
         * Preview might be stabilized depending on devices</td>
         * </tr>
         * <tr> <td>NOT SPECIFIED</td> <td>OFF</td> <td>None of Preview and VideoCapture will be
         * stabilized</td>  </tr>
         * </table><br>
         *
         * @param enabled True if enable, otherwise false.
         * @return the current Builder.
         *
         * @see VideoCapabilities#isStabilizationSupported()
         * @see Preview.Builder#setPreviewStabilizationEnabled(boolean)
         */
        @NonNull
        public Builder<T> setVideoStabilizationEnabled(boolean enabled) {
            getMutableConfig().insertOption(OPTION_VIDEO_STABILIZATION_MODE,
                    enabled ? StabilizationMode.ON : StabilizationMode.OFF);
            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder<T> setCaptureType(@NonNull UseCaseConfigFactory.CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }

        /**
         * Forces surface processing to be enabled.
         *
         * <p>Typically, surface processing is automatically enabled only when required for a
         * specific effect. However, calling this method will force it to be enabled even if no
         * effect is required. Surface processing creates additional processing through the OpenGL
         * pipeline, affecting performance and memory usage. Camera service may treat the surface
         * differently, potentially impacting video quality and stabilization. So it is generally
         * not recommended to enable it.
         *
         * <p>One example where it might be useful is to work around device compatibility issues.
         * For example, UHD video recording might not work on some devices, but enabling surface
         * processing could work around the issue.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder<T> setSurfaceProcessingForceEnabled() {
            getMutableConfig().insertOption(OPTION_FORCE_ENABLE_SURFACE_PROCESSING, true);
            return this;
        }
    }
}
