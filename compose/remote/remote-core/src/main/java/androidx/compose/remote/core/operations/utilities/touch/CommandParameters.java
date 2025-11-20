/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.core.operations.utilities.touch;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.WireBuffer;

import org.jspecify.annotations.NonNull;

/**
 * Utility to read a arbitrary parameter set in a command in a compact format
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CommandParameters {
    public static final byte P_INT = 1;
    public static final byte P_FLOAT = 2;
    public static final byte P_SHORT = 3;
    public static final byte P_BYTE = 4;
    public static final byte P_BOOLEAN = 5;
    public static final byte PA_INT = 6;
    public static final byte PA_FLOAT = 7;
    public static final byte PA_STRING = 8;

    Param[] mParams = new Param[256];

    /**
     * Create a CommandParameters processing utility for processing a set of parameters
     *
     * @param params the parameters to process
     */
    public CommandParameters(@NonNull Param... params) {
        for (Param param : params) {
            mParams[0xFF & param.mId] = param;
        }
    }

    /**
     * Create a parameter with the given name, id and type
     *
     * @param name the name of the parameter
     * @param id   the id of the parameter
     * @param type the type of the parameter
     */
    public static @NonNull Param param(@NonNull String name, byte id, byte type) {
        return new Param(name, id, type);
    }

    /**
     * A parameter
     */
    public static class Param {
        String mName;
        byte mId;
        byte mType;

        Param(@NonNull String name, byte id, byte type) {
            mName = name;
            mId = id;
            mType = type;
        }
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, int value) {
        Param param = mParams[0xFF & id];
        buffer.writeByte(param.mId);
        switch (param.mType) {
            case P_INT:
                buffer.writeInt(value);
                break;
            case P_FLOAT:
                buffer.writeFloat(Float.intBitsToFloat(value));
                break;
            case P_SHORT:
                buffer.writeShort(value);
                break;
            case P_BYTE:
                buffer.writeByte(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, float value) {
        Param param = mParams[0xFF & id];
        if (param.mType != P_FLOAT) {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
        buffer.writeByte(param.mId);

        buffer.writeFloat(value);
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, float @NonNull [] value) {
        Param param = mParams[0xFF & id];
        if (param.mType != PA_FLOAT) {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
        buffer.writeByte(param.mId);
        buffer.writeShort(value.length);
        for (float v : value) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, int @NonNull [] value) {
        Param param = mParams[0xFF & id];
        if (param.mType != PA_INT) {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
        buffer.writeByte(param.mId);
        buffer.writeShort(value.length);
        for (int v : value) {
            buffer.writeInt(v);
        }
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, @NonNull String value) {
        Param param = mParams[0xFF & id];
        if (param.mType != PA_STRING) {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
        buffer.writeByte(param.mId);
        buffer.writeUTF8(value);
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, byte value) {
        Param param = mParams[0xFF & id];
        buffer.writeByte(param.mId);
        if (param.mType == P_BYTE) {
            buffer.writeByte(value);
        } else {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
    }

    /**
     * Write a parameter to a buffer
     *
     * @param buffer the buffer to write to
     * @param id     the id of the parameter
     * @param value  the value of the parameter
     */
    public void write(@NonNull WireBuffer buffer, byte id, boolean value) {
        Param param = mParams[0xFF & id];
        buffer.writeByte(param.mId);
        if (param.mType == P_BOOLEAN) {
            buffer.writeBoolean(value);
        } else {
            throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
    }

    /**
     * Callback for reading parameters from a buffer
     */
    public interface Callback {
        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, int value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, float value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, short value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, byte value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, boolean value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, @NonNull String value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, int @NonNull [] value);

        /**
         * Called when a parameter is read from a buffer
         *
         * @param id    the id of the parameter
         * @param value the value of the parameter
         */
        void value(int id, float @NonNull [] value);
    }

    /**
     * Abstract callback for reading parameters from a buffer
     */
    public abstract static class AbstractCallback implements Callback {

        @Override
        public void value(int id, int value) {
        }

        @Override
        public void value(int id, float value) {
        }

        @Override
        public void value(int id, short value) {
        }

        @Override
        public void value(int id, byte value) {
        }

        @Override
        public void value(int id, boolean value) {
        }

        @Override
        public void value(int id, @NonNull String value) {
        }

        @Override
        public void value(int id, int @NonNull [] value) {
        }

        @Override
        public void value(int id, float @NonNull [] value) {

        }
    }

    /**
     * Read a parameter from a buffer
     *
     * @param buffer   the buffer to read from
     * @param callback the callback to call when a parameter is read
     */
    public void read(@NonNull WireBuffer buffer, @NonNull Callback callback) {
        int id = buffer.readByte();
        System.out.println("read " + id);
        Param param = mParams[0xFF & id];
        switch (param.mType) {
            case P_INT:
                callback.value(id, buffer.readInt());
                break;
            case P_FLOAT:
                callback.value(id, Float.intBitsToFloat(buffer.readInt()));
                break;
            case P_SHORT:
                callback.value(id, (short) buffer.readShort());
                break;
            case P_BYTE:
                callback.value(id, (byte) buffer.readByte());
                break;
            case P_BOOLEAN:
                callback.value(id, buffer.readBoolean());
                break;
            case PA_INT:
                int count = buffer.readShort();
                int[] values = new int[count];
                for (int i = 0; i < count; i++) {
                    values[i] = buffer.readInt();
                }
                callback.value(id, values);
                break;
            case PA_FLOAT:
                count = buffer.readShort();
                float[] floats = new float[count];
                for (int i = 0; i < count; i++) {
                    floats[i] = buffer.readFloat();
                }
                callback.value(id, floats);
                break;
            case PA_STRING:
                String str = buffer.readUTF8();
                callback.value(id, str);
                break;
            default:
                throw new IllegalArgumentException("Unknown parameter type " + param.mType);
        }
    }
}
