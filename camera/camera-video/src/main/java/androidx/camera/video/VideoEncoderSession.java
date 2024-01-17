/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.camera.video.internal.config.VideoConfigUtil.resolveVideoEncoderConfig;
import static androidx.camera.video.internal.config.VideoConfigUtil.resolveVideoMimeInfo;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.config.VideoMimeInfo;
import androidx.camera.video.internal.encoder.Encoder;
import androidx.camera.video.internal.encoder.Encoder.SurfaceInput.OnSurfaceUpdateListener;
import androidx.camera.video.internal.encoder.EncoderFactory;
import androidx.camera.video.internal.encoder.InvalidConfigException;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A session managing the video encoder from configuration to termination.
 *
 * <ul>
 *  <li>The session configures the VideoEncoder for a SurfaceRequest.</li>
 *  <li>The session can only be configured once, cannot be reused for another SurfaceRequest.</li>
 * </ul>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class VideoEncoderSession {

    private static final String TAG = "VideoEncoderSession";
    private final Executor mExecutor;
    private final Executor mSequentialExecutor;
    private final EncoderFactory mVideoEncoderFactory;

    private enum VideoEncoderState {
        /**
         * VideoEncoder is not configured.
         */
        NOT_INITIALIZED,
        /**
         * Creating the VideoEncoder.
         */
        INITIALIZING,
        /**
         * Wait for release, it will not provide Surface anymore.
         */
        PENDING_RELEASE,
        /**
         * Surface is provided to the SurfaceRequest.
         */
        READY,
        /**
         * The VideoEncoderSession is terminated, please never reuse it.
         */
        RELEASED
    }

    private Encoder mVideoEncoder = null;
    private Surface mActiveSurface = null;
    private SurfaceRequest mSurfaceRequest = null;
    private Executor mOnSurfaceUpdateExecutor = null;
    private OnSurfaceUpdateListener mOnSurfaceUpdateListener = null;
    private VideoEncoderState mVideoEncoderState = VideoEncoderState.NOT_INITIALIZED;
    private ListenableFuture<Void> mReleasedFuture = Futures.immediateFailedFuture(
            new IllegalStateException("Cannot close the encoder before configuring."));
    private CallbackToFutureAdapter.Completer<Void> mReleasedCompleter = null;
    private ListenableFuture<Encoder> mReadyToReleaseFuture = Futures.immediateFailedFuture(
            new IllegalStateException("Cannot close the encoder before configuring."));
    private CallbackToFutureAdapter.Completer<Encoder> mReadyToReleaseCompleter = null;

    VideoEncoderSession(@NonNull EncoderFactory videoEncoderFactory,
            @NonNull Executor sequentialExecutor, @NonNull Executor executor) {
        mExecutor = executor;
        mSequentialExecutor = sequentialExecutor;
        mVideoEncoderFactory = videoEncoderFactory;
    }

    @NonNull
    @ExecutedBy("mSequentialExecutor")
    ListenableFuture<Encoder> configure(@NonNull SurfaceRequest surfaceRequest,
            @NonNull Timebase timebase, @NonNull MediaSpec mediaSpec,
            @Nullable VideoValidatedEncoderProfilesProxy resolvedEncoderProfiles) {
        switch (mVideoEncoderState) {
            case NOT_INITIALIZED:
                mVideoEncoderState = VideoEncoderState.INITIALIZING;

                mSurfaceRequest = surfaceRequest;
                Logger.d(TAG, "Create VideoEncoderSession: " + this);
                mReleasedFuture = CallbackToFutureAdapter.getFuture(closeCompleter -> {
                    mReleasedCompleter = closeCompleter;
                    return "ReleasedFuture " + VideoEncoderSession.this;
                });
                mReadyToReleaseFuture = CallbackToFutureAdapter.getFuture(
                        requestCompleteCompleter -> {
                            mReadyToReleaseCompleter = requestCompleteCompleter;
                            return "ReadyToReleaseFuture " + VideoEncoderSession.this;
                        });
                ListenableFuture<Encoder> configureFuture = CallbackToFutureAdapter.getFuture(
                        completer -> {
                            configureVideoEncoderInternal(surfaceRequest, timebase,
                                    resolvedEncoderProfiles,
                                    mediaSpec, completer);
                            return "ConfigureVideoEncoderFuture " + VideoEncoderSession.this;
                        });
                Futures.addCallback(configureFuture, new FutureCallback<Encoder>() {
                    @Override
                    public void onSuccess(@Nullable Encoder result) {
                        // Nothing to do.
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Logger.w(TAG, "VideoEncoder configuration failed.", t);
                        terminateNow();
                    }
                }, mSequentialExecutor);

                return Futures.nonCancellationPropagating(configureFuture);
            case INITIALIZING:
                // Fall-through
            case READY:
                // Fall-through
            case PENDING_RELEASE:
                // Fall-through
            case RELEASED:
                // Fall-through
            default:
                return Futures.immediateFailedFuture(new IllegalStateException(
                        "configure() shouldn't be called in " + mVideoEncoderState));
        }
    }

    @ExecutedBy("mSequentialExecutor")
    boolean isConfiguredSurfaceRequest(@NonNull SurfaceRequest surfaceRequest) {
        switch (mVideoEncoderState) {
            case INITIALIZING:
                // Fall-through
            case READY:
                return mSurfaceRequest == surfaceRequest;
            case PENDING_RELEASE:
                // Fall-through
            case NOT_INITIALIZED:
                // Fall-through
            case RELEASED:
                return false;
            default:
                throw new IllegalStateException("State " + mVideoEncoderState + " is not handled");
        }
    }

    /**
     * Return a ListenableFuture will be completed when the VideoEncoder can safely be released.
     */
    @NonNull
    @ExecutedBy("mSequentialExecutor")
    ListenableFuture<Encoder> getReadyToReleaseFuture() {
        return Futures.nonCancellationPropagating(mReadyToReleaseFuture);
    }

    /**
     * Signal the VideoEncoder is going to the pending release state, it will not provide
     * output Surface anymore.
     *
     * (1) The VideoEncoder will be released immediately if it hasn't provided the Surface to
     * the SurfaceRequest.
     * (2) If the Surface is already provided to the SurfaceRequest, it must needs to call
     * terminateNow() to release the VideoEncoder.
     *
     * @return a ListenableFuture will be completed when the VideoEncoder is released.
     */
    @NonNull
    @ExecutedBy("mSequentialExecutor")
    ListenableFuture<Void> signalTermination() {
        closeInternal();
        return Futures.nonCancellationPropagating(mReleasedFuture);
    }

    @ExecutedBy("mSequentialExecutor")
    void terminateNow() {
        switch (mVideoEncoderState) {
            case NOT_INITIALIZED:
                // Session is not configured, switch to RELEASED state directly.
                mVideoEncoderState = VideoEncoderState.RELEASED;
                return;
            case RELEASED:
                Logger.d(TAG, "terminateNow in " + mVideoEncoderState + ", No-op");
                return;
            case INITIALIZING:
                // Fall-through
            case PENDING_RELEASE:
                // Fall-through
            case READY:
                mVideoEncoderState = VideoEncoderState.RELEASED;
                mReadyToReleaseCompleter.set(mVideoEncoder);
                mSurfaceRequest = null;
                if (mVideoEncoder != null) {
                    Logger.d(TAG, "VideoEncoder is releasing: " + mVideoEncoder);
                    mVideoEncoder.release();
                    mVideoEncoder.getReleasedFuture().addListener(
                            () -> mReleasedCompleter.set(null), mSequentialExecutor);
                    mVideoEncoder = null;
                } else {
                    Logger.w(TAG, "There's no VideoEncoder to release! Finish release completer.");
                    mReleasedCompleter.set(null);
                }
                break;
            default:
                throw new IllegalStateException("State " + mVideoEncoderState + " is not handled");
        }
    }

    @Nullable
    @ExecutedBy("mSequentialExecutor")
    Surface getActiveSurface() {
        if (mVideoEncoderState != VideoEncoderState.READY) {
            return null;
        }
        return mActiveSurface;
    }

    @Nullable
    @ExecutedBy("mSequentialExecutor")
    Encoder getVideoEncoder() {
        return mVideoEncoder;
    }

    @ExecutedBy("mSequentialExecutor")
    private void closeInternal() {
        switch (mVideoEncoderState) {
            case NOT_INITIALIZED:
                // Fall-through
            case INITIALIZING:
                // VideoEncoder can be released directly.
                terminateNow();
                break;
            case PENDING_RELEASE:
                // Fall-through
            case READY:
                Logger.d(TAG, "closeInternal in " + mVideoEncoderState + " state");
                mVideoEncoderState = VideoEncoderState.PENDING_RELEASE;
                break;
            case RELEASED:
                Logger.d(TAG, "closeInternal in RELEASED state, No-op");
                break;
            default:
                throw new IllegalStateException("State " + mVideoEncoderState + " is not handled");
        }
    }

    @ExecutedBy("mSequentialExecutor")
    void setOnSurfaceUpdateListener(@NonNull Executor executor,
            @NonNull OnSurfaceUpdateListener onSurfaceUpdateListener) {
        mOnSurfaceUpdateExecutor = executor;
        mOnSurfaceUpdateListener = onSurfaceUpdateListener;
    }

    @ExecutedBy("mSequentialExecutor")
    private void configureVideoEncoderInternal(@NonNull SurfaceRequest surfaceRequest,
            @NonNull Timebase timebase,
            @Nullable VideoValidatedEncoderProfilesProxy resolvedEncoderProfiles,
            @NonNull MediaSpec mediaSpec,
            @NonNull CallbackToFutureAdapter.Completer<Encoder> configureCompleter) {
        DynamicRange dynamicRange = surfaceRequest.getDynamicRange();
        VideoMimeInfo videoMimeInfo = resolveVideoMimeInfo(mediaSpec, dynamicRange,
                resolvedEncoderProfiles);

        // The VideoSpec from mediaSpec only contains settings requested by the recorder, but
        // the actual settings may need to differ depending on the FPS chosen by the camera.
        // The expected frame rate from the camera is passed on here from the SurfaceRequest.
        VideoEncoderConfig config = resolveVideoEncoderConfig(
                videoMimeInfo,
                timebase,
                mediaSpec.getVideoSpec(),
                surfaceRequest.getResolution(),
                dynamicRange,
                surfaceRequest.getExpectedFrameRate());

        try {
            mVideoEncoder = mVideoEncoderFactory.createEncoder(mExecutor, config);
        } catch (InvalidConfigException e) {
            Logger.e(TAG, "Unable to initialize video encoder.", e);
            configureCompleter.setException(e);
            return;
        }

        Encoder.EncoderInput encoderInput = mVideoEncoder.getInput();
        if (!(encoderInput instanceof Encoder.SurfaceInput)) {
            configureCompleter.setException(
                    new AssertionError("The EncoderInput of video isn't a SurfaceInput."));
            return;
        }
        ((Encoder.SurfaceInput) encoderInput).setOnSurfaceUpdateListener(mSequentialExecutor,
                surface -> {
                    switch (mVideoEncoderState) {
                        case NOT_INITIALIZED:
                            // Fall-through
                        case PENDING_RELEASE:
                            // Fall-through
                        case RELEASED:
                            Logger.d(TAG, "Not provide surface in " + mVideoEncoderState);
                            configureCompleter.set(null);
                            break;
                        case INITIALIZING:
                            if (surfaceRequest.isServiced()) {
                                Logger.d(TAG, "Not provide surface, "
                                        + Objects.toString(surfaceRequest, "EMPTY")
                                        + " is already serviced.");
                                configureCompleter.set(null);
                                closeInternal();
                                break;
                            }

                            mActiveSurface = surface;
                            Logger.d(TAG, "provide surface: " + surface);
                            surfaceRequest.provideSurface(surface, mSequentialExecutor,
                                    this::onSurfaceRequestComplete);
                            mVideoEncoderState = VideoEncoderState.READY;
                            configureCompleter.set(mVideoEncoder);
                            break;
                        case READY:
                            if (mOnSurfaceUpdateListener != null
                                    && mOnSurfaceUpdateExecutor != null) {
                                mOnSurfaceUpdateExecutor.execute(
                                        () -> mOnSurfaceUpdateListener.onSurfaceUpdate(surface));
                            }
                            Logger.w(TAG, "Surface is updated in READY state: " + surface);
                            break;
                        default:
                            throw new IllegalStateException(
                                    "State " + mVideoEncoderState + " is not handled");
                    }
                });
    }

    @ExecutedBy("mSequentialExecutor")
    private void onSurfaceRequestComplete(@NonNull SurfaceRequest.Result result) {
        Logger.d(TAG, "Surface can be closed: " + result.getSurface().hashCode());
        Surface resultSurface = result.getSurface();
        if (resultSurface == mActiveSurface) {
            mActiveSurface = null;
            mReadyToReleaseCompleter.set(mVideoEncoder);
            closeInternal();
        } else {
            // If the surface isn't the active surface, it also can't be the latest surface
            resultSurface.release();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG + "@" + hashCode() + " for " + Objects.toString(mSurfaceRequest,
                "SURFACE_REQUEST_NOT_CONFIGURED");
    }
}
