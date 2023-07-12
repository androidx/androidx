/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ImageProcessor;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.imagecapture.RgbaImageProxy;

/**
 * Create a grayscale image effect for testing.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GrayscaleImageEffect extends CameraEffect {

    public GrayscaleImageEffect() {
        super(IMAGE_CAPTURE,
                directExecutor(),
                new GrayscaleProcessor(),
                throwable -> {
                });
    }

    @Nullable
    public ImageProxy getInputImage() {
        return ((GrayscaleProcessor) requireNonNull(getImageProcessor())).getInputImage();
    }

    static class GrayscaleProcessor implements ImageProcessor {

        private ImageProxy mImageIn;

        @NonNull
        @Override
        public Response process(@NonNull Request request) {
            mImageIn = requireNonNull(request.getInputImage());
            Bitmap bitmapIn = ((RgbaImageProxy) mImageIn).createBitmap();
            Bitmap bitmapOut = createProcessedBitmap(bitmapIn);
            return () -> createOutputImage(bitmapOut, mImageIn);
        }

        @Nullable
        ImageProxy getInputImage() {
            return mImageIn;
        }

        /**
         * Creates output image
         */
        private ImageProxy createOutputImage(Bitmap newBitmap, ImageProxy imageIn) {
            return new RgbaImageProxy(
                    newBitmap,
                    imageIn.getCropRect(),
                    imageIn.getImageInfo().getRotationDegrees(),
                    imageIn.getImageInfo().getSensorToBufferTransformMatrix(),
                    imageIn.getImageInfo().getTimestamp()
            );
        }

        /**
         * Apply grayscale on the [Bitmap]
         */
        private Bitmap createProcessedBitmap(Bitmap bitmapIn) {
            Paint paint = new Paint();
            // R/G/B = 1/3 R + 1/3 G + 1/3 B
            float oneThird = 1 / 3F;
            paint.setColorFilter(new ColorMatrixColorFilter(new float[]{
                    oneThird, oneThird, oneThird, 0F, 0F,
                    oneThird, oneThird, oneThird, 0F, 0F,
                    oneThird, oneThird, oneThird, 0F, 0F,
                    0F, 0F, 0F, 1F, 0F
            }));
            Bitmap bitmapOut = Bitmap.createBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapOut);
            canvas.drawBitmap(bitmapIn, 0F, 0F, paint);
            return bitmapOut;
        }
    }
}
