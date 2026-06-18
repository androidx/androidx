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

package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.layout.LayoutComponentContent;
import androidx.compose.remote.core.operations.layout.RootLayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;

import org.junit.Test;

public class LayoutMemoryChurnTest {

    @Test
    public void testLayoutMemoryChurn() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        short[] tags = new short[] {Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.DOC_PROFILES};
        Object[] values =
                new Object[] {
                    100, 100, RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL
                };
        buffer.addHeader(tags, values);

        // Build a document structure: RootLayoutComponent containing a BoxLayout
        RootLayoutComponent.apply(buffer.getBuffer(), -1);
        BoxLayout.apply(buffer.getBuffer(), 10, -1, BoxLayout.START, BoxLayout.TOP);
        LayoutComponentContent.apply(buffer.getBuffer(), -1);
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout content end
        ContainerEnd.apply(buffer.getBuffer()); // BoxLayout end
        ContainerEnd.apply(buffer.getBuffer()); // RootLayoutComponent end

        CoreDocument doc = new CoreDocument();
        doc.initFromBuffer(buffer);

        MacroTest.MockRemoteContext context = new MacroTest.MockRemoteContext();
        PaintContext paintContext = org.mockito.Mockito.mock(PaintContext.class);
        org.mockito.Mockito.when(paintContext.getContext()).thenReturn(context);
        org.mockito.Mockito.when(paintContext.getMeasureVersion())
                .thenReturn(
                        androidx.compose.remote.core.operations.layout.managers.LayoutManager
                                .DEFAULT_MEASURE_TYPE);
        context.setPaintContext(paintContext);
        context.setAnimationEnabled(false);

        // 1st pass: warm-up to initialize pool
        doc.measure(context, 0f, 100f, 0f, 100f);

        // Reset allocation counter
        ComponentMeasure.sAllocationCount = 0;

        // Run multiple layout measure passes
        for (int i = 0; i < 5; i++) {
            doc.measure(context, 0f, 100f, 0f, 100f);
        }

        assertEquals(
                "Subsequent measure passes should result in 0 allocations",
                0,
                ComponentMeasure.sAllocationCount);
    }
}
