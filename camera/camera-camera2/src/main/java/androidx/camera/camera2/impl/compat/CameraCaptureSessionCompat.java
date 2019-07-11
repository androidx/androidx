/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl.compat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Helper for accessing features in {@link CameraCaptureSession} in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class CameraCaptureSessionCompat {

    private static final CameraCaptureSessionCompatImpl IMPL = chooseImplementation();

    // Class is not a wrapper. Should not be instantiated.
    private CameraCaptureSessionCompat() {
    }

    private static CameraCaptureSessionCompatImpl chooseImplementation() {
        if (Build.VERSION.SDK_INT >= 28) {
            return new CameraCaptureSessionCompatApi28Impl();
        }

        return new CameraCaptureSessionCompatBaseImpl();
    }

    /**
     * Submit a list of requests to be captured in sequence as a burst. The
     * burst will be captured in the minimum amount of time possible, and will
     * not be interleaved with requests submitted by other capture or repeat
     * calls.
     *
     * <p>The behavior of this method matches that of
     * {@link
     * CameraCaptureSession#captureBurst(List, CameraCaptureSession.CaptureCallback, Handler)},
     * except that it uses {@link Executor} as an argument instead of {@link Handler}.
     *
     * @param requests the list of settings for this burst capture
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify each time one of the
     * requests in the burst has been processed.
     *
     * @return int A unique capture sequence ID used by
     *             {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     *
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     * @throws IllegalStateException if this session is no longer active, either because the session
     *                               was explicitly closed, a new session has been created
     *                               or the camera device has been closed.
     * @throws IllegalArgumentException If the requests target no Surfaces, or the requests target
     *                                  Surfaces not currently configured as outputs; or one of the
     *                                  requests targets a set of Surfaces that cannot be submitted
     *                                  simultaneously in a reprocessable capture session; or a
     *                                  reprocess capture request is submitted in a
     *                                  non-reprocessable capture session; or one of the reprocess
     *                                  capture requests was created with a
     *                                  {@link TotalCaptureResult} from a different session; or one
     *                                  of the captures targets a Surface in the middle of being
     *                                  prepared; or if the executor is null; or if
     *                                  the listener is null.
     *
     * @see CameraCaptureSession#capture
     * @see CameraCaptureSession#setRepeatingRequest
     * @see CameraCaptureSession#setRepeatingBurst
     * @see CameraCaptureSession#abortCaptures
     */
    public static int captureBurstRequests(
            @NonNull CameraCaptureSession captureSession,
            @NonNull List<CaptureRequest> requests,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        return IMPL.captureBurstRequests(captureSession, requests, executor, listener);
    }

    /**
     * Submit a request for an image to be captured by the camera device.
     *
     * <p>The behavior of this method matches that of
     * {@link
     * CameraCaptureSession#capture(CaptureRequest, CameraCaptureSession.CaptureCallback, Handler)},
     * except that it uses {@link Executor} as an argument instead of {@link Handler}.
     *
     * @param captureSession the {@link CameraCaptureSession} used to submit the request.
     * @param request        the settings for this capture
     * @param executor       the executor which will be used for invoking the listener.
     * @param listener       The callback object to notify once this request has been
     *                       processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException    if the camera device is no longer connected or has
     *                                  encountered a fatal error
     * @throws IllegalStateException    if this session is no longer active, either because the
     * session
     *                                  was explicitly closed, a new session has been created
     *                                  or the camera device has been closed.
     * @throws IllegalArgumentException if the request targets no Surfaces or Surfaces that are not
     *                                  configured as outputs for this session; or the request
     *                                  targets a set of Surfaces that cannot be submitted
     *                                  simultaneously in a reprocessable capture session; or a
     *                                  reprocess capture request is submitted in a
     *                                  non-reprocessable capture session; or the reprocess capture
     *                                  request was created with a {@link TotalCaptureResult} from
     *                                  a different session; or the capture targets a Surface in
     *                                  the middle of being prepared; or the
     *                                  executor is null, or the listener is not null.
     * @see CameraCaptureSession#captureBurst
     * @see CameraCaptureSession#setRepeatingRequest
     * @see CameraCaptureSession#setRepeatingBurst
     * @see CameraCaptureSession#abortCaptures
     * @see CameraDevice#createReprocessableCaptureSession
     */
    public static int captureSingleRequest(
            @NonNull CameraCaptureSession captureSession,
            @NonNull CaptureRequest request,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        return IMPL.captureSingleRequest(captureSession, request, executor, listener);
    }

    /**
     * <p>Request endlessly repeating capture of a sequence of images by this
     * capture session.</p>
     *
     * <p>The behavior of this method matches that of
     * {@link
     * CameraCaptureSession#setRepeatingBurst(List, CameraCaptureSession.CaptureCallback, Handler)},
     * except that it uses {@link java.util.concurrent.Executor} as an argument
     * instead of {@link android.os.Handler}.</p>
     *
     * @param requests the list of requests to cycle through indefinitely
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify each time one of the
     * requests in the repeating bursts has finished processing.
     *
     * @return int A unique capture sequence ID used by
     *             {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     *
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     * @throws IllegalStateException if this session is no longer active, either because the session
     *                               was explicitly closed, a new session has been created
     *                               or the camera device has been closed.
     * @throws IllegalArgumentException If the requests reference no Surfaces or reference Surfaces
     *                                  not currently configured as outputs; or one of the requests
     *                                  is a reprocess capture request; or one of the captures
     *                                  targets a Surface in the middle of being
     *                                  prepared; or the executor is null; or the
     *                                  listener is null.
     *
     * @see CameraCaptureSession#capture
     * @see CameraCaptureSession#captureBurst
     * @see CameraCaptureSession#setRepeatingRequest
     * @see CameraCaptureSession#stopRepeating
     * @see CameraCaptureSession#abortCaptures
     */
    public static int setRepeatingBurstRequests(
            @NonNull CameraCaptureSession captureSession,
            @NonNull List<CaptureRequest> requests,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        return IMPL.setRepeatingBurstRequests(captureSession, requests, executor, listener);
    }

    /**
     * Request endlessly repeating capture of images by this capture session.
     *
     * <p>The behavior of this method matches that of
     * {@link CameraCaptureSession#setRepeatingRequest(CaptureRequest,
     * CameraCaptureSession.CaptureCallback, Handler)},
     * except that it uses {@link Executor} as an argument instead of {@link Handler}.</p>
     *
     * @param request the request to repeat indefinitely
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify every time the
     * request finishes processing.
     *
     * @return int A unique capture sequence ID used by
     *             {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     *
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     * @throws IllegalStateException if this session is no longer active, either because the session
     *                               was explicitly closed, a new session has been created
     *                               or the camera device has been closed.
     * @throws IllegalArgumentException If the request references no Surfaces or references Surfaces
     *                                  that are not currently configured as outputs; or the request
     *                                  is a reprocess capture request; or the capture targets a
     *                                  Surface in the middle of being prepared; or
     *                                  the executor is null; or the listener is null.
     *
     * @see CameraCaptureSession#capture
     * @see CameraCaptureSession#captureBurst
     * @see CameraCaptureSession#setRepeatingBurst
     * @see CameraCaptureSession#stopRepeating
     * @see CameraCaptureSession#abortCaptures
     */
    public static int setSingleRepeatingRequest(
            @NonNull CameraCaptureSession captureSession,
            @NonNull CaptureRequest request,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        return IMPL.setSingleRepeatingRequest(captureSession, request, executor, listener);
    }

    interface CameraCaptureSessionCompatImpl {
        int captureBurstRequests(
                @NonNull CameraCaptureSession captureSession,
                @NonNull List<CaptureRequest> requests,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;

        int captureSingleRequest(
                @NonNull CameraCaptureSession captureSession,
                @NonNull CaptureRequest request,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;

        int setRepeatingBurstRequests(
                @NonNull CameraCaptureSession captureSession,
                @NonNull List<CaptureRequest> requests,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;

        int setSingleRepeatingRequest(
                @NonNull CameraCaptureSession captureSession,
                @NonNull CaptureRequest request,
                @NonNull /* @CallbackExecutor */ Executor executor,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;
    }

    static final class CaptureCallbackExecutorWrapper extends CameraCaptureSession.CaptureCallback {

        final CameraCaptureSession.CaptureCallback mWrappedCallback;
        private final Executor mExecutor;

        CaptureCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraCaptureSession.CaptureCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        @Override
        public void onCaptureStarted(@NonNull final CameraCaptureSession session,
                @NonNull final CaptureRequest request, final long timestamp,
                final long frameNumber) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            });
        }

        @Override
        public void onCaptureProgressed(@NonNull final CameraCaptureSession session,
                @NonNull final CaptureRequest request, @NonNull final CaptureResult partialResult) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureProgressed(session, request, partialResult);
                }
            });
        }

        @Override
        public void onCaptureCompleted(@NonNull final CameraCaptureSession session,
                @NonNull final CaptureRequest request, @NonNull final TotalCaptureResult result) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureCompleted(session, request, result);
                }
            });
        }

        @Override
        public void onCaptureFailed(@NonNull final CameraCaptureSession session,
                @NonNull final CaptureRequest request, @NonNull final CaptureFailure failure) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureFailed(session, request, failure);
                }
            });
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull final CameraCaptureSession session,
                final int sequenceId, final long frameNumber) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                }
            });
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull final CameraCaptureSession session,
                final int sequenceId) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureSequenceAborted(session, sequenceId);
                }
            });
        }

        @RequiresApi(24)
        @Override
        public void onCaptureBufferLost(@NonNull final CameraCaptureSession session,
                @NonNull final CaptureRequest request, @NonNull final Surface target,
                final long frameNumber) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureBufferLost(session, request, target, frameNumber);
                }
            });
        }
    }

    static final class StateCallbackExecutorWrapper extends CameraCaptureSession.StateCallback {

        final CameraCaptureSession.StateCallback mWrappedCallback;
        private final Executor mExecutor;

        StateCallbackExecutorWrapper(@NonNull Executor executor,
                @NonNull CameraCaptureSession.StateCallback wrappedCallback) {
            mExecutor = executor;
            mWrappedCallback = wrappedCallback;
        }

        @Override
        public void onConfigured(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onConfigured(session);
                }
            });
        }

        @Override
        public void onConfigureFailed(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onConfigureFailed(session);
                }
            });
        }

        @Override
        public void onReady(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onReady(session);
                }
            });
        }

        @Override
        public void onActive(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onActive(session);
                }
            });
        }

        @RequiresApi(26)
        @Override
        public void onCaptureQueueEmpty(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onCaptureQueueEmpty(session);
                }
            });
        }


        @Override
        public void onClosed(@NonNull final CameraCaptureSession session) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onClosed(session);
                }
            });
        }

        @RequiresApi(23)
        @Override
        public void onSurfacePrepared(@NonNull final CameraCaptureSession session,
                @NonNull final Surface surface) {
            mExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    mWrappedCallback.onSurfacePrepared(session, surface);
                }
            });
        }
    }
}
