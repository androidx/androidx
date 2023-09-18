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

package androidx.javascriptengine.common;

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * Utility methods for use in both service and client side of JavaScriptEngine.
 */
public class Utils {
    private static final String TAG = "JavaScriptEngineUtils";

    private Utils() {
        throw new AssertionError();
    }

    /**
     * Utility method to write a byte array into a stream.
     */
    public static void writeByteArrayToStream(@NonNull byte[] inputBytes,
            @NonNull OutputStream outputStream) {
        try {
            outputStream.write(inputBytes);
            outputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Writing to outputStream failed", e);
        } finally {
            closeQuietly(outputStream);
        }
    }

    /**
     * Close, ignoring exception.
     */
    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ex) {
            // Ignore the exception on close.
        }
    }

    /**
     * Creates a pipe, writes the given bytes into one end and returns the other end.
     */
    @NonNull
    public static AssetFileDescriptor writeBytesIntoPipeAsync(@NonNull byte[] inputBytes,
            @NonNull ExecutorService executorService) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];
        OutputStream outputStream =
                new ParcelFileDescriptor.AutoCloseOutputStream(writeSide);
        executorService.execute(
                () -> Utils.writeByteArrayToStream(inputBytes, outputStream));
        return new AssetFileDescriptor(readSide, 0, inputBytes.length);
    }

    /**
     * Checks if the given AssetFileDescriptor passes certain conditions.
     */

    public static void checkAssetFileDescriptor(@NonNull AssetFileDescriptor afd,
            boolean allowUnknownLength) {
        if (afd.getStartOffset() < 0) {
            throw new IllegalArgumentException(
                    "AssetFileDescriptor offset should be >= 0");
        }
        if (afd.getLength() != AssetFileDescriptor.UNKNOWN_LENGTH && afd.getLength() < 0) {
            throw new IllegalArgumentException(
                    "AssetFileDescriptor should have valid length");
        }
        if (afd.getDeclaredLength() != AssetFileDescriptor.UNKNOWN_LENGTH
                && afd.getDeclaredLength() < 0) {
            throw new IllegalArgumentException(
                    "AssetFileDescriptor should have valid declared length");
        }
        if (afd.getLength() == AssetFileDescriptor.UNKNOWN_LENGTH && afd.getStartOffset() != 0) {
            throw new UnsupportedOperationException(
                    "AssetFileDescriptor offset should be 0 for unknown length");
        }

        if (!allowUnknownLength && afd.getLength() == AssetFileDescriptor.UNKNOWN_LENGTH) {
            throw new UnsupportedOperationException(
                    "AssetFileDescriptor should have known length");
        }
    }

    /**
     * Read a given number of bytes from a given stream into a byte array.
     * <p>
     * This allows us to use
     * <a href=https://developer.android.com/reference/java/io/InputStream#readNBytes(byte[],%20int,%20int)">
     * this </a>
     * functionality added in API 33.
     */
    public static int readNBytes(@NonNull InputStream inputStream, @NonNull byte[] b, int off,
            int len)
            throws IOException {
        int n = 0;
        while (n < len) {
            int count = inputStream.read(b, off + n, len - n);
            if (count < 0) {
                break;
            }
            n += count;
        }
        return n;
    }

    /**
     * Checks whether a given byte is a UTF8 continuation byte. If a byte can be part of valid
     * UTF-8 and is not a continuation byte, it must be a starting byte.
     */
    public static boolean isUTF8ContinuationByte(byte b) {
        final byte maskContinuationByte = (byte) 0b11000000;
        final byte targetContinuationByte = (byte) 0b10000000;
        // Checks whether it looks like "0b10xxxxxx"
        return (b & maskContinuationByte) == targetContinuationByte;
    }

    /**
     * Returns the index of right-most UTF-8 starting byte.
     * <p>
     * The input must be valid (or truncated) UTF-8 encoded bytes.
     * Returns -1 if there is no starting byte.
     */
    public static int getLastUTF8StartingByteIndex(@NonNull byte[] bytes) {
        for (int index = bytes.length - 1; index >= 0; index--) {
            if (!isUTF8ContinuationByte(bytes[index])) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Read from a AssetFileDescriptor into a String and closes it in case of both success and
     * failure.
     */
    @NonNull
    public static String readToString(@NonNull AssetFileDescriptor afd, int maxLength,
            boolean truncate)
            throws IOException, LengthLimitExceededException {
        try {
            Utils.checkAssetFileDescriptor(afd, /*allowUnknownLength=*/ false);
            int lengthToRead = (int) afd.getLength();
            if (afd.getLength() > maxLength) {
                if (truncate) {
                    // If truncate is true, read how much ever you are allowed to read.
                    lengthToRead = maxLength;
                } else {
                    throw new LengthLimitExceededException(
                            "AssetFileDescriptor.getLength() should be"
                                    + " <= " + maxLength);
                }
            }
            byte[] bytes = new byte[lengthToRead];
            // We can use AssetFileDescriptor.createInputStream() to get the InputStream directly
            // but this API is currently broken while fixing another issue regarding multiple
            // AssetFileDescriptor pointing to the same file. (b/263325931)
            // Using ParcelFileDescriptor to read the file is correct as long as the offset is 0.
            try (ParcelFileDescriptor pfd = afd.getParcelFileDescriptor()) {
                InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
                if (Utils.readNBytes(inputStream, bytes, 0, lengthToRead) != lengthToRead) {
                    throw new IOException("Couldn't read " + lengthToRead + " bytes from the "
                            + "AssetFileDescriptor");
                }
            }
            int validUtf8PrefixLength = lengthToRead;
            if (truncate) {
                // Ignoring the last partial/complete codepoint.
                validUtf8PrefixLength = getLastUTF8StartingByteIndex(bytes);
            }
            // This process can be made more memory efficient by converting the UTF-8 encoded
            // bytes to String by reading from the pipe in chunks.
            return new String(bytes, 0, validUtf8PrefixLength, StandardCharsets.UTF_8);
        } finally {
            afd.close();
        }
    }
}
