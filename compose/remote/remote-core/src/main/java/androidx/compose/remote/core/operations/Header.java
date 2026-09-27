/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static androidx.compose.remote.core.CoreDocument.MAJOR_VERSION;
import static androidx.compose.remote.core.CoreDocument.MINOR_VERSION;
import static androidx.compose.remote.core.CoreDocument.PATCH_VERSION;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.LONG;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteComposeOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.IntMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Describe some basic information for a RemoteCompose document
 *
 * <p>It encodes the version of the document (following semantic versioning) as well as the
 * dimensions of the document in pixels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Header extends Operation implements RemoteComposeOperation, VariableSupport {
    private static final int OP_CODE = Operations.HEADER;
    private static final String CLASS_NAME = "Header";
    private static final int MAGIC_NUMBER = 0x048C0000; // to uniquely identify the protocol

    int mMajorVersion;
    int mMinorVersion;
    int mPatchVersion;

    int mWidth = 256;
    int mHeight = 256;

    float mDensity = 1;
    long mCapabilities = 0;
    int mProfiles = 0;
    private @Nullable IntMap<Object> mProperties;

    /**
     * Get a property on the header
     *
     * @param property the property to get
     * @return the value of the property
     */
    public @Nullable Object get(short property) {
        if (mProperties == null) {
            return null;
        }
        return mProperties.get(property);
    }

    /** the width of the document */
    public static final short DOC_WIDTH = 5;

    /** The height of the document */
    public static final short DOC_HEIGHT = 6;

    /** The density at generation */
    public static final short DOC_DENSITY_AT_GENERATION = 7;

    /** The desired FPS for the document */
    public static final short DOC_DESIRED_FPS = 8;

    /** The description of the contents of the document */
    public static final short DOC_CONTENT_DESCRIPTION = 9;

    /** The source of the document */
    public static final short DOC_SOURCE = 11;

    /** The document is an update to the existing document */
    public static final short DOC_DATA_UPDATE = 12;

    /** integer host action id to call if exception occurs */
    public static final short HOST_EXCEPTION_HANDLER = 13;

    /** profiles */
    public static final short DOC_PROFILES = 14;

    /** direct measure in paint, instead of wrap behavior */
    public static final short FEATURE_PAINT_MEASURE = 15;

    /** Direct player to be verbose levels= 0=off,1,2,3 */
    public static final short DEBUG = 16;

    /** Specify measure implementation version */
    public static final short FEATURE_MEASURE_VERSION = 17;

    /** Specify touch implementation version */
    public static final short FEATURE_TOUCH_VERSION = 18;

    /** Test capture at time in ms since epoch */
    public static final short TEST_TIME = 19;

    /** Test capture after this seconds float time in seconds */
    public static final short TEST_AFTER = 20;

    /** Simulate color theme "color.system_accent1_100 = 2324323, system.textColor = 32323" */
    public static final short TEST_COLOR_THEME = 21;

    /** Support for actions like "DOWN(time,x,y), MOVE(duration,x,y), UP(x,y)" */
    public static final short TEST_ACTIONS = 22;

    /** Fix priority logic in collapsible layouts */
    public static final short FEATURE_PRIORITY_FIX = 23;

    /** Support for origin-aware resizing animations */
    public static final short FEATURE_LT_RESIZE = 24;

    /** Enable listener pattern for arrays in TextLookup */
    public static final short FEATURE_ARRAY_LISTENERS = 25;

    /**
     * Modify click behavior The default is support for single click, double-click and long press,
     * setting FEATURE_CLICK_VERSION to 1 will only support single click.
     */
    public static final short FEATURE_CLICK_VERSION = 26;

    /**
     * Density behavior for the document. 0: Current behavior (mixed) 1: Values are interpreted as
     * pixels, no density applied by default 2: Values are interpreted as dp, density applied by
     * default
     */
    public static final short DOC_DENSITY_BEHAVIOR = 27;

    /** Specify layout optimization level: 0 = none, 1 = partial, 2 = all */
    public static final short FEATURE_OPTIMIZATION_LEVEL = 28;

    /**
     * Controls the usage of API ViewParent#requestDisallowInterceptTouchEvent when handling gesture
     * propagation between RemoteCompose and host views. 0: disabled, 1: enabled.
     */
    public static final short FEATURE_DISALLOW_INTERCEPT_TOUCH = 29;

    /**
     * Controls whether CanvasOperations within LayoutComponents are evaluated during the DATA pass.
     * 0: disabled, 1: enabled.
     */
    public static final short FEATURE_DATA_PASS_CANVAS_OPS = 30;

    /**
     * Compression of everything that follows the header: {@link #COMPRESSION_NONE} (or absent) for
     * none, {@link #COMPRESSION_DEFLATE} for a zlib stream. The header itself is never compressed,
     * so a compressed document can still be peeked with {@link #readDirect(InputStream)}. See
     * {@link #compressDocument} and {@link #decompressDocument}.
     */
    public static final short COMPRESS = 31;

    /** {@link #COMPRESS} value: the operations are stored as is. */
    public static final int COMPRESSION_NONE = 0;

    /** {@link #COMPRESS} value: the operations are a zlib (RFC 1950) DEFLATE stream. */
    public static final int COMPRESSION_DEFLATE = 1;

    /** The object is an integer */
    private static final short DATA_TYPE_INT = 0;

    /** The object is an float */
    private static final short DATA_TYPE_FLOAT = 1;

    /** The object is an LONG */
    private static final short DATA_TYPE_LONG = 2;

    /** The object is an UTF-8 encoded string */
    private static final short DATA_TYPE_STRING = 3;

    private static final short[] KEYS = {
        DOC_WIDTH,
        DOC_HEIGHT,
        DOC_DENSITY_AT_GENERATION,
        DOC_DESIRED_FPS,
        DOC_CONTENT_DESCRIPTION,
        DOC_SOURCE,
        DOC_DATA_UPDATE,
        HOST_EXCEPTION_HANDLER,
        DOC_PROFILES,
        FEATURE_PAINT_MEASURE,
        DEBUG,
        FEATURE_MEASURE_VERSION,
        FEATURE_TOUCH_VERSION,
        FEATURE_PRIORITY_FIX,
        FEATURE_LT_RESIZE,
        FEATURE_ARRAY_LISTENERS,
        FEATURE_CLICK_VERSION,
        DOC_DENSITY_BEHAVIOR,
        FEATURE_OPTIMIZATION_LEVEL,
        FEATURE_DISALLOW_INTERCEPT_TOUCH,
        FEATURE_DATA_PASS_CANVAS_OPS,
        COMPRESS,
    };
    private static final String[] KEY_NAMES = {
        "DOC_WIDTH",
        "DOC_HEIGHT",
        "DOC_DENSITY_AT_GENERATION",
        "DOC_DESIRED_FPS",
        "DOC_CONTENT_DESCRIPTION",
        "DOC_SOURCE",
        "DOC_DATA_UPDATE",
        "HOST_EXCEPTION_HANDLER",
        "DOC_PROFILES",
        "PAINT_MEASURE",
        "DEBUG",
        "MEASURE_VERSION",
        "TOUCH_VERSION",
        "PRIORITY_FIX",
        "LT_RESIZE",
        "ARRAY_LISTENERS",
        "CLICK_VERSION",
        "DENSITY_BEHAVIOR",
        "OPTIMIZATION_LEVEL",
        "DISALLOW_INTERCEPT_TOUCH",
        "DATA_PASS_CANVAS_OPS",
        "COMPRESS"
    };

    /** Offset of the property count in a properties header: opcode, then major, minor, patch. */
    private static final int PROPERTY_COUNT_OFFSET = 1 + 3 * 4;

    /** Offset of the first property in a properties header. */
    private static final int PROPERTIES_OFFSET = PROPERTY_COUNT_OFFSET + 4;

    /** Size of an INT property: tag, value length, value. */
    private static final int INT_PROPERTY_SIZE = 2 + 2 + 4;

    /** Size of the chunks used to deflate and inflate documents. */
    private static final int CHUNK_SIZE = 8 * 1024;

    /**
     * It encodes the version of the document (following semantic versioning) as well as the
     * dimensions of the document in pixels.
     *
     * @param majorVersion the major version of the RemoteCompose document API
     * @param minorVersion the minor version of the RemoteCompose document API
     * @param patchVersion the patch version of the RemoteCompose document API
     * @param width the width of the RemoteCompose document
     * @param height the height of the RemoteCompose document
     * @param density the density at which the document was originally created
     * @param capabilities bitmask field storing needed capabilities (unused for now)
     */
    public Header(
            int majorVersion,
            int minorVersion,
            int patchVersion,
            int width,
            int height,
            float density,
            long capabilities) {
        this.mMajorVersion = majorVersion;
        this.mMinorVersion = minorVersion;
        this.mPatchVersion = patchVersion;
        this.mWidth = width;
        this.mHeight = height;
        this.mDensity = density;
        this.mCapabilities = capabilities;
    }

    /**
     * @param majorVersion the major version of the RemoteCompose document API
     * @param minorVersion the minor version of the RemoteCompose document API
     * @param patchVersion the patch version of the RemoteCompose document API
     * @param properties the properties of the document
     */
    public Header(
            int majorVersion,
            int minorVersion,
            int patchVersion,
            @Nullable IntMap<Object> properties) {
        this.mMajorVersion = majorVersion;
        this.mMinorVersion = minorVersion;
        this.mPatchVersion = patchVersion;
        if (properties != null) {
            this.mProperties = properties;
            this.mWidth = getInt(DOC_WIDTH, 256);
            this.mHeight = getInt(DOC_HEIGHT, 256);
            this.mDensity = getFloat(DOC_DENSITY_AT_GENERATION, 1);
            this.mProfiles = getInt(DOC_PROFILES, 0);
        }
    }

    /** Put a feature or property into the header. */
    public void put(short key, @NonNull Object value) {
        if (mProperties == null) {
            mProperties = new IntMap<>();
        }
        mProperties.put(key, value);
    }

    public int getProfiles() {
        return mProfiles;
    }

    public float getDensity() {
        return mDensity;
    }

    /** Check for a property on the header */
    public int getInt(int key, int defaultValue) {
        if (mProperties == null) {
            return defaultValue;
        }
        Integer value = (Integer) mProperties.get(key);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    // private long getLong(int key, long defaultValue) {
    //    if (mProperties == null) {
    //         return defaultValue;
    //     }
    //     Long value = (Long) mProperties.get(key);
    //     if (value != null) {
    //         return value;
    //     } else {
    //         return defaultValue;
    //     }
    // }

    private float getFloat(int key, float defaultValue) {
        if (mProperties == null) {
            return defaultValue;
        }
        Float value = (Float) mProperties.get(key);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    // private String getString(int key, String defaultValue) {
    //     if (mProperties == null) {
    //         return defaultValue;
    //     }
    //     String value = (String) mProperties.get(key);
    //     if (value != null) {
    //         return value;
    //     } else {
    //         return defaultValue;
    //     }
    // }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        if (mProperties != null && mProperties.size() > 0) {
            int size = mProperties.size();
            short[] types = new short[size];
            Object[] values = new Object[size];
            List<Integer> keys = new ArrayList<>(mProperties.keySet());
            Collections.sort(keys); // Sort for deterministic output
            int i = 0;
            for (Integer key : keys) {
                types[i] = key.shortValue();
                values[i] = mProperties.get(key);
                i++;
            }
            int apiLevel = versionToApiLevel(MAJOR_VERSION, MINOR_VERSION);
            if (apiLevel < 7) {
                throw new IllegalStateException(
                        "Header has properties but apiLevel is "
                                + apiLevel
                                + " which is less than 7");
            }
            apply(buffer, apiLevel, types, values);
        } else {
            apply(buffer, mWidth, mHeight, mDensity, mCapabilities);
        }
    }

    @NonNull
    @Override
    public String toString() {
        String prop = "";
        if (mProperties != null) {
            for (int i = 0; i < KEYS.length; i++) {
                Object p = mProperties.get(KEYS[i]);
                if (p != null) {
                    prop += "\n  " + KEY_NAMES[i] + " " + p.toString();
                }
            }
            return "HEADER v" + mMajorVersion + "." + mMinorVersion + "." + mPatchVersion + prop;
        }
        return "HEADER v"
                + mMajorVersion
                + "."
                + mMinorVersion
                + "."
                + mPatchVersion
                + ", "
                + mWidth
                + " x "
                + mHeight
                + " ["
                + mCapabilities
                + "]"
                + prop;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (context.getDensityBehavior() == CoreDocument.DENSITY_BEHAVIOR_DP) {
            context.listensTo(RemoteContext.ID_DENSITY, this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {}

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.header(
                mMajorVersion,
                mMinorVersion,
                mPatchVersion,
                mWidth,
                mHeight,
                mCapabilities,
                mProperties);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return toString();
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

    /** Apply flat header to the wire buffer */
    public static void apply(
            @NonNull WireBuffer buffer, int width, int height, float density, long capabilities) {
        buffer.start(OP_CODE);
        buffer.writeInt(MAJOR_VERSION); // major version number of the protocol
        buffer.writeInt(MINOR_VERSION); // minor version number of the protocol
        buffer.writeInt(PATCH_VERSION); // patch version number of the protocol
        buffer.writeInt(width);
        buffer.writeInt(height);
        // buffer.writeFloat(density); TODO fix or remove
        buffer.writeLong(capabilities);
    }

    /** Apply map-based header (supports properties) to the wire buffer */
    public static void apply(
            @NonNull WireBuffer buffer,
            int apiLevel,
            short @NonNull [] type,
            Object @NonNull [] value) {
        buffer.start(OP_CODE);
        if (apiLevel >= 7) {
            buffer.writeInt(MAJOR_VERSION | MAGIC_NUMBER); // major version number of the protocol
            buffer.writeInt(MINOR_VERSION); // minor version number of the protocol
            buffer.writeInt(PATCH_VERSION); // patch version number of the protocol
            buffer.writeInt(type.length);
            writeMap(buffer, type, value);
        } else if (apiLevel == 6) {
            buffer.writeInt(1); // major version number of the protocol
            buffer.writeInt(0); // minor version number of the protocol
            buffer.writeInt(0); // patch version number of the protocol
            int width = getInt(type, value, DOC_WIDTH);
            int height = getInt(type, value, DOC_HEIGHT);
            buffer.writeInt(width);
            buffer.writeInt(height);
            buffer.writeLong(0L);
        } else {
            throw new RuntimeException("Unsupported API level " + apiLevel);
        }
    }

    private static int getInt(short[] type, Object[] value, int key) {
        for (int i = 0; i < type.length; i++) {
            if (type[i] == key) {
                if (value[i] instanceof Integer) {
                    return (Integer) value[i];
                }
                return 0;
            }
        }
        return 0;
    }

    /**
     * @param is the stream to read from
     * @return the header
     * @throws IOException if there is an error reading the header
     */
    public static @NonNull Header readDirect(@NonNull InputStream is) throws IOException {
        DataInputStream stream = new DataInputStream(is);
        try {

            int type = stream.readByte();

            if (type != OP_CODE) {
                throw new IOException("Invalid header " + type + " != " + OP_CODE);
            }
            int majorVersion = stream.readInt();
            int minorVersion = stream.readInt();
            int patchVersion = stream.readInt();

            if (majorVersion < 0x10000) {
                int width = stream.readInt();
                int height = stream.readInt();
                // float density = is.read();
                float density = 1f;
                long capabilities = stream.readLong();
                return new Header(
                        majorVersion,
                        minorVersion,
                        patchVersion,
                        width,
                        height,
                        density,
                        capabilities);
            }

            if ((majorVersion & 0xFFFF0000) != MAGIC_NUMBER) {
                throw new IOException(
                        "Invalid header MAGIC_NUMBER "
                                + (majorVersion & 0xFFFF0000)
                                + " != "
                                + MAGIC_NUMBER);
            }
            majorVersion &= 0xFFFF;
            int len = stream.readInt();
            if (len < 0 || len > Limits.MAX_TABLE_SIZE) {
                throw new IOException("Invalid table size " + len);
            }
            short[] types = new short[len];
            Object[] values = new Object[len];
            readMap(stream, types, values);
            IntMap<Object> map = new IntMap<>();
            for (int i = 0; i < len; i++) {
                map.put(types[i], values[i]);
            }
            return new Header(majorVersion, minorVersion, patchVersion, map);

        } finally {
            stream.close();
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param stream the buffer to read
     * @param types the list of types that will be populated
     * @param values the list of values that will be populated
     */
    private static void readMap(DataInputStream stream, short[] types, Object[] values)
            throws IOException {
        for (int i = 0; i < types.length; i++) {
            short tag = (short) stream.readShort();
            stream.readShort(); // itemLen
            int dataType = tag >> 10;
            types[i] = (short) (tag & 0x3F);
            switch (dataType) {
                case DATA_TYPE_INT:
                    values[i] = stream.readInt();
                    break;
                case DATA_TYPE_FLOAT:
                    values[i] = stream.readFloat();
                    break;
                case DATA_TYPE_LONG:
                    values[i] = stream.readLong();
                    break;
                case DATA_TYPE_STRING:
                    int slen = stream.readInt();
                    if (slen < 0 || slen > Limits.MAX_STRING_SIZE) {
                        throw new IOException("String length exceeds limit: " + slen);
                    }
                    byte[] data = new byte[slen];
                    stream.readFully(data);
                    values[i] = new String(data);
                    break;
            }
        }
    }

    /**
     * Map major and minor version to API level.
     *
     * @param majorVersion Major version
     * @param minorVersion Minor version
     * @return API level, -1 if unknown
     */
    private static int versionToApiLevel(int majorVersion, int minorVersion) {
        if (majorVersion >= 0x10000) {
            if ((majorVersion & 0xFFFF0000) != MAGIC_NUMBER) {
                return -1;
            }
            majorVersion &= 0xFFFF;
        }
        if (majorVersion == 1 && minorVersion == 2) {
            return 8;
        }
        if (majorVersion == 1 && minorVersion == 1) {
            return 7;
        }
        if (majorVersion == 1 && minorVersion == 0) {
            return 6;
        }
        if (majorVersion == 0 && (minorVersion <= 3)) {
            // Cts tests
            return 6;
        }
        if (majorVersion == 0 && minorVersion == 0) {
            // RemoteFloat tests
            return 6;
        }
        return -1;
    }

    /**
     * Peeks and returns the Header api level
     *
     * @return api level, -1 if not found
     */
    public static int peekApiLevel(@NonNull WireBuffer buffer) {
        if (buffer.getIndex() != 0) {
            throw new IllegalStateException(
                    "Invalid buffer reading position; can't read the header");
        }
        int headerOpId = buffer.readByte();
        if (headerOpId != Operations.HEADER) {
            buffer.setIndex(0);
            return -1;
        }
        int majorVersion = buffer.readInt();
        int minorVersion = buffer.readInt();
        buffer.setIndex(0);
        return versionToApiLevel(majorVersion, minorVersion);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int majorVersion = buffer.readInt();
        int minorVersion = buffer.readInt();
        int patchVersion = buffer.readInt();
        if (majorVersion < 0x10000) {
            int width = buffer.readInt();
            int height = buffer.readInt();
            // float density = buffer.readFloat();
            float density = 1f;
            long capabilities = buffer.readLong();
            Header header =
                    new Header(
                            majorVersion,
                            minorVersion,
                            patchVersion,
                            width,
                            height,
                            density,
                            capabilities);
            operations.add(header);
        } else {
            majorVersion &= 0xFFFF;
            int length = buffer.readInt();
            if (length < 0 || length > Limits.MAX_TABLE_SIZE) {
                throw new RuntimeException("Invalid table size " + length);
            }
            short[] types = new short[length];
            Object[] values = new Object[length];
            readMap(buffer, types, values);
            IntMap<Object> map = new IntMap<>();
            for (int i = 0; i < length; i++) {
                map.put(types[i], values[i]);
            }
            Header header = new Header(majorVersion, minorVersion, patchVersion, map);
            operations.add(header);
        }
    }

    /**
     * @param buffer the stream to read from
     * @return the header
     * @throws IOException if there is an error reading the header
     */
    public static @NonNull Header readDirect(@NonNull WireBuffer buffer) throws IOException {
        if (buffer.getIndex() != 0) {
            throw new IllegalStateException(
                    "Invalid buffer reading position; can't read the header.");
        }
        int type = buffer.readByte();

        if (type != OP_CODE) {
            throw new IOException("Invalid header " + type + " != " + OP_CODE);
        }
        int majorVersion = buffer.readInt();
        int minorVersion = buffer.readInt();
        int patchVersion = buffer.readInt();

        if (majorVersion < 0x10000) {
            int width = buffer.readInt();
            int height = buffer.readInt();
            // float density = is.read();
            float density = 1f;
            long capabilities = buffer.readLong();
            return new Header(
                    majorVersion, minorVersion, patchVersion, width, height, density, capabilities);
        }

        if ((majorVersion & 0xFFFF0000) != MAGIC_NUMBER) {
            throw new IOException(
                    "Invalid header MAGIC_NUMBER "
                            + (majorVersion & 0xFFFF0000)
                            + " != "
                            + MAGIC_NUMBER);
        }
        majorVersion &= 0xFFFF;
        int len = buffer.readInt();
        if (len < 0 || len > Limits.MAX_TABLE_SIZE) {
            throw new IOException("Invalid table size " + len);
        }
        short[] types = new short[len];
        Object[] values = new Object[len];
        readMap(buffer, types, values);
        IntMap<Object> map = new IntMap<>();
        for (int i = 0; i < len; i++) {
            map.put(types[i], values[i]);
        }
        return new Header(majorVersion, minorVersion, patchVersion, map);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param types the list of types that will be populated
     * @param values the list of values that will be populated
     */
    private static void readMap(@NonNull WireBuffer buffer, short[] types, Object[] values) {
        for (int i = 0; i < types.length; i++) {
            short tag = (short) buffer.readShort();
            buffer.readShort(); // itemLen
            int dataType = tag >> 10;
            types[i] = (short) (tag & 0x3F);
            switch (dataType) {
                case DATA_TYPE_INT:
                    values[i] = buffer.readInt();
                    break;
                case DATA_TYPE_FLOAT:
                    values[i] = buffer.readFloat();
                    break;
                case DATA_TYPE_LONG:
                    values[i] = buffer.readLong();
                    break;
                case DATA_TYPE_STRING:
                    values[i] = buffer.readUTF8(Limits.MAX_STRING_SIZE);
                    break;
            }
        }
    }

    /**
     * Write the map of values to the buffer
     *
     * @param buffer the buffer to read
     * @param types the list of types that will be written
     * @param values the list of values that will be written
     */
    private static void writeMap(@NonNull WireBuffer buffer, short[] types, Object[] values) {
        for (int i = 0; i < types.length; i++) {
            short tag = types[i];
            if (values[i] instanceof String) {
                tag = (short) (tag | (DATA_TYPE_STRING << 10));
                buffer.writeShort(tag);
                String str = (String) values[i];
                byte[] data = str.getBytes();
                buffer.writeShort((data.length + 4));
                buffer.writeBuffer(data);
            } else if (values[i] instanceof Integer) {
                tag = (short) (tag | (DATA_TYPE_INT << 10));
                buffer.writeShort(tag);
                buffer.writeShort(4);
                buffer.writeInt((Integer) values[i]);
            } else if (values[i] instanceof Float) {
                tag = (short) (tag | (DATA_TYPE_FLOAT << 10));
                buffer.writeShort(tag);
                buffer.writeShort(4);

                buffer.writeFloat((float) values[i]);
            } else if (values[i] instanceof Long) {
                tag = (short) (tag | (DATA_TYPE_LONG << 10));
                buffer.writeShort(tag);
                buffer.writeShort(8);
                buffer.writeLong((Long) values[i]);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported property type: "
                                + (values[i] == null ? "null" : values[i].getClass().getName()));
            }
        }
    }

    /**
     * Compresses a document: everything after the header becomes a zlib stream, and a {@link
     * #COMPRESS} property is appended to the header. The rest of the header is copied as is, which
     * keeps the document's version, and stays uncompressed so {@link #readDirect(InputStream)} can
     * still peek at it. Players decompress transparently while inflating the document.
     *
     * @param document an uncompressed document with a properties (API level 7+) header
     * @param size the number of valid bytes in {@code document}
     * @return a new array holding the compressed document, or a copy of {@code document} if it is
     *     already compressed
     * @throws IllegalArgumentException if the header is invalid or legacy (without properties)
     */
    public static byte @NonNull [] compressDocument(byte @NonNull [] document, int size) {
        ByteArrayInputStream stream = new ByteArrayInputStream(document, 0, size);
        try {
            Header header = readDirect(stream);
            int headerSize = size - stream.available();
            if (header.mProperties == null) {
                throw new IllegalArgumentException(
                        "Compression needs a header with properties (API level 7+)");
            }
            Object compression = header.get(COMPRESS);
            if (compression != null && !Integer.valueOf(COMPRESSION_NONE).equals(compression)) {
                return Arrays.copyOf(document, size);
            }
            byte[] head = new byte[headerSize + INT_PROPERTY_SIZE];
            int headSize = copyHeaderWithoutCompress(document, headerSize, head, 1);
            ByteBuffer.wrap(head, headSize, INT_PROPERTY_SIZE)
                    .putShort((short) (COMPRESS | (DATA_TYPE_INT << 10)))
                    .putShort((short) 4)
                    .putInt(COMPRESSION_DEFLATE);
            ByteArrayOutputStream out = new ByteArrayOutputStream(size / 2 + INT_PROPERTY_SIZE);
            out.write(head, 0, headSize + INT_PROPERTY_SIZE);
            Deflater deflater = new Deflater();
            try {
                deflater.setInput(document, headerSize, size - headerSize);
                deflater.finish();
                byte[] chunk = new byte[CHUNK_SIZE];
                while (!deflater.finished()) {
                    out.write(chunk, 0, deflater.deflate(chunk));
                }
            } finally {
                deflater.end();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid document header", e);
        }
    }

    /**
     * Decompresses a document made by {@link #compressDocument}: inflates everything after the
     * header and drops the {@link #COMPRESS} property, restoring the original document.
     *
     * @param document a document, compressed or not
     * @param size the number of valid bytes in {@code document}
     * @return a new array holding the uncompressed document, or a copy of {@code document} if it
     *     isn't compressed
     * @throws IOException if the header is invalid or uses an unsupported compression, or if the
     *     compressed data is corrupted, truncated, followed by extra bytes, or inflates to more
     *     than {@link Limits#MAX_DECOMPRESSED_SIZE} bytes
     */
    public static byte @NonNull [] decompressDocument(byte @NonNull [] document, int size)
            throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(document, 0, size);
        Header header = readDirect(stream);
        int headerSize = size - stream.available();
        Object compression = header.get(COMPRESS);
        if (compression == null || Integer.valueOf(COMPRESSION_NONE).equals(compression)) {
            return Arrays.copyOf(document, size);
        }
        if (!Integer.valueOf(COMPRESSION_DEFLATE).equals(compression)) {
            throw new IOException("Unsupported document compression " + compression);
        }
        byte[] head = new byte[headerSize];
        int headSize = copyHeaderWithoutCompress(document, headerSize, head, 0);
        ByteArrayOutputStream out =
                new ByteArrayOutputStream((int) Math.min(4L * size, Limits.BUFFER_SIZE));
        out.write(head, 0, headSize);
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(document, headerSize, size - headerSize);
            byte[] chunk = new byte[CHUNK_SIZE];
            long inflated = 0;
            while (!inflater.finished()) {
                int count = inflater.inflate(chunk);
                if (count == 0 && !inflater.finished()) {
                    throw new IOException(
                            inflater.needsInput()
                                    ? "Truncated compressed document"
                                    : "Invalid compressed document");
                }
                inflated += count;
                if (inflated > Limits.MAX_DECOMPRESSED_SIZE) {
                    throw new IOException(
                            "Decompressed document exceeds "
                                    + Limits.MAX_DECOMPRESSED_SIZE
                                    + " bytes");
                }
                out.write(chunk, 0, count);
            }
            if (inflater.getRemaining() > 0) {
                throw new IOException("Unexpected data after the compressed document");
            }
        } catch (DataFormatException e) {
            throw new IOException("Corrupted compressed document", e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    /**
     * Copies the properties header that starts {@code document} into {@code out}, without its
     * {@link #COMPRESS} properties and with {@code extraProperties} added to the property count,
     * and returns the number of bytes written. Properties are walked using their stored length, and
     * must end exactly where {@link #readDirect} found the end of the header.
     */
    private static int copyHeaderWithoutCompress(
            byte @NonNull [] document, int headerSize, byte @NonNull [] out, int extraProperties)
            throws IOException {
        ByteBuffer in = ByteBuffer.wrap(document, 0, headerSize);
        int count = in.getInt(PROPERTY_COUNT_OFFSET);
        System.arraycopy(document, 0, out, 0, PROPERTY_COUNT_OFFSET);
        int read = PROPERTIES_OFFSET;
        int written = PROPERTIES_OFFSET;
        int kept = 0;
        int i = 0;
        while (i < count && read + 4 <= headerSize) {
            int end = read + 4 + (in.getShort(read + 2) & 0xFFFF);
            if (end > headerSize) {
                break;
            }
            if ((in.getShort(read) & 0x3F) != COMPRESS) {
                System.arraycopy(document, read, out, written, end - read);
                written += end - read;
                kept++;
            }
            read = end;
            i++;
        }
        if (i != count || read != headerSize) {
            throw new IOException("Malformed header properties");
        }
        ByteBuffer.wrap(out).putInt(PROPERTY_COUNT_OFFSET, kept + extraProperties);
        return written;
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Document Protocol Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Document metadata, containing the version,"
                                + " original size & density, capabilities mask")
                .field(INT, "majorVersion", "Major version")
                .field(INT, "minorVersion", "Minor version")
                .field(INT, "patchVersion", "Patch version")
                .field(INT, "width", "Document width in pixels")
                .field(INT, "height", "Document height in pixels")
                .field(LONG, "capabilities", "Capabilities mask");
    }

    /** Set the version on a document */
    public void setVersion(@NonNull CoreDocument document) {
        document.setHostExceptionID(getInt(HOST_EXCEPTION_HANDLER, 0));
        document.setUpdateDoc(getInt(DOC_DATA_UPDATE, 0) != 0);
        document.setVersion(mMajorVersion, mMinorVersion, mPatchVersion);
    }
}
