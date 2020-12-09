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

package androidx.window.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ExtensionDeviceState} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionDeviceStateTest {

    @Test
    public void testEquals_samePosture() {
        ExtensionDeviceState original = new ExtensionDeviceState(0);
        ExtensionDeviceState copy = new ExtensionDeviceState(0);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentPosture() {
        ExtensionDeviceState original = new ExtensionDeviceState(0);
        ExtensionDeviceState different = new ExtensionDeviceState(1);

        assertNotEquals(original, different);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        int posture = 111;
        ExtensionDeviceState original = new ExtensionDeviceState(posture);
        ExtensionDeviceState matching = new ExtensionDeviceState(posture);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
