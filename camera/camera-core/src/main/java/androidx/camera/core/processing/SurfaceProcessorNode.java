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

import static androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED;
import static androidx.camera.core.impl.utils.Threads.runOnMain;
import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.camera.core.impl.utils.TransformUtils.isAspectRatioMatchingWithRoundingError;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRect;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRectF;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.core.processing.TargetUtils.getHumanReadableName;
import static androidx.core.util.Preconditions.checkArgument;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.Logger;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.util.OutConfig;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * A {@link Node} implementation that wraps around the public {@link SurfaceProcessor} interface.
 *
 * <p>Responsibilities:
 * <ul>
 * <li>Calculating transformation and passing it to the {@link SurfaceProcessor}.
 * <li>Tracking the state of previously calculate specification and only recreate the pipeline
 * when necessary.
 * </ul>
 *
 * TODO(b/261270972): currently the upstream pipeline is always connected, which means that the
 *  camera is always producing frames. This might be wasteful, if the downstream pipeline is not
 *  connected. For example, when app fails to provide a Surface or when VideoCapture is paused.
 *  One possible optimization is only connecting the upstream when the downstream are available.
 */
// TODO(b/233627260): remove once implemented.
@SuppressWarnings("UnusedVariable")
public class SurfaceProcessorNode implements
        Node<SurfaceProcessorNode.In, SurfaceProcessorNode.Out> {

    private static final String TAG = "SurfaceProcessorNode";

    @NonNull
    final SurfaceProcessorInternal mSurfaceProcessor;
    @NonNull
    final CameraInternal mCameraInternal;
    // Guarded by main thread.
    @Nullable
    private Out mOutput;
    @Nullable
    private In mInput;

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
    public Out transform(@NonNull In input) {
        Threads.checkMainThread();
        mInput = input;
        mOutput = new Out();

        SurfaceEdge inputSurface = input.getSurfaceEdge();
        for (OutConfig config : input.getOutConfigs()) {
            mOutput.put(config, transformSingleOutput(inputSurface, config));
        }

        sendSurfaceRequest(inputSurface);
        sendSurfaceOutputs(inputSurface, mOutput);
        setUpRotationUpdates(inputSurface, mOutput);
        return mOutput;
    }

    @NonNull
    private SurfaceEdge transformSingleOutput(@NonNull SurfaceEdge input,
            @NonNull OutConfig outConfig) {
        SurfaceEdge outputSurface;
        Rect cropRect = outConfig.getCropRect();
        int rotationDegrees = outConfig.getRotationDegrees();
        boolean mirroring = outConfig.isMirroring();

        // Calculate sensorToBufferTransform
        android.graphics.Matrix sensorToBufferTransform =
                new android.graphics.Matrix(input.getSensorToBufferTransform());
        android.graphics.Matrix newTransform = getRectToRect(
                new RectF(cropRect),
                sizeToRectF(outConfig.getSize()), rotationDegrees, mirroring);
        sensorToBufferTransform.postConcat(newTransform);

        // The aspect ratio of the output must match the aspect ratio of the crop rect. Otherwise
        // the output will be stretched.
        Size rotatedCropSize = getRotatedSize(cropRect, rotationDegrees);
        checkArgument(isAspectRatioMatchingWithRoundingError(rotatedCropSize, outConfig.getSize()));

        // Calculate the transformed crop rect.
        Rect newCropRect;
        if (outConfig.shouldRespectInputCropRect()) {
            checkArgument(outConfig.getCropRect().contains(input.getCropRect()),
                    String.format("Output crop rect %s must contain input crop rect %s",
                            outConfig.getCropRect(), input.getCropRect()));
            newCropRect = new Rect();
            RectF newCropRectF = new RectF(input.getCropRect());
            newTransform.mapRect(newCropRectF);
            newCropRectF.round(newCropRect);
        } else {
            newCropRect = sizeToRect(outConfig.getSize());
        }

        // Copy the stream spec from the input to the output, except for the resolution.
        StreamSpec streamSpec = input.getStreamSpec().toBuilder().setResolution(
                outConfig.getSize()).build();

        outputSurface = new SurfaceEdge(
                outConfig.getTargets(),
                outConfig.getFormat(),
                streamSpec,
                sensorToBufferTransform,
                // The Surface transform cannot be carried over during buffer copy.
                /*hasCameraTransform=*/false,
                newCropRect,
                /*rotationDegrees=*/input.getRotationDegrees() - rotationDegrees,
                // Once copied, the target rotation is no longer useful.
                /*targetRotation*/ ROTATION_NOT_SPECIFIED,
                /*mirroring=*/input.isMirroring() != mirroring);

        return outputSurface;
    }

    /**
     * Creates {@link SurfaceRequest} and send it to {@link SurfaceProcessor}.
     */
    private void sendSurfaceRequest(@NonNull SurfaceEdge input) {
        try {
            mSurfaceProcessor.onInputSurface(input.createSurfaceRequest(mCameraInternal));
        } catch (ProcessingException e) {
            Logger.e(TAG, "Failed to send SurfaceRequest to SurfaceProcessor.", e);
        }
    }

    /**
     * Creates all {@link SurfaceOutput} and send them to {@link SurfaceProcessor}.
     */
    private void sendSurfaceOutputs(@NonNull SurfaceEdge input,
            @NonNull Map<OutConfig, SurfaceEdge> outputs) {
        for (Map.Entry<OutConfig, SurfaceEdge> output : outputs.entrySet()) {
            createAndSendSurfaceOutput(input, output);
            // Send the new surface to SurfaceProcessor when it resets.
            output.getValue().addOnInvalidatedListener(
                    () -> createAndSendSurfaceOutput(input, output));
        }
    }

    /**
     * Creates a single {@link SurfaceOutput} and send it to {@link SurfaceProcessor}.
     */
    private void createAndSendSurfaceOutput(@NonNull SurfaceEdge input,
            Map.Entry<OutConfig, SurfaceEdge> output) {
        SurfaceEdge outputEdge = output.getValue();
        SurfaceOutput.CameraInputInfo cameraInputInfo = SurfaceOutput.CameraInputInfo.of(
                input.getStreamSpec().getResolution(),
                output.getKey().getCropRect(),
                input.hasCameraTransform() ? mCameraInternal : null,
                output.getKey().getRotationDegrees(),
                output.getKey().isMirroring());
        ListenableFuture<SurfaceOutput> future = outputEdge.createSurfaceOutputFuture(
                output.getKey().getFormat(),
                cameraInputInfo,
                null);
        Futures.addCallback(future, new FutureCallback<SurfaceOutput>() {
            @Override
            public void onSuccess(@Nullable SurfaceOutput output) {
                Preconditions.checkNotNull(output);
                try {
                    mSurfaceProcessor.onOutputSurface(output);
                } catch (ProcessingException e) {
                    Logger.e(TAG, "Failed to send SurfaceOutput to SurfaceProcessor.", e);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (outputEdge.getTargets() == CameraEffect.VIDEO_CAPTURE
                        && t instanceof CancellationException) {
                    Logger.d(TAG, "Downstream VideoCapture failed to provide Surface.");
                } else {
                    Logger.w(TAG, "Downstream node failed to provide Surface. Target: "
                            + getHumanReadableName(outputEdge.getTargets()), t);
                }
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
     * @param inputEdge the input edge.
     * @param outputs   the output edges.
     */
    void setUpRotationUpdates(
            @NonNull SurfaceEdge inputEdge,
            @NonNull Map<OutConfig, SurfaceEdge> outputs) {
        inputEdge.addTransformationUpdateListener(info -> {
            for (Map.Entry<OutConfig, SurfaceEdge> output : outputs.entrySet()) {
                // To obtain the rotation degrees delta, the rotation performed by the node must be
                // eliminated.
                int rotationDegrees =
                        info.getRotationDegrees() - output.getKey().getRotationDegrees();
                if (output.getKey().isMirroring()) {
                    // The order of transformation is cropping -> rotation -> mirroring. To
                    // change the rotation, one must consider the mirroring.
                    rotationDegrees = -rotationDegrees;
                }
                rotationDegrees = within360(rotationDegrees);
                // Once copied, the target rotation is no longer useful.
                output.getValue().updateTransformation(rotationDegrees, ROTATION_NOT_SPECIFIED);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        mSurfaceProcessor.release();
        // Required for b/309409701. For some reason, the cleanup posted on {@link #release()} is
        // not executed in unit tests which causes failures.
        runOnMain(() -> {
            if (mOutput != null) {
                for (SurfaceEdge surface : mOutput.values()) {
                    // The output DeferrableSurface will later be terminated by the processor.
                    surface.close();
                }
            }
        });
    }

    /**
     * Gets the {@link SurfaceProcessorInternal} used by this node.
     */
    @NonNull
    public SurfaceProcessorInternal getSurfaceProcessor() {
        return mSurfaceProcessor;
    }

    /**
     * The input of a {@link SurfaceProcessorNode}.
     */
    @AutoValue
    public abstract static class In {

        /**
         * Gets the input stream.
         *
         * <p> {@link SurfaceProcessorNode} only supports a single input stream.
         */
        @NonNull
        public abstract SurfaceEdge getSurfaceEdge();

        /**
         * Gets the config for generating output streams.
         *
         * <p>{@link SurfaceProcessorNode#transform} creates one {@link SurfaceEdge} per
         * {@link OutConfig} in this list.
         */
        @SuppressWarnings("AutoValueImmutableFields")
        @NonNull
        public abstract List<OutConfig> getOutConfigs();

        /**
         * Creates a {@link In} instance.
         */
        @NonNull
        public static In of(@NonNull SurfaceEdge edge, @NonNull List<OutConfig> configs) {
            return new AutoValue_SurfaceProcessorNode_In(edge, configs);
        }
    }

    /**
     * The output of a {@link SurfaceProcessorNode}.
     *
     * <p>A map of {@link OutConfig} with their corresponding {@link SurfaceEdge}.
     */
    public static class Out extends HashMap<OutConfig, SurfaceEdge> {
    }
}
