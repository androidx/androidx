/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.heifwriter;

import static android.support.test.InstrumentationRegistry.getContext;

import static androidx.heifwriter.HeifWriter.INPUT_MODE_BITMAP;
import static androidx.heifwriter.HeifWriter.INPUT_MODE_BUFFER;
import static androidx.heifwriter.HeifWriter.INPUT_MODE_SURFACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import androidx.heifwriter.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Test {@link HeifWriter}.
 */
@RunWith(AndroidJUnit4.class)
public class HeifWriterTest {
    private static final String TAG = HeifWriterTest.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final boolean DUMP_YUV_INPUT = false;

    private static byte[][] TEST_COLORS = {
            {(byte) 255, (byte) 0, (byte) 0},
            {(byte) 255, (byte) 0, (byte) 255},
            {(byte) 255, (byte) 255, (byte) 255},
            {(byte) 255, (byte) 255, (byte) 0},
    };

    private static final String TEST_HEIC = "test.heic";
    private static final int[] IMAGE_RESOURCES = new int[] {
            R.raw.test
    };
    private static final String[] IMAGE_FILENAMES = new String[] {
            TEST_HEIC
    };
    private static final String OUTPUT_FILENAME = "output.heic";

    private EglWindowSurface mInputEglSurface;
    private Handler mHandler;
    private int mInputIndex;

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String outputPath = new File(Environment.getExternalStorageDirectory(),
                    IMAGE_FILENAMES[i]).getAbsolutePath();

            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = getContext().getResources().openRawResource(IMAGE_RESOURCES[i]);
                outputStream = new FileOutputStream(outputPath);
                copy(inputStream, outputStream);
            } finally {
                closeQuietly(inputStream);
                closeQuietly(outputStream);
            }
        }

        HandlerThread handlerThread = new HandlerThread(
                "HeifEncoderThread", Process.THREAD_PRIORITY_FOREGROUND);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    @After
    public void tearDown() throws Exception {
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String imageFilePath =
                    new File(Environment.getExternalStorageDirectory(), IMAGE_FILENAMES[i])
                            .getAbsolutePath();
            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BUFFER, false, false);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBuffer_Grid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BUFFER, true, false);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BUFFER, false, true);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBuffer_Grid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BUFFER, true, true);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputSurface_NoGrid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_SURFACE, false, false);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputSurface_Grid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_SURFACE, true, false);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputSurface_NoGrid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_SURFACE, false, true);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputSurface_Grid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_SURFACE, true, true);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBitmap_NoGrid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BITMAP, false, false);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(Environment.getExternalStorageDirectory(),
                    IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @Test
    @LargeTest
    public void testInputBitmap_Grid_NoHandler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BITMAP, true, false);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(Environment.getExternalStorageDirectory(),
                    IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @Test
    @LargeTest
    public void testInputBitmap_NoGrid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BITMAP, false, true);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(Environment.getExternalStorageDirectory(),
                    IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @Test
    @LargeTest
    public void testInputBitmap_Grid_Handler() throws Throwable {
        TestConfig.Builder builder = new TestConfig.Builder(INPUT_MODE_BITMAP, true, true);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(Environment.getExternalStorageDirectory(),
                    IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    private void doTestForVariousNumberImages(TestConfig.Builder builder) throws Exception {
        doTest(builder.setNumImages(4).build());
        doTest(builder.setNumImages(1).build());
        doTest(builder.setNumImages(8).build());
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }

    private static class TestConfig {
        final int inputMode;
        final boolean useGrid;
        final boolean useHandler;
        final int maxNumImages;
        final int numImages;
        final int width;
        final int height;
        final int quality;
        final String inputPath;
        final String outputPath;
        final Bitmap[] bitmaps;

        TestConfig(int _inputMode, boolean _useGrid, boolean _useHandler,
                   int _maxNumImage, int _numImages, int _width, int _height, int _quality,
                   String _inputPath, String _outputPath, Bitmap[] _bitmaps) {
            inputMode = _inputMode;
            useGrid = _useGrid;
            useHandler = _useHandler;
            maxNumImages = _maxNumImage;
            numImages = _numImages;
            width = _width;
            height = _height;
            quality = _quality;
            inputPath = _inputPath;
            outputPath = _outputPath;
            bitmaps = _bitmaps;
        }

        static class Builder {
            final int inputMode;
            final boolean useGrid;
            final boolean useHandler;
            int maxNumImages;
            int numImages;
            int width;
            int height;
            int quality;
            String inputPath;
            final String outputPath;
            Bitmap[] bitmaps;

            boolean numImagesSetExplicitly;


            Builder(int _inputMode, boolean _useGrids, boolean _useHandler) {
                inputMode = _inputMode;
                useGrid = _useGrids;
                useHandler = _useHandler;
                maxNumImages = numImages = 4;
                width = 1920;
                height = 1080;
                quality = 100;
                outputPath = new File(Environment.getExternalStorageDirectory(),
                        OUTPUT_FILENAME).getAbsolutePath();
            }

            Builder setInputPath(String _inputPath) {
                inputPath = (inputMode == INPUT_MODE_BITMAP) ? _inputPath : null;
                return this;
            }

            Builder setNumImages(int _numImages) {
                numImagesSetExplicitly = true;
                numImages = _numImages;
                return this;
            }

            private void loadBitmapInputs() {
                if (inputMode != INPUT_MODE_BITMAP) {
                    return;
                }
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(inputPath);
                String hasImage = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE);
                if (!"yes".equals(hasImage)) {
                    throw new IllegalArgumentException("no bitmap found!");
                }
                width = Integer.parseInt(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH));
                height = Integer.parseInt(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT));
                maxNumImages = Math.min(maxNumImages, Integer.parseInt(retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT)));
                if (!numImagesSetExplicitly) {
                    numImages = maxNumImages;
                }
                bitmaps = new Bitmap[maxNumImages];
                for (int i = 0; i < bitmaps.length; i++) {
                    bitmaps[i] = retriever.getImageAtIndex(i);
                }
                retriever.release();
            }

            private void cleanupStaleOutputs() {
                File outputFile = new File(outputPath);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            }

            TestConfig build() {
                cleanupStaleOutputs();
                loadBitmapInputs();

                return new TestConfig(inputMode, useGrid, useHandler, maxNumImages, numImages,
                        width, height, quality, inputPath, outputPath, bitmaps);
            }
        }

        @Override
        public String toString() {
            return "TestConfig" +
                    ": inputMode " + inputMode +
                    ", useGrid " + useGrid +
                    ", useHandler " + useHandler +
                    ", maxNumImages " + maxNumImages +
                    ", numImages " + numImages +
                    ", width " + width +
                    ", height " + height +
                    ", quality " + quality +
                    ", inputPath " + inputPath +
                    ", outputPath " + outputPath;
        }
    }

    private void doTest(TestConfig testConfig) throws Exception {
        int width = testConfig.width;
        int height = testConfig.height;
        int numImages = testConfig.numImages;

        mInputIndex = 0;
        HeifWriter heifWriter = null;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (DEBUG) Log.d(TAG, "started: " + testConfig);

            heifWriter = new HeifWriter(testConfig.outputPath, width, height,
                    testConfig.useGrid,
                    testConfig.quality,
                    testConfig.maxNumImages,
                    testConfig.maxNumImages - 1,
                    testConfig.inputMode,
                    testConfig.useHandler ? mHandler : null);

            if (testConfig.inputMode == INPUT_MODE_SURFACE) {
                mInputEglSurface = new EglWindowSurface(heifWriter.getInputSurface());
            }

            heifWriter.start();

            if (testConfig.inputMode == INPUT_MODE_BUFFER) {
                byte[] data = new byte[width * height * 3 / 2];

                if (testConfig.inputPath != null) {
                    inputStream = new FileInputStream(testConfig.inputPath);
                }

                if (DUMP_YUV_INPUT) {
                    File outputFile = new File("/sdcard/input.yuv");
                    outputFile.createNewFile();
                    outputStream = new FileOutputStream(outputFile);
                }

                for (int i = 0; i < numImages; i++) {
                    if (DEBUG) Log.d(TAG, "fillYuvBuffer: " + i);
                    fillYuvBuffer(i, data, width, height, inputStream);
                    if (DUMP_YUV_INPUT) {
                        Log.d(TAG, "@@@ dumping input YUV");
                        outputStream.write(data);
                    }
                    heifWriter.addYuvBuffer(ImageFormat.YUV_420_888, data);
                }
            } else if (testConfig.inputMode == INPUT_MODE_SURFACE) {
                // The input surface is a surface texture using single buffer mode, draws will be
                // blocked until onFrameAvailable is done with the buffer, which is dependant on
                // how fast MediaCodec processes them, which is further dependent on how fast the
                // MediaCodec callbacks are handled. We can't put draws on the same looper that
                // handles MediaCodec callback, it will cause deadlock.
                for (int i = 0; i < numImages; i++) {
                    if (DEBUG) Log.d(TAG, "drawFrame: " + i);
                    drawFrame(width, height);
                }
                heifWriter.setInputEndOfStreamTimestamp(
                        1000 * computePresentationTime(numImages - 1));
            } else if (testConfig.inputMode == INPUT_MODE_BITMAP) {
                Bitmap[] bitmaps = testConfig.bitmaps;
                for (int i = 0; i < Math.min(bitmaps.length, numImages); i++) {
                    if (DEBUG) Log.d(TAG, "addBitmap: " + i);
                    heifWriter.addBitmap(bitmaps[i]);
                    bitmaps[i].recycle();
                }
            }

            heifWriter.stop(3000);
            verifyResult(testConfig.outputPath, width, height, testConfig.useGrid,
                    Math.min(numImages, testConfig.maxNumImages));
            if (DEBUG) Log.d(TAG, "finished: PASS");
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {}

            if (heifWriter != null) {
                heifWriter.close();
                heifWriter = null;
            }
            if (mInputEglSurface != null) {
                // This also releases the surface from encoder.
                mInputEglSurface.release();
                mInputEglSurface = null;
            }
        }
    }

    private long computePresentationTime(int frameIndex) {
        return 132 + (long)frameIndex * 1000000;
    }

    private void fillYuvBuffer(int frameIndex, @NonNull byte[] data, int width, int height,
                               @Nullable FileInputStream inputStream) throws IOException {
        if (inputStream != null) {
            inputStream.read(data);
        } else {
            byte[] color = TEST_COLORS[frameIndex % TEST_COLORS.length];
            int sizeY = width * height;
            Arrays.fill(data, 0, sizeY, color[0]);
            Arrays.fill(data, sizeY, sizeY * 5 / 4, color[1]);
            Arrays.fill(data, sizeY * 5 / 4, sizeY * 3 / 2, color[2]);
        }
    }

    private void drawFrame(int width, int height) {
        mInputEglSurface.makeCurrent();
        generateSurfaceFrame(mInputIndex, width, height);
        mInputEglSurface.setPresentationTime(1000 * computePresentationTime(mInputIndex));
        mInputEglSurface.swapBuffers();
        mInputIndex++;
    }

    private void generateSurfaceFrame(int frameIndex, int width, int height) {
        frameIndex %= 4;

        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        int startX, startY;
        int borderWidth = 16;
        for (int i = 0; i < 7; i++) {
            startX = (width - borderWidth * 2) * i / 7 + borderWidth;
            GLES20.glScissor(startX, borderWidth,
                    (width - borderWidth * 2) / 7, height - borderWidth * 2);
            GLES20.glClearColor(((7 - i) & 0x4) * 0.16f,
                    ((7 - i) & 0x2) * 0.32f,
                    ((7 - i) & 0x1) * 0.64f,
                    1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        startX = (width / 6) + (width / 6) * frameIndex;
        startY = height / 4;
        GLES20.glScissor(startX, startY, width / 6, height / 3);
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glScissor(startX + borderWidth, startY + borderWidth,
                width / 6 - borderWidth * 2, height / 3 - borderWidth * 2);
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    private void verifyResult(
            String filename, int width, int height, boolean useGrid, int numImages)
            throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filename);
        String hasImage = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE);
        if (!"yes".equals(hasImage)) {
            throw new Exception("No images found in file " + filename);
        }
        assertEquals("Wrong image count", numImages,
                Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT)));
        assertEquals("Wrong width", width,
                Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH)));
        assertEquals("Wrong height", height,
                Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT)));
        retriever.release();

        if (useGrid) {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(filename);
            MediaFormat format = extractor.getTrackFormat(0);
            int gridWidth = format.getInteger(MediaFormat.KEY_GRID_WIDTH);
            int gridHeight = format.getInteger(MediaFormat.KEY_GRID_HEIGHT);
            assertEquals("Wrong grid width", 512, gridWidth);
            assertEquals("Wrong grid height", 512, gridHeight);
            extractor.release();
        }
    }
}
