/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.camera.video.AudioStats.AUDIO_AMPLITUDE_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_UNKNOWN;
import static androidx.camera.video.VideoRecordEvent.Finalize.VideoRecordError;
import static androidx.camera.video.internal.DebugUtils.readableUs;
import static androidx.camera.video.internal.config.AudioConfigUtil.resolveAudioEncoderConfig;
import static androidx.camera.video.internal.config.AudioConfigUtil.resolveAudioMimeInfo;
import static androidx.camera.video.internal.config.AudioConfigUtil.resolveAudioSettings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.MutableStateObservable;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.StateObservable;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.CloseGuardHelper;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.utils.ArrayRingBuffer;
import androidx.camera.core.internal.utils.RingBuffer;
import androidx.camera.video.StreamInfo.StreamState;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.camera.video.internal.audio.AudioSource;
import androidx.camera.video.internal.audio.AudioSourceAccessException;
import androidx.camera.video.internal.compat.Api26Impl;
import androidx.camera.video.internal.compat.quirk.DeactivateEncoderSurfaceBeforeStopEncoderQuirk;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.EncoderNotUsePersistentInputSurfaceQuirk;
import androidx.camera.video.internal.config.MimeInfo;
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.camera.video.internal.encoder.BufferCopiedEncodedData;
import androidx.camera.video.internal.encoder.EncodeException;
import androidx.camera.video.internal.encoder.EncodedData;
import androidx.camera.video.internal.encoder.Encoder;
import androidx.camera.video.internal.encoder.EncoderCallback;
import androidx.camera.video.internal.encoder.EncoderFactory;
import androidx.camera.video.internal.encoder.EncoderImpl;
import androidx.camera.video.internal.encoder.InvalidConfigException;
import androidx.camera.video.internal.encoder.OutputConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;
import androidx.camera.video.internal.utils.OutputUtil;
import androidx.camera.video.internal.workaround.CorrectNegativeLatLongForMediaMuxer;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link VideoOutput} for starting video recordings that are saved
 * to a {@link File}, {@link ParcelFileDescriptor}, or {@link MediaStore}.
 *
 * <p>A recorder can be used to save the video frames sent from the {@link VideoCapture} use case
 * in common recording formats such as MPEG4.
 *
 * <p>Usage example of setting up {@link VideoCapture} with a recorder as output:
 * <pre>
 * ProcessCameraProvider cameraProvider = ...;
 * CameraSelector cameraSelector = ...;
 * ...
 * // Create our preview to show on screen
 * Preview preview = new Preview.Builder.build();
 * // Create the video capture use case with a Recorder as the output
 * VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(new Recorder.Builder().build());
 *
 * // Bind use cases to Fragment/Activity lifecycle
 * cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
 * </pre>
 *
 * <p>Once the recorder is attached to a video source as a {@link VideoOutput}, e.g. using it to
 * create a {@link VideoCapture} by calling {@link VideoCapture#withOutput(VideoOutput)}, a new
 * recording can be generated with one of the prepareRecording methods, such as
 * {@link #prepareRecording(Context, MediaStoreOutputOptions)}. The {@link PendingRecording} class
 * then can be used to adjust per-recording settings and to start the recording. It also requires
 * passing a listener to {@link PendingRecording#start(Executor, Consumer)} to
 * listen for {@link VideoRecordEvent}s such as {@link VideoRecordEvent.Start},
 * {@link VideoRecordEvent.Pause}, {@link VideoRecordEvent.Resume}, and
 * {@link VideoRecordEvent.Finalize}. This listener will also receive regular recording status
 * updates via the {@link VideoRecordEvent.Status} event.
 *
 * <p>Attaching a single Recorder instance to multiple video sources at the same time may causes
 * unexpected behaviors and is not recommended.
 *
 * <p>A recorder can also capture and save audio alongside video. The audio must be explicitly
 * enabled with {@link PendingRecording#withAudioEnabled()} before starting the recording.
 *
 * @see VideoCapture#withOutput(VideoOutput)
 * @see PendingRecording
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Recorder implements VideoOutput {

    private static final String TAG = "Recorder";

    enum State {
        /**
         * The Recorder is being configured.
         *
         * <p>The Recorder will reach this state whenever it is waiting for a surface request.
         */
        CONFIGURING,
        /**
         * There's a recording waiting for being started.
         *
         * <p>The Recorder will reach this state whenever a recording can not be serviced
         * immediately.
         */
        PENDING_RECORDING,
        /**
         * There's a recording waiting for being paused.
         *
         * <p>The Recorder will reach this state whenever a recording can not be serviced
         * immediately.
         */
        PENDING_PAUSED,
        /**
         * The Recorder is idling and ready to start a new recording.
         */
        IDLING,
        /**
         * There's a running recording and the Recorder is producing output.
         */
        RECORDING,
        /**
         * There's a running recording and it's paused.
         */
        PAUSED,
        /**
         * There's a recording being stopped.
         */
        STOPPING,
        /**
         * There's a running recording and the Recorder is being reset.
         */
        RESETTING,
        /**
         * The Recorder encountered errors and any operation will attempt will throw an
         * {@link IllegalStateException}. Users can handle the error by monitoring
         * {@link VideoRecordEvent}.
         */
        ERROR
    }

    enum AudioState {
        /**
         * The audio is being initializing.
         */
        INITIALIZING,
        /**
         * The audio has been initialized and is waiting for a new recording to be started.
         */
        IDLING,
        /**
         * Audio recording is disabled for the running recording.
         */
        DISABLED,
        /**
         * Audio recording is enabled for the running recording.
         */
        ENABLED,
        /**
         * The audio encoder encountered errors.
         */
        ERROR_ENCODER,
        /**
         * The audio source encountered errors.
         */
        ERROR_SOURCE,
    }

    /**
     * The subset of states considered pending states.
     */
    private static final Set<State> PENDING_STATES =
            Collections.unmodifiableSet(EnumSet.of(State.PENDING_RECORDING, State.PENDING_PAUSED));

    /**
     * The subset of states which are valid non-pending states while in a pending state.
     *
     * <p>All other states should not be possible if in a PENDING_* state. Pending states are
     * meant to be transient states that occur while waiting for another operation to finish.
     */
    private static final Set<State> VALID_NON_PENDING_STATES_WHILE_PENDING =
            Collections.unmodifiableSet(EnumSet.of(
                    State.CONFIGURING, // Waiting for camera before starting recording.
                    State.IDLING, // Waiting for sequential executor to start pending recording.
                    State.RESETTING, // Waiting for camera/encoders to reset before starting.
                    State.STOPPING, // Waiting for previous recording to finalize before starting.
                    State.ERROR // Waiting for re-initialization before starting.
            ));

    /**
     * Default quality selector for recordings.
     *
     * <p>The default quality selector chooses a video quality suitable for recordings based on
     * device and compatibility constraints. It is equivalent to:
     * <pre>{@code
     * QualitySelector.fromOrderedList(Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
     *         FallbackStrategy.higherQualityOrLowerThan(Quality.FHD));
     * }</pre>
     *
     * @see QualitySelector
     */
    public static final QualitySelector DEFAULT_QUALITY_SELECTOR =
            QualitySelector.fromOrderedList(Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD));

    private static final VideoSpec VIDEO_SPEC_DEFAULT =
            VideoSpec.builder()
                    .setQualitySelector(DEFAULT_QUALITY_SELECTOR)
                    .setAspectRatio(AspectRatio.RATIO_DEFAULT)
                    .build();
    private static final MediaSpec MEDIA_SPEC_DEFAULT =
            MediaSpec.builder()
                    .setOutputFormat(MediaSpec.OUTPUT_FORMAT_AUTO)
                    .setVideoSpec(VIDEO_SPEC_DEFAULT)
                    .build();
    @SuppressWarnings("deprecation")
    private static final String MEDIA_COLUMN = MediaStore.Video.Media.DATA;
    private static final Exception PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE =
            new RuntimeException("The video frame producer became inactive before any "
                    + "data was received.");
    private static final int PENDING = 1;
    private static final int NOT_PENDING = 0;
    private static final long SOURCE_NON_STREAMING_TIMEOUT_MS = 1000L;
    // The audio data is expected to be less than 1 kB, the value of the cache size is used to limit
    // the memory used within an acceptable range.
    private static final int AUDIO_CACHE_SIZE = 60;
    @VisibleForTesting
    static final EncoderFactory DEFAULT_ENCODER_FACTORY = EncoderImpl::new;
    private static final Executor AUDIO_EXECUTOR =
            CameraXExecutors.newSequentialExecutor(CameraXExecutors.ioExecutor());

    private final MutableStateObservable<StreamInfo> mStreamInfo;
    // Used only by getExecutor()
    private final Executor mUserProvidedExecutor;
    // May be equivalent to mUserProvidedExecutor or an internal executor if the user did not
    // provide an executor.
    private final Executor mExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mSequentialExecutor;
    private final EncoderFactory mVideoEncoderFactory;
    private final EncoderFactory mAudioEncoderFactory;
    private final Object mLock = new Object();
    private final boolean mEncoderNotUsePersistentInputSurface = DeviceQuirks.get(
            EncoderNotUsePersistentInputSurfaceQuirk.class) != null;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                          Members only accessed when holding mLock                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @GuardedBy("mLock")
    private State mState = State.CONFIGURING;
    // Tracks the underlying state when in a PENDING_* state. When not in a PENDING_* state, this
    // should be null.
    @GuardedBy("mLock")
    private State mNonPendingState = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    int mStreamId = StreamInfo.STREAM_ID_ANY;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    RecordingRecord mActiveRecordingRecord = null;
    // A recording that will be started once the previous recording has finalized or the
    // recorder has finished initializing.
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    RecordingRecord mPendingRecordingRecord = null;
    @GuardedBy("mLock")
    private long mLastGeneratedRecordingId = 0L;
    //--------------------------------------------------------------------------------------------//

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                      Members only accessed on mSequentialExecutor                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private RecordingRecord mInProgressRecording = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mInProgressRecordingStopping = false;
    private SurfaceRequest.TransformationInfo mSurfaceTransformationInfo = null;
    private VideoValidatedEncoderProfilesProxy mResolvedEncoderProfiles = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final List<ListenableFuture<Void>> mEncodingFutures = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mAudioTrackIndex = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mVideoTrackIndex = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SurfaceRequest mLatestSurfaceRequest;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Timebase mVideoSourceTimebase;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Surface mLatestSurface = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Surface mActiveSurface = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    MediaMuxer mMediaMuxer = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MutableStateObservable<MediaSpec> mMediaSpec;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioSource mAudioSource = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Encoder mVideoEncoder = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    OutputConfig mVideoOutputConfig = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Encoder mAudioEncoder = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    OutputConfig mAudioOutputConfig = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioState mAudioState = AudioState.INITIALIZING;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    Uri mOutputUri = Uri.EMPTY;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mRecordingBytes = 0L;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mRecordingDurationNs = 0L;
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFirstRecordingVideoDataTimeUs = Long.MAX_VALUE;
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int mFirstRecordingVideoBitrate = 0;
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Range<Integer> mVideoEncoderBitrateRange = null;
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFirstRecordingAudioDataTimeUs = Long.MAX_VALUE;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mPreviousRecordingVideoDataTimeUs = Long.MAX_VALUE;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mPreviousRecordingAudioDataTimeUs = Long.MAX_VALUE;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFileSizeLimitInBytes = OutputOptions.FILE_SIZE_UNLIMITED;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mDurationLimitNs = OutputOptions.DURATION_UNLIMITED;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @VideoRecordError
    int mRecordingStopError = ERROR_UNKNOWN;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Throwable mRecordingStopErrorCause = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    EncodedData mPendingFirstVideoData = null;
    // A cache that hold audio data created before the muxer starts to prevent A/V out of sync in
    // the beginning of the recording.
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final RingBuffer<EncodedData> mPendingAudioRingBuffer = new ArrayRingBuffer<>(
            AUDIO_CACHE_SIZE);
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Throwable mAudioErrorCause = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mIsAudioSourceSilenced = false;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    SourceState mSourceState = SourceState.INACTIVE;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    ScheduledFuture<?> mSourceNonStreamingTimeout = null;
    // The Recorder has to be reset first before being configured again.
    private boolean mNeedsReset = false;
    @NonNull
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    VideoEncoderSession mVideoEncoderSession;
    @Nullable
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    VideoEncoderSession mVideoEncoderSessionToRelease = null;
    double mAudioAmplitude = 0;
    //--------------------------------------------------------------------------------------------//

    Recorder(@Nullable Executor executor, @NonNull MediaSpec mediaSpec,
            @NonNull EncoderFactory videoEncoderFactory,
            @NonNull EncoderFactory audioEncoderFactory) {
        mUserProvidedExecutor = executor;
        mExecutor = executor != null ? executor : CameraXExecutors.ioExecutor();
        mSequentialExecutor = CameraXExecutors.newSequentialExecutor(mExecutor);

        mMediaSpec = MutableStateObservable.withInitialState(composeRecorderMediaSpec(mediaSpec));
        mStreamInfo = MutableStateObservable.withInitialState(
                StreamInfo.of(mStreamId, internalStateToStreamState(mState)));
        mVideoEncoderFactory = videoEncoderFactory;
        mAudioEncoderFactory = audioEncoderFactory;
        mVideoEncoderSession =
                new VideoEncoderSession(mVideoEncoderFactory, mSequentialExecutor, mExecutor);
    }

    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        onSurfaceRequested(request, Timebase.UPTIME);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request, @NonNull Timebase timebase) {
        synchronized (mLock) {
            Logger.d(TAG, "Surface is requested in state: " + mState + ", Current surface: "
                    + mStreamId);
            if (mState == State.ERROR) {
                setState(State.CONFIGURING);
            }
        }
        mSequentialExecutor.execute(() -> onSurfaceRequestedInternal(request, timebase));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @NonNull
    public Observable<MediaSpec> getMediaSpec() {
        return mMediaSpec;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @NonNull
    public Observable<StreamInfo> getStreamInfo() {
        return mStreamInfo;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onSourceStateChanged(@NonNull SourceState newState) {
        mSequentialExecutor.execute(() -> onSourceStateChangedInternal(newState));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @NonNull
    public VideoCapabilities getMediaCapabilities(@NonNull CameraInfo cameraInfo) {
        return getVideoCapabilities(cameraInfo);
    }

    /**
     * Prepares a recording that will be saved to a {@link File}.
     *
     * <p>The provided {@link FileOutputOptions} specifies the file to use.
     *
     * <p>Calling this method multiple times will generate multiple {@link PendingRecording}s,
     * each of the recordings can be used to adjust per-recording settings individually. The
     * recording will not begin until {@link PendingRecording#start(Executor, Consumer)} is called.
     * Only a single pending recording can be started per {@link Recorder} instance.
     *
     * @param context the context used to enforce runtime permissions, interface with the media
     *                scanner service, and attribute access to permission protected data, such as
     *                audio. If using this context to <a href="{@docRoot}guide
     *                /topics/data/audit-access#audit-by-attribution-tagaudit">audit audio
     *                access</a> on API level 31+, a context created with
     *                {@link Context#createAttributionContext(String)} should be used.
     * @param fileOutputOptions the options that configures how the output will be handled.
     * @return a {@link PendingRecording} that is associated with this Recorder.
     * @see FileOutputOptions
     */
    @NonNull
    public PendingRecording prepareRecording(@NonNull Context context,
            @NonNull FileOutputOptions fileOutputOptions) {
        return prepareRecordingInternal(context, fileOutputOptions);
    }

    /**
     * Prepares a recording that will be saved to a {@link ParcelFileDescriptor}.
     *
     * <p>The provided {@link FileDescriptorOutputOptions} specifies the
     * {@link ParcelFileDescriptor} to use.
     *
     * <p>Currently, file descriptors as output destinations are not supported on pre-Android O
     * (API 26) devices.
     *
     * <p>Calling this method multiple times will generate multiple {@link PendingRecording}s,
     * each of the recordings can be used to adjust per-recording settings individually. The
     * recording will not begin until {@link PendingRecording#start(Executor, Consumer)} is called.
     * Only a single pending recording can be started per {@link Recorder} instance.
     *
     * @param context the context used to enforce runtime permissions, interface with the media
     *                scanner service, and attribute access to permission protected data, such as
     *                audio. If using this context to <a href="{@docRoot}guide
     *                /topics/data/audit-access#audit-by-attribution-tagaudit">audit audio
     *                access</a> on API level 31+, a context created with
     *                {@link Context#createAttributionContext(String)} should be used.
     * @param fileDescriptorOutputOptions the options that configures how the output will be
     *                                    handled.
     * @return a {@link PendingRecording} that is associated with this Recorder.
     * @throws UnsupportedOperationException if this method is called on per-Android O (API 26)
     * devices.
     * @see FileDescriptorOutputOptions
     */
    @RequiresApi(26)
    @NonNull
    public PendingRecording prepareRecording(@NonNull Context context,
            @NonNull FileDescriptorOutputOptions fileDescriptorOutputOptions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw new UnsupportedOperationException(
                    "File descriptors as output destinations are not supported on pre-Android O "
                            + "(API 26) devices.");
        }
        return prepareRecordingInternal(context, fileDescriptorOutputOptions);
    }

    /**
     * Prepares a recording that will be saved to a {@link MediaStore}.
     *
     * <p>The provided {@link MediaStoreOutputOptions} specifies the options which will be used
     * to save the recording to a {@link MediaStore}.
     *
     * <p>Calling this method multiple times will generate multiple {@link PendingRecording}s,
     * each of the recordings can be used to adjust per-recording settings individually. The
     * recording will not begin until {@link PendingRecording#start(Executor, Consumer)} is called.
     * Only a single pending recording can be started per {@link Recorder} instance.
     *
     * @param context the context used to enforce runtime permissions, interface with the media
     *                scanner service, and attribute access to permission protected data, such as
     *                audio. If using this context to <a href="{@docRoot}guide
     *                /topics/data/audit-access#audit-by-attribution-tagaudit">audit audio
     *                access</a> on API level 31+, a context created with
     *                {@link Context#createAttributionContext(String)} should be used.
     * @param mediaStoreOutputOptions the options that configures how the output will be handled.
     * @return a {@link PendingRecording} that is associated with this Recorder.
     * @see MediaStoreOutputOptions
     */
    @NonNull
    public PendingRecording prepareRecording(@NonNull Context context,
            @NonNull MediaStoreOutputOptions mediaStoreOutputOptions) {
        return prepareRecordingInternal(context, mediaStoreOutputOptions);
    }

    @NonNull
    private PendingRecording prepareRecordingInternal(@NonNull Context context,
            @NonNull OutputOptions options) {
        Preconditions.checkNotNull(options, "The OutputOptions cannot be null.");
        return new PendingRecording(context, this, options);
    }

    /**
     * Gets the quality selector of this Recorder.
     *
     * @return the {@link QualitySelector} provided to
     * {@link Builder#setQualitySelector(QualitySelector)} on the builder used to create this
     * recorder, or the default value of {@link Recorder#DEFAULT_QUALITY_SELECTOR} if no quality
     * selector was provided.
     */
    @NonNull
    public QualitySelector getQualitySelector() {
        return getObservableData(mMediaSpec).getVideoSpec().getQualitySelector();
    }

    /**
     * Gets the audio source of this Recorder.
     *
     * @return the value provided to {@link Builder#setAudioSource(int)} on the builder used to
     * create this recorder, or the default value of {@link AudioSpec#SOURCE_AUTO} if no source was
     * set.
     */
    @AudioSpec.Source
    int getAudioSource() {
        return getObservableData(mMediaSpec).getAudioSpec().getSource();
    }

    /**
     * Returns the executor provided to the builder for this recorder.
     *
     * @return the {@link Executor} provided to {@link Builder#setExecutor(Executor)} on the
     * builder used to create this recorder. If no executor was provided, returns {code null}.
     */
    @Nullable
    public Executor getExecutor() {
        return mUserProvidedExecutor;
    }

    /**
     * Gets the target video encoding bitrate of this Recorder.
     *
     * @return the value provided to {@link Builder#setTargetVideoEncodingBitRate(int)} on the
     * builder used to create this recorder. Returns 0, if
     * {@link Builder#setTargetVideoEncodingBitRate(int)} is not called.
     */
    public int getTargetVideoEncodingBitRate() {
        return getObservableData(mMediaSpec).getVideoSpec().getBitrate().getLower();
    }

    /**
     * Gets the aspect ratio of this Recorder.
     *
     * @return the value from {@link Builder#setAspectRatio(int)} or
     * {@link AspectRatio#RATIO_DEFAULT} if not set.
     */
    @AspectRatio.Ratio
    public int getAspectRatio() {
        return getObservableData(mMediaSpec).getVideoSpec().getAspectRatio();
    }

    /**
     * Starts a pending recording and returns an active recording instance.
     *
     * <p>If the Recorder is already running a recording, an {@link IllegalStateException} will
     * be thrown when calling this method.
     *
     * <p>If the video encoder hasn't been setup with {@link #onSurfaceRequested(SurfaceRequest)}
     * , the {@link PendingRecording} specified will be started once the video encoder setup
     * completes. The recording will be considered active, so before it's finalized, an
     * {@link IllegalStateException} will be thrown if this method is called for a second time.
     *
     * <p>If the video producer stops sending frames to the provided surface, the recording will
     * be automatically finalized with {@link VideoRecordEvent.Finalize#ERROR_SOURCE_INACTIVE}.
     * This can happen, for example, when the {@link VideoCapture} this Recorder is associated
     * with is detached from the camera.
     *
     * @throws IllegalStateException if there's an active recording, or the audio is
     *                               {@link PendingRecording#withAudioEnabled() enabled} for the
     *                               recording but
     *                               {@link android.Manifest.permission#RECORD_AUDIO} is not
     *                               granted.
     */
    @NonNull
    Recording start(@NonNull PendingRecording pendingRecording) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        RecordingRecord alreadyInProgressRecording = null;
        @VideoRecordError int error = ERROR_NONE;
        Throwable errorCause = null;
        long recordingId;
        synchronized (mLock) {
            recordingId = ++mLastGeneratedRecordingId;
            switch (mState) {
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    alreadyInProgressRecording = mActiveRecordingRecord;
                    break;
                case PENDING_PAUSED:
                    // Fall-through
                case PENDING_RECORDING:
                    // There is already a recording pending that hasn't been stopped.
                    alreadyInProgressRecording =
                            Preconditions.checkNotNull(mPendingRecordingRecord);
                    break;
                case RESETTING:
                    // Fall-through
                case STOPPING:
                    // Fall-through
                case CONFIGURING:
                    // Fall-through
                case ERROR:
                    // Fall-through
                case IDLING:
                    if (mState == State.IDLING) {
                        Preconditions.checkState(
                                mActiveRecordingRecord == null
                                        && mPendingRecordingRecord == null,
                                "Expected recorder to be idle but a recording is either "
                                        + "pending or in progress.");
                    }
                    try {
                        RecordingRecord recordingRecord = RecordingRecord.from(pendingRecording,
                                recordingId);
                        recordingRecord.initializeRecording(
                                pendingRecording.getApplicationContext());
                        mPendingRecordingRecord = recordingRecord;
                        if (mState == State.IDLING) {
                            setState(State.PENDING_RECORDING);
                            mSequentialExecutor.execute(this::tryServicePendingRecording);
                        } else if (mState == State.ERROR) {
                            setState(State.PENDING_RECORDING);
                            // Retry initialization.
                            mSequentialExecutor.execute(() -> {
                                if (mLatestSurfaceRequest == null) {
                                    throw new AssertionError(
                                            "surface request is required to retry "
                                                    + "initialization.");
                                }
                                configureInternal(mLatestSurfaceRequest, mVideoSourceTimebase);
                            });
                        } else {
                            setState(State.PENDING_RECORDING);
                            // The recording will automatically start once the initialization
                            // completes.
                        }
                    } catch (IOException e) {
                        error = ERROR_INVALID_OUTPUT_OPTIONS;
                        errorCause = e;
                    }
                    break;
            }
        }

        if (alreadyInProgressRecording != null) {
            throw new IllegalStateException("A recording is already in progress. Previous "
                    + "recordings must be stopped before a new recording can be started.");
        } else if (error != ERROR_NONE) {
            Logger.e(TAG,
                    "Recording was started when the Recorder had encountered error " + errorCause);
            // Immediately update the listener if the Recorder encountered an error.
            finalizePendingRecording(RecordingRecord.from(pendingRecording, recordingId),
                    error, errorCause);
            return Recording.createFinalizedFrom(pendingRecording, recordingId);
        }

        return Recording.from(pendingRecording, recordingId);
    }

    void pause(@NonNull Recording activeRecording) {
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this Recording is no longer active, log and treat as a no-op.
                // This is not technically an error since the recording can be finalized
                // asynchronously.
                Logger.d(TAG,
                        "pause() called on a recording that is no longer active: "
                                + activeRecording.getOutputOptions());
                return;
            }

            switch (mState) {
                case PENDING_RECORDING:
                    // The recording will automatically pause once the initialization completes.
                    setState(State.PENDING_PAUSED);
                    break;
                case CONFIGURING:
                    // Fall-through
                case IDLING:
                    throw new IllegalStateException("Called pause() from invalid state: " + mState);
                case RECORDING:
                    setState(State.PAUSED);
                    RecordingRecord finalActiveRecordingRecord = mActiveRecordingRecord;
                    mSequentialExecutor.execute(() -> pauseInternal(finalActiveRecordingRecord));
                    break;
                case PENDING_PAUSED:
                    // Fall-through
                case PAUSED:
                    // No-op when the recording is already paused.
                    break;
                case RESETTING:
                    // Fall-through
                case STOPPING:
                    // If recorder is resetting or stopping, then pause is a no-op.
                    break;
                case ERROR:
                    // In an error state, the recording will already be finalized. Treat as a
                    // no-op in pause()
                    break;
            }
        }
    }

    void resume(@NonNull Recording activeRecording) {
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this Recording is no longer active, log and treat as a no-op.
                // This is not technically an error since the recording can be finalized
                // asynchronously.
                Logger.d(TAG,
                        "resume() called on a recording that is no longer active: "
                                + activeRecording.getOutputOptions());
                return;
            }
            switch (mState) {
                case PENDING_PAUSED:
                    // The recording will automatically start once the initialization completes.
                    setState(State.PENDING_RECORDING);
                    break;
                case CONFIGURING:
                    // Should not be able to resume when initializing. Should be in a PENDING state.
                    // Fall-through
                case IDLING:
                    throw new IllegalStateException("Called resume() from invalid state: "
                            + mState);
                case RESETTING:
                    // Fall-through
                case STOPPING:
                    // If recorder is stopping or resetting, then resume is a no-op.
                    // Fall-through
                case PENDING_RECORDING:
                    // Fall-through
                case RECORDING:
                    // No-op when the recording is running.
                    break;
                case PAUSED:
                    setState(State.RECORDING);
                    RecordingRecord finalActiveRecordingRecord = mActiveRecordingRecord;
                    mSequentialExecutor.execute(() -> resumeInternal(finalActiveRecordingRecord));
                    break;
                case ERROR:
                    // In an error state, the recording will already be finalized. Treat as a
                    // no-op in resume()
                    break;
            }
        }
    }

    void stop(@NonNull Recording activeRecording, @VideoRecordError int error,
            @Nullable Throwable errorCause) {
        RecordingRecord pendingRecordingToFinalize = null;
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this Recording is no longer active, log and treat as a no-op.
                // This is not technically an error since the recording can be finalized
                // asynchronously.
                Logger.d(TAG,
                        "stop() called on a recording that is no longer active: "
                                + activeRecording.getOutputOptions());
                return;
            }
            switch (mState) {
                case PENDING_RECORDING:
                    // Fall-through
                case PENDING_PAUSED:
                    // Immediately finalize pending recording since it never started.
                    Preconditions.checkState(isSameRecording(activeRecording,
                            mPendingRecordingRecord));
                    pendingRecordingToFinalize = mPendingRecordingRecord;
                    mPendingRecordingRecord = null;
                    restoreNonPendingState(); // Equivalent to setState(mNonPendingState)
                    break;
                case STOPPING:
                    // Fall-through
                case RESETTING:
                    // We are already resetting, likely due to an error that stopped the recording.
                    // Ensure this is the current active recording and treat as a no-op. The
                    // active recording will be cleared once stop/reset is complete.
                    Preconditions.checkState(isSameRecording(activeRecording,
                            mActiveRecordingRecord));
                    break;
                case CONFIGURING:
                    // Fall-through
                case IDLING:
                    throw new IllegalStateException("Calling stop() while idling or initializing "
                            + "is invalid.");
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    setState(State.STOPPING);
                    long explicitlyStopTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
                    RecordingRecord finalActiveRecordingRecord = mActiveRecordingRecord;
                    mSequentialExecutor.execute(() -> stopInternal(finalActiveRecordingRecord,
                            explicitlyStopTimeUs, error, errorCause));
                    break;
                case ERROR:
                    // In an error state, the recording will already be finalized. Treat as a
                    // no-op in stop()
                    break;
            }
        }

        if (pendingRecordingToFinalize != null) {
            if (error == VideoRecordEvent.Finalize.ERROR_RECORDING_GARBAGE_COLLECTED) {
                Logger.e(TAG, "Recording was stopped due to recording being garbage collected "
                        + "before any valid data has been produced.");
            }
            finalizePendingRecording(pendingRecordingToFinalize, ERROR_NO_VALID_DATA,
                    new RuntimeException("Recording was stopped before any data could be "
                            + "produced.", errorCause));
        }
    }

    void mute(@NonNull Recording activeRecording, boolean muted) {
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this Recording is no longer active, log and treat as a no-op.
                // This is not technically an error since the recording can be finalized
                // asynchronously.
                Logger.d(TAG,
                        "mute() called on a recording that is no longer active: "
                                + activeRecording.getOutputOptions());
                return;
            }
            RecordingRecord finalRecordingRecord = isSameRecording(activeRecording,
                    mPendingRecordingRecord) ? mPendingRecordingRecord : mActiveRecordingRecord;
            mSequentialExecutor.execute(() -> muteInternal(finalRecordingRecord, muted));
        }
    }

    private void finalizePendingRecording(@NonNull RecordingRecord recordingToFinalize,
            @VideoRecordError int error, @Nullable Throwable cause) {
        recordingToFinalize.finalizeRecording(Uri.EMPTY);
        recordingToFinalize.updateVideoRecordEvent(
                VideoRecordEvent.finalizeWithError(
                        recordingToFinalize.getOutputOptions(),
                        RecordingStats.of(/*duration=*/0L,
                                /*bytes=*/0L,
                                AudioStats.of(AudioStats.AUDIO_STATE_DISABLED, mAudioErrorCause,
                                        AUDIO_AMPLITUDE_NONE)),
                        OutputResults.of(Uri.EMPTY),
                        error,
                        cause));
    }

    @ExecutedBy("mSequentialExecutor")
    private void onSurfaceRequestedInternal(@NonNull SurfaceRequest request,
            @NonNull Timebase timebase) {
        if (mLatestSurfaceRequest != null && !mLatestSurfaceRequest.isServiced()) {
            mLatestSurfaceRequest.willNotProvideSurface();
        }
        configureInternal(mLatestSurfaceRequest = request, mVideoSourceTimebase = timebase);
    }

    @ExecutedBy("mSequentialExecutor")
    void onSourceStateChangedInternal(@NonNull SourceState newState) {
        SourceState oldState = mSourceState;
        mSourceState = newState;
        if (oldState != newState) {
            Logger.d(TAG, "Video source has transitioned to state: " + newState);
        } else {
            Logger.d(TAG, "Video source transitions to the same state: " + newState);
            return;
        }

        if (newState == SourceState.INACTIVE) {
            if (mActiveSurface == null) {
                // If we're inactive and have no active surface, we'll reset the encoder directly.
                // Otherwise, we'll wait for the active surface's surface request listener to
                // reset the encoder.
                requestReset(ERROR_SOURCE_INACTIVE, null);
            } else {
                // The source becomes inactive, the incoming new surface request has to be cached
                // and be serviced after the Recorder is reset when receiving the previous
                // surface request complete callback.
                mNeedsReset = true;
                if (mInProgressRecording != null) {
                    // Stop any in progress recording with "source inactive" error
                    onInProgressRecordingInternalError(mInProgressRecording, ERROR_SOURCE_INACTIVE,
                            null);
                }
            }
        } else if (newState == SourceState.ACTIVE_NON_STREAMING) {
            // We are expecting the source to transition to NON_STREAMING state.
            if (mSourceNonStreamingTimeout != null && mSourceNonStreamingTimeout.cancel(false)
                    && mVideoEncoder != null) {
                notifyEncoderSourceStopped(mVideoEncoder);
            }
        }
    }

    /**
     * Requests the Recorder to be reset.
     *
     * <p>If a recording is in progress, it will be stopped asynchronously and reset once it has
     * been finalized.
     *
     * <p>The Recorder is expected to be reset when there's no active surface. Otherwise, wait for
     * the surface request complete callback first.
     */
    @ExecutedBy("mSequentialExecutor")
    void requestReset(@VideoRecordError int errorCode, @Nullable Throwable errorCause) {
        boolean shouldReset = false;
        boolean shouldStop = false;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_RECORDING:
                    // Fall-through
                case PENDING_PAUSED:
                    // Fall-through
                    shouldReset = true;
                    updateNonPendingState(State.RESETTING);
                    break;
                case ERROR:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case CONFIGURING:
                    shouldReset = true;
                    break;
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    if (mActiveRecordingRecord != mInProgressRecording) {
                        throw new AssertionError("In-progress recording does not match the active"
                                + " recording. Unable to reset encoder.");
                    }
                    // If there's an active recording, stop it first then release the resources
                    // at onRecordingFinalized().
                    shouldStop = true;
                    // Fall-through
                case STOPPING:
                    // Already stopping. Set state to RESETTING so resources will be released once
                    // onRecordingFinalized() runs.
                    setState(State.RESETTING);
                    break;
                case RESETTING:
                    // No-Op, the Recorder is already being reset.
                    break;
            }
        }

        // These calls must not be posted to the executor to ensure they are executed inline on
        // the sequential executor and the state changes above are correctly handled.
        if (shouldReset) {
            reset();
        } else if (shouldStop) {
            stopInternal(mInProgressRecording, Encoder.NO_TIMESTAMP, errorCode, errorCause);
        }
    }

    @ExecutedBy("mSequentialExecutor")

    private void configureInternal(@NonNull SurfaceRequest surfaceRequest,
            @NonNull Timebase videoSourceTimebase) {
        if (surfaceRequest.isServiced()) {
            Logger.w(TAG, "Ignore the SurfaceRequest since it is already served.");
            return;
        }
        surfaceRequest.setTransformationInfoListener(mSequentialExecutor,
                (transformationInfo) -> mSurfaceTransformationInfo = transformationInfo);
        Size surfaceSize = surfaceRequest.getResolution();
        // Fetch and cache nearest encoder profiles, if one exists.
        DynamicRange dynamicRange = surfaceRequest.getDynamicRange();
        VideoCapabilities capabilities = getVideoCapabilities(
                surfaceRequest.getCamera().getCameraInfo());
        Quality highestSupportedQuality = capabilities.findHighestSupportedQualityFor(surfaceSize,
                dynamicRange);
        Logger.d(TAG, "Using supported quality of " + highestSupportedQuality
                + " for surface size " + surfaceSize);
        if (highestSupportedQuality != Quality.NONE) {
            mResolvedEncoderProfiles = capabilities.getProfiles(highestSupportedQuality,
                    dynamicRange);
            if (mResolvedEncoderProfiles == null) {
                throw new AssertionError("Camera advertised available quality but did not "
                        + "produce EncoderProfiles  for advertised quality.");
            }
        }
        setupVideo(surfaceRequest, videoSourceTimebase);
    }

    @SuppressWarnings("ObjectToString")
    @ExecutedBy("mSequentialExecutor")
    private void setupVideo(@NonNull SurfaceRequest request, @NonNull Timebase timebase) {
        safeToCloseVideoEncoder().addListener(() -> {
            if (request.isServiced() || mVideoEncoderSession.isConfiguredSurfaceRequest(request)) {
                Logger.w(TAG, "Ignore the SurfaceRequest " + request + " isServiced: "
                        + request.isServiced() + " VideoEncoderSession: " + mVideoEncoderSession);
                return;
            }
            VideoEncoderSession videoEncoderSession =
                    new VideoEncoderSession(mVideoEncoderFactory, mSequentialExecutor, mExecutor);
            MediaSpec mediaSpec = getObservableData(mMediaSpec);
            ListenableFuture<Encoder> configureFuture =
                    videoEncoderSession.configure(request, timebase, mediaSpec,
                            mResolvedEncoderProfiles);
            mVideoEncoderSession = videoEncoderSession;
            Futures.addCallback(configureFuture, new FutureCallback<Encoder>() {
                @Override
                public void onSuccess(@Nullable Encoder result) {
                    Logger.d(TAG, "VideoEncoder is created. " + result);
                    if (result == null) {
                        return;
                    }
                    Preconditions.checkState(mVideoEncoderSession == videoEncoderSession);
                    Preconditions.checkState(mVideoEncoder == null);
                    onVideoEncoderReady(videoEncoderSession);
                    onConfigured();
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Logger.d(TAG, "VideoEncoder Setup error: " + t);
                    onEncoderSetupError(t);
                }
            }, mSequentialExecutor);
        }, mSequentialExecutor);
    }

    @NonNull
    @ExecutedBy("mSequentialExecutor")
    private ListenableFuture<Void> safeToCloseVideoEncoder() {
        Logger.d(TAG, "Try to safely release video encoder: " + mVideoEncoder);
        return mVideoEncoderSession.signalTermination();
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void onVideoEncoderReady(@NonNull VideoEncoderSession videoEncoderSession) {
        mVideoEncoder = videoEncoderSession.getVideoEncoder();
        mVideoEncoderBitrateRange =
                ((VideoEncoderInfo) mVideoEncoder.getEncoderInfo()).getSupportedBitrateRange();
        mFirstRecordingVideoBitrate = mVideoEncoder.getConfiguredBitrate();
        mActiveSurface = videoEncoderSession.getActiveSurface();
        setLatestSurface(mActiveSurface);

        videoEncoderSession.setOnSurfaceUpdateListener(mSequentialExecutor, this::setLatestSurface);

        Futures.addCallback(videoEncoderSession.getReadyToReleaseFuture(),
                new FutureCallback<Encoder>() {
                    @Override
                    public void onSuccess(@Nullable Encoder result) {
                        Logger.d(TAG, "VideoEncoder can be released: " + result);
                        if (result == null) {
                            return;
                        }
                        if (mSourceNonStreamingTimeout != null
                                && mSourceNonStreamingTimeout.cancel(false)
                                && mVideoEncoder != null && mVideoEncoder == result) {
                            notifyEncoderSourceStopped(mVideoEncoder);
                        }

                        mVideoEncoderSessionToRelease = videoEncoderSession;
                        setLatestSurface(null);
                        requestReset(ERROR_SOURCE_INACTIVE, null);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Logger.d(TAG, "Error in ReadyToReleaseFuture: " + t);
                    }
                }, mSequentialExecutor);
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void onConfigured() {
        RecordingRecord recordingToStart = null;
        RecordingRecord pendingRecordingToFinalize = null;
        @VideoRecordError int error = ERROR_NONE;
        Throwable errorCause = null;
        boolean startRecordingPaused = false;
        synchronized (mLock) {
            switch (mState) {
                case IDLING:
                    // Fall-through
                case RECORDING:
                    // Fall-through
                case PAUSED:
                    // Fall-through
                case RESETTING:
                    throw new AssertionError(
                            "Incorrectly invoke onConfigured() in state " + mState);
                case STOPPING:
                    if (!mEncoderNotUsePersistentInputSurface) {
                        throw new AssertionError("Unexpectedly invoke onConfigured() in a "
                                + "STOPPING state when it's not waiting for a new surface.");
                    }
                    break;
                case CONFIGURING:
                    setState(State.IDLING);
                    break;
                case ERROR:
                    Logger.e(TAG,
                            "onConfigured() was invoked when the Recorder had encountered error");
                    break;
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall through
                case PENDING_RECORDING:
                    if (mActiveRecordingRecord != null) {
                        // Active recording is still finalizing. Pending recording will be
                        // serviced in onRecordingFinalized().
                        break;
                    }
                    if (mSourceState == SourceState.INACTIVE) {
                        pendingRecordingToFinalize = mPendingRecordingRecord;
                        mPendingRecordingRecord = null;
                        restoreNonPendingState(); // Equivalent to setState(mNonPendingState)
                        error = ERROR_SOURCE_INACTIVE;
                        errorCause = PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE;
                    } else {
                        recordingToStart = makePendingRecordingActiveLocked(mState);
                    }
                    break;
            }
        }

        if (recordingToStart != null) {
            // Start new active recording inline on sequential executor (but unlocked).
            startRecording(recordingToStart, startRecordingPaused);
        } else if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, error, errorCause);
        }
    }

    @NonNull
    private MediaSpec composeRecorderMediaSpec(@NonNull MediaSpec mediaSpec) {
        MediaSpec.Builder mediaSpecBuilder = mediaSpec.toBuilder();

        // Append default video configurations
        VideoSpec videoSpec = mediaSpec.getVideoSpec();
        if (videoSpec.getAspectRatio() == AspectRatio.RATIO_DEFAULT) {
            mediaSpecBuilder.configureVideo(
                    builder -> builder.setAspectRatio(VIDEO_SPEC_DEFAULT.getAspectRatio()));
        }

        return mediaSpecBuilder.build();
    }

    private static boolean isSameRecording(@NonNull Recording activeRecording,
            @Nullable RecordingRecord recordingRecord) {
        if (recordingRecord == null) {
            return false;
        }

        return activeRecording.getRecordingId() == recordingRecord.getRecordingId();
    }

    /**
     * Setup audio related resources.
     *
     * @throws AudioSourceAccessException if the audio source failed to be setup.
     * @throws InvalidConfigException if the audio encoder failed to be setup.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @ExecutedBy("mSequentialExecutor")
    private void setupAudio(@NonNull RecordingRecord recordingToStart)
            throws AudioSourceAccessException, InvalidConfigException {
        MediaSpec mediaSpec = getObservableData(mMediaSpec);
        // Resolve the audio mime info
        MimeInfo audioMimeInfo = resolveAudioMimeInfo(mediaSpec, mResolvedEncoderProfiles);
        Timebase audioSourceTimebase = Timebase.UPTIME;

        // Select and create the audio source
        AudioSettings audioSettings =
                resolveAudioSettings(audioMimeInfo, mediaSpec.getAudioSpec());
        if (mAudioSource != null) {
            releaseCurrentAudioSource();
        }
        // TODO: set audioSourceTimebase to AudioSource. Currently AudioSource hard code
        //  AudioTimestamp.TIMEBASE_MONOTONIC.
        mAudioSource = setupAudioSource(recordingToStart, audioSettings);
        Logger.d(TAG, String.format("Set up new audio source: 0x%x", mAudioSource.hashCode()));

        // Select and create the audio encoder
        AudioEncoderConfig audioEncoderConfig = resolveAudioEncoderConfig(audioMimeInfo,
                audioSourceTimebase, audioSettings, mediaSpec.getAudioSpec());
        mAudioEncoder = mAudioEncoderFactory.createEncoder(mExecutor, audioEncoderConfig);

        // Connect the audio source to the audio encoder
        Encoder.EncoderInput bufferProvider = mAudioEncoder.getInput();
        if (!(bufferProvider instanceof Encoder.ByteBufferInput)) {
            throw new AssertionError("The EncoderInput of audio isn't a ByteBufferInput.");
        }
        mAudioSource.setBufferProvider((Encoder.ByteBufferInput) bufferProvider);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @NonNull
    private AudioSource setupAudioSource(@NonNull RecordingRecord recordingToStart,
            @NonNull AudioSettings audioSettings)
            throws AudioSourceAccessException {
        return recordingToStart.performOneTimeAudioSourceCreation(audioSettings, AUDIO_EXECUTOR);
    }

    private void releaseCurrentAudioSource() {
        if (mAudioSource == null) {
            throw new AssertionError("Cannot release null audio source.");
        }
        AudioSource audioSource = mAudioSource;
        mAudioSource = null;
        Logger.d(TAG, String.format("Releasing audio source: 0x%x", audioSource.hashCode()));
        // Run callback on direct executor since it is only logging
        Futures.addCallback(audioSource.release(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                Logger.d(TAG, String.format("Released audio source successfully: 0x%x",
                        audioSource.hashCode()));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Logger.d(TAG, String.format("An error occurred while attempting to "
                        + "release audio source: 0x%x", audioSource.hashCode()));
            }
        }, CameraXExecutors.directExecutor());
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void onEncoderSetupError(@Nullable Throwable cause) {
        RecordingRecord pendingRecordingToFinalize = null;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_PAUSED:
                    // Fall-through
                case PENDING_RECORDING:
                    pendingRecordingToFinalize = mPendingRecordingRecord;
                    mPendingRecordingRecord = null;
                    // Fall-through
                case CONFIGURING:
                    setStreamId(StreamInfo.STREAM_ID_ERROR);
                    setState(State.ERROR);
                    break;
                case ERROR:
                    // Already in an error state. Ignore new error.
                    break;
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case RESETTING:
                    // Fall-through
                case STOPPING:
                    throw new AssertionError("Encountered encoder setup error while in unexpected"
                            + " state " + mState + ": " + cause);
            }
        }

        if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, ERROR_RECORDER_ERROR, cause);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void setupAndStartMediaMuxer(@NonNull RecordingRecord recordingToStart) {
        if (mMediaMuxer != null) {
            throw new AssertionError("Unable to set up media muxer when one already exists.");
        }

        if (isAudioEnabled() && mPendingAudioRingBuffer.isEmpty()) {
            throw new AssertionError("Audio is enabled but no audio sample is ready. Cannot start"
                    + " media muxer.");
        }

        if (mPendingFirstVideoData == null) {
            throw new AssertionError("Media muxer cannot be started without an encoded video "
                    + "frame.");
        }

        try (EncodedData videoDataToWrite = mPendingFirstVideoData) {
            mPendingFirstVideoData = null;
            List<EncodedData> audioDataToWrite = getAudioDataToWriteAndClearCache(
                    videoDataToWrite.getPresentationTimeUs()
            );
            // Make sure we can write the first audio and video data without hitting the file size
            // limit. Otherwise we will be left with a malformed (empty) track on stop.
            long firstDataSize = videoDataToWrite.size();
            for (EncodedData data : audioDataToWrite) {
                firstDataSize += data.size();
            }
            if (mFileSizeLimitInBytes != OutputOptions.FILE_SIZE_UNLIMITED
                    && firstDataSize > mFileSizeLimitInBytes) {
                Logger.d(TAG,
                        String.format("Initial data exceeds file size limit %d > %d", firstDataSize,
                                mFileSizeLimitInBytes));
                onInProgressRecordingInternalError(recordingToStart,
                        ERROR_FILE_SIZE_LIMIT_REACHED, null);
                return;
            }

            MediaMuxer mediaMuxer;
            try {
                MediaSpec mediaSpec = getObservableData(mMediaSpec);
                int muxerOutputFormat =
                        mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO
                                ? supportedMuxerFormatOrDefaultFrom(mResolvedEncoderProfiles,
                                MediaSpec.outputFormatToMuxerFormat(
                                        MEDIA_SPEC_DEFAULT.getOutputFormat()))
                                : MediaSpec.outputFormatToMuxerFormat(mediaSpec.getOutputFormat());
                mediaMuxer = recordingToStart.performOneTimeMediaMuxerCreation(muxerOutputFormat,
                        uri -> mOutputUri = uri);
            } catch (IOException e) {
                onInProgressRecordingInternalError(recordingToStart, ERROR_INVALID_OUTPUT_OPTIONS,
                        e);
                return;
            }

            if (mSurfaceTransformationInfo != null) {
                mediaMuxer.setOrientationHint(mSurfaceTransformationInfo.getRotationDegrees());
            }
            Location location = recordingToStart.getOutputOptions().getLocation();
            if (location != null) {
                try {
                    Pair<Double, Double> geoLocation =
                            CorrectNegativeLatLongForMediaMuxer.adjustGeoLocation(
                                    location.getLatitude(), location.getLongitude());
                    mediaMuxer.setLocation((float) geoLocation.first.doubleValue(),
                            (float) geoLocation.second.doubleValue());
                } catch (IllegalArgumentException e) {
                    mediaMuxer.release();
                    onInProgressRecordingInternalError(recordingToStart,
                            ERROR_INVALID_OUTPUT_OPTIONS, e);
                    return;
                }
            }

            mVideoTrackIndex = mediaMuxer.addTrack(mVideoOutputConfig.getMediaFormat());
            if (isAudioEnabled()) {
                mAudioTrackIndex = mediaMuxer.addTrack(mAudioOutputConfig.getMediaFormat());
            }
            mediaMuxer.start();

            // MediaMuxer is successfully initialized, transfer the ownership to Recorder.
            mMediaMuxer = mediaMuxer;

            // Write first data to ensure tracks are not empty
            writeVideoData(videoDataToWrite, recordingToStart);
            for (EncodedData data : audioDataToWrite) {
                writeAudioData(data, recordingToStart);
            }
        }
    }

    @ExecutedBy("mSequentialExecutor")
    @NonNull
    private List<EncodedData> getAudioDataToWriteAndClearCache(long firstVideoDataTimeUs) {
        List<EncodedData> res = new ArrayList<>();

        while (!mPendingAudioRingBuffer.isEmpty()) {
            EncodedData data = mPendingAudioRingBuffer.dequeue();

            // Add all audio data that has timestamp greater than or equal to the first video data
            // timestamp.
            if (data.getPresentationTimeUs() >= firstVideoDataTimeUs) {
                res.add(data);
            }
        }

        return res;
    }

    @SuppressLint("MissingPermission")
    @ExecutedBy("mSequentialExecutor")
    private void startInternal(@NonNull RecordingRecord recordingToStart) {
        if (mInProgressRecording != null) {
            throw new AssertionError("Attempted to start a new recording while another was in "
                    + "progress.");
        }

        if (recordingToStart.getOutputOptions().getFileSizeLimit() > 0) {
            // Use %95 of the given file size limit as the criteria, which refers to the
            // MPEG4Writer.cpp in libstagefright.
            mFileSizeLimitInBytes = Math.round(
                    recordingToStart.getOutputOptions().getFileSizeLimit() * 0.95);
            Logger.d(TAG, "File size limit in bytes: " + mFileSizeLimitInBytes);
        } else {
            mFileSizeLimitInBytes = OutputOptions.FILE_SIZE_UNLIMITED;
        }

        if (recordingToStart.getOutputOptions().getDurationLimitMillis() > 0) {
            mDurationLimitNs = TimeUnit.MILLISECONDS.toNanos(
                    recordingToStart.getOutputOptions().getDurationLimitMillis());
            Logger.d(TAG, "Duration limit in nanoseconds: " + mDurationLimitNs);
        } else {
            mDurationLimitNs = OutputOptions.DURATION_UNLIMITED;
        }

        mInProgressRecording = recordingToStart;

        // Configure audio based on the current audio state.
        switch (mAudioState) {
            case ERROR_ENCODER:
                // Fall-through
            case ERROR_SOURCE:
                // Fall-through
            case ENABLED:
                // Fall-through
            case DISABLED:
                throw new AssertionError(
                        "Incorrectly invoke startInternal in audio state " + mAudioState);
            case IDLING:
                setAudioState(recordingToStart.hasAudioEnabled() ? AudioState.ENABLED
                        : AudioState.DISABLED);
                break;
            case INITIALIZING:
                if (recordingToStart.hasAudioEnabled()) {
                    if (!isAudioSupported()) {
                        throw new AssertionError(
                                "The Recorder doesn't support recording with audio");
                    }
                    try {
                        setupAudio(recordingToStart);
                        setAudioState(AudioState.ENABLED);
                    } catch (AudioSourceAccessException | InvalidConfigException e) {
                        Logger.e(TAG, "Unable to create audio resource with error: ", e);
                        AudioState audioState;
                        if (e instanceof InvalidConfigException) {
                            audioState = AudioState.ERROR_ENCODER;
                        } else {
                            audioState = AudioState.ERROR_SOURCE;
                        }
                        setAudioState(audioState);
                        mAudioErrorCause = e;
                    }
                }
                break;
        }

        initEncoderAndAudioSourceCallbacks(recordingToStart);
        if (isAudioEnabled()) {
            mAudioSource.start(recordingToStart.isMuted());
            mAudioEncoder.start();
        }
        mVideoEncoder.start();

        mInProgressRecording.updateVideoRecordEvent(VideoRecordEvent.start(
                mInProgressRecording.getOutputOptions(),
                getInProgressRecordingStats()));
    }

    @ExecutedBy("mSequentialExecutor")
    private void initEncoderAndAudioSourceCallbacks(@NonNull RecordingRecord recordingToStart) {
        mEncodingFutures.add(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mVideoEncoder.setEncoderCallback(new EncoderCallback() {
                        @ExecutedBy("mSequentialExecutor")
                        @Override
                        public void onEncodeStart() {
                            // No-op.
                        }

                        @ExecutedBy("mSequentialExecutor")
                        @Override
                        public void onEncodeStop() {
                            completer.set(null);
                        }

                        @ExecutedBy("mSequentialExecutor")
                        @Override
                        public void onEncodeError(@NonNull EncodeException e) {
                            completer.setException(e);
                        }

                        @ExecutedBy("mSequentialExecutor")
                        @Override
                        public void onEncodedData(@NonNull EncodedData encodedData) {
                            // If the media muxer doesn't yet exist, we may need to create and
                            // start it. Otherwise we can write the data.
                            if (mMediaMuxer == null) {
                                if (!mInProgressRecordingStopping) {
                                    // Clear any previously pending video data since we now
                                    // have newer data.
                                    boolean cachedDataDropped = false;
                                    if (mPendingFirstVideoData != null) {
                                        cachedDataDropped = true;
                                        mPendingFirstVideoData.close();
                                        mPendingFirstVideoData = null;
                                    }

                                    if (encodedData.isKeyFrame()) {
                                        // We have a keyframe. Cache it in case we need to wait
                                        // for audio data.
                                        mPendingFirstVideoData = encodedData;
                                        // If first pending audio data exists or audio is
                                        // disabled, we can start the muxer.
                                        if (!isAudioEnabled()
                                                || !mPendingAudioRingBuffer.isEmpty()) {
                                            Logger.d(TAG, "Received video keyframe. Starting "
                                                    + "muxer...");
                                            setupAndStartMediaMuxer(recordingToStart);
                                        } else {
                                            if (cachedDataDropped) {
                                                Logger.d(TAG, "Replaced cached video keyframe "
                                                        + "with newer keyframe.");
                                            } else {
                                                Logger.d(TAG, "Cached video keyframe while we wait "
                                                        + "for first audio sample before starting "
                                                        + "muxer.");
                                            }
                                        }
                                    } else {
                                        // If the video data is not a key frame,
                                        // MediaMuxer#writeSampleData will drop it. It will
                                        // cause incorrect estimated record bytes and should
                                        // be dropped.
                                        if (cachedDataDropped) {
                                            Logger.d(TAG, "Dropped cached keyframe since we have "
                                                    + "new video data and have not yet received "
                                                    + "audio data.");
                                        }
                                        Logger.d(TAG, "Dropped video data since muxer has not yet "
                                                + "started and data is not a keyframe.");
                                        mVideoEncoder.requestKeyFrame();
                                        encodedData.close();
                                    }
                                } else {
                                    // Recording is stopping before muxer has been started.
                                    Logger.d(TAG, "Drop video data since recording is stopping.");
                                    encodedData.close();
                                }
                            } else {
                                // MediaMuxer is already started, write the data.
                                try (EncodedData videoDataToWrite = encodedData) {
                                    writeVideoData(videoDataToWrite, recordingToStart);
                                }
                            }
                        }

                        @ExecutedBy("mSequentialExecutor")
                        @Override
                        public void onOutputConfigUpdate(@NonNull OutputConfig outputConfig) {
                            mVideoOutputConfig = outputConfig;
                        }
                    }, mSequentialExecutor);
                    return "videoEncodingFuture";
                }));

        if (isAudioEnabled()) {
            mEncodingFutures.add(CallbackToFutureAdapter.getFuture(
                    completer -> {
                        Consumer<Throwable> audioErrorConsumer = throwable -> {
                            if (mAudioErrorCause == null) {
                                // If the audio source or encoder encounters error, update the
                                // status event to notify users. Then continue recording without
                                // audio data.
                                if (throwable instanceof EncodeException) {
                                    setAudioState(AudioState.ERROR_ENCODER);
                                } else {
                                    setAudioState(AudioState.ERROR_SOURCE);
                                }
                                mAudioErrorCause = throwable;
                                updateInProgressStatusEvent();
                                completer.set(null);
                            }
                        };

                        mAudioSource.setAudioSourceCallback(mSequentialExecutor,
                                new AudioSource.AudioSourceCallback() {
                                    @Override
                                    public void onSilenceStateChanged(boolean silenced) {
                                        if (mIsAudioSourceSilenced != silenced) {
                                            mIsAudioSourceSilenced = silenced;
                                            updateInProgressStatusEvent();
                                        } else {
                                            Logger.w(TAG, "Audio source silenced transitions"
                                                    + " to the same state " + silenced);
                                        }
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable throwable) {
                                        Logger.e(TAG, "Error occurred after audio source started.",
                                                throwable);
                                        if (throwable instanceof AudioSourceAccessException) {
                                            audioErrorConsumer.accept(throwable);
                                        }
                                    }

                                    @Override
                                    public void onAmplitudeValue(double maxAmplitude) {
                                        mAudioAmplitude = maxAmplitude;
                                    }
                                });

                        mAudioEncoder.setEncoderCallback(new EncoderCallback() {
                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onEncodeStart() {
                                // No-op.
                            }

                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onEncodeStop() {
                                completer.set(null);
                            }

                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onEncodeError(@NonNull EncodeException e) {
                                if (mAudioErrorCause == null) {
                                    audioErrorConsumer.accept(e);
                                }
                            }

                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onEncodedData(@NonNull EncodedData encodedData) {
                                if (mAudioState == AudioState.DISABLED) {
                                    encodedData.close();
                                    throw new AssertionError("Audio is not enabled but audio "
                                            + "encoded data is being produced.");
                                }

                                // If the media muxer doesn't yet exist, we may need to create and
                                // start it. Otherwise we can write the data.
                                if (mMediaMuxer == null) {
                                    if (!mInProgressRecordingStopping) {
                                        // BufferCopiedEncodedData is used to copy the content of
                                        // the encoded data, preventing byte buffers of the media
                                        // codec from being occupied. Also, since the resources of
                                        // BufferCopiedEncodedData will be automatically released
                                        // by garbage collection, there is no need to call its
                                        // close() function.
                                        mPendingAudioRingBuffer.enqueue(
                                                new BufferCopiedEncodedData(encodedData));

                                        if (mPendingFirstVideoData != null) {
                                            // Both audio and data are ready. Start the muxer.
                                            Logger.d(TAG, "Received audio data. Starting muxer...");
                                            setupAndStartMediaMuxer(recordingToStart);
                                        } else {
                                            Logger.d(TAG, "Cached audio data while we wait"
                                                    + " for video keyframe before starting muxer.");
                                        }
                                    } else {
                                        // Recording is stopping before muxer has been started.
                                        Logger.d(TAG,
                                                "Drop audio data since recording is stopping.");
                                    }
                                    encodedData.close();
                                } else {
                                    try (EncodedData audioDataToWrite = encodedData) {
                                        writeAudioData(audioDataToWrite, recordingToStart);
                                    }
                                }
                            }

                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onOutputConfigUpdate(@NonNull OutputConfig outputConfig) {
                                mAudioOutputConfig = outputConfig;
                            }
                        }, mSequentialExecutor);
                        return "audioEncodingFuture";
                    }));
        }

        Futures.addCallback(Futures.allAsList(mEncodingFutures),
                new FutureCallback<List<Void>>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> result) {
                        Logger.d(TAG, "Encodings end successfully.");
                        finalizeInProgressRecording(mRecordingStopError, mRecordingStopErrorCause);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Logger.d(TAG, "Encodings end with error: " + t);
                        // If the media muxer hasn't been set up, assume the encoding fails
                        // because of no valid data has been produced.
                        finalizeInProgressRecording(
                                mMediaMuxer == null ? ERROR_NO_VALID_DATA : ERROR_ENCODING_FAILED,
                                t);
                    }
                },
                // Can use direct executor since completers are always completed on sequential
                // executor.
                CameraXExecutors.directExecutor());
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void writeVideoData(@NonNull EncodedData encodedData,
            @NonNull RecordingRecord recording) {
        if (mVideoTrackIndex == null) {
            // Throw an exception if the data comes before the track is added.
            throw new AssertionError(
                    "Video data comes before the track is added to MediaMuxer.");
        }

        long newRecordingBytes = mRecordingBytes + encodedData.size();
        if (mFileSizeLimitInBytes != OutputOptions.FILE_SIZE_UNLIMITED
                && newRecordingBytes > mFileSizeLimitInBytes) {
            Logger.d(TAG,
                    String.format("Reach file size limit %d > %d", newRecordingBytes,
                            mFileSizeLimitInBytes));
            onInProgressRecordingInternalError(recording, ERROR_FILE_SIZE_LIMIT_REACHED, null);
            return;
        }

        long newRecordingDurationNs = 0L;
        long currentPresentationTimeUs = encodedData.getPresentationTimeUs();

        if (mFirstRecordingVideoDataTimeUs == Long.MAX_VALUE) {
            mFirstRecordingVideoDataTimeUs = currentPresentationTimeUs;
            Logger.d(TAG, String.format("First video time: %d (%s)", mFirstRecordingVideoDataTimeUs,
                    readableUs(mFirstRecordingVideoDataTimeUs)));
        } else {
            newRecordingDurationNs = TimeUnit.MICROSECONDS.toNanos(
                    currentPresentationTimeUs - Math.min(mFirstRecordingVideoDataTimeUs,
                            mFirstRecordingAudioDataTimeUs));
            Preconditions.checkState(mPreviousRecordingVideoDataTimeUs != Long.MAX_VALUE, "There "
                    + "should be a previous data for adjusting the duration.");
            // We currently don't send an additional empty buffer (bufferInfo.size = 0) with
            // MediaCodec.BUFFER_FLAG_END_OF_STREAM to let the muxer know the duration of the
            // last data, so it will be assumed to have the same duration as the data before it. So
            // add the estimated value to the duration to ensure the final duration will not
            // exceed the limit.
            long adjustedDurationNs = newRecordingDurationNs + TimeUnit.MICROSECONDS.toNanos(
                    currentPresentationTimeUs - mPreviousRecordingVideoDataTimeUs);
            if (mDurationLimitNs != OutputOptions.DURATION_UNLIMITED
                    && adjustedDurationNs > mDurationLimitNs) {
                Logger.d(TAG, String.format("Video data reaches duration limit %d > %d",
                        adjustedDurationNs, mDurationLimitNs));
                onInProgressRecordingInternalError(recording, ERROR_DURATION_LIMIT_REACHED, null);
                return;
            }
        }

        mMediaMuxer.writeSampleData(mVideoTrackIndex, encodedData.getByteBuffer(),
                encodedData.getBufferInfo());

        mRecordingBytes = newRecordingBytes;
        mRecordingDurationNs = newRecordingDurationNs;
        mPreviousRecordingVideoDataTimeUs = currentPresentationTimeUs;

        updateInProgressStatusEvent();
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void writeAudioData(@NonNull EncodedData encodedData,
            @NonNull RecordingRecord recording) {

        long newRecordingBytes = mRecordingBytes + encodedData.size();
        if (mFileSizeLimitInBytes != OutputOptions.FILE_SIZE_UNLIMITED
                && newRecordingBytes > mFileSizeLimitInBytes) {
            Logger.d(TAG,
                    String.format("Reach file size limit %d > %d",
                            newRecordingBytes,
                            mFileSizeLimitInBytes));
            onInProgressRecordingInternalError(recording, ERROR_FILE_SIZE_LIMIT_REACHED, null);
            return;
        }

        long newRecordingDurationNs = 0L;
        long currentPresentationTimeUs = encodedData.getPresentationTimeUs();
        if (mFirstRecordingAudioDataTimeUs == Long.MAX_VALUE) {
            mFirstRecordingAudioDataTimeUs = currentPresentationTimeUs;
            Logger.d(TAG, String.format("First audio time: %d (%s)", mFirstRecordingAudioDataTimeUs,
                    readableUs(mFirstRecordingAudioDataTimeUs)));
        } else {
            newRecordingDurationNs = TimeUnit.MICROSECONDS.toNanos(
                    currentPresentationTimeUs - Math.min(mFirstRecordingVideoDataTimeUs,
                            mFirstRecordingAudioDataTimeUs));
            Preconditions.checkState(mPreviousRecordingAudioDataTimeUs != Long.MAX_VALUE, "There "
                    + "should be a previous data for adjusting the duration.");
            // We currently don't send an additional empty buffer (bufferInfo.size = 0) with
            // MediaCodec.BUFFER_FLAG_END_OF_STREAM to let the muxer know the duration of the
            // last data, so it will be assumed to have the same duration as the data before it. So
            // add the estimated value to the duration to ensure the final duration will not
            // exceed the limit.
            long adjustedDurationNs = newRecordingDurationNs + TimeUnit.MICROSECONDS.toNanos(
                    currentPresentationTimeUs - mPreviousRecordingAudioDataTimeUs);
            if (mDurationLimitNs != OutputOptions.DURATION_UNLIMITED
                    && adjustedDurationNs > mDurationLimitNs) {
                Logger.d(TAG, String.format("Audio data reaches duration limit %d > %d",
                        adjustedDurationNs, mDurationLimitNs));
                onInProgressRecordingInternalError(recording, ERROR_DURATION_LIMIT_REACHED, null);
                return;
            }
        }

        mMediaMuxer.writeSampleData(mAudioTrackIndex,
                encodedData.getByteBuffer(),
                encodedData.getBufferInfo());

        mRecordingBytes = newRecordingBytes;
        mPreviousRecordingAudioDataTimeUs = currentPresentationTimeUs;
    }

    @ExecutedBy("mSequentialExecutor")
    private void pauseInternal(@NonNull RecordingRecord recordingToPause) {
        // Only pause recording if recording is in-progress and it is not stopping.
        if (mInProgressRecording == recordingToPause && !mInProgressRecordingStopping) {
            if (isAudioEnabled()) {
                mAudioEncoder.pause();
            }
            mVideoEncoder.pause();

            mInProgressRecording.updateVideoRecordEvent(VideoRecordEvent.pause(
                    mInProgressRecording.getOutputOptions(),
                    getInProgressRecordingStats()));
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void resumeInternal(@NonNull RecordingRecord recordingToResume) {
        // Only resume recording if recording is in-progress and it is not stopping.
        if (mInProgressRecording == recordingToResume && !mInProgressRecordingStopping) {
            if (isAudioEnabled()) {
                mAudioEncoder.start();
            }
            mVideoEncoder.start();

            mInProgressRecording.updateVideoRecordEvent(VideoRecordEvent.resume(
                    mInProgressRecording.getOutputOptions(),
                    getInProgressRecordingStats()));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    void stopInternal(@NonNull RecordingRecord recordingToStop,
            long explicitlyStopTime, @VideoRecordError int stopError,
            @Nullable Throwable errorCause) {
        // Only stop recording if recording is in-progress and it is not already stopping.
        if (mInProgressRecording == recordingToStop && !mInProgressRecordingStopping) {
            mInProgressRecordingStopping = true;
            mRecordingStopError = stopError;
            mRecordingStopErrorCause = errorCause;
            if (isAudioEnabled()) {
                clearPendingAudioRingBuffer();
                mAudioEncoder.stop(explicitlyStopTime);
            }
            if (mPendingFirstVideoData != null) {
                mPendingFirstVideoData.close();
                mPendingFirstVideoData = null;
            }

            if (mSourceState != SourceState.ACTIVE_NON_STREAMING) {
                // As b/197047288, if the source is still ACTIVE, we will wait for the source to
                // become non-streaming before notifying the encoder the source has stopped.
                // Similarly, if the source is already INACTIVE, we won't know that the source
                // has stopped until the surface request callback, so we'll wait for that.
                // In both cases, we set a timeout to ensure the source is always signalled on
                // devices that require it and to act as a flag that we need to signal the source
                // stopped.
                Encoder finalVideoEncoder = mVideoEncoder;
                mSourceNonStreamingTimeout = CameraXExecutors.mainThreadExecutor().schedule(
                        () -> mSequentialExecutor.execute(() -> {
                            Logger.d(TAG, "The source didn't become non-streaming "
                                    + "before timeout. Waited " + SOURCE_NON_STREAMING_TIMEOUT_MS
                                    + "ms");
                            if (DeviceQuirks.get(
                                    DeactivateEncoderSurfaceBeforeStopEncoderQuirk.class)
                                    != null) {
                                // Even in the case of timeout, we tell the encoder the source has
                                // stopped because devices with this quirk require that the codec
                                // produce a new surface.
                                notifyEncoderSourceStopped(finalVideoEncoder);
                            }
                        }), SOURCE_NON_STREAMING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } else {
                // Source is already non-streaming. Signal source is stopped right away.
                notifyEncoderSourceStopped(mVideoEncoder);
            }

            // Stop the encoder. This will tell the encoder to stop encoding new data. We'll notify
            // the encoder when the source has actually stopped in the FutureCallback.
            // If the recording is explicitly stopped by the user, pass the stop timestamp to the
            // encoder so that the encoding can be stop as close as to the actual stop time.
            mVideoEncoder.stop(explicitlyStopTime);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void muteInternal(@NonNull RecordingRecord recordingToMute, boolean muted) {
        if (recordingToMute.isMuted() == muted) {
            return;
        }
        recordingToMute.mute(muted);
        // Only mute/unmute audio source if recording is in-progress and it is not already stopping.
        if (mInProgressRecording == recordingToMute && !mInProgressRecordingStopping
                && mAudioSource != null) {
            mAudioSource.mute(muted);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    static void notifyEncoderSourceStopped(@NonNull Encoder encoder) {
        if (encoder instanceof EncoderImpl) {
            ((EncoderImpl) encoder).signalSourceStopped();
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void clearPendingAudioRingBuffer() {
        while (!mPendingAudioRingBuffer.isEmpty()) {
            mPendingAudioRingBuffer.dequeue();
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void reset() {
        if (mAudioEncoder != null) {
            Logger.d(TAG, "Releasing audio encoder.");
            mAudioEncoder.release();
            mAudioEncoder = null;
            mAudioOutputConfig = null;
        }
        tryReleaseVideoEncoder();
        if (mAudioSource != null) {
            releaseCurrentAudioSource();
        }

        setAudioState(AudioState.INITIALIZING);
        onReset();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @ExecutedBy("mSequentialExecutor")
    private void tryReleaseVideoEncoder() {
        if (mVideoEncoderSessionToRelease != null) {
            Preconditions.checkState(
                    mVideoEncoderSessionToRelease.getVideoEncoder() == mVideoEncoder);

            Logger.d(TAG, "Releasing video encoder: " + mVideoEncoder);
            mVideoEncoderSessionToRelease.terminateNow();
            mVideoEncoderSessionToRelease = null;
            mVideoEncoder = null;
            mVideoOutputConfig = null;
            setLatestSurface(null);
        } else {
            safeToCloseVideoEncoder();
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void onReset() {
        synchronized (mLock) {
            switch (mState) {
                case PENDING_PAUSED:
                    // Fall-through
                case PENDING_RECORDING:
                    updateNonPendingState(State.CONFIGURING);
                    break;
                case ERROR:
                    // Fall-through
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case RESETTING:
                    // Fall-through
                case STOPPING:
                    setState(State.CONFIGURING);
                    break;
                case CONFIGURING:
                    // No-op
                    break;
            }
        }

        mNeedsReset = false;

        // If the latest surface request hasn't been serviced, use it to re-configure the Recorder.
        if (mLatestSurfaceRequest != null && !mLatestSurfaceRequest.isServiced()) {
            configureInternal(mLatestSurfaceRequest, mVideoSourceTimebase);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private int internalAudioStateToAudioStatsState(@NonNull AudioState audioState) {
        switch (audioState) {
            case DISABLED:
                // Fall-through
            case INITIALIZING:
                // Audio will not be initialized until the first recording with audio enabled is
                // started. So if the audio state is INITIALIZING, consider the audio is disabled.
                return AudioStats.AUDIO_STATE_DISABLED;
            case ENABLED:
                if (mInProgressRecording != null && mInProgressRecording.isMuted()) {
                    return AudioStats.AUDIO_STATE_MUTED;
                } else if (mIsAudioSourceSilenced) {
                    return AudioStats.AUDIO_STATE_SOURCE_SILENCED;
                } else {
                    return AudioStats.AUDIO_STATE_ACTIVE;
                }
            case ERROR_ENCODER:
                return AudioStats.AUDIO_STATE_ENCODER_ERROR;
            case ERROR_SOURCE:
                return AudioStats.AUDIO_STATE_SOURCE_ERROR;
            case IDLING:
                // AudioStats should not be produced when audio is in IDLING state.
                break;
        }
        // Should not reach.
        throw new AssertionError("Invalid internal audio state: " + audioState);
    }

    @NonNull
    private StreamState internalStateToStreamState(@NonNull State state) {
        // Stopping state should be treated as inactive on certain chipsets. See b/196039619.
        DeactivateEncoderSurfaceBeforeStopEncoderQuirk quirk =
                DeviceQuirks.get(DeactivateEncoderSurfaceBeforeStopEncoderQuirk.class);
        return state == State.RECORDING || (state == State.STOPPING && quirk == null)
                ? StreamState.ACTIVE : StreamState.INACTIVE;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    boolean isAudioEnabled() {
        return mAudioState == AudioState.ENABLED;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    void finalizeInProgressRecording(@VideoRecordError int error, @Nullable Throwable throwable) {
        if (mInProgressRecording == null) {
            throw new AssertionError("Attempted to finalize in-progress recording, but no "
                    + "recording is in progress.");
        }

        @VideoRecordError int errorToSend = error;
        if (mMediaMuxer != null) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (IllegalStateException e) {
                Logger.e(TAG, "MediaMuxer failed to stop or release with error: " + e.getMessage());
                if (errorToSend == ERROR_NONE) {
                    errorToSend = ERROR_UNKNOWN;
                }
            }
            mMediaMuxer = null;
        } else if (errorToSend == ERROR_NONE) {
            // Muxer was never started, so recording has no data.
            errorToSend = ERROR_NO_VALID_DATA;
        }

        mInProgressRecording.finalizeRecording(mOutputUri);

        OutputOptions outputOptions = mInProgressRecording.getOutputOptions();
        RecordingStats stats = getInProgressRecordingStats();
        OutputResults outputResults = OutputResults.of(mOutputUri);
        mInProgressRecording.updateVideoRecordEvent(errorToSend == ERROR_NONE
                ? VideoRecordEvent.finalize(
                outputOptions,
                stats,
                outputResults)
                : VideoRecordEvent.finalizeWithError(
                        outputOptions,
                        stats,
                        outputResults,
                        errorToSend,
                        throwable));

        RecordingRecord finalizedRecording = mInProgressRecording;
        mInProgressRecording = null;
        mInProgressRecordingStopping = false;
        mAudioTrackIndex = null;
        mVideoTrackIndex = null;
        mEncodingFutures.clear();
        mOutputUri = Uri.EMPTY;
        mRecordingBytes = 0L;
        mRecordingDurationNs = 0L;
        mFirstRecordingVideoDataTimeUs = Long.MAX_VALUE;
        mFirstRecordingAudioDataTimeUs = Long.MAX_VALUE;
        mPreviousRecordingVideoDataTimeUs = Long.MAX_VALUE;
        mPreviousRecordingAudioDataTimeUs = Long.MAX_VALUE;
        mRecordingStopError = ERROR_UNKNOWN;
        mRecordingStopErrorCause = null;
        mAudioErrorCause = null;
        mAudioAmplitude = AUDIO_AMPLITUDE_NONE;
        clearPendingAudioRingBuffer();

        switch (mAudioState) {
            case IDLING:
                throw new AssertionError(
                        "Incorrectly finalize recording when audio state is IDLING");
            case INITIALIZING:
                // No-op, the audio hasn't been initialized. Keep it in INITIALIZING state.
                break;
            case DISABLED:
                // Fall-through
            case ENABLED:
                setAudioState(AudioState.IDLING);
                mAudioSource.stop();
                break;
            case ERROR_ENCODER:
                // Fall-through
            case ERROR_SOURCE:
                // Reset audio state to INITIALIZING if the audio encoder encountered error, so
                // that it can be setup again when the next recording with audio enabled is started.
                setAudioState(AudioState.INITIALIZING);
                break;
        }

        onRecordingFinalized(finalizedRecording);
    }

    @ExecutedBy("mSequentialExecutor")
    private void onRecordingFinalized(@NonNull RecordingRecord finalizedRecording) {
        boolean needsReset = false;
        boolean startRecordingPaused = false;
        boolean needsConfigure = false;
        RecordingRecord recordingToStart = null;
        RecordingRecord pendingRecordingToFinalize = null;
        @VideoRecordError int error = ERROR_NONE;
        Throwable errorCause = null;
        synchronized (mLock) {
            if (mActiveRecordingRecord != finalizedRecording) {
                throw new AssertionError("Active recording did not match finalized recording on "
                        + "finalize.");
            }

            mActiveRecordingRecord = null;
            switch (mState) {
                case RESETTING:
                    needsReset = true;
                    break;
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    // If finalized while in a RECORDING or PAUSED state, then the recording was
                    // likely finalized due to an error.
                    // Fall-through
                case STOPPING:
                    if (mEncoderNotUsePersistentInputSurface) {
                        // If the encoder doesn't use persistent input surface, the active
                        // surface will become invalid after a recording is finalized. If there's
                        // an unserviced surface request, configure with it directly, otherwise
                        // wait for a new surface update.
                        mActiveSurface = null;
                        if (mLatestSurfaceRequest != null && !mLatestSurfaceRequest.isServiced()) {
                            needsConfigure = true;
                        }
                        setState(State.CONFIGURING);
                    } else {
                        setState(State.IDLING);
                    }
                    break;
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall-through
                case PENDING_RECORDING:
                    if (mSourceState == SourceState.INACTIVE) {
                        pendingRecordingToFinalize = mPendingRecordingRecord;
                        mPendingRecordingRecord = null;
                        setState(State.CONFIGURING);
                        error = ERROR_SOURCE_INACTIVE;
                        errorCause = PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE;
                    } else if (mEncoderNotUsePersistentInputSurface) {
                        // If the encoder doesn't use persistent input surface, the active
                        // surface will become invalid after a recording is finalized. If there's
                        // an unserviced surface request, configure with it directly, otherwise
                        // wait for a new surface update.
                        mActiveSurface = null;
                        if (mLatestSurfaceRequest != null && !mLatestSurfaceRequest.isServiced()) {
                            needsConfigure = true;
                        }
                        updateNonPendingState(State.CONFIGURING);
                    } else if (mVideoEncoder != null) {
                        // If there's no VideoEncoder, it may need to wait for the new
                        // VideoEncoder to be configured.
                        recordingToStart = makePendingRecordingActiveLocked(mState);
                    }
                    break;
                case ERROR:
                    // Error state is non-recoverable. Nothing to do here.
                    break;
                case CONFIGURING:
                    // No-op, the Recorder has been reset before the recording is finalized. So
                    // keep the state in CONFIGURING.
                    break;
                case IDLING:
                    throw new AssertionError("Unexpected state on finalize of recording: "
                            + mState);
            }
        }

        // Perform required actions from state changes inline on sequential executor but unlocked.
        if (needsConfigure) {
            configureInternal(mLatestSurfaceRequest, mVideoSourceTimebase);
        } else if (needsReset) {
            reset();
        } else if (recordingToStart != null) {
            // A pending recording will only be started if we're not waiting for a new surface.
            // Otherwise the recording will be started after receiving a new surface request.
            if (mEncoderNotUsePersistentInputSurface) {
                throw new AssertionError("Attempt to start a pending recording while the Recorder"
                        + " is waiting for a new surface request.");
            }
            startRecording(recordingToStart, startRecordingPaused);
        } else if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, error, errorCause);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    void onInProgressRecordingInternalError(@NonNull RecordingRecord recording,
            @VideoRecordError int error, @Nullable Throwable cause) {
        if (recording != mInProgressRecording) {
            throw new AssertionError("Internal error occurred on recording that is not the current "
                    + "in-progress recording.");
        }

        boolean needsStop = false;
        synchronized (mLock) {
            switch (mState) {
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    setState(State.STOPPING);
                    needsStop = true;
                    // Fall-through
                case STOPPING:
                    // Fall-through
                case RESETTING:
                    // Fall-through
                case PENDING_RECORDING:
                    // Fall-through
                case PENDING_PAUSED:
                    // Fall-through
                    if (recording != mActiveRecordingRecord) {
                        throw new AssertionError("Internal error occurred for recording but it is"
                                + " not the active recording.");
                    }
                    break;
                case CONFIGURING:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case ERROR:
                    throw new AssertionError("In-progress recording error occurred while in "
                            + "unexpected state: " + mState);
            }
        }

        if (needsStop) {
            stopInternal(recording, Encoder.NO_TIMESTAMP, error, cause);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    void tryServicePendingRecording() {
        boolean startRecordingPaused = false;
        RecordingRecord recordingToStart = null;
        RecordingRecord pendingRecordingToFinalize = null;
        @VideoRecordError int error = ERROR_NONE;
        Throwable errorCause = null;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall-through
                case PENDING_RECORDING:
                    if (mActiveRecordingRecord != null || mNeedsReset) {
                        // Active recording is still finalizing or the Recorder is expected to be
                        // reset. Pending recording will be serviced in onRecordingFinalized() or
                        // in onReset().
                        break;
                    }
                    if (mSourceState == SourceState.INACTIVE) {
                        pendingRecordingToFinalize = mPendingRecordingRecord;
                        mPendingRecordingRecord = null;
                        restoreNonPendingState(); // Equivalent to setState(mNonPendingState)
                        error = ERROR_SOURCE_INACTIVE;
                        errorCause = PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE;
                    } else if (mVideoEncoder != null) {
                        // If there's no VideoEncoder, it may need to wait for the new
                        // VideoEncoder to be configured.
                        recordingToStart = makePendingRecordingActiveLocked(mState);
                    }
                    break;
                case CONFIGURING:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case RECORDING:
                    // Fall-through
                case PAUSED:
                    // Fall-through
                case STOPPING:
                    // Fall-through
                case RESETTING:
                    // Fall-through
                case ERROR:
                    break;
            }
        }

        if (recordingToStart != null) {
            // Start new active recording inline on sequential executor (but unlocked).
            startRecording(recordingToStart, startRecordingPaused);
        } else if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, error, errorCause);
        }
    }

    /**
     * Makes the pending recording active and returns the new active recording.
     *
     * <p>This method will not actually start the recording. It is up to the caller to start the
     * returned recording. However, the Recorder.State will be updated to reflect what the state
     * should be after the recording is started. This allows the recording to be started when no
     * longer under lock.
     */
    @GuardedBy("mLock")
    @NonNull
    private RecordingRecord makePendingRecordingActiveLocked(@NonNull State state) {
        boolean startRecordingPaused = false;
        if (state == State.PENDING_PAUSED) {
            startRecordingPaused = true;
        } else if (state != State.PENDING_RECORDING) {
            throw new AssertionError("makePendingRecordingActiveLocked() can only be called from "
                    + "a pending state.");
        }
        if (mActiveRecordingRecord != null) {
            throw new AssertionError("Cannot make pending recording active because another "
                    + "recording is already active.");
        }
        if (mPendingRecordingRecord == null) {
            throw new AssertionError("Pending recording should exist when in a PENDING"
                    + " state.");
        }
        // Swap the pending recording to the active recording and start it
        RecordingRecord recordingToStart = mActiveRecordingRecord = mPendingRecordingRecord;
        mPendingRecordingRecord = null;
        // Start recording if start() has been called before video encoder is setup.
        if (startRecordingPaused) {
            setState(State.PAUSED);
        } else {
            setState(State.RECORDING);
        }

        return recordingToStart;
    }

    /**
     * Actually starts a recording on the sequential executor.
     *
     * <p>This is intended to be called while unlocked on the sequential executor. It should only
     * be called immediately after a pending recording has just been made active. The recording
     * passed to this method should be the newly-made-active recording.
     */
    @ExecutedBy("mSequentialExecutor")
    private void startRecording(@NonNull RecordingRecord recordingToStart,
            boolean startRecordingPaused) {
        // Start pending recording inline since we are already on sequential executor.
        startInternal(recordingToStart);
        if (startRecordingPaused) {
            pauseInternal(recordingToStart);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    void updateInProgressStatusEvent() {
        if (mInProgressRecording != null) {
            mInProgressRecording.updateVideoRecordEvent(
                    VideoRecordEvent.status(
                            mInProgressRecording.getOutputOptions(),
                            getInProgressRecordingStats()));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    @NonNull
    RecordingStats getInProgressRecordingStats() {
        return RecordingStats.of(mRecordingDurationNs, mRecordingBytes,
                AudioStats.of(internalAudioStateToAudioStatsState(mAudioState), mAudioErrorCause,
                        mAudioAmplitude));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    <T> T getObservableData(@NonNull StateObservable<T> observable) {
        ListenableFuture<T> future = observable.fetchData();
        try {
            // A StateObservable always has a state available and the future got from fetchData()
            // will complete immediately.
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    boolean isAudioSupported() {
        return getObservableData(mMediaSpec).getAudioSpec().getChannelCount()
                != AudioSpec.CHANNEL_COUNT_NONE;
    }

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void setState(@NonNull State state) {
        // If we're attempt to transition to the same state, then we likely have a logic error.
        // All state transitions should be intentional, so throw an AssertionError here.
        if (mState == state) {
            throw new AssertionError("Attempted to transition to state " + state + ", but "
                    + "Recorder is already in state " + state);
        }

        Logger.d(TAG, "Transitioning Recorder internal state: " + mState + " --> " + state);
        // If we are transitioning from a non-pending state to a pending state, we need to store
        // the non-pending state so we can transition back if the pending recording is stopped
        // before it becomes active.
        StreamInfo.StreamState streamState = null;
        if (PENDING_STATES.contains(state)) {
            if (!PENDING_STATES.contains(mState)) {
                if (!VALID_NON_PENDING_STATES_WHILE_PENDING.contains(mState)) {
                    throw new AssertionError(
                            "Invalid state transition. Should not be transitioning "
                                    + "to a PENDING state from state " + mState);
                }
                mNonPendingState = mState;
                streamState = internalStateToStreamState(mNonPendingState);
            }
        } else if (mNonPendingState != null) {
            // Transitioning out of a pending state. Clear the non-pending state.
            mNonPendingState = null;
        }

        mState = state;
        if (streamState == null) {
            streamState = internalStateToStreamState(mState);
        }
        mStreamInfo.setState(StreamInfo.of(mStreamId, streamState));
    }

    @ExecutedBy("mSequentialExecutor")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void setLatestSurface(@Nullable Surface surface) {
        if (mLatestSurface == surface) {
            return;
        }
        mLatestSurface = surface;
        synchronized (mLock) {
            setStreamId(surface != null ? surface.hashCode() : StreamInfo.STREAM_ID_ANY);
        }
    }

    @GuardedBy("mLock")
    private void setStreamId(int streamId) {
        if (mStreamId == streamId) {
            return;
        }
        Logger.d(TAG, "Transitioning streamId: " + mStreamId + " --> " + streamId);
        mStreamId = streamId;
        mStreamInfo.setState(StreamInfo.of(streamId, internalStateToStreamState(mState)));
    }

    /**
     * Updates the non-pending state while in a pending state.
     *
     * <p>If called from a non-pending state, an assertion error will be thrown.
     */
    @GuardedBy("mLock")
    private void updateNonPendingState(@NonNull State state) {
        if (!PENDING_STATES.contains(mState)) {
            throw new AssertionError("Can only updated non-pending state from a pending state, "
                    + "but state is " + mState);
        }

        if (!VALID_NON_PENDING_STATES_WHILE_PENDING.contains(state)) {
            throw new AssertionError(
                    "Invalid state transition. State is not a valid non-pending state while in a "
                            + "pending state: " + state);
        }

        if (mNonPendingState != state) {
            mNonPendingState = state;
            mStreamInfo.setState(
                    StreamInfo.of(mStreamId, internalStateToStreamState(state)));
        }
    }

    /**
     * Convenience for restoring the state to the non-pending state.
     *
     * <p>This is equivalent to calling setState(mNonPendingState), but performs a few safety
     * checks. This can only be called while in a pending state.
     */
    @GuardedBy("mLock")
    private void restoreNonPendingState() {
        if (!PENDING_STATES.contains(mState)) {
            throw new AssertionError("Cannot restore non-pending state when in state " + mState);
        }

        setState(mNonPendingState);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    void setAudioState(@NonNull AudioState audioState) {
        Logger.d(TAG, "Transitioning audio state: " + mAudioState + " --> " + audioState);
        mAudioState = audioState;
    }

    private static int supportedMuxerFormatOrDefaultFrom(
            @Nullable VideoValidatedEncoderProfilesProxy profilesProxy, int defaultMuxerFormat) {
        if (profilesProxy != null) {
            switch (profilesProxy.getRecommendedFileFormat()) {
                case MediaRecorder.OutputFormat.MPEG_4:
                    return MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
                case MediaRecorder.OutputFormat.WEBM:
                    return MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM;
                case MediaRecorder.OutputFormat.THREE_GPP:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        // MediaMuxer does not support 3GPP on pre-Android O(API 26) devices.
                        return MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
                    } else {
                        return MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP;
                    }
                default:
                    break;
            }
        }
        return defaultMuxerFormat;
    }

    /**
     * Gets the {@link VideoCapabilities} of Recorder.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static VideoCapabilities getVideoCapabilities(@NonNull CameraInfo cameraInfo) {
        return RecorderVideoCapabilities.from(cameraInfo);
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    @AutoValue
    abstract static class RecordingRecord implements AutoCloseable {

        private final CloseGuardHelper mCloseGuard = CloseGuardHelper.create();

        private final AtomicBoolean mInitialized = new AtomicBoolean(false);

        private final AtomicReference<MediaMuxerSupplier> mMediaMuxerSupplier =
                new AtomicReference<>(null);

        private final AtomicReference<AudioSourceSupplier> mAudioSourceSupplier =
                new AtomicReference<>(null);

        private final AtomicReference<Consumer<Uri>> mRecordingFinalizer =
                new AtomicReference<>(ignored -> {
                    /* no-op by default */
                });

        private final AtomicBoolean mMuted = new AtomicBoolean(false);

        @NonNull
        static RecordingRecord from(@NonNull PendingRecording pendingRecording, long recordingId) {
            return new AutoValue_Recorder_RecordingRecord(
                    pendingRecording.getOutputOptions(),
                    pendingRecording.getListenerExecutor(),
                    pendingRecording.getEventListener(),
                    pendingRecording.isAudioEnabled(),
                    recordingId
            );
        }

        @NonNull
        abstract OutputOptions getOutputOptions();

        @Nullable
        abstract Executor getCallbackExecutor();

        @Nullable
        abstract Consumer<VideoRecordEvent> getEventListener();

        abstract boolean hasAudioEnabled();

        abstract long getRecordingId();

        /**
         * Performs initialization for this recording.
         *
         * @throws AssertionError if this recording has already been initialized.
         * @throws IOException if it fails to duplicate the file descriptor when the
         * {@link #getOutputOptions() OutputOptions} is {@link FileDescriptorOutputOptions}.
         */
        void initializeRecording(@NonNull Context context) throws IOException {
            if (mInitialized.getAndSet(true)) {
                throw new AssertionError("Recording " + this + " has already been initialized");
            }
            OutputOptions outputOptions = getOutputOptions();

            final ParcelFileDescriptor dupedParcelFileDescriptor;
            if (outputOptions instanceof FileDescriptorOutputOptions) {
                // Duplicate ParcelFileDescriptor to make input descriptor can be safely closed,
                // or throw an IOException if it fails.
                dupedParcelFileDescriptor =
                        ((FileDescriptorOutputOptions) outputOptions)
                                .getParcelFileDescriptor().dup();
            } else {
                dupedParcelFileDescriptor = null;
            }

            mCloseGuard.open("finalizeRecording");

            MediaMuxerSupplier mediaMuxerSupplier =
                    (muxerOutputFormat, outputUriCreatedCallback) -> {
                        MediaMuxer mediaMuxer;
                        Uri outputUri = Uri.EMPTY;
                        if (outputOptions instanceof FileOutputOptions) {
                            FileOutputOptions fileOutputOptions = (FileOutputOptions) outputOptions;
                            File file = fileOutputOptions.getFile();
                            if (!OutputUtil.createParentFolder(file)) {
                                Logger.w(TAG,
                                        "Failed to create folder for " + file.getAbsolutePath());
                            }
                            mediaMuxer = new MediaMuxer(file.getAbsolutePath(), muxerOutputFormat);
                            outputUri = Uri.fromFile(file);
                        } else if (outputOptions instanceof FileDescriptorOutputOptions) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // Use dup'd ParcelFileDescriptor to prevent the descriptor in
                                // OutputOptions from being closed.
                                mediaMuxer = Api26Impl.createMediaMuxer(
                                        dupedParcelFileDescriptor.getFileDescriptor(),
                                        muxerOutputFormat);
                            } else {
                                throw new IOException(
                                        "MediaMuxer doesn't accept FileDescriptor as output "
                                                + "destination.");
                            }
                        } else if (outputOptions instanceof MediaStoreOutputOptions) {
                            MediaStoreOutputOptions mediaStoreOutputOptions =
                                    (MediaStoreOutputOptions) outputOptions;

                            ContentValues contentValues =
                                    new ContentValues(mediaStoreOutputOptions.getContentValues());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Toggle on pending status for the video file.
                                contentValues.put(MediaStore.Video.Media.IS_PENDING, PENDING);
                            }
                            outputUri = mediaStoreOutputOptions.getContentResolver().insert(
                                    mediaStoreOutputOptions.getCollectionUri(), contentValues);
                            if (outputUri == null) {
                                throw new IOException("Unable to create MediaStore entry.");
                            }

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                String path = OutputUtil.getAbsolutePathFromUri(
                                        mediaStoreOutputOptions.getContentResolver(),
                                        outputUri, MEDIA_COLUMN);
                                if (path == null) {
                                    throw new IOException(
                                            "Unable to get path from uri " + outputUri);
                                }
                                if (!OutputUtil.createParentFolder(new File(path))) {
                                    Logger.w(TAG, "Failed to create folder for " + path);
                                }
                                mediaMuxer = new MediaMuxer(path, muxerOutputFormat);
                            } else {
                                ParcelFileDescriptor fileDescriptor =
                                        mediaStoreOutputOptions.getContentResolver()
                                                .openFileDescriptor(outputUri, "rw");
                                mediaMuxer = Api26Impl.createMediaMuxer(
                                        fileDescriptor.getFileDescriptor(),
                                        muxerOutputFormat);
                                fileDescriptor.close();
                            }
                        } else {
                            throw new AssertionError(
                                    "Invalid output options type: "
                                            + outputOptions.getClass().getSimpleName());
                        }
                        outputUriCreatedCallback.accept(outputUri);
                        return mediaMuxer;
                    };
            mMediaMuxerSupplier.set(mediaMuxerSupplier);

            Consumer<Uri> recordingFinalizer = null;
            if (hasAudioEnabled()) {
                if (Build.VERSION.SDK_INT >= 31) {
                    // Use anonymous inner class instead of lambda since we need to propagate
                    // permission requirements
                    @SuppressWarnings("Convert2Lambda")
                    AudioSourceSupplier audioSourceSupplier = new AudioSourceSupplier() {
                        @NonNull
                        @Override
                        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                        public AudioSource get(@NonNull AudioSettings settings,
                                @NonNull Executor executor)
                                throws AudioSourceAccessException {
                            // Context will only be held in local scope of the supplier so it will
                            // not be retained after performOneTimeAudioSourceCreation() is called.
                            return new AudioSource(settings, executor, context);
                        }
                    };
                    mAudioSourceSupplier.set(audioSourceSupplier);
                } else {
                    // Use anonymous inner class instead of lambda since we need to propagate
                    // permission requirements
                    @SuppressWarnings("Convert2Lambda")
                    AudioSourceSupplier audioSourceSupplier = new AudioSourceSupplier() {
                        @NonNull
                        @Override
                        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                        public AudioSource get(@NonNull AudioSettings settings,
                                @NonNull Executor executor)
                                throws AudioSourceAccessException {
                            // Do not set (or retain) context on other API levels
                            return new AudioSource(settings, executor, null);
                        }
                    };
                    mAudioSourceSupplier.set(audioSourceSupplier);
                }
            }

            if (outputOptions instanceof MediaStoreOutputOptions) {
                MediaStoreOutputOptions mediaStoreOutputOptions =
                        (MediaStoreOutputOptions) outputOptions;
                // TODO(b/201946954): Investigate whether we should add a setting to disable
                //  scan/update to allow users to perform it themselves.
                if (Build.VERSION.SDK_INT >= 29) {
                    recordingFinalizer = outputUri -> {
                        if (outputUri.equals(Uri.EMPTY)) {
                            return;
                        }
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.Video.Media.IS_PENDING, NOT_PENDING);
                        mediaStoreOutputOptions.getContentResolver().update(outputUri,
                                contentValues, null, null);
                    };
                } else {
                    // Context will only be held in local scope of the consumer so it will not be
                    // retained after finalizeOutputFile() is called.
                    recordingFinalizer = outputUri -> {
                        if (outputUri.equals(Uri.EMPTY)) {
                            return;
                        }
                        String filePath = OutputUtil.getAbsolutePathFromUri(
                                mediaStoreOutputOptions.getContentResolver(), outputUri,
                                MEDIA_COLUMN);
                        if (filePath != null) {
                            // Use null mime type list to have MediaScanner derive mime type from
                            // extension
                            MediaScannerConnection.scanFile(context,
                                    new String[]{filePath}, /*mimeTypes=*/null, (path, uri) -> {
                                        if (uri == null) {
                                            Logger.e(TAG, String.format("File scanning operation "
                                                    + "failed [path: %s]", path));
                                        } else {
                                            Logger.d(TAG, String.format("File scan completed "
                                                    + "successfully [path: %s, URI: %s]", path,
                                                    uri));
                                        }
                                    });
                        } else {
                            Logger.d(TAG,
                                    "Skipping media scanner scan. Unable to retrieve file path "
                                            + "from URI: " + outputUri);
                        }
                    };
                }
            } else if (outputOptions instanceof FileDescriptorOutputOptions) {
                recordingFinalizer = ignored -> {
                    try {
                        // dupedParcelFileDescriptor should be non-null.
                        dupedParcelFileDescriptor.close();
                    } catch (IOException e) {
                        // IOException is not expected to be thrown while closing
                        // ParcelFileDescriptor.
                        Logger.e(TAG, "Failed to close dup'd ParcelFileDescriptor", e);
                    }
                };
            }

            if (recordingFinalizer != null) {
                mRecordingFinalizer.set(recordingFinalizer);
            }
        }

        /**
         * Updates the recording status and callback to users.
         */
        void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
            if (!Objects.equals(event.getOutputOptions(), getOutputOptions())) {
                throw new AssertionError("Attempted to update event listener with event from "
                    + "incorrect recording [Recording: " + event.getOutputOptions()
                        + ", Expected: " + getOutputOptions() + "]");
            }
            String message = "Sending VideoRecordEvent " + event.getClass().getSimpleName();
            if (event instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                if (finalizeEvent.hasError()) {
                    message += String.format(" [error: %s]",
                            VideoRecordEvent.Finalize.errorToString(
                                    finalizeEvent.getError()));
                }
            }
            Logger.d(TAG, message);
            if (getCallbackExecutor() != null && getEventListener() != null) {
                try {
                    getCallbackExecutor().execute(() -> getEventListener().accept(event));
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "The callback executor is invalid.", e);
                }
            }
        }

        /**
         * Creates an {@link AudioSource} for this recording.
         *
         * <p>An audio source can only be created once per recording, so subsequent calls to this
         * method will throw an {@link AssertionError}.
         *
         * <p>Calling this method when audio is not enabled for this recording will also throw an
         * {@link AssertionError}.
         */
        @NonNull
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        AudioSource performOneTimeAudioSourceCreation(
                @NonNull AudioSettings settings, @NonNull Executor audioSourceExecutor)
                throws AudioSourceAccessException {
            if (!hasAudioEnabled()) {
                throw new AssertionError("Recording does not have audio enabled. Unable to create"
                        + " audio source for recording " + this);
            }

            AudioSourceSupplier audioSourceSupplier = mAudioSourceSupplier.getAndSet(null);
            if (audioSourceSupplier == null) {
                throw new AssertionError("One-time audio source creation has already occurred for"
                        + " recording " + this);
            }

            return audioSourceSupplier.get(settings, audioSourceExecutor);
        }

        /**
         * Creates a {@link MediaMuxer} for this recording.
         *
         * <p>A media muxer can only be created once per recording, so subsequent calls to this
         * method will throw an {@link AssertionError}.
         *
         * @param muxerOutputFormat the output file format.
         * @param outputUriCreatedCallback A callback that will send the returned media muxer's
         *                                 output {@link Uri}. It will be {@link Uri#EMPTY} if the
         *                                 {@link #getOutputOptions() OutputOptions} is
         *                                 {@link FileDescriptorOutputOptions}.
         *                                 Note: This callback will be called inline.
         * @return the media muxer.
         * @throws IOException if the creation of the media mixer fails.
         * @throws AssertionError if the recording is not initialized or subsequent calls to this
         * method.
         */
        @NonNull
        MediaMuxer performOneTimeMediaMuxerCreation(int muxerOutputFormat,
                @NonNull Consumer<Uri> outputUriCreatedCallback) throws IOException {
            if (!mInitialized.get()) {
                throw new AssertionError("Recording " + this + " has not been initialized");
            }
            MediaMuxerSupplier mediaMuxerSupplier = mMediaMuxerSupplier.getAndSet(null);
            if (mediaMuxerSupplier == null) {
                throw new AssertionError("One-time media muxer creation has already occurred for"
                        + " recording " + this);
            }
            return mediaMuxerSupplier.get(muxerOutputFormat, outputUriCreatedCallback);
        }

        /**
         * Performs final operations required to finalize this recording.
         *
         * <p>Recording finalization can only occur once. Any subsequent calls to this method or
         * {@link #close()} will throw an {@link AssertionError}.
         *
         * <p>Finalizing an uninitialized recording is no-op.
         *
         * @param uri The uri of the output file.
         */
        void finalizeRecording(@NonNull Uri uri) {
            if (!mInitialized.get()) {
                return;
            }
            finalizeRecordingInternal(mRecordingFinalizer.getAndSet(null), uri);
        }

        void mute(boolean muted) {
            mMuted.set(muted);
        }

        boolean isMuted() {
            return mMuted.get();
        }

        /**
         * Close this recording, as if calling {@link #finalizeRecording(Uri)} with parameter
         * {@link Uri#EMPTY}.
         *
         * <p>This method is equivalent to calling {@link #finalizeRecording(Uri)} with parameter
         * {@link Uri#EMPTY}.
         *
         * <p>Recording finalization can only occur once. Any subsequent calls to this method or
         * {@link #finalizeRecording(Uri)} will throw an {@link AssertionError}.
         *
         * <p>Closing an uninitialized recording is no-op.
         */
        @Override
        public void close() {
            finalizeRecording(Uri.EMPTY);
        }

        @Override
        @SuppressWarnings("GenericException") // super.finalize() throws Throwable
        protected void finalize() throws Throwable {
            try {
                mCloseGuard.warnIfOpen();
                Consumer<Uri> finalizer = mRecordingFinalizer.getAndSet(null);
                if (finalizer != null) {
                    finalizeRecordingInternal(finalizer, Uri.EMPTY);
                }
            } finally {
                super.finalize();
            }
        }

        private void finalizeRecordingInternal(@Nullable Consumer<Uri> finalizer,
                @NonNull Uri uri) {
            if (finalizer == null) {
                throw new AssertionError(
                        "Recording " + this + " has already been finalized");
            }
            mCloseGuard.close();
            finalizer.accept(uri);
        }

        private interface MediaMuxerSupplier {
            @NonNull
            MediaMuxer get(int muxerOutputFormat, @NonNull Consumer<Uri> outputUriCreatedCallback)
                    throws IOException;
        }

        private interface AudioSourceSupplier {
            @RequiresPermission(Manifest.permission.RECORD_AUDIO)
            @NonNull
            AudioSource get(@NonNull AudioSettings settings,
                    @NonNull Executor audioSourceExecutor) throws AudioSourceAccessException;
        }
    }

    /**
     * Builder class for {@link Recorder} objects.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder {

        private final MediaSpec.Builder mMediaSpecBuilder;
        private Executor mExecutor = null;
        private EncoderFactory mVideoEncoderFactory = DEFAULT_ENCODER_FACTORY;
        private EncoderFactory mAudioEncoderFactory = DEFAULT_ENCODER_FACTORY;

        /**
         * Constructor for {@code Recorder.Builder}.
         *
         * <p>Creates a builder which is pre-populated with appropriate default configuration
         * options.
         */
        public Builder() {
            mMediaSpecBuilder = MediaSpec.builder();
        }

        /**
         * Sets the {@link Executor} that runs the Recorder background task.
         *
         * <p>The executor is used to run the Recorder tasks, the audio encoding and the video
         * encoding. For the best performance, it's recommended to be an {@link Executor} that is
         * capable of running at least two tasks concurrently, such as a
         * {@link java.util.concurrent.ThreadPoolExecutor} backed by 2 or more threads.
         *
         * <p>If not set, the Recorder will be run on the IO executor internally managed by CameraX.
         */
        @NonNull
        public Builder setExecutor(@NonNull Executor executor) {
            Preconditions.checkNotNull(executor, "The specified executor can't be null.");
            mExecutor = executor;
            return this;
        }

        // Usually users can use the CameraX predefined configuration for creating a recorder. We
        // may see which options of MediaSpec to be exposed.

        /**
         * Sets the {@link QualitySelector} of this Recorder.
         *
         * <p>The provided quality selector is used to select the resolution of the recording
         * depending on the resolutions supported by the camera and codec capabilities.
         *
         * <p>If no quality selector is provided, the default is
         * {@link Recorder#DEFAULT_QUALITY_SELECTOR}.
         *
         * <p>{@link #setAspectRatio(int)} can be used with to specify the intended video aspect
         * ratio.
         *
         * @see QualitySelector
         * @see #setAspectRatio(int)
         */
        @NonNull
        public Builder setQualitySelector(@NonNull QualitySelector qualitySelector) {
            Preconditions.checkNotNull(qualitySelector,
                    "The specified quality selector can't be null.");
            mMediaSpecBuilder.configureVideo(
                    builder -> builder.setQualitySelector(qualitySelector));
            return this;
        }

        /**
         * Sets the intended video encoding bitrate for recording.
         *
         * <p>The target video encoding bitrate attempts to keep the actual video encoding
         * bitrate close to the requested {@code bitrate}. Bitrate may vary during a recording
         * depending on the scene
         * being recorded.
         *
         * <p>Additional checks will be performed on the requested {@code bitrate} to make sure the
         * specified bitrate is applicable, and sometimes the passed bitrate will be changed
         * internally to ensure the video recording can proceed smoothly based on the
         * capabilities of the platform.
         *
         * <p>This API only affects the video stream and should not be considered the
         * target for the entire recording. The audio stream's bitrate is not affected by this API.
         *
         * <p>If this method isn't called, an appropriate bitrate for normal video
         * recording is selected by default. Only call this method if a custom bitrate is desired.
         *
         * @param bitrate the target video encoding bitrate in bits per second.
         * @throws IllegalArgumentException if bitrate is 0 or less.
         */
        @NonNull
        public Builder setTargetVideoEncodingBitRate(@IntRange(from = 1) int bitrate) {
            if (bitrate <= 0) {
                throw new IllegalArgumentException("The requested target bitrate " + bitrate
                        + " is not supported. Target bitrate must be greater than 0.");
            }

            mMediaSpecBuilder.configureVideo(
                    builder -> builder.setBitrate(new Range<>(bitrate, bitrate)));
            return this;
        }

        /**
         * Sets the video aspect ratio of this Recorder.
         *
         * <p>The final video resolution will be based on the input aspect ratio and the
         * QualitySelector in {@link #setQualitySelector(QualitySelector)}. Both settings will be
         * respected. For example, if the aspect ratio is 4:3 and the preferred quality in
         * QualitySelector is HD, then a HD quality resolution with 4:3 aspect ratio such as
         * 1280x960 or 960x720 will be used. CameraX will choose an appropriate one depending on
         * the resolutions supported by the camera and the codec capabilities. With this setting,
         * no other aspect ratios (such as 16:9) will be used, nor any other qualities (such as
         * UHD, FHD and SD). If no resolution with the settings can be found, it will fail to
         * bind VideoCapture. Therefore, a recommended way is to provide a flexible
         * QualitySelector if there is no specific video quality requirement, such as the setting
         * in {@link Recorder#DEFAULT_QUALITY_SELECTOR}.
         *
         * <p>The default value is {@link AspectRatio#RATIO_DEFAULT}. If no aspect ratio is set, the
         * selected resolution will be based only on the QualitySelector.
         *
         * @param aspectRatio the aspect ratio. Possible values are {@link AspectRatio#RATIO_4_3}
         *                    and {@link AspectRatio#RATIO_16_9}.
         *
         * @see #setQualitySelector(QualitySelector)
         */
        @NonNull
        public Builder setAspectRatio(@AspectRatio.Ratio int aspectRatio) {
            mMediaSpecBuilder.configureVideo(builder -> builder.setAspectRatio(aspectRatio));
            return this;
        }

        /**
         * Sets the audio source for recordings with audio enabled.
         *
         * <p>This will only set the source of audio for recordings, but audio must still be
         * enabled on a per-recording basis with {@link PendingRecording#withAudioEnabled()}
         * before starting the recording.
         *
         * @param source The audio source to use. One of {@link AudioSpec#SOURCE_AUTO} or
         *               {@link AudioSpec#SOURCE_CAMCORDER}. Default is
         *               {@link AudioSpec#SOURCE_AUTO}.
         */
        @NonNull
        Builder setAudioSource(@AudioSpec.Source int source) {
            mMediaSpecBuilder.configureAudio(builder -> builder.setSource(source));
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        Builder setVideoEncoderFactory(@NonNull EncoderFactory videoEncoderFactory) {
            mVideoEncoderFactory = videoEncoderFactory;
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        Builder setAudioEncoderFactory(@NonNull EncoderFactory audioEncoderFactory) {
            mAudioEncoderFactory = audioEncoderFactory;
            return this;
        }

        /**
         * Builds the {@link Recorder} instance.
         *
         * <p>The {code build()} method can be called multiple times, generating a new
         * {@link Recorder} instance each time. The returned instance is configured with the
         * options set on this builder.
         */
        @NonNull
        public Recorder build() {
            return new Recorder(mExecutor, mMediaSpecBuilder.build(), mVideoEncoderFactory,
                    mAudioEncoderFactory);
        }
    }
}
