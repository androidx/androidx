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

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.LowMemoryQuirk;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.InternalImageProcessor;
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

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
    final Executor mBlockingExecutor;
    @Nullable
    final InternalImageProcessor mImageProcessor;

    private ProcessingNode.In mInputEdge;
    private Operation<InputPacket, Packet<ImageProxy>> mInput2Packet;
    private Operation<Image2JpegBytes.In, Packet<byte[]>> mImage2JpegBytes;
    private Operation<Bitmap2JpegBytes.In, Packet<byte[]>> mBitmap2JpegBytes;
    private Operation<JpegBytes2Disk.In, ImageCapture.OutputFileResults> mJpegBytes2Disk;
    private Operation<Packet<byte[]>, Packet<Bitmap>> mJpegBytes2CroppedBitmap;
    private Operation<Packet<ImageProxy>, ImageProxy> mJpegImage2Result;
    private Operation<Packet<byte[]>, Packet<ImageProxy>> mJpegBytes2Image;
    private Operation<Packet<Bitmap>, Packet<Bitmap>> mBitmapEffect;

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     */
    @VisibleForTesting
    ProcessingNode(@NonNull Executor blockingExecutor) {
        this(blockingExecutor, /*imageProcessor=*/null);
    }

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     * @param imageProcessor   external effect for post-processing.
     */
    ProcessingNode(@NonNull Executor blockingExecutor,
            @Nullable InternalImageProcessor imageProcessor) {
        boolean isLowMemoryDevice = DeviceQuirks.get(LowMemoryQuirk.class) != null;
        if (isLowMemoryDevice) {
            mBlockingExecutor = CameraXExecutors.newSequentialExecutor(blockingExecutor);
        } else {
            mBlockingExecutor = blockingExecutor;
        }
        mImageProcessor = imageProcessor;
    }

    @NonNull
    @Override
    public Void transform(@NonNull ProcessingNode.In inputEdge) {
        mInputEdge = inputEdge;
        // Listen to the input edge.
        inputEdge.getEdge().setListener(
                inputPacket -> {
                    if (inputPacket.getProcessingRequest().isAborted()) {
                        // No-ops if the request is aborted.
                        return;
                    }
                    mBlockingExecutor.execute(() -> processInputPacket(inputPacket));
                });

        mInput2Packet = new ProcessingInput2Packet();
        mImage2JpegBytes = new Image2JpegBytes();
        mJpegBytes2CroppedBitmap = new JpegBytes2CroppedBitmap();
        mBitmap2JpegBytes = new Bitmap2JpegBytes();
        mJpegBytes2Disk = new JpegBytes2Disk();
        mJpegImage2Result = new JpegImage2Result();
        if (inputEdge.getInputFormat() == YUV_420_888 || mImageProcessor != null) {
            // Convert JPEG bytes to ImageProxy for:
            // - YUV input: YUV -> JPEG -> ImageProxy
            // - Effects: JPEG -> Bitmap -> effect -> Bitmap -> JPEG -> ImageProxy
            mJpegBytes2Image = new JpegBytes2Image();
        }
        if (mImageProcessor != null) {
            mBitmapEffect = new BitmapEffect(mImageProcessor);
        }
        // No output. The request callback will be invoked to deliver the final result.
        return null;
    }

    @Override
    public void release() {
    }

    /**
     * Processes an {@link InputPacket} and delivers the result to {@link TakePictureManager}.
     */
    @WorkerThread
    void processInputPacket(@NonNull InputPacket inputPacket) {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        try {
            if (inputPacket.getProcessingRequest().isInMemoryCapture()) {
                ImageProxy result = processInMemoryCapture(inputPacket);
                mainThreadExecutor().execute(() -> request.onFinalResult(result));
            } else {
                ImageCapture.OutputFileResults result = processOnDiskCapture(inputPacket);
                mainThreadExecutor().execute(() -> request.onFinalResult(result));
            }
        } catch (ImageCaptureException e) {
            sendError(request, e);
        } catch (OutOfMemoryError e) {
            sendError(request, new ImageCaptureException(
                    ERROR_UNKNOWN, "Processing failed due to low memory.", e));
        } catch (RuntimeException e) {
            // For unexpected exceptions, throw an ERROR_UNKNOWN ImageCaptureException.
            sendError(request, new ImageCaptureException(ERROR_UNKNOWN, "Processing failed.", e));
        }
    }

    @NonNull
    @WorkerThread
    ImageCapture.OutputFileResults processOnDiskCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        checkArgument(mInputEdge.getOutputFormat() == JPEG,
                String.format("On-disk capture only support JPEG output format. Output format: %s",
                        mInputEdge.getOutputFormat()));
        ProcessingRequest request = inputPacket.getProcessingRequest();
        Packet<ImageProxy> originalImage = mInput2Packet.apply(inputPacket);
        Packet<byte[]> jpegBytes = mImage2JpegBytes.apply(
                Image2JpegBytes.In.of(originalImage, request.getJpegQuality()));
        if (jpegBytes.hasCropping() || mBitmapEffect != null) {
            jpegBytes = cropAndMaybeApplyEffect(jpegBytes, request.getJpegQuality());
        }
        return mJpegBytes2Disk.apply(
                JpegBytes2Disk.In.of(jpegBytes, requireNonNull(request.getOutputFileOptions())));
    }

    @NonNull
    @WorkerThread
    ImageProxy processInMemoryCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        Packet<ImageProxy> image = mInput2Packet.apply(inputPacket);
        if ((image.getFormat() == YUV_420_888 || mBitmapEffect != null)
                && mInputEdge.getOutputFormat() == JPEG) {
            Packet<byte[]> jpegBytes = mImage2JpegBytes.apply(
                    Image2JpegBytes.In.of(image, request.getJpegQuality()));
            if (mBitmapEffect != null) {
                jpegBytes = cropAndMaybeApplyEffect(jpegBytes, request.getJpegQuality());
            }
            image = mJpegBytes2Image.apply(jpegBytes);
        }
        return mJpegImage2Result.apply(image);
    }

    /**
     * Crops JPEG byte array and apply effect if present.
     */
    private Packet<byte[]> cropAndMaybeApplyEffect(Packet<byte[]> jpegPacket, int jpegQuality)
            throws ImageCaptureException {
        checkState(jpegPacket.getFormat() == JPEG);
        Packet<Bitmap> bitmapPacket = mJpegBytes2CroppedBitmap.apply(jpegPacket);
        if (mBitmapEffect != null) {
            // Apply effect if present.
            bitmapPacket = mBitmapEffect.apply(bitmapPacket);
        }
        return mBitmap2JpegBytes.apply(
                Bitmap2JpegBytes.In.of(bitmapPacket, jpegQuality));
    }

    /**
     * Sends {@link ImageCaptureException} to {@link TakePictureManager}.
     */
    private static void sendError(@NonNull ProcessingRequest request,
            @NonNull ImageCaptureException e) {
        mainThreadExecutor().execute(() -> request.onProcessFailure(e));
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

        /**
         * Whether the pipeline is connected to a virtual camera.
         */
        abstract boolean isVirtualCamera();

        static InputPacket of(@NonNull ProcessingRequest processingRequest,
                @NonNull ImageProxy imageProxy, boolean isVirtualCamera) {
            return new AutoValue_ProcessingNode_InputPacket(processingRequest, imageProxy,
                    isVirtualCamera);
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
        abstract int getInputFormat();

        /**
         * The output format of the pipeline.
         *
         * <p> For public users, only {@link ImageFormat#JPEG} is supported. Other formats are
         * only used by in-memory capture in tests.
         */
        abstract int getOutputFormat();

        static In of(int inputFormat, int outputFormat) {
            return new AutoValue_ProcessingNode_In(new Edge<>(), inputFormat, outputFormat);
        }
    }

    @VisibleForTesting
    void injectProcessingInput2Packet(
            @NonNull Operation<InputPacket, Packet<ImageProxy>> input2Packet) {
        mInput2Packet = input2Packet;
    }
}
