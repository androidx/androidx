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

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_foldWithNonZeroArea() {
        DisplayFeature feature = new DisplayFeature.Builder()
                .setBounds(new Rect(10, 0, 20, 30))
                .setType(DisplayFeature.TYPE_FOLD).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_horizontalHingeWithNonZeroOrigin() {
        DisplayFeature horizontalHinge = new DisplayFeature.Builder()
                .setBounds(new Rect(1, 10, 20, 10))
                .setType(DisplayFeature.TYPE_HINGE).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_verticalHingeWithNonZeroOrigin() {
        DisplayFeature verticalHinge = new DisplayFeature.Builder()
                .setBounds(new Rect(10, 1, 10, 20))
                .setType(DisplayFeature.TYPE_HINGE).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_horizontalFoldWithNonZeroOrigin() {
        DisplayFeature horizontalFold = new DisplayFeature.Builder()
                .setBounds(new Rect(1, 10, 20, 10))
                .setType(DisplayFeature.TYPE_FOLD).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_verticalFoldWithNonZeroOrigin() {
        DisplayFeature verticalFold = new DisplayFeature.Builder()
                .setBounds(new Rect(10, 1, 10, 20))
                .setType(DisplayFeature.TYPE_FOLD).build();
    }

    @Test
    public void testBuilder_setBoundsAndType() {
        DisplayFeature.Builder builder = new DisplayFeature.Builder();
        Rect bounds = new Rect(0, 10, 30, 10);
        builder.setBounds(bounds);
        builder.setType(DisplayFeature.TYPE_HINGE);
        DisplayFeature feature = builder.build();

        assertEquals(bounds, feature.getBounds());
        assertEquals(DisplayFeature.TYPE_HINGE, feature.getType());
    }

    @Test
    public void testEquals_sameAttributes() {
        Rect bounds = new Rect(1, 0, 1, 10);
        int type = DisplayFeature.TYPE_FOLD;

        DisplayFeature original = new DisplayFeature(bounds, type);
        DisplayFeature copy = new DisplayFeature(bounds, type);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentRect() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect otherRect = new Rect(2, 0, 2, 10);
        int type = DisplayFeature.TYPE_FOLD;

        DisplayFeature original = new DisplayFeature(originalRect, type);
        DisplayFeature other = new DisplayFeature(otherRect, type);

        assertNotEquals(original, other);
    }

    @Test
    public void testEquals_differentType() {
        Rect rect = new Rect(1, 0, 1, 10);
        int originalType = DisplayFeature.TYPE_FOLD;
        int otherType = DisplayFeature.TYPE_HINGE;

        DisplayFeature original = new DisplayFeature(rect, originalType);
        DisplayFeature other = new DisplayFeature(rect, otherType);

        assertNotEquals(original, other);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        Rect originalRect = new Rect(1, 0, 1, 10);
        Rect matchingRect = new Rect(1, 0, 1, 10);
        int type = DisplayFeature.TYPE_FOLD;

        DisplayFeature original = new DisplayFeature(originalRect, type);
        DisplayFeature matching = new DisplayFeature(matchingRect, type);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
