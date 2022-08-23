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

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.Node;

import com.google.auto.value.AutoValue;

import java.util.concurrent.Executor;

/**
 * Processes a single image and invokes {@link TakePictureCallback}.
 *
 * <p>This node performs operations that runs on a single image, such as cropping, format
 * conversion, effects and/or saving to disk.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ProcessingNode implements Node<ProcessingNode.In, Void> {

    @NonNull
    private final Executor mBlockingExecutor;

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     */
    ProcessingNode(@NonNull Executor blockingExecutor) {
        mBlockingExecutor = blockingExecutor;
    }

    @NonNull
    @Override
    public Void transform(@NonNull ProcessingNode.In inputEdge) {
        // Listen to the input edge.
        inputEdge.getEdge().setListener(
                inputPacket -> mBlockingExecutor.execute(() -> processInputPacket(inputPacket)));
        // No output. The request callback will be invoked to deliver the final result.
        return null;
    }

    @Override
    public void release() {
    }

    /**
     * Processes an {@link InputPacket} and delivers the result to {@link TakePictureManager}.
     *
     * TODO: implement this method.
     */
    @WorkerThread
    void processInputPacket(@NonNull InputPacket inputPacket) {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        ImageProxy image = inputPacket.getImageProxy();
        if (inputPacket.getProcessingRequest().isInMemoryCapture()) {
            mainThreadExecutor().execute(() -> request.onFinalResult(image));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Input packet which is a combination of camera frame and processing request.
     */
    @AutoValue
    abstract static class InputPacket {

        @NonNull
        abstract ProcessingRequest getProcessingRequest();

        @NonNull
        abstract ImageProxy getImageProxy();

        static InputPacket of(@NonNull ProcessingRequest processingRequest,
                @NonNull ImageProxy imageProxy) {
            return new AutoValue_ProcessingNode_InputPacket(processingRequest, imageProxy);
        }
    }

    /**
     * Input edges of {@link ProcessingNode}.
     */
    @AutoValue
    abstract static class In {

        /**
         * Get the single input edge that contains a {@link InputPacket} flow.
         */
        abstract Edge<InputPacket> getEdge();

        /**
         * Gets the format of the image in {@link InputPacket}.
         */
        abstract int getFormat();

        static In of(int format) {
            return new AutoValue_ProcessingNode_In(new Edge<>(), format);
        }
    }
}
