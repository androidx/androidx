/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.mocks;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import androidx.pdf.util.BitmapParcel;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MockBitmapParcel extends BitmapParcel {

    private final int mBufferSize;

    public MockBitmapParcel(Bitmap bitmap, int bufferSize) {
        super(bitmap);
        this.mBufferSize = bufferSize;
    }

    @Override
    protected void receiveBitmap(Bitmap bitmap, ParcelFileDescriptor sourceFd) {
        try (FileInputStream fis = new FileInputStream(sourceFd.getFileDescriptor())) {
            FileChannel channel = fis.getChannel();

            ByteBuffer buffer = ByteBuffer.allocateDirect(mBufferSize);
            while (channel.read(buffer) != -1) {
                buffer.rewind();
                bitmap.copyPixelsFromBuffer(buffer);
            }
            buffer.rewind();
            channel.close();
            sourceFd.close();
        } catch (IOException ignored) {
        }
    }
}
