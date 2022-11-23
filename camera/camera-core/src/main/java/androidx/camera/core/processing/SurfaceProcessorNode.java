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
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRect;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRectF;
import static androidx.camera.core.impl.utils.TransformUtils.within360;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;

import android.graphics.Rect;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.Logger;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        SettableSurface inputSurface = input.getSurfaceEdge();
        for (OutConfig config : input.getOutConfigs()) {
            mOutput.put(config, transformSingleOutput(inputSurface, config));
        }
        sendSurfacesToProcessorWhenReady(inputSurface, mOutput);
        return mOutput;
    }

    @NonNull
    private SettableSurface transformSingleOutput(@NonNull SettableSurface input,
            @NonNull OutConfig outConfig) {
        // TODO: Can be improved by only restarting part of the pipeline. e.g. only update the
        //  output Surface (between Effect/App), and still use the same input Surface (between
        //  Camera/Effect). It's just simpler for now.
        final Runnable onSurfaceInvalidated = input::invalidate;

        SettableSurface outputSurface;
        Size inputSize = input.getSize();
        Rect cropRect = outConfig.getCropRect();
        int rotationDegrees = input.getRotationDegrees();
        boolean mirroring = input.getMirroring();

        // Calculate sensorToBufferTransform
        android.graphics.Matrix sensorToBufferTransform =
                new android.graphics.Matrix(input.getSensorToBufferTransform());
        android.graphics.Matrix imageTransform = getRectToRect(sizeToRectF(inputSize),
                sizeToRectF(outConfig.getSize()), rotationDegrees, mirroring);
        sensorToBufferTransform.postConcat(imageTransform);

        // TODO(b/259308680): Checks that the aspect ratio of the rotated crop rect matches the
        //  output size.
        outputSurface = new SettableSurface(
                outConfig.getTargets(),
                outConfig.getSize(),
                input.getFormat(),
                sensorToBufferTransform,
                // The Surface transform cannot be carried over during buffer copy.
                /*hasEmbeddedTransform=*/false,
                // Crop rect is always the full size.
                sizeToRect(outConfig.getSize()),
                /*rotationDegrees=*/0,
                /*mirroring=*/false,
                onSurfaceInvalidated);

        return outputSurface;
    }

    private void sendSurfacesToProcessorWhenReady(@NonNull SettableSurface input,
            @NonNull Map<OutConfig, SettableSurface> outputs) {
        SurfaceRequest surfaceRequest = input.createSurfaceRequest(mCameraInternal);
        List<ListenableFuture<SurfaceOutput>> outputFutures = new ArrayList<>();
        for (Map.Entry<OutConfig, SettableSurface> output : outputs.entrySet()) {
            outputFutures.add(output.getValue().createSurfaceOutputFuture(
                    input.getSize(),
                    output.getKey().getCropRect(),
                    input.getRotationDegrees(),
                    input.getMirroring()));
        }
        setupRotationUpdates(
                surfaceRequest,
                outputs.values(),
                input.getMirroring(),
                input.getRotationDegrees());

        ListenableFuture<List<SurfaceOutput>> outputListFuture = Futures.allAsList(outputFutures);
        Futures.addCallback(outputListFuture,
                new FutureCallback<List<SurfaceOutput>>() {

                    @Override
                    public void onSuccess(@Nullable List<SurfaceOutput> outputs) {
                        Preconditions.checkNotNull(outputs);
                        try {
                            for (SurfaceOutput output : outputs) {
                                mSurfaceProcessor.onOutputSurface(output);
                            }
                            mSurfaceProcessor.onInputSurface(surfaceRequest);
                        } catch (ProcessingException e) {
                            Logger.e(TAG, "Failed to setup SurfaceProcessor input.", e);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        // Do not send surfaces to the processor if the downstream provider
                        // (e.g.the app) fails to provide a Surface. Instead, notify the
                        // consumer that the Surface will not be provided.
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
     * @param outputSurfaces      {@link SettableSurface} of the output edge.
     * @param mirrored            whether the node mirrors the buffer.
     * @param rotatedDegrees      how much the node rotates the buffer.
     */
    void setupRotationUpdates(
            @NonNull SurfaceRequest inputSurfaceRequest,
            @NonNull Collection<SettableSurface> outputSurfaces,
            boolean mirrored,
            int rotatedDegrees) {
        inputSurfaceRequest.setTransformationInfoListener(mainThreadExecutor(), info -> {
            // To obtain the rotation degrees delta, the rotation performed by the node must be
            // eliminated.
            int rotationDegrees = info.getRotationDegrees() - rotatedDegrees;
            if (mirrored) {
                rotationDegrees = -rotationDegrees;
            }
            rotationDegrees = within360(rotationDegrees);
            for (SettableSurface output : outputSurfaces) {
                output.setRotationDegrees(rotationDegrees);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        mSurfaceProcessor.release();
        mainThreadExecutor().execute(() -> {
            if (mOutput != null) {
                for (SettableSurface surface : mOutput.values()) {
                    // The output DeferrableSurface will later be terminated by the processor.
                    surface.close();
                }
            }
        });
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
        public abstract SettableSurface getSurfaceEdge();

        /**
         * Gets the config for generating output streams.
         *
         * <p>{@link SurfaceProcessorNode#transform} creates one {@link SettableSurface} per
         * {@link OutConfig} in this list.
         */
        @SuppressWarnings("AutoValueImmutableFields")
        @NonNull
        public abstract List<OutConfig> getOutConfigs();

        /**
         * Creates a {@link In} instance.
         */
        @NonNull
        public static In of(@NonNull SettableSurface edge, @NonNull List<OutConfig> configs) {
            return new AutoValue_SurfaceProcessorNode_In(edge, configs);
        }
    }

    /**
     * The output of a {@link SurfaceProcessorNode}.
     *
     * <p>A map of {@link OutConfig} with their corresponding {@link SettableSurface}.
     */
    public static class Out extends HashMap<OutConfig, SettableSurface> {
    }

    /**
     * Configuration of how to create an output stream from an input stream.
     *
     * <p>The value in this class will override the corresponding value in the
     * {@link SettableSurface} class. The override is necessary when a single stream is shared
     * to multiple output streams with different transformations. For example, if a single 4:3
     * preview stream is shared to a 16:9 video stream, the video stream must override the crop
     * rect.
     */
    @AutoValue
    public abstract static class OutConfig {

        /**
         * The target {@link UseCase} of the output stream.
         */
        @CameraEffect.Targets
        abstract int getTargets();

        /**
         * How the input should be cropped.
         */
        @NonNull
        abstract Rect getCropRect();

        /**
         * The stream should scale to this size after cropping and rotating.
         *
         * <p>The input stream should be scaled to match this size after cropping and rotating
         */
        @NonNull
        abstract Size getSize();

        /**
         * Creates an {@link OutConfig} instance from the input edge.
         *
         * <p>The result is an output edge with the input's transformation applied.
         */
        @NonNull
        public static OutConfig of(@NonNull SettableSurface surface) {
            return of(surface.getTargets(),
                    surface.getCropRect(),
                    getRotatedSize(surface.getCropRect(), surface.getRotationDegrees()));
        }

        /**
         * Creates an {@link OutConfig} instance with custom transformations.
         */
        @NonNull
        public static OutConfig of(int targets, @NonNull Rect cropRect, @NonNull Size size) {
            return new AutoValue_SurfaceProcessorNode_OutConfig(targets, cropRect, size);
        }
    }
}
