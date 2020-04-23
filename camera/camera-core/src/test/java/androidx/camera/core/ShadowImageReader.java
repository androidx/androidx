/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import static org.mockito.Mockito.when;

import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * A Robolectric shadow of {@link ImageReader}.
 */
@Implements(ImageReader.class)
public class ShadowImageReader {

    // Image to return when user call acquireLatestImage() or acquireNextImage().
    private static Image sIncomingImage;

    @Nullable
    private volatile ImageReader.OnImageAvailableListener mListener;

    private static ImageReader sImageReader;
    private static ShadowImageReader sShadowImageReader;

    private static int sWidth;
    private static int sHeight;
    private static int sMaxImages;

    /**
     * Shadow of {@link ImageReader#newInstance(int, int, int, int, long)}.
     */
    @Implementation
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static ImageReader newInstance(int width, int height, int format, int maxImages,
            long usage) {
        throw new UnsupportedOperationException("Shadow method not implemented.");
    }

    /**
     * Shadow of {@link ImageReader#newInstance(int, int, int, int)}.
     */
    @Implementation
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static ImageReader newInstance(int width, int height, int format, int maxImages) {
        Preconditions.checkState(sImageReader == null,
                "Only support one shadow ImageReader instance");
        sMaxImages = maxImages;
        sWidth = width;
        sHeight = height;
        sImageReader = Shadow.newInstanceOf(ImageReader.class);
        sShadowImageReader = Shadow.extract(sImageReader);
        return sImageReader;
    }

    /**
     * Sets the incoming image and triggers
     * {@link ImageReader.OnImageAvailableListener#onImageAvailable(ImageReader)}.
     */
    public static void triggerCallbackWithMockImage(Image mockImage) {
        when(mockImage.getWidth()).thenReturn(sWidth);
        when(mockImage.getHeight()).thenReturn(sHeight);
        sIncomingImage = mockImage;
        sShadowImageReader.getListener().onImageAvailable(sImageReader);
    }

    @Implementation
    public void close() {
        // no-op.
    }

    @Implementation
    public int getMaxImages() {
        return sMaxImages;
    }

    /**
     * Clears incoming images.
     */
    public static void clear() {
        sIncomingImage = null;
        sImageReader = null;
        sShadowImageReader = null;
    }


    /**
     * Shadow of {@link ImageReader#setOnImageAvailableListener}.
     */
    @Implementation
    public void setOnImageAvailableListener(ImageReader.OnImageAvailableListener listener,
            Handler handler) {
        this.mListener = listener;
    }

    /**
     * Shadow of {@link ImageReader#acquireLatestImage()}.
     */
    @Implementation
    public Image acquireLatestImage() {
        return popIncomingImage();
    }

    /**
     * Shadow of {@link ImageReader#acquireNextImage()}.
     */
    @Implementation
    public Image acquireNextImage() {
        return popIncomingImage();
    }

    private Image popIncomingImage() {
        try {
            return sIncomingImage;
        } finally {
            sIncomingImage = null;
        }
    }

    /**
     * Returns the last OnImageAvailableListener that was passed in call to
     * setOnImageAvailableListener or null if never called.
     */
    @Nullable
    public ImageReader.OnImageAvailableListener getListener() {
        return mListener;
    }
}
