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

import android.graphics.Shader;

import androidx.compose.remote.creation.RemoteComposeWriter;

/** Background modifier, takes a color and a shape */
public class BackgroundModifier implements RecordingModifier.Element {

    Shader mShader;
    int mColor;

    public BackgroundModifier(Shader shader, int color) {
        this.mShader = shader;
        this.mColor = color;
    }

    public Shader getShader() {
        return mShader;
    }

    @Override
    public void write(RemoteComposeWriter writer) {
        if (mShader == null) {
            writer.getBuffer().addModifierBackground(mColor, 0);
        }
    }
}
