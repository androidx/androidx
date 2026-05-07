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

import androidx.compose.remote.core.operations.DrawText;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.TextMerge;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;

import org.junit.Test;

import java.util.List;

public class MacroLocalTextMergeTest {

    @Test
    public void testMacroLocalTextMerge() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a macro with two parameters
        int macroId = 100;
        int p1 = 1001;
        int p2 = 1002;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {p1, p2});

        // Inside macro:
        // 1. Merge parameters p1 and p2 into a local variable local1
        int local1 = 0x4001;
        TextMerge.apply(buffer.getBuffer(), local1, p1, p2);

        // 2. Define a constant suffix in local2
        int local2 = 0x4002;
        TextData.apply(buffer.getBuffer(), local2, "!");

        // 3. Merge local1 and local2 into local3
        int local3 = 0x4003;
        TextMerge.apply(buffer.getBuffer(), local3, local1, local2);

        // 4. Use local3 in a DrawText
        DrawText.apply(buffer.getBuffer(), local3, 0, 0, 0, 0, 10f, 20f, false);

        ContainerEnd.apply(buffer.getBuffer());

        // External string data for arguments
        int arg1 = 2001;
        int arg2 = 2002;
        TextData.apply(buffer.getBuffer(), arg1, "Hello");
        TextData.apply(buffer.getBuffer(), arg2, " World");

        // Call the macro
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {arg1, arg2});
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // Verify expansion
        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root = findOperation(ops, RootLayoutComponent.class);
        assertNotNull(root);

        List<Operation> expanded = root.getList();
        // Expected ops in expansion: TextMerge, TextData, TextMerge, DrawText

        TextMerge tm1 = (TextMerge) expanded.get(0);
        TextData tdSuffix = (TextData) expanded.get(1);
        TextMerge tm2 = (TextMerge) expanded.get(2);
        DrawText dt = (DrawText) expanded.get(3);

        // Assertions for unique-ification
        assertNotEquals("local1 should be remapped", local1, tm1.getId());
        assertNotEquals("local2 should be remapped", local2, tdSuffix.getId());
        assertNotEquals("local3 should be remapped", local3, tm2.getId());

        // Assertions for parameter substitution
        assertEquals("tm1 source 1 should be arg1", arg1, tm1.mSrcId1);
        assertEquals("tm1 source 2 should be arg2", arg2, tm1.mSrcId2);

        // Assertions for local propagation
        assertEquals("tm1 result should be tm2 source 1", tm1.getId(), tm2.mSrcId1);
        assertEquals("tdSuffix id should be tm2 source 2", tdSuffix.getId(), tm2.mSrcId2);
        assertEquals("tm2 result should be dt textId", tm2.getId(), dt.mTextID);
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
