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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteComposeOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.IntMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Describe some basic information for a RemoteCompose document
 *
 * <p>It encodes the version of the document (following semantic versioning) as well as the
 * dimensions of the document in pixels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Header extends Operation implements RemoteComposeOperation {
    private static final int OP_CODE = Operations.HEADER;
    private static final String CLASS_NAME = "Header";
    private static final int MAGIC_NUMBER = 0x048C0000; // to uniquely identify the protocol
    private static final int MAX_TABLE_SIZE = 1000;

    int mMajorVersion;
    int mMinorVersion;
    int mPatchVersion;

    int mWidth = 256;
    int mHeight = 256;

    float mDensity = 3;
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
        DOC_PROFILES
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
        "DOC_PROFILES"
    };

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
            this.mDensity = getFloat(DOC_DENSITY_AT_GENERATION, 0);
            this.mProfiles = getInt(DOC_PROFILES, 0);
        }
    }

    public int getProfiles() {
        return mProfiles;
    }

    private int getInt(int key, int defaultValue) {
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
        apply(buffer, mWidth, mHeight, mDensity, mCapabilities);
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

    /**
     * Apply the header to the wire buffer
     *
     * @param buffer
     * @param width
     * @param height
     * @param density
     * @param capabilities
     */
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

    /**
     * Apply the header to the wire buffer
     *
     * @param buffer
     * @param apiLevel
     * @param type
     * @param value
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int apiLevel,
            short @NonNull [] type,
            Object @NonNull [] value) {
        buffer.start(OP_CODE);
        if (apiLevel == CoreDocument.DOCUMENT_API_LEVEL) {
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
                    byte[] data = new byte[slen];
                    stream.readFully(data);
                    values[i] = new String(data);
                    break;
            }
        }
    }

    /**
     * Read the Header api level
     *
     * @param buffer
     * @return api level, -1 if not found
     */
    public static int readApiLevel(@NonNull WireBuffer buffer) {
        int index = buffer.getIndex();
        int headerOpId = buffer.readByte();
        if (headerOpId != Operations.HEADER) {
            return -1;
        }
        int majorVersion = buffer.readInt();
        int minorVersion = buffer.readInt();
        buffer.setIndex(index);
        if (majorVersion >= 0x10000) {
            if ((majorVersion & 0xFFFF0000) != MAGIC_NUMBER) {
                return -1;
            }
            majorVersion &= 0xFFFF;
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
            if (length > MAX_TABLE_SIZE) {
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
        int index = buffer.getIndex();
        try {

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
            int len = buffer.readInt();
            short[] types = new short[len];
            Object[] values = new Object[len];
            readMap(buffer, types, values);
            IntMap<Object> map = new IntMap<>();
            for (int i = 0; i < len; i++) {
                map.put(types[i], values[i]);
            }
            return new Header(majorVersion, minorVersion, patchVersion, map);

        } finally {
            buffer.setIndex(index);
        }
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
                    values[i] = buffer.readUTF8();
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
            }
        }
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Protocol Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Document metadata, containing the version,"
                                + " original size & density, capabilities mask")
                .field(INT, "MAJOR_VERSION", "Major version")
                .field(INT, "MINOR_VERSION", "Minor version")
                .field(INT, "PATCH_VERSION", "Patch version")
                .field(INT, "WIDTH", "Major version")
                .field(INT, "HEIGHT", "Major version")
                // .field(FLOAT, "DENSITY", "Major version")
                .field(LONG, "CAPABILITIES", "Major version");
    }

    /**
     * Set the version on a document
     *
     * @param document
     */
    public void setVersion(@NonNull CoreDocument document) {
        document.setHostExceptionID(getInt(HOST_EXCEPTION_HANDLER, 0));
        document.setUpdateDoc(getInt(DOC_DATA_UPDATE, 0) != 0);
        document.setVersion(mMajorVersion, mMinorVersion, mPatchVersion);
    }
}
