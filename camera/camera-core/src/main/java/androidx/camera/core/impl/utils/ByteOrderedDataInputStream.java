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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * An input stream to parse EXIF data area, which can be written in either little or big endian
 * order.
 */
// Note: This class is adapted from {@link androidx.exifinterface.media.ExifInterface}
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ByteOrderedDataInputStream extends InputStream implements DataInput {
    private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
    private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

    private final DataInputStream mDataInputStream;
    private ByteOrder mByteOrder = ByteOrder.BIG_ENDIAN;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final int mLength;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            int mPosition;

    ByteOrderedDataInputStream(InputStream in) throws IOException {
        this(in, ByteOrder.BIG_ENDIAN);
    }

    ByteOrderedDataInputStream(InputStream in, ByteOrder byteOrder) throws IOException {
        mDataInputStream = new DataInputStream(in);
        mLength = mDataInputStream.available();
        mPosition = 0;
        // TODO (b/142218289): Need to handle case where input stream does not support mark
        mDataInputStream.mark(mLength);
        mByteOrder = byteOrder;
    }

    ByteOrderedDataInputStream(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    public void setByteOrder(ByteOrder byteOrder) {
        mByteOrder = byteOrder;
    }

    public void seek(long byteCount) throws IOException {
        if (mPosition > byteCount) {
            mPosition = 0;
            mDataInputStream.reset();
            // TODO (b/142218289): Need to handle case where input stream does not support mark
            mDataInputStream.mark(mLength);
        } else {
            byteCount -= mPosition;
        }

        if (skipBytes((int) byteCount) != (int) byteCount) {
            throw new IOException("Couldn't seek up to the byteCount");
        }
    }

    public int peek() {
        return mPosition;
    }

    @Override
    public int available() throws IOException {
        return mDataInputStream.available();
    }

    @Override
    public int read() throws IOException {
        ++mPosition;
        return mDataInputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = mDataInputStream.read(b, off, len);
        mPosition += bytesRead;
        return bytesRead;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        ++mPosition;
        return mDataInputStream.readUnsignedByte();
    }

    @Override
    public String readLine() {
        throw new UnsupportedOperationException("readLine() not implemented.");
    }

    @Override
    public boolean readBoolean() throws IOException {
        ++mPosition;
        return mDataInputStream.readBoolean();
    }

    @Override
    public char readChar() throws IOException {
        mPosition += 2;
        return mDataInputStream.readChar();
    }

    @Override
    public String readUTF() throws IOException {
        mPosition += 2;
        return mDataInputStream.readUTF();
    }

    @Override
    public void readFully(byte[] buffer, int offset, int length) throws IOException {
        mPosition += length;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        if (mDataInputStream.read(buffer, offset, length) != length) {
            throw new IOException("Couldn't read up to the length of buffer");
        }
    }

    @Override
    public void readFully(byte[] buffer) throws IOException {
        mPosition += buffer.length;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        if (mDataInputStream.read(buffer, 0, buffer.length) != buffer.length) {
            throw new IOException("Couldn't read up to the length of buffer");
        }
    }

    @Override
    public byte readByte() throws IOException {
        ++mPosition;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        int ch = mDataInputStream.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    @Override
    public short readShort() throws IOException {
        mPosition += 2;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return (short) ((ch2 << 8) + ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return (short) ((ch1 << 8) + ch2);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    @Override
    public int readInt() throws IOException {
        mPosition += 4;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        int ch3 = mDataInputStream.read();
        int ch4 = mDataInputStream.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    @Override
    public int skipBytes(int byteCount) throws IOException {
        int totalSkip = Math.min(byteCount, mLength - mPosition);
        int skipped = 0;
        while (skipped < totalSkip) {
            skipped += mDataInputStream.skipBytes(totalSkip - skipped);
        }
        mPosition += skipped;
        return skipped;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        mPosition += 2;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return ((ch2 << 8) + ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return ((ch1 << 8) + ch2);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    public long readUnsignedInt() throws IOException {
        return readInt() & 0xffffffffL;
    }

    @Override
    public long readLong() throws IOException {
        mPosition += 8;
        if (mPosition > mLength) {
            throw new EOFException();
        }
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        int ch3 = mDataInputStream.read();
        int ch4 = mDataInputStream.read();
        int ch5 = mDataInputStream.read();
        int ch6 = mDataInputStream.read();
        int ch7 = mDataInputStream.read();
        int ch8 = mDataInputStream.read();
        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return (((long) ch8 << 56) + ((long) ch7 << 48) + ((long) ch6 << 40)
                    + ((long) ch5 << 32) + ((long) ch4 << 24) + ((long) ch3 << 16)
                    + ((long) ch2 << 8) + (long) ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return (((long) ch1 << 56) + ((long) ch2 << 48) + ((long) ch3 << 40)
                    + ((long) ch4 << 32) + ((long) ch5 << 24) + ((long) ch6 << 16)
                    + ((long) ch7 << 8) + (long) ch8);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public void mark(int readlimit) {
        synchronized (mDataInputStream) {
            mDataInputStream.mark(readlimit);
        }
    }

    public int getLength() {
        return mLength;
    }
}
