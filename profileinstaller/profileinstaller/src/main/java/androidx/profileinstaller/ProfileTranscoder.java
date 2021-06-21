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

import static androidx.profileinstaller.Encoding.UINT_16_SIZE;
import static androidx.profileinstaller.Encoding.bitsToBytes;
import static androidx.profileinstaller.Encoding.error;
import static androidx.profileinstaller.Encoding.read;
import static androidx.profileinstaller.Encoding.readCompressed;
import static androidx.profileinstaller.Encoding.readString;
import static androidx.profileinstaller.Encoding.readUInt16;
import static androidx.profileinstaller.Encoding.readUInt32;
import static androidx.profileinstaller.Encoding.readUInt8;
import static androidx.profileinstaller.Encoding.utf8Length;
import static androidx.profileinstaller.Encoding.writeString;
import static androidx.profileinstaller.Encoding.writeUInt16;
import static androidx.profileinstaller.Encoding.writeUInt32;
import static androidx.profileinstaller.Encoding.writeUInt8;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@RequiresApi(19)
class ProfileTranscoder {
    private ProfileTranscoder() {
    }

    private static final int HOT = 1;
    private static final int STARTUP = 1 << 1;
    private static final int POST_STARTUP = 1 << 2;
    private static final int INLINE_CACHE_MISSING_TYPES_ENCODING = 6;
    private static final int INLINE_CACHE_MEGAMORPHIC_ENCODING = 7;

    static final byte[] MAGIC = new byte[]{'p', 'r', 'o', '\u0000'};

    static byte[] readHeader(@NonNull InputStream is) throws IOException {
        byte[] fileMagic = read(is, MAGIC.length);
        if (!Arrays.equals(MAGIC, fileMagic)) {
            // If we find a file that doesn't claim to be a profile, something really unexpected
            // has happened. Fail.
            throw error("Invalid magic");
        }
        return read(is, ProfileVersion.V010_P.length);
    }

    static void writeHeader(@NonNull OutputStream os, byte[] version) throws IOException {
        os.write(MAGIC);
        os.write(version);
    }

    /**
     * Transcode (or convert) a binary profile from one format version to another.
     *
     * @param os The destination output stream for the binary ART profile to be written to. This
     *           profile will be encoded in the [desiredVersion] format.
     * @param desiredVersion The desired version of the ART Profile to be written to [os]
     * @return A boolean indicating whether or not the profile was successfully written to the
     * output stream in the desired format.
     */
    static boolean transcodeAndWriteBody(
            @NonNull OutputStream os,
            @NonNull byte[] desiredVersion,
            @NonNull Map<String, DexProfileData> data
    ) throws IOException {
        if (Arrays.equals(desiredVersion, ProfileVersion.V005_O)) {
            writeProfileForO(os, data);
            return true;
        }

        if (Arrays.equals(desiredVersion, ProfileVersion.V001_N)) {
            writeProfileForN(os, data);
            return true;
        }

        return false;
    }

    /**
     * Writes the provided [lines] out into a binary profile suitable for N devices. This method
     * expects that the MAGIC and Version of the profile header have already been written to the
     * OutputStream.
     *
     * This format has the following encoding:
     *
     *    magic,version,number_of_lines
     *    dex_location1,number_of_methods1,number_of_classes1,dex_location_checksum1, \
     *        method_id11,method_id12...,class_id1,class_id2...
     *    dex_location2,number_of_methods2,number_of_classes2,dex_location_checksum2, \
     *        method_id21,method_id22...,,class_id1,class_id2...
     *    .....
     */
    private static void writeProfileForN(
            @NonNull OutputStream os,
            @NonNull Map<String, DexProfileData> lines
    ) throws IOException {
        writeUInt16(os, lines.size()); // number of dex files
        for (Map.Entry<String, DexProfileData> entry : lines.entrySet()) {
            String profileKey = entry.getKey();
            DexProfileData data = entry.getValue();
            writeUInt16(os, utf8Length(profileKey));
            writeUInt16(os, data.methods.size());
            writeUInt16(os, data.classes.size());
            writeUInt32(os, data.dexChecksum);
            writeString(os, profileKey);

            for (int id : data.methods.keySet()) {
                writeUInt16(os, id);
            }

            for (int id : data.classes) {
                writeUInt16(os, id);
            }
        }
    }

    /**
     * Writes the provided [lines] out into a binary profile suitable for O devices. This method
     * expects that the MAGIC and Version of the profile header have already been written to the
     * OutputStream.
     *
     * This format has the following encoding:
     *
     *    magic,version,number_of_dex_files
     *    dex_location1,number_of_classes1,methods_region_size,dex_location_checksum1, \
     *        method_encoding_11,method_encoding_12...,class_id1,class_id2...
     *    dex_location2,number_of_classes2,methods_region_size,dex_location_checksum2, \
     *        method_encoding_21,method_encoding_22...,,class_id1,class_id2...
     *    .....
     *
     * The method_encoding is:
     *    method_id,number_of_inline_caches,inline_cache1,inline_cache2...
     *
     * The inline_cache is:
     *    dex_pc,[M|dex_map_size], dex_profile_index,class_id1,class_id2...,dex_profile_index2,...
     *    dex_map_size is the number of dex_indices that follows.
     *       Classes are grouped per their dex files and the line
     *       `dex_profile_index,class_id1,class_id2...,dex_profile_index2,...` encodes the
     *       mapping from `dex_profile_index` to the set of classes `class_id1,class_id2...`
     *    M stands for megamorphic or missing types and it's encoded as either
     *    the byte [INLINE_CACHE_MEGAMORPHIC_ENCODING] or [INLINE_CACHE_MISSING_TYPES_ENCODING].
     *    When present, there will be no class ids following.
     *    .....
     *
     * Note that currently we never encode any inline cache data.
     */
    private static void writeProfileForO(
            @NonNull OutputStream os,
            @NonNull Map<String, DexProfileData> lines
    ) throws IOException {
        writeUInt8(os, lines.size()); // number of dex files
        for (Map.Entry<String, DexProfileData> entry : lines.entrySet()) {
            String key = entry.getKey();
            DexProfileData data = entry.getValue();
            int hotMethodRegionSize = data.methods.size() * (
                    UINT_16_SIZE + // method id
                            UINT_16_SIZE);// inline cache size (should always be 0 for us)
            writeUInt16(os, utf8Length(key));
            writeUInt16(os, data.classes.size());
            writeUInt32(os, hotMethodRegionSize);
            writeUInt32(os, data.dexChecksum);
            writeString(os, key);

            for (int id : data.methods.keySet()) {
                writeUInt16(os, id);
                // 0 for inline cache size, since we never encode any inline cache data.
                writeUInt16(os, 0);
            }

            for (int id : data.classes) {
                writeUInt16(os, id);
            }
        }
    }

    /**
     * Reads and parses data from the InputStream into an in-memory representation, to later be
     * written to disk using [writeProfileForO] or [writeProfileForN]. This method expects that
     * the MAGIC and the VERSION of the InputStream have already been read.
     *
     * This method assumes the profile is stored with the [V010_P] encoding.
     *
     * This encoding is as follows:
     *
     * [profile_header, zipped[[dex_data_header1, dex_data_header2...],[dex_data1,
     *    dex_data2...]]]
     *
     * profile_header:
     *   magic,version,number_of_dex_files,uncompressed_size_of_zipped_data,compressed_data_size
     *
     * dex_data_header:
     *   dex_location,number_of_classes,methods_region_size,dex_location_checksum,num_method_ids
     *
     * dex_data:
     *   method_encoding_1,method_encoding_2...,class_id1,class_id2...,startup/post startup bitmap.
     *
     * The method_encoding is:
     *    method_id,number_of_inline_caches,inline_cache1,inline_cache2...
     *
     * The inline_cache is:
     *    dex_pc,[M|dex_map_size], dex_profile_index,class_id1,class_id2...,dex_profile_index2,...
     *    dex_map_size os the number of dex_indices that follows.
     *       Classes are grouped per their dex files and the line
     *       `dex_profile_index,class_id1,class_id2...,dex_profile_index2,...` encodes the
     *       mapping from `dex_profile_index` to the set of classes `class_id1,class_id2...`
     *    M stands for megamorphic or missing types and it's encoded as either
     *    the byte [INLINE_CACHE_MEGAMORPHIC_ENCODING] or [INLINE_CACHE_MISSING_TYPES_ENCODING].
     *    When present, there will be no class ids following.
     *
     * @param is The InputStream for the P+ binary profile
     * @return A map of keys (dex names) to the parsed [DexProfileData] for that dex.
     */
    static @NonNull Map<String, DexProfileData> readProfile(
            @NonNull InputStream is,
            @NonNull byte[] version
    ) throws IOException {
        if (!Arrays.equals(version, ProfileVersion.V010_P)) {
            throw error("Unsupported version");
        }
        int numberOfDexFiles = readUInt8(is);
        long uncompressedDataSize = readUInt32(is);
        long compressedDataSize = readUInt32(is);

        // We are done with the header, so everything that follows is the compressed blob. We
        // uncompress it all and load it into memory
        byte[] uncompressedData = readCompressed(
                is,
                (int) compressedDataSize,
                (int) uncompressedDataSize
        );
        if (is.read() > 0) throw error("Content found after the end of file");

        try (InputStream dataStream = new ByteArrayInputStream(uncompressedData)) {
            return readUncompressedBody(dataStream, numberOfDexFiles);
        }
    }

    /**
     * Parses the un-zipped blob of data in the P+ profile format. It is assumed that no data has
     * been read from this blob, and that the InputStream that this method is passed was just
     * decompressed from the original file.
     *
     * @return A map of keys (dex names) to the parsed [DexProfileData] for that dex.
     */
    private static @NonNull Map<String, DexProfileData> readUncompressedBody(
            @NonNull InputStream is,
            int numberOfDexFiles
    ) throws IOException {
        // If the uncompressed profile data stream is empty then we have nothing more to do.
        if (is.available() == 0) {
            return new HashMap<>();
        }
        // Read the dex file line headers.
        DexProfileData[] lines = new DexProfileData[numberOfDexFiles];
        for (int i = 0; i < numberOfDexFiles; i++) {
            int keySize = readUInt16(is);
            int classSetSize = readUInt16(is);
            long hotMethodRegionSize = readUInt32(is);
            long dexChecksum = readUInt32(is);
            long numMethodIds = readUInt32(is);
            String key = readString(is, keySize);
            lines[i] = new DexProfileData(
                    key,
                    dexChecksum,
                    classSetSize,
                    (int) hotMethodRegionSize,
                    (int) numMethodIds,
                    // NOTE: It is important to use LinkedHashSet/LinkedHashMap here to
                    // ensure that iteration order matches insertion order
                    new LinkedHashSet<>(),
                    new LinkedHashMap<>()
            );
        }

        HashMap<String, DexProfileData> result = new HashMap<>(numberOfDexFiles);

        // Load data for each discovered dex file.
        for (DexProfileData data : lines) {
            // The hot methods are stored one-by-one with the inline cache information alongside it.
            readHotMethodRegion(is, data);

            // Then the startup classes are stored
            readClasses(is, data);

            // In addition to [HOT], the methods can be labeled as [STARTUP] and [POST_STARTUP].
            // To compress this information better, this information is stored as a bitmap, with
            // 2-bits per method in the entire dex.
            readMethodBitmap(is, data);

            // save the parsed data for each dex
            result.put(data.key, data);
        }

        return result;
    }

    private static void readHotMethodRegion(
            @NonNull InputStream is,
            @NonNull DexProfileData data
    ) throws IOException {
        int expectedBytesAvailableAfterRead = is.available() - data.hotMethodRegionSize;
        int lastMethodIndex = 0;

        // Read one method at a time until we reach the end of the method region.
        while (is.available() > expectedBytesAvailableAfterRead) {
            // The profile stores the first method index, then the remainder are relative to the
            // previous
            // value.
            int diffWithLastMethodDexIndex = readUInt16(is);
            int methodDexIndex = lastMethodIndex + diffWithLastMethodDexIndex;

            data.methods.put(methodDexIndex, HOT);

            // Read the inline caches.
            int inlineCacheSize = readUInt16(is);
            while (inlineCacheSize > 0) {
                skipInlineCache(is);
                --inlineCacheSize;
            }
            // Update the last method index.
            lastMethodIndex = methodDexIndex;
        }

        // Check that we read exactly the amount of bytes specified by the method region size.
        if (is.available() != expectedBytesAvailableAfterRead) {
            throw error(
                    "Read too much data during profile line parse"
            );
        }
    }

    private static void skipInlineCache(@NonNull InputStream is) throws IOException {
        /* val dexPc = */readUInt16(is);
        int dexPcMapSize = readUInt8(is);

        // Check for missing type encoding.
        if (dexPcMapSize == INLINE_CACHE_MISSING_TYPES_ENCODING) {
            return;
        }
        // Check for megamorphic encoding.
        if (dexPcMapSize == INLINE_CACHE_MEGAMORPHIC_ENCODING) {
            return;
        }

        // The inline cache is not missing types and it's not megamorphic. Read the types available
        // for each dex pc.
        while (dexPcMapSize > 0) {
            /* val profileIndex = */readUInt8(is);
            int numClasses = readUInt8(is);
            while (numClasses > 0) {
                /* val classDexIndex = */readUInt16(is);
                --numClasses;
            }
            --dexPcMapSize;
        }
    }

    private static void readClasses(
            @NonNull InputStream is,
            @NonNull DexProfileData data
    ) throws IOException {
        int lastClassIndex = 0;
        for (int k = 0; k < data.classSetSize; k++) {
            int diffWithTheLastClassIndex = readUInt16(is);
            int classDexIndex = lastClassIndex + diffWithTheLastClassIndex;
            data.classes.add(classDexIndex);
            lastClassIndex = classDexIndex;
        }
    }

    private static void readMethodBitmap(
            @NonNull InputStream is,
            @NonNull DexProfileData data
    ) throws IOException {
        int methodBitmapStorageSize = bitsToBytes(data.numMethodIds * 2);
        byte[] methodBitmap = read(is, methodBitmapStorageSize);
        BitSet bs = BitSet.valueOf(methodBitmap);
        for (int methodIndex = 0; methodIndex < data.numMethodIds; methodIndex++) {
            int newFlags = readFlagsFromBitmap(bs, methodIndex, data.numMethodIds);
            if (newFlags != 0) {
                Integer current = data.methods.get(methodIndex);
                if (current == null) current = 0;
                data.methods.put(methodIndex, current | newFlags);
            }
        }
    }

    private static int readFlagsFromBitmap(@NonNull BitSet bs, int methodIndex, int numMethodIds) {
        int result = 0;
        if (bs.get(methodFlagBitmapIndex(STARTUP, methodIndex, numMethodIds))) {
            result |= STARTUP;
        }
        if (bs.get(methodFlagBitmapIndex(POST_STARTUP, methodIndex, numMethodIds))) {
            result |= POST_STARTUP;
        }
        return result;
    }

    private static int methodFlagBitmapIndex(int flag, int methodIndex, int numMethodIds) {
        // The format is [startup bitmap][post startup bitmap][AmStartup][...]
        // This compresses better than ([startup bit][post startup bit])*
        switch (flag) {
            case HOT:
                throw error("HOT methods are not stored in the bitmap");
            case STARTUP:
                return methodIndex;
            case POST_STARTUP:
                return methodIndex + numMethodIds;
            default:
                throw error("Unexpected flag: " + flag);
        }
    }
}
