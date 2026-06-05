/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.modifiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.WireBuffer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GraphicsLayerModifierOperationTest {

    @Test
    public void testGraphicsLayerModifierLengthBoundsCheck() {
        WireBuffer buffer = new WireBuffer();
        buffer.reset(100);
        buffer.start(Operations.MODIFIER_GRAPHICS_LAYER);
        buffer.writeInt(Integer.MAX_VALUE); // huge length
        buffer.setIndex(0);

        int opType = buffer.readOperationType();
        assertEquals(Operations.MODIFIER_GRAPHICS_LAYER, opType);

        List<Operation> operations = new ArrayList<>();
        try {
            GraphicsLayerModifierOperation.read(buffer, operations);
            org.junit.Assert.fail("Should throw RuntimeException due to invalid length");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(
                    "attempt to allocate an invalid number of attributes"));
        }
    }

    @Test
    public void testGraphicsLayerModifierIndexBoundsCheck() {
        WireBuffer buffer = new WireBuffer();
        buffer.reset(100);
        buffer.start(Operations.MODIFIER_GRAPHICS_LAYER);
        buffer.writeInt(1); // length = 1
        // tag: index = 63 (invalid), dataType = DATA_TYPE_FLOAT (1)
        buffer.writeInt(63 | (1 << 10));
        buffer.writeFloat(1.0f);
        buffer.setIndex(0);

        int opType = buffer.readOperationType();
        assertEquals(Operations.MODIFIER_GRAPHICS_LAYER, opType);

        List<Operation> operations = new ArrayList<>();
        try {
            GraphicsLayerModifierOperation.read(buffer, operations);
            org.junit.Assert.fail("Should throw RuntimeException due to invalid attribute index");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("attempt to read an invalid attribute index"));
        }
    }
}
