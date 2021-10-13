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

import static androidx.camera.video.QualitySelector.FALLBACK_STRATEGY_HIGHER;
import static androidx.camera.video.QualitySelector.QUALITY_FHD;
import static androidx.camera.video.QualitySelector.QUALITY_HD;
import static androidx.camera.video.QualitySelector.QUALITY_SD;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_UNKNOWN;
import static androidx.camera.video.VideoRecordEvent.Finalize.VideoRecordError;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.MutableStateObservable;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.StateObservable;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.AudioSource;
import androidx.camera.video.internal.AudioSourceAccessException;
import androidx.camera.video.internal.BufferProvider;
import androidx.camera.video.internal.compat.Api26Impl;
import androidx.camera.video.internal.compat.quirk.DeactivateEncoderSurfaceBeforeStopEncoderQuirk;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.camera.video.internal.encoder.EncodeException;
import androidx.camera.video.internal.encoder.EncodedData;
import androidx.camera.video.internal.encoder.Encoder;
import androidx.camera.video.internal.encoder.EncoderCallback;
import androidx.camera.video.internal.encoder.EncoderImpl;
import androidx.camera.video.internal.encoder.InputBuffer;
import androidx.camera.video.internal.encoder.InvalidConfigException;
import androidx.camera.video.internal.encoder.OutputConfig;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.utils.OutputUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
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
 * then can be used to adjust per-recording settings and to start the recording. It also allows
 * setting a listener with {@link PendingRecording#withEventListener(Executor, Consumer)} to
 * listen for {@link VideoRecordEvent}s such as {@link VideoRecordEvent.Start},
 * {@link VideoRecordEvent.Pause}, {@link VideoRecordEvent.Resume}, and
 * {@link VideoRecordEvent.Finalize}. This listener will also receive regular recording status
 * updates via the {@link VideoRecordEvent.Status} event.
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
         * The Recorder is being initialized.
         */
        INITIALIZING,
        /**
         * The Recorder is being initialized and a recording is waiting for being run.
         */
        PENDING_RECORDING,
        /**
         * The Recorder is being initialized and a recording is waiting for being paused.
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
         * Audio recording is disabled for the running recording.
         */
        DISABLED,
        /**
         * The recording is being recorded with audio.
         */
        ACTIVE,
        /**
         * The recording is muted because the audio source is silenced.
         */
        SOURCE_SILENCED,
        /**
         * The recording is muted because the audio encoder encountered errors.
         */
        ENCODER_ERROR
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
                    State.INITIALIZING, // Waiting for camera before starting recording.
                    State.IDLING, // Waiting for sequential executor to start pending recording.
                    State.RESETTING, // Waiting for camera/encoders to reset before starting.
                    State.STOPPING // Waiting for previous recording to finalize before starting.
            ));

    /**
     * Default quality selector for recordings.
     *
     * <p>The default quality selector chooses a video quality suitable for recordings based on
     * device and compatibility constraints. It is equivalent to:
     * <pre>{@code
     * QualitySelector.firstTry(QUALITY_FHD)
     *         .thenTry(QUALITY_HD)
     *         .thenTry(QUALITY_SD)
     *         .finallyTry(QUALITY_FHD, FALLBACK_STRATEGY_HIGHER);
     * }</pre>
     *
     * @see QualitySelector
     */
    public static final QualitySelector DEFAULT_QUALITY_SELECTOR =
            QualitySelector.firstTry(QUALITY_FHD)
                    .thenTry(QUALITY_HD)
                    .thenTry(QUALITY_SD)
                    .finallyTry(QUALITY_FHD, FALLBACK_STRATEGY_HIGHER);

    private static final AudioSpec AUDIO_SPEC_DEFAULT =
            AudioSpec.builder()
                    .setSourceFormat(
                            AudioSpec.SOURCE_FORMAT_PCM_16BIT) /* Defaults to PCM_16BIT as it's
                            guaranteed supported on devices. May consider allowing users to set
                            format through AudioSpec later. */
                    .setSource(AudioSpec.SOURCE_CAMCORDER)
                    .setChannelCount(AudioSpec.CHANNEL_COUNT_MONO)
                    .build();
    private static final VideoSpec VIDEO_SPEC_DEFAULT =
            VideoSpec.builder()
                    .setQualitySelector(DEFAULT_QUALITY_SELECTOR)
                    .setAspectRatio(VideoSpec.ASPECT_RATIO_16_9)
                    .build();
    private static final MediaSpec MEDIA_SPEC_DEFAULT =
            MediaSpec.builder()
                    .setOutputFormat(MediaSpec.OUTPUT_FORMAT_MPEG_4)
                    .setAudioSpec(AUDIO_SPEC_DEFAULT)
                    .setVideoSpec(VIDEO_SPEC_DEFAULT)
                    .build();
    private static final int AUDIO_BITRATE_DEFAULT = 88200;
    // Default to 44100 for now as it's guaranteed supported on devices.
    private static final int AUDIO_SAMPLE_RATE_DEFAULT = 44100;
    private static final int VIDEO_FRAME_RATE_DEFAULT = 30;
    private static final int VIDEO_BITRATE_DEFAULT = 10 * 1024 * 1024; // 10M
    private static final int VIDEO_INTRA_FRAME_INTERVAL_DEFAULT = 1;
    @SuppressWarnings("deprecation")
    private static final String MEDIA_COLUMN = MediaStore.Video.Media.DATA;
    private static final Exception PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE =
            new RuntimeException("The video frame producer became inactive before any "
                    + "data was received.");
    private static final int PENDING = 1;
    private static final int NOT_PENDING = 0;

    private final MutableStateObservable<StreamState> mStreamState =
            MutableStateObservable.withInitialState(StreamState.INACTIVE);
    // Used only by getExecutor()
    private final Executor mUserProvidedExecutor;
    // May be equivalent to mUserProvidedExecutor or an internal executor if the user did not
    // provide an executor.
    private final Executor mExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mSequentialExecutor;
    private final Object mLock = new Object();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                          Members only accessed when holding mLock                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @GuardedBy("mLock")
    private State mState = State.INITIALIZING;
    // Tracks the underlying state when in a PENDING_* state. When not in a PENDING_* state, this
    // should be null.
    @GuardedBy("mLock")
    private State mNonPendingState = null;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    RecordingRecord mActiveRecordingRecord = null;
    // A recording that will be started once the previous recording has finalized or the
    // recorder has finished initializing.
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    RecordingRecord mPendingRecordingRecord = null;
    @GuardedBy("mLock")
    private SourceState mSourceState = SourceState.ACTIVE;
    @GuardedBy("mLock")
    private Throwable mErrorCause;
    @GuardedBy("mLock")
    private boolean mSurfaceRequested = false;
    @GuardedBy("mLock")
    private long mLastGeneratedRecordingId = 0L;
    //--------------------------------------------------------------------------------------------//


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                      Members only accessed on mSequentialExecutor                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private RecordingRecord mInProgressRecording = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mInProgressRecordingStopping = false;
    private boolean mAudioInitialized = false;
    private SurfaceRequest.TransformationInfo mSurfaceTransformationInfo = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final List<ListenableFuture<Void>> mEncodingFutures = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mAudioTrackIndex = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mVideoTrackIndex = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Surface mSurface = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    MediaMuxer mMediaMuxer = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final MutableStateObservable<MediaSpec> mMediaSpec;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioSource mAudioSource = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    EncoderImpl mVideoEncoder = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    OutputConfig mVideoOutputConfig = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    EncoderImpl mAudioEncoder = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    OutputConfig mAudioOutputConfig = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioState mAudioState = AudioState.INITIALIZING;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull Uri mOutputUri = Uri.EMPTY;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mRecordingBytes = 0L;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mRecordingDurationNs = 0L;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFirstRecordingVideoDataTimeUs = 0L;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFileSizeLimitInBytes = OutputOptions.FILE_SIZE_UNLIMITED;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @VideoRecordError
    int mRecordingStopError = ERROR_UNKNOWN;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Throwable mRecordingStopErrorCause = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    AudioState mCachedAudioState;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    EncodedData mPendingFirstVideoData = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    EncodedData mPendingFirstAudioData = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Throwable mAudioErrorCause = null;
    //--------------------------------------------------------------------------------------------//

    Recorder(@Nullable Executor executor, @NonNull MediaSpec mediaSpec) {
        mUserProvidedExecutor = executor;
        mExecutor = executor != null ? executor : CameraXExecutors.ioExecutor();
        mSequentialExecutor = CameraXExecutors.newSequentialExecutor(mExecutor);

        mMediaSpec = MutableStateObservable.withInitialState(composeRecorderMediaSpec(mediaSpec));
    }

    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        synchronized (mLock) {
            switch (mState) {
                case RESETTING:
                    // Fall-through
                case PENDING_RECORDING:
                    // Fall-through
                case PENDING_PAUSED:
                    // Fall-through
                case INITIALIZING:
                    // The recorder should be initialized only once until it is released.
                    // TODO (b/198551531): Make this code more robust to multiple SurfaceRequests.
                    if (!mSurfaceRequested) {
                        mSurfaceRequested = true;
                        mSequentialExecutor.execute(() -> initializeInternal(request));
                    }
                    break;
                case IDLING:
                    // Fall-through
                case RECORDING:
                    // Fall-through
                case STOPPING:
                    // Fall-through
                case PAUSED:
                    throw new IllegalStateException("Surface was requested when the Recorder had "
                            + "been initialized with state " + mState);
                case ERROR:
                    throw new IllegalStateException("Surface was requested when the Recorder had "
                            + "encountered error " + mErrorCause);
            }
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @NonNull
    public Observable<MediaSpec> getMediaSpec() {
        return mMediaSpec;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @NonNull
    public Observable<StreamState> getStreamState() {
        return mStreamState;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onSourceStateChanged(@NonNull SourceState newState) {
        RecordingRecord pendingRecordingToFinalize = null;
        synchronized (mLock) {
            SourceState oldState = mSourceState;
            mSourceState = newState;
            if (oldState == SourceState.ACTIVE && newState == SourceState.INACTIVE) {
                Logger.d(TAG, "Video source has transitioned to an INACTIVE state.");
                switch (mState) {
                    case PENDING_RECORDING:
                        // Fall-through
                    case PENDING_PAUSED:
                        // Immediately finalize pending recording since it never started.
                        pendingRecordingToFinalize = mPendingRecordingRecord;
                        mPendingRecordingRecord = null;
                        restoreNonPendingState(); // Equivalent to setState(mNonPendingState)
                        break;
                    case PAUSED:
                        // Fall-through
                    case RECORDING:
                        setState(State.STOPPING);
                        RecordingRecord finalActiveRecordingRecord = mActiveRecordingRecord;
                        mSequentialExecutor.execute(() -> stopInternal(finalActiveRecordingRecord,
                                ERROR_SOURCE_INACTIVE, null));
                        break;
                    case STOPPING:
                        // Fall-through
                    case RESETTING:
                        // We are already stopping or resetting, nothing needs to be done.
                        break;
                    case INITIALIZING:
                        // Fall-through
                    case IDLING:
                        break;
                    case ERROR:
                        // In an error state, the recording will already be finalized. Nothing
                        // needs to be done.
                        break;
                }
            } else if (oldState == SourceState.INACTIVE && newState == SourceState.ACTIVE) {
                Logger.d(TAG, "Video source has transitioned to an ACTIVE state.");
            }
        }

        if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, ERROR_SOURCE_INACTIVE,
                    PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE);
        }
    }

    /**
     * Prepares a recording that will be saved to a {@link File}.
     *
     * <p>The provided {@link FileOutputOptions} specifies the file to use.
     *
     * <p>Calling this method multiple times will generate multiple {@link PendingRecording}s,
     * each of the recordings can be used to adjust per-recording settings individually. The
     * recording will not begin until {@link PendingRecording#start()} is called. Only a single
     * pending recording can be started per {@link Recorder} instance.
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
     * recording will not begin until {@link PendingRecording#start()} is called. Only a single
     * pending recording can be started per {@link Recorder} instance.
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
     * recording will not begin until {@link PendingRecording#start()} is called. Only a single
     * pending recording can be started per {@link Recorder} instance.
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
     * Gets the aspect ratio of this Recorder.
     */
    @VideoSpec.AspectRatio
    int getAspectRatio() {
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
    ActiveRecording start(@NonNull PendingRecording pendingRecording) {
        Preconditions.checkNotNull(pendingRecording, "The given PendingRecording cannot be null.");
        RecordingRecord alreadyInProgressRecording = null;
        @VideoRecordError int error = ERROR_NONE;
        Throwable errorCause = null;
        long recordingId;
        synchronized (mLock) {
            recordingId = ++mLastGeneratedRecordingId;
            if (mSourceState == SourceState.INACTIVE) {
                error = ERROR_SOURCE_INACTIVE;
                errorCause = PENDING_RECORDING_ERROR_CAUSE_SOURCE_INACTIVE;
            } else {
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
                    case INITIALIZING:
                        mPendingRecordingRecord = RecordingRecord.from(pendingRecording,
                                recordingId);
                        // The recording will automatically start once the initialization completes.
                        setState(State.PENDING_RECORDING);
                        break;
                    case IDLING:
                        Preconditions.checkState(
                                mActiveRecordingRecord == null && mPendingRecordingRecord == null,
                                "Expected recorder to be idle but a recording is either pending or "
                                        + "in progress.");
                        mPendingRecordingRecord = RecordingRecord.from(pendingRecording,
                                recordingId);
                        setState(State.PENDING_RECORDING);
                        mSequentialExecutor.execute(this::tryServicePendingRecording);
                        break;
                    case ERROR:
                        error = ERROR_RECORDER_ERROR;
                        errorCause = mErrorCause;
                        break;
                }
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
            return ActiveRecording.createFinalizedFrom(pendingRecording, recordingId);
        }

        return ActiveRecording.from(pendingRecording, recordingId);
    }

    void pause(@NonNull ActiveRecording activeRecording) {
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this ActiveRecording is no longer active, log and treat as a no-op.
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
                case INITIALIZING:
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

    void resume(@NonNull ActiveRecording activeRecording) {
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this ActiveRecording is no longer active, log and treat as a no-op.
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
                case INITIALIZING:
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

    void stop(@NonNull ActiveRecording activeRecording) {
        RecordingRecord pendingRecordingToFinalize = null;
        synchronized (mLock) {
            if (!isSameRecording(activeRecording, mPendingRecordingRecord) && !isSameRecording(
                    activeRecording, mActiveRecordingRecord)) {
                // If this ActiveRecording is no longer active, log and treat as a no-op.
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
                case INITIALIZING:
                    // Fall-through
                case IDLING:
                    throw new IllegalStateException("Calling stop() while idling or "
                            + "initializing is invalid.");
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    setState(State.STOPPING);
                    RecordingRecord finalActiveRecordingRecord = mActiveRecordingRecord;
                    mSequentialExecutor.execute(() -> stopInternal(finalActiveRecordingRecord,
                            ERROR_NONE, null));
                    break;
                case ERROR:
                    // In an error state, the recording will already be finalized. Treat as a
                    // no-op in stop()
                    break;
            }
        }

        if (pendingRecordingToFinalize != null) {
            finalizePendingRecording(pendingRecordingToFinalize, ERROR_NO_VALID_DATA,
                    new RuntimeException("Recording was stopped before any data could be "
                            + "produced."));
        }
    }

    private void finalizePendingRecording(@NonNull RecordingRecord recordingToFinalize,
            @VideoRecordError int error, @Nullable Throwable cause) {
        recordingToFinalize.updateVideoRecordEvent(
                VideoRecordEvent.finalizeWithError(
                        recordingToFinalize.getOutputOptions(),
                        RecordingStats.of(/*duration=*/0L,
                                /*bytes=*/0L,
                                AudioStats.of(AudioStats.AUDIO_STATE_DISABLED, mAudioErrorCause)),
                        OutputResults.of(Uri.EMPTY),
                        error,
                        cause));
    }

    /**
     * Resets the state on the sequential executor for a new recording.
     *
     * <p>If a recording is in progress, it will be stopped asynchronously and reset once it has
     * been finalized.
     *
     * <p>If there is a recording in progress, reset() will stop the recording and rely on the
     * recording's onRecordingFinalized() to actually release resources.
     */
    @ExecutedBy("mSequentialExecutor")
    void reset() {
        boolean shouldReset = false;
        boolean shouldStop = false;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_RECORDING:
                    // Fall-through
                case PENDING_PAUSED:
                    // Fall-through
                    mSurfaceRequested = false;
                    shouldReset = true;
                    updateNonPendingState(State.RESETTING);
                    break;
                case INITIALIZING:
                    // Fall-through
                case ERROR:
                    // Fall-through
                case IDLING:
                    setState(State.INITIALIZING);
                    mSurfaceRequested = false;
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
                    setState(State.RESETTING);
                    shouldStop = true;
                    break;
                case STOPPING:
                    // Already stopping. Set state to RESETTING so resources will be released once
                    // onRecordingFinalized() runs.
                    setState(State.RESETTING);
                    // Fall-through
                case RESETTING:
                    // No-Op, the Recorder is already being reset.
                    break;
            }
        }

        // These calls must not be posted to the executor to ensure they are executed inline on
        // the sequential executor and the state changes above are correctly handled.
        if (shouldReset) {
            resetInternal();
        } else if (shouldStop) {
            stopInternal(mInProgressRecording, ERROR_NONE, null);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void initializeInternal(SurfaceRequest surfaceRequest) {
        if (mSurface != null) {
            // The video encoder has already be created, providing the surface directly.
            surfaceRequest.provideSurface(mSurface, mSequentialExecutor, (result) -> {
                Surface resultSurface = result.getSurface();
                // The latest surface will be released by the encoder when encoder is released.
                if (mSurface != resultSurface) {
                    resultSurface.release();
                }
                mSurface = null;
                reset();
            });
            onInitialized();
        } else {
            setupVideo(surfaceRequest);
            surfaceRequest.setTransformationInfoListener(mSequentialExecutor,
                    (transformationInfo) -> mSurfaceTransformationInfo =
                            transformationInfo);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void onInitialized() {
        RecordingRecord recordingToStart = null;
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
                    // Fall-through
                case STOPPING:
                    throw new AssertionError(
                            "Incorrectly invoke onInitialized() in state " + mState);
                case INITIALIZING:
                    setState(State.IDLING);
                    break;
                case ERROR:
                    Logger.e(TAG,
                            "onInitialized() was invoked when the Recorder had encountered error "
                                    + mErrorCause);
                    break;
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall through
                case PENDING_RECORDING:
                    recordingToStart = makePendingRecordingActiveLocked(mState);
                    break;
            }
        }

        if (recordingToStart != null) {
            // Start new active recording inline on sequential executor (but unlocked).
            startActiveRecording(recordingToStart, startRecordingPaused);
        }
    }

    @NonNull
    private MediaSpec composeRecorderMediaSpec(@NonNull MediaSpec mediaSpec) {
        MediaSpec.Builder mediaSpecBuilder = mediaSpec.toBuilder();
        if (mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO) {
            mediaSpecBuilder.setOutputFormat(MEDIA_SPEC_DEFAULT.getOutputFormat());
        }

        // Append default audio configurations
        AudioSpec audioSpec = mediaSpec.getAudioSpec();
        if (audioSpec.getSourceFormat() == AudioSpec.SOURCE_FORMAT_AUTO) {
            mediaSpecBuilder.configureAudio(
                    builder -> builder.setSourceFormat(AUDIO_SPEC_DEFAULT.getSourceFormat()));
        }
        if (audioSpec.getSource() == AudioSpec.SOURCE_AUTO) {
            mediaSpecBuilder.configureAudio(
                    builder -> builder.setSource(AUDIO_SPEC_DEFAULT.getSource()));
        }
        if (audioSpec.getChannelCount() == AudioSpec.CHANNEL_COUNT_AUTO) {
            mediaSpecBuilder.configureAudio(
                    builder -> builder.setChannelCount(AUDIO_SPEC_DEFAULT.getChannelCount()));
        }

        // Append default video configurations
        VideoSpec videoSpec = mediaSpec.getVideoSpec();
        if (videoSpec.getAspectRatio() == VideoSpec.ASPECT_RATIO_AUTO) {
            mediaSpecBuilder.configureVideo(
                    builder -> builder.setAspectRatio(VIDEO_SPEC_DEFAULT.getAspectRatio()));
        }

        return mediaSpecBuilder.build();
    }

    private static boolean isSameRecording(@NonNull ActiveRecording activeRecording,
            @Nullable RecordingRecord recordingRecord) {
        if (recordingRecord == null) {
            return false;
        }

        return activeRecording.getRecordingId() == recordingRecord.getRecordingId();
    }

    @ExecutedBy("mSequentialExecutor")
    @NonNull
    private AudioEncoderConfig composeAudioEncoderConfig(@NonNull MediaSpec mediaSpec) {
        return AudioEncoderConfig.builder()
                .setMimeType(MediaSpec.outputFormatToAudioMime(mediaSpec.getOutputFormat()))
                .setBitrate(AUDIO_BITRATE_DEFAULT)
                .setSampleRate(selectSampleRate(mediaSpec.getAudioSpec()))
                .setChannelCount(mediaSpec.getAudioSpec().getChannelCount())
                .build();
    }

    @ExecutedBy("mSequentialExecutor")
    @NonNull
    private VideoEncoderConfig composeVideoEncoderConfig(@NonNull MediaSpec mediaSpec,
            @NonNull Size surfaceSize) {
        return VideoEncoderConfig.builder()
                .setMimeType(MediaSpec.outputFormatToVideoMime(mediaSpec.getOutputFormat()))
                .setResolution(surfaceSize)
                // TODO: Add mechanism to pick a value from the specified range and
                //  CamcorderProfile.
                .setBitrate(VIDEO_BITRATE_DEFAULT)
                .setFrameRate(VIDEO_FRAME_RATE_DEFAULT)
                .setColorFormat(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                .setIFrameInterval(VIDEO_INTRA_FRAME_INTERVAL_DEFAULT)
                .build();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @ExecutedBy("mSequentialExecutor")
    private void setupAudioIfNeeded(@NonNull RecordingRecord activeRecording) {
        if (!activeRecording.hasAudioEnabled()) {
            // Skip if audio is not enabled for the recording.
            return;
        }

        if (!isAudioSupported()) {
            throw new IllegalStateException("The Recorder doesn't support recording with audio");
        }

        if (mAudioInitialized) {
            // Skip if audio has already been initialized.
            return;
        }

        MediaSpec mediaSpec = getObservableData(mMediaSpec);
        AudioEncoderConfig config = composeAudioEncoderConfig(mediaSpec);

        try {
            mAudioEncoder = new EncoderImpl(mExecutor, config);
        } catch (InvalidConfigException e) {
            Logger.e(TAG, "Unable to initialize audio encoder." + e);
            onEncoderSetupError(e);
            return;
        }

        Encoder.EncoderInput bufferProvider = mAudioEncoder.getInput();
        if (!(bufferProvider instanceof Encoder.ByteBufferInput)) {
            throw new AssertionError("The EncoderInput of audio isn't a ByteBufferInput.");
        }
        try {
            mAudioSource = setupAudioSource((Encoder.ByteBufferInput) bufferProvider,
                    mediaSpec.getAudioSpec());
        } catch (AudioSourceAccessException e) {
            Logger.e(TAG, "Unable to create audio source." + e);
            throw new AssertionError("Unable to create audio source.", e);
        }

        mAudioInitialized = true;
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @NonNull
    private AudioSource setupAudioSource(@NonNull BufferProvider<InputBuffer> bufferProvider,
            @NonNull AudioSpec audioSpec) throws AudioSourceAccessException {
        AudioSource audioSource = new AudioSource.Builder()
                .setExecutor(CameraXExecutors.ioExecutor())
                .setBufferProvider(bufferProvider)
                .setAudioSource(audioSpec.getSource())
                .setSampleRate(selectSampleRate(audioSpec))
                .setChannelCount(audioSpec.getChannelCount())
                .setAudioFormat(audioSpec.getSourceFormat())
                .build();
        audioSource.setAudioSourceCallback(mSequentialExecutor,
                new AudioSource.AudioSourceCallback() {
                    @Override
                    public void onSilenced(boolean silenced) {
                        switch (mAudioState) {
                            case DISABLED:
                                // Fall-through
                            case ENCODER_ERROR:
                                // Fall-through
                            case INITIALIZING:
                                // No-op
                                break;
                            case ACTIVE:
                                if (silenced) {
                                    mCachedAudioState = mAudioState;
                                    setAudioState(AudioState.SOURCE_SILENCED);
                                    mAudioErrorCause = new IllegalStateException("The audio "
                                            + "source has been silenced.");
                                    updateInProgressStatusEvent();
                                }
                                break;
                            case SOURCE_SILENCED:
                                if (!silenced) {
                                    setAudioState(mCachedAudioState);
                                    mAudioErrorCause = null;
                                    updateInProgressStatusEvent();
                                }
                                break;
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        if (throwable instanceof AudioSourceAccessException) {
                            setAudioState(AudioState.DISABLED);
                            updateInProgressStatusEvent();
                        }
                    }
                });
        return audioSource;
    }

    @ExecutedBy("mSequentialExecutor")
    private int selectSampleRate(AudioSpec audioSpec) {
        // The default sample rate should work on most devices. May consider throw an
        // exception or have other way to notify users that the specified sample rate
        // can not be satisfied.
        int selectedSampleRate = AUDIO_SAMPLE_RATE_DEFAULT;
        for (int sampleRate : AudioSource.COMMON_SAMPLE_RATES) {
            if (audioSpec.getSampleRate().contains(sampleRate)) {
                if (AudioSource.isSettingsSupported(sampleRate, audioSpec.getChannelCount(),
                        audioSpec.getSourceFormat())) {
                    // Choose the largest valid sample rate as the list has descending order.
                    selectedSampleRate = sampleRate;
                    break;
                }
            }
        }

        return selectedSampleRate;
    }

    @ExecutedBy("mSequentialExecutor")
    private void setupVideo(@NonNull SurfaceRequest surfaceRequest) {
        MediaSpec mediaSpec = getObservableData(mMediaSpec);
        VideoEncoderConfig config = composeVideoEncoderConfig(mediaSpec,
                surfaceRequest.getResolution());

        try {
            mVideoEncoder = new EncoderImpl(mExecutor, config);
        } catch (InvalidConfigException e) {
            surfaceRequest.willNotProvideSurface();
            Logger.e(TAG, "Unable to initialize video encoder." + e);
            onEncoderSetupError(e);
            return;
        }

        Encoder.EncoderInput encoderInput = mVideoEncoder.getInput();
        if (!(encoderInput instanceof Encoder.SurfaceInput)) {
            throw new AssertionError("The EncoderInput of video isn't a SurfaceInput.");
        }
        ((Encoder.SurfaceInput) encoderInput).setOnSurfaceUpdateListener(
                mSequentialExecutor,
                surface -> {
                    mSurface = surface;
                    surfaceRequest.provideSurface(surface, mSequentialExecutor, (result) -> {
                        Surface resultSurface = result.getSurface();
                        // The latest surface will be released by the encoder when encoder is
                        // released.
                        if (mSurface != resultSurface) {
                            resultSurface.release();
                        }
                        mSurface = null;
                        reset();
                    });
                    onInitialized();
                });
    }

    @ExecutedBy("mSequentialExecutor")
    private void onEncoderSetupError(@Nullable Throwable cause) {
        RecordingRecord pendingRecordingToFinalize = null;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_PAUSED:
                    // Fall-through
                case PENDING_RECORDING:
                    pendingRecordingToFinalize = mPendingRecordingRecord;
                    mPendingRecordingRecord = null;
                    // Fall-through
                case INITIALIZING:
                    setState(State.ERROR);
                    mErrorCause = cause;
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

        if (isAudioEnabled() && mPendingFirstAudioData == null) {
            throw new AssertionError("Audio is enabled but no audio sample is ready. Cannot start"
                    + " media muxer.");
        }

        if (mPendingFirstVideoData == null) {
            throw new AssertionError("Media muxer cannot be started without an encoded video "
                    + "frame.");
        }

        try (EncodedData videoDataToWrite = mPendingFirstVideoData; EncodedData audioDataToWrite =
                mPendingFirstAudioData) {
            mPendingFirstVideoData = null;
            mPendingFirstAudioData = null;
            // Make sure we can write the first audio and video data without hitting the file size
            // limit. Otherwise we will be left with a malformed (empty) track on stop.
            long firstDataSize = videoDataToWrite.size();
            if (audioDataToWrite != null) {
                firstDataSize += audioDataToWrite.size();
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

            try {
                setupMediaMuxer(recordingToStart.getOutputOptions());
            } catch (IOException e) {
                onInProgressRecordingInternalError(recordingToStart, ERROR_INVALID_OUTPUT_OPTIONS,
                        e);
                return;
            }

            mVideoTrackIndex = mMediaMuxer.addTrack(mVideoOutputConfig.getMediaFormat());
            if (isAudioEnabled()) {
                mAudioTrackIndex = mMediaMuxer.addTrack(mAudioOutputConfig.getMediaFormat());
            }
            mMediaMuxer.start();

            // Write first data to ensure tracks are not empty
            writeVideoData(videoDataToWrite, recordingToStart);
            if (audioDataToWrite != null) {
                writeAudioData(audioDataToWrite, recordingToStart);
            }
        }
    }

    @SuppressLint("WrongConstant")
    @ExecutedBy("mSequentialExecutor")
    private void setupMediaMuxer(@NonNull OutputOptions options) throws IOException {
        int muxerOutputFormat = MediaSpec.outputFormatToMuxerFormat(
                getObservableData(mMediaSpec).getOutputFormat());
        if (options instanceof FileOutputOptions) {
            FileOutputOptions fileOutputOptions = (FileOutputOptions) options;
            File file = fileOutputOptions.getFile();
            if (!OutputUtil.createParentFolder(file)) {
                Logger.w(TAG, "Failed to create folder for " + file.getAbsolutePath());
            }
            mMediaMuxer = new MediaMuxer(file.getAbsolutePath(), muxerOutputFormat);
            mOutputUri = Uri.fromFile(file);
        } else if (options instanceof FileDescriptorOutputOptions) {
            FileDescriptorOutputOptions fileDescriptorOutputOptions =
                    (FileDescriptorOutputOptions) options;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaMuxer = Api26Impl.createMediaMuxer(
                        fileDescriptorOutputOptions.getParcelFileDescriptor()
                                .getFileDescriptor(), muxerOutputFormat);
            } else {
                throw new IOException(
                        "MediaMuxer doesn't accept FileDescriptor as output destination.");
            }
        } else if (options instanceof MediaStoreOutputOptions) {
            MediaStoreOutputOptions mediaStoreOutputOptions = (MediaStoreOutputOptions) options;

            ContentValues contentValues =
                    new ContentValues(mediaStoreOutputOptions.getContentValues());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Toggle on pending status for the video file.
                contentValues.put(MediaStore.Video.Media.IS_PENDING, PENDING);
            }
            Uri outputUri = mediaStoreOutputOptions.getContentResolver().insert(
                    mediaStoreOutputOptions.getCollectionUri(), contentValues);
            if (outputUri == null) {
                throw new IOException("Unable to create MediaStore entry.");
            }
            mOutputUri = outputUri;  // Guarantee mOutputUri is non-null value.

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                String path =
                        OutputUtil.getAbsolutePathFromUri(
                                mediaStoreOutputOptions.getContentResolver(),
                                mOutputUri, MEDIA_COLUMN);
                if (path == null) {
                    throw new IOException("Unable to get path from uri " + mOutputUri);
                }
                if (!OutputUtil.createParentFolder(new File(path))) {
                    Logger.w(TAG, "Failed to create folder for " + path);
                }
                mMediaMuxer = new MediaMuxer(path, muxerOutputFormat);
            } else {
                ParcelFileDescriptor fileDescriptor =
                        mediaStoreOutputOptions.getContentResolver().openFileDescriptor(
                                mOutputUri, "rw");
                mMediaMuxer = Api26Impl.createMediaMuxer(fileDescriptor.getFileDescriptor(),
                        muxerOutputFormat);
                fileDescriptor.close();
            }
        } else {
            throw new AssertionError(
                    "Invalid output options type: " + options.getClass().getSimpleName());
        }
        // TODO: Add more metadata to MediaMuxer, e.g. location information.
        if (mSurfaceTransformationInfo != null) {
            mMediaMuxer.setOrientationHint(mSurfaceTransformationInfo.getRotationDegrees());
        }
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

        setupAudioIfNeeded(recordingToStart);

        mInProgressRecording = recordingToStart;
        if (mAudioState == AudioState.INITIALIZING) {
            setAudioState(recordingToStart.hasAudioEnabled() ? AudioState.ACTIVE
                    : AudioState.DISABLED);
        }

        initEncoderCallbacks(recordingToStart);
        if (isAudioEnabled()) {
            mAudioSource.start();
            mAudioEncoder.start();
        }
        mVideoEncoder.start();

        mInProgressRecording.updateVideoRecordEvent(VideoRecordEvent.start(
                mInProgressRecording.getOutputOptions(),
                getInProgressRecordingStats()));
    }

    @ExecutedBy("mSequentialExecutor")
    private void initEncoderCallbacks(@NonNull RecordingRecord recordingToStart) {
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
                                        if (!isAudioEnabled() || mPendingFirstAudioData != null) {
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
                                // If the audio encoder encounters error, update the status event
                                // to notify users. Then continue recording without audio data.
                                setAudioState(AudioState.ENCODER_ERROR);
                                mAudioErrorCause = e;
                                updateInProgressStatusEvent();
                                completer.set(null);
                            }

                            @ExecutedBy("mSequentialExecutor")
                            @Override
                            public void onEncodedData(@NonNull EncodedData encodedData) {
                                if (mAudioState == AudioState.DISABLED) {
                                    throw new AssertionError(
                                            "Audio is not enabled but audio encoded data is "
                                                    + "produced.");
                                }

                                // If the media muxer doesn't yet exist, we may need to create and
                                // start it. Otherwise we can write the data.
                                if (mMediaMuxer == null) {
                                    if (!mInProgressRecordingStopping) {
                                        boolean cachedDataDropped = false;
                                        if (mPendingFirstAudioData != null) {
                                            cachedDataDropped = true;
                                            mPendingFirstAudioData.close();
                                            mPendingFirstAudioData = null;
                                        }

                                        mPendingFirstAudioData = encodedData;
                                        if (mPendingFirstVideoData != null) {
                                            // Both audio and data are ready. Start the muxer.
                                            Logger.d(TAG, "Received audio data. Starting muxer...");
                                            setupAndStartMediaMuxer(recordingToStart);
                                        } else {
                                            if (cachedDataDropped) {
                                                Logger.d(TAG, "Replaced cached audio data with "
                                                        + "newer data.");
                                            } else {
                                                Logger.d(TAG, "Cached audio data while we wait for "
                                                        + "video keyframe before starting muxer.");
                                            }
                                        }
                                    } else {
                                        // Recording is stopping before muxer has been started.
                                        Logger.d(TAG,
                                                "Drop audio data since recording is stopping.");
                                        encodedData.close();
                                    }
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
                        finalizeInProgressRecording(mRecordingStopError, mRecordingStopErrorCause);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        finalizeInProgressRecording(ERROR_ENCODING_FAILED, t);
                    }
                },
                // Can use direct executor since completers are always completed on sequential
                // executor.
                CameraXExecutors.directExecutor());
    }

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

        mMediaMuxer.writeSampleData(mVideoTrackIndex, encodedData.getByteBuffer(),
                encodedData.getBufferInfo());

        mRecordingBytes = newRecordingBytes;

        if (mFirstRecordingVideoDataTimeUs == 0L) {
            mFirstRecordingVideoDataTimeUs = encodedData.getPresentationTimeUs();
        }
        mRecordingDurationNs = TimeUnit.MICROSECONDS.toNanos(
                encodedData.getPresentationTimeUs() - mFirstRecordingVideoDataTimeUs);

        updateInProgressStatusEvent();
    }

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

        mMediaMuxer.writeSampleData(mAudioTrackIndex,
                encodedData.getByteBuffer(),
                encodedData.getBufferInfo());

        mRecordingBytes = newRecordingBytes;
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
    void stopInternal(@NonNull RecordingRecord recordingToStop, @VideoRecordError int stopError,
            @Nullable Throwable errorCause) {
        // Only stop recording if recording is in-progress and it is not already stopping.
        if (mInProgressRecording == recordingToStop && !mInProgressRecordingStopping) {
            mInProgressRecordingStopping = true;
            mRecordingStopError = stopError;
            mRecordingStopErrorCause = errorCause;
            if (isAudioEnabled()) {
                if (mPendingFirstAudioData != null) {
                    mPendingFirstAudioData.close();
                    mPendingFirstAudioData = null;
                }
                mAudioEncoder.stop();
            }
            if (mPendingFirstVideoData != null) {
                mPendingFirstVideoData.close();
                mPendingFirstVideoData = null;
            }
            mVideoEncoder.stop();
        }
    }

    @ExecutedBy("mSequentialExecutor")
    private void resetInternal() {
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
            mAudioOutputConfig = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
            mVideoOutputConfig = null;
        }
        if (mAudioSource != null) {
            mAudioSource.release();
            mAudioSource = null;
        }

        mAudioInitialized = false;
    }

    private int internalAudioStateToAudioStatsState(@NonNull AudioState audioState) {
        switch (audioState) {
            case DISABLED:
                return AudioStats.AUDIO_STATE_DISABLED;
            case INITIALIZING:
                // Fall-through
            case ACTIVE:
                return AudioStats.AUDIO_STATE_ACTIVE;
            case SOURCE_SILENCED:
                return AudioStats.AUDIO_STATE_SOURCE_SILENCED;
            case ENCODER_ERROR:
                return AudioStats.AUDIO_STATE_ENCODER_ERROR;
        }
        // Should not reach.
        throw new AssertionError("Invalid internal audio state: " + audioState);
    }

    @NonNull
    private StreamState internalStateToStreamState(@NonNull State state) {
        // Stopping state should be treated as inactive on certain chipsets. See b/196039619.
        DeactivateEncoderSurfaceBeforeStopEncoderQuirk quirk =
                DeviceQuirks.get(DeactivateEncoderSurfaceBeforeStopEncoderQuirk.class);
        // TODO(b/197047288): For devices that have the above quirk, wait for a signal that the
        //  surface is no longer in use before stopping the video encoder.
        return state == State.RECORDING || (state == State.STOPPING && quirk == null)
                ? StreamState.ACTIVE : StreamState.INACTIVE;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    boolean isAudioEnabled() {
        return mAudioState != AudioState.DISABLED && mAudioState != AudioState.ENCODER_ERROR;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mSequentialExecutor")
    void finalizeInProgressRecording(@VideoRecordError int error, @Nullable Throwable throwable) {
        if (mInProgressRecording == null) {
            throw new AssertionError("Attempted to finalize in-progress recording, but no "
                    + "recording is in progress.");
        }
        int errorToSend = error;
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

        OutputOptions outputOptions = mInProgressRecording.getOutputOptions();
        RecordingStats stats = getInProgressRecordingStats();

        mInProgressRecording.finalizeOutputFile(mOutputUri);

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
        mFirstRecordingVideoDataTimeUs = 0L;
        mRecordingStopError = ERROR_UNKNOWN;
        mRecordingStopErrorCause = null;
        mAudioErrorCause = null;

        onRecordingFinalized(finalizedRecording);
    }

    @ExecutedBy("mSequentialExecutor")
    private void onRecordingFinalized(@NonNull RecordingRecord finalizedRecording) {
        boolean needsReset = false;
        boolean startRecordingPaused = false;
        RecordingRecord recordingToStart = null;
        synchronized (mLock) {
            if (mActiveRecordingRecord != finalizedRecording) {
                throw new AssertionError("Active recording did not match finalized recording on "
                        + "finalize.");
            }

            mActiveRecordingRecord = null;
            switch (mState) {
                case RESETTING:
                    setState(State.INITIALIZING);
                    mSurfaceRequested = false;
                    needsReset = true;
                    break;
                case PAUSED:
                    // Fall-through
                case RECORDING:
                    // If finalized while in a RECORDING or PAUSED state, then the recording was
                    // likely finalized due to an error.
                    // Fall-through
                case STOPPING:
                    setState(State.IDLING);
                    break;
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall-through
                case PENDING_RECORDING:
                    recordingToStart = makePendingRecordingActiveLocked(mState);
                    break;
                case ERROR:
                    // Error state is non-recoverable. Nothing to do here.
                    break;
                case IDLING:
                    // Fall-through
                case INITIALIZING:
                    throw new AssertionError("Unexpected state on finalize of recording: "
                            + mState);
            }
        }

        // Perform required actions from state changes inline on sequential executor but unlocked.
        if (needsReset) {
            resetInternal();
        } else if (recordingToStart != null) {
            startActiveRecording(recordingToStart, startRecordingPaused);
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
                case INITIALIZING:
                    // Fall-through
                case IDLING:
                    // Fall-through
                case ERROR:
                    throw new AssertionError("In-progress recording error occurred while in "
                            + "unexpected state: " + mState);
            }
        }

        if (needsStop) {
            stopInternal(recording, error, cause);
        }
    }

    @ExecutedBy("mSequentialExecutor")
    void tryServicePendingRecording() {
        boolean startRecordingPaused = false;
        RecordingRecord recordingToStart = null;
        synchronized (mLock) {
            switch (mState) {
                case PENDING_PAUSED:
                    startRecordingPaused = true;
                    // Fall-through
                case PENDING_RECORDING:
                    if (mActiveRecordingRecord != null) {
                        // Active recording is still finalizing. Pending recording will be
                        // serviced in onRecordingFinalized().
                        break;
                    }
                    recordingToStart = makePendingRecordingActiveLocked(mState);
                    break;
                case INITIALIZING:
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
            startActiveRecording(recordingToStart, startRecordingPaused);
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
    private void startActiveRecording(@NonNull RecordingRecord recordingToStart,
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
                AudioStats.of(internalAudioStateToAudioStatsState(mAudioState), mAudioErrorCause));
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
        StreamState streamState = null;
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
        mStreamState.setState(streamState);
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
            mStreamState.setState(internalStateToStreamState(state));
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
    void setAudioState(AudioState audioState) {
        Logger.d(TAG, "Transitioning audio state: " + mAudioState + " --> " + audioState);
        mAudioState = audioState;
    }

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    @AutoValue
    abstract static class RecordingRecord {

        private final AtomicReference<Consumer<Uri>> mOutputFileFinalizer =
                new AtomicReference<>(ignored -> {
                    /* no-op by default */
                });

        static RecordingRecord from(@NonNull PendingRecording pendingRecording, long recordingId) {
            OutputOptions outputOptions = pendingRecording.getOutputOptions();
            RecordingRecord recordingRecord = new AutoValue_Recorder_RecordingRecord(
                    outputOptions,
                    pendingRecording.getCallbackExecutor(),
                    pendingRecording.getEventListener(),
                    pendingRecording.isAudioEnabled(),
                    recordingId
            );

            if (outputOptions instanceof MediaStoreOutputOptions) {
                MediaStoreOutputOptions mediaStoreOutputOptions =
                        (MediaStoreOutputOptions) outputOptions;
                // TODO(b/201946954): Investigate whether we should add a setting to disable
                //  scan/update to allow users to perform it themselves.
                Consumer<Uri> outputFileFinalizer;
                if (Build.VERSION.SDK_INT >= 29) {
                    outputFileFinalizer = outputUri -> {
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
                    Context finalContext = pendingRecording.getApplicationContext();
                    outputFileFinalizer = outputUri -> {
                        if (outputUri.equals(Uri.EMPTY)) {
                            return;
                        }
                        String filePath = OutputUtil.getAbsolutePathFromUri(
                                mediaStoreOutputOptions.getContentResolver(), outputUri,
                                MEDIA_COLUMN);
                        if (filePath != null) {
                            // Use null mime type list to have MediaScanner derive mime type from
                            // extension
                            MediaScannerConnection.scanFile(finalContext,
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
                recordingRecord.mOutputFileFinalizer.set(outputFileFinalizer);
            }

            return recordingRecord;
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
         * Updates the recording status and callback to users.
         */
        void updateVideoRecordEvent(@NonNull VideoRecordEvent event) {
            Preconditions.checkState(Objects.equals(event.getOutputOptions(), getOutputOptions()),
                    "Attempted to update event listener with event from incorrect recording "
                            + "[Recording: " + event.getOutputOptions() + ", Expected: "
                            + getOutputOptions() + "]");
            if (getCallbackExecutor() != null && getEventListener() != null) {
                try {
                    getCallbackExecutor().execute(() -> getEventListener().accept(event));
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "The callback executor is invalid.", e);
                }
            }
        }

        /**
         * Performs final operations required to prepare completed output file.
         *
         * <p>Output file finalization can only occur once. Any subsequent calls to this method
         * will throw an {@link AssertionError}.
         *
         * @param uri The uri of the output file.
         */
        void finalizeOutputFile(@NonNull Uri uri) {
            Consumer<Uri> outputFileFinalizer = mOutputFileFinalizer.getAndSet(null);
            if (outputFileFinalizer == null) {
                throw new AssertionError(
                        "Output file has already been finalized for recording " + this);
            }

            outputFileFinalizer.accept(uri);
        }
    }

    /**
     * Builder class for {@link Recorder} objects.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder {

        private final MediaSpec.Builder mMediaSpecBuilder;
        private Executor mExecutor = null;

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
         * @see QualitySelector
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
         * Sets the aspect ratio of this Recorder.
         */
        @NonNull
        Builder setAspectRatio(@AspectRatio.Ratio int aspectRatio) {
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

        /**
         * Builds the {@link Recorder} instance.
         *
         * <p>The {code build()} method can be called multiple times, generating a new
         * {@link Recorder} instance each time. The returned instance is configured with the
         * options set on this builder.
         */
        @NonNull
        public Recorder build() {
            return new Recorder(mExecutor, mMediaSpecBuilder.build());
        }
    }
}
