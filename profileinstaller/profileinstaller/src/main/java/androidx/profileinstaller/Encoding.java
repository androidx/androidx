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

package androidx.profileinstaller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A set of utilities on top of InputStream / OutputStream that are used by [ProfileTranscoder].
 */
@RequiresApi(19)
class Encoding {
    private Encoding() {}

    static final int SIZEOF_BYTE = java.lang.Byte.SIZE;
    static final int UINT_8_SIZE = 1;
    static final int UINT_16_SIZE = 2;
    static final int UINT_32_SIZE = 4;

    static int utf8Length(@NonNull String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    static void writeUInt(@NonNull OutputStream os, long value, int numberOfBytes) throws
            IOException {
        byte[] buffer = new byte[numberOfBytes];
        for (int i = 0; i < numberOfBytes; i++) {
            buffer[i] = (byte) ((value >> (i * SIZEOF_BYTE)) & 0xff);
        }
        os.write(buffer);
    }

    static void writeUInt8(@NonNull OutputStream os, int value) throws IOException {
        writeUInt(os, value, UINT_8_SIZE);
    }

    static void writeUInt16(@NonNull OutputStream os, int value) throws IOException {
        writeUInt(os, value, UINT_16_SIZE);
    }

    static void writeUInt32(@NonNull OutputStream os, long value) throws IOException {
        writeUInt(os, value, UINT_32_SIZE);
    }

    static void writeString(@NonNull OutputStream os, @NonNull String s) throws IOException {
        os.write(s.getBytes(StandardCharsets.UTF_8));
    }

    static int bitsToBytes(int numberOfBits) {
        return (numberOfBits + SIZEOF_BYTE - 1 & -SIZEOF_BYTE) / SIZEOF_BYTE;
    }

    static @NonNull byte[] read(@NonNull InputStream is, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int result = is.read(buffer, offset, length - offset);
            if (result < 0) {
                throw error("Not enough bytes to read: " + length);
            }
            offset += result;
        }
        return buffer;
    }

    static long readUInt(@NonNull InputStream is, int numberOfBytes) throws IOException {
        byte[] buffer = read(is, numberOfBytes);
        long value = 0;
        for (int i = 0; i < numberOfBytes; i++) {
            long next = buffer[i] & 0xff;
            value += next << (i * SIZEOF_BYTE);
        }
        return value;
    }

    static int readUInt8(@NonNull InputStream is) throws IOException {
        return (int) readUInt(is, UINT_8_SIZE);
    }

    static int readUInt16(@NonNull InputStream is) throws IOException {
        return (int) readUInt(is, UINT_16_SIZE);
    }

    static long readUInt32(@NonNull InputStream is) throws IOException {
        return readUInt(is, UINT_32_SIZE);
    }

    static @NonNull String readString(InputStream is, int size) throws IOException {
        return new String(read(is, size), StandardCharsets.UTF_8);
    }

    static @NonNull byte[] readCompressed(
            @NonNull InputStream is,
            int compressedDataSize,
            int uncompressedDataSize
    ) throws IOException {
        // Read the expected compressed data size.
        Inflater inf = new Inflater();
        byte[] result = new byte[uncompressedDataSize];
        int totalBytesRead = 0;
        int totalBytesInflated = 0;
        byte[] input = new byte[2048]; // 2KB read window size;
        while (!inf.finished() && !inf.needsDictionary() && totalBytesRead < compressedDataSize) {
            int bytesRead = is.read(input);
            if (bytesRead < 0) {
                throw error(
                        "Invalid zip data. Stream ended after $totalBytesRead bytes. " +
                                "Expected " + compressedDataSize + " bytes"
                );
            }
            inf.setInput(input, 0, bytesRead);
            try {
                totalBytesInflated += inf.inflate(
                        result,
                        totalBytesInflated,
                        uncompressedDataSize - totalBytesInflated
                );
            } catch (DataFormatException e) {
                throw error(e.getMessage());
            }
            totalBytesRead += bytesRead;
        }
        if (totalBytesRead != compressedDataSize) {
            throw error(
                    "Didn't read enough bytes during decompression." +
                            " expected=" + compressedDataSize +
                            " actual=" + totalBytesRead
            );
        }
        if (!inf.finished()) {
            throw error("Inflater did not finish");
        }
        return result;
    }

    static void writeAll(@NonNull InputStream is, @NonNull OutputStream os) throws IOException {
        byte[] buf = new byte[512];
        int length;
        while ((length = is.read(buf)) > 0) {
            os.write(buf, 0, length);
        }
    }

    static @NonNull RuntimeException error(@Nullable String message) {
        return new IllegalStateException(message);
    }
}
