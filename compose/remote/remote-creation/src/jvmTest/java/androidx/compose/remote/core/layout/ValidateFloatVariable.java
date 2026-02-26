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

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValidateFloatVariable extends TestOperation {
    private final float mIdNan;
    private final float mExpected;

    public ValidateFloatVariable(float idNan, float expected) {
        mIdNan = idNan;
        mExpected = expected;
    }

    @Override
    public boolean apply(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull TestParameters testParameters, @Nullable List<Map<String, Object>> commands) {
        int id = Utils.idFromNan(mIdNan);
        float actualValue = context.getFloat(id);
        if (commands != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("id", id);
            params.put("expected", mExpected);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("actual", actualValue);
            commands.add(command("Validate Float Variable", params, result));
        }
        if (Math.abs(actualValue - mExpected) > 0.001f) {
            throw new AssertionError(
                    "Variable " + id + " actual value " + actualValue + " != expected "
                            + mExpected);
        }
        return false;
    }
}
