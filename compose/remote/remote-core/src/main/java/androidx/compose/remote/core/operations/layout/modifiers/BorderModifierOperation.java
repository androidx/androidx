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
package androidx.compose.remote.core.operations.layout.modifiers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Component size-aware border draw */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BorderModifierOperation extends DecoratorModifierOperation implements VariableSupport {
    private static final int OP_CODE = Operations.MODIFIER_BORDER;
    public static final String CLASS_NAME = "BorderModifierOperation";

    float mX;
    float mY;
    float mWidth;
    float mHeight;
    float mBorderWidth;
    float mBorderWidthValue;
    float mRoundedCorner;
    float mR;
    float mG;
    float mB;
    float mA;
    boolean mUseColorId = false;
    int mColorId;
    int mShapeType = ShapeType.RECTANGLE;
    /** Color is through and ID */
    public static final int COLOR_REF = 2;

    @NonNull public PaintBundle mPaint = new PaintBundle();

    public BorderModifierOperation(
            int flags,
            int colorId,
            int reserved1,
            int reserved2,
            float borderWidth,
            float roundedCorner,
            float r,
            float g,
            float b,
            float a,
            int shapeType) {
        this.mX = 0;
        this.mY = 0;
        this.mWidth = 0;
        this.mHeight = 0;
        this.mBorderWidth = borderWidth;
        if (!Float.isNaN(mBorderWidth)) {
            mBorderWidthValue = mBorderWidth;
        }
        this.mRoundedCorner = roundedCorner;
        if (flags == COLOR_REF) {
            mUseColorId = true;
            mColorId = colorId;
        }
        this.mR = r;
        this.mG = g;
        this.mB = b;
        this.mA = a;
        this.mShapeType = shapeType;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "BORDER = ["
                        + mX
                        + ", "
                        + mY
                        + ", "
                        + mWidth
                        + ", "
                        + mHeight
                        + "] "
                        + "color ["
                        + mR
                        + ", "
                        + mG
                        + ", "
                        + mB
                        + ", "
                        + mA
                        + "] "
                        + "border ["
                        + mBorderWidth
                        + ", "
                        + mRoundedCorner
                        + "] "
                        + "shape ["
                        + mShapeType
                        + "]");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                0,
                0,
                0,
                0,
                mBorderWidth,
                mRoundedCorner,
                mR,
                mG,
                mB,
                mA,
                mShapeType);
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @NonNull
    @Override
    public String toString() {
        return "BorderModifierOperation("
                + mX
                + ","
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + "borderWidth("
                + mBorderWidth
                + ") "
                + "color("
                + mR
                + ","
                + mG
                + ","
                + mB
                + ","
                + mA
                + ")";
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
     * @param buffer the WireBuffer
     * @param flags flag
     * @param colorId the id of the color if flag is set
     * @param reserve1 reserved for future expansion
     * @param reserve2 reserved for future expansion
     * @param borderWidth the width of the border outline
     * @param roundedCorner rounded corner value in pixels
     * @param r red component of the border color
     * @param g green component of the border color
     * @param b blue component of the border color
     * @param a alpha component of the border color
     * @param shapeType the shape type (0 = RECTANGLE, 1 = CIRCLE)
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int flags,
            int colorId,
            int reserve1,
            int reserve2,
            float borderWidth,
            float roundedCorner,
            float r,
            float g,
            float b,
            float a,
            int shapeType) {
        buffer.start(OP_CODE);
        buffer.writeInt(flags);
        buffer.writeInt(colorId);
        buffer.writeInt(reserve1);
        buffer.writeInt(reserve2);
        buffer.writeFloat(borderWidth);
        buffer.writeFloat(roundedCorner);
        buffer.writeFloat(r);
        buffer.writeFloat(g);
        buffer.writeFloat(b);
        buffer.writeFloat(a);
        // shape type
        buffer.writeInt(shapeType);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int flags = buffer.readInt();
        int colorId = buffer.readInt();
        int reserve1 = buffer.readInt();
        int reserve2 = buffer.readInt();
        float bw = buffer.readFloat();
        float rc = buffer.readFloat();
        float r = buffer.readFloat();
        float g = buffer.readFloat();
        float b = buffer.readFloat();
        float a = buffer.readFloat();
        // shape type
        int shapeType = buffer.readInt();
        operations.add(
                new BorderModifierOperation(flags,
                        colorId, reserve1, reserve2, bw, rc, r, g, b, a, shapeType));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.savePaint();
        mPaint.reset();
        mPaint.setStyle(PaintBundle.STYLE_FILL);
        if (mUseColorId) {
            int col = context.getContext().getColor(mColorId);
            mPaint.setColor(col);
        } else {
            mPaint.setColor(mR, mG, mB, mA);
        }
        if (isAtLeastVersion7(context.getContext())) {
            mPaint.setStrokeWidth(mBorderWidthValue);
        } else {
            mPaint.setStrokeWidth(mBorderWidth * context.getContext().getDensity());
        }
        mPaint.setStyle(PaintBundle.STYLE_STROKE);
        context.replacePaint(mPaint);
        if (mShapeType == ShapeType.RECTANGLE) {
            context.drawRect(0f, 0f, mWidth, mHeight);
        } else {
            float size = mRoundedCorner;
            if (mShapeType == ShapeType.CIRCLE) {
                size = Math.min(mWidth, mHeight) / 2f;
            }
            context.drawRoundRect(0f, 0f, mWidth, mHeight, size, size);
        }
        context.restorePaint();
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("modifier_border")
                .description("Define a border for a component")
                .field(INT, "flags", "Behavior flags")
                .field(INT, "colorId", "The ID of the color if flags include COLOR_REF")
                .field(INT, "reserve1", "Reserved for future use")
                .field(INT, "reserve2", "Reserved for future use")
                .field(FLOAT, "borderWidth", "Width of the border")
                .field(FLOAT, "roundedCorner", "Radius for rounded corners")
                .field(FLOAT, "r", "Red component [0..1]")
                .field(FLOAT, "g", "Green component [0..1]")
                .field(FLOAT, "b", "Blue component [0..1]")
                .field(FLOAT, "a", "Alpha component [0..1]")
                .field(INT, "shapeType", "The shape type (0=RECTANGLE, 1=CIRCLE)");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("BorderModifierOperation")
                .add("x", mX)
                .add("y", mY)
                .add("width", mWidth)
                .add("height", mHeight)
                .add("borderWidth", mBorderWidth)
                .add("roundedCornerRadius", mRoundedCorner)
                .add("color", mA, mR, mG, mB)
                .add("shapeType", ShapeType.getString(mShapeType));
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (isAtLeastVersion7(context)) {
            if (Float.isNaN(mBorderWidth)) {
                context.listensTo(Utils.idFromNan(mBorderWidth), this);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (isAtLeastVersion7(context)) {
            mBorderWidthValue =
                    Float.isNaN(mBorderWidth)
                            ? context.getFloat(Utils.idFromNan(mBorderWidth))
                            : mBorderWidth;
        } else {
            mBorderWidthValue = mBorderWidth;
        }
    }

    private static boolean isAtLeastVersion7(@NonNull RemoteContext context) {
        return context.supportsVersion(1, 1, 0);
    }
}
