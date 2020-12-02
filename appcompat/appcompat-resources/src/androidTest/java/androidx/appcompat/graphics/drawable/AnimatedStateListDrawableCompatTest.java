/*
 * Copyright 2019 The Android Open Source Project
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.StateSet;

import androidx.appcompat.resources.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AnimatedStateListDrawableCompatTest {
    private static final int[] STATE_EMPTY = new int[]{};
    private static final int[] STATE_FOCUSED = new int[]{android.R.attr.state_focused};

    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
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
        assertEquals(0, asld.getStateCount());

        try {
            asld.addState(StateSet.WILD_CARD, null, R.id.focused);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        Drawable unfocused = new MockDrawable();
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.focused);
        assertEquals(1, asld.getStateCount());

        Drawable focused = new MockDrawable();
        asld.addState(STATE_FOCUSED, focused, R.id.unfocused);
        assertEquals(2, asld.getStateCount());
    }

    @Test
    public void testAddTransition() {
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();

        Drawable focused = new MockDrawable();
        Drawable unfocused = new MockDrawable();
        asld.addState(STATE_FOCUSED, focused, R.id.focused);
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused);

        try {
            asld.addTransition(R.id.focused, R.id.focused, null, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected.
        }

        MockTransition focusedToUnfocused = new MockTransition();
        asld.addTransition(R.id.focused, R.id.unfocused, focusedToUnfocused, false);
        assertEquals(3, asld.getStateCount());

        MockTransition unfocusedToFocused = new MockTransition();
        asld.addTransition(R.id.unfocused, R.id.focused, unfocusedToFocused, false);
        assertEquals(4, asld.getStateCount());

        MockTransition reversible = new MockTransition();
        asld.addTransition(R.id.focused, R.id.unfocused, reversible, true);
        assertEquals(5, asld.getStateCount());
    }

    @Test
    public void testIsStateful() {
        assertTrue(new AnimatedStateListDrawableCompat().isStateful());
    }

    @Test
    public void testOnStateChange() {
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();

        Drawable focused = new MockDrawable();
        Drawable unfocused = new MockDrawable();
        asld.addState(STATE_FOCUSED, focused, R.id.focused);
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused);

        MockTransition focusedToUnfocused = new MockTransition();
        MockTransition unfocusedToFocused = new MockTransition();
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
        assertTrue(asld.isVisible());
        // Missing public API to verify these.
        //assertFalse(asld.isConstantSize());
        //assertNull(asld.getConstantPadding());
        // Check that 4 drawables were parsed
        assertEquals(4, asld.getStateCount());
    }

    @Test
    public void testAnimatedVectorTransition() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.asl_heart, mContext.getTheme());
        // Check that 4 drawables were parsed
        assertEquals(4, asld.getStateCount());
    }

    @Test
    public void testChildAnimatedVectorTransition() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.animated_state_list_with_avd, mContext.getTheme());
        // Check that 6 drawables were parsed
        assertEquals(6, asld.getStateCount());
    }

    @Test
    public void testChildVectorItem() {
        AnimatedStateListDrawableCompat asld = AnimatedStateListDrawableCompat.create(mContext,
                R.drawable.asl_heart_embedded, mContext.getTheme());
        // Check that 4 drawables were parsed
        assertEquals(4, asld.getStateCount());
    }

    @Test
    public void testConstantStateWhenChildHasNullConstantState() {
        // Given an empty ASLD which returns a constant state
        AnimatedStateListDrawableCompat asld = new AnimatedStateListDrawableCompat();
        assertNotNull(asld.getConstantState());

        // When a drawable who returns a null constant state is added
        // MockDrawable returns null from getConstantState() - same as Drawable's default impl
        Drawable noConstantStateDrawable = new MockDrawable();
        asld.addState(StateSet.WILD_CARD, noConstantStateDrawable, R.id.focused);

        // Then the ASLD should also return a null constant state
        assertNull(asld.getConstantState());
    }

    public class MockTransition extends MockDrawable implements Animatable,
            Animatable2Compat {
        private HashSet<AnimationCallback> mCallbacks = new HashSet<>();

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }

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
