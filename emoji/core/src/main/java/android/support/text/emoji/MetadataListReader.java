/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.text.emoji;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.AssetManager;
import android.support.annotation.AnyThread;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.text.emoji.flatbuffer.MetadataList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads the emoji metadata from a given InputStream or ByteBuffer.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@AnyThread
@RequiresApi(19)
class MetadataListReader {

    /**
     * Meta tag for emoji metadata. This string is used by the font update script to insert the
     * emoji meta into the font. This meta table contains the list of all emojis which are stored in
     * binary format using FlatBuffers. This flat list is later converted by the system into a trie.
     * {@code int} representation for "Emji"
     *
     * @see MetadataRepo
     */
    private static final int EMJI_TAG = 'E' << 24 | 'm' << 16 | 'j' << 8 | 'i';

    /**
     * Deprecated meta tag name. Do not use, kept for compatibility reasons, will be removed soon.
     */
    private static final int EMJI_TAG_DEPRECATED = 'e' << 24 | 'm' << 16 | 'j' << 8 | 'i';

    /**
     * The name of the meta table in the font. int representation for "meta"
     */
    private static final int META_TABLE_NAME = 'm' << 24 | 'e' << 16 | 't' << 8 | 'a';

    /**
     * Construct MetadataList from an input stream. Does not close the given InputStream, therefore
     * it is caller's responsibility to properly close the stream.
     *
     * @param inputStream InputStream to read emoji metadata from
     */
    static MetadataList read(InputStream inputStream) throws IOException {
        final OpenTypeReader openTypeReader = new InputStreamOpenTypeReader(inputStream);
        final OffsetInfo offsetInfo = findOffsetInfo(openTypeReader);
        // skip to where metadata is
        openTypeReader.skip((int) (offsetInfo.getStartOffset() - openTypeReader.getPosition()));
        // allocate a ByteBuffer and read into it since FlatBuffers can read only from a ByteBuffer
        final ByteBuffer buffer = ByteBuffer.allocate((int) offsetInfo.getLength());
        final int numRead = inputStream.read(buffer.array());
        if (numRead != offsetInfo.getLength()) {
            throw new IOException("Needed " + offsetInfo.getLength() + " bytes, got " + numRead);
        }

        return MetadataList.getRootAsMetadataList(buffer);
    }

    /**
     * Construct MetadataList from a byte buffer.
     *
     * @param byteBuffer ByteBuffer to read emoji metadata from
     */
    static MetadataList read(final ByteBuffer byteBuffer) throws IOException {
        final ByteBuffer newBuffer = byteBuffer.duplicate();
        final OpenTypeReader reader = new ByteBufferReader(newBuffer);
        final OffsetInfo offsetInfo = findOffsetInfo(reader);
        // skip to where metadata is
        newBuffer.position((int) offsetInfo.getStartOffset());
        return MetadataList.getRootAsMetadataList(newBuffer);
    }

    /**
     * Construct MetadataList from an asset.
     *
     * @param assetManager AssetManager instance
     * @param assetPath asset manager path of the file that the Typeface and metadata will be
     *                  created from
     */
    static MetadataList read(AssetManager assetManager, String assetPath)
            throws IOException {
        try (InputStream inputStream = assetManager.open(assetPath)) {
            return read(inputStream);
        }
    }

    /**
     * Finds the start offset and length of the emoji metadata in the font.
     *
     * @return OffsetInfo which contains start offset and length of the emoji metadata in the font
     *
     * @throws IOException
     */
    private static OffsetInfo findOffsetInfo(OpenTypeReader reader) throws IOException {
        // skip sfnt version
        reader.skip(OpenTypeReader.UINT32_BYTE_COUNT);
        // start of Table Count
        final int tableCount = reader.readUnsignedShort();
        if (tableCount > 100) {
            //something is wrong quit
            throw new IOException("Cannot read metadata.");
        }
        //skip to begining of tables data
        reader.skip(OpenTypeReader.UINT16_BYTE_COUNT * 3);

        long metaOffset = -1;
        for (int i = 0; i < tableCount; i++) {
            final int tag = reader.readTag();
            // skip checksum
            reader.skip(OpenTypeReader.UINT32_BYTE_COUNT);
            final long offset = reader.readUnsignedInt();
            // skip mLength
            reader.skip(OpenTypeReader.UINT32_BYTE_COUNT);
            if (META_TABLE_NAME == tag) {
                metaOffset = offset;
                break;
            }
        }

        if (metaOffset != -1) {
            // skip to the begining of meta tables.
            reader.skip((int) (metaOffset - reader.getPosition()));
            // skip minorVersion, majorVersion, flags, reserved,
            reader.skip(
                    OpenTypeReader.UINT16_BYTE_COUNT * 2 + OpenTypeReader.UINT32_BYTE_COUNT * 2);
            final long mapsCount = reader.readUnsignedInt();
            for (int i = 0; i < mapsCount; i++) {
                final int tag = reader.readTag();
                final long dataOffset = reader.readUnsignedInt();
                final long dataLength = reader.readUnsignedInt();
                if (EMJI_TAG == tag || EMJI_TAG_DEPRECATED == tag) {
                    return new OffsetInfo(dataOffset + metaOffset, dataLength);
                }
            }
        }

        throw new IOException("Cannot read metadata.");
    }

    /**
     * Start offset and length of the emoji metadata in the font.
     */
    private static class OffsetInfo {
        private final long mStartOffset;
        private final long mLength;

        OffsetInfo(long startOffset, long length) {
            mStartOffset = startOffset;
            mLength = length;
        }

        long getStartOffset() {
            return mStartOffset;
        }

        long getLength() {
            return mLength;
        }
    }

    private static int toUnsignedShort(final short value) {
        return value & 0xFFFF;
    }

    private static long toUnsignedInt(final int value) {
        return value & 0xFFFFFFFFL;
    }

    private interface OpenTypeReader {
        int UINT16_BYTE_COUNT = 2;
        int UINT32_BYTE_COUNT = 4;

        /**
         * Reads an {@code OpenType uint16}.
         *
         * @throws IOException
         */
        int readUnsignedShort() throws IOException;

        /**
         * Reads an {@code OpenType uint32}.
         *
         * @throws IOException
         */
        long readUnsignedInt() throws IOException;

        /**
         * Reads an {@code OpenType Tag}.
         *
         * @throws IOException
         */
        int readTag() throws IOException;

        /**
         * Skip the given amount of numOfBytes
         *
         * @throws IOException
         */
        void skip(int numOfBytes) throws IOException;

        /**
         * @return the position of the reader
         */
        long getPosition();
    }

    /**
     * Reads {@code OpenType} data from an {@link InputStream}.
     */
    private static class InputStreamOpenTypeReader implements OpenTypeReader {

        private final byte[] mByteArray;
        private final ByteBuffer mByteBuffer;
        private final InputStream mInputStream;
        private long mPosition = 0;

        /**
         * Constructs the reader with the given InputStream. Does not close the InputStream, it is
         * caller's responsibility to close it.
         *
         * @param inputStream InputStream to read from
         */
        InputStreamOpenTypeReader(final InputStream inputStream) {
            mInputStream = inputStream;
            mByteArray = new byte[UINT32_BYTE_COUNT];
            mByteBuffer = ByteBuffer.wrap(mByteArray);
            mByteBuffer.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public int readUnsignedShort() throws IOException {
            mByteBuffer.position(0);
            read(UINT16_BYTE_COUNT);
            return toUnsignedShort(mByteBuffer.getShort());
        }

        @Override
        public long readUnsignedInt() throws IOException {
            mByteBuffer.position(0);
            read(UINT32_BYTE_COUNT);
            return toUnsignedInt(mByteBuffer.getInt());
        }

        @Override
        public int readTag() throws IOException {
            mByteBuffer.position(0);
            read(UINT32_BYTE_COUNT);
            return mByteBuffer.getInt();
        }

        @Override
        public void skip(int numOfBytes) throws IOException {
            while (numOfBytes > 0) {
                long skipped = mInputStream.skip(numOfBytes);
                if (skipped < 1) {
                    throw new IOException("Skip didn't move at least 1 byte forward");
                }
                numOfBytes -= skipped;
                mPosition += skipped;
            }
        }

        @Override
        public long getPosition() {
            return mPosition;
        }

        private void read(@IntRange(from = 0, to = UINT32_BYTE_COUNT) final int numOfBytes)
                throws IOException {
            if (mInputStream.read(mByteArray, 0, numOfBytes) != numOfBytes) {
                throw new IOException("read failed");
            }
            mPosition += numOfBytes;
        }
    }

    /**
     * Reads OpenType data from a ByteBuffer.
     */
    private static class ByteBufferReader implements OpenTypeReader {

        private final ByteBuffer mByteBuffer;

        /**
         * Constructs the reader with the given ByteBuffer.
         *
         * @param byteBuffer ByteBuffer to read from
         */
        ByteBufferReader(final ByteBuffer byteBuffer) {
            mByteBuffer = byteBuffer;
            mByteBuffer.order(ByteOrder.BIG_ENDIAN);
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return toUnsignedShort(mByteBuffer.getShort());
        }

        @Override
        public long readUnsignedInt() throws IOException {
            return toUnsignedInt(mByteBuffer.getInt());
        }

        @Override
        public int readTag() throws IOException {
            return mByteBuffer.getInt();
        }

        @Override
        public void skip(final int numOfBytes) throws IOException {
            mByteBuffer.position(mByteBuffer.position() + numOfBytes);
        }

        @Override
        public long getPosition() {
            return mByteBuffer.position();
        }
    }
}
