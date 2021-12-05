/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.extensions.layout;

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

    @Test
    public void testEquals_sameAttributes() {
        Rect bounds = new Rect(1, 0, 1, 10);
        int type = FoldingFeature.TYPE_FOLD;
        int state = FoldingFeature.STATE_FLAT;

        FoldingFeature original = new FoldingFeature(bounds, type, state);
        FoldingFeature copy = new FoldingFeature(bounds, type, state);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentRect() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect otherRect = new Rect(2, 0, 2, 10);
        int type = FoldingFeature.TYPE_FOLD;
        int state = FoldingFeature.STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type,
                state);
        FoldingFeature other = new FoldingFeature(otherRect, type, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentType() {
        Rect rect = new Rect(1, 0, 1, 10);
        int originalType = FoldingFeature.TYPE_FOLD;
        int otherType = FoldingFeature.TYPE_HINGE;
        int state = FoldingFeature.STATE_FLAT;

        FoldingFeature original = new FoldingFeature(rect, originalType,
                state);
        FoldingFeature other = new FoldingFeature(rect, otherType, state);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentState() {
        Rect rect = new Rect(1, 0, 1, 10);
        int type = FoldingFeature.TYPE_FOLD;
        int originalState = FoldingFeature.STATE_FLAT;
        int otherState = FoldingFeature.STATE_HALF_OPENED;

        FoldingFeature original = new FoldingFeature(rect, type,
                originalState);
        FoldingFeature other = new FoldingFeature(rect, type, otherState);

        assertNotEquals(original, other);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect matchingRect = new Rect(1, 0, 1, 10);
        int type = FoldingFeature.TYPE_FOLD;
        int state = FoldingFeature.STATE_FLAT;

        FoldingFeature original = new FoldingFeature(originalRect, type,
                state);
        FoldingFeature matching = new FoldingFeature(matchingRect, type,
                state);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
