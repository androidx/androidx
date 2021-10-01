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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.SessionConfig;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * An interface for manipulating the session to capture images from the camera which is tied to a
 * specific {@link CameraDevice}.
 *
 * <p>A session can only be opened a single time. Once has {@link CaptureSessionInterface
 * #close()} been called then it is permanently closed so a new session has to be created for
 * capturing images.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface CaptureSessionInterface {
    /**
     * Opens the capture session.
     *
     * <p>When the session is opened and the configurations have been set then the capture requests
     * will be issued.
     *
     * <p>The cancellation of the returned ListenableFuture will not propagate into the inner
     * future, that is, the capture session creation process is not cancelable.
     *
     * @param sessionConfig which is used to configure the camera capture session.
     *                      This contains configurations which may or may not be currently
     *                      active in issuing capture requests.
     * @param cameraDevice  the camera with which to generate the capture session
     * @param opener        The opener to open the {@link SynchronizedCaptureSession}.
     * @return A {@link ListenableFuture} that will be completed once the
     * {@link CameraCaptureSession} has been configured.
     */
    @NonNull
    ListenableFuture<Void> open(@NonNull SessionConfig sessionConfig,
            @NonNull CameraDevice cameraDevice,
            @NonNull SynchronizedCaptureSessionOpener opener);


    /**
     * Sets the active configurations for the capture session.
     *
     * <p>Once both the session configuration has been set and the session has been opened, then the
     * repeating capture requests will immediately be issued.
     *
     * @param sessionConfig has the configuration that will currently active in issuing capture
     *                      request. The surfaces contained in this must be a subset of the
     *                      surfaces that were used to open this capture session.
     */
    void setSessionConfig(@Nullable SessionConfig sessionConfig);

    /**
     * Returns the configurations of the capture session, or null if it has not yet been set
     * or if the capture session has been closed.
     */
    @Nullable
    SessionConfig getSessionConfig();

    /** Returns the configurations of the capture requests. */
    @NonNull
    List<CaptureConfig> getCaptureConfigs();

    /**
     * Issues capture requests.
     *
     * @param captureConfigs which is used to construct {@link CaptureRequest}.
     */
    void issueCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);

    /**
     * Cancels issued capture requests.
     */
    void cancelIssuedCaptureRequests();

    /**
     * Closes the capture session.
     *
     * <p>Close() needs be called on a session in order to safely open another session. However,
     * this stops minimal resources so that another session can be quickly opened.
     *
     * <p>Once a session is closed it can no longer be opened again. After the session is closed all
     * method calls on it do nothing.
     */
    void close();

    /**
     * Releases the capture session.
     *
     * <p>This releases all of the sessions resources and should be called when ready to close the
     * camera.
     *
     * <p>Once a session is released it can no longer be opened again. After the session is released
     * all method calls on it do nothing.
     */
    @NonNull
    ListenableFuture<Void> release(boolean abortInFlightCaptures);
}
