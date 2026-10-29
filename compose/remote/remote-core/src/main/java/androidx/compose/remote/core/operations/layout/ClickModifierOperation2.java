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
package androidx.compose.remote.core.operations.layout;

// This class duplicates parts of ClickModifierOperation until that class is removed.
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
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.LayoutManager;
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
 * Click modifier operation dispatched through the unified pointer input router.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClickModifierOperation2 extends PaintOperation
        implements Container,
                ModifierOperation,
                DecoratorComponent,
                EventHandler,
                AccessibleComponent {
    private static final int OP_CODE = Operations.MODIFIER_CLICK;

    Component mComponent = null;
    long mAnimateRippleStart = 0;
    float mAnimateRippleX = 0f;
    float mAnimateRippleY = 0f;
    int mAnimateRippleDuration = 1000;

    float mWidth = 0;
    float mHeight = 0;

    private boolean mReceivedDown = false;
    private final float[] mLocationInWindow = new float[2];
    private final PaintBundle mPaint = new PaintBundle();

    /**
     * Returns whether this component is clickable.
     *
     * @return true as click modifier makes the component clickable
     */
    @Override
    public boolean isClickable() {
        return true;
    }

    /**
     * Returns the accessibility role.
     *
     * @return button accessibility role
     */
    @Nullable
    @Override
    public Role getRole() {
        return Role.BUTTON;
    }

    /**
     * Returns the accessibility node merge mode.
     *
     * @return merge mode
     */
    @Override
    public CoreSemantics.@NonNull Mode getMode() {
        return CoreSemantics.Mode.MERGE;
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
     * Handles a pointer event and triggers click actions on pointer up.
     *
     * <p>The gesture is detected in the main pass. Like {@code waitForUpOrCancellation} in
     * Compose, the final pass of each move is also inspected: a parent (e.g. a scroll container)
     * that starts a drag consumes the move in its own main pass, which runs <em>after</em> this
     * child's main pass, so the consumption is only visible here in the final pass.
     *
     * @param context current remote context
     * @param document document containing this component
     * @param event pointer event being dispatched
     * @return status indicating whether the event was consumed
     */
    @Override
    public int onEvent(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull Event event) {
        int metadata = event.getMetadata();
        int pass = MetadataUtil.unpackPass(metadata);
        int subtype = MetadataUtil.unpackSubtype(metadata);
        boolean consumed = MetadataUtil.unpackConsumed(metadata) == POINTER_STATUS_CONSUMED;

        if (pass == POINTER_PASS_FINAL) {
            // Cancel by position consumption: an ancestor took over the gesture in its main pass.
            if (mReceivedDown && consumed && subtype == POINTER_SUBTYPE_MOVE) {
                mReceivedDown = false;
            }
            return POINTER_STATUS_NOT_CONSUMED;
        }
        if (pass != POINTER_PASS_MAIN) return POINTER_STATUS_NOT_CONSUMED;

        float[] data = event.getData();
        assert data.length >= 3;
        float x = data[1];
        float y = data[2];

        int eventStatus = POINTER_STATUS_NOT_CONSUMED;
        switch (subtype) {
            case POINTER_SUBTYPE_DOWN:
                if (!consumed) {
                    mReceivedDown = true;
                    eventStatus |= POINTER_STATUS_CONSUMED;
                }
                break;
            case POINTER_SUBTYPE_UP:
                if (mReceivedDown && !consumed && onClick(context, document, x, y)) {
                    eventStatus |= POINTER_STATUS_CONSUMED;
                }
                mReceivedDown = false;
                break;
            case POINTER_SUBTYPE_MOVE:
                // A move consumed before reaching us (by a descendant, or by an ancestor in the
                // initial pass) or leaving the bounds cancels the click.
                if (mReceivedDown && (consumed || !inBounds(x, y))) {
                    // TODO(b/585250534): consider touch slop for beyond bounds calculation.
                    mReceivedDown = false;
                }
                break;
            case POINTER_SUBTYPE_CANCEL:
                mReceivedDown = false;
                break;
            default:
                break;
        }
        return eventStatus;
    }

    /**
     * Returns whether coordinates fall within the component bounds.
     *
     * @param x x coordinate relative to component
     * @param y y coordinate relative to component
     * @return true if coordinates are inside component bounds
     */
    private boolean inBounds(float x, float y) {
        return x >= 0 && y >= 0 && x < mWidth && y < mHeight;
    }

    /**
     * Starts a ripple animation from the given position.
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

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    /**
     * Returns the list of child action operations.
     *
     * @return list of operations
     */
    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    /**
     * Writes this operation to the wire buffer.
     *
     * @param buffer wire buffer to write to
     */
    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClickModifier");
        for (Operation modifierOperation : mList) {
            sb.append("\n        ");
            sb.append(modifierOperation.toString());
        }
        return sb.toString();
    }

    /**
     * Applies this operation to the remote context.
     *
     * @param context remote context to update
     */
    @Override
    public void apply(@NonNull RemoteContext context) {
        // The pointer input router does not use the registered handlers: it walks the component
        // tree to find them. This handler is still registered, for consistency.
        if (context.getMode() == RemoteContext.ContextMode.DATA) {
            context.getEventManager().registerHandler(this);
        }
        for (Operation op : mList) {
            if (op instanceof TextData) {
                op.apply(context);
                context.incrementOpCount();
            }
        }
    }

    /**
     * Returns an indented string representation of this modifier.
     *
     * @param indent prefix indentation string
     * @return formatted string representation
     */
    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    /**
     * Paints the click ripple effect if currently animating.
     *
     * @param context paint context to draw on
     */
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
        context.applyPaint(mPaint);
        context.clipRect(0f, 0f, mWidth, mHeight);
        context.drawCircle(mAnimateRippleX, mAnimateRippleY, radius);
        context.restorePaint();
        context.restore();
    }

    /**
     * Updates layout dimensions and parent component reference.
     *
     * @param context remote context
     * @param component parent component
     * @param width component width
     * @param height component height
     */
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

    /**
     * Serializes this operation to a formatted string.
     *
     * @param indent indentation level
     * @param serializer string serializer to append to
     */
    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "CLICK_MODIFIER");
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).serializeToString(indent + 1, serializer);
            }
        }
    }

    /**
     * Performs a click that does not come from a pointer, for example an accessibility click.
     *
     * <p>The ripple starts at the center of the component, as there is no pointer position.
     *
     * @param context remote context
     * @param document current core document
     * @return true if the click was performed, false if the component is not laid out or not
     *     visible
     */
    public boolean performClick(@NonNull RemoteContext context, @NonNull CoreDocument document) {
        // TODO(b/586559163): replace this method with a new type of event for accessibility.
        //  matchesEvent returns true for that type too, and onEvent calls onClick without the
        //  ripple when the subtype is click.
        Component component = mComponent;
        if (component == null || !component.isVisible()) {
            return false;
        }
        return onClick(context, document, mWidth / 2f, mHeight / 2f);
    }

    /**
     * Executes click actions, animates ripple, and triggers haptic feedback.
     *
     * @param context remote context
     * @param document current core document
     * @param x click x coordinate
     * @param y click y coordinate
     * @return true if click was handled
     */
    private boolean onClick(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            float x,
            float y) {
        if (context.getTouchVersion() == LayoutManager.FIX_TOUCH_EVENT) {
            if (context.isAnimationEnabled()) {
                // x and y are already content-relative coordinates
                animateRipple(x, y, context.getClock().millis());
            }
        } else if (mComponent != null) {
            mLocationInWindow[0] = 0f;
            mLocationInWindow[1] = 0f;
            mComponent.getLocationInWindow(context, mLocationInWindow);
            if (context.isAnimationEnabled()) {
                animateRipple(
                        x - mLocationInWindow[0],
                        y - mLocationInWindow[1],
                        context.getClock().millis());
            }
        }
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).runAction(context, document);
            }
        }
        context.hapticEffect(3);
        return true;
    }

    /**
     * Returns the name of the operation.
     *
     * @return operation name
     */
    @NonNull
    public static String name() {
        return "ClickModifier";
    }

    /**
     * Writes the operation opcode to the wire buffer.
     *
     * @param buffer wire buffer to write to
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Reads a click modifier operation from the wire buffer.
     *
     * @param buffer wire buffer to read from
     * @param operations list of operations to append to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new ClickModifierOperation2());
    }

    /**
     * Populates documentation with a description of this operation.
     *
     * @param doc documentation builder to append to
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, name())
                .description(
                        "Click modifier. This operation contains"
                                + " a list of action operations executed on click");
    }

    /**
     * Serializes this operation to a map serializer.
     *
     * @param serializer map serializer to write to
     */
    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addTags(SerializeTags.MODIFIER).addType("ClickModifierOperation");
    }
}
// CPD-ON
