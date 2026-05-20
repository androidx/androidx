/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.compose.remote.core.operations.BitmapData;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RemoteBitmapDecoderTest {

    private final BitmapLoader mEmptyLoader = (url) -> null;

    @Test
    public void testDecodeEmpty() {
        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_EMPTY,
                        BitmapData.TYPE_PNG_8888,
                        100,
                        200,
                        new byte[0],
                        mEmptyLoader);
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isEqualTo(100);
        assertThat(bitmap.getHeight()).isEqualTo(200);
        assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
    }

    @Test
    public void testDecodeInline_exactBounds() {
        byte[] pngData = createPng(10, 10);
        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_INLINE,
                        BitmapData.TYPE_PNG_8888,
                        10,
                        10,
                        pngData,
                        mEmptyLoader);
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isEqualTo(10);
        assertThat(bitmap.getHeight()).isEqualTo(10);
    }

    @Test
    public void testDecodeInline_exceedsWidth() {
        byte[] pngData = createPng(11, 10);
        RuntimeException e =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            RemoteBitmapDecoder.decodeBitmap(
                                    1,
                                    BitmapData.ENCODING_INLINE,
                                    BitmapData.TYPE_PNG_8888,
                                    10,
                                    10,
                                    pngData,
                                    mEmptyLoader);
                        });
        assertThat(e.getMessage()).contains("dimensions don't match");
    }

    @Test
    public void testDecodeInline_exceedsHeight() {
        byte[] pngData = createPng(10, 11);
        RuntimeException e =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            RemoteBitmapDecoder.decodeBitmap(
                                    1,
                                    BitmapData.ENCODING_INLINE,
                                    BitmapData.TYPE_PNG_8888,
                                    10,
                                    10,
                                    pngData,
                                    mEmptyLoader);
                        });
        assertThat(e.getMessage()).contains("dimensions don't match");
    }

    @Test
    public void testDecodeInline_alpha8_exactBounds() {
        byte[] pngData = createPng(10, 10);
        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_INLINE,
                        BitmapData.TYPE_PNG_ALPHA_8,
                        10,
                        10,
                        pngData,
                        mEmptyLoader);
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isEqualTo(10);
        assertThat(bitmap.getHeight()).isEqualTo(10);
        assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ALPHA_8);
    }

    @Test
    public void testDecodeFile_exactBounds() throws IOException {
        byte[] pngData = createPng(10, 10);
        File file =
                File.createTempFile(
                        "test",
                        ".png",
                        InstrumentationRegistry.getInstrumentation()
                                .getTargetContext()
                                .getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pngData);
        }

        byte[] pathData = file.getAbsolutePath().getBytes(StandardCharsets.UTF_8);
        try {
            Bitmap bitmap =
                    RemoteBitmapDecoder.decodeBitmap(
                            1,
                            BitmapData.ENCODING_FILE,
                            BitmapData.TYPE_PNG_8888,
                            10,
                            10,
                            pathData,
                            mEmptyLoader);
            assertThat(bitmap).isNotNull();
            assertThat(bitmap.getWidth()).isEqualTo(10);
            assertThat(bitmap.getHeight()).isEqualTo(10);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testDecodeFile_exceedsBounds() throws IOException {
        byte[] pngData = createPng(11, 10);
        File file =
                File.createTempFile(
                        "test",
                        ".png",
                        InstrumentationRegistry.getInstrumentation()
                                .getTargetContext()
                                .getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pngData);
        }

        byte[] pathData = file.getAbsolutePath().getBytes(StandardCharsets.UTF_8);
        try {
            RuntimeException e =
                    assertThrows(
                            RuntimeException.class,
                            () -> {
                                RemoteBitmapDecoder.decodeBitmap(
                                        1,
                                        BitmapData.ENCODING_FILE,
                                        BitmapData.TYPE_PNG_8888,
                                        10,
                                        10,
                                        pathData,
                                        mEmptyLoader);
                            });
            assertThat(e.getMessage()).contains("dimensions don't match");
        } finally {
            file.delete();
        }
    }

    @Test
    public void testDecodeUrl_exactBounds() {
        byte[] pngData = createPng(10, 10);
        BitmapLoader urlLoader = (url) -> new ByteArrayInputStream(pngData);

        byte[] urlData = "http://example.com/test.png".getBytes(StandardCharsets.UTF_8);
        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_URL,
                        BitmapData.TYPE_PNG_8888,
                        10,
                        10,
                        urlData,
                        urlLoader);
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isEqualTo(10);
        assertThat(bitmap.getHeight()).isEqualTo(10);
    }

    @Test
    public void testDecodeUrl_exceedsBounds() {
        byte[] pngData = createPng(11, 10);
        BitmapLoader urlLoader = (url) -> new ByteArrayInputStream(pngData);

        byte[] urlData = "http://example.com/test.png".getBytes(StandardCharsets.UTF_8);
        RuntimeException e =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            RemoteBitmapDecoder.decodeBitmap(
                                    1,
                                    BitmapData.ENCODING_URL,
                                    BitmapData.TYPE_PNG_8888,
                                    10,
                                    10,
                                    urlData,
                                    urlLoader);
                        });
        assertThat(e.getMessage()).contains("dimensions don't match");
    }

    @Test
    public void testDecodeInline_raw8888() {
        int width = 2;
        int height = 2;
        byte[] rawData = new byte[width * height * 4];
        // 2x2 Red pixels (ARGB: 0xFFFF0000)
        for (int i = 0; i < width * height; i++) {
            rawData[i * 4] = (byte) 0xFF; // A
            rawData[i * 4 + 1] = (byte) 0xFF; // R
            rawData[i * 4 + 2] = 0x00; // G
            rawData[i * 4 + 3] = 0x00; // B
        }

        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_INLINE,
                        BitmapData.TYPE_RAW8888,
                        width,
                        height,
                        rawData,
                        mEmptyLoader);
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(Color.RED);
    }

    @Test
    public void testDecodeInline_raw8() {
        int width = 2;
        int height = 2;
        byte[] rawData = new byte[width * height];
        // 2x2 Mid-gray pixels (0x80)
        for (int i = 0; i < width * height; i++) {
            rawData[i] = (byte) 0x80;
        }

        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_INLINE,
                        BitmapData.TYPE_RAW8,
                        width,
                        height,
                        rawData,
                        mEmptyLoader);
        assertThat(bitmap).isNotNull();
        // Verify it is opaque mid-gray (0xFF808080)
        assertThat(bitmap.getPixel(0, 0)).isEqualTo(0xFF808080);
    }

    private byte[] createPng(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.RED);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] data = bos.toByteArray();
        bitmap.recycle();
        return data;
    }
}
