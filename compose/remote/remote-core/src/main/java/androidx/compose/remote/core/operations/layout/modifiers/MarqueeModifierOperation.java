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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.ScrollDelegate;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Represents a Marquee modifier. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MarqueeModifierOperation extends DecoratorModifierOperation implements ScrollDelegate {
    private static final int OP_CODE = Operations.MODIFIER_MARQUEE;
    public static final String CLASS_NAME = "MarqueeModifierOperation";

    int mIterations;
    int mAnimationMode;
    float mRepeatDelayMillis;
    float mInitialDelayMillis;
    float mSpacing;
    float mVelocity;

    private float mComponentWidth;
    private float mComponentHeight;
    private float mContentWidth;
    private float mContentHeight;

    public MarqueeModifierOperation(
            int iterations,
            int animationMode,
            float repeatDelayMillis,
            float initialDelayMillis,
            float spacing,
            float velocity) {
        this.mIterations = iterations;
        this.mAnimationMode = animationMode;
        this.mRepeatDelayMillis = repeatDelayMillis;
        this.mInitialDelayMillis = initialDelayMillis;
        this.mSpacing = spacing;
        this.mVelocity = velocity;
    }

    public void setContentWidth(float value) {
        mContentWidth = value;
    }

    public void setContentHeight(float value) {
        mContentHeight = value;
    }

    @Override
    public float getScrollX(float currentValue) {
        return mScrollX;
    }

    @Override
    public float getScrollY(float currentValue) {
        return 0;
    }

    @Override
    public boolean handlesHorizontalScroll() {
        return true;
    }

    @Override
    public boolean handlesVerticalScroll() {
        return false;
    }

    /** Reset the modifier */
    @Override
    public void reset() {
        mLastTime = 0;
        mScrollX = 0f;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mIterations,
                mAnimationMode,
                mRepeatDelayMillis,
                mInitialDelayMillis,
                mSpacing,
                mVelocity);
    }

    /**
     * Serialize the string
     *
     * @param indent padding to display
     * @param serializer append the string
     */
    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "MARQUEE = ["
                        + mIterations
                        + "] "
                        + mComponentWidth
                        + " x "
                        + mComponentHeight
                        + " / "
                        + mContentWidth
                        + " x "
                        + mContentHeight);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    private long mLastTime = 0;
    private long mStartTime = 0;

    private float mScrollX = 0f;

    @Override
    public void paint(@NonNull PaintContext context) {
        long currentTime = context.getClock().millis();
        if (mLastTime == 0) {
            mLastTime = currentTime;
            mStartTime = mLastTime + (long) mInitialDelayMillis;
            context.needsRepaint();
        }
        if (mContentWidth > mComponentWidth && currentTime - mStartTime > mInitialDelayMillis) {
            float density = context.getContext().getDensity(); // in dp
            float delta = mContentWidth - mComponentWidth;
            float duration = delta / (density * mVelocity);
            float elapsed = ((currentTime - mStartTime) / 1000f);
            elapsed = (elapsed % duration) / duration;
            float offset =
                    (1f + (float) Math.sin(elapsed * 2 * Math.PI - Math.PI / 2f)) / 2f * -delta;

            mScrollX = offset;
            context.needsRepaint();
        }
    }

    @Override
    public String toString() {
        return "MarqueeModifierOperation(" + mIterations + ")";
    }

    /**
     * Name of the operation
     *
     * @return name
     */
    public static @NonNull String name() {
        return CLASS_NAME;
    }

    /**
     * id of the operation
     *
     * @return the operation id
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param iterations the number of iterations
     * @param animationMode animation mode
     * @param repeatDelayMillis repeat delay in ms
     * @param initialDelayMillis initial delay before the marquee start in ms
     * @param spacing the spacing between marquee
     * @param velocity the velocity of the marquee animation
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int iterations,
            int animationMode,
            float repeatDelayMillis,
            float initialDelayMillis,
            float spacing,
            float velocity) {
        buffer.start(OP_CODE);
        buffer.writeInt(iterations);
        buffer.writeInt(animationMode);
        buffer.writeFloat(repeatDelayMillis);
        buffer.writeFloat(initialDelayMillis);
        buffer.writeFloat(spacing);
        buffer.writeFloat(velocity);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int iterations = buffer.readInt();
        int animationMode = buffer.readInt();
        float repeatDelayMillis = buffer.readFloat();
        float initialDelayMillis = buffer.readFloat();
        float spacing = buffer.readFloat();
        float velocity = buffer.readFloat();
        operations.add(
                new MarqueeModifierOperation(
                        iterations,
                        animationMode,
                        repeatDelayMillis,
                        initialDelayMillis,
                        spacing,
                        velocity));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .description("specify a Marquee Modifier")
                .field(FLOAT, "value", "");
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        mComponentWidth = width;
        mComponentHeight = height;
        if (component instanceof LayoutComponent) {
            LayoutComponent layoutComponent = (LayoutComponent) component;
            setContentWidth(layoutComponent.minIntrinsicWidth(context));
            setContentHeight(layoutComponent.minIntrinsicHeight(context));
        }
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("MarqueeModifierOperation")
                .add("iterations", mIterations)
                .add("animationMode", mAnimationMode)
                .add("repeatDelayMillis", mRepeatDelayMillis)
                .add("initialDelayMillis", mInitialDelayMillis)
                .add("spacing", mSpacing)
                .add("velocity", mVelocity);
    }
}
