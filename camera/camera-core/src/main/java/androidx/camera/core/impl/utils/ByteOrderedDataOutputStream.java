/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import androidx.annotation.RequiresApi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

/**
 * An output stream to write EXIF data area, which can be written in either little or big endian
 * order.
 */
// Note: This class is adapted from {@link androidx.exifinterface.media.ExifInterface}
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ByteOrderedDataOutputStream extends FilterOutputStream {
    final OutputStream mOutputStream;
    private ByteOrder mByteOrder;

    ByteOrderedDataOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out);
        mOutputStream = out;
        mByteOrder = byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        mByteOrder = byteOrder;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        mOutputStream.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        mOutputStream.write(bytes, offset, length);
    }

    public void writeByte(int val) throws IOException {
        mOutputStream.write(val);
    }

    public void writeShort(short val) throws IOException {
        if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
            mOutputStream.write((val >>> 0) & 0xFF);
            mOutputStream.write((val >>> 8) & 0xFF);
        } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
            mOutputStream.write((val >>> 8) & 0xFF);
            mOutputStream.write((val >>> 0) & 0xFF);
        }
    }

    public void writeInt(int val) throws IOException {
        if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
            mOutputStream.write((val >>> 0) & 0xFF);
            mOutputStream.write((val >>> 8) & 0xFF);
            mOutputStream.write((val >>> 16) & 0xFF);
            mOutputStream.write((val >>> 24) & 0xFF);
        } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
            mOutputStream.write((val >>> 24) & 0xFF);
            mOutputStream.write((val >>> 16) & 0xFF);
            mOutputStream.write((val >>> 8) & 0xFF);
            mOutputStream.write((val >>> 0) & 0xFF);
        }
    }

    public void writeUnsignedShort(int val) throws IOException {
        writeShort((short) val);
    }

    public void writeUnsignedInt(long val) throws IOException {
        writeInt((int) val);
    }
}
