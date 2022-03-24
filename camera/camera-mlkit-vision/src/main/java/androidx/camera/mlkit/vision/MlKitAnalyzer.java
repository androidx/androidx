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
package androidx.camera.mlkit.vision;

import static androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL;

import static com.google.android.gms.common.internal.Preconditions.checkArgument;
import static com.google.mlkit.vision.interfaces.Detector.TYPE_BARCODE_SCANNING;
import static com.google.mlkit.vision.interfaces.Detector.TYPE_SEGMENTATION;
import static com.google.mlkit.vision.interfaces.Detector.TYPE_TEXT_RECOGNITION;

import android.graphics.Matrix;
import android.media.Image;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalAnalyzer;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.core.util.Consumer;

import com.google.mlkit.vision.interfaces.Detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link ImageAnalysis.Analyzer} with MLKit libraries.
 *
 * <p> This class is a wrapper of one or many MLKit {@link Detector}s. It forwards
 * {@link ImageAnalysis} frames to all the {@link Detector}s sequentially. Once all the
 * {@link Detector}s finish analyzing the frame, {@link Consumer#accept} will be
 * invoked with the aggregated analysis results.
 *
 * <p> This class is designed to work seamlessly with the {@code CameraController} class in
 * camera-view. When used with {@link ImageAnalysis} in camera-core, the following scenarios may
 * need special handling:
 * <ul>
 * <li> Cannot transform coordinates to UI coordinate system. e.g. camera-core only supports
 * {@link ImageAnalysis#COORDINATE_SYSTEM_ORIGINAL}.
 * <li>For the value of {@link #getTargetResolutionOverride()} to be effective, make sure
 * the {@link ImageAnalysis#setAnalyzer} is called before it's bound to the lifecycle.
 * </ul>
 *
 * Code sample:
 * <pre><code>
 *  cameraController.setImageAnalysisAnalyzer(executor,
 *       new MlKitAnalyzer(List.of(barcodeScanner), COORDINATE_SYSTEM_VIEW_REFERENCED,
 *       executor, result -> {
 *    // The value of result.getResult(barcodeScanner) can be used directly for drawying UI layover.
 *  });
 * </pre></code>
 *
 * @see ImageAnalysis.Analyzer
 */
@ExperimentalAnalyzer
@RequiresApi(21)
public class MlKitAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "MlKitAnalyzer";

    private static final Size DEFAULT_SIZE = new Size(480, 360);

    @NonNull
    private final List<Detector<?>> mDetectors;
    private final int mTargetCoordinateSystem;
    // Synthetic access
    @NonNull
    final Consumer<Result> mConsumer;
    // Synthetic access
    final ImageProxyTransformFactory mImageAnalysisTransformFactory;
    @NonNull
    private final Executor mExecutor;

    @Nullable
    private Matrix mSensorToTarget;

    /**
     * Constructor of {@link MlKitAnalyzer}.
     *
     * <p>The list detectors will be invoked sequentially in order.
     *
     * <p>When the targetCoordinateSystem is {@link ImageAnalysis#COORDINATE_SYSTEM_ORIGINAL}, the
     * output coordinate system is defined by MLKit, which is the buffer with rotation applied. For
     * example, if {@link ImageProxy#getHeight()} is {@code h} and the rotation is 90°, (0, 0) in
     * the result maps to the pixel (0, h) in the original buffer.
     *
     * <p>The constructor throws {@link IllegalArgumentException} if
     * {@link Detector#getDetectorType()} is TYPE_SEGMENTATION and {@code targetCoordinateSystem}
     * is COORDINATE_SYSTEM_ORIGINAL. Currently MLKit does not support transformation with
     * segmentation.
     */
    @OptIn(markerClass = TransformExperimental.class)
    public MlKitAnalyzer(
            @NonNull List<Detector<?>> detectors,
            int targetCoordinateSystem,
            @NonNull Executor executor,
            @NonNull Consumer<Result> consumer) {
        if (targetCoordinateSystem != COORDINATE_SYSTEM_ORIGINAL) {
            for (Detector<?> detector : detectors) {
                checkArgument(detector.getDetectorType() != TYPE_SEGMENTATION,
                        "Segmentation only works with COORDINATE_SYSTEM_ORIGINAL");
            }
        }
        // Make an immutable copy of the app provided detectors.
        mDetectors = new ArrayList<>(detectors);
        mTargetCoordinateSystem = targetCoordinateSystem;
        mConsumer = consumer;
        mExecutor = executor;
        mImageAnalysisTransformFactory = new ImageProxyTransformFactory();
        mImageAnalysisTransformFactory.setUsingRotationDegrees(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @OptIn(markerClass = TransformExperimental.class)
    public final void analyze(@NonNull ImageProxy imageProxy) {
        // By default, the matrix is identity for COORDINATE_SYSTEM_ORIGINAL.
        Matrix transform = new Matrix();
        if (mTargetCoordinateSystem != COORDINATE_SYSTEM_ORIGINAL) {
            // Calculate the transform if not COORDINATE_SYSTEM_ORIGINAL.
            Matrix sensorToTarget = mSensorToTarget;
            if (sensorToTarget == null) {
                // If the app set a target coordinate system, do not perform detection until the
                // transform is ready.
                Logger.d(TAG, "Transform is null.");
                imageProxy.close();
                return;
            }
            OutputTransform analysisTransform =
                    mImageAnalysisTransformFactory.getOutputTransform(imageProxy);
            Size cropRectSize = new Size(imageProxy.getCropRect().width(),
                    imageProxy.getCropRect().height());
            CoordinateTransform coordinateTransform = new CoordinateTransform(analysisTransform,
                    new OutputTransform(sensorToTarget, cropRectSize));
            coordinateTransform.transform(transform);
        }
        // Detect the image recursively, starting from index 0.
        detectRecursively(imageProxy, 0, transform, new HashMap<>(), new HashMap<>());
    }

    /**
     * Recursively processes the image with {@link #mDetectors}.
     *
     * @param detectorIndex the current index of {@link #mDetectors} being processed.
     * @param values        values returned from the {@link #mDetectors}.
     * @param throwables    exceptions returned from the {@link #mDetectors}.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void detectRecursively(
            @NonNull ImageProxy imageProxy,
            int detectorIndex,
            @NonNull Matrix transform,
            Map<Detector<?>, Object> values,
            @NonNull Map<Detector<?>, Throwable> throwables) {
        Image image = imageProxy.getImage();
        if (image == null) {
            // No-op if the frame is not backed by ImageProxy.
            Logger.e(TAG, "Image is null.");
            imageProxy.close();
            return;
        }

        if (detectorIndex > mDetectors.size() - 1) {
            // Termination condition is met when the index reaches the end of the list.
            imageProxy.close();
            mExecutor.execute(() -> mConsumer.accept(
                    new Result(values, imageProxy.getImageInfo().getTimestamp(), throwables)));
            return;
        }
        Detector<?> detector = mDetectors.get(detectorIndex);
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        detector.process(image, rotationDegrees, transform).addOnCompleteListener(
                mExecutor,
                task -> {
                    // Record the return value / exception.
                    values.put(detector, task.getResult());
                    if (task.getException() != null) {
                        throwables.put(detector, task.getException());
                    }
                    // Go to the next detector.
                    detectRecursively(imageProxy, detectorIndex + 1, transform, values,
                            throwables);
                });
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public final Size getTargetResolutionOverride() {
        Size size = DEFAULT_SIZE;
        for (Detector<?> detector : mDetectors) {
            Size detectorSize = getTargetResolution(detector.getDetectorType());
            if (detectorSize.getHeight() * detectorSize.getWidth()
                    > size.getWidth() * size.getHeight()) {
                size = detectorSize;
            }
        }
        return size;
    }

    /**
     * Gets the recommended resolution for the given {@link Detector} type.
     *
     * <p> The resolution can be found on MLKit's DAC page.
     */
    @NonNull
    private Size getTargetResolution(int detectorType) {
        switch (detectorType) {
            case TYPE_BARCODE_SCANNING:
            case TYPE_TEXT_RECOGNITION:
                return new Size(1280, 720);
            default:
                return DEFAULT_SIZE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getTargetCoordinateSystem() {
        return mTargetCoordinateSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void updateTransform(@Nullable Matrix matrix) {
        if (matrix == null) {
            mSensorToTarget = null;
        } else {
            mSensorToTarget = new Matrix(matrix);
        }
    }

    /**
     * The aggregated MLKit result of a camera frame.
     */
    public static final class Result {

        @NonNull
        private final Map<Detector<?>, Object> mValues;
        @NonNull
        private final Map<Detector<?>, Throwable> mThrowables;
        private final long mTimestamp;

        public Result(@NonNull Map<Detector<?>, Object> values, long timestamp,
                @NonNull Map<Detector<?>, Throwable> throwables) {
            mValues = values;
            mThrowables = throwables;
            mTimestamp = timestamp;
        }

        /**
         * Get the analysis result for the given MLKit {@link Detector}.
         *
         * <p>Returns {@code null} if the detection is unsuccessful.
         *
         * <p>This method and {@link #getThrowable} may both return {@code null}. For example,
         * when a face detector processes a frame successfully and does not detect any faces.
         * However, if {@link #getThrowable} returns a non-null {@link Throwable}, then this
         * method will always return {@code null}.
         *
         * @param detector has to be one of the {@link Detector}s provided in
         *                 {@link MlKitAnalyzer}'s constructor.
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public <T> T getValue(@NonNull Detector<T> detector) {
            checkDetectorExists(detector);
            return (T) mValues.get(detector);
        }

        /**
         * The error returned from the given {@link Detector}.
         *
         * <p>Returns {@code null} if the {@link Detector} finishes without exceptions.
         *
         * @param detector has to be one of the {@link Detector}s provided in
         *                 {@link MlKitAnalyzer}'s constructor.
         */
        @Nullable
        public Throwable getThrowable(@NonNull Detector<?> detector) {
            checkDetectorExists(detector);
            return mThrowables.get(detector);
        }

        /**
         * The timestamp of the camera frame.
         *
         * <p> The timestamp of the camera frame based on which the analysis result is produced.
         * This is the value of {@link ImageProxy#getImageInfo()#getTimestamp()}.
         */
        public long getTimestamp() {
            return mTimestamp;
        }

        private void checkDetectorExists(@NonNull Detector<?> detector) {
            checkArgument(mValues.containsKey(detector), "The detector does not exist");
        }
    }
}
