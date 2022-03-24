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
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.camera2.internal.compat.workaround.ForceCloseCaptureSession;
import androidx.camera.camera2.internal.compat.workaround.ForceCloseDeferrableSurface;
import androidx.camera.camera2.internal.compat.workaround.WaitForRepeatingRequestStart;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>The SynchronizedCaptureSessionImpl synchronizing methods between the other
 * SynchronizedCaptureSessions to fix b/135050586, b/145725334, b/144817309, b/146773463. The
 * SynchronizedCaptureSessionBaseImpl would be a non-synchronizing version.
 *
 * <p>In b/144817309, the onClosed() callback on
 * {@link android.hardware.camera2.CameraCaptureSession.StateCallback}
 * might not be invoked if the capture session is not the latest one. To align the fixed
 * framework behavior, we manually call the onClosed() when a new CameraCaptureSession is created.
 *
 * <p>The b/135050586, b/145725334 need to close the {@link DeferrableSurface} to force the
 * {@link DeferrableSurface} recreate in the new CaptureSession.
 *
 * <p>b/146773463: It needs to check all the releasing capture sessions are ready for opening
 * next capture session.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class SynchronizedCaptureSessionImpl extends SynchronizedCaptureSessionBaseImpl {

    private static final String TAG = "SyncCaptureSessionImpl";

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    private final Object mObjectLock = new Object();

    @Nullable
    @GuardedBy("mObjectLock")
    private List<DeferrableSurface> mDeferrableSurfaces;
    @Nullable
    @GuardedBy("mObjectLock")
    ListenableFuture<Void> mOpeningCaptureSession;

    private final ForceCloseDeferrableSurface mCloseSurfaceQuirk;
    private final WaitForRepeatingRequestStart mWaitForOtherSessionCompleteQuirk;
    private final ForceCloseCaptureSession mForceCloseSessionQuirk;

    SynchronizedCaptureSessionImpl(
            @NonNull Set<String> enabledFeature,
            @NonNull CaptureSessionRepository repository,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService,
            @NonNull Handler compatHandler) {
        super(repository, executor, scheduledExecutorService, compatHandler);
        mCloseSurfaceQuirk = new ForceCloseDeferrableSurface(new Quirks(new ArrayList<>()),
                new Quirks(new ArrayList<>()));
        mWaitForOtherSessionCompleteQuirk = new WaitForRepeatingRequestStart(
                new Quirks(new ArrayList<>()));
        mForceCloseSessionQuirk = new ForceCloseCaptureSession(new Quirks(new ArrayList<>()));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat,
            @NonNull List<DeferrableSurface> deferrableSurfaces) {
        synchronized (mObjectLock) {
            mOpeningCaptureSession = mWaitForOtherSessionCompleteQuirk.openCaptureSession(
                    cameraDevice, sessionConfigurationCompat, deferrableSurfaces,
                    mCaptureSessionRepository.getClosingCaptureSession(),
                    super::openCaptureSession);
            return Futures.nonCancellationPropagating(mOpeningCaptureSession);
        }
    }

    @NonNull
    @Override
    public ListenableFuture<Void> getOpeningBlocker() {
        return mWaitForOtherSessionCompleteQuirk.getStartStreamFuture();
    }

    @NonNull
    @Override
    public ListenableFuture<List<Surface>> startWithDeferrableSurface(
            @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
        synchronized (mObjectLock) {
            mDeferrableSurfaces = deferrableSurfaces;
            return super.startWithDeferrableSurface(deferrableSurfaces, timeout);
        }
    }

    @Override
    public boolean stop() {
        synchronized (mObjectLock) {
            if (isCameraCaptureSessionOpen()) {
                mCloseSurfaceQuirk.onSessionEnd(mDeferrableSurfaces);
            } else if (mOpeningCaptureSession != null) {
                mOpeningCaptureSession.cancel(true);
            }
            return super.stop();
        }
    }

    @Override
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        return mWaitForOtherSessionCompleteQuirk.setSingleRepeatingRequest(
                request, listener, super::setSingleRepeatingRequest);
    }

    @Override
    public void onConfigured(@NonNull SynchronizedCaptureSession session) {
        debugLog("Session onConfigured()");
        mForceCloseSessionQuirk.onSessionConfigured(session,
                mCaptureSessionRepository.getCreatingCaptureSessions(),
                mCaptureSessionRepository.getCaptureSessions(),
                super::onConfigured);
    }

    @Override
    public void close() {
        debugLog("Session call close()");
        mWaitForOtherSessionCompleteQuirk.onSessionEnd();
        mWaitForOtherSessionCompleteQuirk.getStartStreamFuture().addListener(() -> {
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

    void debugLog(String message) {
        Logger.d(TAG, "[" + SynchronizedCaptureSessionImpl.this + "] " + message);
    }
}
