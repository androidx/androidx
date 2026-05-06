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
package androidx.compose.remote.core.operations.layout.modifiers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Represents an offset modifier. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OffsetModifierOperation extends DecoratorModifierOperation implements VariableSupport {
    private static final int OP_CODE = Operations.MODIFIER_OFFSET;
    public static final String CLASS_NAME = "OffsetModifierOperation";

    float mX;
    float mY;

    float mXValue;
    float mYValue;

    public OffsetModifierOperation(float x, float y) {
        this.mX = x;
        this.mY = y;
        this.mXValue = mX;
        this.mYValue = mY;
    }

    public float getX() {
        return mXValue;
    }

    public float getY() {
        return mYValue;
    }

    public void setX(float x) {
        this.mX = x;
        this.mXValue = x;
    }

    public void setY(float y) {
        this.mY = y;
        this.mYValue = y;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mX, mY);
    }

    /**
     * Serialize the string
     *
     * @param indent padding to display
     * @param serializer append the string
     */
    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "OFFSET = [" + mX + ", " + mY + "]");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        float x = mXValue;
        float y = mYValue;
        if (context.getDensityBehavior() == CoreDocument.DENSITY_BEHAVIOR_DP) {
            float density = context.getDensity();
            x *= density;
            y *= density;
        }
        context.translate(x, y);
    }

    @Override
    public String toString() {
        return "OffsetModifierOperation(" + mX + ", " + mY + ")";
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
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param x x offset
     * @param y y offset
     */
    public static void apply(@NonNull WireBuffer buffer, float x, float y) {
        buffer.start(OP_CODE);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        float x = buffer.readNanId();
        float y = buffer.readNanId();
        operations.add(new OffsetModifierOperation(x, y));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("modifier_offset")
                .description("Shift the component's position")
                .field(FLOAT, "x", "X offset")
                .field(FLOAT, "y", "Y offset");
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {}

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("OffsetModifierOperation")
                .add("x", mX)
                .add("y", mY);
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mX)) {
            context.listensTo(Utils.idFromNan(mX), this);
        }
        if (Float.isNaN(mY)) {
            context.listensTo(Utils.idFromNan(mY), this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mXValue = Float.isNaN(mX) ? context.getFloat(Utils.idFromNan(mX)) : mX;
        mYValue = Float.isNaN(mY) ? context.getFloat(Utils.idFromNan(mY)) : mY;
    }
}
