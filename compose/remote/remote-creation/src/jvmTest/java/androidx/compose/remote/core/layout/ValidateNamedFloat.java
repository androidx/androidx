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

package androidx.compose.remote.core.layout;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValidateNamedFloat extends TestOperation {
    private final String mName;
    private final float mExpected;
    private final float mEpsilon;

    public ValidateNamedFloat(String name, float expected) {
        this(name, expected, 0.001f);
    }

    public ValidateNamedFloat(String name, float expected, float epsilon) {
        mName = name;
        mExpected = expected;
        mEpsilon = epsilon;
    }

    @Override
    public boolean apply(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull TestParameters testParameters, @Nullable List<Map<String, Object>> commands) {
        if (!(context instanceof MockRemoteContext)) {
            return false;
        }
        MockRemoteContext mockContext = (MockRemoteContext) context;
        Integer id = mockContext.varNamesMap.get(mName);
        if (id == null) {
            throw new AssertionError("Named variable " + mName + " not found");
        }
        float actualValue = mockContext.getFloat(id);
        if (commands != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", mName);
            params.put("expected", mExpected);
            Map<String, Object> testResult = new LinkedHashMap<>();
            testResult.put("actual", actualValue);
            commands.add(command("Validate Named Float", params, testResult));
        }
        assertEquals("Checking Named Float " + mName, mExpected, actualValue, mEpsilon);
        return false;
    }
}
