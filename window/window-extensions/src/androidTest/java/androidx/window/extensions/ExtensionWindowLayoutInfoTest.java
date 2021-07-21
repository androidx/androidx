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

package androidx.window.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tests for {@link ExtensionWindowLayoutInfo} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionWindowLayoutInfoTest {

    @Test
    public void testEquals_sameFeatures() {
        List<ExtensionDisplayFeature> features = new ArrayList<>();

        ExtensionWindowLayoutInfo original = new ExtensionWindowLayoutInfo(features);
        ExtensionWindowLayoutInfo copy = new ExtensionWindowLayoutInfo(features);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentFeatures() {
        List<ExtensionDisplayFeature> originalFeatures = new ArrayList<>();
        List<ExtensionDisplayFeature> differentFeatures = new ArrayList<>();
        Rect rect = new Rect(1, 0, 1, 10);
        differentFeatures.add(new ExtensionFoldingFeature(
                rect, ExtensionFoldingFeature.TYPE_HINGE,
                ExtensionFoldingFeature.STATE_FLAT));

        ExtensionWindowLayoutInfo original = new ExtensionWindowLayoutInfo(originalFeatures);
        ExtensionWindowLayoutInfo different = new ExtensionWindowLayoutInfo(differentFeatures);

        assertNotEquals(original, different);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        List<ExtensionDisplayFeature> firstFeatures = new ArrayList<>();
        List<ExtensionDisplayFeature> secondFeatures = new ArrayList<>();
        ExtensionWindowLayoutInfo first = new ExtensionWindowLayoutInfo(firstFeatures);
        ExtensionWindowLayoutInfo second = new ExtensionWindowLayoutInfo(secondFeatures);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testHashCode_matchesIfEqualFeatures() {
        ExtensionDisplayFeature originalFeature = new ExtensionFoldingFeature(
                new Rect(0, 0, 100, 0),
                ExtensionFoldingFeature.TYPE_HINGE,
                ExtensionFoldingFeature.STATE_FLAT
        );
        ExtensionDisplayFeature matchingFeature = new ExtensionFoldingFeature(
                new Rect(0, 0, 100, 0),
                ExtensionFoldingFeature.TYPE_HINGE,
                ExtensionFoldingFeature.STATE_FLAT
        );
        List<ExtensionDisplayFeature> firstFeatures = Collections.singletonList(originalFeature);
        List<ExtensionDisplayFeature> secondFeatures = Collections.singletonList(matchingFeature);
        ExtensionWindowLayoutInfo first = new ExtensionWindowLayoutInfo(firstFeatures);
        ExtensionWindowLayoutInfo second = new ExtensionWindowLayoutInfo(secondFeatures);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
