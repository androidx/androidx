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

import static androidx.compose.remote.core.operations.Utils.floatToString;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteComposeState;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Base class for commands that take 3 float */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpdateDynamicFloatList extends Operation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.UPDATE_DYNAMIC_FLOAT_LIST;
    @NonNull protected String mName = "UpdateDynamicFloatList";
    int mArrayId;
    float mIndex;
    float mIndexOut;
    float mValueOut;
    float mValue;

    public UpdateDynamicFloatList(int arrayId, float index, float value) {
        mArrayId = arrayId;
        mIndex = index;
        mIndexOut = index;
        mValue = value;
        mValueOut = value;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mIndexOut = Float.isNaN(mIndex) ? context.getFloat(Utils.idFromNan(mIndex)) : mIndex;
        mValueOut = Float.isNaN(mValue) ? context.getFloat(Utils.idFromNan(mValue)) : mValue;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mIndex)) {
            context.listensTo(Utils.idFromNan(mIndex), this);
        }
        if (Float.isNaN(mValue)) {
            context.listensTo(Utils.idFromNan(mValue), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        UpdateDynamicFloatList.apply(buffer, mArrayId, mIndexOut, mValueOut);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        RemoteComposeState state = context.mRemoteComposeState;
        int id = mArrayId;
        float[] values = state.getDynamicFloats(id);
        if (values != null) {
            int index = (int) mIndexOut;
            if (index < values.length) {
                values[index] = mValueOut;
            }
            state.markVariableDirty(id);
        }
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return toString();
    }

    /**
     * Write the operation to the buffer
     * @param buffer
     * @param id
     * @param index
     * @param value
     */
    public static void apply(@NonNull WireBuffer buffer, int id, float index, float value) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeFloat(index);
        buffer.writeFloat(value);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .add("arrayId", mArrayId)
                .add("index", mIndex, mIndexOut)
                .add("value", mValue, mValueOut);
    }

    @NonNull
    @Override
    public String toString() {
        return mName + " array: " + Utils.idString(Utils.idFromNan(mArrayId))
                + " index: " + floatToString(mIndexOut)
                + " value: " + floatToString(mValueOut);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations to add to
     */
    public static void read(
            @NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        float index = buffer.readFloat();
        float value = buffer.readFloat();

        Operation op = new UpdateDynamicFloatList(id, index, value);
        operations.add(op);
    }
}
