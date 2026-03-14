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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteComposeState;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.IntegerExpression;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.HashMap;

@RunWith(JUnit4.class)
public class ValueIntegerExpressionChangeActionOperationTest {

    @Test
    public void testIncrementMultipleTimes() throws Exception {
        CoreDocument document = new CoreDocument();
        RemoteContext context = mock(RemoteContext.class);
        RemoteComposeState state = new RemoteComposeState();

        // mock context behavior using state
        doAnswer(invocation -> {
            int id = invocation.getArgument(0);
            int value = invocation.getArgument(1);
            state.overrideInteger(id, value);
            return null;
        }).when(context).overrideInteger(anyInt(), anyInt());

        when(context.getInteger(anyInt())).thenAnswer(invocation -> {
            int id = invocation.getArgument(0);
            return state.getInteger(id);
        });

        when(context.getAnimationTime()).thenReturn(0f);
        when(context.getCollectionsAccess()).thenReturn(state);

        // target id
        int targetId = 42;
        state.overrideInteger(targetId, 0);

        // create expression: targetId + 1
        int exprId = 10;
        int mask = 1 | (1 << 2); // bit 0 is ID, bit 2 is OP_ADD
        int[] srcValue = new int[]{targetId, 1, IntegerExpressionEvaluator.I_ADD};

        Field field = CoreDocument.class.getDeclaredField("mIntegerExpressions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<Long, IntegerExpression> map = (HashMap<Long, IntegerExpression>) field.get(
                document);
        map.put((long) exprId, new IntegerExpression(exprId, mask, srcValue));

        // Use targetId + 0x100000000L exactly as the remote compose engine does
        ValueIntegerExpressionChangeActionOperation op =
                new ValueIntegerExpressionChangeActionOperation(targetId + 0x100000000L,
                        exprId + 0x100000000L);

        Component comp = mock(Component.class);

        assertEquals(0, state.getInteger(targetId));

        op.runAction(context, document, comp, 0f, 0f);
        assertEquals(1, state.getInteger(targetId));

        op.runAction(context, document, comp, 0f, 0f);
        assertEquals(2, state.getInteger(targetId));

        op.runAction(context, document, comp, 0f, 0f);
        assertEquals(3, state.getInteger(targetId));
    }
}
