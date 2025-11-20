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
import static android.graphics.ImageFormat.JPEG_R;
import static android.graphics.ImageFormat.RAW_SENSOR;
import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.core.internal.utils.ImageUtil.isJpegFormats;
import static androidx.camera.core.internal.utils.ImageUtil.isRawFormats;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraXTracer;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.IncorrectJpegMetadataQuirk;
import androidx.camera.core.internal.compat.quirk.LowMemoryQuirk;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.InternalImageProcessor;
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Processes a single image and invokes {@link TakePictureCallback}.
 *
 * <p>This node performs operations that runs on a single image, such as cropping, format
 * conversion, effects and/or saving to disk.
 */
public class ProcessingNode implements Node<ProcessingNode.In, Void> {
    private static final String TAG = "ProcessingNode";
    final @NonNull Executor mBlockingExecutor;
    final @Nullable InternalImageProcessor mImageProcessor;

    private final @Nullable CameraCharacteristics mCameraCharacteristics;

    @VisibleForTesting
    @Nullable DngImage2Disk mDngImage2Disk;

    private ProcessingNode.In mInputEdge;
    private Operation<InputPacket, Packet<ImageProxy>> mInput2Packet;
    private Operation<Image2JpegBytes.In, Packet<byte[]>> mImage2JpegBytes;
    private Operation<Bitmap2JpegBytes.In, Packet<byte[]>> mBitmap2JpegBytes;
    private Operation<JpegBytes2Disk.In, ImageCapture.OutputFileResults> mJpegBytes2Disk;
    private Operation<Packet<byte[]>, Packet<Bitmap>> mJpegBytes2CroppedBitmap;
    private Operation<Packet<ImageProxy>, ImageProxy> mJpegImage2Result;
    private Operation<Packet<byte[]>, Packet<ImageProxy>> mJpegBytes2Image;
    private Operation<Packet<ImageProxy>, Bitmap> mImage2Bitmap;
    private Operation<Packet<Bitmap>, Packet<Bitmap>> mBitmapEffect;
    private final Quirks mQuirks;
    private final boolean mHasIncorrectJpegMetadataQuirk;

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     */
    @VisibleForTesting
    ProcessingNode(@NonNull Executor blockingExecutor,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        this(blockingExecutor, cameraCharacteristics,
                /*imageProcessor=*/null, DeviceQuirks.getAll());
    }

    @VisibleForTesting
    ProcessingNode(@NonNull Executor blockingExecutor,
            @NonNull Quirks quirks,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        this(blockingExecutor, cameraCharacteristics, /*imageProcessor=*/null, quirks);
    }

    ProcessingNode(@NonNull Executor blockingExecutor,
            @Nullable CameraCharacteristics cameraCharacteristics,
            @Nullable InternalImageProcessor imageProcessor) {
        this(blockingExecutor, cameraCharacteristics, imageProcessor, DeviceQuirks.getAll());
    }

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     * @param imageProcessor   external effect for post-processing.
     */
    ProcessingNode(@NonNull Executor blockingExecutor,
            @Nullable CameraCharacteristics cameraCharacteristics,
            @Nullable InternalImageProcessor imageProcessor,
            @NonNull Quirks quirks) {
        boolean isLowMemoryDevice = DeviceQuirks.get(LowMemoryQuirk.class) != null;
        if (isLowMemoryDevice) {
            mBlockingExecutor = CameraXExecutors.newSequentialExecutor(blockingExecutor);
        } else {
            mBlockingExecutor = blockingExecutor;
        }
        mImageProcessor = imageProcessor;
        mCameraCharacteristics = cameraCharacteristics;
        mQuirks = quirks;
        mHasIncorrectJpegMetadataQuirk = quirks.contains(IncorrectJpegMetadataQuirk.class);
    }

    @Override
    public @NonNull Void transform(ProcessingNode.@NonNull In inputEdge) {
        mInputEdge = inputEdge;
        // Listen to the input edge.
        inputEdge.getEdge().setListener(
                inputPacket -> {
                    if (inputPacket.getProcessingRequest().isAborted()) {
                        // No-ops if the request is aborted.
                        inputPacket.getImageProxy().close();
                        return;
                    }
                    mBlockingExecutor.execute(() -> processInputPacket(inputPacket));
                });
        inputEdge.getPostviewEdge().setListener(
                inputPacket ->  {
                    if (inputPacket.getProcessingRequest().isAborted()) {
                        Logger.w(TAG,
                                "The postview image is closed due to request aborted");
                        // No-ops if the request is aborted.
                        inputPacket.getImageProxy().close();
                        return;
                    }
                    mBlockingExecutor.execute(() -> processPostviewInputPacket(inputPacket));
                }
        );

        mInput2Packet = new ProcessingInput2Packet();
        mImage2JpegBytes = new Image2JpegBytes(mQuirks);
        mJpegBytes2CroppedBitmap = new JpegBytes2CroppedBitmap();
        mBitmap2JpegBytes = new Bitmap2JpegBytes();
        mJpegBytes2Disk = new JpegBytes2Disk();
        mJpegImage2Result = new JpegImage2Result();
        mImage2Bitmap = new Image2Bitmap();
        if (inputEdge.getInputFormat() == YUV_420_888 || mImageProcessor != null
                || mHasIncorrectJpegMetadataQuirk) {
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
        CameraXTracer.trace("processInputPacket", () -> {
            ProcessingRequest request = inputPacket.getProcessingRequest();
            try {
                // If simultaneous capture RAW + JPEG, only trigger callback when both images
                // are available and processed.
                boolean isSimultaneousCaptureEnabled = mInputEdge.getOutputFormats().size() > 1;
                if (inputPacket.getProcessingRequest().isInMemoryCapture()) {
                    ImageProxy result = processInMemoryCapture(inputPacket);
                    mainThreadExecutor().execute(() -> request.onFinalResult(result));
                } else {
                    ImageCapture.OutputFileResults result = processOnDiskCapture(inputPacket);
                    boolean isProcessed =
                            !isSimultaneousCaptureEnabled || request.getTakePictureRequest()
                                    .isFormatProcessedInSimultaneousCapture();
                    if (isProcessed) {
                        mainThreadExecutor().execute(() -> request.onFinalResult(result));
                    }
                }
            } catch (ImageCaptureException e) {
                sendError(request, e);
            } catch (OutOfMemoryError e) {
                sendError(request, new ImageCaptureException(
                        ERROR_UNKNOWN, "Processing failed due to low memory.", e));
            } catch (RuntimeException e) {
                // For unexpected exceptions, throw an ERROR_UNKNOWN ImageCaptureException.
                sendError(request,
                        new ImageCaptureException(ERROR_UNKNOWN, "Processing failed.", e));
            }
        });
    }

    @WorkerThread
    void processPostviewInputPacket(@NonNull InputPacket inputPacket) {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        try {
            Packet<ImageProxy> image = mInput2Packet.apply(inputPacket);
            int imageFormat = image.getFormat();
            checkArgument(
                    imageFormat == YUV_420_888 || imageFormat == JPEG || imageFormat == JPEG_R,
                    String.format(
                            "Postview only supports to convert YUV, JPEG and JPEG_R format image "
                                    + "to the postview output bitmap. Image format: %s",
                            imageFormat));
            Bitmap bitmap = mImage2Bitmap.apply(image);
            mainThreadExecutor().execute(() -> request.onPostviewBitmapAvailable(bitmap));
        } catch (Exception e) {
            inputPacket.getImageProxy().close();
            Logger.e(TAG, "process postview input packet failed.", e);
        }
    }

    @WorkerThread
    ImageCapture.@NonNull OutputFileResults processOnDiskCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        Logger.d(TAG, "processOnDiskCapture: request ID = "
                + inputPacket.getProcessingRequest().getRequestId());
        List<Integer> outputFormats = mInputEdge.getOutputFormats();
        checkArgument(!outputFormats.isEmpty());
        int format = outputFormats.get(0);
        checkArgument(isJpegFormats(format)
                || isRawFormats(format),
                String.format("On-disk capture only support JPEG and"
                + " JPEG/R and RAW output formats. Output format: %s", format));
        ProcessingRequest request = inputPacket.getProcessingRequest();
        checkArgument(request.getOutputFileOptions() != null,
                "OutputFileOptions cannot be empty");
        Packet<ImageProxy> originalImage = mInput2Packet.apply(inputPacket);
        boolean isSimultaneousCaptureEnabled = outputFormats.size() > 1;
        if (isSimultaneousCaptureEnabled) {
            // If simultaneous capture RAW + JPEG, use the first output file options for RAW and
            // the second for JPEG.
            checkArgument(request.getOutputFileOptions() != null
                            && request.getSecondaryOutputFileOptions() != null,
                    "The number of OutputFileOptions for simultaneous capture "
                            + "should be at least two");
            ImageCapture.OutputFileResults outputFileResults = null;
            switch (originalImage.getFormat()) {
                case RAW_SENSOR:
                    outputFileResults = saveRawToDisk(originalImage,
                            requireNonNull(request.getOutputFileOptions()));
                    request.getTakePictureRequest()
                            .markFormatProcessStatusInSimultaneousCapture(RAW_SENSOR, true);
                    return outputFileResults;
                case JPEG:
                default:
                    outputFileResults = saveJpegToDisk(originalImage,
                            requireNonNull(request.getSecondaryOutputFileOptions()),
                            request.getJpegQuality());
                    request.getTakePictureRequest()
                            .markFormatProcessStatusInSimultaneousCapture(JPEG, true);
                    return outputFileResults;
            }
        } else {
            switch (format) {
                case RAW_SENSOR:
                    return saveRawToDisk(originalImage,
                            requireNonNull(request.getOutputFileOptions()));
                case JPEG:
                default:
                    return saveJpegToDisk(originalImage,
                            requireNonNull(request.getOutputFileOptions()),
                            request.getJpegQuality());
            }
        }
    }

    private ImageCapture.@NonNull OutputFileResults saveRawToDisk(
            @NonNull Packet<ImageProxy> originalImage,
            ImageCapture.@NonNull OutputFileOptions outputFileOptions)
            throws ImageCaptureException {

        if (mDngImage2Disk == null) {
            if (mCameraCharacteristics == null) {
                throw new ImageCaptureException(ERROR_UNKNOWN,
                        "CameraCharacteristics is null, DngCreator cannot be created", null);
            }
            if (originalImage.getCameraCaptureResult().getCaptureResult() == null) {
                throw new ImageCaptureException(ERROR_UNKNOWN,
                        "CameraCaptureResult is null, DngCreator cannot be created", null);
            }
            mDngImage2Disk = new DngImage2Disk(
                    requireNonNull(mCameraCharacteristics),
                    requireNonNull(originalImage.getCameraCaptureResult().getCaptureResult()));
        }
        return mDngImage2Disk.apply(DngImage2Disk.In.of(
                originalImage.getData(),
                originalImage.getRotationDegrees(),
                requireNonNull(outputFileOptions)));
    }

    private ImageCapture.@NonNull OutputFileResults saveJpegToDisk(
            @NonNull Packet<ImageProxy> originalImage,
            ImageCapture.@NonNull OutputFileOptions outputFileOptions,
            int jpegQuality) throws ImageCaptureException {
        Packet<byte[]> jpegBytes = mImage2JpegBytes.apply(
                Image2JpegBytes.In.of(originalImage, jpegQuality));
        if (jpegBytes.hasCropping() || mBitmapEffect != null) {
            jpegBytes = cropAndMaybeApplyEffect(jpegBytes, jpegQuality);
        }
        return mJpegBytes2Disk.apply(
                JpegBytes2Disk.In.of(jpegBytes, requireNonNull(outputFileOptions)));
    }

    @WorkerThread
    @NonNull ImageProxy processInMemoryCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        Logger.d(TAG, "processInMemoryCapture: request ID = "
                + inputPacket.getProcessingRequest().getRequestId());
        ProcessingRequest request = inputPacket.getProcessingRequest();
        Packet<ImageProxy> image = mInput2Packet.apply(inputPacket);
        // TODO(b/322311893): Update to handle JPEG/R as output format in the if-statement when YUV
        //  to JPEG/R and effect with JPEG/R are supported.
        List<Integer> outputFormats = mInputEdge.getOutputFormats();
        checkArgument(!outputFormats.isEmpty());
        int format = outputFormats.get(0);

        if ((image.getFormat() == YUV_420_888 || mBitmapEffect != null
                || mHasIncorrectJpegMetadataQuirk) && (format == JPEG)) {
            Packet<byte[]> jpegBytes = mImage2JpegBytes.apply(
                    Image2JpegBytes.In.of(image, request.getJpegQuality()));
            if (mBitmapEffect != null) {
                jpegBytes = cropAndMaybeApplyEffect(jpegBytes, request.getJpegQuality());
            }
            image = mJpegBytes2Image.apply(jpegBytes);
        }
        ImageProxy imageProxy = mJpegImage2Result.apply(image);
        boolean isSimultaneousCaptureEnabled = outputFormats.size() > 1;
        if (isSimultaneousCaptureEnabled) {
            request.getTakePictureRequest()
                    .markFormatProcessStatusInSimultaneousCapture(imageProxy.getFormat(), true);
        }
        return imageProxy;
    }

    /**
     * Crops JPEG byte array and apply effect if present.
     */
    private Packet<byte[]> cropAndMaybeApplyEffect(Packet<byte[]> jpegPacket, int jpegQuality)
            throws ImageCaptureException {
        checkState(isJpegFormats(jpegPacket.getFormat()));
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
    private void sendError(@NonNull ProcessingRequest request,
            @NonNull ImageCaptureException e) {
        mainThreadExecutor().execute(() -> {
            request.onProcessFailure(e);
        });
    }

    /**
     * Input packet which is a combination of camera frame and processing request.
     */
    @AutoValue
    abstract static class InputPacket {

        abstract @NonNull ProcessingRequest getProcessingRequest();

        abstract @NonNull ImageProxy getImageProxy();

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
         * Get the main input edge that contains a {@link InputPacket} flow.
         */
        abstract @NonNull Edge<InputPacket> getEdge();


        /**
         * Get the postview input edge.
         */
        abstract @NonNull Edge<InputPacket> getPostviewEdge();

        /**
         * Gets the format of the image in {@link InputPacket}.
         */
        abstract int getInputFormat();

        /**
         * The output format of the pipeline.
         *
         * <p> For public users, {@link ImageFormat#JPEG}, {@link ImageFormat#JPEG_R} and
         * {@link ImageFormat#RAW_SENSOR}} are supported. Other formats are only used by in-memory
         * capture in tests.
         */
        @SuppressWarnings("AutoValueImmutableFields")
        abstract @NonNull List<Integer> getOutputFormats();

        static In of(int inputFormat,
                @NonNull List<Integer> outputFormats) {
            return new AutoValue_ProcessingNode_In(new Edge<>(), new Edge<>(),
                    inputFormat, outputFormats);
        }
    }

    @VisibleForTesting
    void injectProcessingInput2Packet(
            @NonNull Operation<InputPacket, Packet<ImageProxy>> input2Packet) {
        mInput2Packet = input2Packet;
    }
}
