/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RemoteComposeStateTest {

    @Test
    public void testBoundsChecking() {
        RemoteComposeState state = new RemoteComposeState();

        // Test overrideFloat out of bounds (should return early without throwing or modifying
        // state)
        state.overrideFloat(Limits.MAX_STATE_DATA, 1.0f);
        assertEquals(0.0f, state.getFloat(Limits.MAX_STATE_DATA), 0.0f);

        state.overrideFloat(-1, 1.0f);
        assertEquals(0.0f, state.getFloat(-1), 0.0f);

        // Test overrideInteger out of bounds
        state.overrideInteger(Limits.MAX_STATE_DATA, 1);
        assertEquals(0, state.getInteger(Limits.MAX_STATE_DATA));

        state.overrideInteger(-1, 1);
        assertEquals(0, state.getInteger(-1));

        // Test overrideData out of bounds
        state.overrideData(Limits.MAX_STATE_DATA, "test");
        assertNull(state.getFromId(Limits.MAX_STATE_DATA));

        state.overrideData(-1, "test");
        assertNull(state.getFromId(-1));

        // Test updateFloat out of bounds
        state.updateFloat(Limits.MAX_STATE_DATA, 1.0f);
        assertEquals(0.0f, state.getFloat(Limits.MAX_STATE_DATA), 0.0f);

        state.updateFloat(-1, 1.0f);
        assertEquals(0.0f, state.getFloat(-1), 0.0f);

        // Test updateInteger out of bounds
        state.updateInteger(Limits.MAX_STATE_DATA, 1);
        assertEquals(0, state.getInteger(Limits.MAX_STATE_DATA));

        state.updateInteger(-1, 1);
        assertEquals(0, state.getInteger(-1));

        // Test updateData out of bounds
        state.updateData(Limits.MAX_STATE_DATA, "test");
        assertNull(state.getFromId(Limits.MAX_STATE_DATA));

        state.updateData(-1, "test");
        assertNull(state.getFromId(-1));

        // Test clearFloatOverride out of bounds (should not throw)
        state.clearFloatOverride(Limits.MAX_STATE_DATA);
        state.clearFloatOverride(-1);

        // Test clearIntegerOverride out of bounds (should not throw)
        state.clearIntegerOverride(Limits.MAX_STATE_DATA);
        state.clearIntegerOverride(-1);

        // Test clearDataOverride out of bounds (should not throw)
        state.clearDataOverride(Limits.MAX_STATE_DATA);
        state.clearDataOverride(-1);
    }
}
