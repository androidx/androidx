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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ImageProxyDownsamplerTest {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;

    private static ImageProxy createYuv420Image(int uvPixelStride) {
        ImageProxy image = mock(ImageProxy.class);
        ImageProxy.PlaneProxy[] planes = new ImageProxy.PlaneProxy[3];

        when(image.getWidth()).thenReturn(WIDTH);
        when(image.getHeight()).thenReturn(HEIGHT);
        when(image.getFormat()).thenReturn(ImageFormat.YUV_420_888);
        when(image.getPlanes()).thenReturn(planes);

        planes[0] =
                createPlaneWithRampPattern(WIDTH, HEIGHT, /*pixelStride=*/ 1, /*initialValue=*/ 0);
        planes[1] =
                createPlaneWithRampPattern(
                        WIDTH / 2, HEIGHT / 2, uvPixelStride, /*initialValue=*/ 1);
        planes[2] =
                createPlaneWithRampPattern(
                        WIDTH / 2, HEIGHT / 2, uvPixelStride, /*initialValue=*/ 2);

        return image;
    }

    private static ImageProxy.PlaneProxy createPlaneWithRampPattern(
            final int width, final int height, final int pixelStride, final int initialValue) {
        return new ImageProxy.PlaneProxy() {
            final ByteBuffer mBuffer =
                    createBufferWithRampPattern(width, height, pixelStride, initialValue);

            @Override
            public int getRowStride() {
                return width * pixelStride;
            }

            @Override
            public int getPixelStride() {
                return pixelStride;
            }

            @Override
            public @NonNull ByteBuffer getBuffer() {
                return mBuffer;
            }
        };
    }

    private static ByteBuffer createBufferWithRampPattern(
            int width, int height, int pixelStride, int initialValue) {
        int rowStride = width * pixelStride;
        ByteBuffer buffer = ByteBuffer.allocateDirect(rowStride * height);
        int value = initialValue;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                buffer.position(y * rowStride + x * pixelStride);
                buffer.put((byte) (value++ & 0xFF));
            }
        }
        return buffer;
    }

    private static void checkOutputIsNearestNeighborDownsampledInput(
            ImageProxy inputImage, ImageProxy outputImage, int downsamplingFactor) {
        ImageProxy.PlaneProxy[] inputPlanes = inputImage.getPlanes();
        ImageProxy.PlaneProxy[] outputPlanes = outputImage.getPlanes();
        for (int c = 0; c < 3; ++c) {
            ByteBuffer inputBuffer = inputPlanes[c].getBuffer();
            ByteBuffer outputBuffer = outputPlanes[c].getBuffer();
            inputBuffer.rewind();
            outputBuffer.rewind();
            int divisor = (c == 0) ? 1 : 2;
            int inputRowStride = inputPlanes[c].getRowStride();
            int inputPixelStride = inputPlanes[c].getPixelStride();
            int outputRowStride = outputPlanes[c].getRowStride();
            int outputPixelStride = outputPlanes[c].getPixelStride();
            for (int y = 0; y < outputImage.getHeight() / divisor; ++y) {
                for (int x = 0; x < outputImage.getWidth() / divisor; ++x) {
                    byte inputPixel =
                            inputBuffer.get(
                                    y * downsamplingFactor * inputRowStride
                                            + x * downsamplingFactor * inputPixelStride);
                    byte outputPixel =
                            outputBuffer.get(y * outputRowStride + x * outputPixelStride);
                    assertThat(outputPixel).isEqualTo(inputPixel);
                }
            }
        }
    }

    private static void checkOutputIsAveragingDownsampledInput(
            ImageProxy inputImage, ImageProxy outputImage, int downsamplingFactor) {
        ImageProxy.PlaneProxy[] inputPlanes = inputImage.getPlanes();
        ImageProxy.PlaneProxy[] outputPlanes = outputImage.getPlanes();
        for (int c = 0; c < 3; ++c) {
            ByteBuffer inputBuffer = inputPlanes[c].getBuffer();
            ByteBuffer outputBuffer = outputPlanes[c].getBuffer();
            inputBuffer.rewind();
            outputBuffer.rewind();
            int divisor = (c == 0) ? 1 : 2;
            int inputRowStride = inputPlanes[c].getRowStride();
            int inputPixelStride = inputPlanes[c].getPixelStride();
            int outputRowStride = outputPlanes[c].getRowStride();
            int outputPixelStride = outputPlanes[c].getPixelStride();
            for (int y = 0; y < outputImage.getHeight() / divisor; ++y) {
                for (int x = 0; x < outputImage.getWidth() / divisor; ++x) {
                    byte inputPixelA =
                            inputBuffer.get(
                                    y * downsamplingFactor * inputRowStride
                                            + x * downsamplingFactor * inputPixelStride);
                    byte inputPixelB =
                            inputBuffer.get(
                                    y * downsamplingFactor * inputRowStride
                                            + (x * downsamplingFactor + 1) * inputPixelStride);
                    byte inputPixelC =
                            inputBuffer.get(
                                    (y * downsamplingFactor + 1) * inputRowStride
                                            + x * downsamplingFactor * inputPixelStride);
                    byte inputPixelD =
                            inputBuffer.get(
                                    (y * downsamplingFactor + 1) * inputRowStride
                                            + (x * downsamplingFactor + 1) * inputPixelStride);
                    byte averaged =
                            (byte)
                                    ((((inputPixelA & 0xFF)
                                            + (inputPixelB & 0xFF)
                                            + (inputPixelC & 0xFF)
                                            + (inputPixelD & 0xFF))
                                            / 4)
                                            & 0xFF);
                    byte outputPixel =
                            outputBuffer.get(y * outputRowStride + x * outputPixelStride);
                    assertThat(outputPixel).isEqualTo(averaged);
                }
            }
        }
    }

    @Test
    public void nearestNeighborDownsamplingBy2X_whenUVPlanesHavePixelStride1() {
        ImageProxy inputImage = createYuv420Image(/*uvPixelStride=*/ 1);
        int downsamplingFactor = 2;
        ImageProxy outputImage =
                ImageProxyDownsampler.downsample(
                        inputImage,
                        WIDTH / downsamplingFactor,
                        HEIGHT / downsamplingFactor,
                        ImageProxyDownsampler.DownsamplingMethod.NEAREST_NEIGHBOR);

        checkOutputIsNearestNeighborDownsampledInput(inputImage, outputImage, downsamplingFactor);
    }

    @Test
    public void nearestNeighborDownsamplingBy2X_whenUVPlanesHavePixelStride2() {
        ImageProxy inputImage = createYuv420Image(/*uvPixelStride=*/ 2);
        int downsamplingFactor = 2;
        ImageProxy outputImage =
                ImageProxyDownsampler.downsample(
                        inputImage,
                        WIDTH / downsamplingFactor,
                        HEIGHT / downsamplingFactor,
                        ImageProxyDownsampler.DownsamplingMethod.NEAREST_NEIGHBOR);

        checkOutputIsNearestNeighborDownsampledInput(inputImage, outputImage, downsamplingFactor);
    }

    @Test
    public void averagingDownsamplingBy2X_whenUVPlanesHavePixelStride1() {
        ImageProxy inputImage = createYuv420Image(/*uvPixelStride=*/ 1);
        int downsamplingFactor = 2;
        ImageProxy outputImage =
                ImageProxyDownsampler.downsample(
                        inputImage,
                        WIDTH / downsamplingFactor,
                        HEIGHT / downsamplingFactor,
                        ImageProxyDownsampler.DownsamplingMethod.AVERAGING);

        checkOutputIsAveragingDownsampledInput(inputImage, outputImage, downsamplingFactor);
    }

    @Test
    public void averagingDownsamplingBy2X_whenUVPlanesHavePixelStride2() {
        ImageProxy inputImage = createYuv420Image(/*uvPixelStride=*/ 2);
        int downsamplingFactor = 2;
        ImageProxy outputImage =
                ImageProxyDownsampler.downsample(
                        inputImage,
                        WIDTH / downsamplingFactor,
                        HEIGHT / downsamplingFactor,
                        ImageProxyDownsampler.DownsamplingMethod.AVERAGING);

        checkOutputIsAveragingDownsampledInput(inputImage, outputImage, downsamplingFactor);
    }
}
