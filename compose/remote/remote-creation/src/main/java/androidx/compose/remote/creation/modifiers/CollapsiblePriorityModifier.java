/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;

import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Collapsible Priority modifier */
public class CollapsiblePriorityModifier implements RecordingModifier.Element {

    float mPriority;
    int mOrientation;

    public static final int HORIZONTAL = CollapsiblePriority.HORIZONTAL;
    public static final int VERTICAL = CollapsiblePriority.VERTICAL;

    public CollapsiblePriorityModifier(int orientation, float priority) {
        mOrientation = orientation;
        mPriority = priority;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        CollapsiblePriorityModifierOperation.apply(
                writer.getBuffer().getBuffer(), mOrientation, mPriority);
    }
}
