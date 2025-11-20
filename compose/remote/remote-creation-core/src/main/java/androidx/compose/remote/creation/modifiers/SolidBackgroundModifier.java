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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Background modifier, takes a color and a shape */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SolidBackgroundModifier implements RecordingModifier.Element {
    float mRed;
    float mGreen;
    float mBlue;
    float mAlpha;

    public SolidBackgroundModifier(int color) {
        mRed = (color >> 16 & 0xff) / 255.0f;
        mGreen = (color >> 8 & 0xff) / 255.0f;
        mBlue = (color & 0xff) / 255.0f;
        mAlpha = (color >> 24 & 0xff) / 255.0f;
    }

    public SolidBackgroundModifier(float red, float green, float blue, float alpha) {
        mRed = red;
        mGreen = green;
        mBlue = blue;
        mAlpha = alpha;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public float getBlue() {
        return mBlue;
    }

    public float getGreen() {
        return mGreen;
    }

    public float getRed() {
        return mRed;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.addModifierBackground(mRed, mGreen, mBlue, mAlpha, 0);
    }
}
