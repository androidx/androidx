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

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApplyComponentTouchDown extends TestComponentOperation {

    private final float mX;
    private final float mY;
    public ApplyComponentTouchDown(int id, float x, float y) {
        super(id);
        mX = x;
        mY = y;
    }

    @Override
    public boolean apply(@NonNull Component component) {
        ArrayList<Operation> ops = component.getList();
        boolean forceRepaint = false;
        for (int i = 0; i < ops.size(); i++) {
            Operation op = ops.get(i);
            if (op instanceof ComponentModifiers) {
                ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                for (int j = 0; j < mods.size(); j++) {
                    ModifierOperation m = mods.get(j);
                    if (m instanceof TouchDownModifierOperation) {
                        ((TouchDownModifierOperation) m)
                                .onTouchDown(getContext(), getDocument(), component, mX, mY);
                        forceRepaint = true;
                    }
                }
            }
        }
        if (getCommands() != null) {
            Map<String, Object> applyTouchdown = new LinkedHashMap<>();
            applyTouchdown.put("id", getId());
            applyTouchdown.put("x", mX);
            applyTouchdown.put("y", mY);
            Map<String, Object> testResult = new LinkedHashMap<>();
            getCommands().add(command("Apply Component TouchDown", applyTouchdown, testResult));
        }
        return forceRepaint;
    }
}
