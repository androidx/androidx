/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/**
 * Fake {@link ImageProxy.PlaneProxy} for testing.
 */
public class FakePlaneProxy implements ImageProxy.PlaneProxy {

    private final @NonNull ByteBuffer mByteBuffer;

    private final int mRowStride;

    private final int mPixelStride;

    public FakePlaneProxy(@NonNull ByteBuffer byteBuffer, int rowStride, int pixelStride) {
        mByteBuffer = byteBuffer;
        mRowStride = rowStride;
        mPixelStride = pixelStride;
    }

    @Override
    public int getRowStride() {
        return mRowStride;
    }

    @Override
    public int getPixelStride() {
        return mPixelStride;
    }

    @Override
    public @NonNull ByteBuffer getBuffer() {
        return mByteBuffer;
    }

    @Override
    public <T> T unwrapAs(@NonNull Class<T> type) {
        return null;
    }
}
