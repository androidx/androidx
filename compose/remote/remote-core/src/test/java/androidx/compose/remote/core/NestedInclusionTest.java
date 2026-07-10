/*
 * Copyright (C) 2024 The Android Open Source Project
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
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.DrawText;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.IncludeReferencedOperations;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.loom.PatternArgument;
import androidx.compose.remote.core.operations.loom.PatternBlock;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;

import org.junit.Test;

import java.util.List;

public class NestedInclusionTest {

    @Test
    public void testRecursiveInclusion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);

        int textId1 = 201;
        int textId2 = 202;
        buffer.addText(textId1, "Level 1");
        buffer.addText(textId2, "Level 2");

        int refIdB = 302;
        int refIdA = 301;

        // Block B: just a text
        buffer.addReferencedOperations(refIdB);
        DrawText.apply(buffer.getBuffer(), textId2, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();

        // Block A: a Row containing an include(B)
        buffer.addReferencedOperations(refIdA);
        buffer.addRowStart(-1, -1, 0, 0, 0);
        DrawText.apply(buffer.getBuffer(), textId1, 0, 0, 0, 0, 0f, 0f, false);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refIdB);
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        buffer.addRootStart();
        // Include A
        IncludeReferencedOperations.apply(buffer.getBuffer(), refIdA);
        buffer.addContainerEnd();

        CoreDocument doc = new CoreDocument(new SystemClock());
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root = (RootLayoutComponent) ops.get(ops.size() - 1);

        // Root -> Row (from A) -> [Text1, Text2 (from B)]
        assertTrue("Root should have 1 child", root.getList().size() == 1);
        assertTrue("Child should be RowLayout", root.getList().get(0) instanceof RowLayout);

        RowLayout row = (RowLayout) root.getList().get(0);
        // Children list: [Text1, Text2, ComponentModifiers]
        assertEquals(
                "Row should have 3 children (Text1, Text2, Modifiers)", 3, row.getList().size());
        assertTrue("First child should be DrawText", row.getList().get(0) instanceof DrawText);
        assertEquals(
                "First textId should be textId1",
                textId1,
                ((DrawText) row.getList().get(0)).mTextID);

        assertTrue(
                "Second child should be DrawText (expanded from B)",
                row.getList().get(1) instanceof DrawText);
        assertEquals(
                "Second textId should be textId2",
                textId2,
                ((DrawText) row.getList().get(1)).mTextID);
    }

    @Test
    public void testIncludeInMacroBlock() {
        // Test that include() works when passed inside a MacroBlock (argument substitution)
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);

        int textId = 200;
        buffer.addText(textId, "Shared");

        int refId = 300;
        buffer.addReferencedOperations(refId);
        DrawText.apply(buffer.getBuffer(), textId, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();

        // Define Macro: takes 1 argument placeholder
        int macroId = 201;
        buffer.addText(macroId, "MyMacro");
        int param1 = 1001;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {param1});
        buffer.addColumnStart(-1, -1, 0, 0, 0);
        PatternArgument.apply(buffer.getBuffer(), 0); // placeholder for param1
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        buffer.addRootStart();
        // Call Macro: provide include(refId) as the block for the argument
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {0});
        PatternBlock.apply(buffer.getBuffer(), 0);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refId);
        buffer.addContainerEnd();
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        CoreDocument doc = new CoreDocument(new SystemClock());
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root = (RootLayoutComponent) ops.get(ops.size() - 1);

        // Root -> Column -> [DrawText (expanded from refId through MacroBlock), ComponentModifiers]
        ColumnLayout col = (ColumnLayout) root.getList().get(0);
        assertEquals("Column should have 2 children (Text, Modifiers)", 2, col.getList().size());
        assertTrue("Child should be DrawText", col.getList().get(0) instanceof DrawText);
        assertEquals(textId, ((DrawText) col.getList().get(0)).mTextID);
    }
}
