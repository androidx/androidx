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
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessorSurface;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Camera2 implementation of {@link RequestProcessor} which offers the capability of
 * sending capture request by a {@link RequestProcessor.Request} consisting of target output
 * config id, parameters and template.
 *
 * <p>The {@link CaptureSession} is used for sending capture requests.
 * The {@link SessionProcessorSurface} contains the output config id. The target output config ids
 * specified in {@link RequestProcessor.Request}s can be mapped to the target Surface by checking
 * the {@link SessionProcessorSurface#getOutputConfigId()}.
 *
 * <p>{@link #close()} is expected to be called before capture session is closed. All methods will
 * be no-op once {@link #close()} is invoked.
 *
 * <p>This class is thread-safe. It is safe to invoke methods of {@link Camera2RequestProcessor}
 * from any threads.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Camera2RequestProcessor implements RequestProcessor {
    private static final String TAG = "Camera2RequestProcessor";
    private final CaptureSession mCaptureSession;
    private final List<SessionProcessorSurface> mProcessorSurfaces;
    private volatile boolean mIsClosed = false;

    public Camera2RequestProcessor(@NonNull CaptureSession captureSession,
            @NonNull List<SessionProcessorSurface> processorSurfaces) {
        Preconditions.checkArgument(captureSession.mState == CaptureSession.State.OPENED,
                "CaptureSession state must be OPENED. Current state:" + captureSession.mState);
        mCaptureSession = captureSession;
        mProcessorSurfaces =  Collections.unmodifiableList(new ArrayList<>(processorSurfaces));
    }

    /**
     * After close(), all submit / setRepeating will be disabled.
     */
    public void close() {
        mIsClosed = true;
    }

    private boolean areRequestsValid(@NonNull List<RequestProcessor.Request> requests) {
        for (Request request : requests) {
            if (!isRequestValid(request)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRequestValid(@NonNull RequestProcessor.Request request) {
        if (request.getTargetOutputConfigIds().isEmpty()) {
            Logger.e(TAG, "Unable to submit the RequestProcessor.Request: "
                    + "empty targetOutputConfigIds");
            return false;
        }
        for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
            if (findSurface(outputConfigId) == null) {
                Logger.e(TAG, "Unable to submit the RequestProcessor.Request: "
                        + "targetOutputConfigId(" + outputConfigId + ") is not a valid id");
                return false;
            }
        }

        return true;
    }

    @Override
    public int submit(
            @NonNull RequestProcessor.Request request,
            @NonNull RequestProcessor.Callback callback) {
        return submit(Arrays.asList(request), callback);
    }

    @Override
    public int submit(
            @NonNull List<RequestProcessor.Request> requests,
            @NonNull RequestProcessor.Callback callback) {
        if (mIsClosed || !areRequestsValid(requests)) {
            return -1;
        }

        ArrayList<CaptureConfig> captureConfigs = new ArrayList<>();
        boolean shouldInvokeSequenceCallback = true;
        for (RequestProcessor.Request request : requests) {
            CaptureConfig.Builder builder = new CaptureConfig.Builder();
            builder.setTemplateType(request.getTemplateId());
            builder.setImplementationOptions(request.getParameters());
            builder.addCameraCaptureCallback(
                    CaptureCallbackContainer.create(
                            new Camera2CallbackWrapper(request, callback,
                                    shouldInvokeSequenceCallback)));
            // Only invoke the sequence callback on the first callback wrapper to avoid
            // duplicate calls on this RequestProcessor.Callback.
            shouldInvokeSequenceCallback = false;

            for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
                builder.addSurface(findSurface(outputConfigId));
            }
            captureConfigs.add(builder.build());
        }
        return mCaptureSession.issueBurstCaptureRequest(captureConfigs);
    }

    @Override
    public int setRepeating(
            @NonNull RequestProcessor.Request request,
            @NonNull RequestProcessor.Callback callback) {
        if (mIsClosed || !isRequestValid(request)) {
            return -1;
        }

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
        sessionConfigBuilder.setTemplateType(request.getTemplateId());
        sessionConfigBuilder.setImplementationOptions(request.getParameters());
        sessionConfigBuilder.addCameraCaptureCallback(CaptureCallbackContainer.create(
                new Camera2CallbackWrapper(request, callback, true)));

        for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
            sessionConfigBuilder.addSurface(findSurface(outputConfigId));
        }

        return mCaptureSession.issueRepeatingCaptureRequests(sessionConfigBuilder.build());
    }

    @Override
    public void abortCaptures() {
        if (mIsClosed) {
            return;
        }
        mCaptureSession.abortCaptures();
    }

    @Override
    public void stopRepeating() {
        if (mIsClosed) {
            return;
        }
        mCaptureSession.stopRepeating();
    }

    /**
     * A wrapper for redirect camera2 CameraCaptureSession.CaptureCallback to the
     * {@link RequestProcessor.Callback}. Due to the CaptureSession design, each request has
     * its own CaptureCallback which could lead to onCaptureSequenceCompleted() being called
     * multiple times. Thus the parameter invokeSequenceCallback is added for specifying which
     * CaptureCallback should redirect onCaptureSequenceCompleted and onCaptureSequenceAborted to
     * {@link RequestProcessor.Callback} to avoid duplicate invoking.
     */
    private class Camera2CallbackWrapper extends CameraCaptureSession.CaptureCallback {
        private final RequestProcessor.Callback mCallback;
        private final RequestProcessor.Request mRequest;
        private final boolean mInvokeSequenceCallback;

        Camera2CallbackWrapper(@NonNull RequestProcessor.Request captureRequest,
                @NonNull RequestProcessor.Callback callback, boolean invokeSequenceCallback) {
            mCallback = callback;
            mRequest = captureRequest;
            mInvokeSequenceCallback = invokeSequenceCallback;
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            mCallback.onCaptureStarted(mRequest, frameNumber, timestamp);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mCallback.onCaptureProgressed(mRequest, new Camera2CameraCaptureResult(partialResult));
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            mCallback.onCaptureCompleted(mRequest, new Camera2CameraCaptureResult(result));
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            mCallback.onCaptureFailed(mRequest, new Camera2CameraCaptureFailure(
                    CameraCaptureFailure.Reason.ERROR, failure));
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                int sequenceId, long frameNumber) {
            if (mInvokeSequenceCallback) {
                mCallback.onCaptureSequenceCompleted(sequenceId, frameNumber);
            }
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session,
                int sequenceId) {
            if (mInvokeSequenceCallback) {
                mCallback.onCaptureSequenceAborted(sequenceId);
            }
        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            mCallback.onCaptureBufferLost(mRequest, frameNumber,
                    findOutputConfigId(target));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int findOutputConfigId(@NonNull Surface surface) {
        for (SessionProcessorSurface sessionProcessorSurface : mProcessorSurfaces) {
            try {
                if (sessionProcessorSurface.getSurface().get() == surface) {
                    return sessionProcessorSurface.getOutputConfigId();
                }
            } catch (InterruptedException | ExecutionException e) {
                // This will not happen since SessionProcessorSurface.get() will always
                // succeed.
            }
        }

        return -1;
    }

    @Nullable
    private DeferrableSurface findSurface(int outputConfigId) {
        for (SessionProcessorSurface sessionProcessorSurface : mProcessorSurfaces) {
            if (sessionProcessorSurface.getOutputConfigId() == outputConfigId) {
                return sessionProcessorSurface;
            }
        }
        return null;
    }
}
