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

// This class duplicates parts of RippleModifierOperation until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN;
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
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ColorUtils;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.easing.Easing;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Ripple (press indication) modifier dispatched through the unified pointer input router.
 *
 * <p>Same opcode and (empty) wire format as {@link RippleModifierOperation}. Unlike the legacy
 * fire-and-forget animation, this ripple follows the press like Views and Compose do: it expands
 * from the down position while the pointer is held and fades out on up or cancel.
 *
 * <p>The modifier runs in the <em>main</em> pass like the other pointer input modifiers, never
 * consumes, and deliberately ignores the consumed state of the events: every {@code DOWN} inside
 * the bounds starts a ripple, whether or not a click modifier on the same component or a
 * clickable child claimed the pointer, and every ripple modifier on the hit path ripples. The
 * ripple is released on {@code UP}, {@code CANCEL}, or when the pointer leaves the bounds.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RippleModifierOperation2 extends DecoratorModifierOperation implements EventHandler {
    private static final int OP_CODE = Operations.MODIFIER_RIPPLE;

    /** Time for the ripple to grow from the touch point to its full radius. */
    static final int EXPAND_DURATION_MS = 300;

    /** Time for the ripple to fade out once the pointer is released. */
    static final int FADE_DURATION_MS = 300;

    private static final int NO_POINTER = -1;
    private static final int PRESSED_COLOR = ColorUtils.createColor(250, 250, 250, 180);
    private static final int RELEASED_COLOR = ColorUtils.createColor(200, 200, 200, 0);

    float mWidth = 0;
    float mHeight = 0;

    /** Clock time at which the current ripple started, 0 if none is showing. */
    long mPressStartMillis = 0;

    /** Clock time at which the pointer was released, 0 while still pressed. */
    long mReleaseStartMillis = 0;

    float mRippleX = 0f;
    float mRippleY = 0f;

    private int mPointerId = NO_POINTER;
    private final @NonNull PaintBundle mPaint = new PaintBundle();

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        return "RippleModifier";
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mPressStartMillis == 0) {
            return;
        }
        long now = context.getClock().millis();

        // Expansion: from the touch point to the full radius, continuing even if released early.
        float expand = Math.min(1f, (now - mPressStartMillis) / (float) EXPAND_DURATION_MS);
        FloatAnimation expandAnim =
                new FloatAnimation(Easing.CUBIC_STANDARD, 1f, null, Float.NaN, Float.NaN);
        expandAnim.setInitialValue(0f);
        expandAnim.setTargetValue(1f);
        float radius = Math.max(mWidth, mHeight) * expandAnim.get(expand);

        // Fade: starts on release.
        float fade = 0f;
        if (mReleaseStartMillis != 0) {
            fade = Math.min(1f, (now - mReleaseStartMillis) / (float) FADE_DURATION_MS);
        }
        boolean animating = expand < 1f || (mReleaseStartMillis != 0 && fade < 1f);
        if (animating) {
            context.needsRepaint();
        }
        if (mReleaseStartMillis != 0 && fade >= 1f) {
            // Done: draw nothing further.
            mPressStartMillis = 0;
            mReleaseStartMillis = 0;
            return;
        }

        context.save();
        context.savePaint();
        mPaint.reset();
        mPaint.setColor(Utils.interpolateColor(PRESSED_COLOR, RELEASED_COLOR, fade));
        context.replacePaint(mPaint);
        context.clipRect(0f, 0f, mWidth, mHeight);
        context.drawCircle(mRippleX, mRippleY, radius);
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
     * Write the operation to the buffer. The wire format is identical to {@link
     * RippleModifierOperation}.
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
        operations.add(new RippleModifierOperation2());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, name())
                .description(
                        "Ripple modifier. This modifier will do a ripple animation on touch down");
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
     * Observes pointer events and drives the press indication. Never consumes, and does not look
     * at the consumed state of the event.
     *
     * @param context current remote context
     * @param document document containing this component
     * @param event pointer event being dispatched
     * @return always {@link
     *     androidx.compose.remote.core.input.PointerInputRouter#POINTER_STATUS_NOT_CONSUMED}
     */
    @Override
    public int onEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {
        int metadata = event.getMetadata();
        if (MetadataUtil.unpackPass(metadata) != POINTER_PASS_MAIN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        float[] data = event.getData();
        if (data == null || data.length < 3) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int pointerId = (int) data[0];
        float x = data[1];
        float y = data[2];

        switch (MetadataUtil.unpackSubtype(metadata)) {
            case POINTER_SUBTYPE_DOWN:
                if (!isPressed() && inBounds(x, y)) {
                    press(context, pointerId, x, y);
                }
                break;
            case POINTER_SUBTYPE_MOVE:
                if (isPressed(pointerId) && !inBounds(x, y)) {
                    // TODO(b/585250534): consider touch slop for beyond bounds calculation.
                    release(context);
                }
                break;
            case POINTER_SUBTYPE_UP:
            case POINTER_SUBTYPE_CANCEL:
                if (isPressed(pointerId)) {
                    release(context);
                }
                break;
            default:
                break;
        }
        return POINTER_STATUS_NOT_CONSUMED;
    }

    private void press(@NonNull RemoteContext context, int pointerId, float x, float y) {
        mPointerId = pointerId;
        mRippleX = x;
        mRippleY = y;
        mPressStartMillis = context.getClock().millis();
        mReleaseStartMillis = 0;
        context.needsRepaint();
    }

    private void release(@NonNull RemoteContext context) {
        mPointerId = NO_POINTER;
        if (mPressStartMillis != 0) {
            mReleaseStartMillis = context.getClock().millis();
        }
        context.needsRepaint();
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

    /**
     * Returns whether a ripple is currently showing (expanding, held or fading).
     *
     * @return true while the ripple is animating or held
     */
    public boolean isRippleActive() {
        return mPressStartMillis != 0;
    }

    /**
     * Returns whether the pointer that started the ripple is still down.
     *
     * @return true between the down and the matching up / cancel
     */
    public boolean isPointerDown() {
        return isPressed();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("RippleModifierOperation")
                .add("pressStart", mPressStartMillis)
                .add("releaseStart", mReleaseStartMillis)
                .add("rippleX", mRippleX)
                .add("rippleY", mRippleY)
                .add("width", mWidth)
                .add("height", mHeight);
    }
}
// CPD-ON
