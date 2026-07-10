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
import static org.junit.Assert.assertNotEquals;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.operations.Utils;

import org.junit.Test;

public class RemapContextTest {

    private static class MockDocument extends CoreDocument {
        MockDocument() {
            mCurrentId = -100;
        }
    }

    @Test
    public void testBasicMapping() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        remapContext.addMapping(10, 20);

        assertEquals(20, remapContext.resolveId(10));
    }

    @Test
    public void testForkMappings() {
        MockDocument doc = new MockDocument();
        RemapContext parent = new RemapContext(doc);
        parent.addMapping(1, 100);

        RemapContext child = parent.fork();
        child.addMapping(2, 200);

        assertEquals(Integer.valueOf(100), child.getIdMap().get(1));
        assertEquals(Integer.valueOf(200), child.getIdMap().get(2));

        // Child should not affect parent
        assertEquals(1, parent.getIdMap().size());
    }

    @Test
    public void testUniqueification() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        // A macro-local ID should be uniqueified
        int localId = 0x4005; // Within Macro-local range [0x4000, 0x4FFF]
        int newId = remapContext.declareId(localId);

        assertNotEquals(localId, newId);
        assertEquals(doc.mCurrentId, newId);
        assertEquals(Integer.valueOf(newId), remapContext.getIdMap().get(localId));
    }

    @Test
    public void testNoUniqueificationForGlobal() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        // A system global ID should NOT be uniqueified
        int globalId = 5; // Within System Global range [0, 41]
        int result = remapContext.declareId(globalId);

        assertEquals(globalId, result);
    }

    @Test
    public void declareId_returnsOriginal_whenNotMappedAndNotInsideMacro() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        int originalId = 500;
        int result = remapContext.declareId(originalId);

        assertEquals(originalId, result);
    }

    @Test
    public void declareId_allocatesNewId_forMacroLocalId() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        int macroLocalId = 0x4005;
        int result = remapContext.declareId(macroLocalId);

        assertNotEquals(macroLocalId, result);
        assertEquals(doc.mCurrentId, result);
        assertEquals(Integer.valueOf(result), remapContext.getIdMap().get(macroLocalId));
    }

    @Test
    public void declareId_returnsMappedId_whenExplicitMappingExists() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        int originalId = 500;
        int mappedId = 600;
        remapContext.addMapping(originalId, mappedId);

        int result = remapContext.declareId(originalId);

        assertEquals(mappedId, result);
    }

    @Test
    public void declareId_allocatesNewId_whenInsideMacroForNonSystemId() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc).withInsideMacro(true);

        int nonSystemId = 100;
        int result = remapContext.declareId(nonSystemId);

        assertNotEquals(nonSystemId, result);
        assertEquals(doc.mCurrentId, result);
    }

    @Test
    public void declareId_returnsOriginal_forSystemGlobalId() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc).withInsideMacro(true);

        int systemId = 5;
        int result = remapContext.declareId(systemId);

        assertEquals(systemId, result);
    }

    @Test
    public void resolveNanId_translatesEncodedFloatId() {
        MockDocument doc = new MockDocument();
        RemapContext remapContext = new RemapContext(doc);

        int originalId = 100;
        int mappedId = 200;
        remapContext.addMapping(originalId, mappedId);

        float nanId = Utils.asNan(originalId);
        float result = remapContext.resolveNanId(nanId);

        assertEquals(Utils.asNan(mappedId), result, 0.0f);
    }
}
