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
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ReferencedModifierTest {

    @Test
    public void testReferencedModifierExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);

        int textId = 200;
        buffer.addText(textId, "Hello");

        int refId = 300;
        buffer.addReferencedOperations(refId);
        buffer.addModifierPadding(10, 10, 10, 10);
        buffer.addContainerEnd();

        buffer.addRootStart();
        buffer.addColumnStart(-1, -1, 0, 0, 0);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refId);
        DrawText.apply(buffer.getBuffer(), textId, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        CoreDocument doc = new CoreDocument(new SystemClock());
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();

        // Find ColumnLayout
        ColumnLayout column = null;
        for (Operation op : ops) {
            if (op instanceof RootLayoutComponent) {
                for (Operation child : ((RootLayoutComponent) op).getList()) {
                    if (child instanceof ColumnLayout) {
                        column = (ColumnLayout) child;
                    }
                }
            }
        }

        assertTrue("Should find ColumnLayout", column != null);

        // Check children of ColumnLayout
        // It should contain the content of ReferencedOperations (PaddingModifierOperation)
        // followed by DrawText
        List<ModifierOperation> children = column.getComponentModifiers().getModifiersList();

        boolean foundPadding = false;
        boolean foundDrawText = false;
        for (ModifierOperation op : column.getComponentModifiers().getModifiersList()) {
            if (op instanceof PaddingModifierOperation) {
                foundPadding = true;
            }
        }
        for (Operation op : column.getList()) {
            if (op instanceof DrawText) {
                foundDrawText = true;
            }
        }

        assertTrue("Should have found PaddingModifierOperation", foundPadding);
        assertTrue("Should have found DrawText", foundDrawText);
    }

    @Test
    public void testReferencedModifierOnBox() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);

        int textId = 200;
        buffer.addText(textId, "Hello");

        int refId = 300;
        buffer.addReferencedOperations(refId);
        buffer.addModifierPadding(10, 10, 10, 10);
        buffer.addContainerEnd();

        buffer.addRootStart();
        // Box with referenced modifier
        buffer.addBoxStart(-1, -1, 0, 0);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refId);
        DrawText.apply(buffer.getBuffer(), textId, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        CoreDocument doc = new CoreDocument(new SystemClock());
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();

        // Find Root
        RootLayoutComponent root =
                (RootLayoutComponent) findOperation(ops, RootLayoutComponent.class);
        // Find Box
        androidx.compose.remote.core.operations.layout.managers.BoxLayout box = null;
        for (Operation op : root.getList()) {
            if (op instanceof androidx.compose.remote.core.operations.layout.managers.BoxLayout) {
                box = (androidx.compose.remote.core.operations.layout.managers.BoxLayout) op;
            }
        }

        assertTrue("Should find BoxLayout", box != null);

        // Check modifiers of BoxLayout
        ComponentModifiers modifiers = box.getComponentModifiers();
        boolean foundPadding = false;
        for (ModifierOperation mod : modifiers.getModifiersList()) {
            if (mod instanceof PaddingModifierOperation) {
                foundPadding = true;
            }
        }

        assertTrue("Should have found PaddingModifierOperation in box modifiers", foundPadding);
    }

    @Test
    public void testReferencedModifierSharing() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);

        int textId = 200;
        buffer.addText(textId, "Hello");

        int refId = 300;
        buffer.addReferencedOperations(refId);
        buffer.addModifierPadding(10, 10, 10, 10);
        buffer.addContainerEnd();

        buffer.addRootStart();
        // Box 1
        buffer.addBoxStart(-1, -1, 0, 0);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refId);
        DrawText.apply(buffer.getBuffer(), textId, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();

        // Box 2
        buffer.addBoxStart(-1, -1, 0, 0);
        IncludeReferencedOperations.apply(buffer.getBuffer(), refId);
        DrawText.apply(buffer.getBuffer(), textId, 0, 0, 0, 0, 0f, 0f, false);
        buffer.addContainerEnd();

        buffer.addContainerEnd();

        CoreDocument doc = new CoreDocument(new SystemClock());
        doc.initFromBuffer(buffer);

        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root =
                (RootLayoutComponent) findOperation(ops, RootLayoutComponent.class);

        List<androidx.compose.remote.core.operations.layout.managers.BoxLayout> boxes =
                new ArrayList<>();
        for (Operation op : root.getList()) {
            if (op instanceof androidx.compose.remote.core.operations.layout.managers.BoxLayout) {
                boxes.add((androidx.compose.remote.core.operations.layout.managers.BoxLayout) op);
            }
        }

        assertEquals(2, boxes.size());

        PaddingModifierOperation pad1 = null;
        for (ModifierOperation mod : boxes.get(0).getComponentModifiers().getModifiersList()) {
            if (mod instanceof PaddingModifierOperation) {
                pad1 = (PaddingModifierOperation) mod;
            }
        }

        PaddingModifierOperation pad2 = null;
        for (ModifierOperation mod : boxes.get(1).getComponentModifiers().getModifiersList()) {
            if (mod instanceof PaddingModifierOperation) {
                pad2 = (PaddingModifierOperation) mod;
            }
        }

        assertTrue("Box 1 should have padding", pad1 != null);
        assertTrue("Box 2 should have padding", pad2 != null);
        assertTrue("Padding instances should be different", pad1 != pad2);
    }

    private <T extends Operation> T findOperation(List<Operation> ops, Class<T> clazz) {
        for (Operation op : ops) {
            if (clazz.isInstance(op)) {
                return clazz.cast(op);
            }
            if (op instanceof androidx.compose.remote.core.operations.layout.Component) {
                T found =
                        findOperation(
                                ((androidx.compose.remote.core.operations.layout.Component) op)
                                        .getList(),
                                clazz);
                if (found != null) return found;
            }
        }
        return null;
    }
}
