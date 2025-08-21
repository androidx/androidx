/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Background modifier, takes a color and a shape */
public class ScrollModifier implements RecordingModifier.Element {

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    int mDirection;
    float mPositionId;
    int mNotches;

    public ScrollModifier(int direction, float positionId, int notches) {
        this.mDirection = direction;
        this.mPositionId = positionId;
        this.mNotches = notches;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        if (mPositionId <= 0f) {
            // direct scrolling, no touch expression
            writer.addModifierScroll(mDirection);
        } else if (mNotches <= 0f) {
            writer.addModifierScroll(mDirection, mPositionId);
        } else {
            writer.addModifierScroll(mDirection, mPositionId, mNotches);
        }
    }
}
