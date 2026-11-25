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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.loom.RemapContext;
import androidx.compose.remote.core.testing.HeadlessRemoteContext;
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class ClipPathModifierOperationTest {

    private static final int PATH_ID = 101;

    @Test
    public void writeThenRead_roundTripsFields() {
        WireBuffer buffer = new WireBuffer();
        ClipPathModifierOperation.apply(buffer, PATH_ID);

        // opcode byte + pathId int
        assertEquals(1 + 4, buffer.getIndex());

        buffer.setIndex(0);
        assertEquals(Operations.MODIFIER_CLIP_PATH, buffer.readByte());
        List<Operation> operations = new ArrayList<>();
        ClipPathModifierOperation.read(buffer, operations);

        assertEquals(1, operations.size());
        ClipPathModifierOperation op = (ClipPathModifierOperation) operations.get(0);
        assertEquals(PATH_ID, op.getPathId());
    }

    @Test
    public void write_matchesApply() {
        WireBuffer expected = new WireBuffer();
        ClipPathModifierOperation.apply(expected, PATH_ID);

        WireBuffer actual = new WireBuffer();
        new ClipPathModifierOperation(PATH_ID).write(actual);

        assertEquals(expected.getIndex(), actual.getIndex());
        for (int i = 0; i < expected.getIndex(); i++) {
            assertEquals("byte " + i, expected.getBuffer()[i], actual.getBuffer()[i]);
        }
    }

    @Test
    public void read_remapsPathIdWhenExpandedByLoom() {
        WireBuffer buffer = new WireBuffer();
        ClipPathModifierOperation.apply(buffer, PATH_ID);
        buffer.setIndex(0);
        buffer.readByte(); // opcode

        RemapContext remap = new RemapContext(new CoreDocument());
        remap.addMapping(PATH_ID, 501);
        List<Operation> operations = new ArrayList<>();
        ClipPathModifierOperation.read(remap.wrap(buffer), operations);

        ClipPathModifierOperation op = (ClipPathModifierOperation) operations.get(0);
        assertEquals(501, op.getPathId());
    }

    @Test
    public void paint_intersectsClipWithPath() {
        final int[] clipped = new int[] {-1, -1};
        final int[] calls = new int[] {0};
        SoftwareRasterPaintContext paintContext =
                new SoftwareRasterPaintContext(new HeadlessRemoteContext(), 10, 10) {
                    @Override
                    public void clipPath(int pathId, int regionOp) {
                        calls[0]++;
                        clipped[0] = pathId;
                        clipped[1] = regionOp;
                    }
                };

        new ClipPathModifierOperation(PATH_ID).paint(paintContext);

        assertEquals(1, calls[0]);
        assertEquals(PATH_ID, clipped[0]);
        assertEquals(ClipPath.INTERSECT, clipped[1]);
    }

    @Test
    public void isAnExperimentalOperation() {
        int apiLevel = CoreDocument.DOCUMENT_API_LEVEL;
        assertFalse(
                Operations.valid(
                        Operations.MODIFIER_CLIP_PATH, apiLevel, RcProfiles.PROFILE_ANDROIDX));
        assertFalse(
                Operations.valid(
                        Operations.MODIFIER_CLIP_PATH, apiLevel, RcProfiles.PROFILE_WIDGETS));
        assertTrue(
                Operations.valid(
                        Operations.MODIFIER_CLIP_PATH,
                        apiLevel,
                        RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL));
        assertTrue(
                Operations.valid(
                        Operations.MODIFIER_CLIP_PATH,
                        apiLevel,
                        RcProfiles.PROFILE_WIDGETS | RcProfiles.PROFILE_EXPERIMENTAL));
    }

    @Test
    public void document_attachesModifierToComponent() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL);
        short[] tags = {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values = {100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL};
        buffer.addHeader(tags, values);
        buffer.addPathData(PATH_ID, new float[] {0f, 0f, 10f, 10f});

        buffer.addRootStart();
        buffer.addBoxStart(-1, -1, 0, 0);
        buffer.addClipPathModifier(PATH_ID);
        buffer.addContainerEnd();
        buffer.addContainerEnd();

        CoreDocument document = new CoreDocument(new SystemClock());
        document.initFromBuffer(buffer);

        RootLayoutComponent root = document.getRootLayoutComponent();
        assertNotNull(root);
        BoxLayout box = null;
        for (Operation op : root.getList()) {
            if (op instanceof BoxLayout) {
                box = (BoxLayout) op;
            }
        }
        assertNotNull("Should find BoxLayout", box);

        ClipPathModifierOperation clip = null;
        for (ModifierOperation modifier : box.getComponentModifiers().getModifiersList()) {
            if (modifier instanceof ClipPathModifierOperation) {
                clip = (ClipPathModifierOperation) modifier;
            }
        }
        assertNotNull("Should find ClipPathModifierOperation in box modifiers", clip);
        assertEquals(PATH_ID, clip.getPathId());
        assertTrue(document.displayHierarchy().contains("CLIP_PATH = [" + PATH_ID + "]"));
    }
}
