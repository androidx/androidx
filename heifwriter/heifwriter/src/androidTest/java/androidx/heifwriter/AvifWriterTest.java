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

import static androidx.heifwriter.AvifWriter.INPUT_MODE_BITMAP;
import static androidx.heifwriter.AvifWriter.INPUT_MODE_BUFFER;
import static androidx.heifwriter.AvifWriter.INPUT_MODE_SURFACE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertNotNull;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
 * Test {@link AvifWriter}.
 */
@RunWith(AndroidJUnit4.class)
@FlakyTest
public class AvifWriterTest extends TestBase {
    private static final String TAG = AvifWriterTest.class.getSimpleName();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule1 =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final boolean DEBUG = true;
    private static final boolean DUMP_YUV_INPUT = false;

    private static final String AVIFWRITER_INPUT = "heifwriter_input.heic";
    private static final int[] IMAGE_RESOURCES = new int[] {
        R.raw.heifwriter_input
    };
    private static final String[] IMAGE_FILENAMES = new String[] {
        AVIFWRITER_INPUT
    };
    private static final String OUTPUT_FILENAME = "output.avif";

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
            "AvifEncoderThread", Process.THREAD_PRIORITY_FOREGROUND);
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

    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, false, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBuffer_Grid_NoHandler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, true, false, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

    @Test
    @LargeTest
    public void testInputBuffer_NoGrid_Handler() throws Throwable {
        if (shouldSkip()) return;

        TestConfig.Builder builder =
            new TestConfig.Builder(INPUT_MODE_BUFFER, false, true, OUTPUT_FILENAME);
        doTestForVariousNumberImages(builder);
    }

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
        AvifWriter avifWriter = new AvifWriter.Builder(
            outputPath, 1920, 1080, INPUT_MODE_SURFACE)
            .setGridEnabled(true)
            .setMaxImages(4)
            .setQuality(90)
            .setPrimaryIndex(0)
            .setHandler(mHandler)
            .build();

        avifWriter.close();
    }

    private void doTestForVariousNumberImages(TestConfig.Builder builder) throws Exception {
        builder.setHighBitDepthEnabled(false);
        builder.setNumImages(4);
        doTest(builder.setRotation(270).build());
        doTest(builder.setRotation(180).build());
        doTest(builder.setRotation(90).build());
        doTest(builder.setRotation(0).build());
        doTest(builder.setNumImages(1).build());
        doTest(builder.setNumImages(8).build());

        builder.setHighBitDepthEnabled(true);
        builder.setNumImages(1);
        doTest(builder.setRotation(270).build());
        doTest(builder.setRotation(180).build());
        doTest(builder.setRotation(90).build());
        doTest(builder.setRotation(0).build());
        doTest(builder.setNumImages(1).build());
        doTest(builder.setNumImages(8).build());
    }

    private boolean shouldSkip() {
        return !hasEncoderForMime(MediaFormat.MIMETYPE_VIDEO_AV1);
    }

    private static byte[] mYuvData;
    private void doTest(final TestConfig config) throws Exception {
        final int width = config.mWidth;
        final int height = config.mHeight;
        final int actualNumImages = config.mActualNumImages;

        mInputIndex = 0;
        AvifWriter avifWriter = null;
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        String outputFileName;
        try {
            if (DEBUG)
                Log.d(TAG, "started: " + config);
            outputFileName = new File(getApplicationContext().getExternalFilesDir(null),
                    OUTPUT_FILENAME).getAbsolutePath();

            if(!config.mUseHighBitDepth){
                avifWriter =
                    new AvifWriter.Builder(outputFileName, width, height, config.mInputMode)
                        .setRotation(config.mRotation)
                        .setGridEnabled(config.mUseGrid)
                        .setMaxImages(config.mMaxNumImages)
                        .setQuality(config.mQuality)
                        .setPrimaryIndex(config.mMaxNumImages - 1)
                        .setHandler(config.mUseHandler ? mHandler : null)
                        .build();
            } else {
                avifWriter =
                    new AvifWriter.Builder(outputFileName, width, height, config.mInputMode)
                        .setRotation(config.mRotation)
                        .setGridEnabled(config.mUseGrid)
                        .setMaxImages(config.mMaxNumImages)
                        .setQuality(config.mQuality)
                        .setPrimaryIndex(config.mMaxNumImages - 1)
                        .setHandler(config.mUseHandler ? mHandler : null)
                        .setHighBitDepthEnabled(true)
                        .build();
            }

            if (config.mInputMode == INPUT_MODE_SURFACE) {
                mInputEglSurface = new EglWindowSurface(avifWriter.getInputSurface());
            }

            avifWriter.start();

            if (config.mInputMode == INPUT_MODE_BUFFER) {
                if (!config.mUseHighBitDepth) {
                    if (mYuvData == null || mYuvData.length != width * height * 3 / 2) {
                        mYuvData = new byte[width * height * 3 / 2];
                    }
                } else {
                    if (mYuvData == null || mYuvData.length != width * height * 3) {
                        mYuvData = new byte[width * height * 3];
                    }
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
                    if (!config.mUseHighBitDepth) {
                        avifWriter.addYuvBuffer(ImageFormat.YUV_420_888, mYuvData);
                    } else {
                        avifWriter.addYuvBuffer(ImageFormat.YCBCR_P010, mYuvData);
                    }
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
                avifWriter.setInputEndOfStreamTimestamp(
                    1000 * computePresentationTime(actualNumImages - 1));
            } else if (config.mInputMode == INPUT_MODE_BITMAP) {
                if(!config.mUseHighBitDepth) {
                    Bitmap[] bitmaps = config.mBitmaps;
                    for (int i = 0; i < Math.min(bitmaps.length, actualNumImages); i++) {
                        if (DEBUG) {
                            Log.d(TAG, "addBitmap: " + i);
                        }
                        avifWriter.addBitmap(bitmaps[i]);
                        bitmaps[i].recycle();
                    }
                } else {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inPreferredConfig = Bitmap.Config.RGBA_F16;
                    InputStream inputStream10Bit = getApplicationContext().getResources()
                        .openRawResource(R.raw.heifwriter_input10);
                    Bitmap bm = BitmapFactory.decodeStream(inputStream10Bit, null, opt);
                    assertNotNull(bm);
                    avifWriter.addBitmap(bm);
                    bm.recycle();
                }
            }

            avifWriter.stop(10000);
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

            if (avifWriter != null) {
                avifWriter.close();
                avifWriter = null;
            }
            if (mInputEglSurface != null) {
                // This also releases the surface from encoder.
                mInputEglSurface.release();
                mInputEglSurface = null;
            }
        }
    }
}