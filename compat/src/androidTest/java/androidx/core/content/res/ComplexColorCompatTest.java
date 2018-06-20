/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.content.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ComplexColorCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ComplexColorCompatTest {

    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mResources = mContext.getResources();
    }

    @Test
    public void testSimpleColor() {
        final ComplexColorCompat result = ComplexColorCompat.from(Color.MAGENTA);
        assertNotNull(result);
        assertFalse(result.isStateful());
        assertFalse(result.isGradient());
        assertEquals(result.getColor(), Color.MAGENTA);
        assertTrue(result.willDraw());
    }

    @Test
    public void testColorStateList() {
        final ColorStateList csl = mResources.getColorStateList(R.color.complex_unthemed_selector);
        final ComplexColorCompat result = ComplexColorCompat.from(csl);
        assertNotNull(result);
        assertTrue(result.isStateful());
        assertFalse(result.isGradient());
        assertTrue(result.willDraw());

        // initially getColor() should return the ColorStateList's default color
        assertEquals(result.getColor(), csl.getDefaultColor());

        // this ColorStateList does not declare an activated state so this should not yield a change
        boolean changed = result.onStateChanged(new int[] {android.R.attr.state_activated});
        assertFalse(changed);
        assertEquals(result.getColor(), csl.getDefaultColor());

        // setting a pressed state should result in a change and a new result from getColor()
        changed = result.onStateChanged(new int[] {android.R.attr.state_pressed});
        assertTrue(changed);
        assertEquals(result.getColor(), mResources.getColor(R.color.selector_color_pressed));

        // this should also change
        changed = result.onStateChanged(new int[] {android.R.attr.state_focused});
        assertTrue(changed);
        assertEquals(result.getColor(), mResources.getColor(R.color.selector_color_focused));

        // pushing this un-declared state should reset back to default
        changed = result.onStateChanged(new int[] {android.R.attr.state_activated});
        assertTrue(changed);
        assertEquals(result.getColor(), csl.getDefaultColor());
    }

    @Test
    public void testGradient() {
        final Shader linearGradient = new LinearGradient(
                0f, /* startX */
                0f, /* startY */
                100f, /* endX */
                100f, /* endY */
                new int[] {0xffff0000, 0xff00ff00, 0xff0000ff}, /* colors */
                new float[] { 0.0f, 0.5f, 1.0f}, /* offsets */
                Shader.TileMode.CLAMP /* tileMode */);
        final ComplexColorCompat result = ComplexColorCompat.from(linearGradient);
        assertNotNull(result);
        assertFalse(result.isStateful());
        assertTrue(result.isGradient());
        assertEquals(result.getShader(), linearGradient);
        assertTrue(result.willDraw());
        assertEquals(result.getColor(), Color.TRANSPARENT);
    }

    @Test
    public void testTransparency() {
        final ComplexColorCompat result = ComplexColorCompat.from(Color.TRANSPARENT);
        assertNotNull(result);
        assertFalse(result.isStateful());
        assertFalse(result.isGradient());
        assertEquals(result.getColor(), Color.TRANSPARENT);
        assertFalse(result.willDraw());
    }

    @Test
    public void testSetColor() {
        final ComplexColorCompat result = ComplexColorCompat.from(Color.WHITE);
        assertNotNull(result);
        assertFalse(result.isStateful());
        assertFalse(result.isGradient());
        assertEquals(result.getColor(), Color.WHITE);
        result.setColor(Color.RED);
        assertEquals(result.getColor(), Color.RED);
    }

    @Test
    public void testInflateGradient() {
        final ComplexColorCompat result = ComplexColorCompat.inflate(mResources,
                R.color.gradient_linear, mContext.getTheme());
        assertNotNull(result);
        assertFalse(result.isStateful());
        assertTrue(result.isGradient());
        assertTrue(result.willDraw());
    }

    @Test
    public void testInflateColorStateList() {
        final ComplexColorCompat result = ComplexColorCompat.inflate(mResources,
                R.color.complex_unthemed_selector, mContext.getTheme());
        assertNotNull(result);
        assertTrue(result.isStateful());
        assertFalse(result.isGradient());
        assertTrue(result.willDraw());
        assertEquals(result.getColor(), mResources.getColor(R.color.selector_color_default));
    }
}
