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
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.SynchronizedCaptureSessionOpener.SynchronizedSessionFeature;
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;

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
 * <p>The {@link SynchronizedCaptureSessionOpener} can help to create the
 * {@link SynchronizedCaptureSession} object.
 *
 * @see SynchronizedCaptureSessionOpener
 */
interface SynchronizedCaptureSession {

    @NonNull
    CameraDevice getDevice();

    @NonNull
    StateCallback getStateCallback();

    /**
     * Get a {@link ListenableFuture} which indicate the progress of specific task on this
     * SynchronizedCaptureSession.
     *
     * @param feature the key to get the ListenableFuture. The key can be
     *                {@link SynchronizedSessionFeature#FEATURE_WAIT_FOR_REQUEST}.
     * @return the ListenableFuture which completes when the specific task is completed.
     */
    @NonNull
    ListenableFuture<Void> getSynchronizedBlocker(
            @SynchronizedSessionFeature @NonNull String feature);

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

        void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {

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
        void onClosed(@NonNull SynchronizedCaptureSession session) {

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
}
