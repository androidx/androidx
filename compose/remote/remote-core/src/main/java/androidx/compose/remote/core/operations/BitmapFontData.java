/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT_ARRAY;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Operation to deal with bitmap font data. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapFontData extends Operation implements Serializable {
    private static final int OP_CODE = Operations.DATA_BITMAP_FONT;
    private static final String CLASS_NAME = "BitmapFontData";

    static final short VERSION_1 = 0;
    static final short VERSION_2 = 1; // Adds kerning table support.
    private static final int MAX_GLYPHS = 0xffff;
    private static final int MAX_KERNING_TABLE_SIZE = 0xffff;

    short mVersion; // O doesn't have a kerning table, 1 has a kerning table.
    int mId;

    // Sorted in order of decreasing mChars length.
    @NonNull Glyph[] mFontGlyphs;

    Map<String, Short> mKerningTable;

    /**
     * A bitmap font is comprised of a collection of Glyphs. Note each Glyph has its own bitmap
     * rather than using a texture atlas.
     */
    public static class Glyph {
        /** The character(s) this glyph represents. */
        public @Nullable String mChars;

        /** The id of the bitmap for this glyph, or -1 for space. */
        public int mBitmapId;

        /** The margin in pixels to the left of the glyph bitmap. */
        public short mMarginLeft;

        /** The margin in pixels above of the glyph bitmap. */
        public short mMarginTop;

        /** The margin in pixels to the right of the glyph bitmap. */
        public short mMarginRight;

        /** The margin in pixels below the glyph bitmap. */
        public short mMarginBottom;

        public short mBitmapWidth;
        public short mBitmapHeight;

        public Glyph() {}

        public Glyph(
                @NonNull String chars,
                int bitmapId,
                short marginLeft,
                short marginTop,
                short marginRight,
                short marginBottom,
                short width,
                short height) {
            mChars = chars;
            mBitmapId = bitmapId;
            mMarginLeft = marginLeft;
            mMarginTop = marginTop;
            mMarginRight = marginRight;
            mMarginBottom = marginBottom;
            mBitmapWidth = width;
            mBitmapHeight = height;
        }
    }

    /**
     * create a bitmap font structure.
     *
     * @param id the id of the bitmap font
     * @param fontGlyphs the glyphs that define the bitmap font. The maximum number of glyphs is
     *     65535.
     */
    public BitmapFontData(int id, @NonNull Glyph[] fontGlyphs) {
        mId = id;
        mFontGlyphs = fontGlyphs;
        mVersion = VERSION_1;
        mKerningTable = new HashMap<String, Short>();

        if (fontGlyphs.length >= MAX_GLYPHS) {
            throw new IllegalArgumentException("Too many glyphs, the maximum is " + MAX_GLYPHS);
        }

        // Sort in order of decreasing mChars length.
        Arrays.sort(mFontGlyphs, (o1, o2) -> o2.mChars.length() - o1.mChars.length());
    }

    /**
     * create a bitmap font structure.
     *
     * @param id the id of the bitmap font
     * @param fontGlyphs the glyphs that define the bitmap font. The maximum number of glyphs is
     *     65535.
     * @param version the Version number. 0 = no kerning table, 1 = has kerning table
     * @param kerningTable The kerning table, where the key is pairs of glyphs (literally $1$2) and
     *     the value is the horizontal adjustment in pixels for that glyph pair. Can be empty. The
     *     maximum size of the kerning table is 65535 entries.
     */
    public BitmapFontData(
            int id,
            @NonNull Glyph[] fontGlyphs,
            short version,
            @NonNull Map<String, Short> kerningTable) {
        mId = id;
        mFontGlyphs = fontGlyphs;
        mVersion = version;
        mKerningTable = kerningTable;

        if (fontGlyphs.length >= MAX_GLYPHS) {
            throw new IllegalArgumentException("Too many glyphs, the maximum is " + MAX_GLYPHS);
        }

        if (kerningTable.size() >= MAX_GLYPHS) {
            throw new IllegalArgumentException(
                    "Kerning table too big, the maximum size is " + MAX_KERNING_TABLE_SIZE);
        }

        // Sort in order of decreasing mChars length.
        Arrays.sort(mFontGlyphs, (o1, o2) -> o2.mChars.length() - o1.mChars.length());
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mFontGlyphs, mKerningTable);
    }

    @NonNull
    @Override
    public String toString() {
        return "BITMAP FONT DATA " + mId;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Add the image to the document
     *
     * @param buffer document to write to
     * @param id the id the bitmap font will be stored under
     * @param glyphs glyph metadata
     * @param kerningTable The kerning table, where the key is pairs of glyphs (literally $1$2) and
     *     the value is the horizontal adjustment in pixels for that glyph pair. Can be empty.
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            @NonNull Glyph[] glyphs,
            @Nullable Map<String, Short> kerningTable) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);

        // Kerning tables are a V2 feature and we encode the version in the top 16 bits of the
        // glyph array length.  It's highly improbable we'll ever support a bitmap font with more
        // then 65535 glyphs (that would be very memory inefficient).
        if (kerningTable != null && !kerningTable.isEmpty()) {
            buffer.writeInt(glyphs.length + (((int) VERSION_2) << 16));
        } else {
            buffer.writeInt(glyphs.length);
        }

        for (Glyph element : glyphs) {
            buffer.writeUTF8(element.mChars);
            buffer.writeInt(element.mBitmapId);
            buffer.writeShort(element.mMarginLeft);
            buffer.writeShort(element.mMarginTop);
            buffer.writeShort(element.mMarginRight);
            buffer.writeShort(element.mMarginBottom);
            buffer.writeShort(element.mBitmapWidth);
            buffer.writeShort(element.mBitmapHeight);
        }

        if (kerningTable != null && !kerningTable.isEmpty()) {
            buffer.writeShort((short) kerningTable.size());
            for (Map.Entry<String, Short> pair : kerningTable.entrySet()) {
                buffer.writeUTF8(pair.getKey());
                buffer.writeShort(pair.getValue());
            }
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int versionAndNumGlyphElements = buffer.readInt();
        // The version is encoded in the top 16 bits to maintain backwards compatibility.
        short version = (short) (versionAndNumGlyphElements >>> 16);
        int numGlyphElements = versionAndNumGlyphElements & 0xffff;
        Glyph[] glyphs = new Glyph[numGlyphElements];
        for (int i = 0; i < numGlyphElements; i++) {
            glyphs[i] = new Glyph();
            glyphs[i].mChars = buffer.readUTF8();
            glyphs[i].mBitmapId = buffer.readInt();
            glyphs[i].mMarginLeft = (short) buffer.readShort();
            glyphs[i].mMarginTop = (short) buffer.readShort();
            glyphs[i].mMarginRight = (short) buffer.readShort();
            glyphs[i].mMarginBottom = (short) buffer.readShort();
            glyphs[i].mBitmapWidth = (short) buffer.readShort();
            glyphs[i].mBitmapHeight = (short) buffer.readShort();
        }

        Map<String, Short> kerningTable = new HashMap<>();

        if (version >= VERSION_2) {
            int numKerningTableEntries = (int) buffer.readShort();
            for (int i = 0; i < numKerningTableEntries; i++) {
                String glyphPair = buffer.readUTF8();
                Short adjustment = (short) buffer.readShort();
                kerningTable.put(glyphPair, adjustment);
            }
        }

        operations.add(new BitmapFontData(id, glyphs, version, kerningTable));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Bitmap font data")
                .field(DocumentedOperation.INT, "id", "id of bitmap font data")
                .field(INT_ARRAY, "glyphNodes", "list used to greedily convert strings into glyphs")
                .field(INT_ARRAY, "glyphElements", "");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mId, this);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    /** Finds the largest glyph matching the string at the specified offset, or returns null. */
    @Nullable
    public Glyph lookupGlyph(@NonNull String string, int offset) {
        // Since mFontGlyphs is sorted on decreasing size, it will match the longest items first.
        // It is expected that the mFontGlyphs array will be fairly small.
        for (Glyph glyph : mFontGlyphs) {
            if (string.startsWith(glyph.mChars, offset)) {
                return glyph;
            }
        }
        return null;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId);
    }
}
