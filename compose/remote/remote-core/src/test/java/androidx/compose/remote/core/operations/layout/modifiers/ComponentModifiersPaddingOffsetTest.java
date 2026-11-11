/*
 * Copyright 2026 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.ClickHandler;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.TouchHandler;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.testing.HeadlessRemoteContext;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verifies that every gesture dispatched through {@link ComponentModifiers} converts the
 * component-local coordinates into the handler's local space by subtracting the paddings that
 * precede the handler in the modifier chain, and nothing else.
 */
@RunWith(JUnit4.class)
public class ComponentModifiersPaddingOffsetTest {

    private static final float EPSILON = 1e-5f;

    private static final float PADDING_LEFT = 10f;
    private static final float PADDING_TOP = 20f;
    private static final float TOUCH_X = 50f;
    private static final float TOUCH_Y = 60f;

    /** Minimal stub modifier that records the coordinates dispatched for a target gesture. */
    private static final class RecordingModifier extends Operation
            implements ModifierOperation, ClickHandler, TouchHandler {
        private final Gesture mTargetGesture;
        int mCount = 0;
        float mX = Float.NaN;
        float mY = Float.NaN;

        RecordingModifier(Gesture targetGesture) {
            mTargetGesture = targetGesture;
        }

        private boolean record(Gesture gesture, float x, float y) {
            if (mTargetGesture != gesture) {
                return false;
            }
            mCount++;
            mX = x;
            mY = y;
            return true;
        }

        @Override
        public boolean onClick(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.CLICK, x, y);
        }

        @Override
        public boolean onLongPress(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.LONG_PRESS, x, y);
        }

        @Override
        public boolean onDoubleClick(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.DOUBLE_CLICK, x, y);
        }

        @Override
        public boolean onTouchDown(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.TOUCH_DOWN, x, y);
        }

        @Override
        public boolean onTouchUp(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y,
                float dx,
                float dy) {
            return record(Gesture.TOUCH_UP, x, y);
        }

        @Override
        public boolean onTouchDrag(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.TOUCH_DRAG, x, y);
        }

        @Override
        public boolean onTouchCancel(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            return record(Gesture.TOUCH_CANCEL, x, y);
        }

        @Override
        public void serializeToString(int indent, @NonNull StringSerializer serializer) {}

        @Override
        public void serialize(@NonNull MapSerializer serializer) {}

        @Override
        public void write(@NonNull WireBuffer buffer) {}

        @Override
        public void apply(@NonNull RemoteContext context) {}

        @NonNull
        @Override
        public String deepToString(@NonNull String indent) {
            return indent + "RecordingModifier";
        }
    }

    private RemoteContext mContext;
    private CoreDocument mDocument;
    private Component mComponent;

    @Before
    public void setUp() {
        mContext = new HeadlessRemoteContext();
        mDocument = new CoreDocument();
        mComponent = new Component(1, 0f, 0f, 200f, 200f, null);
    }

    private static ComponentModifiers paddingThen(ModifierOperation handler) {
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(handler);
        return modifiers;
    }

    private static void assertOffsetApplied(RecordingModifier handler) {
        assertThat(handler.mCount).isEqualTo(1);
        assertThat(handler.mX).isWithin(EPSILON).of(TOUCH_X - PADDING_LEFT);
        assertThat(handler.mY).isWithin(EPSILON).of(TOUCH_Y - PADDING_TOP);
    }

    private static void assertNoOffset(RecordingModifier handler) {
        assertThat(handler.mCount).isEqualTo(1);
        assertThat(handler.mX).isWithin(EPSILON).of(TOUCH_X);
        assertThat(handler.mY).isWithin(EPSILON).of(TOUCH_Y);
    }

    // ---------------------------------------------------------------------------------------------
    // Click handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void onClick_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.CLICK);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(modifiers.onClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y)).isTrue();

        assertOffsetApplied(handler);
    }

    @Test
    public void onLongPress_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.LONG_PRESS);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(modifiers.onLongPress(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y))
                .isTrue();

        assertOffsetApplied(handler);
    }

    @Test
    public void onDoubleClick_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.DOUBLE_CLICK);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(modifiers.onDoubleClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y))
                .isTrue();

        assertOffsetApplied(handler);
    }

    // ---------------------------------------------------------------------------------------------
    // Touch handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void onTouchDown_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.TOUCH_DOWN);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(modifiers.onTouchDown(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y))
                .isTrue();

        assertOffsetApplied(handler);
    }

    @Test
    public void onTouchUp_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.TOUCH_UP);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(
                        modifiers.onTouchUp(
                                mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y, 0f, 0f))
                .isTrue();

        assertOffsetApplied(handler);
    }

    @Test
    public void onTouchCancel_subtractsPrecedingPadding() {
        RecordingModifier handler = new RecordingModifier(Gesture.TOUCH_CANCEL);
        ComponentModifiers modifiers = paddingThen(handler);

        assertThat(modifiers.onTouchCancel(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y))
                .isTrue();

        assertOffsetApplied(handler);
    }

    // ---------------------------------------------------------------------------------------------
    // Ordering semantics shared by all handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void paddingAfterHandler_isNotSubtracted() {
        RecordingModifier longPress = new RecordingModifier(Gesture.LONG_PRESS);
        RecordingModifier doubleClick = new RecordingModifier(Gesture.DOUBLE_CLICK);
        RecordingModifier cancel = new RecordingModifier(Gesture.TOUCH_CANCEL);
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(longPress);
        modifiers.add(doubleClick);
        modifiers.add(cancel);
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));

        modifiers.onLongPress(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);
        modifiers.onDoubleClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);
        modifiers.onTouchCancel(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);

        assertNoOffset(longPress);
        assertNoOffset(doubleClick);
        assertNoOffset(cancel);
    }

    @Test
    public void multiplePrecedingPaddings_accumulate() {
        RecordingModifier longPress = new RecordingModifier(Gesture.LONG_PRESS);
        RecordingModifier click = new RecordingModifier(Gesture.CLICK);
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(new PaddingModifierOperation(5f, 7f, 0f, 0f));
        modifiers.add(longPress);
        modifiers.add(click);

        modifiers.onLongPress(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);
        modifiers.onClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);

        // Long press and click sit behind the same paddings, so they must see the same point.
        assertThat(longPress.mCount).isEqualTo(1);
        assertThat(click.mCount).isEqualTo(1);
        assertThat(longPress.mX).isWithin(EPSILON).of(TOUCH_X - PADDING_LEFT - 5f);
        assertThat(longPress.mY).isWithin(EPSILON).of(TOUCH_Y - PADDING_TOP - 7f);
        assertThat(click.mX).isWithin(EPSILON).of(longPress.mX);
        assertThat(click.mY).isWithin(EPSILON).of(longPress.mY);
    }

    @Test
    public void handlersBetweenPaddings_seeOnlyEarlierPaddings() {
        RecordingModifier first = new RecordingModifier(Gesture.DOUBLE_CLICK);
        RecordingModifier second = new RecordingModifier(Gesture.DOUBLE_CLICK);
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(first);
        modifiers.add(new PaddingModifierOperation(5f, 7f, 0f, 0f));
        modifiers.add(second);

        modifiers.onDoubleClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);

        assertThat(first.mCount).isEqualTo(1);
        assertThat(second.mCount).isEqualTo(1);
        assertThat(first.mX).isWithin(EPSILON).of(TOUCH_X - PADDING_LEFT);
        assertThat(first.mY).isWithin(EPSILON).of(TOUCH_Y - PADDING_TOP);
        assertThat(second.mX).isWithin(EPSILON).of(TOUCH_X - PADDING_LEFT - 5f);
        assertThat(second.mY).isWithin(EPSILON).of(TOUCH_Y - PADDING_TOP - 7f);
    }

    private enum Gesture {
        CLICK,
        LONG_PRESS,
        DOUBLE_CLICK,
        TOUCH_DOWN,
        TOUCH_UP,
        TOUCH_CANCEL,
        TOUCH_DRAG,
    }
}
