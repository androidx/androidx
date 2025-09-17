/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static androidx.compose.remote.core.operations.Utils.floatToString;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Base class for commands that take 3 float */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class DrawBase2 extends PaintOperation implements VariableSupport, Serializable {
    @NonNull protected String mName = "DrawRectBase";
    float mV1;
    float mV2;
    float mValue1;
    float mValue2;

    public DrawBase2(float v1, float v2) {
        mValue1 = v1;
        mValue2 = v2;
        mV1 = v1;
        mV2 = v2;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mV1 = Float.isNaN(mValue1) ? context.getFloat(Utils.idFromNan(mValue1)) : mValue1;
        mV2 = Float.isNaN(mValue2) ? context.getFloat(Utils.idFromNan(mValue2)) : mValue2;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mValue1)) {
            context.listensTo(Utils.idFromNan(mValue1), this);
        }
        if (Float.isNaN(mValue2)) {
            context.listensTo(Utils.idFromNan(mValue2), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        write(buffer, mV1, mV2);
    }

    protected abstract void write(@NonNull WireBuffer buffer, float v1, float v2);

    protected interface Maker {
        DrawBase2 create(float v1, float v2);
    }

    @NonNull
    @Override
    public String toString() {
        return mName + " " + floatToString(mV1) + " " + floatToString(mV2);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations to add to
     * @param maker the maker of the operation
     */
    public static void read(
            @NonNull WireBuffer buffer, @NonNull List<Operation> operations, @NonNull Maker maker) {
        float v1 = buffer.readFloat();
        float v2 = buffer.readFloat();

        Operation op = maker.create(v1, v2);
        operations.add(op);
    }

    /**
     * Override to construct a 2 float value operation
     *
     * @param x1
     * @param y1
     * @return
     */
    @Nullable
    public Operation construct(float x1, float y1) {
        return null;
    }

    /**
     * Writes out the operation to the buffer
     *
     * @param buffer
     * @param opCode
     * @param x1
     * @param y1
     */
    protected static void write(@NonNull WireBuffer buffer, int opCode, float x1, float y1) {
        buffer.start(opCode);
        buffer.writeFloat(x1);
        buffer.writeFloat(y1);
    }

    protected @NonNull MapSerializer serialize(
            @NonNull MapSerializer serializer, @NonNull String v1Name, @NonNull String v2Name) {
        return serializer.add(v1Name, mValue1, mV1).add(v2Name, mValue2, mV2);
    }
}
