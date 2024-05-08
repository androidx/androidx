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
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionStuckQuirk;
import androidx.camera.camera2.internal.compat.quirk.IncorrectCaptureStateQuirk;
import androidx.camera.camera2.internal.compat.workaround.ForceCloseCaptureSession;
import androidx.camera.camera2.internal.compat.workaround.ForceCloseDeferrableSurface;
import androidx.camera.camera2.internal.compat.workaround.RequestMonitor;
import androidx.camera.camera2.internal.compat.workaround.SessionResetPolicy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The SynchronizedCaptureSessionImpl applies workarounds for Quirks.
 */
class SynchronizedCaptureSessionImpl extends SynchronizedCaptureSessionBaseImpl {

    private static final String TAG = "SyncCaptureSessionImpl";

    @NonNull
    private final ScheduledExecutorService mScheduledExecutorService;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private final Object mObjectLock = new Object();

    @Nullable
    @GuardedBy("mObjectLock")
    private List<DeferrableSurface> mDeferrableSurfaces;
    @Nullable
    @GuardedBy("mObjectLock")
    ListenableFuture<List<Void>> mOpenSessionBlockerFuture;

    private final ForceCloseDeferrableSurface mCloseSurfaceQuirk;
    private final ForceCloseCaptureSession mForceCloseSessionQuirk;
    private final RequestMonitor mRequestMonitor;
    private final SessionResetPolicy mSessionResetPolicy;
    private final AtomicBoolean mClosed = new AtomicBoolean(false);

    SynchronizedCaptureSessionImpl(
            @NonNull Quirks cameraQuirks,
            @NonNull Quirks deviceQuirks,
            @NonNull CaptureSessionRepository repository,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService,
            @NonNull Handler compatHandler) {
        super(repository, executor, scheduledExecutorService, compatHandler);
        mCloseSurfaceQuirk = new ForceCloseDeferrableSurface(cameraQuirks, deviceQuirks);
        mRequestMonitor = new RequestMonitor(cameraQuirks.contains(CaptureSessionStuckQuirk.class)
                || cameraQuirks.contains(IncorrectCaptureStateQuirk.class));
        mForceCloseSessionQuirk = new ForceCloseCaptureSession(deviceQuirks);
        mSessionResetPolicy = new SessionResetPolicy(deviceQuirks);
        mScheduledExecutorService = scheduledExecutorService;
    }

    @ExecutedBy("mExecutor")
    @NonNull
    @Override
    public ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat,
            @NonNull List<DeferrableSurface> deferrableSurfaces) {
        synchronized (mObjectLock) {
            // For b/146773463: It needs to check all the configured capture sessions are ready for
            // opening next capture session.
            List<SynchronizedCaptureSession>
                    configured = mCaptureSessionRepository.getCaptureSessions();
            List<ListenableFuture<Void>> futureList = new ArrayList<>();
            for (SynchronizedCaptureSession session : configured) {
                futureList.add(session.getOpeningBlocker());
            }
            mOpenSessionBlockerFuture = Futures.successfulAsList(futureList);

            return Futures.nonCancellationPropagating(
                    FutureChain.from(mOpenSessionBlockerFuture).transformAsync(v -> {
                        if (mSessionResetPolicy.needAbortCapture()) {
                            closeCreatedSession();
                        }
                        debugLog("start openCaptureSession");
                        return super.openCaptureSession(cameraDevice, sessionConfigurationCompat,
                                deferrableSurfaces);
                    }, getExecutor()));
        }
    }

    @ExecutedBy("mExecutor")
    private void closeCreatedSession() {
        List<SynchronizedCaptureSession> sessions = mCaptureSessionRepository.getCaptureSessions();
        for (SynchronizedCaptureSession session : sessions) {
            session.close();
        }
    }

    @ExecutedBy("mExecutor")
    @NonNull
    @Override
    public ListenableFuture<Void> getOpeningBlocker() {
        return Futures.makeTimeoutFuture(1500, mScheduledExecutorService,
                mRequestMonitor.getRequestsProcessedFuture());
    }

    @ExecutedBy("mExecutor")
    @NonNull
    @Override
    public ListenableFuture<List<Surface>> startWithDeferrableSurface(
            @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
        synchronized (mObjectLock) {
            mDeferrableSurfaces = deferrableSurfaces;
            return super.startWithDeferrableSurface(deferrableSurfaces, timeout);
        }
    }

    @ExecutedBy("mExecutor")
    @Override
    public boolean stop() {
        synchronized (mObjectLock) {
            if (isCameraCaptureSessionOpen()) {
                mCloseSurfaceQuirk.onSessionEnd(mDeferrableSurfaces);
            } else {
                if (mOpenSessionBlockerFuture != null) {
                    mOpenSessionBlockerFuture.cancel(true);
                }
            }
            return super.stop();
        }
    }

    @ExecutedBy("mExecutor")
    @Override
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        return super.setSingleRepeatingRequest(
                request, mRequestMonitor.createMonitorListener(listener));
    }

    @ExecutedBy("mExecutor")
    @Override
    public int captureBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        return super.captureBurstRequests(
                requests, mRequestMonitor.createMonitorListener(listener));
    }

    @Override
    public void onConfigured(@NonNull SynchronizedCaptureSession session) {
        debugLog("Session onConfigured()");
        mForceCloseSessionQuirk.onSessionConfigured(session,
                mCaptureSessionRepository.getCreatingCaptureSessions(),
                mCaptureSessionRepository.getCaptureSessions(),
                super::onConfigured);
    }

    @ExecutedBy("mExecutor")
    @Override
    public void close() {
        if (!mClosed.compareAndSet(false, true)) {
            debugLog("close() has been called. Skip this invocation.");
            return;
        }

        if (mSessionResetPolicy.needAbortCapture()) {
            try {
                debugLog("Call abortCaptures() before closing session.");
                abortCaptures();
            } catch (Exception e) {
                debugLog("Exception when calling abortCaptures()" + e);
            }
        }

        debugLog("Session call close()");
        mRequestMonitor.getRequestsProcessedFuture().addListener(() -> {
            // Checks the capture session is ready before closing. See: b/146773463.
            debugLog("Session call super.close()");
            super.close();
        }, getExecutor());
    }

    @Override
    public void onClosed(@NonNull SynchronizedCaptureSession session) {
        synchronized (mObjectLock) {
            mCloseSurfaceQuirk.onSessionEnd(mDeferrableSurfaces);
        }
        debugLog("onClosed()");
        super.onClosed(session);
    }

    @Override
    public void finishClose() {
        super.finishClose();
        mRequestMonitor.stop();
    }

    @ExecutedBy("mExecutor")
    @Override
    public void onCameraDeviceError(int error) {
        super.onCameraDeviceError(error);
        if (error == CameraDevice.StateCallback.ERROR_CAMERA_SERVICE) {
            synchronized (mObjectLock) {
                if (isCameraCaptureSessionOpen() && mDeferrableSurfaces != null) {
                    debugLog("Close DeferrableSurfaces for CameraDevice error.");
                    // b/290861504#comment4, close the DeferrableSurfaces.
                    for (DeferrableSurface deferrableSurface : mDeferrableSurfaces) {
                        deferrableSurface.close();
                    }
                }
            }
        }
    }

    void debugLog(String message) {
        Logger.d(TAG, "[" + SynchronizedCaptureSessionImpl.this + "] " + message);
    }
}
