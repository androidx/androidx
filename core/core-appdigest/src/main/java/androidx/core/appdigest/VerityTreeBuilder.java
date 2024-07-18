/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.core.appdigest;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * VerityTreeBuilder is used to generate the root hash of verity tree built from the input file.
 * This version was adopted from VerityTreeBuilder.java in ApkSigner tool and changed to work on
 * all target APIs.
 */
class VerityTreeBuilder {
    /**
     * Maximum size (in bytes) of each node of the tree.
     */
    private static final int CHUNK_SIZE = 4096;

    /**
     * Digest algorithm (JCA Digest algorithm name) used in the tree.
     */
    private static final String JCA_ALGORITHM = "SHA-256";
    /**
     * Typical prefetch size.
     */
    private static final int MAX_PREFETCH_CHUNKS = 1024;

    static byte[] computeChunkVerityTreeAndDigest(@NonNull String apkPath)
            throws IOException, NoSuchAlgorithmException {
        RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
        try {
            final MessageDigest md = getNewMessageDigest();
            ByteBuffer tree = generateVerityTree(md, apk);
            return getRootHashFromTree(md, tree);
        } finally {
            apk.close();
        }
    }

    private VerityTreeBuilder() {
    }

    private interface DataSource {
        void copyTo(long offset, int size, ByteBuffer dest) throws IOException;
    }

    /**
     * Returns the digested root hash from the top level (only page) of a verity tree.
     */
    private static byte[] getRootHashFromTree(MessageDigest md, ByteBuffer verityBuffer) {
        ByteBuffer firstPage = slice(verityBuffer.asReadOnlyBuffer(), 0, CHUNK_SIZE);
        return digest(md, firstPage);
    }

    /**
     * Returns the byte buffer that contains the whole verity tree.
     *
     * The tree is built bottom up. The bottom level has 256-bit digest for each 4 KB block in the
     * input file.  If the total size is larger than 4 KB, take this level as input and repeat the
     * same procedure, until the level is within 4 KB.  If salt is given, it will apply to each
     * digestion before the actual data.
     *
     * The returned root hash is calculated from the last level of 4 KB chunk, similarly with salt.
     *
     * The tree is currently stored only in memory and is never written out.  Nevertheless, it is
     * the actual verity tree format on disk, and is supposed to be re-generated on device.
     */
    private static ByteBuffer generateVerityTree(MessageDigest md, final RandomAccessFile file)
            throws IOException {
        int digestSize = md.getDigestLength();

        // Calculate the summed area table of level size. In other word, this is the offset
        // table of each level, plus the next non-existing level.
        int[] levelOffset = calculateLevelOffset(file.length(), digestSize);

        ByteBuffer verityBuffer = ByteBuffer.allocate(levelOffset[levelOffset.length - 1]).order(
                ByteOrder.LITTLE_ENDIAN);

        // Generate the hash tree bottom-up.
        for (int i = levelOffset.length - 2; i >= 0; i--) {
            ByteBuffer middleBuffer = slice(verityBuffer, levelOffset[i], levelOffset[i + 1]);
            final long srcSize;
            if (i == levelOffset.length - 2) {
                srcSize = file.length();
                final FileChannel channel = file.getChannel();
                digestDataByChunks(md, srcSize, new DataSource() {
                    @Override
                    public void copyTo(long offset, int size, ByteBuffer dest) throws IOException {
                        if (size == 0) {
                            return;
                        }
                        if (size > dest.remaining()) {
                            throw new IOException();
                        }

                        long offsetInFile = offset;
                        int remaining = size;
                        int prevLimit = dest.limit();
                        try {
                            // FileChannel.read(ByteBuffer) reads up to dest.remaining(). Thus,
                            // we need to adjust the buffer's limit to avoid reading more than
                            // size bytes.
                            dest.limit(dest.position() + size);
                            while (remaining > 0) {
                                int chunkSize;
                                synchronized (file) {
                                    channel.position(offsetInFile);
                                    chunkSize = channel.read(dest);
                                }
                                offsetInFile += chunkSize;
                                remaining -= chunkSize;
                            }
                        } finally {
                            dest.limit(prevLimit);
                        }
                    }
                }, middleBuffer);
            } else {
                srcSize = (long) levelOffset[i + 2] - levelOffset[i + 1];
                final ByteBuffer srcBuffer = slice(verityBuffer.asReadOnlyBuffer(),
                        levelOffset[i + 1], levelOffset[i + 2]).asReadOnlyBuffer();
                digestDataByChunks(md, srcSize, new DataSource() {
                    @Override
                    public void copyTo(long offset, int size, ByteBuffer dest) throws IOException {
                        int chunkPosition = (int) offset;
                        int chunkLimit = chunkPosition + size;

                        final ByteBuffer slice;
                        synchronized (srcBuffer) {
                            srcBuffer.position(0);  // to ensure position <= limit invariant
                            srcBuffer.limit(chunkLimit);
                            srcBuffer.position(chunkPosition);
                            slice = srcBuffer.slice();
                        }

                        dest.put(slice);
                    }
                }, middleBuffer);
            }

            // If the output is not full chunk, pad with 0s.
            long totalOutput = divideRoundup(srcSize, CHUNK_SIZE) * digestSize;
            int incomplete = (int) (totalOutput % CHUNK_SIZE);
            if (incomplete > 0) {
                byte[] padding = new byte[CHUNK_SIZE - incomplete];
                middleBuffer.put(padding, 0, padding.length);
            }
        }
        return verityBuffer;
    }

    /**
     * Returns an array of summed area table of level size in the verity tree.  In other words, the
     * returned array is offset of each level in the verity tree file format, plus an additional
     * offset of the next non-existing level (i.e. end of the last level + 1).  Thus the array size
     * is level + 1.
     */
    private static int[] calculateLevelOffset(long dataSize, int digestSize) {
        // Compute total size of each level, bottom to top.
        ArrayList<Long> levelSize = new ArrayList<>();
        while (true) {
            long chunkCount = divideRoundup(dataSize, CHUNK_SIZE);
            long size = CHUNK_SIZE * divideRoundup(chunkCount * digestSize, CHUNK_SIZE);
            levelSize.add(size);
            if (chunkCount * digestSize <= CHUNK_SIZE) {
                break;
            }
            dataSize = chunkCount * digestSize;
        }

        // Reverse and convert to summed area table.
        int[] levelOffset = new int[levelSize.size() + 1];
        levelOffset[0] = 0;
        for (int i = 0; i < levelSize.size(); i++) {
            final long size = levelSize.get(levelSize.size() - i - 1);
            // We don't support verity tree if it is larger then Integer.MAX_VALUE.
            levelOffset[i + 1] = levelOffset[i] + (int) size;
        }
        return levelOffset;
    }

    /**
     * Digest data source by chunks then feeds them to the sink one by one.  If the last unit is
     * less than the chunk size and padding is desired, feed with extra padding 0 to fill up the
     * chunk before digesting.
     */
    private static void digestDataByChunks(MessageDigest md, long size,
            DataSource dataSource, ByteBuffer dataSink) throws IOException {
        final int chunks = (int) divideRoundup(size, CHUNK_SIZE);

        /** Single IO operation size, in chunks. */
        final int ioSizeChunks = MAX_PREFETCH_CHUNKS;

        final byte[][] hashes = new byte[chunks][];

        // Reading the input file as fast as we can.
        final long maxReadSize = ioSizeChunks * CHUNK_SIZE;

        long readOffset = 0;
        int startChunkIndex = 0;
        while (readOffset < size) {
            final long readLimit = Math.min(readOffset + maxReadSize, size);
            final int readSize = (int) (readLimit - readOffset);
            final int bufferSizeChunks = (int) divideRoundup(readSize, CHUNK_SIZE);

            // Overllocating to zero-pad last chunk.
            // With 4MiB block size, 32 threads and 4 queue size we might allocate up to 144MiB.
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSizeChunks * CHUNK_SIZE);
            dataSource.copyTo(readOffset, readSize, buffer);
            buffer.rewind();

            final int readChunkIndex = startChunkIndex;
            for (int offset = 0, finish = buffer.capacity(), chunkIndex = readChunkIndex;
                    offset < finish; offset += CHUNK_SIZE, ++chunkIndex) {
                ByteBuffer chunk = slice(buffer, offset, offset + CHUNK_SIZE);
                hashes[chunkIndex] = digest(md, chunk);
            }

            startChunkIndex += bufferSizeChunks;
            readOffset += readSize;
        }

        // Streaming hashes back.
        for (byte[] hash : hashes) {
            dataSink.put(hash, 0, hash.length);
        }
    }

    /**
     * Obtains a new instance of the message digest algorithm.
     */
    private static MessageDigest getNewMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(JCA_ALGORITHM);
    }

    /** Returns the digest of data with salt prepended. */
    private static byte[] digest(MessageDigest md, ByteBuffer data) {
        md.reset();
        md.update(data);
        return md.digest();
    }

    /** Divides a number and round up to the closest integer. */
    private static long divideRoundup(long dividend, long divisor) {
        return (dividend + divisor - 1) / divisor;
    }

    /** Returns a slice of the buffer with shared the content. */
    private static ByteBuffer slice(ByteBuffer buffer, int begin, int end) {
        ByteBuffer b = buffer.duplicate();
        b.position(0);  // to ensure position <= limit invariant.
        b.limit(end);
        b.position(begin);
        return b.slice();
    }
}
