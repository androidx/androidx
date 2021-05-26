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

package androidx.core.os;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link BuildCompat}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BuildCompatTest {
    @Test
    public void isAtLeastPreReleaseCodename() {
        assertTrue(BuildCompat.isAtLeastPreReleaseCodename("S", "S"));
        assertTrue(BuildCompat.isAtLeastPreReleaseCodename("T", "S"));
        assertFalse(BuildCompat.isAtLeastPreReleaseCodename("S", "T"));

        assertTrue(BuildCompat.isAtLeastPreReleaseCodename("OMR1", "O"));
        assertFalse(BuildCompat.isAtLeastPreReleaseCodename("O", "OMR1"));

        assertTrue(BuildCompat.isAtLeastPreReleaseCodename("OMR1", "OMR1"));
        assertTrue(BuildCompat.isAtLeastPreReleaseCodename("OMR2", "OMR1"));
        assertFalse(BuildCompat.isAtLeastPreReleaseCodename("OMR1", "OMR2"));

        assertFalse(BuildCompat.isAtLeastPreReleaseCodename("S", "REL"));

        assertFalse(BuildCompat.isAtLeastPreReleaseCodename("RMR1", "REL"));
    }

}
