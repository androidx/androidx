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

/** Clip modifier */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClipModifier implements RecordingModifier.Element {
    @NonNull Shape mShape;

    public ClipModifier(@NonNull Shape shape) {
        mShape = shape;
    }

    public @NonNull Shape getShape() {
        return mShape;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        if (mShape instanceof RectShape) {
            writer.addClipRectModifier();
        } else if (mShape instanceof RoundedRectShape) {
            RoundedRectShape rShape = (RoundedRectShape) mShape;
            writer.addRoundClipRectModifier(
                            rShape.getTopStart(),
                            rShape.getTopEnd(),
                            rShape.getBottomStart(),
                            rShape.getBottomEnd());
        }
    }
}
