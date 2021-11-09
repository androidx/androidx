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

import static androidx.profileinstaller.Encoding.SIZEOF_BYTE;
import static androidx.profileinstaller.Encoding.UINT_16_SIZE;
import static androidx.profileinstaller.Encoding.UINT_32_SIZE;
import static androidx.profileinstaller.Encoding.bitsToBytes;
import static androidx.profileinstaller.Encoding.error;
import static androidx.profileinstaller.Encoding.read;
import static androidx.profileinstaller.Encoding.readCompressed;
import static androidx.profileinstaller.Encoding.readString;
import static androidx.profileinstaller.Encoding.readUInt16;
import static androidx.profileinstaller.Encoding.readUInt32;
import static androidx.profileinstaller.Encoding.readUInt8;
import static androidx.profileinstaller.Encoding.utf8Length;
import static androidx.profileinstaller.Encoding.writeCompressed;
import static androidx.profileinstaller.Encoding.writeString;
import static androidx.profileinstaller.Encoding.writeUInt16;
import static androidx.profileinstaller.Encoding.writeUInt32;
import static androidx.profileinstaller.Encoding.writeUInt8;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@RequiresApi(19)
class ProfileTranscoder {
    private ProfileTranscoder() {
    }

    private static final int HOT = 1;
    private static final int STARTUP = 1 << 1;
    private static final int POST_STARTUP = 1 << 2;
    private static final int INLINE_CACHE_MISSING_TYPES_ENCODING = 6;
    private static final int INLINE_CACHE_MEGAMORPHIC_ENCODING = 7;

    static final byte[] MAGIC_PROF = new byte[]{'p', 'r', 'o', '\u0000'};
    static final byte[] MAGIC_PROFM = new byte[]{'p', 'r', 'm', '\u0000'};

    static byte[] readHeader(@NonNull InputStream is, @NonNull byte[] magic) throws IOException {
        byte[] fileMagic = read(is, magic.length);
        if (!Arrays.equals(magic, fileMagic)) {
            // If we find a file that doesn't claim to be a profile, something really unexpected
            // has happened. Fail.
            throw error("Invalid magic");
        }
        return read(is, ProfileVersion.V010_P.length);
    }

    static void writeHeader(@NonNull OutputStream os, byte[] version) throws IOException {
        os.write(MAGIC_PROF);
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
            @NonNull DexProfileData[] data
    ) throws IOException {
        if (Arrays.equals(desiredVersion, ProfileVersion.V010_P)) {
            writeProfileForP(os, data);
            return true;
        }

        if (Arrays.equals(desiredVersion, ProfileVersion.V005_O)) {
            writeProfileForO(os, data);
            return true;
        }

        if (Arrays.equals(desiredVersion, ProfileVersion.V009_O_MR1)) {
            writeProfileForO_MR1(os, data);
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
            @NonNull DexProfileData[] lines
    ) throws IOException {
        writeUInt16(os, lines.length); // number of dex files
        for (DexProfileData data : lines) {
            String profileKey = generateDexKey(data.apkName, data.dexName, ProfileVersion.V001_N);
            writeUInt16(os, utf8Length(profileKey));
            writeUInt16(os, data.methods.size());
            writeUInt16(os, data.classes.length);
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
     * Writes the provided [lines] out into a binary profile suitable for P,Q,R devices. This
     * method expects that the MAGIC and Version of the profile header have already been written
     * to the OutputStream.
     *
     * This format has the following encoding:
     *
     * [profile_header, zipped[[dex_data_header1, dex_data_header2...],[dex_data1,
     *    dex_data2...], global_aggregation_count]]
     * profile_header:
     *   magic,version,number_of_dex_files,uncompressed_size_of_zipped_data,compressed_data_size
     * dex_data_header:
     *   dex_location,number_of_classes,methods_region_size,dex_location_checksum,num_method_ids
     * dex_data:
     *   method_encoding_1,method_encoding_2...,class_id1,class_id2...,startup/post startup bitmap,
     *   aggregation_counters_for_classes, aggregation_counters_for_methods.
     * The method_encoding is:
     *    method_id,number_of_inline_caches,inline_cache1,inline_cache2...
     * The inline_cache is:
     *    dex_pc,[M|dex_map_size], dex_profile_index,class_id1,class_id2...,dex_profile_index2,...
     *    dex_map_size os the number of dex_indices that follows.
     *       Classes are grouped per their dex files and the line
     *       `dex_profile_index,class_id1,class_id2...,dex_profile_index2,...` encodes the
     *       mapping from `dex_profile_index` to the set of classes `class_id1,class_id2...`
     *    M stands for megamorphic or missing types and it's encoded as either
     *    the byte kIsMegamorphicEncoding or kIsMissingTypesEncoding.
     *    When present, there will be no class ids following.
     * The aggregation_counters_for_classes is stored only for 5.0.0 version and its format is:
     *   num_classes,count_for_class1,count_for_class2....
     * The aggregation_counters_for_methods is stored only for 5.0.0 version and its format is:
     *   num_methods,count_for_method1,count_for_method2....
     * The aggregation counters are sorted based on the index of the class/method.
     *
     * Note that currently we never encode any inline cache data.
     */
    private static void writeProfileForP(
            @NonNull OutputStream os,
            @NonNull DexProfileData[] lines
    ) throws IOException {
        byte[] profileBytes = createCompressibleBody(lines, ProfileVersion.V010_P);
        writeUInt8(os, lines.length); // number of dex files
        writeCompressed(os, profileBytes);
    }

    private static void writeProfileForO_MR1(
            @NonNull OutputStream os,
            @NonNull DexProfileData[] lines
    ) throws IOException {
        byte[] profileBytes = createCompressibleBody(lines, ProfileVersion.V009_O_MR1);
        writeUInt8(os, lines.length); // number of dex files
        writeCompressed(os, profileBytes);
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
            @NonNull DexProfileData[] lines
    ) throws IOException {
        writeUInt8(os, lines.length); // number of dex files
        for (DexProfileData data : lines) {
            int hotMethodRegionSize = data.methods.size() * (
                    UINT_16_SIZE + // method id
                            UINT_16_SIZE);// inline cache size (should always be 0 for us)
            String dexKey = generateDexKey(data.apkName, data.dexName, ProfileVersion.V005_O);
            writeUInt16(os, utf8Length(dexKey));
            writeUInt16(os, data.classes.length);
            writeUInt32(os, hotMethodRegionSize);
            writeUInt32(os, data.dexChecksum);
            writeString(os, dexKey);

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
     * Create compressable body only for V0.1.0 v0.0.9.
     *
     * For 0.1.0 this will write header/header/header/body/body/body
     * For 0.0.9 this will write header/body/header/body/header/body
     */
    private static @NonNull byte[] createCompressibleBody(
            @NonNull DexProfileData[] lines,
            @NonNull byte[] version
    ) throws IOException {
        // Start by creating a couple of caches for the data we re-use during serialization.

        // The required capacity in bytes for the uncompressed profile data.
        int requiredCapacity = 0;
        // Maps dex file to the size their method region will occupy. We need this when computing
        // the overall size requirements and for serializing the dex file data. The computation is
        // expensive as it walks all methods recorded in the profile.
        for (DexProfileData data : lines) {
            int lineHeaderSize =
                    (UINT_16_SIZE // classes set size
                            + UINT_16_SIZE // dex location size
                            + UINT_32_SIZE // method map size
                            + UINT_32_SIZE // checksum
                            + UINT_32_SIZE); // number of method ids
            String dexKey = generateDexKey(data.apkName, data.dexName, version);
            requiredCapacity += lineHeaderSize
                    + utf8Length(dexKey)
                    + data.classSetSize * UINT_16_SIZE + data.hotMethodRegionSize
                    + getMethodBitmapStorageSize(data.numMethodIds);
        }

        // Start serializing the data.
        ByteArrayOutputStream dataBos = new ByteArrayOutputStream(requiredCapacity);

        // Dex files must be written in the order of their profile index. This
        // avoids writing the index in the output file and simplifies the parsing logic.
        // Write profile line headers.

        if (Arrays.equals(version, ProfileVersion.V009_O_MR1)) {
            // interleave header/body/header/body on V009
            for (DexProfileData data : lines) {
                String dexKey = generateDexKey(data.apkName, data.dexName, version);
                writeLineHeader(dataBos, data, dexKey);
                writeLineData(dataBos, data);
            }
        } else {
            // after V010 format is always header/header/header/body/body/body
            // Write dex file line headers.
            for (DexProfileData data : lines) {
                String dexKey = generateDexKey(data.apkName, data.dexName, version);
                writeLineHeader(dataBos, data, dexKey);
            }

            // Write dex file data.
            for (DexProfileData data : lines) {
                writeLineData(dataBos, data);
            }
        }

        if (dataBos.size() != requiredCapacity) {
            throw error("The bytes saved do not match expectation. actual="
                    + dataBos.size() + " expected=" + requiredCapacity);
        }
        return dataBos.toByteArray();
    }

    private static int getMethodBitmapStorageSize(int numMethodIds) {
        int methodBitmapBits = numMethodIds * 2; /* 2 bits per method */
        return roundUpToByte(methodBitmapBits) / SIZEOF_BYTE;
    }

    private static int roundUpToByte(int bits) {
        return (bits + SIZEOF_BYTE - 1) & -SIZEOF_BYTE;
    }

    /**
     * Sets the bit corresponding to the {@param isStartup} flag in the method bitmap.
     *
     * @param bitmap the method bitmap
     * @param flag whether or not this is the startup bit
     * @param methodIndex the method index in the dex file
     * @param dexData the method dex file
     */
    private static void setMethodBitmapBit(
            @NonNull byte[] bitmap,
            int flag,
            int methodIndex,
            @NonNull DexProfileData dexData
    ) {
        int bitIndex = methodFlagBitmapIndex(flag, methodIndex, dexData.numMethodIds);
        int bitmapIndex = bitIndex / SIZEOF_BYTE;
        byte value = (byte)(bitmap[bitmapIndex] | (1 << (bitIndex % SIZEOF_BYTE)));
        bitmap[bitmapIndex] = value;
    }


    /**
     * Writes the dex data header for the given dex file into the output stream.
     * @param os the destination OutputStream to write to
     * @param dexData the dex data to which the header belongs
     */
    private static void writeLineHeader(
            @NonNull OutputStream os,
            @NonNull DexProfileData dexData,
            @NonNull String dexKey
    ) throws IOException {
        writeUInt16(os, utf8Length(dexKey));
        writeUInt16(os, dexData.classSetSize);
        writeUInt32(os, dexData.hotMethodRegionSize);
        writeUInt32(os, dexData.dexChecksum);
        writeUInt32(os, dexData.numMethodIds);
        writeString(os, dexKey);
    }

    /**
     * Writes the given dex file data into the stream.
     *
     * Note that we allow dex files without any methods or classes, so that
     * inline caches can refer to valid dex files.
     * @param os the destination OutputStream to write to
     * @param dexData the dex data that should be serialized
     */
    private static void writeLineData(
            @NonNull OutputStream os,
            @NonNull DexProfileData dexData
    ) throws IOException {
        writeMethodsWithInlineCaches(os, dexData);
        writeClasses(os, dexData);
        writeMethodBitmap(os, dexData);
    }

    /**
     * Writes the methods with inline caches to the output stream.
     *
     * @param os the destination OutputStream to write to
     * @param dexData the dex data containing the methods that should be serialized
     */
    private static void writeMethodsWithInlineCaches(
            @NonNull OutputStream os,
            @NonNull DexProfileData dexData
    ) throws IOException {
        // The profile stores the first method index, then the remainder are relative
        // to the previous value.
        int lastMethodIndex = 0;
        for (Map.Entry<Integer, Integer> entry : dexData.methods.entrySet()) {
            int methodId = entry.getKey();
            int flags = entry.getValue();
            if ((flags & HOT) == 0) {
                continue;
            }
            int diffWithTheLastMethodIndex = methodId - lastMethodIndex;
            writeUInt16(os, diffWithTheLastMethodIndex);
            writeUInt16(os, 0); // no inline cache data
            lastMethodIndex = methodId;
        }
    }

    /**
     * Writes the dex file classes to the output stream.
     *
     * @param os the destination OutputStream to write to
     * @param dexData the dex data containing the classes that should be serialized
     */
    private static void writeClasses(
            @NonNull OutputStream os,
            @NonNull DexProfileData dexData
    ) throws IOException {
        // The profile stores the first class index, then the remainder are relative
        // to the previous value.
        int lastClassIndex = 0;
        // class ids must be sorted ascending so that each id is greater than the last since we
        // are writing unsigned ints and cannot represent negative values
        for (Integer classIndex : dexData.classes) {
            int diffWithTheLastClassIndex = classIndex - lastClassIndex;
            writeUInt16(os, diffWithTheLastClassIndex);
            lastClassIndex = classIndex;
        }
    }

    /**
     * Writes the methods flags as a bitmap to the output stream.
     * @param os the destination OutputStream to write to
     * @param dexData the dex data that should be serialized
     */
    private static void writeMethodBitmap(
            @NonNull OutputStream os,
            @NonNull DexProfileData dexData
    ) throws IOException {
        byte[] bitmap = new byte[getMethodBitmapStorageSize(dexData.numMethodIds)];
        for (Map.Entry<Integer, Integer> entry : dexData.methods.entrySet()) {
            int methodIndex = entry.getKey();
            int flagValue = entry.getValue();

            if ((flagValue & STARTUP) != 0) {
                setMethodBitmapBit(bitmap, STARTUP, methodIndex, dexData);
            }

            if ((flagValue & POST_STARTUP) != 0) {
                setMethodBitmapBit(bitmap, POST_STARTUP, methodIndex, dexData);
            }
        }
        os.write(bitmap);
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
    static @NonNull DexProfileData[] readProfile(
            @NonNull InputStream is,
            @NonNull byte[] version,
            @NonNull String apkName
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
            return readUncompressedBody(dataStream, apkName, numberOfDexFiles);
        }
    }

    /**
     *
     *
     * [profile_header, zipped[[dex_data_header1, dex_data_header2...],[dex_data1,
     *    dex_data2...], global_aggregation_count]]
     * profile_header:
     *   magic,version,number_of_dex_files,uncompressed_size_of_zipped_data,compressed_data_size
     * dex_data_header:
     *   dex_location,number_of_classes
     * dex_data:
     *   class_id1,class_id2...
     *
     * @param is
     * @param version
     * @param profile
     * @return
     * @throws IOException
     */
    static @NonNull DexProfileData[] readMeta(
            @NonNull InputStream is,
            @NonNull byte[] version,
            DexProfileData[] profile
    ) throws IOException {
        if (!Arrays.equals(version, ProfileVersion.METADATA_V001_N)) {
            throw error("Unsupported meta version");
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
            return readMetadataForNBody(dataStream, numberOfDexFiles, profile);
        }
    }

    /**
     * Parses the un-zipped blob of data in the P+ profile format. It is assumed that no data has
     * been read from this blob, and that the InputStream that this method is passed was just
     * decompressed from the original file.
     *
     * @return A map of keys (dex names) to the parsed [DexProfileData] for that dex.
     */
    private static @NonNull DexProfileData[] readMetadataForNBody(
            @NonNull InputStream is,
            int numberOfDexFiles,
            DexProfileData[] profile
    ) throws IOException {
        // If the uncompressed profile data stream is empty then we have nothing more to do.
        if (is.available() == 0) {
            return new DexProfileData[0];
        }
        if (numberOfDexFiles != profile.length) {
            throw error("Mismatched number of dex files found in metadata");
        }
        // Read the dex file line headers.
        String[] names = new String[numberOfDexFiles];
        int[] sizes = new int[numberOfDexFiles];
        for (int i = 0; i < numberOfDexFiles; i++) {
            int dexNameSize = readUInt16(is);
            sizes[i] = readUInt16(is);
            names[i] = readString(is, dexNameSize);
        }

        // Load data for each discovered dex file.
        for (int i = 0; i < numberOfDexFiles; i++) {
            DexProfileData data = profile[i];
            if (!data.dexName.equals(names[i])) {
                throw error("Order of dexfiles in metadata did not match baseline");
            }
            data.classSetSize = sizes[i];
            data.classes = new int[data.classSetSize];
            // Then the startup classes are stored
            readClasses(is, data);
        }

        return profile;
    }

    /**
     * Return a correctly formatted dex key in the format
     *       APK_NAME SEPARATOR DEX_NAME
     *
     * This returns one of:
     * 1. If dexName is "classes.dex" -> apkName
     * 2. If dexName ends with ".apk" -> dexName
     * 3. else -> $apkName$separator$deXName
     *
     * @param apkName name of APK to generate key for
     * @param dexName name of dex file, or input string if original profile dex key matched ".*\
     *                .apk"
     * @param version version array from {@see ProfileVersion}
     * @return correctly formatted dex key for this API version
     */
    @NonNull
    private static String generateDexKey(@NonNull String apkName, @NonNull String dexName,
            @NonNull byte[] version) {
        if (dexName.equals("classes.dex")) return apkName;
        return apkName + ProfileVersion.dexKeySeparator(version) + dexName;
    }

    /**
     * Parses the un-zipped blob of data in the P+ profile format. It is assumed that no data has
     * been read from this blob, and that the InputStream that this method is passed was just
     * decompressed from the original file.
     *
     * @return A map of keys (dex names) to the parsed [DexProfileData] for that dex.
     */
    private static @NonNull DexProfileData[] readUncompressedBody(
            @NonNull InputStream is,
            @NonNull String apkName,
            int numberOfDexFiles
    ) throws IOException {
        // If the uncompressed profile data stream is empty then we have nothing more to do.
        if (is.available() == 0) {
            return new DexProfileData[0];
        }
        // Read the dex file line headers.
        DexProfileData[] lines = new DexProfileData[numberOfDexFiles];
        for (int i = 0; i < numberOfDexFiles; i++) {
            int dexNameSize = readUInt16(is);
            int classSetSize = readUInt16(is);
            long hotMethodRegionSize = readUInt32(is);
            long dexChecksum = readUInt32(is);
            long numMethodIds = readUInt32(is);

            lines[i] = new DexProfileData(
                    apkName,
                    readString(is, dexNameSize), /* req: only dex name no separater from profgen */
                    dexChecksum,
                    classSetSize,
                    (int) hotMethodRegionSize,
                    (int) numMethodIds,
                    // NOTE: It is important to use LinkedHashSet/LinkedHashMap here to
                    // ensure that iteration order matches insertion order
                    new int[classSetSize],
                    new TreeMap<>()
            );
        }

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
        }

        return lines;
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
            // previous value.
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
            data.classes[k] = classDexIndex;
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
