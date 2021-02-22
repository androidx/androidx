/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static androidx.window.FoldingFeature.STATE_FLAT;
import static androidx.window.FoldingFeature.STATE_FLIPPED;
import static androidx.window.FoldingFeature.STATE_HALF_OPENED;
import static androidx.window.FoldingFeature.TYPE_FOLD;
import static androidx.window.FoldingFeature.TYPE_HINGE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link FoldingFeature} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class FoldingFeatureTest {

    @Test(expected = IllegalArgumentException.class)
    public void tesEmptyRect() {
        new FoldingFeature(new Rect(), TYPE_HINGE, STATE_HALF_OPENED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFoldWithNonZeroArea() {
        new FoldingFeature(new Rect(0, 0, 20, 30), TYPE_FOLD, STATE_FLIPPED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalHingeWithNonZeroOrigin() {
        new FoldingFeature(new Rect(1, 10, 20, 10), TYPE_HINGE, STATE_FLIPPED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerticalHingeWithNonZeroOrigin() {
        new FoldingFeature(new Rect(10, 1, 19, 29), TYPE_HINGE, STATE_FLIPPED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalFoldWithNonZeroOrigin() {
        new FoldingFeature(new Rect(1, 10, 20, 10), TYPE_FOLD, STATE_FLIPPED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerticalFoldWithNonZeroOrigin() {
        new FoldingFeature(new Rect(10, 1, 10, 20), TYPE_FOLD, STATE_FLIPPED);
    }

    @Test
    public void testSetBoundsAndType() {
        Rect bounds = new Rect(0, 10, 30, 10);
        int type = TYPE_HINGE;
        int state = STATE_HALF_OPENED;
        FoldingFeature feature = new FoldingFeature(bounds, type, state);

        assertEquals(bounds, feature.getBounds());
        assertEquals(type, feature.getType());
        assertEquals(state, feature.getState());
    }

    @Test
    public void testEquals_sameAttributes() {
        Rect bounds = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(bounds, type, state);
        FoldingFeature copy = new FoldingFeature(bounds, type, state);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentRect() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect otherRect = new Rect(2, 0, 2, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type, state);
        FoldingFeature other = new FoldingFeature(otherRect, type, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentType() {
        Rect rect = new Rect(1, 0, 1, 10);
        int originalType = TYPE_FOLD;
        int otherType = TYPE_HINGE;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(rect, originalType, state);
        FoldingFeature other = new FoldingFeature(rect, otherType, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentState() {
        Rect rect = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int originalState = STATE_FLAT;
        int otherState = STATE_FLIPPED;

        FoldingFeature original = new FoldingFeature(rect, type, originalState);
        FoldingFeature other = new FoldingFeature(rect, type, otherState);

        assertNotEquals(original, other);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect matchingRect = new Rect(1, 0, 1, 10);
        int type = TYPE_FOLD;
        int state = STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type, state);
        FoldingFeature matching = new FoldingFeature(matchingRect, type, state);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
