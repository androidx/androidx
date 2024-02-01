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

package androidx.camera.video.internal.encoder;

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.video.internal.utils.CodecUtil.createCodec;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.CONFIGURED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.ERROR;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_RELEASE;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.PENDING_START_PAUSED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.RELEASED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STARTED;
import static androidx.camera.video.internal.encoder.EncoderImpl.InternalState.STOPPING;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.DebugUtils;
import androidx.camera.video.internal.compat.quirk.AudioEncoderIgnoresInputTimestampQuirk;
import androidx.camera.video.internal.compat.quirk.CameraUseInconsistentTimebaseQuirk;
import androidx.camera.video.internal.compat.quirk.CodecStuckOnFlushQuirk;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.EncoderNotUsePersistentInputSurfaceQuirk;
import androidx.camera.video.internal.compat.quirk.SignalEosOutputBufferNotComeQuirk;
import androidx.camera.video.internal.compat.quirk.StopCodecAfterSurfaceRemovalCrashMediaServerQuirk;
import androidx.camera.video.internal.compat.quirk.VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk;
import androidx.camera.video.internal.workaround.VideoTimebaseConverter;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The encoder implementation.
 *
 * <p>An encoder could be either a video encoder or an audio encoder.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class EncoderImpl implements Encoder {

    enum InternalState {
        /**
         * The initial state.
         */
        CONFIGURED,

        /**
         * The state is when encoder is in {@link InternalState#CONFIGURED} state and {@link #start}
         * is called.
         */
        STARTED,

        /**
         * The state is when encoder is in {@link InternalState#STARTED} state and {@link #pause}
         * is called.
         */
        PAUSED,

        /**
         * The state is when encoder is in {@link InternalState#STARTED} state and {@link #stop} is
         * called.
         */
        STOPPING,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state and a
         * {@link #start} is called. It is an extension of {@link InternalState#STOPPING}.
         */
        PENDING_START,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state, then
         * {@link #start} and {@link #pause} is called. It is an extension of
         * {@link InternalState#STOPPING}.
         */
        PENDING_START_PAUSED,

        /**
         * The state is when the encoder is in {@link InternalState#STOPPING} state and a
         * {@link #release} is called. It is an extension of {@link InternalState#STOPPING}.
         */
        PENDING_RELEASE,

        /**
         * Then state is when the encoder encounter error. Error state is a transitional state
         * where encoder user is supposed to wait for {@link EncoderCallback#onEncodeStop} or
         * {@link EncoderCallback#onEncodeError}. Any method call during this state should be
         * ignore except {@link #release}.
         */
        ERROR,

        /** The state is when the encoder is released. */
        RELEASED,
    }

    private static final boolean DEBUG = false;
    private static final long NO_LIMIT_LONG = Long.MAX_VALUE;
    private static final Range<Long> NO_RANGE = Range.create(NO_LIMIT_LONG, NO_LIMIT_LONG);
    private static final long STOP_TIMEOUT_MS = 1000L;
    private static final long SIGNAL_EOS_TIMEOUT_MS = 1000L;

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final String mTag;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final Object mLock = new Object();
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final boolean mIsVideoEncoder;
    private final MediaFormat mMediaFormat;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final MediaCodec mMediaCodec;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final EncoderInput mEncoderInput;
    private final EncoderInfo mEncoderInfo;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final Executor mEncoderExecutor;
    private final ListenableFuture<Void> mReleasedFuture;
    private final Completer<Void> mReleasedCompleter;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final Queue<Integer> mFreeInputBufferIndexQueue = new ArrayDeque<>();
    private final Queue<Completer<InputBuffer>> mAcquisitionQueue = new ArrayDeque<>();
    private final Set<InputBuffer> mInputBufferSet = new HashSet<>();
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final Set<EncodedDataImpl> mEncodedDataSet = new HashSet<>();
    /*
     * mActivePauseResumeTimeRanges is a queue used to track all active pause/resume time ranges.
     * An active pause/resume range means the latest output buffer still has not exceeded this
     * range, so this range is still needed to check for later output buffers. The first element
     * in the queue is the oldest range and the last element is the newest.
     */
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    final Deque<Range<Long>> mActivePauseResumeTimeRanges = new ArrayDeque<>();
    final Timebase mInputTimebase;
    final TimeProvider mTimeProvider = new SystemTimeProvider();

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @GuardedBy("mLock")
    EncoderCallback mEncoderCallback = EncoderCallback.EMPTY;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @GuardedBy("mLock")
    Executor mEncoderCallbackExecutor = CameraXExecutors.directExecutor();
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    InternalState mState;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    Range<Long> mStartStopTimeRangeUs = NO_RANGE;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    long mTotalPausedDurationUs = 0L;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    boolean mPendingCodecStop = false;
    // The data timestamp that an encoding stops at. If this timestamp is null, it means the
    // encoding hasn't receiving enough data to be stopped.
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    Long mLastDataStopTimestamp = null;
    @SuppressWarnings("WeakerAccess") // synthetic accessor
    Future<?> mStopTimeoutFuture = null;
    private MediaCodecCallback mMediaCodecCallback = null;

    private boolean mIsFlushedAfterEndOfStream = false;
    private boolean mSourceStoppedSignalled = false;
    boolean mMediaCodecEosSignalled = false;
    @Nullable
    private Future<?> mSignalEosTimeoutFuture;

    /**
     * Creates the encoder with a {@link EncoderConfig}
     *
     * @param executor      the executor suitable for background task
     * @param encoderConfig the encoder config
     * @throws InvalidConfigException when the encoder cannot be configured.
     */
    public EncoderImpl(@NonNull Executor executor, @NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(encoderConfig);

        mMediaCodec = createCodec(encoderConfig);
        MediaCodecInfo mediaCodecInfo = mMediaCodec.getCodecInfo();
        mEncoderExecutor = CameraXExecutors.newSequentialExecutor(executor);
        mMediaFormat = encoderConfig.toMediaFormat();
        mInputTimebase = encoderConfig.getInputTimebase();
        if (encoderConfig instanceof AudioEncoderConfig) {
            mTag = "AudioEncoder";
            mIsVideoEncoder = false;
            mEncoderInput = new ByteBufferInput();
            mEncoderInfo = new AudioEncoderInfoImpl(mediaCodecInfo, encoderConfig.getMimeType());
        } else if (encoderConfig instanceof VideoEncoderConfig) {
            mTag = "VideoEncoder";
            mIsVideoEncoder = true;
            mEncoderInput = new SurfaceInput();
            VideoEncoderInfo videoEncoderInfo = new VideoEncoderInfoImpl(mediaCodecInfo,
                    encoderConfig.getMimeType());
            clampVideoBitrateIfNotSupported(videoEncoderInfo, mMediaFormat);
            mEncoderInfo = videoEncoderInfo;
        } else {
            throw new InvalidConfigException("Unknown encoder config type");
        }

        Logger.d(mTag, "mInputTimebase = " + mInputTimebase);
        Logger.d(mTag, "mMediaFormat = " + mMediaFormat);

        try {
            reset();
        } catch (MediaCodec.CodecException e) {
            throw new InvalidConfigException(e);
        }

        AtomicReference<Completer<Void>> releaseFutureRef = new AtomicReference<>();
        mReleasedFuture = Futures.nonCancellationPropagating(
                CallbackToFutureAdapter.getFuture(completer -> {
                    releaseFutureRef.set(completer);
                    return "mReleasedFuture";
                }));
        mReleasedCompleter = Preconditions.checkNotNull(releaseFutureRef.get());

        setState(CONFIGURED);
    }

    /**
     * Clamps the video bitrate in MediaFormat if the video bitrate is not supported by the
     * supplied VideoEncoderInfo.
     *
     * @param videoEncoderInfo VideoEncoderInfo object
     * @param mediaFormat      MediaFormat object
     */
    private void clampVideoBitrateIfNotSupported(@NonNull VideoEncoderInfo videoEncoderInfo,
            @NonNull MediaFormat mediaFormat) {
        checkState(mIsVideoEncoder);
        if (mediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            int origBitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
            int newBitrate = videoEncoderInfo.getSupportedBitrateRange().clamp(origBitrate);
            if (origBitrate != newBitrate) {
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, newBitrate);
                Logger.d(mTag, "updated bitrate from " + origBitrate + " to " + newBitrate);
            }
        }
    }

    @ExecutedBy("mEncoderExecutor")
    private void reset() {
        mStartStopTimeRangeUs = NO_RANGE;
        mTotalPausedDurationUs = 0L;
        mActivePauseResumeTimeRanges.clear();
        mFreeInputBufferIndexQueue.clear();

        // Cancel incomplete acquisitions if exists.
        for (Completer<InputBuffer> completer : mAcquisitionQueue) {
            completer.setCancelled();
        }
        mAcquisitionQueue.clear();

        mMediaCodec.reset();
        mIsFlushedAfterEndOfStream = false;
        mSourceStoppedSignalled = false;
        mMediaCodecEosSignalled = false;
        mPendingCodecStop = false;
        if (mStopTimeoutFuture != null) {
            mStopTimeoutFuture.cancel(true);
            mStopTimeoutFuture = null;
        }
        if (mSignalEosTimeoutFuture != null) {
            mSignalEosTimeoutFuture.cancel(false);
            mSignalEosTimeoutFuture = null;
        }
        if (mMediaCodecCallback != null) {
            mMediaCodecCallback.stop();
        }
        mMediaCodecCallback = new MediaCodecCallback();
        mMediaCodec.setCallback(mMediaCodecCallback);

        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        if (mEncoderInput instanceof SurfaceInput) {
            ((SurfaceInput) mEncoderInput).resetSurface();
        }
    }

    /** Gets the {@link EncoderInput} of the encoder */
    @Override
    @NonNull
    public EncoderInput getInput() {
        return mEncoderInput;
    }

    @NonNull
    @Override
    public EncoderInfo getEncoderInfo() {
        return mEncoderInfo;
    }

    @Override
    public int getConfiguredBitrate() {
        int configuredBitrate = 0;
        if (mMediaFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            configuredBitrate = mMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        }
        return configuredBitrate;
    }

    /**
     * Starts the encoder.
     *
     * <p>If the encoder is not started yet, it will first trigger
     * {@link EncoderCallback#onEncodeStart}. Then continually invoke the
     * {@link EncoderCallback#onEncodedData} callback until the encoder is paused, stopped or
     * released. It can call {@link #pause} to pause the encoding after started. If the encoder is
     * in paused state, then calling this method will resume the encoding.
     */
    @SuppressWarnings("StatementWithEmptyBody") // to better organize the logic and comments
    @Override
    public void start() {
        final long startTriggerTimeUs = generatePresentationTimeUs();
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                    mLastDataStopTimestamp = null;

                    Logger.d(mTag, "Start on " + DebugUtils.readableUs(startTriggerTimeUs));
                    try {
                        if (mIsFlushedAfterEndOfStream) {
                            // If the codec is flushed after an end-of-stream, it was never
                            // signalled that the source stopped, so we will reset the codec
                            // before starting it again.
                            reset();
                        }
                        mStartStopTimeRangeUs = Range.create(startTriggerTimeUs, NO_LIMIT_LONG);
                        mMediaCodec.start();
                    } catch (MediaCodec.CodecException e) {
                        handleEncodeError(e);
                        return;
                    }
                    if (mEncoderInput instanceof ByteBufferInput) {
                        ((ByteBufferInput) mEncoderInput).setActive(true);
                    }
                    setState(STARTED);
                    break;
                case PAUSED:
                    // Resume

                    // The Encoder has been resumed, so reset the stop timestamp flags.
                    mLastDataStopTimestamp = null;

                    final Range<Long> pauseRange = mActivePauseResumeTimeRanges.removeLast();
                    checkState(
                            pauseRange != null && pauseRange.getUpper() == NO_LIMIT_LONG,
                            "There should be a \"pause\" before \"resume\"");
                    final long pauseTimeUs = pauseRange.getLower();
                    mActivePauseResumeTimeRanges.addLast(
                            Range.create(pauseTimeUs, startTriggerTimeUs));
                    // Do not update total paused duration here since current output buffer may
                    // still before the pause range.

                    Logger.d(mTag, "Resume on " + DebugUtils.readableUs(startTriggerTimeUs)
                            + "\nPaused duration = " + DebugUtils.readableUs(
                            (startTriggerTimeUs - pauseTimeUs))
                    );

                    if (!mIsVideoEncoder && DeviceQuirks.get(
                            AudioEncoderIgnoresInputTimestampQuirk.class) != null) {
                        // Do nothing. Since we keep handling audio data in the codec after
                        // paused, we don't have to resume the codec and the input source.
                    } else if (mIsVideoEncoder && DeviceQuirks.get(
                            VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk.class) != null) {
                        // Do nothing. Since we don't pause the codec when paused, we don't have
                        // to resume the codec.
                    } else {
                        setMediaCodecPaused(false);
                        if (mEncoderInput instanceof ByteBufferInput) {
                            ((ByteBufferInput) mEncoderInput).setActive(true);
                        }
                    }
                    // If this is a video encoder, then request a key frame in order to complete
                    // the resume process as soon as possible in MediaCodec.Callback
                    // .onOutputBufferAvailable().
                    if (mIsVideoEncoder) {
                        requestKeyFrameToMediaCodec();
                    }
                    setState(STARTED);
                    break;
                case STARTED:
                case ERROR:
                case PENDING_START:
                    // Do nothing
                    break;
                case STOPPING:
                case PENDING_START_PAUSED:
                    setState(PENDING_START);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        stop(NO_TIMESTAMP);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(long expectedStopTimeUs) {
        final long stopTriggerTimeUs = generatePresentationTimeUs();
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case STOPPING:
                case ERROR:
                    // Do nothing
                    break;
                case STARTED:
                case PAUSED:
                    InternalState currentState = mState;
                    setState(STOPPING);
                    final long startTimeUs = mStartStopTimeRangeUs.getLower();
                    if (startTimeUs == NO_LIMIT_LONG) {
                        throw new AssertionError("There should be a \"start\" before \"stop\"");
                    }
                    long stopTimeUs;
                    if (expectedStopTimeUs == NO_TIMESTAMP) {
                        stopTimeUs = stopTriggerTimeUs;
                    } else if (expectedStopTimeUs < startTimeUs) {
                        // If the recording is stopped immediately after started, it's possible
                        // that the expected stop time is less than the start time because the
                        // encoder is run on different executor. Ignore the expected stop time in
                        // this case so that the recording can be stopped correctly.
                        Logger.w(mTag, "The expected stop time is less than the start time. Use "
                                + "current time as stop time.");
                        stopTimeUs = stopTriggerTimeUs;
                    } else {
                        stopTimeUs = expectedStopTimeUs;
                    }
                    if (stopTimeUs < startTimeUs) {
                        throw new AssertionError("The start time should be before the stop time.");
                    }
                    // Store the stop time. The codec will be stopped after receiving the data
                    // that has a timestamp equal or greater than the stop time.
                    mStartStopTimeRangeUs = Range.create(startTimeUs, stopTimeUs);
                    Logger.d(mTag, "Stop on " + DebugUtils.readableUs(stopTimeUs));
                    // If the Encoder is paused and has received enough data, directly signal
                    // the codec to stop.
                    if (currentState == PAUSED && mLastDataStopTimestamp != null) {
                        signalCodecStop();
                    } else {
                        mPendingCodecStop = true;
                        // If somehow the data doesn't reach the expected timestamp before it
                        // times out, stop the codec so that the Encoder can at least be stopped.
                        // Set mDataStopTimeStamp to be null in order to catch this issue in test.
                        mStopTimeoutFuture =
                                mainThreadExecutor().schedule(
                                        () -> mEncoderExecutor.execute(() -> {
                                            if (mPendingCodecStop) {
                                                Logger.w(mTag,
                                                        "The data didn't reach the expected "
                                                                + "timestamp before timeout, stop"
                                                                + " the codec.");
                                                mLastDataStopTimestamp = null;
                                                signalCodecStop();
                                                mPendingCodecStop = false;
                                            }
                                        }), STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                    break;
                case PENDING_START:
                case PENDING_START_PAUSED:
                    setState(CONFIGURED);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        });
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void signalCodecStop() {
        Logger.d(mTag, "signalCodecStop");
        if (mEncoderInput instanceof ByteBufferInput) {
            ((ByteBufferInput) mEncoderInput).setActive(false);
            // Wait for all issued input buffer done to avoid input loss.
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            for (InputBuffer inputBuffer : mInputBufferSet) {
                futures.add(inputBuffer.getTerminationFuture());
            }
            Futures.successfulAsList(futures).addListener(this::signalEndOfInputStream,
                    mEncoderExecutor);
        } else if (mEncoderInput instanceof SurfaceInput) {
            try {
                addSignalEosTimeoutIfNeeded();
                mMediaCodec.signalEndOfInputStream();
                mMediaCodecEosSignalled = true;
            } catch (MediaCodec.CodecException e) {
                handleEncodeError(e);
            }
        }
    }

    /**
     * Pauses the encoder.
     *
     * <p>{@code pause} only work between {@link #start} and {@link #stop}. Once the encoder is
     * paused, it will drop the input data until {@link #start} is invoked again.
     */
    @Override
    public void pause() {
        final long pauseTriggerTimeUs = generatePresentationTimeUs();
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case PAUSED:
                case ERROR:
                case STOPPING:
                case PENDING_START_PAUSED:
                    // Do nothing
                    break;
                case PENDING_START:
                    setState(PENDING_START_PAUSED);
                    break;
                case STARTED:
                    // Create and insert a pause/resume range.
                    Logger.d(mTag, "Pause on " + DebugUtils.readableUs(pauseTriggerTimeUs));
                    mActivePauseResumeTimeRanges.addLast(
                            Range.create(pauseTriggerTimeUs, NO_LIMIT_LONG));
                    setState(PAUSED);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    throw new IllegalStateException("Encoder is released");
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        });
    }

    /**
     * Releases the encoder.
     *
     * <p>Once the encoder is released, it cannot be used anymore. Any other method call after
     * the encoder is released will get {@link IllegalStateException}. If it is in encoding, make
     * sure call {@link #stop} before {@code release} to normally end the stream, or it may get
     * uncertain result if call {@code release} while encoding.
     */
    @Override
    public void release() {
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case CONFIGURED:
                case STARTED:
                case PAUSED:
                case ERROR:
                    releaseInternal();
                    break;
                case STOPPING:
                case PENDING_START:
                case PENDING_START_PAUSED:
                    setState(PENDING_RELEASE);
                    break;
                case PENDING_RELEASE:
                case RELEASED:
                    // Do nothing
                    break;
                default:
                    throw new IllegalStateException("Unknown state: " + mState);
            }
        });
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public ListenableFuture<Void> getReleasedFuture() {
        return mReleasedFuture;
    }

    /**
     * Sends a hint to the encoder that the source has stopped producing data.
     *
     * <p>This will allow the encoder to reset when it is stopped and no more input data is
     * incoming. This can optimize the time needed to start the next session with
     * {@link #start()} and can regenerate a {@link Surface} on devices that don't support
     * persistent input surfaces.
     */
    public void signalSourceStopped() {
        mEncoderExecutor.execute(() -> {
            mSourceStoppedSignalled = true;
            if (mIsFlushedAfterEndOfStream) {
                mMediaCodec.stop();
                reset();
            }
        });
    }

    @ExecutedBy("mEncoderExecutor")
    private void releaseInternal() {
        if (mIsFlushedAfterEndOfStream) {
            mMediaCodec.stop();
            mIsFlushedAfterEndOfStream = false;
        }

        mMediaCodec.release();

        if (mEncoderInput instanceof SurfaceInput) {
            ((SurfaceInput) mEncoderInput).releaseSurface();
        }

        setState(RELEASED);

        mReleasedCompleter.set(null);
    }

    /**
     * Sets callback to encoder.
     *
     * @param encoderCallback the encoder callback
     * @param executor the callback executor
     */
    @Override
    public void setEncoderCallback(
            @NonNull EncoderCallback encoderCallback,
            @NonNull Executor executor) {
        synchronized (mLock) {
            mEncoderCallback = encoderCallback;
            mEncoderCallbackExecutor = executor;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void requestKeyFrame() {
        mEncoderExecutor.execute(() -> {
            switch (mState) {
                case STARTED:
                    requestKeyFrameToMediaCodec();
                    break;
                case CONFIGURED:
                case PAUSED:
                case ERROR:
                case STOPPING:
                case PENDING_START:
                case PENDING_START_PAUSED:
                    // No-op
                    break;
                case RELEASED:
                case PENDING_RELEASE:
                    throw new IllegalStateException("Encoder is released");
            }
        });
    }

    @ExecutedBy("mEncoderExecutor")
    private void setState(InternalState state) {
        if (mState == state) {
            return;
        }
        Logger.d(mTag, "Transitioning encoder internal state: " + mState + " --> " + state);
        mState = state;
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void setMediaCodecPaused(boolean paused) {
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, paused ? 1 : 0);
        mMediaCodec.setParameters(bundle);
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void requestKeyFrameToMediaCodec() {
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
        mMediaCodec.setParameters(bundle);
    }

    @ExecutedBy("mEncoderExecutor")
    private void addSignalEosTimeoutIfNeeded() {
        if (DeviceQuirks.get(SignalEosOutputBufferNotComeQuirk.class) != null) {
            MediaCodecCallback codecCallback = mMediaCodecCallback;
            Executor executor = mEncoderExecutor;
            if (mSignalEosTimeoutFuture != null) {
                mSignalEosTimeoutFuture.cancel(false);
            }
            mSignalEosTimeoutFuture = mainThreadExecutor().schedule(
                    () -> executor.execute(codecCallback::reachEndData),
                    SIGNAL_EOS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    @ExecutedBy("mEncoderExecutor")
    private void signalEndOfInputStream() {
        Futures.addCallback(acquireInputBuffer(),
                new FutureCallback<InputBuffer>() {
                    @Override
                    public void onSuccess(InputBuffer inputBuffer) {
                        inputBuffer.setPresentationTimeUs(generatePresentationTimeUs());
                        inputBuffer.setEndOfStream(true);
                        inputBuffer.submit();

                        Futures.addCallback(inputBuffer.getTerminationFuture(),
                                new FutureCallback<Void>() {
                                    @ExecutedBy("mEncoderExecutor")
                                    @Override
                                    public void onSuccess(@Nullable Void result) {
                                        // Do nothing.
                                    }

                                    @ExecutedBy("mEncoderExecutor")
                                    @Override
                                    public void onFailure(@NonNull Throwable t) {
                                        if (t instanceof MediaCodec.CodecException) {
                                            handleEncodeError(
                                                    (MediaCodec.CodecException) t);
                                        } else {
                                            handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                                    t.getMessage(), t);
                                        }
                                    }
                                }, mEncoderExecutor);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                "Unable to acquire InputBuffer.", t);
                    }
                }, mEncoderExecutor);
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void handleEncodeError(@NonNull MediaCodec.CodecException e) {
        handleEncodeError(EncodeException.ERROR_CODEC, e.getMessage(), e);
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void handleEncodeError(@EncodeException.ErrorType int error, @Nullable String message,
            @Nullable Throwable throwable) {
        switch (mState) {
            case CONFIGURED:
                // Unable to start MediaCodec. This is a fatal error. Try to reset the encoder.
                notifyError(error, message, throwable);
                reset();
                break;
            case STARTED:
            case PAUSED:
            case STOPPING:
            case PENDING_START_PAUSED:
            case PENDING_START:
            case PENDING_RELEASE:
                setState(ERROR);
                stopMediaCodec(() -> notifyError(error, message, throwable));
                break;
            case ERROR:
                //noinspection ConstantConditions
                Logger.w(mTag, "Get more than one error: " + message + "(" + error + ")",
                        throwable);
                break;
            case RELEASED:
                // Do nothing
                break;
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    void notifyError(@EncodeException.ErrorType int error, @Nullable String message,
            @Nullable Throwable throwable) {
        EncoderCallback callback;
        Executor executor;
        synchronized (mLock) {
            callback = mEncoderCallback;
            executor = mEncoderCallbackExecutor;
        }
        try {
            executor.execute(
                    () -> callback.onEncodeError(new EncodeException(error, message, throwable)));
        } catch (RejectedExecutionException e) {
            Logger.e(mTag, "Unable to post to the supplied executor.", e);
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void stopMediaCodec(@Nullable Runnable afterStop) {
        Logger.d(mTag, "stopMediaCodec");
        /*
         * MediaCodec#stop will free all its input/output ByteBuffers. Therefore, before calling
         * MediaCodec#stop, it must ensure all dispatched EncodedData(output ByteBuffers) and
         * InputBuffer(input ByteBuffers) are complete. Otherwise, the ByteBuffer receiver will
         * get buffer overflow when accessing the ByteBuffers.
         */
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EncodedDataImpl dataToClose : mEncodedDataSet) {
            futures.add(dataToClose.getClosedFuture());
        }
        for (InputBuffer inputBuffer : mInputBufferSet) {
            futures.add(inputBuffer.getTerminationFuture());
        }
        if (!futures.isEmpty()) {
            Logger.d(mTag, "Waiting for resources to return."
                    + " encoded data = " + mEncodedDataSet.size()
                    + ", input buffers = " + mInputBufferSet.size());
        }
        Futures.successfulAsList(futures).addListener(() -> {
            // If the encoder is not in ERROR state, stop the codec first before resetting.
            // Otherwise, reset directly.
            if (mState != ERROR) {
                if (!futures.isEmpty()) {
                    Logger.d(mTag, "encoded data and input buffers are returned");
                }
                if (mEncoderInput instanceof SurfaceInput && !mSourceStoppedSignalled
                        && !hasStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()
                ) {
                    // For a SurfaceInput, the codec is in control of de-queuing buffers from the
                    // underlying BufferQueue. If we stop the codec, then it will stop de-queuing
                    // buffers and the BufferQueue may run out of input buffers, causing the camera
                    // pipeline to stall. Instead of stopping, we will flush the codec. Since the
                    // codec is operating in asynchronous mode, this will cause the codec to
                    // continue to discard buffers. We should have already received the
                    // end-of-stream signal on an output buffer at this point, so those buffers
                    // are not needed anyways. We will defer resetting the codec until just
                    // before starting the codec again.
                    mMediaCodec.flush();
                    mIsFlushedAfterEndOfStream = true;
                } else {
                    // Non-SurfaceInputs give us more control over input buffers. We can directly
                    // stop the codec instead of flushing.
                    // Additionally, if we already received a signal that the source is stopped,
                    // then there shouldn't be new buffers being produced, and we don't need to
                    // flush.
                    mMediaCodec.stop();
                }
            }
            if (afterStop != null) {
                afterStop.run();
            }
            handleStopped();
        }, mEncoderExecutor);
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void handleStopped() {
        if (mState == PENDING_RELEASE) {
            releaseInternal();
        } else {
            InternalState oldState = mState;
            if (!mIsFlushedAfterEndOfStream) {
                // Only reset if the codec is stopped (not flushed). If the codec is flushed, we
                // want it to continue to discard buffers. We will reset before starting the
                // codec again.
                reset();
            }
            setState(CONFIGURED);
            if (oldState == PENDING_START || oldState == PENDING_START_PAUSED) {
                start();
                if (oldState == PENDING_START_PAUSED) {
                    pause();
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void updateTotalPausedDuration(long bufferPresentationTimeUs) {
        while (!mActivePauseResumeTimeRanges.isEmpty()) {
            Range<Long> pauseRange = mActivePauseResumeTimeRanges.getFirst();
            if (bufferPresentationTimeUs > pauseRange.getUpper()) {
                // Later than current pause, remove this pause and update total paused duration.
                mActivePauseResumeTimeRanges.removeFirst();
                mTotalPausedDurationUs += (pauseRange.getUpper() - pauseRange.getLower());
                Logger.d(mTag,
                        "Total paused duration = " + DebugUtils.readableUs(mTotalPausedDurationUs));
            } else {
                break;
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    long getAdjustedTimeUs(@NonNull BufferInfo bufferInfo) {
        long adjustedTimeUs;
        if (mTotalPausedDurationUs > 0L) {
            adjustedTimeUs = bufferInfo.presentationTimeUs - mTotalPausedDurationUs;
        } else {
            adjustedTimeUs = bufferInfo.presentationTimeUs;
        }
        return adjustedTimeUs;
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    boolean isInPauseRange(long timeUs) {
        for (Range<Long> range : mActivePauseResumeTimeRanges) {
            if (range.contains(timeUs)) {
                return true;
            } else if (timeUs < range.getLower()) {
                // Earlier than pause range.
                return false;
            }
            // Later than current pause, keep searching.
        }
        return false;
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    @NonNull
    ListenableFuture<InputBuffer> acquireInputBuffer() {
        switch (mState) {
            case CONFIGURED:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is not started yet."));
            case STARTED:
            case PAUSED:
            case STOPPING:
            case PENDING_START:
            case PENDING_START_PAUSED:
            case PENDING_RELEASE:
                AtomicReference<Completer<InputBuffer>> ref = new AtomicReference<>();
                ListenableFuture<InputBuffer> future = CallbackToFutureAdapter.getFuture(
                        completer -> {
                            ref.set(completer);
                            return "acquireInputBuffer";
                        });
                Completer<InputBuffer> completer = Preconditions.checkNotNull(ref.get());
                mAcquisitionQueue.offer(completer);
                completer.addCancellationListener(() -> mAcquisitionQueue.remove(completer),
                        mEncoderExecutor);
                matchAcquisitionsAndFreeBufferIndexes();
                return future;
            case ERROR:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is in error state."));
            case RELEASED:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "Encoder is released."));
            default:
                throw new IllegalStateException("Unknown state: " + mState);
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @ExecutedBy("mEncoderExecutor")
    void matchAcquisitionsAndFreeBufferIndexes() {
        while (!mAcquisitionQueue.isEmpty() && !mFreeInputBufferIndexQueue.isEmpty()) {
            Completer<InputBuffer> completer = requireNonNull(mAcquisitionQueue.poll());
            int bufferIndex = requireNonNull(mFreeInputBufferIndexQueue.poll());

            InputBufferImpl inputBuffer;
            try {
                inputBuffer = new InputBufferImpl(mMediaCodec, bufferIndex);
            } catch (MediaCodec.CodecException e) {
                handleEncodeError(e);
                return;
            }
            if (completer.set(inputBuffer)) {
                mInputBufferSet.add(inputBuffer);
                inputBuffer.getTerminationFuture().addListener(
                        () -> mInputBufferSet.remove(inputBuffer), mEncoderExecutor);
            } else {
                inputBuffer.cancel();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    long generatePresentationTimeUs() {
        return mTimeProvider.uptimeUs();
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    static boolean isKeyFrame(@NonNull BufferInfo bufferInfo) {
        return (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    static boolean hasEndOfStreamFlag(@NonNull BufferInfo bufferInfo) {
        return (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    private boolean hasStopCodecAfterSurfaceRemovalCrashMediaServerQuirk() {
        return DeviceQuirks.get(StopCodecAfterSurfaceRemovalCrashMediaServerQuirk.class) != null;
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    class MediaCodecCallback extends MediaCodec.Callback {
        @Nullable
        private final VideoTimebaseConverter mVideoTimestampConverter;
        // See b/255209101. On some devices, MediaCodec#signalEndOfInputStream() doesn't trigger
        // an end-of-stream buffer. This flag is used for a general workaround to take an output
        // buffer which reaches stop timestamp as an end-of-stream buffer. It should be true by
        // default to enable the workaround.
        private boolean mReachStopTimeAsEos = true;

        private boolean mHasSendStartCallback = false;
        private boolean mHasFirstData = false;
        private boolean mHasEndData = false;
        /** The last presentation time of BufferInfo without modified. */
        private long mLastPresentationTimeUs = 0L;
        /**
         * The last sent presentation time of BufferInfo. The value could be adjusted by total
         * pause duration.
         */
        private long mLastSentAdjustedTimeUs = 0L;
        private boolean mIsOutputBufferInPauseState = false;
        private boolean mIsKeyFrameRequired = false;
        private boolean mStopped = false;

        MediaCodecCallback() {
            if (mIsVideoEncoder) {
                mVideoTimestampConverter = new VideoTimebaseConverter(mTimeProvider,
                        mInputTimebase, DeviceQuirks.get(CameraUseInconsistentTimebaseQuirk.class));
            } else {
                mVideoTimestampConverter = null;
            }

            CodecStuckOnFlushQuirk codecStuckOnFlushQuirk = DeviceQuirks.get(
                    CodecStuckOnFlushQuirk.class);
            if (codecStuckOnFlushQuirk != null && codecStuckOnFlushQuirk.isProblematicMimeType(
                    mMediaFormat.getString(MediaFormat.KEY_MIME))) {
                mReachStopTimeAsEos = false;
            }
        }

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int index) {
            mEncoderExecutor.execute(() -> {
                if (mStopped) {
                    Logger.w(mTag, "Receives input frame after codec is reset.");
                    return;
                }
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        mFreeInputBufferIndexQueue.offer(index);
                        matchAcquisitionsAndFreeBufferIndexes();
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index,
                @NonNull BufferInfo bufferInfo) {
            mEncoderExecutor.execute(() -> {
                if (mStopped) {
                    Logger.w(mTag, "Receives frame after codec is reset.");
                    return;
                }
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        final EncoderCallback encoderCallback;
                        final Executor executor;
                        synchronized (mLock) {
                            encoderCallback = mEncoderCallback;
                            executor = mEncoderCallbackExecutor;
                        }

                        if (DEBUG) {
                            Logger.d(mTag, DebugUtils.readableBufferInfo(bufferInfo));
                        }

                        // Handle start of stream
                        if (!mHasSendStartCallback) {
                            mHasSendStartCallback = true;
                            try {
                                executor.execute(encoderCallback::onEncodeStart);
                            } catch (RejectedExecutionException e) {
                                Logger.e(mTag, "Unable to post to the supplied executor.", e);
                            }
                        }

                        if (checkBufferInfo(bufferInfo)) {
                            if (!mHasFirstData) {
                                mHasFirstData = true;
                                // Only print the first data to avoid flooding the log.
                                Logger.d(mTag,
                                        "data timestampUs = " + bufferInfo.presentationTimeUs
                                                + ", data timebase = " + mInputTimebase
                                                + ", current system uptimeMs = "
                                                + SystemClock.uptimeMillis()
                                                + ", current system realtimeMs = "
                                                + SystemClock.elapsedRealtime()
                                );
                            }
                            BufferInfo outBufferInfo = resolveOutputBufferInfo(bufferInfo);
                            mLastSentAdjustedTimeUs = outBufferInfo.presentationTimeUs;
                            try {
                                EncodedDataImpl encodedData = new EncodedDataImpl(mediaCodec, index,
                                        outBufferInfo);
                                sendEncodedData(encodedData, encoderCallback, executor);
                            } catch (MediaCodec.CodecException e) {
                                handleEncodeError(e);
                                return;
                            }
                        } else {
                            try {
                                mMediaCodec.releaseOutputBuffer(index, false);
                            } catch (MediaCodec.CodecException e) {
                                handleEncodeError(e);
                                return;
                            }
                        }

                        // Handle end of stream
                        if (!mHasEndData && isEndOfStream(bufferInfo)) {
                            reachEndData();
                        }
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @ExecutedBy("mEncoderExecutor")
        void reachEndData() {
            if (mHasEndData) {
                return;
            }
            mHasEndData = true;
            if (mSignalEosTimeoutFuture != null) {
                mSignalEosTimeoutFuture.cancel(false);
                mSignalEosTimeoutFuture = null;
            }
            EncoderCallback encoderCallback;
            Executor executor;
            synchronized (mLock) {
                encoderCallback = mEncoderCallback;
                executor = mEncoderCallbackExecutor;
            }
            stopMediaCodec(() -> {
                if (mState == ERROR) {
                    // Error occur during stopping.
                    return;
                }
                try {
                    executor.execute(encoderCallback::onEncodeStop);
                } catch (RejectedExecutionException e) {
                    Logger.e(mTag, "Unable to post to the supplied executor.", e);
                }
            });
        }

        @ExecutedBy("mEncoderExecutor")
        @NonNull
        private BufferInfo resolveOutputBufferInfo(@NonNull BufferInfo bufferInfo) {
            long adjustedTimeUs = getAdjustedTimeUs(bufferInfo);
            if (bufferInfo.presentationTimeUs == adjustedTimeUs) {
                return bufferInfo;
            }

            // If adjusted time <= last sent time, the buffer should have been detected and
            // dropped in checkBufferInfo().
            checkState(adjustedTimeUs > mLastSentAdjustedTimeUs);
            if (DEBUG) {
                Logger.d(mTag, "Adjust bufferInfo.presentationTimeUs to "
                        + DebugUtils.readableUs(adjustedTimeUs));
            }
            BufferInfo newBufferInfo = new BufferInfo();
            newBufferInfo.set(bufferInfo.offset, bufferInfo.size, adjustedTimeUs, bufferInfo.flags);
            return newBufferInfo;
        }

        @ExecutedBy("mEncoderExecutor")
        private void sendEncodedData(@NonNull EncodedDataImpl encodedData,
                @NonNull EncoderCallback callback, @NonNull Executor executor) {
            mEncodedDataSet.add(encodedData);
            Futures.addCallback(encodedData.getClosedFuture(),
                    new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            mEncodedDataSet.remove(encodedData);
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            mEncodedDataSet.remove(encodedData);
                            if (t instanceof MediaCodec.CodecException) {
                                handleEncodeError(
                                        (MediaCodec.CodecException) t);
                            } else {
                                handleEncodeError(EncodeException.ERROR_UNKNOWN,
                                        t.getMessage(), t);
                            }
                        }
                    }, mEncoderExecutor);
            try {
                executor.execute(() -> callback.onEncodedData(encodedData));
            } catch (RejectedExecutionException e) {
                Logger.e(mTag, "Unable to post to the supplied executor.", e);
                encodedData.close();
            }
        }

        /**
         * Checks the {@link BufferInfo} and updates related states.
         *
         * @return {@code true} if the buffer is valid, otherwise {@code false}.
         */
        @ExecutedBy("mEncoderExecutor")
        private boolean checkBufferInfo(@NonNull BufferInfo bufferInfo) {
            if (mHasEndData) {
                Logger.d(mTag, "Drop buffer by already reach end of stream.");
                return false;
            }

            if (bufferInfo.size <= 0) {
                Logger.d(mTag, "Drop buffer by invalid buffer size.");
                return false;
            }

            // Sometimes the codec config data was notified by output callback, they should have
            // been sent out by onOutputFormatChanged(), so ignore it.
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                Logger.d(mTag, "Drop buffer by codec config.");
                return false;
            }

            if (mVideoTimestampConverter != null) {
                bufferInfo.presentationTimeUs =
                        mVideoTimestampConverter.convertToUptimeUs(bufferInfo.presentationTimeUs);
            }

            // MediaCodec may send out of order buffer
            if (bufferInfo.presentationTimeUs <= mLastPresentationTimeUs) {
                Logger.d(mTag, "Drop buffer by out of order buffer from MediaCodec.");
                return false;
            }
            mLastPresentationTimeUs = bufferInfo.presentationTimeUs;

            // Ignore buffers are not in start/stop range. One situation is to ignore outdated
            // frames when using the Surface of MediaCodec#createPersistentInputSurface. After
            // the persistent Surface stops, it will keep a small number of old frames in its
            // buffer, and send those old frames in the next startup.
            if (!mStartStopTimeRangeUs.contains(bufferInfo.presentationTimeUs)) {
                Logger.d(mTag, "Drop buffer by not in start-stop range.");
                // If data hasn't reached the expected stop timestamp, set the stop timestamp.
                if (mPendingCodecStop
                        && bufferInfo.presentationTimeUs >= mStartStopTimeRangeUs.getUpper()) {
                    if (mStopTimeoutFuture != null) {
                        mStopTimeoutFuture.cancel(true);
                    }
                    mLastDataStopTimestamp = bufferInfo.presentationTimeUs;
                    signalCodecStop();
                    mPendingCodecStop = false;
                }
                return false;
            }

            if (updatePauseRangeStateAndCheckIfBufferPaused(bufferInfo)) {
                Logger.d(mTag, "Drop buffer by pause.");
                return false;
            }

            // We should check if the adjusted time is valid. see b/189114207.
            if (getAdjustedTimeUs(bufferInfo) <= mLastSentAdjustedTimeUs) {
                Logger.d(mTag, "Drop buffer by adjusted time is less than the last sent time.");
                if (mIsVideoEncoder && isKeyFrame(bufferInfo)) {
                    mIsKeyFrameRequired = true;
                }
                return false;
            }

            if (!mHasFirstData && !mIsKeyFrameRequired && mIsVideoEncoder) {
                mIsKeyFrameRequired = true;
            }

            if (mIsKeyFrameRequired) {
                if (!isKeyFrame(bufferInfo)) {
                    Logger.d(mTag, "Drop buffer by not a key frame.");
                    requestKeyFrameToMediaCodec();
                    return false;
                }
                mIsKeyFrameRequired = false;
            }

            return true;
        }

        @ExecutedBy("mEncoderExecutor")
        private boolean isEndOfStream(@NonNull BufferInfo bufferInfo) {
            return hasEndOfStreamFlag(bufferInfo)
                    || (mReachStopTimeAsEos && isEosSignalledAndStopTimeReached(bufferInfo));
        }

        @ExecutedBy("mEncoderExecutor")
        private boolean isEosSignalledAndStopTimeReached(@NonNull BufferInfo bufferInfo) {
            return mMediaCodecEosSignalled
                    && bufferInfo.presentationTimeUs > mStartStopTimeRangeUs.getUpper();
        }

        @SuppressWarnings("StatementWithEmptyBody") // to better organize the logic and comments
        @ExecutedBy("mEncoderExecutor")
        private boolean updatePauseRangeStateAndCheckIfBufferPaused(
                @NonNull BufferInfo bufferInfo) {
            updateTotalPausedDuration(bufferInfo.presentationTimeUs);
            boolean isInPauseRange = isInPauseRange(bufferInfo.presentationTimeUs);
            if (!mIsOutputBufferInPauseState && isInPauseRange) {
                Logger.d(mTag, "Switch to pause state");
                // From resume to pause
                mIsOutputBufferInPauseState = true;

                // Invoke paused callback
                Executor executor;
                EncoderCallback encoderCallback;
                synchronized (mLock) {
                    executor = mEncoderCallbackExecutor;
                    encoderCallback = mEncoderCallback;
                }
                executor.execute(encoderCallback::onEncodePaused);

                // We must ensure that the current state is PAUSED before we stop the input
                // source and pause the codec. This is because start() may be called before the
                // output buffer reaches the pause range.
                if (mState == PAUSED) {
                    if (!mIsVideoEncoder && DeviceQuirks.get(
                            AudioEncoderIgnoresInputTimestampQuirk.class) != null) {
                        // Do nothing, which means keep handling audio data in the codec.
                    } else if (mIsVideoEncoder && DeviceQuirks.get(
                            VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk.class) != null) {
                        // Do nothing, which means don't pause the codec.
                    } else {
                        if (mEncoderInput instanceof ByteBufferInput) {
                            ((ByteBufferInput) mEncoderInput).setActive(false);
                        }
                        setMediaCodecPaused(true);
                    }
                }

                // An encoding session could be pause/resume for multiple times. So a later pause
                // should overwrite the previous data stop time.
                mLastDataStopTimestamp = bufferInfo.presentationTimeUs;
                // If the encoder has been stopped before the data enters pause period, stop the
                // codec directly.
                if (mPendingCodecStop) {
                    if (mStopTimeoutFuture != null) {
                        mStopTimeoutFuture.cancel(true);
                    }
                    signalCodecStop();
                    mPendingCodecStop = false;
                }
            } else if (mIsOutputBufferInPauseState && !isInPauseRange) {
                // From pause to resume
                Logger.d(mTag, "Switch to resume state");
                mIsOutputBufferInPauseState = false;
                if (mIsVideoEncoder && !isKeyFrame(bufferInfo)) {
                    mIsKeyFrameRequired = true;
                }
            }

            return mIsOutputBufferInPauseState;
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            mEncoderExecutor.execute(() -> {
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        handleEncodeError(e);
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                @NonNull MediaFormat mediaFormat) {
            mEncoderExecutor.execute(() -> {
                if (mStopped) {
                    Logger.w(mTag, "Receives onOutputFormatChanged after codec is reset.");
                    return;
                }
                switch (mState) {
                    case STARTED:
                    case PAUSED:
                    case STOPPING:
                    case PENDING_START:
                    case PENDING_START_PAUSED:
                    case PENDING_RELEASE:
                        EncoderCallback encoderCallback;
                        Executor executor;
                        synchronized (mLock) {
                            encoderCallback = mEncoderCallback;
                            executor = mEncoderCallbackExecutor;
                        }
                        try {
                            executor.execute(
                                    () -> encoderCallback.onOutputConfigUpdate(() -> mediaFormat));
                        } catch (RejectedExecutionException e) {
                            Logger.e(mTag, "Unable to post to the supplied executor.", e);
                        }
                        break;
                    case CONFIGURED:
                    case ERROR:
                    case RELEASED:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalStateException("Unknown state: " + mState);
                }
            });
        }

        /** Stop process further frame output. */
        @ExecutedBy("mEncoderExecutor")
        void stop() {
            mStopped = true;
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    class SurfaceInput implements Encoder.SurfaceInput {

        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private Surface mSurface;

        @GuardedBy("mLock")
        private final Set<Surface> mObsoleteSurfaces = new HashSet<>();

        @GuardedBy("mLock")
        private OnSurfaceUpdateListener mSurfaceUpdateListener;

        @GuardedBy("mLock")
        private Executor mSurfaceUpdateExecutor;

        /**
         * Sets the surface update listener.
         *
         * @param executor the executor to invoke the listener
         * @param listener the surface update listener
         */
        @Override
        public void setOnSurfaceUpdateListener(@NonNull Executor executor,
                @NonNull OnSurfaceUpdateListener listener) {
            Surface surface;
            synchronized (mLock) {
                mSurfaceUpdateListener = Preconditions.checkNotNull(listener);
                mSurfaceUpdateExecutor = Preconditions.checkNotNull(executor);
                surface = mSurface;
            }
            if (surface != null) {
                notifySurfaceUpdate(executor, listener, surface);
            }
        }

        @SuppressLint("NewApi")
        void resetSurface() {
            Surface surface;
            Executor executor;
            OnSurfaceUpdateListener listener;
            EncoderNotUsePersistentInputSurfaceQuirk quirk = DeviceQuirks.get(
                    EncoderNotUsePersistentInputSurfaceQuirk.class);
            synchronized (mLock) {
                if (quirk == null) {
                    if (mSurface == null) {
                        mSurface = Api23Impl.createPersistentInputSurface();
                        surface = mSurface;
                    } else {
                        surface = null;
                    }
                    Api23Impl.setInputSurface(mMediaCodec, mSurface);
                } else {
                    if (mSurface != null) {
                        mObsoleteSurfaces.add(mSurface);
                    }
                    mSurface = mMediaCodec.createInputSurface();
                    surface = mSurface;
                }
                listener = mSurfaceUpdateListener;
                executor = mSurfaceUpdateExecutor;
            }
            if (surface != null && listener != null && executor != null) {
                notifySurfaceUpdate(executor, listener, surface);
            }
        }

        void releaseSurface() {
            Surface surface;
            Set<Surface> obsoleteSurfaces;
            synchronized (mLock) {
                surface = mSurface;
                mSurface = null;
                obsoleteSurfaces = new HashSet<>(mObsoleteSurfaces);
                mObsoleteSurfaces.clear();
            }
            if (surface != null) {
                surface.release();
            }
            for (Surface obsoleteSurface : obsoleteSurfaces) {
                obsoleteSurface.release();
            }
        }

        private void notifySurfaceUpdate(@NonNull Executor executor,
                @NonNull OnSurfaceUpdateListener listener, @NonNull Surface surface) {
            try {
                executor.execute(() -> listener.onSurfaceUpdate(surface));
            } catch (RejectedExecutionException e) {
                Logger.e(mTag, "Unable to post to the supplied executor.", e);
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // synthetic accessor
    class ByteBufferInput implements Encoder.ByteBufferInput {

        private final Map<Observer<? super State>, Executor> mStateObservers =
                new LinkedHashMap<>();

        private State mBufferProviderState = State.INACTIVE;

        private final List<ListenableFuture<InputBuffer>> mAcquisitionList = new ArrayList<>();

        /** {@inheritDoc} */
        @NonNull
        @Override
        public ListenableFuture<State> fetchData() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                mEncoderExecutor.execute(() -> completer.set(mBufferProviderState));
                return "fetchData";
            });
        }

        /** {@inheritDoc} */
        @NonNull
        @Override
        public ListenableFuture<InputBuffer> acquireBuffer() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                mEncoderExecutor.execute(() -> {
                    if (mBufferProviderState == State.ACTIVE) {
                        ListenableFuture<InputBuffer> future = acquireInputBuffer();
                        Futures.propagate(future, completer);
                        // Cancel by outer, also cancel internal future.
                        completer.addCancellationListener(() -> cancelInputBuffer(future),
                                CameraXExecutors.directExecutor());

                        // Keep tracking the acquisition by internal future. Once the provider state
                        // transition to inactive, cancel the internal future can also send signal
                        // to outer future since we propagate the internal result to the completer.
                        mAcquisitionList.add(future);
                        future.addListener(() -> mAcquisitionList.remove(future), mEncoderExecutor);
                    } else if (mBufferProviderState == State.INACTIVE) {
                        completer.setException(
                                new IllegalStateException("BufferProvider is not active."));
                    } else {
                        completer.setException(
                                new IllegalStateException(
                                        "Unknown state: " + mBufferProviderState));
                    }
                });
                return "acquireBuffer";
            });
        }

        private void cancelInputBuffer(@NonNull ListenableFuture<InputBuffer> inputBufferFuture) {
            if (!inputBufferFuture.cancel(true)) {
                // Not able to cancel the future, need to cancel the input buffer as possible.
                checkState(inputBufferFuture.isDone());
                try {
                    inputBufferFuture.get().cancel();
                } catch (ExecutionException | InterruptedException | CancellationException e) {
                    Logger.w(mTag, "Unable to cancel the input buffer: " + e);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addObserver(@NonNull Executor executor,
                @NonNull Observer<? super State> observer) {
            mEncoderExecutor.execute(() -> {
                mStateObservers.put(Preconditions.checkNotNull(observer),
                        Preconditions.checkNotNull(executor));
                final State state = mBufferProviderState;
                executor.execute(() -> observer.onNewData(state));
            });
        }

        /** {@inheritDoc} */
        @Override
        public void removeObserver(@NonNull Observer<? super State> observer) {
            mEncoderExecutor.execute(
                    () -> mStateObservers.remove(Preconditions.checkNotNull(observer)));
        }

        @ExecutedBy("mEncoderExecutor")
        void setActive(boolean isActive) {
            final State newState = isActive ? State.ACTIVE : State.INACTIVE;
            if (mBufferProviderState == newState) {
                return;
            }
            mBufferProviderState = newState;

            if (newState == State.INACTIVE) {
                for (ListenableFuture<InputBuffer> future : mAcquisitionList) {
                    future.cancel(true);
                }
                mAcquisitionList.clear();
            }

            for (Map.Entry<Observer<? super State>, Executor> entry : mStateObservers.entrySet()) {
                try {
                    entry.getValue().execute(() -> entry.getKey().onNewData(newState));
                } catch (RejectedExecutionException e) {
                    Logger.e(mTag, "Unable to post to the supplied executor.", e);
                }
            }
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(23)
    private static class Api23Impl {

        private Api23Impl() {
        }

        @DoNotInline
        @NonNull
        static Surface createPersistentInputSurface() {
            return MediaCodec.createPersistentInputSurface();
        }

        @DoNotInline
        static void setInputSurface(@NonNull MediaCodec mediaCodec, @NonNull Surface surface) {
            mediaCodec.setInputSurface(surface);
        }
    }
}
