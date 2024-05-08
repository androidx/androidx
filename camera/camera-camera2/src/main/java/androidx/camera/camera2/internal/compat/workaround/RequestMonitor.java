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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.Camera2CaptureCallbacks;
import androidx.camera.camera2.internal.compat.quirk.CaptureNoResponseQuirk;
import androidx.camera.camera2.internal.compat.quirk.CaptureSessionStuckQuirk;
import androidx.camera.camera2.internal.compat.quirk.IncorrectCaptureStateQuirk;
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
 * Monitors in-flight capture sequences on devices with specific quirks.
 *
 * <p>Quirks on Certain Devices:
 * <p>Some devices may fail to configure new CameraCaptureSessions
 * if existing in-flight capture sequences haven't completed. This class helps you work around
 * these issues.
 * <p>Single capture requests may not receive a response if they are submitted
 * simultaneously with repeating capture requests. Single capture requests fail to receive a
 * response approximately 10% of the time when submitted within milliseconds of a repeating
 * capture request.
 *
 * <p>How it works: Use `RequestMonitor#getRequestsProcessedFuture()` to get a ListenableFuture.
 * This future signals when all in-flight capture sequences have been processed.
 *
 * @see CaptureNoResponseQuirk
 * @see CaptureSessionStuckQuirk
 * @see IncorrectCaptureStateQuirk
 */
public class RequestMonitor {

    private static final String TAG = "RequestMonitor";
    private final boolean mQuirkEnabled;
    private final List<ListenableFuture<Void>> mRequestTasks =
            Collections.synchronizedList(new ArrayList<>());

    /** Constructor of the RequestMonitor */
    public RequestMonitor(boolean quirkEnabled) {
        mQuirkEnabled = quirkEnabled;
    }

    /**
     * Indicates whether capture sequence monitoring is enabled.
     *
     * <p>Returns true if a quirk is enabled that necessitates tracking in-flight capture requests.
     * Returns false otherwise.
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

    /**
     * Creates a listener that monitors request completion for the `RequestMonitor`.
     *
     * <p>This listener should be assigned to the CameraCaptureSession via
     * the `setSingleRepeatingRequest` or `captureBurstRequests` method to track when submitted
     * requests are fully processed.
     * The `RequestMonitor` can then use this information to ensure proper capture sequence
     * handling.
     *
     * <p>Note: the created listener wraps the provided `originalListener`, ensuring any original
     * capture callbacks still function as intended.
     *
     * @param originalListener The original CaptureCallback to combine with monitoring
     *                         functionality.
     * @return A new CaptureCallback that includes request completion tracking for the
     * `RequestMonitor`.
     */
    @ExecutedBy("mExecutor")
    @NonNull
    public CameraCaptureSession.CaptureCallback createMonitorListener(
            @NonNull CameraCaptureSession.CaptureCallback originalListener) {
        if (shouldMonitorRequest()) {
            return Camera2CaptureCallbacks.createComboCallback(createMonitorListener(),
                    originalListener);
        } else {
            return originalListener;
        }
    }

    private CameraCaptureSession.CaptureCallback createMonitorListener() {
        RequestCompleteListener completeListener = new RequestCompleteListener();
        ListenableFuture<Void> future = completeListener.mStartRequestFuture;

        mRequestTasks.add(future);
        Log.d(TAG, "RequestListener " + completeListener + " monitoring " + this);
        future.addListener(() -> {
            Log.d(TAG, "RequestListener " + completeListener + " done " + this);
            mRequestTasks.remove(future);
        }, CameraXExecutors.directExecutor());
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
}
