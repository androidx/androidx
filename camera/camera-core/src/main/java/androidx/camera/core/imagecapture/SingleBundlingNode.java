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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

/**
 * Matches {@link ProcessingRequest} with a single {@link ImageProxy}.
 *
 * <p>This node handles the basic scenarios where there is only one image captured per
 * request.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SingleBundlingNode implements BundlingNode {

    ProcessingRequest mPendingRequest;
    private ProcessingNode.In mOutputEdge;

    @NonNull
    @Override
    public ProcessingNode.In transform(@NonNull CaptureNode.Out captureNodeOut) {
        // Listen to input edges.
        captureNodeOut.getImageEdge().setListener(this::matchImageWithRequest);
        captureNodeOut.getRequestEdge().setListener(this::trackIncomingRequest);
        // Set up output edge.
        mOutputEdge = ProcessingNode.In.of(captureNodeOut.getInputFormat(),
                captureNodeOut.getOutputFormat());
        return mOutputEdge;
    }

    @Override
    public void release() {
        // No-op.
    }

    @MainThread
    private void trackIncomingRequest(@NonNull ProcessingRequest request) {
        checkMainThread();
        checkState(request.getStageIds().size() == 1,
                "Cannot handle multi-image capture.");
        checkState(mPendingRequest == null,
                "Already has an existing request.");
        mPendingRequest = request;

        Futures.addCallback(request.getCaptureFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // Do nothing
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                checkMainThread();
                if (request == mPendingRequest) {
                    mPendingRequest = null;
                }
            }
        }, CameraXExecutors.directExecutor());
    }

    @MainThread
    private void matchImageWithRequest(@NonNull ImageProxy imageProxy) {
        checkMainThread();
        checkState(mPendingRequest != null);
        int stageId = (Integer) requireNonNull(
                imageProxy.getImageInfo().getTagBundle().getTag(
                        mPendingRequest.getTagBundleKey()));
        checkState(stageId == mPendingRequest.getStageIds().get(0));

        mOutputEdge.getEdge().accept(ProcessingNode.InputPacket.of(mPendingRequest, imageProxy));
        mPendingRequest = null;
    }
}
