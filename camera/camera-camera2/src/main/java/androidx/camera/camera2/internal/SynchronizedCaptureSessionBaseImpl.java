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
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.SynchronizedCaptureSessionOpener.SynchronizedSessionFeature;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCaptureSessionCompat;
import androidx.camera.camera2.internal.compat.CameraDeviceCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.DeferrableSurfaces;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The basic implementation of {@link SynchronizedCaptureSession} to forward the feature calls
 * into the {@link CameraCaptureSession}. It will not synchronize methods with the other
 * SynchronizedCaptureSessions.
 *
 * The {@link StateCallback} to receives the state callbacks from the
 * {@link CameraCaptureSession.StateCallback} and convert the {@link CameraCaptureSession} to the
 * SynchronizedCaptureSession object.
 */
class SynchronizedCaptureSessionBaseImpl extends SynchronizedCaptureSession.StateCallback implements
        SynchronizedCaptureSession, SynchronizedCaptureSessionOpener.OpenerImpl {

    private static final String TAG = "SyncCaptureSessionBase";

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();

    @NonNull
    final CaptureSessionRepository mCaptureSessionRepository;
    @NonNull
    final Handler mCompatHandler;
    @NonNull
    @CameraExecutor
    final Executor mExecutor;
    @NonNull
    private final ScheduledExecutorService mScheduledExecutorService;

    @Nullable
    StateCallback mCaptureSessionStateCallback;
    @Nullable
    CameraCaptureSessionCompat mCameraCaptureSessionCompat;

    @Nullable
    @GuardedBy("mLock")
    ListenableFuture<Void> mOpenCaptureSessionFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    @GuardedBy("mLock")
    Completer<Void> mOpenCaptureSessionCompleter;

    @Nullable
    @GuardedBy("mLock")
    private ListenableFuture<List<Surface>> mStartingSurface;

    @GuardedBy("mLock")
    private boolean mClosed = false;
    @GuardedBy("mLock")
    private boolean mOpenerDisabled = false;

    SynchronizedCaptureSessionBaseImpl(@NonNull CaptureSessionRepository repository,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService,
            @NonNull Handler compatHandler) {
        mCaptureSessionRepository = repository;
        mCompatHandler = compatHandler;
        mExecutor = executor;
        mScheduledExecutorService = scheduledExecutorService;
    }

    @NonNull
    @Override
    public StateCallback getStateCallback() {
        return this;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> getSynchronizedBlocker(
            @SynchronizedSessionFeature @NonNull String feature) {
        return Futures.immediateFuture(null);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> openCaptureSession(@NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat) {
        synchronized (mLock) {
            if (mOpenerDisabled) {
                return Futures.immediateFailedFuture(
                        new CancellationException("Opener is disabled"));
            }
            mCaptureSessionRepository.onCreateCaptureSession(this);
            CameraDeviceCompat cameraDeviceCompat =
                    CameraDeviceCompat.toCameraDeviceCompat(cameraDevice, mCompatHandler);
            mOpenCaptureSessionFuture = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        synchronized (mLock) {
                            Preconditions.checkState(mOpenCaptureSessionCompleter == null,
                                    "The openCaptureSessionCompleter can only set once!");

                            mOpenCaptureSessionCompleter = completer;
                            cameraDeviceCompat.createCaptureSession(sessionConfigurationCompat);
                            return "openCaptureSession[session="
                                    + SynchronizedCaptureSessionBaseImpl.this + "]";
                        }
                    });

            return Futures.nonCancellationPropagating(mOpenCaptureSessionFuture);
        }
    }

    boolean isCameraCaptureSessionOpen() {
        synchronized (mLock) {
            return mOpenCaptureSessionFuture != null;
        }
    }

    @NonNull
    @Override
    public SessionConfigurationCompat createSessionConfigurationCompat(
            int sessionType,
            @NonNull List<OutputConfigurationCompat> outputsCompat,
            @NonNull StateCallback stateCallback) {
        mCaptureSessionStateCallback = stateCallback;
        return new SessionConfigurationCompat(sessionType, outputsCompat, getExecutor(),
                new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onReady(@NonNull CameraCaptureSession session) {
                        createCaptureSessionCompat(session);
                        SynchronizedCaptureSessionBaseImpl.this.onReady(
                                SynchronizedCaptureSessionBaseImpl.this);
                    }

                    @Override
                    public void onActive(@NonNull CameraCaptureSession session) {
                        createCaptureSessionCompat(session);
                        SynchronizedCaptureSessionBaseImpl.this.onActive(
                                SynchronizedCaptureSessionBaseImpl.this);
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
                        createCaptureSessionCompat(session);
                        SynchronizedCaptureSessionBaseImpl.this.onCaptureQueueEmpty(
                                SynchronizedCaptureSessionBaseImpl.this);
                    }

                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onSurfacePrepared(
                            @NonNull CameraCaptureSession session,
                            @NonNull Surface surface) {
                        createCaptureSessionCompat(session);
                        SynchronizedCaptureSessionBaseImpl.this.onSurfacePrepared(
                                SynchronizedCaptureSessionBaseImpl.this, surface);
                    }

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            createCaptureSessionCompat(session);
                            SynchronizedCaptureSessionBaseImpl.this.onConfigured(
                                    SynchronizedCaptureSessionBaseImpl.this);
                        } finally {
                            // Finish the mOpenCaptureSessionCompleter after callback.
                            Completer<Void> completer;
                            synchronized (mLock) {
                                Preconditions.checkNotNull(mOpenCaptureSessionCompleter,
                                        "OpenCaptureSession completer should not null");
                                completer = mOpenCaptureSessionCompleter;
                                mOpenCaptureSessionCompleter = null;
                            }
                            completer.set(null);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        try {
                            createCaptureSessionCompat(session);
                            SynchronizedCaptureSessionBaseImpl.this.onConfigureFailed(
                                    SynchronizedCaptureSessionBaseImpl.this);
                        } finally {
                            // Finish the mOpenCaptureSessionCompleter after callback.
                            Completer<Void> completer;
                            synchronized (mLock) {
                                Preconditions.checkNotNull(mOpenCaptureSessionCompleter,
                                        "OpenCaptureSession completer should not null");
                                completer = mOpenCaptureSessionCompleter;
                                mOpenCaptureSessionCompleter = null;
                            }
                            completer.setException(new IllegalStateException("onConfigureFailed"));
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        createCaptureSessionCompat(session);
                        SynchronizedCaptureSessionBaseImpl.this.onClosed(
                                SynchronizedCaptureSessionBaseImpl.this);
                    }
                });
    }

    @NonNull
    @Override
    @CameraExecutor
    public Executor getExecutor() {
        return mExecutor;
    }

    void createCaptureSessionCompat(@NonNull CameraCaptureSession session) {
        if (mCameraCaptureSessionCompat == null) {
            mCameraCaptureSessionCompat = CameraCaptureSessionCompat.toCameraCaptureSessionCompat(
                    session, mCompatHandler);
        }
    }

    @NonNull
    @Override
    public ListenableFuture<List<Surface>> startWithDeferrableSurface(
            @NonNull List<DeferrableSurface> deferrableSurfaces, long timeout) {
        synchronized (mLock) {
            if (mOpenerDisabled) {
                return Futures.immediateFailedFuture(
                        new CancellationException("Opener is disabled"));
            }

            mStartingSurface = FutureChain.from(
                    DeferrableSurfaces.surfaceListWithTimeout(deferrableSurfaces, false, timeout,
                            getExecutor(), mScheduledExecutorService)).transformAsync(surfaces -> {
                                debugLog("getSurface...done");
                                // If a Surface in configuredSurfaces is null it means the
                                // Surface was not retrieved from the ListenableFuture. Only
                                // handle the first failed Surface since subsequent calls to
                                // CaptureSession.open() will handle the other failed Surfaces if
                                // there are any.
                                if (surfaces.contains(null)) {
                                    DeferrableSurface deferrableSurface = deferrableSurfaces.get(
                                            surfaces.indexOf(null));
                                    return Futures.immediateFailedFuture(
                                            new DeferrableSurface.SurfaceClosedException(
                                                    "Surface closed", deferrableSurface));
                                }

                                if (surfaces.isEmpty()) {
                                    return Futures.immediateFailedFuture(
                                            new IllegalArgumentException(
                                                    "Unable to open capture session without "
                                                            + "surfaces"));
                                }

                                return Futures.immediateFuture(surfaces);
                            }, getExecutor());

            return Futures.nonCancellationPropagating(mStartingSurface);
        }
    }

    @Override
    public boolean stop() {
        ListenableFuture<List<Surface>> startingSurface = null;
        try {
            synchronized (mLock) {
                if (!mOpenerDisabled) {
                    if (mStartingSurface != null) {
                        startingSurface = mStartingSurface;
                    }
                    mOpenerDisabled = true;
                }

                // Return true if the CameraCaptureSession creation has not been started yet.
                return !isCameraCaptureSessionOpen();
            }
        } finally {
            if (startingSurface != null) {
                startingSurface.cancel(true);
            }
        }
    }

    @NonNull
    @Override
    public CameraCaptureSessionCompat toCameraCaptureSessionCompat() {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat);
        return mCameraCaptureSessionCompat;
    }

    @NonNull
    @Override
    public CameraDevice getDevice() {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat);
        return mCameraCaptureSessionCompat.toCameraCaptureSession().getDevice();
    }

    @Override
    public int captureSingleRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        return mCameraCaptureSessionCompat.captureSingleRequest(request, getExecutor(), listener);
    }

    @Override
    public int captureBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        return mCameraCaptureSessionCompat.captureBurstRequests(requests, getExecutor(), listener);
    }

    @Override
    public int setSingleRepeatingRequest(
            @NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        return mCameraCaptureSessionCompat.setSingleRepeatingRequest(request, getExecutor(),
                listener);
    }

    @Override
    public int setRepeatingBurstRequests(
            @NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        return mCameraCaptureSessionCompat.setRepeatingBurstRequests(requests, getExecutor(),
                listener);
    }

    @Override
    public int captureSingleRequest(@NonNull CaptureRequest request, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat,
                "Need to call openCaptureSession before using this API.");
        return mCameraCaptureSessionCompat.captureSingleRequest(request, executor, listener);
    }

    @Override
    public int captureBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull Executor executor, @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat,
                "Need to call openCaptureSession before using this API.");
        return mCameraCaptureSessionCompat.captureBurstRequests(requests, executor, listener);
    }

    @Override
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull Executor executor, @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat,
                "Need to call openCaptureSession before using this API.");
        return mCameraCaptureSessionCompat.setSingleRepeatingRequest(request, executor, listener);
    }

    @Override
    public int setRepeatingBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull Executor executor, @NonNull CameraCaptureSession.CaptureCallback listener)
            throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat,
                "Need to call openCaptureSession before using this API.");
        return mCameraCaptureSessionCompat.setRepeatingBurstRequests(requests, executor, listener);
    }

    @Override
    public void stopRepeating() throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        mCameraCaptureSessionCompat.toCameraCaptureSession().stopRepeating();
    }

    @Override
    public void abortCaptures() throws CameraAccessException {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        mCameraCaptureSessionCompat.toCameraCaptureSession().abortCaptures();
    }

    @Override
    public void close() {
        Preconditions.checkNotNull(mCameraCaptureSessionCompat, "Need to call openCaptureSession "
                + "before using this API.");
        mCaptureSessionRepository.onCaptureSessionClosing(this);
        mCameraCaptureSessionCompat.toCameraCaptureSession().close();
    }

    @Override
    public void onReady(@NonNull SynchronizedCaptureSession session) {
        mCaptureSessionStateCallback.onReady(session);
    }

    @Override
    public void onActive(@NonNull SynchronizedCaptureSession session) {
        mCaptureSessionStateCallback.onActive(session);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCaptureQueueEmpty(@NonNull SynchronizedCaptureSession session) {
        mCaptureSessionStateCallback.onCaptureQueueEmpty(session);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSurfacePrepared(@NonNull SynchronizedCaptureSession session,
            @NonNull Surface surface) {
        mCaptureSessionStateCallback.onSurfacePrepared(session, surface);
    }

    @Override
    public void onConfigured(@NonNull SynchronizedCaptureSession session) {
        mCaptureSessionRepository.onCaptureSessionCreated(this);
        mCaptureSessionStateCallback.onConfigured(session);
    }

    @Override
    public void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {
        mCaptureSessionRepository.onCaptureSessionConfigureFail(this);
        mCaptureSessionStateCallback.onConfigureFailed(session);
    }

    @Override
    public void onClosed(@NonNull SynchronizedCaptureSession session) {
        ListenableFuture<Void> openFuture = null;
        synchronized (mLock) {
            if (!mClosed) {
                mClosed = true;
                Preconditions.checkNotNull(mOpenCaptureSessionFuture,
                        "Need to call openCaptureSession before using this API.");
                // Only callback onClosed after the capture session is configured.
                openFuture = mOpenCaptureSessionFuture;
            }
        }
        if (openFuture != null) {
            openFuture.addListener(() -> {
                // Set the CaptureSession closed before invoke the state callback.
                mCaptureSessionRepository.onCaptureSessionClosed(
                        SynchronizedCaptureSessionBaseImpl.this);
                mCaptureSessionStateCallback.onClosed(session);
            }, CameraXExecutors.directExecutor());
        }
    }

    private void debugLog(String message) {
        Logger.d(TAG, "[" + SynchronizedCaptureSessionBaseImpl.this + "] " + message);
    }
}
