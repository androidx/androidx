/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.processing.concurrent;

import static androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED;
import static androidx.camera.core.impl.utils.Threads.runOnMain;
import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;
import static androidx.camera.core.impl.utils.TransformUtils.isAspectRatioMatchingWithRoundingError;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRect;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.core.processing.TargetUtils.getHumanReadableName;
import static androidx.core.util.Preconditions.checkArgument;

import android.graphics.Rect;
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
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.SurfaceEdge;
import androidx.camera.core.processing.SurfaceProcessorInternal;
import androidx.camera.core.processing.SurfaceProcessorNode;
import androidx.camera.core.processing.util.OutConfig;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * An internal augmented {@link SurfaceProcessorNode} for dual concurrent cameras.
 */
@SuppressWarnings("UnusedVariable")
public class DualSurfaceProcessorNode implements
        Node<DualSurfaceProcessorNode.In, DualSurfaceProcessorNode.Out> {

    private static final String TAG = "DualSurfaceProcessorNode";

    @NonNull
    final SurfaceProcessorInternal mSurfaceProcessor;
    @NonNull
    final CameraInternal mPrimaryCameraInternal;
    @NonNull
    final CameraInternal mSecondaryCameraInternal;
    // Guarded by main thread.
    @Nullable
    private Out mOutput;
    @Nullable
    private In mInput;

    /**
     * Constructs the {@link DualSurfaceProcessorNode}.
     *
     * @param primaryCameraInternal the associated primary camera instance.
     * @param secondaryCameraInternal the associated secondary camera instance.
     * @param surfaceProcessor the interface to wrap around.
     */
    public DualSurfaceProcessorNode(
            @NonNull CameraInternal primaryCameraInternal,
            @NonNull CameraInternal secondaryCameraInternal,
            @NonNull SurfaceProcessorInternal surfaceProcessor) {
        mPrimaryCameraInternal = primaryCameraInternal;
        mSecondaryCameraInternal = secondaryCameraInternal;
        mSurfaceProcessor = surfaceProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    @MainThread
    public Out transform(@NonNull In in) {
        Threads.checkMainThread();
        mInput = in;
        mOutput = new Out();

        SurfaceEdge primaryInputSurfaceEdge = mInput.getPrimarySurfaceEdge();
        SurfaceEdge secondaryInputSurfaceEdge = mInput.getSecondarySurfaceEdge();

        for (DualOutConfig config : mInput.getOutConfigs()) {
            mOutput.put(config, transformSingleOutput(
                    primaryInputSurfaceEdge,
                    config.getPrimaryOutConfig()));
        }
        sendSurfaceRequest(mPrimaryCameraInternal, primaryInputSurfaceEdge, mOutput, true);
        sendSurfaceRequest(mSecondaryCameraInternal, secondaryInputSurfaceEdge, mOutput, false);

        sendSurfaceOutputs(mPrimaryCameraInternal, mSecondaryCameraInternal,
                primaryInputSurfaceEdge, secondaryInputSurfaceEdge, mOutput);
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
                new android.graphics.Matrix();

        // The aspect ratio of the output must match the aspect ratio of the crop rect. Otherwise
        // the output will be stretched.
        Size rotatedCropSize = getRotatedSize(cropRect, rotationDegrees);
        checkArgument(isAspectRatioMatchingWithRoundingError(rotatedCropSize, outConfig.getSize()));

        // Calculate the crop rect.
        Rect newCropRect = sizeToRect(outConfig.getSize());

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
    private void sendSurfaceRequest(
            @NonNull CameraInternal cameraInternal,
            @NonNull SurfaceEdge input,
            @NonNull Map<DualOutConfig, SurfaceEdge> outputs,
            boolean isPrimary) {
        SurfaceRequest surfaceRequest = input.createSurfaceRequest(cameraInternal, isPrimary);
        // TODO(b/348402401): Different from SurfaceProcessorNode, we don't support device rotation
        //  for dual camera recording.
        try {
            mSurfaceProcessor.onInputSurface(surfaceRequest);
        } catch (ProcessingException e) {
            Logger.e(TAG, "Failed to send SurfaceRequest to SurfaceProcessor.", e);
        }
    }

    /**
     * Creates all {@link SurfaceOutput} and send them to {@link SurfaceProcessor}.
     */
    private void sendSurfaceOutputs(
            @NonNull CameraInternal primaryCameraInternal,
            @NonNull CameraInternal secondaryCameraInternal,
            @NonNull SurfaceEdge primarySurfaceEdge,
            @NonNull SurfaceEdge secondarySurfaceEdge,
            @NonNull Map<DualOutConfig, SurfaceEdge> outputs) {
        for (Map.Entry<DualOutConfig, SurfaceEdge> output : outputs.entrySet()) {
            createAndSendSurfaceOutput(
                    primaryCameraInternal,
                    secondaryCameraInternal,
                    primarySurfaceEdge,
                    secondarySurfaceEdge,
                    output);
            // Send the new surface to SurfaceProcessor when it resets.
            output.getValue().addOnInvalidatedListener(
                    () -> createAndSendSurfaceOutput(
                            primaryCameraInternal, secondaryCameraInternal,
                            primarySurfaceEdge, secondarySurfaceEdge, output));
        }
    }

    /**
     * Creates a single {@link SurfaceOutput} and send it to {@link SurfaceProcessor}.
     */
    private void createAndSendSurfaceOutput(
            @NonNull CameraInternal primaryCameraInternal,
            @NonNull CameraInternal secondaryCameraInternal,
            @NonNull SurfaceEdge primarySurfaceEdge,
            @NonNull SurfaceEdge secondarySurfaceEdge,
            Map.Entry<DualOutConfig, SurfaceEdge> output) {
        SurfaceEdge outputEdge = output.getValue();
        SurfaceOutput.CameraInputInfo primaryCameraInputInfo = SurfaceOutput.CameraInputInfo.of(
                primarySurfaceEdge.getStreamSpec().getResolution(),
                output.getKey().getPrimaryOutConfig().getCropRect(),
                primarySurfaceEdge.hasCameraTransform() ? primaryCameraInternal : null,
                output.getKey().getPrimaryOutConfig().getRotationDegrees(),
                output.getKey().getPrimaryOutConfig().isMirroring());
        SurfaceOutput.CameraInputInfo secondaryCameraInputInfo = SurfaceOutput.CameraInputInfo.of(
                secondarySurfaceEdge.getStreamSpec().getResolution(),
                output.getKey().getSecondaryOutConfig().getCropRect(),
                secondarySurfaceEdge.hasCameraTransform() ? secondaryCameraInternal : null,
                output.getKey().getSecondaryOutConfig().getRotationDegrees(),
                output.getKey().getSecondaryOutConfig().isMirroring());
        ListenableFuture<SurfaceOutput> future = outputEdge.createSurfaceOutputFuture(
                output.getKey().getPrimaryOutConfig().getFormat(),
                primaryCameraInputInfo,
                secondaryCameraInputInfo);
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
     * The input of a {@link DualSurfaceProcessorNode}.
     */
    @AutoValue
    public abstract static class In {

        /**
         * Gets the input stream from primary camera.
         *
         * <p> {@link DualSurfaceProcessorNode} supports dual camera streams.
         */
        @NonNull
        public abstract SurfaceEdge getPrimarySurfaceEdge();

        /**
         * Gets the input stream from secondary camera.
         *
         * <p> {@link DualSurfaceProcessorNode} supports dual camera streams.
         */
        @NonNull
        public abstract SurfaceEdge getSecondarySurfaceEdge();

        /**
         * Gets the config for generating output streams.
         *
         * <p>{@link DualSurfaceProcessorNode#transform} creates two {@link SurfaceEdge} per
         * {@link DualOutConfig} in this list.
         */
        @SuppressWarnings("AutoValueImmutableFields")
        @NonNull
        public abstract List<DualOutConfig> getOutConfigs();

        /**
         * Creates a {@link In} instance.
         */
        @NonNull
        public static In of(
                @NonNull SurfaceEdge primaryEdge,
                @NonNull SurfaceEdge secondaryEdge,
                @NonNull List<DualOutConfig> configs) {
            return new AutoValue_DualSurfaceProcessorNode_In(primaryEdge, secondaryEdge, configs);
        }
    }

    /**
     * The output of a {@link DualSurfaceProcessorNode}.
     *
     * <p>A map of {@link OutConfig} with their corresponding {@link SurfaceEdge}.
     */
    public static class Out extends HashMap<DualOutConfig, SurfaceEdge> {
    }
}
