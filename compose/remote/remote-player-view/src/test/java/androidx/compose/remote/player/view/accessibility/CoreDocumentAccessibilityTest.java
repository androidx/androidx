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
package androidx.compose.remote.player.view.accessibility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContextActions;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.ScrollableComponent;

import org.junit.Test;

public class CoreDocumentAccessibilityTest {

    @Test
    public void testPerformAction_runtimeExceptionSwallowed() {
        CoreDocument document = new CoreDocument();
        RemoteContextActions contextActions = new RemoteContextActions() {
            @Override
            public boolean showOnScreen(Component component) {
                return false;
            }

            @Override
            public int scrollByOffset(Component component, int offset) {
                return 0;
            }

            @Override
            public boolean scrollDirection(
                    Component component, ScrollableComponent.ScrollDirection direction) {
                return false;
            }

            @Override
            public boolean performClick(
                    CoreDocument document, Component component, String metadata) {
                throw new NullPointerException("NPE simulation");
            }
        };

        CoreDocumentAccessibility accessibility =
                new CoreDocumentAccessibility(document, contextActions);
        Component component = new Component(null, 1, -1, 0f, 0f, 100f, 100f);

        // ACTION_CLICK constant value is 16
        boolean result = accessibility.performAction(component, 16, null);
        assertFalse(result);
    }

    @Test
    public void testPerformAction_stackOverflowErrorFallsThrough() {
        CoreDocument document = new CoreDocument();
        RemoteContextActions contextActions = new RemoteContextActions() {
            @Override
            public boolean showOnScreen(Component component) {
                return false;
            }

            @Override
            public int scrollByOffset(Component component, int offset) {
                return 0;
            }

            @Override
            public boolean scrollDirection(
                    Component component, ScrollableComponent.ScrollDirection direction) {
                return false;
            }

            @Override
            public boolean performClick(
                    CoreDocument document, Component component, String metadata) {
                throw new StackOverflowError("Stack overflow simulation");
            }
        };

        CoreDocumentAccessibility accessibility =
                new CoreDocumentAccessibility(document, contextActions);
        Component component = new Component(null, 1, -1, 0f, 0f, 100f, 100f);

        // ACTION_CLICK constant value is 16
        assertThrows(StackOverflowError.class, () -> {
            accessibility.performAction(component, 16, null);
        });
    }

    @Test
    public void testPerformAction_outOfMemoryErrorFallsThrough() {
        CoreDocument document = new CoreDocument();
        RemoteContextActions contextActions = new RemoteContextActions() {
            @Override
            public boolean showOnScreen(Component component) {
                return false;
            }

            @Override
            public int scrollByOffset(Component component, int offset) {
                return 0;
            }

            @Override
            public boolean scrollDirection(
                    Component component, ScrollableComponent.ScrollDirection direction) {
                return false;
            }

            @Override
            public boolean performClick(
                    CoreDocument document, Component component, String metadata) {
                throw new OutOfMemoryError("Out of memory simulation");
            }
        };

        CoreDocumentAccessibility accessibility =
                new CoreDocumentAccessibility(document, contextActions);
        Component component = new Component(null, 1, -1, 0f, 0f, 100f, 100f);

        // ACTION_CLICK constant value is 16
        assertThrows(OutOfMemoryError.class, () -> {
            accessibility.performAction(component, 16, null);
        });
    }
}
