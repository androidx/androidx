/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CaptureCallbacks;
import androidx.camera.camera2.internal.SynchronizedCaptureSession;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionStuckQuirk;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * The workaround is used to wait for the other CameraCaptureSessions to complete their in-flight
 * capture sequences before opening the current session.
 * <p>If it tries to open the CameraCaptureSession before the others to complete their in-flight
 * capture sequences, the current session may fail to be configured.
 *
 * @see CaptureSessionStuckQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class WaitForRepeatingRequestStart {
    private final boolean mHasCaptureSessionStuckQuirk;
    private final Object mLock = new Object();

    @NonNull
    private final ListenableFuture<Void> mStartStreamingFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mStartStreamingCompleter;
    /** Whether the capture session has submitted the repeating request. */
    private boolean mHasSubmittedRepeating;

    /** Constructor of the WaitForRepeatingRequestStart workaround */
    public WaitForRepeatingRequestStart(@NonNull Quirks cameraQuirks) {
        mHasCaptureSessionStuckQuirk = cameraQuirks.contains(CaptureSessionStuckQuirk.class);

        if (shouldWaitRepeatingSubmit()) {
            mStartStreamingFuture = CallbackToFutureAdapter.getFuture(completer -> {
                mStartStreamingCompleter = completer;
                return "WaitForRepeatingRequestStart[" + this + "]";
            });
        } else {
            mStartStreamingFuture = Futures.immediateFuture(null);
        }
    }

    /**
     * Return true if the opening of the session should wait for the other CameraCaptureSessions
     * to complete their in-flight capture sequences before opening the current session.
     */
    public boolean shouldWaitRepeatingSubmit() {
        return mHasCaptureSessionStuckQuirk;
    }

    /** Returns a ListenableFuture to indicate whether the start repeating request is done. */
    @NonNull
    public ListenableFuture<Void> getStartStreamFuture() {
        return Futures.nonCancellationPropagating(mStartStreamingFuture);
    }

    /**
     * For b/146773463: It needs to check all the releasing capture sessions are ready for
     * opening next capture session.
     */
    @NonNull
    public ListenableFuture<Void> openCaptureSession(
            @NonNull CameraDevice cameraDevice,
            @NonNull SessionConfigurationCompat sessionConfigurationCompat,
            @NonNull List<DeferrableSurface> deferrableSurfaces,
            @NonNull List<SynchronizedCaptureSession> closingSessions,
            @NonNull OpenCaptureSession openCaptureSession) {
        List<ListenableFuture<Void>> futureList = new ArrayList<>();
        for (SynchronizedCaptureSession session : closingSessions) {
            futureList.add(session.getOpeningBlocker());
        }

        return FutureChain.from(Futures.successfulAsList(futureList)).transformAsync(
                v -> openCaptureSession.run(cameraDevice, sessionConfigurationCompat,
                        deferrableSurfaces), CameraXExecutors.directExecutor());
    }

    /** Hook the setSingleRepeatingRequest() to know if it has started a repeating request. */
    public int setSingleRepeatingRequest(
            @NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener,
            @NonNull SingleRepeatingRequest singleRepeatingRequest)
            throws CameraAccessException {
        synchronized (mLock) {
            if (shouldWaitRepeatingSubmit()) {
                listener = Camera2CaptureCallbacks.createComboCallback(mCaptureCallback, listener);
                mHasSubmittedRepeating = true;
            }
            return singleRepeatingRequest.run(request, listener);
        }
    }

    /** This should be called when a SynchronizedCaptureSession is stopped or closed. */
    public void onSessionEnd() {
        synchronized (mLock) {
            if (shouldWaitRepeatingSubmit() && !mHasSubmittedRepeating) {
                // If the session is closed before any repeating requests have been issued,
                // then the startStreamingFuture should be cancelled.
                mStartStreamingFuture.cancel(true);
            }
        }
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

    /** Interface to forward call of the setSingleRepeatingRequest() method. */
    @FunctionalInterface
    public interface SingleRepeatingRequest {
        /** Run the setSingleRepeatingRequest() method. */
        int run(@NonNull CaptureRequest request,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;
    }

    /** Interface to forward call of the openCaptureSession() method. */
    @FunctionalInterface
    public interface OpenCaptureSession {
        /** Run the openCaptureSession() method. */
        @NonNull
        ListenableFuture<Void> run(@NonNull CameraDevice cameraDevice,
                @NonNull SessionConfigurationCompat sessionConfigurationCompat,
                @NonNull List<DeferrableSurface> deferrableSurfaces);
    }
}
