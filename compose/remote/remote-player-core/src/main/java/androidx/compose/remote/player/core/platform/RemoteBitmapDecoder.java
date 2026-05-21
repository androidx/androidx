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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.operations.BitmapData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;

/** Utility for centrally and securely decoding images from Remote Compose documents. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBitmapDecoder {
    private static final boolean CHECK_DATA_SIZE = true;
    private static final int MAX_IMAGE_HEADER_BUFFER_SIZE = 100 * 1024;

    /** Decode a byte array into an image, with robust security constraints. */
    public static @Nullable Bitmap decodeBitmap(
            int imageId,
            short encoding,
            short type,
            int width,
            int height,
            byte @NonNull [] data,
            @NonNull BitmapLoader bitmapLoader) {
        Bitmap image = null;
        switch (encoding) {
            case BitmapData.ENCODING_INLINE:
                switch (type) {
                    case BitmapData.TYPE_PNG_8888:
                    case BitmapData.TYPE_PNG_ALPHA_8:
                        if (CHECK_DATA_SIZE) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true; // <-- do a bounds-only pass
                            BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                            checkBounds(opts, width, height);
                        }

                        if (type == BitmapData.TYPE_PNG_8888) {
                            image = BitmapFactory.decodeByteArray(data, 0, data.length);
                        } else {
                            image = decodePreferringAlpha8(data);
                            // If needed convert to ALPHA_8.
                            if (!image.getConfig().equals(Bitmap.Config.ALPHA_8)) {
                                Bitmap alpha8Bitmap =
                                        Bitmap.createBitmap(
                                                image.getWidth(),
                                                image.getHeight(),
                                                Bitmap.Config.ALPHA_8);
                                Canvas canvas = new Canvas(alpha8Bitmap);
                                Paint paint = new Paint();
                                paint.setXfermode(
                                        new android.graphics.PorterDuffXfermode(
                                                android.graphics.PorterDuff.Mode.SRC));
                                canvas.drawBitmap(image, 0, 0, paint);
                                image.recycle(); // Release resources

                                image = alpha8Bitmap;
                            }
                        }
                        break;
                    case BitmapData.TYPE_RAW8888:
                        // RAW types are already bounded by the Bitmap.createBitmap call using
                        // width/height metadata which was pre-vetted during document parsing.
                        image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        int[] idata = new int[data.length / 4];
                        for (int i = 0; i < idata.length; i++) {
                            int p = i * 4;
                            idata[i] =
                                    (data[p] << 24)
                                            | (data[p + 1] << 16)
                                            | (data[p + 2] << 8)
                                            | data[p + 3];
                        }
                        image.setPixels(idata, 0, width, 0, 0, width, height);
                        break;
                    case BitmapData.TYPE_RAW8:
                        // RAW types are already bounded by the Bitmap.createBitmap call using
                        // width/height metadata which was pre-vetted during document parsing.
                        image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        int[] bdata = new int[data.length];
                        for (int i = 0; i < bdata.length; i++) {
                            bdata[i] = 0xFF000000 | (0x00010101 * (data[i] & 0xFF));
                        }
                        image.setPixels(bdata, 0, width, 0, 0, width, height);
                        break;
                    case BitmapData.TYPE_PNG:
                        throw new RuntimeException("TYPE_PNG is not allowed for ENCODING_INLINE");
                }
                break;
            case BitmapData.ENCODING_FILE: {
                if (!Limits.ENABLE_IMAGE_FILES) {
                    throw new RuntimeException("File image not supported [" + imageId + "]");
                }
                String path = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                try (java.io.FileInputStream fis = new java.io.FileInputStream(path)) {
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    if (CHECK_DATA_SIZE) {
                        bis.mark(MAX_IMAGE_HEADER_BUFFER_SIZE);

                        BitmapFactory.Options optsFile = new BitmapFactory.Options();
                        optsFile.inJustDecodeBounds = true; // <-- do a bounds-only pass
                        BitmapFactory.decodeStream(bis, null, optsFile);
                        checkBounds(optsFile, width, height);

                        // Rewind the stream back to the beginning. Throws if mark was exceeded.
                        bis.reset();
                    }
                    image = BitmapFactory.decodeStream(bis);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case BitmapData.ENCODING_URL: {
                if (!Limits.ENABLE_IMAGE_URLS) {
                    throw new RuntimeException("URL image not supported [" + imageId + "]");
                }
                try (java.io.InputStream is =
                        bitmapLoader.loadBitmap(
                                new String(data, java.nio.charset.StandardCharsets.UTF_8))) {
                    BufferedInputStream bis = new BufferedInputStream(is);
                    if (CHECK_DATA_SIZE) {
                        bis.mark(MAX_IMAGE_HEADER_BUFFER_SIZE);
                        BitmapFactory.Options optsUrl = new BitmapFactory.Options();
                        optsUrl.inJustDecodeBounds = true; // <-- do a bounds-only pass
                        BitmapFactory.decodeStream(bis, null, optsUrl);
                        checkBounds(optsUrl, width, height);
                        // Rewind the stream back to the beginning. Throws if mark was exceeded.
                        bis.reset();
                    }
                    image = BitmapFactory.decodeStream(bis);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case BitmapData.ENCODING_EMPTY:
                image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                break;
        }
        return image;
    }

    private static Bitmap decodePreferringAlpha8(byte @NonNull [] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static void checkBounds(BitmapFactory.Options opts, int maxWidth, int maxHeight) {
        if (opts.outWidth > maxWidth || opts.outHeight > maxHeight) {
            throw new RuntimeException(
                    "dimensions don't match "
                            + opts.outWidth
                            + "x"
                            + opts.outHeight
                            + " vs "
                            + maxWidth
                            + "x"
                            + maxHeight);
        }
    }

    private RemoteBitmapDecoder() {}
}
