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
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.DataListIds;
import androidx.compose.remote.core.operations.DrawRect;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.loom.PatternArgument;
import androidx.compose.remote.core.operations.loom.PatternBlock;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternForEach;
import androidx.compose.remote.core.operations.loom.PatternInflation;

import org.junit.Test;

import java.util.List;

public class PatternBlockTest {

    @Test
    public void testMultiSlotMacroBlockExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);
        int macroId = 100;

        // Define a macro with two slots
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {});
        // Slot 0
        PatternArgument.apply(buffer.getBuffer(), 0);
        // Slot 1
        PatternArgument.apply(buffer.getBuffer(), 1);
        ContainerEnd.apply(buffer.getBuffer());

        // Call the macro with two blocks
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {});

        // Block 0
        PatternBlock.apply(buffer.getBuffer(), 0);
        DrawRect.apply(buffer.getBuffer(), 0f, 0f, 10f, 10f);
        ContainerEnd.apply(buffer.getBuffer());

        // Block 1
        PatternBlock.apply(buffer.getBuffer(), 1);
        DrawRect.apply(buffer.getBuffer(), 20f, 20f, 30f, 30f);
        ContainerEnd.apply(buffer.getBuffer());

        ContainerEnd.apply(buffer.getBuffer()); // End MacroCall
        ContainerEnd.apply(buffer.getBuffer()); // End Root

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        List<Operation> expanded = doc.getOperations();
        RootLayoutComponent root = null;
        for (Operation op : expanded) {
            if (op instanceof RootLayoutComponent) {
                root = (RootLayoutComponent) op;
                break;
            }
        }

        List<Operation> rootChildren = root.getList();
        // The MacroCall should have been replaced by the contents of its two blocks.
        assertEquals(2, rootChildren.size());
        assertTrue(rootChildren.get(0) instanceof DrawRect);
        assertTrue(rootChildren.get(1) instanceof DrawRect);

        DrawRect dr1 = (DrawRect) rootChildren.get(0);
        DrawRect dr2 = (DrawRect) rootChildren.get(1);

        assertEquals(0f, dr1.getX1(), 0.01f);
        assertEquals(20f, dr2.getX1(), 0.01f);
    }

    @Test
    public void testMacroForEachExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        int collectionId = 200;
        int item1Id = 301;
        int item2Id = 302;
        int item3Id = 303;

        // 1. Create a collection of IDs
        DataListIds.apply(buffer.getBuffer(), collectionId, new int[] {item1Id, item2Id, item3Id});

        // 2. Define a macro that iterates over a collection parameter
        int macroId = 100;
        int collectionParam = 50; // parameter ID
        int localItemId = 0x4001; // Tier 2 macro-local ID

        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {collectionParam});
        // Iterate over the passed collection
        PatternForEach.apply(buffer.getBuffer(), collectionParam, localItemId);
        // For each item, draw a rect using the item ID as coordinate (just for verification)
        DrawRect.apply(buffer.getBuffer(), Utils.asNan(localItemId), 0f, 10f, 10f);
        ContainerEnd.apply(buffer.getBuffer()); // end for-each
        ContainerEnd.apply(buffer.getBuffer()); // end macro

        // 3. Call the macro
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {collectionId});
        ContainerEnd.apply(buffer.getBuffer()); // end macro call
        ContainerEnd.apply(buffer.getBuffer()); // end root

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        RootLayoutComponent root = null;
        for (Operation op : doc.getOperations()) {
            if (op instanceof RootLayoutComponent) {
                root = (RootLayoutComponent) op;
                break;
            }
        }
        List<Operation> expanded = root.getList();

        // MacroForEach should have expanded into 3 DrawRect operations
        assertEquals(3, expanded.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(expanded.get(i) instanceof DrawRect);
            DrawRect dr = (DrawRect) expanded.get(i);
            // Verify that the localItemId was correctly remapped to the collection item
            int expectedId = 301 + i;
            assertEquals(expectedId, Utils.idFromNan(dr.getX1()));
        }
    }
}
