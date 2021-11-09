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
import androidx.camera.camera2.internal.SynchronizedCaptureSessionOpener.SynchronizedSessionFeature;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    @NonNull
    private final Set<String> mEnabledFeature;
    @NonNull
    private final ListenableFuture<Void> mStartStreamingFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mStartStreamingCompleter;

    @Nullable
    @GuardedBy("mObjectLock")
    private List<DeferrableSurface> mDeferrableSurfaces;
    @Nullable
    @GuardedBy("mObjectLock")
    ListenableFuture<Void> mOpeningCaptureSession;
    @Nullable
    @GuardedBy("mObjectLock")
    ListenableFuture<List<Surface>> mStartingSurface;

    /** Whether the capture session has submitted the repeating request. */
    @GuardedBy("mObjectLock")
    private boolean mHasSubmittedRepeating;

    SynchronizedCaptureSessionImpl(
            @NonNull Set<String> enabledFeature,
            @NonNull CaptureSessionRepository repository,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService,
            @NonNull Handler compatHandler) {
        super(repository, executor, scheduledExecutorService, compatHandler);
        mEnabledFeature = enabledFeature;

        if (enabledFeature.contains(SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST)) {
            mStartStreamingFuture = CallbackToFutureAdapter.getFuture(completer -> {
                // Opening and releasing the capture session quickly and constantly is a problem for
                // LEGACY devices. See: b/146773463. It needs to check all the releasing capture
                // sessions are ready for opening next capture session.
                mStartStreamingCompleter = completer;
                return "StartStreamingFuture[session=" + SynchronizedCaptureSessionImpl.this
                        + "]";
            });
        } else {
            mStartStreamingFuture = Futures.immediateFuture(null);
        }
    }

    @NonNull
    @Override
    public ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat,
            @NonNull List<DeferrableSurface> deferrableSurfaces) {
        synchronized (mObjectLock) {
            List<ListenableFuture<Void>> futureList =
                    getBlockerFuture(SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST,
                            mCaptureSessionRepository.getClosingCaptureSession());

            mOpeningCaptureSession =
                    FutureChain.from(Futures.successfulAsList(futureList)).transformAsync(
                            v -> super.openCaptureSession(cameraDevice,
                                    sessionConfigurationCompat, deferrableSurfaces),
                            CameraXExecutors.directExecutor());

            return Futures.nonCancellationPropagating(mOpeningCaptureSession);
        }
    }

    @NonNull
    @Override
    public ListenableFuture<Void> getSynchronizedBlocker(
            @SynchronizedSessionFeature @NonNull String feature) {
        switch (feature) {
            case SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST:
                // Returns the future which is completed once the session starts streaming
                // frames.
                return Futures.nonCancellationPropagating(mStartStreamingFuture);
            default:
                return super.getSynchronizedBlocker(feature);
        }
    }

    private List<ListenableFuture<Void>> getBlockerFuture(
            @SynchronizedSessionFeature @NonNull String feature,
            List<SynchronizedCaptureSession> sessions) {
        List<ListenableFuture<Void>> futureList = new ArrayList<>();
        for (SynchronizedCaptureSession session : sessions) {
            futureList.add(session.getSynchronizedBlocker(feature));
        }
        return futureList;
    }

    @NonNull
    @Override
    public ListenableFuture<List<Surface>> startWithDeferrableSurface(
            @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
        synchronized (mObjectLock) {
            mDeferrableSurfaces = deferrableSurfaces;
            return Futures.nonCancellationPropagating(
                    super.startWithDeferrableSurface(deferrableSurfaces, timeout));
        }
    }

    @Override
    public boolean stop() {
        synchronized (mObjectLock) {
            if (isCameraCaptureSessionOpen()) {
                closeConfiguredDeferrableSurfaces();
            } else {
                if (mOpeningCaptureSession != null) {
                    mOpeningCaptureSession.cancel(true);
                }
                if (mStartingSurface != null) {
                    mStartingSurface.cancel(true);
                }
            }
            return super.stop();
        }
    }

    @Override
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        if (mEnabledFeature.contains(SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST)) {
            synchronized (mObjectLock) {
                mHasSubmittedRepeating = true;
                CameraCaptureSession.CaptureCallback comboCaptureCallback =
                        Camera2CaptureCallbacks.createComboCallback(mCaptureCallback, listener);

                return super.setSingleRepeatingRequest(request, comboCaptureCallback);
            }
        } else {
            return super.setSingleRepeatingRequest(request, listener);
        }
    }

    @Override
    public void onConfigured(@NonNull SynchronizedCaptureSession session) {
        debugLog("Session onConfigured()");
        if (mEnabledFeature.contains(SynchronizedCaptureSessionOpener.FEATURE_FORCE_CLOSE)) {
            Set<SynchronizedCaptureSession> staleCreatingSessions = new LinkedHashSet<>();
            for (SynchronizedCaptureSession s :
                    mCaptureSessionRepository.getCreatingCaptureSessions()) {
                // Collect the sessions that started configuring before the current session. The
                // current session and the session that starts configure after the current session
                // are not included since they don't need to be closed.
                if (s == session) {
                    break;
                }
                staleCreatingSessions.add(s);
            }
            // Once the CaptureSession is configured, the stale CaptureSessions should not have
            // chance to complete the configuration flow. Force change to configure fail since
            // the configureFail will treat the CaptureSession is closed. More detail please see
            // b/158540776.
            forceOnConfigureFailed(staleCreatingSessions);
        }

        super.onConfigured(session);

        // Once the new CameraCaptureSession is created, all the previous opened
        // CameraCaptureSession can be treated as closed (more detail in b/144817309),
        // trigger its associated StateCallback#onClosed callback to finish the
        // session close flow.
        if (mEnabledFeature.contains(SynchronizedCaptureSessionOpener.FEATURE_FORCE_CLOSE)) {
            Set<SynchronizedCaptureSession> openedSessions = new LinkedHashSet<>();
            for (SynchronizedCaptureSession s : mCaptureSessionRepository.getCaptureSessions()) {

                // The entrySet keys of the LinkedHashMap should be insertion-ordered, so we
                // get the previous capture sessions by iterate it from the beginning.
                if (s == session) {
                    break;
                }
                openedSessions.add(s);
            }

            forceOnClosed(openedSessions);
        }
    }

    @Override
    public void close() {
        debugLog("Session call close()");
        if (mEnabledFeature.contains(SynchronizedCaptureSessionOpener.FEATURE_WAIT_FOR_REQUEST)) {
            synchronized (mObjectLock) {
                if (!mHasSubmittedRepeating) {
                    // If the release() is called before any repeating requests have been issued,
                    // then the startStreamingFuture should be cancelled.
                    mStartStreamingFuture.cancel(true);
                }
            }
        }

        mStartStreamingFuture.addListener(() -> {
            // Checks the capture session is ready before closing. See: b/146773463.
            debugLog("Session call super.close()");
            super.close();
        }, getExecutor());
    }

    void closeConfiguredDeferrableSurfaces() {
        synchronized (mObjectLock) {
            if (mDeferrableSurfaces == null) {
                debugLog("deferrableSurface == null, maybe forceClose, skip close");
                return;
            }

            if (mEnabledFeature.contains(
                    SynchronizedCaptureSessionOpener.FEATURE_DEFERRABLE_SURFACE_CLOSE)) {
                // Do not close for non-LEGACY devices. Reusing {@link DeferrableSurface} is only a
                // problem for LEGACY devices. See: b/135050586.
                // Another problem is the behavior of TextureView below API 23. It releases {@link
                // SurfaceTexture}. Hence, request to close and recreate {@link DeferrableSurface}.
                // See: b/145725334.
                for (DeferrableSurface deferrableSurface : mDeferrableSurfaces) {
                    deferrableSurface.close();
                }
                debugLog("deferrableSurface closed");
            }
        }
    }

    @Override
    public void onClosed(@NonNull SynchronizedCaptureSession session) {
        closeConfiguredDeferrableSurfaces();
        debugLog("onClosed()");
        super.onClosed(session);
    }

    static void forceOnClosed(@NonNull Set<SynchronizedCaptureSession> sessions) {
        for (SynchronizedCaptureSession session : sessions) {
            session.getStateCallback().onClosed(session);
        }
    }

    private void forceOnConfigureFailed(@NonNull Set<SynchronizedCaptureSession> sessions) {
        for (SynchronizedCaptureSession session : sessions) {
            session.getStateCallback().onConfigureFailed(session);
        }
    }

    void debugLog(String message) {
        Logger.d(TAG, "[" + SynchronizedCaptureSessionImpl.this + "] " + message);
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    if (mStartStreamingCompleter != null) {
                        mStartStreamingCompleter.set(null);
                        mStartStreamingCompleter = null;
                    }
                }

                @Override
                public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session,
                        int sequenceId) {
                    if (mStartStreamingCompleter != null) {
                        mStartStreamingCompleter.setCancelled();
                        mStartStreamingCompleter = null;
                    }
                }
            };
}
