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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.DecoratorComponent;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.ListActionsOperation;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.ScrollDelegate;
import androidx.compose.remote.core.operations.layout.TouchHandler;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.semantics.ScrollableComponent;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Represents a scroll modifier. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScrollModifierOperation extends ListActionsOperation
        implements TouchHandler,
                DecoratorComponent,
                ScrollDelegate,
                VariableSupport,
                ScrollableComponent {
    private static final int OP_CODE = Operations.MODIFIER_SCROLL;
    public static final String CLASS_NAME = "ScrollModifierOperation";

    private final float mPositionExpression;
    private final float mMax;
    private final float mNotchMax;

    int mDirection;

    float mTouchDownX;
    float mTouchDownY;

    float mInitialScrollX;
    float mInitialScrollY;

    float mScrollX;
    float mScrollY;

    float mMaxScrollX;
    float mMaxScrollY;

    float mHostDimension;
    float mContentDimension;

    private TouchExpression mTouchExpression;

    public ScrollModifierOperation(int direction, float position, float max, float notchMax) {
        super("SCROLL_MODIFIER");
        this.mDirection = direction;
        this.mPositionExpression = position;
        this.mMax = max;
        this.mNotchMax = notchMax;
    }

    /**
     * Inflate the operation
     *
     * @param component
     */
    public void inflate(@NonNull Component component) {
        for (Operation op : mList) {
            if (op instanceof TouchExpression) {
                mTouchExpression = (TouchExpression) op;
                mTouchExpression.setComponent(component);
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (mTouchExpression != null) {
            mTouchExpression.registerListening(context);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mTouchExpression != null) {
            mTouchExpression.updateVariables(context);
        }
    }

    public boolean isVerticalScroll() {
        return mDirection == 0;
    }

    public boolean isHorizontalScroll() {
        return mDirection != 0;
    }

    public float getScrollX() {
        return mScrollX;
    }

    public float getScrollY() {
        return mScrollY;
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        RootLayoutComponent root = context.getDocument().getRootLayoutComponent();
        if (root != null) {
            root.setHasTouchListeners(true);
        }
        super.apply(context);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mDirection, mPositionExpression, mMax, mNotchMax);
    }

    /**
     * Serialize the string
     *
     * @param indent padding to display
     * @param serializer append the string
     */
    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "SCROLL = [" + mDirection + "]");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        for (Operation op : mList) {
            op.apply(context.getContext());
        }
        if (mTouchExpression == null) {
            return;
        }
        float position =
                context.getContext()
                        .mRemoteComposeState
                        .getFloat(Utils.idFromNan(mPositionExpression));

        if (mDirection == 0) {
            mScrollY = -position;
        } else {
            mScrollX = -position;
        }
    }

    @Override
    public String toString() {
        return "ScrollModifierOperation(" + mDirection + ")";
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
     * @param direction direction of the scroll (HORIZONTAL, VERTICAL)
     * @param position the current position
     * @param max the maximum position
     * @param notchMax the maximum notch
     */
    public static void apply(
            @NonNull WireBuffer buffer, int direction, float position, float max, float notchMax) {
        buffer.start(OP_CODE);
        buffer.writeInt(direction);
        buffer.writeFloat(position);
        buffer.writeFloat(max);
        buffer.writeFloat(notchMax);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int direction = buffer.readInt();
        float position = buffer.readFloat();
        float max = buffer.readFloat();
        float notchMax = buffer.readFloat();
        operations.add(new ScrollModifierOperation(direction, position, max, notchMax));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .description("define a Scroll Modifier")
                .field(INT, "direction", "");
    }

    private float getMaxScrollPosition(@NonNull Component component, int direction) {
        if (component instanceof LayoutComponent) {
            LayoutComponent layoutComponent = (LayoutComponent) component;
            int numChildren = layoutComponent.getChildrenComponents().size();
            if (numChildren > 0) {
                Component lastChild = layoutComponent.getChildrenComponents().get(numChildren - 1);
                if (direction == 0) { // VERTICAL
                    return lastChild.getY();
                } else {
                    return lastChild.getX();
                }
            }
        }
        return 0f;
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        mWidth = width;
        mHeight = height;
        float max = mMaxScrollY;
        if (mDirection != 0) { // HORIZONTAL
            max = mMaxScrollX;
        }
        if (mTouchExpression != null) {
            float maxScrollPosition = getMaxScrollPosition(component, mDirection);
            if (maxScrollPosition > 0) {
                max = maxScrollPosition;
            }
        }
        context.loadFloat(Utils.idFromNan(mMax), max);
        context.loadFloat(Utils.idFromNan(mNotchMax), mContentDimension);
    }

    @Override
    public void onTouchDown(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        mTouchDownX = x;
        mTouchDownY = y;
        mInitialScrollX = mScrollX;
        mInitialScrollY = mScrollY;
        if (mTouchExpression != null) {
            mTouchExpression.updateVariables(context);
            mTouchExpression.touchDown(context, x + mScrollX, y + mScrollY);
        }
        document.appliedTouchOperation(component);
    }

    @Override
    public void onTouchUp(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y,
            float dx,
            float dy) {
        if (mTouchExpression != null) {
            mTouchExpression.updateVariables(context);
            mTouchExpression.touchUp(context, x + mScrollX, y + mScrollY, dx, dy);
        }
        // If not using touch expression, should add velocity decay here
    }

    @Override
    public void onTouchDrag(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        if (mTouchExpression != null) {
            mTouchExpression.updateVariables(context);
            mTouchExpression.touchDrag(context, x + mScrollX, y + mScrollY);
        }
        float dx = x - mTouchDownX;
        float dy = y - mTouchDownY;

        if (!Utils.isVariable(mPositionExpression)) {
            if (mDirection == 0) {
                mScrollY = Math.max(-mMaxScrollY, Math.min(0, mInitialScrollY + dy));
            } else {
                mScrollX = Math.max(-mMaxScrollX, Math.min(0, mInitialScrollX + dx));
            }
        }
    }

    @Override
    public void onTouchCancel(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {}

    /**
     * Set the horizontal scroll dimension
     *
     * @param hostDimension the horizontal host dimension
     * @param contentDimension the horizontal content dimension
     */
    public void setHorizontalScrollDimension(float hostDimension, float contentDimension) {
        mHostDimension = hostDimension;
        mContentDimension = contentDimension;
        mMaxScrollX = contentDimension - hostDimension;
    }

    /**
     * Set the vertical scroll dimension
     *
     * @param hostDimension the vertical host dimension
     * @param contentDimension the vertical content dimension
     */
    public void setVerticalScrollDimension(float hostDimension, float contentDimension) {
        mHostDimension = hostDimension;
        mContentDimension = contentDimension;
        mMaxScrollY = contentDimension - hostDimension;
    }

    public float getContentDimension() {
        return mContentDimension;
    }

    @Override
    public float getScrollX(float currentValue) {
        if (mDirection == 1) {
            return mScrollX;
        }
        return 0f;
    }

    @Override
    public float getScrollY(float currentValue) {
        if (mDirection == 0) {
            return mScrollY;
        }
        return 0f;
    }

    @Override
    public boolean handlesHorizontalScroll() {
        return mDirection == 1;
    }

    @Override
    public boolean handlesVerticalScroll() {
        return mDirection == 0;
    }

    @Override
    public void reset() {
        // nothing here for now
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("ScrollModifierOperation")
                .add("direction", mDirection)
                .add("max", mMax)
                .add("notchMax", mNotchMax)
                .add("scrollValue", isVerticalScroll() ? mScrollY : mScrollX)
                .add("maxScrollValue", isVerticalScroll() ? mMaxScrollY : mMaxScrollX)
                .add("contentDimension", mContentDimension)
                .add("hostDimension", mHostDimension);
    }

    @Override
    public int scrollDirection() {
        if (handlesVerticalScroll()) {
            return ScrollableComponent.SCROLL_VERTICAL;
        } else {
            return ScrollableComponent.SCROLL_HORIZONTAL;
        }
    }

    @Override
    public int scrollByOffset(@NonNull RemoteContext context, int offset) {
        // TODO work out how to avoid disabling this
        mTouchExpression = null;

        if (handlesVerticalScroll()) {
            mScrollY = Math.max(-mMaxScrollY, Math.min(0, mScrollY + offset));
        } else {
            mScrollX = Math.max(-mMaxScrollX, Math.min(0, mScrollX + offset));
        }
        return offset;
    }

    @Override
    public boolean scrollDirection(
            @NonNull RemoteContext context, @NonNull ScrollDirection direction) {
        float offset = mHostDimension * 0.7f;

        if (direction == ScrollDirection.FORWARD
                || direction == ScrollDirection.DOWN
                || direction == ScrollDirection.RIGHT) {
            offset *= -1;
        }

        return scrollByOffset(context, (int) offset) != 0;
    }

    @Override
    public boolean showOnScreen(@NonNull RemoteContext context, @NonNull Component child) {
        float[] locationInWindow = new float[2];
        child.getLocationInWindow(locationInWindow);

        int offset = 0;
        if (handlesVerticalScroll()) {
            offset = (int) -locationInWindow[1];
        } else {
            offset = (int) -locationInWindow[0];
        }

        if (offset == 0) {
            return true;
        } else {
            return scrollByOffset(context, offset) != 0;
        }
    }

    @Nullable
    @Override
    public ScrollAxisRange getScrollAxisRange() {
        if (handlesVerticalScroll()) {
            return new ScrollAxisRange(mScrollY, mMaxScrollY, true, true);
        } else {
            return new ScrollAxisRange(mScrollX, mMaxScrollX, true, true);
        }
    }
}
