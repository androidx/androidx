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

package android.support.v4.graphics;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DrawableCompatTest {
    @Test
    public void testDrawableUnwrap() {
        final Drawable original = new GradientDrawable();
        final Drawable wrappedDrawable = DrawableCompat.wrap(original);
        assertSame(original, DrawableCompat.unwrap(wrappedDrawable));
    }

    @Test
    public void testDrawableChangeBoundsCopy() {
        final Rect bounds = new Rect(0, 0, 10, 10);

        final Drawable original = new GradientDrawable();
        original.setBounds(bounds);

        final Drawable wrapped = DrawableCompat.wrap(original);
        assertEquals(bounds, wrapped.getBounds());
    }

    @Test
    public void testDrawableWrapOnlyWrapsOnce() {
        final Drawable wrappedDrawable = DrawableCompat.wrap(new GradientDrawable());
        assertSame(wrappedDrawable, DrawableCompat.wrap(wrappedDrawable));
    }
}