/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.appcompat.graphics.drawable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.StateSet;

import androidx.appcompat.test.R;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AnimatedStateListDrawableCompatTest {
    private static final int[] STATE_EMPTY = new int[]{};
    private static final int[] STATE_FOCUSED = new int[]{android.R.attr.state_focused};

    private Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testStateListDrawable() {
        new AnimatedStateListDrawableCompat();

        // Check the values set in the constructor
        assertNotNull(new AnimatedStateListDrawableCompat().getConstantState());
    }

    @Test
    public void testAddState() {
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();
        DrawableContainer.DrawableContainerState cs =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();
        assertEquals(0, cs.getChildCount());

        try {
            asld.addState(StateSet.WILD_CARD, null, R.id.focused);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        Drawable unfocused = mock(Drawable.class);
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.focused);
        assertEquals(1, cs.getChildCount());

        Drawable focused = mock(Drawable.class);
        asld.addState(STATE_FOCUSED, focused, R.id.unfocused);
        assertEquals(2, cs.getChildCount());
    }

    @Test
    public void testAddTransition() {
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();
        DrawableContainer.DrawableContainerState cs =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();

        Drawable focused = mock(Drawable.class);
        Drawable unfocused = mock(Drawable.class);
        asld.addState(STATE_FOCUSED, focused, R.id.focused);
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused);

        try {
            asld.addTransition(R.id.focused, R.id.focused, null, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        MockTransition focusedToUnfocused = mock(MockTransition.class);
        asld.addTransition(R.id.focused, R.id.unfocused, focusedToUnfocused, false);
        assertEquals(3, cs.getChildCount());

        MockTransition unfocusedToFocused = mock(MockTransition.class);
        asld.addTransition(R.id.unfocused, R.id.focused, unfocusedToFocused, false);
        assertEquals(4, cs.getChildCount());

        MockTransition reversible = mock(MockTransition.class);
        asld.addTransition(R.id.focused, R.id.unfocused, reversible, true);
        assertEquals(5, cs.getChildCount());
    }

    @Test
    public void testIsStateful() {
        assertTrue(new AnimatedStateListDrawableCompat().isStateful());
    }

    @Test
    public void testOnStateChange() {
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();

        Drawable focused = mock(Drawable.class);
        Drawable unfocused = mock(Drawable.class);
        asld.addState(STATE_FOCUSED, focused, R.id.focused);
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused);

        MockTransition focusedToUnfocused = mock(MockTransition.class);
        MockTransition unfocusedToFocused = mock(MockTransition.class);
        asld.addTransition(R.id.focused, R.id.unfocused, focusedToUnfocused, false);
        asld.addTransition(R.id.unfocused, R.id.focused, unfocusedToFocused, false);

        asld.setState(STATE_EMPTY);
        assertSame(unfocused, asld.getCurrent());

        asld.setState(STATE_FOCUSED);
        assertSame(unfocusedToFocused, asld.getCurrent());

        asld.setState(STATE_FOCUSED);
        assertSame(unfocusedToFocused, asld.getCurrent());
    }

    @Test
    public void testAnimationDrawableTransition() throws XmlPullParserException, IOException {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.animated_state_list_density, mContext.getTheme());
        DrawableContainer.DrawableContainerState asldState =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();
        assertTrue(asld.isVisible());
        assertFalse(asldState.isConstantSize());
        assertNull(asldState.getConstantPadding());
        // Check that 4 drawables were parsed
        assertEquals(4, asldState.getChildCount());
    }

    @Test
    public void testAnimatedVectorTransition() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.asl_heart, mContext.getTheme());
        DrawableContainer.DrawableContainerState asldState =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();
        // Check that 4 drawables were parsed
        assertEquals(4, asldState.getChildCount());
    }

    @Test
    public void testChildAnimatedVectorTransition() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.animated_state_list_with_avd, mContext.getTheme());
        DrawableContainer.DrawableContainerState asldState =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();
        // Check that 6 drawables were parsed
        assertEquals(6, asldState.getChildCount());
    }

    @Test
    public void testChildVectorItem() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.asl_heart_embedded, mContext.getTheme());
        DrawableContainer.DrawableContainerState asldState =
                (DrawableContainer.DrawableContainerState) asld.getConstantState();
        // Check that 4 drawables were parsed
        assertEquals(4, asldState.getChildCount());
    }

    public abstract class MockTransition extends MockDrawable implements Animatable,
            Animatable2Compat {
        private HashSet<AnimationCallback> mCallbacks = new HashSet<>();

        @Override
        public void registerAnimationCallback(AnimationCallback callback) {
            mCallbacks.add(callback);
        }

        @Override
        public boolean unregisterAnimationCallback(AnimationCallback callback) {
            return mCallbacks.remove(callback);
        }

        @Override
        public void clearAnimationCallbacks() {
            mCallbacks.clear();
        }
    }

    public class MockDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
