/*
 * Copyright 2023 The Android Open Source Project
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
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CaptureCallbacks;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionStuckQuirk;
import androidx.camera.camera2.internal.compat.quirk.IncorrectCaptureStateQuirk;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Tracking in-flight capture sequences of a CameraCaptureSession if certain Quirks are enabled.
 *
 * <p>If you try to open a new CameraCaptureSession before the existing CameraCaptureSession
 * processes its in-flight capture sequences on certain devices, the new session may fail to be
 * configured. To track the status of in-flight capture sequences, use the
 * RequestMonitor#getRequestsProcessedFuture() method. This method returns a ListenableFuture that
 * indicates when all in-flight capture sequences have been processed.
 *
 * @see CaptureSessionStuckQuirk
 * @see IncorrectCaptureStateQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class RequestMonitor {
    private final boolean mQuirkEnabled;
    private final List<ListenableFuture<Void>> mRequestTasks =
            Collections.synchronizedList(new ArrayList<>());

    /** Constructor of the RequestMonitor */
    public RequestMonitor(@NonNull Quirks cameraQuirks) {
        mQuirkEnabled = cameraQuirks.contains(CaptureSessionStuckQuirk.class)
                || cameraQuirks.contains(IncorrectCaptureStateQuirk.class);
    }

    /**
     * Return true if the opening of the session should wait for the other CameraCaptureSessions
     * to complete their in-flight capture sequences before opening the current session.
     */
    public boolean shouldMonitorRequest() {
        return mQuirkEnabled;
    }

    /**
     * Returns a ListenableFuture that indicates whether all capture requests have been
     * processed.
     */
    @ExecutedBy("mExecutor")
    @NonNull
    public ListenableFuture<Void> getRequestsProcessedFuture() {
        if (mRequestTasks.isEmpty()) {
            return Futures.immediateFuture(null);
        }

        return Futures.nonCancellationPropagating(
                Futures.transform(Futures.successfulAsList(new ArrayList<>(mRequestTasks)),
                        input -> null, CameraXExecutors.directExecutor()));
    }

    /** Hook the setSingleRepeatingRequest() to know if it has started a repeating request. */
    @ExecutedBy("mExecutor")
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull CameraCaptureSession.CaptureCallback listener,
            @NonNull SingleRequest singleRequest) throws CameraAccessException {
        if (shouldMonitorRequest()) {
            listener =
                    Camera2CaptureCallbacks.createComboCallback(createMonitorListener(), listener);
        }
        return singleRequest.run(request, listener);
    }

    /** Hook the captureBurstRequests() to know if it has started the requests. */
    @ExecutedBy("mExecutor")
    public int captureBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull CameraCaptureSession.CaptureCallback listener,
            @NonNull MultiRequest multiRequest) throws CameraAccessException {
        if (shouldMonitorRequest()) {
            listener =
                    Camera2CaptureCallbacks.createComboCallback(createMonitorListener(), listener);
        }
        return multiRequest.run(requests, listener);
    }

    private CameraCaptureSession.CaptureCallback createMonitorListener() {
        RequestCompleteListener completeListener = new RequestCompleteListener();
        ListenableFuture<Void> future = completeListener.mStartRequestFuture;

        mRequestTasks.add(future);
        future.addListener(() -> mRequestTasks.remove(future), CameraXExecutors.directExecutor());
        return completeListener;
    }

    /** This should be called when a SynchronizedCaptureSession is stopped or closed. */
    @ExecutedBy("mExecutor")
    public void stop() {
        LinkedList<ListenableFuture<Void>> tasks = new LinkedList<>(mRequestTasks);
        while (!tasks.isEmpty()) {
            Objects.requireNonNull(tasks.poll()).cancel(true);
        }
    }

    static class RequestCompleteListener extends CameraCaptureSession.CaptureCallback {
        @NonNull
        final ListenableFuture<Void> mStartRequestFuture;
        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        CallbackToFutureAdapter.Completer<Void> mStartRequestCompleter;

        RequestCompleteListener() {
            mStartRequestFuture = CallbackToFutureAdapter.getFuture(completer -> {
                mStartRequestCompleter = completer;
                return "RequestCompleteListener[" + this + "]";
            });
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            completeFuture();
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session,
                int sequenceId) {
            completeFuture();
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            completeFuture();
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            completeFuture();
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                int sequenceId, long frameNumber) {
            completeFuture();
        }

        private void completeFuture() {
            if (mStartRequestCompleter != null) {
                mStartRequestCompleter.set(null);
                mStartRequestCompleter = null;
            }
        }
    }

    /** Interface to forward call of the setSingleRepeatingRequest() method. */
    @FunctionalInterface
    public interface SingleRequest {
        /** Run the setSingleRepeatingRequest() method. */
        int run(@NonNull CaptureRequest request,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;
    }

    /** Interface to forward call of the captureBurstRequests() method. */
    @FunctionalInterface
    public interface MultiRequest {
        /** Run the captureBurstRequests() method. */
        int run(@NonNull List<CaptureRequest> requests,
                @NonNull CameraCaptureSession.CaptureCallback listener)
                throws CameraAccessException;
    }
}
