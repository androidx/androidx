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

import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.loom.PatternDefine;
import androidx.compose.remote.core.operations.loom.PatternInflation;

import org.junit.Test;

import java.util.List;

public class MacroSystemBypassTest {

    @Test
    public void testSystemVariableAsMacroParameter() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a macro with one parameter
        int macroId = 100;
        int paramId = 1000;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {paramId});

        // Inside the macro, create a FloatExpression using the parameter ID
        // Note: we use Utils.asNan(paramId) to indicate it's a parameter reference
        FloatExpression.apply(buffer.getBuffer(), 2000, new float[] {Utils.asNan(paramId)}, null);

        ContainerEnd.apply(buffer.getBuffer());

        // Now call the macro, passing a system variable ID (e.g. ID_WINDOW_WIDTH = 5)
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        PatternInflation.apply(
                buffer.getBuffer(), macroId, new int[] {RemoteContext.ID_WINDOW_WIDTH});
        ContainerEnd.apply(buffer.getBuffer());
        ContainerEnd.apply(buffer.getBuffer());

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        // Verify the expanded operation
        List<Operation> ops = doc.getOperations();
        FloatExpression floatExpr = findOperation(ops, FloatExpression.class);

        assertNotNull("FloatExpression should be found", floatExpr);
        // The srcValue[0] should be remapped from paramId (1000) to ID_WINDOW_WIDTH (5)
        assertEquals(
                "Parameter should be replaced by system variable ID",
                RemoteContext.ID_WINDOW_WIDTH,
                Utils.idFromNan(floatExpr.mSrcValue[0]));

        // Also verify that the FloatExpression's own ID (2000) might have been remapped
        // because it's NOT a system ID. (In this simple document it might stay 2000 if not
        // already used)
    }

    @Test
    public void testSystemVariableDirectUseInMacro() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a macro that uses a system variable ID directly (not as a parameter)
        int macroId = 101;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {});

        // FloatExpression with ID = ID_TIME_IN_SEC (2)
        FloatExpression.apply(
                buffer.getBuffer(), RemoteContext.ID_TIME_IN_SEC, new float[] {0f}, null);

        ContainerEnd.apply(buffer.getBuffer());

        // Call the macro twice to see if the system ID is preserved in both expansions
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

        int count = 0;
        for (Operation op : root.getList()) {
            if (op instanceof FloatExpression) {
                FloatExpression fe = (FloatExpression) op;
                assertEquals(
                        "System ID should be preserved across macro expansions",
                        RemoteContext.ID_TIME_IN_SEC,
                        fe.mId);
                count++;
            }
        }
        assertEquals("Should have two FloatExpressions", 2, count);
    }

    @Test
    public void testNormalVariableRemapping() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Define a macro that uses a normal variable ID (e.g. 1000)
        int macroId = 101;
        int normalVarId = 1000;
        PatternDefine.apply(buffer.getBuffer(), macroId, new int[] {});

        // FloatExpression with ID = 1000
        FloatExpression.apply(buffer.getBuffer(), normalVarId, new float[] {0f}, null);

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

        int id1 = -1;
        int id2 = -1;
        for (Operation op : root.getList()) {
            if (op instanceof FloatExpression) {
                FloatExpression fe = (FloatExpression) op;
                if (id1 == -1) {
                    id1 = fe.mId;
                } else {
                    id2 = fe.mId;
                }
            }
        }

        assertNotEquals("Normal variable ID should be remapped", normalVarId, id1);
        assertNotEquals("Normal variable ID should be remapped", normalVarId, id2);
        assertNotEquals("Each expansion should have a unique ID", id1, id2);
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
