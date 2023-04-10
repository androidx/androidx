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

package androidx.window.testing.embedding;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.app.Activity;

import androidx.annotation.OptIn;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.embedding.ActivityStack;
import androidx.window.embedding.SplitAttributes;
import androidx.window.embedding.SplitInfo;

import org.junit.Test;

import java.util.Collections;

/** Test class to verify {@link TestSplitInfo} */
@OptIn(markerClass = ExperimentalWindowApi.class)
public class SplitInfoTestingJavaTest {

    /** Verifies the default value of {@link TestSplitInfo}. */
    @Test
    public void testSplitInfoDefaultValue() {
        final SplitInfo splitInfo = TestSplitInfo.createTestSplitInfo();

        assertEquals(TestActivityStack.createTestActivityStack(),
                splitInfo.getPrimaryActivityStack());
        assertEquals(TestActivityStack.createTestActivityStack(),
                splitInfo.getSecondaryActivityStack());
        assertEquals(new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.SPLIT_TYPE_EQUAL)
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .build(), splitInfo.getSplitAttributes());
    }

    /** Verifies {@link TestSplitInfo} */
    @Test
    public void testSplitInfoWithNonEmptyActivityStacks() {
        final ActivityStack primaryActivityStack = TestActivityStack.createTestActivityStack(
                Collections.singletonList(mock(Activity.class)), false /* isEmpty */);
        final ActivityStack secondaryActivityStack = TestActivityStack.createTestActivityStack(
                Collections.singletonList(mock(Activity.class)), false /* isEmpty */);
        final SplitAttributes splitAttributes = new SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.SPLIT_TYPE_HINGE)
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                .build();

        final SplitInfo splitInfo = TestSplitInfo.createTestSplitInfo(primaryActivityStack,
                secondaryActivityStack, splitAttributes);

        assertEquals(primaryActivityStack, splitInfo.getPrimaryActivityStack());
        assertEquals(secondaryActivityStack, splitInfo.getSecondaryActivityStack());
        assertEquals(splitAttributes, splitInfo.getSplitAttributes());
    }
}
