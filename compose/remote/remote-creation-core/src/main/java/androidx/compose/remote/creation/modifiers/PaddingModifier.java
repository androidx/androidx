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

/** Padding modifier */
public class PaddingModifier implements RecordingModifier.Element {
    float mLeft;
    float mTop;
    float mRight;
    float mBottom;

    public PaddingModifier(float left, float top, float right, float bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    public float getLeft() {
        return mLeft;
    }

    public float getTop() {
        return mTop;
    }

    public float getRight() {
        return mRight;
    }

    public float getBottom() {
        return mBottom;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.addModifierPadding(mLeft, mTop, mRight, mBottom);
    }
}
