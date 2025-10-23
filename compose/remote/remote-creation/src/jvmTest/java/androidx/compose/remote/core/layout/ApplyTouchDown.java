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

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplyTouchDown extends TestOperation {

    private final float mX;
    private final float mY;
    public ApplyTouchDown(float x, float y) {
        mX = x;
        mY = y;
    }

    @Override
    public boolean apply(@NonNull RemoteContext context, @NonNull CoreDocument document,
            @NonNull TestParameters testParameters, @Nullable List<Map<String, Object>> commands) {
        if (commands != null) {
            Map<String, Object> applyTouchdown = new LinkedHashMap<>();
            applyTouchdown.put("x", mX);
            applyTouchdown.put("y", mY);
            Map<String, Object> testResult = new LinkedHashMap<>();
            commands.add(command("Apply TouchDown", applyTouchdown, testResult));
        }
        document.touchDown(context, mX, mY);
        return false;
    }
}
