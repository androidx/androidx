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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.ActionOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ListActionsOperation;
import androidx.compose.remote.core.operations.layout.MultiClickModifier;
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation;
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

    /**
     * Records the coordinates it is invoked with. Extends {@link Operation} because click and touch
     * modifiers keep their actions in an {@code ArrayList<Operation>} and dispatch to the ones that
     * implement {@link ActionOperation}.
     */
    private static final class RecordingAction extends Operation implements ActionOperation {
        int mCount = 0;
        float mX = Float.NaN;
        float mY = Float.NaN;

        @Override
        public void runAction(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Component component,
                float x,
                float y) {
            mCount++;
            mX = x;
            mY = y;
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
            return indent + "RecordingAction";
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

    private static MultiClickModifier clickModifier(int clickType, RecordingAction action) {
        MultiClickModifier modifier = new MultiClickModifier(clickType);
        modifier.mList.add(action);
        return modifier;
    }

    private static <T extends ListActionsOperation> T withAction(
            T modifier, RecordingAction action) {
        modifier.mList.add(action);
        return modifier;
    }

    private static ComponentModifiers paddingThen(ModifierOperation handler) {
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(handler);
        return modifiers;
    }

    private static void assertOffsetApplied(RecordingAction action) {
        assertEquals(1, action.mCount);
        assertEquals(TOUCH_X - PADDING_LEFT, action.mX, EPSILON);
        assertEquals(TOUCH_Y - PADDING_TOP, action.mY, EPSILON);
    }

    private static void assertNoOffset(RecordingAction action) {
        assertEquals(1, action.mCount);
        assertEquals(TOUCH_X, action.mX, EPSILON);
        assertEquals(TOUCH_Y, action.mY, EPSILON);
    }

    // ---------------------------------------------------------------------------------------------
    // Click handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void onClick_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(clickModifier(MultiClickModifier.CLICK_TYPE_SINGLE, action));

        assertTrue(modifiers.onClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y));

        assertOffsetApplied(action);
    }

    @Test
    public void onLongPress_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(clickModifier(MultiClickModifier.CLICK_TYPE_LONG, action));

        assertTrue(modifiers.onLongPress(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y));

        assertOffsetApplied(action);
    }

    @Test
    public void onDoubleClick_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(clickModifier(MultiClickModifier.CLICK_TYPE_DOUBLE, action));

        assertTrue(modifiers.onDoubleClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y));

        assertOffsetApplied(action);
    }

    // ---------------------------------------------------------------------------------------------
    // Touch handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void onTouchDown_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(withAction(new TouchDownModifierOperation(), action));

        assertTrue(modifiers.onTouchDown(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y));

        assertOffsetApplied(action);
    }

    @Test
    public void onTouchUp_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(withAction(new TouchUpModifierOperation(), action));

        assertTrue(modifiers.onTouchUp(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y, 0f, 0f));

        assertOffsetApplied(action);
    }

    @Test
    public void onTouchCancel_subtractsPrecedingPadding() {
        RecordingAction action = new RecordingAction();
        ComponentModifiers modifiers =
                paddingThen(withAction(new TouchCancelModifierOperation(), action));

        assertTrue(modifiers.onTouchCancel(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y));

        assertOffsetApplied(action);
    }

    // ---------------------------------------------------------------------------------------------
    // Ordering semantics shared by all handlers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void paddingAfterHandler_isNotSubtracted() {
        RecordingAction longPress = new RecordingAction();
        RecordingAction doubleClick = new RecordingAction();
        RecordingAction cancel = new RecordingAction();
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_LONG, longPress));
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_DOUBLE, doubleClick));
        modifiers.add(withAction(new TouchCancelModifierOperation(), cancel));
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
        RecordingAction longPress = new RecordingAction();
        RecordingAction click = new RecordingAction();
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(new PaddingModifierOperation(5f, 7f, 0f, 0f));
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_LONG, longPress));
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_SINGLE, click));

        modifiers.onLongPress(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);
        modifiers.onClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);

        // Long press and click sit behind the same paddings, so they must see the same point.
        assertEquals(TOUCH_X - PADDING_LEFT - 5f, longPress.mX, EPSILON);
        assertEquals(TOUCH_Y - PADDING_TOP - 7f, longPress.mY, EPSILON);
        assertEquals(click.mX, longPress.mX, EPSILON);
        assertEquals(click.mY, longPress.mY, EPSILON);
    }

    @Test
    public void handlersBetweenPaddings_seeOnlyEarlierPaddings() {
        RecordingAction first = new RecordingAction();
        RecordingAction second = new RecordingAction();
        ComponentModifiers modifiers = new ComponentModifiers();
        modifiers.add(new PaddingModifierOperation(PADDING_LEFT, PADDING_TOP, 0f, 0f));
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_DOUBLE, first));
        modifiers.add(new PaddingModifierOperation(5f, 7f, 0f, 0f));
        modifiers.add(clickModifier(MultiClickModifier.CLICK_TYPE_DOUBLE, second));

        modifiers.onDoubleClick(mContext, mDocument, mComponent, TOUCH_X, TOUCH_Y);

        assertEquals(TOUCH_X - PADDING_LEFT, first.mX, EPSILON);
        assertEquals(TOUCH_Y - PADDING_TOP, first.mY, EPSILON);
        assertEquals(TOUCH_X - PADDING_LEFT - 5f, second.mX, EPSILON);
        assertEquals(TOUCH_Y - PADDING_TOP - 7f, second.mY, EPSILON);
    }
}
