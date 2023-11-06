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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Fake {@link ImageProxy.PlaneProxy} with JPEG format.
 *
 * TODO: Rename this to FakeByteArrayPlaneProxy and inherit {@link FakePlaneProxy}.
 *
 */
@RequiresApi(21)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeJpegPlaneProxy implements ImageProxy.PlaneProxy {

    private final ByteBuffer mByteBuffer;

    public FakeJpegPlaneProxy(@NonNull byte[] jpegBytes) {
        mByteBuffer = ByteBuffer.allocateDirect(jpegBytes.length);
        mByteBuffer.put(jpegBytes);
    }

    @Override
    public int getRowStride() {
        return 0;
    }

    @Override
    public int getPixelStride() {
        return 0;
    }

    @NonNull
    @Override
    public ByteBuffer getBuffer() {
        return mByteBuffer;
    }
}
