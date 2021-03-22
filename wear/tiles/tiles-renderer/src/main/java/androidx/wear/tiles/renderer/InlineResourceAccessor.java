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

package androidx.wear.tiles.renderer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.tiles.proto.ResourceProto.ImageFormat;
import androidx.wear.tiles.proto.ResourceProto.InlineImageResource;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;

/**
 * Resource accessor for inline resources.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InlineResourceAccessor implements ResourceAccessors.InlineImageResourceAccessor {
    private static final int RGB565_BYTES_PER_PX = 2;

    private final Context mAppContext;

    /** Constructor. */
    public InlineResourceAccessor(@NonNull Context appContext) {
        this.mAppContext = appContext;
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

    @Override
    @NonNull
    @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
    public ListenableFuture<Drawable> getDrawable(@NonNull InlineImageResource inlineImage) {
        Config config = imageFormatToBitmapConfig(inlineImage.getFormat());
        ResolvableFuture<Drawable> future = ResolvableFuture.create();

        // Only handles RGB_565 for now
        if (config != Config.RGB_565) {
            future.setException(
                    new ResourceAccessors.ResourceAccessException(
                            "Unknown image format in image resource."));
            return future;
        }

        int widthPx = inlineImage.getWidthPx();
        int heightPx = inlineImage.getHeightPx();

        int expectedDataSize = widthPx * heightPx * RGB565_BYTES_PER_PX;
        if (inlineImage.getData().size() != expectedDataSize) {
            future.setException(
                    new ResourceAccessors.ResourceAccessException(
                            "Mismatch between image data size and dimensions in image resource."));
            return future;
        }

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, config);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(inlineImage.getData().toByteArray()));

        // The app Context is correct here, as it's just used for display density, so it doesn't
        // depend
        // on anything from the provider app.
        future.set(new BitmapDrawable(mAppContext.getResources(), bitmap));
        return future;
    }
}
