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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT_ARRAY;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Paint data operation */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PaintData extends PaintOperation
        implements ComponentData, VariableSupport, Serializable {
    private static final int OP_CODE = Operations.PAINT_VALUES;
    private static final String CLASS_NAME = "PaintData";
    @NonNull public PaintBundle mPaintData = new PaintBundle();
    public static final int MAX_STRING_SIZE = 4000;

    public PaintData() {}

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mPaintData.updateVariables(context);
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        mPaintData.registerVars(context, this);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mPaintData);
    }

    @NonNull
    @Override
    public String toString() {
        return "PaintData " + "\"" + mPaintData + "\"";
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
     * add a paint data to the buffer
     *
     * @param buffer the buffer to add to
     * @param paintBundle the paint bundle
     */
    public static void apply(@NonNull WireBuffer buffer, @NonNull PaintBundle paintBundle) {
        buffer.start(Operations.PAINT_VALUES);
        paintBundle.writeBundle(buffer);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        PaintData data = new PaintData();
        data.mPaintData.readBundle(buffer);
        operations.add(data);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Encode a Paint ")
                .field(INT, "length", "id string")
                .field(INT_ARRAY, "paint", "length", "path encoded as floats");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.applyPaint(mPaintData);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("paintBundle", mPaintData);
    }
}
