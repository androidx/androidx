/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.proto.ResourceProto.ImageFormat;
import androidx.wear.tiles.proto.ResourceProto.InlineImageResource;
import androidx.wear.tiles.renderer.internal.ResourceAccessors.InlineImageResourceAccessor;
import androidx.wear.tiles.renderer.internal.ResourceAccessors.ResourceAccessException;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;

/** Resource accessor for inline resources. */
public class InlineResourceAccessor implements InlineImageResourceAccessor {
    private static final int RGB565_BYTES_PER_PX = 2;

    private final Context mAppContext;

    /** Constructor. */
    public InlineResourceAccessor(@NonNull Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    @NonNull
    public ListenableFuture<Drawable> getDrawable(@NonNull InlineImageResource inlineImage) {
        @Nullable Bitmap bitmap = null;

        if (inlineImage.getFormat() == ImageFormat.IMAGE_FORMAT_RGB_565) {
            try {
                bitmap = loadRawBitmap(inlineImage);
            } catch (ResourceAccessException ex) {
                return ResourceAccessors.createFailedFuture(ex);
            }
        } else if (inlineImage.getFormat() == ImageFormat.IMAGE_FORMAT_UNDEFINED) {
            try {
                bitmap = loadStructuredBitmap(inlineImage);
            } catch (RuntimeException ex) {
                return ResourceAccessors.createFailedFuture(ex);
            }
        }

        if (bitmap == null) {
            return ResourceAccessors.createFailedFuture(
                    new ResourceAccessException("Unknown image format in image resource."));
        }

        // The app Context is correct here, as it's just used for display density, so it doesn't
        // depend on anything from the provider app.
        return ResourceAccessors.createImmediateFuture(
                new BitmapDrawable(mAppContext.getResources(), bitmap));
    }

    @Nullable
    private static Config imageFormatToBitmapConfig(ImageFormat imageFormat) {
        switch (imageFormat) {
            case IMAGE_FORMAT_RGB_565:
                return Config.RGB_565;
            case IMAGE_FORMAT_UNDEFINED:
            case UNRECOGNIZED:
                return null;
        }

        return null;
    }

    @NonNull
    private Bitmap loadRawBitmap(@NonNull InlineImageResource inlineImage)
            throws ResourceAccessException {
        Config config = imageFormatToBitmapConfig(inlineImage.getFormat());

        // Only handles RGB_565 for now
        if (config != Config.RGB_565) {
            throw new ResourceAccessException("Unknown image format in image resource.");
        }

        int widthPx = inlineImage.getWidthPx();
        int heightPx = inlineImage.getHeightPx();

        int expectedDataSize = widthPx * heightPx * RGB565_BYTES_PER_PX;
        if (inlineImage.getData().size() != expectedDataSize) {
            throw new ResourceAccessException(
                    "Mismatch between image data size and dimensions in image resource.");
        }

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, config);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(inlineImage.getData().toByteArray()));

        return bitmap;
    }

    @NonNull
    private Bitmap loadStructuredBitmap(@NonNull InlineImageResource inlineImage) {
        Bitmap bitmap =
                BitmapFactory.decodeByteArray(
                        inlineImage.getData().toByteArray(), 0, inlineImage.getData().size());

        return Bitmap.createScaledBitmap(
                bitmap, inlineImage.getWidthPx(), inlineImage.getHeightPx(), /* filter= */ true);
    }
}
