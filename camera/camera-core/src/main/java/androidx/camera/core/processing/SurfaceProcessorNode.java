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

package androidx.camera.core.processing;

import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRect;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRectF;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.singletonList;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

/**
 * A {@link Node} implementation that wraps around the public {@link SurfaceProcessor} interface.
 *
 * <p>Responsibilities:
 * <ul>
 * <li>Calculating transformation and passing it to the {@link SurfaceProcessor}.
 * <li>Tracking the state of previously calculate specification and only recreate the pipeline
 * when necessary.
 * </ul>
 */
@RequiresApi(api = 21)
// TODO(b/233627260): remove once implemented.
@SuppressWarnings("UnusedVariable")
public class SurfaceProcessorNode implements Node<SurfaceEdge, SurfaceEdge> {

    @NonNull
    final SurfaceProcessorInternal mSurfaceProcessor;
    @NonNull
    final CameraInternal mCameraInternal;
    // Guarded by main thread.
    @Nullable
    private SurfaceEdge mOutputEdge;
    @Nullable
    private SurfaceEdge mInputEdge;

    /**
     * Constructs the {@link SurfaceProcessorNode}.
     *
     * @param cameraInternal   the associated camera instance.
     * @param surfaceProcessor the interface to wrap around.
     */
    public SurfaceProcessorNode(@NonNull CameraInternal cameraInternal,
            @NonNull SurfaceProcessorInternal surfaceProcessor) {
        mCameraInternal = cameraInternal;
        mSurfaceProcessor = surfaceProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    @MainThread
    public SurfaceEdge transform(@NonNull SurfaceEdge inputEdge) {
        Threads.checkMainThread();
        checkArgument(inputEdge.getSurfaces().size() == 1,
                "Multiple input stream not supported yet.");
        mInputEdge = inputEdge;
        SettableSurface inputSurface = inputEdge.getSurfaces().get(0);
        SettableSurface outputSurface = createOutputSurface(inputSurface);
        sendSurfacesToProcessorWhenReady(inputSurface, outputSurface);
        mOutputEdge = SurfaceEdge.create(singletonList(outputSurface));
        return mOutputEdge;
    }

    @NonNull
    private SettableSurface createOutputSurface(@NonNull SettableSurface inputSurface) {
        // TODO: Can be improved by only restarting part of the pipeline. e.g. only update the
        //  output Surface (between Effect/App), and still use the same input Surface (between
        //  Camera/Effect). It's just simpler for now.
        final Runnable onSurfaceInvalidated = inputSurface::invalidate;

        SettableSurface outputSurface;
        Size resolution = inputSurface.getSize();
        Rect cropRect = inputSurface.getCropRect();
        int rotationDegrees = inputSurface.getRotationDegrees();
        boolean mirroring = inputSurface.getMirroring();

        // Calculate rotated resolution and cropRect
        Size rotatedCroppedSize = is90or270(rotationDegrees)
                ? new Size(/*width=*/cropRect.height(), /*height=*/cropRect.width())
                : rectToSize(cropRect);

        // Calculate sensorToBufferTransform
        android.graphics.Matrix sensorToBufferTransform =
                new android.graphics.Matrix(inputSurface.getSensorToBufferTransform());
        android.graphics.Matrix imageTransform = getRectToRect(sizeToRectF(resolution),
                new RectF(cropRect), rotationDegrees, mirroring);
        sensorToBufferTransform.postConcat(imageTransform);

        outputSurface = new SettableSurface(
                inputSurface.getTargets(),
                rotatedCroppedSize,
                inputSurface.getFormat(),
                sensorToBufferTransform,
                // The Surface transform cannot be carried over during buffer copy.
                /*hasEmbeddedTransform=*/false,
                sizeToRect(rotatedCroppedSize),
                /*rotationDegrees=*/0,
                /*mirroring=*/false,
                onSurfaceInvalidated);

        return outputSurface;
    }

    private void sendSurfacesToProcessorWhenReady(@NonNull SettableSurface input,
            @NonNull SettableSurface output) {
        SurfaceRequest surfaceRequest = input.createSurfaceRequest(mCameraInternal);
        setupRotationUpdates(
                surfaceRequest,
                output,
                input.getMirroring(),
                input.getRotationDegrees());
        Futures.addCallback(output.createSurfaceOutputFuture(input.getSize(), input.getCropRect(),
                        input.getRotationDegrees(), input.getMirroring()),
                new FutureCallback<SurfaceOutput>() {
                    @Override
                    public void onSuccess(@Nullable SurfaceOutput surfaceOutput) {
                        Preconditions.checkNotNull(surfaceOutput);
                        mSurfaceProcessor.onOutputSurface(surfaceOutput);
                        mSurfaceProcessor.onInputSurface(surfaceRequest);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        // Do not send surfaces to the processor if the downstream provider (e.g.
                        // the app) fails to provide a Surface. Instead, notify the consumer that
                        // the Surface will not be provided.
                        surfaceRequest.willNotProvideSurface();
                    }
                }, mainThreadExecutor());
    }

    /**
     * Propagates rotation updates from the input edge to the output edge.
     *
     * <p>Transformation info, such as rotation and crop rect, can be updated after the
     * connection is established. When that happens, the node should update the output
     * transformation via e.g. {@link SurfaceRequest#updateTransformationInfo} without recreating
     * the pipeline.
     *
     * <p>Currently, we only propagates the rotation. When the
     * input edge's rotation changes, we re-calculate the delta and notify the output edge.
     *
     * @param inputSurfaceRequest {@link SurfaceRequest} of the input edge.
     * @param outputSurface       {@link SettableSurface} of the output edge.
     * @param mirrored            whether the node mirrors the buffer.
     * @param rotatedDegrees      how much the node rotates the buffer.
     */
    void setupRotationUpdates(
            @NonNull SurfaceRequest inputSurfaceRequest,
            @NonNull SettableSurface outputSurface,
            boolean mirrored,
            int rotatedDegrees) {
        inputSurfaceRequest.setTransformationInfoListener(mainThreadExecutor(), info -> {
            // To obtain the rotation degrees delta, the rotation performed by the node must be
            // eliminated.
            int rotationDegrees = info.getRotationDegrees() - rotatedDegrees;
            if (mirrored) {
                rotationDegrees = -rotationDegrees;
            }
            outputSurface.setRotationDegrees(within360(rotationDegrees));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        mSurfaceProcessor.release();
        mainThreadExecutor().execute(() -> {
            if (mOutputEdge != null) {
                for (SettableSurface surface : mOutputEdge.getSurfaces()) {
                    // The output DeferrableSurface will later be terminated by the processor.
                    surface.close();
                }
            }
        });
    }
}
