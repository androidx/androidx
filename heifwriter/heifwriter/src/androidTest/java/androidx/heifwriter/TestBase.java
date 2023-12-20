/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static androidx.heifwriter.HeifWriter.INPUT_MODE_BITMAP;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Base class holding common utilities for {@link HeifWriterTest} and {@link AvifWriterTest}.
 */
public class TestBase {
    private static final String TAG = HeifWriterTest.class.getSimpleName();

    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    private static final byte[][] TEST_YUV_COLORS = {
        {(byte) 255, (byte) 0, (byte) 0},
        {(byte) 255, (byte) 0, (byte) 255},
        {(byte) 255, (byte) 255, (byte) 255},
        {(byte) 255, (byte) 255, (byte) 0},
    };
    private static final byte[][] TEST_YUV_10BIT_COLORS = {
        {(byte) 1023, (byte) 0, (byte) 0},
        {(byte) 1023, (byte) 0, (byte) 1023},
        {(byte) 1023, (byte) 1023, (byte) 1023},
        {(byte) 1023, (byte) 1023, (byte) 0},
    };
    private static final Color COLOR_BLOCK =
        Color.valueOf(1.0f, 1.0f, 1.0f);
    private static final Color[] COLOR_BARS = {
        Color.valueOf(0.0f, 0.0f, 0.0f),
        Color.valueOf(0.0f, 0.0f, 0.64f),
        Color.valueOf(0.0f, 0.64f, 0.0f),
        Color.valueOf(0.0f, 0.64f, 0.64f),
        Color.valueOf(0.64f, 0.0f, 0.0f),
        Color.valueOf(0.64f, 0.0f, 0.64f),
        Color.valueOf(0.64f, 0.64f, 0.0f),
    };
    private static final float MAX_DELTA = 0.025f;
    private static final int BORDER_WIDTH = 16;

    protected EglWindowSurface mInputEglSurface;
    protected Handler mHandler;
    protected int mInputIndex;
    protected boolean mHighBitDepthEnabled = false;

    protected long computePresentationTime(int frameIndex) {
        return 132 + (long)frameIndex * 1000000;
    }

    protected void fillYuvBuffer(int frameIndex, @NonNull byte[] data, int width, int height,
        @Nullable FileInputStream inputStream) throws IOException {
        if (inputStream != null) {
            inputStream.read(data);
        } else {
            byte[] color;
            int sizeY = width * height;
            if (!mHighBitDepthEnabled) {
                color = TEST_YUV_COLORS[frameIndex % TEST_YUV_COLORS.length];
                Arrays.fill(data, 0, sizeY, color[0]);
                Arrays.fill(data, sizeY, sizeY * 5 / 4, color[1]);
                Arrays.fill(data, sizeY * 5 / 4, sizeY * 3 / 2, color[2]);

            } else {
                color = TEST_YUV_10BIT_COLORS[frameIndex % TEST_YUV_10BIT_COLORS.length];
                Arrays.fill(data, 0, sizeY, color[0]);
                Arrays.fill(data, sizeY, sizeY * 2, color[1]);
                Arrays.fill(data, sizeY * 2, sizeY * 3, color[2]);
            }
        }
    }

    protected static Rect getColorBarRect(int index, int width, int height) {
        int barWidth = (width - BORDER_WIDTH * 2) / COLOR_BARS.length;
        return new Rect(BORDER_WIDTH + barWidth * index, BORDER_WIDTH,
            BORDER_WIDTH + barWidth * (index + 1), height - BORDER_WIDTH);
    }

    protected static Rect getColorBlockRect(int index, int width, int height) {
        int blockCenterX = (width / 5) * (index % 4 + 1);
        return new Rect(blockCenterX - width / 10, height / 6,
            blockCenterX + width / 10, height / 3);
    }

    protected void generateSurfaceFrame(int frameIndex, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        for (int i = 0; i < COLOR_BARS.length; i++) {
            Rect r = getColorBarRect(i, width, height);

            GLES20.glScissor(r.left, r.top, r.width(), r.height());
            final Color color = COLOR_BARS[i];
            GLES20.glClearColor(color.red(), color.green(), color.blue(), 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        Rect r = getColorBlockRect(frameIndex, width, height);
        GLES20.glScissor(r.left, r.top, r.width(), r.height());
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        r.inset(BORDER_WIDTH, BORDER_WIDTH);
        GLES20.glScissor(r.left, r.top, r.width(), r.height());
        GLES20.glClearColor(COLOR_BLOCK.red(), COLOR_BLOCK.green(), COLOR_BLOCK.blue(), 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Determines if two color values are approximately equal.
     */
    protected static boolean approxEquals(Color expected, Color actual) {
        return (Math.abs(expected.red() - actual.red()) <= MAX_DELTA)
            && (Math.abs(expected.green() - actual.green()) <= MAX_DELTA)
            && (Math.abs(expected.blue() - actual.blue()) <= MAX_DELTA);
    }

    protected void verifyResult(
        String filename, int width, int height, int rotation,
        int imageCount, int primary, boolean useGrid, boolean checkColor)
        throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filename);
        String hasImage = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE);
        if (!"yes".equals(hasImage)) {
            throw new Exception("No images found in file " + filename);
        }
        assertEquals("Wrong width", width,
            Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH)));
        assertEquals("Wrong height", height,
            Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT)));
        assertEquals("Wrong rotation", rotation,
            Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_ROTATION)));
        assertEquals("Wrong image count", imageCount,
            Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT)));
        assertEquals("Wrong primary index", primary,
            Integer.parseInt(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_IMAGE_PRIMARY)));
        try {
            retriever.release();
        } catch (IOException e) {
            // Nothing we can  do about it.
        }

        if (useGrid) {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(filename);
            MediaFormat format = extractor.getTrackFormat(0);
            int tileWidth = format.getInteger(MediaFormat.KEY_TILE_WIDTH);
            int tileHeight = format.getInteger(MediaFormat.KEY_TILE_HEIGHT);
            int gridRows = format.getInteger(MediaFormat.KEY_GRID_ROWS);
            int gridCols = format.getInteger(MediaFormat.KEY_GRID_COLUMNS);
            assertTrue("Wrong tile width or grid cols",
                ((width + tileWidth - 1) / tileWidth) == gridCols);
            assertTrue("Wrong tile height or grid rows",
                ((height + tileHeight - 1) / tileHeight) == gridRows);
            extractor.release();
        }

        if (checkColor) {
            Bitmap bitmap = BitmapFactory.decodeFile(filename);

            for (int i = 0; i < COLOR_BARS.length; i++) {
                Rect r = getColorBarRect(i, width, height);
                assertTrue("Color bar " + i + " doesn't match", approxEquals(COLOR_BARS[i],
                    Color.valueOf(bitmap.getPixel(r.centerX(), r.centerY()))));
            }

            Rect r = getColorBlockRect(primary, width, height);
            assertTrue("Color block doesn't match", approxEquals(COLOR_BLOCK,
                Color.valueOf(bitmap.getPixel(r.centerX(), height - r.centerY()))));

            bitmap.recycle();
        }
    }

    protected void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    protected int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }

    protected boolean hasEncoderForMime(String mime) {
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (info.isEncoder()) {
                for (String type : info.getSupportedTypes()) {
                    if (type.equalsIgnoreCase(mime)) {
                        Log.i(TAG, "found codec " + info.getName() + " for mime " + mime);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void drawFrame(int width, int height) {
        mInputEglSurface.makeCurrent();
        generateSurfaceFrame(mInputIndex, width, height);
        mInputEglSurface.setPresentationTime(1000 * computePresentationTime(mInputIndex));
        mInputEglSurface.swapBuffers();
        mInputIndex++;
    }

    protected static class TestConfig {
        final int mInputMode;
        final boolean mUseGrid;
        final boolean mUseHandler;
        final boolean mUseHighBitDepth;
        final int mMaxNumImages;
        final int mActualNumImages;
        final int mWidth;
        final int mHeight;
        final int mRotation;
        final int mQuality;
        final String mInputPath;
        final String mOutputPath;
        final Bitmap[] mBitmaps;

        TestConfig(int inputMode, boolean useGrid, boolean useHandler, boolean useHighBitDepth,
            int maxNumImages, int actualNumImages, int width, int height, int rotation,
            int quality, String inputPath, String outputPath, Bitmap[] bitmaps) {
            mInputMode = inputMode;
            mUseGrid = useGrid;
            mUseHandler = useHandler;
            mUseHighBitDepth = useHighBitDepth;
            mMaxNumImages = maxNumImages;
            mActualNumImages = actualNumImages;
            mWidth = width;
            mHeight = height;
            mRotation = rotation;
            mQuality = quality;
            mInputPath = inputPath;
            mOutputPath = outputPath;
            mBitmaps = bitmaps;
        }

        static class Builder {
            final int mInputMode;
            final boolean mUseGrid;
            final boolean mUseHandler;
            boolean mUseHighBitDepth;
            int mMaxNumImages;
            int mNumImages;
            int mWidth;
            int mHeight;
            int mRotation;
            final int mQuality;
            String mInputPath;
            final String mOutputPath;
            Bitmap[] mBitmaps;
            boolean mNumImagesSetExplicitly;


            Builder(int inputMode, boolean useGrids, boolean useHandler, String outputFileName) {
                mInputMode = inputMode;
                mUseGrid = useGrids;
                mUseHandler = useHandler;
                mUseHighBitDepth = false;
                mMaxNumImages = mNumImages = 4;
                mWidth = 1920;
                mHeight = 1080;
                mRotation = 0;
                mQuality = 100;
                mOutputPath = new File(getApplicationContext().getExternalFilesDir(null),
                    outputFileName).getAbsolutePath();
            }

            Builder setInputPath(String inputPath) {
                mInputPath = (mInputMode == INPUT_MODE_BITMAP) ? inputPath : null;
                return this;
            }

            Builder setNumImages(int numImages) {
                mNumImagesSetExplicitly = true;
                mNumImages = numImages;
                return this;
            }

            Builder setRotation(int rotation) {
                mRotation = rotation;
                return this;
            }

            Builder setHighBitDepthEnabled(boolean useHighBitDepth) {
                mUseHighBitDepth = useHighBitDepth;
                return this;
            }

            private void loadBitmapInputs() {
                if (mInputMode != INPUT_MODE_BITMAP) {
                    return;
                }
                if (!mUseHighBitDepth) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mInputPath);
                    String hasImage = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE);
                    if (!"yes".equals(hasImage)) {
                        throw new IllegalArgumentException("no bitmap found!");
                    }
                    mMaxNumImages = Math.min(mMaxNumImages,
                        Integer.parseInt(retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT)));
                    if (!mNumImagesSetExplicitly) {
                        mNumImages = mMaxNumImages;
                    }
                    mBitmaps = new Bitmap[mMaxNumImages];
                    for (int i = 0; i < mBitmaps.length; i++) {
                        mBitmaps[i] = retriever.getImageAtIndex(i);
                    }
                    mWidth = mBitmaps[0].getWidth();
                    mHeight = mBitmaps[0].getHeight();
                    try {
                        retriever.release();
                    } catch (IOException e) {
                        // Nothing we can  do about it.
                    }
                } else {
                    mMaxNumImages = 1;
                    mNumImages = 1;
                }
            }

            private void cleanupStaleOutputs() {
                File outputFile = new File(mOutputPath);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            }

            TestConfig build() {
                cleanupStaleOutputs();
                loadBitmapInputs();

                return new TestConfig(mInputMode, mUseGrid, mUseHandler, mUseHighBitDepth,
                    mMaxNumImages, mNumImages, mWidth, mHeight, mRotation, mQuality, mInputPath,
                    mOutputPath, mBitmaps);
            }
        }

        @Override
        public String toString() {
            return "TestConfig"
                + ": mInputMode " + mInputMode
                + ", mUseGrid " + mUseGrid
                + ", mUseHandler " + mUseHandler
                + ", mMaxNumImages " + mMaxNumImages
                + ", mNumImages " + mActualNumImages
                + ", mWidth " + mWidth
                + ", mHeight " + mHeight
                + ", mRotation " + mRotation
                + ", mQuality " + mQuality
                + ", mInputPath " + mInputPath
                + ", mOutputPath " + mOutputPath;
        }
    }
}