/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.core.Logger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reconciles Multi-Picture Format (CIPA DC-007 {@code APP2} {@code "MPF\0"}) image size and data
 * offset entries across {@code ExifInterface.saveAttributes()} rewrites in Multi-Picture JPEGs such
 * as Ultra HDR ({@code JPEG_R}).
 *
 * <p>{@code ExifInterface.saveJpegAttributes()} writes updated {@code APP1} segments right after
 * {@code SOI} while copying {@code APP2 MPF} and everything from primary {@code SOS} through {@code
 * EOF} verbatim. Reconciliation uses only the measured pre-save and post-save file lengths, MPF
 * TIFF header offsets, and pre-save {@code MP Entry} values, so it stays exact regardless of {@code
 * ExifInterface} segment ordering and remains idempotent if MPF entries are already up to date.
 */
// ExifInterface does not maintain APP2 MPF offsets by design (b/585464808); CameraX reconciles
// them on file-backed Exif.save() (b/585078908).
final class JpegMpfUpdater {

    private static final String TAG = "JpegMpfUpdater";

    private static final int MARKER_PREFIX = 0xFF;
    private static final int MARKER_SOI = 0xD8;
    private static final int MARKER_EOI = 0xD9;
    private static final int MARKER_SOS = 0xDA;
    private static final int MARKER_APP2 = 0xE2;

    private static final int MPF_SIGNATURE_SIZE = 4;
    private static final int TIFF_HEADER_SIZE = 8;
    private static final int IFD_ENTRY_SIZE = 12;
    private static final int MP_ENTRY_SIZE = 16;

    private static final int TAG_NUMBER_OF_IMAGES = 0xB001;
    private static final int TAG_MP_ENTRY = 0xB002;
    private static final int TYPE_ULONG = 4;
    private static final int TYPE_UNDEFINED = 7;
    private static final long MP_ATTR_DATA_FORMAT_MASK = 0x07000000L;

    private JpegMpfUpdater() {}

    /**
     * Captures the pre-save {@code APP2 MPF} section if {@code file} is a Multi-Picture JPEG with a
     * valid Index IFD before primary {@code SOS}, or returns {@code null} for standard JPEGs.
     */
    static @Nullable Snapshot snapshot(@NonNull File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return readPrimaryMpfSection(raf);
        }
    }

    /**
     * Applies the exact header shift introduced by {@code ExifInterface.saveAttributes()} to the
     * {@code APP2 MPF} {@code MP Entry} table in-place when any size or offset entry needs update.
     */
    static void reconcile(@NonNull File file, @NonNull Snapshot before) throws IOException {
        if (!file.exists()) {
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            Snapshot after = readPrimaryMpfSection(raf);
            if (after == null || after.mIfd.mNumberOfImages != before.mIfd.mNumberOfImages) {
                Logger.d(TAG, "Skipping MPF reconcile: post-save MPF header missing or changed.");
                return;
            }

            long tailDelta = after.mFileLength - before.mFileLength;
            long offsetDelta =
                    tailDelta - (after.mIfd.mMpHeaderOffset - before.mIfd.mMpHeaderOffset);
            if (tailDelta == 0L && offsetDelta == 0L) {
                return;
            }

            ByteBuffer beforeBuf = before.mIfd.mMpBuffer;
            ByteBuffer afterBuf = after.mIfd.mMpBuffer;
            byte[] soi = new byte[2];
            boolean modified = false;
            for (int i = 0; i < after.mIfd.mNumberOfImages; i++) {
                int beforePos = before.mIfd.mMpEntriesOffset + i * MP_ENTRY_SIZE;
                long beforeSize = beforeBuf.getInt(beforePos + 4) & 0xFFFFFFFFL;
                long beforeOffset = beforeBuf.getInt(beforePos + 8) & 0xFFFFFFFFL;

                int afterPos = after.mIfd.mMpEntriesOffset + i * MP_ENTRY_SIZE;
                long attr = afterBuf.getInt(afterPos) & 0xFFFFFFFFL;
                long currentSize = afterBuf.getInt(afterPos + 4) & 0xFFFFFFFFL;
                long currentOffset = afterBuf.getInt(afterPos + 8) & 0xFFFFFFFFL;

                long expectedSize;
                long expectedOffset;
                if (i == 0) {
                    if (beforeOffset != 0L || currentOffset != 0L) {
                        Logger.d(TAG, "Skipping MPF reconcile: non-zero primary image offset.");
                        return;
                    }
                    expectedOffset = 0L;
                    expectedSize = (beforeSize == 0L) ? 0L : beforeSize + tailDelta;
                    if (beforeSize != 0L
                            && (expectedSize <= 0L
                                    || expectedSize > after.mFileLength
                                    || expectedSize > 0xFFFFFFFFL)) {
                        Logger.d(TAG, "Skipping MPF reconcile: primary size out of bounds.");
                        return;
                    }
                } else {
                    if (beforeOffset == 0L || currentOffset == 0L) {
                        Logger.d(TAG, "Skipping MPF reconcile: missing dependent image offset.");
                        return;
                    }
                    expectedSize = beforeSize;
                    expectedOffset = beforeOffset + offsetDelta;
                    long targetStart = after.mIfd.mMpHeaderOffset + expectedOffset;
                    if (expectedOffset <= 0L
                            || expectedOffset > 0xFFFFFFFFL
                            || targetStart < after.mPrimarySosEnd
                            || targetStart + 2 > after.mFileLength) {
                        Logger.d(TAG, "Skipping MPF reconcile: dependent offset out of bounds.");
                        return;
                    }
                    if ((attr & MP_ATTR_DATA_FORMAT_MASK) == 0L) {
                        raf.seek(targetStart);
                        raf.readFully(soi);
                        if ((soi[0] & 0xFF) != MARKER_PREFIX || (soi[1] & 0xFF) != MARKER_SOI) {
                            Logger.d(TAG, "Skipping MPF reconcile: missing SOI at target offset.");
                            return;
                        }
                    }
                }

                if (currentSize != expectedSize || currentOffset != expectedOffset) {
                    afterBuf.putInt(afterPos + 4, (int) expectedSize);
                    afterBuf.putInt(afterPos + 8, (int) expectedOffset);
                    modified = true;
                }
            }

            if (modified) {
                raf.seek(after.mIfd.mMpHeaderOffset + after.mIfd.mMpEntriesOffset);
                raf.write(
                        afterBuf.array(),
                        afterBuf.arrayOffset() + after.mIfd.mMpEntriesOffset,
                        after.mIfd.mNumberOfImages * MP_ENTRY_SIZE);
            }
        }
    }

    private static @Nullable Snapshot readPrimaryMpfSection(@NonNull RandomAccessFile raf)
            throws IOException {
        long fileLength = raf.length();
        if (fileLength < 4) {
            return null;
        }
        byte[] scratch = new byte[4];
        raf.readFully(scratch, 0, 2);
        if ((scratch[0] & 0xFF) != MARKER_PREFIX || (scratch[1] & 0xFF) != MARKER_SOI) {
            return null;
        }

        ParsedMpfIfd parsedMpf = null;
        long pos = 2;
        while (pos + 4 <= fileLength) {
            raf.readFully(scratch, 0, 4);
            if ((scratch[0] & 0xFF) != MARKER_PREFIX) {
                return null;
            }
            int marker = scratch[1] & 0xFF;
            int segLen;
            if (marker != MARKER_PREFIX) {
                segLen = ((scratch[2] & 0xFF) << 8) | (scratch[3] & 0xFF);
            } else {
                // Handle optional 0xFF fill bytes before marker code (ITU T.81 B.1.1.2).
                raf.seek(pos + 2);
                do {
                    marker = raf.read();
                } while (marker == MARKER_PREFIX);
                if (marker < 0 || raf.getFilePointer() + 2 > fileLength) {
                    return null;
                }
                raf.readFully(scratch, 0, 2);
                segLen = ((scratch[0] & 0xFF) << 8) | (scratch[1] & 0xFF);
            }

            if (marker == 0 || marker == MARKER_SOI || marker == MARKER_EOI || segLen < 2) {
                return null;
            }
            long segEnd = raf.getFilePointer() + (segLen - 2);
            if (segEnd > fileLength) {
                return null;
            }

            if (marker == MARKER_APP2
                    && parsedMpf == null
                    && segLen >= 2 + MPF_SIGNATURE_SIZE + TIFF_HEADER_SIZE) {
                raf.readFully(scratch, 0, MPF_SIGNATURE_SIZE);
                if (scratch[0] == 'M'
                        && scratch[1] == 'P'
                        && scratch[2] == 'F'
                        && scratch[3] == 0) {
                    long candidateOffset = raf.getFilePointer();
                    byte[] candidateBody = new byte[segLen - 2 - MPF_SIGNATURE_SIZE];
                    raf.readFully(candidateBody);
                    parsedMpf = parseMpfIndexIfd(candidateOffset, candidateBody);
                }
            }

            if (marker == MARKER_SOS) {
                return parsedMpf == null ? null : new Snapshot(fileLength, segEnd, parsedMpf);
            }
            raf.seek(segEnd);
            pos = segEnd;
        }
        return null;
    }

    private static @Nullable ParsedMpfIfd parseMpfIndexIfd(
            long mpHeaderOffset, byte @NonNull [] mpBody) {
        if (mpBody.length < TIFF_HEADER_SIZE) {
            return null;
        }
        ByteOrder byteOrder;
        if (mpBody[0] == 'I' && mpBody[1] == 'I' && mpBody[2] == 0x2A && mpBody[3] == 0x00) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else if (mpBody[0] == 'M' && mpBody[1] == 'M' && mpBody[2] == 0x00 && mpBody[3] == 0x2A) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else {
            return null;
        }

        ByteBuffer buf = ByteBuffer.wrap(mpBody).order(byteOrder);
        long ifdOffset = buf.getInt(4) & 0xFFFFFFFFL;
        if (ifdOffset < TIFF_HEADER_SIZE || ifdOffset + 2 > mpBody.length) {
            return null;
        }
        int tagCount = buf.getShort((int) ifdOffset) & 0xFFFF;
        // Include 2-byte tagCount + 12-byte entries + 4-byte nextIfdOffset (CIPA DC-007 Fig. 6).
        long ifdEnd = ifdOffset + 2 + (long) tagCount * IFD_ENTRY_SIZE + 4;
        if (ifdEnd > mpBody.length) {
            return null;
        }

        long numberOfImages = -1;
        int mpEntriesOffset = -1;
        int previousTag = -1;
        int pos = (int) ifdOffset + 2;
        for (int i = 0; i < tagCount; i++, pos += IFD_ENTRY_SIZE) {
            int tag = buf.getShort(pos) & 0xFFFF;
            if (tag <= previousTag) {
                return null;
            }
            previousTag = tag;

            int type = buf.getShort(pos + 2) & 0xFFFF;
            long count = buf.getInt(pos + 4) & 0xFFFFFFFFL;
            long valueOrOffset = buf.getInt(pos + 8) & 0xFFFFFFFFL;

            if (tag == TAG_NUMBER_OF_IMAGES) {
                if (type != TYPE_ULONG || count != 1 || valueOrOffset < 2) {
                    return null;
                }
                numberOfImages = valueOrOffset;
            } else if (tag == TAG_MP_ENTRY) {
                if (type != TYPE_UNDEFINED
                        || numberOfImages < 2
                        || count != numberOfImages * MP_ENTRY_SIZE
                        || valueOrOffset < TIFF_HEADER_SIZE
                        || valueOrOffset + count > mpBody.length
                        || (valueOrOffset < ifdEnd && valueOrOffset + count > ifdOffset)) {
                    return null;
                }
                mpEntriesOffset = (int) valueOrOffset;
            }
        }

        if (numberOfImages < 2 || mpEntriesOffset < 0) {
            return null;
        }
        return new ParsedMpfIfd(mpHeaderOffset, buf, (int) numberOfImages, mpEntriesOffset);
    }

    private static final class ParsedMpfIfd {
        private final long mMpHeaderOffset;
        private final ByteBuffer mMpBuffer;
        private final int mNumberOfImages;
        private final int mMpEntriesOffset;

        private ParsedMpfIfd(
                long mpHeaderOffset,
                @NonNull ByteBuffer mpBuffer,
                int numberOfImages,
                int mpEntriesOffset) {
            mMpHeaderOffset = mpHeaderOffset;
            mMpBuffer = mpBuffer;
            mNumberOfImages = numberOfImages;
            mMpEntriesOffset = mpEntriesOffset;
        }
    }

    static final class Snapshot {
        private final long mFileLength;
        private final long mPrimarySosEnd;
        private final ParsedMpfIfd mIfd;

        private Snapshot(long fileLength, long primarySosEnd, @NonNull ParsedMpfIfd ifd) {
            mFileLength = fileLength;
            mPrimarySosEnd = primarySosEnd;
            mIfd = ifd;
        }
    }
}
