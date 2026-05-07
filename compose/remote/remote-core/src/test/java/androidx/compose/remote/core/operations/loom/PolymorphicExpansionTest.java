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
package androidx.compose.remote.core.operations.loom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.types.IntegerConstant;

import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PolymorphicExpansionTest {

    private static class MockDocument extends CoreDocument {
        void addArray(int id, int[] data) {
            getRemoteComposeState()
                    .addCollection(
                            id,
                            new ArrayAccess() {
                                @Override
                                public int getLength() {
                                    return data.length;
                                }

                                @Override
                                public int getId(int index) {
                                    return data[index];
                                }

                                @Override
                                public float getFloatValue(int index) {
                                    return data[index];
                                }

                                @Override
                                public float @Nullable [] getFloats() {
                                    return null;
                                }
                            });
        }
    }

    @Test
    public void testStandardOperationMaterialize() {
        MockDocument doc = new MockDocument();
        ExpansionContext expansionContext =
                new ExpansionContext(
                        new LoomManager(), doc, new RemapContext(doc), new HashMap<>());

        IntegerConstant op = new IntegerConstant(10, 100);
        ArrayList<Operation> result = new ArrayList<>();

        op.materialize(expansionContext, result, expansionContext.getMacroManager());

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof IntegerConstant);
        assertEquals(10, ((IntegerConstant) result.get(0)).mId);
    }

    @Test
    public void testMacroArgumentMaterialize() {
        MockDocument doc = new MockDocument();
        Map<Integer, ArrayList<Operation>> blocks = new HashMap<>();
        ArrayList<Operation> blockOps = new ArrayList<>();
        blockOps.add(new IntegerConstant(1, 111));
        blocks.put(0, blockOps);

        ExpansionContext expansionContext =
                new ExpansionContext(new LoomManager(), doc, new RemapContext(doc), blocks);

        PatternArgument arg = new PatternArgument(0);
        ArrayList<Operation> result = new ArrayList<>();

        arg.materialize(expansionContext, result, expansionContext.getMacroManager());

        assertEquals(1, result.size());
        assertEquals(1, ((IntegerConstant) result.get(0)).getId());
    }

    @Test
    public void testMacroForEachMaterialize() {
        MockDocument doc = new MockDocument();
        doc.addArray(100, new int[] {10, 20});

        ExpansionContext expansionContext =
                new ExpansionContext(
                        new LoomManager(), doc, new RemapContext(doc), new HashMap<>());

        PatternForEach forEach = new PatternForEach(100, 1); // coll=100, local=1
        forEach.getList().add(new IntegerConstant(1, 0)); // Template uses local var 1

        ArrayList<Operation> result = new ArrayList<>();
        forEach.materialize(expansionContext, result, expansionContext.getMacroManager());

        // Should unroll to 2 operations
        assertEquals(2, result.size());
        assertEquals(10, ((IntegerConstant) result.get(0)).mId);
        assertEquals(20, ((IntegerConstant) result.get(1)).mId);
    }
}
