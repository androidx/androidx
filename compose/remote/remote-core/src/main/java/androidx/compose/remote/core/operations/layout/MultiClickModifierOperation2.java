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
package androidx.compose.remote.core.operations.layout;

// This class duplicates parts of MultiClickModifier until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_FINAL;
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
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ColorUtils;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.easing.Easing;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.semantics.AccessibleComponent;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi click modifier (single click, long press or double click) dispatched through the unified
 * pointer input router.
 *
 * <p>Same opcode and wire format as {@link MultiClickModifier}: each operation detects exactly one
 * gesture, selected by {@code clickType}. Gestures are detected in the <em>main</em> pass with
 * Compose-like semantics (a tap requires an unconsumed down; a consumed move — also observed in the
 * final pass — or a move outside the bounds cancels the press).
 *
 * <p>Several of these modifiers are typically stacked on one component (e.g. {@code
 * combinedClickable} emits {@code SINGLE, LONG, DOUBLE}). They coordinate through consumption,
 * relying on the main pass visiting a component's modifiers inner to outer, i.e. in <b>reverse</b>
 * list order:
 *
 * <ul>
 *   <li>{@code DOUBLE} consumes the <em>down</em> of the second tap, so a {@code SINGLE} placed
 *       before it in the list never starts for that tap.
 *   <li>{@code LONG} consumes the <em>up</em> once the long press has been performed, so a {@code
 *       SINGLE} placed before it in the list does not also click.
 *   <li>{@code SINGLE} behaves like {@link ClickModifierOperation2}.
 * </ul>
 *
 * <p>The long press is detected while the pointer is held, by polling the clock from {@link
 * #paint}; the modifier requests repaints while a press is pending, as the legacy player did in
 * its draw loop. If no frame was painted after the timeout elapsed, it is detected on release
 * instead.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MultiClickModifierOperation2 extends PaintOperation
        implements Container,
                ModifierOperation,
                DecoratorComponent,
                EventHandler,
                AccessibleComponent {
    private static final int OP_CODE = Operations.MODIFIER_MULTI_CLICK;

    public static final int CLICK_TYPE_SINGLE = 0;
    public static final int CLICK_TYPE_LONG = 1;
    public static final int CLICK_TYPE_DOUBLE = 2;

    private static final int NO_POINTER = -1;

    int mClickType = CLICK_TYPE_SINGLE;

    long mAnimateRippleStart = 0;
    float mAnimateRippleX = 0f;
    float mAnimateRippleY = 0f;
    int mAnimateRippleDuration = 1000;

    float mWidth = 0;
    float mHeight = 0;

    private @Nullable Component mComponent;
    private final @NonNull PaintBundle mPaint = new PaintBundle();

    // ---------------------------------------------------------------------------------------
    // Gesture state
    // ---------------------------------------------------------------------------------------
    private int mPointerId = NO_POINTER;
    private long mDownTimeMillis;
    private float mDownX;
    private float mDownY;

    /** LONG: set once the long press fired for the current press. */
    private boolean mLongPressPerformed;

    /** LONG: document of the press in flight, needed to run the actions from {@link #paint}. */
    private @Nullable CoreDocument mPressDocument;

    /** DOUBLE: whether a first tap is waiting for its second tap. */
    private boolean mHasFirstTap;

    /** DOUBLE: time / position of the up that ended the first tap. */
    private long mLastTapUpTimeMillis;

    private float mLastTapUpX;
    private float mLastTapUpY;

    /** DOUBLE: the current press is the second tap of a double tap. */
    private boolean mDoubleTapCandidate;

    public MultiClickModifierOperation2() {}

    public MultiClickModifierOperation2(int clickType) {
        mClickType = clickType;
    }

    @Override
    public boolean isClickable() {
        return true;
    }

    /**
     * Returns the click type: {@link #CLICK_TYPE_SINGLE}, {@link #CLICK_TYPE_LONG}, or {@link
     * #CLICK_TYPE_DOUBLE}.
     */
    public int getClickType() {
        return mClickType;
    }

    @Nullable
    @Override
    public Role getRole() {
        return Role.BUTTON;
    }

    @Override
    public CoreSemantics.@NonNull Mode getMode() {
        return CoreSemantics.Mode.MERGE;
    }

    /**
     * Animate ripple
     *
     * @param x starting position x of the ripple
     * @param y starting position y of the ripple
     * @param timeStampMillis animation start timestamp in milliseconds
     */
    public void animateRipple(float x, float y, long timeStampMillis) {
        mAnimateRippleStart = timeStampMillis;
        mAnimateRippleX = x;
        mAnimateRippleY = y;
    }

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mClickType);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClickModifier");
        if (mClickType != CLICK_TYPE_SINGLE) {
            sb.append(" type: " + mClickType);
        }
        for (Operation modifierOperation : mList) {
            sb.append("\n        ");
            sb.append(modifierOperation.toString());
        }
        return sb.toString();
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        for (Operation op : mList) {
            if (op instanceof TextData) {
                op.apply(context);
                context.incrementOpCount();
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        checkLongPress(context.getContext());

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
        context.applyPaint(mPaint);
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
        mComponent = component;
        mWidth = width;
        mHeight = height;
    }


    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        if (mClickType == CLICK_TYPE_SINGLE) {
            serializer.append(indent, "CLICK_MODIFIER");
        } else {
            serializer.append(indent, "CLICK_MODIFIER " + mClickType);
        }
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).serializeToString(indent + 1, serializer);
            }
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
     * Handles a pointer event and triggers the actions when the configured gesture completes.
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
        int pass = MetadataUtil.unpackPass(metadata);
        int subtype = MetadataUtil.unpackSubtype(metadata);
        boolean consumed = MetadataUtil.unpackConsumed(metadata) == POINTER_STATUS_CONSUMED;

        if (pass == POINTER_PASS_FINAL) {
            // Cancel by position consumption: an ancestor took over the gesture in its main pass.
            if (isPressed() && consumed && subtype == POINTER_SUBTYPE_MOVE) {
                cancelPress();
            }
            return POINTER_STATUS_NOT_CONSUMED;
        }
        if (pass != POINTER_PASS_MAIN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }

        float[] data = event.getData();
        if (data == null || data.length < 3) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int pointerId = (int) data[0];
        float x = data[1];
        float y = data[2];

        switch (subtype) {
            case POINTER_SUBTYPE_DOWN:
                return onDown(context, document, pointerId, x, y, consumed);
            case POINTER_SUBTYPE_MOVE:
                if (isPressed(pointerId) && (consumed || !inBounds(x, y))) {
                    // TODO(b/585250534): consider touch slop for beyond bounds calculation.
                    cancelPress();
                }
                return POINTER_STATUS_NOT_CONSUMED;
            case POINTER_SUBTYPE_UP:
                return onUp(context, document, pointerId, x, y, consumed);
            case POINTER_SUBTYPE_CANCEL:
                if (isPressed(pointerId)) {
                    cancelPress();
                }
                return POINTER_STATUS_NOT_CONSUMED;
            default:
                return POINTER_STATUS_NOT_CONSUMED;
        }
    }

    private int onDown(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            int pointerId,
            float x,
            float y,
            boolean consumed) {
        if (consumed || isPressed()) {
            // Claimed by a descendant or an inner modifier, or a second pointer: ignore.
            return POINTER_STATUS_NOT_CONSUMED;
        }
        long now = context.getClock().millis();
        mPointerId = pointerId;
        mDownTimeMillis = now;
        mDownX = x;
        mDownY = y;
        mLongPressPerformed = false;
        mDoubleTapCandidate = false;

        switch (mClickType) {
            case CLICK_TYPE_SINGLE:
                return POINTER_STATUS_CONSUMED;
            case CLICK_TYPE_LONG:
                mPressDocument = document;
                // Start polling the clock from paint().
                context.needsRepaint();
                return POINTER_STATUS_NOT_CONSUMED;
            case CLICK_TYPE_DOUBLE:
                mDoubleTapCandidate =
                        mHasFirstTap
                                && now - mLastTapUpTimeMillis <= context.getDoubleTapTimeoutMillis()
                                && isWithinDoubleTapSlop(context, x, y);
                // The second tap is ours; the first is only observed so that a single click
                // modifier underneath still fires.
                return mDoubleTapCandidate ? POINTER_STATUS_CONSUMED : POINTER_STATUS_NOT_CONSUMED;
            default:
                return POINTER_STATUS_NOT_CONSUMED;
        }
    }

    private int onUp(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            int pointerId,
            float x,
            float y,
            boolean consumed) {
        if (!isPressed(pointerId)) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int status = POINTER_STATUS_NOT_CONSUMED;
        switch (mClickType) {
            case CLICK_TYPE_SINGLE:
                if (!consumed && performAction(context, document, x, y)) {
                    context.hapticEffect(3);
                    status = POINTER_STATUS_CONSUMED;
                }
                break;
            case CLICK_TYPE_LONG:
                if (!mLongPressPerformed && isLongPressTimeoutElapsed(context)) {
                    // No frame was painted since the timeout elapsed: detect the long press on
                    // release, so that the gesture does not depend on frames being drawn.
                    performLongPress(context);
                }
                if (mLongPressPerformed) {
                    // Swallow the up so that a single click modifier underneath does not fire.
                    status = POINTER_STATUS_CONSUMED;
                }
                break;
            case CLICK_TYPE_DOUBLE:
                if (mDoubleTapCandidate) {
                    if (!consumed && performAction(context, document, x, y)) {
                        context.hapticEffect(3);
                        status = POINTER_STATUS_CONSUMED;
                    }
                    // A third tap starts over.
                    mHasFirstTap = false;
                } else {
                    mHasFirstTap = true;
                    mLastTapUpTimeMillis = context.getClock().millis();
                    mLastTapUpX = x;
                    mLastTapUpY = y;
                }
                break;
            default:
                break;
        }
        endPress();
        return status;
    }

    /**
     * Fires the long press if the pointer has been held long enough, and keeps requesting
     * repaints until then. Called from {@link #paint} while a long press is pending.
     *
     * @param context current remote context
     */
    private void checkLongPress(@NonNull RemoteContext context) {
        if (mClickType != CLICK_TYPE_LONG || !isPressed() || mLongPressPerformed) {
            return;
        }
        if (isLongPressTimeoutElapsed(context)) {
            performLongPress(context);
        } else {
            context.needsRepaint();
        }
    }

    private boolean isLongPressTimeoutElapsed(@NonNull RemoteContext context) {
        return context.getClock().millis() - mDownTimeMillis >= context.getLongPressTimeoutMillis();
    }

    /** Runs the long press actions for the press in flight, at the position of its down. */
    private void performLongPress(@NonNull RemoteContext context) {
        mLongPressPerformed = true;
        CoreDocument document = mPressDocument;
        if (document != null && performAction(context, document, mDownX, mDownY)) {
            context.hapticEffect(0);
        }
    }

    private void cancelPress() {
        mDoubleTapCandidate = false;
        endPress();
    }

    private void endPress() {
        mPointerId = NO_POINTER;
        mLongPressPerformed = false;
        mPressDocument = null;
    }

    private boolean isPressed() {
        return mPointerId != NO_POINTER;
    }

    private boolean isPressed(int pointerId) {
        return isPressed() && mPointerId == pointerId;
    }

    private boolean inBounds(float x, float y) {
        return x >= 0 && y >= 0 && x < mWidth && y < mHeight;
    }

    private boolean isWithinDoubleTapSlop(@NonNull RemoteContext context, float x, float y) {
        float slop = context.getDoubleTapSlop();
        float dx = x - mLastTapUpX;
        float dy = y - mLastTapUpY;
        return dx * dx + dy * dy < slop * slop;
    }

    /**
     * Performs a click that does not come from a pointer, for example an accessibility click.
     * Only a {@link #CLICK_TYPE_SINGLE} modifier reacts to it.
     *
     * <p>The ripple starts at the center of the component, as there is no pointer position.
     *
     * @param context current remote context
     * @param document document containing this component
     * @return true if the click was performed
     */
    public boolean performClick(@NonNull RemoteContext context, @NonNull CoreDocument document) {
        // TODO(b/586559163): replace this method with a new type of event for accessibility.
        //  matchesEvent returns true for that type too, and onEvent runs the click without
        //  the ripple when the subtype is click.
        if (mClickType != CLICK_TYPE_SINGLE
                || !performAction(context, document, mWidth / 2f, mHeight / 2f)) {
            return false;
        }
        context.hapticEffect(3);
        return true;
    }

    /**
     * Runs the actions and starts the ripple.
     *
     * @return false if the hosting component is not laid out or not visible
     */
    private boolean performAction(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        Component component = mComponent;
        if (component == null || !component.isVisible()) {
            return false;
        }
        if (context.isAnimationEnabled()) {
            // x and y are component local coordinates.
            animateRipple(x, y, context.getClock().millis());
        }
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).runAction(context, document);
            }
        }
        return true;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "ClickModifier";
    }

    /**
     * Write the operation on the buffer. The wire format is identical to {@link
     * MultiClickModifier}.
     *
     * @param buffer wire buffer to write to
     * @param clickType the gesture to detect
     */
    public static void apply(@NonNull WireBuffer buffer, int clickType) {
        buffer.start(OP_CODE);
        buffer.writeInt(clickType);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int clickType = buffer.readInt();
        operations.add(new MultiClickModifierOperation2(clickType));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, name())
                .description(
                        "MultiClick modifier. This operation contains"
                                + " a list of action operations executed on click")
                .field(
                        DocumentedOperation.INT,
                        "clickType",
                        "Type of click (0=single, 1=long, 2=double)");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("MultiClickModifier")
                .add("clickType", mClickType);
    }
}
// CPD-ON
