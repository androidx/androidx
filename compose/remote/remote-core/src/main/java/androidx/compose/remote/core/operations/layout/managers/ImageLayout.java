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
package androidx.compose.remote.core.operations.layout.managers;

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
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ImageScaling;
import androidx.compose.remote.core.operations.utilities.StringSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImageLayout extends LayoutManager implements VariableSupport {
    private int mBitmapId = -1;
    private int mScaleType;
    private float mAlpha = 1f;
    private float mOutAlpha;

    @NonNull
    ImageScaling mScaling = new ImageScaling();
    @NonNull
    PaintBundle mPaint = new PaintBundle();

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (mBitmapId != -1) {
            context.listensTo(mBitmapId, this);
        }
        context.listensTo(Utils.idFromNan(mAlpha), this);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutAlpha = Float.isNaN(mAlpha) ? context.getFloat(Utils.idFromNan(mAlpha)) : mAlpha;
    }

    public ImageLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int bitmapId,
            float x,
            float y,
            float width,
            float height,
            int scaleType,
            float alpha) {
        super(parent, componentId, animationId, x, y, width, height);
        mBitmapId = bitmapId;
        mScaleType = scaleType & 0xFF;
        mAlpha = alpha;
        mOutAlpha = Float.isNaN(alpha) ? 1f : alpha;
    }

    public ImageLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int bitmapId,
            int scaleType,
            float alpha) {
        this(parent, componentId, animationId, bitmapId, 0, 0, 0, 0, scaleType, alpha);
    }

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {

        BitmapData bitmapData = (BitmapData) context.getContext().getObject(mBitmapId);
        if (bitmapData != null) {
            size.setWidth(bitmapData.getWidth());
            size.setHeight(bitmapData.getHeight());
        }
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        float modifiersWidth = computeModifierDefinedWidth(context.getContext());
        float modifiersHeight = computeModifierDefinedHeight(context.getContext());
        ComponentMeasure m = measure.get(this);
        m.setW(modifiersWidth);
        m.setH(modifiersHeight);
    }

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        context.save();
        context.translate(mX, mY);
        mComponentModifiers.paint(context);
        float tx = mPaddingLeft;
        float ty = mPaddingTop;
        context.translate(tx, ty);
        float w = mWidth - mPaddingLeft - mPaddingRight;
        float h = mHeight - mPaddingTop - mPaddingBottom;
        context.clipRect(0f, 0f, w, h);

        BitmapData bitmapData = (BitmapData) context.getContext().getObject(mBitmapId);
        if (bitmapData != null) {
            mScaling.setup(
                    0f,
                    0f,
                    bitmapData.getWidth(),
                    bitmapData.getHeight(),
                    0f,
                    0f,
                    w,
                    h,
                    mScaleType,
                    1f);

            context.savePaint();
            if (mOutAlpha == 1f) {
                context.drawBitmap(
                        mBitmapId,
                        (int) 0f,
                        (int) 0f,
                        (int) bitmapData.getWidth(),
                        (int) bitmapData.getHeight(),
                        (int) mScaling.mFinalDstLeft,
                        (int) mScaling.mFinalDstTop,
                        (int) mScaling.mFinalDstRight,
                        (int) mScaling.mFinalDstBottom,
                        -1);
            } else {
                context.savePaint();
                mPaint.reset();
                // Set paint color to black with the alpha value, this will apply to the bitmap.
                mPaint.setColor(0f, 0f, 0f, mOutAlpha);
                context.applyPaint(mPaint);
                context.drawBitmap(
                        mBitmapId,
                        (int) 0f,
                        (int) 0f,
                        (int) bitmapData.getWidth(),
                        (int) bitmapData.getHeight(),
                        (int) mScaling.mFinalDstLeft,
                        (int) mScaling.mFinalDstTop,
                        (int) mScaling.mFinalDstRight,
                        (int) mScaling.mFinalDstBottom,
                        -1);
                context.restorePaint();
            }
            context.restorePaint();
        }

        // debugBox(this, context);
        context.translate(-tx, -ty);
        context.restore();
    }

    @NonNull
    @Override
    public String toString() {
        return "IMAGE_LAYOUT ["
                + mComponentId
                + ":"
                + mAnimationId
                + "] ("
                + mX
                + ", "
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + mVisibility;
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "IMAGE_LAYOUT";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                getSerializedName()
                        + " ["
                        + mComponentId
                        + ":"
                        + mAnimationId
                        + "] = "
                        + "["
                        + mX
                        + ", "
                        + mY
                        + ", "
                        + mWidth
                        + ", "
                        + mHeight
                        + "] "
                        + mVisibility
                        + " ("
                        + mBitmapId
                        + "\")");
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "ImageLayout";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_IMAGE;
    }

    /**
     * Write the operation to the buffer
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int bitmapId,
            int scaleType,
            float alpha) {
        buffer.start(id());
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(bitmapId);
        buffer.writeInt(scaleType);
        buffer.writeFloat(alpha);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.readInt();
        int animationId = buffer.readInt();
        int bitmapId = buffer.readInt();
        int scaleType = buffer.readInt();
        float alpha = buffer.readFloat();
        operations.add(new ImageLayout(null, componentId, animationId, bitmapId, scaleType, alpha));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", id(), name())
                .description("Image layout implementation.\n\n")
                .field(INT, "COMPONENT_ID", "unique id for this component")
                .field(
                        INT,
                        "ANIMATION_ID",
                        "id used to match components," + " for animation purposes")
                .field(INT, "BITMAP_ID", "bitmap id")
                .field(INT, "SCALE_TYPE", "scale type")
                .field(FLOAT, "ALPHA", "alpha");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mComponentId, mAnimationId, mBitmapId, mScaleType, mAlpha);
    }
}
