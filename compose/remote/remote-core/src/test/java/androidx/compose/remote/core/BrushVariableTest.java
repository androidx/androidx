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

import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.ComponentValue;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.layout.CanvasOperations;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.lang.reflect.Field;

public class BrushVariableTest {

    private static class TestRemoteContext extends RemoteContext {
        TestRemoteContext() {
            super(RemoteClock.SYSTEM);
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {}

        @Override
        public float @NonNull [] getPathData(int instanceId) {
            return new float[0];
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {}

        @Override
        public void loadColor(int id, int color) {}

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
        public void putObject(int id, @NonNull Object value) {}

        @Override
        public @Nullable Object getObject(int id) {
            return null;
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
        public void loadText(int id, @NonNull String text) {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void loadFloat(int id, float value) {}

        @Override
        public void overrideFloat(int id, float value) {}

        @Override
        public void loadInteger(int id, int value) {}

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
            return 0f;
        }

        @Override
        public int getInteger(int id) {
            return 0;
        }

        @Override
        public long getLong(int id) {
            return 0L;
        }

        @Override
        public int getColor(int id) {
            return 0;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public @Nullable ShaderData getShader(int id) {
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
    public void testBrushVariableRegistration() throws Exception {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        RootLayoutComponent.apply(buffer.getBuffer(), -1);

        int boxId = 10;
        BoxLayout.apply(buffer.getBuffer(), boxId, -1, BoxLayout.CENTER, BoxLayout.CENTER);

        CanvasOperations.apply(buffer.getBuffer());

        int valueId = 20;
        ComponentValue.apply(buffer.getBuffer(), ComponentValue.WIDTH, boxId, valueId);

        ContainerEnd.apply(buffer.getBuffer()); // end CanvasOperations
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer()); // end BoxLayout
        ContainerEnd.apply(buffer.getBuffer()); // end Root

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        doc.applyDataOperations(context);

        // Find BoxLayout
        BoxLayout box = null;
        for (Operation op : doc.getOperations()) {
            if (op instanceof RootLayoutComponent) {
                for (Operation child : ((RootLayoutComponent) op).getList()) {
                    if (child instanceof BoxLayout) {
                        box = (BoxLayout) child;
                        break;
                    }
                }
            }
        }

        assertTrue("Should find BoxLayout", box != null);

        // Verify using reflection
        Field field = Component.class.getDeclaredField("mComponentValues");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Collection<ComponentValue> list =
                (java.util.Collection<ComponentValue>) field.get(box);

        assertTrue(
                "BoxLayout should have ComponentValue mapped from nested brush",
                list != null && !list.isEmpty());
    }
}
