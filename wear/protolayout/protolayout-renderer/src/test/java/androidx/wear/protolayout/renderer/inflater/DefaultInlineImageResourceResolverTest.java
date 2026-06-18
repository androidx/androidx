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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.proto.ResourceProto.ImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultInlineImageResourceResolverTest {
  private final DefaultInlineImageResourceResolver mResolver =
      new DefaultInlineImageResourceResolver(
          getApplicationContext(), /* restrictImageSize= */ true);

  @Test
  public void loadRawBitmap_validImage_succeeds() throws ResourceAccessException {
    int width = 100;
    int height = 100;
    int bytesPerPixel = 4; // ARGB_8888
    byte[] data = new byte[width * height * bytesPerPixel];

    InlineImageResource resource =
        InlineImageResource.newBuilder()
            .setWidthPx(width)
            .setHeightPx(height)
            .setFormat(ImageFormat.IMAGE_FORMAT_ARGB_8888)
            .setData(ByteString.copyFrom(data))
            .build();

    Bitmap bitmap = mResolver.loadRawBitmap(resource);

    assertThat(bitmap.getWidth()).isEqualTo(width);
    assertThat(bitmap.getHeight()).isEqualTo(height);
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void loadStructuredBitmap_validImage_succeeds() {
    byte[] minPng = getMinPng();
    int width = 10;
    int height = 10;

    InlineImageResource resource =
        InlineImageResource.newBuilder()
            .setWidthPx(width)
            .setHeightPx(height)
            .setFormat(ImageFormat.IMAGE_FORMAT_UNDEFINED)
            .setData(ByteString.copyFrom(minPng))
            .build();

    Bitmap bitmap = mResolver.loadStructuredBitmap(resource);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getWidth()).isEqualTo(width);
    assertThat(bitmap.getHeight()).isEqualTo(height);
  }

  @Test
  public void loadRawBitmap_overflow32Bit_throws() {
    // Create a non-restricting resolver to test the 'long' overflow logic specifically,
    // bypassing the 2048px linear limit check.
    DefaultInlineImageResourceResolver nonRestrictiveResolver =
        new DefaultInlineImageResourceResolver(
            getApplicationContext(), /* restrictImageSize= */ false);

    // 65536 * 65536 * 2 (RGB_565) overflows to 0 if calculated as a 32-bit int.
    InlineImageResource resource =
        InlineImageResource.newBuilder()
            .setWidthPx(65536)
            .setHeightPx(65536)
            .setFormat(ImageFormat.IMAGE_FORMAT_RGB_565)
            .setData(ByteString.EMPTY)
            .build();

    // Verifies that the 'long' calculation correctly identifies the mismatch with EMPTY data.
    assertThrows(
        ResourceAccessException.class, () -> nonRestrictiveResolver.loadRawBitmap(resource));
  }

  @Test
  public void loadRawBitmap_linearDimensionTooLarge_throws() {
    // 2049px exceeds the 2048px limit while keeping height small.
    InlineImageResource resource =
        InlineImageResource.newBuilder()
            .setWidthPx(2049)
            .setHeightPx(1)
            .setFormat(ImageFormat.IMAGE_FORMAT_RGB_565)
            .setData(ByteString.EMPTY)
            .build();

    assertThrows(ResourceAccessException.class, () -> mResolver.loadRawBitmap(resource));
  }

  @Test
  public void loadStructuredBitmap_linearDimensionTooLarge_throws() {
    // A minimal valid pixel PNG image.
    // This 'stub' data provides a valid file signature and header so that the Android ImageDecoder
    // can initialize without crashing. This allows the test to reach the callback where our
    // security dimension limit (e.g., 2048px) are enforced against the 'target' dimensions.
    byte[] minPng = getMinPng();

    InlineImageResource resource =
        InlineImageResource.newBuilder()
            .setWidthPx(2049) // Exceeds the 2048px limit
            .setHeightPx(1)
            .setFormat(ImageFormat.IMAGE_FORMAT_UNDEFINED)
            .setData(ByteString.copyFrom(minPng))
            .build();

    // This triggers the checkSize(targetSize) callback in ConstrainedImageDecoder
    assertThrows(IllegalArgumentException.class, () -> mResolver.loadStructuredBitmap(resource));
  }

  private static byte[] getMinPng() {
    return new byte[] {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01,
      0x00,
      0x00,
      0x00,
      0x01,
      0x08,
      0x06,
      0x00,
      0x00,
      0x00,
      0x1F,
      0x15,
      (byte) 0xC4,
      (byte) 0x89,
      0x00,
      0x00,
      0x00,
      0x0A,
      0x49,
      0x44,
      0x41,
      0x54,
      0x78,
      (byte) 0x9C,
      0x63,
      0x00,
      0x01,
      0x00,
      0x00,
      0x05,
      0x00,
      0x01,
      0x0D,
      0x0A,
      0x2D,
      (byte) 0xB4,
      0x00,
      0x00,
      0x00,
      0x00,
      0x49,
      0x45,
      0x4E,
      0x44,
      (byte) 0xAE,
      0x42,
      0x60,
      (byte) 0x82
    };
  }
}