/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.core.layout;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.layout.Component;

import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestComponentVisibility extends TestComponentOperation {
    private final int mVisibility;

    public TestComponentVisibility(int id, int visibility) {
        super(id);
        mVisibility = visibility;
    }

    @Override
    public boolean apply(@NonNull Component component) {
        if (getCommands() != null) {
            Map<String, Object> testVisibility = new LinkedHashMap<>();
            testVisibility.put("visibility", mVisibility);
            Map<String, Object> testResult = new LinkedHashMap<>();
            testResult.put("result", component.mVisibility);
            getCommands().add(command("Test Visibility", testVisibility, testResult));
        }
        assertEquals("Checking Component " + getId() + " visibility", mVisibility,
                component.mVisibility);
        return false;
    }
}
