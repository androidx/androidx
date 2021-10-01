/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

/** Utility functions for downsampling an {@link ImageProxy}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageProxyDownsampler {

    private ImageProxyDownsampler() {
    }

    /**
     * Downsamples an {@link ImageProxy}.
     *
     * @param image              to downsample
     * @param downsampledWidth   width of the downsampled image
     * @param downsampledHeight  height of the dowsampled image
     * @param downsamplingMethod the downsampling method
     * @return the downsampled image
     */
    static ForwardingImageProxy downsample(
            ImageProxy image,
            int downsampledWidth,
            int downsampledHeight,
            DownsamplingMethod downsamplingMethod) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new UnsupportedOperationException(
                    "Only YUV_420_888 format is currently supported.");
        }
        if (image.getWidth() < downsampledWidth || image.getHeight() < downsampledHeight) {
            throw new IllegalArgumentException(
                    "Downsampled dimension "
                            + new Size(downsampledWidth, downsampledHeight)
                            + " is not <= original dimension "
                            + new Size(image.getWidth(), image.getHeight())
                            + ".");
        }

        if (image.getWidth() == downsampledWidth && image.getHeight() == downsampledHeight) {
            return new ForwardingImageProxyImpl(
                    image, image.getPlanes(), downsampledWidth, downsampledHeight);
        }

        int[] inputWidths = {image.getWidth(), image.getWidth() / 2, image.getWidth() / 2};
        int[] inputHeights = {image.getHeight(), image.getHeight() / 2, image.getHeight() / 2};
        int[] outputWidths = {downsampledWidth, downsampledWidth / 2, downsampledWidth / 2};
        int[] outputHeights = {downsampledHeight, downsampledHeight / 2, downsampledHeight / 2};

        ImageProxy.PlaneProxy[] outputPlanes = new ImageProxy.PlaneProxy[3];
        for (int i = 0; i < 3; ++i) {
            ImageProxy.PlaneProxy inputPlane = image.getPlanes()[i];
            ByteBuffer inputBuffer = inputPlane.getBuffer();
            byte[] output = new byte[outputWidths[i] * outputHeights[i]];
            switch (downsamplingMethod) {
                case NEAREST_NEIGHBOR:
                    resizeNearestNeighbor(
                            inputBuffer,
                            inputWidths[i],
                            inputPlane.getPixelStride(),
                            inputPlane.getRowStride(),
                            inputHeights[i],
                            output,
                            outputWidths[i],
                            outputHeights[i]);
                    break;
                case AVERAGING:
                    resizeAveraging(
                            inputBuffer,
                            inputWidths[i],
                            inputPlane.getPixelStride(),
                            inputPlane.getRowStride(),
                            inputHeights[i],
                            output,
                            outputWidths[i],
                            outputHeights[i]);
                    break;
            }
            outputPlanes[i] = createPlaneProxy(outputWidths[i], 1, output);
        }
        return new ForwardingImageProxyImpl(
                image, outputPlanes, downsampledWidth, downsampledHeight);
    }

    private static void resizeNearestNeighbor(
            ByteBuffer input,
            int inputWidth,
            int inputPixelStride,
            int inputRowStride,
            int inputHeight,
            byte[] output,
            int outputWidth,
            int outputHeight) {
        float scaleX = (float) inputWidth / outputWidth;
        float scaleY = (float) inputHeight / outputHeight;
        int outputRowStride = outputWidth;

        byte[] row = new byte[inputRowStride];
        int[] sourceIndices = new int[outputWidth];
        for (int ix = 0; ix < outputWidth; ++ix) {
            float sourceX = ix * scaleX;
            int floorSourceX = (int) sourceX;
            sourceIndices[ix] = floorSourceX * inputPixelStride;
        }

        synchronized (input) {
            input.rewind();
            for (int iy = 0; iy < outputHeight; ++iy) {
                float sourceY = iy * scaleY;
                int floorSourceY = (int) sourceY;
                int rowOffsetSource = Math.min(floorSourceY, inputHeight - 1) * inputRowStride;
                int rowOffsetTarget = iy * outputRowStride;

                input.position(rowOffsetSource);
                input.get(row, 0, Math.min(inputRowStride, input.remaining()));

                for (int ix = 0; ix < outputWidth; ++ix) {
                    output[rowOffsetTarget + ix] = row[sourceIndices[ix]];
                }
            }
        }
    }

    private static void resizeAveraging(
            ByteBuffer input,
            int inputWidth,
            int inputPixelStride,
            int inputRowStride,
            int inputHeight,
            byte[] output,
            int outputWidth,
            int outputHeight) {
        float scaleX = (float) inputWidth / outputWidth;
        float scaleY = (float) inputHeight / outputHeight;
        int outputRowStride = outputWidth;

        byte[] row0 = new byte[inputRowStride];
        byte[] row1 = new byte[inputRowStride];
        int[] sourceIndices = new int[outputWidth];
        for (int ix = 0; ix < outputWidth; ++ix) {
            float sourceX = ix * scaleX;
            int floorSourceX = (int) sourceX;
            sourceIndices[ix] = floorSourceX * inputPixelStride;
        }

        synchronized (input) {
            input.rewind();
            for (int iy = 0; iy < outputHeight; ++iy) {
                float sourceY = iy * scaleY;
                int floorSourceY = (int) sourceY;
                int rowOffsetSource0 = Math.min(floorSourceY, inputHeight - 1) * inputRowStride;
                int rowOffsetSource1 = Math.min(floorSourceY + 1, inputHeight - 1) * inputRowStride;
                int rowOffsetTarget = iy * outputRowStride;

                input.position(rowOffsetSource0);
                input.get(row0, 0, Math.min(inputRowStride, input.remaining()));
                input.position(rowOffsetSource1);
                input.get(row1, 0, Math.min(inputRowStride, input.remaining()));

                for (int ix = 0; ix < outputWidth; ++ix) {
                    int sampleA = row0[sourceIndices[ix]] & 0xFF;
                    int sampleB = row0[sourceIndices[ix] + inputPixelStride] & 0xFF;
                    int sampleC = row1[sourceIndices[ix]] & 0xFF;
                    int sampleD = row1[sourceIndices[ix] + inputPixelStride] & 0xFF;
                    int mixed = (sampleA + sampleB + sampleC + sampleD) / 4;
                    output[rowOffsetTarget + ix] = (byte) (mixed & 0xFF);
                }
            }
        }
    }

    private static ImageProxy.PlaneProxy createPlaneProxy(
            final int rowStride, final int pixelStride, final byte[] data) {
        return new ImageProxy.PlaneProxy() {
            final ByteBuffer mBuffer = ByteBuffer.wrap(data);

            @Override
            public int getRowStride() {
                return rowStride;
            }

            @Override
            public int getPixelStride() {
                return pixelStride;
            }

            @Override
            @NonNull
            public ByteBuffer getBuffer() {
                return mBuffer;
            }
        };
    }

    enum DownsamplingMethod {
        // Uses nearest sample.
        NEAREST_NEIGHBOR,
        // Uses average of 4 nearest samples.
        AVERAGING,
    }

    private static final class ForwardingImageProxyImpl extends ForwardingImageProxy {
        private final PlaneProxy[] mDownsampledPlanes;
        private final int mDownsampledWidth;
        private final int mDownsampledHeight;

        ForwardingImageProxyImpl(
                ImageProxy originalImage,
                PlaneProxy[] downsampledPlanes,
                int downsampledWidth,
                int downsampledHeight) {
            super(originalImage);
            mDownsampledPlanes = downsampledPlanes;
            mDownsampledWidth = downsampledWidth;
            mDownsampledHeight = downsampledHeight;
        }

        @Override
        public synchronized int getWidth() {
            return mDownsampledWidth;
        }

        @Override
        public synchronized int getHeight() {
            return mDownsampledHeight;
        }

        @Override
        @NonNull
        public synchronized PlaneProxy[] getPlanes() {
            return mDownsampledPlanes;
        }
    }
}
