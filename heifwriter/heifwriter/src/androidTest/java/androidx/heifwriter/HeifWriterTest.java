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

import static androidx.heifwriter.HeifWriter.INPUT_MODE_BITMAP;
import static androidx.heifwriter.HeifWriter.INPUT_MODE_BUFFER;
import static androidx.heifwriter.HeifWriter.INPUT_MODE_SURFACE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.heifwriter.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test {@link HeifWriter}.
 */
@RunWith(AndroidJUnit4.class)
@FlakyTest
public class HeifWriterTest extends TestBase {
    private static final String TAG = HeifWriterTest.class.getSimpleName();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule1 =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final boolean DEBUG = true;
    private static final boolean DUMP_YUV_INPUT = false;

    private static final String HEIFWRITER_INPUT = "heifwriter_input.heic";
    private static final int[] IMAGE_RESOURCES = new int[] {
        R.raw.heifwriter_input
    };
    private static final String[] IMAGE_FILENAMES = new String[] {
        HEIFWRITER_INPUT
    };
    private static final String OUTPUT_FILENAME = "output.heic";

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String outputPath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();

            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = getApplicationContext()
                    .getResources().openRawResource(IMAGE_RESOURCES[i]);
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
            String imageFilePath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();
            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    @Ignore // b/239415930
    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, false, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @Ignore // b/239415930
    @Test
    @LargeTest
    public void testInputBuffer_Grid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, true, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @Ignore // b/239415930
    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, false, true, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @Ignore // b/239415930
    @Test
    @LargeTest
    public void testInputBuffer_Grid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, true, true, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputSurface_NoGrid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_SURFACE, false, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }
    //
    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputSurface_Grid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_SURFACE, true, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputSurface_NoGrid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_SURFACE, false, true, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputSurface_Grid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_SURFACE, true, true, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }


    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputBitmap_NoGrid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BITMAP, false, false, OUTPUT_FILENAME);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputBitmap_Grid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BITMAP, true, false, OUTPUT_FILENAME);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputBitmap_NoGrid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BITMAP, false, true, OUTPUT_FILENAME);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @LargeTest
    public void testInputBitmap_Grid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BITMAP, true, true, OUTPUT_FILENAME);
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String inputPath = new File(getApplicationContext().getExternalFilesDir(null),
                IMAGE_FILENAMES[i]).getAbsolutePath();
            doTestForVariousNumberImages(builder.setInputPath(inputPath));
        }
    }

    @SdkSuppress(maxSdkVersion = 29) // b/192261638
    @Test
    @SmallTest
    public void testCloseWithoutStart() throws Throwable {
        if (shouldSkip()) return;

        final String outputPath = new File(getApplicationContext().getExternalFilesDir(null),
            OUTPUT_FILENAME).getAbsolutePath();
        HeifWriter heifWriter = new HeifWriter.Builder(
            outputPath, 1920, 1080, INPUT_MODE_SURFACE)
            .setGridEnabled(true)
            .setMaxImages(4)
            .setQuality(90)
            .setPrimaryIndex(0)
            .setHandler(mHandler)
            .build();

        heifWriter.close();
    }

    private void doTestForVariousNumberImages(TestConfig.Builder builder) throws Exception {
        builder.setNumImages(4);
        doTest(builder.setRotation(270).build());
        doTest(builder.setRotation(180).build());
        doTest(builder.setRotation(90).build());
        doTest(builder.setRotation(0).build());
        doTest(builder.setNumImages(1).build());
        doTest(builder.setNumImages(8).build());
    }

    private boolean shouldSkip() {
        return !hasEncoderForMime(MediaFormat.MIMETYPE_VIDEO_HEVC)
            && !hasEncoderForMime(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC);
    }

    private static byte[] mYuvData;
    private void doTest(final TestConfig config) throws Exception {
        final int width = config.mWidth;
        final int height = config.mHeight;
        final int actualNumImages = config.mActualNumImages;

        mInputIndex = 0;
        HeifWriter heifWriter = null;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (DEBUG)
                Log.d(TAG, "started: " + config);

            heifWriter = new HeifWriter.Builder(
                new File(getApplicationContext().getExternalFilesDir(null),
                    OUTPUT_FILENAME).getAbsolutePath(), width, height, config.mInputMode)
                .setRotation(config.mRotation)
                .setGridEnabled(config.mUseGrid)
                .setMaxImages(config.mMaxNumImages)
                .setQuality(config.mQuality)
                .setPrimaryIndex(config.mMaxNumImages - 1)
                .setHandler(config.mUseHandler ? mHandler : null)
                .build();

            if (config.mInputMode == INPUT_MODE_SURFACE) {
                mInputEglSurface = new EglWindowSurface(heifWriter.getInputSurface());
            }

            heifWriter.start();

            if (config.mInputMode == INPUT_MODE_BUFFER) {
                if (mYuvData == null || mYuvData.length != width * height * 3 / 2) {
                    mYuvData = new byte[width * height * 3 / 2];
                }

                if (config.mInputPath != null) {
                    inputStream = new FileInputStream(config.mInputPath);
                }

                if (DUMP_YUV_INPUT) {
                    File outputFile = new File("/sdcard/input.yuv");
                    outputFile.createNewFile();
                    outputStream = new FileOutputStream(outputFile);
                }

                for (int i = 0; i < actualNumImages; i++) {
                    if (DEBUG)
                        Log.d(TAG, "fillYuvBuffer: " + i);
                    fillYuvBuffer(i, mYuvData, width, height, inputStream);
                    if (DUMP_YUV_INPUT) {
                        Log.d(TAG, "@@@ dumping input YUV");
                        outputStream.write(mYuvData);
                    }
                    heifWriter.addYuvBuffer(ImageFormat.YUV_420_888, mYuvData);
                }
            } else if (config.mInputMode == INPUT_MODE_SURFACE) {
                // The input surface is a surface texture using single buffer mode, draws will be
                // blocked until onFrameAvailable is done with the buffer, which is dependant on
                // how fast MediaCodec processes them, which is further dependent on how fast the
                // MediaCodec callbacks are handled. We can't put draws on the same looper that
                // handles MediaCodec callback, it will cause deadlock.
                for (int i = 0; i < actualNumImages; i++) {
                    if (DEBUG)
                        Log.d(TAG, "drawFrame: " + i);
                    drawFrame(width, height);
                }
                heifWriter.setInputEndOfStreamTimestamp(
                    1000 * computePresentationTime(actualNumImages - 1));
            } else if (config.mInputMode == INPUT_MODE_BITMAP) {
                Bitmap[] bitmaps = config.mBitmaps;
                for (int i = 0; i < Math.min(bitmaps.length, actualNumImages); i++) {
                    if (DEBUG)
                        Log.d(TAG, "addBitmap: " + i);
                    heifWriter.addBitmap(bitmaps[i]);
                    bitmaps[i].recycle();
                }
            }

            heifWriter.stop(10000);
            // The test sets the primary index to the last image.
            // However, if we're testing early abort, the last image will not be
            // present and the muxer is supposed to set it to 0 by default.
            int expectedPrimary = config.mMaxNumImages - 1;
            int expectedImageCount = config.mMaxNumImages;
            if (actualNumImages < config.mMaxNumImages) {
                expectedPrimary = 0;
                expectedImageCount = actualNumImages;
            }
            verifyResult(config.mOutputPath, width, height, config.mRotation,
                expectedImageCount, expectedPrimary, config.mUseGrid,
                config.mInputMode == INPUT_MODE_SURFACE);
            if (DEBUG)
                Log.d(TAG, "finished: PASS");
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }

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
}