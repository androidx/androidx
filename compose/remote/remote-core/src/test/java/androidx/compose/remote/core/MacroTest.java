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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.DrawText;
import androidx.compose.remote.core.operations.FloatConstant;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.IntegerExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.StateLayout;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ValueIntegerExpressionChangeActionOperation;
import androidx.compose.remote.core.operations.loom.PatternArgument;
import androidx.compose.remote.core.operations.loom.PatternBlock;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.types.IntegerConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

public class MacroTest {

    private static class MockRemoteContext extends RemoteContext {
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
        public void overrideFloat(int id, float value) {}

        @Override
        public void loadInteger(int id, int value) {
            mRemoteComposeState.updateInteger(id, value);
        }

        @Override
        public void overrideInteger(int id, int value) {}

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
    public void testFloatParameterExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Data (Top Level)
        int labelId = 201;
        TextData.apply(buffer.getBuffer(), labelId, "Inside Box");
        int fsId = 301;
        FloatConstant.apply(buffer.getBuffer(), fsId, 45f);

        // 2. Define Macro (Top Level)
        int macroId = 101;
        int pLabel = 1;
        int pSize = 2;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pLabel, pSize});

        // TEMPLATE START
        // TextLayout start
        TextLayout.apply(
                buffer.getBuffer(),
                -1,
                -1,
                pLabel,
                0xFF000000,
                Utils.asNan(pSize),
                0,
                400f,
                -1,
                TextLayout.TEXT_ALIGN_CENTER,
                TextLayout.OVERFLOW_CLIP,
                Integer.MAX_VALUE);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Close MacroDefine
        ContainerEnd.apply(buffer.getBuffer());
        // TEMPLATE END

        // 3. Document Content (Top Level)
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {labelId, fsId});
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        List<Operation> ops = doc.getOperations();
        TextLayout text = null;
        for (Operation op : ops) {
            if (op instanceof RootLayoutComponent) {
                for (Operation child : ((RootLayoutComponent) op).getList()) {
                    if (child instanceof TextLayout) {
                        text = (TextLayout) child;
                        break;
                    }
                }
            }
        }

        assertTrue("Should find TextLayout", text != null);
        assertEquals(
                "TextLayout should have remapped fontSize ID",
                (Integer) fsId,
                (Integer) Utils.idFromNan(text.getFontSize()));

        // Verify updateVariables works
        MockRemoteContext context = new MockRemoteContext();
        // Populate context with data from expanded doc
        for (Operation op : ops) {
            if (op instanceof FloatConstant || op instanceof TextData) {
                op.apply(context);
            }
        }

        text.updateVariables(context);
        assertEquals(
                "TextLayout should have fetched correct fontSize value",
                45f,
                text.getFontSizeValue(),
                0.001f);
    }

    @Test
    public void testLayoutMacroExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Data (Top Level)
        int labelId = 201;
        TextData.apply(buffer.getBuffer(), labelId, "Inside Box");

        // 2. Define Macro (Top Level)
        int macroId = 101;
        int pLabel = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pLabel});

        // TEMPLATE START
        // Box start
        BoxLayout.apply(buffer.getBuffer(), -1, -1, BoxLayout.CENTER, BoxLayout.CENTER);
        // Content start (REQUIRED for LayoutComponents)
        LayoutComponentContent.apply(buffer.getBuffer(), -1);

        // TextLayout start
        TextLayout.apply(
                buffer.getBuffer(),
                -1,
                -1,
                pLabel,
                0xFF000000,
                20f,
                0,
                400f,
                -1,
                TextLayout.TEXT_ALIGN_CENTER,
                TextLayout.OVERFLOW_CLIP,
                Integer.MAX_VALUE);
        // TextLayout content start
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        // TextLayout content end
        ContainerEnd.apply(buffer.getBuffer());
        // TextLayout end
        ContainerEnd.apply(buffer.getBuffer());

        // Content end (for BoxLayout)
        ContainerEnd.apply(buffer.getBuffer());
        // Box end
        ContainerEnd.apply(buffer.getBuffer());

        // Close MacroDefine
        ContainerEnd.apply(buffer.getBuffer());
        // TEMPLATE END

        // 3. Document Content (Top Level)
        // RootLayoutComponent
        RootLayoutComponent.apply(buffer.getBuffer(), -1);

        // Call Macro inside Root
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {labelId});

        // CRITICAL: MacroCall is a container, needs an end
        ContainerEnd.apply(buffer.getBuffer());

        // Root end
        ContainerEnd.apply(buffer.getBuffer());

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        List<Operation> ops = doc.getOperations();
        System.out.println("Layout operations after expansion (Root + BoxLayout + TextLayout):");
        for (Operation op : ops) {
            System.out.println("  " + op);
            if (op instanceof Container) {
                printChildren((Container) op, "    ");
            }
        }

        // Find RootLayoutComponent
        RootLayoutComponent root = null;
        for (Operation op : ops) {
            if (op instanceof RootLayoutComponent) {
                root = (RootLayoutComponent) op;
                break;
            }
        }
        assertTrue("Should find RootLayoutComponent", root != null);

        // Find BoxLayout inside Root
        BoxLayout box = null;
        for (Operation op : root.getList()) {
            if (op instanceof BoxLayout) {
                box = (BoxLayout) op;
                break;
            }
        }

        assertTrue("Should find BoxLayout inside Root", box != null);

        // After inflate(), children are moved from LayoutComponentContent to mList
        TextLayout text = null;
        for (Operation child : box.getList()) {
            if (child instanceof TextLayout) {
                text = (TextLayout) child;
                break;
            }
        }

        assertTrue("BoxLayout should contain TextLayout after inflation", text != null);
        assertEquals(
                "TextLayout should have remapped labelId",
                (Integer) labelId,
                (Integer) text.getTextId());
    }

    @Test
    public void testStateLayoutMacroExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Define Macro with StateLayout
        int macroId = 101;
        int pState = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pState});

        // TEMPLATE START
        // StateLayout start
        StateLayout.apply(buffer.getBuffer(), -1, -1, 0, 0, pState);
        // Content start
        LayoutComponentContent.apply(buffer.getBuffer(), -1);

        // State 0: Box
        BoxLayout.apply(buffer.getBuffer(), -1, -1, BoxLayout.CENTER, BoxLayout.CENTER);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        // Content end (for StateLayout)
        ContainerEnd.apply(buffer.getBuffer());
        // StateLayout end
        ContainerEnd.apply(buffer.getBuffer());

        // Close MacroDefine
        ContainerEnd.apply(buffer.getBuffer());
        // TEMPLATE END

        // 2. Document Content
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(
                buffer.getBuffer(), macroId, new int[] {200}); // state index variable id 200
        ContainerEnd.apply(buffer.getBuffer()); // end MacroCall
        ContainerEnd.apply(buffer.getBuffer()); // end LCC
        ContainerEnd.apply(buffer.getBuffer()); // end Root
        ContainerEnd.apply(buffer.getBuffer()); // end Document (optional but good)

        // 3. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 4. Verify
        List<Operation> ops = doc.getOperations();
        System.out.println("StateLayout expansion tree:");
        printTree(ops, "");
        StateLayout stateLayout = findOperation(ops, StateLayout.class);

        assertNotNull("Should find StateLayout after expansion", stateLayout);
        assertTrue("StateLayout should have correct class", stateLayout instanceof StateLayout);
        // The toString should be "STATE_LAYOUT" not "UNKNOWN LAYOUT_COMPONENT"
        assertTrue(
                "StateLayout toString should be correct: " + stateLayout,
                stateLayout.toString().contains("STATE_LAYOUT"));
    }

    private void printTree(List<Operation> ops, String indent) {
        for (Operation op : ops) {
            System.out.println(indent + op.toString());
            if (op instanceof Container) {
                printTree(((Container) op).getList(), indent + "  ");
            }
        }
    }

    private <T extends Operation> T findOperation(List<Operation> ops, Class<T> clazz) {
        for (Operation op : ops) {
            if (clazz.isInstance(op)) {
                return clazz.cast(op);
            }
            if (op instanceof Container) {
                T found = findOperation(((Container) op).getList(), clazz);
                if (found != null) {
                    return found;
                }
            }
            if (op instanceof ComponentModifiers) {
                for (ModifierOperation mod : ((ComponentModifiers) op).getModifiersList()) {
                    if (clazz.isInstance(mod)) {
                        return clazz.cast(mod);
                    }
                    if (mod instanceof Container) {
                        T found = findOperation(((Container) mod).getList(), clazz);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void printChildren(Container container, String indent) {
        for (Operation op : container.getList()) {
            System.out.println(indent + op);
            if (op instanceof Container) {
                printChildren((Container) op, indent + "  ");
            }
        }
    }

    @Test
    public void testBasicMacroExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Data (Top Level)
        int labelId = 201;
        TextData.apply(buffer.getBuffer(), labelId, "Hello Macro");

        // 2. Define Macro (Top Level)
        int macroId = 101;
        int pLabel = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pLabel});
        // DrawText Run
        DrawText.apply(buffer.getBuffer(), pLabel, 0, 0, 0, 0, 0f, 0f, false);
        // Macro end
        ContainerEnd.apply(buffer.getBuffer());

        // 3. Call Macro (Top Level)
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {labelId});
        // MacroCall is a container
        ContainerEnd.apply(buffer.getBuffer());

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        List<Operation> ops = doc.getOperations();
        System.out.println("Basic operations after expansion:");
        for (Operation op : ops) {
            System.out.println("  " + op);
        }

        DrawText text = null;
        for (Operation op : ops) {
            if (op instanceof DrawText) {
                text = (DrawText) op;
                break;
            }
        }

        assertTrue("Should find DrawText", text != null);
        assertEquals("DrawText should have remapped labelId", labelId, text.mTextID);
    }

    @Test
    public void testMacroExpansion() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Data
        int labelId = 200;
        TextData.apply(buffer.getBuffer(), labelId, "Hello Macro");

        // 2. Define Macro (Button-like)
        // Params: [P1: labelId]
        // Template: [DrawText(P1), MacroArgument(0)]
        int macroId = 100;
        int p1 = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {p1});
        DrawText.apply(buffer.getBuffer(), p1, 0, 0, 0, 0, 0f, 0f, false);
        PatternArgument.apply(buffer.getBuffer(), 0);
        ContainerEnd.apply(buffer.getBuffer()); // end macro

        // 3. Call Macro
        // Args: [labelId]
        // Blocks: [MacroBlock(0) { DrawText("Clicked") }]
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {labelId});
        PatternBlock.apply(buffer.getBuffer(), 0);
        DrawText.apply(buffer.getBuffer(), 300, 0, 0, 0, 0, 0f, 0f, false);
        ContainerEnd.apply(buffer.getBuffer()); // end block
        ContainerEnd.apply(buffer.getBuffer()); // end call

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        List<Operation> ops = doc.getOperations();
        System.out.println("Operations after expansion:");
        for (Operation op : ops) {
            System.out.println("  " + op);
        }

        // Find expanded DrawText operations
        DrawText dt1 = null;
        DrawText dt2 = null;
        for (Operation op : ops) {
            if (op instanceof DrawText) {
                if (dt1 == null) {
                    dt1 = (DrawText) op;
                } else {
                    dt2 = (DrawText) op;
                }
            }
        }

        assertTrue("Should find first DrawText", dt1 != null);
        assertEquals("First DrawText should have remapped labelId", labelId, dt1.mTextID);

        assertTrue("Should find second DrawText (from block)", dt2 != null);
        assertEquals("Second DrawText should have original block textId", 300, dt2.mTextID);
    }

    @Test
    public void testIntegerExpressionParameterExpansion() throws Exception {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Data (Top Level)
        int globalStateId = 200;
        IntegerConstant.apply(buffer.getBuffer(), globalStateId, 0);

        // 2. Define Macro (Top Level)
        int macroId = 101;
        int pState = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pState});

        // TEMPLATE START
        // IntegerExpression inside macro
        int exprId = 100;
        int[] exprData =
                new int[] {
                    pState,
                    1,
                    androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
                            .I_ADD
                };
        IntegerExpression.apply(buffer.getBuffer(), exprId, 5, exprData);

        // ValueIntegerExpressionChange inside macro
        long taggedPState = ((long) pState) + 0x100000000L;
        long taggedExpr = ((long) exprId) + 0x100000000L;
        ValueIntegerExpressionChangeActionOperation.apply(
                buffer.getBuffer(), taggedPState, taggedExpr);

        ContainerEnd.apply(buffer.getBuffer()); // end macro
        // TEMPLATE END

        // 3. Call Macro
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {globalStateId});
        ContainerEnd.apply(buffer.getBuffer()); // end call

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        List<Operation> ops = doc.getOperations();
        IntegerExpression expandedExpr = findOperation(ops, IntegerExpression.class);
        assertNotNull("Should find IntegerExpression after expansion", expandedExpr);

        // Verify that the parameter was remapped to globalStateId
        assertEquals(
                "Parameter should be remapped to globalStateId",
                globalStateId,
                expandedExpr.mSrcValue[0]);

        ValueIntegerExpressionChangeActionOperation expandedAction =
                findOperation(ops, ValueIntegerExpressionChangeActionOperation.class);
        assertNotNull(
                "Should find ValueIntegerExpressionChangeActionOperation after expansion",
                expandedAction);

        // Verify that the target ID was remapped to globalStateId using reflection
        Field targetField =
                ValueIntegerExpressionChangeActionOperation.class.getDeclaredField(
                        "mTargetValueId");
        targetField.setAccessible(true);
        long targetValueId = (long) targetField.get(expandedAction);
        assertEquals(
                "Target ID should be remapped to globalStateId",
                globalStateId,
                (int) targetValueId);

        // Verify that the expression ID was remapped to a new ID using reflection
        Field exprField =
                ValueIntegerExpressionChangeActionOperation.class.getDeclaredField(
                        "mValueExpressionId");
        exprField.setAccessible(true);
        long valueExpressionId = (long) exprField.get(expandedAction);
        assertTrue("Expression ID should be remapped to a new ID", valueExpressionId != taggedExpr);
    }

    @Test
    public void testNegativeComponentIdDoesNotRuinMaxId() throws Exception {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // 1. Define Macro
        int macroId = 101;
        int pState = 1;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {pState});
        ContainerEnd.apply(buffer.getBuffer()); // end macro

        // 2. Add operation with negative ID (simulating generated component ID)
        int exprId = -2;
        int[] exprData = new int[] {0};
        IntegerExpression.apply(buffer.getBuffer(), exprId, 0, exprData);

        // 3. Call Macro
        PatternInflation.apply(buffer.getBuffer(), macroId, new int[] {200});
        ContainerEnd.apply(buffer.getBuffer()); // end call

        // 4. Inflate and Expand
        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // 5. Verify
        // If it didn't crash, it's success!
        assertTrue(true);
    }
}
