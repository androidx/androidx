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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.TouchHandler;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ColorUtils;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.easing.Easing;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Represents a ripple effect */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RippleModifierOperation extends DecoratorModifierOperation implements TouchHandler {
    private static final int OP_CODE = Operations.MODIFIER_RIPPLE;

    long mAnimateRippleStart = 0;
    float mAnimateRippleX = 0f;
    float mAnimateRippleY = 0f;
    int mAnimateRippleDuration = 1000;

    float mWidth = 0;
    float mHeight = 0;

    public float @NonNull [] locationInWindow = new float[2];

    @NonNull PaintBundle mPaint = new PaintBundle();

    /**
     * Animate the ripple effect
     *
     * @param x
     * @param y
     * @param timeStampMillis
     */
    public void animateRipple(float x, float y, long timeStampMillis) {
        mAnimateRippleStart = timeStampMillis;
        mAnimateRippleX = x;
        mAnimateRippleY = y;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        return "RippleModifier";
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        RootLayoutComponent root = context.getDocument().getRootLayoutComponent();
        if (root != null) {
            root.setHasTouchListeners(true);
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mAnimateRippleStart == 0) {
            return;
        }
        context.needsRepaint();

        float progress = (context.getClock().millis() - mAnimateRippleStart);
        progress /= (float) mAnimateRippleDuration;
        if (progress > 1f) {
            mAnimateRippleStart = 0;
        }
        progress = Math.min(1f, progress);
        context.save();
        context.savePaint();
        mPaint.reset();

        FloatAnimation anim1 =
                new FloatAnimation(Easing.CUBIC_STANDARD, 1f, null, Float.NaN, Float.NaN);
        anim1.setInitialValue(0f);
        anim1.setTargetValue(1f);
        float tween = anim1.get(progress);

        FloatAnimation anim2 =
                new FloatAnimation(Easing.CUBIC_STANDARD, 0.5f, null, Float.NaN, Float.NaN);
        anim2.setInitialValue(0f);
        anim2.setTargetValue(1f);
        float tweenRadius = anim2.get(progress);

        int startColor = ColorUtils.createColor(250, 250, 250, 180);
        int endColor = ColorUtils.createColor(200, 200, 200, 0);
        int paintedColor = Utils.interpolateColor(startColor, endColor, tween);

        float radius = Math.max(mWidth, mHeight) * tweenRadius;
        mPaint.setColor(paintedColor);
        context.replacePaint(mPaint);
        context.clipRect(0f, 0f, mWidth, mHeight);
        context.drawCircle(mAnimateRippleX, mAnimateRippleY, radius);
        context.restorePaint();
        context.restore();
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "RIPPLE_MODIFIER");
    }

    /**
     * The operation name
     *
     * @return operation name
     */
    @NonNull
    public static String name() {
        return "RippleModifier";
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new RippleModifierOperation());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, name())
                .description(
                        "Ripple modifier. This modifier will do a ripple animation on touch down");
    }

    @Override
    public void onTouchDown(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        locationInWindow[0] = 0f;
        locationInWindow[1] = 0f;
        component.getLocationInWindow(locationInWindow);
        animateRipple(
                x - locationInWindow[0], y - locationInWindow[1], context.getClock().millis());
        context.hapticEffect(3);
    }

    @Override
    public void onTouchUp(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y,
            float dx,
            float dy) {}

    @Override
    public void onTouchDrag(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {}

    @Override
    public void onTouchCancel(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {}

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("RippleModifierOperation")
                .add("animateRippleStart", mAnimateRippleStart)
                .add("animateRippleX", mAnimateRippleX)
                .add("animateRippleY", mAnimateRippleY)
                .add("animateRippleDuration", mAnimateRippleDuration)
                .add("width", mWidth)
                .add("height", mHeight);
    }
}
