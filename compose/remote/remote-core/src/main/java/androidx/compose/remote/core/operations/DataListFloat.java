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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DataListFloat extends Operation implements VariableSupport, ArrayAccess, Serializable {
    private static final int OP_CODE = Operations.FLOAT_LIST;
    private static final String CLASS_NAME = "IdListData";
    public final int mId;
    private float @NonNull [] mValues;
    private static final int MAX_FLOAT_ARRAY = 2000;

    public DataListFloat(int id, float @NonNull [] values) {
        mId = id;
        mValues = values;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        // TODO add support for variables in arrays
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.addCollection(mId, this);
        for (float value : mValues) {
            if (Utils.isVariable(value)) {
                context.listensTo(Utils.idFromNan(value), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mValues);
    }

    @NonNull
    @Override
    public String toString() {
        return "DataListFloat[" + Utils.idString(mId) + "] " + Arrays.toString(mValues);
    }

    /**
     * Write this operation to the buffer
     *
     * @param buffer the buffer to apply the operation to
     * @param id the id of the array
     * @param values the values of the array
     */
    public static void apply(@NonNull WireBuffer buffer, int id, float @NonNull [] values) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(values.length);
        for (int i = 0; i < values.length; i++) {
            buffer.writeFloat(values[i]);
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
        int len = buffer.readInt();
        if (len > MAX_FLOAT_ARRAY) {
            throw new RuntimeException(len + " map entries more than max = " + MAX_FLOAT_ARRAY);
        }
        float[] values = new float[len];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.readFloat();
        }
        DataListFloat data = new DataListFloat(id, values);
        operations.add(data);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("a list of Floats")
                .field(DocumentedOperation.INT, "id", "id the array (2xxxxx)")
                .field(INT, "length", "number of floats")
                .field(FLOAT_ARRAY, "values", "length", "array of floats");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.addCollection(mId, this);
    }

    @Override
    public float getFloatValue(int index) {
        return mValues[index];
    }

    @Override
    public float @Nullable [] getFloats() {
        return mValues;
    }

    @Override
    public int getLength() {
        return mValues.length;
    }

    @SuppressWarnings("JdkImmutableCollections")
    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId).add("values", List.of(mValues));
    }

    /**
     * Update the DataListFloat with values from a new one
     *
     * @param lc
     */
    public void update(@NonNull DataListFloat lc) {
        mValues = lc.mValues;
    }
}
