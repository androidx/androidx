/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import static androidx.window.extensions.embedding.SplitAttributes.SplitType.RatioSplitType.splitEqually;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.graphics.Color;

import androidx.test.filters.SmallTest;
import androidx.window.extensions.embedding.SplitAttributes.LayoutDirection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Test for {@link SplitAttributes} */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class SplitAttributesTest {
    @Test
    public void testSplitAttributesEquals() {
        final SplitAttributes attrs1 = new SplitAttributes.Builder()
                .setSplitType(splitEqually())
                .setLayoutDirection(LayoutDirection.LOCALE)
                .setAnimationBackground(AnimationBackground.ANIMATION_BACKGROUND_DEFAULT)
                .build();
        final SplitAttributes attrs2 = new SplitAttributes.Builder()
                .setSplitType(new SplitAttributes.SplitType.HingeSplitType(splitEqually()))
                .setLayoutDirection(LayoutDirection.LOCALE)
                .setAnimationBackground(AnimationBackground.ANIMATION_BACKGROUND_DEFAULT)
                .build();
        final SplitAttributes attrs3 = new SplitAttributes.Builder()
                .setSplitType(new SplitAttributes.SplitType.HingeSplitType(splitEqually()))
                .setLayoutDirection(LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackground(AnimationBackground.ANIMATION_BACKGROUND_DEFAULT)
                .build();
        final SplitAttributes attrs4 = new SplitAttributes.Builder()
                .setSplitType(new SplitAttributes.SplitType.HingeSplitType(splitEqually()))
                .setLayoutDirection(LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackground(AnimationBackground.createColorBackground(Color.BLUE))
                .build();
        final SplitAttributes attrs5 = new SplitAttributes.Builder()
                .setSplitType(new SplitAttributes.SplitType.HingeSplitType(splitEqually()))
                .setLayoutDirection(LayoutDirection.TOP_TO_BOTTOM)
                .setAnimationBackground(AnimationBackground.createColorBackground(Color.BLUE))
                .build();

        assertNotEquals(attrs1, attrs2);
        assertNotEquals(attrs1.hashCode(), attrs2.hashCode());

        assertNotEquals(attrs2, attrs3);
        assertNotEquals(attrs2.hashCode(), attrs3.hashCode());

        assertNotEquals(attrs3, attrs1);
        assertNotEquals(attrs3.hashCode(), attrs1.hashCode());

        assertNotEquals(attrs4, attrs3);
        assertNotEquals(attrs4.hashCode(), attrs3.hashCode());

        assertEquals(attrs4, attrs5);
        assertEquals(attrs4.hashCode(), attrs5.hashCode());
    }

    @Test
    public void testSplitAttributesEqualsUsingBuilderFromExistingInstance() {
        final SplitAttributes attrs1 = new SplitAttributes.Builder()
                .setSplitType(splitEqually())
                .setLayoutDirection(LayoutDirection.LOCALE)
                .setAnimationBackground(AnimationBackground.ANIMATION_BACKGROUND_DEFAULT)
                .build();
        final SplitAttributes attrs2 = new SplitAttributes.Builder(attrs1).build();
        assertEquals(attrs1, attrs2);
    }

    @Test
    public void testSplitTypeEquals() {
        final SplitAttributes.SplitType[] splitTypes = new SplitAttributes.SplitType[]{
                new SplitAttributes.SplitType.ExpandContainersSplitType(),
                new SplitAttributes.SplitType.RatioSplitType(0.3f),
                splitEqually(),
                new SplitAttributes.SplitType.HingeSplitType(splitEqually()),
                new SplitAttributes.SplitType.HingeSplitType(
                        new SplitAttributes.SplitType.ExpandContainersSplitType()
                ),
        };

        for (int i = 0; i < splitTypes.length; i++) {
            for (int j = 0; j < splitTypes.length; j++) {
                final SplitAttributes.SplitType splitType0 = splitTypes[i];
                final SplitAttributes.SplitType splitType1 = splitTypes[j];
                if (i == j) {
                    assertEquals(splitType0, splitType1);
                    assertEquals(splitType0.hashCode(), splitType1.hashCode());
                } else {
                    assertNotEquals(splitType0, splitType1);
                    assertNotEquals(splitType0.hashCode(), splitType1.hashCode());
                }
            }
        }
    }
}
