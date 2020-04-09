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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link DisplayFeature} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class DisplayFeatureTest {

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_empty() {
        new DisplayFeature.Builder().build();
    }

    @Test
    public void testBuilder_setBoundsAndType() {
        DisplayFeature.Builder builder = new DisplayFeature.Builder();
        Rect bounds = new Rect(1, 2, 3, 4);
        builder.setBounds(bounds);
        builder.setType(DisplayFeature.TYPE_HINGE);
        DisplayFeature feature = builder.build();

        assertEquals(bounds, feature.getBounds());
        assertEquals(DisplayFeature.TYPE_HINGE, feature.getType());
    }

    @Test
    public void testEquals_sameAttributes() {
        Rect nonEmptyRect = new Rect(-1, 1, 1, -1);
        int type = 1;

        DisplayFeature original = new DisplayFeature(nonEmptyRect, type);
        DisplayFeature copy = new DisplayFeature(nonEmptyRect, type);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentRect() {
        Rect originalRect = new Rect(-1, 1, 1, -1);
        Rect otherRect = new Rect(-3, 3, 3, -3);
        int type = 1;

        DisplayFeature original = new DisplayFeature(originalRect, type);
        DisplayFeature other = new DisplayFeature(otherRect, type);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentType() {
        Rect rect = new Rect(-1, 1, 1, -1);
        int originalType = 1;
        int otherType = 2;

        DisplayFeature original = new DisplayFeature(rect, originalType);
        DisplayFeature other = new DisplayFeature(rect, otherType);

        assertNotEquals(original, other);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect originalRect = new Rect(-1, 2, 3, -1);
        Rect matchingRect = new Rect(-1, 2, 3, -1);
        int type = 1;

        DisplayFeature original = new DisplayFeature(originalRect, type);
        DisplayFeature matching = new DisplayFeature(matchingRect, type);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
