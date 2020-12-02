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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link DeviceState} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class DeviceStateTest {

    @Test
    public void testBuilder_empty() {
        DeviceState.Builder builder = new DeviceState.Builder();
        DeviceState state = builder.build();

        assertEquals(DeviceState.POSTURE_UNKNOWN, state.getPosture());
    }

    @Test
    public void testBuilder_setPosture() {
        DeviceState.Builder builder = new DeviceState.Builder();
        builder.setPosture(DeviceState.POSTURE_OPENED);
        DeviceState state = builder.build();

        assertEquals(DeviceState.POSTURE_OPENED, state.getPosture());
    }

    @Test
    public void testEquals_samePosture() {
        DeviceState original = new DeviceState(0);
        DeviceState copy = new DeviceState(0);

        assertEquals(original, copy);
    }

    @Test
    public void testEquals_differentPosture() {
        DeviceState original = new DeviceState(0);
        DeviceState different = new DeviceState(1);

        assertNotEquals(original, different);
    }

    @Test
    public void testHashCode_matchesIfEqual() {
        int posture = 111;
        DeviceState original = new DeviceState(posture);
        DeviceState matching = new DeviceState(posture);

        assertEquals(original, matching);
        assertEquals(original.hashCode(), matching.hashCode());
    }
}
