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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.impl.DeferrableSurface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * The repository to maintain a list of the created and releasing SynchronizedCaptureSession.
 *
 * <p> The repository also help to close the created SynchronizedCaptureSession when the camera is
 * disconnected.
 */
class CaptureSessionRepository {
    /** Executor for all the callbacks from the {@link CameraCaptureSession}. */
    @NonNull
    @CameraExecutor
    final Executor mExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();

    @GuardedBy("mLock")
    final Set<SynchronizedCaptureSession> mCaptureSessions = new LinkedHashSet<>();
    @GuardedBy("mLock")
    final Set<SynchronizedCaptureSession> mClosingCaptureSession = new LinkedHashSet<>();
    @GuardedBy("mLock")
    final Set<SynchronizedCaptureSession> mCreatingCaptureSessions = new LinkedHashSet<>();
    @GuardedBy("mLock")
    final Map<SynchronizedCaptureSession, List<DeferrableSurface>> mDeferrableSurfaceMap =
            new HashMap<>();

    CaptureSessionRepository(@NonNull @CameraExecutor Executor executor) {
        mExecutor = executor;
    }

    private final CameraDevice.StateCallback mCameraStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    // Nothing to do.
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    // Force close all opened CameraCaptureSessions since the CameraDevice is in
                    // error state. The CameraCaptureSession.close() may not invoke the onClosed()
                    // callback so it has to finish the close process forcibly.
                    forceOnClosedCaptureSessions();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    // Force the onClosed() callback to be made. This is necessary because the
                    // onClosed() callback never gets called if CameraDevice.StateCallback
                    // .onDisconnected() is called. See
                    // TODO(b/140955560) If the issue is fixed then on OS releases with the fix
                    //  this should not be called and instead onClosed() should be called by the
                    //  framework instead.

                    // Force close all opened CameraCaptureSessions since the CameraDevice is
                    // disconnected.
                    // The CameraCaptureSession will call its close() automatically once the
                    // onDisconnected callback is invoked.
                    forceOnClosedCaptureSessions();
                }

                private void forceOnClosedCaptureSessions() {
                    mExecutor.execute(() -> {
                        LinkedHashSet<SynchronizedCaptureSession> sessions = new LinkedHashSet<>();
                        synchronized (mLock) {
                            sessions.addAll(new LinkedHashSet<>(mCreatingCaptureSessions));
                            sessions.addAll(new LinkedHashSet<>(mCaptureSessions));
                        }
                        forceOnClosed(sessions);
                    });

                }
            };

    @NonNull
    CameraDevice.StateCallback getCameraStateCallback() {
        return mCameraStateCallback;
    }

    static void forceOnClosed(@NonNull Set<SynchronizedCaptureSession> sessions) {
        for (SynchronizedCaptureSession session : sessions) {
            session.getStateCallback().onClosed(session);
        }
    }

    @NonNull
    List<SynchronizedCaptureSession> getCaptureSessions() {
        synchronized (mLock) {
            return new ArrayList<>(mCaptureSessions);
        }
    }

    @NonNull
    List<SynchronizedCaptureSession> getClosingCaptureSession() {
        synchronized (mLock) {
            return new ArrayList<>(mClosingCaptureSession);
        }
    }

    @NonNull
    List<SynchronizedCaptureSession> getCreatingCaptureSessions() {
        synchronized (mLock) {
            return new ArrayList<>(mCreatingCaptureSessions);
        }
    }

    /**
     * To register a DeferrableSurface list that is using by the SynchronizedCaptureSession.
     *
     *  <p>To register the deferrableSurface list means to identifying the list of
     *  deferrableSurfaces is already occupied by the SynchronizedCaptureSession.The
     *  registration information is shared between SynchronizedCaptureSession. Every
     *  SynchronizedCaptureSessions can get the registered information to know if a
     *  DeferrableSurface is already occupied by the another SynchronizedCaptureSession.
     *
     * @param synchronizedCaptureSession The SynchronizedCaptureSession that is going to configure
     *                                   a DeferrableSurfaceList.
     * @param deferrableSurfaces         The deferrable surface list that is configured to the
     *                                   SynchronizedCaptureSession.
     * @return a map of all the registered SynchronizedCaptureSession to the DeferrableSurfaceList.
     */
    Map<SynchronizedCaptureSession, List<DeferrableSurface>> registerDeferrableSurface(
            @NonNull SynchronizedCaptureSession synchronizedCaptureSession,
            @NonNull List<DeferrableSurface> deferrableSurfaces) {
        synchronized (mLock) {
            mDeferrableSurfaceMap.put(synchronizedCaptureSession, deferrableSurfaces);
            return new HashMap<>(mDeferrableSurfaceMap);
        }
    }

    /**
     * Unregister the {@link SynchronizedCaptureSession} you previously registered using
     * {@link #registerDeferrableSurface}.
     *
     * @param synchronizedCaptureSession the SynchronizedCaptureSession to be removed from the list.
     */
    void unregisterDeferrableSurface(
            @NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mDeferrableSurfaceMap.remove(synchronizedCaptureSession);
        }
    }

    void onCreateCaptureSession(@NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mCreatingCaptureSessions.add(synchronizedCaptureSession);
        }
    }

    void onCaptureSessionConfigureFail(
            @NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mCreatingCaptureSessions.remove(synchronizedCaptureSession);
        }
    }

    void onCaptureSessionCreated(
            @NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mCaptureSessions.add(synchronizedCaptureSession);
            mCreatingCaptureSessions.remove(synchronizedCaptureSession);
        }
    }

    void onCaptureSessionClosed(@NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mCaptureSessions.remove(synchronizedCaptureSession);
            mClosingCaptureSession.remove(synchronizedCaptureSession);
        }
    }

    void onCaptureSessionClosing(@NonNull SynchronizedCaptureSession synchronizedCaptureSession) {
        synchronized (mLock) {
            mClosingCaptureSession.add(synchronizedCaptureSession);
        }
    }
}
