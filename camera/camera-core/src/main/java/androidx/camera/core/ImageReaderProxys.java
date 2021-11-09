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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageReaderProxy;

/**
 * Different implementations of {@link ImageReaderProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ImageReaderProxys {

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
    @NonNull
    static ImageReaderProxy createIsolatedReader(
            int width, int height, int format, int maxImages) {
        ImageReader imageReader = ImageReader.newInstance(width, height, format, maxImages);
        return new AndroidImageReaderProxy(imageReader);
    }
}
