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
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Operation to perform Color related calculation TODO support color update */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ColorAttribute extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.ATTRIBUTE_COLOR;
    private static final String CLASS_NAME = "ColorAttribute";
    public int mId;
    public int mColorId;
    public short mType;

    /** The hue value of the color */
    public static final short COLOR_HUE = 0;

    /** The saturation value of the color */
    public static final short COLOR_SATURATION = 1;

    /** The brightness value of the color */
    public static final short COLOR_BRIGHTNESS = 2;

    /** The red value of the color */
    public static final short COLOR_RED = 3;

    /** The green value of the color */
    public static final short COLOR_GREEN = 4;

    /** The blue value of the color */
    public static final short COLOR_BLUE = 5;

    /** The alpha value of the color */
    public static final short COLOR_ALPHA = 6;

    /**
     * creates a new operation
     *
     * @param id to write value to
     * @param colorId of long to calculate on
     * @param type the type of calculation
     */
    public ColorAttribute(int id, int colorId, short type) {
        this.mId = id;
        this.mColorId = colorId;
        this.mType = type;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mColorId, mType);
    }

    @Override
    public @NonNull String toString() {
        return CLASS_NAME + "[" + mId + "] = " + mColorId + " " + mType;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    public static @NonNull String name() {
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
     * Writes out the operation to the buffer
     *
     * @param buffer write command to this buffer
     * @param id the id
     * @param textId the id
     * @param type the value of the float
     */
    public static void apply(@NonNull WireBuffer buffer, int id, int textId, short type) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(textId);
        buffer.writeShort(type);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int textId = buffer.readInt();
        short type = (short) buffer.readShort();
        operations.add(new ColorAttribute(id, textId, type));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Color Operations", OP_CODE, CLASS_NAME)
                .description("Calculate Information about a Color")
                .field(INT, "id", "id to output")
                .field(INT, "longId", "id of color")
                .field(SHORT, "type", "the type information to extract");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        int val = mType & 255;

        RemoteContext ctx = context.getContext();
        int color = ctx.getColor(mColorId);
        switch (val) {
            case COLOR_HUE:
                ctx.loadFloat(mId, Utils.getHue(color));
                break;
            case COLOR_SATURATION:
                ctx.loadFloat(mId, Utils.getSaturation(color));
                break;
            case COLOR_BRIGHTNESS:
                ctx.loadFloat(mId, Utils.getBrightness(color));
                break;
            case COLOR_RED:
                ctx.loadFloat(mId, ((color >> 16) & 0xFF) / 255.0f);
                break;
            case COLOR_GREEN:
                ctx.loadFloat(mId, ((color >> 8) & 0xFF) / 255.0f);
                break;
            case COLOR_BLUE:
                ctx.loadFloat(mId, (color & 0xFF) / 255.0f);
                break;
            case COLOR_ALPHA:
                ctx.loadFloat(mId, ((color >> 24) & 0xFF) / 255.0f);
                break;
        }
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("timeId", mColorId)
                .addType(getTypeString());
    }

    private String getTypeString() {
        int val = mType & 255;
        switch (val) {
            case COLOR_HUE:
                return "COLOR_HUE";
            case COLOR_SATURATION:
                return "COLOR_SATURATION";
            case COLOR_BRIGHTNESS:
                return "COLOR_BRIGHTNESS";
            case COLOR_RED:
                return "COLOR_RED";
            case COLOR_GREEN:
                return "COLOR_GREEN";
            case COLOR_BLUE:
                return "COLOR_BLUE";
            case COLOR_ALPHA:
                return "COLOR_ALPHA";
            default:
                return "INVALID_TIME_TYPE";
        }
    }

    /**
     * Call to allow an operator to register interest in variables. Typically they call
     * context.listensTo(id, this)
     *
     * @param context
     */
    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(Utils.idFromNan(mColorId), this);
    }

    /**
     * Called to be notified that the variables you are interested have changed.
     *
     * @param context
     */
    @Override
    public void updateVariables(@NonNull RemoteContext context) {}
}
