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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.DeferrableSurface;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The Opener to open the {@link SynchronizedCaptureSession}.
 *
 * <p>The {@link #openCaptureSession} method can be used to open a new
 * {@link SynchronizedCaptureSession}, and the {@link SessionConfigurationCompat} object that
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
 * <p>The {@link #stop} method should be invoked when the SynchronizedCaptureSession opening flow
 * is interropted.
 * @see #startWithDeferrableSurface
 * @see #stop()
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SynchronizedCaptureSessionOpener {

    /**
     * Force close CaptureSession
     * Criteria: OS version
     * Description: on API22 or lower CaptureSession's close event might not be triggered and
     * thus have to be force closed
     */
    static final String FEATURE_FORCE_CLOSE = "force_close";

    /**
     * Do not close for non-LEGACY devices
     * Criteria: hardware level, OS version
     * Description: Do not close for non-LEGACY devices. Reusing {@link DeferrableSurface} is
     * only a problem for LEGACY devices. See: b/135050586. Another problem is the behavior of
     * TextureView below API 23. It releases {@link SurfaceTexture}. Hence, request to close and
     * recreate {@link DeferrableSurface}. See: b/145725334. (A better place for View related
     * workaround is in the view artifact. However changing this now would be a breaking change.)
     */
    static final String FEATURE_DEFERRABLE_SURFACE_CLOSE = "deferrableSurface_close";

    /**
     * Delay Opening and releasing the capture session after set repeating request complete.
     * Criteria: hardware level
     * Description: Opening and releasing the capture session quickly and constantly is a problem
     * for LEGACY devices. See: b/146773463. It needs to check all the releasing capture
     * sessions are ready for opening next capture session.
     */
    static final String FEATURE_WAIT_FOR_REQUEST = "wait_for_request";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({FEATURE_DEFERRABLE_SURFACE_CLOSE, FEATURE_WAIT_FOR_REQUEST, FEATURE_FORCE_CLOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SynchronizedSessionFeature {
    }

    @NonNull
    private final OpenerImpl mImpl;

    SynchronizedCaptureSessionOpener(@NonNull OpenerImpl impl) {
        mImpl = impl;
    }

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
     * automatically decreased when the surface is not used by the camera. For instance, when the
     * opened SynchronizedCaptureSession is closed completely or when the configuration of the
     * session is failed.
     *
     * <p>Cancellation of the returned future is a no-op. The opening task can only be
     * cancelled by the {@link #stop()}. The {@link #stop()} only effective when the
     * CameraDevice#createCaptureSession() hasn't been invoked. If the {@link #stop()} is called
     * before the CameraDevice#createCaptureSession(), it will stop the
     * SynchronizedCaptureSession creation.
     * Otherwise, the SynchronizedCaptureSession will be created and the
     * {@link SynchronizedCaptureSession.StateCallback#onConfigured} or
     * {@link SynchronizedCaptureSession.StateCallback#onConfigureFailed} callback will be invoked.
     *
     * @param cameraDevice               the camera with which to generate the
     *                                   SynchronizedCaptureSession
     * @param sessionConfigurationCompat A {@link SessionConfigurationCompat} that is created via
     *                                   the {@link #createSessionConfigurationCompat}.
     * @param deferrableSurfaces         the list of the DeferrableSurface that be used to
     *                                   configure the session.
     * @return a ListenableFuture object which completes when the SynchronizedCaptureSession is
     * configured.
     * @see #createSessionConfigurationCompat(int, List, SynchronizedCaptureSession.StateCallback)
     * @see #stop()
     */
    @NonNull
    ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat,
            @NonNull List<DeferrableSurface> deferrableSurfaces) {
        return mImpl.openCaptureSession(cameraDevice, sessionConfigurationCompat,
                deferrableSurfaces);
    }

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
            @NonNull SynchronizedCaptureSession.StateCallback stateCallback) {
        return mImpl.createSessionConfigurationCompat(sessionType, outputsCompat,
                stateCallback);
    }

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
            @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
        return mImpl.startWithDeferrableSurface(deferrableSurfaces, timeout);
    }

    /**
     * Disable the startWithDeferrableSurface() and openCaptureSession() ability, and stop the
     * startWithDeferrableSurface() and openCaptureSession() if CameraDevice#createCaptureSession()
     * hasn't been invoked. Once the CameraDevice#createCaptureSession() already been invoked, the
     * task of openCaptureSession() will keep going.
     *
     * @return true if the CameraCaptureSession creation has not been started yet. Otherwise true
     * false.
     */
    boolean stop() {
        return mImpl.stop();
    }

    @NonNull
    @CameraExecutor
    public Executor getExecutor() {
        return mImpl.getExecutor();
    }

    static class Builder {
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduledExecutorService;
        private final Handler mCompatHandler;
        private final CaptureSessionRepository mCaptureSessionRepository;
        private final int mSupportedHardwareLevel;
        private final Set<String> mEnableFeature = new HashSet<>();

        Builder(@NonNull @CameraExecutor Executor executor,
                @NonNull ScheduledExecutorService scheduledExecutorService,
                @NonNull Handler compatHandler,
                @NonNull CaptureSessionRepository captureSessionRepository,
                int supportedHardwareLevel) {
            mExecutor = executor;
            mScheduledExecutorService = scheduledExecutorService;
            mCompatHandler = compatHandler;
            mCaptureSessionRepository = captureSessionRepository;
            mSupportedHardwareLevel = supportedHardwareLevel;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                mEnableFeature.add(FEATURE_FORCE_CLOSE);
            }

            if (mSupportedHardwareLevel
                    == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                    || Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                mEnableFeature.add(FEATURE_DEFERRABLE_SURFACE_CLOSE);
            }

            if (mSupportedHardwareLevel
                    == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                mEnableFeature.add(FEATURE_WAIT_FOR_REQUEST);
            }
        }

        @NonNull
        SynchronizedCaptureSessionOpener build() {
            if (mEnableFeature.isEmpty()) {
                return new SynchronizedCaptureSessionOpener(
                        new SynchronizedCaptureSessionBaseImpl(mCaptureSessionRepository, mExecutor,
                                mScheduledExecutorService, mCompatHandler));
            }

            return new SynchronizedCaptureSessionOpener(
                    new SynchronizedCaptureSessionImpl(mEnableFeature, mCaptureSessionRepository,
                            mExecutor, mScheduledExecutorService, mCompatHandler));
        }
    }

    interface OpenerImpl {

        @NonNull
        ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice, @NonNull
                SessionConfigurationCompat sessionConfigurationCompat,
                @NonNull List<DeferrableSurface> deferrableSurfaces);

        @NonNull
        SessionConfigurationCompat createSessionConfigurationCompat(int sessionType,
                @NonNull List<OutputConfigurationCompat> outputsCompat,
                @NonNull SynchronizedCaptureSession.StateCallback stateCallback);

        @NonNull
        @CameraExecutor
        Executor getExecutor();

        @NonNull
        ListenableFuture<List<Surface>> startWithDeferrableSurface(
                @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout);

        boolean stop();
    }
}
