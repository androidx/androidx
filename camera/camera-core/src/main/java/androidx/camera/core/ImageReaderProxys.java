/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.media.ImageReader;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.ImageReaderProxy;

import org.jspecify.annotations.NonNull;

/**
 * Different implementations of {@link ImageReaderProxy}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ImageReaderProxys {

    private ImageReaderProxys() {
    }

    /**
     * Creates an {@link ImageReaderProxy} which uses its own isolated {@link ImageReader}.
     *
     * @param width     of the reader
     * @param height    of the reader
     * @param format    of the reader
     * @param maxImages of the reader
     * @return new {@link ImageReaderProxy} instance
     */
    public static @NonNull ImageReaderProxy createIsolatedReader(
            int width, int height, int format, int maxImages) {
        return createIsolatedReader(width, height, format, maxImages, 0);
    }

    /**
     * Creates an {@link ImageReaderProxy} which uses its own isolated {@link ImageReader}.
     *
     * @param width     of the reader
     * @param height    of the reader
     * @param format    of the reader
     * @param maxImages of the reader
     * @param usage     of the reader
     * @return new {@link ImageReaderProxy} instance
     */
    public static @NonNull ImageReaderProxy createIsolatedReader(
            int width, int height, int format, int maxImages, long usage) {
        ImageReader imageReader;
        if (Build.VERSION.SDK_INT >= 29 && usage != 0) {
            imageReader = ImageReaderApi29Impl.newInstance(width, height, format, maxImages, usage);
        } else {
            // Use the older newInstance API if usage is 0 or if the device is before API 29. For
            // non-PRIVATE formats like YUV or RGBA, if usage is 0, the older API will
            // automatically set the proper usage flags (e.g. USAGE_CPU_READ_OFTEN) to allow CPU
            // access. If the newer API is used with usage 0, it might not have CPU access
            // permission and cause "lock buffer failed" error when Image.getPlanes() is called.
            imageReader = ImageReader.newInstance(width, height, format, maxImages);
        }
        return new AndroidImageReaderProxy(imageReader);
    }

    @RequiresApi(29)
    private static class ImageReaderApi29Impl {
        private ImageReaderApi29Impl() {
        }

        static ImageReader newInstance(int width, int height, int format, int maxImages,
                long usage) {
            return ImageReader.newInstance(width, height, format, maxImages, usage);
        }
    }
}
