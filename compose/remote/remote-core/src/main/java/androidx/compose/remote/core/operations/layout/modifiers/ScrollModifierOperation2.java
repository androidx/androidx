/*
 * Copyright (C) 2026 The Android Open Source Project
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

// This class duplicates parts of ScrollModifierOperation until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.ScrollingEdgeEffect;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.operations.TouchExpressionOperation2;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.ListActionsOperation;
import androidx.compose.remote.core.operations.layout.ScrollDelegate;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.semantics.ScrollableComponent;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Scroll modifier dispatched through the unified pointer input router.
 *
 * <p>Shares the opcode and wire format of {@link ScrollModifierOperation} and offers the same
 * layout-facing API (scroll dimensions, {@link ScrollDelegate}) and accessibility semantics
 * ({@link ScrollableComponent}), but detects the drag through the {@link EventHandler} interface
 * and drives a nested {@link TouchExpressionOperation2}.
 *
 * <p>Gesture detection follows {@code Modifier.scrollable} in Compose and runs in the <em>main</em>
 * (bubbling) pass:
 *
 * <ul>
 *   <li>{@code DOWN} is tracked whether or not a child consumed it, and is never consumed by the
 *       scroll modifier itself.
 *   <li>{@code MOVE} is consumed once the pointer travels beyond the touch slop along the scroll
 *       axis. Because children run before their parent in the main pass, they observe this
 *       consumption in the <em>final</em> pass of the same event and cancel their gesture (see
 *       {@code ClickModifierOperation2}). A move that reaches the modifier already consumed (by a
 *       child slider, an inner scroll container, or an ancestor in the initial pass) cancels the
 *       scroll instead, so the innermost scrollable wins.
 *   <li>{@code UP} / {@code CANCEL} are consumed only if a scroll was in progress.
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScrollModifierOperation2 extends ListActionsOperation
        implements ScrollDelegate, VariableSupport, EventHandler, ScrollableComponent {
    private static final int OP_CODE = Operations.MODIFIER_SCROLL;
    public static final String CLASS_NAME = "ScrollModifierOperation";

    private static final int NO_POINTER = -1;

    /** Wire value of {@code direction} for a vertical scroll (anything else is horizontal). */
    private static final int DIRECTION_VERTICAL_VALUE = 0;

    // ---------------------------------------------------------------------------------------
    // Wire format
    // ---------------------------------------------------------------------------------------
    private final int mDirection;
    private final float mPositionExpression;
    private final float mMax;
    private final float mNotchMax;

    // ---------------------------------------------------------------------------------------
    // Layout state
    // ---------------------------------------------------------------------------------------
    private float mScrollX;
    private float mScrollY;
    private float mMaxScrollX;
    private float mMaxScrollY;
    private float mHostDimension;
    private float mContentDimension;

    private @Nullable TouchExpressionOperation2 mTouchExpression;
    private @Nullable Component mComponent;
    private @Nullable ScrollingEdgeEffect mEdgeEffectA;
    private @Nullable ScrollingEdgeEffect mEdgeEffectB;

    // ---------------------------------------------------------------------------------------
    // Gesture state
    // ---------------------------------------------------------------------------------------
    private int mPointerId = NO_POINTER;
    private boolean mScrolling;
    private float mDownX;
    private float mDownY;
    private float mLastX;
    private float mLastY;
    private float mInitialScrollX;
    private float mInitialScrollY;

    public ScrollModifierOperation2(int direction, float position, float max, float notchMax) {
        super("SCROLL_MODIFIER");
        mDirection = direction;
        mPositionExpression = position;
        mMax = max;
        mNotchMax = notchMax;
    }

    /**
     * Inflate the operation: binds the hosting component and finds the nested touch expression.
     *
     * @param component the component this modifier is attached to
     */
    public void inflate(@NonNull Component component) {
        mComponent = component;
        for (Operation op : mList) {
            if (op instanceof TouchExpressionOperation2) {
                mTouchExpression = (TouchExpressionOperation2) op;
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        // The nested touch expression is registered through the container recursion.
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mTouchExpression != null) {
            mTouchExpression.updateVariables(context);
        }
    }

    /** Returns true if this modifier scrolls vertically. */
    public boolean isVerticalScroll() {
        return mDirection == DIRECTION_VERTICAL_VALUE;
    }

    /** Returns true if this modifier scrolls horizontally. */
    public boolean isHorizontalScroll() {
        return mDirection != DIRECTION_VERTICAL_VALUE;
    }

    /** Returns the current horizontal scroll offset (negative when scrolled right). */
    public float getScrollX() {
        return mScrollX;
    }

    /** Returns the current vertical scroll offset (negative when scrolled down). */
    public float getScrollY() {
        return mScrollY;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mDirection, mPositionExpression, mMax, mNotchMax);
    }

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
        float position = context.getContext().getFloat(Utils.idFromNan(mPositionExpression));
        if (isVerticalScroll()) {
            mScrollY = -Math.min(mMaxScrollY, position);
        } else {
            mScrollX = -Math.min(mMaxScrollX, position);
        }
    }

    @Override
    public String toString() {
        return "ScrollModifierOperation2(" + mDirection + ")";
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
     * Write the operation to the buffer. The wire format is identical to {@link
     * ScrollModifierOperation}.
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
        float position = buffer.readNanId();
        float max = buffer.readNanId();
        float notchMax = buffer.readNanId();
        operations.add(new ScrollModifierOperation2(direction, position, max, notchMax));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("modifier_scroll")
                .description("Define a scrolling behavior for a component")
                .field(INT, "direction", "Direction of the scroll (0=VERTICAL, 1=HORIZONTAL)")
                .field(FLOAT, "position", "The current scroll position (expression)")
                .field(FLOAT, "max", "The maximum scroll position")
                .field(FLOAT, "notchMax", "The maximum notch position");
    }

    private float getMaxScrollPosition(@NonNull Component component) {
        if (component instanceof LayoutComponent) {
            List<Component> children = ((LayoutComponent) component).getChildrenComponents();
            if (!children.isEmpty()) {
                Component lastChild = children.get(children.size() - 1);
                return isVerticalScroll() ? lastChild.getY() : lastChild.getX();
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
        mComponent = component;
        mWidth = width;
        mHeight = height;
        float max = isVerticalScroll() ? mMaxScrollY : mMaxScrollX;
        if (mTouchExpression != null) {
            float maxScrollPosition = getMaxScrollPosition(component);
            if (maxScrollPosition > 0) {
                max = Math.min(maxScrollPosition, max);
            }
        }
        context.loadFloat(Utils.idFromNan(mMax), max);
        context.loadFloat(Utils.idFromNan(mNotchMax), mContentDimension);
        if (mEdgeEffectA != null) {
            mEdgeEffectA.setSize(mWidth, mHeight);
        }
        if (mEdgeEffectB != null) {
            mEdgeEffectB.setSize(mWidth, mHeight);
        }
    }

    /**
     * Returns whether this handler matches the given event type.
     *
     * @param eventType event type identifier
     * @return true if event is a pointer event
     */
    @Override
    public boolean matchesEvent(int eventType) {
        return eventType == EVENT_TYPE_POINTER;
    }

    /**
     * Handles a pointer event in the main pass and scrolls once the touch slop is exceeded.
     *
     * <p>Event data layout: {@code [pointerId, localX, localY, velocityX, velocityY]}.
     *
     * @param context current remote context
     * @param document document containing this component
     * @param event pointer event being dispatched
     * @return status indicating whether the event was consumed
     */
    @Override
    public int onEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {
        int metadata = event.getMetadata();
        if (MetadataUtil.unpackPass(metadata) != POINTER_PASS_MAIN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        float[] data = event.getData();
        Component component = mComponent;
        if (data == null || data.length < 3 || component == null) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int pointerId = (int) data[0];
        float x = data[1];
        float y = data[2];
        boolean consumed = MetadataUtil.unpackConsumed(metadata) == POINTER_STATUS_CONSUMED;

        switch (MetadataUtil.unpackSubtype(metadata)) {
            case POINTER_SUBTYPE_DOWN:
                return onDown(context, pointerId, x, y);
            case POINTER_SUBTYPE_MOVE:
                return onMove(context, component, pointerId, x, y, consumed);
            case POINTER_SUBTYPE_UP:
                float velocityX = data.length > 4 ? data[3] : 0f;
                float velocityY = data.length > 4 ? data[4] : 0f;
                return onUp(context, component, pointerId, x, y, velocityX, velocityY);
            case POINTER_SUBTYPE_CANCEL:
                return onCancel(context, pointerId, x, y);
            default:
                return POINTER_STATUS_NOT_CONSUMED;
        }
    }

    private int onDown(@NonNull RemoteContext context, int pointerId, float x, float y) {
        if (isTracking()) {
            // Second pointer: ignore.
            return POINTER_STATUS_NOT_CONSUMED;
        }
        // Track the pointer even if a child consumed the down (Compose: requireUnconsumed =
        // false), so that a drag starting on a button still scrolls the list.
        mPointerId = pointerId;
        mScrolling = false;
        mDownX = mLastX = x;
        mDownY = mLastY = y;
        mInitialScrollX = mScrollX;
        mInitialScrollY = mScrollY;
        if (mEdgeEffectA != null) {
            mEdgeEffectA.reset();
        }
        if (mEdgeEffectB != null) {
            mEdgeEffectB.reset();
        }
        if (mTouchExpression != null) {
            // Stops any in-flight fling immediately, like Views and Compose.
            mTouchExpression.onPointerDown(context, x, y);
        }
        // Never consume the down: it is not ours until the slop is crossed.
        return POINTER_STATUS_NOT_CONSUMED;
    }

    private int onMove(
            @NonNull RemoteContext context,
            @NonNull Component component,
            int pointerId,
            float x,
            float y,
            boolean consumed) {
        if (!isTracking(pointerId)) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        if (consumed) {
            // A child (slider, inner scroll container) or an ancestor in the initial pass owns
            // the drag now.
            cancelGesture(context, x, y);
            return POINTER_STATUS_NOT_CONSUMED;
        }
        if (!mScrolling) {
            float mainAxisDelta = isVerticalScroll() ? y - mDownY : x - mDownX;
            if (Math.abs(mainAxisDelta) <= context.getTouchSlop()) {
                return POINTER_STATUS_NOT_CONSUMED;
            }
            mScrolling = true;
            // Re-anchor the drag at the current position so the content doesn't jump by the
            // slop distance on the first scrolled frame.
            mDownX = mLastX = x;
            mDownY = mLastY = y;
            mInitialScrollX = mScrollX;
            mInitialScrollY = mScrollY;
            if (mTouchExpression != null) {
                mTouchExpression.onPointerDown(context, x, y);
            }
            return POINTER_STATUS_CONSUMED;
        }
        drag(context, component, x, y);
        return POINTER_STATUS_CONSUMED;
    }

    private int onUp(
            @NonNull RemoteContext context,
            @NonNull Component component,
            int pointerId,
            float x,
            float y,
            float velocityX,
            float velocityY) {
        if (!isTracking(pointerId)) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        boolean wasScrolling = mScrolling;
        if (mTouchExpression != null) {
            mTouchExpression.onPointerUp(context, x, y, velocityX, velocityY);
        }
        releaseEdgeEffects(context);
        component.invalidateMeasure();
        endGesture();
        return wasScrolling ? POINTER_STATUS_CONSUMED : POINTER_STATUS_NOT_CONSUMED;
    }

    private int onCancel(@NonNull RemoteContext context, int pointerId, float x, float y) {
        if (!isTracking(pointerId)) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        boolean wasScrolling = mScrolling;
        cancelGesture(context, x, y);
        return wasScrolling ? POINTER_STATUS_CONSUMED : POINTER_STATUS_NOT_CONSUMED;
    }

    private void drag(
            @NonNull RemoteContext context, @NonNull Component component, float x, float y) {
        if (mTouchExpression != null) {
            mTouchExpression.onPointerMove(context, x, y);
        }
        float dx = x - mDownX;
        float dy = y - mDownY;
        float edx = x - mLastX;
        float edy = y - mLastY;
        mLastX = x;
        mLastY = y;

        if (!Utils.isVariable(mPositionExpression)) {
            // No touch expression drives the position: scroll directly.
            if (isVerticalScroll()) {
                mScrollY = Math.max(-mMaxScrollY, Math.min(0, mInitialScrollY + dy));
            } else {
                mScrollX = Math.max(-mMaxScrollX, Math.min(0, mInitialScrollX + dx));
            }
        }

        if (isVerticalScroll()) {
            if (mEdgeEffectA != null && mScrollY == 0 && edy > 0) {
                mEdgeEffectA.pull(edy, component.getHeight());
            }
            if (mEdgeEffectB != null && mScrollY == -mMaxScrollY && edy < 0) {
                mEdgeEffectB.pull(edy, component.getHeight());
            }
        } else {
            if (mEdgeEffectA != null && mScrollX == 0 && edx > 0) {
                mEdgeEffectA.pull(edx, component.getWidth());
            }
            if (mEdgeEffectB != null && mScrollX == -mMaxScrollX && edx < 0) {
                mEdgeEffectB.pull(edx, component.getWidth());
            }
        }
        component.invalidateMeasure();
    }

    private void cancelGesture(@NonNull RemoteContext context, float x, float y) {
        if (mTouchExpression != null) {
            mTouchExpression.onPointerCancel(context, x, y);
        }
        releaseEdgeEffects(context);
        endGesture();
    }

    private void releaseEdgeEffects(@NonNull RemoteContext context) {
        if (mEdgeEffectA != null) {
            mEdgeEffectA.release();
            context.needsRepaint();
        }
        if (mEdgeEffectB != null) {
            mEdgeEffectB.release();
            context.needsRepaint();
        }
    }

    private void endGesture() {
        mPointerId = NO_POINTER;
        mScrolling = false;
    }

    private boolean isTracking() {
        return mPointerId != NO_POINTER;
    }

    private boolean isTracking(int pointerId) {
        return isTracking() && mPointerId == pointerId;
    }

    /**
     * Returns whether a scroll drag is currently in progress.
     *
     * @return true once the pointer has moved beyond the touch slop and until it is lifted
     */
    public boolean isScrolling() {
        return mScrolling;
    }

    /**
     * Set the horizontal scroll dimension
     *
     * @param hostDimension the horizontal host dimension
     * @param contentDimension the horizontal content dimension
     */
    public void setHorizontalScrollDimension(float hostDimension, float contentDimension) {
        mHostDimension = hostDimension;
        mContentDimension = contentDimension;
        mMaxScrollX = Math.max(0, contentDimension - hostDimension);
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
        mMaxScrollY = Math.max(0, contentDimension - hostDimension);
    }

    /** Returns the content dimension along the scroll axis. */
    public float getContentDimension() {
        return mContentDimension;
    }

    @Override
    public float getScrollX(float currentValue) {
        return isHorizontalScroll() ? mScrollX : 0f;
    }

    @Override
    public float getScrollY(float currentValue) {
        return isVerticalScroll() ? mScrollY : 0f;
    }

    @Override
    public boolean handlesHorizontalScroll() {
        return isHorizontalScroll();
    }

    @Override
    public boolean handlesVerticalScroll() {
        return isVerticalScroll();
    }

    @Override
    public void reset() {
        // nothing here for now
    }

    @Override
    public void applyEdgeEffect(
            @NonNull PaintContext context, @NonNull Component component, int phase) {
        if (mEdgeEffectA == null) {
            if (isVerticalScroll()) {
                mEdgeEffectA = context.getContext().createEdgeEffect(ScrollingEdgeEffect.TOP);
                if (mEdgeEffectA != null) {
                    mEdgeEffectA.setSize(mWidth, mContentDimension);
                }
            } else {
                mEdgeEffectA = context.getContext().createEdgeEffect(ScrollingEdgeEffect.LEFT);
                if (mEdgeEffectA != null) {
                    mEdgeEffectA.setSize(mContentDimension, mHeight);
                }
            }
        }
        if (mEdgeEffectB == null) {
            if (isVerticalScroll()) {
                mEdgeEffectB = context.getContext().createEdgeEffect(ScrollingEdgeEffect.BOTTOM);
                if (mEdgeEffectB != null) {
                    mEdgeEffectB.setSize(mWidth, mContentDimension);
                }
            } else {
                mEdgeEffectB = context.getContext().createEdgeEffect(ScrollingEdgeEffect.RIGHT);
                if (mEdgeEffectB != null) {
                    mEdgeEffectB.setSize(mContentDimension, mHeight);
                }
            }
        }
        if (mEdgeEffectA != null) {
            mEdgeEffectA.apply(context, component, mContentDimension, phase);
        }
        if (mEdgeEffectB != null) {
            mEdgeEffectB.apply(context, component, mContentDimension, phase);
        }
    }

    @Override
    public float contentWidth() {
        return isHorizontalScroll() ? mContentDimension : mWidth;
    }

    @Override
    public float contentHeight() {
        return isVerticalScroll() ? mContentDimension : mHeight;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType(CLASS_NAME)
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
        return isVerticalScroll()
                ? ScrollableComponent.SCROLL_VERTICAL
                : ScrollableComponent.SCROLL_HORIZONTAL;
    }

    @Override
    public int scrollByOffset(@NonNull RemoteContext context, int offset) {
        // Set the scroll position through an override so the touch expression driving the
        // position adopts it (and the list stays draggable), rather than disabling touch.
        // mScrollX/mScrollY are stored negated: paint() does mScrollY = -position.
        if (isVerticalScroll()) {
            mScrollY = Math.max(-mMaxScrollY, Math.min(0, mScrollY + offset));
            context.overrideFloat(Utils.idFromNan(mPositionExpression), 0f - mScrollY);
        } else {
            mScrollX = Math.max(-mMaxScrollX, Math.min(0, mScrollX + offset));
            context.overrideFloat(Utils.idFromNan(mPositionExpression), 0f - mScrollX);
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
        child.getLocationInWindow(context, locationInWindow);

        int offset = (int) -(isVerticalScroll() ? locationInWindow[1] : locationInWindow[0]);
        if (offset == 0) {
            return true;
        }
        return scrollByOffset(context, offset) != 0;
    }

    @Nullable
    @Override
    public ScrollAxisRange getScrollAxisRange() {
        if (isVerticalScroll()) {
            return new ScrollAxisRange(mScrollY, mMaxScrollY, true, true);
        }
        return new ScrollAxisRange(mScrollX, mMaxScrollX, true, true);
    }

    /** Returns {@code mPositionExpression}. */
    public float getPositionExpression() {
        return mPositionExpression;
    }
}
// CPD-ON
