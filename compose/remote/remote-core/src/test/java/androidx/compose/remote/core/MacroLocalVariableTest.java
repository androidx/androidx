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
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.IntegerExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.ClickModifierOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.ImageLayout;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.core.types.IntegerConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.List;

public class MacroLocalVariableTest {

    private static class MockRemoteContext extends RemoteContext {
        MockRemoteContext(CoreDocument document) {
            super(RemoteClock.SYSTEM);
            mDocument = document;
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {}

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return null;
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {}

        @Override
        public void loadColor(int id, int color) {
            mRemoteComposeState.updateColor(id, color);
        }

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {}

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {}

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {}

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {}

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {}

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {}

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {}

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {}

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {}

        @Override
        public void setNamedLong(@NonNull String name, long value) {}

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {}

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {}

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {}

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {}

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return null;
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {}

        @Override
        public void runNamedAction(int id, @Nullable Object value) {}

        @Override
        public void putObject(int id, @NonNull Object value) {
            mRemoteComposeState.updateData(id, value);
        }

        @Override
        public @Nullable Object getObject(int id) {
            return mRemoteComposeState.getObject(id);
        }

        @Override
        public void hapticEffect(int type) {}

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {}

        @Override
        public void loadText(int id, @NonNull String text) {
            mRemoteComposeState.updateData(id, text);
        }

        @Override
        public @Nullable String getText(int id) {
            return (String) mRemoteComposeState.getObject(id);
        }

        @Override
        public void loadFloat(int id, float value) {
            mRemoteComposeState.updateFloat(id, value);
        }

        @Override
        public void overrideFloat(int id, float value) {
            mRemoteComposeState.updateFloat(id, value);
        }

        @Override
        public void loadInteger(int id, int value) {
            mRemoteComposeState.updateInteger(id, value);
        }

        @Override
        public void overrideInteger(int id, int value) {
            mRemoteComposeState.updateInteger(id, value);
        }

        @Override
        public void overrideText(int id, int valueId) {}

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {}

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {}

        @Override
        public float getFloat(int id) {
            return mRemoteComposeState.getFloat(id);
        }

        @Override
        public int getInteger(int id) {
            return mRemoteComposeState.getInteger(id);
        }

        @Override
        public long getLong(int id) {
            return 0;
        }

        @Override
        public int getColor(int id) {
            return mRemoteComposeState.getColor(id);
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public ShaderData getShader(int id) {
            return null;
        }

        @Override
        public void addClickArea(
                int id,
                int contentId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {}
    }

    @Test
    public void testMacroLocalVariableUniqueification() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a macro with a local variable in the Tier 2 range (0x4000 - 0x4FFF)
        int macroId = 100;
        int localId = 0x4001;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {});

        // 1. Define a local FloatConstant
        FloatConstant.apply(buffer.getBuffer(), localId, 42f);

        // 2. Define a local TextData (using same range for simplicity of the test)
        int localTextId = 0x4002;
        TextData.apply(buffer.getBuffer(), localTextId, "Value: ");

        // 3. Use them in a DrawText (referencing the local IDs)
        // We need to use NaN-encoded ID for X to test DrawText's remap
        DrawText.apply(
                buffer.getBuffer(), localTextId, 0, 0, 0, 0, Utils.asNan(localId), 50f, false);

        ContainerEnd.apply(buffer.getBuffer());

        // Call the macro twice
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer());

        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer());

        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // Verify the expanded operations
        List<Operation> ops = doc.getOperations();
        RootLayoutComponent root = findOperation(ops, RootLayoutComponent.class);
        assertNotNull(root);

        // We expect 2 sets of (FloatConstant, TextData, DrawText)
        List<Operation> expanded = root.getList();

        // Find first set
        FloatConstant fc1 = (FloatConstant) expanded.get(0);
        TextData td1 = (TextData) expanded.get(1);
        DrawText dt1 = (DrawText) expanded.get(2);

        // Find second set
        FloatConstant fc2 = (FloatConstant) expanded.get(3);
        TextData td2 = (TextData) expanded.get(4);
        DrawText dt2 = (DrawText) expanded.get(5);

        // Assertions for Instance 1
        assertNotEquals("Local ID should be unique-ified", localId, fc1.getId());
        assertNotEquals("Local Text ID should be unique-ified", localTextId, td1.getId());
        assertEquals(
                "DrawText should reference unique-ified local ID",
                fc1.getId(),
                Utils.idFromNan(dt1.mX));
        assertEquals("DrawText should reference unique-ified text ID", td1.getId(), dt1.mTextID);

        // Assertions for Instance 2
        assertNotEquals("Local ID should be unique-ified", localId, fc2.getId());
        assertNotEquals("Local Text ID should be unique-ified", localTextId, td2.getId());
        assertEquals(
                "DrawText should reference unique-ified local ID",
                fc2.getId(),
                Utils.idFromNan(dt2.mX));
        assertEquals("DrawText should reference unique-ified text ID", td2.getId(), dt2.mTextID);

        // Assertions for Uniqueness across instances
        assertNotEquals("Each instance should have its own unique IDs", fc1.getId(), fc2.getId());
        assertNotEquals("Each instance should have its own unique IDs", td1.getId(), td2.getId());
    }

    @Test
    public void testNonMacroExpressionWithClick() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        int varId = 42;
        int exprId = 43;

        // Define variable and expression (non-macro)
        IntegerConstant.apply(buffer.getBuffer(), varId, 0);

        // Create a toggle expression: (varId + 1) % 2
        // Mask: 1 (varId), 0 (1), 1 (ADD), 0 (2), 1 (MOD) -> 10101 = 0x15
        int[] exprData =
                new int[] {
                    varId, 1, IntegerExpressionEvaluator.I_ADD, 2, IntegerExpressionEvaluator.I_MOD
                };
        IntegerExpression.apply(buffer.getBuffer(), exprId, 0x15, exprData);

        // root component
        RootLayoutComponent.apply(buffer.getBuffer(), -1);

        // box component
        ImageLayout.apply(buffer.getBuffer(), -1, -1, -1, (short) 0, 1f);

        // click modifier
        ClickModifierOperation.apply(buffer.getBuffer());
        // toggle action: varId = exprId
        ValueIntegerExpressionChangeActionOperation.apply(
                buffer.getBuffer(), varId + 0x100000000L, exprId + 0x100000000L);
        ContainerEnd.apply(buffer.getBuffer());

        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // Verify expression is collected
        IntegerExpression expression = doc.getIntegerExpressions().get((long) exprId);
        assertNotNull("Expression should be collected", expression);
        assertEquals("Expression ID should match", exprId, expression.getId());

        // Simulate click
        RemoteContext context = new MockRemoteContext(doc);
        context.mRemoteComposeState.updateInteger(varId, 0);

        RootLayoutComponent root = doc.getRootLayoutComponent();
        ImageLayout box = (ImageLayout) root.getList().get(0);

        // Actually Component.onClick handles the traversal, let's just call that!
        box.onClick(context, doc, -1, -1);
        // Verify variable updated
        assertEquals("Variable should be toggled to 1", 1, context.getInteger(varId));

        // Run action again
        box.onClick(context, doc, -1, -1);
        assertEquals("Variable should be toggled to 0", 0, context.getInteger(varId));
    }

    @Test
    public void testMacroModifierLocalVariableUniqueification() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a "Style Macro" with a local variable
        int macroId = 100;
        int localId = 0x4001;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {});
        FloatConstant.apply(buffer.getBuffer(), localId, 10f);
        // Use localId as a parameter to some modifier (e.g. padding - just simulating with a
        // custom op if needed,
        // but MacroCall as modifier just emits its list)
        // Here we just put the FloatConstant in the macro.
        // When MacroCall is a modifier, it expands its content into the component's modifier list.
        ContainerEnd.apply(buffer.getBuffer());

        RootLayoutComponent.apply(buffer.getBuffer(), -1);

        // Component 1 using the macro modifier
        ImageLayout.apply(buffer.getBuffer(), -1, -1, -1, (short) 0, 1f);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer()); // end macro call
        ContainerEnd.apply(buffer.getBuffer()); // end ImageLayout

        // Component 2 using the same macro modifier
        ImageLayout.apply(buffer.getBuffer(), -1, -1, -1, (short) 0, 1f);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {});
        ContainerEnd.apply(buffer.getBuffer()); // end macro call
        ContainerEnd.apply(buffer.getBuffer()); // end ImageLayout

        ContainerEnd.apply(buffer.getBuffer()); // end Root

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        RootLayoutComponent root = doc.getRootLayoutComponent();
        ImageLayout box1 = (ImageLayout) root.getList().get(0);
        ImageLayout box2 = (ImageLayout) root.getList().get(1);

        FloatConstant fc1 = findOperation(box1.getList(), FloatConstant.class);
        FloatConstant fc2 = findOperation(box2.getList(), FloatConstant.class);

        assertNotNull("FloatConstant should be found in box1 modifiers", fc1);
        assertNotNull("FloatConstant should be found in box2 modifiers", fc2);

        assertNotEquals("Local ID should be unique-ified", localId, fc1.getId());
        assertNotEquals("Local ID should be unique-ified", localId, fc2.getId());
        assertNotEquals("Each instance should have its own unique IDs", fc1.getId(), fc2.getId());
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
