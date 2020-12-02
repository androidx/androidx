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

package androidx.testinfra.placeholderintegrationtest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing infra requires at least 1 test to be run. This breaks the
 * incremental test runner which runs only affected tests, which may
 * not include tests from each group. This test ensures that there
 * is always one and it is always allowed to run by the
 * AffectedModuleDetector.
 */
@RunWith(AndroidJUnit4.class)
public class PlaceholderTest {
    @Test
    @SmallTest
    public void testSmall() {
    }

    @Test
    @MediumTest
    public void testMedium() {
    }

    @Test
    @LargeTest
    public void testLarge() {
    }
}
