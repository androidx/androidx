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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tests for {@link WindowLayoutInfo} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class WindowLayoutInfoTest {

    @Test
    public void testBuilder_empty() {
        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        WindowLayoutInfo windowLayoutInfo = builder.build();

        assertThat(windowLayoutInfo.getDisplayFeatures()).isEmpty();
    }

    @Test
    public void testBuilder_setDisplayFeatures() {
        DisplayFeature feature1 = new FoldingFeature(new Rect(1, 0, 3, 4),
                FoldingFeature.TYPE_HINGE, FoldingFeature.STATE_FLAT);

        DisplayFeature feature2 = new FoldingFeature(new Rect(1, 0, 1, 4),
                FoldingFeature.STATE_FLAT, FoldingFeature.STATE_FLAT);

        List<DisplayFeature> displayFeatures = new ArrayList<>();
        displayFeatures.add(feature1);
        displayFeatures.add(feature2);

        WindowLayoutInfo.Builder builder = new WindowLayoutInfo.Builder();
        builder.setDisplayFeatures(displayFeatures);
        WindowLayoutInfo windowLayoutInfo = builder.build();

        assertEquals(displayFeatures, windowLayoutInfo.getDisplayFeatures());
    }

    @Test
    public void testEquals_sameFeatures() {
        List<DisplayFeature> features = new ArrayList<>();

        WindowLayoutInfo original = new WindowLayoutInfo(features);
        WindowLayoutInfo copy = new WindowLayoutInfo(features);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentFeatures() {
        List<DisplayFeature> originalFeatures = new ArrayList<>();
        List<DisplayFeature> differentFeatures = new ArrayList<>();
        Rect rect = new Rect(1, 0, 1, 10);
        differentFeatures.add(new FoldingFeature(rect, FoldingFeature.TYPE_HINGE,
                FoldingFeature.STATE_FLAT));

        WindowLayoutInfo original = new WindowLayoutInfo(originalFeatures);
        WindowLayoutInfo different = new WindowLayoutInfo(differentFeatures);

        assertNotEquals(original, different);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        List<DisplayFeature> firstFeatures = new ArrayList<>();
        List<DisplayFeature> secondFeatures = new ArrayList<>();
        WindowLayoutInfo first = new WindowLayoutInfo(firstFeatures);
        WindowLayoutInfo second = new WindowLayoutInfo(secondFeatures);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Ignore
    @Test
    public void testHashCode_matchesIfEqualFeatures() {
        DisplayFeature originalFeature = new FoldingFeature(
                new Rect(0, 0, 100, 0),
                FoldingFeature.TYPE_HINGE,
                FoldingFeature.STATE_FLAT
        );
        DisplayFeature matchingFeature = new FoldingFeature(
                new Rect(0, 0, 100, 0),
                FoldingFeature.TYPE_HINGE,
                FoldingFeature.STATE_FLAT
        );
        List<DisplayFeature> firstFeatures = Collections.singletonList(originalFeature);
        List<DisplayFeature> secondFeatures = Collections.singletonList(matchingFeature);
        WindowLayoutInfo first = new WindowLayoutInfo(firstFeatures);
        WindowLayoutInfo second = new WindowLayoutInfo(secondFeatures);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
