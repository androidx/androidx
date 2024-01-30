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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.Quirks;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The interface for accessing features in {@link CameraCaptureSession}.
 *
 * <p>The SynchronizedCaptureSession is similar to the {@link CameraCaptureSession}. Some device
 * compatibility issues are already fixed in the SynchronizedCaptureSession.
 * CameraX can access almost all the APIs in the CameraCaptureSession via the
 * SynchronizedCaptureSession interface.
 *
 * <p>{@link SynchronizedCaptureSession} provide some extra overloaded methods that similar to the
 * {@link CameraCaptureSession} APIs but doesn't need the Executor parameter input. These methods
 * will automatically adopt the {@link androidx.camera.camera2.internal.annotation.CameraExecutor}
 * if it need to use a Executor. Most use cases should attempt to call the overloaded method
 * instead.
 *
 * <p>The {@link SynchronizedCaptureSession.Opener} can help to create the
 * {@link SynchronizedCaptureSession} object.
 *
 * @see SynchronizedCaptureSession.Opener
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface SynchronizedCaptureSession {

    @NonNull
    CameraDevice getDevice();

    @NonNull
    StateCallback getStateCallback();

    /**
     * Get the input Surface associated with a reprocessable capture session.
     *
     * <p> It is only supported from API 23. Each reprocessable capture session has an input
     * {@link Surface} where the reprocess capture requests get the input images from, rather
     * than the camera device. The application can create a {@link android.media.ImageWriter
     * ImageWriter} with this input {@link Surface} and use it to provide input images for
     * reprocess capture requests. When the reprocessable capture session is closed, the input
     * {@link Surface} is abandoned and becomes invalid.</p>
     *
     * @return The {@link Surface} where reprocessing capture requests get the input images from. If
     *         this is not a reprocess capture session, {@code null} will be returned.
     *
     * @see CameraCaptureSession#getInputSurface()
     */
    @Nullable
    Surface getInputSurface();

    /**
     * Get a {@link ListenableFuture} which indicates the task should be finished before another
     * {@link SynchronizedCaptureSession} to be opened.
     */
    @NonNull
    ListenableFuture<Void> getOpeningBlocker();

    /**
     * Return the {@link CameraCaptureSessionCompat} object which is used in this
     * SynchronizedCaptureSession.
     */
    @NonNull
    CameraCaptureSessionCompat toCameraCaptureSessionCompat();

    /**
     * Submit a request for an image to be captured by the camera device.
     *
     * <p>The behavior of this method similar to the
     * captureSingleRequest(CaptureRequest, Executor, CameraCaptureSession.CaptureCallback),
     * except that it uses the {@link Executor} that has been set in the constructor of the
     * SynchronizedCaptureSession.
     *
     * @param request  the settings for this capture
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int captureSingleRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException;

    /**
     * Submit a list of requests to be captured in sequence as a burst. The
     * burst will be captured in the minimum amount of time possible, and will
     * not be interleaved with requests submitted by other capture or repeat
     * calls.
     *
     * <p>The behavior of this method similar to the
     * captureBurstRequests(List, Executor, CameraCaptureSession.CaptureCallback),
     * except that it uses the {@link Executor} that has been set in the constructor of the
     * SynchronizedCaptureSession.
     *
     * @param requests the settings for this capture
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int captureBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    /**
     * <p>Request endlessly repeating capture of a sequence of images by this capture session.</p>
     *
     * <p>The behavior of this method similar to the
     * setSingleRepeatingRequest(CaptureRequest, Executor, CameraCaptureSession.CaptureCallback),
     * except that it uses the {@link Executor} that has been set in the constructor of the
     * SynchronizedCaptureSession.
     *
     * @param request  the settings for this capture
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int setSingleRepeatingRequest(
            @NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    /**
     * <p>Request endlessly repeating capture of a sequence of images by this capture session.</p>
     *
     * <p>The behavior of this method similar to the
     * setRepeatingBurstRequests(List, Executor, CameraCaptureSession.CaptureCallback),
     * except that it uses the {@link Executor} that has been set in the constructor of the
     * SynchronizedCaptureSession.
     *
     * @param requests the settings for this capture
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int setRepeatingBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    /**
     * Submit a request for an image to be captured by the camera device.
     *
     * <p>The behavior of this method matches that of
     * CameraCaptureSessionCompat#captureSingleRequest(CaptureRequest, Executor,
     * CameraCaptureSession.CaptureCallback)
     *
     * @param request  the settings for this capture
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify once this request has been processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int captureSingleRequest(@NonNull CaptureRequest request,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException;

    /**
     * Submit a list of requests to be captured in sequence as a burst. The burst will be
     * captured in the minimum amount of time possible, and will not be interleaved with requests
     * submitted by other capture or repeat calls.
     *
     * <p>The behavior of this method matches that of
     * CameraCaptureSessionCompat#captureBurstRequests(List, Executor,
     * CameraCaptureSession.CaptureCallback)
     *
     * @param requests the settings for this capture
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int captureBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    /**
     * <p>Request endlessly repeating capture of a sequence of images by this capture session.</p>
     *
     * <p>The behavior of this method matches that of
     * CameraCaptureSessionCompat#setSingleRepeatingRequest(CaptureRequest, Executor,
     * CameraCaptureSession.CaptureCallback)
     *
     * @param request  the settings for this capture
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int setSingleRepeatingRequest(
            @NonNull CaptureRequest request,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    /**
     * <p>Request endlessly repeating capture of a sequence of images by this capture session.</p>
     *
     * <p>The behavior of this method matches that of
     * CameraCaptureSessionCompat#setRepeatingBurstRequests(List, Executor,
     * CameraCaptureSession.CaptureCallback)
     *
     * @param requests the settings for this capture
     * @param executor the executor which will be used for invoking the listener.
     * @param listener The callback object to notify once this request has been
     *                 processed.
     * @return int A unique capture sequence ID used by
     * {@link CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted}.
     * @throws CameraAccessException if the camera device is no longer connected or has
     *                               encountered a fatal error
     */
    int setRepeatingBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException;

    void stopRepeating() throws CameraAccessException;

    void abortCaptures() throws CameraAccessException;

    /**
     * To speed up the camera switching, the close method will close the configured session and post
     * run the {@link StateCallback#onSessionFinished(SynchronizedCaptureSession)} to
     * inform the SynchronizedCaptureSession is already in the closed state.
     * The {@link StateCallback#onSessionFinished(SynchronizedCaptureSession)} means the session
     * is changed to a closed state, any further operations on this object is not acceptable.
     */
    void close();

    /**
     * Set the session has already been completely closed.
     *
     * <p>This is an internal state control method for SynchronizedSession and
     * CaptureSessionRepository, so you may not need to call this method outside.
     */
    void finishClose();

    /**
     * This method should be called when CameraDevice.StateCallback#onError happens.
     *
     * <p>It is used to inform the error of the CameraDevice, and should not be called for
     * other reasons.
     */
    void onCameraDeviceError(int error);

    /**
     * A callback object interface to adapting the updates from
     * {@link CameraCaptureSession.StateCallback}.
     *
     * <p>This method is similar to the {@link CameraCaptureSession.StateCallback}. The main
     * difference is users can receive the SynchronizedCaptureSession object from the callback.
     */
    abstract class StateCallback {

        void onReady(@NonNull SynchronizedCaptureSession session) {

        }

        void onActive(@NonNull SynchronizedCaptureSession session) {

        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        void onCaptureQueueEmpty(@NonNull SynchronizedCaptureSession session) {

        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        void onSurfacePrepared(@NonNull SynchronizedCaptureSession session,
                @NonNull Surface surface) {

        }

        void onConfigured(@NonNull SynchronizedCaptureSession session) {

        }

        public void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {

        }

        /**
         * This onClosed callback is a wrap of the CameraCaptureSession.StateCallback.onClosed, it
         * will be invoked when:
         * (1) CameraCaptureSession.StateCallback.onClosed is called.
         * (2) The CameraDevice is disconnected. When the CameraDevice.StateCallback#onDisconnect
         * is called, we will invoke this onClosed callback. Please see b/140955560.
         * (3) When a new CameraCaptureSession is created, all the previous opened
         * CameraCaptureSession can be treated as closed. Please see more detail in b/144817309.
         *
         * <p>Please note: The onClosed callback might not been called when the CameraDevice is
         * closed before the CameraCaptureSession is closed.
         *
         * @param session the SynchronizedCaptureSession that is created by
         * {@link SynchronizedCaptureSessionImpl#openCaptureSession}
         */
        public void onClosed(@NonNull SynchronizedCaptureSession session) {

        }

        /**
         * This callback will be invoked in the following condition:
         * (1) After the {@link SynchronizedCaptureSession#close()} is called. It means the
         * SynchronizedCaptureSession is changed to a closed state. Any further operations are not
         * expected for this SynchronizedCaptureSession.
         * (2) When the {@link SynchronizedCaptureSession.StateCallback#onClosed} is called.
         * This means the session is already detached from the camera device. For
         * example, close the camera device or open a second session, which should cause the first
         * one to be closed.
         *
         * <p>This callback only would be invoked at most one time for a configured
         * SynchronizedCaptureSession. Once the callback is called, we can treat this
         * SynchronizedCaptureSession is no longer active and further operations on this object
         * will fail.
         *
         * @param session the SynchronizedCaptureSession that is created by
         * {@link SynchronizedCaptureSessionImpl#openCaptureSession}
         */
        void onSessionFinished(@NonNull SynchronizedCaptureSession session) {

        }
    }

    /**
     * Opener interface to open the {@link SynchronizedCaptureSession}.
     *
     * <p>The {@link #openCaptureSession} method can be used to open a new
     * {@link SynchronizedCaptureSession}, and the {@link SessionConfigurationCompat} object is
     * needed by the {@link #openCaptureSession} should be created via the
     * {@link #createSessionConfigurationCompat}. It will send the ready-to-use
     * {@link SynchronizedCaptureSession} to the provided listener's
     * {@link SynchronizedCaptureSession.StateCallback#onConfigured} callback.
     *
     * <p>An Opener should only be used to open one SynchronizedCaptureSession. The Opener cannot be
     * reused to open the second SynchronizedCaptureSession. The {@link #openCaptureSession} can't
     * be called more than once in the same Opener.
     *
     * @see #openCaptureSession(CameraDevice, SessionConfigurationCompat, List)
     * @see #createSessionConfigurationCompat(int, List, SynchronizedCaptureSession.StateCallback)
     * @see SynchronizedCaptureSession.StateCallback
     *
     * <p>The {@link #stop} method should be invoked when the SynchronizedCaptureSession opening
     * flow is interrupted.
     * @see #startWithDeferrableSurface
     * @see #stop()
     */
    interface Opener {

        /**
         * Opens the SynchronizedCaptureSession.
         *
         * <p>The behavior of this method similar to the
         * {@link CameraDevice#createCaptureSession(SessionConfiguration)}. It will use the
         * input cameraDevice to create the SynchronizedCaptureSession.
         *
         * <p>The {@link SessionConfigurationCompat} object that is needed in this method should be
         * created via the {@link #createSessionConfigurationCompat}.
         *
         * <p>The use count of the input DeferrableSurfaces will be increased. It will be
         * automatically decreased when the surface is not used by the camera. For instance, when
         * the opened SynchronizedCaptureSession is closed completely or when the configuration of
         * the session is failed.
         *
         * <p>Cancellation of the returned future is a no-op. The opening task can only be
         * cancelled by the {@link #stop()}. The {@link #stop()} only effective when the
         * CameraDevice#createCaptureSession() hasn't been invoked. If the {@link #stop()} is called
         * before the CameraDevice#createCaptureSession(), it will stop the
         * SynchronizedCaptureSession creation.
         * Otherwise, the SynchronizedCaptureSession will be created and the
         * {@link SynchronizedCaptureSession.StateCallback#onConfigured} or
         * {@link SynchronizedCaptureSession.StateCallback#onConfigureFailed} callback will be
         * invoked.
         *
         * @param cameraDevice               the camera with which to generate the
         *                                   SynchronizedCaptureSession
         * @param sessionConfigurationCompat A {@link SessionConfigurationCompat} that is created
         *                                   via the {@link #createSessionConfigurationCompat}.
         * @param deferrableSurfaces         the list of the DeferrableSurface that be used to
         *                                   configure the session.
         * @return a ListenableFuture object which completes when the SynchronizedCaptureSession is
         * configured.
         * @see #createSessionConfigurationCompat
         * @see #stop()
         */
        @NonNull
        ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
                @NonNull SessionConfigurationCompat sessionConfigurationCompat,
                @NonNull List<DeferrableSurface> deferrableSurfaces);

        /**
         * Create the SessionConfigurationCompat for {@link #openCaptureSession} used.
         *
         * This method will add necessary information into the created SessionConfigurationCompat
         * instance for SynchronizedCaptureSession.
         *
         * @param sessionType   The session type.
         * @param outputsCompat A list of output configurations for the SynchronizedCaptureSession.
         * @param stateCallback A state callback interface implementation.
         */
        @NonNull
        SessionConfigurationCompat createSessionConfigurationCompat(int sessionType,
                @NonNull List<OutputConfigurationCompat> outputsCompat,
                @NonNull SynchronizedCaptureSession.StateCallback stateCallback);

        /**
         * Get the surface from the DeferrableSurfaces.
         *
         * <p>The {@link #startWithDeferrableSurface} method will return a Surface list that
         * is held in the List<DeferrableSurface>. The Opener helps in maintaining the timing to
         * close the returned DeferrableSurface list. Most use case should attempt to use the
         * {@link #startWithDeferrableSurface} method to get the Surface for creating the
         * SynchronizedCaptureSession.
         *
         * @param deferrableSurfaces The deferrable surfaces to open.
         * @param timeout            the timeout to get surfaces from the deferrable surface list.
         * @return the Future which will contain the surface list, Cancellation of this
         * future is a no-op. The returned Surface list can be used to create the
         * SynchronizedCaptureSession.
         * @see #openCaptureSession
         * @see #stop
         */
        @NonNull
        ListenableFuture<List<Surface>> startWithDeferrableSurface(
                @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout);

        @NonNull
        @CameraExecutor
        Executor getExecutor();

        /**
         * Disable the startWithDeferrableSurface() and openCaptureSession() ability, and stop the
         * startWithDeferrableSurface() and openCaptureSession() if
         * CameraDevice#createCaptureSession() hasn't been invoked. Once the
         * CameraDevice#createCaptureSession() already been invoked, the task of
         * openCaptureSession() will keep going.
         *
         * @return true if the CameraCaptureSession creation has not been started yet. Otherwise
         * return false.
         */
        boolean stop();
    }

    /**
     * A builder to create new {@link SynchronizedCaptureSession.Opener}
     */
    class OpenerBuilder {

        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduledExecutorService;
        private final Handler mCompatHandler;
        private final CaptureSessionRepository mCaptureSessionRepository;
        private final Quirks mCameraQuirks;
        private final Quirks mDeviceQuirks;

        OpenerBuilder(@NonNull @CameraExecutor Executor executor,
                @NonNull ScheduledExecutorService scheduledExecutorService,
                @NonNull Handler compatHandler,
                @NonNull CaptureSessionRepository captureSessionRepository,
                @NonNull Quirks cameraQuirks,
                @NonNull Quirks deviceQuirks) {
            mExecutor = executor;
            mScheduledExecutorService = scheduledExecutorService;
            mCompatHandler = compatHandler;
            mCaptureSessionRepository = captureSessionRepository;
            mCameraQuirks = cameraQuirks;
            mDeviceQuirks = deviceQuirks;
        }

        @NonNull
        Opener build() {
            return new SynchronizedCaptureSessionImpl(mCameraQuirks, mDeviceQuirks,
                    mCaptureSessionRepository, mExecutor, mScheduledExecutorService,
                    mCompatHandler);
        }
    }
}
