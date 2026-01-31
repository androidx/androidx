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

/** Component size-aware background draw */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BackgroundModifierOperation extends DecoratorModifierOperation implements
        VariableSupport {
    private static final int OP_CODE = Operations.MODIFIER_BACKGROUND;
    private static final String CLASS_NAME = "BackgroundModifierOperation";
    float mX;
    float mY;
    float mWidth;
    float mHeight;
    float mR;
    float mRId;
    float mG;
    float mGId;
    float mB;
    float mBId;
    float mA;
    float mAId;
    boolean mUseColorId = false;
    int mColorId;
    int mShapeType = ShapeType.RECTANGLE;

    public static final int COLOR_REF = 2;
    @NonNull
    public PaintBundle mPaint = new PaintBundle();

    public BackgroundModifierOperation(
            int flags,
            int colorId,
            int reserve1,
            int reserve2,
            float r,
            float g,
            float b,
            float a,
            int shapeType) {

        this.mX = 0;
        this.mY = 0;
        this.mWidth = 0;
        this.mHeight = 0;
        if (flags == COLOR_REF) {
            mUseColorId = true;
        }
        this.mColorId = colorId;
        this.mRId = r;
        this.mR = mRId;
        this.mGId = g;
        this.mG = mGId;
        this.mBId = b;
        this.mB = mBId;
        this.mAId = a;
        this.mA = mAId;
        this.mShapeType = shapeType;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        if (mUseColorId) {
            apply(buffer, COLOR_REF, mColorId, 0, 0, mRId, mGId, mBId, mAId, mShapeType);
        } else {
            apply(buffer, 0, 0, 0, 0, mRId, mGId, mBId, mAId, mShapeType);
        }
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "BACKGROUND = ["
                        + mX
                        + ", "
                        + mY
                        + ", "
                        + mWidth
                        + ", "
                        + mHeight
                        + "] color ["
                        + mRId
                        + ", "
                        + mGId
                        + ", "
                        + mBId
                        + ", "
                        + mAId
                        + "] shape ["
                        + mShapeType
                        + "]");
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
        return "BackgroundModifierOperation(" + mWidth + " x " + mHeight + ")";
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
     * @param buffer    the WireBuffer
     * @param flags     flag
     * @param colorId   color ref
     * @param reserve1  reserved for future use
     * @param reserve2  reserved for future use
     * @param r         red component of the background color
     * @param g         green component of the background color
     * @param b         blue component of the background color
     * @param a         alpha component of the background color
     * @param shapeType the shape of the background (RECTANGLE=0, CIRCLE=1)
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int flags,
            int colorId,
            int reserve1,
            int reserve2,
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
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int flags = buffer.readInt();
        int colorId = buffer.readInt();
        int reserve1 = buffer.readInt();
        int reserve2 = buffer.readInt();
        float r = buffer.readFloat();
        float g = buffer.readFloat();
        float b = buffer.readFloat();
        float a = buffer.readFloat();
        // shape type
        int shapeType = buffer.readInt();
        operations.add(
                new BackgroundModifierOperation(flags, colorId, reserve1, reserve2, r, g, b, a,
                        shapeType));
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
        context.replacePaint(mPaint);
        if (mShapeType == ShapeType.RECTANGLE) {
            context.drawRect(0f, 0f, mWidth, mHeight);
        } else if (mShapeType == ShapeType.CIRCLE) {
            context.drawCircle(mWidth / 2f, mHeight / 2f, Math.min(mWidth, mHeight) / 2f);
        }
        context.restorePaint();
    }

    private static boolean isAtLeastVersion7(@NonNull RemoteContext context) {
        return context.supportsVersion(1, 1, 0);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (isAtLeastVersion7(context)) {
            if (Float.isNaN(mRId)) {
                mR = context.getFloat(Utils.idFromNan(mRId));
            }
            if (Float.isNaN(mGId)) {
                mG = context.getFloat(Utils.idFromNan(mGId));
            }
            if (Float.isNaN(mBId)) {
                mB = context.getFloat(Utils.idFromNan(mBId));
            }
            if (Float.isNaN(mAId)) {
                mA = context.getFloat(Utils.idFromNan(mAId));
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (isAtLeastVersion7(context)) {
            if (Float.isNaN(mRId)) {
                context.listensTo(Utils.idFromNan(mRId), this);
            }
            if (Float.isNaN(mGId)) {
                context.listensTo(Utils.idFromNan(mGId), this);
            }
            if (Float.isNaN(mBId)) {
                context.listensTo(Utils.idFromNan(mBId), this);
            }
            if (Float.isNaN(mAId)) {
                context.listensTo(Utils.idFromNan(mAId), this);
            }
        }
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("modifier_background")
                .description("Define a background color or shape for a component")
                .field(INT, "flags", "Behavior flags")
                .field(INT, "colorId", "The ID of the color if flags include COLOR_REF")
                .field(INT, "reserve1", "Reserved for future use")
                .field(INT, "reserve2", "Reserved for future use")
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
                .addType("BackgroundModifierOperation")
                .add("x", mX)
                .add("y", mY)
                .add("width", mWidth)
                .add("height", mHeight)
                .add("color", mA, mR, mG, mB)
                .add("shapeType", ShapeType.getString(mShapeType));
    }
}
