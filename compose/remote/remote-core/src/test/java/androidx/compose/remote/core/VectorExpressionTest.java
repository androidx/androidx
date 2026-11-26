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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.ADD;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.MUL;
import static androidx.compose.remote.core.operations.utilities.VectorOpCodes.VBUILD3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.VectorExpression;

import org.junit.Test;

import java.util.ArrayList;

public class VectorExpressionTest {

    @Test
    public void writeReadRoundTrip() {
        // p += v*dt as a vec3 expression  ->  one VectorExpression producing 3 components.
        float[] program = {
            1f, 2f, 3f, VBUILD3,
            0.5f, 0.5f, 0.5f, VBUILD3,
            2f, MUL,
            ADD
        };
        WireBuffer buffer = new WireBuffer();
        VectorExpression.apply(buffer, /* id= */ 42, /* dim= */ 3, /* flags= */ 0, program);

        buffer.setIndex(0);
        int opcode = buffer.readByte() & 0xFF;
        assertEquals(Operations.VECTOR_EXPRESSION, opcode);
        assertEquals(116, Operations.VECTOR_EXPRESSION);

        ArrayList<Operation> ops = new ArrayList<>();
        VectorExpression.read(buffer, ops);

        assertEquals(1, ops.size());
        assertTrue(ops.get(0) instanceof VectorExpression);
        VectorExpression v = (VectorExpression) ops.get(0);
        assertEquals(42, v.getId());
        assertEquals(3, v.getDimension());
        assertEquals(0, v.getFlags());
        assertArrayEquals(program, v.getSrcValue(), 0f);
    }

    @Test
    public void registeredInOperationsMap() {
        // The opcode must be reachable via the operations map so documents can be parsed.
        boolean found = false;
        for (int profiles = 0; profiles <= 0x400; profiles++) {
            Operations.UniqueIntMap<CompanionOperation> map =
                    Operations.getOperations(7, profiles);
            if (map != null && map.get(Operations.VECTOR_EXPRESSION) != null) {
                found = true;
                break;
            }
        }
        assertTrue("VECTOR_EXPRESSION not registered in any v7 profile map", found);
    }

    @Test
    public void dimensionAccessor() {
        VectorExpression v = new VectorExpression(1, 4, 0, new float[] {0f});
        assertEquals(4, v.getDimension());
        assertNotNull(v.toString());
    }
}
