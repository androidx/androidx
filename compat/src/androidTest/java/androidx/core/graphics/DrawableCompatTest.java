/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.core.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.graphics.drawable.DrawableCompat;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DrawableCompatTest {
    @Test
    public void testDrawableWrap() {
        final Drawable original = new GradientDrawable();
        final Drawable wrappedDrawable = DrawableCompat.wrap(original);

        if (Build.VERSION.SDK_INT < 23) {
            assertNotSame(original, wrappedDrawable);
        } else {
            assertSame(original, wrappedDrawable);
        }
    }

    @Test
    public void testDrawableUnwrap() {
        final Drawable original = new GradientDrawable();
        final Drawable wrappedDrawable = DrawableCompat.wrap(original);
        assertSame(original, DrawableCompat.unwrap(wrappedDrawable));
    }

    @Test
    public void testDrawableChangeBoundsCopy() {
        final Rect bounds = new Rect(0, 0, 10, 10);

        final Drawable drawable = new GradientDrawable();

        final Drawable wrapper = DrawableCompat.wrap(drawable);
        wrapper.setBounds(bounds);

        // Assert that the bounds were given to the original drawable
        assertEquals(bounds, drawable.getBounds());
    }

    @Test
    public void testWrapCopiesDrawableState() {
        final Rect bounds = new Rect(0, 0, 10, 10);

        // Create a drawable and set some bounds
        final Drawable drawable = new GradientDrawable();
        drawable.setBounds(bounds);

        // Now wrap it
        final Drawable wrapper = DrawableCompat.wrap(drawable);

        // Assert that the bounds were copied to the wrapper
        assertEquals(bounds, wrapper.getBounds());
    }

    @Test
    public void testDrawableWrapOnlyWrapsOnce() {
        final Drawable wrappedDrawable = DrawableCompat.wrap(new GradientDrawable());
        assertSame(wrappedDrawable, DrawableCompat.wrap(wrappedDrawable));
    }

    @Test
    public void testWrapMutatedDrawableHasConstantState() {
        // First create a Drawable, and mutated it so that it has a constant state
        Drawable drawable = new GradientDrawable();
        drawable = drawable.mutate();
        assertNotNull(drawable.getConstantState());

        // Now wrap and assert that the wrapper also returns a constant state
        final Drawable wrapper = DrawableCompat.wrap(drawable);
        assertNotNull(wrapper.getConstantState());
    }

    @Test
    public void testWrappedDrawableHasCallbackSet() {
        // First create a Drawable
        final Drawable drawable = new GradientDrawable();

        // Now wrap it and set a mock as the wrapper's callback
        final Drawable wrapper = DrawableCompat.wrap(drawable);
        final Drawable.Callback mockCallback = mock(Drawable.Callback.class);
        wrapper.setCallback(mockCallback);

        // Now make the wrapped drawable invalidate itself
        drawable.invalidateSelf();

        // ...and verify that the wrapper calls to be invalidated
        verify(mockCallback, times(1)).invalidateDrawable(wrapper);
    }

    @Test
    public void testDoesNotWrapTintAwareDrawable() {
        final TestTintAwareDrawable tintAwareDrawable = new TestTintAwareDrawable();
        final Drawable wrapped = DrawableCompat.wrap(tintAwareDrawable);
        // Assert that the tint aware drawable was not wrapped
        assertSame(tintAwareDrawable, wrapped);
    }

    @Test
    public void testTintAwareDrawableGetsTintCallsDirectly() {
        final TestTintAwareDrawable d = mock(TestTintAwareDrawable.class);

        final ColorStateList tint = ColorStateList.valueOf(Color.BLACK);
        final PorterDuff.Mode tintMode = PorterDuff.Mode.DST;

        // Now set the tint list and mode using DrawableCompat
        DrawableCompat.setTintList(d, tint);
        DrawableCompat.setTintMode(d, tintMode);

        // Verify that the calls were directly on to the TintAwareDrawable
        verify(d).setTintList(tint);
        verify(d).setTintMode(tintMode);
    }

}