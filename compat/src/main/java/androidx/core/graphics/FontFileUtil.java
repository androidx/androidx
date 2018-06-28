/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.fonts.FontVariationAxis;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a utility for font file operations.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class FontFileUtil {

    /**
     * A helper class for reading font files.
     */
    private static class FontFileReader {
        private final InputStream mIs;
        private byte[] mBuf;
        private int mPos = 0;

        FontFileReader(InputStream is) {
            mIs = is;
            mBuf = new byte[4];
        }

        public void skip(int length) throws IOException {
            mIs.skip(length);
            mPos += length;
        }

        public int read32() throws IOException {
            if (mIs.read(mBuf, 0, 4) != 4) {
                throw new IOException("Couldn't read 4 bytes.");
            }
            mPos += 4;
            return (((mBuf[0]) & 0xFF) << 24) | ((mBuf[1] & 0xFF) << 16) | ((mBuf[2] & 0xFF) << 8)
                    | (mBuf[3] & 0xFF);
        }

        public int read16() throws IOException {
            if (mIs.read(mBuf, 0, 2) != 2) {
                throw new IOException("Couldn't read 4 bytes.");
            }
            mPos += 2;
            return ((mBuf[0] & 0xFF) << 8) | (mBuf[1] & 0xFF);
        }

        public void jumptTo(int offset) throws IOException {
            if (offset < mPos) {
                throw new IOException("Unable to jump to offset: " + offset + " < " + mPos);
            }
            skip(offset - mPos);
        }
    }

    private FontFileUtil() {}  // Do not instanciate

    /**
     * Unpack the weight value from packed integer.
     */
    public static int unpackWeight(int packed) {
        return packed & 0xFFFF;
    }

    /**
     * Unpack the italic value from packed integer.
     */
    public static boolean unpackItalic(int packed) {
        return (packed & 0x10000) != 0;
    }

    private static int pack(@IntRange(from = 0, to = 1000) int weight, boolean italic) {
        return weight | (italic ? 0x10000 : 0);
    }

    private static final int SFNT_VERSION_1 = 0x00010000;
    private static final int SFNT_VERSION_OTTO = 0x4F54544F;
    private static final int TTC_TAG = 0x74746366;
    private static final int OS2_TABLE_TAG = 0x4F532F32;

    /**
     * Analyze the font file returns packed style info
     *
     * @param is
     * @param ttcIndex
     * @param varSettings
     * @return
     * @throws IOException
     */
    public static final int analyzeStyle(@NonNull InputStream is, @IntRange(from = 0) int ttcIndex,
            @Nullable String varSettings)
            throws IOException {
        int weight = -1;
        int italic = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && varSettings != null) {
            for (FontVariationAxis axis :
                    FontVariationAxis.fromFontVariationSettings(varSettings)) {
                if ("wght".equals(axis.getTag())) {
                    weight = (int) axis.getStyleValue();
                } else if ("ital".equals(axis.getTag())) {
                    italic = (axis.getStyleValue() == 1.0f) ? 1 : 0;
                }
            }
        }

        if (weight != -1 && italic != -1) {
            // Both weight/italic style are specifeid by variation settings.
            // No need to look into OS/2 table.
            // TODO: Good to look HVAR table to check if this font supports wght/ital axes.
            return pack(weight, italic == 1);
        }

        FontFileReader ffr = new FontFileReader(is);
        int sfntVersion;
        int magicNumber = ffr.read32();
        if (magicNumber == TTC_TAG) {
            // TTC file. jumpt to target font offset table.
            ffr.skip(4);  // skip majorVersion, minorVersion
            int numFonts = ffr.read32();
            if (ttcIndex >= numFonts) {
                throw new IOException("Font index out of bounds");
            }
            ffr.skip(4 * ttcIndex);  // Skip offset tables until specified index
            ffr.jumptTo(ffr.read32());  // Jumpt to specified offset table.
            sfntVersion = ffr.read32();
        } else {
            sfntVersion = magicNumber;
        }

        if (sfntVersion != SFNT_VERSION_1 && sfntVersion != SFNT_VERSION_OTTO) {
            throw new IOException("Unknown font file format");
        }

        int numTables = ffr.read16();
        ffr.skip(6);  // skip searchRange, entrySelector, rangeShift

        int os2TableOffset = -1;
        for (int i = 0; i < numTables; ++i) {
            if (ffr.read32() == OS2_TABLE_TAG) {
                ffr.skip(4);  // skip checksum
                os2TableOffset = ffr.read32();
                break;
            } else {
                ffr.skip(12);  // skip checksum, offset, length
            }
        }

        if (os2TableOffset == -1) {
            // Couldn't find OS/2 table. use regular style
            return pack(400, false);
        }

        ffr.jumptTo(os2TableOffset);

        ffr.skip(4);  // skip version, xAvgCharWidth
        int weightFromOS2 = ffr.read16();
        ffr.skip(56);  // skip until fsSelection
        boolean italicFromOS2 = (ffr.read16() & 1) != 0;

        return pack(weight == -1 ? weightFromOS2 : weight,
                italic == -1 ? italicFromOS2 : italic == 1);
    }
}
