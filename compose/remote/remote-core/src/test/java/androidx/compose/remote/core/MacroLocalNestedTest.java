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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;

import org.junit.Test;

import java.util.List;

public class MacroLocalNestedTest {

    @Test
    public void testNestedMacroLocalVariableUniqueification() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        int innerMacroId = 100;
        int outerMacroId = 101;
        int innerLocalId = 0x4001;
        int outerLocalId = 0x4002;

        // Define Inner Macro
        PatternDefine.apply(buffer.getBuffer(), innerMacroId, new int[] {});
        FloatConstant.apply(buffer.getBuffer(), innerLocalId, 1f);
        ContainerEnd.apply(buffer.getBuffer());

        // Define Outer Macro that calls Inner Macro
        PatternDefine.apply(buffer.getBuffer(), outerMacroId, new int[] {});
        FloatConstant.apply(buffer.getBuffer(), outerLocalId, 2f);
        PatternInflation.apply(buffer.getBuffer(), innerMacroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Call Outer Macro twice
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), outerMacroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer());

        PatternInflation.apply(buffer.getBuffer(), outerMacroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer());

        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root = findOperation(ops, RootLayoutComponent.class);
        assertNotNull(root);

        List<Operation> expanded = root.getList();
        // Expecting 2 sets of (OuterLocal, InnerLocal)
        assertEquals(4, expanded.size());

        FloatConstant outer1 = (FloatConstant) expanded.get(0);
        FloatConstant inner1 = (FloatConstant) expanded.get(1);
        FloatConstant outer2 = (FloatConstant) expanded.get(2);
        FloatConstant inner2 = (FloatConstant) expanded.get(3);

        // Verify Outer uniqueification
        assertNotEquals(outerLocalId, outer1.getId());
        assertNotEquals(outerLocalId, outer2.getId());
        assertNotEquals(outer1.getId(), outer2.getId());

        // Verify Inner uniqueification
        assertNotEquals(innerLocalId, inner1.getId());
        assertNotEquals(innerLocalId, inner2.getId());
        assertNotEquals(inner1.getId(), inner2.getId());

        // Verify cross-uniqueness
        assertNotEquals(outer1.getId(), inner1.getId());
        assertNotEquals(outer2.getId(), inner2.getId());
    }

    private <T extends Operation> T findOperation(List<Operation> ops, Class<T> clazz) {
        for (Operation op : ops) {
            if (clazz.isInstance(op)) {
                return clazz.cast(op);
            }
            if (op instanceof Component) {
                T found = findOperation(((Component) op).getList(), clazz);
                if (found != null) return found;
            }
        }
        return null;
    }
}
